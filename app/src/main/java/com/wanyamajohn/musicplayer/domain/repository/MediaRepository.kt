package com.wanyamajohn.musicplayer.domain.repository

import com.wanyamajohn.musicplayer.domain.model.Song
import com.wanyamajohn.musicplayer.domain.model.Video
import kotlinx.coroutines.flow.StateFlow

interface MediaRepository {
    val songs: StateFlow<List<Song>>
    val videos: StateFlow<List<Video>>
    /** True once at least one audio scan has completed successfully. */
    val scanned: StateFlow<Boolean>
    val videosScanned: StateFlow<Boolean>
    suspend fun refreshSongs(): Result<Unit>
    suspend fun refreshVideos(): Result<Unit>
}
