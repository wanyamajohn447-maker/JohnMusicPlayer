package com.wanyamajohn.musicplayer.ui.player

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wanyamajohn.musicplayer.data.db.LyricsEntity
import com.wanyamajohn.musicplayer.data.prefs.AppSettings
import com.wanyamajohn.musicplayer.data.prefs.PreferencesRepository
import com.wanyamajohn.musicplayer.data.repository.LyricsRepository
import com.wanyamajohn.musicplayer.data.repository.UserDataRepository
import com.wanyamajohn.musicplayer.domain.model.Song
import com.wanyamajohn.musicplayer.domain.repository.MediaRepository
import com.wanyamajohn.musicplayer.playback.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class NowPlayingViewModel @Inject constructor(
    private val player: PlayerConnection,
    media: MediaRepository,
    private val userData: UserDataRepository,
    private val lyricsRepo: LyricsRepository,
    private val sleep: SleepTimerManager,
    output: AudioOutputMonitor,
    prefs: PreferencesRepository,
) : ViewModel() {
    val state: StateFlow<PlayerUiState> = player.state
    val outputLabel: StateFlow<String> = output.label
    val sleepState: StateFlow<SleepState?> = sleep.state
    val settings: StateFlow<AppSettings> = prefs.settings.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppSettings())
    val favouriteIds: StateFlow<Set<Long>> = userData.favouriteIds.map { it.toSet() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    val song: StateFlow<Song?> = combine(player.state.map { it.songId }.distinctUntilChanged(), media.songs) { id, songs ->
        songs.firstOrNull { it.id == id }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val lyrics: StateFlow<LyricsEntity?> = player.state.map { it.songId }.distinctUntilChanged()
        .flatMapLatest { id -> if (id == null) flowOf(null) else lyricsRepo.observe(id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    init {
        // Pick up lyrics from the user's lyrics folder the first time a song plays.
        viewModelScope.launch {
            song.filterNotNull().distinctUntilChangedBy { it.id }.collect { s ->
                if (lyricsRepo.get(s.id) == null) lyricsRepo.autoLoad(s)
            }
        }
    }

    fun togglePlay() = player.togglePlay()
    fun next() = player.next()
    fun previous() = player.previous()
    fun seekTo(ms: Long) = player.seekTo(ms)
    fun toggleShuffle() = player.toggleShuffle()
    fun cycleRepeat() = player.cycleRepeat()
    fun setSpeed(s: Float) = player.setSpeed(s)
    fun skipTo(i: Int) = player.skipTo(i)
    fun removeFromQueue(i: Int) = player.removeFromQueue(i)
    fun moveInQueue(from: Int, to: Int) = player.moveInQueue(from, to)
    fun clearError() = player.clearError()

    fun toggleFavourite() {
        val id = state.value.songId ?: return
        val fav = id in favouriteIds.value
        viewModelScope.launch { userData.setFavourite(id, !fav) }
    }

    fun loadLyricsFrom(uri: Uri) {
        val id = state.value.songId ?: return
        viewModelScope.launch { lyricsRepo.readText(uri)?.takeIf { it.isNotBlank() }?.let { lyricsRepo.save(id, it) } }
    }

    fun removeLyrics() {
        val id = state.value.songId ?: return
        viewModelScope.launch { lyricsRepo.delete(id) }
    }

    fun sleepMinutes(m: Int) = sleep.startTimed(m)
    fun sleepEndOfSong() = sleep.startEndOfSong()
    fun sleepEndOfPlaylist() = sleep.startEndOfPlaylist()
    fun sleepCancel() = sleep.cancel()
}
