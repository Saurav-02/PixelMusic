package com.saurav.pixelmusic.presentation.components.subcomps

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Search
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import com.saurav.pixelmusic.presentation.components.SmoothModalBottomSheet
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.saurav.pixelmusic.R
import com.saurav.pixelmusic.data.model.Lyrics
import com.saurav.pixelmusic.lyrics.PixelMusicLyricsPreferences
import com.saurav.pixelmusic.presentation.components.ToggleSegmentButton
import androidx.compose.ui.text.style.TextOverflow
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun LyricsMoreBottomSheet(
    onDismissRequest: () -> Unit,
    sheetState: SheetState,
    lyrics: Lyrics?,
    showSyncedLyrics: Boolean,
    isSyncControlsVisible: Boolean,
    onManualSearchLyrics: () -> Unit = {},
    onSaveLyricsAsLrc: () -> Unit,
    onResetImportedLyrics: () -> Unit,
    onToggleSyncControls: () -> Unit,
    isImmersiveTemporarilyDisabled: Boolean,
    onSetImmersiveTemporarilyDisabled: (Boolean) -> Unit,
    keepScreenOn: Boolean,
    onKeepScreenOnChange: (Boolean) -> Unit,
    lyricsAlignment: String,
    onLyricsAlignmentChange: (String) -> Unit,
    hasTranslatedLyrics: Boolean,
    hasRomanizedLyrics: Boolean,
    showTranslation: Boolean,
    showRomanization: Boolean,
    onShowTranslationChange: (Boolean) -> Unit,
    onShowRomanizationChange: (Boolean) -> Unit,
    immersiveLyricsEnabled: Boolean,
    // BottomToggleRow params
    isShuffleEnabled: Boolean,
    repeatMode: Int,
    isFavoriteProvider: () -> Boolean,
    onShuffleToggle: () -> Unit,
    onRepeatToggle: () -> Unit,
    onFavoriteToggle: () -> Unit,
    // Colors
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainerLow,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    accentColor: Color = MaterialTheme.colorScheme.primary,
    onAccentColor: Color = MaterialTheme.colorScheme.onPrimary,
    tertiaryColor: Color = MaterialTheme.colorScheme.tertiary,
    onTertiaryColor: Color = MaterialTheme.colorScheme.onTertiary
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val navigationBarsPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    var showResetDialog by remember { mutableStateOf(false) }
    var showLanguagePicker by remember { mutableStateOf(false) }

    val autoTranslate by remember(context) {
        PixelMusicLyricsPreferences.autoTranslateFlow(context)
    }.collectAsStateWithLifecycle(initialValue = false)
    val translateLang by remember(context) {
        PixelMusicLyricsPreferences.translateLanguageFlow(context)
    }.collectAsStateWithLifecycle(initialValue = "en")

    SmoothModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = containerColor,
        contentColor = contentColor,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = navigationBarsPadding + 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // No Title - "Expressive" relies on visual grouping

            val itemBackgroundColor = contentColor.copy(alpha = 0.08f)

            // Lyrics Actions Group
            Column(
                verticalArrangement = Arrangement.spacedBy(2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    modifier = Modifier
                        .padding(start = 6.dp, bottom = 6.dp),
                    text = stringResource(R.string.lyrics),
                    color = accentColor,
                    style = MaterialTheme.typography.bodyLargeEmphasized
                )
                // Search lyrics manually (Song ~ Artist)
                ListItem(
                    headlineContent = { Text("Search lyrics manually") },
                    supportingContent = { Text("Find lyrics by Song ~ Artist name") },
                    leadingContent = {
                        Icon(
                            imageVector = Icons.Rounded.Search,
                            contentDescription = null
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = 8.dp, bottomEnd = 8.dp))
                        .background(itemBackgroundColor)
                        .clickable {
                            onDismissRequest()
                            onManualSearchLyrics()
                        },
                    colors = ListItemDefaults.colors(
                        containerColor = Color.Transparent,
                        headlineColor = contentColor,
                        supportingColor = contentColor.copy(alpha = 0.7f),
                        leadingIconColor = contentColor
                    )
                )

                // Save lyrics to .lrc
                if (lyrics != null) {
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.save_lyrics_dialog_title).substringBefore("?")) },
                        leadingContent = {
                            Icon(
                                painter = painterResource(R.drawable.outline_save_24),
                                contentDescription = null
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(itemBackgroundColor)
                            .clickable {
                                onDismissRequest()
                                onSaveLyricsAsLrc()
                            },
                        colors = ListItemDefaults.colors(
                            containerColor = Color.Transparent,
                            headlineColor = contentColor,
                            leadingIconColor = contentColor
                        )
                    )
                }

                // Reset imported lyrics
                val resetShape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp, bottomStart = 18.dp, bottomEnd = 18.dp)

                ListItem(
                    headlineContent = { Text(stringResource(R.string.reset_imported_lyrics)) },
                    leadingContent = {
                        Icon(
                            painter = painterResource(R.drawable.outline_restart_alt_24),
                            contentDescription = null
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(resetShape)
                        .background(itemBackgroundColor)
                        .clickable {
                            showResetDialog = true
                        },
                    colors = ListItemDefaults.colors(
                        containerColor = Color.Transparent,
                        headlineColor = contentColor,
                        leadingIconColor = contentColor
                    )
                )
            }

            if (showResetDialog) {
                androidx.compose.material3.AlertDialog(
                    onDismissRequest = { showResetDialog = false },
                    title = { Text(stringResource(R.string.lyrics_more_dialog_reset_title)) },
                    text = { Text(stringResource(R.string.lyrics_more_dialog_reset_message)) },
                    confirmButton = {
                        androidx.compose.material3.TextButton(
                            onClick = {
                                showResetDialog = false
                                onDismissRequest()
                                onResetImportedLyrics()
                            }
                        ) {
                            Text(stringResource(R.string.action_reset), color = MaterialTheme.colorScheme.error, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    },
                    dismissButton = {
                        androidx.compose.material3.TextButton(
                            onClick = { showResetDialog = false }
                        ) {
                            Text(stringResource(R.string.cancel), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    textContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            }

            // Appearance Group
            Column(
                verticalArrangement = Arrangement.spacedBy(2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    modifier = Modifier
                        .padding(start = 6.dp, bottom = 6.dp),
                    text = stringResource(R.string.lyrics_more_appearance),
                    color = accentColor,
                    style = MaterialTheme.typography.bodyLargeEmphasized
                 )
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(itemBackgroundColor)
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = stringResource(R.string.lyrics_more_alignment),
                        color = contentColor,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ToggleSegmentButton(
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            active = lyricsAlignment == "left",
                            activeColor = accentColor,
                            inactiveColor = containerColor,
                            activeContentColor = onAccentColor,
                            inactiveContentColor = contentColor.copy(alpha = 0.78f),
                            activeCornerRadius = 50.dp,
                            onClick = { onLyricsAlignmentChange("left") },
                            imageVector = ImageVector.vectorResource(R.drawable.rounded_format_align_left_24),
                            contentDesc = stringResource(R.string.cd_lyrics_align_left)
                        )

                        ToggleSegmentButton(
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            active = lyricsAlignment == "center",
                            activeColor = accentColor,
                            inactiveColor = containerColor,
                            activeContentColor = onAccentColor,
                            inactiveContentColor = contentColor.copy(alpha = 0.78f),
                            activeCornerRadius = 50.dp,
                            onClick = { onLyricsAlignmentChange("center") },
                            imageVector = ImageVector.vectorResource(R.drawable.rounded_align_justify_space_even_24),
                            contentDesc = stringResource(R.string.cd_lyrics_align_center)
                        )

                        ToggleSegmentButton(
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            active = lyricsAlignment == "right",
                            activeColor = accentColor,
                            inactiveColor = containerColor,
                            activeContentColor = onAccentColor,
                            inactiveContentColor = contentColor.copy(alpha = 0.78f),
                            activeCornerRadius = 50.dp,
                            onClick = { onLyricsAlignmentChange("right") },
                            imageVector = ImageVector.vectorResource(R.drawable.rounded_format_align_right_24),
                            contentDesc = stringResource(R.string.cd_lyrics_align_right)
                        )
                    }
                }
            }

            // Control Settings Group
            val isSyncVisible = showSyncedLyrics
            val isRomanizationVisible = hasRomanizedLyrics
            val isAutoTranslateVisible = true
            val isTranslateLangVisible = autoTranslate
            val isImmersiveVisible = showSyncedLyrics && immersiveLyricsEnabled
            val isKeepScreenOnVisible = true

            if (isSyncVisible || isRomanizationVisible || isAutoTranslateVisible || isKeepScreenOnVisible) {
                // Determine first and last items for rounding
                val isRomanizationFirst = isRomanizationVisible && !isSyncVisible
                val isAutoTranslateFirst = !isSyncVisible && !isRomanizationVisible

                val isSyncLast = false
                val isRomanizationLast = false
                val isImmersiveLast = isImmersiveVisible && !isKeepScreenOnVisible

                Column(
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        modifier = Modifier
                            .padding(start = 6.dp, bottom = 6.dp),
                        text = stringResource(R.string.lyrics_more_controls),
                        color = accentColor,
                        style = MaterialTheme.typography.bodyLargeEmphasized
                    )

                    if (isSyncVisible) {
                        ListItem(
                            headlineContent = {
                                Text(
                                    if (isSyncControlsVisible) {
                                        stringResource(R.string.lyrics_more_hide_sync_controls)
                                    } else {
                                        stringResource(R.string.lyrics_more_adjust_sync)
                                    }
                                )
                            },
                            leadingContent = {
                                Icon(
                                    painter = painterResource(R.drawable.rounded_tune_24),
                                    contentDescription = null
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(
                                    RoundedCornerShape(
                                        topStart = 18.dp,
                                        topEnd = 18.dp,
                                        bottomStart = if (isSyncLast) 24.dp else 8.dp,
                                        bottomEnd = if (isSyncLast) 24.dp else 8.dp
                                    )
                                )
                                .background(itemBackgroundColor)
                                .clickable {
                                    onDismissRequest()
                                    onToggleSyncControls()
                                },
                            colors = ListItemDefaults.colors(
                                containerColor = Color.Transparent,
                                headlineColor = contentColor,
                                leadingIconColor = contentColor
                            )
                        )
                    }

                    if (isRomanizationVisible) {
                        ListItem(
                            headlineContent = { Text(stringResource(R.string.lyrics_more_show_romanization)) },
                            leadingContent = {
                                Icon(
                                    painter = painterResource(R.drawable.rounded_text_format_24),
                                    contentDescription = null
                                )
                            },
                            trailingContent = {
                                LyricsSheetSwitch(
                                    checked = showRomanization,
                                    onCheckedChange = onShowRomanizationChange,
                                    accentColor = accentColor,
                                    onAccentColor = onAccentColor,
                                    contentColor = contentColor
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(
                                    RoundedCornerShape(
                                        topStart = if (isRomanizationFirst) 18.dp else 8.dp,
                                        topEnd = if (isRomanizationFirst) 18.dp else 8.dp,
                                        bottomStart = if (isRomanizationLast) 24.dp else 8.dp,
                                        bottomEnd = if (isRomanizationLast) 24.dp else 8.dp
                                    )
                                )
                                .background(itemBackgroundColor)
                                .clickable { onShowRomanizationChange(!showRomanization) },
                            colors = ListItemDefaults.colors(
                                containerColor = Color.Transparent,
                                headlineColor = contentColor,
                                leadingIconColor = contentColor
                            )
                        )
                    }

                    if (isAutoTranslateVisible) {
                        ListItem(
                            headlineContent = { Text("Translation") },
                            supportingContent = { Text("Translate lyrics into your preferred language") },
                            leadingContent = {
                                Icon(
                                    painter = painterResource(R.drawable.rounded_translate_24),
                                    contentDescription = null
                                )
                            },
                            trailingContent = {
                                LyricsSheetSwitch(
                                    checked = autoTranslate,
                                    onCheckedChange = { enabled ->
                                        onShowTranslationChange(enabled)
                                        coroutineScope.launch {
                                            PixelMusicLyricsPreferences.setAutoTranslate(context, enabled)
                                        }
                                    },
                                    accentColor = accentColor,
                                    onAccentColor = onAccentColor,
                                    contentColor = contentColor
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(
                                    RoundedCornerShape(
                                        topStart = if (isAutoTranslateFirst) 18.dp else 8.dp,
                                        topEnd = if (isAutoTranslateFirst) 18.dp else 8.dp,
                                        bottomStart = 8.dp,
                                        bottomEnd = 8.dp
                                    )
                                )
                                .background(itemBackgroundColor)
                                .clickable {
                                    val next = !autoTranslate
                                    onShowTranslationChange(next)
                                    coroutineScope.launch {
                                        PixelMusicLyricsPreferences.setAutoTranslate(context, next)
                                    }
                                },
                            colors = ListItemDefaults.colors(
                                containerColor = Color.Transparent,
                                headlineColor = contentColor,
                                supportingColor = contentColor.copy(alpha = 0.7f),
                                leadingIconColor = contentColor
                            )
                        )
                    }

                    if (isTranslateLangVisible) {
                        val selectedLangLabel = PixelMusicLyricsPreferences.languageDisplayName(translateLang)
                        ListItem(
                            headlineContent = { Text("Translation Language") },
                            supportingContent = { Text(selectedLangLabel) },
                            leadingContent = {
                                Icon(
                                    painter = painterResource(R.drawable.rounded_translate_24),
                                    contentDescription = null
                                )
                            },
                            trailingContent = {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                                    contentDescription = null,
                                    tint = contentColor.copy(alpha = 0.7f)
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(itemBackgroundColor)
                                .clickable { showLanguagePicker = true },
                            colors = ListItemDefaults.colors(
                                containerColor = Color.Transparent,
                                headlineColor = contentColor,
                                supportingColor = accentColor,
                                leadingIconColor = contentColor
                            )
                        )
                    }

                    // Immersive Mode Toggle
                    if (isImmersiveVisible) {
                        ListItem(
                            headlineContent = { Text(stringResource(R.string.lyrics_more_disable_immersive_once)) },
                            leadingContent = {
                                Icon(
                                    painter = painterResource(R.drawable.rounded_visibility_off_24),
                                    contentDescription = null
                                )
                            },
                            trailingContent = {
                                LyricsSheetSwitch(
                                    checked = isImmersiveTemporarilyDisabled,
                                    onCheckedChange = {
                                        onSetImmersiveTemporarilyDisabled(it)
                                    },
                                    accentColor = accentColor,
                                    onAccentColor = onAccentColor,
                                    contentColor = contentColor
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(
                                    RoundedCornerShape(
                                        topStart = 8.dp,
                                        topEnd = 8.dp,
                                        bottomStart = if (isImmersiveLast) 24.dp else 8.dp,
                                        bottomEnd = if (isImmersiveLast) 24.dp else 8.dp
                                    )
                                )
                                .background(itemBackgroundColor),
                            colors = ListItemDefaults.colors(
                                containerColor = Color.Transparent,
                                headlineColor = contentColor,
                                leadingIconColor = contentColor
                            )
                        )
                    }

                    // Keep Screen On Toggle
                    if (isKeepScreenOnVisible) {
                        ListItem(
                            headlineContent = { Text(stringResource(R.string.lyrics_more_keep_screen_on)) },
                            leadingContent = {
                                Icon(
                                    painter = painterResource(R.drawable.rounded_light_mode_24),
                                    contentDescription = null
                                )
                            },
                            trailingContent = {
                                LyricsSheetSwitch(
                                    checked = keepScreenOn,
                                    onCheckedChange = onKeepScreenOnChange,
                                    accentColor = accentColor,
                                    onAccentColor = onAccentColor,
                                    contentColor = contentColor
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(
                                    RoundedCornerShape(
                                        topStart = 8.dp,
                                        topEnd = 8.dp,
                                        bottomStart = 24.dp,
                                        bottomEnd = 24.dp
                                    )
                                )
                                .background(itemBackgroundColor)
                                .clickable { onKeepScreenOnChange(!keepScreenOn) },
                            colors = ListItemDefaults.colors(
                                containerColor = Color.Transparent,
                                headlineColor = contentColor,
                                leadingIconColor = contentColor
                            )
                        )
                    }
                }
            }
            
        }
    }

    if (showLanguagePicker) {
        SmoothModalBottomSheet(
            onDismissRequest = { showLanguagePicker = false },
            containerColor = containerColor,
            contentColor = contentColor,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = navigationBarsPadding + 16.dp)
            ) {
                Text(
                    text = "Translation Language",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = contentColor,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 14.dp)
                )
                val languages = PixelMusicLyricsPreferences.SupportedTranslateLanguages
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .heightIn(max = 440.dp),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    itemsIndexed(languages, key = { _, pair -> pair.first }) { idx, (code, name) ->
                        val isSelected = code == translateLang
                        val cardBg by animateColorAsState(
                            targetValue = if (isSelected) accentColor else contentColor.copy(alpha = 0.08f),
                            animationSpec = tween(380, easing = FastOutSlowInEasing),
                            label = "lyrics_lang_bg"
                        )
                        val cardFg by animateColorAsState(
                            targetValue = if (isSelected) onAccentColor else contentColor,
                            animationSpec = tween(380, easing = FastOutSlowInEasing),
                            label = "lyrics_lang_fg"
                        )
                        val outer = 24.dp
                        val inner = 6.dp
                        val cardShape = when {
                            languages.size <= 1 -> RoundedCornerShape(outer)
                            idx == 0 -> RoundedCornerShape(topStart = outer, topEnd = outer, bottomStart = inner, bottomEnd = inner)
                            idx == languages.lastIndex -> RoundedCornerShape(topStart = inner, topEnd = inner, bottomStart = outer, bottomEnd = outer)
                            else -> RoundedCornerShape(inner)
                        }
                        Surface(
                            onClick = {
                                coroutineScope.launch {
                                    PixelMusicLyricsPreferences.setTranslateLanguage(context, code)
                                }
                                showLanguagePicker = false
                            },
                            shape = cardShape,
                            color = cardBg,
                            modifier = Modifier
                                .fillMaxWidth()
                                .animateItem(
                                    placementSpec = spring(
                                        dampingRatio = Spring.DampingRatioNoBouncy,
                                        stiffness = Spring.StiffnessLow
                                    )
                                )
                                .animateContentSize(
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioNoBouncy,
                                        stiffness = Spring.StiffnessLow
                                    )
                                )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 18.dp, vertical = 16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = name,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = cardFg
                                )
                                AnimatedContent(
                                    targetState = isSelected,
                                    transitionSpec = {
                                        (fadeIn(tween(300)) + scaleIn(tween(300), initialScale = 0.85f)) togetherWith
                                            (fadeOut(tween(220)) + scaleOut(tween(220), targetScale = 0.85f))
                                    },
                                    label = "lyrics_lang_check"
                                ) { active ->
                                    if (active) {
                                        Surface(
                                            shape = CircleShape,
                                            color = onAccentColor.copy(alpha = 0.2f)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.Check,
                                                contentDescription = null,
                                                tint = onAccentColor,
                                                modifier = Modifier
                                                    .padding(5.dp)
                                                    .size(16.dp)
                                            )
                                        }
                                    } else {
                                        Spacer(modifier = Modifier.size(26.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LyricsSheetSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    accentColor: Color,
    onAccentColor: Color,
    contentColor: Color,
) {
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        thumbContent = {
            AnimatedContent(
                targetState = checked,
                transitionSpec = { fadeIn(tween(100)) togetherWith fadeOut(tween(100)) },
                label = "lyrics_switch_thumb_icon"
            ) { isChecked ->
                Icon(
                    imageVector = if (isChecked) Icons.Rounded.Check else Icons.Rounded.Close,
                    contentDescription = null,
                    modifier = Modifier.size(SwitchDefaults.IconSize)
                )
            }
        },
        colors = SwitchDefaults.colors(
            checkedThumbColor = onAccentColor,
            checkedTrackColor = accentColor,
            checkedIconColor = accentColor,
            uncheckedThumbColor = contentColor,
            uncheckedTrackColor = contentColor.copy(alpha = 0.3f),
            uncheckedIconColor = contentColor.copy(alpha = 0.3f)
        )
    )
}
