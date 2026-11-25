package com.wanyamajohn.musicplayer.data.repository

import com.wanyamajohn.musicplayer.data.media.MediaStoreScanner
import com.wanyamajohn.musicplayer.domain.model.Song
import com.wanyamajohn.musicplayer.domain.model.Video
import com.wanyamajohn.musicplayer.domain.repository.MediaRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

class MediaRepositoryImpl @Inject constructor(
    private val scanner: MediaStoreScanner,
) : MediaRepository {
    private val _songs = MutableStateFlow<List<Song>>(emptyList())
    private val _videos = MutableStateFlow<List<Video>>(emptyList())
    private val _scanned = MutableStateFlow(false)
    private val _videosScanned = MutableStateFlow(false)
    override val songs: StateFlow<List<Song>> = _songs.asStateFlow()
    override val videos: StateFlow<List<Video>> = _videos.asStateFlow()
    override val scanned: StateFlow<Boolean> = _scanned.asStateFlow()
    override val videosScanned: StateFlow<Boolean> = _videosScanned.asStateFlow()

    override suspend fun refreshSongs(): Result<Unit> = guarded {
        _songs.value = scanner.scanAudio()
        _scanned.value = true
    }

    override suspend fun refreshVideos(): Result<Unit> = guarded {
        _videos.value = scanner.scanVideo()
        _videosScanned.value = true
    }

    private suspend fun guarded(block: suspend () -> Unit): Result<Unit> = try {
        block(); Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(e)
    }
}
