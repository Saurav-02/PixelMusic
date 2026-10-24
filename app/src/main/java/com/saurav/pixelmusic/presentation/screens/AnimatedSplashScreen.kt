package com.saurav.pixelmusic.presentation.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

private const val OUTER_RIBBON_PATH_DATA =
    "M 191.6 53.5 C 180.5 55.6 167 60.4 156.9 65.7 C 122.7 83.7 96.2 120.2 91 156.5 C 90.2 162.3 90 203 90.2 301.5 L 90.5 438.5 L 93.2 448.2 C 108 501.8 146.4 534.2 198 536.7 C 229.2 538.1 257.6 528.8 296.5 504.2 C 344.1 474.1 352 469.5 367.8 462.5 C 382.6 456 398.2 450.8 416 446.6 C 450.3 438.4 456.9 436.7 466 433.3 C 504.5 419 530.6 395.2 541.5 364.6 C 546.3 351.4 548 340.4 548 323.9 C 548 296.9 540.8 273.1 525.8 250.5 C 516.5 236.4 503.6 224.7 472 201.9 C 442.2 180.3 408.9 156.7 376 133.8 C 365.3 126.4 346.8 113.6 335 105.3 C 278.9 66.3 270.5 61.6 245.6 55.4 C 232.6 52.1 204.3 51.1 191.6 53.5 Z M 231.2 81.4 C 247.9 84.4 261.4 90.2 279.8 102.4 C 290.4 109.4 293.1 111.8 288.9 110.6 C 283.8 109.2 260.3 108.7 252 109.9 C 201.6 117.3 159.6 161.8 153.1 214.5 C 152.5 219.4 152 231.4 152 241 C 152 276 147.5 291.2 128.2 321 L 120.2 333.2 L 119.5 248.4 C 118.9 171 119 162.8 120.5 155.5 C 128.2 119.1 158.4 89 195 81.5 C 203.5 79.8 221.6 79.7 231.2 81.4 Z M 297.3 136.5 C 310.2 138.7 321.1 142.5 335.5 149.5 C 353.1 158.1 369.2 168.8 458.5 231.6 C 479.6 246.4 494.4 260.6 501.4 272.5 C 517.3 299.9 519.9 333.4 508 359.7 C 500.4 376.5 484.7 392.1 466.2 401.4 C 455.7 406.7 444.2 410.3 419.9 416 C 395.8 421.6 382.9 425.5 369 431 C 346.2 440.1 327.3 450.3 299.8 468.1 C 263.9 491.4 245.4 497.7 216 496.7 C 194.9 496 179.3 491.1 163.3 480.2 C 144.8 467.5 130.5 445.8 125.9 423.5 C 120.3 396.6 126.7 375 151.1 338.3 C 161 323.5 166.3 313.2 170.5 300.5 C 176 284.2 177.1 276.5 178 246 C 178.5 230.3 179.4 215.2 180 212.5 C 188.1 173.9 216.4 144.7 253.5 136.5 C 261.9 134.7 286.8 134.7 297.3 136.5 Z"

private const val INNER_NOTES_PATH_DATA =
    "M 351.5 192.9 C 346 194.4 317.4 201.9 288 209.6 C 255.5 218 233.4 224.3 231.6 225.5 C 230 226.6 227.7 228.8 226.6 230.3 C 224.5 233.1 224.5 233.9 224.2 301.5 L 223.9 369.8 L 219.3 368.4 C 208.8 365.2 196.4 367.1 183.6 373.8 C 163.1 384.5 152.1 405.5 157.5 423.6 C 162.8 441.5 182.2 452.4 202.7 448.9 C 221.9 445.7 239.5 430.7 246.6 411.5 C 248.3 406.9 248.5 402.3 248.8 355.3 L 249.1 304.1 L 252.8 303.1 C 274.9 297.4 352.2 278 353 278 C 353.6 278 354 289.6 354 310.6 L 354 343.2 L 351.8 342.5 C 350.5 342.2 346.3 341.6 342.3 341.3 C 321.7 339.7 299.2 355.9 292 377.6 C 289.2 385.9 289.5 397 292.8 404.2 C 295.9 411.2 303.2 418.5 310.9 422.2 C 322.9 427.9 340 427.2 351.6 420.4 C 362 414.3 370.8 403.7 375.8 391.2 L 378.5 384.5 L 378.8 292.5 C 379 202 379 200.4 377 197.2 C 374.4 193 369.4 190 365 190.1 C 363.1 190.1 357 191.4 351.5 192.9 Z M 354 242.4 L 354 252.8 L 329.8 258.9 C 316.4 262.2 293.4 268.1 278.5 272 C 263.7 275.8 250.9 279 250.3 279 C 249.3 279 249 276.3 249 268.7 L 249 258.5 L 300.8 245.3 C 329.2 238.1 352.8 232.1 353.3 232.1 C 353.7 232 354 236.7 354 242.4 Z"

