package com.wanyamajohn.musicplayer.playback

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EqualizerCurveTest {
    @Test fun mapsFivePointCurveOntoFiveBandsUnchanged() {
        val curve = listOf(500, 300, -100, 300, 500)
        assertEquals(curve, AudioEffectsManager.mapCurve(curve, 5, -1500, 1500))
    }
    @Test fun clampsToDeviceRange() {
        val out = AudioEffectsManager.mapCurve(listOf(700, 700, 700, 700, 700), 3, -300, 300)
        assertTrue(out.all { it == 300 })
    }
    @Test fun handlesSingleBand() {
        assertEquals(1, AudioEffectsManager.mapCurve(listOf(100, 0, 0, 0, 0), 1, -1500, 1500).size)
    }
}
