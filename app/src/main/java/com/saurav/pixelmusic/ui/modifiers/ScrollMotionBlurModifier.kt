package com.saurav.pixelmusic.ui.modifiers

import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import kotlinx.coroutines.flow.collectLatest
import kotlin.math.abs

/**
 * Global motion blur intensity (0f = off, 1f = normal, 2f = extreme).
 * Provided at the app root from user preferences; screens using scrollMotionBlur
 * pick it up automatically without needing to thread the value through.
 */
val LocalMotionBlurIntensity = compositionLocalOf { 1f }

/**
 * Whether motion blur is enabled globally. Provided at the app root from user
 * preferences; screens without direct SettingsViewModel access can use this.
 */
val LocalMotionBlurEnabled = compositionLocalOf { true }

/**
 * Modifier that applies a dynamic vertical or horizontal blur based on scroll speed.
 * Exact matching implementation from sameerasw/essentials.
 */
fun Modifier.scrollMotionBlur(
    scrollState: ScrollState,
    enabled: Boolean? = null,
    isHorizontal: Boolean = false,
    intensity: Float? = null,
    maxBlurPx: Float = 25f,
    velocityThreshold: Float = 50f
): Modifier = composed {
    val isBlurEnabled = enabled ?: LocalMotionBlurEnabled.current
    val currentIntensity = intensity ?: LocalMotionBlurIntensity.current
    if (!isBlurEnabled || currentIntensity <= 0f || Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
        return@composed Modifier
    }

    val effectiveMaxBlur = (maxBlurPx * currentIntensity).coerceAtLeast(1f)
    var previousPosition by remember { mutableIntStateOf(scrollState.value) }
    var previousTime by remember { mutableLongStateOf(System.currentTimeMillis()) }
    val blurRadius = remember { Animatable(0f) }

    LaunchedEffect(scrollState) {
        snapshotFlow { scrollState.value }.collectLatest { currentPosition ->
            val currentTime = System.currentTimeMillis()
            val timeDelta = (currentTime - previousTime).coerceAtLeast(1)
            val positionDelta = abs(currentPosition - previousPosition)

            previousPosition = currentPosition
            previousTime = currentTime

            if (positionDelta > 0 && timeDelta in 1..250) {
                // Velocity in pixels per second
                val velocity = (positionDelta.toFloat() / timeDelta) * 1000f

                if (velocity > velocityThreshold) {
                    val targetBlur = ((velocity - velocityThreshold) / 2000f * effectiveMaxBlur)
                        .coerceIn(0f, effectiveMaxBlur)
                    blurRadius.snapTo(targetBlur)
                }
            }

            // Decay blur to 0 smoothly
            blurRadius.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = 150, easing = LinearEasing)
            )
        }
    }

    this.graphicsLayer {
        val radius = blurRadius.value
        if (radius > 0.5f) {
            val blurX = if (isHorizontal) radius else 0.1f
            val blurY = if (isHorizontal) 0.1f else radius
            renderEffect = RenderEffect.createBlurEffect(
                blurX,
                blurY,
                Shader.TileMode.DECAL
            ).asComposeRenderEffect()
        } else {
            renderEffect = null
        }
    }
}

/**
 * Modifier that applies a dynamic vertical or horizontal blur based on LazyList scroll speed.
 * Exact matching implementation from sameerasw/essentials.
 */
