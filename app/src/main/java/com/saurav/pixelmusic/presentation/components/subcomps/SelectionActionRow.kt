package com.saurav.pixelmusic.presentation.components.subcomps

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Deselect
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.SelectAll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.saurav.pixelmusic.R
import com.saurav.pixelmusic.ui.theme.GoogleSansRounded
import com.saurav.pixelmusic.ui.theme.LocalLiquidGlassEnabled
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import racra.compose.smooth_corner_rect_library.AbsoluteSmoothCornerShape

private val buttonHeight = 44.dp
private val segmentedOuterCornerRadius = 26.dp
private val segmentedInnerCornerRadius = 8.dp
private val segmentGap = 3.dp
private val optionsGap = 4.dp

private fun Modifier.liquidGlassSelectionButton(
    shape: Shape,
    isLiquidGlass: Boolean,
    press: Animatable<Float, *>,
    jiggle: Animatable<Float, *>,
    isCircle: Boolean = false,
    borderBrush: Brush,
    coroutineScope: CoroutineScope,
    pressSpec: SpringSpec<Float>,
    jiggleSpec: SpringSpec<Float>
): Modifier {
    if (!isLiquidGlass) return this
    val scaleXMult = if (isCircle) 0.10f else 0.08f
    val jiggleXMult = if (isCircle) 0.15f else 0.12f
    val scaleYMult = if (isCircle) 0.08f else 0.06f
    val jiggleYMult = if (isCircle) 0.13f else 0.10f

    return this
        .graphicsLayer {
            scaleX = 1f + (press.value * scaleXMult) + (jiggle.value * jiggleXMult)
            scaleY = 1f - (press.value * scaleYMult) - (jiggle.value * jiggleYMult)
            shadowElevation = 0f
        }
        .pointerInput(Unit) {
            awaitEachGesture {
                awaitFirstDown(requireUnconsumed = false)
                coroutineScope.launch {
                    jiggle.snapTo(1f)
                    jiggle.animateTo(0f, jiggleSpec)
                }
                coroutineScope.launch { press.animateTo(1f, pressSpec) }
                waitForUpOrCancellation()
                coroutineScope.launch { press.animateTo(0f, pressSpec) }
            }
        }
        .border(BorderStroke(0.75.dp, borderBrush), shape)
        .clip(shape)
}

/**
 * Action row displayed during multi-selection mode.
 * Shows segmented Select All/Deselect buttons and Options button.
 * The selection count is shown as a floating pill separately.
 */
