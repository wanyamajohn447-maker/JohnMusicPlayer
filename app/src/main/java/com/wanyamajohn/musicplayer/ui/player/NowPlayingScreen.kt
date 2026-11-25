package com.wanyamajohn.musicplayer.ui.player

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.Player
import com.wanyamajohn.musicplayer.core.LrcParser
import com.wanyamajohn.musicplayer.core.formatDuration
import com.wanyamajohn.musicplayer.playback.SleepState
import com.wanyamajohn.musicplayer.ui.components.Artwork
import com.wanyamajohn.musicplayer.ui.components.SongInfoDialog
import com.wanyamajohn.musicplayer.ui.components.shareSongs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NowPlayingScreen(
    onBack: () -> Unit,
    onOpenEqualizer: () -> Unit,
    onOpenVisualizer: () -> Unit,
    vm: NowPlayingViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val state by vm.state.collectAsStateWithLifecycle()
    val song by vm.song.collectAsStateWithLifecycle()
    val favourites by vm.favouriteIds.collectAsStateWithLifecycle()
    val output by vm.outputLabel.collectAsStateWithLifecycle()
    val sleepState by vm.sleepState.collectAsStateWithLifecycle()
    val lyrics by vm.lyrics.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()

    var showLyrics by remember { mutableStateOf(false) }
    var showQueue by remember { mutableStateOf(false) }
    var showSleep by remember { mutableStateOf(false) }
    var showSpeed by remember { mutableStateOf(false) }
    var showInfo by remember { mutableStateOf(false) }
    var menu by remember { mutableStateOf(false) }
    var drag by remember { mutableStateOf<Float?>(null) }
    val snackbar = remember { SnackbarHostState() }

    val lyricsPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> if (uri != null) vm.loadLyricsFrom(uri) }

    LaunchedEffect(state.error) {
        state.error?.let { snackbar.showSnackbar(it); vm.clearError() }
    }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.Default.KeyboardArrowDown, "Close") }
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("NOW PLAYING", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Output: $output", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                }
                Box {
                    IconButton(onClick = { menu = true }) { Icon(Icons.Default.MoreVert, "More options") }
                    DropdownMenu(menu, { menu = false }) {
                        DropdownMenuItem(text = { Text("Sleep timer") }, onClick = { menu = false; showSleep = true })
                        DropdownMenuItem(text = { Text("Playback speed (${state.speed}x)") }, onClick = { menu = false; showSpeed = true })
                        DropdownMenuItem(text = { Text("Load lyrics file…") }, onClick = { menu = false; lyricsPicker.launch(arrayOf("*/*")) })
                        if (lyrics != null) DropdownMenuItem(text = { Text("Remove lyrics") }, onClick = { menu = false; vm.removeLyrics() })
                        if (song != null) {
                            DropdownMenuItem(text = { Text("Share") }, onClick = { menu = false; shareSongs(context, listOf(song!!)) })
                            DropdownMenuItem(text = { Text("Song info") }, onClick = { menu = false; showInfo = true })
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
            if (showLyrics) {
                LyricsPanel(lyrics?.text, state.positionMs, settings.lyricsSize, Modifier.fillMaxWidth().height(340.dp), onSeek = vm::seekTo)
            } else {
                Crossfade(state.artworkUri, label = "artwork", modifier = Modifier.fillMaxWidth()) { art ->
                    Artwork(
                        art, Modifier.fillMaxWidth().aspectRatio(1f),
                        if (settings.artCircle) CircleShape else RoundedCornerShape(20.dp),
                    )
                }
            }

            Spacer(Modifier.height(20.dp))
            Text(state.title.ifEmpty { "Nothing playing" }, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold,
                maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(listOf(state.artist, state.album).filter { it.isNotBlank() }.joinToString(" • "),
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1, overflow = TextOverflow.Ellipsis)

            Spacer(Modifier.height(12.dp))
            val max = state.durationMs.coerceAtLeast(1).toFloat()
            Slider(
                value = (drag ?: state.positionMs.toFloat()).coerceIn(0f, max),
                onValueChange = { drag = it },
                onValueChangeFinished = { drag?.let { vm.seekTo(it.toLong()) }; drag = null },
                valueRange = 0f..max,
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(formatDuration((drag ?: state.positionMs.toFloat()).toLong()), style = MaterialTheme.typography.labelSmall)
                Text(formatDuration(state.durationMs), style = MaterialTheme.typography.labelSmall)
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = vm::toggleShuffle) {
                    Icon(Icons.Default.Shuffle, "Shuffle", tint = if (state.shuffle) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = vm::previous, modifier = Modifier.size(56.dp)) { Icon(Icons.Default.SkipPrevious, "Previous", Modifier.size(36.dp)) }
                FilledIconButton(onClick = vm::togglePlay, modifier = Modifier.size(72.dp)) {
                    Icon(if (state.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, if (state.isPlaying) "Pause" else "Play", Modifier.size(40.dp))
                }
                IconButton(onClick = vm::next, modifier = Modifier.size(56.dp)) { Icon(Icons.Default.SkipNext, "Next", Modifier.size(36.dp)) }
                IconButton(onClick = vm::cycleRepeat) {
                    val on = state.repeatMode != Player.REPEAT_MODE_OFF
                    Icon(if (state.repeatMode == Player.REPEAT_MODE_ONE) Icons.Default.RepeatOne else Icons.Default.Repeat, "Repeat",
                        tint = if (on) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                val isFav = state.songId?.let { it in favourites } == true
                IconButton(onClick = vm::toggleFavourite, enabled = state.songId != null) {
                    Icon(if (isFav) Icons.Default.Favorite else Icons.Default.FavoriteBorder, "Favourite",
                        tint = if (isFav) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
                }
                IconButton(onClick = { showLyrics = !showLyrics }) {
                    Icon(Icons.Default.Description, "Lyrics", tint = if (showLyrics) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
                }
                IconButton(onClick = onOpenEqualizer) { Icon(Icons.Default.Equalizer, "Equalizer") }
                IconButton(onClick = onOpenVisualizer) { Icon(Icons.Default.GraphicEq, "Visualizer") }
                IconButton(onClick = { showSleep = true }) {
                    Icon(Icons.Default.Timer, "Sleep timer", tint = if (sleepState != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
                }
                IconButton(onClick = { showQueue = true }) { Icon(Icons.Default.QueueMusic, "Queue") }
            }
            sleepState?.let {
                Text(
                    when (it) {
                        is SleepState.Timed -> "Sleep timer: ${formatDuration(it.remainingMs)} left"
                        SleepState.EndOfSong -> "Sleep timer: stops after this song"
                        SleepState.EndOfPlaylist -> "Sleep timer: stops at end of playlist"
                    },
                    Modifier.fillMaxWidth(), textAlign = TextAlign.Center, style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Spacer(Modifier.height(16.dp))
        }
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).navigationBarsPadding())
    }

    if (showQueue) {
        ModalBottomSheet(onDismissRequest = { showQueue = false }) {
            Text("Queue (${state.queue.size})", Modifier.padding(horizontal = 24.dp, vertical = 8.dp), style = MaterialTheme.typography.titleMedium)
            LazyColumn(Modifier.heightIn(max = 480.dp)) {
                itemsIndexed(state.queue) { i, item ->
                    val current = i == state.currentIndex
                    ListItem(
                        headlineContent = { Text(item.title.ifEmpty { "Unknown" }, maxLines = 1, overflow = TextOverflow.Ellipsis,
                            color = if (current) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface) },
                        supportingContent = { Text(item.artist, maxLines = 1) },
                        leadingContent = { Artwork(item.artworkUri, Modifier.size(40.dp), RoundedCornerShape(6.dp)) },
                        trailingContent = {
                            Row {
                                IconButton(onClick = { vm.moveInQueue(i, i - 1) }, enabled = i > 0) { Icon(Icons.Default.KeyboardArrowUp, "Move up") }
                                IconButton(onClick = { vm.moveInQueue(i, i + 1) }, enabled = i < state.queue.lastIndex) { Icon(Icons.Default.KeyboardArrowDown, "Move down") }
                                IconButton(onClick = { vm.removeFromQueue(i) }) { Icon(Icons.Default.Close, "Remove") }
                            }
                        },
                        modifier = Modifier.clickable { vm.skipTo(i) },
                        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surface),
                    )
                }
            }
        }
    }

    if (showSleep) {
        AlertDialog(
            onDismissRequest = { showSleep = false },
            title = { Text("Sleep timer") },
            text = {
                Column {
                    listOf(15, 30, 45, 60).forEach { m ->
                        TextButton(onClick = { vm.sleepMinutes(m); showSleep = false }) { Text("Stop after $m minutes") }
                    }
                    TextButton(onClick = { vm.sleepEndOfSong(); showSleep = false }) { Text("Stop after current song") }
                    TextButton(onClick = { vm.sleepEndOfPlaylist(); showSleep = false }) { Text("Stop after the playlist") }
                    if (sleepState != null) TextButton(onClick = { vm.sleepCancel(); showSleep = false }) { Text("Cancel timer") }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { showSleep = false }) { Text("Close") } },
        )
    }

    if (showSpeed) {
        AlertDialog(
            onDismissRequest = { showSpeed = false },
            title = { Text("Playback speed") },
            text = {
                Column {
                    listOf(0.5f, 0.75f, 1f, 1.25f, 1.5f, 2f).forEach { s ->
                        TextButton(onClick = { vm.setSpeed(s); showSpeed = false }) { Text("${s}x" + if (s == state.speed) "  ✓" else "") }
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { showSpeed = false }) { Text("Close") } },
        )
    }

    if (showInfo) song?.let { SongInfoDialog(it) { showInfo = false } }
}

@Composable
fun LyricsPanel(text: String?, positionMs: Long, textSizeSp: Int, modifier: Modifier = Modifier, onSeek: (Long) -> Unit) {
    if (text.isNullOrBlank()) {
        Box(modifier, contentAlignment = Alignment.Center) {
            Text(
                "No lyrics for this song.\nUse ⋮ → Load lyrics file, or choose a lyrics folder in Settings.",
                textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        return
    }
    val lines = remember(text) { LrcParser.parse(text) }
    if (lines.isEmpty()) {
        Column(modifier.verticalScroll(rememberScrollState())) { Text(text, fontSize = textSizeSp.sp, lineHeight = (textSizeSp * 1.5).sp) }
        return
    }
    val current = LrcParser.currentIndex(lines, positionMs)
    val listState = rememberLazyListState()
    LaunchedEffect(current) { if (current >= 0) listState.animateScrollToItem((current - 2).coerceAtLeast(0)) }
    LazyColumn(modifier, state = listState, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        itemsIndexed(lines) { i, l ->
            Text(
                l.text.ifEmpty { "♪" },
                modifier = Modifier.fillMaxWidth().clickable { onSeek(l.timeMs) },
                fontSize = textSizeSp.sp,
                fontWeight = if (i == current) FontWeight.Bold else FontWeight.Normal,
                color = if (i == current) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
