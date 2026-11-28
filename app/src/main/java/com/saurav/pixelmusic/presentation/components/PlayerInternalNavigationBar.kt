package com.saurav.pixelmusic.presentation.components

import android.os.SystemClock
import android.view.HapticFeedbackConstants
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
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
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
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.PlatformTextStyle
import kotlinx.coroutines.launch
import androidx.navigation.NavHostController
import com.saurav.pixelmusic.BottomNavItem
import com.saurav.pixelmusic.R
import com.saurav.pixelmusic.presentation.navigation.Screen
import com.saurav.pixelmusic.presentation.navigation.navigateSafely
import com.saurav.pixelmusic.presentation.navigation.navigateToSettingsSafely
import com.saurav.pixelmusic.presentation.navigation.navigateToTopLevelSafely
import racra.compose.smooth_corner_rect_library.AbsoluteSmoothCornerShape
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.ui.unit.coerceAtLeast
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
    onSearchIconDoubleTap: () -> Unit = {}
) {
    val latestCurrentRoute by rememberUpdatedState(currentRoute)
    val latestNavigationEnabled by rememberUpdatedState(currentRoute != null)

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
    val bottomPadding = 12.dp

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = horizontalOuterPadding, end = horizontalOuterPadding, bottom = bottomPadding),
        horizontalArrangement = Arrangement.spacedBy(itemSpacing),
        verticalAlignment = Alignment.CenterVertically
    ) {
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

        // Detached Search Button
        val isSearchSelected = latestCurrentRoute == Screen.Search.route
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

@Composable
fun PlayerInternalNavigationRail(
    navController: NavHostController,
    navItems: ImmutableList<BottomNavItem>,
    currentRoute: String?,
    modifier: Modifier = Modifier,
    profileAvatarSpec: String = "",
    profileDisplayName: String = "Guest",
    profileAvatarCacheKey: Long = 0L,
    onProfileClick: () -> Unit = { navController.navigateToSettingsSafely() },
    onSearchIconDoubleTap: () -> Unit = {}
) {
    val latestCurrentRoute by rememberUpdatedState(currentRoute)
    val latestNavigationEnabled by rememberUpdatedState(currentRoute != null)
    val haptic = LocalHapticFeedback.current
    val view = LocalView.current
    val coroutineScope = rememberCoroutineScope()
    val configuration = LocalConfiguration.current
    val isCompactHeight = configuration.screenHeightDp < 400

    val railItems = remember(navItems) {
        val base = navItems.filter { it.screen.route != Screen.Search.route }
        val searchItem = navItems.firstOrNull { it.screen.route == Screen.Search.route }
            ?: BottomNavItem(
                label = "Search",
                iconResId = R.drawable.rounded_search_24,
                selectedIconResId = R.drawable.rounded_search_24,
                screen = Screen.Search
            )
        (base + searchItem).toImmutableList()
    }

    val isSettingsSelected = remember(latestCurrentRoute) {
        val route = latestCurrentRoute ?: return@remember false
        route == Screen.Settings.route ||
            route.startsWith("settings") ||
            route == Screen.Profile.route ||
            route == Screen.Accounts.route ||
            route == Screen.About.route ||
            route == Screen.PixelJamSettings.route ||
            route == Screen.PaletteStyle.route ||
            route == Screen.NavBarCrRad.route ||
            route == Screen.Experimental.route ||
            route == Screen.ArtistSettings.route ||
            route == Screen.MusicLiveWallpaper.route ||
            route == Screen.DelimiterConfig.route ||
            route == Screen.WordDelimiterConfig.route ||
            route == Screen.DeviceCapabilities.route ||
            route == Screen.FeatureCompatibility.route
    }
    val isAtSettingsRoot = latestCurrentRoute == Screen.Settings.route

    val performExpressiveHaptic: (Boolean) -> Unit = remember(view, haptic) {
        { isPrimaryAction ->
            val constant = if (isPrimaryAction) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    HapticFeedbackConstants.CONFIRM
                } else {
                    HapticFeedbackConstants.VIRTUAL_KEY
                }
            } else {
                HapticFeedbackConstants.CLOCK_TICK
            }
            if (!view.performHapticFeedback(constant)) {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            }
        }
    }

    val capsuleSpacing = if (isCompactHeight) 2.dp else 4.dp
    val capsuleVerticalPadding = if (isCompactHeight) 6.dp else 8.dp
    val profileSpacing = if (isCompactHeight) 8.dp else 12.dp
    val profileButtonSize = if (isCompactHeight) 48.dp else 54.dp
    val profileAvatarSize = if (isCompactHeight) 38.dp else 42.dp

    Column(
        modifier = modifier
            .fillMaxHeight()
            .windowInsetsPadding(
                WindowInsets.displayCutout.only(WindowInsetsSides.Start)
            )
            .windowInsetsPadding(
                WindowInsets.navigationBars.only(WindowInsetsSides.Start)
            )
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(start = 10.dp, end = 8.dp, top = 6.dp, bottom = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Main Floating Material 3 Expressive Navigation Capsule
        Surface(
            shape = AbsoluteSmoothCornerShape(30.dp, 60),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 4.dp,
            shadowElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .width(68.dp)
                    .padding(vertical = capsuleVerticalPadding, horizontal = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(capsuleSpacing)
            ) {
                railItems.forEach { item ->
                    val isSelected = latestCurrentRoute == item.screen.route
                    val iconRes = if (isSelected && item.selectedIconResId != null && item.selectedIconResId != 0) {
                        item.selectedIconResId
                    } else {
                        item.iconResId
                    }

                    ExpressiveNavigationRailItem(
                        label = item.label,
                        iconRes = iconRes,
                        isSelected = isSelected,
                        compact = isCompactHeight,
                        onClick = {
                            if (!latestNavigationEnabled) return@ExpressiveNavigationRailItem
                            performExpressiveHaptic(!isSelected)
                            if (item.screen.route == Screen.Search.route && isSelected) {
                                onSearchIconDoubleTap()
                            } else if (!isSelected) {
                                navController.navigateToTopLevelSafely(item.screen.route)
                            }
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(profileSpacing))

        // Detached Profile / Settings Expressive Pill
        val profileInteractionSource = remember { MutableInteractionSource() }
        val isProfilePressed by profileInteractionSource.collectIsPressedAsState()
        val profilePressScale by animateFloatAsState(
            targetValue = if (isProfilePressed) 0.88f else 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMedium
            ),
            label = "rail_profile_press_scale"
        )
        val profileTapBounce = remember { Animatable(1f) }
        val profileCorner by animateDpAsState(
            targetValue = when {
                isProfilePressed -> 16.dp
                isSettingsSelected -> 18.dp
                else -> profileButtonSize / 2
            },
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMedium
            ),
            label = "rail_profile_corner"
        )
        val profileContainerColor by animateColorAsState(
            targetValue = if (isSettingsSelected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.surfaceContainerHigh
            },
            animationSpec = tween(220),
            label = "rail_profile_container_color"
        )

        Surface(
            onClick = {
                if (!latestNavigationEnabled) return@Surface
                performExpressiveHaptic(!isAtSettingsRoot)
                coroutineScope.launch {
                    profileTapBounce.snapTo(0.86f)
                    profileTapBounce.animateTo(
                        targetValue = 1f,
                        animationSpec = spring(
                            dampingRatio = 0.48f,
                            stiffness = Spring.StiffnessMediumLow
                        )
                    )
                }
                // Tapping while on a Settings sub-screen pops back to Settings root;
                // tapping from any other screen opens Settings cleanly.
                if (!isAtSettingsRoot) {
                    onProfileClick()
                }
            },
            interactionSource = profileInteractionSource,
            shape = AbsoluteSmoothCornerShape(profileCorner, 60),
            color = profileContainerColor,
            tonalElevation = 4.dp,
            shadowElevation = 6.dp,
            modifier = Modifier
                .size(profileButtonSize)
                .graphicsLayer {
                    val combinedScale = profilePressScale * profileTapBounce.value
                    scaleX = combinedScale
                    scaleY = combinedScale
                }
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                ProfileAvatar(
                    avatarSpec = profileAvatarSpec,
                    displayName = profileDisplayName,
                    size = profileAvatarSize,
                    cacheKey = profileAvatarCacheKey,
                    fallbackInitial = "G"
                )
            }
        }
    }
}

@Composable
private fun ExpressiveNavigationRailItem(
    label: String,
    iconRes: Int,
    isSelected: Boolean,
    compact: Boolean = false,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val coroutineScope = rememberCoroutineScope()

    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 0.90f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "rail_item_press_scale_$label"
    )
    val tapBounce = remember { Animatable(1f) }

    val indicatorHeight = if (compact) 30.dp else 34.dp
    val indicatorWidth by animateDpAsState(
        targetValue = when {
            isSelected -> 52.dp
            isPressed -> 44.dp
            else -> 0.dp
        },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "rail_item_indicator_width_$label"
    )
    val indicatorCorner by animateDpAsState(
        targetValue = when {
            isPressed -> 13.dp
            isSelected -> 17.dp
            else -> 24.dp
        },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "rail_item_indicator_corner_$label"
    )
    val indicatorColor by animateColorAsState(
        targetValue = when {
            isSelected -> MaterialTheme.colorScheme.primary
            isPressed -> MaterialTheme.colorScheme.surfaceContainerHighest
            else -> Color.Transparent
        },
        animationSpec = tween(180),
        label = "rail_item_indicator_color_$label"
    )
    val iconTint by animateColorAsState(
        targetValue = if (isSelected) {
            MaterialTheme.colorScheme.onPrimary
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        animationSpec = tween(200),
        label = "rail_item_icon_tint_$label"
    )
    val labelColor by animateColorAsState(
        targetValue = if (isSelected) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.82f)
        },
        animationSpec = tween(200),
        label = "rail_item_label_color_$label"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = pressScale
                scaleY = pressScale
            }
            .clip(AbsoluteSmoothCornerShape(20.dp, 60))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = {
                    coroutineScope.launch {
                        tapBounce.snapTo(0.84f)
                        tapBounce.animateTo(
                            targetValue = 1f,
                            animationSpec = spring(
                                dampingRatio = 0.45f,
                                stiffness = Spring.StiffnessMediumLow
                            )
                        )
                    }
                    onClick()
                }
            )
            .padding(vertical = if (compact) 2.dp else 3.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(width = 52.dp, height = indicatorHeight)
        ) {
            if (indicatorWidth > 1.dp) {
                Box(
                    modifier = Modifier
                        .size(width = indicatorWidth, height = indicatorHeight)
                        .clip(AbsoluteSmoothCornerShape(indicatorCorner, 60))
                        .background(indicatorColor)
                )
            }
            Icon(
                painter = painterResource(id = iconRes),
                contentDescription = label,
                tint = iconTint,
                modifier = Modifier
                    .size(if (compact) 20.dp else 21.dp)
                    .graphicsLayer {
                        scaleX = tapBounce.value
                        scaleY = tapBounce.value
                    }
            )
        }
        Spacer(modifier = Modifier.height(if (compact) 1.dp else 2.dp))
        Text(
            text = label,
            fontFamily = GoogleSansRounded,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            fontSize = if (compact) 10.sp else 10.5.sp,
            lineHeight = 12.sp,
            color = labelColor,
            maxLines = 1,
            softWrap = false,
            style = MaterialTheme.typography.labelSmall.copy(
                platformStyle = PlatformTextStyle(includeFontPadding = false)
            )
        )
    }
}
