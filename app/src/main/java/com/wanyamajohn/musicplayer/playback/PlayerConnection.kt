package com.wanyamajohn.musicplayer.playback

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.wanyamajohn.musicplayer.domain.model.Song
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

data class QueueItem(val mediaId: String, val title: String, val artist: String, val artworkUri: String?)

data class PlayerUiState(
    val mediaId: String? = null,
    val title: String = "",
    val artist: String = "",
    val album: String = "",
    val artworkUri: String? = null,
    val isPlaying: Boolean = false,
    val positionMs: Long = 0,
    val durationMs: Long = 0,
    val shuffle: Boolean = false,
    val repeatMode: Int = Player.REPEAT_MODE_OFF,
    val queue: List<QueueItem> = emptyList(),
    val currentIndex: Int = -1,
    val speed: Float = 1f,
    val error: String? = null,
) {
    val hasMedia: Boolean get() = mediaId != null
    /** MediaStore id when the current item is a library song; null for browsed files. */
    val songId: Long? get() = mediaId?.toLongOrNull()
}

fun Song.toMediaItem(): MediaItem {
    val uri = Uri.parse(contentUri)
    return MediaItem.Builder()
        .setMediaId(id.toString())
        .setUri(uri)
        .setRequestMetadata(MediaItem.RequestMetadata.Builder().setMediaUri(uri).build())
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle(title).setArtist(artist).setAlbumTitle(album)
                .setArtworkUri(Uri.parse(artworkUri))
                .build(),
        )
        .build()
}

/** App-wide connection to PlaybackService through a MediaController. UI never touches the player directly. */
@Singleton
class PlayerConnection @Inject constructor(@ApplicationContext private val context: Context) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val controllerReady = CompletableDeferred<MediaController>()
    private val _state = MutableStateFlow(PlayerUiState())
    val state: StateFlow<PlayerUiState> = _state.asStateFlow()
    private var queueCache: List<QueueItem> = emptyList()
    private var ticker: Job? = null

    private val listener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) {
            refresh(player, rebuildQueue = events.contains(Player.EVENT_TIMELINE_CHANGED))
        }
        override fun onPlayerError(error: PlaybackException) {
            _state.update { it.copy(error = describe(error)) }
        }
    }

    init {
        val token = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        val future = MediaController.Builder(context, token).buildAsync()
        future.addListener({
            try {
                val c = future.get()
                c.addListener(listener)
                controllerReady.complete(c)
                refresh(c, rebuildQueue = true)
            } catch (e: Exception) {
                controllerReady.completeExceptionally(e)
            }
        }, ContextCompat.getMainExecutor(context))
    }

    private fun describe(e: PlaybackException): String = when (e.errorCode) {
        PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND -> "This file is missing or was moved. Skipping it."
        PlaybackException.ERROR_CODE_IO_NO_PERMISSION -> "Storage access was removed. Please grant permission again."
        PlaybackException.ERROR_CODE_DECODING_FAILED,
        PlaybackException.ERROR_CODE_DECODER_INIT_FAILED,
        PlaybackException.ERROR_CODE_PARSING_CONTAINER_MALFORMED,
        PlaybackException.ERROR_CODE_PARSING_CONTAINER_UNSUPPORTED ->
            "This file is damaged or its format isn't supported on this device."
        else -> "Playback problem. Trying the next item."
    }

    private fun refresh(p: Player, rebuildQueue: Boolean) {
        if (rebuildQueue) {
            queueCache = (0 until p.mediaItemCount).map { i ->
                val item = p.getMediaItemAt(i)
                QueueItem(
                    item.mediaId,
                    item.mediaMetadata.title?.toString().orEmpty(),
                    item.mediaMetadata.artist?.toString().orEmpty(),
                    item.mediaMetadata.artworkUri?.toString(),
                )
            }
        }
        val item = p.currentMediaItem
        val md = item?.mediaMetadata
        _state.update {
            it.copy(
                mediaId = item?.mediaId?.takeIf { id -> id.isNotEmpty() },
                title = md?.title?.toString().orEmpty(),
                artist = md?.artist?.toString().orEmpty(),
                album = md?.albumTitle?.toString().orEmpty(),
                artworkUri = md?.artworkUri?.toString(),
                isPlaying = p.isPlaying,
                positionMs = p.currentPosition.coerceAtLeast(0),
                durationMs = p.duration.takeIf { d -> d > 0 } ?: 0L,
                shuffle = p.shuffleModeEnabled,
                repeatMode = p.repeatMode,
                queue = queueCache,
                currentIndex = if (item == null) -1 else p.currentMediaItemIndex,
                speed = p.playbackParameters.speed,
            )
        }
        if (p.isPlaying && ticker?.isActive != true) {
            ticker = scope.launch {
                while (isActive) {
                    _state.update { it.copy(positionMs = p.currentPosition.coerceAtLeast(0)) }
                    delay(500)
                }
            }
        } else if (!p.isPlaying) {
            ticker?.cancel()
        }
    }

    private fun run(block: (MediaController) -> Unit) {
        scope.launch { runCatching { block(controllerReady.await()) } }
    }

    fun playSongs(songs: List<Song>, startIndex: Int = 0, shuffle: Boolean = false) {
        if (songs.isEmpty()) return
        playItems(songs.map { it.toMediaItem() }, startIndex.coerceIn(0, songs.lastIndex), shuffle)
    }

    fun playItems(items: List<MediaItem>, startIndex: Int = 0, shuffle: Boolean = false) {
        if (items.isEmpty()) return
        run { c ->
            c.shuffleModeEnabled = shuffle
            c.setMediaItems(items, startIndex.coerceIn(0, items.lastIndex), 0L)
            c.prepare()
            c.play()
        }
    }

    fun togglePlay() = run { c ->
        if (c.isPlaying) c.pause() else {
            if (c.playbackState == Player.STATE_IDLE) c.prepare()
            c.play()
        }
    }

    fun pause() = run { it.pause() }
    fun next() = run { it.seekToNextMediaItem() }
    fun previous() = run { it.seekToPrevious() }
    fun seekTo(ms: Long) = run { it.seekTo(ms) }
    fun toggleShuffle() = run { it.shuffleModeEnabled = !it.shuffleModeEnabled }
    fun cycleRepeat() = run {
        it.repeatMode = when (it.repeatMode) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
            else -> Player.REPEAT_MODE_OFF
        }
    }
    fun setSpeed(speed: Float) = run { it.setPlaybackSpeed(speed) }

    fun addToQueue(songs: List<Song>) {
        if (songs.isEmpty()) return
        run { c ->
            val empty = c.mediaItemCount == 0
            c.addMediaItems(songs.map { it.toMediaItem() })
            if (empty) { c.prepare(); c.play() }
        }
    }

    fun playNext(song: Song) = run { c ->
        if (c.mediaItemCount == 0) {
            c.setMediaItem(song.toMediaItem()); c.prepare(); c.play()
        } else c.addMediaItem(c.currentMediaItemIndex + 1, song.toMediaItem())
    }

    fun removeFromQueue(index: Int) = run { if (index in 0 until it.mediaItemCount) it.removeMediaItem(index) }
    fun moveInQueue(from: Int, to: Int) = run { c ->
        if (from in 0 until c.mediaItemCount && to in 0 until c.mediaItemCount) c.moveMediaItem(from, to)
    }
    fun skipTo(index: Int) = run { it.seekToDefaultPosition(index); it.play() }
    fun clearError() { _state.update { it.copy(error = null) } }
}
