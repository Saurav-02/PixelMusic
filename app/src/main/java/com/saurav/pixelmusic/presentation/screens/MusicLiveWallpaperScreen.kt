package com.saurav.pixelmusic.presentation.screens

import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
import com.saurav.pixelmusic.presentation.components.PixelToast as Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.ui.res.painterResource
import com.saurav.pixelmusic.R
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavController
import com.saurav.pixelmusic.data.preferences.MusicLiveWallpaperSettings
import com.saurav.pixelmusic.data.preferences.MusicWallpaperShapePosition
import com.saurav.pixelmusic.data.preferences.MusicWallpaperStyleMode
import com.saurav.pixelmusic.data.preferences.MusicWallpaperTargetScreen
import com.saurav.pixelmusic.data.service.wallpaper.MusicLiveWallpaperService
import com.saurav.pixelmusic.data.service.wallpaper.MusicLiveWallpaperStateHolder
import com.saurav.pixelmusic.data.service.wallpaper.WallpaperExpressiveShapeKind
import com.saurav.pixelmusic.data.service.wallpaper.WallpaperMorphingComposeShape
import com.saurav.pixelmusic.data.service.wallpaper.WallpaperSmoothMorphEasing
import com.saurav.pixelmusic.data.service.wallpaper.verticalCenterFraction
import com.saurav.pixelmusic.presentation.components.CollapsibleCommonTopBar
import com.saurav.pixelmusic.presentation.components.MiniPlayerHeight
import com.saurav.pixelmusic.presentation.viewmodel.PlayerViewModel
import com.saurav.pixelmusic.presentation.viewmodel.SettingsViewModel
import com.saurav.pixelmusic.ui.modifiers.scrollMotionBlur
import com.saurav.pixelmusic.ui.theme.ActualGoogleSansRounded
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

private enum class WallpaperPreviewStateMode {
    MUSIC_ART,
    FALLBACK_WALLPAPER
}

