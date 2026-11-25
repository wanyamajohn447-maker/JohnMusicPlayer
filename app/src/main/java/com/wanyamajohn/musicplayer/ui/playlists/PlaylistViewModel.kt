package com.wanyamajohn.musicplayer.ui.playlists

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wanyamajohn.musicplayer.core.M3u
import com.wanyamajohn.musicplayer.core.queryDisplayName
import com.wanyamajohn.musicplayer.data.db.PlaylistSummary
import com.wanyamajohn.musicplayer.data.repository.UserDataRepository
import com.wanyamajohn.musicplayer.domain.model.Song
import com.wanyamajohn.musicplayer.domain.repository.MediaRepository
import com.wanyamajohn.musicplayer.playback.PlayerConnection
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Collections
import javax.inject.Inject

data class PlaylistRow(val entryId: Long, val songId: Long, val song: Song?)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class PlaylistViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val userData: UserDataRepository,
    private val media: MediaRepository,
    private val player: PlayerConnection,
) : ViewModel() {
    val playlists: StateFlow<List<PlaylistSummary>> = userData.playlists()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    private val selectedId = MutableStateFlow<Long?>(null)
    val selected: StateFlow<Long?> = selectedId.asStateFlow()
    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    val rows: StateFlow<List<PlaylistRow>> = selectedId.flatMapLatest { id ->
        if (id == null) flowOf(emptyList())
        else combine(userData.entries(id), media.songs) { entries, songs ->
            val byId = songs.associateBy { it.id }
            entries.map { PlaylistRow(it.id, it.songId, byId[it.songId]) }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun select(id: Long?) { selectedId.value = id }
    fun messageShown() { _message.value = null }

    fun create(name: String) { viewModelScope.launch { userData.createPlaylist(name) } }
    fun rename(id: Long, name: String) { viewModelScope.launch { userData.renamePlaylist(id, name) } }
    fun delete(id: Long) {
        if (selectedId.value == id) selectedId.value = null
        viewModelScope.launch { userData.deletePlaylist(id) }
    }

    private fun playable(): List<Song> = rows.value.mapNotNull { it.song }

    fun playAll(shuffle: Boolean) {
        val songs = playable()
        if (songs.isEmpty()) _message.value = "No playable songs in this playlist." else player.playSongs(songs, 0, shuffle)
    }

    fun playFrom(row: PlaylistRow) {
        val songs = playable()
        val idx = songs.indexOf(row.song)
        if (idx >= 0) player.playSongs(songs, idx) else _message.value = "This file is missing or no longer accessible."
    }

    fun remove(entryId: Long) { viewModelScope.launch { userData.removeEntry(entryId) } }

    fun move(index: Int, delta: Int) {
        val id = selectedId.value ?: return
        val ids = rows.value.map { it.entryId }.toMutableList()
        val to = index + delta
        if (index !in ids.indices || to !in ids.indices) return
        Collections.swap(ids, index, to)
        viewModelScope.launch { userData.reorder(id, ids) }
    }

    fun shareText(name: String): String =
        buildString {
            appendLine(name)
            playable().forEachIndexed { i, s -> appendLine("${i + 1}. ${s.artist} - ${s.title}") }
        }

    fun exportTo(uri: Uri) {
        val songs = playable()
        viewModelScope.launch(Dispatchers.IO) {
            val ok = runCatching {
                context.contentResolver.openOutputStream(uri, "wt")!!.bufferedWriter().use { it.write(M3u.build(songs)) }
            }.isSuccess
            _message.value = if (ok) "Exported ${songs.size} songs." else "Could not export the playlist."
        }
    }

    fun importFrom(uri: Uri) {
        viewModelScope.launch {
            val songs = media.songs.value
            val result = withContext(Dispatchers.IO) {
                val text = runCatching {
                    context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                }.getOrNull() ?: return@withContext null
                val entries = M3u.parse(text)
                val byName = HashMap<String, Long>(songs.size * 2)
                for (s in songs) byName.putIfAbsent(s.displayName.lowercase(), s.id)
                val matchedIds = entries.mapNotNull { byName[M3u.baseName(it.path).lowercase()] }
                val name = (queryDisplayName(context, uri) ?: "Imported playlist").substringBeforeLast('.')
                Triple(name, entries.size, matchedIds)
            }
            if (result == null) { _message.value = "Could not read that file."; return@launch }
            val (name, total, matchedIds) = result
            if (total == 0) { _message.value = "No songs found in that playlist file."; return@launch }
            val id = userData.createPlaylist(name)
            userData.addToPlaylist(id, matchedIds)
            _message.value = "Imported ${matchedIds.size} of $total songs" +
                if (songs.isEmpty()) " (open the Library tab first so songs can be matched)." else "."
        }
    }
}
