package com.saurav.pixelmusic.presentation.components

import android.os.Build
import android.window.OnBackInvokedCallback
import android.window.OnBackInvokedDispatcher
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalBottomSheetProperties
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.SheetState
import androidx.compose.material3.SheetValue
import androidx.compose.material3.contentColorFor
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.abs
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * CompositionLocal providing a helper lambda to trigger a smooth animated dismiss
 * from any child composable inside [SmoothModalBottomSheet].
 */
val LocalSmoothDismiss = compositionLocalOf<((() -> Unit)?) -> Unit> {
    { it?.invoke() }
}

/**
 * Shared critically-damped spring for bottom sheet lift-up, lift-down,
 * and the synchronized background page zoom transition.
 */
val SmoothBottomSheetSpringSpec = spring<Float>(
    dampingRatio = 0.94f,
    stiffness = 260f
)

/**
 * Coordinates the background page zoom-out / zoom-in animation in [com.saurav.pixelmusic.MainActivity]
 * whenever any [SmoothModalBottomSheet] opens, is dragged, or closes.
 *
 * Uses a single quantized [mutableFloatStateOf] instead of a SnapshotStateMap so updates
 * never trigger redundant root recompositions.
 */
object BottomSheetSceneController {
    private val nextId = AtomicInteger(1)
    private val activeSheetTargets = HashMap<Int, Float>()

    var targetOpenFraction by mutableFloatStateOf(0f)
        private set

    fun allocateSheetId(): Int = nextId.getAndIncrement()

    fun updateSheetTarget(id: Int, fraction: Float) {
        val clamped = fraction.coerceIn(0f, 1f)
        val previous = activeSheetTargets[id]
        if (previous != null && abs(previous - clamped) < 0.015f && clamped != 0f && clamped != 1f) {
            return
        }
        activeSheetTargets[id] = clamped
        recomputeTargetFraction()
    }

    fun removeSheet(id: Int) {
        if (activeSheetTargets.remove(id) != null) {
            recomputeTargetFraction()
        }
    }

    private fun recomputeTargetFraction() {
        var maxVal = 0f
        for (v in activeSheetTargets.values) {
            if (v > maxVal) maxVal = v
        }
        if (abs(targetOpenFraction - maxVal) >= 0.005f || maxVal == 0f || maxVal == 1f) {
            targetOpenFraction = maxVal
        }
    }
}

/**
 * Isolated state holder composable so [BottomSheetSceneController.targetOpenFraction] is only
 * observed here and never invalidates the caller's composition scope.
 */
