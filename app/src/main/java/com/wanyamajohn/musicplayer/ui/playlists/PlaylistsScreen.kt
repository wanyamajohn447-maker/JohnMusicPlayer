package com.wanyamajohn.musicplayer.ui.playlists

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wanyamajohn.musicplayer.ui.components.Artwork
import com.wanyamajohn.musicplayer.ui.components.ConfirmDialog
import com.wanyamajohn.musicplayer.ui.components.TextInputDialog
import com.wanyamajohn.musicplayer.ui.components.shareText

@Composable
fun PlaylistsScreen(vm: PlaylistViewModel = hiltViewModel()) {
    val context = LocalContext.current
    val playlists by vm.playlists.collectAsStateWithLifecycle()
    val selected by vm.selected.collectAsStateWithLifecycle()
    val rows by vm.rows.collectAsStateWithLifecycle()
    val message by vm.message.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }

    var creating by remember { mutableStateOf(false) }
    var renaming by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf<Long?>(null) }

    val exporter = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("audio/x-mpegurl")) { uri -> if (uri != null) vm.exportTo(uri) }
    val importer = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> if (uri != null) vm.importFrom(uri) }

    LaunchedEffect(message) { message?.let { snackbar.showSnackbar(it); vm.messageShown() } }
    BackHandler(enabled = selected != null) { vm.select(null) }

    val current = playlists.firstOrNull { it.id == selected }

    Box(Modifier.fillMaxSize()) {
        if (current == null) {
            Column(Modifier.fillMaxSize()) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("Playlists", Modifier.weight(1f), style = MaterialTheme.typography.titleLarge)
                    TextButton(onClick = { importer.launch(arrayOf("*/*")) }) { Icon(Icons.Default.FileDownload, null); Spacer(Modifier.width(4.dp)); Text("Import M3U") }
                }
                if (playlists.isEmpty()) Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text("No playlists yet. Tap + to create one.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else LazyColumn(Modifier.weight(1f)) {
                    items(playlists, key = { it.id }) { p ->
                        ListItem(
                            headlineContent = { Text(p.name) },
                            supportingContent = { Text("${p.songCount} songs") },
                            leadingContent = { Icon(Icons.Default.QueueMusic, null) },
                            trailingContent = { IconButton(onClick = { deleting = p.id }) { Icon(Icons.Default.Delete, "Delete") } },
                            modifier = Modifier.clickable { vm.select(p.id) },
                            colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.background),
                        )
                    }
                }
            }
            FloatingActionButton(onClick = { creating = true }, Modifier.align(Alignment.BottomEnd).padding(16.dp)) { Icon(Icons.Default.Add, "New playlist") }
        } else {
            Column(Modifier.fillMaxSize()) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { vm.select(null) }) { Icon(Icons.Default.ArrowBack, "Back") }
                    Text(current.name, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    IconButton(onClick = { renaming = true }) { Icon(Icons.Default.Edit, "Rename") }
                    IconButton(onClick = { exporter.launch(current.name + ".m3u") }) { Icon(Icons.Default.FileUpload, "Export M3U") }
                    IconButton(onClick = { shareText(context, vm.shareText(current.name)) }) { Icon(Icons.Default.Share, "Share") }
                }
                Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { vm.playAll(false) }) { Icon(Icons.Default.PlayArrow, null); Text("Play") }
                    OutlinedButton(onClick = { vm.playAll(true) }) { Icon(Icons.Default.Shuffle, null); Spacer(Modifier.width(4.dp)); Text("Shuffle") }
                }
                if (rows.isEmpty()) Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text("Empty. Add songs from the Library (⋮ → Add to playlist).", color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else LazyColumn(Modifier.weight(1f)) {
                    itemsIndexed(rows, key = { _, r -> r.entryId }) { i, r ->
                        val s = r.song
                        ListItem(
                            headlineContent = { Text(s?.title ?: "Unavailable file", maxLines = 1, overflow = TextOverflow.Ellipsis,
                                color = if (s == null) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface) },
                            supportingContent = { Text(s?.artist ?: "This song was moved, deleted or is no longer accessible", maxLines = 1) },
                            leadingContent = { Artwork(s?.artworkUri, Modifier.size(44.dp), RoundedCornerShape(8.dp)) },
                            trailingContent = {
                                Row {
                                    IconButton(onClick = { vm.move(i, -1) }, enabled = i > 0) { Icon(Icons.Default.KeyboardArrowUp, "Move up") }
                                    IconButton(onClick = { vm.move(i, 1) }, enabled = i < rows.lastIndex) { Icon(Icons.Default.KeyboardArrowDown, "Move down") }
                                    IconButton(onClick = { vm.remove(r.entryId) }) { Icon(Icons.Default.Close, "Remove") }
                                }
                            },
                            modifier = Modifier.clickable { vm.playFrom(r) },
                            colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.background),
                        )
                    }
                }
            }
        }
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter))
    }

    if (creating) TextInputDialog("New playlist", confirmLabel = "Create", onConfirm = { vm.create(it); creating = false }, onDismiss = { creating = false })
    if (renaming && current != null) TextInputDialog("Rename playlist", current.name, "Rename", onConfirm = { vm.rename(current.id, it); renaming = false }, onDismiss = { renaming = false })
    deleting?.let { id ->
        ConfirmDialog("Delete playlist?", "Your songs are not deleted, only the playlist.", "Delete",
            onConfirm = { vm.delete(id); deleting = null }, onDismiss = { deleting = null })
    }
}
