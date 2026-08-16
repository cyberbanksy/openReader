package com.orgista.openreader.ui.boot

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

/**
 * Native port of the "Mark 1a — The Spread" boot animation from the shared claude.ai design
 * project (OpenReader Boot.dc.html / openreader-brand.jsx / reader-boot-scenes.jsx): a closed
 * book presses down, swings open into the two-tone spread, settles into the lockup, and keeps
 * flipping pages as a loading state until the real catalog load finishes.
 */
private object BootBrand {
    val Audio = Color(0xFF3AA896)
    val AudioDeep = Color(0xFF2C8577)
    val TextAccent = Color(0xFFE8A552)
    val TextDeep = Color(0xFFC4863D)
    val Paper = Color(0xFFF2E6D0)
    val Shell = Color(0xFF232323)
    val ShellDeep = Color(0xFF1A1A1A)
}

// Scene durations, ported from OM_SCENES in OpenReader Boot.dc.html.
private const val CLOSED_DUR = 0.8f
private const val TURN_DUR = 1.2f
private const val SETTLE_DUR = 0.7f
private const val LOCKUP_DUR = 1.2f
private const val MIN_LOADING_HOLD = 0.9f

private const val TURN_START = CLOSED_DUR
private const val SETTLE_START = TURN_START + TURN_DUR
private const val LOCKUP_START = SETTLE_START + SETTLE_DUR
private const val LOADING_START = LOCKUP_START + LOCKUP_DUR

private val EaseOutCubic = CubicBezierEasing(0.215f, 0.61f, 0.355f, 1f)
private val EaseInOutCubic = CubicBezierEasing(0.645f, 0.045f, 0.355f, 1f)
private val EaseOutBack = CubicBezierEasing(0.34f, 1.56f, 0.64f, 1f)

private fun ease(t: Float, from: Float, to: Float, start: Float, end: Float, easing: Easing): Float {
    if (end <= start) return if (t >= end) to else from
    val raw = ((t - start) / (end - start)).coerceIn(0f, 1f)
    return from + (to - from) * easing.transform(raw)
}

@Composable
fun OpenReaderBootScreen(
    isLoading: Boolean,
    onFinished: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val latestLoading = rememberUpdatedState(isLoading)
    var elapsed by remember { mutableStateOf(0f) }
    var exiting by remember { mutableStateOf(false) }
    val exitAlpha = remember { Animatable(1f) }
    val exitScale = remember { Animatable(1f) }

    LaunchedEffect(Unit) {
        val startNanos = withFrameNanos { it }
        while (true) {
            withFrameNanos { now -> elapsed = (now - startNanos) / 1_000_000_000f }
            if (!exiting && elapsed >= LOADING_START + MIN_LOADING_HOLD && !latestLoading.value) {
                exiting = true
                launch { exitScale.animateTo(1.045f, tween(420, easing = FastOutSlowInEasing)) }
                exitAlpha.animateTo(0f, tween(420, easing = FastOutSlowInEasing))
                onFinished()
            }
        }
    }

    Box(
        modifier
            .fillMaxSize()
            .graphicsLayer {
                alpha = exitAlpha.value
                scaleX = exitScale.value
                scaleY = exitScale.value
            }
            .background(BootBrand.ShellDeep),
    ) {
        Backdrop(elapsed, Modifier.fillMaxSize())
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val isLandscape = maxWidth > maxHeight
            val heroSize = if (isLandscape) minOf(maxWidth, maxHeight) * 0.44f else maxWidth * 0.5f
            val finalSize = heroSize * 0.6f
            val lockupReveal = ease(elapsed, 0f, 1f, LOCKUP_START + 0.2f, LOCKUP_START + 1.0f, EaseOutCubic)
            val taglineReveal = ease(elapsed, 0f, 1f, LOCKUP_START + 0.62f, LOCKUP_START + 1.15f, EaseOutCubic)

            if (isLandscape) {
                Row(
                    modifier = Modifier.align(Alignment.Center),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(32.dp),
                ) {
                    Mark(elapsed, heroSize, finalSize)
                    Column(horizontalAlignment = Alignment.Start) {
                        Wordmark(lockupReveal, fontSize = 32.sp)
                        Spacer(Modifier.height(6.dp))
                        Tagline(taglineReveal)
                    }
                }
            } else {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Mark(elapsed, heroSize, finalSize)
                    Spacer(Modifier.height(20.dp))
                    Wordmark(lockupReveal, fontSize = 34.sp)
                    Spacer(Modifier.height(6.dp))
                    Tagline(taglineReveal)
                }
            }
            LoadingBar(
                elapsed,
                Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 56.dp),
            )
        }
    }
}

