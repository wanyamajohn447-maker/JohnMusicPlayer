package com.wanyamajohn.musicplayer.playback

import android.os.SystemClock
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

sealed interface SleepState {
    data class Timed(val remainingMs: Long) : SleepState
    data object EndOfSong : SleepState
    data object EndOfPlaylist : SleepState
}

/** Lives outside any screen; operates directly on the service's player so it is lifecycle-safe. */
@Singleton
class SleepTimerManager @Inject constructor() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var player: ExoPlayer? = null
    private var job: Job? = null
    private val _state = MutableStateFlow<SleepState?>(null)
    val state: StateFlow<SleepState?> = _state.asStateFlow()

    private val listener = object : Player.Listener {
        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            if (_state.value == SleepState.EndOfPlaylist) armPlaylistEnd()
        }
        override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
            val s = _state.value
            if (!playWhenReady && reason == Player.PLAY_WHEN_READY_CHANGE_REASON_END_OF_MEDIA_ITEM &&
                (s == SleepState.EndOfSong || s == SleepState.EndOfPlaylist)
            ) {
                player?.pauseAtEndOfMediaItems = false
                _state.value = null
            }
        }
    }

    fun attach(p: ExoPlayer) { player = p; p.addListener(listener) }

    fun detach() {
        job?.cancel()
        player?.removeListener(listener)
        player?.volume = 1f
        player = null
        _state.value = null
    }

    fun startTimed(minutes: Int) {
        cancel()
        val end = SystemClock.elapsedRealtime() + minutes * 60_000L
        val fadeMs = 10_000L
        job = scope.launch {
            while (true) {
                val rem = end - SystemClock.elapsedRealtime()
                if (rem <= 0) break
                _state.value = SleepState.Timed(rem)
                // Gradual volume reduction over the final seconds.
                if (rem <= fadeMs) player?.volume = (rem / fadeMs.toFloat()).coerceIn(0f, 1f)
                delay(if (rem <= fadeMs) 250 else 1000)
            }
            player?.pause()
            player?.volume = 1f
            _state.value = null
        }
    }

    fun startEndOfSong() {
        cancel()
        player?.pauseAtEndOfMediaItems = true
        _state.value = SleepState.EndOfSong
    }

    fun startEndOfPlaylist() {
        cancel()
        _state.value = SleepState.EndOfPlaylist
        armPlaylistEnd()
    }

    private fun armPlaylistEnd() {
        val p = player ?: return
        val last = p.currentTimeline.getLastWindowIndex(p.shuffleModeEnabled)
        p.pauseAtEndOfMediaItems = p.currentMediaItemIndex == last
    }

    fun cancel() {
        job?.cancel()
        player?.pauseAtEndOfMediaItems = false
        player?.volume = 1f
        _state.value = null
    }
}
