@file:OptIn(
    ExperimentalLayoutApi::class,
    ExperimentalMaterial3Api::class,
    ExperimentalMaterial3ExpressiveApi::class
)

package com.saurav.pixelmusic.presentation.components

import android.content.res.Resources
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.annotation.DrawableRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Close
import androidx.compose.ui.res.painterResource
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FlexibleBottomAppBar
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LargeExtendedFloatingActionButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MediumExtendedFloatingActionButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.SheetValue
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberBottomSheetState
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.Layout
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsAnimationCompat
import androidx.core.view.WindowInsetsCompat
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import com.saurav.pixelmusic.R
import com.saurav.pixelmusic.ui.theme.GoogleSansRounded
import com.saurav.pixelmusic.utils.shapes.RoundedStarShape
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import racra.compose.smooth_corner_rect_library.AbsoluteSmoothCornerShape

@EntryPoint
@InstallIn(SingletonComponent::class)
interface AiHeroArtworkEntryPoint {
    fun playbackStatsRepository(): com.saurav.pixelmusic.data.stats.PlaybackStatsRepository
    fun musicDao(): com.saurav.pixelmusic.data.database.MusicDao
}

/** Process-wide memo of the last loaded hero artwork so reopening the sheet never starts from an empty stage. */
private object AiHeroArtworkMemo {
    @Volatile
    var uris: List<String> = emptyList()
}

@Composable
internal fun rememberAiHeroArtworkUris(): List<String> {
    val context = LocalContext.current
    var artworkUris by remember { mutableStateOf(AiHeroArtworkMemo.uris) }
    LaunchedEffect(context) {
        val loaded = withContext(Dispatchers.IO) {
            runCatching {
                val appContext = context.applicationContext
                val entryPoint = EntryPointAccessors.fromApplication(appContext, AiHeroArtworkEntryPoint::class.java)
                val history = entryPoint.playbackStatsRepository().loadPlaybackHistory(limit = 36)
                val recentIds = history.mapNotNull { it.songId.toLongOrNull() }.distinct()
                val dbSongsById = if (recentIds.isNotEmpty()) {
                    entryPoint.musicDao().getSongsByIdsListSimple(recentIds).associateBy { it.id }
                } else {
                    emptyMap()
                }
                val collected = LinkedHashSet<String>(24)
                for (entry in history) {
                    val songIdLong = entry.songId.toLongOrNull()
                    val dbArt = songIdLong?.let { dbSongsById[it]?.albumArtUriString }
                    val candidate = dbArt?.takeIf { it.isNotBlank() } ?: entry.thumbnail?.takeIf { it.isNotBlank() }
                    if (candidate != null) {
                        collected.add(candidate)
                        if (collected.size >= 18) break
                    }
                }
                if (collected.size < 9) {
                    val randomSongs = entryPoint.musicDao().getRandomSongs(limit = 24)
                    for (song in randomSongs) {
                        val art = song.albumArtUriString?.takeIf { it.isNotBlank() }
                        if (art != null) {
                            collected.add(art)
                            if (collected.size >= 18) break
                        }
                    }
                }
                collected.toList()
            }.getOrDefault(emptyList())
        }
        if (loaded.isNotEmpty() && loaded != AiHeroArtworkMemo.uris) {
            if (artworkUris.isEmpty()) {
                // First ever open: let the sheet finish lifting before artwork is composed and decoded
                delay(450L)
                artworkUris = loaded
            }
            AiHeroArtworkMemo.uris = loaded
        }
    }
    return artworkUris
}

@Composable
fun PlaylistCreationTypeDialog(
    visible: Boolean,
    onDismiss: () -> Unit,
    onManualSelected: () -> Unit,
    onAiSelected: () -> Unit,
    isAiEnabled: Boolean = true,
    onSetupAiClick: (() -> Unit)? = null
) {
    if (!visible) return

    val dialogShape = AbsoluteSmoothCornerShape(
        cornerRadiusTL = 24.dp,
        smoothnessAsPercentTL = 60,
        cornerRadiusTR = 24.dp,
        smoothnessAsPercentTR = 60,
        cornerRadiusBL = 40.dp,
        smoothnessAsPercentBL = 60,
        cornerRadiusBR = 40.dp,
        smoothnessAsPercentBR = 60
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = true)
    ) {
        Surface(
            shape = dialogShape,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 6.dp,
            shadowElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stringResource(R.string.presentation_batch_e_create_playlist_title),
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontFamily = GoogleSansRounded,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Text(
                        text = stringResource(R.string.presentation_batch_e_create_playlist_subtitle),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                    )
                }

                CreationModeCard(
                    title = stringResource(R.string.presentation_batch_e_creation_mode_manual),
                    subtitle = stringResource(R.string.presentation_batch_e_creation_mode_manual_subtitle),
                    icon = {
                        Icon(
                            painter = painterResource(R.drawable.rounded_playlist_add_24),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    },
                    onClick = onManualSelected,
                    enabled = true,
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )

                CreationModeCard(
                    title = stringResource(R.string.presentation_batch_e_creation_mode_ai),
                    subtitle = if (isAiEnabled) {
                        stringResource(R.string.presentation_batch_e_creation_mode_ai_subtitle_enabled)
                    } else {
                        stringResource(R.string.presentation_batch_e_creation_mode_ai_subtitle_disabled)
                    },
                    icon = {
                        Icon(
                            painter = painterResource(if (isAiEnabled) R.drawable.rounded_auto_awesome_24 else R.drawable.rounded_key_24),
                            contentDescription = null,
                            tint = if (isAiEnabled) {
                                MaterialTheme.colorScheme.onTertiaryContainer
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    },
                    onClick = {
                        if (isAiEnabled) {
                            onAiSelected()
                        } else {
                            onDismiss()
                            onSetupAiClick?.invoke()
                        }
                    },
                    enabled = true,
                    containerColor = if (isAiEnabled) {
                        MaterialTheme.colorScheme.tertiaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceContainer
                    },
                    contentColor = if (isAiEnabled) {
                        MaterialTheme.colorScheme.onTertiaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )

                if (!isAiEnabled) {
                    FilledTonalButton(
                        onClick = {
                            onDismiss()
                            onSetupAiClick?.invoke()
                        },
                        elevation = ButtonDefaults.filledTonalButtonElevation(),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(vertical = 12.dp)
                    ) {
                        Icon(painter = painterResource(R.drawable.rounded_key_24), contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.presentation_batch_e_setup_api_key))
                    }
                }
            }
        }
    }
}

