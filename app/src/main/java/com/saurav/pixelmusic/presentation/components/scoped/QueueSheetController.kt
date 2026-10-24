package com.saurav.pixelmusic.presentation.components.scoped

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.MonotonicFrameClock
import androidx.compose.runtime.withFrameNanos
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield
import kotlin.coroutines.coroutineContext
import kotlin.math.abs

/**
 * Controls queue sheet visibility, drag and snapping decisions.
 * Behavior mirrors the previous inline logic in UnifiedPlayerSheet.
 */
internal class QueueSheetController(
    private val scope: CoroutineScope,
    private val queueSheetOffset: Animatable<Float, AnimationVector1D>,
    private val hiddenOffsetProvider: () -> Float,
    private val allowInteractionProvider: () -> Boolean,
    private val minFlingTravelPxProvider: () -> Float,
    private val dragThresholdPxProvider: () -> Float,
    private val showQueueSheetProvider: () -> Boolean,
    private val onShowQueueSheetChange: (Boolean) -> Unit
) {
    private var dragOffsetCache: Float? = null
    private var pendingDragTarget: Float? = null
    private var dragSnapJob: Job? = null

    private fun resetDragPipeline() {
        dragOffsetCache = null
        pendingDragTarget = null
        dragSnapJob?.cancel()
        dragSnapJob = null
    }

    private fun launchDragSnapLoopIfNeeded() {
        if (dragSnapJob?.isActive == true) return

        dragSnapJob = scope.launch {
            while (isActive) {
                val target = pendingDragTarget ?: break
                pendingDragTarget = null
                queueSheetOffset.snapTo(target)

                // Coalesce high-frequency deltas into frame-paced updates.
                if (coroutineContext[MonotonicFrameClock] != null) {
                    withFrameNanos { }
                } else {
                    yield()
                }
            }
        }
    }

    suspend fun syncOffsetToVisibility() {
        val hiddenOffset = hiddenOffsetProvider()
        if (hiddenOffset <= 0f) return
        // Don't yank the sheet while the user is actively dragging it.
        if (dragOffsetCache != null) return
        // When open was requested, always settle fully open. Preserving a stale
        // offset here could leave the sheet visually closed while showQueueSheet
        // is true, which sticks the scrim/blur on screen with no sheet.
        val targetOffset = if (showQueueSheetProvider()) 0f else hiddenOffset
        if (queueSheetOffset.value != targetOffset) {
            queueSheetOffset.snapTo(targetOffset)
        }
    }

    suspend fun syncCollapsedWhenHidden() {
        val hiddenOffset = hiddenOffsetProvider()
        if (!showQueueSheetProvider() && hiddenOffset > 0f && queueSheetOffset.value != hiddenOffset) {
            queueSheetOffset.snapTo(hiddenOffset)
        }
    }

    suspend fun forceCollapseIfInteractionDisabled() {
        if (allowInteractionProvider()) return
        onShowQueueSheetChange(false)
        val hiddenOffset = hiddenOffsetProvider()
        if (hiddenOffset > 0f) {
            queueSheetOffset.snapTo(hiddenOffset)
        }
    }

    suspend fun animateTo(targetExpanded: Boolean) {
        resetDragPipeline()
        val hiddenOffset = hiddenOffsetProvider()
        if (hiddenOffset == 0f) {
            onShowQueueSheetChange(targetExpanded)
            return
        }
        try {
            val target = if (targetExpanded) 0f else hiddenOffset
            val shouldPrewarmFirstFrame = targetExpanded && !showQueueSheetProvider()
            onShowQueueSheetChange(true)
            if (shouldPrewarmFirstFrame) {
                queueSheetOffset.snapTo(hiddenOffset)
                if (coroutineContext[MonotonicFrameClock] != null) {
                    withFrameNanos { }
                } else {
                    yield()
                }
            }
            val travelFraction = if (hiddenOffset > 0f) {
                (abs(queueSheetOffset.value - target) / hiddenOffset).coerceIn(0f, 1f)
            } else {
                1f
            }
            val durationMillis = if (targetExpanded) {
                (220f + (120f * travelFraction)).toInt()
            } else {
                (190f + (110f * travelFraction)).toInt()
            }
            queueSheetOffset.animateTo(
                targetValue = target,
                animationSpec = tween(
                    durationMillis = durationMillis,
                    easing = FastOutSlowInEasing
                )
            )
        } finally {
            // Always settle the visible state, even if the animation was cancelled
            // (e.g. by a rapid second tap). Otherwise showQueueSheet can stick
            // at true with the sheet off-screen, leaving the blur scrim stuck.
            onShowQueueSheetChange(targetExpanded)
        }
    }

    fun animate(targetExpanded: Boolean) {
        if (!allowInteractionProvider() && targetExpanded) return
        scope.launch { animateTo(targetExpanded && allowInteractionProvider()) }
    }

    fun beginDrag() {
        val hiddenOffset = hiddenOffsetProvider()
        if (hiddenOffset == 0f || !allowInteractionProvider()) return
        resetDragPipeline()
        dragOffsetCache = queueSheetOffset.value
        onShowQueueSheetChange(true)
        scope.launch { queueSheetOffset.stop() }
    }

    fun dragBy(dragAmount: Float) {
        val hiddenOffset = hiddenOffsetProvider()
        if (hiddenOffset == 0f || !allowInteractionProvider()) return
        val baseOffset = dragOffsetCache ?: queueSheetOffset.value
        val newOffset = (baseOffset + dragAmount).coerceIn(0f, hiddenOffset)
        dragOffsetCache = newOffset
        pendingDragTarget = newOffset
        launchDragSnapLoopIfNeeded()
    }

    fun endDrag(totalDrag: Float, velocity: Float) {
        val hiddenOffset = hiddenOffsetProvider()
        if (hiddenOffset == 0f || !allowInteractionProvider()) return

        // Freeze pending deltas before deciding snap target.
        val settledOffset = pendingDragTarget ?: dragOffsetCache ?: queueSheetOffset.value
        resetDragPipeline()

        val isFastUpward = velocity < -520f
        val isFastDownward = velocity > 700f
        val minFlingTravelPx = minFlingTravelPxProvider()
        val hasMeaningfulUpwardTravel = totalDrag < -minFlingTravelPx
        // Quick upward flicks on full player can be short in travel but high in intent.
        val hasQuickUpwardTravel = totalDrag < -(minFlingTravelPx * 0.35f)
        val shouldExpandFromQuickFling = isFastUpward && hasQuickUpwardTravel
        val dragThresholdPx = dragThresholdPxProvider()
        val shouldExpand = shouldExpandFromQuickFling ||
            (isFastUpward && hasMeaningfulUpwardTravel) ||
            (!isFastDownward && (
                settledOffset < hiddenOffset - dragThresholdPx ||
                    totalDrag < -dragThresholdPx
                ))

        animate(shouldExpand)
    }
}
