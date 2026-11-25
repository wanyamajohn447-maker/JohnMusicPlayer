package com.wanyamajohn.musicplayer.playback

import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.Virtualizer
import com.wanyamajohn.musicplayer.data.prefs.PrefKeys
import com.wanyamajohn.musicplayer.data.prefs.PreferencesRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

data class EffectsState(
    val enabled: Boolean = false,
    val eqAvailable: Boolean = false,
    val bandCount: Int = 0,
    val minLevel: Int = 0,
    val maxLevel: Int = 0,
    val centerFreqHz: List<Int> = emptyList(),
    val bandLevels: List<Int> = emptyList(),
    val bassSupported: Boolean = false,
    val bassStrength: Int = 0,
    val virtSupported: Boolean = false,
    val virtStrength: Int = 0,
    val preset: String = "Normal",
    val customPresets: List<String> = emptyList(),
)

/**
 * Real android.media.audiofx effects bound to the ExoPlayer audio session.
 * If a device doesn't support an effect, its availability flag stays false and the UI says so.
 */
@Singleton
class AudioEffectsManager @Inject constructor(private val prefs: PreferencesRepository) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var eq: Equalizer? = null
    private var bass: BassBoost? = null
    private var virt: Virtualizer? = null
    private var attachJob: Job? = null
    private var custom: Map<String, List<Int>> = emptyMap()

    private val _state = MutableStateFlow(EffectsState())
    val state: StateFlow<EffectsState> = _state.asStateFlow()
    private val _sessionId = MutableStateFlow(0)
    val sessionId: StateFlow<Int> = _sessionId.asStateFlow()

    companion object {
        /** 5-point gain curves in millibels, interpolated onto however many bands the device has. */
        val BUILTIN: Map<String, List<Int>> = linkedMapOf(
            "Normal" to listOf(0, 0, 0, 0, 0),
            "Flat" to listOf(0, 0, 0, 0, 0),
            "Rock" to listOf(500, 300, -100, 300, 500),
            "Pop" to listOf(-100, 200, 400, 200, -100),
            "Classical" to listOf(400, 300, -200, 300, 400),
            "Jazz" to listOf(300, 200, -200, 200, 400),
            "Dance" to listOf(600, 400, 0, 200, 300),
            "Hip-hop" to listOf(500, 400, 100, 100, 300),
            "Acoustic" to listOf(400, 400, 200, 100, 300),
            "Bass boost" to listOf(700, 500, 100, 0, 0),
        )

        fun mapCurve(curve: List<Int>, bands: Int, min: Int, max: Int): List<Int> =
            List(bands) { i ->
                val pos = if (bands == 1) 0.0 else i * (curve.size - 1).toDouble() / (bands - 1)
                val lo = pos.toInt().coerceIn(0, curve.size - 1)
                val hi = (lo + 1).coerceAtMost(curve.size - 1)
                val v = curve[lo] + (curve[hi] - curve[lo]) * (pos - lo)
                v.toInt().coerceIn(min, max)
            }
    }

    fun attach(sessionId: Int) {
        _sessionId.value = sessionId
        attachJob?.cancel()
        attachJob = scope.launch { doAttach(sessionId) }
    }

    fun release() {
        attachJob?.cancel()
        releaseEffects()
        _sessionId.value = 0
        _state.value = EffectsState()
    }

    private fun releaseEffects() {
        runCatching { eq?.release() }; runCatching { bass?.release() }; runCatching { virt?.release() }
        eq = null; bass = null; virt = null
    }

    private suspend fun doAttach(sessionId: Int) {
        releaseEffects()
        if (sessionId == 0) { _state.value = EffectsState(); return }
        val enabled = prefs.get(PrefKeys.EQ_ENABLED, false)
        val saved = parseInts(prefs.get(PrefKeys.EQ_LEVELS, ""))
        val bassStrength = prefs.get(PrefKeys.BASS, 0)
        val virtStrength = prefs.get(PrefKeys.VIRT, 0)
        custom = parseCustom(prefs.get(PrefKeys.EQ_CUSTOM, ""))
        var s = EffectsState(enabled = enabled, preset = prefs.get(PrefKeys.EQ_PRESET, "Normal"), customPresets = custom.keys.toList())

        try {
            val e = Equalizer(0, sessionId)
            val range = e.bandLevelRange
            val n = e.numberOfBands.toInt()
            val min = range[0].toInt(); val max = range[1].toInt()
            val levels = if (saved.size == n) saved.map { it.coerceIn(min, max) } else List(n) { 0 }
            levels.forEachIndexed { i, l -> e.setBandLevel(i.toShort(), l.toShort()) }
            e.enabled = enabled
            eq = e
            s = s.copy(
                eqAvailable = true, bandCount = n, minLevel = min, maxLevel = max,
                centerFreqHz = List(n) { e.getCenterFreq(it.toShort()) / 1000 }, bandLevels = levels,
            )
        } catch (_: Throwable) { eq = null }

        try {
            val b = BassBoost(0, sessionId)
            if (b.strengthSupported) {
                b.setStrength(bassStrength.coerceIn(0, 1000).toShort())
                b.enabled = enabled
                bass = b
                s = s.copy(bassSupported = true, bassStrength = bassStrength)
            } else b.release()
        } catch (_: Throwable) { bass = null }

        try {
            val v = Virtualizer(0, sessionId)
            if (v.strengthSupported) {
                v.setStrength(virtStrength.coerceIn(0, 1000).toShort())
                v.enabled = enabled
                virt = v
                s = s.copy(virtSupported = true, virtStrength = virtStrength)
            } else v.release()
        } catch (_: Throwable) { virt = null }

        _state.value = s
    }

    fun setEnabled(on: Boolean) {
        runCatching { eq?.enabled = on }; runCatching { bass?.enabled = on }; runCatching { virt?.enabled = on }
        _state.update { it.copy(enabled = on) }
        prefs.setAsync(PrefKeys.EQ_ENABLED, on)
    }

    fun setBandLevel(band: Int, level: Int) {
        val s = _state.value
        if (band !in s.bandLevels.indices) return
        val l = level.coerceIn(s.minLevel, s.maxLevel)
        runCatching { eq?.setBandLevel(band.toShort(), l.toShort()) }
        val levels = s.bandLevels.toMutableList().also { it[band] = l }
        _state.update { it.copy(bandLevels = levels, preset = "Custom") }
        prefs.setAsync(PrefKeys.EQ_LEVELS, levels.joinToString(","))
        prefs.setAsync(PrefKeys.EQ_PRESET, "Custom")
    }

    fun setBass(strength: Int) {
        runCatching { bass?.setStrength(strength.coerceIn(0, 1000).toShort()) }
        _state.update { it.copy(bassStrength = strength) }
        prefs.setAsync(PrefKeys.BASS, strength)
    }

    fun setVirtualizer(strength: Int) {
        runCatching { virt?.setStrength(strength.coerceIn(0, 1000).toShort()) }
        _state.update { it.copy(virtStrength = strength) }
        prefs.setAsync(PrefKeys.VIRT, strength)
    }

    fun applyPreset(name: String) {
        val s = _state.value
        if (!s.eqAvailable) return
        val levels = custom[name]?.takeIf { it.size == s.bandCount }?.map { it.coerceIn(s.minLevel, s.maxLevel) }
            ?: BUILTIN[name]?.let { mapCurve(it, s.bandCount, s.minLevel, s.maxLevel) }
            ?: return
        levels.forEachIndexed { i, l -> runCatching { eq?.setBandLevel(i.toShort(), l.toShort()) } }
        _state.update { it.copy(bandLevels = levels, preset = name) }
        prefs.setAsync(PrefKeys.EQ_LEVELS, levels.joinToString(","))
        prefs.setAsync(PrefKeys.EQ_PRESET, name)
    }

    fun saveCustomPreset(rawName: String) {
        val name = rawName.filter { it != '=' && it != ';' && it != ',' }.trim().take(24)
        val s = _state.value
        if (name.isEmpty() || name in BUILTIN || !s.eqAvailable) return
        custom = custom + (name to s.bandLevels)
        _state.update { it.copy(preset = name, customPresets = custom.keys.toList()) }
        prefs.setAsync(PrefKeys.EQ_CUSTOM, custom.entries.joinToString(";") { it.key + "=" + it.value.joinToString(",") })
        prefs.setAsync(PrefKeys.EQ_PRESET, name)
    }

    fun deleteCustomPreset(name: String) {
        custom = custom - name
        _state.update { it.copy(customPresets = custom.keys.toList(), preset = if (it.preset == name) "Custom" else it.preset) }
        prefs.setAsync(PrefKeys.EQ_CUSTOM, custom.entries.joinToString(";") { it.key + "=" + it.value.joinToString(",") })
    }

    fun reset() {
        applyPreset("Flat")
        setBass(0)
        setVirtualizer(0)
    }

    private fun parseInts(s: String): List<Int> = s.split(',').mapNotNull { it.trim().toIntOrNull() }
    private fun parseCustom(s: String): Map<String, List<Int>> =
        s.split(';').filter { it.contains('=') }.associate { it.substringBefore('=') to parseInts(it.substringAfter('=')) }
}