@Composable
private fun Backdrop(t: Float, modifier: Modifier = Modifier) {
    val bloom = ease(t, 0f, 1f, TURN_START + 0.3f, LOCKUP_START + 0.4f, EaseOutCubic)
    Box(modifier.background(BootBrand.Shell)) {
        Box(
            Modifier
                .align(Alignment.Center)
                .fillMaxSize(0.9f)
                .graphicsLayer {
                    alpha = 0.25f + bloom * 0.65f
                    scaleX = 0.86f + bloom * 0.14f
                    scaleY = 0.86f + bloom * 0.14f
                }
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            BootBrand.TextAccent.copy(alpha = 0.16f),
                            BootBrand.Audio.copy(alpha = 0.07f),
                            Color.Transparent,
                        ),
                    ),
                ),
        )
    }
}

@Composable
private fun Mark(t: Float, heroSize: Dp, finalSize: Dp) {
    // Anticipation dip, then one continuous swing that overshoots flat and settles.
    val dip = ease(t, 0f, -0.05f, TURN_START - 0.26f, TURN_START, EaseInOutCubic)
    val swing = ease(t, 0f, 1f, TURN_START, SETTLE_START + 0.45f, EaseOutBack)
    val open = max(0f, dip + swing)
    val tilt = ease(t, 9f, 0f, 0.1f, SETTLE_START + 0.2f, EaseInOutCubic)
    val scaleK = ease(t, 0f, 1f, LOCKUP_START, LOCKUP_START + 0.7f, EaseOutCubic)
    val currentSize = heroSize * (1f - scaleK) + finalSize * scaleK
    val breathe = 1f + sin(t * 1.1f) * 0.006f

    val flipAmount = ease(t, 0f, 1f, LOADING_START - 0.55f, LOADING_START - 0.15f, EaseOutCubic)
    val flipPhase = max(0f, (t - (LOADING_START - 0.55f)) * 2.4f)

    Canvas(
        Modifier
            .width(currentSize)
            .height(currentSize)
            .graphicsLayer {
                rotationX = tilt
                scaleX = breathe
                scaleY = breathe
            },
    ) {
        val k = size.minDimension / 120f
        val halfW = size.width / 2f
        val cy = size.height / 2f

        // Far/right page — always flat, the "text" side of the spread.
        withTransform({ translate(left = halfW) }) {
            drawPath(rightPagePath(k), color = BootBrand.TextAccent)
            drawPath(rightEdgePath(k), color = BootBrand.TextDeep)
        }

        // Turning leaf: press down, swing open on the spine, revealing the teal interior.
        val angleRad = Math.toRadians(((1f - open) * 180f).toDouble())
        val faceSign = cos(angleRad).toFloat()
        val foldScale = abs(faceSign).coerceIn(0.02f, 1f)
        val frontFacing = faceSign > 0f
        val leafColor = if (frontFacing) BootBrand.Audio else BootBrand.TextAccent
        val edgeColor = if (frontFacing) BootBrand.AudioDeep else BootBrand.TextDeep

        withTransform({ scale(scaleX = foldScale, scaleY = 1f, pivot = Offset(halfW, cy)) }) {
            drawPath(leftPagePath(k), color = leafColor)
            drawPath(leftEdgePath(k), color = edgeColor)
            val turnShade = sin(Math.PI * open.toDouble().coerceIn(0.0, 1.0)).toFloat()
            if (turnShade > 0f) {
                drawRect(
                    brush = Brush.horizontalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = turnShade * 0.45f)),
                        startX = halfW * 0.4f,
                        endX = halfW,
                    ),
                )
            }
        }

        // Gutter seam, revealed once the spread is more than half open.
        val gutterAlpha = ((open - 0.5f) / 0.45f).coerceIn(0f, 1f)
        if (gutterAlpha > 0f) {
            drawRoundRect(
                color = BootBrand.Shell.copy(alpha = gutterAlpha),
                topLeft = Offset(halfW - size.width * 0.029f, size.height * 0.27f),
                size = Size(size.width * 0.058f, size.height * 0.58f),
                cornerRadius = CornerRadius(size.width * 0.03f),
            )
        }

        // Loading state: pages keep flipping from right to left.
        if (flipAmount > 0.002f) {
            for (i in 0..2) {
                val raw = flipPhase - i * 0.34f
                if (raw <= 0f) continue
                val p = raw % 1f
                val past = p > 0.5f
                val shade = sin(Math.PI * p).toFloat()
                val leafAngleRad = Math.toRadians((-180.0 * p))
                val leafFold = abs(cos(leafAngleRad)).toFloat().coerceIn(0.02f, 1f)
                val base = if (past) BootBrand.Audio else BootBrand.TextAccent
                val shaded = Color(
                    red = base.red * (1f - shade * 0.32f),
                    green = base.green * (1f - shade * 0.32f),
                    blue = base.blue * (1f - shade * 0.32f),
                    alpha = flipAmount,
                )
                withTransform({ scale(scaleX = leafFold, scaleY = 1f, pivot = Offset(halfW, cy)) }) {
                    translate(left = halfW) {
                        drawPath(rightPagePath(k), color = shaded)
                    }
                }
            }
        }
    }
}

