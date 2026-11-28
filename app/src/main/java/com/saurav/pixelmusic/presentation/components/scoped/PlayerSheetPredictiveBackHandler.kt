package com.saurav.pixelmusic.presentation.components.scoped

import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.PredictiveBackHandler
import androidx.annotation.OptIn
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.media3.common.util.UnstableApi
import com.saurav.pixelmusic.presentation.viewmodel.PlayerSheetState
import com.saurav.pixelmusic.presentation.viewmodel.PlayerViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException

/**
 * How far the sheet is allowed to collapse while the finger is still dragging
 * (0f..1f of the full collapse). Kept small on purpose: the Now Playing content
 * (art, song info, controls) runs a heavy morph choreography driven by the expansion
 * fraction, so dragging through the whole range makes everything inside fly around.
 * With a small peek the content stays still and only the sheet gently shrinks. The real
 * collapse (with the full morph) only runs after the gesture is committed.
 * Tune between 0.1f and 0.3f.
 */
private const val PEEK_MAX_FRACTION = 0.2f

@OptIn(UnstableApi::class)
@Composable
internal fun PlayerSheetPredictiveBackHandler(
    enabled: Boolean,
    playerViewModel: PlayerViewModel,
    @Suppress("UNUSED_PARAMETER") sheetCollapsedTargetY: Float,
    @Suppress("UNUSED_PARAMETER") sheetExpandedTargetY: Float,
    @Suppress("UNUSED_PARAMETER") sheetMotionController: SheetMotionController,
    animationDurationMs: Int,
    onSwipeEdgeChanged: (Int?) -> Unit,
    registrationKey: Any?
) {
    val scope = rememberCoroutineScope()
    // Settle animation of the previous gesture. A new gesture must kill it, otherwise it keeps
    // writing the fraction (and ends with expandPlayerSheet(), which resets it to 0) while the
    // new gesture is still being dragged.
    val settleJob = remember { arrayOfNulls<Job>(1) }

    key(registrationKey) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            PredictiveBackHandler(enabled = enabled) { progressFlow ->
                settleJob[0]?.cancel()
                settleJob[0] = null
                try {
                    progressFlow.collect { backEvent ->
                        onSwipeEdgeChanged(backEvent.swipeEdge)
                        playerViewModel.updatePredictiveBackCollapseFraction(
                            backEvent.progress * PEEK_MAX_FRACTION
                        )
                    }

                    // Committed: start the real collapse (sheet animatables animate from
                    // expanded -> collapsed) and fade the peek fraction out over the same
                    // duration. Visual = anim * (1 - fraction), so it blends with no jump.
                    settleJob[0] = scope.launch {
                        val start = playerViewModel.predictiveBackCollapseFraction.value
                        playerViewModel.collapsePlayerSheet(resetPredictiveState = false)
                        if (start > 0f) {
                            Animatable(start).animateTo(
                                targetValue = 0f,
                                animationSpec = tween(animationDurationMs)
                            ) {
                                playerViewModel.updatePredictiveBackCollapseFraction(this.value)
                            }
                        }
                        playerViewModel.resetPredictiveBackState()
                    }
                } catch (_: CancellationException) {
                    // Cancelled: ease back to fully expanded.
                    settleJob[0] = scope.launch {
                        val start = playerViewModel.predictiveBackCollapseFraction.value
                        if (start > 0f) {
                            Animatable(start).animateTo(
                                targetValue = 0f,
                                animationSpec = tween(
                                    (animationDurationMs * (start / PEEK_MAX_FRACTION))
                                        .toInt()
                                        .coerceIn(90, animationDurationMs)
                                )
                            ) {
                                playerViewModel.updatePredictiveBackCollapseFraction(this.value)
                            }
                        }

                        if (playerViewModel.sheetState.value == PlayerSheetState.EXPANDED) {
                            playerViewModel.expandPlayerSheet()
                        } else {
                            playerViewModel.collapsePlayerSheet()
                        }

                        onSwipeEdgeChanged(null)
                    }
                }
            }
        } else {
            BackHandler(enabled = enabled) {
                playerViewModel.collapsePlayerSheet()
            }
        }
    }
}
