package com.wanyamajohn.musicplayer.core

import com.wanyamajohn.musicplayer.domain.model.Song

data class LibraryStats(
    val songCount: Int,
    val albumCount: Int,
    val artistCount: Int,
    val totalDurationMs: Long,
    val totalSizeBytes: Long,
    val formats: Map<String, Int>,
)

/** Read-only analysis. Never modifies or deletes user files. */
object LibraryAnalyzer {
    fun stats(songs: List<Song>) = LibraryStats(
        songCount = songs.size,
        albumCount = songs.map { it.album }.distinct().size,
        artistCount = songs.map { it.artist }.distinct().size,
        totalDurationMs = songs.sumOf { it.durationMs },
        totalSizeBytes = songs.sumOf { it.sizeBytes },
        formats = songs.groupingBy { it.format }.eachCount().toList().sortedByDescending { it.second }.toMap(),
    )

    private fun norm(s: String) = s.lowercase().filter { it.isLetterOrDigit() }

    /** Same normalised title + artist and durations within ~2 seconds. */
    fun findDuplicates(songs: List<Song>): List<List<Song>> =
        songs.groupBy { norm(it.title) + "|" + norm(it.artist) + "|" + (it.durationMs / 2000) }
            .values.filter { it.size > 1 }
}