@Composable
private fun Wordmark(reveal: Float, fontSize: TextUnit) {
    Row {
        WordPart("Open", BootBrand.Paper, reveal, delay = 0f, fontSize)
        WordPart("Reader", BootBrand.TextAccent, reveal, delay = 0.16f, fontSize)
    }
}

@Composable
private fun WordPart(word: String, color: Color, reveal: Float, delay: Float, fontSize: TextUnit) {
    val r = ((reveal - delay) / 0.84f).coerceIn(0f, 1f)
    Text(
        text = word,
        color = color,
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.SemiBold,
        fontSize = fontSize,
        modifier = Modifier
            .alpha(r)
            .graphicsLayer { translationY = (18.dp * (1f - r)).toPx() },
    )
}

@Composable
private fun Tagline(reveal: Float) {
    Text(
        text = "LISTEN · READ · OWN",
        color = Color(0xFFB9B2A6),
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 12.sp,
        letterSpacing = 3.sp,
        modifier = Modifier
            .alpha(reveal * 0.95f)
            .graphicsLayer { translationY = (10.dp * (1f - reveal)).toPx() },
    )
}

@Composable
private fun LoadingBar(t: Float, modifier: Modifier = Modifier) {
    val show = ease(t, 0f, 1f, LOCKUP_START + 0.35f, LOCKUP_START + 0.75f, EaseOutCubic)
    if (show <= 0.001f) return
    val fill = ease(t, 0.06f, 1f, LOCKUP_START + 0.35f, LOADING_START + 2f, EaseInOutCubic).coerceIn(0f, 1f)
    Box(
        modifier
            .width(220.dp)
            .height(4.dp)
            .alpha(show)
            .clip(RoundedCornerShape(2.dp))
            .background(BootBrand.Paper.copy(alpha = 0.14f)),
    ) {
        Box(
            Modifier
                .fillMaxHeight()
                .fillMaxWidth(fill)
                .clip(RoundedCornerShape(2.dp))
                .background(Brush.horizontalGradient(listOf(BootBrand.Audio, BootBrand.TextAccent))),
        )
    }
}

// Per-half page geometry, ported 1:1 from the HalfPage bezier paths in openreader-brand.jsx
// (viewBox 0 0 60 120 per half). k = drawn-size / 120.
private fun leftPagePath(k: Float): Path = Path().apply {
    moveTo(60f * k, 34f * k)
    cubicTo(46f * k, 24f * k, 30f * k, 21f * k, 15f * k, 23f * k)
    lineTo(15f * k, 92f * k)
    cubicTo(30f * k, 90f * k, 46f * k, 93f * k, 60f * k, 102f * k)
    close()
}

private fun leftEdgePath(k: Float): Path = Path().apply {
    moveTo(60f * k, 34f * k)
    cubicTo(46f * k, 24f * k, 30f * k, 21f * k, 15f * k, 23f * k)
    lineTo(15f * k, 30f * k)
    cubicTo(30f * k, 28f * k, 46f * k, 31f * k, 60f * k, 41f * k)
    close()
}

private fun rightPagePath(k: Float): Path = Path().apply {
    moveTo(0f, 34f * k)
    cubicTo(14f * k, 24f * k, 30f * k, 21f * k, 45f * k, 23f * k)
    lineTo(45f * k, 92f * k)
    cubicTo(30f * k, 90f * k, 14f * k, 93f * k, 0f, 102f * k)
    close()
}

private fun rightEdgePath(k: Float): Path = Path().apply {
    moveTo(0f, 34f * k)
    cubicTo(14f * k, 24f * k, 30f * k, 21f * k, 45f * k, 23f * k)
    lineTo(45f * k, 30f * k)
    cubicTo(30f * k, 28f * k, 14f * k, 31f * k, 0f, 41f * k)
    close()
}
