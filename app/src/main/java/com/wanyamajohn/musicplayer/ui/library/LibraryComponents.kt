package com.wanyamajohn.musicplayer.ui.library

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wanyamajohn.musicplayer.core.formatDuration
import com.wanyamajohn.musicplayer.core.formatSize
import com.wanyamajohn.musicplayer.data.db.PlaylistSummary
import com.wanyamajohn.musicplayer.domain.model.Song
import com.wanyamajohn.musicplayer.ui.components.Artwork
import com.wanyamajohn.musicplayer.ui.components.SongInfoDialog
import com.wanyamajohn.musicplayer.ui.components.TextInputDialog
import com.wanyamajohn.musicplayer.ui.components.shareSongs

class SongActions(
    val play: (List<Song>, Int, Boolean) -> Unit,
    val addToQueue: (List<Song>) -> Unit,
    val playNext: (Song) -> Unit,
    val toggleFavourite: (Song) -> Unit,
    val addToPlaylist: (List<Song>) -> Unit,
    val share: (List<Song>) -> Unit,
    val info: (Song) -> Unit,
)

@Composable
fun rememberSongActions(vm: LibraryViewModel): SongActions {
    val context = LocalContext.current
    var pickerSongs by remember { mutableStateOf<List<Song>?>(null) }
    var infoSong by remember { mutableStateOf<Song?>(null) }
    val playlists by vm.playlists.collectAsStateWithLifecycle()

    pickerSongs?.let { songs ->
        PlaylistPickerDialog(
            playlists = playlists,
            onPick = { id -> vm.addToPlaylist(id, songs); pickerSongs = null; Toast.makeText(context, "Added to playlist", Toast.LENGTH_SHORT).show() },
            onCreate = { name -> vm.createPlaylistWith(name, songs); pickerSongs = null; Toast.makeText(context, "Playlist created", Toast.LENGTH_SHORT).show() },
            onDismiss = { pickerSongs = null },
        )
    }
    infoSong?.let { SongInfoDialog(it) { infoSong = null } }

    return remember(vm) {
        SongActions(
            play = vm::play,
            addToQueue = { vm.addToQueue(it); Toast.makeText(context, "Added to queue", Toast.LENGTH_SHORT).show() },
            playNext = { vm.playNext(it); Toast.makeText(context, "Playing next", Toast.LENGTH_SHORT).show() },
            toggleFavourite = vm::toggleFavourite,
            addToPlaylist = { pickerSongs = it },
            share = { shareSongs(context, it) },
            info = { infoSong = it },
        )
    }
}

