package com.saurav.pixelmusic.presentation.components

import android.os.SystemClock
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.painterResource
import com.saurav.pixelmusic.ui.theme.GoogleSansRounded
import com.saurav.pixelmusic.ui.theme.LocalLiquidGlassBlurIntensity
import com.saurav.pixelmusic.ui.theme.LocalLiquidGlassRefractionIntensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.graphics.graphicsLayer
import android.os.Build
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.util.lerp
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.text.PlatformTextStyle
import kotlinx.coroutines.launch
import androidx.navigation.NavHostController
import com.saurav.pixelmusic.BottomNavItem
import com.saurav.pixelmusic.R
import com.saurav.pixelmusic.presentation.navigation.Screen
import com.saurav.pixelmusic.presentation.navigation.navigateToTopLevelSafely
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.effects.colorControls
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.ui.unit.coerceAtLeast
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import androidx.compose.ui.graphics.Color
import androidx.compose.animation.core.animateFloatAsState



internal val NavBarContentHeight = 84.dp
internal val NavBarCompactContentHeight = 72.dp
internal val NavBarContentHeightFullWidth = NavBarContentHeight
private val MainScreenBottomGradientExtraHeight = 64.dp + MiniPlayerBottomSpacer + 8.dp
internal val MaxNavigationBarBottomInset = 96.dp

internal fun sanitizeNavigationBarBottomInset(systemNavBarInset: Dp): Dp {
    if (!systemNavBarInset.value.isFinite()) return 0.dp
    return systemNavBarInset.coerceIn(0.dp, MaxNavigationBarBottomInset)
}

internal fun calculatePlayerSheetCollapsedTargetY(
    containerHeightPx: Float,
    collapsedContentHeightPx: Float,
    bottomMarginPx: Float,
    bottomSpacerPx: Float
): Float {
    val safeContainerHeightPx = containerHeightPx.takeIf { it.isFinite() }?.coerceAtLeast(0f) ?: 0f
    val safeCollapsedContentHeightPx = collapsedContentHeightPx.takeIf { it.isFinite() }?.coerceAtLeast(0f) ?: 0f
    val safeBottomMarginPx = bottomMarginPx.takeIf { it.isFinite() }?.coerceAtLeast(0f) ?: 0f
    val safeBottomSpacerPx = bottomSpacerPx.takeIf { it.isFinite() }?.coerceAtLeast(0f) ?: 0f
    val maxTargetY = (safeContainerHeightPx - safeCollapsedContentHeightPx).coerceAtLeast(0f)

    return (safeContainerHeightPx - safeCollapsedContentHeightPx - safeBottomMarginPx - safeBottomSpacerPx)
        .coerceIn(0f, maxTargetY)
}

internal fun resolveNavBarContentHeight(compactMode: Boolean): Dp =
    if (compactMode) NavBarCompactContentHeight else NavBarContentHeight

internal fun resolveMainScreenBottomGradientHeight(compactMode: Boolean): Dp =
    resolveNavBarContentHeight(compactMode) + MainScreenBottomGradientExtraHeight

internal fun resolveNavBarSurfaceHeight(
    navBarStyle: String,
    systemNavBarInset: Dp,
    compactMode: Boolean
): Dp {
    return resolveNavBarContentHeight(compactMode)
}