@Composable
private fun CreationModeCard(
    title: String,
    subtitle: String,
    icon: @Composable () -> Unit,
    onClick: () -> Unit,
    enabled: Boolean,
    containerColor: Color,
    contentColor: Color
) {
    val cardShape = AbsoluteSmoothCornerShape(
        cornerRadiusTL = 22.dp,
        smoothnessAsPercentTL = 60,
        cornerRadiusTR = 22.dp,
        smoothnessAsPercentTR = 60,
        cornerRadiusBL = 22.dp,
        smoothnessAsPercentBL = 60,
        cornerRadiusBR = 22.dp,
        smoothnessAsPercentBR = 60
    )

    Card(
        onClick = onClick,
        enabled = enabled,
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = cardShape
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = contentColor.copy(alpha = 0.16f),
                shape = AbsoluteSmoothCornerShape(
                    cornerRadiusTL = 12.dp,
                    smoothnessAsPercentTL = 60,
                    cornerRadiusTR = 18.dp,
                    smoothnessAsPercentTR = 60,
                    cornerRadiusBL = 18.dp,
                    smoothnessAsPercentBL = 60,
                    cornerRadiusBR = 12.dp,
                    smoothnessAsPercentBR = 60
                )
            ) {
                Row(modifier = Modifier.padding(10.dp)) {
                    icon()
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = contentColor
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = contentColor.copy(alpha = 0.8f)
                )
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateAiPlaylistDialog(
    visible: Boolean,
    isGenerating: Boolean,
    isSuccess: Boolean = false,
    status: String? = null,
    error: String?,
    onDismiss: () -> Unit,
    onGenerate: (playlistName: String?, prompt: String, minLength: Int, maxLength: Int) -> Unit
) {
    if (visible) {
        val heroArtworkUris = rememberAiHeroArtworkUris()
        if (isGenerating || isSuccess) {
            // Full Screen Edge-to-Edge Generating Page
            AiPlaylistGeneratingFullScreenPage(
                isSuccess = isSuccess,
                status = status,
                error = error,
                heroArtworkUris = heroArtworkUris,
                onDismiss = onDismiss
            )
        } else {
            // Idle Prompt & Vibe Selection Bottom Sheet
            val sheetState = rememberBottomSheetState(
                initialValue = SheetValue.Hidden,
                enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded)
            )

            SmoothModalBottomSheet(
                onDismissRequest = onDismiss,
                sheetState = sheetState,
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
                CreateAiPlaylistSheetContent(
                    error = error,
                    heroArtworkUris = heroArtworkUris,
                    onDismiss = onDismiss,
                    onGenerate = onGenerate
                )
            }
        }
    }
}

@Composable
fun AiPlaylistGeneratingFullScreenPage(
    isSuccess: Boolean,
    status: String?,
    error: String?,
    heroArtworkUris: List<String> = rememberAiHeroArtworkUris(),
    customHeadlines: List<String>? = null,
    customPhases: List<String>? = null,
    successHeadline: String = "Mix synthesized!",
    successSubtitle: String = "Opening your new playlist now…",
    preferLiveStatus: Boolean = false,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        val view = LocalView.current
        LaunchedEffect(view) {
            (view.parent as? DialogWindowProvider)?.window?.apply {
                setDimAmount(0f)
                setBackgroundDrawableResource(android.R.color.transparent)
                setWindowAnimations(0)
            }
        }

        // Unified success progress driving:
        // 1) 0.00..0.36: Three components glide to center and morph into ONE unified component
        // 2) 0.32..0.72: That ONE merged component itself zooms in edge-to-edge across the screen
        // 3) 0.70..1.00: That zoomed-in component dissolves over the already-opened playlist page
        val successProgress = remember { Animatable(0f) }
        LaunchedEffect(isSuccess) {
            if (isSuccess) {
                successProgress.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = 1380, easing = LinearEasing)
                )
            } else {
                successProgress.snapTo(0f)
            }
        }

        val p = successProgress.value
        val bgColor = MaterialTheme.colorScheme.background

        // Fade out the dialog background once the single zoomed-in component covers 100% of the screen
        val bgAlpha = if (isSuccess && p > 0.56f) {
            (1f - ((p - 0.56f) / 0.12f)).coerceIn(0f, 1f)
        } else {
            1f
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawBehind {
                    if (bgAlpha > 0.001f) {
                        drawRect(bgColor.copy(alpha = bgAlpha))
                    }
                }
        ) {
            // Focus-in entrance: content zooms down from 1.35x while the blur resolves
            val enter = remember { Animatable(0f) }
            LaunchedEffect(Unit) {
                enter.animateTo(1f, tween(750, easing = FastOutSlowInEasing))
            }
            val enterBlur = (28f * (1f - enter.value)).dp

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .graphicsLayer {
                        val s = 1.35f - 0.35f * enter.value
                        scaleX = s
                        scaleY = s
                        alpha = (enter.value * 2.2f).coerceIn(0f, 1f)
                    }
                    .then(if (enterBlur > 0.5.dp) Modifier.blur(enterBlur) else Modifier)
            ) {
                // Minimize/Close button at top right
                AnimatedVisibility(
                    visible = !isSuccess,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp)
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

                // Centered content (layout remains fixed during success so the centerpiece never shifts!)
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    AiLibraryIndicatorsAnimation(
                        isGenerating = true,
                        isSuccess = isSuccess,
                        heroArtworkUris = heroArtworkUris,
                        externalSuccessProgress = { successProgress.value }
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    val textAlpha = if (isSuccess) (1f - (p / 0.26f)).coerceIn(0f, 1f) else 1f
                    Box(
                        modifier = Modifier.graphicsLayer {
                            alpha = textAlpha
                            val textScale = 1f - 0.06f * (1f - textAlpha)
                            scaleX = textScale
                            scaleY = textScale
                        }
                    ) {
                        AiPlaylistCreationAnimationView(
                            status = status,
                            isSuccess = isSuccess,
                            error = error,
                            customHeadlines = customHeadlines,
                            customPhases = customPhases,
                            successHeadline = successHeadline,
                            successSubtitle = successSubtitle,
                            preferLiveStatus = preferLiveStatus,
                            onDismiss = onDismiss
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CreateAiPlaylistSheetContent(
    error: String?,
    heroArtworkUris: List<String>,
    onDismiss: () -> Unit,
    onGenerate: (playlistName: String?, prompt: String, minLength: Int, maxLength: Int) -> Unit
) {
    var basePrompt by rememberSaveable { mutableStateOf("") }
    var playlistName by rememberSaveable { mutableStateOf("") }
    var selectedLength by rememberSaveable { mutableStateOf("Auto") }
    var selectedEnergy by rememberSaveable { mutableStateOf("Auto") }
    var isRefining by rememberSaveable { mutableStateOf(false) }

    // Ambient, prompt-reactive background: soft drifting colour blobs that shift mood per vibe
    val ambientScheme = MaterialTheme.colorScheme
    val ambientDark = ambientScheme.surface.luminance() < 0.5f
    val ambientPick = if (basePrompt.isBlank()) 0 else (basePrompt.trim().lowercase().hashCode() and 0x7fffffff) % 3
    val ambientBase = listOf(
        if (ambientDark) ambientScheme.primary else ambientScheme.primaryContainer,
        if (ambientDark) ambientScheme.tertiary else ambientScheme.tertiaryContainer,
        if (ambientDark) ambientScheme.secondary else ambientScheme.secondaryContainer
    )
    val ambientA by animateColorAsState(targetValue = ambientBase[ambientPick], animationSpec = tween(800), label = "ambient_a")
    val ambientB by animateColorAsState(targetValue = ambientBase[(ambientPick + 1) % 3], animationSpec = tween(800), label = "ambient_b")
    val ambientC by animateColorAsState(targetValue = ambientBase[(ambientPick + 2) % 3], animationSpec = tween(800), label = "ambient_c")
    val ambientTransition = rememberInfiniteTransition(label = "ai_sheet_ambient")
    val ambientPhaseState = ambientTransition.animateFloat(
        initialValue = 0f,
        targetValue = 6.2831855f,
        animationSpec = infiniteRepeatable(
            animation = tween(16000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ambient_phase"
    )
    val ambientEnergy by animateFloatAsState(
        targetValue = if (basePrompt.isBlank()) 0.55f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessVeryLow),
        label = "ambient_energy"
    )
    // Hero shapes morph & spin smoothly (AOD style) whenever the vibe, prompt, or refine options change
    var heroFrom by remember { mutableIntStateOf(0) }
    var heroTo by remember { mutableIntStateOf(0) }
    val heroMorph = remember { Animatable(1f) }
    val promptSignature = remember(basePrompt, selectedEnergy, selectedLength) {
        val trimmed = basePrompt.trim().lowercase()
        if (trimmed.isEmpty()) 0 else ((trimmed.hashCode() * 31 + selectedEnergy.hashCode() * 7 + selectedLength.hashCode()) and 0x7fffffff) % 11 + 1
    }
    LaunchedEffect(promptSignature) {
        if (promptSignature != heroTo) {
            // Brief debounce while typing so rapid keystrokes coalesce into one silky morph
            if (basePrompt.isNotBlank()) delay(90L)
            heroFrom = heroTo
            heroTo = promptSignature
            heroMorph.snapTo(0f)
            heroMorph.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 820, easing = AiSmoothMorphEasing)
            )
        }
    }
    // Smooth, non-snapping breathing swell when prompt or vibe changes (zero 1-frame snap)
    val reactionKick = remember { Animatable(0f) }
    LaunchedEffect(promptSignature) {
        if (basePrompt.isNotEmpty()) {
            reactionKick.animateTo(
                targetValue = 0.45f,
                animationSpec = tween(durationMillis = 200, easing = AiSmoothMorphEasing)
            )
            reactionKick.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = 620, easing = AiSmoothMorphEasing)
            )
        }
    }

    val triggerGeneration: () -> Unit = {
        if (basePrompt.isNotBlank()) {
            val builtPrompt = buildString {
                append(basePrompt.trim())
                if (selectedEnergy != "Auto") {
                    append(". Energy: $selectedEnergy.")
                }
                if (selectedLength != "Auto") {
                    append(". Length: $selectedLength tracks.")
                }
            }
            val (minCount, maxCount) = when (selectedLength) {
                "Up to 15" -> 10 to 15
                "Up to 25" -> 15 to 25
                "Up to 50" -> 25 to 50
                else -> 12 to 30
            }
            onGenerate(playlistName.trim().ifBlank { null }, builtPrompt, minCount, maxCount)
        }
    }

    // Lock the bottom sheet dialog window against IME resize/pan jumps when selecting the text input field
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
        animationSpec = tween(durationMillis = 250, easing = AiSmoothMorphEasing),
        label = "ai_playlist_sheet_refine_progress"
    )
    val showMainPage = !isRefining || refineProgress < 0.999f
    val showRefinePage = isRefining || refineProgress > 0.001f
    val mainScrollState = rememberScrollState()
    // The Refine page is composed after the sheet has finished lifting so its fields and chips never add to the open frame
    var refineReady by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(650L)
        refineReady = true
    }

    // Keep both Main Tune and Refine views persistently composed & measured inside a shared ambient canvas
    // so that all glows and shadows are already present before opening the sheet AND when returning from Refine.
    Layout(
        modifier = Modifier
            .fillMaxWidth()
            .drawWithCache {
                // Unit-radius gradients are built once per colour and only translated/scaled per frame,
                // so the drifting glow never allocates shaders while the sheet is moving
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
                    .padding(bottom = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top Row: Clean close button
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

                // Persistently mounted AOD-style expressive hero stage in idle mode
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

                AiPlaylistMainTuneView(
                    prompt = basePrompt,
                    onPromptChange = { basePrompt = it },
                    selectedLength = selectedLength,
                    selectedEnergy = selectedEnergy,
                    error = error,
                    onOpenRefine = { isRefining = true },
                    onGenerate = triggerGeneration
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
                if (refineReady || isRefining) AiPlaylistRefineView(
                    prompt = basePrompt,
                    playlistName = playlistName,
                    onPlaylistNameChange = { playlistName = it },
                    selectedLength = selectedLength,
                    onLengthChange = { selectedLength = it },
                    selectedEnergy = selectedEnergy,
                    onEnergyChange = { selectedEnergy = it },
                    onBack = { isRefining = false },
                    onDismiss = onDismiss
                )
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
private fun AiPlaylistMainTuneView(
    prompt: String,
    onPromptChange: (String) -> Unit,
    selectedLength: String,
    selectedEnergy: String,
    error: String?,
    onOpenRefine: () -> Unit,
    onGenerate: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val primaryGlowColor = MaterialTheme.colorScheme.primary

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Large Headline
        Text(
            text = "What do you want to hear?",
            style = MaterialTheme.typography.headlineMedium.copy(
                fontFamily = GoogleSansRounded,
                fontWeight = FontWeight.Bold,
                fontSize = 26.sp
            ),
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 20.dp)
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "Describe a vibe, scenario, or mood",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 20.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Prompt input: tonal container with a pre-rendered Canvas glow that is already present on
        // Frame 0 before opening the sheet and during all Refine transitions (zero RenderNode pop-in)
        val fieldInteraction = remember { MutableInteractionSource() }
        val fieldFocused by fieldInteraction.collectIsFocusedAsState()
        val fieldActive = fieldFocused || prompt.isNotBlank()
        val fieldContainer by animateColorAsState(
            targetValue = if (fieldActive) MaterialTheme.colorScheme.surfaceContainerHighest else MaterialTheme.colorScheme.surfaceContainerHigh,
            animationSpec = tween(220),
            label = "field_container"
        )
        val fieldShape = remember { AbsoluteSmoothCornerShape(30.dp, 60) }
        Surface(
            shape = fieldShape,
            color = fieldContainer,
            shadowElevation = 0.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .aiSoftSurfaceGlow(
                    color = primaryGlowColor,
                    cornerRadius = 30.dp,
                    spread = 18.dp,
                    offsetY = 5.dp,
                    maxAlpha = if (isDark) 0.34f else 0.22f
                )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 14.dp, vertical = 12.dp)
                ) {
                    if (prompt.isEmpty()) {
                        Text(
                            text = "Upbeat morning…",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontSize = 19.sp,
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
                        )
                    }
                    BasicTextField(
                        value = prompt,
                        onValueChange = onPromptChange,
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = MaterialTheme.typography.titleMedium.copy(
                            fontSize = 19.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        interactionSource = fieldInteraction,
                        singleLine = false,
                        maxLines = 3
                    )
                }

                // Material You Expressive Action Button
                val canSubmit = prompt.isNotBlank()
                val interactionSource = remember { MutableInteractionSource() }
                val isPressed by interactionSource.collectIsPressedAsState()

                val buttonScale by animateFloatAsState(
                    targetValue = if (isPressed) 0.92f else 1f,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
                    label = "arrow_btn_scale"
                )
                val buttonCorner by animateDpAsState(
                    targetValue = if (isPressed) 28.dp else 20.dp,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
                    label = "arrow_btn_corner"
                )

                val arrowTilt by animateFloatAsState(
                    targetValue = if (canSubmit) 0f else -45f,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
                    label = "arrow_tilt"
                )
                val buttonContainer by animateColorAsState(
                    targetValue = if (canSubmit) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHighest,
                    animationSpec = tween(220),
                    label = "arrow_container"
                )
                val buttonContent by animateColorAsState(
                    targetValue = if (canSubmit) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f),
                    animationSpec = tween(220),
                    label = "arrow_content"
                )
                val buttonShape = remember(buttonCorner) { AbsoluteSmoothCornerShape(buttonCorner, 60) }

                Surface(
                    onClick = {
                        if (canSubmit) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onGenerate()
                        }
                    },
                    enabled = canSubmit,
                    interactionSource = interactionSource,
                    shape = buttonShape,
                    color = buttonContainer,
                    contentColor = buttonContent,
                    shadowElevation = 0.dp,
                    modifier = Modifier
                        .size(width = 64.dp, height = 54.dp)
                        .graphicsLayer {
                            scaleX = buttonScale
                            scaleY = buttonScale
                        }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                            contentDescription = "Generate",
                            modifier = Modifier
                                .size(24.dp)
                                .graphicsLayer { rotationZ = arrowTilt }
                        )
                    }
                }
            }
        }

        if (!error.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(12.dp))
            Surface(
                color = MaterialTheme.colorScheme.errorContainer,
                shape = AbsoluteSmoothCornerShape(16.dp, 60),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
            ) {
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(14.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Horizontal scrollable line of thick Material You expressive shortcut pills
        ExpressiveQuickSelectRow(
            selectedPrompt = prompt,
            onPillClick = { vibe ->
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onPromptChange(vibe)
            }
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Refine Section Card following app theme palette
        val refineCardShape = remember { AbsoluteSmoothCornerShape(22.dp, 60) }
        Surface(
            shape = refineCardShape,
            color = MaterialTheme.colorScheme.surfaceContainer,
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
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = AbsoluteSmoothCornerShape(12.dp, 60)
                ) {
                    Box(modifier = Modifier.padding(8.dp)) {
                        Icon(
                            painter = painterResource(R.drawable.rounded_tune_24),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Refine your mix",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RefineBadge(text = "Length: $selectedLength")
                        RefineBadge(text = "Energy: $selectedEnergy")
                    }
                }

                Icon(
                    painter = painterResource(R.drawable.rounded_chevron_right_24),
                    contentDescription = "Refine options",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun AiPlaylistRefineView(
    prompt: String,
    playlistName: String,
    onPlaylistNameChange: (String) -> Unit,
    selectedLength: String,
    onLengthChange: (String) -> Unit,
    selectedEnergy: String,
    onEnergyChange: (String) -> Unit,
    onBack: () -> Unit,
    onDismiss: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp)
            .padding(bottom = 32.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilledTonalIconButton(
                onClick = onBack,
                colors = IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    contentColor = MaterialTheme.colorScheme.onSurface
                )
            ) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
            }

            Text(
                text = "Refine your mix",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

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

        Spacer(modifier = Modifier.height(18.dp))

        // Prompt title echo
        Text(
            text = prompt.ifBlank { "Your mix" },
            style = MaterialTheme.typography.headlineSmall.copy(
                fontFamily = GoogleSansRounded,
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp
            ),
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Optional custom playlist name placed at the top so the keyboard never overlaps it
        Surface(
            shape = remember { AbsoluteSmoothCornerShape(22.dp, 60) },
            color = MaterialTheme.colorScheme.surfaceContainer,
            shadowElevation = 0.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Custom playlist title (optional)",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = playlistName,
                    onValueChange = onPlaylistNameChange,
                    placeholder = { Text("Auto-generated by AI if left blank") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = remember { AbsoluteSmoothCornerShape(14.dp, 60) }
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Playlist length card (Full-filled chips, no borders)
        Surface(
            shape = remember { AbsoluteSmoothCornerShape(22.dp, 60) },
            color = MaterialTheme.colorScheme.surfaceContainer,
            shadowElevation = 0.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Playlist length",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("Auto", "Up to 15", "Up to 25", "Up to 50").forEach { option ->
                        ExpressiveChoiceChip(
                            label = option,
                            selected = selectedLength == option,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onLengthChange(option)
                            }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Energy card (Full-filled chips, no borders)
        Surface(
            shape = remember { AbsoluteSmoothCornerShape(22.dp, 60) },
            color = MaterialTheme.colorScheme.surfaceContainer,
            shadowElevation = 0.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Energy",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("Auto", "Calm", "Steady", "Energetic").forEach { option ->
                        ExpressiveChoiceChip(
                            label = option,
                            selected = selectedEnergy == option,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onEnergyChange(option)
                            }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Auto follows your request. Selecting energy adds a requirement; fewer songs may match.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Done button following Material You app palette with shape morphing touch feedback
        val doneInteractionSource = remember { MutableInteractionSource() }
        val isDonePressed by doneInteractionSource.collectIsPressedAsState()
        val doneScale by animateFloatAsState(
            targetValue = if (isDonePressed) 0.95f else 1f,
            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
            label = "done_scale"
        )
        val doneCorner by animateDpAsState(
            targetValue = if (isDonePressed) 16.dp else 26.dp,
            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
            label = "done_corner"
        )

        Surface(
            onClick = onBack,
            interactionSource = doneInteractionSource,
            shape = AbsoluteSmoothCornerShape(doneCorner, 60),
            color = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
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

internal val AiSmoothMorphEasing = CubicBezierEasing(0.22f, 1f, 0.36f, 1f)

internal const val SHAPE_STEP_COUNT = 240
internal val COS_TABLE = FloatArray(SHAPE_STEP_COUNT) { i ->
    cos((2.0 * PI * i) / SHAPE_STEP_COUNT).toFloat()
}
internal val SIN_TABLE = FloatArray(SHAPE_STEP_COUNT) { i ->
    sin((2.0 * PI * i) / SHAPE_STEP_COUNT).toFloat()
}

/**
 * Curated Material You Expressive shapes ported directly from the AOD screen component:
 * - Soft cookies with gentle, shallow curves (never deeply pinched or jagged)
 * - Clean geometric expressive shapes: Pill, Oval, Rounded Rectangle, Rounded Diamond,
 *   Rounded Triangle, Squircle, and Soft Pentagon.
 */
internal enum class AiAodExpressiveShapeKind {
    // 0: 8-Sided Soft Cookie (subtle 3.6% wave)
    COOKIE_8 {
        override fun rawRadiusAt(th: Double): Double = 1.0 + 0.036 * cos(8.0 * th)
    },
    // 1: Material You Stadium Pill (tilted 28 deg for expressive balance)
    PILL {
        override fun rawRadiusAt(th: Double): Double {
            val a = th - (28.0 * PI / 180.0)
            val c = abs(cos(a)) / 0.78
            val s = abs(sin(a)) / 1.00
            return 1.0 / (c.pow(4.6) + s.pow(4.6)).pow(1.0 / 4.6)
        }
    },
    // 2: 6-Sided Soft Cookie (gentle 4.2% wave)
    COOKIE_6 {
        override fun rawRadiusAt(th: Double): Double = 1.0 + 0.042 * cos(6.0 * th)
    },
    // 3: Material You Rounded Diamond (45-deg rotated superellipse, convex sides + soft corners)
    ROUNDED_DIAMOND {
        override fun rawRadiusAt(th: Double): Double {
            val a = th - PI / 4.0
            val c = abs(cos(a))
            val s = abs(sin(a))
            return 1.0 / (c.pow(3.5) + s.pow(3.5)).pow(1.0 / 3.5)
        }
    },
    // 4: Material You Smooth Oval (tilted -22 deg)
    OVAL {
        override fun rawRadiusAt(th: Double): Double {
            val a = th + (22.0 * PI / 180.0)
            val c = cos(a) / 0.82
            val s = sin(a) / 1.00
            return 1.0 / sqrt(c * c + s * s)
        }
    },
    // 5: 4-Sided Soft Clover Cookie (gentle 4.6% cushion curve)
    COOKIE_4 {
        override fun rawRadiusAt(th: Double): Double = 1.0 + 0.046 * cos(4.0 * th + PI / 4.0)
    },
    // 6: Material You Soft Rounded Triangle (strictly convex with rounded vertices)
    ROUNDED_TRIANGLE {
        override fun rawRadiusAt(th: Double): Double {
            val a = th - PI / 2.0
            return 1.0 + 0.064 * cos(3.0 * a) - 0.011 * cos(6.0 * a)
        }
    },
    // 7: Material You Rounded Rectangle / Soft Squircle
    ROUNDED_RECTANGLE {
        override fun rawRadiusAt(th: Double): Double {
            val c = abs(cos(th)) / 0.88
            val s = abs(sin(th)) / 1.00
            return 1.0 / (c.pow(4.2) + s.pow(4.2)).pow(1.0 / 4.2)
        }
    },
    // 8: 12-Sided Sunny Cookie (refined 2.5% scallop)
    COOKIE_12 {
        override fun rawRadiusAt(th: Double): Double = 1.0 + 0.025 * cos(12.0 * th)
    },
    // 9: Material You Soft Pentagon
    ROUNDED_PENTAGON {
        override fun rawRadiusAt(th: Double): Double {
            val a = th - PI / 2.0
            return 1.0 + 0.034 * cos(5.0 * a) - 0.005 * cos(10.0 * a)
        }
    },
    // 10: 7-Sided Soft Cookie (gentle 3.6% wave)
    COOKIE_7 {
        override fun rawRadiusAt(th: Double): Double = 1.0 + 0.036 * cos(7.0 * th - PI / 2.0)
    },
    // 11: Material You 1:1 Squircle Cushion
    ROUNDED_SQUIRCLE {
        override fun rawRadiusAt(th: Double): Double {
            val c = abs(cos(th))
            val s = abs(sin(th))
            return 1.0 / (c.pow(4.0) + s.pow(4.0)).pow(0.25)
        }
    };

    abstract fun rawRadiusAt(th: Double): Double

    private val normScale: Double by lazy {
        val samples = 180
        var maxR = 0.001
        var sumR = 0.0
        for (i in 0 until samples) {
            val r = rawRadiusAt((2.0 * PI * i) / samples)
            if (r > maxR) maxR = r
            sumR += r
        }
        val meanR = sumR / samples
        (0.62 * maxR + 0.38 * meanR).coerceAtLeast(maxR * 0.94)
    }

    fun normalizedRadiusAt(th: Double): Float {
        return (rawRadiusAt(th) / normScale).toFloat().coerceAtMost(1.04f)
    }
}

private val AI_AOD_EXPRESSIVE_SHAPES = AiAodExpressiveShapeKind.entries

/**
 * 240-step polar morphing shape matching `AodMorphingAlbumShape` on the AOD screen.
 * Rotation is applied via GPU `graphicsLayer { rotationZ = ... }` (with counter-rotation on
 * the inner upright album art) so that `mainShape`, `outerHalo`, and `midHalo` share a single
 * cached `Shape` instance and spin at 120fps with zero per-frame `Path` allocations.
 */
private class AiAodMorphingShape(
    private val fromShape: AiAodExpressiveShapeKind,
    private val toShape: AiAodExpressiveShapeKind,
    private val shapeMorphProgress: Float
) : Shape {
    fun createPath(size: Size): Path {
        val w = size.width.coerceAtLeast(1f)
        val h = size.height.coerceAtLeast(1f)
        val cx = w * 0.5f
        val cy = h * 0.5f
        val baseRadius = min(w, h) * 0.476f
        val clampedMorph = shapeMorphProgress.coerceIn(0f, 1f)
        val twoPi = 2.0 * PI

        return Path().apply {
            for (i in 0 until SHAPE_STEP_COUNT) {
                val theta = (twoPi * i) / SHAPE_STEP_COUNT
                val rFrom = fromShape.normalizedRadiusAt(theta)
                val rTo = toShape.normalizedRadiusAt(theta)
                val r = baseRadius * ((1f - clampedMorph) * rFrom + clampedMorph * rTo)
                val x = cx + r * COS_TABLE[i]
                val y = cy + r * SIN_TABLE[i]
                if (i == 0) moveTo(x, y) else lineTo(x, y)
            }
            close()
        }
    }

    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        return Outline.Generic(createPath(size))
    }
}

/**
 * Material You Expressive polar shapes with 240-step precomputed lookup tables and refined,
 * smooth curves for floating accent shapes and 1:1 shortcut pill badges.
 */
internal enum class AiExpressiveShapeType {
    PILL {
        override fun radiusAt(th: Double): Float =
            AiAodExpressiveShapeKind.PILL.normalizedRadiusAt(th) * 0.94f
    },
    COOKIE {
        override fun radiusAt(th: Double): Float = (0.91 + 0.055 * cos(8.0 * th)).toFloat()
    },
    FOUR_HEAD_COOKIE {
        override fun radiusAt(th: Double): Float = (0.90 + 0.068 * cos(4.0 * th + PI / 4.0)).toFloat()
    },
    ROUNDED_TRIANGLE {
        override fun radiusAt(th: Double): Float =
            AiAodExpressiveShapeKind.ROUNDED_TRIANGLE.normalizedRadiusAt(th) * 0.94f
    },
    ROUNDED_SQUARE {
        override fun radiusAt(th: Double): Float =
            AiAodExpressiveShapeKind.ROUNDED_SQUIRCLE.normalizedRadiusAt(th) * 0.94f
    },
    CIRCLE {
        override fun radiusAt(th: Double): Float = 0.92f
    },
    SIX_COOKIE {
        override fun radiusAt(th: Double): Float = (0.91 + 0.060 * cos(6.0 * th)).toFloat()
    },
    SUNNY_COOKIE {
        override fun radiusAt(th: Double): Float = (0.92 + 0.040 * cos(12.0 * th)).toFloat()
    },
    FIVE_COOKIE {
        override fun radiusAt(th: Double): Float = (0.91 + 0.058 * cos(5.0 * th - PI / 2.0)).toFloat()
    },
    OVAL {
        override fun radiusAt(th: Double): Float =
            AiAodExpressiveShapeKind.OVAL.normalizedRadiusAt(th) * 0.94f
    },
    STAR {
        override fun radiusAt(th: Double): Float = (0.89 + 0.072 * cos(5.0 * th - PI / 2.0)).toFloat()
    },
    DIAMOND {
        override fun radiusAt(th: Double): Float =
            AiAodExpressiveShapeKind.ROUNDED_DIAMOND.normalizedRadiusAt(th) * 0.94f
    };

    abstract fun radiusAt(th: Double): Float

    val radii: FloatArray by lazy {
        FloatArray(SHAPE_STEP_COUNT) { i ->
            radiusAt((2.0 * PI * i) / SHAPE_STEP_COUNT)
        }
    }
}

private class AiMorphingExpressiveShape(
    private val from: AiExpressiveShapeType,
    private val to: AiExpressiveShapeType,
    private val progress: Float
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val clampedT = progress.coerceIn(0f, 1f)
        val fromRadii = from.radii
        val toRadii = to.radii
        val baseRadius = min(size.width, size.height) * 0.485f
        val centerX = size.width * 0.5f
        val centerY = size.height * 0.5f

        val path = Path().apply {
            for (i in 0 until SHAPE_STEP_COUNT) {
                val r = (1f - clampedT) * fromRadii[i] + clampedT * toRadii[i]
                val radiusPx = baseRadius * r
                val x = centerX + radiusPx * COS_TABLE[i]
                val y = centerY + radiusPx * SIN_TABLE[i]
                if (i == 0) moveTo(x, y) else lineTo(x, y)
            }
            close()
        }
        return Outline.Generic(path)
    }
}

/**
 * Fills an expressive morphing shape with a plain drawPath instead of clip + background, so no
 * stencil/path-clip mask is needed per frame. The morph progress is read in the draw phase only.
 */
internal fun Modifier.drawAiMorphShape(
    from: AiExpressiveShapeType,
    to: AiExpressiveShapeType,
    morphProgress: () -> Float,
    brush: Brush,
    alpha: Float = 1f
): Modifier = this.drawWithCache {
    val settledPath = (AiMorphingExpressiveShape(to, to, 1f)
        .createOutline(size, LayoutDirection.Ltr, this) as Outline.Generic).path
    onDrawBehind {
        val t = morphProgress().coerceIn(0f, 1f)
        val path = if (from == to || t >= 1f) {
            settledPath
        } else {
            (AiMorphingExpressiveShape(from, to, t)
                .createOutline(size, LayoutDirection.Ltr, this) as Outline.Generic).path
        }
        drawPath(path = path, brush = brush, alpha = alpha)
    }
}

/**
 * One floating accent blob of the hero stage. Every per-frame value arrives as a lambda and is read
 * inside graphicsLayer / draw blocks, so the continuous float, spin and breathing never recompose.
 */
@Composable
private fun AiFloatingAccentShape(
    spec: AiFloatingShapeSpec,
    index: Int,
    isGenerating: Boolean,
    isDark: Boolean,
    isSuccessActive: Boolean,
    convergeEase: Float,
    baseColor: Color,
    morphFrom: Int,
    morphTo: Int,
    morphProgress: () -> Float,
    floatPhase: () -> Float,
    scalePhase: () -> Float,
    spinDeg: () -> Float
) {
    val scheme = MaterialTheme.colorScheme
    val haloColor = if (isDark) {
        lerp(Color(0xFF09090B), baseColor, 0.38f)
    } else {
        lerp(scheme.surfaceContainerHigh, baseColor, 0.42f)
    }
    val topTone = lerp(baseColor, if (isDark) scheme.primaryContainer else Color.White, 0.22f)
    val fromType = aiHeroShapeFor(spec, index, morphFrom)
    val toType = aiHeroShapeFor(spec, index, morphTo)
    val bodyBrush = remember(topTone, baseColor) { Brush.linearGradient(listOf(topTone, baseColor)) }
    val haloBrush = remember(haloColor) { SolidColor(haloColor) }
    val fade = if (isSuccessActive) (1f - convergeEase).coerceIn(0f, 1f) else 1f
    // Alpha is baked into the fills (no group alpha), so no offscreen layer is needed per accent
    val bodyAlpha = (if (isDark) 0.85f else 0.75f) * fade
    val haloAlpha = bodyAlpha * (if (isDark) 0.44f else 0.34f)

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(spec.sizeDp.dp)
            .graphicsLayer {
                val phase = floatPhase() * spec.harmonic + spec.phaseOffset
                val amp = if (isGenerating) 1.5f else 1.0f
                translationX = (spec.initialX.dp.toPx() + (spec.floatRadiusX * amp).dp.toPx() * kotlin.math.cos(phase)) * fade
                translationY = (spec.initialY.dp.toPx() + (spec.floatRadiusY * amp).dp.toPx() * kotlin.math.sin(phase)) * fade
                val s = if (isSuccessActive) {
                    fade
                } else {
                    1.0f + (if (isGenerating) 0.10f else 0.04f) * kotlin.math.sin(scalePhase() + spec.phaseOffset)
                }
                scaleX = s
                scaleY = s
            }
    ) {
        // Subtle +18 degree rotated AOD-style backing halo
        Box(
            modifier = Modifier
                .requiredSize((spec.sizeDp * 1.14f).dp)
                .graphicsLayer {
                    rotationZ = spec.initialRotation + spinDeg() * spec.spinDirection + 18f * spec.spinDirection
                }
                .drawAiMorphShape(fromType, toType, morphProgress, haloBrush, haloAlpha)
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    rotationZ = spec.initialRotation + spinDeg() * spec.spinDirection
                }
                .drawAiMorphShape(fromType, toType, morphProgress, bodyBrush, bodyAlpha)
        )
    }
}

/**
 * Expressive AI Playlist Creation button copying the exact shape and size
 * of the Daily Mix generator button: RoundedStarShape(sides = 8, curve = 0.05, rotation = 0f)
 * on a LargeExtendedFloatingActionButton.
 */
@Composable
fun AiMixCookieButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val starShape = remember {
        RoundedStarShape(
            sides = 8,
            curve = 0.05,
            rotation = 0f
        )
    }

    LargeExtendedFloatingActionButton(
        modifier = modifier,
        onClick = onClick,
        shape = starShape,
        containerColor = MaterialTheme.colorScheme.primaryContainer,
        elevation = FloatingActionButtonDefaults.elevation()
    ) {
        Icon(
            painter = painterResource(R.drawable.rounded_auto_awesome_24),
            contentDescription = "Create AI Playlist",
            modifier = Modifier.size(36.dp)
        )
    }
}

/** Picks the shape a floating hero blob takes for a given vibe variant (0 = its resting shape). */
private fun aiHeroShapeFor(spec: AiFloatingShapeSpec, index: Int, variant: Int): AiExpressiveShapeType {
    if (variant == 0) return spec.shapeType
    val all = AiExpressiveShapeType.entries
    return all[(spec.shapeType.ordinal + variant * 3 + index % 2) % all.size]
}

/**
 * AOD-Screen Expressive Morphing Hero Component adapted for the AI Playlist page.
 * Features:
 * 1. Unclipped `.requiredSize(...)` ambient radial palette glow that breathes smoothly.
 * 2. Signature AOD `+18°` phase-rotated tonal backing halo (`haloShape`, solid fill, no wireframe)
 *    plus a `-14°` intermediate cushion halo for deep Material You Expressive layering.
 * 3. 240-step normalized `AiAodExpressiveShapeKind` main morphing shape with smooth
 *    `AiSmoothMorphEasing` shape morph + rotational spin on tap, vibe/prompt changes, and
 *    autonomous idle/generating cycles.
 */
@Composable
private fun AiAodExpressiveHeroComponent(
    index: Int,
    color: Color,
    containerTone: Color,
    glowColor: Color,
    isDark: Boolean,
    isGenerating: Boolean,
    isSuccess: Boolean,
    heroArtworkUris: List<String>,
    convergeFade: Float,
    haloFade: Float = 1f,
    artAlpha: Float = 1f,
    glowExpansion: Float,
    morphTrigger: Int,
    continuousSpinDeg: () -> Float,
    ambientGlowScale: () -> Float,
    ambientGlowAlpha: () -> Float,
    blobSize: Dp,
    onTapMorph: () -> Unit,
    modifier: Modifier = Modifier
) {
    val initialIdx = remember(index) {
        when (index) {
            0 -> 5 // COOKIE_4 (Soft Clover Cookie)
            1 -> 0 // COOKIE_8 (8-Sided Soft Cookie centerpiece)
            else -> 2 // COOKIE_6 (6-Sided Soft Cookie)
        }
    }
    var previousShapeIndex by remember { mutableIntStateOf(initialIdx) }
    var targetShapeIndex by remember { mutableIntStateOf(initialIdx) }
    var artCycleStep by remember { mutableIntStateOf(index) }
    val shapeMorphAnim = remember { Animatable(1f) }
    val spinRotationDeg = remember { Animatable(index * 32f) }
    val spinDir = if (index % 2 == 0) 1f else -1f

    // Autonomous smooth morph + spin + artwork crossfade loop
    // Calm staggered cadence when idle; rapid, high-energy spinning & morphing when generating
    LaunchedEffect(isGenerating, isSuccess) {
        if (isSuccess) return@LaunchedEffect
        delay(if (isGenerating) 120L + index * 160L else 650L + index * 850L)
        var loopCount = 0
        while (true) {
            loopCount++
            val step = if (index == 1) 1 else 2
            val nextIdx = (targetShapeIndex + step) % AI_AOD_EXPRESSIVE_SHAPES.size
            previousShapeIndex = targetShapeIndex
            targetShapeIndex = nextIdx
            if (!isGenerating || loopCount % 2 == 0) {
                artCycleStep++
            }
            val durationMs = if (isGenerating) 520 else 920
            val spinDelta = (if (isGenerating) 145f else 90f) * spinDir
            shapeMorphAnim.snapTo(0f)
            launch {
                shapeMorphAnim.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = durationMs, easing = AiSmoothMorphEasing)
                )
            }
            launch {
                spinRotationDeg.animateTo(
                    targetValue = spinRotationDeg.value + spinDelta,
                    animationSpec = tween(durationMillis = durationMs, easing = AiSmoothMorphEasing)
                )
            }
            delay(if (isGenerating) 580L + index * 90L else 3400L + index * 650L)
        }
    }

    // Immediate AOD-style morph + spin + artwork crossfade when user selects a vibe, edits prompt, or taps
    var firstTriggerRun by remember { mutableStateOf(true) }
    LaunchedEffect(morphTrigger) {
        if (firstTriggerRun) {
            firstTriggerRun = false
            return@LaunchedEffect
        }
        if (isSuccess) return@LaunchedEffect
        val jump = ((morphTrigger + index) % 3) + 1
        val nextIdx = (targetShapeIndex + jump) % AI_AOD_EXPRESSIVE_SHAPES.size
        previousShapeIndex = targetShapeIndex
        targetShapeIndex = nextIdx
        artCycleStep++
        shapeMorphAnim.snapTo(0f)
        launch {
            shapeMorphAnim.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 820, easing = AiSmoothMorphEasing)
            )
        }
        launch {
            spinRotationDeg.animateTo(
                targetValue = spinRotationDeg.value + 115f * spinDir,
                animationSpec = tween(durationMillis = 820, easing = AiSmoothMorphEasing)
            )
        }
    }

    // Read lazily from graphicsLayer blocks only, so the continuous spin never recomposes the hero
    val effectiveRotationDeg: () -> Float = { spinRotationDeg.value + continuousSpinDeg() * spinDir }
    val quantizedMorph = (shapeMorphAnim.value * 60f).roundToInt() / 60f

    // Single cached morphing Shape shared across mainShape, outerHalo, and midHalo (zero per-frame Path churn)
    val morphShape = remember(
        previousShapeIndex,
        targetShapeIndex,
        quantizedMorph
    ) {
        AiAodMorphingShape(
            fromShape = AI_AOD_EXPRESSIVE_SHAPES[previousShapeIndex],
            toShape = AI_AOD_EXPRESSIVE_SHAPES[targetShapeIndex],
            shapeMorphProgress = quantizedMorph
        )
    }

    val innerPetalShape = remember(
        previousShapeIndex,
        targetShapeIndex,
        quantizedMorph
    ) {
        AiAodMorphingShape(
            fromShape = AI_AOD_EXPRESSIVE_SHAPES[(previousShapeIndex + 2) % AI_AOD_EXPRESSIVE_SHAPES.size],
            toShape = AI_AOD_EXPRESSIVE_SHAPES[(targetShapeIndex + 2) % AI_AOD_EXPRESSIVE_SHAPES.size],
            shapeMorphProgress = quantizedMorph
        )
    }

    // Derive rich AOD-style tonal palette colors
    val outerHaloColor = remember(color, containerTone, isDark) {
        if (isDark) {
            lerp(Color(0xFF09090B), color, 0.34f)
        } else {
            lerp(containerTone, color, 0.36f)
        }
    }
    val midHaloColor = remember(outerHaloColor, color) {
        lerp(outerHaloColor, color, 0.44f)
    }
    val topVelvetTone = remember(color, containerTone, isDark) {
        if (isDark) {
            lerp(color, containerTone, 0.24f)
        } else {
            lerp(color, Color.White, 0.22f)
        }
    }
    val bottomVelvetTone = remember(color, outerHaloColor, isDark) {
        lerp(color, outerHaloColor, if (isDark) 0.36f else 0.22f)
    }
    val innerPetalTone = remember(color, topVelvetTone) {
        lerp(color, topVelvetTone, 0.55f)
    }

    // Pick a distinct recently played album art URI for this hero piece (index 0, 1, 2 never collide)
    val currentArtUri = remember(heroArtworkUris, artCycleStep, index) {
        if (heroArtworkUris.isEmpty()) {
            null
        } else {
            heroArtworkUris[(artCycleStep * 3 + index).coerceAtLeast(0) % heroArtworkUris.size]
        }
    }

    // Artwork eases in once it is available so late-arriving covers never pop in during the sheet motion
    val artReveal = animateFloatAsState(
        targetValue = if (currentArtUri != null) 1f else 0f,
        animationSpec = tween(durationMillis = 450, easing = FastOutSlowInEasing),
        label = "ai_hero_art_reveal"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(blobSize)
            .then(
                if (isGenerating) Modifier.testTag("loading_indicator") else Modifier
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onTapMorph
            )
    ) {
        // Layer 1: Unclipped ambient radial palette glow (requiredSize prevents parent Box clamping!)
        if (haloFade > 0.01f) {
            Box(
                modifier = Modifier
                    .requiredSize(blobSize * (if (isGenerating) 1.92f else 1.65f) * glowExpansion)
                    .graphicsLayer {
                        val glowScale = ambientGlowScale()
                        scaleX = glowScale
                        scaleY = glowScale
                        alpha = ambientGlowAlpha() * convergeFade * haloFade
                    }
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                glowColor.copy(alpha = if (isDark) 0.46f else 0.36f),
                                glowColor.copy(alpha = if (isDark) 0.15f else 0.12f),
                                Color.Transparent
                            )
                        )
                    )
            )

            // Layer 2: Signature AOD +18° rotated tonal expressive backing halo (drawn directly via Path, zero clipPath!)
            Box(
                modifier = Modifier
                    .requiredSize(blobSize * 1.13f)
                    .graphicsLayer {
                        rotationZ = effectiveRotationDeg() + 18f * spinDir
                        alpha = (if (isDark) 0.56f else 0.44f) * convergeFade * haloFade
                    }
                    .drawWithCache {
                        val path = morphShape.createPath(size)
                        onDrawBehind {
                            drawPath(path = path, color = outerHaloColor)
                        }
                    }
            )

            // Layer 3: Counter-rotated -14° expressive cushion halo (drawn directly via Path, zero clipPath!)
            Box(
                modifier = Modifier
                    .requiredSize(blobSize * 1.055f)
                    .graphicsLayer {
                        rotationZ = effectiveRotationDeg() - 14f * spinDir
                        alpha = (if (isDark) 0.48f else 0.38f) * convergeFade * haloFade
                    }
                    .drawWithCache {
                        val path = morphShape.createPath(size)
                        onDrawBehind {
                            drawPath(path = path, color = midHaloColor)
                        }
                    }
            )
        }

        // Layer 4: Main 240-step AOD Expressive Morphing Shape.
        // Uses direct GPU drawPath for the velvet gradient so that when this single merged component
        // zooms 16x across the screen, zero giant Outline.Generic clip masks are ever allocated!
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(blobSize)
                .graphicsLayer {
                    rotationZ = effectiveRotationDeg()
                    alpha = convergeFade
                }
                .drawWithCache {
                    val path = morphShape.createPath(size)
                    val brush = Brush.linearGradient(
                        colors = listOf(topVelvetTone, color, bottomVelvetTone),
                        start = Offset(0f, 0f),
                        end = Offset(size.width, size.height)
                    )
                    onDrawBehind {
                        drawPath(path = path, brush = brush)
                    }
                }
        ) {
            if (artAlpha > 0.01f) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer { alpha = artAlpha }
                        .clip(morphShape)
                ) {
                    // Fallback sculptural inner petal (visible when no cached artwork is available)
                    Box(
                        modifier = Modifier
                            .size(blobSize * 0.64f)
                            .graphicsLayer {
                                rotationZ = 24f * spinDir
                                alpha = if (isDark) 0.28f else 0.32f
                            }
                            .drawWithCache {
                                val path = innerPetalShape.createPath(size)
                                val brush = Brush.linearGradient(
                                    colors = listOf(innerPetalTone, color.copy(alpha = 0.6f))
                                )
                                onDrawBehind {
                                    drawPath(path = path, brush = brush)
                                }
                            }
                    )

                    // Upright crossfading recently played album art from cache (counter-rotated against shape spin)
                    if (currentArtUri != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer {
                                    rotationZ = -effectiveRotationDeg()
                                    alpha = artReveal.value
                                    // 1.15x scale ensures the square artwork completely covers the rotating polar lobes
                                    scaleX = 1.15f
                                    scaleY = 1.15f
                                }
                        ) {
                            Crossfade(
                                targetState = currentArtUri,
                                animationSpec = tween(durationMillis = 680, easing = FastOutSlowInEasing),
                                label = "ai_hero_art_crossfade"
                            ) { artUri ->
                                SmartImage(
                                    model = artUri,
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    targetSize = SmartImageListTargetSize,
                                    useDiskCache = true,
                                    useMemoryCache = true,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }

                        // Subtle Material You tonal glaze & edge vignette so the artwork harmonizes with each hero color
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer { alpha = artReveal.value }
                                .background(
                                    Brush.radialGradient(
                                        colors = listOf(
                                            color.copy(alpha = if (isDark) 0.14f else 0.10f),
                                            color.copy(alpha = if (isDark) 0.24f else 0.18f),
                                            outerHaloColor.copy(alpha = if (isDark) 0.48f else 0.34f)
                                        )
                                    )
                                )
                        )
                    }
                }
            }
        }
    }
}

