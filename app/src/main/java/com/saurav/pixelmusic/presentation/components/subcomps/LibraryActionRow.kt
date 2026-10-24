package com.saurav.pixelmusic.presentation.components.subcomps

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.automirrored.rounded.Sort
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.FilterList
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.Dataset
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.saurav.pixelmusic.R
import com.saurav.pixelmusic.data.model.MusicFolder
import com.saurav.pixelmusic.ui.theme.GoogleSansRounded
import com.saurav.pixelmusic.ui.theme.LocalLiquidGlassEnabled
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.io.File

val defaultShape = RoundedCornerShape(26.dp) // Fallback shape
val genHeight = 42.dp

private fun Modifier.liquidGlassActionButton(
    shape: Shape,
    isLiquidGlass: Boolean,
    press: Animatable<Float, *>,
    jiggle: Animatable<Float, *>,
    isCircle: Boolean = false,
    borderBrush: Brush,
    coroutineScope: CoroutineScope,
    pressSpec: SpringSpec<Float>,
    jiggleSpec: SpringSpec<Float>
): Modifier {
    if (!isLiquidGlass) return this
    val scaleXMult = if (isCircle) 0.10f else 0.08f
    val jiggleXMult = if (isCircle) 0.15f else 0.12f
    val scaleYMult = if (isCircle) 0.08f else 0.06f
    val jiggleYMult = if (isCircle) 0.13f else 0.10f

    return this
        .graphicsLayer {
            scaleX = 1f + (press.value * scaleXMult) + (jiggle.value * jiggleXMult)
            scaleY = 1f - (press.value * scaleYMult) - (jiggle.value * jiggleYMult)
            shadowElevation = 0f
        }
        .pointerInput(Unit) {
            awaitEachGesture {
                awaitFirstDown(requireUnconsumed = false)
                coroutineScope.launch {
                    jiggle.snapTo(1f)
                    jiggle.animateTo(0f, jiggleSpec)
                }
                coroutineScope.launch { press.animateTo(1f, pressSpec) }
                waitForUpOrCancellation()
                coroutineScope.launch { press.animateTo(0f, pressSpec) }
            }
        }
        .border(BorderStroke(0.75.dp, borderBrush), shape)
        .clip(shape)
}

