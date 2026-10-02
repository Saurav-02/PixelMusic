package com.saurav.pixelmusic.ui.modifiers

import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.os.Build
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import kotlinx.coroutines.isActive
import org.intellij.lang.annotations.Language
import kotlin.math.abs

@Language("AGSL")
private const val DIRECTIONAL_BLUR_AGSL = """
    uniform shader composable;
    uniform float2 resolution;
    uniform float scrollVelocity;
    uniform float isHorizontal;

    half4 main(float2 fragCoord) {
        const int SAMPLES = 10;
        half4 color = half4(0.0);
        float totalWeight = 0.0;
        
        float blurMagnitude = clamp(scrollVelocity * 22.0, -40.0, 40.0);

        // Fade blur in from the top edge so boundary pixels never get smeared.
        // smoothstep gives a natural curve; 80px ≈ status-bar height on most devices.
        float topFadeHeight = 80.0;
        float topFactor = smoothstep(0.0, topFadeHeight, fragCoord.y);
        float effectiveMagnitude = blurMagnitude * topFactor;

        // If the effective blur is negligible, just return the source pixel.
        if (abs(effectiveMagnitude) < 0.5) {
            return composable.eval(fragCoord);
        }

        for (int i = 0; i < SAMPLES; i++) {
            float offset = (float(i) / float(SAMPLES - 1) - 0.5) * effectiveMagnitude;
            
            float2 sampleCoord;
            if (isHorizontal > 0.5) {
                sampleCoord = float2(fragCoord.x + offset, fragCoord.y);
            } else {
                sampleCoord = float2(fragCoord.x, fragCoord.y + offset);
            }
            
            // Skip samples that fall outside the layer bounds — this is what
            // eliminates the horizontal "streak" artifacts at the top edge.
            if (sampleCoord.x >= 0.0 && sampleCoord.x <= resolution.x &&
                sampleCoord.y >= 0.0 && sampleCoord.y <= resolution.y) {
                
                float weight = 1.0 - abs(offset / (abs(effectiveMagnitude) + 0.001)) * 0.5;
                color += composable.eval(sampleCoord) * weight;
                totalWeight += weight;
            }
        }

        // Guard: if all samples were skipped, fall back to the source pixel.
        if (totalWeight < 0.001) {
            return composable.eval(fragCoord);
        }

        return color / totalWeight;
    }
"""

fun Modifier.scrollMotionBlur(
    scrollState: ScrollState,
    enabled: Boolean = true,
    isHorizontal: Boolean = false,
): Modifier = composed {
    if (!enabled || Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
        return@composed Modifier
    }

    val animatedVelocity = remember { Animatable(0f) }

    LaunchedEffect(scrollState) {
        var prevValue = scrollState.value
        var prevTimeNanos = 0L

        while (isActive) {
            var newVelocityToSnap = 0f
            withFrameNanos { frameTimeNanos ->
                val currentValue = scrollState.value
                if (prevTimeNanos != 0L) {
                    val dtMs = (frameTimeNanos - prevTimeNanos) / 1_000_000.0f
                    if (dtMs in 1f..100f) {
                        val delta = (currentValue - prevValue).toFloat()
                        val targetVelocity = (delta / dtMs).coerceIn(-3f, 3f)
                        if (abs(delta) > 0.1f && scrollState.isScrollInProgress) {
                            newVelocityToSnap = animatedVelocity.value * 0.35f + targetVelocity * 0.65f
                        } else {
                            val decayed = animatedVelocity.value * 0.45f
                            newVelocityToSnap = if (abs(decayed) < 0.01f) 0f else decayed
                        }
                    } else {
                        newVelocityToSnap = 0f
                    }
                }
                prevValue = currentValue
                prevTimeNanos = frameTimeNanos
            }
            animatedVelocity.snapTo(newVelocityToSnap)
        }
    }

    LaunchedEffect(scrollState.isScrollInProgress) {
        if (!scrollState.isScrollInProgress) {
            animatedVelocity.animateTo(0f, animationSpec = tween(60))
        }
    }

    val shader = remember { RuntimeShader(DIRECTIONAL_BLUR_AGSL) }

    Modifier.graphicsLayer {
        val vel = animatedVelocity.value
        if (abs(vel) > 0.05f) {
            shader.setFloatUniform("resolution", size.width, size.height)
            shader.setFloatUniform("scrollVelocity", vel)
            shader.setFloatUniform("isHorizontal", if (isHorizontal) 1.0f else 0.0f)

            renderEffect = RenderEffect
                .createRuntimeShaderEffect(shader, "composable")
                .asComposeRenderEffect()
        } else {
            renderEffect = null
        }
    }
}

