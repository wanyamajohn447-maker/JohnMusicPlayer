package com.wanyamajohn.musicplayer.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wanyamajohn.musicplayer.core.LibraryAnalyzer
import com.wanyamajohn.musicplayer.core.LibraryStats
import com.wanyamajohn.musicplayer.data.prefs.AppSettings
import com.wanyamajohn.musicplayer.data.prefs.PrefKeys
import com.wanyamajohn.musicplayer.data.prefs.PreferencesRepository
import com.wanyamajohn.musicplayer.data.repository.UserDataRepository
import com.wanyamajohn.musicplayer.domain.model.Song
import com.wanyamajohn.musicplayer.domain.repository.MediaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class InsightsUi(
    val stats: LibraryStats,
    val duplicates: List<List<Song>>,
    val missingCount: Int,
    val favouriteCount: Int,
    val totalPlays: Int,
    val libraryScanned: Boolean,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val prefs: PreferencesRepository,
    private val userData: UserDataRepository,
    media: MediaRepository,
) : ViewModel() {
    val settings: StateFlow<AppSettings> = prefs.settings.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppSettings())

    val insights: StateFlow<InsightsUi?> = combine(
        media.songs, media.scanned, userData.favouriteIds, userData.playlistSongIds, userData.totalPlays,
    ) { songs, scanned, favs, playlistIds, plays ->
        val known = songs.map { it.id }.toSet()
        // "Missing" = referenced by a playlist/favourite but no longer in the media library.
        val missing = if (scanned) ((playlistIds + favs).toSet() - known).size else 0
        InsightsUi(LibraryAnalyzer.stats(songs), LibraryAnalyzer.findDuplicates(songs), missing, favs.size, plays, scanned)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun setAccent(argb: Int) = prefs.setAsync(PrefKeys.ACCENT, argb)
    fun setGrid(v: Boolean) = prefs.setAsync(PrefKeys.GRID, v)
    fun setArtCircle(v: Boolean) = prefs.setAsync(PrefKeys.ART_CIRCLE, v)
    fun setLyricsSize(v: Int) = prefs.setAsync(PrefKeys.LYRICS_SIZE, v)
    fun setVisStyle(v: String) = prefs.setAsync(PrefKeys.VIS_STYLE, v)
    fun setLyricsTree(uri: String?) { viewModelScope.launch { if (uri == null) prefs.remove(PrefKeys.LYRICS_TREE) else prefs.set(PrefKeys.LYRICS_TREE, uri) } }
    fun setSectionVisible(id: String, visible: Boolean) {
        val hidden = settings.value.hiddenSections
        prefs.setAsync(PrefKeys.HIDDEN_SECTIONS, if (visible) hidden - id else hidden + id)
    }
    fun clearHistory() { viewModelScope.launch { userData.clearHistory() } }
}
