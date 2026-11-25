package com.wanyamajohn.musicplayer.playback

import android.media.audiofx.Visualizer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.hypot
import kotlin.math.pow

/** Real playback audio data via android.media.audiofx.Visualizer (requires RECORD_AUDIO; nothing is recorded or stored). */
@Singleton
class VisualizerManager @Inject constructor(private val effects: AudioEffectsManager) {
    private var visualizer: Visualizer? = null
    private val _bars = MutableStateFlow(FloatArray(0))
    private val _wave = MutableStateFlow(FloatArray(0))
    val bars: StateFlow<FloatArray> = _bars.asStateFlow()
    val wave: StateFlow<FloatArray> = _wave.asStateFlow()

    companion object { const val BAR_COUNT = 48 }

    /** Returns false if the visualizer could not be started (no session yet, no permission, unsupported). */
    fun start(): Boolean {
        stop()
        val id = effects.sessionId.value
        if (id == 0) return false
        return try {
            val v = Visualizer(id)
            v.captureSize = Visualizer.getCaptureSizeRange()[1].coerceAtMost(1024)
            v.setDataCaptureListener(object : Visualizer.OnDataCaptureListener {
                override fun onWaveFormDataCapture(v: Visualizer?, waveform: ByteArray?, samplingRate: Int) {
                    if (waveform == null) return
                    _wave.value = FloatArray(waveform.size) { ((waveform[it].toInt() and 0xFF) - 128) / 128f }
                }
                override fun onFftDataCapture(v: Visualizer?, fft: ByteArray?, samplingRate: Int) {
                    if (fft == null || fft.size < 4) return
                    val bins = fft.size / 2 - 1
                    val out = FloatArray(BAR_COUNT)
                    for (i in 0 until BAR_COUNT) {
                        val start = (bins * (i.toFloat() / BAR_COUNT).pow(2)).toInt().coerceIn(0, bins - 1)
                        val end = (bins * ((i + 1).toFloat() / BAR_COUNT).pow(2)).toInt().coerceIn(start + 1, bins)
                        var sum = 0f
                        for (k in start until end) {
                            val re = fft[2 * (k + 1)].toFloat(); val im = fft[2 * (k + 1) + 1].toFloat()
                            sum += hypot(re, im) / 128f
                        }
                        out[i] = (sum / (end - start)).coerceIn(0f, 1f)
                    }
                    _bars.value = out
                }
            }, Visualizer.getMaxCaptureRate(), true, true)
            v.enabled = true
            visualizer = v
            true
        } catch (_: Throwable) { false }
    }

    fun stop() {
        runCatching { visualizer?.enabled = false }
        runCatching { visualizer?.release() }
        visualizer = null
    }
}
