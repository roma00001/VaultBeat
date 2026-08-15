package com.vaultbeat.ui.nowplaying.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
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
import com.vaultbeat.R
import com.vaultbeat.core.model.Song
import com.vaultbeat.core.utils.formatDuration

@Composable
fun ArtistsSection(
    songs: List<Song>,
    selectedArtist: String?,
    onArtistSelected: (String) -> Unit,
    onBack: () -> Unit,
    onSongSelected: (Int) -> Unit,
    onPlayAlbum: (List<Song>, Int) -> Unit,
    onLongClick: (Song) -> Unit,
    textColor: Color,
    secondaryTextColor: Color
) {
    val artistGroups = remember(songs) {
        songs.groupBy { extractPrimaryArtist(it.artist) }.toSortedMap(String.CASE_INSENSITIVE_ORDER)
    }

    if (selectedArtist == null) {
        LazyColumn {
            items(artistGroups.entries.toList()) { (artist, artistSongs) ->
                val albumCount = artistSongs.map { it.album }.distinct().size
                val songCount = artistSongs.size
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                        .clickable { onArtistSelected(artist) },
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(artist, style = MaterialTheme.typography.titleMedium, color = textColor)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            stringResource(R.string.artist_info, albumCount, songCount),
                            style = MaterialTheme.typography.bodySmall,
                            color = secondaryTextColor
                        )
                    }
                }
            }
        }
    } else {
        val artistSongs = artistGroups[selectedArtist].orEmpty()
        val artistCover = artistSongs.firstNotNullOfOrNull { it.albumArtUri }
        val totalSongs = artistSongs.size

        LazyColumn {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        stringResource(R.string.section_back),
                        modifier = Modifier.clickable { onBack() },
                        style = MaterialTheme.typography.bodyMedium.copy(color = textColor)
                    )
                }
            }
            item {
                if (artistCover != null) {
                    AsyncImage(
                        model = artistCover,
                        contentDescription = "Foto de artista",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp)
                            .clip(RoundedCornerShape(20.dp)),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Imagen de artista", style = MaterialTheme.typography.bodyMedium, color = secondaryTextColor)
                    }
                }
            }
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Text(selectedArtist, style = MaterialTheme.typography.titleLarge, color = textColor)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    stringResource(R.string.artist_songs_count, totalSongs),
                    style = MaterialTheme.typography.bodySmall,
                    color = secondaryTextColor
                )
                Spacer(modifier = Modifier.height(14.dp))
                Button(onClick = { onPlayAlbum(artistSongs, 0) }) {
                    Text(stringResource(R.string.play_artist))
                }
            }
            itemsIndexed(artistSongs) { index, song ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .combinedClickable(
                            onClick = { onPlayAlbum(artistSongs, index) },
                            onLongClick = { onLongClick(song) }
                        )
                        .padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(song.title, style = MaterialTheme.typography.bodyMedium, color = textColor)
                    Spacer(modifier = Modifier.weight(1f))
                    Text(formatDuration(song.durationMs), style = MaterialTheme.typography.bodySmall, color = secondaryTextColor)
                }
            }
        }
    }
}

fun extractPrimaryArtist(artist: String): String {
    val delimiterRegex = Regex("\\s*(?:&|feat\\.|ft\\.|featuring|,|\\band\\b|\\sx\\s|/)\\s*", RegexOption.IGNORE_CASE)
    return artist.split(delimiterRegex).firstOrNull()?.trim().takeIf { it?.isNotEmpty() == true } ?: artist.trim()
}