internal fun resolveNavBarOccupiedHeight(
    systemNavBarInset: Dp,
    compactMode: Boolean
): Dp = resolveNavBarContentHeight(compactMode) + systemNavBarInset

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun PlayerInternalNavigationBar(
    navController: NavHostController,
    navItems: ImmutableList<BottomNavItem>,
    currentRoute: String?,
    modifier: Modifier = Modifier,
    navBarStyle: String,
    compactMode: Boolean,
    bottomBarPadding: Dp = 0.dp,
    onSearchIconDoubleTap: () -> Unit = {},
    isLiquidGlass: Boolean = true,
    backdrop: Backdrop? = null
) {
    val latestCurrentRoute by rememberUpdatedState(currentRoute)
    val latestNavigationEnabled by rememberUpdatedState(currentRoute != null)
    val coroutineScope = rememberCoroutineScope()

    val haptic = LocalHapticFeedback.current
    val configuration = LocalConfiguration.current
    val fontScale = LocalDensity.current.fontScale
    val screenWidth = configuration.screenWidthDp

    // Main pill contains Home, Explore, and Library; Search is separated into the detached button.
    val mainItems = remember(navItems) {
        navItems.filter { it.screen.route != Screen.Search.route }.toImmutableList()
    }

    val isLargeFont = fontScale > 1.25f
    val isCompactScreen = screenWidth < 400
    val shouldHideLabel = isLargeFont || (isCompactScreen && mainItems.size > 3) || compactMode

    val selectedIndex = mainItems.indexOfFirst { it.screen.route == latestCurrentRoute }

    // IMPORTANT: these sizes must always sum (with the bottom padding below) to
    // exactly resolveNavBarContentHeight(compactMode), because that is the value
    // parent screens use to reserve layout space (mini player position, gradient
    // height, etc). Previously these were hardcoded regardless of compactMode,
    // so when compactMode flipped to true the reserved space shrank to 64dp but
    // the bar kept drawing at 76dp worth of content -> squished/cut-off nav bar.
    //   non-compact: 72dp settings button + 12dp bottom padding = 84dp = NavBarContentHeight
    //   compact:     60dp settings button + 12dp bottom padding = 72dp = NavBarCompactContentHeight
    val toolbarRowHeight = if (compactMode) 40.dp else 48.dp
    val settingsButtonSize = if (compactMode) 60.dp else 72.dp
    val itemIconSize = if (compactMode) 20.dp else 24.dp
    val horizontalOuterPadding = if (compactMode) 10.dp else 16.dp
    val itemSpacing = if (compactMode) 8.dp else 12.dp
    val selectedItemWeight = if (compactMode) 1.7f else 2.2f
    val bottomPadding = if (isLiquidGlass) 0.dp else 12.dp

    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val primaryColor = MaterialTheme.colorScheme.primary
    val glassBlurIntensity = LocalLiquidGlassBlurIntensity.current
    val glassRefractionIntensity = LocalLiquidGlassRefractionIntensity.current

    // iOS Liquid Glass specular rim gradient and translucent fill colors
    val glassVibrancy = com.saurav.pixelmusic.ui.theme.LocalLiquidGlassVibrancyIntensity.current
    val glassRefractionHeight = com.saurav.pixelmusic.ui.theme.LocalLiquidGlassRefractionHeight.current
    val glassRefractionAmount = com.saurav.pixelmusic.ui.theme.LocalLiquidGlassRefractionAmount.current
    val glassSurfaceTint = com.saurav.pixelmusic.ui.theme.LocalLiquidGlassSurfaceTint.current
    val glassSurfaceOpacity = com.saurav.pixelmusic.ui.theme.LocalLiquidGlassSurfaceOpacity.current
    val glassStyle = com.saurav.pixelmusic.ui.theme.LocalLiquidGlassStyle.current
    val tabSelectionOpacity = com.saurav.pixelmusic.ui.theme.LocalLiquidGlassTabSelectionOpacity.current
    val highlightOpacity = com.saurav.pixelmusic.ui.theme.LocalLiquidGlassHighlightOpacity.current
    val glassTextColor = com.saurav.pixelmusic.ui.theme.LocalLiquidGlassTextColor.current

    val glassBorderBrush = remember(isDark) {
        Brush.verticalGradient(
            colors = if (isDark) listOf(
                Color.White.copy(alpha = 0.28f),
                Color.White.copy(alpha = 0.06f)
            ) else listOf(
                Color.White.copy(alpha = 0.45f),
                Color.White.copy(alpha = 0.10f)
            )
        )
    }

    val glassContainerColor = remember(isDark, glassSurfaceTint, glassSurfaceOpacity) {
        when (glassSurfaceTint) {
            "WHITE" -> Color.White.copy(alpha = glassSurfaceOpacity.coerceIn(0.08f, 0.40f))
            "DARK" -> Color(0xFF141416).copy(alpha = glassSurfaceOpacity.coerceIn(0.12f, 0.45f))
            "ACCENT" -> primaryColor.copy(alpha = (glassSurfaceOpacity * 0.6f).coerceIn(0.10f, 0.40f))
            else -> if (isDark) Color(0x22FFFFFF) else Color(0x38FFFFFF)
        }
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = horizontalOuterPadding, end = horizontalOuterPadding, bottom = bottomPadding),
        horizontalArrangement = Arrangement.spacedBy(itemSpacing),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isLiquidGlass) {
            // Match Convx's capsule selector instead of using a circular shape for the full bar.
            val capsuleShape = RoundedCornerShape(percent = 50)
            var totalWidthPx by remember { mutableIntStateOf(0) }
            val tabSelectionColorMode = com.saurav.pixelmusic.ui.theme.LocalLiquidGlassTabSelectionColorMode.current
            val chromaticAberration = com.saurav.pixelmusic.ui.theme.LocalLiquidGlassChromaticAberration.current

            val density = LocalDensity.current
            var isDropletActive by remember { mutableStateOf(false) }
            var isDragging by remember { mutableStateOf(false) }
            val dropletOffsetAnimatable = remember { Animatable(0f) }
            var currentHoveredIndex by remember { mutableIntStateOf(selectedIndex) }

            val navBarInset = if (compactMode) 4.dp else 5.dp
            val containerHeight = settingsButtonSize

            // Equal gaps on all sides: top, bottom, and horizontal edges
            val dropletHeight = settingsButtonSize - (navBarInset * 2)

            val tabCount = mainItems.size
            val tabWidthPx = if (tabCount > 0) totalWidthPx.toFloat() / tabCount else 0f
            val tabWidth = with(density) { tabWidthPx.toDp() }

            // Width of the selection pill shares the exact same gap (navBarInset * 2) as top/bottom
            val pillWidth = (tabWidth - (navBarInset * 2)).coerceAtLeast(32.dp)

            // Dynamic interaction response on touch and drag:
            // Subtly elastic stretch that keeps the pill's clean shape without over-stretching
            val dropletStretchX by animateFloatAsState(
                targetValue = if (isDragging) 1.05f else if (isDropletActive) 1.02f else 1f,
                animationSpec = spring(
                    dampingRatio = 0.60f,
                    stiffness = 380f
                ),
                label = "droplet_stretch_x"
            )
            val dropletStretchY by animateFloatAsState(
                targetValue = if (isDragging) 0.96f else if (isDropletActive) 0.98f else 1f,
                animationSpec = spring(
                    dampingRatio = 0.60f,
                    stiffness = 380f
                ),
                label = "droplet_stretch_y"
            )

            // Outer navbar interactive spring response when pill is pressed or dragged
            val navBarPressScale by animateFloatAsState(
                targetValue = if (isDragging) 1.012f else if (isDropletActive) 0.988f else 1f,
                animationSpec = spring(
                    dampingRatio = 0.55f,
                    stiffness = 400f
                ),
                label = "navbar_touch_scale"
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(containerHeight)
                    .onSizeChanged { totalWidthPx = it.width }
                    .graphicsLayer {
                        scaleX = navBarPressScale
                        scaleY = navBarPressScale
                    },
                contentAlignment = Alignment.CenterStart
            ) {
                // Outer Navigation Bar Capsule (refracts the background via backdrop)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(settingsButtonSize)
                        .align(Alignment.Center)
                        .liquidGlassInteractiveSurface(
                            shape = capsuleShape,
                            isCircle = false,
                            enabled = true,
                            backdrop = backdrop
                        )
                )

                // Keep droplet offset in sync when not dragging
                LaunchedEffect(selectedIndex, tabWidthPx) {
                    if (!isDragging && selectedIndex in 0 until tabCount && tabWidthPx > 0f) {
                        currentHoveredIndex = selectedIndex
                        dropletOffsetAnimatable.animateTo(
                            targetValue = selectedIndex * tabWidthPx,
                            animationSpec = spring(
                                dampingRatio = 0.6f,
                                stiffness = 400f
                            )
                        )
                    }
                }

                // Layer 2: Selection Pill (Reflects the navigation bar when bigger than the navigation bar)
                if (((selectedIndex in 0 until tabCount) || isDragging) && tabWidthPx > 0f) {
                    val dropletShape = RoundedCornerShape(percent = 50)
                    val dropletBorder = remember(primaryColor, isDark, chromaticAberration, tabSelectionColorMode) {
                        if (chromaticAberration) {
                            BorderStroke(
                                0.85.dp,
                                Brush.verticalGradient(
                                    colors = if (isDark) listOf(
                                        Color.White.copy(alpha = 0.65f),
                                        Color(0xFF80D8FF).copy(alpha = 0.35f),
                                        Color(0xFFFF80AB).copy(alpha = 0.28f),
                                        Color.White.copy(alpha = 0.14f)
                                    ) else listOf(
                                        Color.White.copy(alpha = 0.85f),
                                        Color(0xFF80D8FF).copy(alpha = 0.45f),
                                        Color(0xFFFF80AB).copy(alpha = 0.38f),
                                        Color.White.copy(alpha = 0.22f)
                                    )
                                )
                            )
                        } else {
                            BorderStroke(
                                0.85.dp,
                                Brush.verticalGradient(
                                    colors = if (isDark) listOf(
                                        Color.White.copy(alpha = 0.60f),
                                        if (tabSelectionColorMode == "PALETTE") primaryColor.copy(alpha = 0.30f) else Color.White.copy(alpha = 0.14f)
                                    ) else listOf(
                                        Color.White.copy(alpha = 0.80f),
                                        if (tabSelectionColorMode == "PALETTE") primaryColor.copy(alpha = 0.40f) else Color.White.copy(alpha = 0.22f)
                                    )
                                )
                            )
                        }
                    }

                    // Selection pill is always grey regardless of the user's tabSelectionColorMode
                    // preference. Only the outer nav bar capsule follows liquid glass settings.
                    val pillGlassBaseColor = if (isDark) Color.Gray.copy(alpha = 0.22f) else Color.Gray.copy(alpha = 0.35f)

                    // The selection pill is intentionally a crisp grey surface — no blur or
                    // refraction pipeline. The outer nav bar capsule handles the crystal-clear
                    // liquid glass effect via its own liquidGlassInteractiveSurface call.
                    // We still apply the theme-aware border and shape so the pill respects
                    // all liquid glass settings (chromatic aberration border, PALETTE tint, etc.).
                    val dropletModifier = Modifier
                        .graphicsLayer {
                            shadowElevation = 0f
                            scaleX = dropletStretchX
                            scaleY = dropletStretchY
                        }
                        .clip(dropletShape)
                        .background(color = pillGlassBaseColor, shape = dropletShape)
                        .border(BorderStroke(0.75.dp, glassBorderBrush), dropletShape)

                    val currentPillWidth = pillWidth
                    val currentPillWidthPx = with(density) { currentPillWidth.toPx() }
                    val horizontalCenteringOffsetPx = (tabWidthPx - currentPillWidthPx) / 2f

                    val reflectionAlpha by animateFloatAsState(
                        targetValue = if (isDropletActive || isDragging) 1f else 0f,
                        animationSpec = spring(
                            dampingRatio = 0.60f,
                            stiffness = 400f
                        ),
                        label = "nav_bar_reflection_alpha"
                    )

                    Box(
                        modifier = Modifier
                            .offset { IntOffset((dropletOffsetAnimatable.value + horizontalCenteringOffsetPx).roundToInt(), 0) }
                            .width(currentPillWidth)
                            .height(dropletHeight)
                            .align(Alignment.CenterStart)
                            .then(dropletModifier)
                    ) {
                        if (reflectionAlpha > 0.01f) {
                            // 1. Navigation bar body reflection sheen across the center intersection
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(settingsButtonSize)
                                    .align(Alignment.Center)
                                    .background(
                                        Brush.verticalGradient(
                                            colors = listOf(
                                                Color.White.copy(alpha = (if (isDark) 0.12f else 0.18f) * reflectionAlpha),
                                                Color.White.copy(alpha = (if (isDark) 0.03f else 0.06f) * reflectionAlpha),
                                                Color.White.copy(alpha = (if (isDark) 0.08f else 0.14f) * reflectionAlpha)
                                            )
                                        )
                                    )
                            )

                            // 2. Specular reflection line along the top intersection contour of the navigation bar
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(1.5.dp)
                                    .align(Alignment.Center)
                                    .offset(y = -(settingsButtonSize / 2f))
                                    .background(
                                        Brush.horizontalGradient(
                                            colors = listOf(
                                                Color.Transparent,
                                                Color.White.copy(alpha = (if (isDark) 0.50f else 0.70f) * reflectionAlpha),
                                                Color(0xFF80D8FF).copy(alpha = (if (isDark) 0.35f else 0.50f) * reflectionAlpha),
                                                Color.White.copy(alpha = (if (isDark) 0.50f else 0.70f) * reflectionAlpha),
                                                Color.Transparent
                                            )
                                        )
                                    )
                            )

                            // 3. Specular reflection line along the bottom intersection contour of the navigation bar
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(1.5.dp)
                                    .align(Alignment.Center)
                                    .offset(y = (settingsButtonSize / 2f))
                                    .background(
                                        Brush.horizontalGradient(
                                            colors = listOf(
                                                Color.Transparent,
                                                Color.White.copy(alpha = (if (isDark) 0.40f else 0.60f) * reflectionAlpha),
                                                Color(0xFFFF80AB).copy(alpha = (if (isDark) 0.25f else 0.40f) * reflectionAlpha),
                                                Color.White.copy(alpha = (if (isDark) 0.40f else 0.60f) * reflectionAlpha),
                                                Color.Transparent
                                            )
                                        )
                                    )
                            )

                            val currentPositionRatio = if (tabCount > 1 && tabWidthPx > 0f) {
                                (dropletOffsetAnimatable.value / ((tabCount - 1) * tabWidthPx)).coerceIn(0f, 1f)
                            } else 0.5f

                            // The selector's own continuous capsule rim handles its contour;
                            // zero these legacy overlay opacities so no separate circles are drawn.
                            val leftEdgeOverhangAlpha = 0f
                            val rightEdgeOverhangAlpha = 0f

                            // 4. Refraction & reflection along the navigation bar's left rounded end cap inside the pill
                            if (leftEdgeOverhangAlpha > 0.01f) {
                                val navBarLeftCapOffset = with(density) { (-horizontalCenteringOffsetPx).coerceAtLeast(0f).toDp() }
                                Box(
                                    modifier = Modifier
                                        .offset(x = navBarLeftCapOffset)
                                        .size(settingsButtonSize)
                                        .align(Alignment.CenterStart)
                                        .border(
                                            BorderStroke(
                                                1.dp,
                                                Brush.horizontalGradient(
                                                    colors = listOf(
                                                        Color.White.copy(alpha = (if (isDark) 0.65f else 0.85f) * leftEdgeOverhangAlpha),
                                                        Color(0xFF80D8FF).copy(alpha = (if (isDark) 0.40f else 0.55f) * leftEdgeOverhangAlpha),
                                                        Color.Transparent
                                                    )
                                                )
                                            ),
                                            shape = CircleShape
                                        )
                                )
                            }

                            // 5. Refraction & reflection along the navigation bar's right rounded end cap inside the pill
                            if (rightEdgeOverhangAlpha > 0.01f) {
                                val navBarRightCapOffset = with(density) { (-horizontalCenteringOffsetPx).coerceAtLeast(0f).toDp() }
                                Box(
                                    modifier = Modifier
                                        .offset(x = -navBarRightCapOffset)
                                        .size(settingsButtonSize)
                                        .align(Alignment.CenterEnd)
                                        .border(
                                            BorderStroke(
                                                1.dp,
                                                Brush.horizontalGradient(
                                                    colors = listOf(
                                                        Color.Transparent,
                                                        Color(0xFFFF80AB).copy(alpha = (if (isDark) 0.35f else 0.50f) * rightEdgeOverhangAlpha),
                                                        Color.White.copy(alpha = (if (isDark) 0.65f else 0.85f) * rightEdgeOverhangAlpha)
                                                    )
                                                )
                                            ),
                                            shape = CircleShape
                                        )
                                )
                            }
                        }

                        // Top outer glass rim specular highlight
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .align(Alignment.TopCenter)
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(
                                            Color.Transparent,
                                            Color.White.copy(alpha = if (isDark) 0.35f else 0.50f),
                                            Color.Transparent
                                        )
                                    )
                                )
                        )
                    }
                }

                // Layer 3: Interactive Row with tab icons and labels
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(settingsButtonSize)
                        .align(Alignment.Center)
                        .pointerInput(tabWidthPx, tabCount, selectedIndex) {
                            if (tabCount <= 0 || tabWidthPx <= 0f) return@pointerInput
                            awaitEachGesture {
                                val down = awaitFirstDown(requireUnconsumed = false)
                                isDropletActive = true
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)

                                val downX = down.position.x
                                val startX = dropletOffsetAnimatable.value
                                var hasDragged = false

                                try {
                                    while (true) {
                                        val event = awaitPointerEvent()
                                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                                        if (!change.pressed) {
                                            change.consume()
                                            break
                                        }
                                        val dx = change.position.x - downX
                                        if (kotlin.math.abs(dx) > 10f || hasDragged) {
                                            if (!hasDragged) {
                                                hasDragged = true
                                                isDragging = true
                                            }
                                            change.consume()
                                            val newX = (startX + dx).coerceIn(0f, (tabCount - 1) * tabWidthPx)
                                            coroutineScope.launch {
                                                dropletOffsetAnimatable.snapTo(newX)
                                            }
                                            val newHovered = ((newX + tabWidthPx / 2f) / tabWidthPx)
                                                .toInt().coerceIn(0, tabCount - 1)
                                            if (newHovered != currentHoveredIndex) {
                                                currentHoveredIndex = newHovered
                                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            }
                                        }
                                    }
                                } finally {
                                    isDropletActive = false
                                    isDragging = false
                                    val targetIndex = if (hasDragged) {
                                        ((dropletOffsetAnimatable.value + tabWidthPx / 2f) / tabWidthPx)
                                            .toInt().coerceIn(0, tabCount - 1)
                                    } else {
                                        (downX / tabWidthPx).toInt().coerceIn(0, tabCount - 1)
                                    }

                                    coroutineScope.launch {
                                        dropletOffsetAnimatable.animateTo(
                                            targetValue = targetIndex * tabWidthPx,
                                            animationSpec = spring(
                                                dampingRatio = 0.6f,
                                                stiffness = 400f
                                            )
                                        )
                                    }

                                    if (latestNavigationEnabled && targetIndex in 0 until tabCount) {
                                        if (targetIndex != selectedIndex) {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            navController.navigateToTopLevelSafely(mainItems[targetIndex].screen.route)
                                        }
                                    }
                                }
                            }
                        },
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    mainItems.forEachIndexed { index, item ->
                        val isSelected = index == (if (isDragging) currentHoveredIndex else selectedIndex)

                        val itemAlpha by animateFloatAsState(
                            targetValue = if (isSelected) 1f else 0.65f,
                            animationSpec = tween(200),
                            label = "tab_alpha_$index"
                        )

                        val iconScale by animateFloatAsState(
                            targetValue = if (isSelected) 1.06f else 1.0f,
                            animationSpec = spring(
                                dampingRatio = 0.50f,
                                stiffness = 380f
                            ),
                            label = "icon_scale_$index"
                        )

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clip(CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                                modifier = Modifier.fillMaxSize()
                            ) {
                                val iconRes = if (isSelected && item.selectedIconResId != null && item.selectedIconResId != 0) {
                                    item.selectedIconResId
                                } else {
                                    item.iconResId
                                }

                                Icon(
                                    painter = painterResource(id = iconRes),
                                    contentDescription = item.label,
                                    tint = (if (isDark) Color.White else Color.Black).copy(alpha = itemAlpha),
                                    modifier = Modifier
                                        .size(if (compactMode) 22.dp else 24.dp)
                                        .graphicsLayer {
                                            scaleX = iconScale
                                            scaleY = iconScale
                                        }
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = item.label,
                                    fontFamily = GoogleSansRounded,
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontSize = if (compactMode) 10.5.sp else 11.5.sp,
                                        lineHeight = if (compactMode) 12.sp else 13.sp,
                                        platformStyle = PlatformTextStyle(includeFontPadding = false)
                                    ),
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = (if (isDark) Color.White else Color.Black).copy(alpha = itemAlpha),
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    }
                }
            }
        } else {
            // Material 3 Standard Floating Toolbar
            HorizontalFloatingToolbar(
                modifier = Modifier.weight(1f),
                expanded = true,
                colors = FloatingToolbarDefaults.vibrantFloatingToolbarColors(
                    toolbarContentColor = MaterialTheme.colorScheme.onSurface,
                    toolbarContainerColor = MaterialTheme.colorScheme.primary
                ),
                content = {
                    Row(
                        modifier = Modifier.fillMaxWidth().height(toolbarRowHeight),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        mainItems.forEachIndexed { index, item ->
                            val isSelected = index == selectedIndex

                            val containerColor by animateColorAsState(
                                targetValue = if (isSelected) MaterialTheme.colorScheme.background else Color.Transparent,
                                animationSpec = tween(200),
                                label = "container_color_$index"
                            )

                            val contentColor by animateColorAsState(
                                targetValue = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.background,
                                animationSpec = tween(200),
                                label = "content_color_$index"
                            )

                            val animatedWeight by animateFloatAsState(
                                targetValue = if (isSelected) selectedItemWeight else 1f,
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness = Spring.StiffnessLow
                                ),
                                label = "weight_$index"
                            )

                            Surface(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    if (latestNavigationEnabled && latestCurrentRoute != item.screen.route) {
                                        navController.navigateToTopLevelSafely(item.screen.route)
                                    }
                                },
                                modifier = Modifier
                                    .weight(animatedWeight)
                                    .height(toolbarRowHeight),
                                shape = CircleShape,
                                color = containerColor,
                                contentColor = contentColor
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center,
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    val iconRes = if (isSelected && item.selectedIconResId != null && item.selectedIconResId != 0) {
                                        item.selectedIconResId
                                    } else {
                                        item.iconResId
                                    }

                                    Icon(
                                        painter = painterResource(id = iconRes),
                                        contentDescription = item.label,
                                        tint = contentColor,
                                        modifier = Modifier.size(itemIconSize)
                                    )

                                    AnimatedVisibility(
                                        visible = isSelected && !shouldHideLabel,
                                        enter = fadeIn(animationSpec = tween(200)) + expandHorizontally(expandFrom = Alignment.Start),
                                        exit = fadeOut(animationSpec = tween(150)) + shrinkHorizontally(shrinkTowards = Alignment.Start)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = item.label,
                                                fontFamily = GoogleSansRounded,
                                                style = if (compactMode) MaterialTheme.typography.labelMedium else MaterialTheme.typography.labelLarge,
                                                maxLines = 1,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary,
                                                softWrap = false
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            )
        }

        // Detached Search Button
        val isSearchSelected = latestCurrentRoute == Screen.Search.route
        val searchContentColor by animateColorAsState(
            targetValue = if (isSearchSelected) {
                if (isDark) Color.White else MaterialTheme.colorScheme.primary
            } else {
                if (isDark) Color.White.copy(alpha = 0.85f) else Color.Black.copy(alpha = 0.75f)
            },
            animationSpec = tween(200),
            label = "search_content_color"
        )

        if (isLiquidGlass) {
            val searchShape = CircleShape
            val searchButtonModifier = Modifier
                .size(settingsButtonSize)
                .liquidGlassInteractiveSurface(
                    shape = searchShape,
                    isCircle = true,
                    enabled = true,
                    backdrop = backdrop,
                    surfaceTint = if (isSearchSelected) primaryColor.copy(alpha = 0.40f) else null
                )
                .pointerInput(latestNavigationEnabled, isSearchSelected, onSearchIconDoubleTap) {
                    awaitEachGesture {
                        awaitFirstDown(requireUnconsumed = false)
                        val up = waitForUpOrCancellation()
                        if (up != null && latestNavigationEnabled) {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            if (!isSearchSelected) {
                                navController.navigateToTopLevelSafely(Screen.Search.route)
                            } else {
                                onSearchIconDoubleTap()
                            }
                        }
                    }
                }

            Box(
                modifier = searchButtonModifier,
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.rounded_search_24),
                    contentDescription = "Search",
                    tint = searchContentColor,
                    modifier = Modifier.size(itemIconSize)
                )
            }
        } else {
            Surface(
                onClick = {
                    if (!latestNavigationEnabled) return@Surface
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    if (!isSearchSelected) {
                        navController.navigateToTopLevelSafely(Screen.Search.route)
                    } else {
                        onSearchIconDoubleTap()
                    }
                },
                modifier = Modifier.size(settingsButtonSize),
                shape = CircleShape,
                color = if (isSearchSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primaryContainer,
                contentColor = if (isSearchSelected) MaterialTheme.colorScheme.background else MaterialTheme.colorScheme.onPrimaryContainer,
                shadowElevation = 8.dp
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Icon(
                        painter = painterResource(id = R.drawable.rounded_search_24),
                        contentDescription = "Search",
                        modifier = Modifier.size(itemIconSize)
                    )
                }
            }
        }
    }
}