@Composable
fun SelectionActionRow(
    selectedCount: Int,
    onSelectAll: () -> Unit,
    onDeselect: () -> Unit,
    onOptionsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isLiquidGlass = LocalLiquidGlassEnabled.current
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val coroutineScope = rememberCoroutineScope()
    val pressSpec = remember { spring<Float>(dampingRatio = 0.5f, stiffness = 450f) }
    val jiggleSpec = remember { spring<Float>(dampingRatio = 0.32f, stiffness = 280f) }

    val glassBorderBrush = remember(isDark) {
        Brush.verticalGradient(
            colors = if (isDark) listOf(
                Color.White.copy(alpha = 0.07f),
                Color.White.copy(alpha = 0.02f)
            ) else listOf(
                Color.White.copy(alpha = 0.14f),
                Color.White.copy(alpha = 0.04f)
            )
        )
    }

    val selectAllPress = remember { Animatable(0f) }
    val selectAllJiggle = remember { Animatable(0f) }
    val deselectPress = remember { Animatable(0f) }
    val deselectJiggle = remember { Animatable(0f) }
    val counterPress = remember { Animatable(0f) }
    val counterJiggle = remember { Animatable(0f) }
    val optionsPress = remember { Animatable(0f) }
    val optionsJiggle = remember { Animatable(0f) }

    val leftSegmentShape = AbsoluteSmoothCornerShape(
        cornerRadiusTL = segmentedOuterCornerRadius,
        cornerRadiusBL = segmentedOuterCornerRadius,
        cornerRadiusTR = segmentedInnerCornerRadius,
        cornerRadiusBR = segmentedInnerCornerRadius,
        smoothnessAsPercentTL = 60,
        smoothnessAsPercentBL = 60,
        smoothnessAsPercentTR = 60,
        smoothnessAsPercentBR = 60
    )
    
    val rightSegmentShape = AbsoluteSmoothCornerShape(
        cornerRadiusTL = segmentedInnerCornerRadius,
        cornerRadiusBL = segmentedInnerCornerRadius,
        cornerRadiusTR = segmentedOuterCornerRadius,
        cornerRadiusBR = segmentedOuterCornerRadius,
        smoothnessAsPercentTL = 60,
        smoothnessAsPercentBL = 60,
        smoothnessAsPercentTR = 60,
        smoothnessAsPercentBR = 60
    )
    
    val optionsShape = AbsoluteSmoothCornerShape(
        cornerRadiusTL = segmentedOuterCornerRadius,
        cornerRadiusBL = segmentedOuterCornerRadius,
        cornerRadiusTR = segmentedOuterCornerRadius,
        cornerRadiusBR = segmentedOuterCornerRadius,
        smoothnessAsPercentTL = 60,
        smoothnessAsPercentBL = 60,
        smoothnessAsPercentTR = 60,
        smoothnessAsPercentBR = 60
    )
    
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        horizontalArrangement = Arrangement.Absolute.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Segmented buttons: Select All + Deselect
        Row(
            horizontalArrangement = Arrangement.spacedBy(segmentGap),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Select All button (left segment)
            FilledTonalButton(
                onClick = onSelectAll,
                shape = leftSegmentShape,
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = if (isLiquidGlass) MaterialTheme.colorScheme.secondaryContainer.copy(alpha = if (isDark) 0.55f else 0.70f) else MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                ),
                elevation = if (isLiquidGlass) ButtonDefaults.filledTonalButtonElevation(0.dp, 0.dp, 0.dp, 0.dp, 0.dp) else ButtonDefaults.filledTonalButtonElevation(),
                contentPadding = PaddingValues(horizontal = 14.dp),
                modifier = Modifier
                    .height(buttonHeight)
                    .liquidGlassSelectionButton(
                        shape = leftSegmentShape,
                        isLiquidGlass = isLiquidGlass,
                        press = selectAllPress,
                        jiggle = selectAllJiggle,
                        isCircle = false,
                        borderBrush = glassBorderBrush,
                        coroutineScope = coroutineScope,
                        pressSpec = pressSpec,
                        jiggleSpec = jiggleSpec
                    )
            ) {
                Icon(
                    imageVector = Icons.Rounded.SelectAll,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = stringResource(R.string.presentation_batch_g_selection_all),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Medium,
                    fontFamily = GoogleSansRounded
                )
            }
            
            // Deselect button (right segment) - using secondary colors
            FilledTonalButton(
                onClick = onDeselect,
                shape = rightSegmentShape,
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = if (isLiquidGlass) MaterialTheme.colorScheme.secondary.copy(alpha = if (isDark) 0.55f else 0.70f) else MaterialTheme.colorScheme.secondary,
                    contentColor = MaterialTheme.colorScheme.onSecondary
                ),
                elevation = if (isLiquidGlass) ButtonDefaults.filledTonalButtonElevation(0.dp, 0.dp, 0.dp, 0.dp, 0.dp) else ButtonDefaults.filledTonalButtonElevation(),
                contentPadding = PaddingValues(horizontal = 14.dp),
                modifier = Modifier
                    .height(buttonHeight)
                    .liquidGlassSelectionButton(
                        shape = rightSegmentShape,
                        isLiquidGlass = isLiquidGlass,
                        press = deselectPress,
                        jiggle = deselectJiggle,
                        isCircle = false,
                        borderBrush = glassBorderBrush,
                        coroutineScope = coroutineScope,
                        pressSpec = pressSpec,
                        jiggleSpec = jiggleSpec
                    )
            ) {
                Icon(
                    imageVector = Icons.Rounded.Deselect,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = stringResource(R.string.presentation_batch_g_selection_deselect),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Medium,
                    fontFamily = GoogleSansRounded
                )
            }
        }
        
        // Right side: Counter button + Options 3-dot button
        Row(
            horizontalArrangement = Arrangement.spacedBy(segmentGap),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (selectedCount > 0) {
                // Selected counter button (left segment of right group)
                FilledTonalButton(
                    onClick = onOptionsClick,
                    shape = leftSegmentShape,
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = if (isLiquidGlass) MaterialTheme.colorScheme.primaryContainer.copy(alpha = if (isDark) 0.55f else 0.70f) else MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                    elevation = if (isLiquidGlass) ButtonDefaults.filledTonalButtonElevation(0.dp, 0.dp, 0.dp, 0.dp, 0.dp) else ButtonDefaults.filledTonalButtonElevation(),
                    contentPadding = PaddingValues(horizontal = 12.dp),
                    modifier = Modifier
                        .height(buttonHeight)
                        .liquidGlassSelectionButton(
                            shape = leftSegmentShape,
                            isLiquidGlass = isLiquidGlass,
                            press = counterPress,
                            jiggle = counterJiggle,
                            isCircle = false,
                            borderBrush = glassBorderBrush,
                            coroutineScope = coroutineScope,
                            pressSpec = pressSpec,
                            jiggleSpec = jiggleSpec
                        )
                ) {
                    Icon(
                        imageVector = Icons.Rounded.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = selectedCount.toString(),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        fontFamily = GoogleSansRounded
                    )
                }
            }

            // Options 3-dot button (right segment of right group)
            val currentOptionsShape = if (selectedCount > 0) rightSegmentShape else optionsShape
            FilledTonalButton(
                onClick = onOptionsClick,
                shape = currentOptionsShape,
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = if (isLiquidGlass) MaterialTheme.colorScheme.primaryContainer.copy(alpha = if (isDark) 0.55f else 0.70f) else MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                elevation = if (isLiquidGlass) ButtonDefaults.filledTonalButtonElevation(0.dp, 0.dp, 0.dp, 0.dp, 0.dp) else ButtonDefaults.filledTonalButtonElevation(),
                contentPadding = PaddingValues(horizontal = 2.dp),
                modifier = Modifier
                    .height(buttonHeight)
                    .width(36.dp)
                    .liquidGlassSelectionButton(
                        shape = currentOptionsShape,
                        isLiquidGlass = isLiquidGlass,
                        press = optionsPress,
                        jiggle = optionsJiggle,
                        isCircle = selectedCount <= 0,
                        borderBrush = glassBorderBrush,
                        coroutineScope = coroutineScope,
                        pressSpec = pressSpec,
                        jiggleSpec = jiggleSpec
                    )
            ) {
                Icon(
                    imageVector = Icons.Rounded.MoreVert,
                    contentDescription = stringResource(R.string.presentation_batch_g_selection_cd_more),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

/**
 * Floating pill that shows the selection count only (no "selected" text).
 */
@Composable
fun SelectionCountPill(
    selectedCount: Int,
    modifier: Modifier = Modifier
) {
    val isLiquidGlass = LocalLiquidGlassEnabled.current
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f

    val glassBorderBrush = remember(isDark) {
        Brush.verticalGradient(
            colors = if (isDark) listOf(
                Color.White.copy(alpha = 0.07f),
                Color.White.copy(alpha = 0.02f)
            ) else listOf(
                Color.White.copy(alpha = 0.14f),
                Color.White.copy(alpha = 0.04f)
            )
        )
    }

    val pillShape = AbsoluteSmoothCornerShape(
        cornerRadiusTL = 20.dp,
        cornerRadiusBL = 20.dp,
        cornerRadiusTR = 20.dp,
        cornerRadiusBR = 20.dp,
        smoothnessAsPercentTL = 60,
        smoothnessAsPercentBL = 60,
        smoothnessAsPercentTR = 60,
        smoothnessAsPercentBR = 60
    )
    
    AnimatedVisibility(
        visible = selectedCount > 0,
        enter = slideInVertically(
            initialOffsetY = { -it },
            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
        ) + fadeIn() + scaleIn(initialScale = 0.8f),
        exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut() + scaleOut(targetScale = 0.8f),
        modifier = modifier
    ) {
        Surface(
            shape = pillShape,
            color = if (isLiquidGlass) MaterialTheme.colorScheme.primaryContainer.copy(alpha = if (isDark) 0.60f else 0.75f) else MaterialTheme.colorScheme.primaryContainer,
            shadowElevation = if (isLiquidGlass) 0.dp else 6.dp,
            tonalElevation = if (isLiquidGlass) 0.dp else 2.dp,
            modifier = Modifier
                .padding(8.dp)
                .then(
                    if (isLiquidGlass) {
                        Modifier
                            .graphicsLayer { shadowElevation = 0f }
                            .border(BorderStroke(0.75.dp, glassBorderBrush), pillShape)
                            .clip(pillShape)
                    } else Modifier
                )
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = selectedCount.toString(),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = GoogleSansRounded,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}