fun Modifier.scrollMotionBlur(
    lazyListState: LazyListState,
    enabled: Boolean = true,
    isHorizontal: Boolean = false,
): Modifier = composed {
    if (!enabled || Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
        return@composed Modifier
    }

    val animatedVelocity = remember { Animatable(0f) }

    LaunchedEffect(lazyListState) {
        var prevIndex = lazyListState.firstVisibleItemIndex
        var prevOffset = lazyListState.firstVisibleItemScrollOffset
        var prevTimeNanos = 0L

        while (isActive) {
            var newVelocityToSnap = 0f
            withFrameNanos { frameTimeNanos ->
                val currentIndex = lazyListState.firstVisibleItemIndex
                val currentOffset = lazyListState.firstVisibleItemScrollOffset
                if (prevTimeNanos != 0L) {
                    val dtMs = (frameTimeNanos - prevTimeNanos) / 1_000_000.0f
                    if (dtMs in 1f..100f) {
                        val indexDelta = currentIndex - prevIndex
                        val offsetDelta = currentOffset - prevOffset
                        val totalDelta = (indexDelta * 80f) + offsetDelta
                        val targetVelocity = (totalDelta / dtMs).coerceIn(-3f, 3f)
                        if (abs(totalDelta) > 0.1f && lazyListState.isScrollInProgress) {
                            newVelocityToSnap = animatedVelocity.value * 0.35f + targetVelocity * 0.65f
                        } else {
                            val decayed = animatedVelocity.value * 0.45f
                            newVelocityToSnap = if (abs(decayed) < 0.01f) 0f else decayed
                        }
                    } else {
                        newVelocityToSnap = 0f
                    }
                }
                prevIndex = currentIndex
                prevOffset = currentOffset
                prevTimeNanos = frameTimeNanos
            }
            animatedVelocity.snapTo(newVelocityToSnap)
        }
    }

    LaunchedEffect(lazyListState.isScrollInProgress) {
        if (!lazyListState.isScrollInProgress) {
            animatedVelocity.animateTo(0f, animationSpec = tween(60))
        }
    }

    val shader = remember { RuntimeShader(DIRECTIONAL_BLUR_AGSL) }

    Modifier.graphicsLayer {
        val vel = animatedVelocity.value
        if (abs(vel) > 0.05f) {
            shader.setFloatUniform("resolution", size.width, size.height)
            shader.setFloatUniform("scrollVelocity", vel)
            shader.setFloatUniform("isHorizontal", if (isHorizontal) 1.0f else 0.0f)

            renderEffect = RenderEffect
                .createRuntimeShaderEffect(shader, "composable")
                .asComposeRenderEffect()
        } else {
            renderEffect = null
        }
    }
}

