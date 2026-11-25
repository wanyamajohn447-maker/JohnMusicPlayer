package com.wanyamajohn.musicplayer.core

import com.wanyamajohn.musicplayer.domain.model.Song
import org.junit.Assert.assertEquals
import org.junit.Test

class M3uTest {
    private fun song(id: Long, name: String) = Song(id, "T$id", "A", "Al", 1, 215_000, 1000, "audio/mpeg", 0, "content://x/$id", name, null, "Music/")

    @Test fun buildThenParseRoundTrips() {
        val text = M3u.build(listOf(song(1, "one.mp3"), song(2, "two.flac")))
        val parsed = M3u.parse(text)
        assertEquals(2, parsed.size)
        assertEquals("Music/one.mp3", parsed[0].path)
        assertEquals(215L, parsed[0].durationSec)
        assertEquals("A - T1", parsed[0].title)
    }
    @Test fun baseNameHandlesBothSeparators() {
        assertEquals("a.mp3", M3u.baseName("C:\\Music\\a.mp3"))
        assertEquals("b.mp3", M3u.baseName("/sdcard/Music/b.mp3"))
    }
    @Test fun parseIgnoresCommentsAndBlankLines() {
        assertEquals(1, M3u.parse("#EXTM3U\n\n# note\nsong.mp3\n").size)
    }
}