fun Modifier.scrollMotionBlur(
    lazyListState: LazyListState,
    enabled: Boolean? = null,
    isHorizontal: Boolean = false,
    intensity: Float? = null,
    maxBlurPx: Float = 25f,
    velocityThreshold: Float = 50f
): Modifier = composed {
    val isBlurEnabled = enabled ?: LocalMotionBlurEnabled.current
    val currentIntensity = intensity ?: LocalMotionBlurIntensity.current
    if (!isBlurEnabled || currentIntensity <= 0f || Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
        return@composed Modifier
    }

    val effectiveMaxBlur = (maxBlurPx * currentIntensity).coerceAtLeast(1f)
    var previousIndex by remember { mutableIntStateOf(lazyListState.firstVisibleItemIndex) }
    var previousOffset by remember { mutableIntStateOf(lazyListState.firstVisibleItemScrollOffset) }
    var previousTime by remember { mutableLongStateOf(System.currentTimeMillis()) }
    val blurRadius = remember { Animatable(0f) }

    LaunchedEffect(lazyListState) {
        snapshotFlow {
            Pair(lazyListState.firstVisibleItemIndex, lazyListState.firstVisibleItemScrollOffset)
        }.collectLatest { (currentIndex, currentOffset) ->
            val currentTime = System.currentTimeMillis()
            val timeDelta = (currentTime - previousTime).coerceAtLeast(1)

            // Approximate scroll distance using item index and offset
            val positionDelta = if (currentIndex == previousIndex) {
                abs(currentOffset - previousOffset)
            } else {
                abs((currentIndex - previousIndex) * 200 + (currentOffset - previousOffset))
            }

            previousIndex = currentIndex
            previousOffset = currentOffset
            previousTime = currentTime

            if (positionDelta > 0 && timeDelta in 1..250) {
                // Velocity in pixels per second
                val velocity = (positionDelta.toFloat() / timeDelta) * 1000f

                if (velocity > velocityThreshold) {
                    val targetBlur = ((velocity - velocityThreshold) / 2000f * effectiveMaxBlur)
                        .coerceIn(0f, effectiveMaxBlur)
                    blurRadius.snapTo(targetBlur)
                }
            }

            blurRadius.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = 150, easing = LinearEasing)
            )
        }
    }

    this.graphicsLayer {
        val radius = blurRadius.value
        if (radius > 0.5f) {
            val blurX = if (isHorizontal) radius else 0.1f
            val blurY = if (isHorizontal) 0.1f else radius
            renderEffect = RenderEffect.createBlurEffect(
                blurX,
                blurY,
                Shader.TileMode.DECAL
            ).asComposeRenderEffect()
        } else {
            renderEffect = null
        }
    }
}

/**
 * Modifier that applies a dynamic vertical blur based on LazyGrid scroll speed.
 * Exact matching implementation from sameerasw/essentials.
 */
fun Modifier.scrollMotionBlur(
    gridState: LazyGridState,
    enabled: Boolean? = null,
    intensity: Float? = null,
    maxBlurPx: Float = 25f,
    velocityThreshold: Float = 50f
): Modifier = composed {
    val isBlurEnabled = enabled ?: LocalMotionBlurEnabled.current
    val currentIntensity = intensity ?: LocalMotionBlurIntensity.current
    if (!isBlurEnabled || currentIntensity <= 0f || Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
        return@composed Modifier
    }

    val effectiveMaxBlur = (maxBlurPx * currentIntensity).coerceAtLeast(1f)
    var previousIndex by remember { mutableIntStateOf(gridState.firstVisibleItemIndex) }
    var previousOffset by remember { mutableIntStateOf(gridState.firstVisibleItemScrollOffset) }
    var previousTime by remember { mutableLongStateOf(System.currentTimeMillis()) }
    val blurRadius = remember { Animatable(0f) }

    LaunchedEffect(gridState) {
        snapshotFlow {
            Pair(gridState.firstVisibleItemIndex, gridState.firstVisibleItemScrollOffset)
        }.collectLatest { (currentIndex, currentOffset) ->
            val currentTime = System.currentTimeMillis()
            val timeDelta = (currentTime - previousTime).coerceAtLeast(1)

            val positionDelta = if (currentIndex == previousIndex) {
                abs(currentOffset - previousOffset)
            } else {
                abs((currentIndex - previousIndex) * 200 + (currentOffset - previousOffset))
            }

            previousIndex = currentIndex
            previousOffset = currentOffset
            previousTime = currentTime

            if (positionDelta > 0 && timeDelta in 1..250) {
                val velocity = (positionDelta.toFloat() / timeDelta) * 1000f

                if (velocity > velocityThreshold) {
                    val targetBlur = ((velocity - velocityThreshold) / 2000f * effectiveMaxBlur)
                        .coerceIn(0f, effectiveMaxBlur)
                    blurRadius.snapTo(targetBlur)
                }
            }

            blurRadius.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = 150, easing = LinearEasing)
            )
        }
    }

    this.graphicsLayer {
        val radius = blurRadius.value
        if (radius > 0.5f) {
            renderEffect = RenderEffect.createBlurEffect(
                0.1f,
                radius,
                Shader.TileMode.DECAL
            ).asComposeRenderEffect()
        } else {
            renderEffect = null
        }
    }
}

/**
 * Modifier that applies a dynamic vertical blur based on LazyStaggeredGrid scroll speed.
 * Exact matching implementation from sameerasw/essentials.
 */
