package com.example.vaultbeat.ui.library

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.example.vaultbeat.core.model.Song
import com.example.vaultbeat.player.PlayerUiState

@Composable
fun LibraryScreen(
    state: LibraryUiState,
    player: PlayerUiState,
    onRefresh: () -> Unit,
    onSongSelected: (Int) -> Unit,
    onTogglePlayback: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onRewind: () -> Unit,
    onForward: () -> Unit,
    onShuffle: () -> Unit,
    onRepeat: () -> Unit,
    onOpenNowPlaying: () -> Unit
) {
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 22.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text("VAULTBEAT", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                Text("Biblioteca", style = MaterialTheme.typography.headlineMedium)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("LOCAL", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(6.dp))
                Button(onClick = onOpenNowPlaying) { Text("Ahora") }
            }
        }
        when {
            state.loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            state.error != null -> EmptyLibrary("No hemos podido abrir tu biblioteca.", "Reintentar", onRefresh)
            state.songs.isEmpty() -> EmptyLibrary("No hay canciones locales todavía.", "Actualizar biblioteca", onRefresh)
            else -> {
                LazyColumn(contentPadding = PaddingValues(bottom = 100.dp)) {
                    item {
                        Text(
                            "${state.songs.size} canciones en este dispositivo",
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    itemsIndexed(state.songs, key = { _, song -> song.id }) { index, song ->
                        SongRow(song, song.id == player.currentSongId, player.isPlaying) { onSongSelected(index) }
                    }
                }
                state.songs.firstOrNull { it.id == player.currentSongId }?.let { song ->
                    MiniPlayer(song, player, onTogglePlayback, onPrevious, onNext, onRewind, onForward, onShuffle, onRepeat)
                }
            }
        }
    }
}

@Composable
private fun EmptyLibrary(message: String, action: String, onAction: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        ArtworkPlaceholder(Modifier.size(120.dp))
        Spacer(Modifier.height(24.dp))
        Text(message, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(12.dp))
        Text("VaultBeat solo reproduce archivos que ya están guardados en tu teléfono.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(24.dp))
        Button(onClick = onAction) { Text(action) }
    }
}

@Composable
private fun SongRow(song: Song, selected: Boolean, isPlaying: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (song.albumArtUri != null) {
            AsyncImage(song.albumArtUri, null, Modifier.size(52.dp).clip(RoundedCornerShape(13.dp)), contentScale = ContentScale.Crop)
        } else ArtworkPlaceholder(Modifier.size(52.dp))
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(song.title, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.titleMedium, color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
            Text(song.artist, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(if (selected && isPlaying) "SONANDO" else formatDuration(song.durationMs), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun MiniPlayer(
    song: Song,
    player: PlayerUiState,
    onToggle: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onRewind: () -> Unit,
    onForward: () -> Unit,
    onShuffle: () -> Unit,
    onRepeat: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(12.dp).semantics { contentDescription = "Minirreproductor: ${song.title}" },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(18.dp)
    ) {
        Row(Modifier.padding(start = 10.dp, top = 10.dp, end = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            ArtworkPlaceholder(Modifier.size(42.dp))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) { Text(song.title, maxLines = 1); Text(song.artist, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            Button(onClick = onToggle) { Text(if (player.isPlaying) "Pausa" else "Reproducir") }
        }
        Row(Modifier.fillMaxWidth().padding(8.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
            Button(onClick = onShuffle) { Text(if (player.shuffleEnabled) "Aleatorio ✓" else "Aleatorio") }
            Button(onClick = onPrevious) { Text("Anterior") }
            Button(onClick = onRewind) { Text("−10 s") }
            Button(onClick = onForward) { Text("+10 s") }
            Button(onClick = onNext) { Text("Siguiente") }
            Button(onClick = onRepeat) { Text("Repetir ${player.repeatMode}") }
        }
    }
}

@Composable
private fun ArtworkPlaceholder(modifier: Modifier = Modifier) {
    Box(modifier.clip(RoundedCornerShape(16.dp)).background(Brush.linearGradient(listOf(MaterialTheme.colorScheme.primary.copy(alpha = .85f), Color(0xFF40372E), MaterialTheme.colorScheme.secondary.copy(alpha = .7f)))))
}

private fun formatDuration(durationMs: Long): String {
    val seconds = durationMs / 1000
    return "%d:%02d".format(seconds / 60, seconds % 60)
}
