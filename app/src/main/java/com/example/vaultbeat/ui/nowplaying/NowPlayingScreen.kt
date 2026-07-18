package com.example.vaultbeat.ui.nowplaying

import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuOpen
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil3.compose.AsyncImage
import coil3.imageLoader
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.allowHardware
import coil3.toBitmap
import androidx.compose.ui.res.stringResource
import com.example.vaultbeat.R
import com.example.vaultbeat.core.model.Song
import com.example.vaultbeat.core.utils.formatDuration
import com.example.vaultbeat.ui.library.LibraryViewModel
import com.example.vaultbeat.ui.nowplaying.components.*
import com.example.vaultbeat.ui.vaultbeat.VaultBeatViewModel

@Composable
private fun MenuItem(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    textColor: Color,
    secondaryTextColor: Color,
    selectedTextColor: Color = textColor
) {
    Box(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        val alpha by animateFloatAsState(if (selected) 1f else 0.6f, label = "MenuItemAlpha")
        Text(
            text,
            style = TextStyle(
                fontSize = 13.sp,
                color = (if (selected) selectedTextColor else secondaryTextColor).copy(alpha = alpha),
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                letterSpacing = 0.5.sp
            ),
            modifier = Modifier
                .clickable(
                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                    indication = null
                ) { onClick() }
                .padding(vertical = 6.dp, horizontal = 4.dp)
        )
    }
}

private data class ContrastTextColors(val primary: Color, val secondary: Color, val tertiary: Color)

