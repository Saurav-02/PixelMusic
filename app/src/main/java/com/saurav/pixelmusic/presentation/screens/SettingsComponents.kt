package com.saurav.pixelmusic.presentation.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.lerp
import kotlin.math.hypot
import kotlin.math.max
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalContext
import android.os.Build
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.saurav.pixelmusic.R

import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.onGloballyPositioned
import com.saurav.pixelmusic.data.preferences.AppThemeMode
import com.saurav.pixelmusic.ui.theme.GoogleSansRounded
import com.saurav.pixelmusic.ui.theme.LocalThemeTransitionController
import com.saurav.pixelmusic.ui.theme.PaletteSwatchPreview
import com.saurav.pixelmusic.ui.theme.getPaletteSwatchPreview
import androidx.compose.ui.graphics.graphicsLayer
import kotlinx.coroutines.launch

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import com.saurav.pixelmusic.data.worker.SyncProgress
import com.saurav.pixelmusic.presentation.viewmodel.LyricsRefreshProgress
import com.saurav.pixelmusic.ui.theme.GoogleSansRounded
import androidx.compose.ui.res.vectorResource
import androidx.core.view.HapticFeedbackConstantsCompat
import com.saurav.pixelmusic.presentation.utils.LocalAppHapticsConfig
import com.saurav.pixelmusic.presentation.utils.performAppCompatHapticFeedback
import kotlinx.coroutines.delay

/**
 * When non-null, any SettingsItem / SwitchSettingItem / ThemeSelectorItem whose
 * `title` (or `label`) matches this value will pulse with a primary border —
 * used by the settings-search feature to draw attention to the tapped setting.
 */
val LocalSettingsHighlightTitle = compositionLocalOf<String?> { null }

/**
 * Full-card tonal highlight pulse (no border or stroke). Gently pulses the entire
 * card surface with a primary tint to draw attention to the navigated setting,
 * then fades out smoothly.
 */
@Composable
internal fun BoxScope.HighlightPulseOverlay(shape: Shape) {
    val alpha = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        delay(120)
        repeat(3) {
            alpha.animateTo(
                targetValue = 0.26f,
                animationSpec = tween(durationMillis = 420, easing = FastOutSlowInEasing)
            )
            alpha.animateTo(
                targetValue = 0.06f,
                animationSpec = tween(durationMillis = 420, easing = FastOutSlowInEasing)
            )
        }
        alpha.animateTo(
            targetValue = 0f,
            animationSpec = tween(durationMillis = 460, easing = FastOutSlowInEasing)
        )
    }
    Box(
        modifier = Modifier
            .matchParentSize()
            .clip(shape)
            .background(
                color = MaterialTheme.colorScheme.primary.copy(alpha = alpha.value),
                shape = shape
            )
    )
}

/**
 * Auto-scrolls the highlighted row into view once the screen has settled.
 * No-op when not highlighted.
 */
internal fun Modifier.autoBringIntoViewWhenHighlighted(isHighlighted: Boolean): Modifier = composed {
    if (!isHighlighted) return@composed this
    val requester = remember { BringIntoViewRequester() }
    LaunchedEffect(Unit) {
        // Wait for top-bar collapse animation + screen transition to finish
        delay(450)
        runCatching { requester.bringIntoView() }
    }
    this.bringIntoViewRequester(requester)
}

@Composable
fun SettingsSection(title: String, icon: @Composable () -> Unit, content: @Composable () -> Unit) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(vertical = 8.dp)
        ) {
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
            )
        }
        content()
    }
}

@Composable
fun SettingsItem(
        title: String,
        subtitle: String,
        leadingIcon: @Composable () -> Unit,
        trailingIcon: @Composable () -> Unit = {},
        onClick: () -> Unit
) {
    val highlightTitle = LocalSettingsHighlightTitle.current
    val highlighted = highlightTitle != null && title.equals(highlightTitle, ignoreCase = true)
    val shape = RoundedCornerShape(10.dp)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .autoBringIntoViewWhenHighlighted(highlighted)
    ) {
        Surface(
                color = MaterialTheme.colorScheme.surfaceContainer,
                modifier =
                        Modifier.fillMaxWidth()
                                .clip(shape)
                                .clickable(onClick = onClick)
        ) {
            Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(16.dp).fillMaxWidth()
            ) {
                Box(
                        modifier = Modifier.padding(end = 16.dp).size(24.dp),
                        contentAlignment = Alignment.Center
                ) { leadingIcon() }

                Column(
                        modifier = Modifier.weight(1f).padding(end = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Box(modifier = Modifier.size(24.dp), contentAlignment = Alignment.Center) {
                    trailingIcon()
                }
            }
        }
        if (highlighted) {
            HighlightPulseOverlay(shape = shape)
        }
    }
}

