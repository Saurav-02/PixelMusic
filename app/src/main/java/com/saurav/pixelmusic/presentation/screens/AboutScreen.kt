package com.saurav.pixelmusic.presentation.screens

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.foundation.BorderStroke

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.annotation.DrawableRes
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDp
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.rememberTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.LocalIndication
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

import kotlinx.coroutines.flow.collectLatest
import com.saurav.pixelmusic.ui.modifiers.scrollMotionBlur
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.lerp as lerpDp
import androidx.compose.ui.util.lerp as lerpFloat
import androidx.core.graphics.drawable.toBitmap
import androidx.core.net.toUri
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavController
import coil.compose.AsyncImagePainter
import coil.request.ImageRequest
import coil.size.Size
import com.saurav.pixelmusic.R
import com.saurav.pixelmusic.presentation.components.CollapsibleCommonTopBar
import com.saurav.pixelmusic.presentation.components.MiniPlayerHeight
import com.saurav.pixelmusic.presentation.navigation.Screen
import com.saurav.pixelmusic.presentation.navigation.navigateSafely
import com.saurav.pixelmusic.presentation.viewmodel.PlayerViewModel
import kotlinx.coroutines.launch
import racra.compose.smooth_corner_rect_library.AbsoluteSmoothCornerShape
import kotlin.math.roundToInt
import com.saurav.pixelmusic.utils.InAppUpdater
import com.saurav.pixelmusic.utils.UpdateState
import com.saurav.pixelmusic.presentation.components.UpdateNotificationSheet
import com.saurav.pixelmusic.ui.theme.LocalIsBlackAndWhiteTheme
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.ColorUtils



// AboutTopBar removed, replaced by CollapsibleCommonTopBar

@androidx.annotation.OptIn(UnstableApi::class)
@Suppress("UNUSED_PARAMETER")
@Composable
fun AboutScreen(
    navController: NavController,
    viewModel: PlayerViewModel,
    onNavigationIconClick: () -> Unit,
) {
    val context = LocalContext.current
    val packageInfo = try {
        context.packageManager.getPackageInfo(context.packageName, 0)
    } catch (_: Exception) { null }
    
    val versionName: String = packageInfo?.versionName ?: "N/A"
    val versionCode: Long = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
        packageInfo?.longVersionCode ?: 0L
    } else {
        @Suppress("DEPRECATION")
        packageInfo?.versionCode?.toLong() ?: 0L
    }

    val transitionState = remember { MutableTransitionState(false) }
    LaunchedEffect(Unit) {
        transitionState.targetState = true
    }
    val transition = rememberTransition(transitionState, label = "AboutAppearTransition")

    val contentAlpha by transition.animateFloat(
        label = "ContentAlpha",
        transitionSpec = { tween(durationMillis = 500) },
    ) { if (it) 1f else 0f }

    val contentOffset by transition.animateDp(
        label = "ContentOffset",
        transitionSpec = { tween(durationMillis = 400, easing = FastOutSlowInEasing) },
    ) { if (it) 0.dp else 40.dp }

    val density = LocalDensity.current
    val coroutineScope = rememberCoroutineScope()
    val lazyListState = rememberLazyListState()

    val statusBarHeight = WindowInsets.statusBars
        .asPaddingValues()
        .calculateTopPadding()
    val minTopBarHeight = 64.dp + statusBarHeight
    val maxTopBarHeight = 170.dp

    val minTopBarHeightPx = with(density) { minTopBarHeight.toPx() }
    val maxTopBarHeightPx = with(density) { maxTopBarHeight.toPx() }

    val topBarHeight = remember { Animatable(maxTopBarHeightPx) }
    var collapseFraction by remember { mutableStateOf(0f) }

    LaunchedEffect(topBarHeight.value) {
        collapseFraction = 1f - (
            (topBarHeight.value - minTopBarHeightPx) / (maxTopBarHeightPx - minTopBarHeightPx)
            ).coerceIn(0f, 1f)
    }

    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                val delta = available.y
                val isScrollingDown = delta < 0

                if (
                    !isScrollingDown &&
                    (lazyListState.firstVisibleItemIndex > 0 || lazyListState.firstVisibleItemScrollOffset > 0)
                ) {
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
            val canExpand =
                lazyListState.firstVisibleItemIndex == 0 && lazyListState.firstVisibleItemScrollOffset == 0
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
            .graphicsLayer {
                alpha = contentAlpha
                translationY = contentOffset.toPx()
            },
    ) {
        val currentTopBarHeightDp = with(density) { topBarHeight.value.toDp() }
        LazyColumn(
            state = lazyListState,
            contentPadding = PaddingValues(
                top = currentTopBarHeightDp + 8.dp,
                bottom = MiniPlayerHeight +
                    WindowInsets.navigationBars
                        .asPaddingValues()
                        .calculateBottomPadding() + 12.dp,
            ),
            modifier = Modifier
                .fillMaxSize()
                .scrollMotionBlur(lazyListState),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            item(key = "hero_card") {
                AboutHeroCard(
                    navController = navController, // <-- ADD THIS LINE!
                    versionName = versionName,
                    versionCode = versionCode,
                    onVersionLongPress = {
                        navController.navigateSafely(Screen.EasterEgg.route)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(top = 8.dp),
                )
            }

            item(key = "credits_card") {
                CreditsCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(top = 16.dp),
                )
            }

            item(key = "bottom_spacer") {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }

        CollapsibleCommonTopBar(
            title = stringResource(R.string.screen_about),
            collapseFraction = collapseFraction,
            headerHeight = currentTopBarHeightDp,
            onBackClick = onNavigationIconClick,
            expandedTitleStartPadding = 20.dp,
            collapsedTitleStartPadding = 68.dp
        )
    }
}

