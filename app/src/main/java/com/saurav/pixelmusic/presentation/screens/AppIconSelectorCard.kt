package com.saurav.pixelmusic.presentation.screens

import com.saurav.pixelmusic.presentation.components.PixelToast as Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import com.saurav.pixelmusic.R
import com.saurav.pixelmusic.presentation.components.AiSmoothMorphEasing
import com.saurav.pixelmusic.presentation.components.LocalSmoothDismiss
import com.saurav.pixelmusic.presentation.components.SmoothModalBottomSheet
import com.saurav.pixelmusic.presentation.components.expressiveListItemScrollAnimation
import com.saurav.pixelmusic.ui.theme.GoogleSansRounded
import com.saurav.pixelmusic.utils.AppIconManager
import com.saurav.pixelmusic.utils.AppIconOption
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppIconSelectorCard(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val highlightTitle = LocalSettingsHighlightTitle.current
    val title = "App Icon"
    val highlighted = highlightTitle != null && title == highlightTitle
    val shape = RoundedCornerShape(10.dp)

    var selectedIcon by remember { mutableStateOf(AppIconManager.getCurrentIcon(context)) }
    var showSheet by remember { mutableStateOf(false) }

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
                .clickable { showSheet = true }
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .padding(end = 16.dp)
                        .size(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(R.drawable.rounded_grid_view_24),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = selectedIcon.label,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                AppIconThumbnail(
                    option = selectedIcon,
                    size = 40.dp,
                    cornerRadius = 12.dp
                )
            }
        }
        if (highlighted) {
            HighlightPulseOverlay(shape = shape)
        }
    }

    if (showSheet) {
        SmoothModalBottomSheet(
            onDismissRequest = { showSheet = false },
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            contentColor = MaterialTheme.colorScheme.onSurface,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
        ) {
            val smoothDismiss = LocalSmoothDismiss.current
            val gridListState = rememberLazyListState()
            val rows = remember { AppIconOption.entries.chunked(3) }

            val headerEntrance = remember { Animatable(0f) }
            var initialOpenComplete by remember { mutableStateOf(false) }

            LaunchedEffect(Unit) {
                headerEntrance.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = 320, easing = AiSmoothMorphEasing)
                )
                delay(120)
                initialOpenComplete = true
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer {
                            val p = headerEntrance.value
                            alpha = p.coerceIn(0f, 1f)
                            translationY = (1f - p) * 20f
                        }
                ) {
                    Text(
                        text = "App Icon",
                        style = MaterialTheme.typography.titleLarge,
                        fontFamily = GoogleSansRounded,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Pick your home screen icon. When system themed icons are enabled, your monochrome icon is used automatically.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                LazyColumn(
                    state = gridListState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 460.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(top = 4.dp, bottom = 12.dp)
                ) {
                    itemsIndexed(
                        items = rows,
                        key = { index, _ -> index }
                    ) { rowIndex, rowOptions ->
                        val rowEntrance = remember {
                            Animatable(if (initialOpenComplete) 1f else 0f)
                        }
                        LaunchedEffect(Unit) {
                            if (rowEntrance.value < 1f) {
                                delay(40L + (rowIndex.coerceAtMost(4) * 42L))
                                rowEntrance.animateTo(
                                    targetValue = 1f,
                                    animationSpec = spring(
                                        dampingRatio = 0.82f,
                                        stiffness = Spring.StiffnessMediumLow
                                    )
                                )
                            }
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .animateItem()
                                .expressiveListItemScrollAnimation(gridListState)
                                .graphicsLayer {
                                    val p = rowEntrance.value
                                    alpha = p.coerceIn(0f, 1f)
                                    translationY = (1f - p) * 30f
                                    val s = 0.94f + 0.06f * p
                                    scaleX = s
                                    scaleY = s
                                },
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            rowOptions.forEach { option ->
                                AppIconGridTile(
                                    option = option,
                                    isSelected = option == selectedIcon,
                                    modifier = Modifier.weight(1f),
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        if (option != selectedIcon) {
                                            selectedIcon = option
                                            smoothDismiss {
                                                AppIconManager.applyIcon(context, option)
                                                Toast.makeText(
                                                    context,
                                                    "App icon changed to ${option.label}",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                        } else {
                                            smoothDismiss(null)
                                        }
                                    }
                                )
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

@Composable
private fun AppIconGridTile(
    option: AppIconOption,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 0.93f else 1f,
        animationSpec = spring(
            dampingRatio = 0.62f,
            stiffness = Spring.StiffnessMedium
        ),
        label = "app_icon_press_scale"
    )
    val cardCorner by animateDpAsState(
        targetValue = if (isSelected) 26.dp else 20.dp,
        animationSpec = tween(durationMillis = 240, easing = AiSmoothMorphEasing),
        label = "app_icon_card_corner"
    )
    val cardColor by animateColorAsState(
        targetValue = if (isSelected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainerHigh
        },
        animationSpec = tween(durationMillis = 220, easing = AiSmoothMorphEasing),
        label = "app_icon_card_bg"
    )
    val labelColor by animateColorAsState(
        targetValue = if (isSelected) {
            MaterialTheme.colorScheme.onPrimaryContainer
        } else {
            MaterialTheme.colorScheme.onSurface
        },
        animationSpec = tween(durationMillis = 220, easing = AiSmoothMorphEasing),
        label = "app_icon_label_color"
    )

    Surface(
        onClick = onClick,
        interactionSource = interactionSource,
        shape = RoundedCornerShape(cardCorner),
        color = cardColor,
        modifier = modifier.graphicsLayer {
            scaleX = pressScale
            scaleY = pressScale
        }
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 14.dp, horizontal = 8.dp)
            ) {
                AppIconThumbnail(
                    option = option,
                    size = 60.dp,
                    cornerRadius = 18.dp
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = option.label,
                    style = MaterialTheme.typography.labelMedium,
                    fontFamily = GoogleSansRounded,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = labelColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
            }

            AnimatedVisibility(
                visible = isSelected,
                enter = scaleIn(
                    initialScale = 0.6f,
                    animationSpec = spring(
                        dampingRatio = 0.65f,
                        stiffness = Spring.StiffnessMedium
                    )
                ) + fadeIn(tween(180)),
                exit = scaleOut(targetScale = 0.6f, animationSpec = tween(150)) + fadeOut(tween(150)),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
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

@Composable
private fun AppIconThumbnail(
    option: AppIconOption,
    size: Dp,
    cornerRadius: Dp
) {
    val context = LocalContext.current
    val shape = RoundedCornerShape(cornerRadius)

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(size)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
    ) {
        val previewRes = option.previewRes
        if (previewRes != null) {
            Image(
                painter = painterResource(previewRes),
                contentDescription = option.label,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        if (option.isAdaptiveZoomPreview) {
                            scaleX = 1.46f
                            scaleY = 1.46f
                        }
                    }
            )
        } else {
            val defaultBitmap = remember(context) {
                ContextCompat.getDrawable(context, R.mipmap.ic_launcher)
                    ?.toBitmap(width = 192, height = 192)
                    ?.asImageBitmap()
            }
            if (defaultBitmap != null) {
                Image(
                    bitmap = defaultBitmap,
                    contentDescription = option.label,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}