private data class AiFloatingShapeSpec(
    val shapeType: AiExpressiveShapeType,
    val sizeDp: Float,
    val initialX: Float,
    val initialY: Float,
    val initialRotation: Float,
    val colorIndex: Int,
    val phaseOffset: Float,
    val floatRadiusX: Float = 8f,
    val floatRadiusY: Float = 10f,
    val spinDirection: Float = 1.0f,
    val harmonic: Float = 1.0f
)

private val FLOATING_EXPRESSIVE_SHAPES = listOf(
    // 1. Top far-left: Olive Oval
    AiFloatingShapeSpec(
        shapeType = AiExpressiveShapeType.OVAL,
        sizeDp = 42f,
        initialX = -142f,
        initialY = -88f,
        initialRotation = -24f,
        colorIndex = 4,
        phaseOffset = 0.0f,
        floatRadiusX = 8f,
        floatRadiusY = 10f,
        spinDirection = -1f
    ),
    // 2. Top left-mid: Peach 4-Head Cookie
    AiFloatingShapeSpec(
        shapeType = AiExpressiveShapeType.FOUR_HEAD_COOKIE,
        sizeDp = 46f,
        initialX = -58f,
        initialY = -98f,
        initialRotation = 16f,
        colorIndex = 3,
        phaseOffset = 0.57f,
        floatRadiusX = 9f,
        floatRadiusY = 11f,
        spinDirection = 1f
    ),
    // 3. Top center-mid: Tan 6-Pointed Cookie
    AiFloatingShapeSpec(
        shapeType = AiExpressiveShapeType.SIX_COOKIE,
        sizeDp = 40f,
        initialX = 48f,
        initialY = -100f,
        initialRotation = -14f,
        colorIndex = 5,
        phaseOffset = 1.14f,
        floatRadiusX = 7f,
        floatRadiusY = 9f,
        spinDirection = -1f
    ),
    // 4. Top right: Brown 5-Pointed Star
    AiFloatingShapeSpec(
        shapeType = AiExpressiveShapeType.STAR,
        sizeDp = 48f,
        initialX = 140f,
        initialY = -82f,
        initialRotation = 22f,
        colorIndex = 0,
        phaseOffset = 1.71f,
        floatRadiusX = 10f,
        floatRadiusY = 12f,
        spinDirection = 1f
    ),
    // 5. Bottom far-left: Tan 8-Pointed Cookie / Star
    AiFloatingShapeSpec(
        shapeType = AiExpressiveShapeType.COOKIE,
        sizeDp = 46f,
        initialX = -146f,
        initialY = 64f,
        initialRotation = 12f,
        colorIndex = 5,
        phaseOffset = 2.28f,
        floatRadiusX = 9f,
        floatRadiusY = 10f,
        spinDirection = -1f
    ),
    // 6. Bottom left-mid: Sage Diamond
    AiFloatingShapeSpec(
        shapeType = AiExpressiveShapeType.DIAMOND,
        sizeDp = 32f,
        initialX = -60f,
        initialY = 92f,
        initialRotation = 45f,
        colorIndex = 1,
        phaseOffset = 2.85f,
        floatRadiusX = 8f,
        floatRadiusY = 9f,
        spinDirection = 1f
    ),
    // 7. Bottom center-right: Rust 12-Pointed Sun Cookie
    AiFloatingShapeSpec(
        shapeType = AiExpressiveShapeType.SUNNY_COOKIE,
        sizeDp = 38f,
        initialX = 56f,
        initialY = 94f,
        initialRotation = -20f,
        colorIndex = 3,
        phaseOffset = 3.42f,
        floatRadiusX = 8f,
        floatRadiusY = 11f,
        spinDirection = -1f
    ),
    // 8. Bottom right: Sage Rounded Square
    AiFloatingShapeSpec(
        shapeType = AiExpressiveShapeType.ROUNDED_SQUARE,
        sizeDp = 44f,
        initialX = 138f,
        initialY = 62f,
        initialRotation = 30f,
        colorIndex = 4,
        phaseOffset = 4.0f,
        floatRadiusX = 9f,
        floatRadiusY = 10f,
        spinDirection = 1f
    ),
    // 9. Accent small star: High top-left
    AiFloatingShapeSpec(
        shapeType = AiExpressiveShapeType.STAR,
        sizeDp = 24f,
        initialX = -106f,
        initialY = -118f,
        initialRotation = 6f,
        colorIndex = 2,
        phaseOffset = 4.57f,
        floatRadiusX = 6f,
        floatRadiusY = 8f,
        spinDirection = -1f
    ),
    // 10. Accent small 4-head cookie: High top-right
    AiFloatingShapeSpec(
        shapeType = AiExpressiveShapeType.FOUR_HEAD_COOKIE,
        sizeDp = 26f,
        initialX = 104f,
        initialY = -120f,
        initialRotation = -18f,
        colorIndex = 1,
        phaseOffset = 5.14f,
        floatRadiusX = 7f,
        floatRadiusY = 8f,
        spinDirection = 1f
    ),
    // 11. Accent small diamond: Low bottom-center
    AiFloatingShapeSpec(
        shapeType = AiExpressiveShapeType.DIAMOND,
        sizeDp = 22f,
        initialX = 0f,
        initialY = 110f,
        initialRotation = 35f,
        colorIndex = 2,
        phaseOffset = 5.71f,
        floatRadiusX = 6f,
        floatRadiusY = 7f,
        spinDirection = 1f
    )
)

