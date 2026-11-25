package com.wanyamajohn.musicplayer.core

import org.junit.Assert.*
import org.junit.Test

class LrcParserTest {
    @Test fun parsesTimedLinesAndIgnoresIdTags() {
        val lines = LrcParser.parse("[ar:Someone]\n[00:12.50]Hello\n[01:02.5]World\n")
        assertEquals(2, lines.size)
        assertEquals(12_500L, lines[0].timeMs)
        assertEquals("Hello", lines[0].text)
        assertEquals(62_500L, lines[1].timeMs)
    }
    @Test fun multipleTagsOnOneLineAreSorted() {
        val lines = LrcParser.parse("[00:30.00][00:10.00]Chorus")
        assertEquals(listOf(10_000L, 30_000L), lines.map { it.timeMs })
        assertTrue(lines.all { it.text == "Chorus" })
    }
    @Test fun plainTextIsNotSynced() = assertFalse(LrcParser.isSynced("just words\nno timing"))
    @Test fun currentIndexTracksPosition() {
        val lines = LrcParser.parse("[00:01.00]a\n[00:05.00]b\n[00:09.00]c")
        assertEquals(-1, LrcParser.currentIndex(lines, 500))
        assertEquals(0, LrcParser.currentIndex(lines, 1000))
        assertEquals(1, LrcParser.currentIndex(lines, 8999))
        assertEquals(2, LrcParser.currentIndex(lines, 60_000))
    }
}
