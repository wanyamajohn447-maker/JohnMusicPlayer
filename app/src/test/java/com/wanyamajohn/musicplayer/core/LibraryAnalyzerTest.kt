package com.wanyamajohn.musicplayer.core

import com.wanyamajohn.musicplayer.domain.model.Song
import org.junit.Assert.assertEquals
import org.junit.Test

class LibraryAnalyzerTest {
    private fun song(id: Long, title: String, artist: String, dur: Long, name: String = "$id.mp3") =
        Song(id, title, artist, "Album", 1, dur, 1_000_000, "audio/mpeg", 0, "content://x/$id", name)

    @Test fun detectsDuplicatesIgnoringCaseAndPunctuation() {
        val dups = LibraryAnalyzer.findDuplicates(listOf(
            song(1, "Hello!", "Adele", 200_000), song(2, "hello", "ADELE", 200_500), song(3, "Other", "Adele", 200_000),
        ))
        assertEquals(1, dups.size)
        assertEquals(setOf(1L, 2L), dups[0].map { it.id }.toSet())
    }
    @Test fun statsCountEverything() {
        val st = LibraryAnalyzer.stats(listOf(song(1, "a", "x", 1000, "a.mp3"), song(2, "b", "y", 2000, "b.flac")))
        assertEquals(2, st.songCount); assertEquals(3000, st.totalDurationMs)
        assertEquals(mapOf("MP3" to 1, "FLAC" to 1), st.formats)
    }
}