fun Modifier.scrollMotionBlur(
    lazyStaggeredGridState: LazyStaggeredGridState,
    enabled: Boolean? = null,
    intensity: Float? = null,
    maxBlurPx: Float = 25f,
    velocityThreshold: Float = 50f
): Modifier = composed {
    val isBlurEnabled = enabled ?: LocalMotionBlurEnabled.current
    val currentIntensity = intensity ?: LocalMotionBlurIntensity.current
    if (!isBlurEnabled || currentIntensity <= 0f || Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
        return@composed Modifier
    }

    val effectiveMaxBlur = (maxBlurPx * currentIntensity).coerceAtLeast(1f)
    var previousIndex by remember { mutableIntStateOf(lazyStaggeredGridState.firstVisibleItemIndex) }
    var previousOffset by remember { mutableIntStateOf(lazyStaggeredGridState.firstVisibleItemScrollOffset) }
    var previousTime by remember { mutableLongStateOf(System.currentTimeMillis()) }
    val blurRadius = remember { Animatable(0f) }

    LaunchedEffect(lazyStaggeredGridState) {
        snapshotFlow {
            Pair(lazyStaggeredGridState.firstVisibleItemIndex, lazyStaggeredGridState.firstVisibleItemScrollOffset)
        }.collectLatest { (currentIndex, currentOffset) ->
            val currentTime = System.currentTimeMillis()
            val timeDelta = (currentTime - previousTime).coerceAtLeast(1)

            val positionDelta = if (currentIndex == previousIndex) {
                abs(currentOffset - previousOffset)
            } else {
                abs((currentIndex - previousIndex) * 200 + (currentOffset - previousOffset))
            }

            previousIndex = currentIndex
            previousOffset = currentOffset
            previousTime = currentTime

            if (positionDelta > 0 && timeDelta in 1..250) {
                val velocity = (positionDelta.toFloat() / timeDelta) * 1000f

                if (velocity > velocityThreshold) {
                    val targetBlur = ((velocity - velocityThreshold) / 2000f * effectiveMaxBlur)
                        .coerceIn(0f, effectiveMaxBlur)
                    blurRadius.snapTo(targetBlur)
                }
            }

            blurRadius.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = 150, easing = LinearEasing)
            )
        }
    }

    this.graphicsLayer {
        val radius = blurRadius.value
        if (radius > 0.5f) {
            renderEffect = RenderEffect.createBlurEffect(
                0.1f,
                radius,
                Shader.TileMode.DECAL
            ).asComposeRenderEffect()
        } else {
            renderEffect = null
        }
    }
}

/**
 * Modifier that applies a dynamic horizontal blur based on Pager scroll speed.
 */
fun Modifier.scrollMotionBlur(
    pagerState: PagerState,
    enabled: Boolean? = null,
    intensity: Float? = null,
    maxBlurPx: Float = 25f,
    velocityThreshold: Float = 50f
): Modifier = composed {
    val isBlurEnabled = enabled ?: LocalMotionBlurEnabled.current
    val currentIntensity = intensity ?: LocalMotionBlurIntensity.current
    if (!isBlurEnabled || currentIntensity <= 0f || Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
        return@composed Modifier
    }

    val effectiveMaxBlur = (maxBlurPx * currentIntensity).coerceAtLeast(1f)
    var previousPage by remember { mutableIntStateOf(pagerState.currentPage) }
    var previousOffset by remember { mutableFloatStateOf(pagerState.currentPageOffsetFraction) }
    var previousTime by remember { mutableLongStateOf(System.currentTimeMillis()) }
    val blurRadius = remember { Animatable(0f) }

    LaunchedEffect(pagerState) {
        snapshotFlow {
            Pair(pagerState.currentPage, pagerState.currentPageOffsetFraction)
        }.collectLatest { (currentPage, currentOffset) ->
            val currentTime = System.currentTimeMillis()
            val timeDelta = (currentTime - previousTime).coerceAtLeast(1)

            val pageDelta = (currentPage - previousPage) + (currentOffset - previousOffset)
            val positionDelta = abs(pageDelta * 1000f)

            previousPage = currentPage
            previousOffset = currentOffset
            previousTime = currentTime

            if (positionDelta > 0 && timeDelta in 1..250) {
                val velocity = (positionDelta / timeDelta) * 1000f

                if (velocity > velocityThreshold) {
                    val targetBlur = ((velocity - velocityThreshold) / 2000f * effectiveMaxBlur)
                        .coerceIn(0f, effectiveMaxBlur)
                    blurRadius.snapTo(targetBlur)
                }
            }

            blurRadius.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = 150, easing = LinearEasing)
            )
        }
    }

    this.graphicsLayer {
        val radius = blurRadius.value
        if (radius > 0.5f) {
            renderEffect = RenderEffect.createBlurEffect(
                radius,
                0.1f,
                Shader.TileMode.DECAL
            ).asComposeRenderEffect()
        } else {
            renderEffect = null
        }
    }
}