@Composable
fun LibraryActionRow(
    onMainActionClick: () -> Unit,
    iconRotation: Float,
    onSortClick: () -> Unit,
    onLocateClick: () -> Unit = {},
    showSortButton: Boolean,
    showLocateButton: Boolean = false,
    showImportButton: Boolean = true,
    isPlaylistTab: Boolean,
    onImportM3uClick: () -> Unit = {},
    isFoldersTab: Boolean,
    modifier: Modifier = Modifier,
    // Breadcrumb parameters
    currentFolder: MusicFolder?,
    folderRootPath: String,
    folderRootLabel: String,
    onFolderClick: (String) -> Unit,
    onNavigateBack: () -> Unit,
    isShuffleEnabled: Boolean = false,
    // Storage Filter
    showStorageFilterButton: Boolean = false,
    currentStorageFilter: com.saurav.pixelmusic.data.model.StorageFilter = com.saurav.pixelmusic.data.model.StorageFilter.ALL,
    onStorageFilterClick: () -> Unit = {}
) {
    val shouldShowImport = isPlaylistTab && showImportButton
    val isLiquidGlass = LocalLiquidGlassEnabled.current
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val coroutineScope = rememberCoroutineScope()
    val pressSpec = remember { spring<Float>(dampingRatio = 0.5f, stiffness = 450f) }
    val jiggleSpec = remember { spring<Float>(dampingRatio = 0.32f, stiffness = 280f) }

    val glassBorderBrush = remember(isDark) {
        Brush.verticalGradient(
            colors = if (isDark) listOf(
                Color.White.copy(alpha = 0.07f),
                Color.White.copy(alpha = 0.02f)
            ) else listOf(
                Color.White.copy(alpha = 0.14f),
                Color.White.copy(alpha = 0.04f)
            )
        )
    }

    val newPress = remember { Animatable(0f) }
    val newJiggle = remember { Animatable(0f) }
    val importPress = remember { Animatable(0f) }
    val importJiggle = remember { Animatable(0f) }
    val locatePress = remember { Animatable(0f) }
    val locateJiggle = remember { Animatable(0f) }
    val filterPress = remember { Animatable(0f) }
    val filterJiggle = remember { Animatable(0f) }
    val sortPress = remember { Animatable(0f) }
    val sortJiggle = remember { Animatable(0f) }

    CompositionLocalProvider(
        LocalMinimumInteractiveComponentSize provides 0.dp
    ) {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .padding(start = 4.dp),
            horizontalArrangement = Arrangement.Absolute.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            AnimatedContent(
                targetState = isFoldersTab,
                label = "ActionRowContent",
                transitionSpec = {
                    if (targetState) { // Transition to Folders (Breadcrumbs)
                        slideInVertically { height -> height } + fadeIn() togetherWith
                                slideOutVertically { height -> -height } + fadeOut()
                    } else { // Transition to other tabs (Buttons)
                        slideInVertically { height -> -height } + fadeIn() togetherWith
                                slideOutVertically { height -> height } + fadeOut()
                    }
                },
                modifier = Modifier.weight(1f)
            ) { isFolders ->
                if (isFolders) {
                    Breadcrumbs(
                        currentFolder = currentFolder,
                        rootPath = folderRootPath,
                        rootLabel = folderRootLabel,
                        onFolderClick = onFolderClick,
                        onNavigateBack = onNavigateBack,
                        isLiquidGlass = isLiquidGlass,
                        isDark = isDark,
                        glassBorderBrush = glassBorderBrush,
                        coroutineScope = coroutineScope,
                        pressSpec = pressSpec,
                        jiggleSpec = jiggleSpec
                    )
                } else {
                    val outerCorner = 26.dp
                    val innerCorner = 8.dp

                    val newButtonEndCorner by animateDpAsState(
                        targetValue = if (shouldShowImport) innerCorner else outerCorner,
                        label = "NewButtonEndCorner"
                    )

                    val importButtonStartCorner by animateDpAsState(
                        targetValue = innerCorner,
                        label = "ImportButtonStartCorner"
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Determine button colors based on shuffle state (not for playlist tab)
                        val buttonContainerColor = MaterialTheme.colorScheme.tertiaryContainer
                        val buttonContentColor = MaterialTheme.colorScheme.onTertiaryContainer
                        val newButtonShape = RoundedCornerShape(
                            topStart = 26.dp, bottomStart = 26.dp,
                            topEnd = newButtonEndCorner, bottomEnd = newButtonEndCorner
                        )
                        
                        FilledTonalButton(
                            onClick = onMainActionClick,
                            shape = newButtonShape,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isLiquidGlass) buttonContainerColor.copy(alpha = if (isDark) 0.55f else 0.70f) else buttonContainerColor,
                                contentColor = buttonContentColor
                            ),
                            elevation = if (isLiquidGlass) ButtonDefaults.buttonElevation(defaultElevation = 0.dp, pressedElevation = 0.dp, focusedElevation = 0.dp, hoveredElevation = 0.dp, disabledElevation = 0.dp) else ButtonDefaults.buttonElevation(defaultElevation = 4.dp, pressedElevation = 6.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                            modifier = Modifier
                                .height(genHeight)
                                .liquidGlassActionButton(
                                    shape = newButtonShape,
                                    isLiquidGlass = isLiquidGlass,
                                    press = newPress,
                                    jiggle = newJiggle,
                                    isCircle = false,
                                    borderBrush = glassBorderBrush,
                                    coroutineScope = coroutineScope,
                                    pressSpec = pressSpec,
                                    jiggleSpec = jiggleSpec
                                )
                        ) {
                            val icon = if (isPlaylistTab) Icons.AutoMirrored.Rounded.PlaylistAdd else Icons.Rounded.Shuffle
                            val text = if (isPlaylistTab) {
                                stringResource(R.string.library_action_new)
                            } else {
                                stringResource(R.string.shortcut_shuffle_short)
                            }
                            val contentDesc = if (isPlaylistTab) {
                                stringResource(R.string.cd_create_new_playlist)
                            } else {
                                stringResource(R.string.cd_shuffle_play)
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = contentDesc,
                                    modifier = Modifier.size(20.dp).rotate(iconRotation)
                                )
                                Text(
                                    modifier = Modifier.animateContentSize(),
                                    text = text,
                                    overflow = TextOverflow.Ellipsis,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        AnimatedVisibility(
                            visible = shouldShowImport,
                            enter = fadeIn() + expandHorizontally(
                                expandFrom = Alignment.Start,
                                clip = false, // <— evita el “corte” durante la expansión
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness = Spring.StiffnessLow
                                )
                            ),
                            exit = fadeOut() + shrinkHorizontally(
                                shrinkTowards = Alignment.Start,
                                clip = false, // <— evita el “corte” durante la expansión
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioNoBouncy,
                                    stiffness = Spring.StiffnessMedium
                                )
                            )
                        ) {
                            Row(modifier = Modifier.height(genHeight), verticalAlignment = Alignment.CenterVertically) {
                                Spacer(modifier = Modifier.width(8.dp))

                                val importButtonShape = RoundedCornerShape(
                                    topStart = importButtonStartCorner,
                                    bottomStart = importButtonStartCorner,
                                    topEnd = 26.dp,
                                    bottomEnd = 26.dp
                                )

                                FilledTonalButton(
                                    onClick = onImportM3uClick,
                                    shape = importButtonShape,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isLiquidGlass) MaterialTheme.colorScheme.secondaryContainer.copy(alpha = if (isDark) 0.55f else 0.70f) else MaterialTheme.colorScheme.secondaryContainer,
                                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                    ),
                                    elevation = if (isLiquidGlass) ButtonDefaults.buttonElevation(defaultElevation = 0.dp, pressedElevation = 0.dp, focusedElevation = 0.dp, hoveredElevation = 0.dp, disabledElevation = 0.dp) else ButtonDefaults.buttonElevation(
                                        defaultElevation = 4.dp,
                                        pressedElevation = 6.dp
                                    ),
                                    contentPadding = PaddingValues(
                                        horizontal = 14.dp,
                                        vertical = 10.dp
                                    ),
                                    modifier = Modifier
                                        .height(genHeight)
                                        .liquidGlassActionButton(
                                            shape = importButtonShape,
                                            isLiquidGlass = isLiquidGlass,
                                            press = importPress,
                                            jiggle = importJiggle,
                                            isCircle = false,
                                            borderBrush = glassBorderBrush,
                                            coroutineScope = coroutineScope,
                                            pressSpec = pressSpec,
                                            jiggleSpec = jiggleSpec
                                        )
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            painter = painterResource(R.drawable.rounded_upload_file_24),
                                            contentDescription = stringResource(R.string.cd_import_m3u_playlist),
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Text(
                                            text = stringResource(R.string.import_file),
                                            overflow = TextOverflow.Ellipsis,
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }


            Spacer(modifier = Modifier.width(8.dp))

            if (showSortButton) {
                val outerCorner = 26.dp
                
                // Logic for Sort Button (Rightmost)
                val sortStartCorner by animateDpAsState(
                    targetValue = if (showLocateButton || showStorageFilterButton) 8.dp else outerCorner,
                    label = "SortStartCorner"
                )

                // Logic for Filter Button (Middle or Left if Locate hidden)
                // Filter is visible if showStorageFilterButton is true
                val filterEndCorner = 8.dp // Connected to Sort
                val filterStartCorner by animateDpAsState(
                    targetValue = if (showLocateButton) 8.dp else outerCorner,
                    label = "FilterStartCorner"
                )
                
                // Logic for Locate Button (Leftmost)
                val locateEndCorner = 8.dp // Connected to Filer or Sort

                val gapBetweenLocateAndNext by animateDpAsState(
                    targetValue = if (showLocateButton) 4.dp else 0.dp,
                    label = "GapLocate"
                )
                val gapBetweenFilterAndSort by animateDpAsState(
                    targetValue = if (showStorageFilterButton) 4.dp else 0.dp,
                    label = "GapFilter"
                )


                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Locate Button
                    AnimatedVisibility(
                        visible = showLocateButton,
                        enter = slideInHorizontally(initialOffsetX = { it / 2 }) + fadeIn(),
                        exit = slideOutHorizontally(targetOffsetX = { it / 2 }) + fadeOut()
                    ) {
                        val locateShape = RoundedCornerShape(
                            topStart = outerCorner,
                            bottomStart = outerCorner,
                            topEnd = locateEndCorner,
                            bottomEnd = locateEndCorner
                        )
                        FilledTonalIconButton(
                            onClick = onLocateClick,
                            shape = locateShape,
                            colors = if (isLiquidGlass) IconButtonDefaults.filledTonalIconButtonColors(
                                containerColor = if (isDark) Color(0x351E1E22) else Color(0x55FFFFFF)
                            ) else IconButtonDefaults.filledTonalIconButtonColors(),
                            modifier = Modifier
                                .size(genHeight)
                                .liquidGlassActionButton(
                                    shape = locateShape,
                                    isLiquidGlass = isLiquidGlass,
                                    press = locatePress,
                                    jiggle = locateJiggle,
                                    isCircle = true,
                                    borderBrush = glassBorderBrush,
                                    coroutineScope = coroutineScope,
                                    pressSpec = pressSpec,
                                    jiggleSpec = jiggleSpec
                                )
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.MyLocation,
                                contentDescription = stringResource(R.string.cd_locate_current_song)
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.width(gapBetweenLocateAndNext))

                    // Storage Filter Button
                    AnimatedVisibility(
                        visible = showStorageFilterButton,
                        enter = slideInHorizontally(initialOffsetX = { it / 2 }) + fadeIn(),
                        exit = slideOutHorizontally(targetOffsetX = { it / 2 }) + fadeOut()
                    ) {
                         val finalIcon = when(currentStorageFilter) {
                             com.saurav.pixelmusic.data.model.StorageFilter.ALL -> Icons.Rounded.Dataset
                             com.saurav.pixelmusic.data.model.StorageFilter.ONLINE -> Icons.Rounded.Cloud
                             com.saurav.pixelmusic.data.model.StorageFilter.OFFLINE -> Icons.Rounded.PhoneAndroid
                             com.saurav.pixelmusic.data.model.StorageFilter.DOWNLOADED_ONLY -> Icons.Rounded.Download
                         }
                         val tooltipText = when(currentStorageFilter) {
                             com.saurav.pixelmusic.data.model.StorageFilter.ALL -> stringResource(R.string.library_storage_filter_all_songs)
                             com.saurav.pixelmusic.data.model.StorageFilter.ONLINE -> stringResource(R.string.library_storage_filter_online)
                             com.saurav.pixelmusic.data.model.StorageFilter.OFFLINE -> stringResource(R.string.library_storage_filter_offline)
                             com.saurav.pixelmusic.data.model.StorageFilter.DOWNLOADED_ONLY -> stringResource(R.string.library_storage_filter_downloaded_only)
                         }
                         val tooltipState = rememberTooltipState()
                         val filterShape = RoundedCornerShape(
                             topStart = filterStartCorner,
                             bottomStart = filterStartCorner,
                             topEnd = filterEndCorner,
                             bottomEnd = filterEndCorner
                         )

                        @OptIn(ExperimentalMaterial3Api::class)
                        TooltipBox(
                            positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
                            tooltip = {
                                PlainTooltip {
                                    Text(tooltipText)
                                }
                            },
                            state = tooltipState
                        ) {
                            FilledTonalIconButton(
                                onClick = onStorageFilterClick,
                                shape = filterShape,
                                colors = if (isLiquidGlass) IconButtonDefaults.filledTonalIconButtonColors(
                                    containerColor = if (isDark) Color(0x351E1E22) else Color(0x55FFFFFF)
                                ) else IconButtonDefaults.filledTonalIconButtonColors(),
                                modifier = Modifier
                                    .size(genHeight)
                                    .liquidGlassActionButton(
                                        shape = filterShape,
                                        isLiquidGlass = isLiquidGlass,
                                        press = filterPress,
                                        jiggle = filterJiggle,
                                        isCircle = true,
                                        borderBrush = glassBorderBrush,
                                        coroutineScope = coroutineScope,
                                        pressSpec = pressSpec,
                                        jiggleSpec = jiggleSpec
                                    )
                            ) {
                                 Icon(
                                    imageVector = finalIcon,
                                    contentDescription = tooltipText
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(gapBetweenFilterAndSort))

                    val sortShape = RoundedCornerShape(
                        topStart = sortStartCorner,
                        bottomStart = sortStartCorner,
                        topEnd = outerCorner,
                        bottomEnd = outerCorner
                    )

                    // Sort Button
                    FilledTonalIconButton(
                        onClick = onSortClick,
                        shape = sortShape,
                        colors = if (isLiquidGlass) IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = if (isDark) Color(0x351E1E22) else Color(0x55FFFFFF)
                        ) else IconButtonDefaults.filledTonalIconButtonColors(),
                        modifier = Modifier
                            .size(genHeight)
                            .liquidGlassActionButton(
                                shape = sortShape,
                                isLiquidGlass = isLiquidGlass,
                                press = sortPress,
                                jiggle = sortJiggle,
                                isCircle = true,
                                borderBrush = glassBorderBrush,
                                coroutineScope = coroutineScope,
                                pressSpec = pressSpec,
                                jiggleSpec = jiggleSpec
                            )
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.Sort,
                            contentDescription = stringResource(R.string.cd_sort_options),
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun Breadcrumbs(
    currentFolder: MusicFolder?,
    rootPath: String,
    rootLabel: String,
    onFolderClick: (String) -> Unit,
    onNavigateBack: () -> Unit,
    isLiquidGlass: Boolean = false,
    isDark: Boolean = false,
    glassBorderBrush: Brush = Brush.verticalGradient(listOf(Color.Transparent, Color.Transparent)),
    coroutineScope: CoroutineScope = rememberCoroutineScope(),
    pressSpec: SpringSpec<Float> = spring(dampingRatio = 0.5f, stiffness = 450f),
    jiggleSpec: SpringSpec<Float> = spring(dampingRatio = 0.32f, stiffness = 280f)
) {
    val rowState = rememberLazyListState()
    val pathSegments = remember(currentFolder?.path, rootPath, rootLabel) {
        val path = currentFolder?.path ?: rootPath
        val normalizedRoot = rootPath.removeSuffix("/")
        val normalizedPath = path.removeSuffix("/")
        val relativePath = normalizedPath
            .removePrefix(normalizedRoot)
            .removePrefix("/")

        if (!normalizedPath.startsWith(normalizedRoot) || relativePath.isEmpty() || normalizedPath == normalizedRoot) {
            listOf(rootLabel to rootPath)
        } else {
            listOf(rootLabel to rootPath) + relativePath.split("/").scan("") { acc, segment ->
                "$acc/$segment"
            }.drop(1).map {
                val file = File(rootPath, it)
                file.name to file.path
            }
        }
    }

    val showStartFade by remember { derivedStateOf { rowState.canScrollBackward } }
    val showEndFade by remember { derivedStateOf { rowState.canScrollForward } }

    LaunchedEffect(pathSegments.size) {
        if (pathSegments.isNotEmpty()) {
            rowState.animateScrollToItem(pathSegments.lastIndex + 1)
        }
    }

    val backPress = remember { Animatable(0f) }
    val backJiggle = remember { Animatable(0f) }
    val backShape = CircleShape

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        FilledTonalIconButton(
            onClick = onNavigateBack,
            shape = backShape,
            colors = if (isLiquidGlass) IconButtonDefaults.filledTonalIconButtonColors(
                containerColor = if (isDark) Color(0x351E1E22) else Color(0x55FFFFFF)
            ) else IconButtonDefaults.filledTonalIconButtonColors(),
            modifier = Modifier
                .size(36.dp)
                .liquidGlassActionButton(
                    shape = backShape,
                    isLiquidGlass = isLiquidGlass,
                    press = backPress,
                    jiggle = backJiggle,
                    isCircle = true,
                    borderBrush = glassBorderBrush,
                    coroutineScope = coroutineScope,
                    pressSpec = pressSpec,
                    jiggleSpec = jiggleSpec
                ),
            enabled = currentFolder != null
        ) {
            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(R.string.auth_cd_back))
        }
        Spacer(Modifier.width(8.dp))

        LazyRow(
            state = rowState,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .weight(1f)
                // 1. Forzamos que el contenido se dibuje en una capa separada.
                .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                .drawWithContent {
                    // 2. Dibujamos el contenido original (el LazyRow).
                    drawContent()

                    // 3. Dibujamos los gradientes que actúan como "máscaras de borrado".
                    val gradientWidth = 24.dp.toPx()

                    // Máscara para el borde IZQUIERDO
                    if (showStartFade) {
                        drawRect(
                            brush = Brush.horizontalGradient(
                                // Gradiente de transparente a opaco (negro)
                                colors = listOf(Color.Transparent, Color.Black),
                                endX = gradientWidth
                            ),
                            // DstIn mantiene el contenido del LazyRow solo donde esta capa es opaca.
                            blendMode = BlendMode.DstIn
                        )
                    }

                    // Máscara para el borde DERECHO
                    if (showEndFade) {
                        drawRect(
                            brush = Brush.horizontalGradient(
                                // Gradiente de opaco (negro) a transparente
                                colors = listOf(Color.Black, Color.Transparent),
                                startX = this.size.width - gradientWidth
                            ),
                            blendMode = BlendMode.DstIn
                        )
                    }
                }
        ) {
            item { Spacer(modifier = Modifier.width(12.dp)) }

            items(pathSegments.size, key = { pathSegments[it].second }) { index ->
                val (name, path) = pathSegments[index]
                val isLast = index == pathSegments.lastIndex
                Text(
                    text = name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = if (isLast) FontWeight.Bold else FontWeight.Normal,
                    fontFamily = GoogleSansRounded,
                    color = if (isLast) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .clip(CircleShape)
                        .clickable(enabled = !isLast) { onFolderClick(path) }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )
                if (!isLast) {
                    Icon(
                        imageVector = Icons.Rounded.ChevronRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            item { Spacer(modifier = Modifier.width(12.dp)) }
        }
    }
}
