package com.saurav.pixelmusic.presentation.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

/**
 * Material You Expressive grouped list card styling matching the Now Playing
 * action menu: rounded outer corners (24.dp), softly rounded inner corners (4.dp),
 * and tight 2.dp separation between adjacent items in a group.
 */
val ExpressiveGroupedCardSpacing = 2.dp

val ExpressiveSingleCardShape = RoundedCornerShape(24.dp)
val ExpressiveTopCardShape = RoundedCornerShape(
    topStart = 24.dp,
    topEnd = 24.dp,
    bottomStart = 4.dp,
    bottomEnd = 4.dp
)
val ExpressiveMiddleCardShape = RoundedCornerShape(4.dp)
val ExpressiveBottomCardShape = RoundedCornerShape(
    topStart = 4.dp,
    topEnd = 4.dp,
    bottomStart = 24.dp,
    bottomEnd = 24.dp
)

fun expressiveGroupedCardShape(index: Int, totalCount: Int): Shape {
    if (totalCount <= 1) return ExpressiveSingleCardShape
    return when (index) {
        0 -> ExpressiveTopCardShape
        totalCount - 1 -> ExpressiveBottomCardShape
        else -> ExpressiveMiddleCardShape
    }
}

/**
 * Smoothly interpolates every corner of [baseShape] (e.g. an expressive grouped
 * card with 24.dp outer / 4.dp inner corners) to a full pill (`min(w, h) / 2`)
 * as [progress] goes from `0f` to `1f`. Eliminates corner snapping on select or
 * deselect and keeps card morphing 100% synchronized with album art & colors.
 */
class ExpressiveMorphingCardShape(
    private val baseShape: RoundedCornerShape,
    private val progress: Float
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val maxRadius = minOf(size.width, size.height) * 0.5f
        val t = progress.coerceIn(0f, 1f)
        val tl = baseShape.topStart.toPx(size, density).let { it + (maxRadius - it) * t }
        val tr = baseShape.topEnd.toPx(size, density).let { it + (maxRadius - it) * t }
        val br = baseShape.bottomEnd.toPx(size, density).let { it + (maxRadius - it) * t }
        val bl = baseShape.bottomStart.toPx(size, density).let { it + (maxRadius - it) * t }
        return Outline.Rounded(
            RoundRect(
                left = 0f,
                top = 0f,
                right = size.width,
                bottom = size.height,
                topLeftCornerRadius = CornerRadius(tl, tl),
                topRightCornerRadius = CornerRadius(tr, tr),
                bottomRightCornerRadius = CornerRadius(br, br),
                bottomLeftCornerRadius = CornerRadius(bl, bl)
            )
        )
    }
}

/**
 * Smooth, draw-phase scroll entrance transition for LazyColumn list items.
 * Waits until the item actually crosses into the visible viewport above the
 * bottom mini player / navigation bar before running a damped spring to 1.0,
 * so the animation is clearly visible while scrolling and always settles cleanly
 * at 100% scale and opacity when resting on screen.
 */
fun Modifier.expressiveListItemScrollAnimation(
    listState: LazyListState? = null
): Modifier = composed {
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val bottomTriggerInsetPx = remember(density) { with(density) { 84.dp.toPx() } }

    val progress = remember { Animatable(0f) }
    var hasTriggered by remember { mutableStateOf(false) }
    val enterDirection = remember { mutableFloatStateOf(1f) }

    this
        .onGloballyPositioned { coordinates ->
            if (hasTriggered) return@onGloballyPositioned
            val parent = coordinates.parentLayoutCoordinates ?: return@onGloballyPositioned
            val viewportHeight = parent.size.height.toFloat()
            if (viewportHeight <= 0f) return@onGloballyPositioned

            val topY = parent.localPositionOf(coordinates, Offset.Zero).y
            val triggerLineY = (viewportHeight - bottomTriggerInsetPx).coerceAtLeast(1f)

            if (topY < triggerLineY) {
                hasTriggered = true
                enterDirection.floatValue = if (listState?.lastScrolledBackward == true) -1f else 1f
                scope.launch {
                    progress.animateTo(
                        targetValue = 1f,
                        animationSpec = spring(
                            dampingRatio = 0.84f,
                            stiffness = 260f,
                            visibilityThreshold = 0.002f
                        )
                    )
                }
            }
        }
        .graphicsLayer {
            val p = progress.value
            alpha = 0.25f + 0.75f * p
            val scale = 0.90f + 0.10f * p
            scaleX = scale
            scaleY = scale
            translationY = (1f - p) * 24.dp.toPx() * enterDirection.floatValue
            transformOrigin = TransformOrigin(0.5f, 0.5f)
        }
}
