package com.wanyamajohn.musicplayer.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wanyamajohn.musicplayer.core.audioPermission
import com.wanyamajohn.musicplayer.domain.model.Song
import com.wanyamajohn.musicplayer.ui.components.Artwork
import com.wanyamajohn.musicplayer.ui.components.LogoMark
import com.wanyamajohn.musicplayer.ui.components.MediaPermissionGate
import com.wanyamajohn.musicplayer.ui.library.LibraryViewModel

val HOME_SECTIONS = listOf(
    "recent_played" to "Recently played", "recent_added" to "Recently added", "favourites" to "Favourite tracks",
    "albums" to "Albums", "artists" to "Artists", "playlists" to "Playlists",
)

@Composable
fun HomeScreen(onOpenLibrary: () -> Unit, onOpenPlaylists: () -> Unit, vm: LibraryViewModel = hiltViewModel()) {
    MediaPermissionGate(
        permission = audioPermission(),
        title = "Welcome to JOHN MUSIC PLAYER",
        rationale = "Allow access to the audio files on your device so your music can show up here. Your files never leave your phone.",
        onGranted = vm::scanIfNeeded,
    ) { HomeContent(onOpenLibrary, onOpenPlaylists, vm) }
}

@Composable
private fun HomeContent(onOpenLibrary: () -> Unit, onOpenPlaylists: () -> Unit, vm: LibraryViewModel) {
    val songs by vm.songs.collectAsStateWithLifecycle()
    val favourites by vm.favouriteIds.collectAsStateWithLifecycle()
    val recentIds by vm.recentIds.collectAsStateWithLifecycle()
    val playlists by vm.playlists.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()
    val byId = remember(songs) { songs.associateBy { it.id } }
    val recent = remember(recentIds, byId) { recentIds.mapNotNull { byId[it] }.take(20) }
    val added = remember(songs) { songs.sortedByDescending { it.dateAddedSec }.take(20) }
    val favs = remember(songs, favourites) { songs.filter { it.id in favourites }.take(20) }
    val albums = remember(songs) { songs.groupBy { it.album }.values.sortedBy { it.first().album.lowercase() }.take(30) }
    val artists = remember(songs) { songs.groupBy { it.artist }.values.sortedBy { it.first().artist.lowercase() }.take(30) }
    fun visible(id: String) = id !in settings.hiddenSections

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        item {
            Row(Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                LogoMark(Modifier.size(width = 56.dp, height = 40.dp))
                Column {
                    Text("YOUR MUSIC. YOUR CONTROL.", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                    Text("${songs.size} songs on this device", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        item {
            Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onOpenLibrary) { Icon(Icons.Default.LibraryMusic, null); Spacer(Modifier.width(6.dp)); Text("All songs") }
                OutlinedButton(onClick = { vm.play(songs, 0, true) }, enabled = songs.isNotEmpty()) {
                    Icon(Icons.Default.Shuffle, null); Spacer(Modifier.width(6.dp)); Text("Shuffle all")
                }
            }
        }
        if (visible("recent_played") && recent.isNotEmpty()) item { SongRowSection("Recently played", recent) { i -> vm.play(recent, i, false) } }
        if (visible("recent_added") && added.isNotEmpty()) item { SongRowSection("Recently added", added) { i -> vm.play(added, i, false) } }
        if (visible("favourites") && favs.isNotEmpty()) item { SongRowSection("Favourite tracks", favs) { i -> vm.play(favs, i, false) } }
        if (visible("albums") && albums.isNotEmpty()) item {
            Section("Albums") { items(albums) { g -> Card(g.first().artworkUri, g.first().album, g.first().artist) { vm.play(g, 0, false) } } }
        }
        if (visible("artists") && artists.isNotEmpty()) item {
            Section("Artists") { items(artists) { g -> Card(g.first().artworkUri, g.first().artist, "${g.size} songs") { vm.play(g, 0, false) } } }
        }
        if (visible("playlists") && playlists.isNotEmpty()) item {
            Section("Playlists") {
                items(playlists) { p ->
                    Column(Modifier.width(120.dp).clickable(onClick = onOpenPlaylists)) {
                        Surface(Modifier.size(120.dp), shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surfaceVariant) {
                            Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.QueueMusic, null, Modifier.size(40.dp)) }
                        }
                        Text(p.name, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 6.dp))
                        Text("${p.songCount} songs", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun Section(title: String, content: androidx.compose.foundation.lazy.LazyListScope.() -> Unit) {
    Column {
        Text(title, Modifier.padding(horizontal = 16.dp, vertical = 8.dp), style = MaterialTheme.typography.titleMedium)
        LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), content = content)
    }
}

@Composable
private fun SongRowSection(title: String, songs: List<Song>, onPlay: (Int) -> Unit) {
    Section(title) {
        items(songs.size) { i -> Card(songs[i].artworkUri, songs[i].title, songs[i].artist) { onPlay(i) } }
    }
}

@Composable
private fun Card(art: String, title: String, subtitle: String, onClick: () -> Unit) {
    Column(Modifier.width(120.dp).clickable(onClick = onClick)) {
        Artwork(art, Modifier.size(120.dp))
        Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 6.dp))
        Text(subtitle, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
