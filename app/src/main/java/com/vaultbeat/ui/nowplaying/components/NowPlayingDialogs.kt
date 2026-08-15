package com.vaultbeat.ui.nowplaying.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import androidx.compose.ui.res.stringResource
import com.vaultbeat.R
import com.vaultbeat.core.model.Song
import com.vaultbeat.core.network.model.SearchResult
import com.vaultbeat.ui.vaultbeat.VaultBeatUiState
import com.vaultbeat.data.local.PlaylistEntity

@Composable
fun DownloadOptionsDialog(
    song: SearchResult?,
    playlists: List<PlaylistEntity>,
    selectedPlaylistId: Long?,
    onPlaylistSelected: (Long?) -> Unit,
    onDownload: (SearchResult, Long?) -> Unit,
    onDismiss: () -> Unit,
    primaryColor: Color,
    secondaryColor: Color
) {
    if (song == null) return
    var expanded by remember { mutableStateOf(false) }
    val selectedPlaylist = playlists.find { it.id == selectedPlaylistId }
    val dropdownLabel = selectedPlaylist?.name ?: stringResource(R.string.dialog_only_music)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.dialog_download_options), color = primaryColor) },
        text = {
            Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(20.dp)) {
                Card(modifier = Modifier.size(140.dp), shape = RoundedCornerShape(20.dp)) {
                    AsyncImage(model = song.thumbnailUrl, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(song.title, style = MaterialTheme.typography.titleMedium, color = primaryColor, textAlign = TextAlign.Center)
                    Text(song.displayArtist, style = MaterialTheme.typography.bodySmall, color = secondaryColor)
                }
                Box(modifier = Modifier.fillMaxWidth()) {
                    Button(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), contentColor = primaryColor)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(dropdownLabel, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text("▼")
                        }
                    }
                    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }, modifier = Modifier.fillMaxWidth(0.7f).background(MaterialTheme.colorScheme.surfaceVariant)) {
                        DropdownMenuItem(text = { Text(stringResource(R.string.dialog_only_music)) }, onClick = { onPlaylistSelected(null); expanded = false })
                        playlists.forEach { playlist ->
                            DropdownMenuItem(text = { Text(playlist.name) }, onClick = { onPlaylistSelected(playlist.id); expanded = false })
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onDownload(song, selectedPlaylistId) }) { Text(stringResource(R.string.dialog_download_now)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.dialog_cancel), color = secondaryColor) } },
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(28.dp)
    )
}

@Composable
fun BatchDownloadDialog(
    vbState: VaultBeatUiState,
    playlists: List<PlaylistEntity>,
    onDownload: (Long?, String?) -> Unit,
    onDismiss: () -> Unit,
    primaryColor: Color,
    secondaryColor: Color
) {
    var expanded by remember { mutableStateOf(false) }
    var selectedPlaylistIdBatch by remember { mutableStateOf<Long?>(null) }
    var showCreatePlaylistInput by remember { mutableStateOf(false) }
    var newPlaylistNameBatch by remember { mutableStateOf("") }
    val selectedPlaylistBatch = playlists.find { it.id == selectedPlaylistIdBatch }
    val dropdownLabelBatch = when {
        showCreatePlaylistInput -> stringResource(R.string.dialog_new_playlist_prefix, newPlaylistNameBatch)
        selectedPlaylistBatch != null -> selectedPlaylistBatch.name
        else -> stringResource(R.string.dialog_only_music)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                stringResource(R.string.dialog_batch_download_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold,
                color = primaryColor
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    stringResource(R.string.dialog_batch_download_desc, vbState.searchResults.size),
                    style = MaterialTheme.typography.bodyMedium,
                    color = secondaryColor
                )

                Text(stringResource(R.string.dialog_download_dest), style = MaterialTheme.typography.labelLarge, color = primaryColor)

                Box(modifier = Modifier.fillMaxWidth()) {
                    Button(
                        onClick = { expanded = true },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                            contentColor = primaryColor
                        )
                    ) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text(dropdownLabelBatch, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodyLarge)
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                        }
                    }
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                        modifier = Modifier.fillMaxWidth(0.8f).background(MaterialTheme.colorScheme.surface).clip(RoundedCornerShape(16.dp))
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.dialog_only_music)) },
                            onClick = { selectedPlaylistIdBatch = null; showCreatePlaylistInput = false; expanded = false },
                            leadingIcon = { Icon(Icons.Default.MusicNote, null) }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.dialog_create_new_playlist)) },
                            onClick = { showCreatePlaylistInput = true; selectedPlaylistIdBatch = null; expanded = false },
                            leadingIcon = { Icon(Icons.Default.Add, null) }
                        )
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = secondaryColor.copy(alpha = 0.1f))
                        playlists.forEach { playlist ->
                            DropdownMenuItem(
                                text = { Text(playlist.name) },
                                onClick = { selectedPlaylistIdBatch = playlist.id; showCreatePlaylistInput = false; expanded = false },
                                leadingIcon = { Icon(Icons.Filled.PlaylistPlay, null) }
                            )
                        }
                    }
                }

                if (showCreatePlaylistInput) {
                    OutlinedTextField(
                        value = newPlaylistNameBatch,
                        onValueChange = { newPlaylistNameBatch = it },
                        placeholder = { Text(stringResource(R.string.dialog_new_playlist_placeholder)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onDownload(selectedPlaylistIdBatch, if (showCreatePlaylistInput) newPlaylistNameBatch else null)
                },
                shape = RoundedCornerShape(14.dp)
            ) { Text(stringResource(R.string.download_all), fontWeight = FontWeight.Bold) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.dialog_cancel), color = secondaryColor)
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(28.dp),
        tonalElevation = 8.dp
    )
}

