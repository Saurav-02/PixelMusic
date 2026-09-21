package com.saurav.pixelmusic.presentation.screens

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.material.icons.rounded.Notes
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
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import kotlinx.coroutines.flow.collectLatest
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Public
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.blur



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
            modifier = Modifier.fillMaxSize(),
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

    LaunchedEffect(Unit) {
        updateState = InAppUpdater.checkForUpdate(versionName)
    }

    val latestChangelog = when (val state = updateState) {
        is UpdateState.UpToDate -> state.changelog
        is UpdateState.Available -> state.changelog
        else -> null
    }

    if (showChangelogDialog) {
        val isUpdate = updateState is UpdateState.Available
        val displayVersion = when (val state = updateState) {
            is UpdateState.Available -> state.versionName
            else -> versionName
        }
        UpdateNotificationSheet(
            isUpdateAvailable = isUpdate,
            versionName = displayVersion,
            changelog = latestChangelog,
            onDismiss = { showChangelogDialog = false },
            onConfirmClick = {
                showChangelogDialog = false
                val availableState = updateState as? UpdateState.Available
                if (availableState != null) {
                    val appContext = context.applicationContext
                    InAppUpdater.startOrResumeDownload(
                        context = appContext,
                        url = availableState.downloadUrl,
                        versionName = availableState.versionName
                    )
                    navController.navigateSafely("update_download")
                }
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
                        text = "${stringResource(R.string.about_version_format, versionName)} ($versionCode)",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                        fontWeight = FontWeight.SemiBold,
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .animateContentSize()
                ) {
                    AnimatedContent<UpdateState>(
                        targetState = updateState,
                        transitionSpec = {
                            fadeIn(animationSpec = tween(300)) togetherWith fadeOut(animationSpec = tween(300))
                        },
                        contentKey = { it::class },
                        label = "update_button_anim"
                    ) { state ->
                        when (state) {
                            is UpdateState.Checking -> {
                                FilledTonalButton(
                                    onClick = { },
                                    enabled = false,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        strokeWidth = 2.dp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text("Checking for updates...")
                                }
                            }
                            is UpdateState.UpToDate -> {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    FilledTonalButton(
                                        onClick = { },
                                        enabled = false,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(Icons.Rounded.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(8.dp))
                                        Text("You are on the latest version")
                                    }
                                    
                                    if (!latestChangelog.isNullOrBlank()) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        
                                        Button(
                                            onClick = { showChangelogDialog = true },
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                            )
                                        ) {
                                            Icon(Icons.AutoMirrored.Rounded.Notes, contentDescription = null, modifier = Modifier.size(18.dp))
                                            Spacer(Modifier.width(8.dp))
                                            Text("Changes in the latest version")
                                        }
                                    }
                                }
                            }
                            is UpdateState.Available -> {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    if (!state.changelog.isNullOrBlank()) {
                                        val previewLines = remember(state.changelog) {
                                            val nonBlankLines = state.changelog
                                                .lines()
                                                .map { it.trim() }
                                                .filter { it.isNotEmpty() }

                                            if (nonBlankLines.size >= 3) {
                                                nonBlankLines.take(3)
                                            } else {
                                                // Fallback if the changelog is a single uninterrupted block
                                                val words = state.changelog.split(Regex("\\s+")).filter { it.isNotEmpty() }
                                                if (words.size >= 6) {
                                                    val chunkSize = (words.size + 2) / 3
                                                    words.chunked(chunkSize).take(3).map { it.joinToString(" ") }
                                                } else {
                                                    nonBlankLines
                                                }
                                            }
                                        }

                                        Surface(
                                            shape = AbsoluteSmoothCornerShape(16.dp, 60),
                                            color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.5f),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(bottom = 12.dp)
                                                .clip(AbsoluteSmoothCornerShape(16.dp, 60))
                                                .clickable { showChangelogDialog = true }
                                        ) {
                                            Column(modifier = Modifier.padding(14.dp)) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Icon(
                                                            imageVector = Icons.Rounded.AutoAwesome,
                                                            contentDescription = null,
                                                            modifier = Modifier.size(16.dp),
                                                            tint = MaterialTheme.colorScheme.primary
                                                        )
                                                        Spacer(Modifier.width(8.dp))
                                                        Text(
                                                            text = "What's new in this update",
                                                            style = MaterialTheme.typography.labelMedium,
                                                            color = MaterialTheme.colorScheme.primary,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                    }
                                                    Icon(
                                                        imageVector = Icons.AutoMirrored.Rounded.Notes,
                                                        contentDescription = "Read full changelog",
                                                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }

                                                Spacer(modifier = Modifier.height(8.dp))

                                                // 1st line: Clearly visible
                                                if (previewLines.isNotEmpty()) {
                                                    Text(
                                                        text = previewLines[0],
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }

                                                // 2nd line: Mid blur + slight fade
                                                if (previewLines.size > 1) {
                                                    Spacer(modifier = Modifier.height(3.dp))
                                                    Text(
                                                        text = previewLines[1],
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis,
                                                        modifier = Modifier
                                                            .blur(1.8.dp)
                                                            .graphicsLayer { alpha = 0.65f }
                                                    )
                                                }

                                                // 3rd line: High blur + heavier fade
                                                if (previewLines.size > 2) {
                                                    Spacer(modifier = Modifier.height(3.dp))
                                                    Text(
                                                        text = previewLines[2],
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis,
                                                        modifier = Modifier
                                                            .blur(4.2.dp)
                                                            .graphicsLayer { alpha = 0.35f }
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    Button(
                                        onClick = {
                                            val appContext = context.applicationContext
                                            val notificationManager = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
                                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                                                val channel = android.app.NotificationChannel(
                                                    "app_updates", 
                                                    "App Updates", 
                                                    android.app.NotificationManager.IMPORTANCE_LOW
                                                )
                                                notificationManager.createNotificationChannel(channel)
                                            }

                                            // Fire off the background singleton
                                            InAppUpdater.startOrResumeDownload(
                                                context = appContext,
                                                url = state.downloadUrl,
                                                versionName = state.versionName
                                            )
                                            
                                            // Launch the new beautiful UI screen!
                                            navController.navigateSafely("update_download")
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(Icons.Rounded.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(8.dp))
                                        Text("Download Version ${state.versionName}")
                                    }
                                }
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
private fun SocialLinksRow() {
    val context = LocalContext.current
    
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // Telegram 1: Main Developer (Saurav)
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .clickable {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/Saurav124x"))
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        try { context.startActivity(intent) } catch (e: ActivityNotFoundException) {}
                    },
                shape = AbsoluteSmoothCornerShape(16.dp, 60),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                        modifier = Modifier.size(30.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            Icon(
                                painter = painterResource(R.drawable.telegram),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(17.dp),
                            )
                        }
                    }
                    Column {
                        Text(
                            text = "Main Dev.",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            maxLines = 1,
                        )
                        Text(
                            text = "@Saurav124x",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.65f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }

            // Telegram 2: UI Makeover (Xyg901)
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .clickable {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/Xyg901"))
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        try { context.startActivity(intent) } catch (e: ActivityNotFoundException) {}
                    },
                shape = AbsoluteSmoothCornerShape(16.dp, 60),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                        modifier = Modifier.size(30.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            Icon(
                                painter = painterResource(R.drawable.telegram),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(17.dp),
                            )
                        }
                    }
                    Column {
                        Text(
                            text = "UI Makeover",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            maxLines = 1,
                        )
                        Text(
                            text = "@Xyg901",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.65f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    val pkgUri = android.net.Uri.parse("https://github.com/Saurav-02/PixelMusic")
                    val intent = Intent(Intent.ACTION_VIEW, pkgUri).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    try { context.startActivity(intent) } catch (e: ActivityNotFoundException) {}
                },
            shape = AbsoluteSmoothCornerShape(16.dp, 60),
            color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.92f),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
                    modifier = Modifier.size(30.dp),
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Icon(
                            painter = painterResource(R.drawable.github),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(17.dp),
                        )
                    }
                }
                Column {
                    Text(
                        text = "GitHub",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                    )
                    Text(
                        text = "saurav-02/PixelMusic",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
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
        stringResource(R.string.about_signal_open_source) to Icons.Rounded.Public,
        stringResource(R.string.about_signal_community_first) to Icons.Rounded.AutoAwesome,
        stringResource(R.string.about_signal_material3) to Icons.Rounded.Palette,
    )

    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        labels.forEach { (label, icon) ->
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
                        imageVector = icon,
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
    val cardShape = AbsoluteSmoothCornerShape(30.dp, 60)

    Surface(
        modifier = modifier,
        shape = cardShape,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = 2.dp,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .clip(cardShape)
                .clickable {
                    val intent = Intent(
                        Intent.ACTION_VIEW,
                        android.net.Uri.parse("https://www.instagram.com/holy_saurav")
                    ).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    try {
                        context.startActivity(intent)
                    } catch (_: ActivityNotFoundException) {
                        // Handle fallback if no browser is present
                    }
                }
        ) {
            // 1. Background Image
            Image(
                painter = painterResource(R.drawable.saurav_profile),
                contentDescription = "Saurav",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // 2. Dark Gradient Overlay for optimal text readability
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        androidx.compose.ui.graphics.Brush.verticalGradient(
                            colors = listOf(
                                androidx.compose.ui.graphics.Color.Transparent,
                                androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.85f)
                            ),
                            startY = 100f
                        )
                    )
            )

            // 3. Text and Instagram Tag
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                verticalArrangement = Arrangement.Bottom,
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = "With love from Saurav",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = androidx.compose.ui.graphics.Color.White
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Glassmorphism-style Instagram pill button
                Surface(
                    shape = CircleShape,
                    color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.2f),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_instagram),
                            contentDescription = "Instagram",
                            tint = androidx.compose.ui.graphics.Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "@holy_saurav",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = androidx.compose.ui.graphics.Color.White
                        )
                    }
                }
            }
        }
    }
}
