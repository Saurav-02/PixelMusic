package com.unshoo.pixelmusic.presentation.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.coerceAtLeast
import androidx.compose.ui.unit.dp
import com.unshoo.pixelmusic.R
import kotlinx.coroutines.delay
import kotlin.math.roundToInt
import androidx.compose.ui.unit.Dp



@Composable
fun HomeShuffleFab(
    isShuffleEnabled: Boolean,
    isPlayerActive: Boolean,
    baseBottomOffset: Dp,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isExploreMode: Boolean = false,
    onLongClick: (() -> Unit)? = null,
    onSwipeUp: (() -> Unit)? = null,
) {
    val systemNavBarInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    var isPlayerActiveDelayed by remember { mutableStateOf(isPlayerActive) }

    LaunchedEffect(isPlayerActive) {
        if (isPlayerActive) {
            isPlayerActiveDelayed = true
        } else {
            delay(4000)
            isPlayerActiveDelayed = false
        }
    }

    // 2. Calculate exact clearance: Scaffold padding + MiniPlayer (if active) + 16dp spacing
    val targetOffset = baseBottomOffset + (if (isPlayerActiveDelayed) MiniPlayerHeight else 0.dp) + 16.dp

    val animatedBottomOffset by animateDpAsState(
        targetValue = targetOffset,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "fabBottomOffset"
    )

    val dynamicHorizontalPadding = if (systemNavBarInset > 30.dp) 14.dp else systemNavBarInset
    val dynamicEndPadding = 16.dp + dynamicHorizontalPadding

    val containerColor = when {
        isExploreMode -> MaterialTheme.colorScheme.primary
        isShuffleEnabled -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.tertiaryContainer
    }
    val contentColor = when {
        isExploreMode -> MaterialTheme.colorScheme.onPrimary
        isShuffleEnabled -> MaterialTheme.colorScheme.onPrimary
        else -> MaterialTheme.colorScheme.onTertiaryContainer
    }

    // ── Gesture-reactive pull ────────────────────────────────────────────────────────────
    val density = LocalDensity.current
    val maxPullPx = with(density) { 40.dp.toPx() }      // visual cap — FAB stops here
    val swipeThresholdPx = with(density) { 28.dp.toPx() } // triggers the recognition dialog

    var isDragging by remember { mutableStateOf(false) }
    var dragOffsetY by remember { mutableFloatStateOf(0f) }
    var swipeTriggered by remember { mutableStateOf(false) }

    // During drag: snaps instantly to finger position. On release: bounces back to 0.
    val animatedOffsetY by animateFloatAsState(
        targetValue = if (isDragging) dragOffsetY else 0f,
        animationSpec = if (isDragging) {
            snap()
        } else {
            spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMedium
            )
        },
        label = "fabDragOffset"
    )

    Box(
        modifier = modifier
            .padding(bottom = animatedBottomOffset.coerceAtLeast(0.dp), end = dynamicEndPadding)
            .offset { IntOffset(0, animatedOffsetY.roundToInt()) }
            .size(64.dp)
            .clip(CircleShape)
            .background(containerColor)
            .then(
                if (onSwipeUp != null) {
                    Modifier.pointerInput(Unit) {
                        detectVerticalDragGestures(
                            onDragStart = {
                                isDragging = true
                                dragOffsetY = 0f
                                swipeTriggered = false
                            },
                            onDragEnd = {
                                isDragging = false
                                swipeTriggered = false
                            },
                            onDragCancel = {
                                isDragging = false
                                swipeTriggered = false
                            },
                            onVerticalDrag = { change, dragAmount ->
                                change.consume()

                                // Clamp so the FAB stops after a tiny pull
                                val newValue = (dragOffsetY + dragAmount).coerceIn(-maxPullPx, 0f)
                                dragOffsetY = newValue

                                // Trigger once when threshold is crossed
                                if (!swipeTriggered && newValue < -swipeThresholdPx) {
                                    swipeTriggered = true
                                    onSwipeUp.invoke()
                                }
                            }
                        )
                    }
                } else Modifier
            )
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick,
                role = Role.Button
            ),
        contentAlignment = Alignment.Center
    ) {
        if (isExploreMode) {
            Icon(
                imageVector = Icons.Rounded.AutoAwesome,
                contentDescription = "Smart Mix",
                tint = contentColor,
                modifier = Modifier.size(32.dp)
            )
        } else {
            Icon(
                painter = painterResource(R.drawable.rounded_shuffle_24),
                contentDescription = stringResource(R.string.cd_shuffle_play),
                tint = contentColor,
                modifier = Modifier.size(32.dp)
            )
        }
    }
}