@Composable
fun SwitchSettingItem(
        modifier: Modifier = Modifier,
        title: String,
        subtitle: String,
        checked: Boolean,
        onCheckedChange: (Boolean) -> Unit,
        leadingIcon: @Composable (() -> Unit)? = null,
        enabled: Boolean = true,
        onDisabledClick: (() -> Unit)? = null
) {
    val view = LocalView.current
    val appHapticsConfig = LocalAppHapticsConfig.current
    val highlightTitle = LocalSettingsHighlightTitle.current
    val highlighted = highlightTitle != null && (
        title.equals(highlightTitle, ignoreCase = true) ||
            (highlightTitle.equals("Live Activity / Dynamic Island", ignoreCase = true) &&
                title.contains("Dynamic Island", ignoreCase = true))
        )
    val shape = RoundedCornerShape(10.dp)
    val themeTransitionController = LocalThemeTransitionController.current
    var switchCenter by remember { mutableStateOf(Offset.Zero) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .autoBringIntoViewWhenHighlighted(highlighted)
            .onGloballyPositioned { coords ->
                val pos = coords.positionInRoot()
                switchCenter = pos + Offset(coords.size.width - 48f, coords.size.height / 2f)
            }
    ) {
        Surface(
                color = MaterialTheme.colorScheme.surfaceContainer,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(shape)
                    .then(
                        if (!enabled && onDisabledClick != null) {
                            Modifier.clickable { onDisabledClick() }
                        } else {
                            Modifier
                        }
                    )
                    .graphicsLayer { alpha = if (enabled) 1f else 0.48f }
        ) {
            Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (leadingIcon != null) {
                    Box(
                            modifier = Modifier.padding(end = 4.dp).size(24.dp),
                            contentAlignment = Alignment.Center
                    ) { leadingIcon() }
                }

                Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium,
                            color =
                                    if (enabled) MaterialTheme.colorScheme.onSurface
                                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodyMedium,
                            color =
                                    if (enabled) MaterialTheme.colorScheme.onSurfaceVariant
                                    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }

                Switch(
                    checked = checked,
                    onCheckedChange = { newValue ->
                        if (enabled) {
                            performAppCompatHapticFeedback(
                                view,
                                appHapticsConfig,
                                HapticFeedbackConstantsCompat.GESTURE_START
                            )
                            themeTransitionController?.lastTouchOffset = switchCenter
                            onCheckedChange(newValue)
                        }
                    },
                    enabled = enabled,
                    thumbContent = {
                        AnimatedContent(
                            targetState = checked,
                            transitionSpec = { fadeIn(tween(100)) togetherWith fadeOut(tween(100)) },
                            label = "switch_thumb_icon"
                        ) { isChecked ->
                            Icon(
                                imageVector = if (isChecked) Icons.Rounded.Check else Icons.Rounded.Close,
                                contentDescription = null,
                                modifier = Modifier.size(SwitchDefaults.IconSize)
                            )
                        }
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                        checkedTrackColor = MaterialTheme.colorScheme.primary,
                        checkedIconColor = MaterialTheme.colorScheme.primary,
                        uncheckedThumbColor = MaterialTheme.colorScheme.onSurface,
                        uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant,
                        uncheckedIconColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                )
            }
        }
        if (highlighted) {
            HighlightPulseOverlay(shape = shape)
        }
    }
}

