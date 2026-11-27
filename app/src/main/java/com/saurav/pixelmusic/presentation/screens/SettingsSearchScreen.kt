@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class
)
package com.saurav.pixelmusic.presentation.screens

import com.saurav.pixelmusic.presentation.components.expressiveTapColorWave

import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.saurav.pixelmusic.R
import com.saurav.pixelmusic.presentation.components.AiExpressiveShapeType
import com.saurav.pixelmusic.presentation.components.drawAiMorphShape
import com.saurav.pixelmusic.presentation.components.expressiveListItemScrollAnimation
import com.saurav.pixelmusic.presentation.model.RecentSearchesStore
import com.saurav.pixelmusic.presentation.model.SearchableSetting
import com.saurav.pixelmusic.presentation.model.SettingsSearchCatalog
import com.saurav.pixelmusic.presentation.navigation.Screen
import com.saurav.pixelmusic.presentation.navigation.navigateSafely
import com.saurav.pixelmusic.ui.modifiers.scrollMotionBlur
import com.saurav.pixelmusic.ui.theme.GoogleSansRounded
import kotlinx.coroutines.delay

private data class QuickSearchPillItem(
    val label: String,
    val query: String,
    @DrawableRes val iconRes: Int,
    val baseShape: AiExpressiveShapeType,
    val morphedShape: AiExpressiveShapeType
)

