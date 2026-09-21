package com.saurav.pixelmusic.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.coerceAtLeast
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.saurav.pixelmusic.R
import com.saurav.pixelmusic.ui.theme.GoogleSansRounded
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

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

    val hapticFeedback = LocalHapticFeedback.current

    // ── Gesture-reactive pull ────────────────────────────────────────────────────────────
    val density = LocalDensity.current
    val maxPullPx = with(density) { 52.dp.toPx() }        // visual cap — FAB stops here
    val swipeThresholdPx = with(density) { 32.dp.toPx() } // triggers release-to-recognize

    var isDragging by remember { mutableStateOf(false) }
    var dragOffsetY by remember { mutableFloatStateOf(0f) }
    var isThresholdReached by remember { mutableStateOf(false) }

    val targetContainerColor = when {
        isThresholdReached -> MaterialTheme.colorScheme.primary
        isExploreMode -> MaterialTheme.colorScheme.primary
        isShuffleEnabled -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.tertiaryContainer
    }
    val animatedContainerColor by animateColorAsState(
        targetValue = targetContainerColor,
        animationSpec = tween(150),
        label = "fabContainerColor"
    )

    val targetContentColor = when {
        isThresholdReached -> MaterialTheme.colorScheme.onPrimary
        isExploreMode -> MaterialTheme.colorScheme.onPrimary
        isShuffleEnabled -> MaterialTheme.colorScheme.onPrimary
        else -> MaterialTheme.colorScheme.onTertiaryContainer
    }
    val animatedContentColor by animateColorAsState(
        targetValue = targetContentColor,
        animationSpec = tween(150),
        label = "fabContentColor"
    )

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
            .padding(bottom = animatedBottomOffset.coerceAtLeast(0.dp), end = dynamicEndPadding),
        contentAlignment = Alignment.BottomEnd
    ) {
        // ── Confirmation Pill ("Swipe up" / "Release to recognize") ──────────────────
        AnimatedVisibility(
            visible = onSwipeUp != null && isDragging && dragOffsetY < -8f,
            modifier = Modifier
                .padding(bottom = 74.dp)
                .offset { IntOffset(0, animatedOffsetY.roundToInt()) },
            enter = fadeIn(animationSpec = tween(120)) + scaleIn(initialScale = 0.85f),
            exit = fadeOut(animationSpec = tween(120)) + scaleOut(targetScale = 0.85f)
        ) {
            Surface(
                shape = CircleShape,
                color = if (isThresholdReached) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh,
                contentColor = if (isThresholdReached) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                shadowElevation = 6.dp,
                tonalElevation = 6.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = if (isThresholdReached) Icons.Rounded.GraphicEq else Icons.Rounded.Mic,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = if (isThresholdReached) "Release to recognize" else "Swipe up to recognize",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = GoogleSansRounded,
                            fontSize = 12.sp
                        )
                    )
                }
            }
        }

        // ── Floating Action Button ───────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .offset { IntOffset(0, animatedOffsetY.roundToInt()) }
                .size(64.dp)
                .clip(CircleShape)
                .background(animatedContainerColor)
                .then(
                    if (onSwipeUp != null) {
                        Modifier.pointerInput(Unit) {
                            detectVerticalDragGestures(
                                onDragStart = {
                                    isDragging = true
                                    dragOffsetY = 0f
                                    isThresholdReached = false
                                },
                                onDragEnd = {
                                    if (isThresholdReached) {
                                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                        onSwipeUp.invoke()
                                    }
                                    isDragging = false
                                    isThresholdReached = false
                                },
                                onDragCancel = {
                                    isDragging = false
                                    isThresholdReached = false
                                },
                                onVerticalDrag = { change, dragAmount ->
                                    change.consume()

                                    // Clamp so the FAB stops after pull
                                    val newValue = (dragOffsetY + dragAmount).coerceIn(-maxPullPx, 0f)
                                    dragOffsetY = newValue

                                    val reached = newValue <= -swipeThresholdPx
                                    if (reached != isThresholdReached) {
                                        isThresholdReached = reached
                                        if (reached) {
                                            hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        }
                                    }
                                }
                            )
                        }
                    } else Modifier
                )
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = onLongClick?.let { longClick ->
                        {
                            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                            longClick.invoke()
                        }
                    },
                    role = Role.Button
                ),
            contentAlignment = Alignment.Center
        ) {
            Crossfade(
                targetState = isThresholdReached,
                animationSpec = tween(150),
                label = "fabIcon"
            ) { reached ->
                if (reached) {
                    Icon(
                        imageVector = Icons.Rounded.GraphicEq,
                        contentDescription = "Recognize Music",
                        tint = animatedContentColor,
                        modifier = Modifier.size(32.dp)
                    )
                } else if (isExploreMode) {
                    Icon(
                        imageVector = Icons.Rounded.AutoAwesome,
                        contentDescription = "Smart Mix",
                        tint = animatedContentColor,
                        modifier = Modifier.size(32.dp)
                    )
                } else {
                    Icon(
                        painter = painterResource(R.drawable.rounded_shuffle_24),
                        contentDescription = stringResource(R.string.cd_shuffle_play),
                        tint = animatedContentColor,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        }
    }
}
