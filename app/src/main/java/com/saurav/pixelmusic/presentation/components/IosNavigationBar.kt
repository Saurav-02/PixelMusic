package com.saurav.pixelmusic.presentation.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.saurav.pixelmusic.ui.theme.GoogleSansRounded
import com.saurav.pixelmusic.ui.theme.LocalLiquidGlassEnabled

/**
 * Convx-style iOS 26 navigation bar: a large title that shrinks into an inline one as the
 * content scrolls under it, with floating liquid glass toolbar capsules.
 *
 * @param collapsedFraction 0 = large title, 1 = inline. Drive it from a list state
 *   with [rememberIosNavigationBarCollapse].
 */
@Composable
fun IosNavigationBar(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    collapsedFraction: Float = 0f,
    navigationIcon: (@Composable RowScope.() -> Unit)? = null,
    actions: (@Composable RowScope.() -> Unit)? = null,
) {
    val collapse by animateFloatAsState(
        targetValue = collapsedFraction.coerceIn(0f, 1f),
        animationSpec = spring(dampingRatio = 1f, stiffness = 500f),
        label = "IosNavCollapse",
    )

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                navigationIcon?.invoke(this)
            }

            // Inline title (visible once large title scrolls away)
            Box(
                modifier = Modifier.graphicsLayer { alpha = collapse },
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = title,
                        fontFamily = GoogleSansRounded,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (subtitle != null) {
                        Text(
                            text = subtitle,
                            fontFamily = GoogleSansRounded,
                            fontWeight = FontWeight.Normal,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                actions?.invoke(this)
            }
        }

        if (collapse < 1f) {
            Column(
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .graphicsLayer {
                        alpha = 1f - collapse
                        translationY = -collapse * 12.dp.toPx()
                    }
            ) {
                Text(
                    text = title,
                    fontFamily = GoogleSansRounded,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 32.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        fontFamily = GoogleSansRounded,
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

/**
 * A group of toolbar buttons riding in one floating glass capsule (Convx/SwiftUI ToolbarItemGroup).
 */
@Composable
fun IosToolbarGroup(
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit,
) {
    val isLiquidGlass = LocalLiquidGlassEnabled.current
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val capsuleShape = CircleShape

    val glassBorderBrush = remember(isDark) {
        Brush.verticalGradient(
            colors = if (isDark) listOf(
                Color.White.copy(alpha = 0.12f),
                Color.White.copy(alpha = 0.03f)
            ) else listOf(
                Color.White.copy(alpha = 0.22f),
                Color.White.copy(alpha = 0.06f)
            )
        )
    }

    Row(
        modifier = modifier
            .height(44.dp)
            .then(
                if (isLiquidGlass) {
                    Modifier.liquidGlassInteractiveSurface(shape = capsuleShape, enabled = true)
                } else {
                    Modifier
                        .clip(capsuleShape)
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh, capsuleShape)
                        .border(BorderStroke(0.75.dp, glassBorderBrush), capsuleShape)
                }
            )
            .padding(horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        CompositionLocalProvider(
            LocalContentColor provides MaterialTheme.colorScheme.onSurface,
            LocalTextStyle provides MaterialTheme.typography.bodyMedium,
        ) {
            content()
        }
    }
}

/** Convx/SwiftUI ToolbarSpacer: the gap that breaks one capsule into two. */
@Composable
fun IosToolbarSpacer() = Box(Modifier.padding(horizontal = 3.dp))

/**
 * Drives [IosNavigationBar]'s collapse from a LazyListState.
 */
@Composable
fun rememberIosNavigationBarCollapse(state: LazyListState, distance: Float = 120f): Float {
    val fraction by remember(state, distance) {
        derivedStateOf {
            if (state.firstVisibleItemIndex > 0) 1f
            else (state.firstVisibleItemScrollOffset / distance).coerceIn(0f, 1f)
        }
    }
    return fraction
}
