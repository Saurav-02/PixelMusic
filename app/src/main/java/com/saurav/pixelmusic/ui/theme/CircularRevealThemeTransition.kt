package com.saurav.pixelmusic.ui.theme

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.graphics.Bitmap
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.PixelCopy
import android.view.View
import android.view.Window
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.hypot
import kotlin.math.max

val LocalThemeTransitionController = staticCompositionLocalOf<ThemeTransitionController?> { null }

class CircularRevealShape(
    private val radius: Float,
    private val center: Offset
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        if (radius <= 0f) {
            return Outline.Generic(Path())
        }
        val path = Path().apply {
            addOval(
                Rect(
                    left = center.x - radius,
                    top = center.y - radius,
                    right = center.x + radius,
                    bottom = center.y + radius
                )
            )
        }
        return Outline.Generic(path)
    }
}

class ThemeTransitionController {
    var previousBitmap by mutableStateOf<ImageBitmap?>(null)
        private set
    var isTransitioning by mutableStateOf(false)
        private set
    var revealRadius by mutableFloatStateOf(0f)
        private set
    var revealCenter by mutableStateOf(Offset.Zero)
        private set
    var lastTouchOffset by mutableStateOf<Offset?>(null)

    private var animJob: Job? = null

    private fun findWindow(view: View): Window? {
        var ctx: Context? = view.context
        while (ctx is ContextWrapper) {
            if (ctx is Activity) return ctx.window
            ctx = ctx.baseContext
        }
        return null
    }

    private fun startReveal(
        resolvedOrigin: Offset,
        maxRadius: Float,
        scope: CoroutineScope,
        change: () -> Unit
    ) {
        revealCenter = resolvedOrigin
        revealRadius = 0f
        isTransitioning = true

        change()

        animJob = scope.launch {
            val animatable = Animatable(0f)
            animatable.animateTo(
                targetValue = maxRadius,
                animationSpec = tween(
                    durationMillis = 440,
                    easing = FastOutSlowInEasing
                )
            ) {
                revealRadius = value
            }
            isTransitioning = false
            previousBitmap = null
        }
    }

    fun transitionThemeChange(
        view: View,
        origin: Offset? = null,
        scope: CoroutineScope,
        change: () -> Unit
    ) {
        val root = view.rootView ?: view
        val w = root.width
        val h = root.height
        if (w <= 0 || h <= 0) {
            change()
            return
        }

        animJob?.cancel()

        val resolvedOrigin = origin
            ?: lastTouchOffset
            ?: Offset(w * 0.5f, h * 0.5f)

        val maxDistX = max(resolvedOrigin.x, w - resolvedOrigin.x)
        val maxDistY = max(resolvedOrigin.y, h - resolvedOrigin.y)
        val maxRadius = hypot(maxDistX.toDouble(), maxDistY.toDouble()).toFloat() + 32f

        val window = findWindow(view)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && window != null) {
            val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            try {
                PixelCopy.request(
                    window,
                    bitmap,
                    { copyResult ->
                        if (copyResult == PixelCopy.SUCCESS) {
                            previousBitmap = bitmap.asImageBitmap()
                            startReveal(resolvedOrigin, maxRadius, scope, change)
                        } else {
                            Log.w("CircularReveal", "PixelCopy failed with result: $copyResult")
                            previousBitmap = null
                            change()
                        }
                    },
                    Handler(Looper.getMainLooper())
                )
                return
            } catch (e: Throwable) {
                Log.e("CircularReveal", "PixelCopy invocation threw error", e)
            }
        }

        try {
            val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            val canvas = android.graphics.Canvas(bitmap)
            root.draw(canvas)
            previousBitmap = bitmap.asImageBitmap()
            startReveal(resolvedOrigin, maxRadius, scope, change)
        } catch (e: Throwable) {
            Log.e("CircularReveal", "Software snapshot capture failed", e)
            previousBitmap = null
            change()
        }
    }
}

@Composable
fun rememberThemeTransitionController(): ThemeTransitionController {
    return remember { ThemeTransitionController() }
}

@Composable
fun CircularThemeRevealHost(
    controller: ThemeTransitionController,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        event.changes.firstOrNull()?.position?.let { pos ->
                            controller.lastTouchOffset = pos
                        }
                    }
                }
            }
    ) {
        if (controller.isTransitioning && controller.previousBitmap != null) {
            Image(
                bitmap = controller.previousBitmap!!,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.FillBounds
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (controller.isTransitioning) {
                        Modifier.graphicsLayer {
                            val r = controller.revealRadius
                            val c = controller.revealCenter
                            shape = CircularRevealShape(radius = r, center = c)
                            clip = true
                        }
                    } else {
                        Modifier
                    }
                )
        ) {
            content()
        }
    }
}
