package com.wanyamajohn.musicplayer.ui.video

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wanyamajohn.musicplayer.core.formatDuration
import com.wanyamajohn.musicplayer.core.formatSize
import com.wanyamajohn.musicplayer.core.videoPermission
import com.wanyamajohn.musicplayer.ui.components.MediaPermissionGate
import com.wanyamajohn.musicplayer.ui.components.VideoThumb

@Composable
fun VideoLibraryScreen(onPlay: (String) -> Unit, vm: VideoViewModel = hiltViewModel()) {
    MediaPermissionGate(
        permission = videoPermission(),
        title = "Find your videos",
        rationale = "Allow access to video files on your device so they can be listed and played. Nothing is uploaded.",
        onGranted = vm::scanIfNeeded,
    ) {
        val videos by vm.videos.collectAsStateWithLifecycle()
        val scanning by vm.scanning.collectAsStateWithLifecycle()
        val scanned by vm.scanned.collectAsStateWithLifecycle()
        val error by vm.error.collectAsStateWithLifecycle()
        val grouped = remember(videos) { videos.groupBy { it.folder }.toSortedMap(String.CASE_INSENSITIVE_ORDER) }

        Column(Modifier.fillMaxSize()) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("${videos.size} videos", Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                IconButton(onClick = vm::scan) { Icon(Icons.Default.Refresh, "Rescan") }
            }
            Box(Modifier.weight(1f).fillMaxWidth()) {
                when {
                    error != null && videos.isEmpty() -> Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(error ?: ""); Button(onClick = vm::scan) { Text("Try again") }
                    }
                    (scanning || !scanned) && videos.isEmpty() -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                    videos.isEmpty() -> Text("No videos found on this device.", Modifier.align(Alignment.Center))
                    else -> LazyColumn(Modifier.fillMaxSize()) {
                        grouped.forEach { (folder, list) ->
                            item(key = "h:$folder") {
                                Text(folder, Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                                    style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                            }
                            items(list.size, key = { list[it].id }) { i ->
                                val v = list[i]
                                Row(Modifier.fillMaxWidth().clickable { onPlay(v.contentUri) }.padding(horizontal = 16.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically) {
                                    VideoThumb(v.contentUri, Modifier.size(width = 96.dp, height = 56.dp))
                                    Spacer(Modifier.width(12.dp))
                                    Column {
                                        Text(v.title, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                        Text("${formatDuration(v.durationMs)} • ${formatSize(v.sizeBytes)}" +
                                            if (v.width > 0) " • ${v.width}x${v.height}" else "",
                                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
