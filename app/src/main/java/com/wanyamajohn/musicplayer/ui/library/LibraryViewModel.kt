package com.wanyamajohn.musicplayer.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wanyamajohn.musicplayer.data.prefs.AppSettings
import com.wanyamajohn.musicplayer.data.prefs.PrefKeys
import com.wanyamajohn.musicplayer.data.prefs.PreferencesRepository
import com.wanyamajohn.musicplayer.data.repository.UserDataRepository
import com.wanyamajohn.musicplayer.data.db.PlaylistSummary
import com.wanyamajohn.musicplayer.domain.model.Song
import com.wanyamajohn.musicplayer.domain.repository.MediaRepository
import com.wanyamajohn.musicplayer.playback.PlayerConnection
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val media: MediaRepository,
    private val userData: UserDataRepository,
    private val player: PlayerConnection,
    private val prefs: PreferencesRepository,
) : ViewModel() {
    val songs: StateFlow<List<Song>> = media.songs
    val scanned: StateFlow<Boolean> = media.scanned
    val favouriteIds: StateFlow<Set<Long>> = userData.favouriteIds.map { it.toSet() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())
    val recentIds: StateFlow<List<Long>> = userData.recentIds
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val mostPlayedIds: StateFlow<List<Long>> = userData.mostPlayedIds
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val playlists: StateFlow<List<PlaylistSummary>> = userData.playlists()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val settings: StateFlow<AppSettings> = prefs.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppSettings())

    private val _scanning = MutableStateFlow(false)
    val scanning: StateFlow<Boolean> = _scanning.asStateFlow()
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun scanIfNeeded() { if (!media.scanned.value) scan() }

    fun scan() {
        if (_scanning.value) return
        viewModelScope.launch {
            _scanning.value = true
            _error.value = null
            media.refreshSongs().onFailure {
                _error.value = if (it is SecurityException) "Storage access was removed. Please grant permission again."
                else "Could not read your music library. Please try again."
            }
            _scanning.value = false
        }
    }

    fun setGrid(grid: Boolean) = prefs.setAsync(PrefKeys.GRID, grid)

    fun play(list: List<Song>, index: Int, shuffle: Boolean) = player.playSongs(list, index, shuffle)
    fun addToQueue(list: List<Song>) = player.addToQueue(list)
    fun playNext(song: Song) = player.playNext(song)

    fun toggleFavourite(song: Song) {
        val isFav = song.id in favouriteIds.value
        viewModelScope.launch { userData.setFavourite(song.id, !isFav) }
    }

    fun addToPlaylist(playlistId: Long, songs: List<Song>) {
        viewModelScope.launch { userData.addToPlaylist(playlistId, songs.map { it.id }) }
    }

    fun createPlaylistWith(name: String, songs: List<Song>) {
        viewModelScope.launch {
            val id = userData.createPlaylist(name)
            userData.addToPlaylist(id, songs.map { it.id })
        }
    }
}
