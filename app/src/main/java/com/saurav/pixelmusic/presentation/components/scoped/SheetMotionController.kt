package com.saurav.pixelmusic.presentation.components.scoped

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.MutatorMutex
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

/**
 * Centralizes sheet motion updates so animation/snap logic lives in one place.
 * This keeps behavior stable while enabling an incremental V2 rewrite.
 */
internal class SheetMotionController(
    private val translationY: Animatable<Float, AnimationVector1D>,
    private val expansionFraction: Animatable<Float, AnimationVector1D>,
    private val dockBounceFraction: Animatable<Float, AnimationVector1D>,
    private val mutex: MutatorMutex,
    private val defaultAnimationSpec: AnimationSpec<Float>,
    private val fractionAnimationSpec: AnimationSpec<Float> = defaultAnimationSpec,
    private val expandedY: Float = 0f
) {
    private val dockBounceSpec = spring<Float>(
        dampingRatio = 0.58f,
        stiffness = 240f,
        visibilityThreshold = 0.002f
    )
    private val dockResetSpec = spring<Float>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = 400f,
        visibilityThreshold = 0.002f
    )

    @Volatile
    private var activeTargetExpanded: Boolean? = null

    suspend fun animateTo(
        targetExpanded: Boolean,
        canExpand: Boolean,
        collapsedY: Float,
        animationSpec: AnimationSpec<Float> = defaultAnimationSpec,
        initialVelocity: Float = 0f
    ) {
        val targetFraction = if (canExpand && targetExpanded) 1f else 0f
        val targetY = if (targetExpanded) expandedY else collapsedY
        val velocityScale = (collapsedY - expandedY).coerceAtLeast(1f)

        if (
            translationY.value == targetY &&
            expansionFraction.value == targetFraction &&
            dockBounceFraction.value == 0f &&
            !translationY.isRunning &&
            !expansionFraction.isRunning &&
            !dockBounceFraction.isRunning
        ) {
            return
        }

        // Do not let a secondary state-change LaunchedEffect (initialVelocity == 0f)
        // cancel and restart an in-flight gesture release animation targeting the same state.
        if (
            initialVelocity == 0f &&
            activeTargetExpanded == targetExpanded &&
            (translationY.isRunning || expansionFraction.isRunning || dockBounceFraction.isRunning)
        ) {
            return
        }

        val resolvedFractionSpec = if (animationSpec === defaultAnimationSpec) {
            fractionAnimationSpec
        } else {
            animationSpec
        }
        // Dampen and cap fling release velocity so drag-release stays smooth and unhurried.
        val maxVelocityY = velocityScale * 0.85f
        val effectiveVelocityY = (initialVelocity * 0.45f).coerceIn(-maxVelocityY, maxVelocityY)
        // translationY increases downward (+Y) while expansionFraction decreases downward (-fraction).
        val effectiveFractionVelocity = -effectiveVelocityY / velocityScale
        val startFraction = expansionFraction.value

        mutex.mutate {
            activeTargetExpanded = targetExpanded
            try {
                coroutineScope {
                    var dockBounceTriggered = false
                    if (targetExpanded && (dockBounceFraction.value != 0f || dockBounceFraction.isRunning)) {
                        launch {
                            dockBounceFraction.animateTo(
                                targetValue = 0f,
                                animationSpec = dockResetSpec
                            )
                        }
                    }
                    launch {
                        translationY.animateTo(
                            targetValue = targetY,
                            initialVelocity = effectiveVelocityY,
                            animationSpec = animationSpec
                        )
                    }
                    launch {
                        expansionFraction.animateTo(
                            targetValue = targetFraction,
                            initialVelocity = effectiveFractionVelocity,
                            animationSpec = resolvedFractionSpec
                        ) {
                            if (
                                !targetExpanded &&
                                !dockBounceTriggered &&
                                startFraction >= 0.12f &&
                                value <= 0.035f
                            ) {
                                dockBounceTriggered = true
                                val intensity = 0.55f + 0.45f * startFraction.coerceIn(0f, 1f)
                                launch {
                                    dockBounceFraction.snapTo(0f)
                                    dockBounceFraction.animateTo(
                                        targetValue = 0f,
                                        initialVelocity = 30.5f * intensity,
                                        animationSpec = dockBounceSpec
                                    )
                                }
                            }
                        }
                    }
                }
            } finally {
                if (activeTargetExpanded == targetExpanded) {
                    activeTargetExpanded = null
                }
            }
        }
    }

    suspend fun stop() {
        activeTargetExpanded = null
        translationY.stop()
        expansionFraction.stop()
        dockBounceFraction.stop()
        if (dockBounceFraction.value != 0f) {
            dockBounceFraction.snapTo(0f)
        }
    }

    suspend fun snapTo(translationYValue: Float, expansionFractionValue: Float) {
        mutex.mutate {
            activeTargetExpanded = null
            translationY.snapTo(translationYValue)
            expansionFraction.snapTo(expansionFractionValue)
            if (dockBounceFraction.value != 0f) {
                dockBounceFraction.snapTo(0f)
            }
        }
    }

    suspend fun snapCollapsed(collapsedY: Float) {
        snapTo(
            translationYValue = collapsedY,
            expansionFractionValue = 0f
        )
    }

    suspend fun syncToExpansion(collapsedY: Float) {
        val adjustedY = collapsedY + (expandedY - collapsedY) * expansionFraction.value
        if (translationY.value == adjustedY && !translationY.isRunning) return
        mutex.mutate {
            translationY.snapTo(adjustedY)
        }
    }
}