@Composable
internal fun AiLibraryIndicatorsAnimation(
    isGenerating: Boolean,
    isSuccess: Boolean = false,
    heroArtworkUris: List<String> = emptyList(),
    morphFrom: Int = 0,
    morphTo: Int = 0,
    morphProgress: () -> Float = { 1f },
    externalSuccessProgress: (() -> Float)? = null,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val haptic = LocalHapticFeedback.current
    var tapMorphCount by remember { mutableIntStateOf(0) }
    val infiniteTransition = rememberInfiniteTransition(label = "lib_indicators")

    // Slow floating translation phase when idle (cycle 7200ms)
    val idleFloatPhase = infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 6.2831855f, // 2 * PI
        animationSpec = infiniteRepeatable(
            animation = tween(7200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "idle_float_phase"
    )

    // Smooth dynamic movement when generating (cycle 2400ms)
    val genFloatPhase = infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 6.2831855f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "gen_float_phase"
    )

    // AOD-style ambient glow breathing scale & alpha
    val aodGlowScale = infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.10f,
        animationSpec = infiniteRepeatable(
            tween(4600, easing = FastOutSlowInEasing),
            RepeatMode.Reverse
        ),
        label = "aod_glow_scale"
    )
    val aodGlowAlpha = infiniteTransition.animateFloat(
        initialValue = 0.72f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            tween(3400, easing = FastOutSlowInEasing),
            RepeatMode.Reverse
        ),
        label = "aod_glow_alpha"
    )

    // Continuous slow rotation when idle (32s cycle) vs energetic fast spin when generating (2.0s cycle)
    val idleShapeSpin = infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(32000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "idle_shape_spin"
    )

    val genShapeSpin = infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "gen_shape_spin"
    )

    val genFloaterSpin = infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(3200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "gen_floater_spin"
    )

    // Zooming in/out pulse phase when generating (cycle 1400ms)
    val genScalePhase = infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 6.2831855f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "gen_scale_phase"
    )

    // Subtle gentle breathing pulse phase when idle (cycle 4200ms)
    val idleScalePhase = infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 6.2831855f,
        animationSpec = infiniteRepeatable(
            animation = tween(4200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "idle_scale_phase"
    )

    // Base indicator size: 158dp when idle -> 182dp when generating (smooth non-bouncy transition)
    val baseIndicatorSize by animateDpAsState(
        targetValue = if (isGenerating) 182.dp else 158.dp,
        animationSpec = tween(durationMillis = 520, easing = AiSmoothMorphEasing),
        label = "base_indicator_size"
    )

    // Animate container height: 248dp when idle -> 350dp when generating
    val containerHeight by animateDpAsState(
        targetValue = if (isGenerating) 350.dp else 248.dp,
        animationSpec = tween(durationMillis = 520, easing = AiSmoothMorphEasing),
        label = "container_height"
    )

    // Horizontal spacing between indicators relative to the center
    val spacingPx = with(density) {
        if (isGenerating) 138.dp.toPx() else 118.dp.toPx()
    }

    // Fallback local success progress if not driven by parent AiPlaylistGeneratingFullScreenPage
    val internalSuccessProgress = remember { Animatable(0f) }
    LaunchedEffect(isSuccess, externalSuccessProgress == null) {
        if (externalSuccessProgress == null) {
            if (isSuccess) {
                internalSuccessProgress.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(1400, easing = LinearEasing)
                )
            } else {
                internalSuccessProgress.snapTo(0f)
            }
        }
    }

    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f

    val indicatorColors = listOf(
        MaterialTheme.colorScheme.primary,
        MaterialTheme.colorScheme.tertiary,
        MaterialTheme.colorScheme.secondary
    )

    val containerTones = listOf(
        MaterialTheme.colorScheme.primaryContainer,
        MaterialTheme.colorScheme.tertiaryContainer,
        MaterialTheme.colorScheme.secondaryContainer
    )

    // For glows: in dark mode, vibrant bright tones; in light mode, luminous pastel container tones
    val glowColors = listOf(
        if (isDark) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primaryContainer,
        if (isDark) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.tertiaryContainer,
        if (isDark) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.secondaryContainer
    )

    val scheme = MaterialTheme.colorScheme
    val shapeColorPalette = remember(
        scheme.primary,
        scheme.tertiary,
        scheme.secondary,
        scheme.primaryContainer,
        scheme.tertiaryContainer,
        scheme.secondaryContainer,
        isDark
    ) {
        listOf(
            if (isDark) scheme.primary.copy(alpha = 0.85f) else scheme.primary.copy(alpha = 0.75f),
            if (isDark) scheme.tertiary.copy(alpha = 0.85f) else scheme.tertiary.copy(alpha = 0.75f),
            if (isDark) scheme.secondary.copy(alpha = 0.85f) else scheme.secondary.copy(alpha = 0.75f),
            if (isDark) scheme.primaryContainer.copy(alpha = 0.90f) else scheme.primaryContainer.copy(alpha = 0.80f),
            if (isDark) scheme.tertiaryContainer.copy(alpha = 0.90f) else scheme.tertiaryContainer.copy(alpha = 0.80f),
            if (isDark) scheme.secondaryContainer.copy(alpha = 0.90f) else scheme.secondaryContainer.copy(alpha = 0.80f)
        )
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(containerHeight),
        contentAlignment = Alignment.Center
    ) {
        val p = externalSuccessProgress?.invoke() ?: internalSuccessProgress.value
        val isSuccessActive = isSuccess && p > 0f
        val convergeT = (p / 0.36f).coerceIn(0f, 1f)
        val convergeEase = FastOutSlowInEasing.transform(convergeT)
        val expandT = ((p - 0.32f) / 0.38f).coerceIn(0f, 1f)
        val expandEase = FastOutSlowInEasing.transform(expandT)
        val revealT = ((p - 0.70f) / 0.30f).coerceIn(0f, 1f)
        val portalAlpha = (1f - FastOutSlowInEasing.transform(revealT)).coerceIn(0f, 1f)

        // Floating Expressive Material You shapes around the 3 main components.
        // Per-frame phases are passed as lambdas and read in graphicsLayer / draw blocks only,
        // so the idle float, spin and breathing never recompose anything.
        for ((index, spec) in FLOATING_EXPRESSIVE_SHAPES.withIndex()) {
            if (!isSuccessActive || convergeEase < 0.98f) {
                AiFloatingAccentShape(
                    spec = spec,
                    index = index,
                    isGenerating = isGenerating,
                    isDark = isDark,
                    isSuccessActive = isSuccessActive,
                    convergeEase = convergeEase,
                    baseColor = shapeColorPalette[spec.colorIndex % shapeColorPalette.size],
                    morphFrom = morphFrom,
                    morphTo = morphTo,
                    morphProgress = morphProgress,
                    floatPhase = { if (isGenerating) genFloatPhase.value else idleFloatPhase.value },
                    scalePhase = { if (isGenerating) genScalePhase.value else idleScalePhase.value },
                    spinDeg = { if (isGenerating) genFloaterSpin.value else idleShapeSpin.value }
                )
            }
        }

        // Render flanking companions (0 = Left, 2 = Right) first and Centerpiece Hero (1 = Center) on top
        // so the focal AOD hero shape sits cleanly in the foreground without awkward side overlap.
        for (i in listOf(0, 2, 1)) {
            val offset = i * (6.2831855f / 3f)
            val baseColor = indicatorColors[i % indicatorColors.size]
            val baseContainerTone = containerTones[i % containerTones.size]
            val baseGlowColor = glowColors[i % glowColors.size]

            // As the 3 components converge into 1, the side components morph their colors into the centerpiece
            val color = if (isSuccessActive && i != 1) lerp(baseColor, indicatorColors[1], convergeEase) else baseColor
            val containerTone = if (isSuccessActive && i != 1) lerp(baseContainerTone, containerTones[1], convergeEase) else baseContainerTone
            val glowColor = if (isSuccessActive && i != 1) lerp(baseGlowColor, glowColors[1], convergeEase) else baseGlowColor

            // Base position relative to exact center of screen
            val baseX = when (i) {
                0 -> -spacingPx
                2 -> spacingPx
                else -> 0f
            }
            val baseY = when (i) {
                1 -> with(density) { (-4).dp.toPx() }
                else -> with(density) { 8.dp.toPx() }
            }

            // Focal hierarchy: Centerpiece (i == 1) is larger (1.06x), companions (0, 2) are 0.86x
            val hierarchyScale = if (i == 1) 1.06f else 0.86f

            // Dynamic scale: side components glide into the center component and fuse into it,
            // then that single unified centerpiece component zooms in (up to 16x) to cover the screen!
            val successScale = when {
                isSuccessActive -> {
                    if (i == 1) {
                        val convergeSwell = hierarchyScale + (convergeEase * 0.22f)
                        val zoomExpansion = expandEase * expandEase * 15.0f
                        convergeSwell + zoomExpansion
                    } else {
                        val sideShrink = 1f - 0.25f * convergeEase
                        val finalMergeCollapse = 1f - ((convergeT - 0.72f) / 0.28f).coerceIn(0f, 1f)
                        (hierarchyScale * sideShrink * finalMergeCollapse).coerceAtLeast(0f)
                    }
                }
                else -> 0f
            }

            // Side pieces stay opaque while sliding inward, fading only as they overlap the center piece;
            // the unified centerpiece stays at 1.0f during merge + zoom and dissolves during reveal.
            val convergeFade = when {
                !isSuccessActive -> 1f
                i != 1 -> (1f - ((convergeT - 0.52f) / 0.44f)).coerceIn(0f, 1f)
                else -> portalAlpha
            }

            val haloFade = if (isSuccessActive && i == 1) (1f - expandEase).coerceIn(0f, 1f) else 1f
            val artAlpha = if (isSuccessActive) (1f - convergeEase).coerceIn(0f, 1f) else 1f

            val currentBaseX = if (isSuccessActive) baseX * (1f - convergeEase) else baseX
            val currentBaseY = if (isSuccessActive) baseY * (1f - convergeEase) else baseY

            val glowExpansion = if (isSuccessActive && i == 1) {
                1f + (convergeEase * 0.35f)
            } else {
                1f
            }

            if ((!isSuccessActive || successScale > 0.001f) && convergeFade > 0.001f) {
                Box(
                    modifier = Modifier
                        .size(baseIndicatorSize)
                        .graphicsLayer {
                            // Orbit + breathing are evaluated in the layer phase, so the idle loop never recomposes the heroes
                            val orbitPhase = if (isGenerating) genFloatPhase.value else idleFloatPhase.value
                            val orbitFade = if (isSuccessActive) 1f - convergeEase else 1f
                            val breathe = if (isSuccessActive) {
                                successScale
                            } else if (isGenerating) {
                                hierarchyScale * (1.03f + 0.08f * kotlin.math.sin(genScalePhase.value + offset))
                            } else {
                                hierarchyScale * (1.0f + 0.028f * kotlin.math.sin(idleScalePhase.value + offset))
                            }
                            scaleX = breathe
                            scaleY = breathe
                            translationX = currentBaseX +
                                (if (isGenerating) 12.dp.toPx() else 8.dp.toPx()) * kotlin.math.cos(orbitPhase + offset) * orbitFade
                            translationY = currentBaseY +
                                (if (isGenerating) 16.dp.toPx() else 10.dp.toPx()) * kotlin.math.sin(orbitPhase + offset) * orbitFade
                        },
                    contentAlignment = Alignment.Center
                ) {
                    AiAodExpressiveHeroComponent(
                        index = i,
                        color = color,
                        containerTone = containerTone,
                        glowColor = glowColor,
                        isDark = isDark,
                        isGenerating = isGenerating,
                        isSuccess = isSuccess,
                        heroArtworkUris = heroArtworkUris,
                        convergeFade = convergeFade,
                        haloFade = haloFade,
                        artAlpha = artAlpha,
                        glowExpansion = glowExpansion,
                        morphTrigger = morphTo + tapMorphCount,
                        continuousSpinDeg = { if (isGenerating) genShapeSpin.value else idleShapeSpin.value },
                        ambientGlowScale = { aodGlowScale.value },
                        ambientGlowAlpha = { aodGlowAlpha.value },
                        blobSize = baseIndicatorSize,
                        onTapMorph = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            tapMorphCount++
                        }
                    )
                }
            }
        }
    }
}

