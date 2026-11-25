package com.wanyamajohn.musicplayer.domain.model

data class Song(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val albumId: Long,
    val durationMs: Long,
    val sizeBytes: Long,
    val mimeType: String?,
    val dateAddedSec: Long,
    /** Stable content:// URI; no copies of user files are made. */
    val contentUri: String,
    val displayName: String = "",
    val genre: String? = null,
    val folder: String? = null,
) {
    val format: String
        get() = displayName.substringAfterLast('.', "").uppercase()
            .ifEmpty { mimeType?.substringAfter('/')?.uppercase() ?: "?" }
    val artworkUri: String get() = "content://media/external/audio/albumart/$albumId"
}