fun Modifier.scrollMotionBlur(
    gridState: LazyGridState,
    enabled: Boolean = true,
): Modifier = composed {
    if (!enabled || Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
        return@composed Modifier
    }

    val animatedVelocity = remember { Animatable(0f) }

    LaunchedEffect(gridState) {
        var prevIndex = gridState.firstVisibleItemIndex
        var prevOffset = gridState.firstVisibleItemScrollOffset
        var prevTimeNanos = 0L

        while (isActive) {
            var newVelocityToSnap = 0f
            withFrameNanos { frameTimeNanos ->
                val currentIndex = gridState.firstVisibleItemIndex
                val currentOffset = gridState.firstVisibleItemScrollOffset
                if (prevTimeNanos != 0L) {
                    val dtMs = (frameTimeNanos - prevTimeNanos) / 1_000_000.0f
                    if (dtMs in 1f..100f) {
                        val indexDelta = currentIndex - prevIndex
                        val offsetDelta = currentOffset - prevOffset
                        val totalDelta = (indexDelta * 80f) + offsetDelta
                        val targetVelocity = (totalDelta / dtMs).coerceIn(-3f, 3f)
                        if (abs(totalDelta) > 0.1f && gridState.isScrollInProgress) {
                            newVelocityToSnap = animatedVelocity.value * 0.35f + targetVelocity * 0.65f
                        } else {
                            val decayed = animatedVelocity.value * 0.45f
                            newVelocityToSnap = if (abs(decayed) < 0.01f) 0f else decayed
                        }
                    } else {
                        newVelocityToSnap = 0f
                    }
                }
                prevIndex = currentIndex
                prevOffset = currentOffset
                prevTimeNanos = frameTimeNanos
            }
            animatedVelocity.snapTo(newVelocityToSnap)
        }
    }

    LaunchedEffect(gridState.isScrollInProgress) {
        if (!gridState.isScrollInProgress) {
            animatedVelocity.animateTo(0f, animationSpec = tween(60))
        }
    }

    val shader = remember { RuntimeShader(DIRECTIONAL_BLUR_AGSL) }

    Modifier.graphicsLayer {
        val vel = animatedVelocity.value
        if (abs(vel) > 0.05f) {
            shader.setFloatUniform("resolution", size.width, size.height)
            shader.setFloatUniform("scrollVelocity", vel)
            shader.setFloatUniform("isHorizontal", 0.0f)

            renderEffect = RenderEffect
                .createRuntimeShaderEffect(shader, "composable")
                .asComposeRenderEffect()
        } else {
            renderEffect = null
        }
    }
}

fun Modifier.scrollMotionBlur(
    pagerState: PagerState,
    enabled: Boolean = true,
): Modifier = composed {
    if (!enabled || Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
        return@composed Modifier
    }

    val animatedVelocity = remember { Animatable(0f) }

    LaunchedEffect(pagerState) {
        var prevPage = pagerState.currentPage
        var prevFraction = pagerState.currentPageOffsetFraction
        var prevTimeNanos = 0L

        while (isActive) {
            var newVelocityToSnap = 0f
            withFrameNanos { frameTimeNanos ->
                val currentPage = pagerState.currentPage
                val currentFraction = pagerState.currentPageOffsetFraction
                if (prevTimeNanos != 0L) {
                    val dtMs = (frameTimeNanos - prevTimeNanos) / 1_000_000.0f
                    if (dtMs in 1f..100f) {
                        val totalPos = currentPage + currentFraction
                        val prevPos = prevPage + prevFraction
                        val delta = (totalPos - prevPos) * 400f
                        val targetVelocity = (delta / dtMs).coerceIn(-3f, 3f)
                        if (abs(delta) > 0.1f && pagerState.isScrollInProgress) {
                            newVelocityToSnap = animatedVelocity.value * 0.35f + targetVelocity * 0.65f
                        } else {
                            val decayed = animatedVelocity.value * 0.45f
                            newVelocityToSnap = if (abs(decayed) < 0.01f) 0f else decayed
                        }
                    } else {
                        newVelocityToSnap = 0f
                    }
                }
                prevPage = currentPage
                prevFraction = currentFraction
                prevTimeNanos = frameTimeNanos
            }
            animatedVelocity.snapTo(newVelocityToSnap)
        }
    }

    LaunchedEffect(pagerState.isScrollInProgress) {
        if (!pagerState.isScrollInProgress) {
            animatedVelocity.animateTo(0f, animationSpec = tween(60))
        }
    }

    val shader = remember { RuntimeShader(DIRECTIONAL_BLUR_AGSL) }

    Modifier.graphicsLayer {
        val vel = animatedVelocity.value
        if (abs(vel) > 0.05f) {
            shader.setFloatUniform("resolution", size.width, size.height)
            shader.setFloatUniform("scrollVelocity", vel)
            shader.setFloatUniform("isHorizontal", 1.0f)

            renderEffect = RenderEffect
                .createRuntimeShaderEffect(shader, "composable")
                .asComposeRenderEffect()
        } else {
            renderEffect = null
        }
    }
}