@Composable
fun PlaylistPickerDialog(playlists: List<PlaylistSummary>, onPick: (Long) -> Unit, onCreate: (String) -> Unit, onDismiss: () -> Unit) {
    var creating by remember { mutableStateOf(false) }
    if (creating) {
        TextInputDialog("New playlist", confirmLabel = "Create", onConfirm = onCreate, onDismiss = { creating = false })
        return
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add to playlist") },
        text = {
            LazyColumn {
                item { ListItem(headlineContent = { Text("New playlist…") }, leadingContent = { Icon(Icons.Default.Add, null) },
                    modifier = Modifier.combinedClickableCompat { creating = true }, colors = ListItemDefaults.colors(containerColor = Color.Transparent)) }
                items(playlists.size) { i ->
                    val p = playlists[i]
                    ListItem(headlineContent = { Text(p.name) }, supportingContent = { Text("${p.songCount} songs") },
                        modifier = Modifier.combinedClickableCompat { onPick(p.id) }, colors = ListItemDefaults.colors(containerColor = Color.Transparent))
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@OptIn(ExperimentalFoundationApi::class)
fun Modifier.combinedClickableCompat(onClick: () -> Unit): Modifier = this.combinedClickable(onClick = onClick)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SongRow(
    song: Song,
    favourite: Boolean,
    selected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    actions: SongActions,
) {
    var menu by remember { mutableStateOf(false) }
    Row(
        Modifier.fillMaxWidth()
            .background(if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.18f) else Color.Transparent)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Artwork(song.artworkUri, Modifier.size(48.dp), RoundedCornerShape(8.dp))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(song.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("${song.artist} • ${song.album}", maxLines = 1, overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("${song.format} • ${formatSize(song.sizeBytes)}", style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (favourite) Icon(Icons.Default.Favorite, "Favourite", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(8.dp))
        Text(formatDuration(song.durationMs), style = MaterialTheme.typography.bodySmall)
        Box {
            IconButton(onClick = { menu = true }) { Icon(Icons.Default.MoreVert, "More") }
            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                DropdownMenuItem(text = { Text("Play next") }, onClick = { menu = false; actions.playNext(song) })
                DropdownMenuItem(text = { Text("Add to queue") }, onClick = { menu = false; actions.addToQueue(listOf(song)) })
                DropdownMenuItem(text = { Text("Add to playlist") }, onClick = { menu = false; actions.addToPlaylist(listOf(song)) })
                DropdownMenuItem(text = { Text(if (favourite) "Remove favourite" else "Add favourite") }, onClick = { menu = false; actions.toggleFavourite(song) })
                DropdownMenuItem(text = { Text("Share") }, onClick = { menu = false; actions.share(listOf(song)) })
                DropdownMenuItem(text = { Text("Song info") }, onClick = { menu = false; actions.info(song) })
            }
        }
    }
}

@Composable
fun SongList(
    songs: List<Song>,
    favourites: Set<Long>,
    selection: Set<Long>,
    actions: SongActions,
    onToggleSelect: (Song) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(modifier.fillMaxSize()) {
        itemsIndexed(songs, key = { _, s -> s.id }) { index, song ->
            SongRow(
                song = song,
                favourite = song.id in favourites,
                selected = song.id in selection,
                onClick = { if (selection.isNotEmpty()) onToggleSelect(song) else actions.play(songs, index, false) },
                onLongClick = { onToggleSelect(song) },
                actions = actions,
            )
        }
    }
}

enum class GroupMode(val label: String) {
    Album("Albums"), Artist("Artists"), Genre("Genres"), Folder("Folders");

    fun key(song: Song): String = when (this) {
        Album -> song.album
        Artist -> song.artist
        Genre -> song.genre ?: "Unknown genre"
        Folder -> song.folder ?: "Unknown folder"
    }
}

private data class SongGroup(val name: String, val songs: List<Song>)

@Composable
fun GroupedSongs(
    songs: List<Song>,
    mode: GroupMode,
    grid: Boolean,
    favourites: Set<Long>,
    selection: Set<Long>,
    actions: SongActions,
    onToggleSelect: (Song) -> Unit,
) {
    val groups = remember(songs, mode) {
        songs.groupBy { mode.key(it) }.map { SongGroup(it.key, it.value) }.sortedBy { it.name.lowercase() }
    }
    var detail by remember(mode) { mutableStateOf<String?>(null) }
    val open = groups.firstOrNull { it.name == detail }
    BackHandler(enabled = open != null) { detail = null }

    if (open != null) {
        Column {
            Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { detail = null }) { Icon(Icons.Default.ArrowBack, "Back") }
                Text(open.name, Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.titleMedium)
                IconButton(onClick = { actions.play(open.songs, 0, false) }) { Icon(Icons.Default.PlayArrow, "Play") }
                IconButton(onClick = { actions.play(open.songs, 0, true) }) { Icon(Icons.Default.Shuffle, "Shuffle") }
            }
            SongList(open.songs, favourites, selection, actions, onToggleSelect)
        }
        return
    }

    if (grid) {
        LazyVerticalGrid(GridCells.Adaptive(150.dp), Modifier.fillMaxSize(), contentPadding = PaddingValues(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(groups, key = { it.name }) { g ->
                Column(Modifier.combinedClickableCompat { detail = g.name }) {
                    Artwork(g.songs.first().artworkUri, Modifier.fillMaxWidth().aspectRatio(1f))
                    Text(g.name, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 6.dp))
                    Text("${g.songs.size} songs", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    } else {
        LazyColumn(Modifier.fillMaxSize()) {
            items(groups.size) { i ->
                val g = groups[i]
                ListItem(
                    headlineContent = { Text(g.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    supportingContent = { Text("${g.songs.size} songs") },
                    leadingContent = { Artwork(g.songs.first().artworkUri, Modifier.size(48.dp), RoundedCornerShape(8.dp)) },
                    modifier = Modifier.combinedClickableCompat { detail = g.name },
                    colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.background),
                )
            }
        }
    }
}
