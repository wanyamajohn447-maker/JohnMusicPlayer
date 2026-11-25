package com.wanyamajohn.musicplayer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import com.wanyamajohn.musicplayer.ui.JohnMusicRoot
import com.wanyamajohn.musicplayer.ui.MainViewModel
import com.wanyamajohn.musicplayer.ui.theme.JohnTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val mainVm: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val settings by mainVm.settings.collectAsState()
            JohnTheme(accent = Color(settings.accent)) { JohnMusicRoot() }
        }
    }
}