@Composable
fun ThemeModeSelectorCard(
    modifier: Modifier = Modifier,
    selectedMode: String,
    onModeSelected: (String, Offset) -> Unit
) {
    val highlightTitle = LocalSettingsHighlightTitle.current
    val title = stringResource(R.string.setcat_app_theme_label)
    val highlighted = highlightTitle != null && title == highlightTitle
    val shape = RoundedCornerShape(10.dp)
    val view = LocalView.current
    val appHapticsConfig = LocalAppHapticsConfig.current
    val themeTransitionController = LocalThemeTransitionController.current
    var showSheet by remember { mutableStateOf(false) }

    val modeLabel = when (selectedMode) {
        AppThemeMode.DARK -> stringResource(R.string.setcat_theme_dark)
        AppThemeMode.LIGHT -> stringResource(R.string.setcat_theme_light)
        else -> stringResource(R.string.setcat_theme_follow_system)
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .autoBringIntoViewWhenHighlighted(highlighted)
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainer,
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape)
                .clickable {
                    showSheet = true
                }
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                // Leading Icon
                Box(
                    modifier = Modifier
                        .padding(end = 16.dp)
                        .size(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(R.drawable.rounded_palette_24),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary
                    )
                }

                // Title and Subtitle
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = modeLabel,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Single animated theme toggle icon button
                var buttonCenter by remember { mutableStateOf(Offset.Zero) }
                Surface(
                    onClick = {
                        performAppCompatHapticFeedback(
                            view,
                            appHapticsConfig,
                            HapticFeedbackConstantsCompat.GESTURE_START
                        )
                        val nextMode = when (selectedMode) {
                            AppThemeMode.DARK -> AppThemeMode.LIGHT
                            AppThemeMode.LIGHT -> AppThemeMode.FOLLOW_SYSTEM
                            else -> AppThemeMode.DARK
                        }
                        onModeSelected(nextMode, buttonCenter)
                    },
                    shape = androidx.compose.foundation.shape.CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    tonalElevation = 2.dp,
                    shadowElevation = 1.dp,
                    modifier = Modifier
                        .size(48.dp)
                        .onGloballyPositioned { coords ->
                            val pos = coords.positionInRoot()
                            buttonCenter = pos + Offset(coords.size.width / 2f, coords.size.height / 2f)
                        }
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        AnimatedContent(
                            targetState = selectedMode,
                            transitionSpec = {
                                fadeIn(tween(250)) togetherWith fadeOut(tween(150))
                            },
                            label = "ThemeToggleIcon"
                        ) { mode ->
                            when (mode) {
                                AppThemeMode.DARK -> {
                                    val liftAnim = remember { Animatable(16f) }
                                    LaunchedEffect(Unit) {
                                        liftAnim.animateTo(
                                            targetValue = 0f,
                                            animationSpec = spring(
                                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                                stiffness = Spring.StiffnessLow
                                            )
                                        )
                                    }
                                    Icon(
                                        painter = painterResource(R.drawable.rounded_dark_mode_24),
                                        contentDescription = stringResource(R.string.setcat_theme_dark),
                                        modifier = Modifier
                                            .size(24.dp)
                                            .graphicsLayer {
                                                translationY = liftAnim.value
                                            },
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                                AppThemeMode.LIGHT -> {
                                    val rotateAnim = remember { Animatable(0f) }
                                    LaunchedEffect(Unit) {
                                        rotateAnim.animateTo(
                                            targetValue = 360f,
                                            animationSpec = tween(durationMillis = 850, easing = FastOutSlowInEasing)
                                        )
                                    }
                                    Icon(
                                        painter = painterResource(R.drawable.rounded_light_mode_24),
                                        contentDescription = stringResource(R.string.setcat_theme_light),
                                        modifier = Modifier
                                            .size(24.dp)
                                            .graphicsLayer {
                                                rotationZ = rotateAnim.value
                                            },
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                                else -> {
                                    val scaleAnim = remember { Animatable(0.5f) }
                                    val rotateAnim = remember { Animatable(-60f) }
                                    LaunchedEffect(Unit) {
                                        launch {
                                            scaleAnim.animateTo(
                                                targetValue = 1f,
                                                animationSpec = spring(
                                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                                    stiffness = Spring.StiffnessMedium
                                                )
                                            )
                                        }
                                        launch {
                                            rotateAnim.animateTo(
                                                targetValue = 0f,
                                                animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing)
                                            )
                                        }
                                    }
                                    Icon(
                                        painter = painterResource(R.drawable.rounded_auto_awesome_24),
                                        contentDescription = stringResource(R.string.setcat_theme_follow_system),
                                        modifier = Modifier
                                            .size(24.dp)
                                            .graphicsLayer {
                                                scaleX = scaleAnim.value
                                                scaleY = scaleAnim.value
                                                rotationZ = rotateAnim.value
                                            },
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        if (highlighted) {
            HighlightPulseOverlay(shape = shape)
        }
    }

    if (showSheet) {
        val optionEntries = listOf(
            AppThemeMode.DARK to "${stringResource(R.string.setcat_theme_dark)} (${stringResource(R.string.setup_recommended)})",
            AppThemeMode.LIGHT to stringResource(R.string.setcat_theme_light),
            AppThemeMode.FOLLOW_SYSTEM to stringResource(R.string.setcat_theme_follow_system)
        )
        com.saurav.pixelmusic.presentation.components.SmoothModalBottomSheet(
            onDismissRequest = { showSheet = false },
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            contentColor = MaterialTheme.colorScheme.onSurface,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
        ) {
            var pendingSelectedMode by remember(selectedMode) { mutableStateOf(selectedMode) }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(bottom = 20.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(horizontal = 22.dp, vertical = 14.dp),
                    fontWeight = FontWeight.Bold
                )

                LazyColumn(
                    modifier = Modifier
                        .padding(horizontal = 20.dp)
                        .heightIn(max = 420.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    itemsIndexed(optionEntries, key = { _, entry -> entry.first }) { idx, (key, optionLabel) ->
                        val isSelected = key == pendingSelectedMode
                        var optionCenter by remember { mutableStateOf(Offset.Zero) }

                        ExpressivePreferenceOptionCard(
                            label = optionLabel,
                            isSelected = isSelected,
                            index = idx,
                            totalCount = optionEntries.size,
                            onPositioned = { optionCenter = it },
                            onClick = {
                                performAppCompatHapticFeedback(
                                    view,
                                    appHapticsConfig,
                                    HapticFeedbackConstantsCompat.GESTURE_START
                                )
                                themeTransitionController?.lastTouchOffset = optionCenter
                                pendingSelectedMode = key
                                onModeSelected(key, optionCenter)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PalettePreviewCircle(
    palette: String,
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp = 28.dp,
    animated: Boolean = false
) {
    val defaultColor = MaterialTheme.colorScheme.primary
    val bgColor = MaterialTheme.colorScheme.surfaceContainerHighest
    val context = LocalContext.current
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f

    // Dynamically retrieve the system's detected Material You Monet palette on Android 12+
    val dynamicSwatch = remember(context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            try {
                val scheme = dynamicDarkColorScheme(context)
                PaletteSwatchPreview(
                    top = scheme.primary,
                    bottomLeft = scheme.secondary,
                    bottomRight = scheme.tertiary
                )
            } catch (_: Throwable) {
                null
            }
        } else {
            null
        }
    }

    val swatchPreview = remember(palette, dynamicSwatch) {
        if (palette == "DYNAMIC") {
            dynamicSwatch ?: PaletteSwatchPreview(
                top = Color(0xFFFFB68E),
                bottomLeft = Color(0xFFE6BFA8),
                bottomRight = Color(0xFFCBC992)
            )
        } else {
            getPaletteSwatchPreview(palette)
        }
    }

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape),
        contentAlignment = Alignment.Center
    ) {
        when {
            palette == "ALBUM_ART" -> {
                val spinAnim = remember { Animatable(if (animated) 0f else 360f) }
                if (animated) {
                    LaunchedEffect(Unit) {
                        spinAnim.animateTo(
                            targetValue = 360f,
                            animationSpec = tween(durationMillis = 700, easing = FastOutSlowInEasing)
                        )
                    }
                }
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer { rotationZ = spinAnim.value }
                ) {
                    val w = this.size.width
                    drawCircle(
                        brush = Brush.sweepGradient(
                            listOf(
                                Color(0xFFF472B6),
                                Color(0xFFA78BFA),
                                Color(0xFF60A5FA),
                                Color(0xFF34D399),
                                Color(0xFFFBBF24),
                                Color(0xFFF472B6)
                            )
                        )
                    )
                    drawCircle(Color(0x33000000), radius = w * 0.36f)
                    drawCircle(Color(0x66000000), radius = w * 0.22f)
                    drawCircle(Color.White.copy(alpha = 0.9f), radius = w * 0.08f)
                }
            }
            swatchPreview != null -> {
                val bloomScale = remember { Animatable(if (animated) 0.72f else 1f) }
                if (animated) {
                    LaunchedEffect(Unit) {
                        bloomScale.animateTo(
                            targetValue = 1f,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessMediumLow
                            )
                        )
                    }
                }
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            scaleX = bloomScale.value
                            scaleY = bloomScale.value
                        }
                ) {
                    // Classic Material You 3-slice swatch circle:
                    // Top half (180°): primary tone
                    // Bottom-left quarter (90°): secondary tone
                    // Bottom-right quarter (90°): tertiary tone
                    drawArc(
                        color = swatchPreview.top,
                        startAngle = 180f,
                        sweepAngle = 180f,
                        useCenter = true
                    )
                    drawArc(
                        color = swatchPreview.bottomLeft,
                        startAngle = 90f,
                        sweepAngle = 90f,
                        useCenter = true
                    )
                    drawArc(
                        color = swatchPreview.bottomRight,
                        startAngle = 0f,
                        sweepAngle = 90f,
                        useCenter = true
                    )
                }
            }
            else -> {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawCircle(defaultColor)
                }
            }
        }
    }
}

@Composable
fun ColorPaletteSelectorCard(
    modifier: Modifier = Modifier,
    selectedPalette: String,
    options: Map<String, String>,
    onPaletteSelected: (String, Offset) -> Unit
) {
    val highlightTitle = LocalSettingsHighlightTitle.current
    val title = stringResource(R.string.setcat_app_color_palette_label)
    val highlighted = highlightTitle != null && title == highlightTitle
    val shape = RoundedCornerShape(10.dp)
    val view = LocalView.current
    val appHapticsConfig = LocalAppHapticsConfig.current
    val themeTransitionController = LocalThemeTransitionController.current
    var showSheet by remember { mutableStateOf(false) }

    val rawSelectedLabel = options[selectedPalette] ?: selectedPalette
    val selectedLabel = rawSelectedLabel.removeSuffix(" (Beta)")

    Box(
        modifier = modifier
            .fillMaxWidth()
            .autoBringIntoViewWhenHighlighted(highlighted)
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainer,
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape)
                .clickable {
                    showSheet = true
                }
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                // Leading Icon
                Box(
                    modifier = Modifier
                        .padding(end = 16.dp)
                        .size(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(R.drawable.rounded_palette_24),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary
                    )
                }

                // Title and Subtitle
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = selectedLabel,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Single animated palette toggle circle button
                var buttonCenter by remember { mutableStateOf(Offset.Zero) }
                Surface(
                    onClick = {
                        performAppCompatHapticFeedback(
                            view,
                            appHapticsConfig,
                            HapticFeedbackConstantsCompat.GESTURE_START
                        )
                        val paletteKeys = options.keys.toList()
                        val currentIndex = paletteKeys.indexOf(selectedPalette).takeIf { it >= 0 } ?: 0
                        val nextPalette = paletteKeys[(currentIndex + 1) % paletteKeys.size]
                        onPaletteSelected(nextPalette, buttonCenter)
                    },
                    shape = androidx.compose.foundation.shape.CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    tonalElevation = 2.dp,
                    shadowElevation = 1.dp,
                    modifier = Modifier
                        .size(48.dp)
                        .onGloballyPositioned { coords ->
                            val pos = coords.positionInRoot()
                            buttonCenter = pos + Offset(coords.size.width / 2f, coords.size.height / 2f)
                        }
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        AnimatedContent(
                            targetState = selectedPalette,
                            transitionSpec = {
                                (scaleIn(tween(250)) + fadeIn(tween(250))) togetherWith
                                    (scaleOut(tween(150)) + fadeOut(tween(150)))
                            },
                            label = "PaletteToggleCircle"
                        ) { palette ->
                            PalettePreviewCircle(
                                palette = palette,
                                size = 28.dp,
                                animated = true
                            )
                        }
                    }
                }
            }
        }
        if (highlighted) {
            HighlightPulseOverlay(shape = shape)
        }
    }

    if (showSheet) {
        com.saurav.pixelmusic.presentation.components.SmoothModalBottomSheet(
            onDismissRequest = { showSheet = false },
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            contentColor = MaterialTheme.colorScheme.onSurface,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
        ) {
            val smoothDismiss = com.saurav.pixelmusic.presentation.components.LocalSmoothDismiss.current
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 24.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontFamily = GoogleSansRounded,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Pick a color palette to theme the app interface.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(20.dp))

                val rows = remember(options) { options.entries.toList().chunked(3) }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 460.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    rows.forEach { rowOptions ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            rowOptions.forEach { (key, optionLabel) ->
                                val isSelected = key == selectedPalette
                                val cleanOptionLabel = optionLabel.removeSuffix(" (Beta)")
                                var optionCenter by remember { mutableStateOf(Offset.Zero) }

                                val cardColor by animateColorAsState(
                                    targetValue = if (isSelected) {
                                        MaterialTheme.colorScheme.primaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.surfaceContainerHigh
                                    },
                                    animationSpec = tween(220),
                                    label = "palette_card_bg"
                                )
                                val labelColor by animateColorAsState(
                                    targetValue = if (isSelected) {
                                        MaterialTheme.colorScheme.onPrimaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.onSurface
                                    },
                                    animationSpec = tween(220),
                                    label = "palette_label_color"
                                )

                                Surface(
                                    onClick = {
                                        performAppCompatHapticFeedback(
                                            view,
                                            appHapticsConfig,
                                            HapticFeedbackConstantsCompat.GESTURE_START
                                        )
                                        themeTransitionController?.lastTouchOffset = optionCenter
                                        onPaletteSelected(key, optionCenter)
                                        smoothDismiss(null)
                                    },
                                    shape = RoundedCornerShape(22.dp),
                                    color = cardColor,
                                    modifier = Modifier
                                        .weight(1f)
                                        .onGloballyPositioned { coords ->
                                            val pos = coords.positionInRoot()
                                            optionCenter = pos + Offset(coords.size.width / 2f, coords.size.height / 2f)
                                        }
                                ) {
                                    Box(modifier = Modifier.fillMaxWidth()) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 14.dp, horizontal = 8.dp)
                                        ) {
                                            PalettePreviewCircle(
                                                palette = key,
                                                size = 56.dp,
                                                animated = isSelected
                                            )
                                            Spacer(modifier = Modifier.height(10.dp))
                                            Text(
                                                text = cleanOptionLabel,
                                                style = MaterialTheme.typography.labelMedium,
                                                fontFamily = GoogleSansRounded,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                color = labelColor,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                textAlign = TextAlign.Center
                                            )
                                        }

                                        if (isSelected) {
                                            Box(
                                                contentAlignment = Alignment.Center,
                                                modifier = Modifier
                                                    .align(Alignment.TopEnd)
                                                    .padding(8.dp)
                                                    .size(20.dp)
                                                    .clip(CircleShape)
                                                    .background(MaterialTheme.colorScheme.primary)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Rounded.Check,
                                                    contentDescription = "Selected",
                                                    tint = MaterialTheme.colorScheme.onPrimary,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                            repeat(3 - rowOptions.size) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemeSelectorItem(
        modifier: Modifier = Modifier,
        label: String,
        description: String,
        options: Map<String, String>,
        selectedKey: String,
        onSelectionChanged: (String) -> Unit,
        leadingIcon: @Composable () -> Unit,
        showBetaBadge: Boolean = false,
        enabled: Boolean = true,
        onDisabledClick: (() -> Unit)? = null
) {
    var showSheet by remember { mutableStateOf(false) }
    val rawSelectedOption = options[selectedKey] ?: selectedKey
    
    // Auto-parse (Beta) suffixes for the badges
    val selectedOptionClean = rawSelectedOption.removeSuffix(" (Beta)")
    val isSelectedOptionBeta = rawSelectedOption.endsWith(" (Beta)")
    
    val highlightTitle = LocalSettingsHighlightTitle.current
    val highlighted = highlightTitle != null && label == highlightTitle
    val shape = RoundedCornerShape(10.dp)
    val view = LocalView.current
    val appHapticsConfig = LocalAppHapticsConfig.current
    val themeTransitionController = LocalThemeTransitionController.current

    Box(
        modifier = modifier
            .fillMaxWidth()
            .autoBringIntoViewWhenHighlighted(highlighted)
    ) {
        Surface(
                color = MaterialTheme.colorScheme.surfaceContainer,
                modifier =
                        Modifier.fillMaxWidth().clip(shape).clickable {
                            if (enabled) {
                                showSheet = true
                            } else {
                                onDisabledClick?.invoke()
                            }
                        }.graphicsLayer { alpha = if (enabled) 1f else 0.48f }
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier.padding(end = 16.dp).size(24.dp),
                            contentAlignment = Alignment.Center
                    ) { leadingIcon() }

                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (showBetaBadge) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Surface(
                                    color = MaterialTheme.colorScheme.errorContainer,
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = "BETA",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onErrorContainer,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Selected Value Badge
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceContainerLowest,
                                shape = androidx.compose.foundation.shape.CircleShape
                            ) {
                                Text(
                                     text = selectedOptionClean,
                                     style = MaterialTheme.typography.labelMedium,
                                     color = MaterialTheme.colorScheme.primary,
                                     fontWeight = FontWeight.Bold,
                                     modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                            
                            if (isSelectedOptionBeta) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Surface(
                                    color = MaterialTheme.colorScheme.errorContainer,
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = "BETA",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onErrorContainer,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        if (highlighted) {
            HighlightPulseOverlay(shape = shape)
        }
    }

    if (showSheet) {
        val optionEntries = remember(options) { options.entries.toList() }
        com.saurav.pixelmusic.presentation.components.SmoothModalBottomSheet(
            onDismissRequest = { showSheet = false },
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            contentColor = MaterialTheme.colorScheme.onSurface,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
        ) {
            var pendingSelectedKey by remember(selectedKey) { mutableStateOf(selectedKey) }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(bottom = 20.dp)
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(horizontal = 22.dp, vertical = 14.dp),
                    fontWeight = FontWeight.Bold
                )

                LazyColumn(
                    modifier = Modifier
                        .padding(horizontal = 20.dp)
                        .heightIn(max = 440.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    itemsIndexed(optionEntries, key = { _, entry -> entry.key }) { idx, (key, optionLabel) ->
                        val isSelected = key == pendingSelectedKey

                        // Parse suffix for bottom sheet options
                        val cleanOptionLabel = optionLabel.removeSuffix(" (Beta)")
                        val isBetaOption = optionLabel.endsWith(" (Beta)")
                        var optionCenter by remember { mutableStateOf(Offset.Zero) }

                        ExpressivePreferenceOptionCard(
                            label = cleanOptionLabel,
                            isSelected = isSelected,
                            index = idx,
                            totalCount = optionEntries.size,
                            isBetaOption = isBetaOption,
                            onPositioned = { optionCenter = it },
                            onClick = {
                                performAppCompatHapticFeedback(
                                    view,
                                    appHapticsConfig,
                                    HapticFeedbackConstantsCompat.GESTURE_START
                                )
                                themeTransitionController?.lastTouchOffset = optionCenter
                                pendingSelectedKey = key
                                onSelectionChanged(key)
                            }
                        )
                    }
                }
            }
        }
    }
}

private val SmoothPreferenceMorphEasing = CubicBezierEasing(0.32f, 0.0f, 0.18f, 1.0f)

@Composable
private fun ExpressivePreferenceOptionCard(
    label: String,
    isSelected: Boolean,
    index: Int,
    totalCount: Int,
    isBetaOption: Boolean = false,
    onPositioned: (Offset) -> Unit = {},
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    var waveOrigin by remember { mutableStateOf<Offset?>(null) }

    LaunchedEffect(interactionSource) {
        interactionSource.interactions.collect { interaction ->
            if (interaction is PressInteraction.Press) {
                waveOrigin = interaction.pressPosition
            }
        }
    }

    val selectionProgress by animateFloatAsState(
        targetValue = if (isSelected) 1f else 0f,
        animationSpec = tween(durationMillis = 650, easing = SmoothPreferenceMorphEasing),
        label = "pref_opt_progress"
    )

    val baseTopCorner = if (totalCount <= 1 || index == 0) 24.dp else 6.dp
    val baseBottomCorner = if (totalCount <= 1 || index == totalCount - 1) 24.dp else 6.dp
    val selectedCorner = 28.dp

    val topCorner = lerp(baseTopCorner, selectedCorner, selectionProgress)
    val bottomCorner = lerp(baseBottomCorner, selectedCorner, selectionProgress)

    val unselectedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh
    val selectedContainerColor = MaterialTheme.colorScheme.primary
    val contentColor = lerp(
        MaterialTheme.colorScheme.onSurface,
        MaterialTheme.colorScheme.onPrimary,
        selectionProgress
    )
    val betaContainerColor = lerp(
        MaterialTheme.colorScheme.errorContainer,
        MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f),
        selectionProgress
    )
    val betaContentColor = lerp(
        MaterialTheme.colorScheme.onErrorContainer,
        MaterialTheme.colorScheme.onPrimary,
        selectionProgress
    )

    val animatedShape = RoundedCornerShape(
        topStart = topCorner,
        topEnd = topCorner,
        bottomStart = bottomCorner,
        bottomEnd = bottomCorner
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .onGloballyPositioned { coords ->
                val pos = coords.positionInRoot()
                onPositioned(pos + Offset(coords.size.width / 2f, coords.size.height / 2f))
            }
            .clip(animatedShape)
            .drawBehind {
                drawRect(color = unselectedContainerColor)
                val p = selectionProgress
                if (p >= 0.999f) {
                    drawRect(color = selectedContainerColor)
                } else if (p > 0.001f) {
                    val origin = waveOrigin ?: Offset(size.width * 0.5f, size.height * 0.5f)
                    val maxDx = max(origin.x, size.width - origin.x)
                    val maxDy = max(origin.y, size.height - origin.y)
                    val maxRadius = hypot(maxDx, maxDy).coerceAtLeast(1f)
                    val feather = maxRadius * 0.38f
                    val outerRadius = (p * (maxRadius + feather)).coerceAtLeast(1f)
                    val solidStop = ((outerRadius - feather) / outerRadius).coerceIn(0f, 1f)
                    drawRect(
                        brush = Brush.radialGradient(
                            colorStops = arrayOf(
                                0f to selectedContainerColor,
                                solidStop to selectedContainerColor,
                                1f to selectedContainerColor.copy(alpha = 0f)
                            ),
                            center = origin,
                            radius = outerRadius
                        )
                    )
                }
            }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = contentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false)
            )

            if (isBetaOption) {
                Spacer(modifier = Modifier.width(8.dp))
                Surface(
                    color = betaContainerColor,
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = "BETA",
                        style = MaterialTheme.typography.labelSmall,
                        color = betaContentColor,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}

internal fun expressiveSelectorCardShape(index: Int, totalCount: Int): Shape {
    val outer = 24.dp
    val inner = 6.dp
    return when {
        totalCount <= 1 -> RoundedCornerShape(outer)
        index == 0 -> RoundedCornerShape(
            topStart = outer,
            topEnd = outer,
            bottomStart = inner,
            bottomEnd = inner
        )
        index == totalCount - 1 -> RoundedCornerShape(
            topStart = inner,
            topEnd = inner,
            bottomStart = outer,
            bottomEnd = outer
        )
        else -> RoundedCornerShape(inner)
    }
}

@Composable
fun ExpressiveSettingsGroup(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(Color.Transparent),
    ) {
        content()
    }
}

@Composable
fun SliderSettingsItem(
        label: String,
        value: Float,
        valueRange: ClosedFloatingPointRange<Float>,
        steps: Int,
        onValueChange: (Float) -> Unit,
        onValueChangeFinished: (() -> Unit)? = null,
        valueText: (Float) -> String,
        enabled: Boolean = true,
        onDisabledClick: (() -> Unit)? = null
) {
    val highlightTitle = LocalSettingsHighlightTitle.current
    val highlighted = highlightTitle != null && (
        label.equals(highlightTitle, ignoreCase = true) ||
            (highlightTitle.equals("Music Storage Limit", ignoreCase = true) &&
                label.contains("storage", ignoreCase = true))
        )
    val shape = RoundedCornerShape(10.dp)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .autoBringIntoViewWhenHighlighted(highlighted)
    ) {
        Surface(
                color = MaterialTheme.colorScheme.surfaceContainer,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(shape)
                    .then(
                        if (!enabled && onDisabledClick != null) {
                            Modifier.clickable { onDisabledClick() }
                        } else {
                            Modifier
                        }
                    )
                    .graphicsLayer { alpha = if (enabled) 1f else 0.48f }
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                            text = label,
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                            text = valueText(value),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false
                    )
                }
                Slider(
                    value = value,
                    onValueChange = onValueChange,
                    onValueChangeFinished = onValueChangeFinished,
                    valueRange = valueRange,
                    steps = steps,
                    enabled = enabled
                )
            }
        }
        if (highlighted) {
            HighlightPulseOverlay(shape = shape)
        }
    }
}

@Composable
fun RefreshLibraryItem(
        isSyncing: Boolean,
        syncProgress: SyncProgress,
        activeOperationLabel: String? = null,
        onFullSync: () -> Unit,
        onRebuild: () -> Unit
) {
    val highlightTitle = LocalSettingsHighlightTitle.current
    val title = stringResource(R.string.presentation_batch_f_refresh_library_title)
    val highlighted = highlightTitle != null && (
        title.equals(highlightTitle, ignoreCase = true) ||
            highlightTitle.equals("Full Rescan", ignoreCase = true) ||
            highlightTitle.equals("Rebuild Database", ignoreCase = true)
        )
    val shape = RoundedCornerShape(10.dp)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .autoBringIntoViewWhenHighlighted(highlighted)
    ) {
    Surface(
            color = MaterialTheme.colorScheme.surfaceContainer,
            modifier = Modifier.fillMaxWidth().clip(shape)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                        modifier = Modifier.padding(end = 16.dp).size(24.dp),
                        contentAlignment = Alignment.Center
                ) {
                    Icon(
                            painter = painterResource(R.drawable.rounded_sync_24),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary
                    )
                }

                Column(
                        modifier = Modifier.weight(1f).padding(end = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                            text = stringResource(R.string.presentation_batch_f_refresh_library_title),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                            text = stringResource(R.string.presentation_batch_f_refresh_library_subtitle),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Full Rescan button
            FilledTonalButton(
                    onClick = onFullSync,
                    enabled = !isSyncing,
                    modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.presentation_batch_f_full_rescan))
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Rebuild Database button - full width, destructive action
            OutlinedButton(
                    onClick = onRebuild,
                    enabled = !isSyncing,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(R.drawable.rounded_delete_forever_24),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.presentation_batch_f_rebuild_database))
                }
            }

            if (isSyncing) {
                Spacer(modifier = Modifier.height(12.dp))
                val phaseLabel = activeOperationLabel ?: syncPhaseLabel(syncProgress.phase)
                if (syncProgress.hasProgress) {
                    LinearProgressIndicator(
                            progress = { syncProgress.progress },
                            modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                            text = stringResource(
                                R.string.presentation_batch_f_sync_progress_detailed,
                                phaseLabel,
                                (syncProgress.progress * 100).toInt(),
                                syncProgress.currentCount,
                                syncProgress.totalCount
                            ),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                            text = stringResource(
                                R.string.presentation_batch_f_sync_progress_indeterminate,
                                phaseLabel
                            ),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
    if (highlighted) {
        HighlightPulseOverlay(shape = shape)
    }
    }
}

@Composable
private fun syncPhaseLabel(phase: SyncProgress.SyncPhase): String =
        stringResource(
                when (phase) {
                    SyncProgress.SyncPhase.IDLE -> R.string.presentation_batch_f_sync_phase_preparing
                    SyncProgress.SyncPhase.FETCHING_MEDIASTORE ->
                            R.string.presentation_batch_f_sync_phase_reading_mediastore
                    SyncProgress.SyncPhase.PROCESSING_FILES ->
                            R.string.presentation_batch_f_sync_phase_processing_tracks
                    SyncProgress.SyncPhase.SAVING_TO_DATABASE ->
                            R.string.presentation_batch_f_sync_phase_saving_db
                    SyncProgress.SyncPhase.SCANNING_LRC -> R.string.presentation_batch_f_sync_phase_scanning_lrc
                    SyncProgress.SyncPhase.CLEANING_CACHE ->
                            R.string.presentation_batch_f_sync_phase_cleaning_cache
                    SyncProgress.SyncPhase.SYNCING_CLOUD ->
                            R.string.presentation_batch_f_sync_phase_syncing_cloud
                    SyncProgress.SyncPhase.COMPLETING -> R.string.presentation_batch_f_sync_phase_completing
                }
        )

@Composable
fun RefreshLyricsItem(
        isRefreshing: Boolean,
        progress: LyricsRefreshProgress,
        onRefresh: () -> Unit
) {
    Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                        modifier = Modifier.padding(end = 16.dp).size(24.dp),
                        contentAlignment = Alignment.Center
                ) {
                    Icon(
                            painter = painterResource(id = R.drawable.rounded_lyrics_24),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary
                    )
                }

                Column(
                        modifier = Modifier.weight(1f).padding(end = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                            text = stringResource(R.string.presentation_batch_f_refresh_lyrics_title),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                            text = stringResource(R.string.presentation_batch_f_refresh_lyrics_subtitle),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                FilledIconButton(
                        onClick = onRefresh,
                        enabled = !isRefreshing,
                        colors =
                                IconButtonDefaults.filledIconButtonColors(
                                        containerColor = MaterialTheme.colorScheme.primaryContainer
                                )
                ) {
                    Icon(
                            painter = painterResource(R.drawable.rounded_sync_24),
                            contentDescription = stringResource(R.string.presentation_batch_f_cd_refresh_lyrics),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            if (isRefreshing && progress.hasProgress) {
                Spacer(modifier = Modifier.height(12.dp))
                LinearProgressIndicator(
                        progress = { progress.progress },
                        modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                        text = stringResource(
                            R.string.presentation_batch_f_refresh_lyrics_processing,
                            progress.currentCount,
                            progress.totalSongs
                        ),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun ActionSettingsItem(
    title: String,
    subtitle: String,
    icon: @Composable () -> Unit,
    primaryActionLabel: String,
    onPrimaryAction: () -> Unit,
    secondaryActionLabel: String? = null,
    onSecondaryAction: (() -> Unit)? = null,
    enabled: Boolean = true
) {
    val highlightTitle = LocalSettingsHighlightTitle.current
    val highlighted = highlightTitle != null && title == highlightTitle
    val shape = RoundedCornerShape(10.dp)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .autoBringIntoViewWhenHighlighted(highlighted)
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainer,
            modifier = Modifier.fillMaxWidth().clip(shape)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier.padding(end = 16.dp).size(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        icon()
                    }

                    Column(
                        modifier = Modifier.weight(1f).padding(end = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Primary Action
                FilledTonalButton(
                    onClick = onPrimaryAction,
                    enabled = enabled,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(primaryActionLabel, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }

                // Secondary Action (Optional)
                if (secondaryActionLabel != null && onSecondaryAction != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = onSecondaryAction,
                        enabled = enabled,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(secondaryActionLabel, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
        if (highlighted) {
            HighlightPulseOverlay(shape = shape)
        }
    }
}

@Composable
fun AiApiKeyItem(
    apiKey: String,
    onApiKeySave: (String) -> Unit,
    title: String,
    subtitle: String
) {
    var localApiKey by remember(apiKey) { mutableStateOf(apiKey) }
    val hasChanges = localApiKey != apiKey
    var showSaved by remember { mutableStateOf(false) }
    val highlightTitle = LocalSettingsHighlightTitle.current
    val highlighted = highlightTitle != null && (
        title.equals(highlightTitle, ignoreCase = true) ||
            highlightTitle.equals("AI API Key", ignoreCase = true)
        )
    val shape = RoundedCornerShape(10.dp)

    LaunchedEffect(showSaved) {
        if (showSaved) {
            kotlinx.coroutines.delay(2000)
            showSaved = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .autoBringIntoViewWhenHighlighted(highlighted)
    ) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier.fillMaxWidth().clip(shape)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = localApiKey,
                onValueChange = { localApiKey = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(stringResource(R.string.presentation_batch_f_enter_api_key)) },
                singleLine = true,
                visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation()
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilledTonalButton(
                    onClick = {
                        onApiKeySave(localApiKey)
                        showSaved = true
                    },
                    enabled = hasChanges
                ) {
                    Text(stringResource(R.string.presentation_batch_f_save), maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                if (showSaved) {
                    Text(
                        text = stringResource(R.string.presentation_batch_f_saved),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
    if (highlighted) {
        HighlightPulseOverlay(shape = shape)
    }
    }
}

@Composable
fun AiSystemPromptItem(
    systemPrompt: String,
    defaultPrompt: String,
    onSystemPromptSave: (String) -> Unit,
    onReset: () -> Unit,
    title: String,
    subtitle: String
) {
    var localPrompt by remember(systemPrompt) { mutableStateOf(systemPrompt) }
    val hasChanges = localPrompt != systemPrompt
    val isDefault = systemPrompt == defaultPrompt
    var showSaved by remember { mutableStateOf(false) }
    val highlightTitle = LocalSettingsHighlightTitle.current
    val highlighted = highlightTitle != null && (
        title.equals(highlightTitle, ignoreCase = true) ||
            highlightTitle.equals("AI System Prompt", ignoreCase = true)
        )
    val shape = RoundedCornerShape(10.dp)
    val presets = listOf(
        stringResource(R.string.presentation_batch_f_ai_preset_professional_curator_name) to
            stringResource(R.string.presentation_batch_f_ai_preset_professional_curator_prompt),
        stringResource(R.string.presentation_batch_f_ai_preset_creative_maverick_name) to
            stringResource(R.string.presentation_batch_f_ai_preset_creative_maverick_prompt),
        stringResource(R.string.presentation_batch_f_ai_preset_strict_librarian_name) to
            stringResource(R.string.presentation_batch_f_ai_preset_strict_librarian_prompt),
        stringResource(R.string.presentation_batch_f_ai_preset_atmospheric_guide_name) to
            stringResource(R.string.presentation_batch_f_ai_preset_atmospheric_guide_prompt),
        stringResource(R.string.presentation_batch_f_ai_preset_sonic_enthusiast_name) to
            stringResource(R.string.presentation_batch_f_ai_preset_sonic_enthusiast_prompt),
        stringResource(R.string.presentation_batch_f_ai_preset_energy_catalyst_name) to
            stringResource(R.string.presentation_batch_f_ai_preset_energy_catalyst_prompt)
    )

    LaunchedEffect(showSaved) {
        if (showSaved) {
            kotlinx.coroutines.delay(2000)
            showSaved = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .autoBringIntoViewWhenHighlighted(highlighted)
    ) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier.fillMaxWidth().clip(shape)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.presentation_batch_f_preset_prompts),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                presets.forEach { preset ->
                    OutlinedButton(
                        onClick = {
                            localPrompt = preset.second
                        },
                        modifier = Modifier.wrapContentWidth()
                    ) {
                        Text(text = preset.first, maxLines = 1)
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = localPrompt,
                onValueChange = { localPrompt = it },
                modifier = Modifier.fillMaxWidth().heightIn(min = 100.dp, max = 200.dp),
                placeholder = { Text(stringResource(R.string.presentation_batch_f_enter_system_prompt_placeholder)) },
                minLines = 3,
                maxLines = 6
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilledTonalButton(
                    onClick = {
                        onSystemPromptSave(localPrompt)
                        showSaved = true
                    },
                    enabled = hasChanges
                ) {
                    Text(stringResource(R.string.presentation_batch_f_save), maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                if (!isDefault) {
                    OutlinedButton(onClick = {
                        onReset()
                    }) {
                        Text(stringResource(R.string.presentation_batch_f_reset), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
                if (showSaved) {
                    Text(
                        text = stringResource(R.string.presentation_batch_f_saved),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
    if (highlighted) {
        HighlightPulseOverlay(shape = shape)
    }
    }
}