@Composable
fun rememberBottomSheetZoomProgress(): State<Float> {
    return animateFloatAsState(
        targetValue = BottomSheetSceneController.targetOpenFraction,
        animationSpec = SmoothBottomSheetSpringSpec,
        label = "BottomSheetBackgroundZoom"
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
private val SmoothSheetMotionScheme = object : MotionScheme {
    override fun <T> defaultSpatialSpec(): FiniteAnimationSpec<T> =
        spring(dampingRatio = 0.94f, stiffness = 260f)

    override fun <T> fastSpatialSpec(): FiniteAnimationSpec<T> =
        spring(dampingRatio = 0.94f, stiffness = 260f)

    override fun <T> slowSpatialSpec(): FiniteAnimationSpec<T> =
        spring(dampingRatio = 0.94f, stiffness = 220f)

    override fun <T> defaultEffectsSpec(): FiniteAnimationSpec<T> =
        spring(dampingRatio = 0.94f, stiffness = 260f)

    override fun <T> fastEffectsSpec(): FiniteAnimationSpec<T> =
        spring(dampingRatio = 0.94f, stiffness = 260f)

    override fun <T> slowEffectsSpec(): FiniteAnimationSpec<T> =
        spring(dampingRatio = 0.94f, stiffness = 220f)
}

/**
 * A wrapper around Material 3 [ModalBottomSheet] that ensures smooth lift-up and
 * lift-down animations and synchronizes background page zoom when:
 * 1. Tapping the back button or performing system back swipe gestures
 * 2. Tapping the scrim / blank space outside the sheet
 * 3. Dragging the sheet down to dismiss
 * 4. Dismissing programmatically via [LocalSmoothDismiss]
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SmoothModalBottomSheet(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    sheetState: SheetState = rememberBottomSheetState(
        initialValue = SheetValue.Hidden,
        enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded)
    ),
    sheetMaxWidth: Dp = BottomSheetDefaults.SheetMaxWidth,
    shape: Shape = BottomSheetDefaults.ExpandedShape,
    containerColor: Color = BottomSheetDefaults.ContainerColor,
    contentColor: Color = contentColorFor(containerColor),
    tonalElevation: Dp = 0.dp,
    scrimColor: Color = BottomSheetDefaults.ScrimColor,
    dragHandle: @Composable (() -> Unit)? = { BottomSheetDefaults.DragHandle() },
    contentWindowInsets: @Composable () -> WindowInsets = { BottomSheetDefaults.modalWindowInsets },
    properties: ModalBottomSheetProperties = ModalBottomSheetProperties(shouldDismissOnBackPress = true),
    content: @Composable ColumnScope.() -> Unit,
) {
    val scope = rememberCoroutineScope()
    val latestOnDismissRequest by rememberUpdatedState(onDismissRequest)
    val sheetId = remember { BottomSheetSceneController.allocateSheetId() }
    var isDismissing by remember { mutableStateOf(false) }

    var measuredSheetHeightPx by remember { mutableFloatStateOf(0f) }
    var expandedOffsetPx by remember { mutableFloatStateOf(Float.NaN) }
    val hasExpandedRef = remember { BooleanArray(1) }

    SideEffect {
        if (!isDismissing && !hasExpandedRef[0]) {
            BottomSheetSceneController.updateSheetTarget(sheetId, 1f)
        }
    }

    DisposableEffect(sheetId) {
        onDispose {
            BottomSheetSceneController.removeSheet(sheetId)
        }
    }

    fun dismissSmoothly(afterDismiss: (() -> Unit)? = null) {
        if (isDismissing) return
        isDismissing = true
        BottomSheetSceneController.updateSheetTarget(sheetId, 0f)
        scope.launch {
            try {
                if (sheetState.isVisible) {
                    sheetState.hide()
                }
            } catch (_: Throwable) {
            } finally {
                BottomSheetSceneController.removeSheet(sheetId)
                latestOnDismissRequest()
                afterDismiss?.invoke()
                isDismissing = false
            }
        }
    }

    LaunchedEffect(sheetState) {
        snapshotFlow {
            Triple(
                sheetState.currentValue,
                sheetState.targetValue,
                runCatching { sheetState.requireOffset() }.getOrElse { Float.NaN }
            )
        }.collectLatest { (currentVal, targetVal, offset) ->
            if (currentVal == SheetValue.Expanded || targetVal == SheetValue.Expanded) {
                hasExpandedRef[0] = true
            }
            if (!offset.isNaN() && currentVal == SheetValue.Expanded && targetVal == SheetValue.Expanded) {
                if (expandedOffsetPx.isNaN() || offset < expandedOffsetPx) {
                    expandedOffsetPx = offset
                }
            }
            if (!isDismissing) {
                if (hasExpandedRef[0] && targetVal == SheetValue.Hidden) {
                    BottomSheetSceneController.updateSheetTarget(sheetId, 0f)
                } else if (!offset.isNaN() && !expandedOffsetPx.isNaN() && measuredSheetHeightPx > 0f && hasExpandedRef[0]) {
                    val dragDown = (offset - expandedOffsetPx).coerceAtLeast(0f)
                    val openFraction = (1f - (dragDown / measuredSheetHeightPx)).coerceIn(0f, 1f)
                    BottomSheetSceneController.updateSheetTarget(sheetId, openFraction)
                } else {
                    BottomSheetSceneController.updateSheetTarget(sheetId, 1f)
                }
            }
        }
    }

    val outerMotionScheme = MaterialTheme.motionScheme

    MaterialTheme(
        colorScheme = MaterialTheme.colorScheme,
        typography = MaterialTheme.typography,
        shapes = MaterialTheme.shapes,
        motionScheme = SmoothSheetMotionScheme
    ) {
        ModalBottomSheet(
            onDismissRequest = {
                if (!isDismissing && sheetState.isVisible) {
                    dismissSmoothly()
                } else if (!isDismissing) {
                    BottomSheetSceneController.removeSheet(sheetId)
                    latestOnDismissRequest()
                }
            },
            modifier = modifier.onSizeChanged { size ->
                if (size.height > 0) {
                    measuredSheetHeightPx = size.height.toFloat()
                }
            },
            sheetState = sheetState,
            sheetMaxWidth = sheetMaxWidth,
            shape = shape,
            containerColor = containerColor,
            contentColor = contentColor,
            tonalElevation = tonalElevation,
            scrimColor = scrimColor,
            dragHandle = dragHandle,
            contentWindowInsets = contentWindowInsets,
            properties = properties,
            content = {
                val dialogView = LocalView.current
                DisposableEffect(dialogView, properties.shouldDismissOnBackPress) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && properties.shouldDismissOnBackPress) {
                        val dispatcher = dialogView.findOnBackInvokedDispatcher()
                        val callback = OnBackInvokedCallback {
                            dismissSmoothly()
                        }
                        dispatcher?.registerOnBackInvokedCallback(
                            OnBackInvokedDispatcher.PRIORITY_DEFAULT,
                            callback
                        )
                        onDispose {
                            dispatcher?.unregisterOnBackInvokedCallback(callback)
                        }
                    } else {
                        onDispose { }
                    }
                }

                BackHandler(enabled = !isDismissing && properties.shouldDismissOnBackPress) {
                    dismissSmoothly()
                }

                MaterialTheme(
                    colorScheme = MaterialTheme.colorScheme,
                    typography = MaterialTheme.typography,
                    shapes = MaterialTheme.shapes,
                    motionScheme = outerMotionScheme
                ) {
                    CompositionLocalProvider(
                        LocalSmoothDismiss provides { afterDismiss -> dismissSmoothly(afterDismiss) }
                    ) {
                        content()
                    }
                    PixelToastHost(usePopup = true)
                }
            }
        )
    }
}
