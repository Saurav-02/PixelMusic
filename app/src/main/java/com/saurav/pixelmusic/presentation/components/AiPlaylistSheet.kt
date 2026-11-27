@file:OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalMaterial3ExpressiveApi::class
)

package com.saurav.pixelmusic.presentation.components

import androidx.activity.compose.BackHandler
import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
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
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsAnimationCompat
import androidx.core.view.WindowInsetsCompat
import com.saurav.pixelmusic.R
import com.saurav.pixelmusic.presentation.viewmodel.LastFmTrack
import com.saurav.pixelmusic.ui.theme.GoogleSansRounded
import kotlinx.coroutines.delay
import racra.compose.smooth_corner_rect_library.AbsoluteSmoothCornerShape

private val LASTFM_DAILY_MIX_HEADLINES = listOf(
    "Updating your Daily Mix…",
    "Syncing with Last.fm…",
    "Reading your Last.fm scrobbles…",
    "Analyzing your listening history…",
    "Matching your Last.fm taste…",
    "Resolving tracks on YouTube Music…",
    "Refreshing your Daily Mix queue…"
)

private val LASTFM_DAILY_MIX_PHASES = listOf(
    "Connecting to your Last.fm profile & scrobble history…",
    "Curating tracks from your Last.fm listening patterns…",
    "Matching playable tracks on YouTube Music…",
    "Updating your Daily Mix songs directly…"
)

@Composable
fun AiPlaylistSheet(
    onDismiss: () -> Unit,
    onGenerateClick: (
        mode: String,
        count: Int,
        timePeriod: String,
        seedTrackName: String,
        seedArtistName: String,
        seedArtistInput: String,
        tagInput: String
    ) -> Unit,
    isGenerating: Boolean,
    isSuccess: Boolean,
    status: String?,
    error: String?,
    onRetry: () -> Unit,
    topTracks: List<LastFmTrack> = emptyList(),
    topArtists: List<String> = emptyList(),
    searchTrackResults: List<LastFmTrack> = emptyList(),
    searchArtistResults: List<String> = emptyList(),
    isSearchingSeeds: Boolean = false,
    isSearchingTopSeeds: Boolean = false,
    onSearchTrack: (String, String) -> Unit = { _, _ -> },
    onSearchArtist: (String) -> Unit = {},
    onLoadTopTracks: () -> Unit = {},
    onLoadTopArtists: () -> Unit = {}
) {
    val heroArtworkUris = rememberAiHeroArtworkUris()

    // Auto-dismiss right as the full-screen Daily Mix update portal animation finishes dissolving
    LaunchedEffect(isSuccess) {
        if (isSuccess) {
            delay(1380L)
            onDismiss()
        }
    }

    if (isGenerating || isSuccess) {
        AiPlaylistGeneratingFullScreenPage(
            isSuccess = isSuccess,
            status = status,
            error = error,
            heroArtworkUris = heroArtworkUris,
            customHeadlines = LASTFM_DAILY_MIX_HEADLINES,
            customPhases = LASTFM_DAILY_MIX_PHASES,
            successHeadline = "Daily Mix Updated!",
            successSubtitle = "Your Last.fm mix is ready in Daily Mix…",
            preferLiveStatus = true,
            onDismiss = onDismiss
        )
    } else {
        val sheetState = rememberBottomSheetState(
            initialValue = SheetValue.Hidden,
            enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded)
        )

        SmoothModalBottomSheet(
            sheetState = sheetState,
            onDismissRequest = onDismiss,
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            contentWindowInsets = { WindowInsets.navigationBars },
            dragHandle = {
                BottomSheetDefaults.DragHandle(
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
                )
            },
            shape = AbsoluteSmoothCornerShape(
                cornerRadiusTL = 28.dp,
                smoothnessAsPercentTL = 60,
                cornerRadiusTR = 28.dp,
                smoothnessAsPercentTR = 60
            )
        ) {
            DailyMixLastFmSheetContent(
                error = error,
                heroArtworkUris = heroArtworkUris,
                onDismiss = onDismiss,
                onRetry = onRetry,
                onGenerateClick = onGenerateClick,
                topTracks = topTracks,
                topArtists = topArtists,
                searchTrackResults = searchTrackResults,
                searchArtistResults = searchArtistResults,
                isSearchingSeeds = isSearchingSeeds,
                isSearchingTopSeeds = isSearchingTopSeeds,
                onSearchTrack = onSearchTrack,
                onSearchArtist = onSearchArtist,
                onLoadTopTracks = onLoadTopTracks,
                onLoadTopArtists = onLoadTopArtists
            )
        }
    }
}

private data class DailyMixModeItem(
    val id: String,
    val label: String,
    @DrawableRes val iconRes: Int,
    val baseShape: AiExpressiveShapeType,
    val morphedShape: AiExpressiveShapeType
)

