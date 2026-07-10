package com.example.vaultbeat.ui.nowplaying

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.vaultbeat.core.model.Song
import com.example.vaultbeat.library.LibraryRepository
import com.example.vaultbeat.player.PlayerConnection
import com.example.vaultbeat.player.PlayerUiState
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
    private val repository: LibraryRepository
) : ViewModel() {
    private val _state = MutableStateFlow(NowPlayingUiState())
    val state: StateFlow<NowPlayingUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            // Observe player state and update UI.
            player.state.collect { ps ->
                // Fetch current song when the currentSongId is present
                val currentSong = ps.currentSongId?.let { id ->
                    try { repository.songs().firstOrNull { it.id == id } } catch (e: Exception) { null }
                }
                _state.value = NowPlayingUiState(song = currentSong, playerState = ps)
            }
        }
    }

    // Actions forwarded to PlayerConnection
    fun togglePlayPause() = player.togglePlayPause()
    fun next() = player.next()
    fun previous() = player.previous()
    fun seekBy(deltaMs: Long) = player.seekBy(deltaMs)
    fun seekTo(positionMs: Long) = player.seekTo(positionMs)
    fun toggleShuffle() = player.toggleShuffle()
    fun cycleRepeat() = player.cycleRepeat()
}
