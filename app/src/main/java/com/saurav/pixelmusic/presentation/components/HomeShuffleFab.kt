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
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.rounded.KeyboardArrowUp
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
import androidx.compose.runtime.rememberUpdatedState
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

private object ColdLaunchRecognitionHintState {
    var hasShownInThisProcess = false
}

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

    // Keep the FAB anchored smoothly as the mini-player appears/disappears;
    // a bouncy spring here makes the whole control feel like it is jumping.
    val animatedBottomOffset by animateDpAsState(
        targetValue = targetOffset,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "fabBottomOffset"
    )

    val dynamicHorizontalPadding = if (systemNavBarInset > 30.dp) 14.dp else systemNavBarInset
    val dynamicEndPadding = 16.dp + dynamicHorizontalPadding

    val hapticFeedback = LocalHapticFeedback.current
    val currentOnSwipeUp by rememberUpdatedState(onSwipeUp)

    // ── Gesture-reactive pull ────────────────────────────────────────────────────────────
    val density = LocalDensity.current
    val maxPullPx = with(density) { 140.dp.toPx() }       // generous visual pull travel
    val swipeThresholdPx = with(density) { 84.dp.toPx() } // triggers release-to-recognize (substantial pull)

    var isDragging by remember { mutableStateOf(false) }
    var dragOffsetY by remember { mutableFloatStateOf(0f) }
    var isThresholdReached by remember { mutableStateOf(false) }

    // This is an instructional hint only; it must never imply recognition is ready
    // until the user actually crosses the drag threshold.
    var showColdLaunchHint by remember {
        mutableStateOf(!ColdLaunchRecognitionHintState.hasShownInThisProcess && onSwipeUp != null)
    }

    LaunchedEffect(showColdLaunchHint) {
        if (showColdLaunchHint) {
            ColdLaunchRecognitionHintState.hasShownInThisProcess = true
            delay(2200)
            showColdLaunchHint = false
        }
    }

    LaunchedEffect(isDragging) {
        if (isDragging) showColdLaunchHint = false
    }

    // Match main navigation pill color (MaterialTheme.colorScheme.primary)
    val targetContainerColor = when {
        isThresholdReached -> MaterialTheme.colorScheme.primaryContainer
        else -> MaterialTheme.colorScheme.primary
    }
    val animatedContainerColor by animateColorAsState(
        targetValue = targetContainerColor,
        animationSpec = tween(200),
        label = "fabContainerColor"
    )

    val targetContentColor = when {
        isThresholdReached -> MaterialTheme.colorScheme.onPrimaryContainer
        else -> MaterialTheme.colorScheme.onPrimary
    }
    val animatedContentColor by animateColorAsState(
        targetValue = targetContentColor,
        animationSpec = tween(200),
        label = "fabContentColor"
    )

    // During drag: follows the finger directly. On release: returns smoothly to rest.
    val animatedOffsetY by animateFloatAsState(
        targetValue = if (isDragging) dragOffsetY else 0f,
        animationSpec = if (isDragging) {
            snap()
        } else {
            spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessMedium
            )
        },
        label = "fabDragOffset"
    )

    // Swipe-up-to-recognize gesture, shared by both FAB forms.
    val fabDragModifier = if (onSwipeUp != null) {
        Modifier.pointerInput(maxPullPx, swipeThresholdPx) {
            detectVerticalDragGestures(
                onDragStart = {
                    isDragging = true
                    dragOffsetY = 0f
                    isThresholdReached = false
                },
                onDragEnd = {
                    if (isThresholdReached) {
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                        currentOnSwipeUp?.invoke()
                    }
                    isDragging = false
                    dragOffsetY = 0f
                    isThresholdReached = false
                },
                onDragCancel = {
                    isDragging = false
                    dragOffsetY = 0f
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
        // ── Dynamic Hint / Confirmation Pill with Shooting Upward Arrow ─────────────
        val isPillVisible = (onSwipeUp != null && isDragging && dragOffsetY < -8f) || showColdLaunchHint
        // Only a real drag crossing the threshold may show the ready-to-release state.
        val isRecognizeActive = isDragging && isThresholdReached

        val pillContainerColor = if (isRecognizeActive) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.surfaceContainerHigh
        }
        val pillContentColor = if (isRecognizeActive) {
            MaterialTheme.colorScheme.onPrimary
        } else {
            MaterialTheme.colorScheme.onSurface
        }

        AnimatedVisibility(
            visible = isPillVisible,
            modifier = Modifier
                .padding(bottom = 100.dp)
                .offset { IntOffset(0, animatedOffsetY.roundToInt()) },
            enter = fadeIn(animationSpec = tween(200)) + scaleIn(initialScale = 0.85f),
            exit = fadeOut(animationSpec = tween(200)) + scaleOut(targetScale = 0.85f)
        ) {
            Box(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = pillContainerColor,
                    contentColor = pillContentColor,
                    shadowElevation = 6.dp,
                    tonalElevation = 6.dp
                ) {
                    Crossfade(
                        targetState = isRecognizeActive,
                        animationSpec = tween(220),
                        label = "pillContentCrossfade"
                    ) { recognize ->
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            if (recognize) {
                                PillEqualizerBars(
                                    color = pillContentColor,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "Release to recognize",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = GoogleSansRounded,
                                        fontSize = 12.sp
                                    )
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Rounded.KeyboardArrowUp,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = if (showColdLaunchHint || !isDragging) "Swipe up to recognize" else "Keep pulling up",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = GoogleSansRounded,
                                        fontSize = 12.sp
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }

        // ── Single FAB with wave when pulling up / Dual FAB when resting ─────────────
        Box(
            modifier = Modifier
                .offset { IntOffset(0, animatedOffsetY.roundToInt()) }
                .height(64.dp)
                .clip(CircleShape)
                .background(animatedContainerColor)
                .then(fabDragModifier)
                .animateContentSize(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioNoBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Crossfade(
                targetState = isDragging,
                animationSpec = tween(180),
                label = "fabModeCrossfade"
            ) { dragging ->
                if (dragging) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        FabRecognitionWave(
                            color = animatedContentColor,
                            isThresholdReached = isThresholdReached,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Shuffle / Smart Mix half
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
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
                                isThresholdReached = false,
                                isExploreMode = isExploreMode,
                                tint = animatedContentColor
                            )
                        }

                        // Listen Together half
                        if (onListenTogetherClick != null) {
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
                                            onListenTogetherClick.invoke()
                                        }
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSessionActive) {
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

/** Upward shooting arrow that animates straight up across a tall distance and vanishes */
@Composable
private fun VanishingUpwardArrow(
    color: Color,
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(label = "arrowShootTransition")
    val offsetY by transition.animateFloat(
        initialValue = 0f,
        targetValue = -95f,
        animationSpec = infiniteRepeatable(
            animation = tween(850, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "arrowShootY"
    )
    val alpha by transition.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(850, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "arrowShootAlpha"
    )

    Icon(
        imageVector = Icons.Rounded.KeyboardArrowUp,
        contentDescription = null,
        tint = color.copy(alpha = alpha),
        modifier = modifier
            .size(36.dp)
            .offset(y = offsetY.dp)
    )
}

/** 5-bar animated audio wave in the center of the single merged FAB when pulling up */
@Composable
private fun FabRecognitionWave(
    color: Color,
    isThresholdReached: Boolean,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "waveTransition")
    val speed = if (isThresholdReached) 380 else 580

    val b1 by infiniteTransition.animateFloat(
        initialValue = if (isThresholdReached) 8f else 5f,
        targetValue = if (isThresholdReached) 20f else 13f,
        animationSpec = infiniteRepeatable(
            animation = tween(speed, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "w1"
    )
    val b2 by infiniteTransition.animateFloat(
        initialValue = if (isThresholdReached) 14f else 8f,
        targetValue = if (isThresholdReached) 28f else 20f,
        animationSpec = infiniteRepeatable(
            animation = tween(speed + 120, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "w2"
    )
    val b3 by infiniteTransition.animateFloat(
        initialValue = if (isThresholdReached) 18f else 12f,
        targetValue = if (isThresholdReached) 34f else 26f,
        animationSpec = infiniteRepeatable(
            animation = tween(speed + 60, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "w3"
    )
    val b4 by infiniteTransition.animateFloat(
        initialValue = if (isThresholdReached) 12f else 7f,
        targetValue = if (isThresholdReached) 26f else 18f,
        animationSpec = infiniteRepeatable(
            animation = tween(speed + 180, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "w4"
    )
    val b5 by infiniteTransition.animateFloat(
        initialValue = if (isThresholdReached) 6f else 4f,
        targetValue = if (isThresholdReached) 18f else 12f,
        animationSpec = infiniteRepeatable(
            animation = tween(speed + 90, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "w5"
    )

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(3.5.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(3.5.dp)
                .height(b1.dp)
                .clip(CircleShape)
                .background(color)
        )
        Box(
            modifier = Modifier
                .width(3.5.dp)
                .height(b2.dp)
                .clip(CircleShape)
                .background(color)
        )
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(b3.dp)
                .clip(CircleShape)
                .background(color)
        )
        Box(
            modifier = Modifier
                .width(3.5.dp)
                .height(b4.dp)
                .clip(CircleShape)
                .background(color)
        )
        Box(
            modifier = Modifier
                .width(3.5.dp)
                .height(b5.dp)
                .clip(CircleShape)
                .background(color)
        )
    }
}

/** Compact 3-bar animated equalizer for the confirmation pill */
@Composable
private fun PillEqualizerBars(
    color: Color,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pillEq")
    val h1 by infiniteTransition.animateFloat(
        initialValue = 4f,
        targetValue = 14f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pillH1"
    )
    val h2 by infiniteTransition.animateFloat(
        initialValue = 14f,
        targetValue = 6f,
        animationSpec = infiniteRepeatable(
            animation = tween(750, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pillH2"
    )
    val h3 by infiniteTransition.animateFloat(
        initialValue = 7f,
        targetValue = 16f,
        animationSpec = infiniteRepeatable(
            animation = tween(680, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pillH3"
    )

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(2.5.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(2.5.dp)
                .height(h1.dp)
                .clip(RoundedCornerShape(1.5.dp))
                .background(color)
        )
        Box(
            modifier = Modifier
                .width(2.5.dp)
                .height(h2.dp)
                .clip(RoundedCornerShape(1.5.dp))
                .background(color)
        )
        Box(
            modifier = Modifier
                .width(2.5.dp)
                .height(h3.dp)
                .clip(RoundedCornerShape(1.5.dp))
                .background(color)
        )
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
