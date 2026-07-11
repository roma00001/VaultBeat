package com.example.vaultbeat.ui.nowplaying

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Checkbox
import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.blur
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import coil3.Image
import coil3.compose.AsyncImage
import coil3.imageLoader
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.allowHardware
import coil3.toBitmap
import com.example.vaultbeat.core.model.Song

@Composable
fun MenuItem(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    textColor: Color,
    secondaryTextColor: Color,
    selectedTextColor: Color = textColor
) {
    Box(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Text(
            text,
            style = TextStyle(
                fontSize = 12.sp,
                color = if (selected) selectedTextColor else secondaryTextColor,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
            ),
            modifier = Modifier
                .clickable { onClick() }
                .padding(6.dp)
        )
    }
}

@Composable
private fun PlayPauseIcon(isPlaying: Boolean, color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        if (isPlaying) {
            val barWidth = size.width * 0.16f
            val barHeight = size.height * 0.55f
            val top = (size.height - barHeight) / 2f
            val left1 = size.width * 0.25f - barWidth / 2f
            val left2 = size.width * 0.70f - barWidth / 2f
            drawRoundRect(
                color = color,
                topLeft = Offset(left1, top),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
            )
            drawRoundRect(
                color = color,
                topLeft = Offset(left2, top),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
            )
        } else {
            val path = Path().apply {
                moveTo(size.width * 0.34f, size.height * 0.18f)
                lineTo(size.width * 0.34f, size.height * 0.82f)
                lineTo(size.width * 0.78f, size.height * 0.5f)
                close()
            }
            drawPath(path = path, color = color, style = Fill)
        }
    }
}

fun formatDuration(ms: Long): String {
    val seconds = ms / 1000
    return "%d:%02d".format(seconds / 60, seconds % 60)
}

private data class ContrastTextColors(val primary: Color, val secondary: Color, val tertiary: Color)

@Composable
private fun rememberContrastTextColors(albumArtUri: android.net.Uri?): ContrastTextColors {
    val context = LocalContext.current
    val defaultColors = ContrastTextColors(
        primary = Color.White,
        secondary = Color(0xFFDDDDDD),
        tertiary = Color(0xFFBFBFBF)
    )
    val textColors = remember(albumArtUri) { androidx.compose.runtime.mutableStateOf(defaultColors) }

    LaunchedEffect(albumArtUri) {
        if (albumArtUri == null) {
            textColors.value = defaultColors
            return@LaunchedEffect
        }

        val request = ImageRequest.Builder(context)
            .data(albumArtUri)
            .allowHardware(false)
            .build()

        val result = context.imageLoader.execute(request)
        val bitmap = when (result) {
            is SuccessResult -> result.image.toBitmap()
            else -> null
        }

        val averageColor = bitmap?.let { bitmapToAverageColor(it) }
        if (averageColor != null) {
            val isBackgroundLight = averageColor.luminance() > 0.5f
            textColors.value = if (isBackgroundLight) {
                ContrastTextColors(
                    primary = Color.Black,
                    secondary = Color.Black.copy(alpha = 0.88f),
                    tertiary = Color.Black.copy(alpha = 0.72f)
                )
            } else {
                ContrastTextColors(
                    primary = Color.White,
                    secondary = Color.White.copy(alpha = 0.88f),
                    tertiary = Color.White.copy(alpha = 0.72f)
                )
            }
        } else {
            textColors.value = defaultColors
        }
    }

    return textColors.value
}

private fun bitmapToAverageColor(bitmap: Bitmap): Color {
    val preview = Bitmap.createScaledBitmap(bitmap, 1, 1, true)
    val pixel = preview.getPixel(0, 0)
    return Color(pixel)
}

private data class AlbumKey(val album: String, val artist: String)

private fun extractPrimaryArtist(artist: String): String {
    val delimiterRegex = Regex("\\s*(?:&|feat\\.|ft\\.|featuring|,|\\band\\b|\\sx\\s|/)\\s*", RegexOption.IGNORE_CASE)
    return artist.split(delimiterRegex).firstOrNull()?.trim().takeIf { it?.isNotEmpty() == true } ?: artist.trim()
}