@Composable
fun AnimatedSplashScreen(
    onExitStarted: () -> Unit = {},
    onSplashFinished: () -> Unit
) {
    val badgeAlpha = remember { Animatable(0f) }
    val badgeScale = remember { Animatable(0.42f) }
    val badgeRotation = remember { Animatable(-60f) }
    val scallopMorph = remember { Animatable(0.11f) }
    val haloProgress = remember { Animatable(0f) }

    val outerAlpha = remember { Animatable(0f) }
    val outerScale = remember { Animatable(0.48f) }
    val outerRotation = remember { Animatable(-36f) }

    val notesAlpha = remember { Animatable(0f) }
    val notesScale = remember { Animatable(0.36f) }
    val notesOffsetY = remember { Animatable(22f) }
    val notesRotation = remember { Animatable(-48f) }

    val exitProgress = remember { Animatable(0f) }

    val outerRibbonPath = remember {
        PathParser().parsePathString(OUTER_RIBBON_PATH_DATA).toPath().apply {
            fillType = PathFillType.EvenOdd
        }
    }
    val innerNotesPath = remember {
        PathParser().parsePathString(INNER_NOTES_PATH_DATA).toPath().apply {
            fillType = PathFillType.EvenOdd
        }
    }
    val scallopPath = remember { Path() }

    LaunchedEffect(Unit) {
        // 1. Slow, graceful badge entrance and continuous slow rotation across the full splash
        launch {
            badgeAlpha.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 420, easing = LinearOutSlowInEasing)
            )
        }
        launch {
            badgeScale.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = 0.68f,
                    stiffness = Spring.StiffnessVeryLow
                )
            )
        }
        launch {
            badgeRotation.animateTo(
                targetValue = 120f,
                animationSpec = tween(durationMillis = 2500, easing = LinearOutSlowInEasing)
            )
        }
        launch {
            scallopMorph.animateTo(
                targetValue = 0.045f,
                animationSpec = tween(durationMillis = 1650, easing = FastOutSlowInEasing)
            )
        }
        launch {
            haloProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 1650, easing = LinearOutSlowInEasing)
            )
        }

        // 2. Outer "P" ribbon slowly rotates and scales into place
        launch {
            outerAlpha.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 480, easing = LinearOutSlowInEasing)
            )
        }
        launch {
            outerScale.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = 0.66f,
                    stiffness = Spring.StiffnessVeryLow
                )
            )
        }
        launch {
            outerRotation.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = 1400, easing = FastOutSlowInEasing)
            )
        }

        // 3. Staggered inner eighth notes (♫) slowly rotate and rise into the loop
        launch {
            delay(180L)
            launch {
                notesAlpha.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = 480, easing = LinearOutSlowInEasing)
                )
            }
            launch {
                notesScale.animateTo(
                    targetValue = 1f,
                    animationSpec = spring(
                        dampingRatio = 0.62f,
                        stiffness = Spring.StiffnessVeryLow
                    )
                )
            }
            launch {
                notesOffsetY.animateTo(
                    targetValue = 0f,
                    animationSpec = tween(durationMillis = 1250, easing = FastOutSlowInEasing)
                )
            }
            launch {
                notesRotation.animateTo(
                    targetValue = 0f,
                    animationSpec = tween(durationMillis = 1350, easing = FastOutSlowInEasing)
                )
            }
        }

        // 4. Hold for the longer rotation choreography, then trigger slow high-level zoom into Home
        delay(1650L)
        onExitStarted()
        exitProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 850, easing = FastOutSlowInEasing)
        )
        onSplashFinished()
    }

    val backgroundColor = MaterialTheme.colorScheme.background
    val badgeColor = MaterialTheme.colorScheme.primaryContainer
    val haloColor = MaterialTheme.colorScheme.secondaryContainer
    val logoColor = MaterialTheme.colorScheme.onPrimaryContainer

    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                alpha = 1f - exitProgress.value
            }
            .background(backgroundColor),
        contentAlignment = Alignment.Center
    ) {
        // Expanding harmonic halo ring with high-level zoom on exit
        Canvas(
            modifier = Modifier
                .size(240.dp)
                .graphicsLayer {
                    val progress = haloProgress.value
                    val exitZoom = 1f + (exitProgress.value * 3.6f)
                    scaleX = (0.55f + (progress * 0.65f)) * exitZoom
                    scaleY = (0.55f + (progress * 0.65f)) * exitZoom
                    alpha = (1f - progress * 0.7f) * 0.55f
                }
        ) {
            drawCircle(
                color = haloColor,
                radius = size.minDimension * 0.46f,
                style = Stroke(width = 20.dp.toPx())
            )
        }

        // Material 3 Expressive 12-lobe scalloped badge slowly rotating + high-level zoom on exit
        Canvas(
            modifier = Modifier
                .size(156.dp)
                .graphicsLayer {
                    val exitZoom = 1f + (exitProgress.value * 3.4f)
                    scaleX = badgeScale.value * exitZoom
                    scaleY = badgeScale.value * exitZoom
                    rotationZ = badgeRotation.value
                    alpha = badgeAlpha.value
                }
        ) {
            val cx = size.width * 0.5f
            val cy = size.height * 0.5f
            val baseRadius = min(cx, cy) * 0.88f
            val waveAmplitude = scallopMorph.value * (1f - exitProgress.value * 0.6f)
            val lobes = 12
            val steps = 144

            scallopPath.rewind()
            for (i in 0..steps) {
                val theta = (2.0 * PI * i) / steps
                val radius = baseRadius * (1f + waveAmplitude * cos(lobes * theta).toFloat())
                val x = cx + radius * cos(theta).toFloat()
                val y = cy + radius * sin(theta).toFloat()
                if (i == 0) {
                    scallopPath.moveTo(x, y)
                } else {
                    scallopPath.lineTo(x, y)
                }
            }
            scallopPath.close()

            drawPath(
                path = scallopPath,
                color = badgeColor
            )
        }

        // Outer "P" Ribbon Layer (Slow rotation + high-level zoom on exit)
        Canvas(
            modifier = Modifier
                .size(88.dp)
                .graphicsLayer {
                    val exitZoom = 1f + (exitProgress.value * 3.8f)
                    scaleX = outerScale.value * exitZoom
                    scaleY = outerScale.value * exitZoom
                    rotationZ = outerRotation.value + (exitProgress.value * 12f)
                    alpha = outerAlpha.value
                }
        ) {
            withTransform({
                scale(
                    scaleX = size.width / 600f,
                    scaleY = size.height / 600f,
                    pivot = Offset.Zero
                )
                scale(
                    scaleX = 1.15f,
                    scaleY = 1.15f,
                    pivot = Offset(319f, 295f)
                )
            }) {
                drawPath(
                    path = outerRibbonPath,
                    color = logoColor
                )
            }
        }

        // Inner Eighth Notes (♫) Layer (Staggered slow rotation + high-level zoom on exit)
        Canvas(
            modifier = Modifier
                .size(88.dp)
                .graphicsLayer {
                    val exitZoom = 1f + (exitProgress.value * 3.8f)
                    scaleX = notesScale.value * exitZoom
                    scaleY = notesScale.value * exitZoom
                    translationY = notesOffsetY.value.dp.toPx()
                    rotationZ = notesRotation.value + (exitProgress.value * 12f)
                    alpha = notesAlpha.value
                }
        ) {
            withTransform({
                scale(
                    scaleX = size.width / 600f,
                    scaleY = size.height / 600f,
                    pivot = Offset.Zero
                )
                scale(
                    scaleX = 1.15f,
                    scaleY = 1.15f,
                    pivot = Offset(319f, 295f)
                )
            }) {
                drawPath(
                    path = innerNotesPath,
                    color = logoColor
                )
            }
        }
    }
}
