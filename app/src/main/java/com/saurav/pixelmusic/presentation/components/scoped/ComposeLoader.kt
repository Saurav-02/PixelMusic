package com.saurav.pixelmusic.presentation.components.scoped

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

// ------------------------------------------------------------
// 1) Phase loader: compose a subtree only after a threshold, then keep it alive
// ------------------------------------------------------------
@Composable
fun DeferAt(
    expansionFraction: Float,
    threshold: Float,
    keepAliveKey: Any? = "default",
    content: @Composable () -> Unit
) {
    var ready by rememberSaveable(keepAliveKey) { mutableStateOf(false) }
    LaunchedEffect(expansionFraction) {
        if (!ready && expansionFraction >= threshold) ready = true
    }
    if (ready) content()
}


@Composable
fun DeferUntil(
    condition: Boolean,
    keepAliveKey: Any? = "default",
    content: @Composable () -> Unit
) {
    var ready by rememberSaveable(keepAliveKey) { mutableStateOf(false) }
    LaunchedEffect(condition) { if (condition) ready = true }
    if (ready) content()
}

// ------------------------------------------------------------
// 2) Progress sampler for long-running sliders/meters
// Emits coarse progress snapshots; visual interpolation is handled in the draw phase
// by the specific component that renders the slider/progress.
// ------------------------------------------------------------
@Composable
fun rememberSmoothProgress(
    isPlayingProvider: () -> Boolean,
    currentPositionProvider: () -> Long,
    totalDuration: Long,
    songId: String,
    sampleWhilePlayingMs: Long = 200L,
    sampleWhilePausedMs: Long = 800L,
    isVisible: Boolean = true
): Pair<androidx.compose.runtime.State<Float>, androidx.compose.runtime.State<Long>> {
    var sampledPosition by remember(songId) { mutableLongStateOf(0L) }
    var sampledFraction by remember(songId) { mutableFloatStateOf(0f) }

    val latestPositionProvider by rememberUpdatedState(newValue = currentPositionProvider)
    val latestIsPlayingProvider by rememberUpdatedState(newValue = isPlayingProvider)
    // Read these inside the loop so the LaunchedEffect doesn't restart every time
    // they change. With the previous keying scheme, crossing the expansion threshold
    // (which flips `isVisible` at 0.01 and the playing sample rate at 0.995) would
    // cancel and relaunch this coroutine mid-gesture — the new fresh coroutine
    // immediately allocated a new Job + had to re-issue its first `sampleNow` and
    // `delay`, which contributed to the post-interaction gesture lag.
    val latestSampleWhilePlayingMs by rememberUpdatedState(sampleWhilePlayingMs)
    val latestSampleWhilePausedMs by rememberUpdatedState(sampleWhilePausedMs)
    val latestIsVisible by rememberUpdatedState(isVisible)

    val latestTotalDuration by rememberUpdatedState(totalDuration)

    LaunchedEffect(songId) {
        fun sampleNow() {
            val rawPosition = latestPositionProvider().coerceAtLeast(0L)
            val dur = latestTotalDuration.coerceAtLeast(0L)
            val clampedPosition = if (dur > 0L) rawPosition.coerceIn(0L, dur) else rawPosition
            sampledPosition = clampedPosition
            sampledFraction = if (dur > 0L) {
                (clampedPosition.toFloat() / dur.toFloat()).coerceIn(0f, 1f)
            } else {
                0f
            }
        }

        sampleNow()

        while (isActive) {
            if (!latestIsVisible) {
                delay(200L)
                continue
            }
            val isPlaying = latestIsPlayingProvider()
            val delayMillis = if (isPlaying) latestSampleWhilePlayingMs else latestSampleWhilePausedMs
            delay(delayMillis.coerceAtLeast(1L))
            sampleNow()
        }
    }

    val fractionState = remember(songId) {
        derivedStateOf { sampledFraction }
    }

    val displayedPositionState = remember(totalDuration, songId) {
        derivedStateOf {
            sampledPosition.coerceIn(0L, totalDuration.coerceAtLeast(0L))
        }
    }

    return fractionState to displayedPositionState
}
