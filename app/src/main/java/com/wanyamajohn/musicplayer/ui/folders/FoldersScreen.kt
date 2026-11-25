package com.wanyamajohn.musicplayer.ui.folders

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.documentfile.provider.DocumentFile
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wanyamajohn.musicplayer.core.formatSize
import com.wanyamajohn.musicplayer.ui.components.InfoDialog
import com.wanyamajohn.musicplayer.ui.library.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.DateFormat
import java.util.Date

data class FileEntry(val name: String, val uri: Uri, val isDir: Boolean, val size: Long, val modified: Long, val mime: String?) {
    val isAudio get() = mime?.startsWith("audio/") == true || name.substringAfterLast('.', "").lowercase() in AUDIO_EXT
    val isVideo get() = mime?.startsWith("video/") == true || name.substringAfterLast('.', "").lowercase() in VIDEO_EXT
}
private val AUDIO_EXT = setOf("mp3", "aac", "m4a", "flac", "wav", "ogg", "opus", "wma")
private val VIDEO_EXT = setOf("mp4", "mkv", "webm", "3gp", "avi", "mov")

private enum class FileSort(val label: String) { Name("Name"), Size("Size"), Date("Date") }

@Composable
fun FoldersScreen(onPlayVideo: (String) -> Unit, libraryVm: LibraryViewModel = hiltViewModel(), vm: FoldersViewModel = hiltViewModel()) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    Column(Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = tab, containerColor = MaterialTheme.colorScheme.background) {
            Tab(tab == 0, { tab = 0 }, text = { Text("Library folders") })
            Tab(tab == 1, { tab = 1 }, text = { Text("Browse files") })
        }
        if (tab == 0) LibraryFolders(libraryVm) else Browser(vm, onPlayVideo)
    }
}

@Composable
private fun LibraryFolders(vm: LibraryViewModel) {
    val songs by vm.songs.collectAsStateWithLifecycle()
    val favourites by vm.favouriteIds.collectAsStateWithLifecycle()
    val actions = rememberSongActions(vm)
    LaunchedEffect(Unit) { vm.scanIfNeeded() }
    var selection by remember { mutableStateOf(setOf<Long>()) }
    if (songs.isEmpty()) Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Open the Library tab once and allow access to see folders here.", color = MaterialTheme.colorScheme.onSurfaceVariant)
    } else GroupedSongs(songs, GroupMode.Folder, false, favourites, selection, actions) { s ->
        selection = if (s.id in selection) selection - s.id else selection + s.id
    }
}

@Composable
private fun Browser(vm: FoldersViewModel, onPlayVideo: (String) -> Unit) {
    val context = LocalContext.current
    val trees by vm.trees.collectAsStateWithLifecycle()
    var stack by remember { mutableStateOf(listOf<Uri>()) }   // directory URIs, last = current
    var query by remember { mutableStateOf("") }
    var sort by remember { mutableStateOf(FileSort.Name) }
    var sortMenu by remember { mutableStateOf(false) }
    var refreshKey by remember { mutableIntStateOf(0) }
    var infoFor by remember { mutableStateOf<FileEntry?>(null) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) {
            // Persist read access so the folder (including SD cards) is still readable next time. Android's storage rules are respected.
            runCatching { context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            vm.addTree(uri.toString())
        }
    }
    BackHandler(enabled = stack.isNotEmpty()) { stack = stack.dropLast(1) }

    val current = stack.lastOrNull()
    if (current == null) {
        Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { picker.launch(null) }) { Icon(Icons.Default.Add, null); Spacer(Modifier.width(6.dp)); Text("Add a folder or SD card location") }
            Text("Only folders you choose are browsed. Nothing is scanned in the background.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            LazyColumn {
                items(trees.toList()) { t ->
                    val name = remember(t) { runCatching { DocumentFile.fromTreeUri(context, Uri.parse(t))?.name }.getOrNull() ?: "Folder" }
                    ListItem(
                        headlineContent = { Text(name) },
                        leadingContent = { Icon(Icons.Default.FolderOpen, null) },
                        trailingContent = { IconButton(onClick = { vm.removeTree(t) }) { Icon(Icons.Default.Close, "Remove") } },
                        modifier = Modifier.clickable { stack = listOf(Uri.parse(t)) },
                        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.background),
                    )
                }
            }
        }
        return
    }

    val entries by produceState<List<FileEntry>?>(null, current, refreshKey) {
        value = null
        value = withContext(Dispatchers.IO) {
            runCatching {
                DocumentFile.fromTreeUri(context, current)?.listFiles().orEmpty().map {
                    FileEntry(it.name ?: "?", it.uri, it.isDirectory, it.length(), it.lastModified(), it.type)
                }.filter { it.isDir || it.isAudio || it.isVideo }
            }.getOrDefault(emptyList())
        }
    }

    val shown = remember(entries, query, sort) {
        val q = query.trim()
        (entries.orEmpty().filter { q.isEmpty() || it.name.contains(q, true) })
            .sortedWith(
                compareByDescending<FileEntry> { it.isDir }.then(
                    when (sort) {
                        FileSort.Name -> compareBy<FileEntry> { it.name.lowercase() }
                        FileSort.Size -> compareByDescending<FileEntry> { it.size }
                        FileSort.Date -> compareByDescending<FileEntry> { it.modified }
                    },
                ),
            )
    }
    val audioFiles = remember(shown) { shown.filter { it.isAudio } }

    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { stack = stack.dropLast(1) }) { Icon(Icons.Default.ArrowBack, "Up") }
            OutlinedTextField(query, { query = it }, Modifier.weight(1f), singleLine = true, placeholder = { Text("Search this folder") },
                leadingIcon = { Icon(Icons.Default.Search, null) })
            Box {
                IconButton(onClick = { sortMenu = true }) { Icon(Icons.Default.Sort, "Sort") }
                DropdownMenu(sortMenu, { sortMenu = false }) {
                    FileSort.entries.forEach { s -> DropdownMenuItem(text = { Text(s.label + if (s == sort) "  ✓" else "") }, onClick = { sort = s; sortMenu = false }) }
                }
            }
            IconButton(onClick = { refreshKey++ }) { Icon(Icons.Default.Refresh, "Refresh") }
        }
        when {
            entries == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            shown.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("No audio or video files here.") }
            else -> LazyColumn(Modifier.fillMaxSize()) {
                items(shown, key = { it.uri.toString() }) { f ->
                    ListItem(
                        headlineContent = { Text(f.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        supportingContent = { if (!f.isDir) Text(formatSize(f.size)) },
                        leadingContent = { Icon(when { f.isDir -> Icons.Default.Folder; f.isVideo -> Icons.Default.VideoLibrary; else -> Icons.Default.MusicNote }, null) },
                        trailingContent = { if (!f.isDir) IconButton(onClick = { infoFor = f }) { Icon(Icons.Default.Info, "File info") } },
                        modifier = Modifier.clickable {
                            when {
                                f.isDir -> stack = stack + f.uri
                                f.isVideo -> onPlayVideo(f.uri.toString())
                                else -> vm.playFiles(audioFiles, audioFiles.indexOfFirst { it.uri == f.uri }.coerceAtLeast(0))
                            }
                        },
                        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.background),
                    )
                }
            }
        }
    }

    infoFor?.let { f ->
        InfoDialog(f.name, listOf(
            "Type" to (f.mime ?: "unknown"), "Size" to formatSize(f.size),
            "Modified" to if (f.modified > 0) DateFormat.getDateTimeInstance().format(Date(f.modified)) else "-",
            "Location" to f.uri.toString(),
        )) { infoFor = null }
    }
}
