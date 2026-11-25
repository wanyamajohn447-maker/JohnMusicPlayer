package com.wanyamajohn.musicplayer.ui.video

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wanyamajohn.musicplayer.data.prefs.PrefKeys
import com.wanyamajohn.musicplayer.data.prefs.PreferencesRepository
import com.wanyamajohn.musicplayer.domain.model.Video
import com.wanyamajohn.musicplayer.domain.repository.MediaRepository
import com.wanyamajohn.musicplayer.playback.PlayerConnection
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class VideoViewModel @Inject constructor(
    private val media: MediaRepository,
    private val prefs: PreferencesRepository,
    private val player: PlayerConnection,
) : ViewModel() {
    val videos: StateFlow<List<Video>> = media.videos
    val scanned: StateFlow<Boolean> = media.videosScanned
    private val _scanning = MutableStateFlow(false)
    val scanning: StateFlow<Boolean> = _scanning.asStateFlow()
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun scanIfNeeded() { if (!media.videosScanned.value) scan() }

    fun scan() {
        if (_scanning.value) return
        viewModelScope.launch {
            _scanning.value = true; _error.value = null
            media.refreshVideos().onFailure { _error.value = "Could not read your videos. Please try again." }
            _scanning.value = false
        }
    }

    /** Music and video must not play at once. */
    fun pauseMusic() = player.pause()

    suspend fun savedPosition(uri: String): Long = prefs.get(PrefKeys.videoPosition(uri), 0L)

    fun savePosition(uri: String, positionMs: Long, durationMs: Long) {
        val nearEnd = durationMs > 0 && durationMs - positionMs < 5000
        prefs.setAsync(PrefKeys.videoPosition(uri), if (nearEnd) 0L else positionMs)
    }
}