/**
 * Horizontal scrollable line of thick, expressive Material You shortcut pills.
 * Each pill has a left-side 1:1 Material You expressive shape (Cookie, 4-Head Cookie,
 * Circle, Rounded Triangle, 1:1 Rounded Square, etc.) housing an icon, right-side text,
 * and rich Material You shape-morphing touch feedback.
 */
private data class VibeItem(
    val label: String,
    @DrawableRes val iconRes: Int,
    val baseShape: AiExpressiveShapeType,
    val morphedShape: AiExpressiveShapeType
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ExpressiveQuickSelectRow(
    selectedPrompt: String,
    onPillClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val vibes = remember {
        listOf(
            VibeItem(
                label = "Feel good",
                iconRes = R.drawable.rounded_whatshot_24,
                baseShape = AiExpressiveShapeType.COOKIE,
                morphedShape = AiExpressiveShapeType.FOUR_HEAD_COOKIE
            ),
            VibeItem(
                label = "Upbeat morning",
                iconRes = R.drawable.rounded_wb_sunny_24,
                baseShape = AiExpressiveShapeType.FOUR_HEAD_COOKIE,
                morphedShape = AiExpressiveShapeType.SUNNY_COOKIE
            ),
            VibeItem(
                label = "Workout energy",
                iconRes = R.drawable.rounded_fitness_center_24,
                baseShape = AiExpressiveShapeType.ROUNDED_TRIANGLE,
                morphedShape = AiExpressiveShapeType.SIX_COOKIE
            ),
            VibeItem(
                label = "Chill evening",
                iconRes = R.drawable.rounded_nights_stay_24,
                baseShape = AiExpressiveShapeType.CIRCLE,
                morphedShape = AiExpressiveShapeType.FOUR_HEAD_COOKIE
            ),
            VibeItem(
                label = "Late night drive",
                iconRes = R.drawable.rounded_directions_car_24,
                baseShape = AiExpressiveShapeType.ROUNDED_SQUARE,
                morphedShape = AiExpressiveShapeType.COOKIE
            ),
            VibeItem(
                label = "Music to study",
                iconRes = R.drawable.rounded_headphones_24,
                baseShape = AiExpressiveShapeType.SIX_COOKIE,
                morphedShape = AiExpressiveShapeType.ROUNDED_TRIANGLE
            ),
            VibeItem(
                label = "Party hype",
                iconRes = R.drawable.rounded_celebration_24,
                baseShape = AiExpressiveShapeType.SUNNY_COOKIE,
                morphedShape = AiExpressiveShapeType.FOUR_HEAD_COOKIE
            ),
            VibeItem(
                label = "Acoustic coffee",
                iconRes = R.drawable.rounded_music_note_24,
                baseShape = AiExpressiveShapeType.COOKIE,
                morphedShape = AiExpressiveShapeType.ROUNDED_SQUARE
            ),
            VibeItem(
                label = "Nostalgic 2000s",
                iconRes = R.drawable.rounded_history_24,
                baseShape = AiExpressiveShapeType.FOUR_HEAD_COOKIE,
                morphedShape = AiExpressiveShapeType.CIRCLE
            ),
            VibeItem(
                label = "Piano peaceful",
                iconRes = R.drawable.rounded_piano_24,
                baseShape = AiExpressiveShapeType.ROUNDED_SQUARE,
                morphedShape = AiExpressiveShapeType.SIX_COOKIE
            ),
            VibeItem(
                label = "Deep focus",
                iconRes = R.drawable.rounded_whatshot_24,
                baseShape = AiExpressiveShapeType.ROUNDED_TRIANGLE,
                morphedShape = AiExpressiveShapeType.COOKIE
            ),
            VibeItem(
                label = "Radio hits",
                iconRes = R.drawable.rounded_graphic_eq_24,
                baseShape = AiExpressiveShapeType.CIRCLE,
                morphedShape = AiExpressiveShapeType.SUNNY_COOKIE
            )
        )
    }

    val scrollState = rememberScrollState()
    val sharedBadgeSpinState = rememberInfiniteTransition(label = "shared_pill_badge_spin").animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(24000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shared_pill_badge_spin_value"
    )
    val rows = remember(vibes) {
        List(3) { rowIdx ->
            vibes.mapIndexedNotNull { idx, item ->
                if (idx % 3 == rowIdx) idx to item else null
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        rows.forEach { rowItems ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                rowItems.forEach { (index, vibe) ->
                    val isSelected = selectedPrompt.trim().equals(vibe.label, ignoreCase = true)
                    ExpressiveQuickSelectPill(
                        vibe = vibe,
                        index = index,
                        isSelected = isSelected,
                        badgeSpinProvider = { sharedBadgeSpinState.value },
                        onClick = { onPillClick(vibe.label) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ExpressiveQuickSelectPill(
    vibe: VibeItem,
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
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "pill_scale"
    )

    // Stadium pill idle (28.dp radius for 58.dp height), morphing to an expressive asymmetrical shape on press/select
    val cornerTL by animateDpAsState(
        targetValue = if (isPressed || isSelected) (if (index % 2 == 0) 14.dp else 28.dp) else 28.dp,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "pill_ctl"
    )
    val cornerBR by animateDpAsState(
        targetValue = if (isPressed || isSelected) (if (index % 2 == 0) 14.dp else 28.dp) else 28.dp,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "pill_cbr"
    )
    val cornerTR by animateDpAsState(
        targetValue = if (isPressed || isSelected) (if (index % 2 == 0) 28.dp else 14.dp) else 28.dp,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "pill_ctr"
    )
    val cornerBL by animateDpAsState(
        targetValue = if (isPressed || isSelected) (if (index % 2 == 0) 28.dp else 14.dp) else 28.dp,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "pill_cbl"
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
        label = "icon_morph"
    )

    val isHighlighted = isPressed || isSelected

    val containerColor by animateColorAsState(
        targetValue = if (isHighlighted) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainerHigh
        },
        animationSpec = tween(150),
        label = "pill_color"
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
        animationSpec = tween(150),
        label = "badge_color"
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
        animationSpec = tween(150),
        label = "icon_tint"
    )

    val pillTilt by animateFloatAsState(
        targetValue = if (isPressed) (if (index % 2 == 0) -2.5f else 2.5f) else 0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "pill_tilt"
    )
    val spinDir = if (index % 2 == 0) 1f else -1f
    val pillGradientAlpha = (0.20f + 0.35f * morphFraction).coerceIn(0f, 1f)
    val badgeHighlight = androidx.compose.ui.graphics.lerp(badgeColor, Color.White, 0.30f)

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
            // Left side 1:1 Material You expressive shape (reads spin inside graphicsLayer so zero idle recomposition!)
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .graphicsLayer {
                        rotationZ = badgeSpinProvider() * spinDir + morphFraction * (if (index % 2 == 0) 25f else -25f)
                    }
                    .drawAiMorphShape(
                        from = vibe.baseShape,
                        to = vibe.morphedShape,
                        morphProgress = { morphFraction },
                        brush = Brush.linearGradient(listOf(badgeHighlight, badgeColor))
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(vibe.iconRes),
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

            // Right side bold text label with dynamic horizontal width matching the label length
            Text(
                text = vibe.label,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontFamily = GoogleSansRounded,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                ),
                color = if (isHighlighted) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
                maxLines = 1,
                softWrap = false
            )
        }
    }
}

/**
 * Full-filled choice chip (no borders) with shape morphing & spring touch feedback.
 */
@Composable
internal fun ExpressiveChoiceChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.93f else 1.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "chip_scale"
    )

    val cornerRadius by animateDpAsState(
        targetValue = if (selected) 22.dp else if (isPressed) 18.dp else 12.dp,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "chip_corner"
    )

    val containerColor by animateColorAsState(
        targetValue = when {
            selected -> MaterialTheme.colorScheme.primary
            isPressed -> MaterialTheme.colorScheme.surfaceContainerHighest
            else -> MaterialTheme.colorScheme.surfaceContainerHigh
        },
        animationSpec = tween(180),
        label = "chip_color"
    )

    val contentColor = if (selected) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    val chipShape = remember(cornerRadius) { AbsoluteSmoothCornerShape(cornerRadius, 60) }

    Surface(
        onClick = onClick,
        interactionSource = interactionSource,
        shape = chipShape,
        color = containerColor,
        contentColor = contentColor,
        shadowElevation = 0.dp,
        modifier = Modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                maxLines = 1,
                softWrap = false
            )
        }
    }
}

/**
 * Draws a pre-computed, zero-allocation soft ambient + spot glow directly onto the Canvas.
 * Unlike hardware RenderNode elevation shadows (`Modifier.shadow` / `shadowElevation`), this is
 * never suppressed or delayed by ancestor `graphicsLayer` alpha transitions (sheet opening or
 * Refine page crossfades) and avoids CPU `Outline.Generic` path shadow tessellation.
 */
internal fun Modifier.aiSoftSurfaceGlow(
    color: Color,
    cornerRadius: Dp,
    spread: Dp,
    offsetY: Dp = 4.dp,
    maxAlpha: Float = 0.22f
): Modifier = this.drawBehind {
    if (maxAlpha <= 0.001f || spread <= 0.dp) return@drawBehind
    val spreadPx = spread.toPx()
    val maxOffsetYPx = offsetY.toPx()
    val baseCornerPx = cornerRadius.toPx()
    val steps = 8
    val layerWeightSum = (steps * (steps + 1)) / 2f
    for (i in steps downTo 1) {
        val fraction = i.toFloat() / steps.toFloat()
        val expand = spreadPx * fraction
        val dy = maxOffsetYPx * (0.25f + 0.75f * fraction)
        val weight = (steps - i + 1).toFloat() / layerWeightSum
        val layerAlpha = (maxAlpha * weight * 1.35f).coerceIn(0f, 1f)
        val r = (baseCornerPx + expand * 0.85f).coerceAtLeast(0f)
        drawRoundRect(
            color = color.copy(alpha = layerAlpha),
            topLeft = Offset(-expand, -expand * 0.55f + dy),
            size = Size(size.width + expand * 2f, size.height + expand * 1.55f),
            cornerRadius = CornerRadius(r, r)
        )
    }
}

@Composable
internal fun RefineBadge(text: String) {
    Surface(
        shape = AbsoluteSmoothCornerShape(10.dp, 60),
        color = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun AiPlaylistCreationAnimationView(
    status: String?,
    isSuccess: Boolean,
    error: String?,
    customHeadlines: List<String>? = null,
    customPhases: List<String>? = null,
    successHeadline: String = "Mix synthesized!",
    successSubtitle: String = "Opening your new playlist now…",
    preferLiveStatus: Boolean = false,
    onDismiss: () -> Unit
) {
    val headlines = remember(customHeadlines) {
        customHeadlines ?: listOf(
            "Creating your AI playlist…",
            "Matching your vibe…",
            "AI is thinking…",
            "Scanning your music library…",
            "Reading the mood & energy…",
            "Picking the best tracks…",
            "Curating smooth transitions…",
            "Sequencing your custom mix…"
        )
    }
    val phases = remember(customPhases) {
        customPhases ?: listOf(
            "Tuning to your taste & listening patterns…",
            "Scanning local & cloud tracks for matching vibes…",
            "Discovering fresh recommendations & flow…",
            "Balancing familiar favorites & deep cuts…",
            "Harmonizing energy transitions & playlist order…",
            "Synthesizing smart titles & final touches…"
        )
    }
    var headlineIndex by remember { mutableIntStateOf(0) }
    var phaseIndex by remember { mutableIntStateOf(0) }
    LaunchedEffect(isSuccess, error) {
        if (!isSuccess && error == null) {
            launch {
                while (true) {
                    kotlinx.coroutines.delay(1750)
                    headlineIndex = (headlineIndex + 1) % headlines.size
                }
            }
            launch {
                while (true) {
                    kotlinx.coroutines.delay(2150)
                    phaseIndex = (phaseIndex + 1) % phases.size
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(top = 4.dp, bottom = 36.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Dynamic animated headline cycling smoothly one by one
        AnimatedContent(
            targetState = if (isSuccess) {
                successHeadline
            } else {
                headlines[headlineIndex % headlines.size]
            },
            transitionSpec = {
                (slideInVertically(
                    animationSpec = tween(480, easing = AiSmoothMorphEasing),
                    initialOffsetY = { fullHeight -> (fullHeight * 0.65f).toInt() }
                ) + fadeIn(
                    animationSpec = tween(420, easing = FastOutSlowInEasing)
                ) + scaleIn(
                    initialScale = 0.92f,
                    animationSpec = tween(480, easing = AiSmoothMorphEasing)
                )) togetherWith (slideOutVertically(
                    animationSpec = tween(380, easing = FastOutSlowInEasing),
                    targetOffsetY = { fullHeight -> -(fullHeight * 0.65f).toInt() }
                ) + fadeOut(
                    animationSpec = tween(280, easing = FastOutSlowInEasing)
                ) + scaleOut(
                    targetScale = 0.92f,
                    animationSpec = tween(380, easing = FastOutSlowInEasing)
                ))
            },
            label = "ai_generation_headline_text"
        ) { currentHeadline ->
            Text(
                text = currentHeadline,
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontFamily = GoogleSansRounded,
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp
                ),
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Animated stage subtitle cycling smoothly through detailed progress lines
        AnimatedContent(
            targetState = if (isSuccess) {
                successSubtitle
            } else if (preferLiveStatus && !status.isNullOrBlank()) {
                status
            } else {
                phases[phaseIndex % phases.size]
            },
            transitionSpec = {
                (slideInVertically(
                    animationSpec = tween(440, easing = AiSmoothMorphEasing),
                    initialOffsetY = { height -> height / 2 }
                ) + fadeIn(tween(380))) togetherWith
                        (slideOutVertically(
                            animationSpec = tween(340, easing = FastOutSlowInEasing),
                            targetOffsetY = { height -> -height / 2 }
                        ) + fadeOut(tween(260)))
            },
            label = "ai_generation_phase_text"
        ) { currentText ->
            Text(
                text = currentText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }

        Spacer(modifier = Modifier.height(28.dp))

        // Animated Equalizer Visualizer Bars
        if (!isSuccess && error == null) {
            AnimatedEqualizerBars()
        }

        // If error occurred
        if (!error.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(16.dp))
            Surface(
                color = MaterialTheme.colorScheme.errorContainer,
                shape = AbsoluteSmoothCornerShape(16.dp, 60),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    FilledTonalButton(onClick = onDismiss) {
                        Text("Dismiss")
                    }
                }
            }
        }
    }
}

@Composable
private fun AnimatedEqualizerBars() {
    val infiniteTransition = rememberInfiniteTransition(label = "eq_bars_transition")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 6.28318f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "eq_phase"
    )

    val primary = MaterialTheme.colorScheme.primary
    val tertiary = MaterialTheme.colorScheme.tertiary

    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.height(28.dp)
    ) {
        listOf(0.0f, 1.2f, 2.4f, 3.6f, 4.8f).forEach { offset ->
            val heightFraction = 0.3f + 0.7f * ((kotlin.math.sin(phase + offset) + 1f) / 2f)
            Box(
                modifier = Modifier
                    .width(5.dp)
                    .height((26 * heightFraction).dp)
                    .clip(CircleShape)
                    .background(
                        brush = Brush.verticalGradient(
                            listOf(primary, tertiary)
                        )
                    )
            )
        }
    }
}
