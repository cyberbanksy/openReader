package com.orgista.openreader.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

object OpenReaderColors {
    val Ink = Color(0xFF2B2118)
    val Canvas = Color(0xFFFAF6F0)
    val Paper = Color(0xFFFFFDF9)
    val Muted = Color(0xFF6F665E)
    val Amber = Color(0xFFC4802A)
    val Forest = Color(0xFF3E6259)
    val Rust = Color(0xFFA54E32)
    val DarkCanvas = Color(0xFF181512)
    val DarkPaper = Color(0xFF25211D)
    val DarkMuted = Color(0xFFC9C0B7)
}

private val lightScheme = lightColorScheme(
    primary = OpenReaderColors.Amber,
    onPrimary = Color.White,
    secondary = OpenReaderColors.Forest,
    onSecondary = Color.White,
    tertiary = OpenReaderColors.Rust,
    background = OpenReaderColors.Canvas,
    onBackground = OpenReaderColors.Ink,
    surface = OpenReaderColors.Paper,
    onSurface = OpenReaderColors.Ink,
    surfaceVariant = Color(0xFFF0E9E1),
    onSurfaceVariant = OpenReaderColors.Muted,
    outline = Color(0xFFC3B8AB),
)

private val darkScheme = darkColorScheme(
    primary = Color(0xFFE3A353),
    onPrimary = Color(0xFF412600),
    secondary = Color(0xFF8EB9AD),
    onSecondary = Color(0xFF0B352D),
    tertiary = Color(0xFFE09276),
    background = OpenReaderColors.DarkCanvas,
    onBackground = Color(0xFFF2F1EE),
    surface = OpenReaderColors.DarkPaper,
    onSurface = Color(0xFFF2F1EE),
    surfaceVariant = Color(0xFF332E29),
    onSurfaceVariant = OpenReaderColors.DarkMuted,
    outline = Color(0xFF55545B),
)

private val typography = Typography(
    headlineSmall = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        letterSpacing = 0.sp,
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        letterSpacing = 0.sp,
    ),
)

@Composable
fun OpenReaderTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) darkScheme else lightScheme,
        typography = typography,
        content = content,
    )
}
