package com.saurav.pixelmusic.presentation.components

import android.content.res.Configuration
import android.os.Build
import android.window.OnBackInvokedCallback
import android.window.OnBackInvokedDispatcher
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalBottomSheetProperties
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.SheetState
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Surface
import androidx.compose.material3.contentColorFor
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.abs
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
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
    dampingRatio = 0.96f,
    stiffness = 380f
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
        spring(dampingRatio = 0.96f, stiffness = 380f)

    override fun <T> fastSpatialSpec(): FiniteAnimationSpec<T> =
        spring(dampingRatio = 0.96f, stiffness = 420f)

    override fun <T> slowSpatialSpec(): FiniteAnimationSpec<T> =
        spring(dampingRatio = 0.96f, stiffness = 340f)

    override fun <T> defaultEffectsSpec(): FiniteAnimationSpec<T> =
        spring(dampingRatio = 0.96f, stiffness = 380f)

    override fun <T> fastEffectsSpec(): FiniteAnimationSpec<T> =
        spring(dampingRatio = 0.96f, stiffness = 420f)

    override fun <T> slowEffectsSpec(): FiniteAnimationSpec<T> =
        spring(dampingRatio = 0.96f, stiffness = 340f)
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
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    // In landscape mode, present all bottom sheets as a 100% GPU-driven right-side modal panel
    // sliding smoothly from the right edge (zero AnchoredDraggable re-anchor stutter, full height).
    if (isLandscape) {
        val density = LocalDensity.current
        val panelWidthDp = (configuration.screenWidthDp.dp * 0.50f).coerceIn(320.dp, 460.dp)
        val panelWidthPx = with(density) { panelWidthDp.toPx() }
        val slideProgress = remember { Animatable(0f) }
        var dragOffsetPx by remember { mutableFloatStateOf(0f) }
        var isDismissing by remember { mutableStateOf(false) }

        LaunchedEffect(Unit) {
            slideProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing)
            )
        }

        fun dismissRightSheetSmoothly(afterDismiss: (() -> Unit)? = null) {
            if (isDismissing) return
            isDismissing = true
            scope.launch {
                try {
                    slideProgress.animateTo(
                        targetValue = 0f,
                        animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing)
                    )
                } finally {
                    latestOnDismissRequest()
                    afterDismiss?.invoke()
                    isDismissing = false
                }
            }
        }

        Dialog(
            onDismissRequest = { dismissRightSheetSmoothly() },
            properties = DialogProperties(
                dismissOnBackPress = false,
                dismissOnClickOutside = false,
                usePlatformDefaultWidth = false,
                decorFitsSystemWindows = false
            )
        ) {
            val dialogView = LocalView.current
            DisposableEffect(dialogView, properties.shouldDismissOnBackPress) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && properties.shouldDismissOnBackPress) {
                    val dispatcher = dialogView.findOnBackInvokedDispatcher()
                    val callback = OnBackInvokedCallback {
                        dismissRightSheetSmoothly()
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
                dismissRightSheetSmoothly()
            }

            Box(modifier = Modifier.fillMaxSize()) {
                // Smoothly fading scrim over the left side; tapping dismisses smoothly
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            val dragFraction = (dragOffsetPx / panelWidthPx.coerceAtLeast(1f)).coerceIn(0f, 1f)
                            alpha = (slideProgress.value * (1f - dragFraction * 0.75f)).coerceIn(0f, 1f)
                        }
                        .background(scrimColor)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            dismissRightSheetSmoothly()
                        }
                )

                // Right-side modal sheet sliding smoothly from the right edge via GPU graphicsLayer
                Surface(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .width(panelWidthDp)
                        .fillMaxHeight()
                        .graphicsLayer {
                            translationX = ((1f - slideProgress.value) * panelWidthPx) + dragOffsetPx.coerceAtLeast(0f)
                        }
                        .pointerInput(panelWidthPx) {
                            detectHorizontalDragGestures(
                                onDragEnd = {
                                    if (dragOffsetPx > panelWidthPx * 0.28f) {
                                        dismissRightSheetSmoothly()
                                    } else {
                                        dragOffsetPx = 0f
                                    }
                                },
                                onDragCancel = {
                                    dragOffsetPx = 0f
                                },
                                onHorizontalDrag = { _, dragAmount ->
                                    dragOffsetPx = (dragOffsetPx + dragAmount).coerceAtLeast(0f)
                                }
                            )
                        },
                    shape = RoundedCornerShape(topStart = 28.dp, bottomStart = 28.dp),
                    color = containerColor,
                    contentColor = contentColor,
                    tonalElevation = tonalElevation,
                    shadowElevation = 12.dp
                ) {
                    CompositionLocalProvider(
                        LocalSmoothDismiss provides { afterDismiss -> dismissRightSheetSmoothly(afterDismiss) }
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .statusBarsPadding()
                                .navigationBarsPadding()
                                .imePadding()
                                .padding(top = 12.dp)
                        ) {
                            content()
                        }
                    }
                    PixelToastHost(usePopup = true)
                }
            }
        }
        return
    }

    val sheetId = remember { BottomSheetSceneController.allocateSheetId() }
    var isDismissing by remember { mutableStateOf(false) }
    val hasExpandedRef = remember { BooleanArray(1) }

    LaunchedEffect(sheetId) {
        BottomSheetSceneController.updateSheetTarget(sheetId, 1f)
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

    // Observe only discrete SheetValue transitions (not per-frame requireOffset()) so the background
    // zoom spring and ModalBottomSheet spring animate smoothly in parallel without chasing each other.
    LaunchedEffect(sheetState) {
        snapshotFlow { sheetState.currentValue to sheetState.targetValue }
            .distinctUntilChanged()
            .collectLatest { (currentVal, targetVal) ->
                if (currentVal == SheetValue.Expanded || targetVal == SheetValue.Expanded) {
                    hasExpandedRef[0] = true
                }
                if (!isDismissing) {
                    if (hasExpandedRef[0] && targetVal == SheetValue.Hidden) {
                        BottomSheetSceneController.updateSheetTarget(sheetId, 0f)
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
            modifier = modifier,
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