@Composable
fun SongOptionsMenu(
    song: Song?,
    inPlaylist: Boolean,
    isLibrary: Boolean,
    onRemoveFromPlaylist: () -> Unit,
    onMoveToPlaylist: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onReloadCover: () -> Unit,
    onDeleteFromDevice: () -> Unit,
    onDismiss: () -> Unit,
    primaryColor: Color,
    secondaryColor: Color
) {
    if (song == null) return
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(song.title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(song.displayArtist, style = MaterialTheme.typography.bodySmall, color = secondaryColor)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                if (inPlaylist) {
                    MenuActionItem(Icons.Default.RemoveCircleOutline, stringResource(R.string.action_remove_playlist), primaryColor, onRemoveFromPlaylist)
                    MenuActionItem(Icons.Default.DriveFileMove, stringResource(R.string.action_move_playlist), primaryColor, onMoveToPlaylist)
                }
                if (isLibrary || inPlaylist) {
                    MenuActionItem(Icons.AutoMirrored.Filled.PlaylistAdd, stringResource(R.string.action_add_playlist), primaryColor, onAddToPlaylist)
                }
                MenuActionItem(Icons.Default.ImageSearch, stringResource(R.string.action_reload_cover), primaryColor, onReloadCover)
                MenuActionItem(Icons.Default.Delete, stringResource(R.string.action_delete_device), Color.Red.copy(alpha = 0.8f), onDeleteFromDevice)
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.dialog_close)) } },
        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.95f),
        shape = RoundedCornerShape(24.dp)
    )
}

@Composable
fun PlaylistPickerDialog(
    playlists: List<PlaylistEntity>,
    excludePlaylistId: Long?,
    onPlaylistSelected: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.dialog_add_songs_title)) },
        text = {
            LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) {
                items(playlists) { playlist ->
                    if (playlist.id != excludePlaylistId) {
                        Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable {
                            onPlaylistSelected(playlist.id)
                        }, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))) {
                            Text(playlist.name, modifier = Modifier.padding(16.dp))
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.dialog_cancel)) } }
    )
}

@Composable
fun CreatePlaylistDialog(
    playlistName: String,
    onNameChange: (String) -> Unit,
    onCreate: () -> Unit,
    onDismiss: () -> Unit,
    secondaryColor: Color
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = onCreate,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) { Text(stringResource(R.string.dialog_create), fontWeight = FontWeight.Bold) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.dialog_cancel), color = secondaryColor)
            }
        },
        title = {
            Text(
                stringResource(R.string.dialog_new_playlist_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = (-0.5).sp
            )
        },
        text = {
            Column {
                Text(
                    stringResource(R.string.dialog_new_playlist_desc),
                    style = MaterialTheme.typography.bodyMedium,
                    color = secondaryColor,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                OutlinedTextField(
                    value = playlistName,
                    onValueChange = onNameChange,
                    placeholder = { Text(stringResource(R.string.dialog_new_playlist_hint), color = secondaryColor.copy(alpha = 0.5f)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = secondaryColor.copy(alpha = 0.3f),
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f)
                    )
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(28.dp),
        tonalElevation = 8.dp
    )
}

@Composable
fun RenamePlaylistDialog(
    currentName: String,
    onNameChange: (String) -> Unit,
    onRename: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onRename) { Text(stringResource(R.string.dialog_rename_confirm)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.dialog_cancel)) } },
        title = { Text(stringResource(R.string.dialog_rename_playlist)) },
        text = { OutlinedTextField(value = currentName, onValueChange = onNameChange, label = { Text(stringResource(R.string.dialog_new_name_label)) }, modifier = Modifier.fillMaxWidth(), singleLine = true) }
    )
}

