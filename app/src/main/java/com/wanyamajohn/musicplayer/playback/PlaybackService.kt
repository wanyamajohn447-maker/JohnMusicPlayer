package com.wanyamajohn.musicplayer.playback

import android.content.Intent
import android.net.Uri
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.wanyamajohn.musicplayer.data.db.PlaybackQueueEntity
import com.wanyamajohn.musicplayer.data.db.QueueDao
import com.wanyamajohn.musicplayer.data.prefs.PrefKeys
import com.wanyamajohn.musicplayer.data.prefs.PreferencesRepository
import com.wanyamajohn.musicplayer.data.repository.UserDataRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import javax.inject.Inject

/**
 * Owns the ExoPlayer and MediaSession so playback is independent of any Activity.
 * Handles audio focus, becoming-noisy (headset unplug), notification / lock-screen / Bluetooth controls (via Media3).
 */
@AndroidEntryPoint
class PlaybackService : MediaSessionService() {
    @Inject lateinit var effects: AudioEffectsManager
    @Inject lateinit var sleepTimer: SleepTimerManager
    @Inject lateinit var userData: UserDataRepository
    @Inject lateinit var queueDao: QueueDao
    @Inject lateinit var prefs: PreferencesRepository

    private var session: MediaSession? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val ioScope = CoroutineScope(SupervisorJob() + Dispatchers.IO) // survives onDestroy so the final save completes
    private var saveJob: Job? = null
    private var lastRecordedId: String? = null

    override fun onCreate() {
        super.onCreate()
        val player = ExoPlayer.Builder(this)
            .setAudioAttributes(
                AudioAttributes.Builder().setUsage(C.USAGE_MEDIA).setContentType(C.AUDIO_CONTENT_TYPE_MUSIC).build(),
                /* handleAudioFocus = */ true,
            )
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_LOCAL)
            .build()
        player.addListener(PlayerListener(player))
        session = MediaSession.Builder(this, player).setCallback(Callback()).build()
        sleepTimer.attach(player)
        effects.attach(player.audioSessionId)
        restoreQueue(player)
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = session

    override fun onTaskRemoved(rootIntent: Intent?) {
        val p = session?.player
        if (p == null || !p.playWhenReady || p.mediaItemCount == 0) stopSelf()
    }

    override fun onDestroy() {
        session?.player?.let { persistNow(it) }
        sleepTimer.detach()
        effects.release()
        session?.run { player.release(); release() }
        session = null
        scope.cancel()
        super.onDestroy()
    }

    private inner class Callback : MediaSession.Callback {
        // Controllers send items without a playable URI; restore it from the request metadata.
        override fun onAddMediaItems(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo,
            mediaItems: MutableList<MediaItem>,
        ): ListenableFuture<MutableList<MediaItem>> {
            val resolved = mediaItems.map { item ->
                val uri = item.requestMetadata.mediaUri ?: item.localConfiguration?.uri
                if (uri != null) item.buildUpon().setUri(uri).build() else item
            }.toMutableList()
            return Futures.immediateFuture(resolved)
        }
    }

    private inner class PlayerListener(private val player: ExoPlayer) : Player.Listener {
        override fun onAudioSessionIdChanged(audioSessionId: Int) { effects.attach(audioSessionId) }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            if (isPlaying) {
                val id = player.currentMediaItem?.mediaId
                if (id != null && id != lastRecordedId) {
                    lastRecordedId = id
                    id.toLongOrNull()?.let { songId -> ioScope.launch { runCatching { userData.recordPlay(songId) } } }
                }
            } else scheduleSave()
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) { scheduleSave() }
        override fun onTimelineChanged(timeline: Timeline, reason: Int) { scheduleSave() }

        override fun onPlayerError(error: PlaybackException) {
            // One bad file must never stop the session: move on to the next item if there is one.
            if (player.hasNextMediaItem()) {
                player.seekToNextMediaItem()
                player.prepare()
                player.play()
            }
        }
    }

    private fun scheduleSave() {
        saveJob?.cancel()
        saveJob = scope.launch {
            delay(1000)
            session?.player?.let { persistNow(it) }
        }
    }

    private fun persistNow(player: Player) {
        val items = (0 until player.mediaItemCount).map { i ->
            val it = player.getMediaItemAt(i)
            PlaybackQueueEntity(
                position = i,
                mediaId = it.mediaId,
                uri = (it.requestMetadata.mediaUri ?: it.localConfiguration?.uri)?.toString() ?: "",
                title = it.mediaMetadata.title?.toString().orEmpty(),
                artist = it.mediaMetadata.artist?.toString().orEmpty(),
                album = it.mediaMetadata.albumTitle?.toString().orEmpty(),
                artworkUri = it.mediaMetadata.artworkUri?.toString(),
            )
        }.filter { it.uri.isNotEmpty() }
        val index = player.currentMediaItemIndex
        val pos = player.currentPosition
        ioScope.launch {
            runCatching {
                queueDao.replace(items)
                prefs.set(PrefKeys.QUEUE_INDEX, index)
                prefs.set(PrefKeys.QUEUE_POS, pos)
            }
        }
    }

    private fun restoreQueue(player: ExoPlayer) {
        scope.launch {
            val entries = withContext(Dispatchers.IO) { runCatching { queueDao.all() }.getOrDefault(emptyList()) }
            if (entries.isEmpty() || player.mediaItemCount > 0) return@launch
            val items = entries.map { e ->
                val uri = Uri.parse(e.uri)
                MediaItem.Builder()
                    .setMediaId(e.mediaId)
                    .setUri(uri)
                    .setRequestMetadata(MediaItem.RequestMetadata.Builder().setMediaUri(uri).build())
                    .setMediaMetadata(
                        MediaMetadata.Builder()
                            .setTitle(e.title).setArtist(e.artist).setAlbumTitle(e.album)
                            .setArtworkUri(e.artworkUri?.let { Uri.parse(it) })
                            .build(),
                    )
                    .build()
            }
            val idx = prefs.get(PrefKeys.QUEUE_INDEX, 0).coerceIn(0, items.lastIndex)
            val pos = prefs.get(PrefKeys.QUEUE_POS, 0L).coerceAtLeast(0)
            if (player.mediaItemCount == 0) {
                player.setMediaItems(items, idx, pos)
                player.prepare() // stays paused until the user presses play
            }
        }
    }
}
