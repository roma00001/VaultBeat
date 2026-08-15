package com.vaultbeat.core.network

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.vaultbeat.R
import com.vaultbeat.core.network.model.SearchResult
import com.vaultbeat.core.network.model.DownloadProgress
import com.vaultbeat.data.remote.YtdlpExecutor
import com.vaultbeat.library.LibraryRepository
import com.vaultbeat.library.PlaylistRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.io.File
import javax.inject.Inject

@AndroidEntryPoint
class DownloadService : Service() {

    @Inject
    lateinit var ytdlpExecutor: YtdlpExecutor

    @Inject
    lateinit var libraryRepository: LibraryRepository

    @Inject
    lateinit var playlistRepository: PlaylistRepository

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var downloadJob: Job? = null

    companion object {
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "download_channel"
        private const val ACTION_START_DOWNLOAD = "ACTION_START_DOWNLOAD"
        private const val ACTION_CANCEL_DOWNLOAD = "ACTION_CANCEL_DOWNLOAD"
        private const val EXTRA_SONG = "EXTRA_SONG"
        private const val EXTRA_PLAYLIST_ID = "EXTRA_PLAYLIST_ID"
        private const val EXTRA_SONG_LIST = "EXTRA_SONG_LIST"

        private val _activeDownloads = MutableStateFlow<Map<String, DownloadProgress>>(emptyMap())
        val activeDownloads = _activeDownloads.asStateFlow()

        private val _batchProgress = MutableStateFlow<Pair<Int, Int>?>(null)
        val batchProgress = _batchProgress.asStateFlow()

        private val _onDownloadCompleted = MutableSharedFlow<String>(
            extraBufferCapacity = 10,
            onBufferOverflow = BufferOverflow.DROP_OLDEST
        )
        val onDownloadCompleted = _onDownloadCompleted.asSharedFlow()

        fun startDownload(context: Context, song: SearchResult, playlistId: Long? = null) {
            val intent = Intent(context, DownloadService::class.java).apply {
                action = ACTION_START_DOWNLOAD
                putExtra(EXTRA_SONG, song)
                playlistId?.let { putExtra(EXTRA_PLAYLIST_ID, it) }
            }
            context.startForegroundService(intent)
        }

        fun startBatchDownload(context: Context, songs: List<SearchResult>, playlistId: Long? = null) {
            val intent = Intent(context, DownloadService::class.java).apply {
                action = ACTION_START_DOWNLOAD
                putParcelableArrayListExtra(EXTRA_SONG_LIST, ArrayList(songs))
                playlistId?.let { putExtra(EXTRA_PLAYLIST_ID, it) }
            }
            context.startForegroundService(intent)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_DOWNLOAD -> {
                val song = intent.getParcelableExtra<SearchResult>(EXTRA_SONG)
                val songList = intent.getParcelableArrayListExtra<SearchResult>(EXTRA_SONG_LIST)
                val playlistId = intent.getLongExtra(EXTRA_PLAYLIST_ID, -1L).takeIf { it != -1L }

                if (song != null) {
                    handleSingleDownload(song, playlistId)
                } else if (songList != null) {
                    handleBatchDownload(songList, playlistId)
                }
            }
            ACTION_CANCEL_DOWNLOAD -> {
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    private fun handleSingleDownload(song: SearchResult, playlistId: Long?) {
        startForeground(NOTIFICATION_ID, createNotification(song.title, 0))
        downloadJob = serviceScope.launch {
            downloadSong(song, playlistId)
            if (_activeDownloads.value.isEmpty()) {
                stopSelf()
            }
        }
    }

    private fun handleBatchDownload(songs: List<SearchResult>, playlistId: Long?) {
        startForeground(NOTIFICATION_ID, createNotification("Starting batch download...", 0))
        downloadJob = serviceScope.launch {
            _batchProgress.value = 0 to songs.size
            val semaphore = Semaphore(3)
            var completedCount = 0
            
            songs.map { song ->
                launch {
                    semaphore.withPermit {
                        downloadSong(song, playlistId)
                        synchronized(this@DownloadService) {
                            completedCount++
                            _batchProgress.value = completedCount to songs.size
                            updateNotification("Downloading playlist ($completedCount/${songs.size})", 0)
                        }
                    }
                }
            }.joinAll()
            _batchProgress.value = null
            stopSelf()
        }
    }

    private suspend fun downloadSong(song: SearchResult, playlistId: Long?) {
        val uniqueDirName = "dl_svc_${System.currentTimeMillis()}_${song.id}"
        val outputDir = File(getExternalFilesDir(null), uniqueDirName).apply { mkdirs() }
        val outputPath = outputDir.absolutePath

        try {
            ytdlpExecutor.download(song.url, outputPath).collect { progress ->
                updateProgress(song.id, progress)
                if (progress is DownloadProgress.Downloading) {
                    updateNotification(song.title, progress.percentage.toInt())
                } else if (progress is DownloadProgress.Completed) {
                    processCompletedDownload(song, outputDir, playlistId)
                    _onDownloadCompleted.emit(song.id)
                }
            }
        } catch (e: Exception) {
            Log.e("DownloadService", "Error downloading ${song.title}: ${e.message}")
            updateProgress(song.id, DownloadProgress.Error(e.message ?: "Unknown error"))
        } finally {
            synchronized(_activeDownloads) {
                val current = _activeDownloads.value.toMutableMap()
                current.remove(song.id)
                _activeDownloads.value = current
            }
            outputDir.deleteRecursively()
        }
    }

    private suspend fun processCompletedDownload(song: SearchResult, outputDir: File, playlistId: Long?) {
        val downloadedFile = outputDir.listFiles()?.firstOrNull { it.extension == "mp3" }
        if (downloadedFile != null) {
            val songId = libraryRepository.insertIntoMediaStore(downloadedFile, song)
            if (songId != null) {
                if (playlistId != null) {
                    playlistRepository.addSongToPlaylist(playlistId, songId)
                }
                libraryRepository.downloadCoverArt(songId, song.thumbnailUrl)
            }
        }
    }

    private fun updateProgress(songId: String, progress: DownloadProgress) {
        synchronized(_activeDownloads) {
            val current = _activeDownloads.value.toMutableMap()
            current[songId] = progress
            _activeDownloads.value = current
        }
    }

    private fun createNotification(title: String, progress: Int): Notification {
        val cancelIntent = Intent(this, DownloadService::class.java).apply {
            action = ACTION_CANCEL_DOWNLOAD
        }
        val pendingCancelIntent = PendingIntent.getService(this, 0, cancelIntent, PendingIntent.FLAG_IMMUTABLE)

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText("Downloading...")
            .setSmallIcon(R.drawable.ic_download) // Ensure this exists
            .setProgress(100, progress, false)
            .setOngoing(true)
            .setSilent(true)
            .addAction(R.drawable.ic_close, "Cancel", pendingCancelIntent)
            .build()
    }

    private fun updateNotification(title: String, progress: Int) {
        val notification = createNotification(title, progress)
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as android.app.NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        downloadJob?.cancel()
        serviceScope.cancel()
        _activeDownloads.value = emptyMap()
        _batchProgress.value = null
    }
}