@Composable
private fun DailyMixLastFmSheetContent(
    error: String?,
    heroArtworkUris: List<String>,
    onDismiss: () -> Unit,
    onRetry: () -> Unit,
    onGenerateClick: (
        mode: String,
        count: Int,
        timePeriod: String,
        seedTrackName: String,
        seedArtistName: String,
        seedArtistInput: String,
        tagInput: String
    ) -> Unit,
    topTracks: List<LastFmTrack>,
    topArtists: List<String>,
    searchTrackResults: List<LastFmTrack>,
    searchArtistResults: List<String>,
    isSearchingSeeds: Boolean,
    isSearchingTopSeeds: Boolean,
    onSearchTrack: (String, String) -> Unit,
    onSearchArtist: (String) -> Unit,
    onLoadTopTracks: () -> Unit,
    onLoadTopArtists: () -> Unit
) {
    var selectedMode by rememberSaveable { mutableStateOf("recommendations") }
    var trackCountText by rememberSaveable { mutableStateOf("30") }
    var seedTrackName by rememberSaveable { mutableStateOf("") }
    var seedArtistName by rememberSaveable { mutableStateOf("") }
    var seedArtistInput by rememberSaveable { mutableStateOf("") }
    var tagInput by rememberSaveable { mutableStateOf("") }
    var timePeriod by rememberSaveable { mutableStateOf("overall") }
    var isRefining by rememberSaveable { mutableStateOf(false) }

    val ambientScheme = MaterialTheme.colorScheme
    val ambientDark = ambientScheme.surface.luminance() < 0.5f
    val ambientPick = remember(selectedMode) { (selectedMode.hashCode() and 0x7fffffff) % 3 }
    val ambientBase = listOf(
        if (ambientDark) ambientScheme.primary else ambientScheme.primaryContainer,
        if (ambientDark) ambientScheme.tertiary else ambientScheme.tertiaryContainer,
        if (ambientDark) ambientScheme.secondary else ambientScheme.secondaryContainer
    )
    val ambientA by animateColorAsState(targetValue = ambientBase[ambientPick], animationSpec = tween(800), label = "dm_ambient_a")
    val ambientB by animateColorAsState(targetValue = ambientBase[(ambientPick + 1) % 3], animationSpec = tween(800), label = "dm_ambient_b")
    val ambientC by animateColorAsState(targetValue = ambientBase[(ambientPick + 2) % 3], animationSpec = tween(800), label = "dm_ambient_c")
    val ambientTransition = rememberInfiniteTransition(label = "dm_sheet_ambient")
    val ambientPhaseState = ambientTransition.animateFloat(
        initialValue = 0f,
        targetValue = 6.2831855f,
        animationSpec = infiniteRepeatable(
            animation = tween(16000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "dm_ambient_phase"
    )
    val ambientEnergy by animateFloatAsState(
        targetValue = 0.85f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessVeryLow),
        label = "dm_ambient_energy"
    )

    var heroFrom by remember { mutableIntStateOf(0) }
    var heroTo by remember { mutableIntStateOf(0) }
    val heroMorph = remember { Animatable(1f) }
    val stateSignature = remember(selectedMode, timePeriod, trackCountText, seedTrackName, seedArtistInput, tagInput) {
        ((selectedMode.hashCode() * 31 + timePeriod.hashCode() * 7 + trackCountText.hashCode() + seedTrackName.hashCode() + seedArtistInput.hashCode() + tagInput.hashCode()) and 0x7fffffff) % 11 + 1
    }
    LaunchedEffect(stateSignature) {
        if (stateSignature != heroTo) {
            heroFrom = heroTo
            heroTo = stateSignature
            heroMorph.snapTo(0f)
            heroMorph.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 820, easing = AiSmoothMorphEasing)
            )
        }
    }
    val reactionKick = remember { Animatable(0f) }
    LaunchedEffect(stateSignature) {
        reactionKick.animateTo(
            targetValue = 0.40f,
            animationSpec = tween(durationMillis = 200, easing = AiSmoothMorphEasing)
        )
        reactionKick.animateTo(
            targetValue = 0f,
            animationSpec = tween(durationMillis = 620, easing = AiSmoothMorphEasing)
        )
    }

    val view = LocalView.current
    DisposableEffect(view) {
        var p = view.parent
        while (p != null) {
            if (p is DialogWindowProvider) {
                p.window.setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING)
                break
            }
            p = p.parent
        }
        val parentView = view.parent as? android.view.View
        if (parentView != null) {
            ViewCompat.setOnApplyWindowInsetsListener(parentView) { _, insets ->
                WindowInsetsCompat.Builder(insets)
                    .setInsets(WindowInsetsCompat.Type.ime(), Insets.NONE)
                    .build()
            }
            ViewCompat.setWindowInsetsAnimationCallback(
                parentView,
                object : WindowInsetsAnimationCompat.Callback(DISPATCH_MODE_STOP) {
                    override fun onProgress(
                        insets: WindowInsetsCompat,
                        runningAnimations: MutableList<WindowInsetsAnimationCompat>
                    ): WindowInsetsCompat = insets
                }
            )
        }
        onDispose {
            if (parentView != null) {
                ViewCompat.setOnApplyWindowInsetsListener(parentView, null)
                ViewCompat.setWindowInsetsAnimationCallback(parentView, null)
            }
        }
    }

    BackHandler(enabled = isRefining) {
        isRefining = false
    }

    val refineProgress by animateFloatAsState(
        targetValue = if (isRefining) 1f else 0f,
        animationSpec = tween(durationMillis = 280, easing = AiSmoothMorphEasing),
        label = "dm_sheet_refine_progress"
    )
    val showMainPage = !isRefining || refineProgress < 0.999f
    val showRefinePage = isRefining || refineProgress > 0.001f
    val mainScrollState = rememberScrollState()
    var refineReady by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(500L)
        refineReady = true
    }

    val isInputReady = when (selectedMode) {
        "similar-tracks" -> seedTrackName.isNotBlank() && seedArtistName.isNotBlank()
        "similar-artists" -> seedArtistInput.isNotBlank()
        "tag" -> tagInput.isNotBlank()
        else -> true
    }

    Layout(
        modifier = Modifier
            .fillMaxWidth()
            .drawWithCache {
                val brushA = Brush.radialGradient(listOf(ambientA.copy(alpha = 1f), Color.Transparent), Offset.Zero, 1f)
                val brushB = Brush.radialGradient(listOf(ambientB.copy(alpha = 1f), Color.Transparent), Offset.Zero, 1f)
                val brushC = Brush.radialGradient(listOf(ambientC.copy(alpha = 1f), Color.Transparent), Offset.Zero, 1f)
                onDrawBehind {
                    val t = ambientPhaseState.value
                    val e = ambientEnergy
                    val k = reactionKick.value.coerceAtLeast(0f)
                    val w = size.width
                    val h = size.height
                    fun blob(brush: Brush, cx: Float, cy: Float, radius: Float, alpha: Float) {
                        withTransform({
                            translate(cx, cy)
                            scale(radius, radius, Offset.Zero)
                        }) {
                            drawCircle(
                                brush = brush,
                                radius = 1f,
                                center = Offset.Zero,
                                alpha = alpha.coerceIn(0f, 1f)
                            )
                        }
                    }
                    blob(
                        brushA,
                        w * (0.15f + 0.10f * kotlin.math.cos(t)),
                        h * (0.16f + 0.05f * kotlin.math.sin(t)),
                        w * (0.75f + 0.18f * k),
                        0.30f * e + 0.18f * k
                    )
                    blob(
                        brushB,
                        w * (0.88f + 0.08f * kotlin.math.sin(t * 1f + 1.7f)),
                        h * (0.42f + 0.07f * kotlin.math.cos(t + 2.3f)),
                        w * (0.65f + 0.15f * k),
                        0.24f * e + 0.14f * k
                    )
                    blob(
                        brushC,
                        w * (0.45f + 0.14f * kotlin.math.cos(t + 4.1f)),
                        h * (0.88f + 0.05f * kotlin.math.sin(t + 0.6f)),
                        w * (0.80f + 0.12f * k),
                        0.22f * e + 0.12f * k
                    )
                }
            },
        content = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        val p = refineProgress.coerceIn(0f, 1f)
                        alpha = 1f - p
                        val s = 1f - 0.035f * p
                        scaleX = s
                        scaleY = s
                        translationX = -18.dp.toPx() * p
                    }
                    .verticalScroll(mainScrollState)
                    .padding(bottom = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilledTonalIconButton(
                        onClick = onDismiss,
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            contentColor = MaterialTheme.colorScheme.onSurface
                        )
                    ) {
                        Icon(Icons.Rounded.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer {
                            val kick = reactionKick.value.coerceAtLeast(0f)
                            val s = 1f + 0.035f * kick
                            scaleX = s
                            scaleY = s
                            translationY = -3.dp.toPx() * kick
                        }
                ) {
                    AiLibraryIndicatorsAnimation(
                        isGenerating = false,
                        isSuccess = false,
                        heroArtworkUris = heroArtworkUris,
                        morphFrom = heroFrom,
                        morphTo = heroTo,
                        morphProgress = { heroMorph.value }
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                DailyMixLastFmMainView(
                    selectedMode = selectedMode,
                    onModeChange = { selectedMode = it },
                    trackCountText = trackCountText,
                    timePeriod = timePeriod,
                    onTimePeriodChange = { timePeriod = it },
                    seedTrackName = seedTrackName,
                    onSeedTrackNameChange = { seedTrackName = it },
                    seedArtistName = seedArtistName,
                    onSeedArtistNameChange = { seedArtistName = it },
                    seedArtistInput = seedArtistInput,
                    onSeedArtistInputChange = { seedArtistInput = it },
                    tagInput = tagInput,
                    onTagInputChange = { tagInput = it },
                    isInputReady = isInputReady,
                    error = error,
                    onRetry = onRetry,
                    onOpenRefine = { isRefining = true },
                    onGenerate = {
                        val count = (trackCountText.toIntOrNull() ?: 30).coerceIn(5, 100)
                        onGenerateClick(
                            selectedMode,
                            count,
                            timePeriod,
                            seedTrackName,
                            seedArtistName,
                            seedArtistInput,
                            tagInput
                        )
                    },
                    topTracks = topTracks,
                    topArtists = topArtists,
                    searchTrackResults = searchTrackResults,
                    searchArtistResults = searchArtistResults,
                    isSearchingSeeds = isSearchingSeeds,
                    isSearchingTopSeeds = isSearchingTopSeeds,
                    onSearchTrack = onSearchTrack,
                    onSearchArtist = onSearchArtist,
                    onLoadTopTracks = onLoadTopTracks,
                    onLoadTopArtists = onLoadTopArtists
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        val p = refineProgress.coerceIn(0f, 1f)
                        alpha = p
                        val s = 0.965f + 0.035f * p
                        scaleX = s
                        scaleY = s
                        translationX = 18.dp.toPx() * (1f - p)
                    }
            ) {
                if (refineReady || isRefining) {
                    DailyMixLastFmRefineView(
                        selectedMode = selectedMode,
                        trackCountText = trackCountText,
                        onTrackCountChange = { trackCountText = it },
                        timePeriod = timePeriod,
                        onTimePeriodChange = { timePeriod = it },
                        onBack = { isRefining = false },
                        onDismiss = onDismiss
                    )
                }
            }
        }
    ) { measurables, constraints ->
        val looseConstraints = constraints.copy(minHeight = 0)
        val mainPlaceable = measurables[0].measure(looseConstraints)
        val refinePlaceable = measurables[1].measure(
            looseConstraints.copy(
                minWidth = mainPlaceable.width,
                maxWidth = mainPlaceable.width,
                maxHeight = mainPlaceable.height.coerceAtLeast(1)
            )
        )
        layout(mainPlaceable.width, mainPlaceable.height) {
            if (showMainPage) {
                mainPlaceable.placeRelative(0, 0)
            }
            if (showRefinePage) {
                refinePlaceable.placeRelative(0, 0)
            }
        }
    }
}

