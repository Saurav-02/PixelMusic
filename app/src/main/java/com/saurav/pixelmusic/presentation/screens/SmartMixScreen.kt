package com.saurav.pixelmusic.presentation.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
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
import com.saurav.pixelmusic.presentation.components.MiniPlayerHeight
import com.saurav.pixelmusic.presentation.components.resolveNavBarOccupiedHeight
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
    ) { innerPadding ->
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
                GenerationProgressDialog(progress = uiState.generationProgress)
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
                    imageVector = Icons.Rounded.AutoAwesome,
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
                    Icon(Icons.Rounded.Link, contentDescription = null)
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
    val playerStableState by playerViewModel.stablePlayerState.collectAsStateWithLifecycle()
    val navBarCompactMode by playerViewModel.navBarCompactMode.collectAsStateWithLifecycle()
    val isPlayerActive = playerStableState.currentSong != null

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

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 8.dp, bottom = fabAreaHeight + 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
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
                    modifier = Modifier.padding(start = 4.dp, top = 12.dp, bottom = 4.dp)
                )
            }

            // 2. Mode items — same expressive list style as Settings
            val modes = listOf(
                ModeItemData("top",             "Top Tracks",       "Your most played tracks of all time",      Icons.Rounded.Leaderboard),
                ModeItemData("recent",          "Recent Tracks",    "What you've been listening to lately",     Icons.Rounded.History),
                ModeItemData("similar-tracks",  "Similar Tracks",   "Tracks similar to one you love",           Icons.Rounded.MusicNote),
                ModeItemData("similar-artists", "Similar Artists",  "Discover artists like your favourites",    Icons.Rounded.People),
                ModeItemData("tag",             "By Tag / Genre",   "Browse by genre like rock, lofi, jazz",    Icons.Rounded.Sell),
                ModeItemData("mix",             "My Mix",           "Smart blend of top, recent & similar",     Icons.Rounded.Shuffle),
                ModeItemData("recommendations", "My Recommendations","30 fresh tracks tailored for you",       Icons.Rounded.AutoAwesome),
                ModeItemData("library",         "My Library",       "Re-discover the sounds of your past",      Icons.Rounded.LibraryMusic)
            )

            val total = modes.size
            modes.forEachIndexed { idx, mode ->
                item(key = mode.id) {
                    val isSelected = uiState.selectedMode == mode.id
                    val colors = modeColors[mode.id]!!

                    // Dynamic scale bounce micro-interaction on selection
                    val cardScale by animateFloatAsState(
                        targetValue = if (isSelected) 1.02f else 1.0f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow
                        ),
                        label = "card_scale"
                    )

                    val itemShape = remember { AbsoluteSmoothCornerShape(22.dp, 60) }

                    val cardBgColor = if (isSelected) {
                        MaterialTheme.colorScheme.surfaceContainerHigh
                    } else {
                        MaterialTheme.colorScheme.surfaceContainerLow
                    }

                    val borderStroke = if (isSelected) {
                        BorderStroke(
                            width = 2.dp,
                            brush = Brush.linearGradient(
                                colors = listOf(colors.second, colors.second.copy(alpha = 0.5f))
                            )
                        )
                    } else {
                        BorderStroke(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                        )
                    }

                    Surface(
                        onClick = {
                            if (isSelected) viewModel.setSelectedMode(null)
                            else viewModel.setSelectedMode(mode.id)
                        },
                        shape = itemShape,
                        color = cardBgColor,
                        border = borderStroke,
                        modifier = Modifier
                            .fillMaxWidth()
                            .graphicsLayer {
                                scaleX = cardScale
                                scaleY = cardScale
                            }
                    ) {
                        val overlayModifier = if (isSelected) {
                            Modifier.background(colors.second.copy(alpha = 0.05f))
                        } else {
                            Modifier
                        }
                        Column(modifier = overlayModifier) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                            ) {
                                // Premium Squircle Icon Container with dynamic inversion
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .size(52.dp)
                                        .clip(AbsoluteSmoothCornerShape(16.dp, 60))
                                        .background(
                                            if (isSelected) {
                                                Brush.verticalGradient(
                                                    colors = listOf(
                                                        colors.second,
                                                        colors.second.copy(alpha = 0.8f)
                                                    )
                                                )
                                            } else {
                                                Brush.verticalGradient(
                                                    colors = listOf(
                                                        MaterialTheme.colorScheme.surfaceContainerHigh,
                                                        MaterialTheme.colorScheme.surfaceContainerHigh
                                                    )
                                                )
                                            }
                                        )
                                        .border(
                                            width = 1.dp,
                                            color = (if (isSelected) colors.first else MaterialTheme.colorScheme.outlineVariant).copy(alpha = 0.2f),
                                            shape = AbsoluteSmoothCornerShape(16.dp, 60)
                                        )
                                ) {
                                    Icon(
                                        imageVector = mode.icon,
                                        contentDescription = null,
                                        modifier = Modifier.size(24.dp),
                                        tint = if (isSelected) colors.first
                                               else MaterialTheme.colorScheme.onSurfaceVariant
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

                                // Animated chevron / collapse icon matching the dynamic color scheme
                                val iconRotation by animateFloatAsState(
                                    targetValue = if (isSelected) 90f else 0f,
                                    animationSpec = tween(200),
                                    label = "chevron_rotation"
                                )
                                Icon(
                                    imageVector = Icons.Rounded.ChevronRight,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(22.dp)
                                        .graphicsLayer { rotationZ = iconRotation },
                                    tint = if (isSelected) colors.second
                                           else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            // Inline expanded options styled symmetrically
                            AnimatedVisibility(
                                visible = isSelected,
                                enter = expandVertically(tween(220)) + fadeIn(tween(220)),
                                exit  = shrinkVertically(tween(180)) + fadeOut(tween(180))
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
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            ) + fadeIn(),
            exit = slideOutVertically(
                targetOffsetY = { it },
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            ) + fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
        ) {
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
                Button(
                    onClick = { keyboardController?.hide(); viewModel.generatePlaylist() },
                    enabled = isGenerateEnabled,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = AbsoluteSmoothCornerShape(28.dp, 60),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                    ),
                    elevation = ButtonDefaults.buttonElevation(
                        defaultElevation = 6.dp,
                        pressedElevation = 2.dp,
                        disabledElevation = 0.dp
                    )
                ) {
                    Icon(
                        imageVector = Icons.Rounded.AutoAwesome,
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

@Composable
private fun TrackCountSliderCard(
    count: Int,
    onCountChange: (Int) -> Unit
) {
    val shape = remember { AbsoluteSmoothCornerShape(22.dp, 60) }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
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
                
                Surface(
                    shape = AbsoluteSmoothCornerShape(12.dp, 60),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    Text(
                        text = "$count",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = GoogleSansRounded,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Slider(
                value = count.toFloat(),
                onValueChange = { onCountChange(it.toInt()) },
                valueRange = 5f..35f,
                steps = 30,
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary,
                    inactiveTrackColor = MaterialTheme.colorScheme.outlineVariant
                )
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                listOf("5", "15", "25", "35").forEach { label ->
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold
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
    val periods = listOf(
        "overall" to "All Time",
        "12month" to "12 Months",
        "6month" to "6 Months",
        "3month" to "3 Months",
        "1month" to "1 Month",
        "7day" to "7 Days"
    )

    Text(
        text = "Select Time Period",
        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
        color = colors.second,
        modifier = Modifier.padding(bottom = 8.dp)
    )

    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        items(periods) { (id, label) ->
            val active = selectedPeriod == id
            FilterChip(
                selected = active,
                onClick = { onPeriodSelect(id) },
                label = { Text(label, fontFamily = GoogleSansRounded) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = colors.second,
                    selectedLabelColor = colors.first,
                    containerColor = colors.second.copy(alpha = 0.15f),
                    labelColor = colors.second
                ),
                shape = AbsoluteSmoothCornerShape(12.dp, 60),
                border = null
            )
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
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OutlinedTextField(
            value = uiState.seedTrackName,
            onValueChange = { viewModel.setSeedTrackName(it) },
            label = { Text("Seed Track Name", fontFamily = GoogleSansRounded) },
            placeholder = { Text("Enter track name...", fontFamily = GoogleSansRounded) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = AbsoluteSmoothCornerShape(16.dp, 60),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = {
                keyboardController?.hide()
                viewModel.searchSeedTrack()
            }),
            trailingIcon = {
                IconButton(onClick = {
                    keyboardController?.hide()
                    viewModel.searchSeedTrack()
                }) {
                    Icon(Icons.Rounded.Search, contentDescription = "Search", tint = colors.second)
                }
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = colors.second,
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
                focusedLabelColor = colors.second,
                unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                cursorColor = colors.second,
                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow
            )
        )

        OutlinedTextField(
            value = uiState.seedArtistName,
            onValueChange = { viewModel.setSeedArtistName(it) },
            label = { Text("Seed Artist Name (Optional)", fontFamily = GoogleSansRounded) },
            placeholder = { Text("Enter artist name...", fontFamily = GoogleSansRounded) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = AbsoluteSmoothCornerShape(16.dp, 60),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = colors.second,
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
                focusedLabelColor = colors.second,
                unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                cursorColor = colors.second,
                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow
            )
        )

        Button(
            onClick = {
                keyboardController?.hide()
                viewModel.loadTopTracksForSeed()
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = colors.second,
                contentColor = colors.first
            ),
            shape = AbsoluteSmoothCornerShape(16.dp, 60),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Rounded.LibraryMusic, contentDescription = null, tint = colors.first)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Pick From My Top Tracks", fontFamily = GoogleSansRounded, fontWeight = FontWeight.Bold)
        }

        if (uiState.isSearchingTopSeeds || uiState.isSearchingSeed) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = colors.second, modifier = Modifier.size(24.dp))
            }
        }

        if (uiState.searchTrackResults.isNotEmpty()) {
            Text(
                text = "Search Results",
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                color = colors.second
            )
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                for (track in uiState.searchTrackResults) {
                    SeedSearchResultItem(
                        title = track.name,
                        subtitle = track.artist,
                        onClick = {
                            viewModel.setSeedTrackName(track.name)
                            viewModel.setSeedArtistName(track.artist)
                        },
                        colors = colors
                    )
                }
            }
        }

        if (uiState.topTracksForSeed.isNotEmpty()) {
            Text(
                text = "My Top Tracks",
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                color = colors.second
            )
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                for (track in uiState.topTracksForSeed) {
                    SeedSearchResultItem(
                        title = track.name,
                        subtitle = track.artist,
                        onClick = {
                            viewModel.setSeedTrackName(track.name)
                            viewModel.setSeedArtistName(track.artist)
                        },
                        colors = colors
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
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OutlinedTextField(
            value = uiState.seedArtistInput,
            onValueChange = { viewModel.setSeedArtistInput(it) },
            label = { Text("Seed Artist Name", fontFamily = GoogleSansRounded) },
            placeholder = { Text("Enter artist name...", fontFamily = GoogleSansRounded) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = AbsoluteSmoothCornerShape(16.dp, 60),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = {
                keyboardController?.hide()
                viewModel.searchSeedArtist()
            }),
            trailingIcon = {
                IconButton(onClick = {
                    keyboardController?.hide()
                    viewModel.searchSeedArtist()
                }) {
                    Icon(Icons.Rounded.Search, contentDescription = "Search", tint = colors.second)
                }
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = colors.second,
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
                focusedLabelColor = colors.second,
                unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                cursorColor = colors.second,
                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow
            )
        )

        Button(
            onClick = {
                keyboardController?.hide()
                viewModel.loadTopArtistsForSeed()
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = colors.second,
                contentColor = colors.first
            ),
            shape = AbsoluteSmoothCornerShape(16.dp, 60),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Rounded.People, contentDescription = null, tint = colors.first)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Pick From My Top Artists", fontFamily = GoogleSansRounded, fontWeight = FontWeight.Bold)
        }

        if (uiState.isSearchingTopSeeds || uiState.isSearchingSeed) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = colors.second, modifier = Modifier.size(24.dp))
            }
        }

        if (uiState.searchArtistResults.isNotEmpty()) {
            Text(
                text = "Search Results",
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                color = colors.second
            )
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                for (artist in uiState.searchArtistResults) {
                    SeedSearchResultItem(
                        title = artist,
                        subtitle = "Artist",
                        onClick = {
                            viewModel.setSeedArtistInput(artist)
                        },
                        colors = colors
                    )
                }
            }
        }

        if (uiState.topArtistsForSeed.isNotEmpty()) {
            Text(
                text = "My Top Artists",
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary
            )
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                for (artist in uiState.topArtistsForSeed) {
                    SeedSearchResultItem(
                        title = artist,
                        subtitle = "Artist",
                        onClick = {
                            viewModel.setSeedArtistInput(artist)
                        },
                        colors = colors
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
        OutlinedTextField(
            value = uiState.tagInput,
            onValueChange = { viewModel.setTagInput(it) },
            label = { Text("Genre / Tag", fontFamily = GoogleSansRounded) },
            placeholder = { Text("e.g. lofi, rock...", fontFamily = GoogleSansRounded) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = AbsoluteSmoothCornerShape(16.dp, 60),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { keyboardController?.hide() }),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = colors.second,
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
                focusedLabelColor = colors.second,
                unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                cursorColor = colors.second,
                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow
            )
        )

        Text(
            text = "Suggested Genres",
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
            color = colors.second
        )

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            mainAxisSpacing = 8.dp,
            crossAxisSpacing = 8.dp
        ) {
            for (tag in suggestions) {
                val active = uiState.tagInput.lowercase() == tag
                FilterChip(
                    selected = active,
                    onClick = { viewModel.setTagInput(tag) },
                    label = { Text(tag, fontFamily = GoogleSansRounded) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = colors.second,
                        selectedLabelColor = colors.first,
                        containerColor = colors.second.copy(alpha = 0.15f),
                        labelColor = colors.second
                    ),
                    shape = AbsoluteSmoothCornerShape(12.dp, 60),
                    border = null
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
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Rounded.MusicNote,
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
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)
