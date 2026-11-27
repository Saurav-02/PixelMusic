package com.saurav.pixelmusic.presentation.screens

import com.saurav.pixelmusic.ui.blur.ProgressiveHeaderBackdrop

import com.saurav.pixelmusic.presentation.components.PixelToast as Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import com.saurav.pixelmusic.ui.modifiers.scrollMotionBlur
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.*
import androidx.compose.ui.res.painterResource
import com.saurav.pixelmusic.R
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import com.saurav.pixelmusic.presentation.components.AiExpressiveShapeType
import com.saurav.pixelmusic.presentation.components.AiPlaylistGeneratingFullScreenPage
import com.saurav.pixelmusic.presentation.components.AiSmoothMorphEasing
import com.saurav.pixelmusic.presentation.components.DailyMixMorphChip
import com.saurav.pixelmusic.presentation.components.ExpressiveGroupedCardSpacing
import com.saurav.pixelmusic.presentation.components.ExpressiveSeedBoardItem
import com.saurav.pixelmusic.presentation.components.ExpressiveSeedBoardSection
import com.saurav.pixelmusic.presentation.components.ExpressiveSeedInputCard
import com.saurav.pixelmusic.presentation.components.ExpressiveTopSeedTriggerCard
import com.saurav.pixelmusic.presentation.components.expressiveListItemScrollAnimation
import com.saurav.pixelmusic.presentation.components.MiniPlayerHeight
import com.saurav.pixelmusic.presentation.components.aiSoftSurfaceGlow
import com.saurav.pixelmusic.presentation.components.drawAiMorphShape
import com.saurav.pixelmusic.presentation.components.resolveNavBarOccupiedHeight
import com.saurav.pixelmusic.utils.shapes.RoundedStarShape
import com.saurav.pixelmusic.presentation.navigation.Screen
import com.saurav.pixelmusic.presentation.viewmodel.PlayerViewModel
import com.saurav.pixelmusic.presentation.navigation.navigateSafelyReplacing
import com.saurav.pixelmusic.presentation.viewmodel.SmartMixViewModel
import com.saurav.pixelmusic.presentation.viewmodel.SmartMixUiState
import com.saurav.pixelmusic.presentation.viewmodel.LastFmTrack
import com.saurav.pixelmusic.ui.theme.GoogleSansRounded
import racra.compose.smooth_corner_rect_library.AbsoluteSmoothCornerShape

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmartMixScreen(
    navController: NavController,
    playerViewModel: com.saurav.pixelmusic.presentation.viewmodel.PlayerViewModel,
    viewModel: SmartMixViewModel = hiltViewModel()
) {
    val localModeEnabled by playerViewModel.localModeEnabled.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Observe navigation and toast events
    LaunchedEffect(uiState.generatedPlaylistId) {
        uiState.generatedPlaylistId?.let { playlistId ->
            viewModel.clearGeneratedPlaylistId()
            // Navigate replacing the SmartMix screen so back goes to Explore
            navController.navigateSafelyReplacing(Screen.PlaylistDetail.createRoute(playlistId), Screen.SmartMix.route)
        }
    }

    LaunchedEffect(uiState.toastMessage) {
        uiState.toastMessage?.let { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            viewModel.clearToast()
        }
    }

    val primaryColor = MaterialTheme.colorScheme.primary
    val surfaceColor = MaterialTheme.colorScheme.surface
    val backgroundBrush = remember(surfaceColor, primaryColor) {
        Brush.verticalGradient(
            colors = listOf(
                primaryColor.copy(alpha = 0.12f),
                surfaceColor.copy(alpha = 0.8f),
                surfaceColor
            ),
            endY = 1200f
        )
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            Box {
                ProgressiveHeaderBackdrop(
                    scrimColor = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.matchParentSize()
                )
                TopAppBar(
                    title = {
                        Text(
                            text = "Smart Mix",
                            fontFamily = GoogleSansRounded,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        scrolledContainerColor = Color.Transparent
                    )
                )
            }
        }
    ) { innerPadding ->
        if (localModeEnabled) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(backgroundBrush)
                    .padding(innerPadding)
                    .padding(horizontal = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    modifier = Modifier.size(72.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            painter = painterResource(id = R.drawable.rounded_auto_awesome_24),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.size(34.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    text = "Smart Mix is unavailable in Local Mode",
                    fontFamily = GoogleSansRounded,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Turn off Local Mode to generate Smart Mix playlists using YouTube Music.",
                    fontFamily = GoogleSansRounded,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(22.dp))
                Button(
                    onClick = { playerViewModel.setLocalModeEnabled(false) },
                    shape = CircleShape
                ) {
                    Text(
                        text = "Turn Off Local Mode",
                        fontFamily = GoogleSansRounded,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            return@Scaffold
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundBrush)
                .padding(innerPadding)
        ) {
            if (!uiState.isLastfmLoggedIn) {
                LastfmLoggedOutState(
                    onConnectClick = {
                        navController.navigate(Screen.Accounts.route)
                    }
                )
            } else {
                SmartMixConfigurator(
                    uiState = uiState,
                    viewModel = viewModel,
                    playerViewModel = playerViewModel
                )
            }

            if (uiState.isGenerating) {
                AiPlaylistGeneratingFullScreenPage(
                    isSuccess = false,
                    status = uiState.generationProgress,
                    error = null,
                    customHeadlines = listOf(
                        "Building your Smart Mix…",
                        "Syncing with Last.fm…",
                        "Scanning your Last.fm history…",
                        "Matching your top artists & tracks…",
                        "Resolving tracks on YouTube Music…",
                        "Curating your Last.fm Smart Mix…"
                    ),
                    customPhases = listOf(
                        "Connecting to your Last.fm profile…",
                        "Gathering scrobbles, top tracks & recommendations…",
                        "Matching playable songs on YouTube Music…",
                        "Saving your Smart Mix playlist…"
                    ),
                    successHeadline = "Smart Mix Ready!",
                    successSubtitle = "Opening your Last.fm Smart Mix…",
                    preferLiveStatus = true,
                    onDismiss = {}
                )
            }
        }
    }
}

@Composable
private fun LastfmLoggedOutState(
    onConnectClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        val shape = remember { AbsoluteSmoothCornerShape(24.dp, 60) }
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = shape,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.6f)
            )
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    painter = painterResource(R.drawable.rounded_auto_awesome_24),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Connect Last.fm Account",
                    style = MaterialTheme.typography.titleLarge.copy(fontFamily = GoogleSansRounded, fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Smart Mix uses your Last.fm scrobble history, top tracks, recent listens, and favorite genres to build highly personalized discovery mixes for you.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = onConnectClick,
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(painterResource(R.drawable.rounded_link_24), contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Go to Linked Accounts", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
private fun SmartMixConfigurator(
    uiState: SmartMixUiState,
    viewModel: SmartMixViewModel,
    playerViewModel: PlayerViewModel
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val isPlayerActive by remember(playerViewModel.stablePlayerState) {
        playerViewModel.stablePlayerState.map { it.currentSong != null }.distinctUntilChanged()
    }.collectAsStateWithLifecycle(initialValue = false)
    val navBarCompactMode by playerViewModel.navBarCompactMode.collectAsStateWithLifecycle()

    val systemNavBarInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val bottomBarHeightDp = systemNavBarInset

    val fabAreaHeight = 56.dp + 20.dp + (if (isPlayerActive) MiniPlayerHeight else 0.dp) + bottomBarHeightDp
    val isDark = androidx.compose.foundation.isSystemInDarkTheme()

    val playerColorSchemePair by playerViewModel.activePlayerColorSchemePair.collectAsStateWithLifecycle()
    val activeColorScheme = playerColorSchemePair?.let { if (isDark) it.dark else it.light }
        ?: MaterialTheme.colorScheme

    val modeColors = remember(activeColorScheme) {
        mapOf(
            "top"             to (activeColorScheme.primaryContainer to activeColorScheme.primary),
            "recent"          to (activeColorScheme.secondaryContainer to activeColorScheme.secondary),
            "similar-tracks"  to (activeColorScheme.tertiaryContainer to activeColorScheme.tertiary),
            "similar-artists" to (activeColorScheme.errorContainer to activeColorScheme.error),
            "tag"             to (activeColorScheme.surfaceVariant to activeColorScheme.onSurfaceVariant),
            "mix"             to (activeColorScheme.primaryContainer to activeColorScheme.tertiary),
            "recommendations" to (activeColorScheme.tertiaryContainer to activeColorScheme.primary),
            "library"         to (activeColorScheme.secondaryContainer to activeColorScheme.primary)
        )
    }

    val ambientScheme = MaterialTheme.colorScheme
    val ambientDark = isDark
    val selectedColorPair = uiState.selectedMode?.let { modeColors[it] }
    val targetAmbientA = selectedColorPair?.second ?: ambientScheme.primary
    val targetAmbientB = selectedColorPair?.first ?: ambientScheme.tertiaryContainer
    val ambientA by animateColorAsState(targetValue = targetAmbientA, animationSpec = tween(900, easing = AiSmoothMorphEasing), label = "sm_ambient_a")
    val ambientB by animateColorAsState(targetValue = targetAmbientB, animationSpec = tween(900, easing = AiSmoothMorphEasing), label = "sm_ambient_b")
    val ambientPhase by rememberInfiniteTransition(label = "sm_ambient").animateFloat(
        initialValue = 0f,
        targetValue = 6.2831855f,
        animationSpec = infiniteRepeatable(tween(18000, easing = LinearEasing), RepeatMode.Restart),
        label = "sm_ambient_phase"
    )
    val haptic = LocalHapticFeedback.current

    val baseShapes = remember {
        listOf(
            AiExpressiveShapeType.ROUNDED_SQUARE to AiExpressiveShapeType.COOKIE,
            AiExpressiveShapeType.CIRCLE to AiExpressiveShapeType.FOUR_HEAD_COOKIE,
            AiExpressiveShapeType.ROUNDED_TRIANGLE to AiExpressiveShapeType.SIX_COOKIE,
            AiExpressiveShapeType.SIX_COOKIE to AiExpressiveShapeType.SUNNY_COOKIE,
            AiExpressiveShapeType.FOUR_HEAD_COOKIE to AiExpressiveShapeType.ROUNDED_TRIANGLE,
            AiExpressiveShapeType.COOKIE to AiExpressiveShapeType.ROUNDED_SQUARE,
            AiExpressiveShapeType.SUNNY_COOKIE to AiExpressiveShapeType.FOUR_HEAD_COOKIE,
            AiExpressiveShapeType.CIRCLE to AiExpressiveShapeType.COOKIE
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .drawWithCache {
                val brushA = Brush.radialGradient(listOf(ambientA.copy(alpha = 1f), Color.Transparent), Offset.Zero, 1f)
                val brushB = Brush.radialGradient(listOf(ambientB.copy(alpha = 1f), Color.Transparent), Offset.Zero, 1f)
                onDrawBehind {
                    val w = size.width
                    val h = size.height
                    val t = ambientPhase
                    withTransform({
                        translate(w * (0.18f + 0.08f * kotlin.math.cos(t)), h * (0.15f + 0.04f * kotlin.math.sin(t)))
                        scale(w * 0.72f, w * 0.72f, Offset.Zero)
                    }) {
                        drawCircle(brush = brushA, radius = 1f, center = Offset.Zero, alpha = if (ambientDark) 0.16f else 0.14f)
                    }
                    withTransform({
                        translate(w * (0.85f + 0.07f * kotlin.math.sin(t + 1.8f)), h * (0.55f + 0.06f * kotlin.math.cos(t + 2.1f)))
                        scale(w * 0.68f, w * 0.68f, Offset.Zero)
                    }) {
                        drawCircle(brush = brushB, radius = 1f, center = Offset.Zero, alpha = if (ambientDark) 0.14f else 0.12f)
                    }
                }
            }
    ) {
        val smartMixListState = rememberLazyListState()
        LazyColumn(
            state = smartMixListState,
            modifier = Modifier
                .fillMaxSize()
                .scrollMotionBlur(lazyListState = smartMixListState),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 12.dp,
                bottom = fabAreaHeight + 36.dp
            ),
            verticalArrangement = Arrangement.spacedBy(ExpressiveGroupedCardSpacing)
        ) {
            // 1. Slider Card
            item {
                TrackCountSliderCard(
                    count = uiState.trackCount,
                    onCountChange = { viewModel.setTrackCount(it) }
                )
            }

            // Section heading
            item {
                Text(
                    text = "Generation Mode",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 0.8.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 4.dp, top = 22.dp, bottom = 10.dp)
                )
            }

            // 2. Mode items — Material You Expressive grouped list cards with scroll entrance
            val modes = listOf(
                ModeItemData("top",             "Top Tracks",       "Your most played tracks of all time",      R.drawable.rounded_leaderboard_24),
                ModeItemData("recent",          "Recent Tracks",    "What you've been listening to lately",     R.drawable.rounded_history_24),
                ModeItemData("similar-tracks",  "Similar Tracks",   "Tracks similar to one you love",           R.drawable.rounded_music_note_24),
                ModeItemData("similar-artists", "Similar Artists",  "Discover artists like your favourites",    R.drawable.rounded_people_24),
                ModeItemData("tag",             "By Tag / Genre",   "Browse by genre like rock, lofi, jazz",    R.drawable.rounded_sell_24),
                ModeItemData("mix",             "My Mix",           "Smart blend of top, recent & similar",     R.drawable.rounded_shuffle_24),
                ModeItemData("recommendations", "My Recommendations","30 fresh tracks tailored for you",       R.drawable.rounded_auto_awesome_24),
                ModeItemData("library",         "My Library",       "Re-discover the sounds of your past",      R.drawable.rounded_library_music_24)
            )

            modes.forEachIndexed { idx, mode ->
                item(key = mode.id) {
                    val isSelected = uiState.selectedMode == mode.id
                    val isFirst = idx == 0
                    val isLast = idx == modes.lastIndex
                    val colors = modeColors[mode.id]!!
                    val (fromShape, toShape) = baseShapes[idx % baseShapes.size]

                    val selectProgress by animateFloatAsState(
                        targetValue = if (isSelected) 1f else 0f,
                        animationSpec = tween(durationMillis = 560, easing = AiSmoothMorphEasing),
                        label = "card_select_progress"
                    )
                    val modeInteraction = remember { MutableInteractionSource() }
                    val isModePressed by modeInteraction.collectIsPressedAsState()
                    val cardScale by animateFloatAsState(
                        targetValue = when {
                            isModePressed -> 0.975f
                            isSelected -> 1.0f
                            else -> 1.0f
                        },
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow
                        ),
                        label = "card_scale"
                    )
                    val topCorner by animateDpAsState(
                        targetValue = when {
                            isModePressed -> if (isFirst || isSelected) 18.dp else 10.dp
                            isSelected -> 24.dp
                            isFirst -> 24.dp
                            else -> 4.dp
                        },
                        animationSpec = tween(durationMillis = 580, easing = AiSmoothMorphEasing),
                        label = "card_top_corner"
                    )
                    val bottomCorner by animateDpAsState(
                        targetValue = when {
                            isModePressed -> if (isLast || isSelected) 18.dp else 10.dp
                            isSelected -> 24.dp
                            isLast -> 24.dp
                            else -> 4.dp
                        },
                        animationSpec = tween(durationMillis = 580, easing = AiSmoothMorphEasing),
                        label = "card_bottom_corner"
                    )
                    val itemShape = remember(topCorner, bottomCorner) {
                        RoundedCornerShape(
                            topStart = topCorner,
                            topEnd = topCorner,
                            bottomStart = bottomCorner,
                            bottomEnd = bottomCorner
                        )
                    }

                    val cardBgColor by animateColorAsState(
                        targetValue = if (isSelected) {
                            lerp(MaterialTheme.colorScheme.surfaceContainerHigh, colors.first, 0.34f)
                        } else {
                            MaterialTheme.colorScheme.surfaceContainer
                        },
                        animationSpec = tween(durationMillis = 540, easing = AiSmoothMorphEasing),
                        label = "card_bg"
                    )

                    val badgeRot by animateFloatAsState(
                        targetValue = if (isSelected) (if (idx % 2 == 0) 24f else -24f) else 0f,
                        animationSpec = tween(durationMillis = 640, easing = AiSmoothMorphEasing),
                        label = "badge_rot"
                    )
                    val badgeBg by animateColorAsState(
                        targetValue = if (isSelected) colors.second else colors.first.copy(alpha = 0.55f),
                        animationSpec = tween(durationMillis = 500, easing = AiSmoothMorphEasing),
                        label = "badge_bg"
                    )
                    val badgeIconTint by animateColorAsState(
                        targetValue = if (isSelected) colors.first else colors.second,
                        animationSpec = tween(durationMillis = 500, easing = AiSmoothMorphEasing),
                        label = "badge_tint"
                    )

                    Surface(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            if (isSelected) viewModel.setSelectedMode(null)
                            else viewModel.setSelectedMode(mode.id)
                        },
                        interactionSource = modeInteraction,
                        shape = itemShape,
                        color = cardBgColor,
                        shadowElevation = 0.dp,
                        modifier = Modifier
                            .animateItem()
                            .fillMaxWidth()
                            .expressiveListItemScrollAnimation(smartMixListState)
                            .graphicsLayer {
                                scaleX = cardScale
                                scaleY = cardScale
                            }
                            .aiSoftSurfaceGlow(
                                color = colors.second,
                                cornerRadius = topCorner,
                                spread = 10.dp * selectProgress,
                                offsetY = 2.dp * selectProgress,
                                maxAlpha = (if (isDark) 0.28f else 0.18f) * selectProgress
                            )
                    ) {
                        Column(
                            modifier = Modifier.background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        colors.second.copy(alpha = 0.10f * selectProgress),
                                        Color.Transparent
                                    )
                                )
                            )
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                            ) {
                                // Material You Expressive Morphing Shape Badge
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .size(54.dp)
                                        .graphicsLayer { rotationZ = badgeRot }
                                        .drawAiMorphShape(
                                            from = fromShape,
                                            to = toShape,
                                            morphProgress = { selectProgress },
                                            brush = Brush.verticalGradient(
                                                colors = listOf(
                                                    lerp(badgeBg, Color.White, if (isSelected) 0.18f else 0.06f),
                                                    badgeBg
                                                )
                                            )
                                        )
                                ) {
                                    Icon(
                                        painter = painterResource(mode.iconRes),
                                        contentDescription = null,
                                        modifier = Modifier
                                            .size(24.dp)
                                            .graphicsLayer { rotationZ = -badgeRot },
                                        tint = badgeIconTint
                                    )
                                }

                                Spacer(modifier = Modifier.width(16.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = mode.title,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                        fontFamily = GoogleSansRounded,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = mode.desc,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                val iconRotation by animateFloatAsState(
                                    targetValue = if (isSelected) 90f else 0f,
                                    animationSpec = tween(durationMillis = 520, easing = AiSmoothMorphEasing),
                                    label = "chevron_rotation"
                                )
                                val chevronCorner by animateDpAsState(
                                    targetValue = if (isSelected) 18.dp else 12.dp,
                                    animationSpec = tween(durationMillis = 520, easing = AiSmoothMorphEasing),
                                    label = "chevron_corner"
                                )
                                Surface(
                                    shape = AbsoluteSmoothCornerShape(chevronCorner, 60),
                                    color = if (isSelected) colors.second else MaterialTheme.colorScheme.surfaceContainerHigh,
                                    modifier = Modifier.size(34.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                                            contentDescription = null,
                                            modifier = Modifier
                                                .size(20.dp)
                                                .graphicsLayer { rotationZ = iconRotation },
                                            tint = if (isSelected) colors.first
                                                   else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }

                            AnimatedVisibility(
                                visible = isSelected,
                                enter = expandVertically(tween(540, easing = AiSmoothMorphEasing)) + fadeIn(tween(480, easing = AiSmoothMorphEasing)),
                                exit  = shrinkVertically(tween(460, easing = AiSmoothMorphEasing)) + fadeOut(tween(320, easing = AiSmoothMorphEasing))
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(start = 20.dp, end = 20.dp, bottom = 20.dp)
                                ) {
                                    HorizontalDivider(
                                        color = colors.second.copy(alpha = 0.15f),
                                        modifier = Modifier.padding(bottom = 16.dp)
                                    )
                                    when (mode.id) {
                                        "top", "library" -> PeriodSelectionSection(
                                            selectedPeriod = uiState.timePeriod,
                                            onPeriodSelect = { viewModel.setTimePeriod(it) },
                                            colors = colors
                                        )
                                        "recent" -> Text(
                                            text = "Fetches what you have scrobbled recently on Last.fm.",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        "similar-tracks" -> SimilarTracksInputs(
                                            uiState = uiState, viewModel = viewModel,
                                            keyboardController = keyboardController,
                                            colors = colors
                                        )
                                        "similar-artists" -> SimilarArtistsInputs(
                                            uiState = uiState, viewModel = viewModel,
                                            keyboardController = keyboardController,
                                            colors = colors
                                        )
                                        "tag" -> TagInputs(
                                            uiState = uiState, viewModel = viewModel,
                                            keyboardController = keyboardController,
                                            colors = colors
                                        )
                                        "mix" -> Text(
                                            text = "A smart blend of your top tracks, recent plays, and tracks similar to your favorite artists.",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        "recommendations" -> Text(
                                            text = "Uses your Last.fm profile to score, filter, and balance familiar sounds with new discoveries.",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // FAB — sticky bottom glass panel to prevent overlapping raw cards text
        val isGenerateEnabled = uiState.selectedMode != null && when (uiState.selectedMode) {
            "similar-tracks"  -> uiState.seedTrackName.isNotBlank() && uiState.seedArtistName.isNotBlank()
            "similar-artists" -> uiState.seedArtistInput.isNotBlank()
            "tag"             -> uiState.tagInput.isNotBlank()
            else              -> true
        }

        AnimatedVisibility(
            visible = uiState.selectedMode != null,
            enter = slideInVertically(
                initialOffsetY = { it },
                animationSpec = tween(durationMillis = 520, easing = AiSmoothMorphEasing)
            ) + fadeIn(tween(durationMillis = 460, easing = AiSmoothMorphEasing)),
            exit = slideOutVertically(
                targetOffsetY = { it },
                animationSpec = tween(durationMillis = 420, easing = AiSmoothMorphEasing)
            ) + fadeOut(tween(durationMillis = 320)),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
        ) {
            val genInteraction = remember { MutableInteractionSource() }
            val isGenPressed by genInteraction.collectIsPressedAsState()
            val genScale by animateFloatAsState(
                targetValue = if (isGenPressed) 0.94f else 1f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
                label = "sm_gen_scale"
            )
            val genCorner by animateDpAsState(
                targetValue = if (isGenPressed) 18.dp else 28.dp,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
                label = "sm_gen_corner"
            )
            val genContainer by animateColorAsState(
                targetValue = if (isGenerateEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHighest,
                animationSpec = tween(320, easing = AiSmoothMorphEasing),
                label = "sm_gen_container"
            )
            val genContent by animateColorAsState(
                targetValue = if (isGenerateEnabled) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f),
                animationSpec = tween(320, easing = AiSmoothMorphEasing),
                label = "sm_gen_content"
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                                MaterialTheme.colorScheme.surface
                            )
                        )
                    )
                    .padding(horizontal = 24.dp)
                    .padding(
                        top = 16.dp,
                        bottom = (if (isPlayerActive) MiniPlayerHeight else 0.dp) + bottomBarHeightDp + 16.dp
                    ),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    onClick = {
                        if (isGenerateEnabled) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            keyboardController?.hide()
                            viewModel.generatePlaylist()
                        }
                    },
                    enabled = isGenerateEnabled,
                    interactionSource = genInteraction,
                    shape = AbsoluteSmoothCornerShape(genCorner, 60),
                    color = genContainer,
                    contentColor = genContent,
                    shadowElevation = 0.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .graphicsLayer {
                            scaleX = genScale
                            scaleY = genScale
                        }
                        .aiSoftSurfaceGlow(
                            color = if (isGenerateEnabled) MaterialTheme.colorScheme.primary else Color.Transparent,
                            cornerRadius = genCorner,
                            spread = 14.dp,
                            offsetY = 4.dp,
                            maxAlpha = if (isGenerateEnabled) (if (isDark) 0.34f else 0.22f) else 0f
                        )
                ) {
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.rounded_auto_awesome_24),
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Generate Mix",
                            fontFamily = GoogleSansRounded,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TrackCountSliderCard(
    count: Int,
    onCountChange: (Int) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val shape = remember { AbsoluteSmoothCornerShape(26.dp, 60) }
    val starShape = remember { RoundedStarShape(sides = 8, curve = 0.08, rotation = 0f) }
    val badgeRotation by animateFloatAsState(
        targetValue = (count - 5) * 4f,
        animationSpec = tween(durationMillis = 480, easing = AiSmoothMorphEasing),
        label = "track_count_badge_rot"
    )

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceContainer,
        shadowElevation = 0.dp
    ) {
        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Track Count",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        fontFamily = GoogleSansRounded,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "How many songs to generate",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(52.dp)
                ) {
                    Surface(
                        shape = starShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shadowElevation = 0.dp,
                        modifier = Modifier
                            .size(52.dp)
                            .graphicsLayer { rotationZ = badgeRotation }
                    ) {}
                    Text(
                        text = "$count",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = GoogleSansRounded,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
            Slider(
                value = count.toFloat(),
                onValueChange = { onCountChange(it.toInt()) },
                valueRange = 5f..35f,
                steps = 30,
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary,
                    inactiveTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest
                )
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                listOf(10, 15, 25, 35).forEach { preset ->
                    DailyMixMorphChip(
                        label = "$preset",
                        selected = count == preset,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onCountChange(preset)
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun PeriodSelectionSection(
    selectedPeriod: String,
    onPeriodSelect: (String) -> Unit,
    colors: Pair<Color, Color>
) {
    val rows = remember {
        listOf(
            listOf("overall" to "All Time", "12month" to "12 Months", "6month" to "6 Months"),
            listOf("3month" to "3 Months", "1month" to "1 Month", "7day" to "7 Days")
        )
    }

    Text(
        text = "Select Time Period",
        style = MaterialTheme.typography.labelLarge.copy(
            fontFamily = GoogleSansRounded,
            fontWeight = FontWeight.Bold
        ),
        color = colors.second,
        modifier = Modifier.padding(bottom = 14.dp)
    )

    val haptic = LocalHapticFeedback.current
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        rows.forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                rowItems.forEach { (id, label) ->
                    DailyMixMorphChip(
                        label = label,
                        selected = selectedPeriod == id,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onPeriodSelect(id)
                        },
                        selectedContainerColor = colors.second,
                        selectedContentColor = colors.first,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun SimilarTracksInputs(
    uiState: SmartMixUiState,
    viewModel: SmartMixViewModel,
    keyboardController: androidx.compose.ui.platform.SoftwareKeyboardController?,
    colors: Pair<Color, Color>
) {
    val haptic = LocalHapticFeedback.current
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        ExpressiveSeedInputCard(
            value = uiState.seedTrackName,
            onValueChange = { viewModel.setSeedTrackName(it) },
            label = "Seed Track",
            placeholder = "e.g. Blinding Lights",
            iconRes = R.drawable.rounded_music_note_24,
            badgeShape = AiExpressiveShapeType.COOKIE,
            morphedBadgeShape = AiExpressiveShapeType.FOUR_HEAD_COOKIE,
            accentColor = colors.second,
            accentContainer = colors.first,
            onSearch = {
                keyboardController?.hide()
                viewModel.searchSeedTrack()
            }
        )

        ExpressiveSeedInputCard(
            value = uiState.seedArtistName,
            onValueChange = { viewModel.setSeedArtistName(it) },
            label = "Seed Artist",
            placeholder = "e.g. The Weeknd",
            iconRes = R.drawable.rounded_people_24,
            badgeShape = AiExpressiveShapeType.FOUR_HEAD_COOKIE,
            morphedBadgeShape = AiExpressiveShapeType.SIX_COOKIE,
            accentColor = colors.second,
            accentContainer = colors.first,
            onSearch = null
        )

        ExpressiveTopSeedTriggerCard(
            text = "Pick From My Top Tracks",
            subtitle = "Choose a track from your Last.fm top listens",
            iconRes = R.drawable.rounded_library_music_24,
            isLoading = uiState.isSearchingTopSeeds || uiState.isSearchingSeed,
            accentColor = colors.second,
            accentContainer = colors.first,
            onAccentContainer = colors.second,
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                keyboardController?.hide()
                viewModel.loadTopTracksForSeed()
            }
        )

        if (uiState.searchTrackResults.isNotEmpty()) {
            ExpressiveSeedBoardSection(
                title = "Search Results",
                count = uiState.searchTrackResults.size,
                accentColor = colors.second
            ) {
                uiState.searchTrackResults.forEachIndexed { idx, track ->
                    val selected = uiState.seedTrackName.equals(track.name, ignoreCase = true) &&
                        uiState.seedArtistName.equals(track.artist, ignoreCase = true)
                    ExpressiveSeedBoardItem(
                        index = idx,
                        title = track.name,
                        subtitle = track.artist,
                        iconRes = R.drawable.rounded_music_note_24,
                        isSelected = selected,
                        accentColor = colors.second,
                        accentContainer = colors.first,
                        onAccent = colors.first,
                        onAccentContainer = colors.second,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            viewModel.setSeedTrackName(track.name)
                            viewModel.setSeedArtistName(track.artist)
                        }
                    )
                }
            }
        }

        if (uiState.topTracksForSeed.isNotEmpty()) {
            ExpressiveSeedBoardSection(
                title = "My Top Tracks",
                count = uiState.topTracksForSeed.size,
                accentColor = colors.second
            ) {
                uiState.topTracksForSeed.forEachIndexed { idx, track ->
                    val selected = uiState.seedTrackName.equals(track.name, ignoreCase = true) &&
                        uiState.seedArtistName.equals(track.artist, ignoreCase = true)
                    ExpressiveSeedBoardItem(
                        index = idx,
                        title = track.name,
                        subtitle = track.artist,
                        iconRes = R.drawable.rounded_music_note_24,
                        isSelected = selected,
                        accentColor = colors.second,
                        accentContainer = colors.first,
                        onAccent = colors.first,
                        onAccentContainer = colors.second,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            viewModel.setSeedTrackName(track.name)
                            viewModel.setSeedArtistName(track.artist)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun SimilarArtistsInputs(
    uiState: SmartMixUiState,
    viewModel: SmartMixViewModel,
    keyboardController: androidx.compose.ui.platform.SoftwareKeyboardController?,
    colors: Pair<Color, Color>
) {
    val haptic = LocalHapticFeedback.current
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        ExpressiveSeedInputCard(
            value = uiState.seedArtistInput,
            onValueChange = { viewModel.setSeedArtistInput(it) },
            label = "Seed Artist",
            placeholder = "e.g. Coldplay, Arijit Singh…",
            iconRes = R.drawable.rounded_people_24,
            badgeShape = AiExpressiveShapeType.SIX_COOKIE,
            morphedBadgeShape = AiExpressiveShapeType.COOKIE,
            accentColor = colors.second,
            accentContainer = colors.first,
            onSearch = {
                keyboardController?.hide()
                viewModel.searchSeedArtist()
            }
        )

        ExpressiveTopSeedTriggerCard(
            text = "Pick From My Top Artists",
            subtitle = "Choose an artist from your Last.fm history",
            iconRes = R.drawable.rounded_people_24,
            isLoading = uiState.isSearchingTopSeeds || uiState.isSearchingSeed,
            accentColor = colors.second,
            accentContainer = colors.first,
            onAccentContainer = colors.second,
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                keyboardController?.hide()
                viewModel.loadTopArtistsForSeed()
            }
        )

        if (uiState.searchArtistResults.isNotEmpty()) {
            ExpressiveSeedBoardSection(
                title = "Search Results",
                count = uiState.searchArtistResults.size,
                accentColor = colors.second
            ) {
                uiState.searchArtistResults.forEachIndexed { idx, artist ->
                    val selected = uiState.seedArtistInput.equals(artist, ignoreCase = true)
                    ExpressiveSeedBoardItem(
                        index = idx,
                        title = artist,
                        subtitle = "Last.fm Artist",
                        iconRes = R.drawable.rounded_people_24,
                        isSelected = selected,
                        accentColor = colors.second,
                        accentContainer = colors.first,
                        onAccent = colors.first,
                        onAccentContainer = colors.second,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            viewModel.setSeedArtistInput(artist)
                        }
                    )
                }
            }
        }

        if (uiState.topArtistsForSeed.isNotEmpty()) {
            ExpressiveSeedBoardSection(
                title = "My Top Artists",
                count = uiState.topArtistsForSeed.size,
                accentColor = colors.second
            ) {
                uiState.topArtistsForSeed.forEachIndexed { idx, artist ->
                    val selected = uiState.seedArtistInput.equals(artist, ignoreCase = true)
                    ExpressiveSeedBoardItem(
                        index = idx,
                        title = artist,
                        subtitle = "Top Last.fm Artist",
                        iconRes = R.drawable.rounded_people_24,
                        isSelected = selected,
                        accentColor = colors.second,
                        accentContainer = colors.first,
                        onAccent = colors.first,
                        onAccentContainer = colors.second,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            viewModel.setSeedArtistInput(artist)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun TagInputs(
    uiState: SmartMixUiState,
    viewModel: SmartMixViewModel,
    keyboardController: androidx.compose.ui.platform.SoftwareKeyboardController?,
    colors: Pair<Color, Color>
) {
    val suggestions = listOf("pop", "rock", "hip-hop", "electronic", "jazz", "lofi", "metal", "indie", "classical", "r&b", "ambient", "punk")

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        ExpressiveSeedInputCard(
            value = uiState.tagInput,
            onValueChange = { viewModel.setTagInput(it) },
            label = "Genre / Tag",
            placeholder = "e.g. lofi, rock, jazz…",
            iconRes = R.drawable.rounded_sell_24,
            badgeShape = AiExpressiveShapeType.SUNNY_COOKIE,
            morphedBadgeShape = AiExpressiveShapeType.FOUR_HEAD_COOKIE,
            accentColor = colors.second,
            accentContainer = colors.first,
            onSearch = { keyboardController?.hide() }
        )

        Text(
            text = "Suggested Genres",
            style = MaterialTheme.typography.labelLarge.copy(
                fontFamily = GoogleSansRounded,
                fontWeight = FontWeight.Bold
            ),
            color = colors.second
        )

        val haptic = LocalHapticFeedback.current
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            mainAxisSpacing = 10.dp,
            crossAxisSpacing = 12.dp
        ) {
            for (tag in suggestions) {
                DailyMixMorphChip(
                    label = tag,
                    selected = uiState.tagInput.equals(tag, ignoreCase = true),
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        viewModel.setTagInput(tag)
                    },
                    selectedContainerColor = colors.second,
                    selectedContentColor = colors.first
                )
            }
        }
    }
}

@Composable
private fun SeedSearchResultItem(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    colors: Pair<Color, Color>
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = AbsoluteSmoothCornerShape(16.dp, 60),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shadowElevation = 0.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(R.drawable.rounded_music_note_24),
                contentDescription = null,
                tint = colors.second,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun GenerationProgressDialog(
    progress: String
) {
    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 6.dp,
            modifier = Modifier
                .width(280.dp)
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "Generating Mix",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = GoogleSansRounded,
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = progress,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

// Custom flow row layout
@Composable
private fun FlowRow(
    modifier: Modifier = Modifier,
    mainAxisSpacing: androidx.compose.ui.unit.Dp = 0.dp,
    crossAxisSpacing: androidx.compose.ui.unit.Dp = 0.dp,
    content: @Composable () -> Unit
) {
    androidx.compose.ui.layout.Layout(
        content = content,
        modifier = modifier
    ) { measurables, constraints ->
        val placeables = measurables.map { it.measure(constraints) }
        val layoutWidth = constraints.maxWidth

        val rows = mutableListOf<List<androidx.compose.ui.layout.Placeable>>()
        var currentRow = mutableListOf<androidx.compose.ui.layout.Placeable>()
        var currentRowWidth = 0

        placeables.forEach { placeable ->
            if (currentRowWidth + placeable.width > layoutWidth && currentRow.isNotEmpty()) {
                rows.add(currentRow)
                currentRow = mutableListOf()
                currentRowWidth = 0
            }
            currentRow.add(placeable)
            currentRowWidth += placeable.width + mainAxisSpacing.roundToPx()
        }
        if (currentRow.isNotEmpty()) {
            rows.add(currentRow)
        }

        var height = 0
        rows.forEachIndexed { index, row ->
            val rowHeight = row.maxOfOrNull { it.height } ?: 0
            height += rowHeight
            if (index < rows.size - 1) {
                height += crossAxisSpacing.roundToPx()
            }
        }

        layout(layoutWidth, height.coerceAtLeast(0)) {
            var y = 0
            rows.forEach { row ->
                var x = 0
                val rowHeight = row.maxOfOrNull { it.height } ?: 0
                row.forEach { placeable ->
                    placeable.placeRelative(x, y + (rowHeight - placeable.height) / 2)
                    x += placeable.width + mainAxisSpacing.roundToPx()
                }
                y += rowHeight + crossAxisSpacing.roundToPx()
            }
        }
    }
}

private data class ModeItemData(
    val id: String,
    val title: String,
    val desc: String,
    val iconRes: Int
)
