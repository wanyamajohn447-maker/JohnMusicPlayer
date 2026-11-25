package com.wanyamajohn.musicplayer.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.wanyamajohn.musicplayer.R
import com.wanyamajohn.musicplayer.ui.components.LogoMark

const val APP_VERSION = "1.0.0"

val PRIVACY_SUMMARY = """
JOHN MUSIC PLAYER is built to be private.

• No account, no cloud, no ads and no analytics.
• The app has no internet permission, so it cannot send your files or data anywhere.
• Your music and videos are read in place through Android's media storage; nothing is copied or uploaded.
• Playlists, favourites, play history, lyrics and settings are stored only on this device.
• Media permission is used to list and play your files. The microphone permission is used only by the visualizer to read the audio the phone is playing; audio is never recorded or stored.
• Notification permission is used only for playback controls.
• You can clear play history in Settings, and uninstalling the app removes all of its data.
""".trimIndent()

@Composable
fun AboutScreen(modifier: Modifier = Modifier) {
    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            LogoMark(Modifier.size(width = 72.dp, height = 52.dp))
            Column {
                Text(stringResource(R.string.app_name), style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
                Text(stringResource(R.string.tagline), style = MaterialTheme.typography.labelLarge)
            }
        }
        HorizontalDivider()
        Text("Product: JOHN MUSIC PLAYER")
        Text("Developer: " + stringResource(R.string.developer))
        Text("Organization: " + stringResource(R.string.organization))
        Text("Version: $APP_VERSION")
        Text("Not affiliated with VLC or VideoLAN.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        HorizontalDivider()
        Text("Privacy", style = MaterialTheme.typography.titleMedium)
        Text(PRIVACY_SUMMARY, style = MaterialTheme.typography.bodyMedium)
    }
}
