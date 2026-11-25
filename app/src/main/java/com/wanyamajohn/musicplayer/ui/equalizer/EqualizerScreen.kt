package com.wanyamajohn.musicplayer.ui.equalizer

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wanyamajohn.musicplayer.playback.AudioEffectsManager
import com.wanyamajohn.musicplayer.playback.EffectsState
import com.wanyamajohn.musicplayer.ui.components.ConfirmDialog
import com.wanyamajohn.musicplayer.ui.components.TextInputDialog
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class EqualizerViewModel @Inject constructor(val effects: AudioEffectsManager) : ViewModel() {
    val state: StateFlow<EffectsState> = effects.state
}

@Composable
fun EqualizerScreen(vm: EqualizerViewModel = hiltViewModel()) {
    val s by vm.state.collectAsStateWithLifecycle()
    val fx = vm.effects
    var saving by remember { mutableStateOf(false) }
    var resetting by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (!s.eqAvailable && !s.bassSupported && !s.virtSupported) {
            Text("Audio effects", style = MaterialTheme.typography.headlineSmall)
            Text(
                "No audio effects are available yet. Start playing a song first. If this message stays, your device or " +
                    "current audio output doesn't support the system equalizer, bass boost or virtualizer.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            return@Column
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Equalizer", Modifier.weight(1f), style = MaterialTheme.typography.headlineSmall)
            Switch(checked = s.enabled, onCheckedChange = fx::setEnabled)
        }

        if (s.eqAvailable) {
            Text("Presets (${s.preset})", style = MaterialTheme.typography.titleSmall)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(AudioEffectsManager.BUILTIN.keys.toList()) { name ->
                    FilterChip(selected = s.preset == name, onClick = { fx.applyPreset(name) }, label = { Text(name) })
                }
                items(s.customPresets) { name ->
                    FilterChip(selected = s.preset == name, onClick = { fx.applyPreset(name) }, label = { Text(name) },
                        trailingIcon = { TextButton(onClick = { fx.deleteCustomPreset(name) }, contentPadding = PaddingValues(0.dp)) { Text("✕") } })
                }
            }
            s.bandLevels.forEachIndexed { i, level ->
                val hz = s.centerFreqHz.getOrNull(i) ?: 0
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(if (hz >= 1000) "%.1f kHz".format(hz / 1000f) else "$hz Hz", Modifier.width(72.dp), style = MaterialTheme.typography.labelMedium)
                    Slider(
                        value = level.toFloat(), onValueChange = { fx.setBandLevel(i, it.toInt()) },
                        valueRange = s.minLevel.toFloat()..s.maxLevel.toFloat(), modifier = Modifier.weight(1f), enabled = s.enabled,
                    )
                    Text("%+.1f dB".format(level / 100f), Modifier.width(64.dp), style = MaterialTheme.typography.labelSmall)
                }
            }
        } else {
            Text("The band equalizer isn't supported on this device or output.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        HorizontalDivider()
        Text("Bass boost", style = MaterialTheme.typography.titleSmall)
        if (s.bassSupported) Slider(s.bassStrength.toFloat(), { fx.setBass(it.toInt()) }, valueRange = 0f..1000f, enabled = s.enabled)
        else Text("Not supported on this device. Use the “Bass boost” preset above if the equalizer is available.", color = MaterialTheme.colorScheme.onSurfaceVariant)

        Text("Virtualizer (surround)", style = MaterialTheme.typography.titleSmall)
        if (s.virtSupported) Slider(s.virtStrength.toFloat(), { fx.setVirtualizer(it.toInt()) }, valueRange = 0f..1000f, enabled = s.enabled)
        else Text("Not supported on this device.", color = MaterialTheme.colorScheme.onSurfaceVariant)

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            if (s.eqAvailable) OutlinedButton(onClick = { saving = true }) { Text("Save as preset") }
            OutlinedButton(onClick = { resetting = true }) { Text("Reset") }
        }
    }

    if (saving) TextInputDialog("Save custom preset", confirmLabel = "Save", onConfirm = { fx.saveCustomPreset(it); saving = false }, onDismiss = { saving = false })
    if (resetting) ConfirmDialog("Reset audio effects?", "Equalizer bands, bass boost and virtualizer go back to flat/off.", "Reset",
        onConfirm = { fx.reset(); resetting = false }, onDismiss = { resetting = false })
}