@Composable
private fun DailyMixLastFmMainView(
    selectedMode: String,
    onModeChange: (String) -> Unit,
    trackCountText: String,
    timePeriod: String,
    onTimePeriodChange: (String) -> Unit,
    seedTrackName: String,
    onSeedTrackNameChange: (String) -> Unit,
    seedArtistName: String,
    onSeedArtistNameChange: (String) -> Unit,
    seedArtistInput: String,
    onSeedArtistInputChange: (String) -> Unit,
    tagInput: String,
    onTagInputChange: (String) -> Unit,
    isInputReady: Boolean,
    error: String?,
    onRetry: () -> Unit,
    onOpenRefine: () -> Unit,
    onGenerate: () -> Unit,
    topTracks: List<LastFmTrack>,
    topArtists: List<String>,
    searchTrackResults: List<LastFmTrack>,
    searchArtistResults: List<String>,
    isSearchingSeeds: Boolean,
    isSearchingTopSeeds: Boolean,
    onSearchTrack: (String, String) -> Unit,
    onSearchArtist: (String) -> Unit,
    onLoadTopTracks: () -> Unit,
    onLoadTopArtists: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val colors = MaterialTheme.colorScheme
    val isDark = colors.surface.luminance() < 0.5f

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Daily Mix",
            style = MaterialTheme.typography.headlineMedium.copy(
                fontFamily = GoogleSansRounded,
                fontWeight = FontWeight.Bold,
                fontSize = 26.sp
            ),
            textAlign = TextAlign.Center,
            color = colors.onSurface,
            modifier = Modifier.padding(horizontal = 20.dp)
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "Update your Daily Mix from your Last.fm history",
            style = MaterialTheme.typography.bodyMedium,
            color = colors.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 20.dp)
        )

        Spacer(modifier = Modifier.height(2.dp))

        DailyMixModeQuickSelectRow(
            selectedMode = selectedMode,
            onModeClick = { mode ->
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onModeChange(mode)
            }
        )

        // Conditional Time Period card when Top Tracks or My Library is selected
        AnimatedVisibility(
            visible = selectedMode == "top" || selectedMode == "library",
            enter = expandVertically(tween(460, easing = AiSmoothMorphEasing)) + fadeIn(tween(380)),
            exit = shrinkVertically(tween(340, easing = AiSmoothMorphEasing)) + fadeOut(tween(240))
        ) {
            Surface(
                shape = remember { AbsoluteSmoothCornerShape(26.dp, 60) },
                color = colors.surfaceContainer,
                shadowElevation = 0.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "Time period",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontFamily = GoogleSansRounded,
                            fontWeight = FontWeight.Bold
                        ),
                        color = colors.onSurface
                    )
                    DailyMixTimePeriodGrid(
                        selectedPeriod = timePeriod,
                        onPeriodSelect = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onTimePeriodChange(it)
                        }
                    )
                }
            }
        }

        // Expressive Board Cards for Similar Tracks, Similar Artists, and Tag
        AnimatedVisibility(
            visible = selectedMode == "similar-tracks" || selectedMode == "similar-artists" || selectedMode == "tag",
            enter = expandVertically(tween(480, easing = AiSmoothMorphEasing)) + fadeIn(tween(400)),
            exit = shrinkVertically(tween(360, easing = AiSmoothMorphEasing)) + fadeOut(tween(260))
        ) {
            Surface(
                shape = remember { AbsoluteSmoothCornerShape(28.dp, 60) },
                color = colors.surfaceContainer,
                shadowElevation = 0.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    when (selectedMode) {
                        "similar-tracks" -> {
                            ExpressiveSeedInputCard(
                                value = seedTrackName,
                                onValueChange = onSeedTrackNameChange,
                                label = "Seed Track",
                                placeholder = "e.g. Blinding Lights",
                                iconRes = R.drawable.rounded_music_note_24,
                                badgeShape = AiExpressiveShapeType.COOKIE,
                                morphedBadgeShape = AiExpressiveShapeType.FOUR_HEAD_COOKIE,
                                accentColor = colors.primary,
                                accentContainer = colors.primaryContainer,
                                onSearch = {
                                    keyboardController?.hide()
                                    onSearchTrack(seedTrackName, seedArtistName)
                                }
                            )

                            ExpressiveSeedInputCard(
                                value = seedArtistName,
                                onValueChange = onSeedArtistNameChange,
                                label = "Seed Artist",
                                placeholder = "e.g. The Weeknd",
                                iconRes = R.drawable.rounded_people_24,
                                badgeShape = AiExpressiveShapeType.FOUR_HEAD_COOKIE,
                                morphedBadgeShape = AiExpressiveShapeType.SIX_COOKIE,
                                accentColor = colors.tertiary,
                                accentContainer = colors.tertiaryContainer,
                                onSearch = null
                            )

                            ExpressiveTopSeedTriggerCard(
                                text = "Pick From My Top Tracks",
                                subtitle = "Choose a track from your Last.fm top listens",
                                iconRes = R.drawable.rounded_library_music_24,
                                isLoading = isSearchingSeeds || isSearchingTopSeeds,
                                accentColor = colors.primary,
                                accentContainer = colors.primaryContainer,
                                onAccentContainer = colors.onPrimaryContainer,
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    keyboardController?.hide()
                                    onLoadTopTracks()
                                }
                            )

                            if (searchTrackResults.isNotEmpty()) {
                                ExpressiveSeedBoardSection(
                                    title = "Search Results",
                                    count = searchTrackResults.size,
                                    accentColor = colors.primary
                                ) {
                                    searchTrackResults.take(6).forEachIndexed { idx, track ->
                                        val selected = seedTrackName.equals(track.name, ignoreCase = true) &&
                                            seedArtistName.equals(track.artist, ignoreCase = true)
                                        ExpressiveSeedBoardItem(
                                            index = idx,
                                            title = track.name,
                                            subtitle = track.artist,
                                            iconRes = R.drawable.rounded_music_note_24,
                                            isSelected = selected,
                                            accentColor = colors.primary,
                                            accentContainer = colors.primaryContainer,
                                            onAccent = colors.onPrimary,
                                            onAccentContainer = colors.onPrimaryContainer,
                                            onClick = {
                                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                onSeedTrackNameChange(track.name)
                                                onSeedArtistNameChange(track.artist)
                                            }
                                        )
                                    }
                                }
                            }

                            if (topTracks.isNotEmpty()) {
                                ExpressiveSeedBoardSection(
                                    title = "My Top Tracks",
                                    count = topTracks.size,
                                    accentColor = colors.primary
                                ) {
                                    topTracks.take(6).forEachIndexed { idx, track ->
                                        val selected = seedTrackName.equals(track.name, ignoreCase = true) &&
                                            seedArtistName.equals(track.artist, ignoreCase = true)
                                        ExpressiveSeedBoardItem(
                                            index = idx,
                                            title = track.name,
                                            subtitle = track.artist,
                                            iconRes = R.drawable.rounded_music_note_24,
                                            isSelected = selected,
                                            accentColor = colors.primary,
                                            accentContainer = colors.primaryContainer,
                                            onAccent = colors.onPrimary,
                                            onAccentContainer = colors.onPrimaryContainer,
                                            onClick = {
                                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                onSeedTrackNameChange(track.name)
                                                onSeedArtistNameChange(track.artist)
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        "similar-artists" -> {
                            ExpressiveSeedInputCard(
                                value = seedArtistInput,
                                onValueChange = onSeedArtistInputChange,
                                label = "Seed Artist",
                                placeholder = "e.g. Coldplay, Arijit Singh…",
                                iconRes = R.drawable.rounded_people_24,
                                badgeShape = AiExpressiveShapeType.SIX_COOKIE,
                                morphedBadgeShape = AiExpressiveShapeType.COOKIE,
                                accentColor = colors.primary,
                                accentContainer = colors.primaryContainer,
                                onSearch = {
                                    keyboardController?.hide()
                                    onSearchArtist(seedArtistInput)
                                }
                            )

                            ExpressiveTopSeedTriggerCard(
                                text = "Pick From My Top Artists",
                                subtitle = "Choose an artist from your Last.fm history",
                                iconRes = R.drawable.rounded_people_24,
                                isLoading = isSearchingSeeds || isSearchingTopSeeds,
                                accentColor = colors.primary,
                                accentContainer = colors.primaryContainer,
                                onAccentContainer = colors.onPrimaryContainer,
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    keyboardController?.hide()
                                    onLoadTopArtists()
                                }
                            )

                            if (searchArtistResults.isNotEmpty()) {
                                ExpressiveSeedBoardSection(
                                    title = "Search Results",
                                    count = searchArtistResults.size,
                                    accentColor = colors.primary
                                ) {
                                    searchArtistResults.take(6).forEachIndexed { idx, artist ->
                                        val selected = seedArtistInput.equals(artist, ignoreCase = true)
                                        ExpressiveSeedBoardItem(
                                            index = idx,
                                            title = artist,
                                            subtitle = "Last.fm Artist",
                                            iconRes = R.drawable.rounded_people_24,
                                            isSelected = selected,
                                            accentColor = colors.primary,
                                            accentContainer = colors.primaryContainer,
                                            onAccent = colors.onPrimary,
                                            onAccentContainer = colors.onPrimaryContainer,
                                            onClick = {
                                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                onSeedArtistInputChange(artist)
                                            }
                                        )
                                    }
                                }
                            }

                            if (topArtists.isNotEmpty()) {
                                ExpressiveSeedBoardSection(
                                    title = "My Top Artists",
                                    count = topArtists.size,
                                    accentColor = colors.primary
                                ) {
                                    topArtists.take(6).forEachIndexed { idx, artist ->
                                        val selected = seedArtistInput.equals(artist, ignoreCase = true)
                                        ExpressiveSeedBoardItem(
                                            index = idx,
                                            title = artist,
                                            subtitle = "Top Last.fm Artist",
                                            iconRes = R.drawable.rounded_people_24,
                                            isSelected = selected,
                                            accentColor = colors.primary,
                                            accentContainer = colors.primaryContainer,
                                            onAccent = colors.onPrimary,
                                            onAccentContainer = colors.onPrimaryContainer,
                                            onClick = {
                                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                onSeedArtistInputChange(artist)
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        "tag" -> {
                            ExpressiveSeedInputCard(
                                value = tagInput,
                                onValueChange = onTagInputChange,
                                label = "Genre or Tag",
                                placeholder = "e.g. rock, lofi, jazz, indie…",
                                iconRes = R.drawable.rounded_sell_24,
                                badgeShape = AiExpressiveShapeType.SUNNY_COOKIE,
                                morphedBadgeShape = AiExpressiveShapeType.FOUR_HEAD_COOKIE,
                                accentColor = colors.primary,
                                accentContainer = colors.primaryContainer,
                                onSearch = null
                            )

                            val quickTags = remember {
                                listOf(
                                    listOf("lofi", "rock", "pop", "jazz"),
                                    listOf("indie", "electronic", "hip-hop", "ambient")
                                )
                            }
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                quickTags.forEach { rowTags ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        rowTags.forEach { tag ->
                                            DailyMixMorphChip(
                                                label = tag,
                                                selected = tagInput.equals(tag, ignoreCase = true),
                                                onClick = {
                                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                    onTagInputChange(tag)
                                                },
                                                modifier = Modifier.weight(1f)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        if (!error.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(8.dp))
            Surface(
                color = colors.errorContainer,
                shape = AbsoluteSmoothCornerShape(18.dp, 60),
                onClick = onRetry,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = error,
                        color = colors.onErrorContainer,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(painter = painterResource(R.drawable.rounded_restore_24), contentDescription = null, tint = colors.error, modifier = Modifier.size(16.dp))
                        Text("Tap to Retry", color = colors.error, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Refine Daily Mix Card
        val refineCardShape = remember { AbsoluteSmoothCornerShape(22.dp, 60) }
        val displayCount = trackCountText.ifBlank { "30" }
        val periodLabel = when (timePeriod) {
            "overall" -> "All Time"
            "12month" -> "Yearly"
            "6month" -> "6 Months"
            "3month" -> "3 Months"
            "1month" -> "Monthly"
            "7day" -> "Weekly"
            else -> "All Time"
        }
        Surface(
            shape = refineCardShape,
            color = colors.surfaceContainer,
            shadowElevation = 0.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .clip(refineCardShape)
                .clickable(onClick = onOpenRefine)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = colors.primaryContainer,
                    shape = AbsoluteSmoothCornerShape(12.dp, 60)
                ) {
                    Box(modifier = Modifier.padding(8.dp)) {
                        Icon(
                            painter = painterResource(R.drawable.rounded_tune_24),
                            contentDescription = null,
                            tint = colors.onPrimaryContainer,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Refine Daily Mix",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = colors.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RefineBadge(text = "Mix size: $displayCount songs")
                        if (selectedMode == "top" || selectedMode == "library") {
                            RefineBadge(text = "Period: $periodLabel")
                        }
                    }
                }

                Icon(
                    painter = painterResource(R.drawable.rounded_chevron_right_24),
                    contentDescription = "Refine Daily Mix",
                    tint = colors.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Update Daily Mix Expressive Action Button
        val btnInteraction = remember { MutableInteractionSource() }
        val isBtnPressed by btnInteraction.collectIsPressedAsState()
        val btnScale by animateFloatAsState(
            targetValue = if (isBtnPressed) 0.94f else 1f,
            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
            label = "dm_gen_scale"
        )
        val btnCorner by animateDpAsState(
            targetValue = if (isBtnPressed) 18.dp else 28.dp,
            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
            label = "dm_gen_corner"
        )
        val btnContainer by animateColorAsState(
            targetValue = if (isInputReady) colors.primary else colors.surfaceContainerHighest,
            animationSpec = tween(220),
            label = "dm_gen_container"
        )
        val btnContent by animateColorAsState(
            targetValue = if (isInputReady) colors.onPrimary else colors.onSurfaceVariant.copy(alpha = 0.45f),
            animationSpec = tween(220),
            label = "dm_gen_content"
        )

        Surface(
            onClick = {
                if (isInputReady) {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onGenerate()
                }
            },
            enabled = isInputReady,
            interactionSource = btnInteraction,
            shape = AbsoluteSmoothCornerShape(btnCorner, 60),
            color = btnContainer,
            contentColor = btnContent,
            shadowElevation = 0.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 6.dp)
                .height(56.dp)
                .graphicsLayer {
                    scaleX = btnScale
                    scaleY = btnScale
                }
                .aiSoftSurfaceGlow(
                    color = if (isInputReady) colors.primary else Color.Transparent,
                    cornerRadius = btnCorner,
                    spread = 10.dp,
                    offsetY = 3.dp,
                    maxAlpha = if (isInputReady) (if (isDark) 0.28f else 0.18f) else 0f
                )
        ) {
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = painterResource(R.drawable.rounded_instant_mix_24),
                    contentDescription = null,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Update Daily Mix",
                    style = MaterialTheme.typography.titleMedium,
                    fontFamily = GoogleSansRounded,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun DailyMixLastFmRefineView(
    selectedMode: String,
    trackCountText: String,
    onTrackCountChange: (String) -> Unit,
    timePeriod: String,
    onTimePeriodChange: (String) -> Unit,
    onBack: () -> Unit,
    onDismiss: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val colors = MaterialTheme.colorScheme
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp)
            .padding(bottom = 32.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilledTonalIconButton(
                onClick = onBack,
                colors = IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = colors.surfaceContainerHigh,
                    contentColor = colors.onSurface
                )
            ) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
            }

            Text(
                text = "Refine Daily Mix",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = colors.onSurface
            )

            FilledTonalIconButton(
                onClick = onDismiss,
                colors = IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = colors.surfaceContainerHigh,
                    contentColor = colors.onSurface
                )
            ) {
                Icon(Icons.Rounded.Close, contentDescription = "Close")
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Daily Mix size card — uses DailyMixMorphChip (zero overflowing shadow clipping!)
        Surface(
            shape = remember { AbsoluteSmoothCornerShape(24.dp, 60) },
            color = colors.surfaceContainer,
            shadowElevation = 0.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Daily Mix size",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontFamily = GoogleSansRounded,
                        fontWeight = FontWeight.Bold
                    ),
                    color = colors.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "How many songs to load into your Daily Mix",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(14.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("15", "25", "30", "40", "50").forEach { countOption ->
                        DailyMixMorphChip(
                            label = countOption,
                            selected = trackCountText == countOption,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onTrackCountChange(countOption)
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))

                // Borderless Material You Expressive tonal input pill (zero wireframe outline!)
                val countInteraction = remember { MutableInteractionSource() }
                val isCountFocused by countInteraction.collectIsFocusedAsState()
                val countFieldBg by animateColorAsState(
                    targetValue = if (isCountFocused) colors.surfaceContainerHighest else colors.surfaceContainerHigh,
                    animationSpec = tween(220),
                    label = "dm_count_field_bg"
                )
                Surface(
                    shape = remember { AbsoluteSmoothCornerShape(18.dp, 60) },
                    color = countFieldBg,
                    shadowElevation = 0.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Custom song count",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.primary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            BasicTextField(
                                value = trackCountText,
                                onValueChange = { onTrackCountChange(it.filter { ch -> ch.isDigit() }.take(3)) },
                                textStyle = MaterialTheme.typography.titleMedium.copy(
                                    fontFamily = GoogleSansRounded,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.onSurface
                                ),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                cursorBrush = SolidColor(colors.primary),
                                interactionSource = countInteraction,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        Surface(
                            shape = AbsoluteSmoothCornerShape(10.dp, 60),
                            color = colors.primaryContainer
                        ) {
                            Text(
                                text = "songs",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = colors.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Time period card — 2x3 grid so all 6 periods are visible with zero horizontal scroll clipping!
        Surface(
            shape = remember { AbsoluteSmoothCornerShape(24.dp, 60) },
            color = colors.surfaceContainer,
            shadowElevation = 0.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Time period",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontFamily = GoogleSansRounded,
                        fontWeight = FontWeight.Bold
                    ),
                    color = colors.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Used when generating from Top Tracks or My Library",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(14.dp))
                DailyMixTimePeriodGrid(
                    selectedPeriod = timePeriod,
                    onPeriodSelect = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onTimePeriodChange(it)
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        val doneInteractionSource = remember { MutableInteractionSource() }
        val isDonePressed by doneInteractionSource.collectIsPressedAsState()
        val doneScale by animateFloatAsState(
            targetValue = if (isDonePressed) 0.95f else 1f,
            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
            label = "dm_done_scale"
        )
        val doneCorner by animateDpAsState(
            targetValue = if (isDonePressed) 16.dp else 26.dp,
            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
            label = "dm_done_corner"
        )

        Surface(
            onClick = onBack,
            interactionSource = doneInteractionSource,
            shape = AbsoluteSmoothCornerShape(doneCorner, 60),
            color = colors.primary,
            contentColor = colors.onPrimary,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .graphicsLayer {
                    scaleX = doneScale
                    scaleY = doneScale
                }
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = "Done",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun DailyMixTimePeriodGrid(
    selectedPeriod: String,
    onPeriodSelect: (String) -> Unit
) {
    val rows = remember {
        listOf(
            listOf("overall" to "All Time", "12month" to "Yearly", "6month" to "6 Months"),
            listOf("3month" to "3 Months", "1month" to "Monthly", "7day" to "Weekly")
        )
    }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        rows.forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                rowItems.forEach { (pId, pLabel) ->
                    DailyMixMorphChip(
                        label = pLabel,
                        selected = selectedPeriod == pId,
                        onClick = { onPeriodSelect(pId) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

/**
 * Clean Material You Expressive morphing chip: shape + colour morphing only.
 * No drawBehind glow — the stacked rounded rects read as a second, larger pill
 * around every chip and bleed over neighbouring chips and section titles.
 */
@Composable
internal fun DailyMixMorphChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selectedContainerColor: Color = MaterialTheme.colorScheme.primary,
    selectedContentColor: Color = MaterialTheme.colorScheme.onPrimary
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.94f else 1.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "dm_morph_chip_scale"
    )
    val cornerRadius by animateDpAsState(
        targetValue = when {
            isPressed -> 10.dp
            selected -> 22.dp
            else -> 14.dp
        },
        animationSpec = tween(durationMillis = 420, easing = AiSmoothMorphEasing),
        label = "dm_morph_chip_corner"
    )
    val containerColor by animateColorAsState(
        targetValue = when {
            selected -> selectedContainerColor
            isPressed -> MaterialTheme.colorScheme.surfaceContainerHighest
            else -> MaterialTheme.colorScheme.surfaceContainerHigh
        },
        animationSpec = tween(durationMillis = 320, easing = AiSmoothMorphEasing),
        label = "dm_morph_chip_bg"
    )
    val contentColor by animateColorAsState(
        targetValue = if (selected) selectedContentColor else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(durationMillis = 320, easing = AiSmoothMorphEasing),
        label = "dm_morph_chip_fg"
    )

    Surface(
        onClick = onClick,
        interactionSource = interactionSource,
        shape = AbsoluteSmoothCornerShape(cornerRadius, 60),
        color = containerColor,
        contentColor = contentColor,
        shadowElevation = 0.dp,
        modifier = modifier
            .height(40.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontFamily = GoogleSansRounded,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                ),
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun DailyMixModeQuickSelectRow(
    selectedMode: String,
    onModeClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val modes = remember {
        listOf(
            DailyMixModeItem("recommendations", "My Recommendations", R.drawable.rounded_auto_awesome_24, AiExpressiveShapeType.COOKIE, AiExpressiveShapeType.FOUR_HEAD_COOKIE),
            DailyMixModeItem("recent", "Recent Tracks", R.drawable.rounded_history_24, AiExpressiveShapeType.FOUR_HEAD_COOKIE, AiExpressiveShapeType.CIRCLE),
            DailyMixModeItem("top", "Top Tracks", R.drawable.rounded_leaderboard_24, AiExpressiveShapeType.ROUNDED_TRIANGLE, AiExpressiveShapeType.SIX_COOKIE),
            DailyMixModeItem("library", "My Library", R.drawable.rounded_library_music_24, AiExpressiveShapeType.CIRCLE, AiExpressiveShapeType.FOUR_HEAD_COOKIE),
            DailyMixModeItem("similar-tracks", "Similar Tracks", R.drawable.rounded_music_note_24, AiExpressiveShapeType.ROUNDED_SQUARE, AiExpressiveShapeType.COOKIE),
            DailyMixModeItem("similar-artists", "Similar Artists", R.drawable.rounded_people_24, AiExpressiveShapeType.SIX_COOKIE, AiExpressiveShapeType.ROUNDED_TRIANGLE),
            DailyMixModeItem("tag", "By Tag / Genre", R.drawable.rounded_sell_24, AiExpressiveShapeType.SUNNY_COOKIE, AiExpressiveShapeType.FOUR_HEAD_COOKIE),
            DailyMixModeItem("mix", "My Mix", R.drawable.rounded_shuffle_24, AiExpressiveShapeType.COOKIE, AiExpressiveShapeType.ROUNDED_SQUARE)
        )
    }

    val scrollState = rememberScrollState()
    val sharedBadgeSpinState = rememberInfiniteTransition(label = "dm_shared_pill_badge_spin").animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(24000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "dm_shared_pill_badge_spin_value"
    )
    val rows = remember(modes) {
        List(2) { rowIdx ->
            modes.mapIndexedNotNull { idx, item ->
                if (idx % 2 == rowIdx) idx to item else null
            }
        }
    }

    // 20.dp vertical padding inside horizontalScroll ensures pill glows never clip on top or bottom
    Column(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState)
            .padding(horizontal = 20.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        rows.forEach { rowItems ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                rowItems.forEach { (index, mode) ->
                    DailyMixModePill(
                        mode = mode,
                        index = index,
                        isSelected = selectedMode == mode.id,
                        badgeSpinProvider = { sharedBadgeSpinState.value },
                        onClick = { onModeClick(mode.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun DailyMixModePill(
    mode: DailyMixModeItem,
    index: Int,
    isSelected: Boolean,
    badgeSpinProvider: () -> Float,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.94f else 1.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "dm_pill_scale"
    )
    val cornerTL by animateDpAsState(
        targetValue = if (isPressed || isSelected) (if (index % 2 == 0) 14.dp else 28.dp) else 28.dp,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "dm_pill_ctl"
    )
    val cornerBR by animateDpAsState(
        targetValue = if (isPressed || isSelected) (if (index % 2 == 0) 14.dp else 28.dp) else 28.dp,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "dm_pill_cbr"
    )
    val cornerTR by animateDpAsState(
        targetValue = if (isPressed || isSelected) (if (index % 2 == 0) 28.dp else 14.dp) else 28.dp,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "dm_pill_ctr"
    )
    val cornerBL by animateDpAsState(
        targetValue = if (isPressed || isSelected) (if (index % 2 == 0) 28.dp else 14.dp) else 28.dp,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "dm_pill_cbl"
    )
    val pillShape = remember(cornerTL, cornerTR, cornerBL, cornerBR) {
        AbsoluteSmoothCornerShape(
            cornerRadiusTL = cornerTL,
            smoothnessAsPercentTL = 60,
            cornerRadiusTR = cornerTR,
            smoothnessAsPercentTR = 60,
            cornerRadiusBL = cornerBL,
            smoothnessAsPercentBL = 60,
            cornerRadiusBR = cornerBR,
            smoothnessAsPercentBR = 60
        )
    }

    val morphFraction by animateFloatAsState(
        targetValue = if (isPressed || isSelected) 1f else 0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "dm_icon_morph"
    )

    val isHighlighted = isPressed || isSelected

    val containerColor by animateColorAsState(
        targetValue = if (isHighlighted) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
        animationSpec = tween(180),
        label = "dm_pill_color"
    )
    val badgeColor by animateColorAsState(
        targetValue = if (isHighlighted) {
            MaterialTheme.colorScheme.primary
        } else {
            when (index % 3) {
                0 -> MaterialTheme.colorScheme.primaryContainer
                1 -> MaterialTheme.colorScheme.tertiaryContainer
                else -> MaterialTheme.colorScheme.secondaryContainer
            }
        },
        animationSpec = tween(180),
        label = "dm_badge_color"
    )
    val iconTint by animateColorAsState(
        targetValue = if (isHighlighted) {
            MaterialTheme.colorScheme.onPrimary
        } else {
            when (index % 3) {
                0 -> MaterialTheme.colorScheme.onPrimaryContainer
                1 -> MaterialTheme.colorScheme.onTertiaryContainer
                else -> MaterialTheme.colorScheme.onSecondaryContainer
            }
        },
        animationSpec = tween(180),
        label = "dm_icon_tint"
    )

    val pillTilt by animateFloatAsState(
        targetValue = if (isPressed) (if (index % 2 == 0) -2.5f else 2.5f) else 0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "dm_pill_tilt"
    )
    val spinDir = if (index % 2 == 0) 1f else -1f
    val pillGradientAlpha = (0.20f + 0.35f * morphFraction).coerceIn(0f, 1f)
    val badgeHighlight = lerp(badgeColor, Color.White, 0.30f)

    Surface(
        onClick = onClick,
        interactionSource = interactionSource,
        shape = pillShape,
        color = containerColor,
        shadowElevation = 0.dp,
        modifier = modifier
            .height(48.dp)
            .wrapContentWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                rotationZ = pillTilt
            }
    ) {
        Row(
            modifier = Modifier
                .height(48.dp)
                .wrapContentWidth()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(badgeColor.copy(alpha = pillGradientAlpha), Color.Transparent)
                    )
                )
                .padding(start = 7.dp, end = 16.dp, top = 5.dp, bottom = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .graphicsLayer {
                        rotationZ = badgeSpinProvider() * spinDir + morphFraction * (if (index % 2 == 0) 25f else -25f)
                    }
                    .drawAiMorphShape(
                        from = mode.baseShape,
                        to = mode.morphedShape,
                        morphProgress = { morphFraction },
                        brush = Brush.linearGradient(listOf(badgeHighlight, badgeColor))
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(mode.iconRes),
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier
                        .size(20.dp)
                        .graphicsLayer {
                            rotationZ = -(badgeSpinProvider() * spinDir + morphFraction * (if (index % 2 == 0) 25f else -25f))
                        }
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Text(
                text = mode.label,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontFamily = GoogleSansRounded,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                ),
                color = if (isHighlighted) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                softWrap = false
            )
        }
    }
}

/**
 * Material You Expressive Seed Input Card (replaces OutlinedTextField so there are no label-cutout
 * notches or thin wireframes). Features a morphing expressive shape badge on the left and a
 * tactile search button on the right.
 */
@Composable
internal fun ExpressiveSeedInputCard(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    @DrawableRes iconRes: Int,
    badgeShape: AiExpressiveShapeType,
    morphedBadgeShape: AiExpressiveShapeType,
    accentColor: Color,
    accentContainer: Color,
    onSearch: (() -> Unit)? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val isActive = isFocused || value.isNotBlank()

    val morphProgress by animateFloatAsState(
        targetValue = if (isActive) 1f else 0f,
        animationSpec = tween(durationMillis = 480, easing = AiSmoothMorphEasing),
        label = "seed_input_morph"
    )
    val cornerRadius by animateDpAsState(
        targetValue = if (isFocused) 26.dp else 20.dp,
        animationSpec = tween(durationMillis = 440, easing = AiSmoothMorphEasing),
        label = "seed_input_corner"
    )
    val containerColor by animateColorAsState(
        targetValue = if (isFocused) {
            lerp(MaterialTheme.colorScheme.surfaceContainerHighest, accentContainer, 0.22f)
        } else {
            MaterialTheme.colorScheme.surfaceContainerHigh
        },
        animationSpec = tween(durationMillis = 340, easing = AiSmoothMorphEasing),
        label = "seed_input_bg"
    )
    val badgeBg by animateColorAsState(
        targetValue = if (isActive) accentColor else accentContainer,
        animationSpec = tween(durationMillis = 360, easing = AiSmoothMorphEasing),
        label = "seed_badge_bg"
    )
    val badgeIconTint by animateColorAsState(
        targetValue = if (isActive) MaterialTheme.colorScheme.surface else accentColor,
        animationSpec = tween(durationMillis = 360, easing = AiSmoothMorphEasing),
        label = "seed_badge_tint"
    )
    val badgeRot by animateFloatAsState(
        targetValue = if (isActive) 20f else 0f,
        animationSpec = tween(durationMillis = 520, easing = AiSmoothMorphEasing),
        label = "seed_badge_rot"
    )

    Surface(
        shape = AbsoluteSmoothCornerShape(cornerRadius, 60),
        color = containerColor,
        shadowElevation = 0.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(42.dp)
                    .graphicsLayer { rotationZ = badgeRot }
                    .drawAiMorphShape(
                        from = badgeShape,
                        to = morphedBadgeShape,
                        morphProgress = { morphProgress },
                        brush = Brush.linearGradient(
                            colors = listOf(lerp(badgeBg, Color.White, 0.20f), badgeBg)
                        )
                    )
            ) {
                Icon(
                    painter = painterResource(iconRes),
                    contentDescription = null,
                    tint = badgeIconTint,
                    modifier = Modifier
                        .size(20.dp)
                        .graphicsLayer { rotationZ = -badgeRot }
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = GoogleSansRounded,
                        fontWeight = FontWeight.Bold
                    ),
                    color = accentColor
                )
                Spacer(modifier = Modifier.height(2.dp))
                Box(modifier = Modifier.fillMaxWidth()) {
                    if (value.isEmpty()) {
                        Text(
                            text = placeholder,
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontFamily = GoogleSansRounded,
                                fontWeight = FontWeight.Medium
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    BasicTextField(
                        value = value,
                        onValueChange = onValueChange,
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyLarge.copy(
                            fontFamily = GoogleSansRounded,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        keyboardOptions = KeyboardOptions(
                            imeAction = if (onSearch != null) ImeAction.Search else ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onSearch = { onSearch?.invoke() },
                            onDone = { onSearch?.invoke() }
                        ),
                        cursorBrush = SolidColor(accentColor),
                        interactionSource = interactionSource,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            if (onSearch != null) {
                Spacer(modifier = Modifier.width(8.dp))
                val searchInteraction = remember { MutableInteractionSource() }
                val isSearchPressed by searchInteraction.collectIsPressedAsState()
                val searchScale by animateFloatAsState(
                    targetValue = if (isSearchPressed) 0.90f else 1f,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
                    label = "seed_search_scale"
                )
                val searchCorner by animateDpAsState(
                    targetValue = if (isSearchPressed) 12.dp else 18.dp,
                    animationSpec = tween(320, easing = AiSmoothMorphEasing),
                    label = "seed_search_corner"
                )
                Surface(
                    onClick = onSearch,
                    interactionSource = searchInteraction,
                    shape = AbsoluteSmoothCornerShape(searchCorner, 60),
                    color = if (value.isNotBlank()) accentColor else accentContainer,
                    contentColor = if (value.isNotBlank()) MaterialTheme.colorScheme.surface else accentColor,
                    shadowElevation = 0.dp,
                    modifier = Modifier
                        .size(42.dp)
                        .graphicsLayer {
                            scaleX = searchScale
                            scaleY = searchScale
                        }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.Search,
                            contentDescription = "Search",
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Material You Expressive action card for loading Top Tracks / Top Artists from Last.fm.
 */
@Composable
internal fun ExpressiveTopSeedTriggerCard(
    text: String,
    subtitle: String,
    @DrawableRes iconRes: Int,
    isLoading: Boolean,
    accentColor: Color,
    accentContainer: Color,
    onAccentContainer: Color,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "top_seed_trigger_scale"
    )
    val corner by animateDpAsState(
        targetValue = if (isPressed) 16.dp else 22.dp,
        animationSpec = tween(380, easing = AiSmoothMorphEasing),
        label = "top_seed_trigger_corner"
    )

    Surface(
        onClick = onClick,
        enabled = !isLoading,
        interactionSource = interactionSource,
        shape = AbsoluteSmoothCornerShape(corner, 60),
        color = accentContainer,
        contentColor = onAccentContainer,
        shadowElevation = 0.dp,
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(38.dp)
                    .drawAiMorphShape(
                        from = AiExpressiveShapeType.COOKIE,
                        to = AiExpressiveShapeType.SUNNY_COOKIE,
                        morphProgress = { if (isPressed || isLoading) 1f else 0f },
                        brush = SolidColor(accentColor)
                    )
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.surface,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(18.dp)
                    )
                } else {
                    Icon(
                        painter = painterResource(iconRes),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isLoading) "Loading from Last.fm…" else text,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontFamily = GoogleSansRounded,
                        fontWeight = FontWeight.Bold
                    ),
                    color = onAccentContainer
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = onAccentContainer.copy(alpha = 0.78f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Icon(
                painter = painterResource(R.drawable.rounded_auto_awesome_24),
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
internal fun ExpressiveSeedBoardSection(
    title: String,
    count: Int,
    accentColor: Color,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontFamily = GoogleSansRounded,
                    fontWeight = FontWeight.Bold
                ),
                color = accentColor
            )
            Surface(
                shape = AbsoluteSmoothCornerShape(10.dp, 60),
                color = accentColor.copy(alpha = 0.14f)
            ) {
                Text(
                    text = "Tap to select",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = GoogleSansRounded,
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = accentColor,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            content()
        }
    }
}

private val SEED_ITEM_SHAPES = listOf(
    AiExpressiveShapeType.COOKIE to AiExpressiveShapeType.FOUR_HEAD_COOKIE,
    AiExpressiveShapeType.FOUR_HEAD_COOKIE to AiExpressiveShapeType.SIX_COOKIE,
    AiExpressiveShapeType.ROUNDED_SQUARE to AiExpressiveShapeType.COOKIE,
    AiExpressiveShapeType.SIX_COOKIE to AiExpressiveShapeType.SUNNY_COOKIE,
    AiExpressiveShapeType.ROUNDED_TRIANGLE to AiExpressiveShapeType.FOUR_HEAD_COOKIE,
    AiExpressiveShapeType.SUNNY_COOKIE to AiExpressiveShapeType.ROUNDED_SQUARE
)

/**
 * Material You Expressive Seed Board Card for Similar Tracks & Similar Artists.
 * Features smooth corner morphing, tonal container morphing, a left-side 1:1 morphing expressive
 * shape badge, and a right-side selected/seed pill indicator.
 */
@Composable
internal fun ExpressiveSeedBoardItem(
    index: Int,
    title: String,
    subtitle: String,
    @DrawableRes iconRes: Int,
    isSelected: Boolean,
    accentColor: Color,
    accentContainer: Color,
    onAccent: Color,
    onAccentContainer: Color,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val (baseShape, morphedShape) = SEED_ITEM_SHAPES[index % SEED_ITEM_SHAPES.size]

    val activeFraction by animateFloatAsState(
        targetValue = if (isSelected || isPressed) 1f else 0f,
        animationSpec = tween(durationMillis = 480, easing = AiSmoothMorphEasing),
        label = "seed_item_morph"
    )
    val scale by animateFloatAsState(
        targetValue = when {
            isPressed -> 0.96f
            isSelected -> 1.01f
            else -> 1.0f
        },
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "seed_item_scale"
    )
    val cornerRadius by animateDpAsState(
        targetValue = when {
            isPressed -> 14.dp
            isSelected -> 26.dp
            else -> 18.dp
        },
        animationSpec = tween(durationMillis = 480, easing = AiSmoothMorphEasing),
        label = "seed_item_corner"
    )
    val containerColor by animateColorAsState(
        targetValue = if (isSelected) {
            lerp(MaterialTheme.colorScheme.surfaceContainerHigh, accentContainer, 0.55f)
        } else if (isPressed) {
            MaterialTheme.colorScheme.surfaceContainerHighest
        } else {
            MaterialTheme.colorScheme.surfaceContainerHigh
        },
        animationSpec = tween(durationMillis = 380, easing = AiSmoothMorphEasing),
        label = "seed_item_bg"
    )
    val badgeColor by animateColorAsState(
        targetValue = if (isSelected) accentColor else accentContainer,
        animationSpec = tween(durationMillis = 380, easing = AiSmoothMorphEasing),
        label = "seed_item_badge_bg"
    )
    val badgeIconColor by animateColorAsState(
        targetValue = if (isSelected) onAccent else accentColor,
        animationSpec = tween(durationMillis = 380, easing = AiSmoothMorphEasing),
        label = "seed_item_badge_fg"
    )
    val badgeRotation by animateFloatAsState(
        targetValue = if (isSelected) (if (index % 2 == 0) 24f else -24f) else 0f,
        animationSpec = tween(durationMillis = 540, easing = AiSmoothMorphEasing),
        label = "seed_item_badge_rot"
    )

    Surface(
        onClick = onClick,
        interactionSource = interactionSource,
        shape = AbsoluteSmoothCornerShape(cornerRadius, 60),
        color = containerColor,
        shadowElevation = 0.dp,
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            accentColor.copy(alpha = if (isSelected) 0.14f else 0.04f),
                            Color.Transparent
                        )
                    )
                )
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(42.dp)
                    .graphicsLayer { rotationZ = badgeRotation }
                    .drawAiMorphShape(
                        from = baseShape,
                        to = morphedShape,
                        morphProgress = { activeFraction },
                        brush = Brush.linearGradient(
                            colors = listOf(lerp(badgeColor, Color.White, 0.22f), badgeColor)
                        )
                    )
            ) {
                Icon(
                    painter = painterResource(iconRes),
                    contentDescription = null,
                    tint = badgeIconColor,
                    modifier = Modifier
                        .size(20.dp)
                        .graphicsLayer { rotationZ = -badgeRotation }
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontFamily = GoogleSansRounded,
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isSelected) accentColor else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Surface(
                shape = AbsoluteSmoothCornerShape(if (isSelected) 16.dp else 12.dp, 60),
                color = if (isSelected) accentColor else MaterialTheme.colorScheme.surfaceContainerHighest
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Rounded.Check,
                            contentDescription = null,
                            tint = onAccent,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    Text(
                        text = if (isSelected) "Active" else "Use",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = GoogleSansRounded,
                            fontWeight = FontWeight.Bold
                        ),
                        color = if (isSelected) onAccent else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
