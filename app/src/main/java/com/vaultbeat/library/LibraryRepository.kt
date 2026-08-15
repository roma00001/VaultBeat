package com.vaultbeat.library

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.vaultbeat.core.model.Song
import com.vaultbeat.core.network.model.SearchResult
import com.vaultbeat.data.library.MediaStoreMusicDataSource
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LibraryRepository @Inject constructor(
    private val mediaStore: MediaStoreMusicDataSource,
    @ApplicationContext private val context: Context
) {
    private val _onLibraryChanged = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val onLibraryChanged = _onLibraryChanged.asSharedFlow()

    suspend fun songs(): List<Song> = withContext(Dispatchers.IO) { mediaStore.loadSongs() }

    suspend fun insertIntoMediaStore(file: File, result: SearchResult): Long? = withContext(Dispatchers.IO) {
        val contentValues = ContentValues().apply {
            put(MediaStore.Audio.Media.DISPLAY_NAME, file.name)
            put(MediaStore.Audio.Media.TITLE, result.title)
            put(MediaStore.Audio.Media.ARTIST, result.artist)
            put(MediaStore.Audio.Media.ALBUM, result.title)
            put(MediaStore.Audio.Media.MIME_TYPE, "audio/mpeg")
            put(MediaStore.Audio.Media.IS_MUSIC, 1)
            put(MediaStore.Audio.Media.DURATION, result.durationMs)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Audio.Media.RELATIVE_PATH, Environment.DIRECTORY_MUSIC + "/VaultBeat")
                put(MediaStore.Audio.Media.IS_PENDING, 1)
            } else {
                val musicDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC)
                val vaultBeatDir = File(musicDir, "VaultBeat").apply { mkdirs() }
                put(MediaStore.Audio.Media.DATA, File(vaultBeatDir, file.name).absolutePath)
            }
        }
        val uri = context.contentResolver.insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, contentValues)
        if (uri != null) {
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                file.inputStream().use { inputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val updateValues = ContentValues().apply {
                    put(MediaStore.Audio.Media.IS_PENDING, 0)
                }
                context.contentResolver.update(uri, updateValues, null, null)
            }
            _onLibraryChanged.emit(Unit)
            return@withContext ContentUris.parseId(uri)
        }
        null
    }

    suspend fun downloadCoverArt(songId: Long, thumbnailUrl: String) = withContext(Dispatchers.IO) {
        try {
            if (thumbnailUrl.isEmpty()) return@withContext

            val coversDir = File(context.filesDir, "covers").apply { mkdirs() }
            val coverFile = File(coversDir, "$songId.jpg")

            val connection = java.net.URL(thumbnailUrl).openConnection() as java.net.HttpURLConnection
            connection.apply {
                connectTimeout = 10000
                readTimeout = 10000
                instanceFollowRedirects = true
                setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
            }

            connection.inputStream.use { input ->
                coverFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            android.util.Log.d("LibraryRepository", "Portada descargada exitosamente para canción: $songId")
        } catch (e: Exception) {
            android.util.Log.w("LibraryRepository", "Error descargando portada: ${e.message}")
        }
    }
}

