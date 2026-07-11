package com.example.vaultbeat.ui.nowplaying

import androidx.compose.runtime.Composable
import com.example.vaultbeat.core.model.Song

/**
 * Implementation moved. This file is kept as a compatibility wrapper so callers that referenced
 * NowPlayingScreen2 can continue to work while the canonical API is NowPlayingScreen.
 */
@Composable
fun NowPlayingScreen2(
    viewModel: NowPlayingViewModel,
    songs: List<Song>,
    onSongSelected: (Int) -> Unit,
    onPlayAlbum: (List<Song>, Int) -> Unit,
    onRefresh: () -> Unit,
    onClose: () -> Unit = {}
) {
    // Forward to the canonical implementation
    NowPlayingScreen(
        viewModel = viewModel,
        songs = songs,
        onSongSelected = onSongSelected,
        onPlayAlbum = onPlayAlbum,
        onRefresh = onRefresh,
        onClose = onClose
    )
}
