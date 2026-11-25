package com.wanyamajohn.musicplayer.domain.model

data class Video(
    val id: Long,
    val title: String,
    val durationMs: Long,
    val sizeBytes: Long,
    val contentUri: String,
    val folder: String,
    val width: Int,
    val height: Int,
    val dateAddedSec: Long,
)
