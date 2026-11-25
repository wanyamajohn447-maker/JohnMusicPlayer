package com.wanyamajohn.musicplayer.ui.visualizer

import android.Manifest
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wanyamajohn.musicplayer.core.hasPermission
import com.wanyamajohn.musicplayer.data.prefs.PrefKeys
import com.wanyamajohn.musicplayer.data.prefs.PreferencesRepository
import com.wanyamajohn.musicplayer.playback.VisualizerManager
import com.wanyamajohn.musicplayer.ui.components.MediaPermissionGate
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

@HiltViewModel
class VisualizerViewModel @Inject constructor(val manager: VisualizerManager, private val prefs: PreferencesRepository) : ViewModel() {
    val bars: StateFlow<FloatArray> = manager.bars
    val wave: StateFlow<FloatArray> = manager.wave
    fun setStyle(s: String) = prefs.setAsync(PrefKeys.VIS_STYLE, s)
}

val VISUALIZER_STYLES = listOf("Spectrum", "Waves", "Circular", "Bars", "Particles", "Minimal")

@Composable
fun VisualizerScreen(style: String, onBack: () -> Unit, vm: VisualizerViewModel = hiltViewModel()) {
    Box(Modifier.fillMaxSize().statusBarsPadding()) {
        MediaPermissionGate(
            permission = Manifest.permission.RECORD_AUDIO,
            title = "Visualizer permission",
            rationale = "Android requires the microphone permission to read the audio your phone is playing for the visualizer. " +
                "JOHN MUSIC PLAYER never records, stores or sends audio.",
            onGranted = {},
        ) { VisualizerContent(style, vm) }
        IconButton(onClick = onBack, Modifier.align(Alignment.TopStart)) { Icon(Icons.Default.ArrowBack, "Back") }
    }
}

@Composable
private fun VisualizerContent(initialStyle: String, vm: VisualizerViewModel) {
    var style by remember { mutableStateOf(initialStyle) }
    var failed by remember { mutableStateOf(false) }
    val view = LocalView.current
    val bars by vm.bars.collectAsStateWithLifecycle()
    val wave by vm.wave.collectAsStateWithLifecycle()
    val accent = MaterialTheme.colorScheme.primary

    // Capture runs only while this screen is visible, to save battery.
    DisposableEffect(Unit) {
        failed = !vm.manager.start()
        view.keepScreenOn = true
        onDispose { vm.manager.stop(); view.keepScreenOn = false }
    }
    var t by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(style) {
        if (style == "Particles") while (true) androidx.compose.runtime.withFrameNanos { t = it / 1e9f }
    }

    Column(Modifier.fillMaxSize()) {
        Spacer(Modifier.height(48.dp))
        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            if (failed) Text("Couldn't start the visualizer. Start playing a song first, then reopen this screen.", Modifier.padding(24.dp))
            else Canvas(Modifier.fillMaxSize().padding(16.dp)) { draw(style, bars, wave, accent, t) }
        }
        LazyRow(Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(VISUALIZER_STYLES) { s -> FilterChip(selected = s == style, onClick = { style = s; vm.setStyle(s) }, label = { Text(s) }) }
        }
    }
}

private fun DrawScope.draw(style: String, bars: FloatArray, wave: FloatArray, accent: Color, t: Float) {
    val w = size.width; val h = size.height
    when (style) {
        "Spectrum" -> if (bars.isNotEmpty()) {
            val slot = w / bars.size
            bars.forEachIndexed { i, v ->
                val bh = (v * h).coerceAtLeast(2f)
                drawRect(accent, Offset(i * slot + slot * 0.15f, h - bh), androidx.compose.ui.geometry.Size(slot * 0.7f, bh))
            }
        }
        "Bars" -> if (bars.isNotEmpty()) {
            val slot = w / bars.size
            bars.forEachIndexed { i, v ->
                val bh = (v * h / 2).coerceAtLeast(2f)
                drawRect(accent, Offset(i * slot + slot * 0.2f, h / 2 - bh), androidx.compose.ui.geometry.Size(slot * 0.6f, bh * 2))
            }
        }
        "Waves", "Minimal" -> if (wave.isNotEmpty()) {
            val path = Path()
            wave.forEachIndexed { i, v ->
                val x = i * w / (wave.size - 1).coerceAtLeast(1); val y = h / 2 + v * h / 2.2f
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            drawPath(path, accent, style = Stroke(width = if (style == "Waves") 6f else 2f))
        }
        "Circular" -> if (bars.isNotEmpty()) {
            val c = Offset(w / 2, h / 2); val r0 = min(w, h) * 0.2f
            bars.forEachIndexed { i, v ->
                val a = 2 * PI * i / bars.size
                val len = v * min(w, h) * 0.28f + 4f
                drawLine(accent, Offset(c.x + (r0 * cos(a)).toFloat(), c.y + (r0 * sin(a)).toFloat()),
                    Offset(c.x + ((r0 + len) * cos(a)).toFloat(), c.y + ((r0 + len) * sin(a)).toFloat()), strokeWidth = 6f)
            }
        }
        "Particles" -> {
            val level = if (bars.isEmpty()) 0f else bars.average().toFloat()
            val maxR = min(w, h) / 2
            for (i in 0 until 60) {
                val a = i * 2.39996f
                val r = ((i * 37 + t * (30 + 120 * level)) % maxR)
                drawCircle(accent.copy(alpha = 1f - r / maxR), 3f + level * 14f,
                    Offset(w / 2 + r * cos(a), h / 2 + r * sin(a)))
            }
        }
    }
}
