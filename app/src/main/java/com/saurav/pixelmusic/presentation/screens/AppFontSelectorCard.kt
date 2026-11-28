package com.saurav.pixelmusic.presentation.screens

import com.saurav.pixelmusic.presentation.components.PixelToast as Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.saurav.pixelmusic.R
import com.saurav.pixelmusic.data.preferences.AppFontMode
import com.saurav.pixelmusic.presentation.components.LocalSmoothDismiss
import com.saurav.pixelmusic.presentation.components.SmoothModalBottomSheet
import com.saurav.pixelmusic.ui.theme.ActualGoogleSansRounded
import com.saurav.pixelmusic.ui.theme.GoogleSansRounded
import com.saurav.pixelmusic.ui.theme.LocalThemeTransitionController
import com.saurav.pixelmusic.utils.CatalogFontEntry
import com.saurav.pixelmusic.utils.CustomFontManager
import com.saurav.pixelmusic.utils.InstalledFontPack
import kotlinx.coroutines.launch

private fun expressiveGroupCardShape(index: Int, totalCount: Int): Shape {
    val outer = 24.dp
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppFontSelectorCard(
    selectedFontMode: String,
    onFontModeSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val view = LocalView.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val themeTransitionController = LocalThemeTransitionController.current
    val highlightTitle = LocalSettingsHighlightTitle.current
    val label = stringResource(R.string.setcat_app_font_label)
    val description = stringResource(R.string.setcat_app_font_desc)
    val highlighted = highlightTitle != null && label == highlightTitle
    val shape = RoundedCornerShape(10.dp)

    LaunchedEffect(Unit) {
        CustomFontManager.initialize(context)
    }

    var showSheet by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedGroup by remember { mutableStateOf("All") }
    var downloadingFontId by remember { mutableStateOf<String?>(null) }

    val installedPacks = CustomFontManager.installedPacks
    val selectedDisplayName = remember(selectedFontMode, installedPacks.size) {
        CustomFontManager.getDisplayNameForMode(selectedFontMode)
    }

    fun applyFontSmoothly(targetMode: String, origin: Offset, dismiss: ((() -> Unit)?) -> Unit) {
        if (targetMode == selectedFontMode) {
            dismiss(null)
            return
        }
        dismiss {
            if (themeTransitionController != null) {
                themeTransitionController.lastTouchOffset = origin
                themeTransitionController.transitionThemeChange(
                    view = view,
                    origin = origin,
                    scope = scope
                ) {
                    onFontModeSelected(targetMode)
                }
            } else {
                onFontModeSelected(targetMode)
            }
        }
    }

    val fontFilePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val result = CustomFontManager.importCustomFontFromUri(context, uri)
                result.onSuccess { pack ->
                    Toast.makeText(
                        context,
                        "${pack.displayName} imported. Tap Apply to use it.",
                        Toast.LENGTH_SHORT
                    ).show()
                }.onFailure { err ->
                    Toast.makeText(
                        context,
                        err.message ?: "Could not import font file",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

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
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .padding(end = 16.dp)
                            .size(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.rounded_style_24),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceContainerLowest,
                                shape = CircleShape
                            ) {
                                Text(
                                    text = selectedDisplayName,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }

                            if (installedPacks.isNotEmpty()) {
                                Surface(
                                    color = MaterialTheme.colorScheme.secondaryContainer,
                                    shape = CircleShape
                                ) {
                                    Text(
                                        text = "${installedPacks.size} saved",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showSheet) {
        SmoothModalBottomSheet(
            onDismissRequest = {
                showSheet = false
                searchQuery = ""
                selectedGroup = "All"
            },
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            contentColor = MaterialTheme.colorScheme.onSurface,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
        ) {
            val smoothDismiss = LocalSmoothDismiss.current
            val packsUsed = installedPacks.size

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(bottom = 16.dp)
            ) {
                // Header Row: Title + Compact Count + Import Pill
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.headlineSmall,
                            fontFamily = GoogleSansRounded,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (packsUsed == 0) {
                                "Download or import custom fonts"
                            } else {
                                "$packsUsed custom ${if (packsUsed == 1) "font" else "fonts"} saved"
                            },
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Surface(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            fontFilePicker.launch(
                                arrayOf(
                                    "font/ttf",
                                    "font/otf",
                                    "application/x-font-ttf",
                                    "application/x-font-otf",
                                    "application/octet-stream",
                                    "*/*"
                                )
                            )
                        },
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.secondaryContainer
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Add,
                                contentDescription = "Import font",
                                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Import",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                maxLines = 1
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Sleek single-line full-width Expressive Search Pill
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .height(48.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Search,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Box(
                            modifier = Modifier.weight(1f),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            if (searchQuery.isEmpty()) {
                                Text(
                                    text = "Search any Google Font...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            BasicTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                singleLine = true,
                                textStyle = MaterialTheme.typography.bodyMedium.copy(
                                    color = MaterialTheme.colorScheme.onSurface
                                ),
                                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        if (searchQuery.isNotEmpty()) {
                            IconButton(
                                onClick = { searchQuery = "" },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Close,
                                    contentDescription = "Clear",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Category Filter Chips
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(CustomFontManager.filterGroups, key = { it }) { group ->
                        val isGroupSelected = selectedGroup == group
                        val chipBg by animateColorAsState(
                            targetValue = if (isGroupSelected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.surfaceContainerHigh
                            },
                            animationSpec = tween(320, easing = FastOutSlowInEasing),
                            label = "font_chip_bg"
                        )
                        val chipText by animateColorAsState(
                            targetValue = if (isGroupSelected) {
                                MaterialTheme.colorScheme.onPrimary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            animationSpec = tween(320, easing = FastOutSlowInEasing),
                            label = "font_chip_text"
                        )
                        Surface(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                selectedGroup = group
                            },
                            shape = CircleShape,
                            color = chipBg
                        ) {
                            Text(
                                text = group,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (isGroupSelected) FontWeight.Bold else FontWeight.Medium,
                                color = chipText,
                                maxLines = 1,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                val queryTrimmed = searchQuery.trim()
                val queryLower = queryTrimmed.lowercase()

                val filteredInstalled = remember(queryLower, installedPacks.toList()) {
                    if (queryLower.isEmpty()) {
                        installedPacks.toList()
                    } else {
                        installedPacks.filter {
                            it.displayName.lowercase().contains(queryLower) ||
                                it.category.lowercase().contains(queryLower)
                        }
                    }
                }

                val filteredCatalog = remember(queryLower, selectedGroup, installedPacks.toList()) {
                    val installedIds = installedPacks.map { it.id }.toSet()
                    CustomFontManager.catalogFonts.filter { entry ->
                        entry.id !in installedIds &&
                            (selectedGroup == "All" || entry.group == selectedGroup) &&
                            (queryLower.isEmpty() ||
                                entry.displayName.lowercase().contains(queryLower) ||
                                entry.category.lowercase().contains(queryLower) ||
                                entry.group.lowercase().contains(queryLower))
                    }
                }

                val dynamicSearchEntry = remember(queryTrimmed, filteredCatalog, installedPacks.toList()) {
                    if (queryTrimmed.length < 3) {
                        null
                    } else {
                        val exactMatchExists = filteredCatalog.any {
                            it.displayName.equals(queryTrimmed, ignoreCase = true)
                        } || installedPacks.any {
                            it.displayName.equals(queryTrimmed, ignoreCase = true)
                        }
                        if (exactMatchExists) null else CustomFontManager.createDynamicGoogleFontEntry(queryTrimmed)
                    }
                }

                val builtInEntries = remember {
                    listOf(
                        Triple(AppFontMode.APP_DEFAULT, "PixelMusic Default", "Google Sans Rounded"),
                        Triple(AppFontMode.SYSTEM, "System Default", "Device Typeface")
                    )
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 460.dp)
                        .padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    // Section 1: Built-in Fonts (grouped Material You Expressive cards)
                    if (queryLower.isEmpty() && selectedGroup == "All") {
                        item(key = "section_builtin") {
                            ExpressiveFontSectionLabel(title = "Built-in")
                        }
                        itemsIndexed(builtInEntries, key = { _, item -> "builtin_${item.first}" }) { idx, (mode, name, sub) ->
                            val family = if (mode == AppFontMode.APP_DEFAULT) ActualGoogleSansRounded else FontFamily.Default
                            ExpressiveBuiltInFontCard(
                                title = name,
                                subtitle = sub,
                                previewFamily = family,
                                isSelected = selectedFontMode == mode,
                                shape = expressiveGroupCardShape(idx, builtInEntries.size),
                                modifier = Modifier.animateItem(
                                    placementSpec = spring(
                                        dampingRatio = Spring.DampingRatioNoBouncy,
                                        stiffness = Spring.StiffnessLow
                                    )
                                ),
                                onApply = { origin ->
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    applyFontSmoothly(mode, origin, smoothDismiss)
                                }
                            )
                        }
                    }

                    // Section 2: Downloaded & Imported Font Packs (download first, then tap Apply)
                    if (filteredInstalled.isNotEmpty()) {
                        item(key = "section_installed") {
                            ExpressiveFontSectionLabel(
                                title = "Downloaded ($packsUsed)",
                                topPadding = if (queryLower.isEmpty() && selectedGroup == "All") 14.dp else 4.dp
                            )
                        }
                        itemsIndexed(filteredInstalled, key = { _, pack -> "installed_${pack.id}" }) { idx, pack ->
                            val isSelected = selectedFontMode == pack.id
                            val packFamily = CustomFontManager.resolveFontFamily(pack.id)
                            ExpressiveInstalledFontCard(
                                pack = pack,
                                previewFamily = packFamily,
                                isSelected = isSelected,
                                shape = expressiveGroupCardShape(idx, filteredInstalled.size),
                                modifier = Modifier.animateItem(
                                    placementSpec = spring(
                                        dampingRatio = Spring.DampingRatioNoBouncy,
                                        stiffness = Spring.StiffnessLow
                                    )
                                ),
                                onApply = { origin ->
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    applyFontSmoothly(pack.id, origin, smoothDismiss)
                                },
                                onDelete = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    scope.launch {
                                        if (isSelected) {
                                            onFontModeSelected(AppFontMode.APP_DEFAULT)
                                        }
                                        CustomFontManager.deleteFontPack(context, pack.id)
                                        Toast.makeText(
                                            context,
                                            "Removed ${pack.displayName}",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                            )
                        }
                    }

                    // Section 3: On-Demand Expressive Font Catalog (Download ONLY — never auto-applies)
                    val catalogDisplayList = buildList {
                        addAll(filteredCatalog)
                        if (dynamicSearchEntry != null) add(dynamicSearchEntry)
                    }
                    if (catalogDisplayList.isNotEmpty()) {
                        item(key = "section_catalog") {
                            ExpressiveFontSectionLabel(
                                title = if (selectedGroup == "All") "Explore Fonts" else "$selectedGroup Fonts",
                                topPadding = 14.dp
                            )
                        }
                        itemsIndexed(catalogDisplayList, key = { _, entry -> "catalog_${entry.id}" }) { idx, entry ->
                            val isDownloading = downloadingFontId == entry.id
                            val previewFamily = remember(entry.id) {
                                CustomFontManager.getPreviewFontFamily(entry)
                            }
                            ExpressiveCatalogFontCard(
                                entry = entry,
                                previewFamily = previewFamily,
                                isDownloading = isDownloading,
                                shape = expressiveGroupCardShape(idx, catalogDisplayList.size),
                                modifier = Modifier.animateItem(
                                    placementSpec = spring(
                                        dampingRatio = Spring.DampingRatioNoBouncy,
                                        stiffness = Spring.StiffnessLow
                                    )
                                ),
                                onDownload = {
                                    if (downloadingFontId != null) return@ExpressiveCatalogFontCard
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    downloadingFontId = entry.id
                                    scope.launch {
                                        val result = CustomFontManager.downloadCatalogFontPack(context, entry)
                                        downloadingFontId = null
                                        result.onSuccess { pack ->
                                            Toast.makeText(
                                                context,
                                                "${pack.displayName} downloaded. Tap Apply to use it.",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }.onFailure { err ->
                                            Toast.makeText(
                                                context,
                                                err.message ?: "Could not download ${entry.displayName}",
                                                Toast.LENGTH_LONG
                                            ).show()
                                        }
                                    }
                                }
                            )
                        }
                    }

                    item(key = "bottom_spacer") {
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun ExpressiveFontSectionLabel(
    title: String,
    topPadding: androidx.compose.ui.unit.Dp = 4.dp
) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        fontFamily = GoogleSansRounded,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = topPadding, bottom = 6.dp, start = 6.dp)
    )
}

@Composable
private fun ExpressiveBuiltInFontCard(
    title: String,
    subtitle: String,
    previewFamily: FontFamily,
    isSelected: Boolean,
    shape: Shape,
    modifier: Modifier = Modifier,
    onApply: (Offset) -> Unit
) {
    var cardCenter by remember { mutableStateOf(Offset.Zero) }
    val containerColor by animateColorAsState(
        targetValue = if (isSelected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainerHigh
        },
        animationSpec = tween(380, easing = FastOutSlowInEasing),
        label = "builtin_card_bg"
    )
    val contentColor by animateColorAsState(
        targetValue = if (isSelected) {
            MaterialTheme.colorScheme.onPrimaryContainer
        } else {
            MaterialTheme.colorScheme.onSurface
        },
        animationSpec = tween(380, easing = FastOutSlowInEasing),
        label = "builtin_card_fg"
    )

    Surface(
        onClick = { onApply(cardCenter) },
        shape = shape,
        color = containerColor,
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessLow
                )
            )
            .onGloballyPositioned { coords ->
                val pos = coords.positionInRoot()
                cardCenter = pos + Offset(coords.size.width / 2f, coords.size.height / 2f)
            }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontFamily = previewFamily,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = contentColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = contentColor.copy(alpha = 0.7f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            AnimatedContent(
                targetState = isSelected,
                transitionSpec = {
                    (fadeIn(tween(300)) + scaleIn(tween(300), initialScale = 0.85f)) togetherWith
                        (fadeOut(tween(220)) + scaleOut(tween(220), targetScale = 0.85f))
                },
                label = "builtin_action"
            ) { active ->
                if (active) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Check,
                                contentDescription = "Active",
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Active",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                } else {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceContainerHighest
                    ) {
                        Text(
                            text = "Apply",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ExpressiveInstalledFontCard(
    pack: InstalledFontPack,
    previewFamily: FontFamily,
    isSelected: Boolean,
    shape: Shape,
    modifier: Modifier = Modifier,
    onApply: (Offset) -> Unit,
    onDelete: () -> Unit
) {
    var cardCenter by remember { mutableStateOf(Offset.Zero) }
    val containerColor by animateColorAsState(
        targetValue = if (isSelected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainerHigh
        },
        animationSpec = tween(380, easing = FastOutSlowInEasing),
        label = "installed_card_bg"
    )
    val contentColor by animateColorAsState(
        targetValue = if (isSelected) {
            MaterialTheme.colorScheme.onPrimaryContainer
        } else {
            MaterialTheme.colorScheme.onSurface
        },
        animationSpec = tween(380, easing = FastOutSlowInEasing),
        label = "installed_card_fg"
    )

    Surface(
        onClick = { onApply(cardCenter) },
        shape = shape,
        color = containerColor,
        modifier = modifier
            .fillMaxWidth()
            .onGloballyPositioned { coords ->
                val pos = coords.positionInRoot()
                cardCenter = pos + Offset(coords.size.width / 2f, coords.size.height / 2f)
            }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 18.dp, end = 10.dp, top = 14.dp, bottom = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = pack.displayName,
                    fontFamily = previewFamily,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Normal,
                    color = contentColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Now Playing · PixelMusic",
                    fontFamily = previewFamily,
                    style = MaterialTheme.typography.bodySmall,
                    color = contentColor.copy(alpha = 0.78f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = if (pack.isCustomImport) "Imported (.ttf/.otf)" else pack.category,
                    style = MaterialTheme.typography.labelSmall,
                    color = contentColor.copy(alpha = 0.6f),
                    maxLines = 1
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            AnimatedContent(
                targetState = isSelected,
                transitionSpec = {
                    (fadeIn(tween(300)) + scaleIn(tween(300), initialScale = 0.85f)) togetherWith
                        (fadeOut(tween(220)) + scaleOut(tween(220), targetScale = 0.85f))
                },
                label = "installed_apply_badge"
            ) { active ->
                if (active) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Check,
                                contentDescription = "Active",
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Active",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                } else {
                    Surface(
                        onClick = { onApply(cardCenter) },
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary
                    ) {
                        Text(
                            text = "Apply",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Delete,
                    contentDescription = "Delete pack",
                    tint = if (isSelected) {
                        MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    } else {
                        MaterialTheme.colorScheme.error
                    },
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun ExpressiveCatalogFontCard(
    entry: CatalogFontEntry,
    previewFamily: FontFamily,
    isDownloading: Boolean,
    shape: Shape,
    modifier: Modifier = Modifier,
    onDownload: () -> Unit
) {
    Surface(
        onClick = onDownload,
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = entry.displayName,
                    fontFamily = previewFamily,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Normal,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = entry.sampleText,
                    fontFamily = previewFamily,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = entry.category,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.85f),
                    maxLines = 1
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            AnimatedContent(
                targetState = isDownloading,
                transitionSpec = {
                    (fadeIn(tween(260)) + scaleIn(tween(260), initialScale = 0.8f)) togetherWith
                        (fadeOut(tween(200)) + scaleOut(tween(200), targetScale = 0.8f))
                },
                label = "catalog_download_btn"
            ) { loading ->
                if (loading) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.size(40.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            strokeWidth = 2.4.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                } else {
                    Surface(
                        onClick = onDownload,
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                painter = painterResource(R.drawable.rounded_download_24),
                                contentDescription = "Download font",
                                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
