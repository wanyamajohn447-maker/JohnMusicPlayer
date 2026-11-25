package com.wanyamajohn.musicplayer.ui.navigation

import android.net.Uri
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

enum class Destination(val route: String, val label: String, val icon: ImageVector, val inBottomBar: Boolean = true) {
    Home("home", "Home", Icons.Default.Home),
    Library("library", "Library", Icons.Default.LibraryMusic),
    Folders("folders", "Folders", Icons.Default.Folder),
    Playlists("playlists", "Playlists", Icons.Default.QueueMusic),
    Videos("videos", "Videos", Icons.Default.VideoLibrary),
    Equalizer("equalizer", "Equalizer", Icons.Default.Equalizer, inBottomBar = false),
    Settings("settings", "Settings", Icons.Default.Settings, inBottomBar = false),
}

object Routes {
    const val NOW_PLAYING = "now_playing"
    const val VISUALIZER = "visualizer"
    const val ABOUT = "about"
    const val INSIGHTS = "insights"
    const val VIDEO = "video?uri={uri}"
    fun video(uri: String) = "video?uri=" + Uri.encode(uri)
}
