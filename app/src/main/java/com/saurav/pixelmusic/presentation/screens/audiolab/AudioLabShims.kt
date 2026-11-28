package com.saurav.pixelmusic.presentation.screens.audiolab

import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.SwitchDefaults
import androidx.compose.foundation.Indication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.saurav.pixelmusic.R
import com.saurav.pixelmusic.presentation.components.MiniPlayerHeight
import com.saurav.pixelmusic.ui.blur.GlassState
import com.saurav.pixelmusic.ui.blur.ProgressiveHeaderBackdrop
import com.saurav.pixelmusic.ui.blur.glassRoot
import com.saurav.pixelmusic.ui.theme.PixelMusicStatusBarStyle

internal val InterFontFamily: FontFamily
    get() = com.saurav.pixelmusic.ui.theme.GoogleSansRounded
internal val RemoteFocus: Indication? = null

internal val WindowInsets.Companion.stableStatusBars: WindowInsets
    @Composable get() = WindowInsets.statusBars

internal val WindowInsets.Companion.stableSystemBars: WindowInsets
    @Composable get() = WindowInsets.systemBars

internal fun Modifier.damageBoundary(): Modifier = this.graphicsLayer()

internal fun Modifier.pressBounce(
    interactionSource: InteractionSource,
    pressedScale: Float = 0.97f,
): Modifier = composed {
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) pressedScale else 1f,
        animationSpec = spring(dampingRatio = 0.75f, stiffness = Spring.StiffnessMediumLow),
        label = "pressBounce",
    )
    this.graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}

internal object BlurEffectCache {
    @RequiresApi(Build.VERSION_CODES.S)
    fun element(radiusPx: Float, tileMode: Shader.TileMode): androidx.compose.ui.graphics.RenderEffect? {
        if (radiusPx < 0.5f) return null
        return RenderEffect.createBlurEffect(radiusPx, radiusPx, tileMode).asComposeRenderEffect()
    }
}

@Composable
internal fun AppSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = modifier,
        enabled = enabled,
        thumbContent = {
            AnimatedContent(
                targetState = checked,
                transitionSpec = { fadeIn(tween(100)) togetherWith fadeOut(tween(100)) },
                label = "switch_thumb_icon",
            ) { isChecked ->
                Icon(
                    imageVector = if (isChecked) Icons.Rounded.Check else Icons.Rounded.Close,
                    contentDescription = null,
                    modifier = Modifier.size(SwitchDefaults.IconSize),
                )
            }
        },
        colors = SwitchDefaults.colors(
            checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
            checkedTrackColor = MaterialTheme.colorScheme.primary,
            checkedIconColor = MaterialTheme.colorScheme.primary,
            uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
            uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant,
            uncheckedIconColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    )
}

@Composable
internal fun DefaultPlayerSeekBarSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    steps: Int = 0,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
) {
    val cs = MaterialTheme.colorScheme
    Slider(
        value = value,
        onValueChange = onValueChange,
        valueRange = valueRange,
        steps = steps,
        enabled = enabled,
        modifier = modifier.fillMaxWidth(),
        colors = SliderDefaults.colors(
            thumbColor = cs.primary,
            activeTrackColor = cs.primary,
            inactiveTrackColor = cs.surfaceContainerHighest,
            disabledThumbColor = cs.onSurface.copy(alpha = 0.38f),
            disabledActiveTrackColor = cs.onSurface.copy(alpha = 0.24f),
            disabledInactiveTrackColor = cs.surfaceContainerHighest.copy(alpha = 0.5f),
        ),
    )
}

@Composable
internal fun SubSettingsScaffold(
    title: String,
    backLabel: String,
    onBack: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    val bg = MaterialTheme.colorScheme.background
    PixelMusicStatusBarStyle(color = bg)
    val statusTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val scrollState = rememberScrollState()
    val glassState = remember { GlassState() }
    val blurAlpha = (scrollState.value / 120f).coerceIn(0f, 1f)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bg),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 680.dp)
                .fillMaxSize()
                .glassRoot(glassState, isActive = { !scrollState.isScrollInProgress })
                .verticalScroll(scrollState)
                .padding(top = statusTop + 14.dp, bottom = navBottom + MiniPlayerHeight + 36.dp)
                .padding(horizontal = 20.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable(onClick = onBack)
                    .padding(end = 10.dp, top = 4.dp, bottom = 4.dp),
            ) {
                Box(
                    Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_lab_arrow_back),
                        contentDescription = backLabel,
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(16.dp),
                    )
                }
                Spacer(Modifier.width(10.dp))
                Text(
                    text = backLabel,
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp,
                    color = LabColors.Secondary,
                )
            }
            Spacer(Modifier.height(14.dp))
            Text(
                text = title,
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 30.sp,
                color = LabColors.Primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(18.dp))
            content()
        }
        ProgressiveHeaderBackdrop(
            alpha = blurAlpha,
            scrimColor = bg,
            glassState = glassState,
            modifier = Modifier
                .fillMaxWidth()
                .height(statusTop + 72.dp)
                .align(Alignment.TopCenter),
        )
    }
}

@Composable
internal fun TextFieldDialog(
    title: @Composable () -> Unit,
    placeholder: @Composable () -> Unit,
    onDone: (String) -> Unit,
    onDismiss: () -> Unit,
    initialValue: String = "",
) {
    var text by remember(initialValue) { mutableStateOf(initialValue) }
    val cs = MaterialTheme.colorScheme
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = cs.surfaceContainerHigh,
        titleContentColor = cs.onSurface,
        textContentColor = cs.onSurface,
        title = title,
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                placeholder = placeholder,
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = cs.onSurface,
                    unfocusedTextColor = cs.onSurface,
                    focusedBorderColor = cs.primary,
                    unfocusedBorderColor = cs.outline,
                    cursorColor = cs.primary,
                ),
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val trimmed = text.trim()
                    if (trimmed.isNotEmpty()) {
                        onDone(trimmed)
                        onDismiss()
                    }
                }
            ) {
                Text("Save", color = cs.primary)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = cs.onSurfaceVariant)
            }
        },
    )
}

@Composable
internal fun ActionPromptDialog(
    title: String,
    onDismiss: () -> Unit,
    onCancel: () -> Unit,
    onConfirm: () -> Unit,
    content: @Composable ColumnScope.() -> Unit = {},
) {
    val cs = MaterialTheme.colorScheme
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = cs.surfaceContainerHigh,
        titleContentColor = cs.onSurface,
        textContentColor = cs.onSurfaceVariant,
        title = { Text(title, fontFamily = InterFontFamily, fontWeight = FontWeight.SemiBold) },
        text = { Column(content = content) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Delete", color = cs.error)
            }
        },
        dismissButton = {
            TextButton(onClick = onCancel) {
                Text("Cancel", color = cs.onSurfaceVariant)
            }
        },
    )
}

@Composable
internal fun SheetLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text.uppercase(),
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 11.sp,
        letterSpacing = 1.sp,
        color = LabColors.Secondary,
        modifier = modifier,
    )
}

@Composable
internal fun SheetOptionRow(
    title: String,
    subtitle: String,
    enabled: Boolean = true,
    onClick: () -> Unit,
    leading: @Composable () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(LabColors.Row)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp)
            .graphicsLayer { alpha = if (enabled) 1f else 0.45f },
    ) {
        leading()
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
                color = LabColors.Primary,
            )
            Text(
                text = subtitle,
                fontFamily = InterFontFamily,
                fontSize = 12.sp,
                color = LabColors.Secondary,
            )
        }
    }
}