@Composable
private fun rememberContrastTextColors(albumArtModel: Any?): ContrastTextColors {
    val context = LocalContext.current
    val defaultColors = ContrastTextColors(
        primary = Color.White,
        secondary = Color(0xFFDDDDDD),
        tertiary = Color(0xFFBFBFBF)
    )
    val textColors = remember(albumArtModel) { mutableStateOf(defaultColors) }

    LaunchedEffect(albumArtModel) {
        if (albumArtModel == null) {
            textColors.value = defaultColors
            return@LaunchedEffect
        }

        val request = ImageRequest.Builder(context)
            .data(albumArtModel)
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

@Composable
private fun AnimatedSection(
    targetState: String,
    content: @Composable (String) -> Unit
) {
    AnimatedContent(
        targetState = targetState,
        transitionSpec = {
            if (targetState == "NOW_PLAYING") {
                (fadeIn(animationSpec = tween(400, easing = LinearOutSlowInEasing)) + scaleIn(initialScale = 0.95f, animationSpec = tween(400, easing = LinearOutSlowInEasing)))
                    .togetherWith(fadeOut(animationSpec = tween(300, easing = FastOutLinearInEasing)) + scaleOut(targetScale = 1.02f, animationSpec = tween(300, easing = FastOutLinearInEasing)))
            } else {
                (fadeIn(animationSpec = tween(400, easing = LinearOutSlowInEasing)) + slideInHorizontally(animationSpec = tween(400, easing = LinearOutSlowInEasing), initialOffsetX = { it / 20 }))
                    .togetherWith(fadeOut(animationSpec = tween(300, easing = FastOutLinearInEasing)) + slideOutHorizontally(animationSpec = tween(300, easing = FastOutLinearInEasing), targetOffsetX = { -it / 20 }))
            }
        },
        label = "FluidSectionTransition"
    ) { state ->
        content(state)
    }
}

@Composable
fun NowPlayingScreen(
    viewModel: NowPlayingViewModel,
    songs: List<Song>,
    onSongSelected: (Int) -> Unit,
    onPlayAlbum: (List<Song>, Int) -> Unit,
    onRefresh: () -> Unit,
    onClose: () -> Unit = {},
    vaultBeatViewModel: VaultBeatViewModel = hiltViewModel(),
    libraryViewModel: LibraryViewModel = hiltViewModel()
) {
    val scope = rememberCoroutineScope()
    val state = viewModel.state.collectAsState()
    val ui = state.value
    val vbState by vaultBeatViewModel.state.collectAsState()
    BackHandler { onClose() }
 
    val position = ui.playerState.positionMs.coerceAtLeast(0L)
    val duration = ui.playerState.durationMs.coerceAtLeast(0L)
    val songsById = remember(songs) { songs.associateBy { it.id } }
    
    val currentSongIndex = remember(ui.song, songs) { 
        songs.indexOfFirst { it.id == ui.song?.id }.takeIf { it >= 0 } ?: 0 
    }
    
    val pagerState = rememberPagerState(initialPage = currentSongIndex) { songs.size }

    // Sync pager with current song from player
    LaunchedEffect(ui.song?.id) {
        val targetIndex = songs.indexOfFirst { it.id == ui.song?.id }
        if (targetIndex >= 0 && targetIndex != pagerState.currentPage) {
            pagerState.scrollToPage(targetIndex)
        }
    }

    // Handle swipe to change song
    LaunchedEffect(pagerState.currentPage) {
        val targetIndex = pagerState.currentPage
        if (ui.song != null && songs.getOrNull(targetIndex)?.id != ui.song.id) {
            onSongSelected(targetIndex)
        }
    }
    
    val currentSongInPager = if (ui.song != null) songs.getOrNull(pagerState.currentPage) else null
    val currentCover = currentSongInPager?.albumArtUri ?: R.drawable.portada
    val contrastTextColors = rememberContrastTextColors(currentCover)
    val pagePrimaryColor = contrastTextColors.primary
    val pageSecondaryColor = contrastTextColors.secondary
    val pageTertiaryColor = contrastTextColors.tertiary

    val menu = remember { mutableStateOf("NOW_PLAYING") }
    val musicPlaylistLabel = remember { mutableStateOf(false) }
    val musicPlaylistMode = remember { mutableStateOf(false) }
    val selectedIndex = remember { mutableStateOf<Int?>(null) }
    val selectedArtist = remember { mutableStateOf<String?>(null) }
    val selectedAlbum = remember { mutableStateOf<AlbumKey?>(null) }
    val playlistName = rememberSaveable { mutableStateOf("") }
    val selectedPlaylistId = rememberSaveable { mutableStateOf<Long?>(null) }
    val showCreatePlaylistDialog = remember { mutableStateOf(false) }
    val showAddSongsDialog = remember { mutableStateOf(false) }
    val selectedSongIdsToAdd = remember { mutableStateOf(setOf<Long>()) }
    val songSearchQuery = rememberSaveable { mutableStateOf("") }
    val showSongSearchField = rememberSaveable { mutableStateOf(false) }
    var selectedSongForDownload by remember { mutableStateOf<com.example.vaultbeat.core.network.model.SearchResult?>(null) }
    var showPlaylistDownloadDialog by remember { mutableStateOf(false) }
    val expandedPlaylistMenuId = remember { mutableStateOf<Long?>(null) }
    val playlistRenameId = rememberSaveable { mutableStateOf<Long?>(null) }
    val renamePlaylistName = rememberSaveable { mutableStateOf("") }
    val vaultBeatSearchQuery = rememberSaveable { mutableStateOf("") }
    val playlists by viewModel.playlists.collectAsState()
    val selectedPlaylistSongIds by if (selectedPlaylistId.value != null) {
        viewModel.observePlaylistSongIds(selectedPlaylistId.value!!).collectAsState(emptyList())
    } else {
        remember { mutableStateOf(emptyList<Long>()) }
    }
    
    val showCoverPicker = remember { mutableStateOf(false) }
    val showSortPicker = remember { mutableStateOf(false) }
    
    val configuration = LocalConfiguration.current
    
    var isMenuCollapsed by rememberSaveable { mutableStateOf(false) }
    
    var selectedSongForMenu by remember { mutableStateOf<Song?>(null) }
    var showSongMenu by remember { mutableStateOf(false) }
    var showPlaylistPickerForAdd by remember { mutableStateOf(false) }
    var showPlaylistPickerForMove by remember { mutableStateOf(false) }
    var sourcePlaylistIdForMove by remember { mutableStateOf<Long?>(null) }
    
    var isReloadingCover by remember { mutableStateOf(false) }
    var reloadArtworkUrl by remember { mutableStateOf<String?>(null) }
    var showReloadConfirmation by remember { mutableStateOf(false) }

    var musicSearchQuery by rememberSaveable { mutableStateOf("") }
    var showMusicSearchField by rememberSaveable { mutableStateOf(false) }
    var musicSortOrder by rememberSaveable { mutableStateOf("RECENT") }
    var isReloadingAllCovers by remember { mutableStateOf(false) }
    var reloadAllProgress by remember { mutableStateOf(0 to 0) }

    val filteredSongs = remember(songs, musicSearchQuery, musicSortOrder) {
        val filtered = if (musicSearchQuery.isBlank()) songs 
        else songs.filter { it.title.contains(musicSearchQuery, ignoreCase = true) || it.artist.contains(musicSearchQuery, ignoreCase = true) }
        
        when (musicSortOrder) {
            "NAME" -> filtered.sortedBy { it.title.lowercase() }
            "ARTIST" -> filtered.sortedBy { it.artist.lowercase() }
            "RECENT" -> filtered.sortedByDescending { it.id }
            else -> filtered
        }
    }
  
    Box(modifier = Modifier.fillMaxSize()) {
        AsyncImage(
            model = currentCover,
            contentDescription = null,
            modifier = Modifier.fillMaxSize().blur(28.dp),
            contentScale = ContentScale.Crop
        )

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

        Column(modifier = Modifier.fillMaxSize()) {
            Row(modifier = Modifier.weight(3f).fillMaxWidth().padding(horizontal = 18.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                val leftMenuWeight by animateFloatAsState(
                    targetValue = if (isMenuCollapsed) 0.001f else 2.2f,
                    animationSpec = tween(450, easing = FastOutSlowInEasing),
                    label = "MenuWeight"
                )

                if (leftMenuWeight > 0.1f) {
                    Box(modifier = Modifier.weight(leftMenuWeight).fillMaxHeight().widthIn(min = 160.dp).padding(end = 8.dp)) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(12.dp)
                                .padding(top = (configuration.screenHeightDp * 0.23f).dp)
                                .graphicsLayer {
                                    alpha = ((leftMenuWeight - 0.5f) / 1.7f).coerceIn(0f, 1f)
                                    translationX = (leftMenuWeight - 2.2f) * 50f
                                },
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    stringResource(R.string.menu_vaultbeat),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (menu.value == "VAULTBEAT") contrastTextColors.primary else contrastTextColors.secondary,
                                    modifier = Modifier.clickable {
                                        menu.value = "VAULTBEAT"
                                        musicPlaylistLabel.value = false
                                        musicPlaylistMode.value = false
                                        selectedPlaylistId.value = null
                                        selectedArtist.value = null
                                        selectedAlbum.value = null
                                    }
                                )
                                Spacer(modifier = Modifier.height(24.dp))
                                val items = listOf("MUSIC", "PODCASTS", "ARTISTS", "ALBUMS", "NOW_PLAYING")
                                items.forEach { item ->
                                    val selectedItem = menu.value == item
                                    val displayText = when (item) {
                                        "MUSIC" -> if (musicPlaylistLabel.value) stringResource(R.string.menu_playlists) else stringResource(R.string.menu_music)
                                        "PODCASTS" -> stringResource(R.string.menu_podcasts)
                                        "ARTISTS" -> stringResource(R.string.menu_artists)
                                        "ALBUMS" -> stringResource(R.string.menu_albums)
                                        "NOW_PLAYING" -> stringResource(R.string.menu_now_playing)
                                        else -> item
                                    }
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
                }

                if (!isMenuCollapsed) {
                    Spacer(modifier = Modifier.width(12.dp))
                }

                Card(
                    modifier = Modifier.weight(4f).fillMaxSize(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (menu.value == "NOW_PLAYING") Color.Transparent 
                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.08f)
                    ),
                    border = if (menu.value == "NOW_PLAYING") null 
                    else BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Top
                    ) {
                        val podcastSongs = songs.filter { it.durationMs > 10 * 60 * 1000 }
                        val songIndexById = songs.mapIndexed { index, song -> song.id to index }.toMap()

                        AnimatedSection(targetState = menu.value) { targetMenu ->
                            when (targetMenu) {
                                "VAULTBEAT" -> {
                                    Column(modifier = Modifier.fillMaxSize().padding(top = 0.dp)) {
                                        TextField(
                                            value = vaultBeatSearchQuery.value,
                                            onValueChange = { vaultBeatSearchQuery.value = it },
                                            placeholder = { 
                                                Text(
                                                    stringResource(R.string.search_placeholder), 
                                                    color = pageSecondaryColor.copy(alpha = 0.5f),
                                                    style = MaterialTheme.typography.bodyMedium
                                                ) 
                                            },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(54.dp),
                                            shape = RoundedCornerShape(27.dp),
                                            leadingIcon = { 
                                                Icon(
                                                    Icons.Default.Search, 
                                                    contentDescription = null, 
                                                    tint = pagePrimaryColor.copy(alpha = 0.7f),
                                                    modifier = Modifier.size(20.dp)
                                                ) 
                                            },
                                            trailingIcon = {
                                                if (vbState.isSearching) {
                                                    CircularProgressIndicator(
                                                        modifier = Modifier.size(20.dp), 
                                                        strokeWidth = 2.dp, 
                                                        color = pagePrimaryColor
                                                    )
                                                } else if (vaultBeatSearchQuery.value.isNotEmpty()) {
                                                    IconButton(
                                                        onClick = { 
                                                            vaultBeatSearchQuery.value = ""
                                                            vaultBeatViewModel.search("")
                                                        }
                                                    ) {
                                                        Icon(
                                                            Icons.Default.Close, 
                                                            contentDescription = "Clear", 
                                                            tint = pagePrimaryColor.copy(alpha = 0.7f)
                                                        )
                                                    }
                                                }
                                            },
                                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                            keyboardActions = KeyboardActions(onSearch = { 
                                                vaultBeatViewModel.search(vaultBeatSearchQuery.value) 
                                            }),
                                            colors = TextFieldDefaults.colors(
                                                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
                                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f),
                                                focusedIndicatorColor = Color.Transparent,
                                                unfocusedIndicatorColor = Color.Transparent,
                                                disabledIndicatorColor = Color.Transparent,
                                                focusedTextColor = pagePrimaryColor,
                                                unfocusedTextColor = pagePrimaryColor,
                                                cursorColor = pagePrimaryColor
                                            ),
                                            singleLine = true
                                        )

                                        if (vbState.searchResults.isNotEmpty()) {
                                            if (vbState.searchResults.size > 1 && !vbState.isBatchDownloading) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                                                    horizontalArrangement = Arrangement.End
                                                ) {
                                                    TextButton(
                                                        onClick = { showPlaylistDownloadDialog = true },
                                                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.primary),
                                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                                                    ) {
                                                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                                                        Spacer(Modifier.width(6.dp))
                                                        Text(stringResource(R.string.download_all), style = MaterialTheme.typography.labelMedium)
                                                    }
                                                }
                                            }
                                            
                                            if (vbState.isBatchDownloading) {
                                                Column(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
                                                    Text(
                                                        stringResource(R.string.downloading_playlist, vbState.batchProgress, if (vbState.batchTotal > 0) vbState.batchTotal else vbState.searchResults.size),
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = pageSecondaryColor
                                                    )
                                                    Spacer(Modifier.height(4.dp))
                                                    LinearProgressIndicator(
                                                        progress = { vbState.batchProgress.toFloat() / vbState.searchResults.size.toFloat() },
                                                        modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                                                        color = MaterialTheme.colorScheme.primary,
                                                        trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                                    )
                                                }
                                            }

                                            Spacer(modifier = Modifier.height(10.dp))
                                            LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                                items(vbState.searchResults.size) { i ->
                                                    val result = vbState.searchResults[i]
                                                    val downloadProgress = vbState.activeDownloads[result.id]
                                                    val isDownloaded = result.id in vbState.downloadedIds
                                                    
                                                    Card(
                                                        modifier = Modifier.fillMaxWidth().clickable { 
                                                            if (!isDownloaded && downloadProgress == null) {
                                                                selectedSongForDownload = result 
                                                            }
                                                        },
                                                        shape = RoundedCornerShape(16.dp),
                                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.12f))
                                                    ) {
                                                        Row(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                                                            AsyncImage(model = result.thumbnailUrl, contentDescription = null, modifier = Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)), contentScale = ContentScale.Crop)
                                                            Spacer(modifier = Modifier.width(14.dp))
                                                            Column(modifier = Modifier.weight(1f)) {
                                                                Text(result.title, style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Medium, color = pagePrimaryColor), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                                                Text("${formatDuration(result.durationMs)} • ${result.artist}", style = TextStyle(fontSize = 11.sp, color = pageSecondaryColor.copy(alpha = 0.8f)))
                                                            }
                                                            
                                                            when {
                                                                downloadProgress is com.example.vaultbeat.core.network.model.DownloadProgress.Downloading -> {
                                                                    CircularProgressIndicator(progress = { downloadProgress.percentage / 100f }, modifier = Modifier.size(24.dp), strokeWidth = 2.dp, color = pagePrimaryColor)
                                                                }
                                                                isDownloaded -> {
                                                                    Icon(
                                                                        Icons.Default.CheckCircle,
                                                                        contentDescription = null,
                                                                        modifier = Modifier.size(24.dp),
                                                                        tint = Color.Green
                                                                    )
                                                                }
                                                                else -> {
                                                                    Text("↓", style = TextStyle(fontSize = 18.sp, color = pagePrimaryColor))
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(16.dp))
                                        }
                                    }
                                }
                                "MUSIC" -> {
                                    AnimatedContent(
                                        targetState = musicPlaylistMode.value,
                                        transitionSpec = {
                                            (fadeIn(animationSpec = tween(400)) + slideInHorizontally(animationSpec = tween(400), initialOffsetX = { if (targetState) it else -it }))
                                                .togetherWith(fadeOut(animationSpec = tween(300)) + slideOutHorizontally(animationSpec = tween(300), targetOffsetX = { if (targetState) -it else it }))
                                        },
                                        label = "MusicPlaylistModeTransition"
                                    ) { isPlaylistMode ->
                                        if (isPlaylistMode) {
                                            Column(modifier = Modifier.fillMaxSize()) {
                                                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                                                    Text(stringResource(R.string.playlists_title), style = MaterialTheme.typography.titleLarge, color = pagePrimaryColor)
                                                    Button(
                                                        onClick = { showCreatePlaylistDialog.value = true },
                                                        modifier = Modifier.height(36.dp),
                                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f), contentColor = MaterialTheme.colorScheme.primary),
                                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                                    ) {
                                                        Text(stringResource(R.string.create_playlist), style = MaterialTheme.typography.labelMedium)
                                                    }
                                                }
                                                Spacer(modifier = Modifier.height(20.dp))

                                                if (selectedPlaylistId.value == null) {
                                                    if (playlists.isEmpty()) {
                                                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                                                            Text(stringResource(R.string.no_playlists), style = MaterialTheme.typography.bodyMedium, color = pageSecondaryColor, textAlign = TextAlign.Center)
                                                        }
                                                    } else {
                                                        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(bottom = 12.dp)) {
                                                            itemsIndexed(playlists) { _, playlist ->
                                                                val playlistSongIds by viewModel.observePlaylistSongIds(playlist.id).collectAsState(emptyList())
                                                                Card(
                                                                    modifier = Modifier.fillMaxWidth(),
                                                                    shape = RoundedCornerShape(20.dp),
                                                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.24f))
                                                                ) {
                                                                    Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                                                                        Column(modifier = Modifier.weight(1f).clickable { selectedPlaylistId.value = playlist.id }) {
                                                                            Text(playlist.name, style = MaterialTheme.typography.titleMedium, color = pagePrimaryColor)
                                                                            Spacer(modifier = Modifier.height(4.dp))
                                                                            Text(stringResource(R.string.playlist_songs_count, playlistSongIds.size), style = MaterialTheme.typography.bodySmall, color = pageSecondaryColor)
                                                                        }
                                                                        Box {
                                                                            Text("⋮", modifier = Modifier.clickable { expandedPlaylistMenuId.value = if (expandedPlaylistMenuId.value == playlist.id) null else playlist.id }.padding(8.dp), style = MaterialTheme.typography.titleLarge.copy(color = pagePrimaryColor))
                                                                            DropdownMenu(expanded = expandedPlaylistMenuId.value == playlist.id, onDismissRequest = { expandedPlaylistMenuId.value = null }) {
                                                                                DropdownMenuItem(text = { Text(stringResource(R.string.playlist_reproduce)) }, onClick = {
                                                                                    expandedPlaylistMenuId.value = null
                                                                                    val songsToPlay = playlistSongIds.mapNotNull { songsById[it] }
                                                                                    if (songsToPlay.isNotEmpty()) onPlayAlbum(songsToPlay, 0)
                                                                                })
                                                                                DropdownMenuItem(text = { Text(stringResource(R.string.sort_random)) }, onClick = {
                                                                                    expandedPlaylistMenuId.value = null
                                                                                    val songsToPlay = playlistSongIds.mapNotNull { songsById[it] }.shuffled()
                                                                                    if (songsToPlay.isNotEmpty()) onPlayAlbum(songsToPlay, 0)
                                                                                })
                                                                                DropdownMenuItem(text = { Text(stringResource(R.string.playlist_add)) }, onClick = {
                                                                                    expandedPlaylistMenuId.value = null
                                                                                    selectedPlaylistId.value = playlist.id
                                                                                    selectedSongIdsToAdd.value = emptySet()
                                                                                    songSearchQuery.value = ""
                                                                                    showSongSearchField.value = false
                                                                                    showAddSongsDialog.value = true
                                                                                })
                                                                                DropdownMenuItem(text = { Text(stringResource(R.string.dialog_rename_confirm)) }, onClick = {
                                                                                    expandedPlaylistMenuId.value = null
                                                                                    playlistRenameId.value = playlist.id
                                                                                    renamePlaylistName.value = playlist.name
                                                                                })
                                                                                DropdownMenuItem(text = { Text(stringResource(R.string.action_delete_device)) }, onClick = {
                                                                                    expandedPlaylistMenuId.value = null
                                                                                    viewModel.deletePlaylist(playlist.id)
                                                                                })
                                                                            }
                                                                        }
                                                                    }
                                                                }
                                                            }
                                                        }
                                                    }
                                                } else {
                                                    val playlist = playlists.firstOrNull { it.id == selectedPlaylistId.value }
                                                    val playlistSongs = remember(selectedPlaylistSongIds, songsById, playlist?.sortOrder) {
                                                        val unsorted = selectedPlaylistSongIds.mapNotNull { songsById[it] }
                                                        when (playlist?.sortOrder) {
                                                            "NAME" -> unsorted.sortedBy { it.title.lowercase() }
                                                            "ARTIST" -> unsorted.sortedBy { it.artist.lowercase() }
                                                            "DURATION" -> unsorted.sortedBy { it.durationMs }
                                                            "RANDOM" -> unsorted.shuffled()
                                                            else -> unsorted
                                                        }
                                                    }
                                                    val coverSong = playlist?.customCoverSongId?.let { songsById[it] }
                                                    val coverUri = coverSong?.albumArtUri ?: playlistSongs.firstOrNull()?.albumArtUri

                                                    if (playlist == null) {
                                                        selectedPlaylistId.value = null
                                                    } else {
                                                        LazyColumn(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(bottom = 12.dp)) {
                                                            item {
                                                                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                                                    Text(stringResource(R.string.section_back_playlists), modifier = Modifier.clickable { selectedPlaylistId.value = null }, style = MaterialTheme.typography.bodyMedium.copy(color = pagePrimaryColor))
                                                                }
                                                                Spacer(modifier = Modifier.height(16.dp))
                                                                Box(modifier = Modifier.fillMaxWidth().height(220.dp).clip(RoundedCornerShape(20.dp)).background(if (coverUri == null) Color.Black else Color.Transparent), contentAlignment = Alignment.Center) {
                                                                    coverUri?.let { AsyncImage(model = it, contentDescription = null, modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(20.dp)), contentScale = ContentScale.Crop) }
                                                                    
                                                                    IconButton(
                                                                        onClick = { showCoverPicker.value = true },
                                                                        modifier = Modifier
                                                                            .align(Alignment.TopEnd)
                                                                            .padding(8.dp)
                                                                            .background(Color.Black.copy(alpha = 0.4f), CircleShape)
                                                                            .size(32.dp)
                                                                    ) {
                                                                        Icon(Icons.Default.Edit, null, tint = Color.White, modifier = Modifier.size(16.dp))
                                                                    }
                                                                }
                                                                Spacer(modifier = Modifier.height(18.dp))
                                                                Text(playlist.name, style = MaterialTheme.typography.titleLarge, color = pagePrimaryColor)
                                                                Spacer(modifier = Modifier.height(6.dp))
                                                                Text(stringResource(R.string.playlist_songs_count, playlistSongs.size), style = MaterialTheme.typography.bodyMedium, color = pageSecondaryColor)
                                                                Spacer(modifier = Modifier.height(18.dp))
                                                                Button(onClick = { if (playlistSongs.isNotEmpty()) onPlayAlbum(playlistSongs, 0) }, enabled = playlistSongs.isNotEmpty(), modifier = Modifier.fillMaxWidth().height(44.dp), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f), contentColor = MaterialTheme.colorScheme.primary)) {
                                                                    Text(stringResource(R.string.playlist_reproduce))
                                                                }
                                                                Spacer(modifier = Modifier.height(12.dp))
                                                                Button(onClick = {
                                                                    selectedSongIdsToAdd.value = emptySet()
                                                                    songSearchQuery.value = ""
                                                                    showSongSearchField.value = false
                                                                    showAddSongsDialog.value = true
                                                                }, modifier = Modifier.fillMaxWidth().height(44.dp), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f), contentColor = MaterialTheme.colorScheme.primary)) {
                                                                    Text(stringResource(R.string.playlist_add))
                                                                }
                                                                Spacer(modifier = Modifier.height(20.dp))
                                                                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                                                                    Text(stringResource(R.string.playlist_songs_label), style = MaterialTheme.typography.titleMedium, color = pagePrimaryColor)
                                                                    IconButton(onClick = { showSortPicker.value = true }) {
                                                                        Icon(Icons.Filled.Sort, null, tint = pageSecondaryColor, modifier = Modifier.size(20.dp))
                                                                    }
                                                                }
                                                            }
                                                            itemsIndexed(playlistSongs) { _, song ->
                                                                Row(
                                                                    modifier = Modifier
                                                                        .fillMaxWidth()
                                                                        .combinedClickable(
                                                                            onClick = { onSongSelected(songs.indexOf(song)) },
                                                                            onLongClick = {
                                                                                selectedSongForMenu = song
                                                                                sourcePlaylistIdForMove = playlist.id
                                                                                showSongMenu = true
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
                                        } else {
                                            Column(modifier = Modifier.fillMaxSize()) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.End
                                                ) {
                                                    IconButton(onClick = { showMusicSearchField = !showMusicSearchField }) {
                                                        Icon(Icons.Default.Search, null, tint = pagePrimaryColor.copy(alpha = 0.7f), modifier = Modifier.size(20.dp))
                                                    }
                                                    IconButton(onClick = { 
                                                        musicSortOrder = when(musicSortOrder) {
                                                            "RECENT" -> "NAME"
                                                            "NAME" -> "ARTIST"
                                                            else -> "RECENT"
                                                        }
                                                    }) {
                                                        Icon(Icons.Default.Sort, null, tint = pagePrimaryColor.copy(alpha = 0.7f), modifier = Modifier.size(20.dp))
                                                    }
                                                    IconButton(onClick = {
                                                        isReloadingAllCovers = true
                                                        scope.launch {
                                                            libraryViewModel.reloadAllCovers { current, total ->
                                                                reloadAllProgress = current to total
                                                            }
                                                            isReloadingAllCovers = false
                                                        }
                                                    }) {
                                                        Icon(Icons.Default.Refresh, null, tint = pagePrimaryColor.copy(alpha = 0.7f), modifier = Modifier.size(20.dp))
                                                    }
                                                }

                                                AnimatedVisibility(visible = showMusicSearchField) {
                                                    TextField(
                                                        value = musicSearchQuery,
                                                        onValueChange = { musicSearchQuery = it },
                                                        placeholder = { Text(stringResource(R.string.search_placeholder), style = MaterialTheme.typography.bodySmall) },
                                                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp).height(48.dp),
                                                        shape = RoundedCornerShape(24.dp),
                                                        colors = TextFieldDefaults.colors(
                                                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                                                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f),
                                                            focusedIndicatorColor = Color.Transparent,
                                                            unfocusedIndicatorColor = Color.Transparent
                                                        ),
                                                        singleLine = true,
                                                        trailingIcon = {
                                                            if (musicSearchQuery.isNotEmpty()) {
                                                                IconButton(onClick = { musicSearchQuery = "" }) {
                                                                    Icon(Icons.Default.Close, null, modifier = Modifier.size(16.dp))
                                                                }
                                                            }
                                                        }
                                                    )
                                                }

                                                LazyColumn(modifier = Modifier.weight(1f), contentPadding = PaddingValues(0.dp)) {
                                                    itemsIndexed(items = filteredSongs, key = { _, s -> s.id }) { _, song ->
                                                        val index = songs.indexOf(song)
                                                        Row(modifier = Modifier
                                                            .fillMaxWidth()
                                                            .combinedClickable(
                                                                onClick = { selectedIndex.value = index; onSongSelected(index) },
                                                                onLongClick = { 
                                                                    selectedSongForMenu = song
                                                                    showSongMenu = true 
                                                                }
                                                            )
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
                                        onLongClick = { 
                                            selectedSongForMenu = it
                                            showSongMenu = true 
                                        },
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
                                        onLongClick = { 
                                            selectedSongForMenu = it
                                            showSongMenu = true 
                                        },
                                        textColor = pagePrimaryColor,
                                        secondaryTextColor = pageSecondaryColor
                                    )
                                }
                                "PODCASTS" -> {
                                    if (podcastSongs.isEmpty()) {
                                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                            Text(stringResource(R.string.no_podcasts), style = MaterialTheme.typography.bodyMedium, color = pagePrimaryColor, textAlign = TextAlign.Center)
                                        }
                                    } else {
                                        LazyColumn(contentPadding = PaddingValues(0.dp)) {
                                            itemsIndexed(items = podcastSongs, key = { _, s -> s.id }) { _, song ->
                                                val globalIndex = songIndexById[song.id] ?: 0
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .combinedClickable(
                                                            onClick = { selectedIndex.value = globalIndex; onSongSelected(globalIndex) },
                                                            onLongClick = {
                                                                selectedSongForMenu = song
                                                                showSongMenu = true
                                                            }
                                                        )
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
                                    Column(
                                        modifier = Modifier.fillMaxSize(),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        HorizontalPager(
                                            state = pagerState,
                                            modifier = Modifier.fillMaxWidth().height(240.dp),
                                            contentPadding = PaddingValues(0.dp),
                                            beyondViewportPageCount = 0
                                        ) { page ->
                                            val song = if (ui.song != null) songs.getOrNull(page) else null
                                            
                                            Box(
                                                modifier = Modifier.fillMaxSize(),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                AsyncImage(
                                                    model = song?.albumArtUri ?: R.drawable.portada,
                                                    contentDescription = "Album art",
                                                    modifier = Modifier
                                                        .size(180.dp)
                                                        .clip(RoundedCornerShape(22.dp)),
                                                    contentScale = ContentScale.Crop
                                                )
                                            }
                                        }
                                        
                                        Spacer(modifier = Modifier.height(12.dp))
                                        
                                        Column(modifier = Modifier.fillMaxWidth(0.9f), horizontalAlignment = Alignment.Start) {
                                            Text(
                                                ui.song?.title ?: stringResource(R.string.select_song),
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis,
                                                style = TextStyle(
                                                    fontSize = 28.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = contrastTextColors.primary,
                                                    letterSpacing = (-0.5).sp
                                                )
                                            )
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Text(
                                                ui.song?.artist ?: "",
                                                style = TextStyle(fontSize = 17.sp, color = contrastTextColors.secondary)
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                ui.song?.album ?: "",
                                                style = TextStyle(fontSize = 13.sp, color = contrastTextColors.tertiary)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Box(modifier = Modifier.weight(1f).fillMaxWidth().padding(12.dp)) {
                Card(
                    modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(24.dp)),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.24f))
                ) {
                    Box(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 14.dp)) {
                        IconButton(
                            onClick = { isMenuCollapsed = !isMenuCollapsed },
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .size(24.dp)
                                .offset(x = (-10).dp, y = 6.dp)
                        ) {
                            Icon(
                                imageVector = if (isMenuCollapsed) Icons.AutoMirrored.Filled.MenuOpen else Icons.Default.Menu,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                                modifier = Modifier.size(14.dp)
                            )
                        }

                        IconButton(
                            onClick = {
                                val currentQueue = when {
                                    selectedPlaylistId.value != null -> songs.filter { it.id in selectedPlaylistSongIds }
                                    selectedArtist.value != null -> songs.filter { extractPrimaryArtist(it.artist) == selectedArtist.value }
                                    selectedAlbum.value != null -> songs.filter { it.album == selectedAlbum.value?.album && it.artist == selectedAlbum.value?.artist }
                                    else -> songs
                                }
                                if (currentQueue.isNotEmpty()) onPlayAlbum(currentQueue.shuffled(), 0)
                            },
                            modifier = Modifier.align(Alignment.TopEnd).size(28.dp)
                        ) {
                            Icon(
                                Icons.Default.Shuffle, 
                                contentDescription = null, 
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), 
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.SpaceEvenly,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.Start) {
                                Text(
                                    ui.song?.title ?: stringResource(R.string.select_song),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    ui.song?.artist ?: "",
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    formatDuration(position),
                                    style = TextStyle(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f), fontSize = 11.sp)
                                )
                                Slider(
                                    value = if (duration > 0L) (position.toFloat() / duration.toFloat()).coerceIn(0f, 1f) else 0f,
                                    onValueChange = { frac -> if (duration > 0L) viewModel.seekTo((frac * duration).toLong()) },
                                    modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                                    enabled = duration > 0L,
                                    colors = SliderDefaults.colors(
                                        thumbColor = MaterialTheme.colorScheme.primary,
                                        activeTrackColor = MaterialTheme.colorScheme.primary
                                    )
                                )
                                Text(
                                    formatDuration(duration),
                                    style = TextStyle(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f), fontSize = 11.sp)
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                                val isPressed by interactionSource.collectIsPressedAsState()
                                val skipScale by animateFloatAsState(if (isPressed) 0.9f else 1f, label = "SkipScale")

                                IconButton(
                                    onClick = { viewModel.previous() },
                                    interactionSource = interactionSource,
                                    modifier = Modifier
                                        .size(48.dp)
                                        .graphicsLayer(scaleX = skipScale, scaleY = skipScale)
                                        .background(
                                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                            CircleShape
                                        )
                                ) {
                                    Icon(
                                        Icons.Rounded.SkipPrevious,
                                        contentDescription = stringResource(R.string.previous),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }

                                Spacer(Modifier.width(24.dp))

                                val playPauseInteractionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                                val isPlayPausePressed by playPauseInteractionSource.collectIsPressedAsState()
                                val playPauseScale by animateFloatAsState(if (isPlayPausePressed) 0.85f else 1f, label = "PlayPauseScale")

                                Box(
                                    modifier = Modifier
                                        .size(64.dp)
                                        .graphicsLayer(scaleX = playPauseScale, scaleY = playPauseScale)
                                        .clip(CircleShape)
                                        .background(
                                            brush = Brush.radialGradient(
                                                colors = listOf(
                                                    MaterialTheme.colorScheme.primary,
                                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.85f)
                                                )
                                            )
                                        )
                                        .clickable(
                                            interactionSource = playPauseInteractionSource, 
                                            indication = ripple(bounded = true)
                                        ) { viewModel.togglePlayPause() },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (ui.playerState.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                        contentDescription = "Play/Pause",
                                        tint = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.size(34.dp)
                                    )
                                }

                                Spacer(Modifier.width(24.dp))

                                val nextInteractionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                                val isNextPressed by nextInteractionSource.collectIsPressedAsState()
                                val nextScale by animateFloatAsState(if (isNextPressed) 0.9f else 1f, label = "NextScale")

                                IconButton(
                                    onClick = { viewModel.next() },
                                    interactionSource = nextInteractionSource,
                                    modifier = Modifier
                                        .size(48.dp)
                                        .graphicsLayer(scaleX = nextScale, scaleY = nextScale)
                                        .background(
                                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                            CircleShape
                                        )
                                ) {
                                    Icon(
                                        Icons.Rounded.SkipNext,
                                        contentDescription = stringResource(R.string.next),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        if (selectedSongForDownload != null) {
            DownloadOptionsDialog(
                song = selectedSongForDownload,
                playlists = playlists,
                selectedPlaylistId = selectedPlaylistId.value,
                onPlaylistSelected = { selectedPlaylistId.value = it },
                onDownload = { result, playlistId ->
                    if (playlistId != null) {
                        vaultBeatViewModel.downloadToPlaylist(result, playlistId) { onRefresh() }
                    } else {
                        vaultBeatViewModel.download(result) { onRefresh() }
                    }
                    selectedSongForDownload = null
                },
                onDismiss = { selectedSongForDownload = null },
                primaryColor = pagePrimaryColor,
                secondaryColor = pageSecondaryColor
            )
        }

        if (showPlaylistDownloadDialog) {
            BatchDownloadDialog(
                vbState = vbState,
                playlists = playlists,
                onDownload = { playlistId, newName ->
                    if (newName != null) {
                        viewModel.createPlaylist(newName) { newId ->
                            vaultBeatViewModel.downloadPlaylist(vbState.searchResults, newId) { onRefresh() }
                        }
                    } else {
                        vaultBeatViewModel.downloadPlaylist(vbState.searchResults, playlistId) { onRefresh() }
                    }
                    showPlaylistDownloadDialog = false
                },
                onDismiss = { showPlaylistDownloadDialog = false },
                primaryColor = pagePrimaryColor,
                secondaryColor = pageSecondaryColor
            )
        }

        if (showSongMenu && selectedSongForMenu != null) {
            SongOptionsMenu(
                song = selectedSongForMenu,
                inPlaylist = sourcePlaylistIdForMove != null,
                isLibrary = menu.value == "MUSIC" && !musicPlaylistMode.value,
                onRemoveFromPlaylist = {
                    viewModel.removeSongFromPlaylist(sourcePlaylistIdForMove!!, selectedSongForMenu!!.id)
                    showSongMenu = false
                    sourcePlaylistIdForMove = null
                },
                onMoveToPlaylist = {
                    showPlaylistPickerForMove = true
                    showSongMenu = false
                },
                onAddToPlaylist = {
                    showPlaylistPickerForAdd = true
                    showSongMenu = false
                },
                onReloadCover = {
                    showSongMenu = false
                    isReloadingCover = true
                    scope.launch {
                        reloadArtworkUrl = libraryViewModel.searchCover(selectedSongForMenu!!)
                        isReloadingCover = false
                        showReloadConfirmation = true
                    }
                },
                onDeleteFromDevice = {
                    libraryViewModel.deleteSongFromDevice(selectedSongForMenu!!)
                    showSongMenu = false
                    sourcePlaylistIdForMove = null
                },
                onDismiss = { showSongMenu = false; sourcePlaylistIdForMove = null },
                primaryColor = pagePrimaryColor,
                secondaryColor = pageSecondaryColor
            )
        }

        if (showPlaylistPickerForAdd || showPlaylistPickerForMove) {
            PlaylistPickerDialog(
                playlists = playlists,
                excludePlaylistId = sourcePlaylistIdForMove,
                onPlaylistSelected = { playlistId ->
                    if (showPlaylistPickerForAdd) {
                        viewModel.addSongToPlaylist(playlistId, selectedSongForMenu!!.id)
                    } else {
                        viewModel.removeSongFromPlaylist(sourcePlaylistIdForMove!!, selectedSongForMenu!!.id)
                        viewModel.addSongToPlaylist(playlistId, selectedSongForMenu!!.id)
                    }
                    showPlaylistPickerForAdd = false
                    showPlaylistPickerForMove = false
                    sourcePlaylistIdForMove = null
                },
                onDismiss = { showPlaylistPickerForAdd = false; showPlaylistPickerForMove = false }
            )
        }

        if (showCreatePlaylistDialog.value) {
            CreatePlaylistDialog(
                playlistName = playlistName.value,
                onNameChange = { playlistName.value = it },
                onCreate = {
                    val name = playlistName.value.trim()
                    if (name.isNotEmpty()) {
                        viewModel.createPlaylist(name) { playlistId -> selectedPlaylistId.value = playlistId }
                        playlistName.value = ""
                        showCreatePlaylistDialog.value = false
                    }
                },
                onDismiss = { showCreatePlaylistDialog.value = false },
                secondaryColor = pageSecondaryColor
            )
        }

        if (isReloadingCover) {
            AlertDialog(
                onDismissRequest = { },
                title = { Text(stringResource(R.string.dialog_reload_searching)) },
                text = {
                    Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = pagePrimaryColor)
                    }
                },
                confirmButton = { },
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.95f),
                shape = RoundedCornerShape(24.dp)
            )
        }

        if (showReloadConfirmation) {
            // ... (keep existing reload confirmation)
            AlertDialog(
                onDismissRequest = { showReloadConfirmation = false },
                title = { Text(stringResource(R.string.dialog_reload_cover_title)) },
                text = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        if (reloadArtworkUrl != null) {
                            AsyncImage(
                                model = reloadArtworkUrl,
                                contentDescription = null,
                                modifier = Modifier.size(160.dp).clip(RoundedCornerShape(16.dp)),
                                contentScale = ContentScale.Crop
                            )
                            Spacer(Modifier.height(16.dp))
                            Text(stringResource(R.string.dialog_reload_cover_desc))
                        } else {
                            Text(stringResource(R.string.dialog_reload_no_artwork))
                        }
                    }
                },
                confirmButton = {
                    if (reloadArtworkUrl != null) {
                        TextButton(onClick = {
                            libraryViewModel.updateSongCover(selectedSongForMenu!!.id, reloadArtworkUrl!!)
                            showReloadConfirmation = false
                        }) {
                            Text(stringResource(R.string.dialog_replace), color = pagePrimaryColor)
                        }
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showReloadConfirmation = false }) {
                        Text(stringResource(R.string.dialog_cancel))
                    }
                },
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.95f),
                shape = RoundedCornerShape(24.dp)
            )
        }

        if (isReloadingAllCovers) {
            AlertDialog(
                onDismissRequest = { },
                title = { Text(stringResource(R.string.dialog_reload_all_title)) },
                text = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(stringResource(R.string.dialog_reload_all_desc), style = MaterialTheme.typography.bodySmall, color = pageSecondaryColor)
                        Spacer(Modifier.height(16.dp))
                        CircularProgressIndicator(
                            progress = { if (reloadAllProgress.second > 0) reloadAllProgress.first.toFloat() / reloadAllProgress.second.toFloat() else 0f },
                            color = pagePrimaryColor
                        )
                        Spacer(Modifier.height(8.dp))
                        Text("${reloadAllProgress.first} / ${reloadAllProgress.second}", style = MaterialTheme.typography.labelLarge, color = pagePrimaryColor)
                    }
                },
                confirmButton = { },
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.95f),
                shape = RoundedCornerShape(24.dp)
            )
        }

        if (playlistRenameId.value != null) {
            RenamePlaylistDialog(
                currentName = renamePlaylistName.value,
                onNameChange = { renamePlaylistName.value = it },
                onRename = {
                    val newName = renamePlaylistName.value.trim()
                    if (newName.isNotEmpty()) {
                        viewModel.renamePlaylist(playlistRenameId.value!!, newName)
                        playlistRenameId.value = null
                    }
                },
                onDismiss = { playlistRenameId.value = null }
            )
        }

        if (showAddSongsDialog.value && selectedPlaylistId.value != null) {
            val availableSongs = songs.filter { it.id !in selectedPlaylistSongIds }
            val displayedSongs = if (songSearchQuery.value.isBlank()) availableSongs else availableSongs.filter { it.title.contains(songSearchQuery.value, ignoreCase = true) || it.artist.contains(songSearchQuery.value, ignoreCase = true) }
            
            AddSongsDialog(
                songs = displayedSongs,
                selectedSongIds = selectedSongIdsToAdd.value,
                onToggleSong = { id ->
                    val current = selectedSongIdsToAdd.value.toMutableSet()
                    if (current.contains(id)) current.remove(id) else current.add(id)
                    selectedSongIdsToAdd.value = current
                },
                onAdd = {
                    selectedSongIdsToAdd.value.forEach { viewModel.addSongToPlaylist(selectedPlaylistId.value!!, it) }
                    showAddSongsDialog.value = false
                },
                onDismiss = { showAddSongsDialog.value = false },
                searchQuery = songSearchQuery.value,
                onSearchQueryChange = { songSearchQuery.value = it },
                showSearchField = showSongSearchField.value,
                onToggleSearchField = { 
                    showSongSearchField.value = !showSongSearchField.value
                    if (!showSongSearchField.value) songSearchQuery.value = ""
                },
                primaryColor = pagePrimaryColor,
                secondaryColor = pageSecondaryColor
            )
        }

        if (showCoverPicker.value && selectedPlaylistId.value != null) {
            val playlistSongs = selectedPlaylistSongIds.mapNotNull { songsById[it] }
            CoverPickerDialog(
                songs = playlistSongs,
                onSelectCover = { songId ->
                    viewModel.updatePlaylistCover(selectedPlaylistId.value!!, songId)
                    showCoverPicker.value = false
                },
                onDismiss = { showCoverPicker.value = false },
                secondaryColor = pageSecondaryColor
            )
        }

        if (showSortPicker.value && selectedPlaylistId.value != null) {
            SortOrderDialog(
                onSelectSortOrder = { sortOrder ->
                    viewModel.updatePlaylistSortOrder(selectedPlaylistId.value!!, sortOrder)
                    showSortPicker.value = false
                },
                onDismiss = { showSortPicker.value = false }
            )
        }
    }
}
