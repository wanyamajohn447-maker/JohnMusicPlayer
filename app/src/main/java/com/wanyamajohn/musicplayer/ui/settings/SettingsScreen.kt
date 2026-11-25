package com.wanyamajohn.musicplayer.ui.settings

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.border
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wanyamajohn.musicplayer.core.formatDuration
import com.wanyamajohn.musicplayer.core.formatSize
import com.wanyamajohn.musicplayer.ui.components.ConfirmDialog
import com.wanyamajohn.musicplayer.ui.home.HOME_SECTIONS
import com.wanyamajohn.musicplayer.ui.screens.APP_VERSION
import com.wanyamajohn.musicplayer.ui.visualizer.VISUALIZER_STYLES

private val ACCENTS = listOf(0xFFFF7A00, 0xFFE53935, 0xFFD81B60, 0xFF8E24AA, 0xFF1E88E5, 0xFF00ACC1, 0xFF43A047, 0xFFFDD835).map { it.toInt() }

@Composable
fun SettingsScreen(onOpenAbout: () -> Unit, onOpenInsights: () -> Unit, vm: SettingsViewModel = hiltViewModel()) {
    val context = LocalContext.current
    val s by vm.settings.collectAsStateWithLifecycle()
    var confirmClear by remember { mutableStateOf(false) }
    val lyricsPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) {
            runCatching { context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            vm.setLyricsTree(uri.toString())
        }
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Header("Appearance")
        Text("Accent colour")
        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(ACCENTS) { c ->
                Box(Modifier.size(36.dp).clip(CircleShape).background(Color(c))
                    .border(if (c == s.accent) 3.dp else 0.dp, Color.White, CircleShape).clickable { vm.setAccent(c) })
            }
        }
        ToggleRow("Circular album artwork in player", s.artCircle, vm::setArtCircle)
        ToggleRow("Grid layout for albums & artists", s.grid, vm::setGrid)

        Header("Home sections")
        HOME_SECTIONS.forEach { (id, label) -> ToggleRow(label, id !in s.hiddenSections) { vm.setSectionVisible(id, it) } }

        Header("Lyrics")
        Text("Text size: ${s.lyricsSize} sp")
        Slider(s.lyricsSize.toFloat(), { vm.setLyricsSize(it.toInt()) }, valueRange = 14f..30f)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedButton(onClick = { lyricsPicker.launch(null) }) { Text(if (s.lyricsFolder == null) "Choose lyrics folder" else "Change lyrics folder") }
            if (s.lyricsFolder != null) TextButton(onClick = { vm.setLyricsTree(null) }) { Text("Clear") }
        }
        Text("Lyrics are matched by file name (Song.mp3 → Song.lrc). Nothing is fetched online.",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

        Header("Visualizer")
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(VISUALIZER_STYLES) { v -> FilterChip(selected = v == s.visStyle, onClick = { vm.setVisStyle(v) }, label = { Text(v) }) }
        }

        Header("Library")
        OutlinedButton(onClick = onOpenInsights) { Text("Library insights & duplicates") }
        OutlinedButton(onClick = { confirmClear = true }) { Text("Clear playback history") }

        Header("About")
        OutlinedButton(onClick = onOpenAbout) { Text("About & privacy") }
        Text("JOHN MUSIC PLAYER $APP_VERSION • Wanyama John Technologies", style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }

    if (confirmClear) ConfirmDialog("Clear history?", "Recently played and most played lists will be reset. Your music and playlists are not affected.", "Clear",
        onConfirm = { vm.clearHistory(); confirmClear = false }, onDismiss = { confirmClear = false })
}

@Composable
private fun Header(t: String) {
    Spacer(Modifier.height(8.dp))
    Text(t, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f))
        Switch(checked, onChange)
    }
}

@Composable
fun InsightsScreen(vm: SettingsViewModel = hiltViewModel()) {
    val ui by vm.insights.collectAsStateWithLifecycle()
    val u = ui
    if (u == null || !u.libraryScanned) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(if (u == null) "Loading…" else "Open the Library tab once to scan your music.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }
    val st = u.stats
    androidx.compose.foundation.lazy.LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        item { Text("Library statistics", style = MaterialTheme.typography.titleMedium) }
        item { Text("Songs: ${st.songCount}  •  Albums: ${st.albumCount}  •  Artists: ${st.artistCount}") }
        item { Text("Total length: ${formatDuration(st.totalDurationMs)}  •  Total size: ${formatSize(st.totalSizeBytes)}") }
        item { Text("Formats: " + st.formats.entries.joinToString { "${it.key} (${it.value})" }) }
        item { Text("Favourites: ${u.favouriteCount}  •  Plays recorded: ${u.totalPlays}") }
        item {
            Text(if (u.missingCount == 0) "No missing media in playlists or favourites." else "${u.missingCount} song(s) in playlists/favourites are no longer available on this device.",
                color = if (u.missingCount == 0) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.primary)
        }
        item { Spacer(Modifier.height(8.dp)); Text("Possible duplicates (${u.duplicates.size})", style = MaterialTheme.typography.titleMedium) }
        item { Text("Read-only: the app never deletes or edits your files.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        items(u.duplicates.size) { i ->
            val g = u.duplicates[i]
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp)) {
                    Text("${g.first().title} — ${g.first().artist}", style = MaterialTheme.typography.bodyMedium)
                    g.forEach { Text("${it.format} • ${formatSize(it.sizeBytes)} • ${it.folder ?: ""}${it.displayName}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                }
            }
        }
    }
}
