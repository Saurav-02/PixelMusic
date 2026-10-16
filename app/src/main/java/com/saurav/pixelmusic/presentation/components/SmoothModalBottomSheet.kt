package com.saurav.pixelmusic.presentation.components

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalBottomSheetDefaults
import androidx.compose.material3.ModalBottomSheetProperties
import androidx.compose.material3.SheetState
import androidx.compose.material3.SheetValue
import androidx.compose.material3.contentColorFor
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

/**
 * CompositionLocal providing a helper lambda to trigger a smooth animated dismiss
 * from any child composable inside [SmoothModalBottomSheet].
 */
val LocalSmoothDismiss = compositionLocalOf<((() -> Unit)?) -> Unit> {
    { it?.invoke() }
}

/**
 * A wrapper around Material 3 [ModalBottomSheet] that ensures smooth dismiss animations when:
 * 1. Tapping the back button or performing system back swipe gestures
 * 2. Tapping the scrim / outside the sheet
 * 3. Dragging the sheet down to dismiss
 * 4. Dismissing programmatically via [LocalSmoothDismiss]
 *
 * This prevents the sheet from vanishing instantly in a single frame without exit animation.
 */
@OptIn(ExperimentalMaterial3Api::class)
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
    var isDismissing by remember { mutableStateOf(false) }

    fun dismissSmoothly(afterDismiss: (() -> Unit)? = null) {
        if (isDismissing) return
        isDismissing = true
        scope.launch {
            try {
                sheetState.hide()
            } catch (_: Throwable) {
            } finally {
                onDismissRequest()
                afterDismiss?.invoke()
                isDismissing = false
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = {
            if (!isDismissing && sheetState.isVisible) {
                dismissSmoothly()
            } else if (!isDismissing) {
                onDismissRequest()
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
            // Intercept system back within the bottom sheet's dialog window
            BackHandler(enabled = !isDismissing) {
                dismissSmoothly()
            }

            CompositionLocalProvider(
                LocalSmoothDismiss provides { afterDismiss -> dismissSmoothly(afterDismiss) }
            ) {
                content()
            }
        }
    )
}
