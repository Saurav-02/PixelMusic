package com.saurav.pixelmusic.presentation.screens

import androidx.annotation.OptIn
import androidx.compose.ui.platform.LocalContext

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDp
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.rememberTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.ui.res.painterResource
import com.saurav.pixelmusic.utils.AndroidVersionCompat
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavController
import com.saurav.pixelmusic.presentation.components.CollapsibleCommonTopBar
import com.saurav.pixelmusic.presentation.components.MiniPlayerHeight
import com.saurav.pixelmusic.presentation.viewmodel.PlayerViewModel
import com.saurav.pixelmusic.presentation.viewmodel.SettingsViewModel
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import androidx.compose.ui.res.stringResource
import com.saurav.pixelmusic.R
import com.saurav.pixelmusic.ui.modifiers.scrollMotionBlur

import com.saurav.pixelmusic.presentation.model.SettingsCategory
import com.saurav.pixelmusic.presentation.navigation.Screen
import com.saurav.pixelmusic.presentation.navigation.navigateSafely



@OptIn(UnstableApi::class)
@Composable
fun ExperimentalSettingsScreen(
    navController: NavController,
    playerViewModel: PlayerViewModel,
    onNavigationIconClick: () -> Unit,
    settingsViewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by settingsViewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val hasSystemEqualizer = remember(context) {
        AndroidVersionCompat.supportsSystemEqualizer(context)
    }

    val transitionState = remember { MutableTransitionState(false) }
    LaunchedEffect(true) { transitionState.targetState = true }

    val transition = rememberTransition(transitionState, label = "ExperimentalSettingsAppearTransition")
    val contentAlpha by transition.animateFloat(
        label = "ContentAlpha",
        transitionSpec = { tween(durationMillis = 500) }
    ) { if (it) 1f else 0f }

    val contentOffset by transition.animateDp(
        label = "ContentOffset",
        transitionSpec = { tween(durationMillis = 400, easing = FastOutSlowInEasing) }
    ) { if (it) 0.dp else 40.dp }

    val density = LocalDensity.current
    val coroutineScope = rememberCoroutineScope()
    val lazyListState = rememberLazyListState()

    val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val minTopBarHeight = 64.dp + statusBarHeight
    val maxTopBarHeight = 180.dp

    val minTopBarHeightPx = with(density) { minTopBarHeight.toPx() }
    val maxTopBarHeightPx = with(density) { maxTopBarHeight.toPx() }

    val topBarHeight = remember { Animatable(maxTopBarHeightPx) }
    var collapseFraction by remember { mutableStateOf(0f) }

    LaunchedEffect(topBarHeight.value) {
        collapseFraction = 1f - ((topBarHeight.value - minTopBarHeightPx) / (maxTopBarHeightPx - minTopBarHeightPx)).coerceIn(0f, 1f)
    }

    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                val delta = available.y
                val isScrollingDown = delta < 0

                if (!isScrollingDown && (lazyListState.firstVisibleItemIndex > 0 || lazyListState.firstVisibleItemScrollOffset > 0)) {
                    return Offset.Zero
                }

                val previousHeight = topBarHeight.value
                val newHeight = (previousHeight + delta).coerceIn(minTopBarHeightPx, maxTopBarHeightPx)
                val consumed = newHeight - previousHeight

                if (consumed.roundToInt() != 0) {
                    coroutineScope.launch {
                        topBarHeight.snapTo(newHeight)
                    }
                }

                val canConsumeScroll = !(isScrollingDown && newHeight == minTopBarHeightPx)
                return if (canConsumeScroll) Offset(0f, consumed) else Offset.Zero
            }
        }
    }

    LaunchedEffect(lazyListState.isScrollInProgress) {
        if (!lazyListState.isScrollInProgress) {
            val shouldExpand = topBarHeight.value > (minTopBarHeightPx + maxTopBarHeightPx) / 2
            val canExpand = lazyListState.firstVisibleItemIndex == 0 && lazyListState.firstVisibleItemScrollOffset == 0

            val targetValue = if (shouldExpand && canExpand) maxTopBarHeightPx else minTopBarHeightPx

            if (topBarHeight.value != targetValue) {
                coroutineScope.launch {
                    topBarHeight.animateTo(targetValue, spring(stiffness = Spring.StiffnessMedium))
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .nestedScroll(nestedScrollConnection)
            .fillMaxSize()
    ) {
        val currentTopBarHeightDp = with(density) { topBarHeight.value.toDp() }

        LazyColumn(
            state = lazyListState,
            contentPadding = PaddingValues(top = currentTopBarHeightDp + 8.dp),
            modifier = Modifier
                .fillMaxSize()
                .scrollMotionBlur(
                    lazyListState = lazyListState,
                    enabled = uiState.isUiMotionBlurEnabled
                )
        ) {
            if (com.saurav.pixelmusic.utils.AndroidVersionCompat.supportsMusicLiveWallpaper) item(key = "music_live_wallpaper_nav_card") {
                SettingsSection(
                    title = stringResource(R.string.music_live_wallpaper_title),
                    icon = {
                        Icon(
                            painter = painterResource(R.drawable.rounded_wallpaper_24),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                ) {
                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        color = Color.Transparent
                    ) {
                        SettingsItem(
                            title = stringResource(R.string.music_live_wallpaper_title),
                            subtitle = stringResource(R.string.music_live_wallpaper_subtitle),
                            leadingIcon = {
                                Icon(
                                    painter = painterResource(R.drawable.rounded_wallpaper_24),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.secondary
                                )
                            },
                            trailingIcon = {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                                    contentDescription = stringResource(R.string.cd_open),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            onClick = {
                                navController.navigateSafely(Screen.MusicLiveWallpaper.route)
                            }
                        )
                    }
                }
            }

            item(key = "player_ui_tweaks_section") {
                SettingsSection(
                    title = stringResource(R.string.presentation_batch_f_exp_player_ui_tweaks_section),
                    icon = {
                        Icon(
                            painter = painterResource(R.drawable.rounded_style_24),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                ) {
                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        color = Color.Transparent,
                        modifier = Modifier.padding(horizontal = 0.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            val loadingTweaks = uiState.fullPlayerLoadingTweaks
                            val delayAllEnabled = loadingTweaks.delayAll
                            val appearThresholdPercent = loadingTweaks.contentAppearThresholdPercent
                            val closeThresholdPercent = loadingTweaks.contentCloseThresholdPercent
                            val switchOnDragRelease = loadingTweaks.switchOnDragRelease
                            val placeholdersEnabled = loadingTweaks.showPlaceholders
                            val isAnyDelayEnabled = loadingTweaks.let {
                                it.delayAll || it.delayAlbumCarousel || it.delaySongMetadata || it.delayProgressBar || it.delayControls
                            }
                            val canUseTriggerMode = isAnyDelayEnabled && placeholdersEnabled

                            Surface(
                                color = MaterialTheme.colorScheme.surfaceContainer,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                            ) {
                                Text(
                                    text = stringResource(R.string.presentation_batch_f_exp_step1_delay_header),
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                                )
                            }

                            SwitchSettingItem(
                                title = stringResource(R.string.presentation_batch_f_exp_delay_everything_title),
                                subtitle = stringResource(R.string.presentation_batch_f_exp_delay_everything_subtitle),
                                checked = delayAllEnabled,
                                onCheckedChange = settingsViewModel::setDelayAllFullPlayerContent,
                                leadingIcon = {
                                    Icon(
                                        painter = painterResource(R.drawable.rounded_visibility_24),
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.secondary
                                    )
                                }
                            )

                            AnimatedVisibility(
                                visible = !delayAllEnabled,
                                enter = fadeIn() + expandVertically(),
                                exit = fadeOut() + shrinkVertically()
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    SwitchSettingItem(
                                        title = stringResource(R.string.presentation_batch_f_exp_album_carousel_title),
                                        subtitle = stringResource(R.string.presentation_batch_f_exp_album_carousel_subtitle),
                                        checked = loadingTweaks.delayAlbumCarousel,
                                        onCheckedChange = settingsViewModel::setDelayAlbumCarousel,
                                        leadingIcon = {
                                            Icon(
                                                painter = painterResource(R.drawable.rounded_view_carousel_24),
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.secondary
                                            )
                                        }
                                    )

                                    SwitchSettingItem(
                                        title = stringResource(R.string.presentation_batch_f_exp_song_metadata_title),
                                        subtitle = stringResource(R.string.presentation_batch_f_exp_song_metadata_subtitle),
                                        checked = loadingTweaks.delaySongMetadata,
                                        onCheckedChange = settingsViewModel::setDelaySongMetadata,
                                        leadingIcon = {
                                            Icon(
                                                painter = painterResource(R.drawable.rounded_linear_scale_24),
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.secondary
                                            )
                                        }
                                    )

                                    SwitchSettingItem(
                                        title = stringResource(R.string.presentation_batch_f_exp_progress_bar_title),
                                        subtitle = stringResource(R.string.presentation_batch_f_exp_progress_bar_subtitle),
                                        checked = loadingTweaks.delayProgressBar,
                                        onCheckedChange = settingsViewModel::setDelayProgressBar,
                                        leadingIcon = {
                                            Icon(
                                                painter = painterResource(R.drawable.rounded_linear_scale_24),
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.secondary
                                            )
                                        }
                                    )

                                    SwitchSettingItem(
                                        title = stringResource(R.string.presentation_batch_f_exp_playback_controls_title),
                                        subtitle = stringResource(R.string.presentation_batch_f_exp_playback_controls_subtitle),
                                        checked = loadingTweaks.delayControls,
                                        onCheckedChange = settingsViewModel::setDelayControls,
                                        leadingIcon = {
                                            Icon(
                                                painter = painterResource(R.drawable.rounded_play_circle_24),
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.secondary
                                            )
                                        }
                                    )
                                }
                            }

                            AnimatedVisibility(
                                visible = delayAllEnabled,
                                enter = fadeIn() + expandVertically(),
                                exit = fadeOut() + shrinkVertically()
                            ) {
                                Surface(
                                    color = MaterialTheme.colorScheme.surfaceContainer,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                ) {
                                    Text(
                                        text = stringResource(R.string.presentation_batch_f_exp_delay_all_active_hint),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                                    )
                                }
                            }

                            Surface(
                                color = MaterialTheme.colorScheme.surfaceContainer,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                            ) {
                                Text(
                                    text = stringResource(R.string.presentation_batch_f_exp_step2_placeholders_header),
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                                )
                            }

                            SwitchSettingItem(
                                title = stringResource(R.string.presentation_batch_f_exp_use_placeholders_title),
                                subtitle = stringResource(R.string.presentation_batch_f_exp_use_placeholders_subtitle),
                                checked = placeholdersEnabled,
                                onCheckedChange = settingsViewModel::setFullPlayerPlaceholders,
                                leadingIcon = {
                                    Icon(
                                        painter = painterResource(R.drawable.rounded_rectangle_24),
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.secondary
                                    )
                                }
                            )

                            AnimatedVisibility(
                                visible = placeholdersEnabled,
                                enter = fadeIn() + expandVertically(),
                                exit = fadeOut() + shrinkVertically()
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Surface(
                                        color = MaterialTheme.colorScheme.surfaceContainer,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(10.dp))
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(16.dp),
                                            verticalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            Text(
                                                text = stringResource(R.string.presentation_batch_f_exp_step3_trigger_header),
                                                style = MaterialTheme.typography.titleMedium,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = if (canUseTriggerMode) {
                                                    stringResource(R.string.presentation_batch_f_exp_trigger_mode_unlocked)
                                                } else {
                                                    stringResource(R.string.presentation_batch_f_exp_trigger_mode_locked)
                                                },
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )

                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                TriggerModeOptionCard(
                                                    title = stringResource(R.string.presentation_batch_f_exp_threshold_title),
                                                    subtitle = stringResource(R.string.presentation_batch_f_exp_threshold_subtitle),
                                                    selected = !switchOnDragRelease,
                                                    enabled = canUseTriggerMode,
                                                    onClick = { settingsViewModel.setFullPlayerSwitchOnDragRelease(false) },
                                                    modifier = Modifier.weight(1f)
                                                )
                                                TriggerModeOptionCard(
                                                    title = stringResource(R.string.presentation_batch_f_exp_drag_release_title),
                                                    subtitle = stringResource(R.string.presentation_batch_f_exp_drag_release_subtitle),
                                                    selected = switchOnDragRelease,
                                                    enabled = canUseTriggerMode,
                                                    onClick = { settingsViewModel.setFullPlayerSwitchOnDragRelease(true) },
                                                    modifier = Modifier.weight(1f)
                                                )
                                            }
                                        }
                                    }

                                    AnimatedVisibility(
                                        visible = canUseTriggerMode && !switchOnDragRelease,
                                        enter = fadeIn() + expandVertically(),
                                        exit = fadeOut() + shrinkVertically()
                                    ) {
                                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Surface(
                                                color = MaterialTheme.colorScheme.surfaceContainer,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clip(RoundedCornerShape(10.dp))
                                            ) {
                                                Column(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(16.dp),
                                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                                ) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                                    ) {
                                                        Icon(
                                                            painter = painterResource(R.drawable.rounded_linear_scale_24),
                                                            contentDescription = null,
                                                            tint = MaterialTheme.colorScheme.secondary
                                                        )

                                                        Column(modifier = Modifier.weight(1f)) {
                                                            Text(
                                                                text = stringResource(R.string.presentation_batch_f_exp_expand_threshold_title),
                                                                style = MaterialTheme.typography.titleMedium,
                                                                color = MaterialTheme.colorScheme.onSurface
                                                            )
                                                            Text(
                                                                text = stringResource(R.string.presentation_batch_f_exp_expand_threshold_subtitle),
                                                                style = MaterialTheme.typography.bodyMedium,
                                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                                            )
                                                        }
                                                    }

                                                    Slider(
                                                        value = appearThresholdPercent.toFloat(),
                                                        onValueChange = { settingsViewModel.setFullPlayerAppearThreshold(it.roundToInt()) },
                                                        valueRange = 0f..100f,
                                                        steps = 99,
                                                        enabled = isAnyDelayEnabled
                                                    )

                                                    Text(
                                                        text = stringResource(
                                                            R.string.presentation_batch_f_exp_content_appears_at,
                                                            appearThresholdPercent
                                                        ),
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }

                                            SwitchSettingItem(
                                                title = stringResource(R.string.presentation_batch_f_exp_apply_on_close_title),
                                                subtitle = stringResource(R.string.presentation_batch_f_exp_apply_on_close_subtitle),
                                                checked = loadingTweaks.applyPlaceholdersOnClose,
                                                onCheckedChange = settingsViewModel::setFullPlayerPlaceholdersOnClose,
                                                enabled = isAnyDelayEnabled,
                                                leadingIcon = {
                                                    Icon(
                                                        painter = painterResource(R.drawable.rounded_rectangle_24),
                                                        contentDescription = null,
                                                        tint = MaterialTheme.colorScheme.secondary
                                                    )
                                                }
                                            )

                                            AnimatedVisibility(
                                                visible = loadingTweaks.applyPlaceholdersOnClose,
                                                enter = fadeIn() + expandVertically(),
                                                exit = fadeOut() + shrinkVertically()
                                            ) {
                                                Surface(
                                                    color = MaterialTheme.colorScheme.surfaceContainer,
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .clip(RoundedCornerShape(10.dp))
                                                ) {
                                                    Column(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .padding(16.dp),
                                                        verticalArrangement = Arrangement.spacedBy(12.dp)
                                                    ) {
                                                        Row(
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                                                        ) {
                                                            Icon(
                                                                painter = painterResource(R.drawable.rounded_linear_scale_24),
                                                                contentDescription = null,
                                                                tint = MaterialTheme.colorScheme.secondary
                                                            )

                                                            Column(modifier = Modifier.weight(1f)) {
                                                                Text(
                                                                    text = stringResource(R.string.presentation_batch_f_exp_close_threshold_title),
                                                                    style = MaterialTheme.typography.titleMedium,
                                                                    color = MaterialTheme.colorScheme.onSurface
                                                                )
                                                                Text(
                                                                    text = stringResource(R.string.presentation_batch_f_exp_close_threshold_subtitle),
                                                                    style = MaterialTheme.typography.bodyMedium,
                                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                                )
                                                            }
                                                        }

                                                        Slider(
                                                            value = closeThresholdPercent.toFloat(),
                                                            onValueChange = { settingsViewModel.setFullPlayerCloseThreshold(it.roundToInt()) },
                                                            valueRange = 0f..100f,
                                                            steps = 99,
                                                            enabled = isAnyDelayEnabled
                                                        )

                                                        Text(
                                                            text = stringResource(
                                                                R.string.presentation_batch_f_exp_placeholders_after_collapse,
                                                                closeThresholdPercent
                                                            ),
                                                            style = MaterialTheme.typography.bodyMedium,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    AnimatedVisibility(
                                        visible = canUseTriggerMode && switchOnDragRelease,
                                        enter = fadeIn() + expandVertically(),
                                        exit = fadeOut() + shrinkVertically()
                                    ) {
                                        Surface(
                                            color = MaterialTheme.colorScheme.surfaceContainer,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(10.dp))
                                        ) {
                                            Text(
                                                text = stringResource(R.string.presentation_batch_f_exp_drag_release_bypass),
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                                            )
                                        }
                                    }

                                    SwitchSettingItem(
                                        title = stringResource(R.string.presentation_batch_f_exp_transparent_placeholders_title),
                                        subtitle = stringResource(R.string.presentation_batch_f_exp_transparent_placeholders_subtitle),
                                        checked = loadingTweaks.transparentPlaceholders,
                                        onCheckedChange = settingsViewModel::setTransparentPlaceholders,
                                        leadingIcon = {
                                            Icon(
                                                painter = painterResource(R.drawable.rounded_visibility_24),
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.secondary
                                            )
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
            




            item(key = "experimental_bottom_spacer") {
                Spacer(modifier = Modifier.height(MiniPlayerHeight + 36.dp))
            }
        }

        CollapsibleCommonTopBar(
            title = stringResource(R.string.presentation_batch_f_exp_screen_title),
            collapseFraction = collapseFraction,
            headerHeight = currentTopBarHeightDp,
            onBackClick = onNavigationIconClick
        )
    }
}

@Composable
private fun TriggerModeOptionCard(
    title: String,
    subtitle: String,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val containerColor = when {
        !enabled -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        selected -> MaterialTheme.colorScheme.primaryContainer
        else -> MaterialTheme.colorScheme.surfaceContainerHighest
    }
    val titleColor = when {
        !enabled -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
        selected -> MaterialTheme.colorScheme.onPrimaryContainer
        else -> MaterialTheme.colorScheme.onSurface
    }
    val subtitleColor = when {
        !enabled -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
        selected -> MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Surface(
        color = containerColor,
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
            .defaultMinSize(minHeight = 94.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(enabled = enabled, onClick = onClick)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = titleColor
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = subtitleColor
            )
        }
    }
}