@Composable
fun SettingsSearchScreen(
    navController: NavController,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    var query by rememberSaveable { mutableStateOf("") }
    var recentSearches by remember { mutableStateOf(RecentSearchesStore.load(context)) }
    val focusRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current

    val results = remember(query) { SettingsSearchCatalog.search(query) }
    val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    LaunchedEffect(Unit) {
        delay(180)
        focusRequester.requestFocus()
    }

    /** Navigate + persist the query. */
    fun openSetting(setting: SearchableSetting, queryUsed: String) {
        keyboard?.hide()
        val q = queryUsed.trim()
        if (q.isNotEmpty()) {
            recentSearches = RecentSearchesStore.add(context, q)
        }

        // Only use the highlight-aware category route when the destination is
        // actually a category screen. Entries whose route points at a standalone
        // screen (Device Capabilities, Experimental, Palette Style, Artist
        // Settings, About, …) must go straight to that route instead.
        val target = when {
            setting.route.startsWith("settings_category/") && setting.category != null -> {
                val routeCategoryId = setting.route.removePrefix("settings_category/").substringBefore("/")
                Screen.SettingsCategory.createRouteWithHighlight(routeCategoryId, setting.title)
            }
            setting.route == Screen.Equalizer.route -> Screen.Equalizer.createRouteWithHighlight(setting.title)
            setting.route == Screen.AudioLabFx.route -> Screen.AudioLabFx.createRouteWithHighlight(setting.title)
            setting.route == Screen.AudioLabEq.route -> Screen.AudioLabEq.createRouteWithHighlight(setting.title)
            setting.route == Screen.AudioLabStage.route -> Screen.AudioLabStage.createRouteWithHighlight(setting.title)
            else -> setting.route
        }

        // Pop the search screen so back returns cleanly to Settings
        navController.popBackStack()
        navController.navigateSafely(target)
    }

    Surface(
        color = Color.Transparent,
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            // ─── Rounded floating pill search bar ──────────────────────────────
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        top = statusBarHeight + 10.dp,
                        start = 16.dp,
                        end = 16.dp,
                        bottom = 12.dp
                    )
                    .expressiveTapColorWave(
                        waveColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
                        shape = CircleShape
                    )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 52.dp)
                        .padding(horizontal = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = stringResource(R.string.auth_cd_back),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = if (query.isEmpty()) 16.dp else 4.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        if (query.isEmpty()) {
                            Text(
                                text = "Search settings...",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        BasicTextField(
                            value = query,
                            onValueChange = { query = it },
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodyLarge.copy(
                                color = MaterialTheme.colorScheme.onSurface
                            ),
                            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(onSearch = { keyboard?.hide() }),
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(focusRequester)
                        )
                    }

                    AnimatedVisibility(
                        visible = query.isNotEmpty(),
                        enter = scaleIn(initialScale = 0.7f) + fadeIn(tween(140)),
                        exit = scaleOut(targetScale = 0.7f) + fadeOut(tween(120))
                    ) {
                        IconButton(onClick = { query = "" }) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = "Clear",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // ─── Body: quick search + recents / results / no results ───────────
            when {
                query.isBlank() -> SettingsSearchDiscoveryContent(
                    recents = recentSearches,
                    onQuerySelected = { q -> query = q },
                    onRemoveRecent = { q ->
                        recentSearches = RecentSearchesStore.remove(context, q)
                    },
                    onClearAllRecents = {
                        RecentSearchesStore.clear(context)
                        recentSearches = emptyList()
                    }
                )
                results.isEmpty() -> NoResultsHint(query)
                else -> {
                    val resultsListState = rememberLazyListState()
                    LazyColumn(
                        state = resultsListState,
                        modifier = Modifier
                            .fillMaxSize()
                            .scrollMotionBlur(resultsListState),
                        contentPadding = PaddingValues(
                            start = 16.dp,
                            end = 16.dp,
                            top = 4.dp,
                            bottom = WindowInsets.navigationBars.asPaddingValues()
                                .calculateBottomPadding() + 24.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        item {
                            Text(
                                text = "${results.size} result${if (results.size == 1) "" else "s"}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(start = 12.dp, top = 6.dp, bottom = 4.dp)
                            )
                        }
                        items(results, key = { it.title + it.route }) { setting ->
                            SearchResultRow(
                                setting = setting,
                                onClick = { openSetting(setting, query) },
                                modifier = Modifier
                                    .animateItem()
                                    .expressiveListItemScrollAnimation(resultsListState)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsSearchDiscoveryContent(
    recents: List<String>,
    onQuerySelected: (String) -> Unit,
    onRemoveRecent: (String) -> Unit,
    onClearAllRecents: () -> Unit
) {
    val quickPills = remember {
        listOf(
            // Row 1
            QuickSearchPillItem(
                label = "App Theme",
                query = "App Theme",
                iconRes = R.drawable.rounded_palette_24,
                baseShape = AiExpressiveShapeType.COOKIE,
                morphedShape = AiExpressiveShapeType.FOUR_HEAD_COOKIE
            ),
            QuickSearchPillItem(
                label = "Crossfade",
                query = "Crossfade",
                iconRes = R.drawable.rounded_shuffle_24,
                baseShape = AiExpressiveShapeType.FOUR_HEAD_COOKIE,
                morphedShape = AiExpressiveShapeType.SUNNY_COOKIE
            ),
            QuickSearchPillItem(
                label = "Equalizer",
                query = "Equalizer",
                iconRes = R.drawable.rounded_graphic_eq_24,
                baseShape = AiExpressiveShapeType.CIRCLE,
                morphedShape = AiExpressiveShapeType.SIX_COOKIE
            ),
            QuickSearchPillItem(
                label = "App Icon",
                query = "App Icon",
                iconRes = R.drawable.rounded_grid_view_24,
                baseShape = AiExpressiveShapeType.ROUNDED_SQUARE,
                morphedShape = AiExpressiveShapeType.COOKIE
            ),
            // Row 2
            QuickSearchPillItem(
                label = "Lyrics",
                query = "Lyrics",
                iconRes = R.drawable.rounded_lyrics_24,
                baseShape = AiExpressiveShapeType.ROUNDED_SQUARE,
                morphedShape = AiExpressiveShapeType.COOKIE
            ),
            QuickSearchPillItem(
                label = "Dynamic Island",
                query = "Dynamic Island",
                iconRes = R.drawable.rounded_auto_awesome_24,
                baseShape = AiExpressiveShapeType.FOUR_HEAD_COOKIE,
                morphedShape = AiExpressiveShapeType.CIRCLE
            ),
            QuickSearchPillItem(
                label = "Hi-Fi Mode",
                query = "Hi-Fi Mode",
                iconRes = R.drawable.rounded_headphones_24,
                baseShape = AiExpressiveShapeType.CIRCLE,
                morphedShape = AiExpressiveShapeType.FOUR_HEAD_COOKIE
            ),
            QuickSearchPillItem(
                label = "AOD Screen",
                query = "AOD",
                iconRes = R.drawable.rounded_dark_mode_24,
                baseShape = AiExpressiveShapeType.SIX_COOKIE,
                morphedShape = AiExpressiveShapeType.ROUNDED_TRIANGLE
            ),
            // Row 3
            QuickSearchPillItem(
                label = "App Language",
                query = "App Language",
                iconRes = R.drawable.rounded_language_24,
                baseShape = AiExpressiveShapeType.FOUR_HEAD_COOKIE,
                morphedShape = AiExpressiveShapeType.SUNNY_COOKIE
            ),
            QuickSearchPillItem(
                label = "AI API Key",
                query = "AI",
                iconRes = R.drawable.rounded_science_24,
                baseShape = AiExpressiveShapeType.COOKIE,
                morphedShape = AiExpressiveShapeType.ROUNDED_SQUARE
            ),
            QuickSearchPillItem(
                label = "Backup",
                query = "Backup",
                iconRes = R.drawable.rounded_restore_24,
                baseShape = AiExpressiveShapeType.ROUNDED_TRIANGLE,
                morphedShape = AiExpressiveShapeType.SIX_COOKIE
            ),
            QuickSearchPillItem(
                label = "Wallpaper",
                query = "Wallpaper",
                iconRes = R.drawable.rounded_imagesmode_24,
                baseShape = AiExpressiveShapeType.SUNNY_COOKIE,
                morphedShape = AiExpressiveShapeType.FOUR_HEAD_COOKIE
            )
        )
    }

    val quickRows = remember(quickPills) {
        listOf(
            quickPills.subList(0, 4),
            quickPills.subList(4, 8),
            quickPills.subList(8, 12)
        )
    }

    val verticalScroll = rememberScrollState()
    val horizontalPillsScroll = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(verticalScroll)
            .padding(
                top = 8.dp,
                bottom = WindowInsets.navigationBars.asPaddingValues()
                    .calculateBottomPadding() + 24.dp
            )
    ) {
        if (recents.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Recent searches",
                    style = MaterialTheme.typography.titleSmall,
                    fontFamily = GoogleSansRounded,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "Clear",
                    style = MaterialTheme.typography.labelLarge,
                    fontFamily = GoogleSansRounded,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .clip(CircleShape)
                        .clickable(onClick = onClearAllRecents)
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(recents, key = { it }) { recentQuery ->
                    RecentSearchPill(
                        query = recentQuery,
                        onClick = { onQuerySelected(recentQuery) },
                        onRemove = { onRemoveRecent(recentQuery) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(22.dp))
        }

        Text(
            text = "Try searching for",
            style = MaterialTheme.typography.titleSmall,
            fontFamily = GoogleSansRounded,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
        )

        Spacer(modifier = Modifier.height(6.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(horizontalPillsScroll)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            quickRows.forEach { rowItems ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    rowItems.forEach { item ->
                        QuickSearchExpressivePill(
                            item = item,
                            onClick = { onQuerySelected(item.query) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RecentSearchPill(
    query: String,
    onClick: () -> Unit,
    onRemove: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.94f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "recent_pill_scale"
    )

    Surface(
        onClick = onClick,
        interactionSource = interactionSource,
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier
            .height(44.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
    ) {
        Row(
            modifier = Modifier.padding(start = 6.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer)
            ) {
                Icon(
                    painter = painterResource(R.drawable.rounded_history_24),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Text(
                text = query,
                style = MaterialTheme.typography.bodyMedium,
                fontFamily = GoogleSansRounded,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.width(6.dp))

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(26.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onRemove)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = "Remove",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(15.dp)
                )
            }
        }
    }
}

@Composable
private fun QuickSearchExpressivePill(
    item: QuickSearchPillItem,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.94f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "quick_pill_scale"
    )
    val morphFraction by animateFloatAsState(
        targetValue = if (isPressed) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "quick_pill_morph"
    )
    val containerColor by animateColorAsState(
        targetValue = if (isPressed) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainerHigh
        },
        animationSpec = tween(160),
        label = "quick_pill_bg"
    )
    val badgeColor by animateColorAsState(
        targetValue = if (isPressed) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.secondaryContainer
        },
        animationSpec = tween(160),
        label = "quick_pill_badge_bg"
    )
    val iconTint by animateColorAsState(
        targetValue = if (isPressed) {
            MaterialTheme.colorScheme.onPrimary
        } else {
            MaterialTheme.colorScheme.onSecondaryContainer
        },
        animationSpec = tween(160),
        label = "quick_pill_icon_tint"
    )

    Surface(
        onClick = onClick,
        interactionSource = interactionSource,
        shape = CircleShape,
        color = containerColor,
        modifier = Modifier
            .height(48.dp)
            .wrapContentWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
    ) {
        Row(
            modifier = Modifier
                .height(48.dp)
                .wrapContentWidth()
                .padding(start = 7.dp, end = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(34.dp)
                    .drawAiMorphShape(
                        from = item.baseShape,
                        to = item.morphedShape,
                        morphProgress = { morphFraction },
                        brush = Brush.linearGradient(listOf(badgeColor, badgeColor))
                    )
            ) {
                Icon(
                    painter = painterResource(item.iconRes),
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Text(
                text = item.label,
                style = MaterialTheme.typography.bodyMedium,
                fontFamily = GoogleSansRounded,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun SearchResultRow(
    setting: SearchableSetting,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = setting.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontFamily = GoogleSansRounded,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = setting.subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            setting.category?.let { cat ->
                Spacer(modifier = Modifier.width(8.dp))
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = CircleShape
                ) {
                    Text(
                        text = stringResource(cat.titleRes),
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        maxLines = 1,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun NoResultsHint(query: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "No results for \"$query\"",
                style = MaterialTheme.typography.titleMedium,
                fontFamily = GoogleSansRounded,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Try a different keyword.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
