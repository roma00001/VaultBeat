package com.example.vaultbeat.ui.vaultbeat

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.vaultbeat.core.network.model.DownloadProgress
import com.example.vaultbeat.core.network.model.SearchResult
import com.example.vaultbeat.data.remote.YtdlpExecutor
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject
import android.content.ContentValues
import android.content.ContentUris
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.example.vaultbeat.library.LibraryRepository
import com.example.vaultbeat.core.utils.formatDuration
import com.example.vaultbeat.library.PlaylistRepository
import com.example.vaultbeat.data.local.PlaylistEntity
import com.example.vaultbeat.core.network.DownloadService
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.joinAll

data class VaultBeatUiState(
    val searchResults: List<SearchResult> = emptyList(),
    val isSearching: Boolean = false,
    val activeDownloads: Map<String, DownloadProgress> = emptyMap(),
    val downloadedIds: Set<String> = emptySet(),
    val playlists: List<PlaylistEntity> = emptyList(),
    val isBatchDownloading: Boolean = false,
    val batchProgress: Int = 0,
    val batchTotal: Int = 0
)

data class DownloadRecord(
    val title: String,
    val duration: String,
    val path: String
)

@HiltViewModel
class VaultBeatViewModel@Inject constructor(
    private val ytdlpExecutor: YtdlpExecutor,
    private val playlistRepository: PlaylistRepository,
    private val libraryRepository: LibraryRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _state = MutableStateFlow(VaultBeatUiState())
    val state: StateFlow<VaultBeatUiState> = _state.asStateFlow()

    init {
        playlistRepository.observePlaylists()
            .onEach { playlists ->
                _state.value = _state.value.copy(playlists = playlists)
            }
            .launchIn(viewModelScope)

        DownloadService.activeDownloads
            .onEach { downloads ->
                val currentDownloaded = _state.value.downloadedIds.toMutableSet()
                downloads.forEach { (id, progress) ->
                    if (progress is DownloadProgress.Completed) {
                        currentDownloaded.add(id)
                    }
                }
                _state.value = _state.value.copy(
                    activeDownloads = downloads,
                    downloadedIds = currentDownloaded
                )
            }
            .launchIn(viewModelScope)

        DownloadService.batchProgress
            .onEach { batch ->
                if (batch != null) {
                    _state.value = _state.value.copy(
                        isBatchDownloading = true,
                        batchProgress = batch.first,
                        batchTotal = batch.second
                    )
                } else {
                    _state.value = _state.value.copy(isBatchDownloading = false)
                }
            }
            .launchIn(viewModelScope)
    }

    fun search(query: String) {
        if (query.isBlank()) return
        
        viewModelScope.launch {
            try {
                _state.value = _state.value.copy(isSearching = true)
                val results = ytdlpExecutor.search(query)
                _state.value = _state.value.copy(searchResults = results, isSearching = false)
            } catch (e: Exception) {
                android.util.Log.e("VaultBeatViewModel", "Error en búsqueda: ${e.message}", e)
                _state.value = _state.value.copy(searchResults = emptyList(), isSearching = false)
            }
        }
    }

    fun downloadToPlaylist(result: SearchResult, playlistId: Long, onComplete: () -> Unit = {}) {
        DownloadService.startDownload(context, result, playlistId)
        onComplete()
    }

    fun download(result: SearchResult, onComplete: () -> Unit = {}) {
        DownloadService.startDownload(context, result)
        onComplete()
    }

    fun downloadPlaylist(results: List<SearchResult>, playlistId: Long?, onRefresh: () -> Unit) {
        DownloadService.startBatchDownload(context, results, playlistId)
        onRefresh()
    }

    // MediaStore and Cover Art logic moved to LibraryRepository
}
