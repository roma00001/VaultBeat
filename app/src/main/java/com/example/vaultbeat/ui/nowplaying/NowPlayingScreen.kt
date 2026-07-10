package com.example.vaultbeat.ui.nowplaying

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.blur
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import androidx.compose.material3.SliderDefaults
import coil3.compose.AsyncImage
import com.example.vaultbeat.core.model.Song

@Composable
fun MenuItem(text: String, selected: Boolean, onClick: () -> Unit) {
    val bg = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.18f) else Color.Transparent
    Box(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Text(
            text,
            style = TextStyle(fontSize = 14.sp, color = if (selected) MaterialTheme.colorScheme.primary else Color(0xFFDDDDDD)),
            modifier = Modifier
                .background(bg)
                .clickable { onClick() }
                .padding(6.dp)
        )
    }
}

fun formatDuration(ms: Long): String {
    val seconds = ms / 1000
    return "%d:%02d".format(seconds / 60, seconds % 60)
}

@Composable
fun NowPlayingScreen(
    viewModel: NowPlayingViewModel,
    songs: List<Song>,
    onSongSelected: (Int) -> Unit,
    onRefresh: () -> Unit,
    onClose: () -> Unit = {}
) {
    val state = viewModel.state.collectAsState()
    val ui = state.value
    BackHandler { onClose() }
 
    val position = ui.playerState.positionMs.coerceAtLeast(0L)
    val duration = ui.playerState.durationMs.coerceAtLeast(0L)
     
    // menu state
    val menu = remember { androidx.compose.runtime.mutableStateOf("NOW_PLAYING") }
    val selectedIndex = remember { androidx.compose.runtime.mutableStateOf<Int?>(null) }
 
    Box(modifier = Modifier.fillMaxSize()) {
        // Background
        ui.song?.albumArtUri?.let { uri ->
            AsyncImage(
                model = uri,
                contentDescription = null,
                modifier = Modifier.fillMaxSize().blur(28.dp),
                contentScale = ContentScale.Crop
            )
            Box(modifier = Modifier.fillMaxSize().background(Color(0xB3BDBDBD)))
        } ?: run {
            Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceVariant))
        }

        Column(modifier = Modifier.fillMaxSize()) {
            Row(modifier = Modifier.weight(3f).fillMaxWidth().padding(18.dp)) {
                // Menu
                Card(
                    modifier = Modifier.weight(2.2f).fillMaxHeight().widthIn(min = 200.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.16f))
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(16.dp),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text("VAULTBEAT", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(24.dp))
                        MenuItem(text = "MUSIC", selected = menu.value == "MUSIC") { menu.value = "MUSIC" }
                        MenuItem(text = "PODCASTS", selected = menu.value == "PODCASTS") { menu.value = "PODCASTS" }
                        MenuItem(text = "ARTISTS", selected = menu.value == "ARTISTS") { menu.value = "ARTISTS" }
                        MenuItem(text = "ALBUMS", selected = menu.value == "ALBUMS") { menu.value = "ALBUMS" }
                        MenuItem(text = "NOW PLAYING", selected = menu.value == "NOW_PLAYING") { menu.value = "NOW_PLAYING" }
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Main content area
                Card(
                    modifier = Modifier.weight(4f).fillMaxHeight(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = if (menu.value == "NOW_PLAYING") Color.Transparent else MaterialTheme.colorScheme.surface.copy(alpha = 0.92f))
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Top
                    ) {
                        when (menu.value) {
                            "MUSIC" -> {
                                LazyColumn(contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)) {
                                    itemsIndexed(items = songs, key = { _, s -> s.id }) { index, song ->
                                        Row(modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { selectedIndex.value = index; onSongSelected(index) }
                                            .padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                                            if (song.albumArtUri != null) {
                                                AsyncImage(model = song.albumArtUri, contentDescription = null, modifier = Modifier.size(52.dp).clip(RoundedCornerShape(12.dp)), contentScale = ContentScale.Crop)
                                            } else {
                                                Box(modifier = Modifier.size(52.dp).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surfaceVariant))
                                            }
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(song.title, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.titleMedium)
                                                Text(song.artist, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                            Text(formatDuration(song.durationMs), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                }
                            }
                            "NOW_PLAYING" -> {
                                if (ui.song == null) {
                                    CircularProgressIndicator()
                                } else {
                                    Column(
                                        modifier = Modifier.fillMaxSize(),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        AsyncImage(
                                            model = ui.song.albumArtUri,
                                            contentDescription = "Album art",
                                            modifier = Modifier.size(200.dp).clip(RoundedCornerShape(24.dp)),
                                            contentScale = ContentScale.Crop
                                        )
                                        Spacer(modifier = Modifier.height(24.dp))
                                        Column(modifier = Modifier.fillMaxWidth(0.95f), horizontalAlignment = Alignment.Start) {
                                            Text(
                                                ui.song.title,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis,
                                                textAlign = TextAlign.Start,
                                                style = TextStyle(fontSize = 28.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                                            )
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text(
                                                ui.song.artist,
                                                textAlign = TextAlign.Start,
                                                style = TextStyle(fontSize = 16.sp, color = Color(0xFFDDDDDD))
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                ui.song.album,
                                                textAlign = TextAlign.Start,
                                                style = TextStyle(fontSize = 12.sp, color = Color(0xFFBFBFBF))
                                            )
                                        }
                                    }
                                }
                            }
                            else -> {
                                Text("Sección: ${menu.value}", style = MaterialTheme.typography.titleMedium)
                            }
                        }
                    }
                }
            }

            // Bottom wheel
            Box(modifier = Modifier.weight(1f).fillMaxWidth().padding(12.dp)) {
                Card(
                    modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(20.dp)),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.18f))
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(18.dp),
                        verticalArrangement = Arrangement.SpaceBetween,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (duration > 0L) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(formatDuration(position), style = TextStyle(color = Color(0x88FFFFFF), fontSize = 12.sp))
                                Spacer(modifier = Modifier.width(10.dp))
                                Slider(
                                    value = (position.toFloat() / duration.toFloat()).coerceIn(0f, 1f),
                                    onValueChange = { frac -> viewModel.seekTo((frac * duration).toLong()) },
                                    modifier = Modifier.weight(1f),
                                    colors = SliderDefaults.colors(thumbColor = MaterialTheme.colorScheme.primary)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(formatDuration(duration), style = TextStyle(color = Color(0x88FFFFFF), fontSize = 12.sp))
                            }
                        }

                        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            Box(contentAlignment = Alignment.Center) {
                                Box(modifier = Modifier.size(260.dp).clip(CircleShape).background(Color(0xFF2A2B2D)))
                                Box(modifier = Modifier.size(200.dp).clip(CircleShape).background(Color(0xFF1E1F20)))
                                Box(modifier = Modifier.size(120.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary), contentAlignment = Alignment.Center) {
                                    Button(onClick = { viewModel.togglePlayPause() }, shape = CircleShape, modifier = Modifier.size(96.dp)) { Text(if (ui.playerState.isPlaying) "‖" else "▶") }
                                }
                                Box(modifier = Modifier.size(64.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary).align(Alignment.CenterStart)) {
                                    IconButton(onClick = { viewModel.previous() }, modifier = Modifier.align(Alignment.Center)) { Text("⏮") }
                                }
                                Box(modifier = Modifier.size(64.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary).align(Alignment.CenterEnd)) {
                                    IconButton(onClick = { viewModel.next() }, modifier = Modifier.align(Alignment.Center)) { Text("⏭") }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
