package com.wanyamajohn.musicplayer.playback

import android.content.Context
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/** Tracks the current audio output route using the supported AudioDeviceCallback API. */
@Singleton
class AudioOutputMonitor @Inject constructor(@ApplicationContext context: Context) {
    private val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val _label = MutableStateFlow(compute())
    val label: StateFlow<String> = _label.asStateFlow()

    init {
        am.registerAudioDeviceCallback(object : AudioDeviceCallback() {
            override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>?) { _label.value = compute() }
            override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>?) { _label.value = compute() }
        }, Handler(Looper.getMainLooper()))
    }

    private fun compute(): String {
        val types = am.getDevices(AudioManager.GET_DEVICES_OUTPUTS).map { it.type }
        val ble = Build.VERSION.SDK_INT >= 31 && (AudioDeviceInfo.TYPE_BLE_HEADSET in types || AudioDeviceInfo.TYPE_BLE_SPEAKER in types)
        return when {
            AudioDeviceInfo.TYPE_BLUETOOTH_A2DP in types || ble || AudioDeviceInfo.TYPE_BLUETOOTH_SCO in types -> "Bluetooth"
            AudioDeviceInfo.TYPE_WIRED_HEADPHONES in types || AudioDeviceInfo.TYPE_WIRED_HEADSET in types -> "Wired headphones"
            AudioDeviceInfo.TYPE_USB_HEADSET in types || AudioDeviceInfo.TYPE_USB_DEVICE in types -> "USB audio"
            else -> "Phone speaker"
        }
    }
}
