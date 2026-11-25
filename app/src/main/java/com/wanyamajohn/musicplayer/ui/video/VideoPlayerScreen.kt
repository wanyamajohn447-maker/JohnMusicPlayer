package com.wanyamajohn.musicplayer.ui.video

import android.app.Activity
import android.app.PictureInPictureParams
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.view.View
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.wanyamajohn.musicplayer.core.formatDuration
import com.wanyamajohn.musicplayer.core.formatSize
import com.wanyamajohn.musicplayer.core.queryDisplayName
import com.wanyamajohn.musicplayer.core.querySize
import com.wanyamajohn.musicplayer.ui.components.InfoDialog
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

fun Context.findActivity(): Activity? {
    var c = this
    while (c is ContextWrapper) { if (c is Activity) return c; c = c.baseContext }
    return null
}

@androidx.annotation.OptIn(UnstableApi::class)
@Composable
fun VideoPlayerScreen(uriString: String, onBack: () -> Unit, vm: VideoViewModel = hiltViewModel()) {
    val context = LocalContext.current
    val activity = context.findActivity()
    val view = LocalView.current
    val uri = remember(uriString) { Uri.parse(uriString) }
    val title = remember(uri) { queryDisplayName(context, uri) ?: "Video" }
    val player = remember { ExoPlayer.Builder(context).build() }
    val resizeModes = remember {
        listOf(AspectRatioFrameLayout.RESIZE_MODE_FIT, AspectRatioFrameLayout.RESIZE_MODE_FILL, AspectRatioFrameLayout.RESIZE_MODE_ZOOM)
    }
    var resizeIdx by remember { mutableIntStateOf(0) }
    var landscape by remember { mutableStateOf(false) }
    var showInfo by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var hud by remember { mutableStateOf<String?>(null) }
    var controlsVisible by remember { mutableStateOf(true) }
    var playerView by remember { mutableStateOf<PlayerView?>(null) }
    var volumeF by remember { mutableFloatStateOf(-1f) }

    fun savePosition() = vm.savePosition(uriString, player.currentPosition, player.duration.takeIf { it > 0 } ?: 0L)

    LaunchedEffect(uri) {
        vm.pauseMusic()
        val saved = vm.savedPosition(uriString)
        player.setMediaItem(MediaItem.fromUri(uri), if (saved > 5000) saved else 0L)
        player.prepare()
        player.playWhenReady = true
    }

    DisposableEffect(player) {
        val l = object : androidx.media3.common.Player.Listener {
            override fun onPlayerError(e: androidx.media3.common.PlaybackException) {
                error = "This video can't be played on this device (unsupported or damaged file)."
            }
        }
        player.addListener(l)
        onDispose { player.removeListener(l) }
    }

    // Full-screen: hide system bars while the player is open.
    DisposableEffect(Unit) {
        val window = activity?.window
        val controller = window?.let { WindowCompat.getInsetsController(it, view) }
        controller?.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller?.hide(WindowInsetsCompat.Type.systemBars())
        onDispose {
            savePosition()
            player.release()
            controller?.show(WindowInsetsCompat.Type.systemBars())
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            window?.let { w -> val lp = w.attributes; lp.screenBrightness = -1f; w.attributes = lp }
        }
    }

    LifecycleEventEffect(Lifecycle.Event.ON_PAUSE) { savePosition() }
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) {
        val inPip = Build.VERSION.SDK_INT >= 24 && activity?.isInPictureInPictureMode == true
        if (!inPip) player.pause()
    }
    LaunchedEffect(hud) { if (hud != null) { delay(800); hud = null } }

    val subtitlePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { picked ->
        if (picked != null) {
            val name = queryDisplayName(context, picked).orEmpty().lowercase()
            val mime = if (name.endsWith(".vtt")) MimeTypes.TEXT_VTT else MimeTypes.APPLICATION_SUBRIP
            val sub = MediaItem.SubtitleConfiguration.Builder(picked).setMimeType(mime).setLanguage("und")
                .setSelectionFlags(C.SELECTION_FLAG_DEFAULT).build()
            val pos = player.currentPosition
            player.setMediaItem(MediaItem.Builder().setUri(uri).setSubtitleConfigurations(listOf(sub)).build(), pos)
            player.prepare()
        }
    }

    fun adjustBrightness(delta: Float) {
        val w = activity?.window ?: return
        val lp = w.attributes
        val cur = if (lp.screenBrightness < 0) 0.5f else lp.screenBrightness
        lp.screenBrightness = (cur + delta).coerceIn(0.02f, 1f)
        w.attributes = lp
        hud = "Brightness ${(lp.screenBrightness * 100).roundToInt()}%"
    }

    fun adjustVolume(delta: Float) {
        val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val max = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        if (volumeF < 0) volumeF = am.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat()
        volumeF = (volumeF + delta * max).coerceIn(0f, max.toFloat())
        am.setStreamVolume(AudioManager.STREAM_MUSIC, volumeF.roundToInt(), 0)
        hud = "Volume ${(volumeF / max * 100).roundToInt()}%"
    }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    this.player = player
                    setShowSubtitleButton(true)
                    controllerShowTimeoutMs = 3000
                    setControllerVisibilityListener(PlayerView.ControllerVisibilityListener { v -> controlsVisible = v == View.VISIBLE })
                    playerView = this
                }
            },
            update = { it.resizeMode = resizeModes[resizeIdx] },
            modifier = Modifier.fillMaxSize(),
        )

        // Gesture zones in the middle band so the built-in controls stay reachable: left = brightness, right = volume.
        Row(Modifier.align(Alignment.Center).fillMaxWidth().fillMaxHeight(0.5f)) {
            for (left in listOf(true, false)) {
                Box(
                    Modifier.weight(1f).fillMaxHeight()
                        .pointerInput(Unit) {
                            detectTapGestures(onTap = {
                                playerView?.let { pv -> if (pv.isControllerFullyVisible) pv.hideController() else pv.showController() }
                            })
                        }
                        .pointerInput(Unit) {
                            detectVerticalDragGestures { change, dy ->
                                change.consume()
                                val delta = -dy / size.height * 1.2f
                                if (left) adjustBrightness(delta) else adjustVolume(delta)
                            }
                        },
                )
            }
        }

        if (controlsVisible) {
            Row(Modifier.align(Alignment.TopCenter).fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back", tint = Color.White) }
                Text(title, Modifier.weight(1f), color = Color.White, maxLines = 1, style = MaterialTheme.typography.titleSmall)
                IconButton(onClick = { subtitlePicker.launch(arrayOf("*/*")) }) { Icon(Icons.Default.Subtitles, "Load subtitles", tint = Color.White) }
                IconButton(onClick = { resizeIdx = (resizeIdx + 1) % resizeModes.size }) { Icon(Icons.Default.AspectRatio, "Aspect ratio", tint = Color.White) }
                IconButton(onClick = {
                    landscape = !landscape
                    activity?.requestedOrientation =
                        if (landscape) ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE else ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                }) { Icon(Icons.Default.ScreenRotation, "Rotate", tint = Color.White) }
                if (Build.VERSION.SDK_INT >= 26 && activity?.packageManager?.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE) == true) {
                    IconButton(onClick = { activity.enterPictureInPictureMode(PictureInPictureParams.Builder().build()) }) {
                        Icon(Icons.Default.PictureInPicture, "Picture in picture", tint = Color.White)
                    }
                }
                IconButton(onClick = { showInfo = true }) { Icon(Icons.Default.Info, "Video info", tint = Color.White) }
            }
        }

        hud?.let {
            Surface(Modifier.align(Alignment.Center), shape = MaterialTheme.shapes.medium, color = Color.Black.copy(alpha = 0.7f)) {
                Text(it, Modifier.padding(horizontal = 16.dp, vertical = 8.dp), color = Color.White)
            }
        }
        error?.let {
            Column(Modifier.align(Alignment.Center).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(it, color = Color.White)
                Spacer(Modifier.height(12.dp))
                Button(onClick = onBack) { Text("Back") }
            }
        }
    }

    if (showInfo) {
        val f = player.videoFormat
        InfoDialog(
            title = title,
            rows = listOf(
                "Duration" to formatDuration(player.duration.coerceAtLeast(0)),
                "File size" to (querySize(context, uri)?.let { formatSize(it) } ?: "-"),
                "Resolution" to (f?.let { "${it.width}x${it.height}" } ?: "-"),
                "Video codec" to (f?.sampleMimeType ?: "-"),
                "Audio codec" to (player.audioFormat?.sampleMimeType ?: "-"),
                "Source" to uriString,
            ),
            onDismiss = { showInfo = false },
        )
    }
}
