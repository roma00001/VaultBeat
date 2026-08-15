package com.vaultbeat.ui.nowplaying

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vaultbeat.core.model.Song
import com.vaultbeat.data.local.PlaylistEntity
import com.vaultbeat.library.LibraryRepository
import com.vaultbeat.library.PlaylistRepository
import com.vaultbeat.player.PlayerConnection
import com.vaultbeat.player.PlayerUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class NowPlayingUiState(
    val song: Song? = null,
    val playerState: PlayerUiState = PlayerUiState()
)

@HiltViewModel
class NowPlayingViewModel @Inject constructor(
    private val player: PlayerConnection,
    private val repository: LibraryRepository,
    private val playlistRepository: PlaylistRepository
) : ViewModel() {
    private val _state = MutableStateFlow(NowPlayingUiState())
    val state: StateFlow<NowPlayingUiState> = _state.asStateFlow()

    private val _playlists = MutableStateFlow<List<PlaylistEntity>>(emptyList())
    val playlists: StateFlow<List<PlaylistEntity>> = _playlists.asStateFlow()

    private val _sessionSongs = MutableStateFlow<List<Song>>(emptyList())

    init {
        viewModelScope.launch {
            // Observe player state and update UI.
            player.state.collect { ps ->
                // Fetch current song when the currentSongId is present
                val currentSong = ps.currentSongId?.let { id ->
                    _sessionSongs.value.find { it.id == id }
                        ?: try { repository.songs().firstOrNull { it.id == id } } catch (e: Exception) { null }
                }
                _state.value = NowPlayingUiState(song = currentSong, playerState = ps)
            }
        }
        viewModelScope.launch {
            playlistRepository.observePlaylists().collect { playlists ->
                _playlists.value = playlists
            }
        }
    }

    fun createPlaylist(name: String, onCreated: (Long) -> Unit) {
        if (name.isBlank()) return
        viewModelScope.launch {
            val playlistId = playlistRepository.createPlaylist(name)
            onCreated(playlistId)
        }
    }

    fun deletePlaylist(playlistId: Long) {
        viewModelScope.launch { playlistRepository.deletePlaylist(playlistId) }
    }

    fun movePlaylist(orderedPlaylistIds: List<Long>) {
        viewModelScope.launch { playlistRepository.movePlaylist(orderedPlaylistIds) }
    }

    fun addSongToPlaylist(playlistId: Long, songId: Long) {
        viewModelScope.launch { playlistRepository.addSongToPlaylist(playlistId, songId) }
    }

    fun removeSongFromPlaylist(playlistId: Long, songId: Long) {
        viewModelScope.launch { playlistRepository.removeSongFromPlaylist(playlistId, songId) }
    }

    fun reorderPlaylistSongs(playlistId: Long, orderedSongIds: List<Long>) {
        viewModelScope.launch { playlistRepository.reorderPlaylistSongs(playlistId, orderedSongIds) }
    }

    fun renamePlaylist(playlistId: Long, name: String) {
        viewModelScope.launch { playlistRepository.renamePlaylist(playlistId, name) }
    }

    fun updatePlaylistCover(playlistId: Long, songId: Long?) {
        viewModelScope.launch { playlistRepository.updatePlaylistCover(playlistId, songId) }
    }

    fun updatePlaylistSortOrder(playlistId: Long, sortOrder: String) {
        viewModelScope.launch { playlistRepository.updatePlaylistSortOrder(playlistId, sortOrder) }
    }

    fun observePlaylistSongIds(playlistId: Long) = playlistRepository.observePlaylistSongIds(playlistId)

    fun updateSessionSongs(songs: List<Song>) {
        _sessionSongs.value = songs
    }

    // Actions forwarded to PlayerConnection
    fun togglePlayPause() = player.togglePlayPause()
    fun next() = player.next()
    fun previous() = player.previous()
    fun seekBy(deltaMs: Long) = player.seekBy(deltaMs)
    fun seekTo(positionMs: Long) = player.seekTo(positionMs)
    fun toggleShuffle() = player.toggleShuffle()
    fun cycleRepeat() = player.cycleRepeat()
    fun playAtIndex(index: Int) = player.playAtIndex(index)
}

