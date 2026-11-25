package com.wanyamajohn.musicplayer.ui

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.wanyamajohn.musicplayer.R
import com.wanyamajohn.musicplayer.core.hasPermission
import com.wanyamajohn.musicplayer.ui.components.LogoMark
import com.wanyamajohn.musicplayer.ui.equalizer.EqualizerScreen
import com.wanyamajohn.musicplayer.ui.folders.FoldersScreen
import com.wanyamajohn.musicplayer.ui.home.HomeScreen
import com.wanyamajohn.musicplayer.ui.library.LibraryScreen
import com.wanyamajohn.musicplayer.ui.navigation.Destination
import com.wanyamajohn.musicplayer.ui.navigation.Routes
import com.wanyamajohn.musicplayer.ui.player.MiniPlayer
import com.wanyamajohn.musicplayer.ui.player.NowPlayingScreen
import com.wanyamajohn.musicplayer.ui.playlists.PlaylistsScreen
import com.wanyamajohn.musicplayer.ui.screens.AboutScreen
import com.wanyamajohn.musicplayer.ui.settings.InsightsScreen
import com.wanyamajohn.musicplayer.ui.settings.SettingsScreen
import com.wanyamajohn.musicplayer.ui.video.VideoLibraryScreen
import com.wanyamajohn.musicplayer.ui.video.VideoPlayerScreen
import com.wanyamajohn.musicplayer.ui.visualizer.VisualizerScreen
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JohnMusicRoot(mainVm: MainViewModel = hiltViewModel()) {
    val context = LocalContext.current
    val nav = rememberNavController()
    val entry by nav.currentBackStackEntryAsState()
    val route = entry?.destination?.route
    val player by mainVm.playerState.collectAsStateWithLifecycle()
    val settings by mainVm.settings.collectAsStateWithLifecycle()

    val topLevel = Destination.entries.any { it.route == route }
    val fullscreen = route == Routes.NOW_PLAYING || route == Routes.VISUALIZER || route?.startsWith("video") == true
    var splash by rememberSaveable { mutableStateOf(true) }
    LaunchedEffect(Unit) { delay(1200); splash = false }

    // Ask for notification permission (Android 13+) only once something starts playing.
    val notifLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
    var askedNotif by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(player.isPlaying) {
        if (player.isPlaying && !askedNotif && Build.VERSION.SDK_INT >= 33 && !hasPermission(context, Manifest.permission.POST_NOTIFICATIONS)) {
            askedNotif = true
            notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    Box(Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                if (!fullscreen) TopAppBar(
                    title = { Text(stringResource(R.string.app_name)) },
                    navigationIcon = { if (!topLevel) IconButton(onClick = { nav.popBackStack() }) { Icon(Icons.Default.ArrowBack, "Back") } },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background, titleContentColor = MaterialTheme.colorScheme.primary,
                    ),
                    actions = {
                        IconButton(onClick = { nav.navigate(Destination.Library.route) { launchSingleTop = true } }) { Icon(Icons.Default.Search, "Search") }
                        IconButton(onClick = { nav.navigate(Destination.Equalizer.route) { launchSingleTop = true } }) { Icon(Icons.Default.Equalizer, "Equalizer") }
                        IconButton(onClick = { nav.navigate(Destination.Settings.route) { launchSingleTop = true } }) { Icon(Icons.Default.Settings, "Settings") }
                    },
                )
            },
            bottomBar = {
                if (!fullscreen) Column {
                    if (player.hasMedia) MiniPlayer(player, onOpen = { nav.navigate(Routes.NOW_PLAYING) }, onToggle = mainVm::togglePlay, onNext = mainVm::next)
                    if (topLevel) NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                        Destination.entries.filter { it.inBottomBar }.forEach { d ->
                            NavigationBarItem(
                                selected = route == d.route,
                                onClick = {
                                    nav.navigate(d.route) {
                                        popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                icon = { Icon(d.icon, contentDescription = d.label) },
                                label = { Text(d.label, maxLines = 1) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.onPrimary, indicatorColor = MaterialTheme.colorScheme.primary,
                                ),
                            )
                        }
                    }
                }
            },
        ) { padding ->
            val pad = if (fullscreen) Modifier else Modifier.padding(padding)
            NavHost(nav, startDestination = Destination.Home.route, modifier = pad) {
                composable(Destination.Home.route) {
                    HomeScreen(
                        onOpenLibrary = { nav.navigate(Destination.Library.route) },
                        onOpenPlaylists = { nav.navigate(Destination.Playlists.route) },
                    )
                }
                composable(Destination.Library.route) { LibraryScreen() }
                composable(Destination.Folders.route) { FoldersScreen(onPlayVideo = { nav.navigate(Routes.video(it)) }) }
                composable(Destination.Playlists.route) { PlaylistsScreen() }
                composable(Destination.Videos.route) { VideoLibraryScreen(onPlay = { nav.navigate(Routes.video(it)) }) }
                composable(Destination.Equalizer.route) { EqualizerScreen() }
                composable(Destination.Settings.route) {
                    SettingsScreen(onOpenAbout = { nav.navigate(Routes.ABOUT) }, onOpenInsights = { nav.navigate(Routes.INSIGHTS) })
                }
                composable(Routes.NOW_PLAYING) {
                    NowPlayingScreen(
                        onBack = { nav.popBackStack() },
                        onOpenEqualizer = { nav.navigate(Destination.Equalizer.route) },
                        onOpenVisualizer = { nav.navigate(Routes.VISUALIZER) },
                    )
                }
                composable(Routes.VISUALIZER) { VisualizerScreen(style = settings.visStyle, onBack = { nav.popBackStack() }) }
                composable(Routes.ABOUT) { AboutScreen() }
                composable(Routes.INSIGHTS) { InsightsScreen() }
                composable(Routes.VIDEO, arguments = listOf(navArgument("uri") { type = NavType.StringType; defaultValue = "" })) { e ->
                    VideoPlayerScreen(uriString = e.arguments?.getString("uri").orEmpty(), onBack = { nav.popBackStack() })
                }
            }
        }

        AnimatedVisibility(visible = splash, exit = fadeOut()) {
            Column(
                Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
                verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                LogoMark(Modifier.size(width = 120.dp, height = 84.dp))
                Spacer(Modifier.height(20.dp))
                Text(stringResource(R.string.app_name), style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
                Text(stringResource(R.string.tagline), style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(32.dp))
                Text("WANYAMA JOHN TECHNOLOGIES", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
