package com.wanyamajohn.musicplayer.ui.library

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wanyamajohn.musicplayer.core.audioPermission
import com.wanyamajohn.musicplayer.domain.model.Song
import com.wanyamajohn.musicplayer.ui.components.MediaPermissionGate
import com.wanyamajohn.musicplayer.ui.components.shareSongs

enum class SortOrder(val label: String, val cmp: Comparator<Song>) {
    Title("Title", compareBy<Song, String>(String.CASE_INSENSITIVE_ORDER) { it.title }),
    Artist("Artist", compareBy<Song, String>(String.CASE_INSENSITIVE_ORDER) { it.artist }),
    Newest("Recently added", compareByDescending<Song> { it.dateAddedSec }),
    Duration("Duration", compareByDescending<Song> { it.durationMs }),
    Size("File size", compareByDescending<Song> { it.sizeBytes }),
}

enum class LibraryTab(val label: String) {
    Songs("Songs"), Albums("Albums"), Artists("Artists"), Genres("Genres"), Folders("Folders"),
    Favourites("Favourites"), Recent("Recently played"), Added("Recently added"), Most("Most played"),
}

@Composable
fun LibraryScreen(modifier: Modifier = Modifier, vm: LibraryViewModel = hiltViewModel()) {
    MediaPermissionGate(
        permission = audioPermission(),
        title = "Find your music",
        rationale = "JOHN MUSIC PLAYER needs permission to read audio files on your device so it can list and play them. Your files never leave your phone.",
        onGranted = vm::scanIfNeeded,
    ) { LibraryContent(vm, modifier) }
}

@Composable
private fun LibraryContent(vm: LibraryViewModel, modifier: Modifier) {
    val context = LocalContext.current
    val songs by vm.songs.collectAsStateWithLifecycle()
    val scanning by vm.scanning.collectAsStateWithLifecycle()
    val scanned by vm.scanned.collectAsStateWithLifecycle()
    val error by vm.error.collectAsStateWithLifecycle()
    val favourites by vm.favouriteIds.collectAsStateWithLifecycle()
    val recentIds by vm.recentIds.collectAsStateWithLifecycle()
    val mostIds by vm.mostPlayedIds.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()
    val actions = rememberSongActions(vm)

    var tab by rememberSaveable { mutableStateOf(LibraryTab.Songs) }
    var query by rememberSaveable { mutableStateOf("") }
    var sort by rememberSaveable { mutableStateOf(SortOrder.Title) }
    var sortMenu by remember { mutableStateOf(false) }
    var selection by remember { mutableStateOf(setOf<Long>()) }
    val toggle: (Song) -> Unit = { s -> selection = if (s.id in selection) selection - s.id else selection + s.id }

    val byId = remember(songs) { songs.associateBy { it.id } }
    val visible = remember(songs, tab, query, sort, recentIds, mostIds, favourites, byId) {
        val base = when (tab) {
            LibraryTab.Favourites -> songs.filter { it.id in favourites }
            LibraryTab.Recent -> recentIds.mapNotNull { byId[it] }
            LibraryTab.Most -> mostIds.mapNotNull { byId[it] }
            LibraryTab.Added -> songs.sortedByDescending { it.dateAddedSec }.take(100)
            else -> songs
        }
        val q = query.trim()
        val filtered = if (q.isEmpty()) base else base.filter {
            it.title.contains(q, true) || it.artist.contains(q, true) || it.album.contains(q, true)
        }
        if (tab == LibraryTab.Recent || tab == LibraryTab.Most || tab == LibraryTab.Added) filtered else filtered.sortedWith(sort.cmp)
    }

    Column(modifier.fillMaxSize()) {
        if (selection.isNotEmpty()) {
            val picked = songs.filter { it.id in selection }
            Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { selection = emptySet() }) { Icon(Icons.Default.Close, "Clear selection") }
                Text("${selection.size} selected", Modifier.weight(1f))
                IconButton(onClick = { actions.addToQueue(picked); selection = emptySet() }) { Icon(Icons.Default.QueueMusic, "Add to queue") }
                IconButton(onClick = { actions.addToPlaylist(picked); selection = emptySet() }) { Icon(Icons.Default.PlaylistAdd, "Add to playlist") }
                IconButton(onClick = { shareSongs(context, picked) }) { Icon(Icons.Default.Share, "Share") }
            }
        } else {
            Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = query, onValueChange = { query = it },
                    modifier = Modifier.weight(1f), singleLine = true,
                    placeholder = { Text("Search songs, artists, albums") },
                    leadingIcon = { Icon(Icons.Default.Search, null) },
                    trailingIcon = { if (query.isNotEmpty()) IconButton(onClick = { query = "" }) { Icon(Icons.Default.Close, "Clear") } },
                )
                Box {
                    IconButton(onClick = { sortMenu = true }) { Icon(Icons.Default.Sort, "Sort") }
                    DropdownMenu(sortMenu, { sortMenu = false }) {
                        SortOrder.entries.forEach { o ->
                            DropdownMenuItem(text = { Text(o.label + if (o == sort) "  ✓" else "") }, onClick = { sort = o; sortMenu = false })
                        }
                    }
                }
                IconButton(onClick = { vm.setGrid(!settings.grid) }) {
                    Icon(if (settings.grid) Icons.Default.ViewList else Icons.Default.GridView, "Toggle layout")
                }
                IconButton(onClick = vm::scan) { Icon(Icons.Default.Refresh, "Rescan") }
            }
        }

        ScrollableTabRow(selectedTabIndex = tab.ordinal, edgePadding = 8.dp, containerColor = MaterialTheme.colorScheme.background) {
            LibraryTab.entries.forEach { t ->
                Tab(selected = t == tab, onClick = { tab = t; selection = emptySet() }, text = { Text(t.label) })
            }
        }

        Box(Modifier.weight(1f).fillMaxWidth()) {
            when {
                error != null && songs.isEmpty() -> Column(Modifier.align(Alignment.Center).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(error ?: "")
                    Spacer(Modifier.height(12.dp))
                    Button(onClick = vm::scan) { Text("Try again") }
                }
                (scanning || !scanned) && songs.isEmpty() -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                songs.isEmpty() -> Text("No music found on this device.", Modifier.align(Alignment.Center))
                visible.isEmpty() -> Text(
                    if (query.isNotBlank()) "No matches for \"$query\"" else "Nothing here yet.",
                    Modifier.align(Alignment.Center), color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                else -> when (tab) {
                    LibraryTab.Albums -> GroupedSongs(visible, GroupMode.Album, settings.grid, favourites, selection, actions, toggle)
                    LibraryTab.Artists -> GroupedSongs(visible, GroupMode.Artist, settings.grid, favourites, selection, actions, toggle)
                    LibraryTab.Genres -> GroupedSongs(visible, GroupMode.Genre, settings.grid, favourites, selection, actions, toggle)
                    LibraryTab.Folders -> GroupedSongs(visible, GroupMode.Folder, false, favourites, selection, actions, toggle)
                    else -> SongList(visible, favourites, selection, actions, toggle)
                }
            }
            if (scanning && songs.isNotEmpty()) LinearProgressIndicator(Modifier.fillMaxWidth().align(Alignment.TopCenter))
        }
    }
}
