package com.saurav.pixelmusic.presentation.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupProperties
import com.saurav.pixelmusic.ui.theme.LocalLiquidGlassEnabled

@Composable
fun LiquidGlassDropdownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    offset: DpOffset = DpOffset(0.dp, 0.dp),
    scrollState: ScrollState = rememberScrollState(),
    properties: PopupProperties = PopupProperties(focusable = true),
    content: @Composable ColumnScope.() -> Unit
) {
    val isLiquidGlass = LocalLiquidGlassEnabled.current
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f

    val menuShape = RoundedCornerShape(18.dp)
    val containerColor = if (isLiquidGlass) {
        if (isDark) Color(0xF21C1C1E) else Color(0xF8FFFFFF)
    } else {
        MaterialTheme.colorScheme.surfaceContainer
    }

    val borderStroke = remember(isLiquidGlass, isDark) {
        if (!isLiquidGlass) null
        else BorderStroke(
            0.75.dp,
            Brush.verticalGradient(
                listOf(
                    Color.White.copy(alpha = if (isDark) 0.16f else 0.30f),
                    Color.White.copy(alpha = if (isDark) 0.04f else 0.08f)
                )
            )
        )
    }

    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        offset = offset,
        scrollState = scrollState,
        properties = properties,
        shape = menuShape,
        containerColor = containerColor,
        tonalElevation = if (isLiquidGlass) 0.dp else 3.dp,
        shadowElevation = if (isLiquidGlass) 8.dp else 3.dp,
        border = borderStroke,
        content = content
    )
}
