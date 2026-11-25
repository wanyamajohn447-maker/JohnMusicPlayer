package com.wanyamajohn.musicplayer.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

object JohnColors {
    val Midnight = Color(0xFF050505)
    val Charcoal = Color(0xFF151515)
    val CharcoalHigh = Color(0xFF1F1F1F)
    val Orange = Color(0xFFFF7A00)
    val TextPrimary = Color(0xFFFFFFFF)
    val TextSecondary = Color(0xFF9A9A9A)
}

@Composable
fun JohnTheme(accent: Color = JohnColors.Orange, content: @Composable () -> Unit) {
    val scheme = darkColorScheme(
        primary = accent,
        onPrimary = Color.Black,
        background = JohnColors.Midnight,
        onBackground = JohnColors.TextPrimary,
        surface = JohnColors.Charcoal,
        onSurface = JohnColors.TextPrimary,
        surfaceVariant = JohnColors.CharcoalHigh,
        onSurfaceVariant = JohnColors.TextSecondary,
        secondaryContainer = accent.copy(alpha = 0.25f),
        onSecondaryContainer = JohnColors.TextPrimary,
    )
    MaterialTheme(colorScheme = scheme, content = content)
}