@Composable
fun AddSongsDialog(
    songs: List<Song>,
    selectedSongIds: Set<Long>,
    onToggleSong: (Long) -> Unit,
    onAdd: () -> Unit,
    onDismiss: () -> Unit,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    showSearchField: Boolean,
    onToggleSearchField: () -> Unit,
    primaryColor: Color,
    secondaryColor: Color
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = onAdd,
                shape = RoundedCornerShape(12.dp)
            ) { Text(stringResource(R.string.dialog_done), fontWeight = FontWeight.Bold) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.dialog_cancel), color = secondaryColor)
            }
        },
        title = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        stringResource(R.string.dialog_add_songs_title),
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onToggleSearchField) {
                        Icon(Icons.Default.Search, contentDescription = null, tint = primaryColor)
                    }
                }
                if (showSearchField) {
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = onSearchQueryChange,
                        placeholder = { Text(stringResource(R.string.dialog_search_library_placeholder)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary)
                    )
                }
            }
        },
        text = {
            Box(modifier = Modifier.heightIn(max = 340.dp)) {
                if (songs.isEmpty()) {
                    Text(
                        stringResource(R.string.dialog_no_more_songs),
                        style = MaterialTheme.typography.bodyMedium,
                        color = secondaryColor,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(16.dp)
                    )
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        items(songs) { song ->
                            val isSelected = selectedSongIds.contains(song.id)
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onToggleSong(song.id) },
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                    else Color.Transparent
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = isSelected,
                                        onCheckedChange = { onToggleSong(song.id) },
                                        colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            song.title,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = primaryColor,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            song.displayArtist,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = secondaryColor
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(28.dp)
    )
}

@Composable
fun CoverPickerDialog(
    songs: List<Song>,
    onSelectCover: (Long?) -> Unit,
    onDismiss: () -> Unit,
    secondaryColor: Color
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.dialog_choose_cover)) },
        text = {
            Column {
                Text(stringResource(R.string.dialog_choose_cover_desc), style = MaterialTheme.typography.bodySmall, color = secondaryColor)
                Spacer(Modifier.height(12.dp))
                LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable {
                                onSelectCover(null)
                            },
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
                        ) {
                            Text(stringResource(R.string.dialog_reset_cover), modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                    items(songs.filter { it.albumArtUri != null }) { song ->
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable {
                                onSelectCover(song.id)
                            },
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
                        ) {
                            Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                AsyncImage(model = song.albumArtUri, contentDescription = null, modifier = Modifier.size(40.dp).clip(RoundedCornerShape(8.dp)), contentScale = ContentScale.Crop)
                                Spacer(Modifier.width(12.dp))
                                Text(song.title, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.dialog_close)) } },
        shape = RoundedCornerShape(24.dp)
    )
}

@Composable
fun SortOrderDialog(
    onSelectSortOrder: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val sortOptions = listOf(
        "DATE_ADDED" to stringResource(R.string.sort_date),
        "NAME" to stringResource(R.string.sort_name),
        "ARTIST" to stringResource(R.string.sort_artist),
        "DURATION" to stringResource(R.string.sort_duration),
        "RANDOM" to stringResource(R.string.sort_random)
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.dialog_sort_title)) },
        text = {
            Column {
                sortOptions.forEach { (key, label) ->
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable {
                            onSelectSortOrder(key)
                        },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
                    ) {
                        Text(label, modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.dialog_close)) } },
        shape = RoundedCornerShape(24.dp)
    )
}

@Composable
private fun MenuActionItem(icon: ImageVector, label: String, color: Color, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 12.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(16.dp))
        Text(label, color = color, style = MaterialTheme.typography.bodyMedium)
    }
}

