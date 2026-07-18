package com.example.vaultbeat.ui.nowplaying.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import androidx.compose.ui.res.stringResource
import com.example.vaultbeat.R
import com.example.vaultbeat.core.model.Song
import com.example.vaultbeat.core.utils.formatDuration

data class AlbumKey(val album: String, val artist: String)

@Composable
fun AlbumsSection(
    songs: List<Song>,
    selectedAlbum: AlbumKey?,
    onAlbumSelected: (AlbumKey) -> Unit,
    onBack: () -> Unit,
    onSongSelected: (Int) -> Unit,
    onPlayAlbum: (List<Song>, Int) -> Unit,
    onLongClick: (Song) -> Unit,
    textColor: Color,
    secondaryTextColor: Color
) {
    val albumGroups = remember(songs) {
        songs.groupBy { AlbumKey(it.album, it.artist) }
            .toSortedMap(compareBy({ it.album }, { it.artist }))
    }
    val indexById = remember(songs) { songs.mapIndexed { index, song -> song.id to index }.toMap() }

    if (selectedAlbum == null) {
        LazyColumn {
            items(albumGroups.entries.toList()) { (albumKey, albumSongs) ->
                val firstCover = albumSongs.firstOrNull()?.albumArtUri
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                        .clickable { onAlbumSelected(albumKey) },
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                ) {
                    Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        if (firstCover != null) {
                            AsyncImage(
                                model = firstCover,
                                contentDescription = "Portada de álbum",
                                modifier = Modifier.size(64.dp).clip(RoundedCornerShape(16.dp)),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.30f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(stringResource(R.string.menu_albums), style = MaterialTheme.typography.bodySmall, color = secondaryTextColor)
                            }
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(albumKey.album, style = MaterialTheme.typography.titleMedium, color = textColor)
                            Text(albumKey.artist, style = MaterialTheme.typography.bodySmall, color = secondaryTextColor)
                        }
                    }
                }
            }
        }
    } else {
        val albumSongs = albumGroups[selectedAlbum].orEmpty()
        val totalDuration = albumSongs.sumOf { it.durationMs }
        val firstCover = albumSongs.firstOrNull()?.albumArtUri
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    stringResource(R.string.section_back_albums),
                    modifier = Modifier.clickable { onBack() },
                    style = MaterialTheme.typography.bodyMedium.copy(color = textColor)
                )
                Spacer(modifier = Modifier.width(14.dp))
                Text(selectedAlbum.album, style = MaterialTheme.typography.titleMedium, color = textColor)
            }
            if (firstCover != null) {
                AsyncImage(
                    model = firstCover,
                    contentDescription = "Portada de álbum",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                        .clip(RoundedCornerShape(20.dp)),
                    contentScale = ContentScale.Crop
                )
            }
            Spacer(modifier = Modifier.height(14.dp))
            Text(selectedAlbum.artist, style = MaterialTheme.typography.bodyMedium, color = secondaryTextColor)
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                stringResource(R.string.album_info, albumSongs.size, formatDuration(totalDuration)),
                style = MaterialTheme.typography.bodySmall,
                color = secondaryTextColor
            )
            Spacer(modifier = Modifier.height(12.dp))
            Button(onClick = { onPlayAlbum(albumSongs, 0) }) {
                Text(stringResource(R.string.play_album))
            }
            Spacer(modifier = Modifier.height(12.dp))
            LazyColumn {
                items(albumSongs) { song ->
                    val songIndex = indexById[song.id] ?: 0
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .combinedClickable(
                                onClick = { onSongSelected(songIndex) },
                                onLongClick = { onLongClick(song) }
                            )
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(song.title, style = MaterialTheme.typography.bodyMedium, color = textColor)
                            Text(song.artist, style = MaterialTheme.typography.bodySmall, color = secondaryTextColor)
                        }
                        Text(formatDuration(song.durationMs), style = MaterialTheme.typography.bodySmall, color = secondaryTextColor)
                    }
                }
            }
        }
    }
}
