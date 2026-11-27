package com.saurav.pixelmusic.presentation.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.ui.graphics.TransformOrigin

// Smooth, unhurried motion curves so the zoom-out and zoom-in stages are clearly visible
val SmoothZoomOutEasing = CubicBezierEasing(0.32f, 0.0f, 0.2f, 1.0f)
val SmoothZoomInEasing = CubicBezierEasing(0.22f, 0.68f, 0.18f, 1.0f)

// Total transition duration (staggered two-phase zoom choreography)
const val TRANSITION_DURATION = 520

// Forward Push: Stage 2 — Incoming screen zooms in (0.80f -> 1.0f) after previous screen starts zooming out
fun enterTransition(): EnterTransition = scaleIn(
    animationSpec = tween(durationMillis = 390, delayMillis = 130, easing = SmoothZoomInEasing),
    initialScale = 0.80f,
    transformOrigin = TransformOrigin(0.5f, 0.5f)
) + fadeIn(
    animationSpec = tween(durationMillis = 330, delayMillis = 130, easing = SmoothZoomInEasing)
)

// Forward Push: Stage 1 — Previous screen zooms out (1.0f -> 0.80f) with blur and fades out
fun exitTransition(): ExitTransition = scaleOut(
    animationSpec = tween(durationMillis = 360, easing = SmoothZoomOutEasing),
    targetScale = 0.80f,
    transformOrigin = TransformOrigin(0.5f, 0.5f)
) + fadeOut(
    animationSpec = tween(durationMillis = 300, delayMillis = 60, easing = SmoothZoomOutEasing)
)

// Backward Pop: Stage 2 — Previous screen zooms back in (0.80f -> 1.0f) as blur clears
fun popEnterTransition(): EnterTransition = scaleIn(
    animationSpec = tween(durationMillis = 380, delayMillis = 120, easing = SmoothZoomInEasing),
    initialScale = 0.80f,
    transformOrigin = TransformOrigin(0.5f, 0.5f)
) + fadeIn(
    animationSpec = tween(durationMillis = 320, delayMillis = 120, easing = SmoothZoomInEasing)
)

// Backward Pop: Stage 1 — Current top screen zooms out (1.0f -> 0.80f) and fades away
fun popExitTransition(): ExitTransition = scaleOut(
    animationSpec = tween(durationMillis = 340, easing = SmoothZoomOutEasing),
    targetScale = 0.80f,
    transformOrigin = TransformOrigin(0.5f, 0.5f)
) + fadeOut(
    animationSpec = tween(durationMillis = 280, easing = SmoothZoomOutEasing)
)