@Composable
private fun AboutHeroCard(
    navController: NavController, // <-- It receives the controller here
    versionName: String,
    versionCode: Long,
    onVersionLongPress: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val heroShape = AbsoluteSmoothCornerShape(30.dp, 60)
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var updateState by remember { mutableStateOf<UpdateState>(UpdateState.Checking) }
    var showChangelogDialog by remember { mutableStateOf(false) }
    var isDowngradeSheetMode by remember { mutableStateOf(false) }
    val downloadState by InAppUpdater.downloadState.collectAsStateWithLifecycle()
    val openUpdateSheetEvent by InAppUpdater.openUpdateSheetEvent.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        updateState = InAppUpdater.checkForUpdate(versionName)
    }

    LaunchedEffect(openUpdateSheetEvent) {
        if (openUpdateSheetEvent) {
            InAppUpdater.consumeOpenUpdateSheetEvent()
            showChangelogDialog = true
        }
    }

    val latestChangelog = when (val state = updateState) {
        is UpdateState.UpToDate -> state.changelog
        is UpdateState.Available -> state.changelog
        else -> null
    }

    if (showChangelogDialog) {
        val isUpdate = (updateState is UpdateState.Available) || isDowngradeSheetMode
        val displayVersion = if (isDowngradeSheetMode) {
            when (val state = updateState) {
                is UpdateState.UpToDate -> state.previousVersionName ?: versionName
                is UpdateState.Available -> state.previousVersionName ?: versionName
                else -> InAppUpdater.getResolvedVersionName() ?: versionName
            }
        } else {
            when (val state = updateState) {
                is UpdateState.Available -> state.versionName
                is UpdateState.UpToDate -> state.versionName ?: versionName
                else -> versionName
            }
        }
        val resolvedUrl = if (isDowngradeSheetMode) {
            when (val state = updateState) {
                is UpdateState.UpToDate -> state.previousDownloadUrl
                is UpdateState.Available -> state.previousDownloadUrl
                else -> InAppUpdater.getResolvedDownloadUrl()
            }
        } else {
            when (val state = updateState) {
                is UpdateState.Available -> state.downloadUrl
                is UpdateState.UpToDate -> state.downloadUrl
                else -> InAppUpdater.getResolvedDownloadUrl()
            }
        }
        val resolvedApkName = if (isDowngradeSheetMode) {
            when (val state = updateState) {
                is UpdateState.UpToDate -> state.previousApkName
                is UpdateState.Available -> state.previousApkName
                else -> InAppUpdater.getResolvedApkName()
            }
        } else {
            when (val state = updateState) {
                is UpdateState.Available -> state.apkName
                is UpdateState.UpToDate -> state.apkName
                else -> InAppUpdater.getResolvedApkName()
            }
        }
        val resolvedApkSize = if (isDowngradeSheetMode) {
            when (val state = updateState) {
                is UpdateState.UpToDate -> state.previousApkSizeBytes
                is UpdateState.Available -> state.previousApkSizeBytes
                else -> InAppUpdater.getResolvedApkSizeBytes()
            }
        } else {
            when (val state = updateState) {
                is UpdateState.Available -> state.apkSizeBytes
                is UpdateState.UpToDate -> state.apkSizeBytes
                else -> InAppUpdater.getResolvedApkSizeBytes()
            }
        }
        val sheetChangelog = if (isDowngradeSheetMode) {
            when (val state = updateState) {
                is UpdateState.UpToDate -> state.previousChangelog
                is UpdateState.Available -> state.previousChangelog
                else -> latestChangelog
            }
        } else {
            latestChangelog
        }
        UpdateNotificationSheet(
            isUpdateAvailable = isUpdate,
            versionName = displayVersion,
            changelog = sheetChangelog,
            downloadUrl = resolvedUrl,
            apkName = resolvedApkName,
            apkSizeBytes = resolvedApkSize,
            isTestBuild = context.packageName.endsWith(".test"),
            isDowngradeMode = isDowngradeSheetMode,
            onDismiss = {
                showChangelogDialog = false
                isDowngradeSheetMode = false
            },
            onConfirmClick = {
                if (!resolvedUrl.isNullOrBlank()) {
                    val appContext = context.applicationContext
                    InAppUpdater.startOrResumeDownload(
                        context = appContext,
                        url = resolvedUrl,
                        versionName = displayVersion,
                        apkName = resolvedApkName,
                        changelog = sheetChangelog
                    )
                }
            },
            onMigrateClick = {
                val intent = Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://github.com/Saurav-02/PixelMusic/releases/latest")
                ).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
                try { context.startActivity(intent) } catch (_: ActivityNotFoundException) {}
            }
        )
    }

    Surface(
        modifier = modifier,
        shape = heroShape,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = 2.dp,
    ) {
        Box(
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 16.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.pixelmusic_base_monochrome),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(10.dp).size(28.dp),
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.about_app_name),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = stringResource(R.string.about_tagline),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                val deviceArch = remember { InAppUpdater.getDeviceArchitecture() }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.tertiaryContainer)
                            .pointerInput(Unit) {
                                detectTapGestures(
                                    onLongPress = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        onVersionLongPress()
                                    },
                                )
                            },
                    ) {
                        Text(
                            text = stringResource(R.string.about_version_format, versionName),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                    ) {
                        Text(
                            text = deviceArch,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .animateContentSize(
                            animationSpec = spring(
                                dampingRatio = 0.9f,
                                stiffness = Spring.StiffnessMediumLow
                            )
                        )
                ) {
                    AnimatedContent<UpdateState>(
                        targetState = updateState,
                        transitionSpec = {
                            (fadeIn(animationSpec = tween(360)) +
                                scaleIn(initialScale = 0.97f, animationSpec = tween(360)))
                                .togetherWith(
                                    fadeOut(animationSpec = tween(240)) +
                                        scaleOut(targetScale = 0.97f, animationSpec = tween(240))
                                )
                        },
                        contentKey = { it::class },
                        label = "update_button_anim"
                    ) { state ->
                        when (state) {
                            is UpdateState.Checking -> {
                                FilledTonalButton(
                                    onClick = { },
                                    enabled = false,
                                    shape = CircleShape,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        strokeWidth = 2.dp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                    )
                                    Spacer(Modifier.width(10.dp))
                                    Text("Checking for updates...")
                                }
                            }
                            is UpdateState.Error -> {
                                val errorMsg = state.message
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    FilledTonalButton(
                                        onClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            updateState = UpdateState.Checking
                                            coroutineScope.launch {
                                                val startedAt = System.currentTimeMillis()
                                                val result = InAppUpdater.checkForUpdate(versionName, forceRefresh = true)
                                                val elapsed = System.currentTimeMillis() - startedAt
                                                if (elapsed < 400L) {
                                                    kotlinx.coroutines.delay(400L - elapsed)
                                                }
                                                updateState = result
                                            }
                                        },
                                        shape = CircleShape,
                                        colors = ButtonDefaults.filledTonalButtonColors(
                                            containerColor = MaterialTheme.colorScheme.errorContainer,
                                            contentColor = MaterialTheme.colorScheme.onErrorContainer
                                        ),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(48.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Refresh,
                                            contentDescription = "Retry update check",
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Text(
                                            text = "Update check failed · Tap to retry",
                                            fontWeight = FontWeight.Medium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = errorMsg,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                            is UpdateState.UpToDate -> {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    // If a download (e.g. downgrade) is active while sheet is closed, show live progress pill
                                    if (downloadState is InAppUpdater.GlobalDownloadState.Downloading ||
                                        downloadState is InAppUpdater.GlobalDownloadState.Finished
                                    ) {
                                        AboutUpdateActionPill(
                                            defaultLabel = "Open download",
                                            downloadState = downloadState,
                                            onClick = {
                                                showChangelogDialog = true
                                            }
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                    }

                                    FilledTonalButton(
                                        onClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            updateState = UpdateState.Checking
                                            coroutineScope.launch {
                                                val startedAt = System.currentTimeMillis()
                                                val result = InAppUpdater.checkForUpdate(versionName, forceRefresh = true)
                                                val elapsed = System.currentTimeMillis() - startedAt
                                                if (elapsed < 400L) {
                                                    kotlinx.coroutines.delay(400L - elapsed)
                                                }
                                                updateState = result
                                            }
                                        },
                                        shape = CircleShape,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(48.dp)
                                    ) {
                                        Icon(Icons.Rounded.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(8.dp))
                                        Text(
                                            text = "You are on the latest version",
                                            fontWeight = FontWeight.Medium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Icon(
                                            imageVector = Icons.Rounded.Refresh,
                                            contentDescription = "Check for updates",
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }

                                    if (!latestChangelog.isNullOrBlank()) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Button(
                                            onClick = {
                                                isDowngradeSheetMode = false
                                                showChangelogDialog = true
                                            },
                                            shape = CircleShape,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(48.dp),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                            )
                                        ) {
                                            Icon(painterResource(R.drawable.rounded_notes_24), contentDescription = null, modifier = Modifier.size(18.dp))
                                            Spacer(Modifier.width(8.dp))
                                            Text("Changes in the latest version")
                                        }
                                    }

                                    if (!state.previousDownloadUrl.isNullOrBlank()) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Button(
                                            onClick = {
                                                isDowngradeSheetMode = true
                                                showChangelogDialog = true
                                            },
                                            shape = CircleShape,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(48.dp),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                                                contentColor = MaterialTheme.colorScheme.onSurface
                                            )
                                        ) {
                                            Icon(painterResource(R.drawable.outline_restart_alt_24), contentDescription = null, modifier = Modifier.size(18.dp))
                                            Spacer(Modifier.width(8.dp))
                                            Text(
                                                if (!state.previousVersionName.isNullOrBlank()) {
                                                    "Downgrade to ${state.previousVersionName}"
                                                } else {
                                                    "Downgrade to previous version"
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                            is UpdateState.Available -> {
                                val readableVersion = formatReadableAboutVersion(state.versionName)
                                AboutUpdateActionPill(
                                    defaultLabel = "$readableVersion is ready to download",
                                    downloadState = downloadState,
                                    onClick = {
                                        isDowngradeSheetMode = false
                                        showChangelogDialog = true
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                CommunitySignalsRow()
                Spacer(modifier = Modifier.height(12.dp))
                SocialLinksRow()
            }
        }
    }
}
@Composable
private fun AboutUpdateActionPill(
    defaultLabel: String,
    downloadState: InAppUpdater.GlobalDownloadState,
    onClick: () -> Unit
) {
    val isBwTheme = LocalIsBlackAndWhiteTheme.current
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val pillShape = CircleShape

    val primaryButtonColor = when {
        isBwTheme -> if (isDark) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.primary
        isDark -> {
            val hsl = FloatArray(3)
            ColorUtils.colorToHSL(MaterialTheme.colorScheme.primary.toArgb(), hsl)
            hsl[2] = (hsl[2] * 0.45f).coerceIn(0.20f, 0.38f)
            Color(ColorUtils.HSLToColor(hsl))
        }
        else -> MaterialTheme.colorScheme.primary
    }
    val buttonContentColor = if (isBwTheme && !isDark) MaterialTheme.colorScheme.onPrimary else Color.White

    val phase = remember(downloadState) {
        when (downloadState) {
            is InAppUpdater.GlobalDownloadState.Downloading -> "DOWNLOADING"
            is InAppUpdater.GlobalDownloadState.Finished -> "FINISHED"
            else -> "IDLE"
        }
    }

    AnimatedContent(
        targetState = phase,
        transitionSpec = {
            (fadeIn(tween(340)) + scaleIn(initialScale = 0.97f, animationSpec = tween(340)))
                .togetherWith(fadeOut(tween(220)) + scaleOut(targetScale = 0.97f, animationSpec = tween(220)))
        },
        label = "AboutUpdatePillPhase"
    ) { currentPhase ->
        when (currentPhase) {
            "DOWNLOADING" -> {
                val dl = downloadState as? InAppUpdater.GlobalDownloadState.Downloading
                val rawProgress = (dl?.progress ?: 0f).coerceIn(0f, 1f)
                val isPaused = dl?.isPaused ?: false
                val animatedProgress by animateFloatAsState(
                    targetValue = rawProgress,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioNoBouncy,
                        stiffness = Spring.StiffnessVeryLow
                    ),
                    label = "AboutPillDownloadProgress"
                )
                val percentInt = (animatedProgress * 100f).roundToInt().coerceIn(0, 100)

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .clip(pillShape)
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                        .clickable(onClick = onClick)
                ) {
                    if (animatedProgress > 0.005f) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(fraction = animatedProgress)
                                .background(MaterialTheme.colorScheme.primaryContainer)
                        )
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 18.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.rounded_download_24),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = if (isPaused) {
                                "Paused · $percentInt%"
                            } else {
                                "Downloading... $percentInt%"
                            },
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
            "FINISHED" -> {
                val fin = downloadState as? InAppUpdater.GlobalDownloadState.Finished
                val versionName = fin?.versionName
                Button(
                    onClick = onClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = pillShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = primaryButtonColor,
                        contentColor = buttonContentColor
                    )
                ) {
                    Icon(
                        painter = painterResource(R.drawable.rounded_done_all_24),
                        contentDescription = null,
                        tint = buttonContentColor,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = if (!versionName.isNullOrBlank()) {
                            "Ready to install (${formatCompactAboutVersion(versionName)})"
                        } else {
                            "Ready to install"
                        },
                        fontWeight = FontWeight.Bold,
                        color = buttonContentColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            else -> {
                Button(
                    onClick = onClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = pillShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = primaryButtonColor,
                        contentColor = buttonContentColor
                    ),
                    elevation = ButtonDefaults.buttonElevation()
                ) {
                    Icon(
                        painter = painterResource(R.drawable.rounded_auto_awesome_24),
                        contentDescription = null,
                        tint = buttonContentColor,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = defaultLabel,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = buttonContentColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

private fun formatReadableAboutVersion(rawVersion: String): String {
    val trimmed = rawVersion.trim()
    if (trimmed.isEmpty()) return trimmed
    val normalized = trimmed.removePrefix("v").removePrefix("V")
    if (normalized.startsWith("test-build-", ignoreCase = true)) {
        val buildNum = normalized.substringAfter("-build-", normalized)
        return "Test build $buildNum"
    }
    return formatCompactAboutVersion(trimmed)
}

private fun formatCompactAboutVersion(rawVersion: String): String {
    val trimmed = rawVersion.trim()
    if (trimmed.isEmpty()) return trimmed
    if (trimmed.startsWith("test-build-", ignoreCase = true)) {
        return "beta-" + trimmed.substringAfter("-build-", trimmed)
    }
    if (trimmed.startsWith("v", ignoreCase = true) || trimmed.startsWith("beta", ignoreCase = true)) {
        return trimmed
    }
    return "v$trimmed"
}

private fun expressiveAboutCardShape(index: Int, totalCount: Int): Shape {
    val outer = 22.dp
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

private data class AboutLinkItem(
    val title: String,
    val subtitle: String,
    val url: String,
    @DrawableRes val iconRes: Int,
    val badge: String? = null,
    val usePrimaryTint: Boolean = false
)

@Composable
private fun SocialLinksRow() {
    val context = LocalContext.current
    val links = remember {
        listOf(
            AboutLinkItem(
                title = "Main Dev.",
                subtitle = "@Saurav124x",
                url = "https://t.me/Saurav124x",
                iconRes = R.drawable.telegram,
                usePrimaryTint = true
            ),
            AboutLinkItem(
                title = "Update Bot",
                subtitle = "@PixelMusicAppBot",
                url = "https://t.me/PixelMusicAppBot",
                iconRes = R.drawable.telegram,
                badge = "Latest & Beta APKs",
                usePrimaryTint = true
            ),
            AboutLinkItem(
                title = "PixelMusic Website",
                subtitle = "pixelmusic.pages.dev",
                url = "https://pixelmusic.pages.dev",
                iconRes = R.drawable.rounded_public_24,
                usePrimaryTint = false
            ),
            AboutLinkItem(
                title = "GitHub",
                subtitle = "saurav-02/PixelMusic",
                url = "https://github.com/Saurav-02/PixelMusic",
                iconRes = R.drawable.github,
                usePrimaryTint = false
            )
        )
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        links.forEachIndexed { index, item ->
            val interactionSource = remember { MutableInteractionSource() }
            val isPressed by interactionSource.collectIsPressedAsState()
            val pressScale by animateFloatAsState(
                targetValue = if (isPressed) 0.98f else 1f,
                animationSpec = spring(
                    dampingRatio = 0.85f,
                    stiffness = Spring.StiffnessMediumLow
                ),
                label = "about_link_press_$index"
            )

            val containerColor = if (item.usePrimaryTint) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
            } else {
                MaterialTheme.colorScheme.surfaceContainerHigh
            }
            val titleColor = if (item.usePrimaryTint) {
                MaterialTheme.colorScheme.onPrimaryContainer
            } else {
                MaterialTheme.colorScheme.onSurface
            }
            val iconBgColor = if (item.usePrimaryTint) {
                MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
            } else {
                MaterialTheme.colorScheme.surfaceContainerHighest
            }
            val iconTint = if (item.usePrimaryTint) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurface
            }

            Surface(
                onClick = {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(item.url)).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    try {
                        context.startActivity(intent)
                    } catch (_: ActivityNotFoundException) {}
                },
                interactionSource = interactionSource,
                shape = expressiveAboutCardShape(index, links.size),
                color = containerColor,
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        scaleX = pressScale
                        scaleY = pressScale
                    }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 13.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Surface(
                        shape = CircleShape,
                        color = iconBgColor,
                        modifier = Modifier.size(36.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            Icon(
                                painter = painterResource(item.iconRes),
                                contentDescription = null,
                                tint = iconTint,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Text(
                                text = item.title,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = titleColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            if (item.badge != null) {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                                ) {
                                    Text(
                                        text = item.badge,
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    )
                                }
                            }
                        }
                        Text(
                            text = item.subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = titleColor.copy(alpha = 0.68f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Icon(
                        painter = painterResource(R.drawable.rounded_open_in_new_24),
                        contentDescription = null,
                        tint = titleColor.copy(alpha = 0.45f),
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CommunitySignalsRow() {
    val labels = listOf(
        stringResource(R.string.about_signal_open_source) to R.drawable.rounded_public_24,
        stringResource(R.string.about_signal_community_first) to R.drawable.rounded_auto_awesome_24,
        stringResource(R.string.about_signal_material3) to R.drawable.rounded_palette_24,
    )

    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        labels.forEach { (label, iconRes) ->
            Surface(
                shape = AbsoluteSmoothCornerShape(16.dp, 60),
                color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.92f),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        painter = painterResource(iconRes),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(15.dp),
                    )
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
private fun CreditsCard(
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val cardShape = RoundedCornerShape(28.dp)
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val enterProgress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        enterProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(
                durationMillis = 560,
                easing = FastOutSlowInEasing
            )
        )
    }

    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 0.975f else 1f,
        animationSpec = spring(
            dampingRatio = 0.85f,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "creator_card_press"
    )
    val enterTranslationY = with(density) { (1f - enterProgress.value) * 20.dp.toPx() }
    val enterScale = 0.96f + (0.04f * enterProgress.value)

    fun openInstagram() {
        val intent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("https://www.instagram.com/holy_saurav")
        ).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(intent)
        } catch (_: ActivityNotFoundException) {}
    }

    Surface(
        onClick = ::openInstagram,
        interactionSource = interactionSource,
        shape = cardShape,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = 2.dp,
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                alpha = enterProgress.value
                translationY = enterTranslationY
                scaleX = enterScale * pressScale
                scaleY = enterScale * pressScale
            }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Left: Rounded Creator Avatar
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
                modifier = Modifier.size(74.dp)
            ) {
                Image(
                    painter = painterResource(R.drawable.saurav_profile),
                    contentDescription = "Saurav",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(2.dp)
                        .clip(CircleShape)
                )
            }

            // Right: Name, Made with love from India, and Instagram pill
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Saurav",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.tertiaryContainer
                    ) {
                        Text(
                            text = "Creator",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }

                Text(
                    text = "Made with ♥ in India",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                Surface(
                    onClick = ::openInstagram,
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_instagram),
                            contentDescription = "Instagram",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = "@holy_saurav",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Icon(
                            painter = painterResource(R.drawable.rounded_open_in_new_24),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.72f),
                            modifier = Modifier.size(13.dp)
                        )
                    }
                }
            }
        }
    }
}
