package com.saurav.pixelmusic.presentation.components.lyrics.pixelmusic

import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.layer.CompositingStrategy
import androidx.compose.ui.graphics.RenderEffect
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onPlaced
import kotlin.math.roundToInt

// how strongly white soft-lights an upcoming line, the brightest resting one; the dimmer states keep their share of it
internal const val SoftLightFill = 0.6f

// a copy this faint isn't drawn at all, so a settled line pays for one layer, not two
private const val HiddenCopyAlpha = 0.002f

// soft light only exists as a layer blend from android 10, older phones keep the plain dimmed lines
internal val SoftLightSupported: Boolean
    get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q

// the lyrics page blending straight onto the player's background; any layer between the lines and the background would blend them onto nothing, so each row draws the list's edge fade itself
@Stable
class LyricsBackdropBlend internal constructor(
    private val topFadeEnd: Float,
    private val bottomFadeStart: Float,
    // reads whatever moves the rows (scroll, the stagger, the page growing in), so a row's fade follows it
    private val track: () -> Unit,
) {
    internal var list: LayoutCoordinates? = null

    // the fade across a row in its own pixels, or null while the row sits in the clear middle of the list
    internal fun maskFor(scope: DrawScope, row: LayoutCoordinates?): Brush? {
        track()
        val list = list?.takeIf { it.isAttached } ?: return null
        val rowCoords = row?.takeIf { it.isAttached } ?: return null
        val listHeight = list.size.height.toFloat()
        val rowHeight = scope.size.height
        if (listHeight <= 0f || rowHeight <= 0f) return null
        val top = list.localPositionOf(rowCoords, Offset.Zero).y
        val bottom = list.localPositionOf(rowCoords, Offset(0f, rowHeight)).y
        if (top >= listHeight * topFadeEnd && bottom <= listHeight * bottomFadeStart) return null
        if (bottom - top < 1f) return null
        // list pixels to row pixels, so a row that's zoomed or still sliding in gets the fade where it really is
        val rowPerList = rowHeight / (bottom - top)
        return Brush.verticalGradient(
            0f to Color.Transparent,
            topFadeEnd to Color.Black,
            bottomFadeStart to Color.Black,
            1f to Color.Transparent,
            startY = -top * rowPerList,
            endY = (listHeight - top) * rowPerList,
        )
    }
}

// the list's edge fade on a row that draws normally; it only takes a buffer of its own while it's in the fade
@Composable
internal fun Modifier.lyricsEdgeFade(blend: LyricsBackdropBlend?): Modifier {
    if (blend == null) return this
    val layer = rememberGraphicsLayer()
    val coords = remember { arrayOfNulls<LayoutCoordinates>(1) }
    return this
        .onPlaced { coords[0] = it }
        .drawWithContent {
            val mask = blend.maskFor(this, coords[0])
            if (mask == null) {
                drawContent()
            } else {
                drawCopy(layer, alpha = 1f, blendMode = BlendMode.SrcOver, blur = null, mask = mask)
            }
        }
}

// a lyric line as up to two copies: white soft-lit onto the background for the resting look, plain for the sung one; both only while it changes
@Composable
internal fun Modifier.softLightLine(
    blend: LyricsBackdropBlend,
    softAlpha: () -> Float,
    litAlpha: () -> Float,
    blur: () -> RenderEffect?,
    // the line's own zoom and drop, read so its fade moves with them
    moving: () -> Unit,
    // how much the soft copy lifts faint words back up to solid white before lighting the background with them, 1 for none
    softBoost: () -> Float = { 1f },
): Modifier {
    val soft = rememberGraphicsLayer()
    val lit = rememberGraphicsLayer()
    val boosted = rememberGraphicsLayer()
    val coords = remember { arrayOfNulls<LayoutCoordinates>(1) }
    return this
        .onPlaced { coords[0] = it }
        .drawWithContent {
            moving()
            val mask = blend.maskFor(this, coords[0])
            val effect = blur()
            val softShare = softAlpha()
            val litShare = litAlpha()
            if (softShare > HiddenCopyAlpha) {
                drawCopy(soft, softShare, BlendMode.Softlight, effect, mask, boostFilterFor(softBoost()), boosted)
            }
            if (litShare > HiddenCopyAlpha) drawCopy(lit, litShare, BlendMode.SrcOver, effect, mask)
        }
}

private fun ContentDrawScope.drawCopy(
    layer: GraphicsLayer,
    alpha: Float,
    blendMode: BlendMode,
    blur: RenderEffect?,
    mask: Brush?,
    boost: ColorFilter? = null,
    boostLayer: GraphicsLayer? = null,
) {
    layer.alpha = alpha
    layer.blendMode = blendMode
    layer.renderEffect = blur
    // the fade wipes out whatever is under it, so the copy needs its own buffer even when nothing else asks for one
    layer.compositingStrategy = if (mask != null) CompositingStrategy.Offscreen else CompositingStrategy.Auto
    layer.record {
        if (boost != null && boostLayer != null) {
            // lifted inside, since on the copy itself the lift ran after its fade, share and blur and undid them: lines stayed bright in the top fade, then snapped dim
            boostLayer.colorFilter = boost
            boostLayer.record { this@drawCopy.drawContent() }
            drawLayer(boostLayer)
        } else {
            this@drawCopy.drawContent()
        }
        if (mask != null) drawRect(brush = mask, blendMode = BlendMode.DstIn)
    }
    drawLayer(layer)
}

// boosts come in eighths, so a line easing its boost down reuses a handful of shared filters instead of making one a frame
private const val BoostStepsPerUnit = 8f
private val BoostFilters = arrayOfNulls<ColorFilter>(64)

// the white filter for one boost, or null for none
private fun boostFilterFor(boost: Float): ColorFilter? {
    val step = ((boost - 1f) * BoostStepsPerUnit).roundToInt().coerceAtMost(BoostFilters.lastIndex)
    if (step <= 0) return null
    BoostFilters[step]?.let { return it }
    val alphaScale = 1f + step / BoostStepsPerUnit
    // every pixel white, its alpha raised by the boost (clamped at opaque), so a faint glyph lights the background like a solid one
    return ColorFilter.colorMatrix(
        ColorMatrix(
            floatArrayOf(
                0f, 0f, 0f, 0f, 255f,
                0f, 0f, 0f, 0f, 255f,
                0f, 0f, 0f, 0f, 255f,
                0f, 0f, 0f, alphaScale, 0f,
            )
        )
    ).also { BoostFilters[step] = it }
}
