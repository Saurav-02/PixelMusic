package com.saurav.pixelmusic.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material3.VerticalDivider
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.Color
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
    isSessionActive: Boolean = false,
    onLongClick: (() -> Unit)? = null,
    onSwipeUp: (() -> Unit)? = null,
    onListenTogetherClick: (() -> Unit)? = null,
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
        isShuffleEnabled && !isExploreMode -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.tertiaryContainer
    }
    val animatedContainerColor by animateColorAsState(
        targetValue = targetContainerColor,
        animationSpec = tween(200),
        label = "fabContainerColor"
    )

    val targetContentColor = when {
        isThresholdReached -> MaterialTheme.colorScheme.onPrimary
        isShuffleEnabled && !isExploreMode -> MaterialTheme.colorScheme.onPrimary
        else -> MaterialTheme.colorScheme.onTertiaryContainer
    }
    val animatedContentColor by animateColorAsState(
        targetValue = targetContentColor,
        animationSpec = tween(200),
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

    // Swipe-up-to-recognize gesture, shared by both FAB forms.
    val fabDragModifier = if (onSwipeUp != null) {
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
    } else {
        Modifier
    }

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

        // ── Shuffle FAB: tap / long-press / drag-up-to-recognize ───────────────────────
        Box(
            modifier = Modifier
                .offset { IntOffset(0, animatedOffsetY.roundToInt()) }
                .height(64.dp)
                .clip(CircleShape)
                .background(animatedContainerColor)
                .animateContentSize(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioNoBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    )
                ),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Shuffle half: tap / long-press / drag-up-to-recognize.
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .then(fabDragModifier)
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
                    ShuffleFabIcon(
                        isThresholdReached = isThresholdReached,
                        isExploreMode = isExploreMode,
                        tint = animatedContentColor
                    )
                }

                // Listen Together half: expands/collapses with a spring — no pop.
                AnimatedVisibility(
                    visible = onListenTogetherClick != null,
                    enter = fadeIn(animationSpec = tween(220)) + expandHorizontally(
                        expandFrom = Alignment.Start,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioNoBouncy,
                            stiffness = Spring.StiffnessMediumLow
                        )
                    ),
                    exit = fadeOut(animationSpec = tween(180)) + shrinkHorizontally(
                        shrinkTowards = Alignment.Start,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioNoBouncy,
                            stiffness = Spring.StiffnessMediumLow
                        )
                    ),
                    label = "ltHalf"
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        VerticalDivider(
                            modifier = Modifier.height(32.dp),
                            thickness = 1.dp,
                            color = animatedContentColor.copy(alpha = 0.35f)
                        )
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .clickable(
                                    role = Role.Button,
                                    onClick = {
                                        hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        onListenTogetherClick?.invoke()
                                    }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Crossfade(
                                targetState = isSessionActive,
                                animationSpec = tween(300),
                                label = "ltFabIconCrossfade"
                            ) { active ->
                                if (active) {
                                    FabEqualizerBars(
                                        color = animatedContentColor,
                                        modifier = Modifier.size(28.dp)
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Rounded.Group,
                                        contentDescription = stringResource(R.string.listen_together),
                                        tint = animatedContentColor,
                                        modifier = Modifier.size(32.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/** 3-bar animated equalizer replacing the static group icon when a session is active */
@Composable
private fun FabEqualizerBars(
    color: Color,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "fabEq")
    val h1 by infiniteTransition.animateFloat(
        initialValue = 6f,
        targetValue = 22f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "fabH1"
    )
    val h2 by infiniteTransition.animateFloat(
        initialValue = 22f,
        targetValue = 8f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "fabH2"
    )
    val h3 by infiniteTransition.animateFloat(
        initialValue = 10f,
        targetValue = 24f,
        animationSpec = infiniteRepeatable(
            animation = tween(1350, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "fabH3"
    )

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(3.5.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(h1.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(color)
        )
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(h2.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(color)
        )
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(h3.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(color)
        )
    }
}

/** The shuffle / Smart Mix / recognize icon. */
@Composable
private fun ShuffleFabIcon(
    isThresholdReached: Boolean,
    isExploreMode: Boolean,
    tint: Color,
) {
    Crossfade(
        targetState = Pair(isThresholdReached, isExploreMode),
        animationSpec = tween(250),
        label = "fabIcon"
    ) { (reached, explore) ->
        if (reached) {
            Icon(
                imageVector = Icons.Rounded.GraphicEq,
                contentDescription = "Recognize Music",
                tint = tint,
                modifier = Modifier.size(32.dp)
            )
        } else if (explore) {
            Icon(
                imageVector = Icons.Rounded.AutoAwesome,
                contentDescription = "Smart Mix",
                tint = tint,
                modifier = Modifier.size(32.dp)
            )
        } else {
            Icon(
                painter = painterResource(R.drawable.rounded_shuffle_24),
                contentDescription = stringResource(R.string.cd_shuffle_play),
                tint = tint,
                modifier = Modifier.size(32.dp)
            )
        }
    }
}