@Composable
private fun ArtistsSection(
    songs: List<Song>,
    selectedArtist: String?,
    onArtistSelected: (String) -> Unit,
    onBack: () -> Unit,
    onSongSelected: (Int) -> Unit,
    onPlayAlbum: (List<Song>, Int) -> Unit,
    textColor: Color,
    secondaryTextColor: Color
) {
    val artistGroups = remember(songs) {
        songs.groupBy { extractPrimaryArtist(it.artist) }.toSortedMap(String.CASE_INSENSITIVE_ORDER)
    }
    val indexById = remember(songs) { songs.mapIndexed { index, song -> song.id to index }.toMap() }

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
                            "$albumCount álbumes · $songCount canciones",
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
                        "← Atrás",
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
                    "$totalSongs canciones",
                    style = MaterialTheme.typography.bodySmall,
                    color = secondaryTextColor
                )
                Spacer(modifier = Modifier.height(14.dp))
                Button(onClick = { onPlayAlbum(artistSongs, 0) }) {
                    Text("Reproducir artista")
                }
            }
            items(artistSongs) { song ->
                val songIndex = indexById[song.id] ?: 0
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSongSelected(songIndex) }
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

@Composable
private fun AlbumsSection(
    songs: List<Song>,
    selectedAlbum: AlbumKey?,
    onAlbumSelected: (AlbumKey) -> Unit,
    onBack: () -> Unit,
    onSongSelected: (Int) -> Unit,
    onPlayAlbum: (List<Song>, Int) -> Unit,
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
                                Text("Álbum", style = MaterialTheme.typography.bodySmall, color = secondaryTextColor)
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
                    "← Álbumes",
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
                "${albumSongs.size} canciones · ${formatDuration(totalDuration)}",
                style = MaterialTheme.typography.bodySmall,
                color = secondaryTextColor
            )
            Spacer(modifier = Modifier.height(12.dp))
            Button(onClick = { onPlayAlbum(albumSongs, 0) }) {
                Text("Reproducir álbum")
            }
            Spacer(modifier = Modifier.height(12.dp))
            LazyColumn {
                items(albumSongs) { song ->
                    val songIndex = indexById[song.id] ?: 0
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSongSelected(songIndex) }
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

@Composable
fun NowPlayingScreen(
    viewModel: NowPlayingViewModel,
    songs: List<Song>,
    onSongSelected: (Int) -> Unit,
    onPlayAlbum: (List<Song>, Int) -> Unit,
    onRefresh: () -> Unit,
    onClose: () -> Unit = {}
) {
    val state = viewModel.state.collectAsState()
    val ui = state.value
    BackHandler { onClose() }
 
    val position = ui.playerState.positionMs.coerceAtLeast(0L)
    val duration = ui.playerState.durationMs.coerceAtLeast(0L)
    val contrastTextColors = rememberContrastTextColors(ui.song?.albumArtUri)
    val pagePrimaryColor = contrastTextColors.primary
    val pageSecondaryColor = contrastTextColors.secondary
    val pageTertiaryColor = contrastTextColors.tertiary

    // menu state
    val menu = remember { androidx.compose.runtime.mutableStateOf("NOW_PLAYING") }
    val musicPlaylistLabel = remember { androidx.compose.runtime.mutableStateOf(false) }
    val musicPlaylistMode = remember { androidx.compose.runtime.mutableStateOf(false) }
    val selectedIndex = remember { androidx.compose.runtime.mutableStateOf<Int?>(null) }
    val selectedArtist = remember { androidx.compose.runtime.mutableStateOf<String?>(null) }
    val selectedAlbum = remember { androidx.compose.runtime.mutableStateOf<AlbumKey?>(null) }
    val playlistName = rememberSaveable { mutableStateOf("") }
    val selectedPlaylistId = rememberSaveable { mutableStateOf<Long?>(null) }
    val showCreatePlaylistDialog = remember { mutableStateOf(false) }
    val showAddSongsDialog = remember { mutableStateOf(false) }
    val selectedSongIdsToAdd = remember { mutableStateOf(setOf<Long>()) }
    val songSearchQuery = rememberSaveable { mutableStateOf("") }
    val showSongSearchField = rememberSaveable { mutableStateOf(false) }
    val expandedPlaylistMenuId = remember { mutableStateOf<Long?>(null) }
    val playlistRenameId = rememberSaveable { mutableStateOf<Long?>(null) }
    val renamePlaylistName = rememberSaveable { mutableStateOf("") }
    val playlists by viewModel.playlists.collectAsState()
    val selectedPlaylistSongIds by if (selectedPlaylistId.value != null) {
        viewModel.observePlaylistSongIds(selectedPlaylistId.value!!).collectAsState(emptyList())
    } else {
        remember { mutableStateOf(emptyList<Long>()) }
    }
    val songsById = remember(songs) { songs.associateBy { it.id } }
    val configuration = LocalConfiguration.current
    val playlistMenuTopOffset = (configuration.screenHeightDp * 0.20f).dp
  
    Box(modifier = Modifier.fillMaxSize()) {
        // Background
        ui.song?.albumArtUri?.let { uri ->
            AsyncImage(
                model = uri,
                contentDescription = null,
                modifier = Modifier.fillMaxSize().blur(28.dp),
                contentScale = ContentScale.Crop
            )

                    // Dynamic overlay: use a subtle vertical gradient blended with theme colors
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.30f),
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                                    )
                                )
                            )
                    )
                } ?: run {
                    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceVariant))
                }

        Column(modifier = Modifier.fillMaxSize()) {
            Row(modifier = Modifier.weight(3f).fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                // Integrated vertical menu (compact, translucent, font-weight to indicate selection)
                val configuration = LocalConfiguration.current
                val leftMenuTopPadding = (configuration.screenHeightDp * 0.20f).dp

                Box(modifier = Modifier.weight(2.2f).fillMaxHeight().widthIn(min = 160.dp).padding(end = 8.dp)) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(12.dp)
                                        .padding(top = leftMenuTopPadding),
                                    verticalArrangement = Arrangement.SpaceBetween
                                ) {
                        Column {
                            Text("VAULTBEAT", style = MaterialTheme.typography.labelMedium, color = contrastTextColors.primary)
                            Spacer(modifier = Modifier.height(24.dp))
                            val items = listOf("MUSIC", "PODCASTS", "ARTISTS", "ALBUMS", "NOW_PLAYING")
                            items.forEach { item ->
                                val selectedItem = menu.value == item
                                val displayText = if (item == "MUSIC" && musicPlaylistLabel.value) "PLAYLISTS" else item.replace("_", " ")
                                MenuItem(
                                    text = displayText,
                                    selected = selectedItem,
                                    onClick = {
                                        when (item) {
                                            "MUSIC" -> {
                                                if (menu.value != "MUSIC") {
                                                    menu.value = "MUSIC"
                                                    musicPlaylistLabel.value = true
                                                    musicPlaylistMode.value = false
                                                    selectedPlaylistId.value = null
                                                } else {
                                                    if (musicPlaylistLabel.value) {
                                                        musicPlaylistMode.value = true
                                                        musicPlaylistLabel.value = false
                                                    } else {
                                                        musicPlaylistMode.value = false
                                                        musicPlaylistLabel.value = true
                                                    }
                                                    selectedPlaylistId.value = null
                                                }
                                            }
                                            else -> {
                                                menu.value = item
                                                musicPlaylistLabel.value = false
                                                musicPlaylistMode.value = false
                                                selectedPlaylistId.value = null
                                            }
                                        }
                                        if (item != "ARTISTS") selectedArtist.value = null
                                        if (item != "ALBUMS") selectedAlbum.value = null
                                    },
                                    textColor = contrastTextColors.primary,
                                    secondaryTextColor = contrastTextColors.secondary,
                                    selectedTextColor = contrastTextColors.primary
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Main content area
                Card(
                    modifier = Modifier.weight(4f).fillMaxSize(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Top
                    ) {
                        val podcastSongs = songs.filter { it.durationMs > 10 * 60 * 1000 }
                        val songIndexById = songs.mapIndexed { index, song -> song.id to index }.toMap()

                        when (menu.value) {
                            "MUSIC" -> {
                                if (musicPlaylistMode.value) {
                                    Column(modifier = Modifier.fillMaxSize().padding(top = playlistMenuTopOffset - 6.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                "Playlists",
                                                style = MaterialTheme.typography.titleLarge,
                                                color = pagePrimaryColor
                                            )
                                            Button(
                                                onClick = { showCreatePlaylistDialog.value = true },
                                                modifier = Modifier.height(36.dp),
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                                                    contentColor = MaterialTheme.colorScheme.primary
                                                ),
                                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                            ) {
                                                Text("Crear", style = MaterialTheme.typography.labelMedium)
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(20.dp))

                                        if (selectedPlaylistId.value == null) {
                                            if (playlists.isEmpty()) {
                                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                                                    Text(
                                                        "No hay playlists aún",
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        color = pageSecondaryColor,
                                                        textAlign = TextAlign.Center
                                                    )
                                                }
                                            } else {
                                                LazyColumn(
                                                    verticalArrangement = Arrangement.spacedBy(12.dp),
                                                    contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 12.dp)
                                                ) {
                                                    items(playlists) { playlist ->
                                                        val playlistSongIds by viewModel.observePlaylistSongIds(playlist.id).collectAsState(emptyList())
                                                        Card(
                                                            modifier = Modifier.fillMaxWidth(),
                                                            shape = RoundedCornerShape(20.dp),
                                                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.24f))
                                                        ) {
                                                            Row(
                                                                modifier = Modifier
                                                                    .fillMaxWidth()
                                                                    .padding(16.dp),
                                                                verticalAlignment = Alignment.CenterVertically,
                                                                horizontalArrangement = Arrangement.SpaceBetween
                                                            ) {
                                                                Column(
                                                                    modifier = Modifier
                                                                        .weight(1f)
                                                                        .clickable { selectedPlaylistId.value = playlist.id }
                                                                ) {
                                                                    Text(playlist.name, style = MaterialTheme.typography.titleMedium, color = pagePrimaryColor)
                                                                    Spacer(modifier = Modifier.height(4.dp))
                                                                    Text(
                                                                        "${playlistSongIds.size} canciones",
                                                                        style = MaterialTheme.typography.bodySmall,
                                                                        color = pageSecondaryColor
                                                                    )
                                                                }
                                                                Box {
                                                                    Text(
                                                                        "⋮",
                                                                        modifier = Modifier
                                                                            .clickable {
                                                                                expandedPlaylistMenuId.value = if (expandedPlaylistMenuId.value == playlist.id) null else playlist.id
                                                                            }
                                                                            .padding(8.dp),
                                                                        style = MaterialTheme.typography.titleLarge.copy(color = pagePrimaryColor)
                                                                    )
                                                                    DropdownMenu(
                                                                        expanded = expandedPlaylistMenuId.value == playlist.id,
                                                                        onDismissRequest = { expandedPlaylistMenuId.value = null }
                                                                    ) {
                                                                        DropdownMenuItem(
                                                                            text = { Text("Reproducir") },
                                                                            onClick = {
                                                                                expandedPlaylistMenuId.value = null
                                                                                val songsToPlay = playlistSongIds.mapNotNull { songsById[it] }
                                                                                if (songsToPlay.isNotEmpty()) onPlayAlbum(songsToPlay, 0)
                                                                            }
                                                                        )
                                                                        DropdownMenuItem(
                                                                            text = { Text("Reproducir aleatoriamente") },
                                                                            onClick = {
                                                                                expandedPlaylistMenuId.value = null
                                                                                val songsToPlay = playlistSongIds.mapNotNull { songsById[it] }.shuffled()
                                                                                if (songsToPlay.isNotEmpty()) onPlayAlbum(songsToPlay, 0)
                                                                            }
                                                                        )
                                                                        DropdownMenuItem(
                                                                            text = { Text("Añadir canciones") },
                                                                            onClick = {
                                                                                expandedPlaylistMenuId.value = null
                                                                                selectedPlaylistId.value = playlist.id
                                                                                selectedSongIdsToAdd.value = emptySet()
                                                                                songSearchQuery.value = ""
                                                                                showSongSearchField.value = false
                                                                                showAddSongsDialog.value = true
                                                                            }
                                                                        )
                                                                        DropdownMenuItem(
                                                                            text = { Text("Renombrar playlist") },
                                                                            onClick = {
                                                                                expandedPlaylistMenuId.value = null
                                                                                playlistRenameId.value = playlist.id
                                                                                renamePlaylistName.value = playlist.name
                                                                            }
                                                                        )
                                                                        DropdownMenuItem(
                                                                            text = { Text("Borrar") },
                                                                            onClick = {
                                                                                expandedPlaylistMenuId.value = null
                                                                                viewModel.deletePlaylist(playlist.id)
                                                                            }
                                                                        )
                                                                    }
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        } else {
                                            val playlist = playlists.firstOrNull { it.id == selectedPlaylistId.value }
                                            val playlistSongIds = selectedPlaylistSongIds
                                            val playlistSongs = playlistSongIds.mapNotNull { songsById[it] }
                                            val coverUri = playlistSongs.firstOrNull()?.albumArtUri

                                            if (playlist == null) {
                                                selectedPlaylistId.value = null
                                            } else {
                                                LazyColumn(
                                                    modifier = Modifier.fillMaxSize(),
                                                    verticalArrangement = Arrangement.spacedBy(12.dp),
                                                    contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 12.dp)
                                                ) {
                                                    item {
                                                        Row(
                                                            modifier = Modifier.fillMaxWidth(),
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            Text(
                                                                "← Playlists",
                                                                modifier = Modifier.clickable { selectedPlaylistId.value = null },
                                                                style = MaterialTheme.typography.bodyMedium.copy(color = pagePrimaryColor)
                                                            )
                                                        }
                                                        Spacer(modifier = Modifier.height(16.dp))

                                                        Box(
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .height(220.dp)
                                                                .clip(RoundedCornerShape(20.dp))
                                                                .background(if (coverUri == null) Color.Black else Color.Transparent),
                                                            contentAlignment = Alignment.Center
                                                        ) {
                                                            coverUri?.let {
                                                                AsyncImage(
                                                                    model = it,
                                                                    contentDescription = "Portada de playlist",
                                                                    modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(20.dp)),
                                                                    contentScale = ContentScale.Crop
                                                                )
                                                            }
                                                        }

                                                        Spacer(modifier = Modifier.height(18.dp))
                                                        Text(playlist.name, style = MaterialTheme.typography.titleLarge, color = pagePrimaryColor)
                                                        Spacer(modifier = Modifier.height(6.dp))
                                                        Text(
                                                            "${playlistSongIds.size} canciones",
                                                            style = MaterialTheme.typography.bodyMedium,
                                                            color = pageSecondaryColor
                                                        )
                                                        Spacer(modifier = Modifier.height(18.dp))
                                                        Button(
                                                            onClick = { if (playlistSongs.isNotEmpty()) onPlayAlbum(playlistSongs, 0) },
                                                            enabled = playlistSongs.isNotEmpty(),
                                                            modifier = Modifier.fillMaxWidth().height(44.dp),
                                                            colors = ButtonDefaults.buttonColors(
                                                                containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                                                                contentColor = MaterialTheme.colorScheme.primary
                                                            )
                                                        ) {
                                                            Text("Reproducir")
                                                        }
                                                        Spacer(modifier = Modifier.height(12.dp))
                                                        Button(
                                                            onClick = {
                                                                selectedSongIdsToAdd.value = emptySet()
                                                                songSearchQuery.value = ""
                                                                showSongSearchField.value = false
                                                                showAddSongsDialog.value = true
                                                            },
                                                            modifier = Modifier.fillMaxWidth().height(44.dp),
                                                            colors = ButtonDefaults.buttonColors(
                                                                containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                                                                contentColor = MaterialTheme.colorScheme.primary
                                                            )
                                                        ) {
                                                            Text("Añadir")
                                                        }
                                                        Spacer(modifier = Modifier.height(20.dp))
                                                        Text("Canciones", style = MaterialTheme.typography.titleMedium, color = pagePrimaryColor)
                                                        Spacer(modifier = Modifier.height(10.dp))
                                                    }

                                                    if (playlistSongs.isEmpty()) {
                                                        item {
                                                            Card(
                                                                modifier = Modifier.fillMaxWidth(),
                                                                shape = RoundedCornerShape(18.dp),
                                                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.18f))
                                                            ) {
                                                                Text(
                                                                    "No hay canciones en esta playlist.",
                                                                    modifier = Modifier.padding(16.dp),
                                                                    style = MaterialTheme.typography.bodyMedium,
                                                                    color = pageSecondaryColor
                                                                )
                                                            }
                                                        }
                                                    } else {
                                                        items(playlistSongs) { song ->
                                                            Row(
                                                                modifier = Modifier
                                                                    .fillMaxWidth()
                                                                    .combinedClickable(
                                                                        onClick = { onSongSelected(songs.indexOf(song)) },
                                                                        onLongClick = {
                                                                            viewModel.removeSongFromPlaylist(playlist.id, song.id)
                                                                        }
                                                                    )
                                                                    .padding(vertical = 12.dp),
                                                                verticalAlignment = Alignment.CenterVertically
                                                            ) {
                                                                Column(modifier = Modifier.weight(1f)) {
                                                                    Text(song.title, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodyMedium, color = pagePrimaryColor)
                                                                    Text(song.artist, style = MaterialTheme.typography.bodySmall, color = pageSecondaryColor)
                                                                }
                                                                Text(formatDuration(song.durationMs), style = MaterialTheme.typography.bodySmall, color = pageSecondaryColor)
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }

                                        if (showCreatePlaylistDialog.value) {
                                            AlertDialog(
                                                onDismissRequest = { showCreatePlaylistDialog.value = false },
                                                confirmButton = {
                                                    TextButton(
                                                        onClick = {
                                                            val name = playlistName.value.trim()
                                                            if (name.isNotEmpty()) {
                                                                viewModel.createPlaylist(name) { playlistId ->
                                                                    selectedPlaylistId.value = playlistId
                                                                }
                                                                playlistName.value = ""
                                                                showCreatePlaylistDialog.value = false
                                                            }
                                                        }
                                                    ) {
                                                        Text("Crear")
                                                    }
                                                },
                                                dismissButton = {
                                                    TextButton(onClick = { showCreatePlaylistDialog.value = false }) {
                                                        Text("Cancelar")
                                                    }
                                                },
                                                title = { Text("Nueva playlist") },
                                                text = {
                                                    OutlinedTextField(
                                                        value = playlistName.value,
                                                        onValueChange = { playlistName.value = it },
                                                        label = { Text("Nombre de playlist") },
                                                        modifier = Modifier.fillMaxWidth()
                                                    )
                                                }
                                            )
                                        }

                                        if (playlistRenameId.value != null) {
                                            AlertDialog(
                                                onDismissRequest = { playlistRenameId.value = null },
                                                confirmButton = {
                                                    TextButton(
                                                        onClick = {
                                                            val newName = renamePlaylistName.value.trim()
                                                            if (newName.isNotEmpty()) {
                                                                viewModel.renamePlaylist(playlistRenameId.value!!, newName)
                                                                playlistRenameId.value = null
                                                            }
                                                        }
                                                    ) {
                                                        Text("Renombrar")
                                                    }
                                                },
                                                dismissButton = {
                                                    TextButton(onClick = { playlistRenameId.value = null }) {
                                                        Text("Cancelar")
                                                    }
                                                },
                                                title = { Text("Renombrar playlist") },
                                                text = {
                                                    OutlinedTextField(
                                                        value = renamePlaylistName.value,
                                                        onValueChange = { renamePlaylistName.value = it },
                                                        label = { Text("Nuevo nombre") },
                                                        modifier = Modifier.fillMaxWidth()
                                                    )
                                                }
                                            )
                                        }

                                        if (showAddSongsDialog.value && selectedPlaylistId.value != null) {
                                            val availableSongs = songs.filter { it.id !in selectedPlaylistSongIds }
                                            val displayedSongs = if (songSearchQuery.value.isBlank()) {
                                                availableSongs
                                            } else {
                                                availableSongs.filter { song ->
                                                    song.title.contains(songSearchQuery.value, ignoreCase = true) ||
                                                        song.artist.contains(songSearchQuery.value, ignoreCase = true)
                                                }
                                            }
                                            AlertDialog(
                                                onDismissRequest = { showAddSongsDialog.value = false },
                                                confirmButton = {
                                                    TextButton(
                                                        onClick = {
                                                            selectedSongIdsToAdd.value.forEach { songId ->
                                                                viewModel.addSongToPlaylist(selectedPlaylistId.value!!, songId)
                                                            }
                                                            showAddSongsDialog.value = false
                                                        }
                                                    ) {
                                                        Text("Hecho")
                                                    }
                                                },
                                                dismissButton = {
                                                    TextButton(onClick = { showAddSongsDialog.value = false }) {
                                                        Text("Cancelar")
                                                    }
                                                },
                                                title = {
                                                    Column(modifier = Modifier.fillMaxWidth()) {
                                                        Row(
                                                            modifier = Modifier.fillMaxWidth(),
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            Text(
                                                                "Añadir canciones",
                                                                modifier = Modifier.weight(1f),
                                                                style = MaterialTheme.typography.titleMedium,
                                                                color = pagePrimaryColor
                                                            )
                                                            IconButton(
                                                                onClick = {
                                                                    showSongSearchField.value = !showSongSearchField.value
                                                                    if (!showSongSearchField.value) {
                                                                        songSearchQuery.value = ""
                                                                    }
                                                                }
                                                            ) {
                                                                Icon(
                                                                    imageVector = Icons.Default.Search,
                                                                    contentDescription = "Buscar canción",
                                                                    tint = pagePrimaryColor
                                                                )
                                                            }
                                                        }
                                                        if (showSongSearchField.value) {
                                                            Spacer(modifier = Modifier.height(8.dp))
                                                            OutlinedTextField(
                                                                value = songSearchQuery.value,
                                                                onValueChange = { songSearchQuery.value = it },
                                                                placeholder = { Text("Buscar canción") },
                                                                modifier = Modifier.fillMaxWidth(),
                                                                singleLine = true
                                                            )
                                                        }
                                                    }
                                                },
                                                text = {
                                                    if (availableSongs.isEmpty()) {
                                                        Text("No hay canciones disponibles para añadir.")
                                                    } else if (displayedSongs.isEmpty()) {
                                                        Text("No se encontraron canciones que coincidan con la búsqueda.")
                                                    } else {
                                                        Column(modifier = Modifier.fillMaxWidth().heightIn(max = 300.dp)) {
                                                            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                                                items(displayedSongs) { song ->
                                                                    Row(
                                                                        modifier = Modifier
                                                                            .fillMaxWidth()
                                                                            .clickable {
                                                                                val current = selectedSongIdsToAdd.value.toMutableSet()
                                                                                if (current.contains(song.id)) {
                                                                                    current.remove(song.id)
                                                                                } else {
                                                                                    current.add(song.id)
                                                                                }
                                                                                selectedSongIdsToAdd.value = current
                                                                            }
                                                                            .padding(8.dp),
                                                                        verticalAlignment = Alignment.CenterVertically
                                                                    ) {
                                                                        Checkbox(
                                                                            checked = selectedSongIdsToAdd.value.contains(song.id),
                                                                            onCheckedChange = {
                                                                                val current = selectedSongIdsToAdd.value.toMutableSet()
                                                                                if (it) current.add(song.id) else current.remove(song.id)
                                                                                selectedSongIdsToAdd.value = current
                                                                            }
                                                                        )
                                                                        Spacer(modifier = Modifier.width(8.dp))
                                                                        Column {
                                                                            Text(song.title, style = MaterialTheme.typography.bodyMedium, color = pagePrimaryColor)
                                                                            Text(song.artist, style = MaterialTheme.typography.bodySmall, color = pageSecondaryColor)
                                                                        }
                                                                    }
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            )
                                        }
                                    }
                                } else {
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
                                                    Text(song.title, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.titleMedium, color = pagePrimaryColor)
                                                    Text(song.artist, style = MaterialTheme.typography.bodySmall, color = pageSecondaryColor)
                                                }
                                                Text(formatDuration(song.durationMs), style = MaterialTheme.typography.labelSmall, color = pageSecondaryColor)
                                            }
                                        }
                                    }
                                }
                            }
                            "ARTISTS" -> {
                                ArtistsSection(
                                    songs = songs,
                                    selectedArtist = selectedArtist.value,
                                    onArtistSelected = { selectedArtist.value = it },
                                    onBack = { selectedArtist.value = null },
                                    onSongSelected = onSongSelected,
                                    onPlayAlbum = onPlayAlbum,
                                    textColor = pagePrimaryColor,
                                    secondaryTextColor = pageSecondaryColor
                                )
                            }
                            "ALBUMS" -> {
                                AlbumsSection(
                                    songs = songs,
                                    selectedAlbum = selectedAlbum.value,
                                    onAlbumSelected = { selectedAlbum.value = it },
                                    onBack = { selectedAlbum.value = null },
                                    onSongSelected = onSongSelected,
                                    onPlayAlbum = onPlayAlbum,
                                    textColor = pagePrimaryColor,
                                    secondaryTextColor = pageSecondaryColor
                                )
                            }
                            "PODCASTS" -> {
                                if (podcastSongs.isEmpty()) {
                                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                        Text(
                                            "No hay episodios largos de más de 10 minutos",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = pagePrimaryColor,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                } else {
                                    LazyColumn(contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)) {
                                        itemsIndexed(items = podcastSongs, key = { _, s -> s.id }) { _, song ->
                                            val globalIndex = songIndexById[song.id] ?: 0
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable { selectedIndex.value = globalIndex; onSongSelected(globalIndex) }
                                                    .padding(vertical = 8.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                if (song.albumArtUri != null) {
                                                    AsyncImage(model = song.albumArtUri, contentDescription = null, modifier = Modifier.size(52.dp).clip(RoundedCornerShape(12.dp)), contentScale = ContentScale.Crop)
                                                } else {
                                                    Box(modifier = Modifier.size(52.dp).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surfaceVariant))
                                                }
                                                Spacer(modifier = Modifier.width(12.dp))
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(song.title, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.titleMedium, color = pagePrimaryColor)
                                                    Text(song.artist, style = MaterialTheme.typography.bodySmall, color = pageSecondaryColor)
                                                }
                                                Text(formatDuration(song.durationMs), style = MaterialTheme.typography.labelSmall, color = pageSecondaryColor)
                                            }
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
                                        // Asymmetric, elongated album art with glassmorphism behind it
                                        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.TopStart) {
                                            Box(modifier = Modifier
                                                .offset(x = 12.dp)
                                                .size(width = 260.dp, height = 180.dp)
                                                .clip(RoundedCornerShape(20.dp))
                                                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.16f))
                                                .blur(8.dp)
                                            )

                                            AsyncImage(
                                                model = ui.song.albumArtUri,
                                                contentDescription = "Album art",
                                                modifier = Modifier
                                                    .offset(x = 20.dp, y = 6.dp)
                                                    .size(width = 260.dp, height = 180.dp)
                                                    .clip(RoundedCornerShape(18.dp)),
                                                contentScale = ContentScale.Crop
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(24.dp))
                                        Column(modifier = Modifier.fillMaxWidth(0.95f), horizontalAlignment = Alignment.Start) {
                                            Text(
                                                ui.song.title,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis,
                                                textAlign = TextAlign.Start,
                                                style = TextStyle(fontSize = 28.sp, fontWeight = FontWeight.SemiBold, color = contrastTextColors.primary)
                                            )
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text(
                                                ui.song.artist,
                                                textAlign = TextAlign.Start,
                                                style = TextStyle(fontSize = 16.sp, color = contrastTextColors.secondary)
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                ui.song.album,
                                                textAlign = TextAlign.Start,
                                                style = TextStyle(fontSize = 12.sp, color = contrastTextColors.tertiary)
                                            )
                                        }
                                    }
                                }
                            }
                            else -> {
                                Text(
                                    "Sección: ${menu.value}",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = pagePrimaryColor
                                )
                            }
                        }
                    }
                }
            }

            // Bottom player bar (themed controls aligned with the app style)
            Box(modifier = Modifier.weight(1f).fillMaxWidth().padding(12.dp)) {
                Card(
                    modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(24.dp)),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.24f))
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(18.dp),
                        verticalArrangement = Arrangement.SpaceBetween,
                        horizontalAlignment = Alignment.Start
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text("Now playing", style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.primary))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                ui.song?.title ?: "Selecciona una canción",
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                ui.song?.artist ?: "",
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    formatDuration(position),
                                    style = TextStyle(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.9f), fontSize = 12.sp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Slider(
                                    value = if (duration > 0L) (position.toFloat() / duration.toFloat()).coerceIn(0f, 1f) else 0f,
                                    onValueChange = { frac -> if (duration > 0L) viewModel.seekTo((frac * duration).toLong()) },
                                    modifier = Modifier.weight(1f),
                                    enabled = duration > 0L,
                                    colors = androidx.compose.material3.SliderDefaults.colors(
                                        thumbColor = MaterialTheme.colorScheme.primary,
                                        activeTrackColor = MaterialTheme.colorScheme.primary,
                                        inactiveTrackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                                    )
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    formatDuration(duration),
                                    style = TextStyle(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.9f), fontSize = 12.sp)
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Card(
                                modifier = Modifier
                                    .size(54.dp)
                                    .clickable { viewModel.previous() },
                                shape = CircleShape,
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.30f)
                                ),
                                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                            ) {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Text("⏮", style = TextStyle(fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurfaceVariant))
                                }
                            }

                            Card(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clickable { viewModel.togglePlayPause() },
                                shape = CircleShape,
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.30f)
                                ),
                                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                            ) {
                                PlayPauseIcon(
                                    isPlaying = ui.playerState.isPlaying,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(14.dp)
                                )
                            }

                            Card(
                                modifier = Modifier
                                    .size(54.dp)
                                    .clickable { viewModel.next() },
                                shape = CircleShape,
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.30f)
                                ),
                                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                            ) {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Text("⏭", style = TextStyle(fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurfaceVariant))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
