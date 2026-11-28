// PixelMusicShims.kt
// The few small helpers PixelMusic's lyric renderer leans on that live outside its lyric files.

package com.saurav.pixelmusic.presentation.components.lyrics.pixelmusic

import android.graphics.RenderNode
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.Animatable as FloatAnimatable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.dp
import com.saurav.pixelmusic.lyrics.PixelMusicLyricsStyle

/** PixelMusic's name for the four animated lyric styles; PixelMusic's own enum carries them. */
typealias LyricsMotionStyle = PixelMusicLyricsStyle

/** Left edge of the lyric text, level with the player's content edge. */
val LyricsHeaderArtX = 24.dp

object AppMotion {
    /** Element blur (lyric line depth blur) on or off. */
    @Volatile
    var elementBlur: Boolean = true
}

/** Eases to [target] only when it changes, so a mounted line costs no animation until then. */
@Composable
fun animateFloatOnChange(target: Float, spec: AnimationSpec<Float>): State<Float> {
    val anim = remember { FloatAnimatable(target) }
    LaunchedEffect(target) {
        if (anim.targetValue != target) anim.animateTo(target, spec)
    }
    return anim.asState()
}

@Composable
fun animateColorOnChange(target: Color, spec: AnimationSpec<Color>): State<Color> {
    val anim = remember { Animatable(target) }
    LaunchedEffect(target) {
        if (anim.targetValue != target) anim.animateTo(target, spec)
    }
    return anim.asState()
}

@RequiresApi(Build.VERSION_CODES.Q)
private fun damageNode(width: Int, height: Int): Any =
    RenderNode("DamageBoundary").apply {
        setPosition(0, 0, width, height)
        // the whole point: the renderer only bounds a change by a node's size when the node clips
        // to it. Compose's own layers clip by outline instead, so to the renderer a change in any
        // of them is a change to the entire screen
        setClipToBounds(true)
    }

@RequiresApi(Build.VERSION_CODES.Q)
private fun damageNodeBeginRecording(node: Any): android.graphics.Canvas = (node as RenderNode).beginRecording()

@RequiresApi(Build.VERSION_CODES.Q)
private fun damageNodeEndRecording(node: Any) {
    (node as RenderNode).endRecording()
}

@RequiresApi(Build.VERSION_CODES.Q)
private fun android.graphics.Canvas.drawDamageNode(node: Any) {
    drawRenderNode(node as RenderNode)
}

/**
 * Keeps whatever animates inside this from counting as a change to the rest of the screen.
 * Ported verbatim from PixelMusic's GlassKit.kt.
 */
fun Modifier.damageBoundary(): Modifier =
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
        this
    } else {
        this.drawWithCache {
            val width = size.width.toInt()
            val height = size.height.toInt()
            val node = if (width > 0 && height > 0) damageNode(width, height) else null
            onDrawWithContent {
                val target = drawContext.canvas
                if (node == null || !target.nativeCanvas.isHardwareAccelerated) {
                    drawContent()
                    return@onDrawWithContent
                }
                drawContext.canvas = Canvas(damageNodeBeginRecording(node))
                try {
                    drawContent()
                } finally {
                    drawContext.canvas = target
                    damageNodeEndRecording(node)
                }
                target.nativeCanvas.drawDamageNode(node)
            }
        }
            // a plain layer inside, so content that draws itself repaints only this rectangle
            .graphicsLayer()
    }

/** The colour of the sung words. PixelMusic draws them white; PixelMusic hands in its own content colour. */
val LocalLyricsHighlight = androidx.compose.runtime.compositionLocalOf { Color.White }

// the lyrics page's recenter scroll; the instrumental dots collapse on the same curve
internal const val LyricsRecenterDurationMs = 720
internal val LyricsRecenterEasing = androidx.compose.animation.core.CubicBezierEasing(0.5f, 0.45f, 0f, 1f)
internal val LyricsSmoothRecenterEasing = androidx.compose.animation.core.CubicBezierEasing(0.42f, 0f, 0.16f, 1f)
