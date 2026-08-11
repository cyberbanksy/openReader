package com.orgista.openreader.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

object OpenReaderColors {
    val Ink = Color(0xFF1A1A1D)
    val Canvas = Color(0xFFF7F7F4)
    val Paper = Color(0xFFFFFFFF)
    val Muted = Color(0xFF66666D)
    val Red = Color(0xFFD8463E)
    val Teal = Color(0xFF176B67)
    val Gold = Color(0xFFB58A38)
    val DarkCanvas = Color(0xFF111113)
    val DarkPaper = Color(0xFF1D1D21)
    val DarkMuted = Color(0xFFB8B7BD)
}

private val lightScheme = lightColorScheme(
    primary = OpenReaderColors.Red,
    onPrimary = Color.White,
    secondary = OpenReaderColors.Teal,
    onSecondary = Color.White,
    tertiary = OpenReaderColors.Gold,
    background = OpenReaderColors.Canvas,
    onBackground = OpenReaderColors.Ink,
    surface = OpenReaderColors.Paper,
    onSurface = OpenReaderColors.Ink,
    surfaceVariant = Color(0xFFEAEAE6),
    onSurfaceVariant = OpenReaderColors.Muted,
    outline = Color(0xFFB9B9B4),
)

private val darkScheme = darkColorScheme(
    primary = Color(0xFFFF736A),
    onPrimary = Color(0xFF3D0705),
    secondary = Color(0xFF70CCC5),
    onSecondary = Color(0xFF003733),
    tertiary = Color(0xFFE7BF6F),
    background = OpenReaderColors.DarkCanvas,
    onBackground = Color(0xFFF2F1EE),
    surface = OpenReaderColors.DarkPaper,
    onSurface = Color(0xFFF2F1EE),
    surfaceVariant = Color(0xFF2A2A2F),
    onSurfaceVariant = OpenReaderColors.DarkMuted,
    outline = Color(0xFF55545B),
)

@Composable
fun OpenReaderTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) darkScheme else lightScheme,
        typography = Typography(),
        content = content,
    )
}
