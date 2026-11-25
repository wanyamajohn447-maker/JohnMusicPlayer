package com.wanyamajohn.musicplayer.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wanyamajohn.musicplayer.data.prefs.AppSettings
import com.wanyamajohn.musicplayer.data.prefs.PreferencesRepository
import com.wanyamajohn.musicplayer.playback.PlayerConnection
import com.wanyamajohn.musicplayer.playback.PlayerUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    prefs: PreferencesRepository,
    private val player: PlayerConnection,
) : ViewModel() {
    val settings: StateFlow<AppSettings> = prefs.settings.stateIn(viewModelScope, SharingStarted.Eagerly, AppSettings())
    val playerState: StateFlow<PlayerUiState> = player.state
    fun togglePlay() = player.togglePlay()
    fun next() = player.next()
}
