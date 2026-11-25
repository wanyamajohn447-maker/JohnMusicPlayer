package com.wanyamajohn.musicplayer.core

import org.junit.Assert.assertEquals
import org.junit.Test

class FormatTest {
    @Test fun minutesAndSeconds() = assertEquals("3:35", formatDuration(215_000))
    @Test fun withHours() = assertEquals("1:02:05", formatDuration(3_725_000))
    @Test fun zeroAndNegative() {
        assertEquals("0:00", formatDuration(0))
        assertEquals("0:00", formatDuration(-5))
    }
}
