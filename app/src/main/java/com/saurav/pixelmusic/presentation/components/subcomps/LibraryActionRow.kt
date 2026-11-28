package com.saurav.pixelmusic.presentation.components.subcomps

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.SizeTransform
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
import java.io.File

val defaultShape = RoundedCornerShape(26.dp) // Fallback shape
val genHeight = 42.dp

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
                        (slideInVertically(animationSpec = tween(220, easing = FastOutSlowInEasing)) { height -> height / 2 } + fadeIn(animationSpec = tween(200)) togetherWith
                                slideOutVertically(animationSpec = tween(180, easing = FastOutLinearInEasing)) { height -> -height / 2 } + fadeOut(animationSpec = tween(150)))
                            .using(SizeTransform(clip = false))
                    } else { // Transition to other tabs (Buttons)
                        (slideInVertically(animationSpec = tween(220, easing = FastOutSlowInEasing)) { height -> -height / 2 } + fadeIn(animationSpec = tween(200)) togetherWith
                                slideOutVertically(animationSpec = tween(180, easing = FastOutLinearInEasing)) { height -> height / 2 } + fadeOut(animationSpec = tween(150)))
                            .using(SizeTransform(clip = false))
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
                        onNavigateBack = onNavigateBack
                    )
                } else {
                    val outerCorner = 26.dp
                    val innerCorner = 8.dp

                    val buttonContainerColor = MaterialTheme.colorScheme.tertiaryContainer
                    val buttonContentColor = MaterialTheme.colorScheme.onTertiaryContainer
                    val flatElevation = ButtonDefaults.buttonElevation(
                        defaultElevation = 0.dp,
                        pressedElevation = 0.dp,
                        focusedElevation = 0.dp,
                        hoveredElevation = 0.dp,
                        disabledElevation = 0.dp
                    )

                    // Dynamic shape morphing: right edge morphs between full pill (26dp) and segmented joint (8dp)
                    val button1EndCorner by animateDpAsState(
                        targetValue = if (shouldShowImport) innerCorner else outerCorner,
                        animationSpec = spring(dampingRatio = 0.8f, stiffness = 400f),
                        label = "button1EndCorner"
                    )

                    val newButtonShape = RoundedCornerShape(
                        topStart = outerCorner,
                        bottomStart = outerCorner,
                        topEnd = button1EndCorner,
                        bottomEnd = button1EndCorner
                    )

                    val importButtonShape = RoundedCornerShape(
                        topStart = innerCorner,
                        bottomStart = innerCorner,
                        topEnd = outerCorner,
                        bottomEnd = outerCorner
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.animateContentSize(
                            animationSpec = spring(dampingRatio = 0.8f, stiffness = 400f)
                        )
                    ) {
                        FilledTonalButton(
                            onClick = onMainActionClick,
                            shape = newButtonShape,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = buttonContainerColor,
                                contentColor = buttonContentColor
                            ),
                            elevation = flatElevation,
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                            modifier = Modifier.height(genHeight)
                        ) {
                            AnimatedContent(
                                targetState = isPlaylistTab,
                                transitionSpec = {
                                    (fadeIn(animationSpec = tween(180, delayMillis = 40)) + scaleIn(initialScale = 0.88f, animationSpec = tween(180, delayMillis = 40)))
                                        .togetherWith(fadeOut(animationSpec = tween(120)) + scaleOut(targetScale = 0.88f, animationSpec = tween(120)))
                                },
                                label = "mainActionLabel"
                            ) { isPlaylist ->
                                val iconRes = if (isPlaylist) R.drawable.rounded_playlist_add_24 else R.drawable.rounded_shuffle_24
                                val text = if (isPlaylist) {
                                    stringResource(R.string.library_action_new)
                                } else {
                                    stringResource(R.string.shortcut_shuffle_short)
                                }
                                val contentDesc = if (isPlaylist) {
                                    stringResource(R.string.cd_create_new_playlist)
                                } else {
                                    stringResource(R.string.cd_shuffle_play)
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        painter = painterResource(iconRes),
                                        contentDescription = contentDesc,
                                        modifier = Modifier.size(20.dp).rotate(iconRotation)
                                    )
                                    Text(
                                        text = text,
                                        overflow = TextOverflow.Ellipsis,
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }

                        AnimatedVisibility(
                            visible = shouldShowImport,
                            enter = expandHorizontally(
                                animationSpec = spring(dampingRatio = 0.8f, stiffness = 400f),
                                expandFrom = Alignment.Start
                            ) + fadeIn(animationSpec = tween(180, delayMillis = 40)),
                            exit = shrinkHorizontally(
                                animationSpec = spring(dampingRatio = 0.8f, stiffness = 400f),
                                shrinkTowards = Alignment.Start
                            ) + fadeOut(animationSpec = tween(120))
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Spacer(modifier = Modifier.width(6.dp))

                                FilledTonalButton(
                                    onClick = onImportM3uClick,
                                    shape = importButtonShape,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                    ),
                                    elevation = flatElevation,
                                    contentPadding = PaddingValues(
                                        horizontal = 14.dp,
                                        vertical = 10.dp
                                    ),
                                    modifier = Modifier.height(genHeight)
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
                            colors = IconButtonDefaults.filledTonalIconButtonColors(),
                            modifier = Modifier.size(genHeight)
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.rounded_my_location_24),
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
                         val finalIconRes = when(currentStorageFilter) {
                             com.saurav.pixelmusic.data.model.StorageFilter.ALL -> R.drawable.rounded_category_24
                             com.saurav.pixelmusic.data.model.StorageFilter.ONLINE -> R.drawable.rounded_cloud_24
                             com.saurav.pixelmusic.data.model.StorageFilter.OFFLINE -> R.drawable.rounded_phone_android_24
                             com.saurav.pixelmusic.data.model.StorageFilter.DOWNLOADED_ONLY -> R.drawable.rounded_download_24
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
                                colors = IconButtonDefaults.filledTonalIconButtonColors(),
                                modifier = Modifier.size(genHeight)
                            ) {
                                androidx.compose.animation.AnimatedContent(
                                    targetState = finalIconRes,
                                    transitionSpec = {
                                        (androidx.compose.animation.scaleIn(
                                            initialScale = 0.72f,
                                            animationSpec = androidx.compose.animation.core.spring(
                                                dampingRatio = 0.72f,
                                                stiffness = 420f
                                            )
                                        ) + androidx.compose.animation.fadeIn(
                                            animationSpec = androidx.compose.animation.core.tween(160)
                                        )).togetherWith(
                                            androidx.compose.animation.scaleOut(targetScale = 0.72f) +
                                                androidx.compose.animation.fadeOut(
                                                    animationSpec = androidx.compose.animation.core.tween(120)
                                                )
                                        )
                                    },
                                    label = "storage_filter_icon_anim"
                                ) { iconRes ->
                                    Icon(
                                        painter = painterResource(iconRes),
                                        contentDescription = tooltipText
                                    )
                                }
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
                        colors = IconButtonDefaults.filledTonalIconButtonColors(),
                        modifier = Modifier.size(genHeight)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.rounded_sort_24),
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
    onNavigateBack: () -> Unit
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
        if (pathSegments.size > 1) {
            rowState.animateScrollToItem(pathSegments.lastIndex)
        }
    }

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
            colors = IconButtonDefaults.filledTonalIconButtonColors(),
            modifier = Modifier.size(36.dp),
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
                        painter = painterResource(R.drawable.rounded_chevron_right_24),
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
