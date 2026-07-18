package com.example.vaultbeat.ui.library

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.vaultbeat.core.model.Song
import com.example.vaultbeat.data.remote.ITunesApi
import com.example.vaultbeat.data.remote.YtdlpExecutor
import com.example.vaultbeat.library.LibraryRepository
import com.example.vaultbeat.player.PlayerConnection
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LibraryUiState(
    val loading: Boolean = false,
    val songs: List<Song> = emptyList(),
    val error: String? = null
)

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val repository: LibraryRepository,
    val player: PlayerConnection,
    private val iTunesApi: ITunesApi,
    private val ytdlpExecutor: YtdlpExecutor,
    @ApplicationContext private val context: Context
) : ViewModel() {
    private val _state = MutableStateFlow(LibraryUiState())
    val state: StateFlow<LibraryUiState> = _state.asStateFlow()

    init {
        repository.onLibraryChanged
            .onEach { loadLibrary() }
            .launchIn(viewModelScope)
    }

    fun loadLibrary() {
        if (_state.value.loading) return
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true, error = null)
            runCatching { repository.songs() }
                .onSuccess { _state.value = LibraryUiState(songs = it) }
                .onFailure { _state.value = LibraryUiState(error = "No se pudo leer la música local.") }
        }
    }

    fun deleteSongFromDevice(song: Song) {
        viewModelScope.launch {
            try {
                context.contentResolver.delete(song.uri, null, null)
                loadLibrary()
            } catch (e: Exception) {
                android.util.Log.e("LibraryViewModel", "Error eliminando archivo: ${e.message}")
            }
        }
    }

    suspend fun searchCover(song: Song): String? {
        val iTunesResult = iTunesApi.getArtworkUrl(song.artist, song.title)
        if (iTunesResult != null) return iTunesResult
        
        return ytdlpExecutor.getBestThumbnail(song.artist, song.title)
    }

    fun updateSongCover(songId: Long, artworkUrl: String) {
        viewModelScope.launch {
            repository.downloadCoverArt(songId, artworkUrl)
            loadLibrary()
        }
    }

    suspend fun reloadAllCovers(onProgress: (Int, Int) -> Unit) {
        val currentSongs = _state.value.songs
        val total = currentSongs.size
        currentSongs.forEachIndexed { index, song ->
            onProgress(index + 1, total)
            val url = iTunesApi.getArtworkUrl(song.artist, song.title)
            if (url != null) {
                repository.downloadCoverArt(song.id, url)
            }
            delay(500) // Avoid rate limiting
        }
        loadLibrary()
    }
}
