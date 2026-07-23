package com.unshoo.pixelmusic.presentation.components

import android.os.SystemClock
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.unshoo.pixelmusic.BottomNavItem
import com.unshoo.pixelmusic.presentation.navigation.Screen
import com.unshoo.pixelmusic.presentation.navigation.navigateToTopLevelSafely
import kotlinx.collections.immutable.ImmutableList

internal val NavBarContentHeight = 76.dp
internal val NavBarCompactContentHeight = 64.dp
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
    var lastSearchTapTimestamp by remember { mutableStateOf(0L) }

    val haptic = LocalHapticFeedback.current
    val configuration = LocalConfiguration.current
    val fontScale = LocalDensity.current.fontScale
    val screenWidth = configuration.screenWidthDp

    // Extract Search logic safely
    val searchItem = navItems.find { it.screen.route == Screen.Search.route }
    val mainItems = navItems.filter { it.screen.route != Screen.Search.route }

    val isLargeFont = fontScale > 1.25f
    val isCompactScreen = screenWidth < 400
    val shouldHideLabel = isLargeFont || (isCompactScreen && mainItems.size > 3)

    val selectedIndex = mainItems.indexOfFirst { it.screen.route == latestCurrentRoute }

    // Outer Row handles layout separation
    Row(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(start = 16.dp, end = 16.dp, bottom = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Original Working Pill (Now uses Modifier.weight(1f) to leave space for Search button)
        HorizontalFloatingToolbar(
            modifier = Modifier.weight(1f),
            expanded = true,
            colors = FloatingToolbarDefaults.vibrantFloatingToolbarColors(
                toolbarContentColor = MaterialTheme.colorScheme.onSurface,
                toolbarContainerColor = MaterialTheme.colorScheme.primary
            ),
            content = {
                mainItems.forEachIndexed { index, item ->
                    val isSelected = selectedIndex == index

                    val itemWidth by animateDpAsState(
                        targetValue = 48.dp,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow
                        ),
                        label = "item_width_$index"
                    )

                    val labelWidth by animateDpAsState(
                        targetValue = if (isSelected && !shouldHideLabel) 80.dp else 0.dp,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow
                        ),
                        label = "label_width_$index"
                    )

                    val spacerWidth by animateDpAsState(
                        targetValue = if (index < mainItems.size - 1) 8.dp else 0.dp,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow
                        ),
                        label = "spacer_width_$index"
                    )

                    Surface(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            if (latestNavigationEnabled && latestCurrentRoute != item.screen.route) {
                                navController.navigateToTopLevelSafely(item.screen.route)
                            }
                        },
                        modifier = Modifier
                            .width(itemWidth + labelWidth)
                            .height(48.dp),
                        shape = CircleShape,
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.background
                        } else {
                            MaterialTheme.colorScheme.primary
                        },
                        contentColor = if (isSelected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.background
                        }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 8.dp)
                        ) {
                            val iconRes = if (isSelected && item.selectedIconResId != null && item.selectedIconResId != 0) {
                                item.selectedIconResId
                            } else {
                                item.iconResId
                            }

                            Box(
                                modifier = Modifier.size(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    painter = painterResource(id = iconRes),
                                    contentDescription = item.label,
                                    tint = if (isSelected) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.background
                                    },
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            AnimatedVisibility(
                                visible = isSelected && !shouldHideLabel,
                                enter = fadeIn(animationSpec = tween(200)) + expandHorizontally(expandFrom = Alignment.Start),
                                exit = fadeOut(animationSpec = tween(150)) + shrinkHorizontally(shrinkTowards = Alignment.Start)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = item.label,
                                        style = MaterialTheme.typography.labelLarge,
                                        maxLines = 1,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        softWrap = false
                                    )
                                }
                            }
                        }
                    }

                    if (index < mainItems.size - 1) {
                        Spacer(modifier = Modifier.width(spacerWidth))
                    }
                }
            }
        )

        // Detached Custom Search Surface
        if (searchItem != null) {
            val isSearchSelected = latestCurrentRoute == searchItem.screen.route
            val searchIconRes = if (isSearchSelected && searchItem.selectedIconResId != null && searchItem.selectedIconResId != 0) searchItem.selectedIconResId else searchItem.iconResId
            
            Surface(
                onClick = {
                    if (!latestNavigationEnabled) return@Surface
                    
                    val now = SystemClock.elapsedRealtime()
                    val isDoubleTap = now - lastSearchTapTimestamp <= 350L
                    lastSearchTapTimestamp = now

                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)

                    if (!isSearchSelected) {
                        navController.navigateToTopLevelSafely(searchItem.screen.route)
                    } else if (isDoubleTap) {
                        lastSearchTapTimestamp = 0L
                        onSearchIconDoubleTap()
                    }
                },
                modifier = Modifier.size(64.dp), // Matches your original app's FAB footprint exactly
                shape = CircleShape,
                color = if (isSearchSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primaryContainer,
                contentColor = if (isSearchSelected) MaterialTheme.colorScheme.background else MaterialTheme.colorScheme.onPrimaryContainer,
                shadowElevation = 8.dp
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Icon(
                        painter = painterResource(id = searchIconRes),
                        contentDescription = searchItem.label,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}