@OptIn(UnstableApi::class)
@Composable
fun MusicLiveWallpaperScreen(
    navController: NavController,
    playerViewModel: PlayerViewModel,
    onNavigationIconClick: () -> Unit,
    settingsViewModel: SettingsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val hapticFeedback = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val uiState by settingsViewModel.uiState.collectAsStateWithLifecycle()
    val prefsRepo = playerViewModel.userPreferencesRepository
    val wallpaperSettings by prefsRepo.musicLiveWallpaperSettingsFlow
        .collectAsStateWithLifecycle(initialValue = MusicLiveWallpaperSettings())
    val playerState by playerViewModel.stablePlayerState.collectAsStateWithLifecycle()

    val currentSong = playerState.currentSong
    val previewTitle = currentSong?.title?.takeIf { it.isNotBlank() } ?: "Midnight Horizon"
    val previewArtist = currentSong?.displayArtist?.takeIf { it.isNotBlank() }
        ?: currentSong?.artist?.takeIf { it.isNotBlank() }
        ?: "PixelMusic Expressive"

    val isWallpaperActive = wallpaperSettings.isEnabled
    var isApplyingWallpaper by remember { mutableStateOf(false) }

    // Default expressive artwork bitmap for preview when no song or gallery wallpaper is loaded yet
    val primaryArgb = MaterialTheme.colorScheme.primary.toArgb()
    val tertiaryArgb = MaterialTheme.colorScheme.tertiary.toArgb()
    val primaryContainerArgb = MaterialTheme.colorScheme.primaryContainer.toArgb()
    val defaultPreviewBitmap = remember(primaryArgb, tertiaryArgb, primaryContainerArgb) {
        createDefaultExpressiveBitmap(primaryContainerArgb, primaryArgb, tertiaryArgb)
    }

    // Load already-cached album art & fallback gallery bitmap
    var cachedSongBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var fallbackBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var softwareBlurredBitmap by remember { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(currentSong?.albumArtUriString) {
        val uriStr = currentSong?.albumArtUriString
        if (!uriStr.isNullOrBlank()) {
            val loaded = MusicLiveWallpaperStateHolder.loadCachedAlbumArtBitmap(context, uriStr)
            if (loaded != null) {
                cachedSongBitmap = loaded
            }
        }
    }

    LaunchedEffect(wallpaperSettings.fallbackWallpaperPath, wallpaperSettings.fallbackWallpaperUpdatedAt) {
        fallbackBitmap = MusicLiveWallpaperStateHolder.loadFallbackWallpaperBitmap(
            context,
            wallpaperSettings.fallbackWallpaperPath,
            wallpaperSettings.fallbackWallpaperUpdatedAt
        )
    }

    // Preview mode toggle so user can inspect both Music Art and Fallback Wallpaper crossfades
    var previewStateMode by remember { mutableStateOf(WallpaperPreviewStateMode.MUSIC_ART) }

    val activePreviewBitmap = when (previewStateMode) {
        WallpaperPreviewStateMode.MUSIC_ART -> cachedSongBitmap ?: fallbackBitmap ?: defaultPreviewBitmap
        WallpaperPreviewStateMode.FALLBACK_WALLPAPER -> fallbackBitmap ?: cachedSongBitmap ?: defaultPreviewBitmap
    }

    // Pre-compute software blurred bitmap for preview & style cards
    LaunchedEffect(activePreviewBitmap, wallpaperSettings.blurAmount) {
        val src = activePreviewBitmap
        if (!src.isRecycled) {
            softwareBlurredBitmap = withContext(Dispatchers.Default) {
                MusicLiveWallpaperStateHolder.getOrCreateBlurredBitmap(
                    source = src,
                    sourceKey = "preview_${src.hashCode()}",
                    blurAmount = wallpaperSettings.blurAmount.coerceAtLeast(0.25f)
                )
            }
        } else {
            softwareBlurredBitmap = null
        }
    }

    // Gallery Picker for Default (Fallback) Wallpaper
    var isSavingFallback by remember { mutableStateOf(false) }
    val handlePickedGalleryUri: (android.net.Uri?) -> Unit = { uri ->
        if (uri != null) {
            isSavingFallback = true
            coroutineScope.launch {
                val updatedAt = System.currentTimeMillis()
                val savedPath = MusicLiveWallpaperStateHolder.saveFallbackWallpaperFromUri(context, uri)
                if (savedPath != null) {
                    prefsRepo.setMusicWallpaperFallback(savedPath, updatedAt)
                    prefsRepo.setMusicWallpaperEnabled(true)
                    val updatedSettings = wallpaperSettings.copy(
                        isEnabled = true,
                        fallbackWallpaperPath = savedPath,
                        fallbackWallpaperUpdatedAt = updatedAt
                    )
                    MusicLiveWallpaperStateHolder.applyPseudoLiveWallpaperNow(
                        context = context,
                        settingsOverride = updatedSettings,
                        forceFallbackIdle = !playerState.isPlaying
                    )
                    isSavingFallback = false
                    Toast.makeText(
                        context,
                        "Default fallback wallpaper saved",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    isSavingFallback = false
                    Toast.makeText(context, "Could not load selected image", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    val pickVisualMediaLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = handlePickedGalleryUri
    )
    val fallbackGetContentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = handlePickedGalleryUri
    )

    val launchGalleryPicker: () -> Unit = {
        runCatching {
            pickVisualMediaLauncher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
        }.onFailure {
            runCatching { fallbackGetContentLauncher.launch("image/*") }
        }
    }

    // Collapsible Top Bar state
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
                    coroutineScope.launch { topBarHeight.snapTo(newHeight) }
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

    // AOD-style Material You Shape Morphing + Rotational Spin state for the Preview
    var previousShapeIdx by remember { mutableIntStateOf(wallpaperSettings.shapeIndex) }
    var targetShapeIdx by remember { mutableIntStateOf(wallpaperSettings.shapeIndex) }
    val shapeMorphAnim = remember { Animatable(1f) }
    val tapSpinRotationDeg = remember { Animatable(0f) }

    val animateToShapeIndex: (Int) -> Unit = { newIdx ->
        val normalized = newIdx.mod(WallpaperExpressiveShapeKind.entries.size)
        previousShapeIdx = targetShapeIdx
        targetShapeIdx = normalized
        coroutineScope.launch {
            prefsRepo.setMusicWallpaperShapeIndex(normalized)
        }
        coroutineScope.launch {
            shapeMorphAnim.snapTo(0f)
            launch {
                shapeMorphAnim.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = 820, easing = WallpaperSmoothMorphEasing)
                )
            }
            launch {
                tapSpinRotationDeg.animateTo(
                    targetValue = tapSpinRotationDeg.value + 95f,
                    animationSpec = tween(durationMillis = 820, easing = WallpaperSmoothMorphEasing)
                )
            }
        }
    }

    LaunchedEffect(wallpaperSettings.shapeIndex) {
        val externalIdx = wallpaperSettings.shapeIndex.mod(WallpaperExpressiveShapeKind.entries.size)
        if (externalIdx != targetShapeIdx) {
            previousShapeIdx = targetShapeIdx
            targetShapeIdx = externalIdx
            shapeMorphAnim.snapTo(0f)
            launch {
                shapeMorphAnim.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = 820, easing = WallpaperSmoothMorphEasing)
                )
            }
            launch {
                tapSpinRotationDeg.animateTo(
                    targetValue = tapSpinRotationDeg.value + 95f,
                    animationSpec = tween(durationMillis = 820, easing = WallpaperSmoothMorphEasing)
                )
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
            // =========================================================================
            // 1. INTERACTIVE PICTURE-ONLY WALLPAPER PREVIEW
            // =========================================================================
            item(key = "wallpaper_preview_section") {
                SettingsSection(
                    title = "Live Preview",
                    icon = {
                        Icon(
                            painter = painterResource(R.drawable.rounded_wallpaper_24),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainer,
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 18.dp)
                        ) {
                            // Picture-only phone wallpaper preview
                            WallpaperPictureOnlyPreview(
                                settings = wallpaperSettings,
                                previewMode = previewStateMode,
                                activeBitmap = activePreviewBitmap,
                                softwareBlurredBitmap = softwareBlurredBitmap,
                                songTitle = previewTitle,
                                artistName = previewArtist,
                                fromShape = WallpaperExpressiveShapeKind.fromIndex(previousShapeIdx),
                                toShape = WallpaperExpressiveShapeKind.fromIndex(targetShapeIdx),
                                shapeMorphProgress = shapeMorphAnim.value,
                                tapRotationDeg = tapSpinRotationDeg.value,
                                onTapShape = {
                                    hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    val nextIdx = WallpaperExpressiveShapeKind.nextIndex(targetShapeIdx)
                                    animateToShapeIndex(nextIdx)
                                },
                                modifier = Modifier
                                    .fillMaxWidth(0.62f)
                                    .aspectRatio(9f / 19.5f)
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            // State switcher to test smooth crossfading between Now Playing Art & Default Fallback Wallpaper
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.65f))
                                    .padding(4.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                PreviewStateTab(
                                    label = "Music Playing",
                                    selected = previewStateMode == WallpaperPreviewStateMode.MUSIC_ART,
                                    onClick = { previewStateMode = WallpaperPreviewStateMode.MUSIC_ART },
                                    modifier = Modifier.weight(1f)
                                )
                                PreviewStateTab(
                                    label = "Fallback (No Music)",
                                    selected = previewStateMode == WallpaperPreviewStateMode.FALLBACK_WALLPAPER,
                                    onClick = { previewStateMode = WallpaperPreviewStateMode.FALLBACK_WALLPAPER },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }

            // =========================================================================
            // 2. APPLY / ACTIVATE PSEUDO-LIVE STATIC WALLPAPER CARD (FORCE-STOP SAFE)
            // =========================================================================
            item(key = "apply_wallpaper_section") {
                SettingsSection(
                    title = "Activation",
                    icon = {
                        Icon(
                            painter = painterResource(R.drawable.rounded_auto_awesome_24),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                ) {
                    Surface(
                        color = if (isWallpaperActive) {
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
                        } else {
                            MaterialTheme.colorScheme.surfaceContainer
                        },
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier.fillMaxWidth()
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
                                if (isWallpaperActive) {
                                    Icon(
                                        imageVector = Icons.Rounded.CheckCircle,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(26.dp)
                                    )
                                } else {
                                    Icon(
                                        painter = painterResource(R.drawable.rounded_wallpaper_24),
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.secondary,
                                        modifier = Modifier.size(26.dp)
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = if (isWallpaperActive) {
                                            "Music Wallpaper is Active"
                                        } else {
                                            "Enable Music Wallpaper"
                                        },
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = if (isWallpaperActive) {
                                            "Album art is drawn live over your gallery fallback. Nothing is written to the system wallpaper, and Material You colors come only from the fallback image."
                                        } else {
                                            "Set PixelMusic as your live wallpaper. Album art is drawn live over your gallery fallback, and Material You colors come only from the fallback image."
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            // Target screen selector (Home, Lock, Both)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                MusicWallpaperTargetScreen.entries.forEach { target ->
                                    WallpaperPositionChip(
                                        label = target.label,
                                        selected = wallpaperSettings.targetScreen == target,
                                        onClick = {
                                            coroutineScope.launch {
                                                prefsRepo.setMusicWallpaperTargetScreen(target)
                                            }
                                        },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Button(
                                    onClick = {
                                        if (fallbackBitmap == null) {
                                            Toast.makeText(
                                                context,
                                                "Select a Default Wallpaper from Gallery first",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                            launchGalleryPicker()
                                            return@Button
                                        }
                                        coroutineScope.launch {
                                            prefsRepo.setMusicWallpaperEnabled(true)
                                        }
                                        // Open Android's live wallpaper preview for PixelMusic. The service draws
                                        // album art itself; nothing is written to the system wallpaper.
                                        val liveWallpaperComponent = ComponentName(
                                            context,
                                            MusicLiveWallpaperService::class.java
                                        )
                                        val opened = runCatching {
                                            context.startActivity(
                                                Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER)
                                                    .putExtra(
                                                        WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
                                                        liveWallpaperComponent
                                                    )
                                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                            )
                                        }.isSuccess || runCatching {
                                            context.startActivity(
                                                Intent(WallpaperManager.ACTION_LIVE_WALLPAPER_CHOOSER)
                                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                            )
                                        }.isSuccess
                                        if (!opened) {
                                            Toast.makeText(
                                                context,
                                                "Could not open the live wallpaper picker",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    },
                                    enabled = !isApplyingWallpaper,
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.rounded_wallpaper_24),
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = when {
                                            isApplyingWallpaper -> "Applying…"
                                            isWallpaperActive -> "Set Live Wallpaper Again"
                                            else -> "Set as Live Wallpaper"
                                        },
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }

                                if (isWallpaperActive) {
                                    FilledTonalButton(
                                        onClick = {
                                            coroutineScope.launch {
                                                MusicLiveWallpaperStateHolder.applyPseudoLiveWallpaperNow(
                                                    context = context,
                                                    settingsOverride = wallpaperSettings.copy(isEnabled = false),
                                                    forceFallbackIdle = true
                                                )
                                                prefsRepo.setMusicWallpaperEnabled(false)
                                                Toast.makeText(
                                                    context,
                                                    "Music Wallpaper paused; fallback wallpaper kept on screen",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                        },
                                        shape = RoundedCornerShape(14.dp)
                                    ) {
                                        Text("Disable")
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // =========================================================================
            // 3. DEFAULT / FALLBACK WALLPAPER (FROM GALLERY)
            // =========================================================================
            item(key = "fallback_wallpaper_section") {
                SettingsSection(
                    title = "Default / Fallback Wallpaper",
                    icon = {
                        Icon(
                            painter = painterResource(R.drawable.rounded_photo_library_24),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainer,
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                // Thumbnail of selected fallback image
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .size(64.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                                        .clickable { launchGalleryPicker() }
                                ) {
                                    val fb = fallbackBitmap
                                    if (fb != null && !fb.isRecycled) {
                                        Image(
                                            bitmap = fb.asImageBitmap(),
                                            contentDescription = "Fallback wallpaper thumbnail",
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    } else {
                                        Icon(
                                            painter = painterResource(R.drawable.rounded_photo_library_24),
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = if (fallbackBitmap != null) {
                                            "Custom Gallery Wallpaper Selected"
                                        } else {
                                            "Select Default Wallpaper from Gallery"
                                        },
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "Shown when no music is playing. Smoothly crossfades to album art when playback starts.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                FilledTonalButton(
                                    onClick = launchGalleryPicker,
                                    enabled = !isSavingFallback,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.rounded_imagesmode_24),
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (isSavingFallback) {
                                            "Saving…"
                                        } else if (fallbackBitmap != null) {
                                            "Change from Gallery"
                                        } else {
                                            "Choose from Gallery"
                                        }
                                    )
                                }

                                if (wallpaperSettings.fallbackWallpaperPath.isNotBlank()) {
                                    FilledTonalButton(
                                        onClick = {
                                            coroutineScope.launch {
                                                MusicLiveWallpaperStateHolder.clearFallbackWallpaper(context)
                                                prefsRepo.setMusicWallpaperFallback("", 0L)
                                                fallbackBitmap = null
                                            }
                                        },
                                        colors = ButtonDefaults.filledTonalButtonColors(
                                            containerColor = MaterialTheme.colorScheme.errorContainer,
                                            contentColor = MaterialTheme.colorScheme.onErrorContainer
                                        ),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Icon(
                                            painter = painterResource(R.drawable.rounded_delete_outline_24),
                                            contentDescription = "Clear fallback wallpaper",
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // =========================================================================
            // 4. WALLPAPER STYLE & BLUR / MATERIAL YOU SHAPES CUSTOMIZATION
            // =========================================================================
            item(key = "wallpaper_style_section") {
                SettingsSection(
                    title = "Wallpaper Styles & Blur",
                    icon = {
                        Icon(
                            painter = painterResource(R.drawable.rounded_auto_awesome_24),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        // Visual Phone Style Gallery (Full Screen, Bottom Blur Fade, Top & Bottom Blur, Less Blur, Cinema Frame, Floating Card, Material You Shapes)
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceContainer,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                                    Text(
                                        text = "Wallpaper Style",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Pick Full Screen, Top Art with Bottom Blur Fade, Top & Bottom Blur, Subtle Bottom Blur, Cinema Frame, Floating Card, or Material You Shapes",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState())
                                        .padding(horizontal = 16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    MusicWallpaperStyleMode.entries.forEach { mode ->
                                        WallpaperStyleMiniPhoneCard(
                                            styleMode = mode,
                                            selected = wallpaperSettings.styleMode == mode,
                                            settings = wallpaperSettings,
                                            sharpBitmap = activePreviewBitmap,
                                            blurredBitmap = softwareBlurredBitmap,
                                            onClick = {
                                                hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                previewStateMode = WallpaperPreviewStateMode.MUSIC_ART
                                                coroutineScope.launch {
                                                    prefsRepo.setMusicWallpaperStyleMode(mode)
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        // Blur & Fade Intensity Slider (shown for all blur-based styles)
                        AnimatedVisibility(
                            visible = wallpaperSettings.styleMode.usesBlurBackdrop,
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically()
                        ) {
                            SliderSettingsItem(
                                label = "Blur & Fade Intensity",
                                value = wallpaperSettings.blurAmount,
                                valueRange = 0.15f..1.0f,
                                steps = 0,
                                onValueChange = { amount ->
                                    coroutineScope.launch {
                                        prefsRepo.setMusicWallpaperBlurAmount(amount)
                                    }
                                },
                                valueText = { "${(it * 100).roundToInt()}%" }
                            )
                        }

                        // Position & Size Controls (shown for Floating Art Card and Material You Shapes)
                        AnimatedVisibility(
                            visible = wallpaperSettings.styleMode.usesShapeOrCardPlacement,
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically()
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Surface(
                                    color = MaterialTheme.colorScheme.surfaceContainer,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        verticalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Text(
                                            text = "Artwork Placement",
                                            style = MaterialTheme.typography.titleMedium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "Place the focal artwork at the top, middle, or bottom of the screen",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            WallpaperPositionChip(
                                                label = "Top",
                                                selected = wallpaperSettings.shapePosition == MusicWallpaperShapePosition.TOP,
                                                onClick = {
                                                    previewStateMode = WallpaperPreviewStateMode.MUSIC_ART
                                                    coroutineScope.launch {
                                                        prefsRepo.setMusicWallpaperShapePosition(MusicWallpaperShapePosition.TOP)
                                                    }
                                                },
                                                modifier = Modifier.weight(1f)
                                            )
                                            WallpaperPositionChip(
                                                label = "Middle",
                                                selected = wallpaperSettings.shapePosition == MusicWallpaperShapePosition.MIDDLE,
                                                onClick = {
                                                    previewStateMode = WallpaperPreviewStateMode.MUSIC_ART
                                                    coroutineScope.launch {
                                                        prefsRepo.setMusicWallpaperShapePosition(MusicWallpaperShapePosition.MIDDLE)
                                                    }
                                                },
                                                modifier = Modifier.weight(1f)
                                            )
                                            WallpaperPositionChip(
                                                label = "Bottom",
                                                selected = wallpaperSettings.shapePosition == MusicWallpaperShapePosition.BOTTOM,
                                                onClick = {
                                                    previewStateMode = WallpaperPreviewStateMode.MUSIC_ART
                                                    coroutineScope.launch {
                                                        prefsRepo.setMusicWallpaperShapePosition(MusicWallpaperShapePosition.BOTTOM)
                                                    }
                                                },
                                                modifier = Modifier.weight(1f)
                                            )
                                        }
                                    }
                                }

                                SliderSettingsItem(
                                    label = "Artwork Size",
                                    value = wallpaperSettings.shapeScale,
                                    valueRange = 0.60f..1.15f,
                                    steps = 0,
                                    onValueChange = { scale ->
                                        coroutineScope.launch {
                                            prefsRepo.setMusicWallpaperShapeScale(scale)
                                        }
                                    },
                                    valueText = { "${(it * 100).roundToInt()}%" }
                                )
                            }
                        }

                        // Material You Shape Gallery / Quick Picker
                        AnimatedVisibility(
                            visible = wallpaperSettings.styleMode == MusicWallpaperStyleMode.MATERIAL_YOU_SHAPE,
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically()
                        ) {
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceContainer,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 16.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                                        Text(
                                            text = "Material You Shape",
                                            style = MaterialTheme.typography.titleMedium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "Tap the shape in the preview above or pick a shape below to smoothly morph",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .horizontalScroll(rememberScrollState())
                                            .padding(horizontal = 16.dp),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        WallpaperExpressiveShapeKind.entries.forEachIndexed { idx, kind ->
                                            val isSelected = targetShapeIdx == idx
                                            ShapeMiniPreviewChip(
                                                kind = kind,
                                                selected = isSelected,
                                                onClick = {
                                                    previewStateMode = WallpaperPreviewStateMode.MUSIC_ART
                                                    hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                    animateToShapeIndex(idx)
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // =========================================================================
            // 5. SONG INFORMATION OVERLAY & SMOOTH CROSSFADE TRANSITIONS
            // =========================================================================
            item(key = "song_info_and_transitions_section") {
                SettingsSection(
                    title = "Overlay & Smooth Transitions",
                    icon = {
                        Icon(
                            painter = painterResource(R.drawable.rounded_music_note_24),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        SwitchSettingItem(
                            title = "Show Song Information",
                            subtitle = "Display the currently playing song title and artist in the top right corner of the wallpaper",
                            checked = wallpaperSettings.showSongInfo,
                            onCheckedChange = { show ->
                                coroutineScope.launch {
                                    prefsRepo.setMusicWallpaperShowSongInfo(show)
                                }
                            },
                            leadingIcon = {
                                Icon(
                                    painter = painterResource(R.drawable.rounded_music_note_24),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.secondary
                                )
                            }
                        )

                        SliderSettingsItem(
                            label = "Song Change Crossfade Duration",
                            value = wallpaperSettings.crossfadeDurationMs / 1000f,
                            valueRange = 2.0f..8.0f,
                            steps = 0,
                            onValueChange = { seconds ->
                                coroutineScope.launch {
                                    prefsRepo.setMusicWallpaperCrossfadeDurationMs((seconds * 1000f).roundToInt())
                                }
                            },
                            valueText = { String.format(java.util.Locale.US, "%.1fs", it) }
                        )

                        SliderSettingsItem(
                            label = "Wallpaper Contrast Dim",
                            value = wallpaperSettings.backgroundDim,
                            valueRange = 0.0f..0.70f,
                            steps = 0,
                            onValueChange = { dim ->
                                coroutineScope.launch {
                                    prefsRepo.setMusicWallpaperBackgroundDim(dim)
                                }
                            },
                            valueText = { "${(it * 100).roundToInt()}%" }
                        )
                    }
                }
            }

            item(key = "bottom_spacer") {
                Spacer(modifier = Modifier.height(MiniPlayerHeight + 36.dp))
            }
        }

        CollapsibleCommonTopBar(
            title = "Music Live Wallpaper",
            collapseFraction = collapseFraction,
            headerHeight = currentTopBarHeightDp,
            onBackClick = onNavigationIconClick
        )
    }
}

/**
 * Picture-only interactive wallpaper preview.
 * Shows only the wallpaper composition (blurred/full background, Material You expressive shape at
 * Top/Middle/Bottom, and top-left song information when enabled), with single-tap shape morphing.
 */
@Composable
private fun WallpaperPictureOnlyPreview(
    settings: MusicLiveWallpaperSettings,
    previewMode: WallpaperPreviewStateMode,
    activeBitmap: Bitmap?,
    softwareBlurredBitmap: Bitmap?,
    songTitle: String,
    artistName: String,
    fromShape: WallpaperExpressiveShapeKind,
    toShape: WallpaperExpressiveShapeKind,
    shapeMorphProgress: Float,
    tapRotationDeg: Float,
    onTapShape: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isFallbackIdleView = previewMode == WallpaperPreviewStateMode.FALLBACK_WALLPAPER

    val positionCenterFraction by animateFloatAsState(
        targetValue = settings.shapePosition.verticalCenterFraction(),
        animationSpec = tween(durationMillis = 680, easing = WallpaperSmoothMorphEasing),
        label = "previewPositionY"
    )

    val animatedShapeScale by animateFloatAsState(
        targetValue = settings.shapeScale,
        animationSpec = tween(durationMillis = 360, easing = FastOutSlowInEasing),
        label = "previewShapeScale"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "previewAmbientSpin")
    val continuousRotationDeg by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 36000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "previewContinuousSpin"
    )

    val effectiveRotationDeg = tapRotationDeg + if (
        settings.slowRotateWhilePlaying &&
            settings.styleMode == MusicWallpaperStyleMode.MATERIAL_YOU_SHAPE &&
            !isFallbackIdleView
    ) {
        continuousRotationDeg
    } else {
        0f
    }

    val crossfadeDuration = settings.crossfadeDurationMs.coerceIn(2000, 8000)

    Surface(
        shape = RoundedCornerShape(26.dp),
        tonalElevation = 6.dp,
        shadowElevation = 10.dp,
        modifier = modifier
            .clip(RoundedCornerShape(26.dp))
            .border(
                width = 1.5.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f),
                shape = RoundedCornerShape(26.dp)
            )
            .pointerInput(settings.styleMode, isFallbackIdleView) {
                if (settings.styleMode == MusicWallpaperStyleMode.MATERIAL_YOU_SHAPE && !isFallbackIdleView) {
                    detectTapGestures(onTap = { onTapShape() })
                }
            }
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // 1. Unified Canvas Wallpaper Layer (renders all 7 styles with authentic feathered gradients)
            Crossfade(
                targetState = Triple(activeBitmap, previewMode, settings.styleMode),
                animationSpec = tween(
                    durationMillis = if (isFallbackIdleView) crossfadeDuration else 620,
                    easing = WallpaperSmoothMorphEasing
                ),
                label = "previewWallpaperSceneCrossfade",
                modifier = Modifier.fillMaxSize()
            ) { (bmp, mode, style) ->
                val isIdle = mode == WallpaperPreviewStateMode.FALLBACK_WALLPAPER
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawIntoCanvas { composeCanvas ->
                        MusicLiveWallpaperStateHolder.renderWallpaperSceneToCanvas(
                            context = context,
                            canvas = composeCanvas.nativeCanvas,
                            w = size.width,
                            h = size.height,
                            settings = settings.copy(
                                styleMode = style,
                                shapeScale = animatedShapeScale
                            ),
                            sharpBmp = bmp,
                            blurredBmp = softwareBlurredBitmap ?: bmp,
                            sourceKey = "preview_${bmp?.hashCode() ?: 0}",
                            isFallbackIdle = isIdle,
                            songTitle = songTitle,
                            artistName = artistName,
                            fromShape = fromShape,
                            toShape = toShape,
                            shapeMorphProgress = shapeMorphProgress,
                            shapeRotationDeg = effectiveRotationDeg,
                            positionCenterY = positionCenterFraction,
                            modeMorphT = if (isIdle || style == MusicWallpaperStyleMode.FULL_SCREEN) 0f else 1f,
                            drawSongInfoOverlay = false
                        )
                    }
                }
            }

            // 2. Top-Right Song Information Overlay using bundled PixelMusic default font (ActualGoogleSansRounded)
            AnimatedVisibility(
                visible = settings.showSongInfo && !isFallbackIdleView,
                enter = fadeIn(tween(420)),
                exit = fadeOut(tween(320)),
                modifier = Modifier.align(Alignment.TopEnd)
            ) {
                Box(
                    contentAlignment = Alignment.TopEnd,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.46f),
                                    Color.Black.copy(alpha = 0.16f),
                                    Color.Transparent
                                )
                            )
                        )
                        .padding(start = 14.dp, end = 14.dp, top = 16.dp, bottom = 18.dp)
                ) {
                    AnimatedContent(
                        targetState = Pair(songTitle, artistName),
                        transitionSpec = {
                            fadeIn(tween(480)) togetherWith fadeOut(tween(360))
                        },
                        label = "previewSongInfoCrossfade"
                    ) { (title, artist) ->
                        Column(
                            horizontalAlignment = Alignment.End,
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                text = title,
                                textAlign = TextAlign.End,
                                style = TextStyle(
                                    fontFamily = ActualGoogleSansRounded,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = Color.White,
                                    shadow = Shadow(
                                        color = Color.Black.copy(alpha = 0.65f),
                                        offset = Offset(0f, 2f),
                                        blurRadius = 6f
                                    )
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = artist,
                                textAlign = TextAlign.End,
                                style = TextStyle(
                                    fontFamily = ActualGoogleSansRounded,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 10.5.sp,
                                    color = Color.White.copy(alpha = 0.84f),
                                    shadow = Shadow(
                                        color = Color.Black.copy(alpha = 0.55f),
                                        offset = Offset(0f, 1.5f),
                                        blurRadius = 4f
                                    )
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Mini phone style card modeled after the 3-card blur/fade wallpaper selector, showing the
 * live rendered style preview and a centered circular checkmark badge when selected.
 */
@Composable
private fun WallpaperStyleMiniPhoneCard(
    styleMode: MusicWallpaperStyleMode,
    selected: Boolean,
    settings: MusicLiveWallpaperSettings,
    sharpBitmap: Bitmap?,
    blurredBitmap: Bitmap?,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val shapeKind = WallpaperExpressiveShapeKind.fromIndex(settings.shapeIndex)
    val borderColor = if (selected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier
            .width(108.dp)
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(4.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(9f / 18.5f)
                .clip(RoundedCornerShape(18.dp))
                .border(
                    width = if (selected) 2.2.dp else 1.dp,
                    color = borderColor,
                    shape = RoundedCornerShape(18.dp)
                )
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawIntoCanvas { composeCanvas ->
                    MusicLiveWallpaperStateHolder.renderWallpaperSceneToCanvas(
                        context = context,
                        canvas = composeCanvas.nativeCanvas,
                        w = size.width,
                        h = size.height,
                        settings = settings.copy(
                            styleMode = styleMode,
                            blurAmount = settings.blurAmount.coerceAtLeast(0.62f)
                        ),
                        sharpBmp = sharpBitmap,
                        blurredBmp = blurredBitmap ?: sharpBitmap,
                        sourceKey = "card_${sharpBitmap?.hashCode() ?: 0}",
                        isFallbackIdle = false,
                        songTitle = "",
                        artistName = "",
                        fromShape = shapeKind,
                        toShape = shapeKind,
                        shapeMorphProgress = 1f,
                        shapeRotationDeg = 0f,
                        positionCenterY = settings.shapePosition.verticalCenterFraction(),
                        modeMorphT = if (styleMode == MusicWallpaperStyleMode.FULL_SCREEN) 0f else 1f,
                        drawSongInfoOverlay = false
                    )
                }
            }

            if (selected) {
                Surface(
                    shape = CircleShape,
                    color = Color.White,
                    shadowElevation = 6.dp,
                    modifier = Modifier.size(34.dp)
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Icon(
                            imageVector = Icons.Rounded.Check,
                            contentDescription = "Selected style",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        Text(
            text = styleMode.title,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = styleMode.subtitle,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            lineHeight = 13.sp
        )
    }
}

private fun createDefaultExpressiveBitmap(
    primaryContainerArgb: Int,
    primaryArgb: Int,
    tertiaryArgb: Int
): Bitmap {
    val w = 360
    val h = 640
    val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bmp)
    val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)
    paint.shader = android.graphics.LinearGradient(
        0f, 0f, w.toFloat(), h.toFloat(),
        intArrayOf(primaryContainerArgb, primaryArgb, tertiaryArgb, android.graphics.Color.rgb(245, 160, 145)),
        floatArrayOf(0f, 0.38f, 0.72f, 1f),
        android.graphics.Shader.TileMode.CLAMP
    )
    canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), paint)
    return bmp
}

@Composable
private fun ShapeMiniPreviewChip(
    kind: WallpaperExpressiveShapeKind,
    selected: Boolean,
    onClick: () -> Unit
) {
    val containerColor = if (selected) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceContainerHighest
    }
    val contentColor = if (selected) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    val shapeColor = if (selected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f)
    }

    Surface(
        color = containerColor,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Canvas(modifier = Modifier.size(34.dp)) {
                val cx = size.width * 0.5f
                val cy = size.height * 0.5f
                val rBase = size.minDimension * 0.45f
                val count = 96
                val twoPi = 2.0 * PI
                val path = Path().apply {
                    for (i in 0 until count) {
                        val th = (twoPi * i) / count
                        val r = rBase * kind.normalizedRadiusAt(th)
                        val x = cx + r * cos(th).toFloat()
                        val y = cy + r * sin(th).toFloat()
                        if (i == 0) moveTo(x, y) else lineTo(x, y)
                    }
                    close()
                }
                drawPath(path = path, color = shapeColor.copy(alpha = if (selected) 0.28f else 0.16f))
                drawPath(path = path, color = shapeColor, style = Stroke(width = 2.dp.toPx()))
            }

            Text(
                text = kind.shortLabel,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = contentColor
            )
        }
    }
}

@Composable
private fun PreviewStateTab(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bg = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent
    val fg = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp, horizontal = 10.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = fg
        )
    }
}

@Composable
private fun WallpaperOptionCard(
    title: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val containerColor = if (selected) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceContainerHighest
    }
    val titleColor = if (selected) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    val subtitleColor = if (selected) {
        MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Surface(
        color = containerColor,
        shape = RoundedCornerShape(14.dp),
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
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

@Composable
private fun WallpaperPositionChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val containerColor = if (selected) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceContainerHighest
    }
    val textColor = if (selected) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    Surface(
        color = containerColor,
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 10.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = textColor
            )
        }
    }
}
