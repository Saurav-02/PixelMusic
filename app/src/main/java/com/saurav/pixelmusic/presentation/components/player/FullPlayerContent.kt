package com.saurav.pixelmusic.presentation.components.player

import com.saurav.pixelmusic.presentation.components.player.styles.ExpressivePlayerLayout
import com.saurav.pixelmusic.presentation.components.player.styles.PillarsPlayerLayout
import com.saurav.pixelmusic.presentation.components.player.styles.PlayerStyleParams
import com.saurav.pixelmusic.presentation.components.player.styles.WavePlayerLayout
import android.annotation.SuppressLint
import android.os.Build
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.animation.animateColorAsState
import android.content.Context
import android.content.res.Configuration
import androidx.compose.foundation.layout.offset
import android.net.Uri
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.TransformOrigin
import com.saurav.pixelmusic.data.model.Lyrics
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.ui.unit.IntSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.ui.draw.alpha
import androidx.compose.animation.fadeIn
import coil.imageLoader
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LoadingIndicator
// import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults // Removed
// import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState // Removed
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.material3.rememberBottomSheetState
import com.saurav.pixelmusic.presentation.components.SmoothModalBottomSheet
import androidx.compose.material3.SheetValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.coerceAtLeast
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Share
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.material3.HorizontalDivider
import com.saurav.pixelmusic.presentation.components.TimerOptionsBottomSheet
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.draw.shadow
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.ModalBottomSheet
import android.widget.Toast
import androidx.compose.foundation.LocalOverscrollFactory
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.unit.Constraints
import kotlin.math.roundToInt
import com.saurav.pixelmusic.presentation.components.SmartImage
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.runtime.derivedStateOf
import androidx.compose.ui.res.stringResource
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import com.saurav.pixelmusic.R
import com.saurav.pixelmusic.data.model.Artist
import com.saurav.pixelmusic.data.model.Song
import com.saurav.pixelmusic.data.preferences.AlbumArtQuality
import com.saurav.pixelmusic.data.preferences.CarouselStyle
import com.saurav.pixelmusic.data.preferences.FullPlayerLoadingTweaks
import com.saurav.pixelmusic.presentation.components.AlbumCarouselSection
import com.saurav.pixelmusic.presentation.components.AutoScrollingTextOnDemand
import com.saurav.pixelmusic.presentation.components.LocalMaterialTheme
import com.saurav.pixelmusic.presentation.components.LyricsSheet
import com.saurav.pixelmusic.presentation.components.ShareBottomSheet
import com.saurav.pixelmusic.presentation.viewmodel.SongInfoBottomSheetViewModel
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.saurav.pixelmusic.presentation.components.PlaylistBottomSheet
import com.saurav.pixelmusic.presentation.viewmodel.PlaylistViewModel
import com.saurav.pixelmusic.presentation.components.scoped.rememberSmoothProgress
import com.saurav.pixelmusic.presentation.components.subcomps.FetchLyricsDialog
import com.saurav.pixelmusic.presentation.viewmodel.LyricsSearchUiState
import com.saurav.pixelmusic.presentation.viewmodel.PlayerSheetState
import com.saurav.pixelmusic.presentation.viewmodel.PlayerViewModel
import com.saurav.pixelmusic.ui.theme.GoogleSansRounded
import com.saurav.pixelmusic.ui.theme.LocalPixelMusicDarkTheme
import com.saurav.pixelmusic.utils.AudioMetaUtils.mimeTypeToFormat
import com.saurav.pixelmusic.utils.LyricsImportFailureReason
import com.saurav.pixelmusic.utils.LyricsImportSecurity
import com.saurav.pixelmusic.utils.LyricsImportValidationResult
import com.saurav.pixelmusic.utils.ValidatedLyricsImport
import com.saurav.pixelmusic.utils.formatDuration
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import racra.compose.smooth_corner_rect_library.AbsoluteSmoothCornerShape
import timber.log.Timber
import java.util.Locale
import kotlin.math.roundToLong
import com.saurav.pixelmusic.presentation.components.WavySliderExpressive
import com.saurav.pixelmusic.presentation.components.ToggleSegmentButton
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.withContext
import androidx.compose.ui.draw.scale

private const val PREVIOUS_TRACK_RESTART_THRESHOLD_MS = 10_000L
private const val SKIP_COMMAND_GUARD_MS = 96L

private enum class SkipDirection { PREVIOUS, NEXT }

private suspend fun validateLyricsImport(
    context: Context,
    uri: Uri
): LyricsImportValidationResult = withContext(Dispatchers.IO) {
    val contentResolver = context.contentResolver

    var fileName = ""
    var fileSize: Long? = null
    contentResolver.query(uri, null, null, null, null)?.use { cursor ->
        val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
        val sizeIndex = cursor.getColumnIndex(android.provider.OpenableColumns.SIZE)
        if (cursor.moveToFirst()) {
            fileName = if (nameIndex != -1) cursor.getString(nameIndex) else ""
            fileSize = if (sizeIndex != -1 && !cursor.isNull(sizeIndex)) {
                cursor.getLong(sizeIndex)
            } else {
                null
            }
        }
    }

    contentResolver.openInputStream(uri)?.use { inputStream ->
        LyricsImportSecurity.validateImportedLyricsFile(
            fileName = fileName,
            mimeType = contentResolver.getType(uri),
            inputStream = inputStream,
            reportedSizeBytes = fileSize
        )
    } ?: LyricsImportValidationResult.Invalid(LyricsImportFailureReason.EMPTY_CONTENT)
}

@androidx.annotation.OptIn(UnstableApi::class)
@SuppressLint("StateFlowValueCalledInComposition")
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun FullPlayerContent(
    currentSong: Song?,
    currentPlaybackQueue: ImmutableList<Song>,
    currentQueueSourceName: String,
    currentMediaItemIndex: Int = -1,
    isShuffleEnabled: Boolean,
    shuffleTransitionInProgress: Boolean,
    repeatMode: Int,
    allowRealtimeUpdates: Boolean = true,
    expansionFractionProvider: () -> Float,
    currentSheetState: PlayerSheetState,
    carouselStyle: String,
    loadingTweaks: FullPlayerLoadingTweaks,
    isSheetDragGestureActive: Boolean = false,
    playerViewModel: PlayerViewModel, // For stable state like totalDuration and lyrics
    // State Providers
    currentPositionProvider: () -> Long,
    isPlayingProvider: () -> Boolean,
    playWhenReadyProvider: () -> Boolean,
    isFavoriteProvider: () -> Boolean,
    repeatModeProvider: () -> Int,
    isShuffleEnabledProvider: () -> Boolean,
    totalDurationProvider: () -> Long,
    lyricsProvider: () -> Lyrics? = { null }, 
    // State
    isCastConnecting: Boolean = false,
    // Event Handlers
    onPlayPause: () -> Unit,
    onSeek: (Long) -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onCollapse: () -> Unit,
    onShowQueueClicked: () -> Unit,
    onQueueDragStart: () -> Unit,
    onQueueDrag: (Float) -> Unit,
    onQueueRelease: (Float, Float) -> Unit,
    onShowCastClicked: () -> Unit,
    onShuffleToggle: () -> Unit,
    onRepeatToggle: () -> Unit,
    onFavoriteToggle: () -> Unit,
    sharedAodSourceBounds: AodSourceBounds? = null
) {
    var retainedSong by remember { mutableStateOf(currentSong) }
    LaunchedEffect(currentSong?.id) {
        if (currentSong != null) {
            retainedSong = currentSong
        }
    }

    val song = currentSong ?: retainedSong ?: return // Keep the player visible while transitioning
    var showSongInfoBottomSheet by remember { mutableStateOf(false) }
    var showPlaylistBottomSheet by remember { mutableStateOf(false) }
    var showLyricsSheet by remember { mutableStateOf(false) }
    var showShareSheet by remember { mutableStateOf(false) }
    var showSleepTimerSheet by rememberSaveable { mutableStateOf(false) }
    var showArtistPicker by rememberSaveable { mutableStateOf(false) }
    val activeTimerValueDisplay by playerViewModel.activeTimerValueDisplay.collectAsStateWithLifecycle()
    
    val lyricsSearchUiState by playerViewModel.lyricsSearchUiState.collectAsStateWithLifecycle()
    val playerState by playerViewModel.stablePlayerState.collectAsStateWithLifecycle()
    val lyricsLines = remember(playerState.lyrics) {
        val lyrics = playerState.lyrics
        if (lyrics != null) {
            lyrics.synced?.map { it.line }?.filter { it.isNotBlank() }
                ?: lyrics.plain?.filter { it.isNotBlank() }
                ?: emptyList()
        } else {
            emptyList()
        }
    }
    
    val playerDesignStyle by playerViewModel.playerDesignStyle.collectAsStateWithLifecycle()
    
    val nowPlayingLyricsStyle by playerViewModel.userPreferencesRepository.nowPlayingLyricsStyleFlow
        .collectAsStateWithLifecycle(initialValue = com.saurav.pixelmusic.data.preferences.NowPlayingLyricsStyle.HIDDEN)
    val isImmersive = playerDesignStyle == com.saurav.pixelmusic.data.preferences.PlayerDesignStyle.IMMERSIVE || 
                      playerDesignStyle == com.saurav.pixelmusic.data.preferences.PlayerDesignStyle.IMMERSIVE_EXTENDED
    val isImmersiveExtended = playerDesignStyle == com.saurav.pixelmusic.data.preferences.PlayerDesignStyle.IMMERSIVE_EXTENDED
    val isComplex = playerDesignStyle == com.saurav.pixelmusic.data.preferences.PlayerDesignStyle.COMPLEX
    val isCustomStyle = playerDesignStyle == com.saurav.pixelmusic.data.preferences.PlayerDesignStyle.WAVE ||
                        playerDesignStyle == com.saurav.pixelmusic.data.preferences.PlayerDesignStyle.PILLARS ||
                        playerDesignStyle == com.saurav.pixelmusic.data.preferences.PlayerDesignStyle.EXPRESSIVE

    // Single subscription — replaces 11 independent collectAsStateWithLifecycle calls.
    // distinctUntilChanged in the ViewModel ensures this only emits when something
    // actually changed, batching multiple rapid updates into one recomposition.
    val fullPlayerSlice by playerViewModel.fullPlayerSlice.collectAsStateWithLifecycle()
    val aodScreenEnabled = fullPlayerSlice.aodScreenEnabled
    val aodScreenStyle = fullPlayerSlice.aodScreenStyle
    val currentSongArtists = fullPlayerSlice.currentSongArtists
    val lyricsSyncOffset = fullPlayerSlice.lyricsSyncOffset
    val albumArtQuality = fullPlayerSlice.albumArtQuality
    val playbackAudioMetadata = fullPlayerSlice.audioMetadata
    val showPlayerFileInfo = fullPlayerSlice.showPlayerFileInfo
    val immersiveLyricsEnabled = fullPlayerSlice.immersiveLyricsEnabled
    val immersiveLyricsTimeout = fullPlayerSlice.immersiveLyricsTimeout
    val isImmersiveTemporarilyDisabled = fullPlayerSlice.isImmersiveTemporarilyDisabled
    val isRemotePlaybackActive = fullPlayerSlice.isRemotePlaybackActive
    val selectedRouteName = fullPlayerSlice.selectedRouteName
    val isBluetoothEnabled = fullPlayerSlice.isBluetoothEnabled
    val bluetoothName = fullPlayerSlice.bluetoothName
    val navigationBarBottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val queueGestureBottomExclusion = maxOf(20.dp, navigationBarBottomInset + 8.dp)
    val queueGestureBottomExclusionPx = with(LocalDensity.current) {
        queueGestureBottomExclusion.toPx()
    }

    var showFetchLyricsDialog by remember { mutableStateOf(false) }
    var showAodScreen by remember { mutableStateOf(false) }
    val aodMorphAnim = remember { Animatable(0f) }
    val aodSourceBounds = sharedAodSourceBounds ?: remember { AodSourceBounds() }
    val aodTransitionProgressProvider = remember(aodMorphAnim) { { aodMorphAnim.value } }
    val isLyricsAodActive = aodScreenStyle == com.saurav.pixelmusic.data.preferences.AodScreenStyle.LYRICS &&
        !playerState.lyrics?.synced.isNullOrEmpty()
    val latestIsLyricsAod by rememberUpdatedState(isLyricsAodActive)
    val nowPlayingAodHideProvider = remember(aodMorphAnim) {
        { if (latestIsLyricsAod) 0f else aodMorphAnim.value }
    }
    val aodVisible by remember {
        androidx.compose.runtime.derivedStateOf { showAodScreen || aodMorphAnim.value > 0.001f }
    }
    var totalDrag by remember { mutableStateOf(0f) }

    LaunchedEffect(showAodScreen) {
        val isLyricsAod = aodScreenStyle == com.saurav.pixelmusic.data.preferences.AodScreenStyle.LYRICS &&
            !playerState.lyrics?.synced.isNullOrEmpty()
        val enterCurve = if (isLyricsAod) {
            CubicBezierEasing(0.16f, 1f, 0.3f, 1f)
        } else {
            CubicBezierEasing(0.22f, 0.68f, 0.35f, 1f)
        }
        val exitCurve = CubicBezierEasing(0.25f, 0.1f, 0.25f, 1f)
        if (showAodScreen) {
            aodMorphAnim.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = if (isLyricsAod) 760 else 520, easing = enterCurve)
            )
        } else if (aodMorphAnim.value > 0f) {
            aodMorphAnim.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = if (isLyricsAod) 560 else 400, easing = exitCurve)
            )
        }
    }

    val context = LocalContext.current
    val fileImportScope = rememberCoroutineScope()
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri: Uri? ->
            uri?.let {
                fileImportScope.launch {
                    try {
                        val validation = validateLyricsImport(context, it)
                        val validatedImport: ValidatedLyricsImport = when (validation) {
                            is LyricsImportValidationResult.Valid -> validation.value
                            is LyricsImportValidationResult.Invalid -> {
                                playerViewModel.sendToast(
                                    LyricsImportSecurity.messageFor(validation.reason)
                                )
                                return@launch
                            }
                        }

                        val currentSongId = currentSong?.id?.toLongOrNull()
                        if (currentSongId == null) {
                            playerViewModel.sendToast("No song selected for lyrics import.")
                            return@launch
                        }

                        playerViewModel.importLyricsFromFile(currentSongId, validatedImport)
                        showFetchLyricsDialog = false
                        showLyricsSheet = true
                    } catch (e: Exception) {
                        Timber.e(e, "Error reading imported lyrics file")
                        playerViewModel.sendToast("Error reading file.")
                    }
                }
            }
        }
    )

    // totalDurationValue is derived from stablePlayerState, so it's fine.
    // OPTIMIZATION: Use passed provider instead of collecting flow
    val totalDurationValue = totalDurationProvider()

    // ----------------------------------------------------
    // STANDARD MATERIAL 3 COLORS
    // ----------------------------------------------------
    val baseTheme = LocalMaterialTheme.current
    val playerOnBaseColor = baseTheme.onPrimaryContainer
    val playerAccentColor = baseTheme.primary
    val playerOnAccentColor = baseTheme.onPrimary

    val transportPlayPauseColors = TransportButtonColors(container = playerAccentColor, content = playerOnAccentColor)
    val transportSkipButtonColors = TransportButtonColors(container = playerOnAccentColor, content = playerAccentColor)

    val topBarButtonBg = if (isComplex || isCustomStyle) playerOnAccentColor else playerOnAccentColor.copy(alpha = 0.78f)
    val topBarIconColor = playerAccentColor

    val progressActiveColor = playerOnBaseColor
    val progressInactiveColor = playerOnBaseColor.copy(alpha = 0.2f)

    val placeholderColor = playerOnBaseColor.copy(alpha = 0.1f)
    val placeholderOnColor = playerOnBaseColor.copy(alpha = 0.2f)

    val gradientEdgeColor = baseTheme.primaryContainer
    val chipColor = playerOnAccentColor.copy(alpha = 0.8f)
    val chipContentColor = playerAccentColor

    val isLandscape =
        LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE

    val coroutineScope = rememberCoroutineScope()

    // Lógica para el botón de Lyrics en el reproductor expandido
    val onLyricsClick = {
        val lyrics = lyricsProvider()
        if (lyrics?.synced.isNullOrEmpty() && lyrics?.plain.isNullOrEmpty()) {
            showFetchLyricsDialog = true
        } else {
            showLyricsSheet = true
        }
    }

    // AodScreen is rendered as an in-window morphing overlay at the end of FullPlayerContent

    if (showFetchLyricsDialog) {
        MaterialTheme(
            colorScheme = LocalMaterialTheme.current,
            typography = MaterialTheme.typography,
            shapes = MaterialTheme.shapes
        ) {
            FetchLyricsDialog(
                uiState = lyricsSearchUiState,
                currentSong = song, // Use 'song' which is derived from args/retained
                onConfirm = { forcePick ->
                    playerViewModel.fetchLyricsForCurrentSong(forcePick)
                },
                onPickResult = { result ->
                    playerViewModel.acceptLyricsSearchResultForCurrentSong(result)
                },
                onManualSearch = { title, artist, autoApply ->
                    playerViewModel.searchLyricsManually(title, artist, autoApply)
                },
                onDismiss = {
                    showFetchLyricsDialog = false
                    playerViewModel.resetLyricsSearchState()
                },
                onImport = {
                    filePickerLauncher.launch(com.saurav.pixelmusic.utils.LyricsImportSecurity.pickerMimeTypes())
                }
            )
        }
    }

    // Observador para reaccionar al resultado de la búsqueda de letras
    LaunchedEffect(lyricsSearchUiState) {
        when (val state = lyricsSearchUiState) {
            is LyricsSearchUiState.Success -> {
                if (showFetchLyricsDialog) {
                    showFetchLyricsDialog = false
                    showLyricsSheet = true
                    playerViewModel.resetLyricsSearchState()
                }
            }
            is LyricsSearchUiState.Error -> {
            }
            else -> Unit
        }
    }

    val onAlbumSongSelected: (Song) -> Unit = { newSong ->
        playerViewModel.showAndPlaySong(
            song = newSong,
            contextSongs = currentPlaybackQueue,
            queueName = currentQueueSourceName
        )
    }

    val onSongMetadataQueueClick = {
        onShowQueueClicked()
    }

    val onSongMetadataArtistClick = {
        val primaryArtist = currentSongArtists.firstOrNull { !it.channelId.isNullOrBlank() }
            ?: currentSongArtists.firstOrNull { it.id != 0L && it.id != -1L }
            ?: currentSongArtists.firstOrNull()
        val resolvedArtistIdStr = primaryArtist?.channelId?.takeIf { it.isNotBlank() }
            ?: primaryArtist?.id?.takeIf { it != 0L && it != -1L }?.toString()
            ?: song.artists.firstOrNull()?.channelId?.takeIf { it.isNotBlank() }
            ?: song.artists.firstOrNull()?.id?.takeIf { it != 0L && it != -1L }?.toString()
            ?: song.artistId.toString()
        val fallbackArtistName = primaryArtist?.name?.takeIf { it.isNotBlank() }
            ?: song.displayArtist.takeIf { it.isNotBlank() }
            ?: song.artist
        if (currentSongArtists.size > 1) {
            showArtistPicker = true
        } else {
            playerViewModel.triggerArtistNavigationFromPlayer(
                artistIdStr = resolvedArtistIdStr,
                fallbackArtistName = fallbackArtistName
            )
        }
    }

    val onSongMetadataTitleClick = {
        val hasNavigableAlbum = !song.albumBrowseId.isNullOrBlank() ||
            (song.albumId != 0L && song.albumId != -1L) ||
            (song.album.isNotBlank() &&
                !song.album.equals("YouTube Music", ignoreCase = true) &&
                !song.album.equals("Unknown Album", ignoreCase = true) &&
                !song.album.equals(song.title, ignoreCase = true))
        if (hasNavigableAlbum) {
            playerViewModel.triggerAlbumNavigationForSong(song)
        } else {
            onSongMetadataArtistClick()
        }
    }

    var pendingCarouselIndex by remember { mutableStateOf<Int?>(null) }
    val currentQueueIndex = remember(song.id, currentMediaItemIndex, currentPlaybackQueue) {
        resolveQueueIndex(
            queue = currentPlaybackQueue,
            songId = song.id,
            currentMediaItemIndex = currentMediaItemIndex
        )
    }
    val skipRequests = remember {
        MutableSharedFlow<SkipDirection>(
            extraBufferCapacity = 16
        )
    }
    val latestQueue by rememberUpdatedState(currentPlaybackQueue)
    val latestSongId by rememberUpdatedState(song.id)
    val latestCurrentQueueIndex by rememberUpdatedState(currentQueueIndex)
    val latestRepeatMode by rememberUpdatedState(repeatMode)
    val latestIsRemotePlaybackActive by rememberUpdatedState(isRemotePlaybackActive)
    val latestCurrentPositionProvider by rememberUpdatedState(currentPositionProvider)
    val latestOnNext by rememberUpdatedState(onNext)
    val latestOnPrevious by rememberUpdatedState(onPrevious)

    LaunchedEffect(currentQueueIndex, pendingCarouselIndex) {
        if (pendingCarouselIndex == currentQueueIndex) {
            pendingCarouselIndex = null
        }
    }

    LaunchedEffect(pendingCarouselIndex, currentQueueIndex) {
        val targetIndex = pendingCarouselIndex ?: return@LaunchedEffect
        kotlinx.coroutines.delay(900)
        if (pendingCarouselIndex == targetIndex && currentQueueIndex != targetIndex) {
            pendingCarouselIndex = null
        }
    }

    LaunchedEffect(skipRequests) {
        skipRequests.collect { direction ->
            when (direction) {
                SkipDirection.NEXT -> latestOnNext()
                SkipDirection.PREVIOUS -> latestOnPrevious()
            }

            kotlinx.coroutines.delay(SKIP_COMMAND_GUARD_MS)
        }
    }

    fun predictSkipCarouselIndex(direction: SkipDirection): Int? {
        val queueSnapshot = latestQueue
        val baseIndex = pendingCarouselIndex
            ?: latestCurrentQueueIndex
            ?: queueSnapshot.indexOfFirst { it.id == latestSongId }.takeIf { it >= 0 }

        return when (direction) {
            SkipDirection.NEXT -> predictSkipNextCarouselIndex(
                currentIndex = baseIndex,
                queue = queueSnapshot,
                repeatMode = latestRepeatMode,
                isRemotePlaybackActive = latestIsRemotePlaybackActive
            )
            SkipDirection.PREVIOUS -> predictSkipPreviousCarouselIndex(
                currentIndex = baseIndex,
                queue = queueSnapshot,
                currentPositionMs = latestCurrentPositionProvider(),
                repeatMode = latestRepeatMode,
                isRemotePlaybackActive = latestIsRemotePlaybackActive
            )
        }
    }

    fun requestSkip(direction: SkipDirection) {
        val predictedTargetIndex = predictSkipCarouselIndex(direction)
        if (skipRequests.tryEmit(direction) && predictedTargetIndex != null) {
            pendingCarouselIndex = predictedTargetIndex
        }
    }

    val onNextWithOptimisticCarousel = {
        requestSkip(SkipDirection.NEXT)
        Unit
    }

    val onPreviousWithOptimisticCarousel = {
        requestSkip(SkipDirection.PREVIOUS)
        Unit
    }

    val rootCoordsHolder = remember { LayoutCoordsHolder() }
    val defaultArtBounds = aodSourceBounds.artBounds
    val artworkMorphTracker = remember { ArtworkMorphTracker() }
    val density = LocalDensity.current
    val miniArtSizePx = remember(density) { with(density) { 44.dp.toPx() } }
    val miniArtOffsetPx = remember(density) { with(density) { 10.dp.toPx() } }

    if (!isComplex && !isCustomStyle) {
        aodSourceBounds.artStyle = if (isImmersive) AodSourceArtStyle.IMMERSIVE_RECT else AodSourceArtStyle.STANDARD_ROUNDED
        // Same 130dp black top shadow both immersive styles draw over the art.
        aodSourceBounds.topShadowHeightDp = if (isImmersive) 130f else 0f
        aodSourceBounds.topShadowStartAlpha = 0.45f
        aodSourceBounds.topShadowMidAlpha = 0.225f
        aodSourceBounds.carouselStyle =
            if (currentPlaybackQueue.size <= 1) CarouselStyle.NO_PEEK else carouselStyle
    }

    val albumCoverSection: @Composable (Modifier) -> Unit = { modifier ->
        FullPlayerAlbumCoverSection(
            song = song,
            currentPlaybackQueue = currentPlaybackQueue,
            currentMediaItemIndex = currentQueueIndex ?: currentMediaItemIndex,
            carouselStyle = carouselStyle,
            loadingTweaks = loadingTweaks,
            aodScreenEnabled = aodScreenEnabled,
            onEnterAodScreen = { showAodScreen = true },
            isSheetDragGestureActive = isSheetDragGestureActive,
            expansionFractionProvider = expansionFractionProvider,
            currentSheetState = currentSheetState,
            isPlayingProvider = isPlayingProvider,
            playWhenReadyProvider = playWhenReadyProvider,
            placeholderColor = placeholderColor,
            placeholderOnColor = placeholderOnColor,
            albumArtQuality = albumArtQuality,
            requestedScrollIndex = pendingCarouselIndex,
            onSongSelected = onAlbumSongSelected,
            onAlbumClick = { albumSong ->
                playerViewModel.triggerAlbumNavigationForSong(albumSong)
            },
            artMorphModifier = Modifier
                .captureUntransformedBounds(rootCoordsHolder, defaultArtBounds)
                .graphicsLayer {
                    val s = artworkMorphTracker.evaluate(expansionFractionProvider())
                    val destX = defaultArtBounds.offset.x
                    val destY = defaultArtBounds.offset.y
                    val curW = androidx.compose.ui.util.lerp(miniArtSizePx, size.width, s.sizeT)
                    val curH = androidx.compose.ui.util.lerp(miniArtSizePx, size.height, s.sizeT)
                    val sx = if (size.width > 0f) (curW / size.width).coerceAtLeast(0.01f) else 1f
                    val sy = if (size.height > 0f) (curH / size.height).coerceAtLeast(0.01f) else 1f
                    transformOrigin = TransformOrigin(0f, 0f)
                    scaleX = sx
                    scaleY = sy
                    translationX = (miniArtOffsetPx - destX) * (1f - s.posT)
                    translationY = (miniArtOffsetPx - destY) * (1f - s.posT)
                    alpha = if (nowPlayingAodHideProvider() > 0.001f) 0f else 1f

                    if (s.sizeT < 0.995f && size.width > 1f && size.height > 1f) {
                        val miniCornerPx = miniArtSizePx * 0.5f
                        val fullCornerPx = 18.dp.toPx().coerceAtMost(minOf(size.width, size.height) * 0.5f)
                        val targetVisualR = androidx.compose.ui.util.lerp(miniCornerPx, fullCornerPx, s.sizeT)
                        val rx = (targetVisualR / sx).coerceIn(0f, size.width * 0.5f)
                        val ry = (targetVisualR / sy).coerceIn(0f, size.height * 0.5f)
                        clip = true
                        shape = DynamicArtMorphShape(rxPx = rx, ryPx = ry)
                    } else {
                        clip = false
                    }
                },
            modifier = modifier
        )
    }

    val playerProgressSection: @Composable () -> Unit = {
        FullPlayerProgressSection(
            song = song,
            playbackMetadataMediaId = playbackAudioMetadata.mediaId,
            playbackMetadataMimeType = playbackAudioMetadata.mimeType,
            playbackMetadataBitrate = playbackAudioMetadata.bitrate,
            playbackMetadataSampleRate = playbackAudioMetadata.sampleRate,
            currentPositionProvider = currentPositionProvider,
            totalDurationValue = totalDurationValue,
            showPlayerFileInfo = showPlayerFileInfo,
            onSeek = onSeek,
            expansionFractionProvider = expansionFractionProvider,
            isPlayingProvider = isPlayingProvider,
            currentSheetState = currentSheetState,
            progressActiveColor = progressActiveColor,
            progressInactiveColor = progressInactiveColor,
            playerOnBaseColor = playerOnBaseColor,
            allowRealtimeUpdates = allowRealtimeUpdates,
            isSheetDragGestureActive = isSheetDragGestureActive,
            loadingTweaks = loadingTweaks,
            rootCoordsHolder = rootCoordsHolder,
            aodSourceBounds = aodSourceBounds,
            aodTransitionProgressProvider = nowPlayingAodHideProvider
        )
    }

    val controlsSection: @Composable () -> Unit = {
        FullPlayerControlsSection(
            loadingTweaks = loadingTweaks,
            isSheetDragGestureActive = isSheetDragGestureActive,
            expansionFractionProvider = expansionFractionProvider,
            currentSheetState = currentSheetState,
            placeholderColor = placeholderColor,
            placeholderOnColor = placeholderOnColor,
            isPlayingProvider = isPlayingProvider,
            onPrevious = onPreviousWithOptimisticCarousel,
            onPlayPause = onPlayPause,
            onNext = onNextWithOptimisticCarousel,
            transportPlayPauseColors = transportPlayPauseColors,
            transportSkipColors = transportSkipButtonColors,
            isShuffleEnabledProvider = isShuffleEnabledProvider,
            shuffleTransitionInProgress = shuffleTransitionInProgress,
            repeatModeProvider = repeatModeProvider,
            isFavoriteProvider = isFavoriteProvider,
            onShuffleToggle = onShuffleToggle,
            onRepeatToggle = onRepeatToggle,
            onFavoriteToggle = onFavoriteToggle,
            isFullScreenArt = isImmersive,
            morphTracker = artworkMorphTracker,
            rootCoordsHolder = rootCoordsHolder,
            containerWidthPxProvider = { rootCoordsHolder.widthPx },
            aodSourceBounds = aodSourceBounds,
            aodTransitionProgressProvider = nowPlayingAodHideProvider
        )
    }

    val singleLineLyricsSection: @Composable () -> Unit = {
        if (isImmersive &&
            nowPlayingLyricsStyle != com.saurav.pixelmusic.data.preferences.NowPlayingLyricsStyle.HIDDEN && 
            lyricsProvider()?.synced?.isNotEmpty() == true) {
            ImmersiveSingleLineLyrics(
                lyrics = lyricsProvider(),
                playbackPositionFlow = playerViewModel.currentPlaybackPosition,
                syncOffsetMs = lyricsSyncOffset,
                textColor = playerOnBaseColor,
                accentColor = playerAccentColor,
                lyricsStyle = nowPlayingLyricsStyle,
                onClick = onLyricsClick,
                modifier = Modifier.graphicsLayer {
                    val aodFade = (1f - nowPlayingAodHideProvider() * 2.2f).coerceIn(0f, 1f)
                    alpha = artworkMorphTracker.evaluate(expansionFractionProvider()).nonArtAlpha * aodFade
                }
            )
        }
    }

    val portraitSongMetadataSection: @Composable () -> Unit = {
        FullPlayerSongMetadataSection(
            song = song,
            currentSongArtists = currentSongArtists,
            loadingTweaks = loadingTweaks,
            isSheetDragGestureActive = isSheetDragGestureActive,
            expansionFractionProvider = expansionFractionProvider,
            currentSheetState = currentSheetState,
            placeholderColor = placeholderColor,
            placeholderOnColor = placeholderOnColor,
            isLandscape = false,
            onLyricsClick = onLyricsClick,
            onShareClick = { showSongInfoBottomSheet = true },
            playerOnBaseColor = playerOnBaseColor,
            playerViewModel = playerViewModel,
            gradientEdgeColor = gradientEdgeColor,
            chipColor = chipColor,
            chipContentColor = chipContentColor,
            onQueueClick = onSongMetadataQueueClick,
            onTitleClick = onSongMetadataTitleClick,
            onArtistClick = onSongMetadataArtistClick,
            morphTracker = artworkMorphTracker,
            rootCoordsHolder = rootCoordsHolder,
            aodSourceBounds = aodSourceBounds,
            aodTransitionProgressProvider = nowPlayingAodHideProvider
        )
    }

    val landscapeSongMetadataSection: @Composable () -> Unit = {
        FullPlayerSongMetadataSection(
            song = song,
            currentSongArtists = currentSongArtists,
            loadingTweaks = loadingTweaks,
            isSheetDragGestureActive = isSheetDragGestureActive,
            expansionFractionProvider = expansionFractionProvider,
            currentSheetState = currentSheetState,
            placeholderColor = placeholderColor,
            placeholderOnColor = placeholderOnColor,
            isLandscape = true,
            onLyricsClick = onLyricsClick,
            onShareClick = { showSongInfoBottomSheet = true },
            playerOnBaseColor = playerOnBaseColor,
            playerViewModel = playerViewModel,
            gradientEdgeColor = gradientEdgeColor,
            chipColor = chipColor,
            chipContentColor = chipContentColor,
            onQueueClick = onSongMetadataQueueClick,
            onTitleClick = onSongMetadataTitleClick,
            onArtistClick = onSongMetadataArtistClick,
            morphTracker = artworkMorphTracker,
            rootCoordsHolder = rootCoordsHolder,
            aodSourceBounds = aodSourceBounds,
            aodTransitionProgressProvider = nowPlayingAodHideProvider
        )
    }

    val latestAodVisible by rememberUpdatedState(aodVisible)

    Scaffold(
        containerColor = Color.Transparent,
        modifier = Modifier.pointerInput(currentSheetState, queueGestureBottomExclusionPx) {
            val queueDragActivationThresholdPx = maxOf(viewConfiguration.touchSlop, 18.dp.toPx())
            val quickFlickVelocityThreshold = -520f

            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = true)
                // Check condition AFTER the down event occurs
                val isFullyExpanded = currentSheetState == PlayerSheetState.EXPANDED &&
                    expansionFractionProvider() >= 0.99f &&
                    !latestAodVisible

                if (!isFullyExpanded) {
                    return@awaitEachGesture
                }

                val bottomGestureBoundaryY =
                    (size.height.toFloat() - queueGestureBottomExclusionPx).coerceAtLeast(0f)
                if (down.position.y >= bottomGestureBoundaryY) {
                    // Let the system Home/back gesture win near the bottom edge.
                    return@awaitEachGesture
                }

                // Proceed with gesture logic
                var dragConsumedByQueue = false
                val velocityTracker = VelocityTracker()
                var totalDrag = 0f
                var totalHorizontalDrag = 0f
                velocityTracker.addPosition(down.uptimeMillis, down.position)

                drag(down.id) { change ->
                    if (latestAodVisible) {
                        return@drag
                    }
                    if (!dragConsumedByQueue && change.isConsumed) {
                        return@drag
                    }
                    val delta = change.positionChange()
                    val dragAmount = delta.y
                    totalDrag += dragAmount
                    totalHorizontalDrag += delta.x
                    velocityTracker.addPosition(change.uptimeMillis, change.position)
                    val isVerticalDominant = kotlin.math.abs(totalDrag) > kotlin.math.abs(totalHorizontalDrag) * 1.15f
                    val isDraggingUp = isVerticalDominant && totalDrag < -queueDragActivationThresholdPx

                    if (isDraggingUp && !dragConsumedByQueue) {
                        dragConsumedByQueue = true
                        onQueueDragStart()
                    }

                    if (dragConsumedByQueue) {
                        change.consume()
                        onQueueDrag(dragAmount)
                    }
                }

                val velocity = velocityTracker.calculateVelocity().y
                if (dragConsumedByQueue) {
                    onQueueRelease(totalDrag, velocity)
                } else if (
                    !latestAodVisible &&
                    kotlin.math.abs(totalDrag) > kotlin.math.abs(totalHorizontalDrag) * 1.15f &&
                    totalDrag < -(queueDragActivationThresholdPx * 1.5f) &&
                    velocity < quickFlickVelocityThreshold
                ) {
                    // Treat short/fast upward flick as queue-open intent.
                    onQueueRelease(totalDrag, velocity)
                }
            }
        },
        topBar = {
            // Reserve exact TopAppBar height for Scaffold paddingValues; the actual TopAppBar is
            // drawn above AodScreen so it fades out/in smoothly over the morphing artwork instead
            // of being covered by AodScreen and popping in after the return transition finishes.
            AnimatedVisibility(
                visible = !isLandscape,
                enter = fadeIn(animationSpec = tween(350, easing = FastOutSlowInEasing)) +
                        slideInVertically(
                            initialOffsetY = { -it / 2 },
                            animationSpec = tween(350, easing = FastOutSlowInEasing)
                        ),
                exit = fadeOut(animationSpec = tween(220, easing = FastOutSlowInEasing)) +
                       slideOutVertically(
                           targetOffsetY = { -it / 2 },
                           animationSpec = tween(220, easing = FastOutSlowInEasing)
                       )
            ) {
                Spacer(
                    modifier = Modifier
                        .fillMaxWidth()
                        .windowInsetsPadding(TopAppBarDefaults.windowInsets)
                        .height(TopAppBarDefaults.TopAppBarExpandedHeight)
                )
            }
        }
    ) { paddingValues ->
        // MD3: 方向变化时先 alpha=0 再淡入新布局，避免双布局同时测量导致错位
        var contentVisible by remember(isLandscape) { mutableStateOf(false) }
        LaunchedEffect(isLandscape) { contentVisible = true }
        val contentAlpha by animateFloatAsState(
            targetValue = if (contentVisible) 1f else 0f,
            animationSpec = tween(durationMillis = 380, easing = FastOutSlowInEasing),
            label = "orientationAlpha"
        )
        val slideOffsetPx = remember(density) { with(density) { 24.dp.toPx() } }

        val configuration = LocalConfiguration.current
        val fallbackFullWidthPx = with(density) { configuration.screenWidthDp.dp.toPx() }
        val fallbackFullHeightPx = with(density) { configuration.screenHeightDp.dp.toPx() }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .captureRootCoords(rootCoordsHolder)
                .graphicsLayer { alpha = contentAlpha }
        ) {
            val fullWidthPx = fallbackFullWidthPx
            val fullHeightPx = fallbackFullHeightPx

            if (isImmersive && !isImmersiveExtended) {
                // ==========================================
                // 1. STANDARD IMMERSIVE MODE (WITH MORPHING ARTWORK)
                // ==========================================
                val bgColor = LocalMaterialTheme.current.surface
                val isDarkTheme = LocalPixelMusicDarkTheme.current
                val context = LocalContext.current
                val effectiveQuality = albumArtQuality
                val optimizedAlbumArtUri = remember(song.id, effectiveQuality) {
                    com.saurav.pixelmusic.utils.ThumbnailUrlUtils.optimizeArtworkUrl(song.albumArtUriString, effectiveQuality) ?: song.albumArtUriString.orEmpty()
                }

                var extractedColor by remember { mutableStateOf(bgColor) }
                LaunchedEffect(optimizedAlbumArtUri, isDarkTheme) {
                    extractedColor = extractDominantColor(context, optimizedAlbumArtUri, bgColor, isDarkTheme)
                }
                val isBwTheme = com.saurav.pixelmusic.ui.theme.LocalIsBlackAndWhiteTheme.current
                val targetWash = if (isBwTheme) bgColor else androidx.compose.ui.graphics.lerp(bgColor, extractedColor, 0.5f)
                aodSourceBounds.washColor = targetWash
                aodSourceBounds.washStart = 0.55f
                aodSourceBounds.washMid = 0.80f
                aodSourceBounds.washMidAlpha = 0.85f
                aodSourceBounds.topScrimAlpha = 0f
                val washColor by animateColorAsState(
                    targetValue = targetWash,
                    animationSpec = tween(500),
                    label = "immersiveWashColor"
                )
                val targetArtHeightPx = fullHeightPx * 0.42f
                val morphShape = remember(artworkMorphTracker, expansionFractionProvider, density) {
                    object : androidx.compose.ui.graphics.Shape {
                        override fun createOutline(
                            size: androidx.compose.ui.geometry.Size,
                            layoutDirection: androidx.compose.ui.unit.LayoutDirection,
                            density: androidx.compose.ui.unit.Density
                        ): androidx.compose.ui.graphics.Outline {
                            val t = artworkMorphTracker.evaluate(expansionFractionProvider()).sizeT
                            val maxR = minOf(size.width, size.height) * 0.5f
                            val r = (maxR * (1f - t)).coerceIn(0f, maxR)
                            if (r <= 0.5f) {
                                return androidx.compose.ui.graphics.Outline.Rectangle(
                                    Rect(0f, 0f, size.width, size.height)
                                )
                            }
                            return androidx.compose.ui.graphics.Outline.Rounded(
                                androidx.compose.ui.geometry.RoundRect(
                                    rect = Rect(0f, 0f, size.width, size.height),
                                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(r, r)
                                )
                            )
                        }
                    }
                }

                Box(modifier = Modifier.fillMaxSize()) {
                    // Wash background fades in with nonArtAlpha
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                alpha = artworkMorphTracker.evaluate(expansionFractionProvider()).nonArtAlpha
                            }
                            .background(washColor)
                    )

                    // Morphing top 42% (portrait) or left 50% (landscape) artwork
                    Box(
                        modifier = Modifier
                            .layout { measurable, constraints ->
                                val sample = artworkMorphTracker.evaluate(expansionFractionProvider())
                                val wPx = if (isLandscape) {
                                    (constraints.maxWidth.toFloat() * 0.50f).takeIf { it > 0f } ?: (fullWidthPx * 0.50f)
                                } else {
                                    constraints.maxWidth.toFloat().takeIf { it > 0f } ?: fullWidthPx
                                }
                                val hPx = if (isLandscape) {
                                    constraints.maxHeight.toFloat().takeIf { it > 0f } ?: fullHeightPx
                                } else {
                                    (constraints.maxHeight.toFloat() * 0.42f).takeIf { it > 0f } ?: targetArtHeightPx
                                }
                                val baseW = androidx.compose.ui.util.lerp(miniArtSizePx, wPx, sample.sizeT)
                                val baseH = androidx.compose.ui.util.lerp(miniArtSizePx, hPx, sample.sizeT)
                                val targetW = (baseW * sample.overshootScale).roundToInt().coerceAtLeast(1)
                                val targetH = (baseH * sample.overshootScale).roundToInt().coerceAtLeast(1)
                                val baseX = androidx.compose.ui.util.lerp(miniArtOffsetPx, 0f, sample.posT)
                                val baseY = androidx.compose.ui.util.lerp(miniArtOffsetPx, 0f, sample.posT)
                                val placeX = (baseX - (targetW - baseW) * 0.5f).roundToInt()
                                val placeY = (baseY - (targetH - baseH) * 0.5f).roundToInt()
                                val placeable = measurable.measure(Constraints.fixed(targetW, targetH))
                                layout(targetW, targetH) {
                                    defaultArtBounds.offset = Offset(placeX.toFloat(), placeY.toFloat())
                                    defaultArtBounds.size = IntSize(targetW, targetH)
                                    defaultArtBounds.hasBounds = true
                                    placeable.place(placeX, placeY)
                                }
                            }
                            .graphicsLayer {
                                alpha = if (nowPlayingAodHideProvider() > 0.001f) 0f else 1f
                            }
                            .clip(morphShape)
                            .combinedClickable(
                                onClick = {},
                                onLongClick = { if (aodScreenEnabled) showAodScreen = true }
                            )
                    ) {
                        SmartImage(
                            model = optimizedAlbumArtUri,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            targetSize = coil.size.Size.ORIGINAL,
                            modifier = Modifier.fillMaxSize()
                        )
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .graphicsLayer {
                                    alpha = artworkMorphTracker.evaluate(expansionFractionProvider()).nonArtAlpha
                                }
                                .background(
                                    if (isLandscape) {
                                        Brush.horizontalGradient(
                                            colorStops = arrayOf(
                                                0.0f to Color.Transparent,
                                                0.62f to Color.Transparent,
                                                0.86f to washColor.copy(alpha = 0.85f),
                                                0.96f to washColor,
                                                1.0f to washColor
                                            )
                                        )
                                    } else {
                                        Brush.verticalGradient(
                                            colorStops = arrayOf(
                                                0.0f to Color.Transparent,
                                                0.52f to Color.Transparent,
                                                0.78f to washColor.copy(alpha = 0.85f),
                                                0.92f to washColor,
                                                1.0f to washColor
                                            )
                                        )
                                    }
                                )
                        )
                    }

                    // Subtle top shadow for status bar readability (hidden while the AOD stand-in art carries it)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp)
                            .align(Alignment.TopCenter)
                            .graphicsLayer {
                                alpha = artworkMorphTracker.evaluate(expansionFractionProvider()).nonArtAlpha *
                                    (if (nowPlayingAodHideProvider() > 0.001f) 0f else 1f)
                            }
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(Color.Black.copy(alpha = 0.45f), Color.Transparent)
                                )
                            )
                    )
                }
            } else if (isImmersiveExtended) {
                // ==========================================
                // 2. IMMERSIVE EXTENDED MODE (WITH MORPHING ARTWORK)
                // ==========================================
                val bgColor = LocalMaterialTheme.current.surface
                val isDarkTheme = LocalPixelMusicDarkTheme.current
                val effectiveQuality = albumArtQuality
                val optimizedAlbumArtUri = remember(song.id, effectiveQuality) {
                    com.saurav.pixelmusic.utils.ThumbnailUrlUtils.optimizeArtworkUrl(song.albumArtUriString, effectiveQuality) ?: song.albumArtUriString.orEmpty()
                }
                val targetArtHeightPx = fullHeightPx * 0.55f
                // Share the exact art gradient with the AOD stand-in art so it doesn't pop in after the morph starts.
                aodSourceBounds.washColor = bgColor
                aodSourceBounds.washStart = 0.50f
                aodSourceBounds.washMid = 0.85f
                aodSourceBounds.washMidAlpha = 0.9f
                aodSourceBounds.topScrimAlpha = if (isDarkTheme) 0.3f else 0f
                val morphShape = remember(artworkMorphTracker, expansionFractionProvider, density) {
                    object : androidx.compose.ui.graphics.Shape {
                        override fun createOutline(
                            size: androidx.compose.ui.geometry.Size,
                            layoutDirection: androidx.compose.ui.unit.LayoutDirection,
                            density: androidx.compose.ui.unit.Density
                        ): androidx.compose.ui.graphics.Outline {
                            val t = artworkMorphTracker.evaluate(expansionFractionProvider()).sizeT
                            val maxR = minOf(size.width, size.height) * 0.5f
                            val r = (maxR * (1f - t)).coerceIn(0f, maxR)
                            if (r <= 0.5f) {
                                return androidx.compose.ui.graphics.Outline.Rectangle(
                                    Rect(0f, 0f, size.width, size.height)
                                )
                            }
                            return androidx.compose.ui.graphics.Outline.Rounded(
                                androidx.compose.ui.geometry.RoundRect(
                                    rect = Rect(0f, 0f, size.width, size.height),
                                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(r, r)
                                )
                            )
                        }
                    }
                }

                Box(modifier = Modifier.fillMaxSize()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                alpha = artworkMorphTracker.evaluate(expansionFractionProvider()).nonArtAlpha
                            }
                            .background(bgColor)
                    )

                    // LAYER 1 & 2: Morphing top 55% (portrait) or left 50% (landscape) image + gradient mask
                    Box(
                        modifier = Modifier
                            .layout { measurable, constraints ->
                                val sample = artworkMorphTracker.evaluate(expansionFractionProvider())
                                val wPx = if (isLandscape) {
                                    (constraints.maxWidth.toFloat() * 0.50f).takeIf { it > 0f } ?: (fullWidthPx * 0.50f)
                                } else {
                                    constraints.maxWidth.toFloat().takeIf { it > 0f } ?: fullWidthPx
                                }
                                val hPx = if (isLandscape) {
                                    constraints.maxHeight.toFloat().takeIf { it > 0f } ?: fullHeightPx
                                } else {
                                    (constraints.maxHeight.toFloat() * 0.55f).takeIf { it > 0f } ?: targetArtHeightPx
                                }
                                val baseW = androidx.compose.ui.util.lerp(miniArtSizePx, wPx, sample.sizeT)
                                val baseH = androidx.compose.ui.util.lerp(miniArtSizePx, hPx, sample.sizeT)
                                val targetW = (baseW * sample.overshootScale).roundToInt().coerceAtLeast(1)
                                val targetH = (baseH * sample.overshootScale).roundToInt().coerceAtLeast(1)
                                val baseX = androidx.compose.ui.util.lerp(miniArtOffsetPx, 0f, sample.posT)
                                val baseY = androidx.compose.ui.util.lerp(miniArtOffsetPx, 0f, sample.posT)
                                val placeX = (baseX - (targetW - baseW) * 0.5f).roundToInt()
                                val placeY = (baseY - (targetH - baseH) * 0.5f).roundToInt()
                                val placeable = measurable.measure(Constraints.fixed(targetW, targetH))
                                layout(targetW, targetH) {
                                    defaultArtBounds.offset = Offset(placeX.toFloat(), placeY.toFloat())
                                    defaultArtBounds.size = IntSize(targetW, targetH)
                                    defaultArtBounds.hasBounds = true
                                    placeable.place(placeX, placeY)
                                }
                            }
                            .graphicsLayer {
                                alpha = if (nowPlayingAodHideProvider() > 0.001f) 0f else 1f
                            }
                            .clip(morphShape)
                            .combinedClickable(
                                onClick = {},
                                onLongClick = { if (aodScreenEnabled) showAodScreen = true }
                            )
                    ) {
                        SmartImage(
                            model = optimizedAlbumArtUri,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            targetSize = coil.size.Size.ORIGINAL,
                            modifier = Modifier.fillMaxSize()
                        )
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .graphicsLayer {
                                    // Starts fading in as soon as the art starts growing instead of waiting
                                    // for the sheet to be ~22% open, so the cover doesn't arrive late.
                                    alpha = ((artworkMorphTracker.evaluate(expansionFractionProvider()).rawFraction - 0.04f) / 0.5f)
                                        .coerceIn(0f, 1f)
                                }
                                .background(
                                    if (isLandscape) {
                                        Brush.horizontalGradient(
                                            colorStops = arrayOf(
                                                0.0f to Color.Transparent,
                                                0.62f to Color.Transparent,
                                                0.86f to bgColor.copy(alpha = 0.90f),
                                                0.96f to bgColor,
                                                1.0f to bgColor
                                            )
                                        )
                                    } else {
                                        Brush.verticalGradient(
                                            colorStops = arrayOf(
                                                0.0f to if (isDarkTheme) Color.Black.copy(alpha = 0.3f) else Color.Transparent,
                                                0.48f to Color.Transparent,
                                                0.82f to bgColor.copy(alpha = 0.90f),
                                                0.93f to bgColor,
                                                1.0f to bgColor
                                            )
                                        )
                                    }
                                )
                        )
                    }

                    // LAYER 3: Subtle top shadow for status bar and top buttons readability (hidden while the AOD stand-in art carries it)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp)
                            .align(Alignment.TopCenter)
                            .graphicsLayer {
                                alpha = artworkMorphTracker.evaluate(expansionFractionProvider()).nonArtAlpha *
                                    (if (nowPlayingAodHideProvider() > 0.001f) 0f else 1f)
                            }
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Black.copy(alpha = 0.45f),
                                        Color.Transparent
                                    )
                                )
                            )
                    )
                }
            }

            if (isComplex) {
                val isComplexMetadataForCurrent = playbackAudioMetadata.mediaId == null || playbackAudioMetadata.mediaId == song.id
                val complexAudioMetaLabel = remember(
                    showPlayerFileInfo,
                    song.id,
                    song.mimeType,
                    song.bitrate,
                    song.sampleRate,
                    isComplexMetadataForCurrent,
                    playbackAudioMetadata.mimeType,
                    playbackAudioMetadata.bitrate,
                    playbackAudioMetadata.sampleRate
                ) {
                    if (showPlayerFileInfo) {
                        formatAudioMetaLabel(
                            mimeType = if (isComplexMetadataForCurrent) playbackAudioMetadata.mimeType ?: song.mimeType else song.mimeType,
                            bitrate = if (isComplexMetadataForCurrent) playbackAudioMetadata.bitrate ?: song.bitrate else song.bitrate,
                            sampleRate = if (isComplexMetadataForCurrent) playbackAudioMetadata.sampleRate ?: song.sampleRate else song.sampleRate
                        )
                    } else {
                        null
                    }
                }
                ComplexPlayerLayout(
                    song = song,
                    currentSongArtists = currentSongArtists,
                    albumArtQuality = albumArtQuality,
                    aodScreenEnabled = aodScreenEnabled,
                    onEnterAodScreen = { showAodScreen = true },
                    expansionFractionProvider = expansionFractionProvider,
                    currentSheetState = currentSheetState,
                    allowRealtimeUpdates = allowRealtimeUpdates,
                    currentPositionProvider = currentPositionProvider,
                    totalDurationValue = totalDurationValue,
                    isPlayingProvider = isPlayingProvider,
                    isFavoriteProvider = isFavoriteProvider,
                    lyricsProvider = lyricsProvider,
                    playbackPositionFlow = playerViewModel.currentPlaybackPosition,
                    lyricsSyncOffset = lyricsSyncOffset,
                    onPlayPause = onPlayPause,
                    onSeek = onSeek,
                    onNext = onNextWithOptimisticCarousel,
                    onPrevious = onPreviousWithOptimisticCarousel,
                    onFavoriteToggle = onFavoriteToggle,
                    onCollapse = onCollapse,
                    onLyricsClick = onLyricsClick,
                    onTitleClick = onSongMetadataTitleClick,
                    onArtistClick = onSongMetadataArtistClick,
                    onMoreOptionsClick = { showSongInfoBottomSheet = true },
                    audioMetaLabel = complexAudioMetaLabel,
                    aodSourceBounds = aodSourceBounds,
                    aodTransitionProgressProvider = nowPlayingAodHideProvider
                )
            } else if (isCustomStyle) {
                val isCustomMetadataForCurrent = playbackAudioMetadata.mediaId == null || playbackAudioMetadata.mediaId == song.id
                val customAudioMetaLabel = remember(
                    showPlayerFileInfo,
                    song.id,
                    song.mimeType,
                    song.bitrate,
                    song.sampleRate,
                    isCustomMetadataForCurrent,
                    playbackAudioMetadata.mimeType,
                    playbackAudioMetadata.bitrate,
                    playbackAudioMetadata.sampleRate
                ) {
                    if (showPlayerFileInfo) {
                        formatAudioMetaLabel(
                            mimeType = if (isCustomMetadataForCurrent) playbackAudioMetadata.mimeType ?: song.mimeType else song.mimeType,
                            bitrate = if (isCustomMetadataForCurrent) playbackAudioMetadata.bitrate ?: song.bitrate else song.bitrate,
                            sampleRate = if (isCustomMetadataForCurrent) playbackAudioMetadata.sampleRate ?: song.sampleRate else song.sampleRate
                        )
                    } else {
                        null
                    }
                }
                val styleParams = PlayerStyleParams(
                    song = song,
                    currentSongArtists = currentSongArtists,
                    albumArtQuality = albumArtQuality,
                    aodScreenEnabled = aodScreenEnabled,
                    onEnterAodScreen = { showAodScreen = true },
                    expansionFractionProvider = expansionFractionProvider,
                    currentSheetState = currentSheetState,
                    allowRealtimeUpdates = allowRealtimeUpdates,
                    currentPositionProvider = currentPositionProvider,
                    totalDurationValue = totalDurationValue,
                    isPlayingProvider = isPlayingProvider,
                    isFavoriteProvider = isFavoriteProvider,
                    isShuffleEnabledProvider = isShuffleEnabledProvider,
                    repeatModeProvider = repeatModeProvider,
                    onPlayPause = onPlayPause,
                    onSeek = onSeek,
                    onNext = onNextWithOptimisticCarousel,
                    onPrevious = onPreviousWithOptimisticCarousel,
                    onFavoriteToggle = onFavoriteToggle,
                    onShuffleToggle = onShuffleToggle,
                    onRepeatToggle = onRepeatToggle,
                    onLyricsClick = onLyricsClick,
                    onTitleClick = onSongMetadataTitleClick,
                    onArtistClick = onSongMetadataArtistClick,
                    onMoreOptionsClick = { showSongInfoBottomSheet = true },
                    audioMetaLabel = customAudioMetaLabel,
                    aodSourceBounds = aodSourceBounds,
                    aodTransitionProgressProvider = nowPlayingAodHideProvider
                )
                when (playerDesignStyle) {
                    com.saurav.pixelmusic.data.preferences.PlayerDesignStyle.WAVE -> WavePlayerLayout(params = styleParams)
                    com.saurav.pixelmusic.data.preferences.PlayerDesignStyle.PILLARS -> PillarsPlayerLayout(params = styleParams)
                    com.saurav.pixelmusic.data.preferences.PlayerDesignStyle.EXPRESSIVE -> ExpressivePlayerLayout(params = styleParams)
                    else -> WavePlayerLayout(params = styleParams)
                }
            } else {
                if (isLandscape) {
                    FullPlayerLandscapeContent(
                        paddingValues = paddingValues,
                        albumCoverSection = albumCoverSection,
                        songMetadataSection = landscapeSongMetadataSection,
                        playerProgressSection = playerProgressSection,
                        controlsSection = controlsSection,
                        singleLineLyricsSection = singleLineLyricsSection,
                        isFullScreenArt = isImmersive
                    )
                } else {
                    FullPlayerPortraitContent(
                        paddingValues = paddingValues,
                        albumCoverSection = albumCoverSection,
                        songMetadataSection = portraitSongMetadataSection,
                        playerProgressSection = playerProgressSection,
                        controlsSection = controlsSection,
                        singleLineLyricsSection = singleLineLyricsSection,
                        isFullScreenArt = isImmersive
                    )
                }
            }

        }
    }
    if (aodVisible) {
        AodScreen(
            // Hide the AOD overlay in the exact draw frame the real Now Playing art/title/scrim flip
            // visible (same 0.001 threshold), so the two are never drawn on top of each other.
            modifier = Modifier.graphicsLayer {
                alpha = if (aodMorphAnim.value > 0.001f) 1f else 0f
            },
            songTitle = song.title,
            artistName = song.displayArtist.takeIf { it.isNotBlank() } ?: song.artist,
            albumArtUriString = song.albumArtUriString,
            isPlayingProvider = isPlayingProvider,
            currentPositionProvider = currentPositionProvider,
            totalDurationProvider = totalDurationProvider,
            onPlayPauseClick = { playerViewModel.playPause() },
            onSkipNextClick = onNextWithOptimisticCarousel,
            onSkipPreviousClick = onPreviousWithOptimisticCarousel,
            onDismiss = { showAodScreen = false },
            onSeekTo = { playerViewModel.seekTo(it) },
            enterProgressProvider = aodTransitionProgressProvider,
            sourceBounds = aodSourceBounds,
            aodStyle = aodScreenStyle,
            lyrics = playerState.lyrics,
            lyricsSyncOffsetMs = lyricsSyncOffset
        )
    }
    // MD3: TopAppBar rendered above AodScreen so its fade-out/in stays visible over the morphing
    // artwork during AOD enter/exit instead of popping in after AodScreen is removed.
    AnimatedVisibility(
        visible = !isLandscape,
        enter = fadeIn(animationSpec = tween(350, easing = FastOutSlowInEasing)) +
                slideInVertically(
                    initialOffsetY = { -it / 2 },
                    animationSpec = tween(350, easing = FastOutSlowInEasing)
                ),
        exit = fadeOut(animationSpec = tween(220, easing = FastOutSlowInEasing)) +
               slideOutVertically(
                   targetOffsetY = { -it / 2 },
                   animationSpec = tween(220, easing = FastOutSlowInEasing)
               )
    ) {
        TopAppBar(
            modifier = Modifier.graphicsLayer {
                val aodFade = (1f - aodTransitionProgressProvider() * 2.2f).coerceIn(0f, 1f)
                alpha = artworkMorphTracker.evaluate(expansionFractionProvider()).nonArtAlpha * aodFade
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent,
                titleContentColor = LocalMaterialTheme.current.onPrimaryContainer,
            ),
            title = {
                if (!isCastConnecting && !isComplex && !isCustomStyle) {
                    AnimatedVisibility(visible = (!isRemotePlaybackActive)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                modifier = Modifier.padding(start = 18.dp),
                                text = stringResource(R.string.setcat_now_playing),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.labelLargeEmphasized,
                                fontWeight = FontWeight.SemiBold
                            )

                            if (currentSong != null && (currentSong.telegramChatId != null || currentSong.contentUriString.startsWith("telegram:"))) {
                                Icon(
                                    painter = painterResource(R.drawable.rounded_cloud_24),
                                    contentDescription = stringResource(R.string.presentation_batch_g_player_cd_cloud_stream),
                                    tint = LocalMaterialTheme.current.onPrimaryContainer.copy(alpha = 0.6f),
                                    modifier = Modifier.padding(start = 8.dp).size(16.dp)
                                )
                            }
                        }
                    }
                }
            },
            navigationIcon = {
                val collapseIconOffsetY = remember { Animatable(0f) }
                val collapseIconScale = remember { Animatable(1f) }
                val animateCollapseIcon: () -> Unit = {
                    coroutineScope.launch {
                        collapseIconOffsetY.snapTo(8f)
                        collapseIconScale.snapTo(1.18f)
                        launch {
                            collapseIconOffsetY.animateTo(
                                0f,
                                spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
                            )
                        }
                        launch {
                            collapseIconScale.animateTo(
                                1f,
                                spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .width(56.dp)
                        .height(48.dp),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(topBarButtonBg)
                            .clickable(enabled = !aodVisible) {
                                animateCollapseIcon()
                                onCollapse()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.rounded_keyboard_arrow_down_24),
                            contentDescription = stringResource(R.string.presentation_batch_g_player_cd_collapse),
                            tint = topBarIconColor,
                            modifier = Modifier.graphicsLayer {
                                translationY = collapseIconOffsetY.value
                                scaleX = collapseIconScale.value
                                scaleY = collapseIconScale.value
                            }
                        )
                    }
                }
            },
            actions = {
                val showCastLabel = isCastConnecting || (isRemotePlaybackActive && selectedRouteName != null)
                val isBluetoothActive =
                    isBluetoothEnabled && !bluetoothName.isNullOrEmpty() && !isRemotePlaybackActive && !isCastConnecting
                val castIconPainter = when {
                    isCastConnecting || isRemotePlaybackActive -> painterResource(R.drawable.rounded_cast_24)
                    isBluetoothActive -> painterResource(R.drawable.rounded_bluetooth_24)
                    else -> painterResource(R.drawable.rounded_mobile_speaker_24)
                }

                val topCastShape = remember {
                    RoundedCornerShape(
                        topStart = 50.dp, topEnd = 6.dp,
                        bottomStart = 50.dp, bottomEnd = 6.dp
                    )
                }
                val topLtShape = remember {
                    RoundedCornerShape(
                        topStart = 6.dp, topEnd = 6.dp,
                        bottomStart = 6.dp, bottomEnd = 6.dp
                    )
                }
                val topQueueShape = remember {
                    RoundedCornerShape(
                        topStart = 6.dp, topEnd = 50.dp,
                        bottomStart = 6.dp, bottomEnd = 50.dp
                    )
                }

                val pjUiState by playerViewModel.pixelJamUiState.collectAsStateWithLifecycle()
                val ltActive = pjUiState is com.saurav.pixelmusic.data.session.PixelJamUiState.InRoom

                Row(
                    modifier = Modifier.padding(end = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 1. Output / Cast Button (Reversed D shape ᗡ)
                    Box(
                        modifier = Modifier
                            .height(48.dp)
                            .then(if (showCastLabel) Modifier.widthIn(min = 44.dp).padding(horizontal = 14.dp) else Modifier.width(44.dp))
                            .animateContentSize()
                            .clip(topCastShape)
                            .background(topBarButtonBg, topCastShape)
                            .clickable(enabled = !aodVisible) {
                                onShowCastClicked()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                painter = castIconPainter,
                                contentDescription = when {
                                    isCastConnecting || isRemotePlaybackActive -> stringResource(R.string.presentation_batch_g_player_cd_cast)
                                    isBluetoothActive -> stringResource(R.string.presentation_batch_g_player_cd_bluetooth)
                                    else -> stringResource(R.string.presentation_batch_g_player_cd_local_playback)
                                },
                                tint = topBarIconColor,
                                modifier = Modifier.size(24.dp)
                            )
                            AnimatedVisibility(visible = showCastLabel) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        text = when {
                                            isCastConnecting -> stringResource(R.string.presentation_batch_g_player_connecting)
                                            isRemotePlaybackActive && selectedRouteName != null -> selectedRouteName
                                            else -> ""
                                        },
                                        style = MaterialTheme.typography.labelMedium,
                                        color = topBarIconColor,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }

                    // 2. PixelJam Button (Expressive rounded square matching Cast & Queue)
                    Box(
                        modifier = Modifier
                            .size(width = 44.dp, height = 48.dp)
                            .clip(topLtShape)
                            .background(
                                if (ltActive) MaterialTheme.colorScheme.primary else topBarButtonBg,
                                topLtShape
                            )
                            .clickable(enabled = !aodVisible) {
                                playerViewModel.openPixelJamSheet()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.rounded_group_24),
                            contentDescription = stringResource(R.string.pixeljam),
                            tint = if (ltActive) MaterialTheme.colorScheme.onPrimary else topBarIconColor,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    // 3. Queue Button (D shape D)
                    Box(
                        modifier = Modifier
                            .size(width = 44.dp, height = 48.dp)
                            .clip(topQueueShape)
                            .background(topBarButtonBg, topQueueShape)
                            .clickable(enabled = !aodVisible) {
                                onShowQueueClicked()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.rounded_queue_music_24),
                            contentDescription = stringResource(R.string.presentation_batch_g_player_cd_queue),
                            tint = topBarIconColor,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        )
    }
    AnimatedVisibility(
        visible = showLyricsSheet,
        enter = slideInVertically(
            initialOffsetY = { it / 5 },
            animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing)
        ) + fadeIn(animationSpec = tween(durationMillis = 160)),
        exit = slideOutVertically(
            targetOffsetY = { it / 6 },
            animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing)
        ) + fadeOut(animationSpec = tween(durationMillis = 120))
    ) {
        LyricsSheet(
            stablePlayerStateFlow = playerViewModel.stablePlayerState,
            playbackPositionFlow = playerViewModel.currentPlaybackPosition,
            lyricsSearchUiState = lyricsSearchUiState,
            resetLyricsForCurrentSong = {
                showLyricsSheet = false
                playerViewModel.resetLyricsForCurrentSong()
            },
            onSearchLyrics = { forcePick -> playerViewModel.fetchLyricsForCurrentSong(forcePick) },
            onPickResult = { playerViewModel.acceptLyricsSearchResultForCurrentSong(it) },
            onManualSearch = { title, artist, autoApply ->
                playerViewModel.searchLyricsManually(title, artist, autoApply)
            },
            onImportLyrics = { filePickerLauncher.launch(com.saurav.pixelmusic.utils.LyricsImportSecurity.pickerMimeTypes()) },
            onDismissLyricsSearch = { playerViewModel.resetLyricsSearchState() },
            lyricsSyncOffset = lyricsSyncOffset,
            onLyricsSyncOffsetChange = { currentSong?.id?.let { songId -> playerViewModel.setLyricsSyncOffset(songId, it) } },
            lyricsTextStyle = MaterialTheme.typography.titleLarge,
            colorScheme = LocalMaterialTheme.current,
            onBackClick = { showLyricsSheet = false },
            onSaveLyricsToFile = playerViewModel::saveLyricsToFile,
            onSeekTo = { playerViewModel.seekTo(it) },
            onPlayPause = {
                playerViewModel.playPause()
            },
            onNext = onNext,
            onPrev = onPrevious,
            immersiveLyricsEnabled = immersiveLyricsEnabled,
            immersiveLyricsTimeout = immersiveLyricsTimeout,
            isImmersiveTemporarilyDisabled = isImmersiveTemporarilyDisabled,
            onSetImmersiveTemporarilyDisabled = { playerViewModel.setImmersiveTemporarilyDisabled(it) },
            isShuffleEnabled = isShuffleEnabled,
            repeatMode = repeatMode,
            isFavoriteProvider = isFavoriteProvider,
            onShuffleToggle = onShuffleToggle,
            onRepeatToggle = onRepeatToggle,
            onFavoriteToggle = onFavoriteToggle
        )
    }

    val artistPickerSheetState = rememberBottomSheetState(
        initialValue = SheetValue.Hidden,
        enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded)
    )
    if (showArtistPicker && currentSongArtists.isNotEmpty()) {
        PlayerArtistPickerBottomSheet(
            song = song,
            artists = currentSongArtists,
            sheetState = artistPickerSheetState,
            onDismiss = { showArtistPicker = false },
            onArtistClick = { artist ->
                val artistIdStr = artist.channelId?.takeIf { it.isNotBlank() } ?: artist.id.toString()
                playerViewModel.triggerArtistNavigationFromPlayer(
                    artistIdStr = artistIdStr,
                    fallbackArtistName = artist.name
                )
                showArtistPicker = false
            }
        )
    }

    // Share Bottom Sheet
    if (showShareSheet) {
        ShareBottomSheet(
            song = song,
            onDismiss = { showShareSheet = false },
            onAddToPlaylist = {
                showShareSheet = false
                showPlaylistBottomSheet = true
            },
            colorScheme = LocalMaterialTheme.current,
            lyricsLines = lyricsLines,
            currentPlaybackPositionMs = currentPositionProvider()
        )
    }

    if (showPlaylistBottomSheet) {
        val playlistViewModel: PlaylistViewModel = hiltViewModel()
        val playlistUiState by playlistViewModel.uiState.collectAsStateWithLifecycle()
        PlaylistBottomSheet(
            playlistUiState = playlistUiState,
            songs = listOf(song),
            onDismiss = { showPlaylistBottomSheet = false },
            bottomBarHeight = 0.dp,
            playerViewModel = playerViewModel
        )
    }

    if (showSleepTimerSheet) {
        val activeTimerDurationMinutes by playerViewModel.activeTimerDurationMinutes.collectAsStateWithLifecycle()
        val isEndOfTrackTimerActive by playerViewModel.isEndOfTrackTimerActive.collectAsStateWithLifecycle()
        val playCount by playerViewModel.playCount.collectAsStateWithLifecycle()

        TimerOptionsBottomSheet(
            onDismiss = { showSleepTimerSheet = false },
            activeTimerValueDisplay = activeTimerValueDisplay,
            activeTimerDurationMinutes = activeTimerDurationMinutes,
            playCount = playCount,
            isEndOfTrackTimerActive = isEndOfTrackTimerActive,
            onSetPredefinedTimer = { minutes -> playerViewModel.setSleepTimer(minutes) },
            onSetEndOfTrackTimer = { enable -> playerViewModel.setEndOfTrackTimer(enable) },
            onOpenCustomTimePicker = {},
            onCancelTimer = { playerViewModel.cancelSleepTimer() },
            onCancelCountedPlay = { playerViewModel.cancelCountedPlay() },
            onPlayCounter = { count -> playerViewModel.playCounted(count) }
        )
    }

    // Player Options Menu Bottom Sheet
    if (showSongInfoBottomSheet) {
        val songInfoViewModel: SongInfoBottomSheetViewModel = hiltViewModel()
        val songOptionsSheetState = rememberBottomSheetState(
            initialValue = SheetValue.Hidden,
            enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded)
        )

        val sheetShape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
        SmoothModalBottomSheet(
            onDismissRequest = { showSongInfoBottomSheet = false },
            sheetState = songOptionsSheetState,
            dragHandle = null,
            containerColor = Color.Transparent,
            shape = sheetShape,
            contentWindowInsets = { WindowInsets(0, 0, 0, 0) }
        ) {
            val smoothDismiss = com.saurav.pixelmusic.presentation.components.LocalSmoothDismiss.current

            MaterialTheme(
                colorScheme = LocalMaterialTheme.current,
                typography = MaterialTheme.typography,
                shapes = MaterialTheme.shapes
            ) {
                CompositionLocalProvider(
                    LocalOverscrollFactory provides null,
                    LocalMinimumInteractiveComponentSize provides 0.dp
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(sheetShape)
                            .background(LocalMaterialTheme.current.surfaceContainerHigh)
                            .navigationBarsPadding()
                            .padding(bottom = 16.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScroll(rememberScrollState())
                                .padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Custom Drag Handle inside the Column
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 12.dp, bottom = 4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .width(36.dp)
                                        .height(4.dp)
                                        .clip(CircleShape)
                                        .background(LocalMaterialTheme.current.onSurfaceVariant.copy(alpha = 0.35f))
                                )
                            }
                            
                            val navigateToOrigin = {
                                smoothDismiss {
                                    onSongMetadataTitleClick()
                                }
                            }

                            // Header Card: Cover + Metadata (Tapping redirects to Origin / Album / Artist)
                            val headerCardShape = remember { RoundedCornerShape(22.dp) }
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = headerCardShape,
                                color = LocalMaterialTheme.current.surfaceContainerHigh,
                                tonalElevation = 2.dp,
                                shadowElevation = 0.dp,
                                onClick = { navigateToOrigin() }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 12.dp),
                                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    SmartImage(
                                        model = song.albumArtUriString,
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .size(56.dp)
                                            .clip(RoundedCornerShape(14.dp))
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = song.title,
                                            fontFamily = GoogleSansRounded,
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.titleMedium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            color = LocalMaterialTheme.current.onSurface
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = song.displayArtist,
                                            fontFamily = GoogleSansRounded,
                                            fontWeight = FontWeight.Medium,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = LocalMaterialTheme.current.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.clickable {
                                                smoothDismiss {
                                                    onSongMetadataArtistClick()
                                                }
                                            }
                                        )
                                        val sourceOrAlbum = if (song.album.isNotBlank() && song.album != song.title) {
                                            song.album
                                        } else if (!song.youtubeId.isNullOrEmpty() || song.contentUriString.startsWith("youtube://") || song.id.startsWith("youtube_")) {
                                            "YouTube Music"
                                        } else {
                                            null
                                        }
                                        if (sourceOrAlbum != null) {
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = sourceOrAlbum,
                                                fontFamily = GoogleSansRounded,
                                                fontWeight = FontWeight.Normal,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = LocalMaterialTheme.current.primary,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(LocalMaterialTheme.current.surfaceVariant),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                                            contentDescription = null,
                                            tint = LocalMaterialTheme.current.onSurfaceVariant,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }

                            // FLAGSHIP HERO OPTION: Start Mix from this
                            val heroShape = remember { RoundedCornerShape(22.dp) }
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = heroShape,
                                color = LocalMaterialTheme.current.primaryContainer.copy(alpha = 0.90f),
                                tonalElevation = 2.dp,
                                shadowElevation = 0.dp,
                                onClick = {
                                    playerViewModel.startMixFromSong(song)
                                    smoothDismiss(null)
                                }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(44.dp)
                                            .clip(CircleShape)
                                            .background(LocalMaterialTheme.current.primary),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            painter = painterResource(R.drawable.rounded_shuffle_24),
                                            contentDescription = null,
                                            tint = LocalMaterialTheme.current.onPrimary,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Start Mix from this",
                                            fontFamily = GoogleSansRounded,
                                            fontWeight = FontWeight.Bold,
                                            color = LocalMaterialTheme.current.onPrimaryContainer,
                                            style = MaterialTheme.typography.titleMedium
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "Continuous smart radio based on this track",
                                            fontFamily = GoogleSansRounded,
                                            fontWeight = FontWeight.Normal,
                                            color = LocalMaterialTheme.current.onPrimaryContainer.copy(alpha = 0.75f),
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(CircleShape)
                                            .background(LocalMaterialTheme.current.onPrimaryContainer.copy(alpha = 0.12f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                                            contentDescription = null,
                                            tint = LocalMaterialTheme.current.onPrimaryContainer,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }

                            // Material You Expressive Separated Action Cards
                            val isLocalModePlayer by playerViewModel.localModeEnabled.collectAsStateWithLifecycle()
                            var isDownloaded by remember(song.id) { mutableStateOf(false) }
                            val activeDownloadProgress by com.saurav.pixelmusic.utils.SongDownloader.activeDownloadProgress.collectAsStateWithLifecycle()
                            val downloadNotificationId = remember(song.id, song.youtubeId) { (song.youtubeId ?: song.id).hashCode() }
                            val downloadProgress = activeDownloadProgress[downloadNotificationId]
                            val isDownloading = downloadProgress != null
                            val animatedDownloadProgress by animateFloatAsState(
                                targetValue = (downloadProgress ?: 0f).coerceAtLeast(0f),
                                animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium),
                                label = "SongDownloadProgress"
                            )
                            LaunchedEffect(song.id) {
                                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                                    val musicDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_MUSIC)
                                    val pixelMusicDir = java.io.File(musicDir, "PixelMusic")
                                    val cleanTitle = song.title.replace(Regex("[\\\\/:*?\"<>|]"), "_")
                                    val cleanArtist = song.displayArtist.replace(Regex("[\\\\/:*?\"<>|]"), "_")
                                    val targetFile = java.io.File(pixelMusicDir, "$cleanTitle - $cleanArtist.m4a")
                                    isDownloaded = targetFile.exists()
                                }
                            }

                            val topCardShape = remember { RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp, bottomStart = 4.dp, bottomEnd = 4.dp) }
                            val middleCardShape = remember { RoundedCornerShape(4.dp) }
                            val bottomCardShape = remember { RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp, bottomStart = 24.dp, bottomEnd = 24.dp) }
                            val cardColor = LocalMaterialTheme.current.surfaceContainerLowest
                            val defaultIconBg = LocalMaterialTheme.current.secondaryContainer.copy(alpha = 0.75f)
                            val defaultIconTint = LocalMaterialTheme.current.onSecondaryContainer

                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                // Card 1: Add to Playlist
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = topCardShape,
                                    color = cardColor,
                                    tonalElevation = 0.dp,
                                    shadowElevation = 0.dp
                                ) {
                                    SongActionRow(
                                        painter = painterResource(R.drawable.rounded_playlist_add_24),
                                        title = "Add to playlist",
                                        iconBgColor = LocalMaterialTheme.current.primaryContainer,
                                        iconTint = LocalMaterialTheme.current.onPrimaryContainer,
                                        onClick = {
                                            smoothDismiss {
                                                showPlaylistBottomSheet = true
                                            }
                                        }
                                    )
                                }

                                // Card 2: Download
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = middleCardShape,
                                    color = cardColor,
                                    tonalElevation = 0.dp,
                                    shadowElevation = 0.dp
                                ) {
                                    SongActionRow(
                                        painter = if (isDownloaded) rememberVectorPainter(Icons.Rounded.CheckCircle) else painterResource(R.drawable.rounded_download_24),
                                        title = when {
                                            isLocalModePlayer -> "Playing from storage"
                                            isDownloaded -> "Downloaded"
                                            isDownloading && downloadProgress >= 0f -> "Downloading · ${(animatedDownloadProgress * 100).toInt()}%"
                                            isDownloading -> "Downloading…"
                                            else -> "Download"
                                        },
                                        subtitle = when {
                                            isLocalModePlayer -> "Local Mode active"
                                            isDownloaded -> "Saved to device storage"
                                            isDownloading && downloadProgress >= 0f -> "Downloading song · ${(animatedDownloadProgress * 100).toInt()}% complete"
                                            isDownloading -> "Preparing audio download…"
                                            else -> null
                                        },
                                        iconTint = if (isDownloaded) LocalMaterialTheme.current.onPrimaryContainer else defaultIconTint,
                                        iconBgColor = if (isDownloaded) LocalMaterialTheme.current.primaryContainer else defaultIconBg,
                                        trailingContent = if (isDownloading) {
                                            {
                                                if (downloadProgress < 0f) {
                                                    CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.5.dp)
                                                } else {
                                                    CircularProgressIndicator(
                                                        progress = { animatedDownloadProgress },
                                                        modifier = Modifier.size(24.dp),
                                                        strokeWidth = 2.5.dp,
                                                        trackColor = LocalMaterialTheme.current.onSurface.copy(alpha = 0.10f)
                                                    )
                                                }
                                            }
                                        } else null,
                                        onClick = {
                                            if (isLocalModePlayer) {
                                                com.saurav.pixelmusic.presentation.components.PixelToast.show(context, "Playing from storage")
                                            } else if (!isDownloaded && !isDownloading) {
                                                fileImportScope.launch {
                                                    var currentLyricsObj = lyricsProvider()
                                                    if (currentLyricsObj == null) {
                                                        currentLyricsObj = playerViewModel.stablePlayerState.value.lyrics
                                                    }

                                                    val lyricsText = if (!currentLyricsObj?.synced.isNullOrEmpty()) {
                                                        currentLyricsObj.synced.joinToString("\n") { it.line }
                                                    } else {
                                                        currentLyricsObj?.plain?.toString()
                                                    }

                                                    if (lyricsText.isNullOrBlank()) {
                                                        withContext(kotlinx.coroutines.Dispatchers.Main) {
                                                            com.saurav.pixelmusic.presentation.components.PixelToast.show(context, "Note: Downloading without lyrics (None found)")
                                                        }
                                                    }

                                                    val success = com.saurav.pixelmusic.utils.SongDownloader.downloadAndTagSong(
                                                        context = context,
                                                        song = song,
                                                        lyricsText = lyricsText
                                                    )
                                                    if (success) isDownloaded = true
                                                }
                                            }
                                        }
                                    )
                                }

                                // Card 3: Share to Stories
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = middleCardShape,
                                    color = cardColor,
                                    tonalElevation = 0.dp,
                                    shadowElevation = 0.dp
                                ) {
                                    SongActionRow(
                                        painter = rememberVectorPainter(Icons.Rounded.Share),
                                        title = "Share to Stories",
                                        iconBgColor = defaultIconBg,
                                        iconTint = defaultIconTint,
                                        onClick = {
                                            smoothDismiss {
                                                showShareSheet = true
                                            }
                                        }
                                    )
                                }

                                // Card 4: Sleep Timer
                                val isSleepTimerActive = activeTimerValueDisplay != null
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = middleCardShape,
                                    color = cardColor,
                                    tonalElevation = 0.dp,
                                    shadowElevation = 0.dp
                                ) {
                                    SongActionRow(
                                        painter = painterResource(R.drawable.rounded_timer_24),
                                        title = "Sleep timer",
                                        iconBgColor = if (isSleepTimerActive) LocalMaterialTheme.current.primaryContainer else defaultIconBg,
                                        iconTint = if (isSleepTimerActive) LocalMaterialTheme.current.onPrimaryContainer else defaultIconTint,
                                        trailingContent = {
                                            if (isSleepTimerActive) {
                                                Surface(
                                                    shape = CircleShape,
                                                    color = LocalMaterialTheme.current.primaryContainer,
                                                    contentColor = LocalMaterialTheme.current.onPrimaryContainer
                                                ) {
                                                    Text(
                                                        text = activeTimerValueDisplay.orEmpty(),
                                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                                        style = MaterialTheme.typography.labelMedium,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            } else {
                                                Box(
                                                    modifier = Modifier
                                                        .size(30.dp)
                                                        .clip(CircleShape)
                                                        .background(LocalMaterialTheme.current.surfaceContainerHigh),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                                                        contentDescription = null,
                                                        tint = LocalMaterialTheme.current.onSurfaceVariant,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                            }
                                        },
                                        onClick = {
                                            smoothDismiss {
                                                showSleepTimerSheet = true
                                            }
                                        }
                                    )
                                }

                                // Card 5: Equalizer Shortcut
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = bottomCardShape,
                                    color = cardColor,
                                    tonalElevation = 0.dp,
                                    shadowElevation = 0.dp
                                ) {
                                    SongActionRow(
                                        painter = painterResource(R.drawable.rounded_graphic_eq_24),
                                        title = "Equalizer",
                                        subtitle = "Audio effects & tuning",
                                        iconBgColor = defaultIconBg,
                                        iconTint = defaultIconTint,
                                        trailingContent = {
                                            Box(
                                                modifier = Modifier
                                                    .size(30.dp)
                                                    .clip(CircleShape)
                                                    .background(LocalMaterialTheme.current.surfaceContainerHigh),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                                                    contentDescription = null,
                                                    tint = LocalMaterialTheme.current.onSurfaceVariant,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        },
                                        onClick = {
                                            smoothDismiss {
                                                playerViewModel.triggerEqualizerNavigation()
                                            }
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
}

@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
private fun FullPlayerAlbumCoverSection(
    song: Song,
    currentPlaybackQueue: ImmutableList<Song>,
    currentMediaItemIndex: Int,
    carouselStyle: String,
    loadingTweaks: FullPlayerLoadingTweaks,
    isSheetDragGestureActive: Boolean,
    expansionFractionProvider: () -> Float,
    currentSheetState: PlayerSheetState,
    isPlayingProvider: () -> Boolean,
    playWhenReadyProvider: () -> Boolean,
    placeholderColor: Color,
    placeholderOnColor: Color,
    albumArtQuality: AlbumArtQuality,
    requestedScrollIndex: Int?,
    onSongSelected: (Song) -> Unit,
    onAlbumClick: (Song) -> Unit,
    aodScreenEnabled: Boolean,        // ADD THIS
    onEnterAodScreen: () -> Unit,
    artMorphModifier: Modifier = Modifier,
    modifier: Modifier = Modifier
) {
    val shouldApplyPausedScale = !isPlayingProvider() && !playWhenReadyProvider()
    val albumArtScale by animateFloatAsState(
        targetValue = if (shouldApplyPausedScale) 0.95f else 1f,
        animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing),
        label = "AlbumArtScale"
    )
    val configuration = LocalConfiguration.current
    val isLandscapeMode = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    val renderAlbumCover: @Composable (Dp) -> Unit = { effectiveCarouselWidth ->
        val effectiveCarouselStyle = if (isLandscapeMode) CarouselStyle.NO_PEEK else carouselStyle
        val carouselHeight = when (effectiveCarouselStyle) {
            CarouselStyle.NO_PEEK -> effectiveCarouselWidth
            CarouselStyle.ONE_PEEK -> effectiveCarouselWidth * 0.8f
            CarouselStyle.TWO_PEEK -> effectiveCarouselWidth * 0.6f
            else -> effectiveCarouselWidth * 0.8f
        }

        Box(
            modifier = Modifier
                .width(effectiveCarouselWidth)
                .height(carouselHeight)
                .then(artMorphModifier)
        ) {
            DelayedContent(
                shouldDelay = false,
                showPlaceholders = loadingTweaks.showPlaceholders,
                applyPlaceholderDelayOnClose = loadingTweaks.applyPlaceholdersOnClose,
                switchOnDragRelease = loadingTweaks.switchOnDragRelease,
                isSheetDragGestureActive = isSheetDragGestureActive,
                sharedBoundsModifier = Modifier.fillMaxWidth().height(carouselHeight),
                expansionFractionProvider = expansionFractionProvider,
                isExpandedOverride = currentSheetState == PlayerSheetState.EXPANDED,
                normalStartThreshold = 0f,
                delayAppearThreshold = loadingTweaks.contentAppearThresholdPercent / 100f,
                delayCloseThreshold = 1f - (loadingTweaks.contentCloseThresholdPercent / 100f),
                placeholder = {
                    if (loadingTweaks.transparentPlaceholders) {
                        Box(
                            Modifier
                                .height(carouselHeight)
                                .fillMaxWidth()
                                .graphicsLayer {
                                    scaleX = albumArtScale
                                    scaleY = albumArtScale
                                }
                        )
                    } else {
                        AlbumPlaceholder(
                            height = carouselHeight,
                            color = placeholderColor,
                            onColor = placeholderOnColor,
                            modifier = Modifier.graphicsLayer {
                                scaleX = albumArtScale
                                scaleY = albumArtScale
                            }
                        )
                    }
                }
            ) {
                AlbumCarouselSection(
                    currentSong = song,
                    queue = currentPlaybackQueue,
                    expansionFraction = 1f,
                    currentMediaItemIndex = currentMediaItemIndex,
                    requestedScrollIndex = requestedScrollIndex,
                    onSongSelected = { newSong ->
                        if (newSong.id != song.id) {
                            onSongSelected(newSong)
                        }
                    },
                    onAlbumClick = onAlbumClick,
                    onAlbumLongPress = { if (aodScreenEnabled) onEnterAodScreen() },
                    carouselStyle = effectiveCarouselStyle,
                    modifier = Modifier
                        .height(carouselHeight)
                        .graphicsLayer {
                            scaleX = albumArtScale
                            scaleY = albumArtScale
                        },
                    albumArtQuality = albumArtQuality,
                    carouselWidthOverride = effectiveCarouselWidth
                )
            }
        }
    }

    if (isLandscapeMode) {
        BoxWithConstraints(
            modifier = modifier
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            val landscapeWidth = minOf(
                maxWidth,
                maxHeight
            ).coerceAtLeast(1.dp)
            renderAlbumCover(landscapeWidth)
        }
    } else {
        // Stable width in portrait prevents BoxWithConstraints / HorizontalPager from
        // recomposing on every drag frame while the sheet horizontal padding animates.
        // Cap width by available window height so PiP / floating windows don't clip bottom controls.
        val stablePortraitWidth = (configuration.screenWidthDp.dp - 48.dp).coerceAtLeast(1.dp)
        val maxPortraitArtHeight = (configuration.screenHeightDp.dp - 396.dp).coerceAtLeast(72.dp)
        val styleHeightRatio = when (carouselStyle) {
            CarouselStyle.NO_PEEK -> 1f
            CarouselStyle.ONE_PEEK -> 0.8f
            CarouselStyle.TWO_PEEK -> 0.6f
            else -> 0.8f
        }
        val effectivePortraitWidth = minOf(
            stablePortraitWidth,
            maxPortraitArtHeight / styleHeightRatio
        ).coerceAtLeast(1.dp)
        Box(
            modifier = modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            renderAlbumCover(effectivePortraitWidth)
        }
    }
}

private data class DynamicArtMorphShape(
    val rxPx: Float,
    val ryPx: Float
) : androidx.compose.ui.graphics.Shape {
    override fun createOutline(
        size: androidx.compose.ui.geometry.Size,
        layoutDirection: androidx.compose.ui.unit.LayoutDirection,
        density: androidx.compose.ui.unit.Density
    ): androidx.compose.ui.graphics.Outline {
        val rx = rxPx.coerceIn(0f, size.width * 0.5f)
        val ry = ryPx.coerceIn(0f, size.height * 0.5f)
        return androidx.compose.ui.graphics.Outline.Rounded(
            androidx.compose.ui.geometry.RoundRect(
                rect = Rect(0f, 0f, size.width, size.height),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(rx, ry)
            )
        )
    }
}

@Composable
private fun FullPlayerControlsSection(
    loadingTweaks: FullPlayerLoadingTweaks,
    isSheetDragGestureActive: Boolean,
    expansionFractionProvider: () -> Float,
    currentSheetState: PlayerSheetState,
    placeholderColor: Color,
    placeholderOnColor: Color,
    isPlayingProvider: () -> Boolean,
    onPrevious: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    transportPlayPauseColors: TransportButtonColors,
    transportSkipColors: TransportButtonColors,
    isShuffleEnabledProvider: () -> Boolean,
    shuffleTransitionInProgress: Boolean,
    repeatModeProvider: () -> Int,
    isFavoriteProvider: () -> Boolean,
    onShuffleToggle: () -> Unit,
    onRepeatToggle: () -> Unit,
    onFavoriteToggle: () -> Unit,
    isFullScreenArt: Boolean,
    morphTracker: ArtworkMorphTracker = remember { ArtworkMorphTracker() },
    rootCoordsHolder: LayoutCoordsHolder? = null,
    containerWidthPxProvider: () -> Float = { 0f },
    aodSourceBounds: AodSourceBounds? = null,
    aodTransitionProgressProvider: () -> Float = { 0f }
) {
    val isLandscapeControls = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    val controlsSectionHeight = if (isLandscapeControls) 152.dp else 182.dp
    val transportHeight = if (isLandscapeControls) 76.dp else 80.dp
    val controlsSpacerHeight = if (isLandscapeControls) 6.dp else 14.dp
    val toggleMinHeight = if (isLandscapeControls) 56.dp else 66.dp
    val toggleMaxHeight = if (isLandscapeControls) 64.dp else 86.dp

    val stableControlAnimationSpec = remember {
        tween<Float>(durationMillis = 240, easing = FastOutSlowInEasing)
    }

    DelayedContent(
        shouldDelay = false,
        showPlaceholders = loadingTweaks.showPlaceholders,
        applyPlaceholderDelayOnClose = loadingTweaks.applyPlaceholdersOnClose,
        switchOnDragRelease = loadingTweaks.switchOnDragRelease,
        isSheetDragGestureActive = isSheetDragGestureActive,
        sharedBoundsModifier = Modifier.fillMaxWidth().height(controlsSectionHeight),
        expansionFractionProvider = expansionFractionProvider,
        isExpandedOverride = currentSheetState == PlayerSheetState.EXPANDED,
        normalStartThreshold = 0f,
        delayAppearThreshold = loadingTweaks.contentAppearThresholdPercent / 100f,
        delayCloseThreshold = 1f - (loadingTweaks.contentCloseThresholdPercent / 100f),
        placeholder = {
            if (loadingTweaks.transparentPlaceholders) {
                Box(Modifier.fillMaxWidth().height(controlsSectionHeight))
            } else {
                ControlsPlaceholder(placeholderColor, placeholderOnColor)
            }
        }
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AnimatedPlaybackControls(
                modifier = Modifier
                    .padding(horizontal = 12.dp, vertical = if (isLandscapeControls) 4.dp else 8.dp)
                    .graphicsLayer {
                        alpha = if (aodTransitionProgressProvider() > 0.001f) 0f else 1f
                    },
                isPlayingProvider = isPlayingProvider,
                onPrevious = onPrevious,
                onPlayPause = onPlayPause,
                onNext = onNext,
                height = transportHeight,
                pressAnimationSpec = stableControlAnimationSpec,
                releaseDelay = 220L,
                colorOtherButtons = transportSkipColors.container,
                colorPlayPause = transportPlayPauseColors.container,
                tintPlayPauseIcon = transportPlayPauseColors.content,
                tintOtherIcons = transportSkipColors.content,
                colorPreviousButton = transportSkipColors.container,
                colorNextButton = transportSkipColors.container,
                tintPreviousIcon = transportSkipColors.content,
                tintNextIcon = transportSkipColors.content,
                morphTracker = morphTracker,
                expansionFractionProvider = expansionFractionProvider,
                rootCoordsHolder = rootCoordsHolder,
                containerWidthPxProvider = containerWidthPxProvider,
                capturePrevBounds = aodSourceBounds?.prevBounds,
                capturePlayBounds = aodSourceBounds?.playBounds,
                captureNextBounds = aodSourceBounds?.nextBounds
            )

            Spacer(modifier = Modifier.height(controlsSpacerHeight))

            BottomToggleRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = toggleMinHeight, max = toggleMaxHeight)
                    .padding(horizontal = if (isLandscapeControls) 12.dp else 26.dp, vertical = 0.dp)
                    .padding(bottom = if (isLandscapeControls) 2.dp else 6.dp)
                    .graphicsLayer {
                        val aodFade = (1f - aodTransitionProgressProvider() * 2.2f).coerceIn(0f, 1f)
                        alpha = morphTracker.evaluate(expansionFractionProvider()).nonArtAlpha * aodFade
                    },
                isShuffleEnabled = isShuffleEnabledProvider(),
                isShuffleTransitionInProgress = shuffleTransitionInProgress,
                repeatMode = repeatModeProvider(),
                isFavoriteProvider = isFavoriteProvider,
                onShuffleToggle = onShuffleToggle,
                onRepeatToggle = onRepeatToggle,
                onFavoriteToggle = onFavoriteToggle,
                isFullScreenArt = isFullScreenArt
            )
        }
    }
}

@Composable
private fun FullPlayerProgressSection(
    song: Song,
    playbackMetadataMediaId: String?,
    playbackMetadataMimeType: String?,
    playbackMetadataBitrate: Int?,
    playbackMetadataSampleRate: Int?,
    currentPositionProvider: () -> Long,
    totalDurationValue: Long,
    showPlayerFileInfo: Boolean,
    onSeek: (Long) -> Unit,
    expansionFractionProvider: () -> Float,
    isPlayingProvider: () -> Boolean,
    currentSheetState: PlayerSheetState,
    progressActiveColor: Color,
    progressInactiveColor: Color,
    playerOnBaseColor: Color,
    allowRealtimeUpdates: Boolean,
    isSheetDragGestureActive: Boolean,
    loadingTweaks: FullPlayerLoadingTweaks,
    rootCoordsHolder: LayoutCoordsHolder? = null,
    aodSourceBounds: AodSourceBounds? = null,
    aodTransitionProgressProvider: () -> Float = { 0f }
) {
    val isMetadataForCurrentSong = playbackMetadataMediaId == song.id
    val audioMimeType = if (isMetadataForCurrentSong) {
        playbackMetadataMimeType ?: song.mimeType
    } else {
        song.mimeType
    }
    val audioBitrate = if (isMetadataForCurrentSong) {
        playbackMetadataBitrate ?: song.bitrate
    } else {
        song.bitrate
    }
    val audioSampleRate = if (isMetadataForCurrentSong) {
        playbackMetadataSampleRate ?: song.sampleRate
    } else {
        song.sampleRate
    }

    PlayerProgressBarSection(
        songId = song.id,
        currentPositionProvider = currentPositionProvider,
        totalDurationValue = totalDurationValue,
        songDurationHintMs = song.duration,
        audioMimeType = audioMimeType,
        audioBitrate = audioBitrate,
        audioSampleRate = audioSampleRate,
        showAudioFileInfo = showPlayerFileInfo,
        onSeek = onSeek,
        expansionFractionProvider = expansionFractionProvider,
        isPlayingProvider = isPlayingProvider,
        currentSheetState = currentSheetState,
        activeTrackColor = progressActiveColor,
        inactiveTrackColor = progressInactiveColor,
        thumbColor = progressActiveColor,
        timeTextColor = playerOnBaseColor,
        allowRealtimeUpdates = allowRealtimeUpdates,
        isSheetDragGestureActive = isSheetDragGestureActive,
        loadingTweaks = loadingTweaks,
        rootCoordsHolder = rootCoordsHolder,
        aodSourceBounds = aodSourceBounds,
        aodTransitionProgressProvider = aodTransitionProgressProvider
    )
}

private fun resolveQueueIndex(
    queue: ImmutableList<Song>,
    songId: String,
    currentMediaItemIndex: Int
): Int? {
    if (currentMediaItemIndex in queue.indices && queue[currentMediaItemIndex].id == songId) {
        return currentMediaItemIndex
    }
    return queue.indexOfFirst { it.id == songId }.takeIf { it >= 0 }
}

private fun predictSkipNextCarouselIndex(
    currentIndex: Int?,
    queue: ImmutableList<Song>,
    repeatMode: Int,
    isRemotePlaybackActive: Boolean
): Int? {
    if (isRemotePlaybackActive || queue.size <= 1) return null
    val safeCurrentIndex = currentIndex?.takeIf { it in queue.indices } ?: return null

    return when {
        safeCurrentIndex < queue.lastIndex -> safeCurrentIndex + 1
        repeatMode == Player.REPEAT_MODE_ALL -> 0
        else -> null
    }
}

private fun predictSkipPreviousCarouselIndex(
    currentIndex: Int?,
    queue: ImmutableList<Song>,
    currentPositionMs: Long,
    repeatMode: Int,
    isRemotePlaybackActive: Boolean
): Int? {
    if (isRemotePlaybackActive || queue.size <= 1) return null
    if (currentPositionMs > PREVIOUS_TRACK_RESTART_THRESHOLD_MS) return null
    val safeCurrentIndex = currentIndex?.takeIf { it in queue.indices } ?: return null

    return when {
        safeCurrentIndex > 0 -> safeCurrentIndex - 1
        repeatMode == Player.REPEAT_MODE_ALL -> queue.lastIndex
        else -> null
    }
}

suspend fun extractDominantColor(
    context: Context,
    uriString: String?,
    fallback: Color,
    isDarkTheme: Boolean
): Color = withContext(Dispatchers.IO) {
    if (uriString.isNullOrBlank()) return@withContext fallback
    try {
        val effectiveQuality = com.saurav.pixelmusic.presentation.components.SmartImageCache.getEffectiveQuality()
        val optimizedUri = com.saurav.pixelmusic.utils.ThumbnailUrlUtils.optimizeArtworkUrl(uriString, effectiveQuality) ?: uriString
        val canonicalDiskKey = com.saurav.pixelmusic.utils.ThumbnailUrlUtils.getCanonicalArtworkDiskCacheKey(optimizedUri, effectiveQuality) ?: optimizedUri

        val cachedMemBitmap = context.imageLoader.memoryCache
            ?.get(coil.memory.MemoryCache.Key(canonicalDiskKey))
            ?.bitmap
            ?.takeIf { !it.isRecycled }
            ?.let { bmp ->
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O &&
                    bmp.config == android.graphics.Bitmap.Config.HARDWARE
                ) {
                    bmp.copy(android.graphics.Bitmap.Config.ARGB_8888, false)
                } else {
                    bmp
                }
            }

        val bitmap = cachedMemBitmap ?: run {
            val request = coil.request.ImageRequest.Builder(context)
                .data(optimizedUri)
                .diskCacheKey(canonicalDiskKey)
                .allowHardware(false) // Palette needs a software bitmap
                .size(160, 160)       // small = fast, plenty for color extraction
                .build()
            val result = context.imageLoader.execute(request)
            (result.drawable as? android.graphics.drawable.BitmapDrawable)?.bitmap
        } ?: return@withContext fallback
        val palette = androidx.palette.graphics.Palette.from(bitmap).generate()
        val swatch = if (isDarkTheme) {
            palette.darkMutedSwatch ?: palette.dominantSwatch ?: palette.mutedSwatch
        } else {
            palette.lightMutedSwatch ?: palette.mutedSwatch ?: palette.dominantSwatch
        }
        swatch?.let { Color(it.rgb) } ?: fallback
    } catch (e: Exception) {
        fallback
    }
}

@androidx.annotation.OptIn(UnstableApi::class)
@Composable
private fun FullPlayerSongMetadataSection(
    song: Song,
    currentSongArtists: List<Artist>,
    loadingTweaks: FullPlayerLoadingTweaks,
    isSheetDragGestureActive: Boolean,
    expansionFractionProvider: () -> Float,
    currentSheetState: PlayerSheetState,
    placeholderColor: Color,
    placeholderOnColor: Color,
    isLandscape: Boolean,
    onLyricsClick: () -> Unit,
    onShareClick: () -> Unit,
    playerOnBaseColor: Color,
    playerViewModel: PlayerViewModel,
    gradientEdgeColor: Color,
    chipColor: Color,
    chipContentColor: Color,
    onQueueClick: () -> Unit,
    onTitleClick: () -> Unit = {},
    onArtistClick: () -> Unit,
    morphTracker: ArtworkMorphTracker = remember { ArtworkMorphTracker() },
    rootCoordsHolder: LayoutCoordsHolder? = null,
    aodSourceBounds: AodSourceBounds? = null,
    aodTransitionProgressProvider: () -> Float = { 0f }
) {
    DelayedContent(
        shouldDelay = false,
        showPlaceholders = loadingTweaks.showPlaceholders,
        applyPlaceholderDelayOnClose = loadingTweaks.applyPlaceholdersOnClose,
        switchOnDragRelease = loadingTweaks.switchOnDragRelease,
        isSheetDragGestureActive = isSheetDragGestureActive,
        sharedBoundsModifier = Modifier.fillMaxWidth().heightIn(min = 70.dp),
        expansionFractionProvider = expansionFractionProvider,
        isExpandedOverride = currentSheetState == PlayerSheetState.EXPANDED,
        normalStartThreshold = 0f,
        delayAppearThreshold = loadingTweaks.contentAppearThresholdPercent / 100f,
        delayCloseThreshold = 1f - (loadingTweaks.contentCloseThresholdPercent / 100f),
        placeholder = {
            if (loadingTweaks.transparentPlaceholders) {
                Box(Modifier.fillMaxWidth().height(70.dp))
            } else {
                MetadataPlaceholder(
                    expansionFractionProvider = expansionFractionProvider,
                    color = placeholderColor,
                    onColor = placeholderOnColor,
                    showQueueButtons = isLandscape
                )
            }
        }
    ) {
        SongMetadataDisplaySection(
            modifier = Modifier
                .padding(start = 0.dp),
            onClickLyrics = onLyricsClick,
            onClickShare = onShareClick,
            song = song,
            currentSongArtists = currentSongArtists,
            expansionFractionProvider = expansionFractionProvider,
            textColor = playerOnBaseColor,
            artistTextColor = playerOnBaseColor.copy(alpha = 0.7f),
            playerViewModel = playerViewModel,
            gradientEdgeColor = gradientEdgeColor,
            chipColor = chipColor,
            chipContentColor = chipContentColor,
            showQueueButton = isLandscape,
            onClickQueue = onQueueClick,
            onClickTitle = onTitleClick,
            onClickArtist = onArtistClick,
            morphTracker = morphTracker,
            rootCoordsHolder = rootCoordsHolder,
            aodSourceBounds = aodSourceBounds,
            aodTransitionProgressProvider = aodTransitionProgressProvider
        )
    }
}

@Composable
private fun FullPlayerPortraitContent(
    paddingValues: PaddingValues,
    albumCoverSection: @Composable (Modifier) -> Unit,
    songMetadataSection: @Composable () -> Unit,
    playerProgressSection: @Composable () -> Unit,
    controlsSection: @Composable () -> Unit,
    singleLineLyricsSection: @Composable () -> Unit,
    isFullScreenArt: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(
                horizontal = 24.dp,
                vertical = 0.dp
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceAround
    ) {
        if (isFullScreenArt) {
            Spacer(modifier = Modifier.weight(1f))
        } else {
            albumCoverSection(Modifier.weight(1f, fill = false))
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            singleLineLyricsSection()
            Box(Modifier.align(Alignment.Start)) {
                songMetadataSection()
            }
            playerProgressSection()
        }

        controlsSection()
    }
}
@Composable
private fun FullPlayerLandscapeContent(
    paddingValues: PaddingValues,
    albumCoverSection: @Composable (Modifier) -> Unit,
    songMetadataSection: @Composable () -> Unit,
    playerProgressSection: @Composable () -> Unit,
    controlsSection: @Composable () -> Unit,
    singleLineLyricsSection: @Composable () -> Unit,
    isFullScreenArt: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(
                start = 14.dp,
                end = 20.dp,
                top = 12.dp,
                bottom = 12.dp
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isFullScreenArt) {
            Spacer(Modifier.fillMaxHeight().weight(1f))
        } else {
            albumCoverSection(Modifier.fillMaxHeight().weight(1f))
        }
        Spacer(Modifier.width(12.dp))
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .weight(1f)
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.Bottom)
        ) {
            singleLineLyricsSection()
            songMetadataSection()
            playerProgressSection()
            controlsSection()
        }
    }
}

@androidx.annotation.OptIn(UnstableApi::class)
@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun SongMetadataDisplaySection(
    song: Song?,
    currentSongArtists: List<Artist>,
    expansionFractionProvider: () -> Float,
    textColor: Color,
    artistTextColor: Color,
    gradientEdgeColor: Color,
    playerViewModel: PlayerViewModel,
    chipColor: Color,
    chipContentColor: Color,
    onClickLyrics: () -> Unit,
    onClickShare: () -> Unit,
    showQueueButton: Boolean,
    onClickQueue: () -> Unit,
    onClickTitle: () -> Unit = {},
    onClickArtist: () -> Unit,
    modifier: Modifier = Modifier,
    morphTracker: ArtworkMorphTracker = remember { ArtworkMorphTracker() },
    rootCoordsHolder: LayoutCoordsHolder? = null,
    aodSourceBounds: AodSourceBounds? = null,
    aodTransitionProgressProvider: () -> Float = { 0f }
) {
    val density = LocalDensity.current
    val configuration = LocalConfiguration.current
    val fallbackScreenWidthPx = remember(configuration, density) {
        with(density) { configuration.screenWidthDp.dp.toPx() }
    }
    val miniSongInfoLeftPx = remember(density) { with(density) { 66.dp.toPx() } }
    val miniSongInfoTopPx = remember(density) { with(density) { 13.5.dp.toPx() } }
    val miniReservedWidthPx = remember(density) { with(density) { 210.dp.toPx() } }
    val fallbackSongInfoLeftPx = remember(density) { with(density) { 24.dp.toPx() } }
    val fallbackSongInfoTopPx = remember(configuration, density) {
        with(density) { (configuration.screenHeightDp.dp * 0.56f).toPx() }
    }
    val songInfoBounds = aodSourceBounds?.songInfoBounds ?: remember { MorphElementBounds() }
    Row(
        modifier
            .fillMaxWidth()
            .heightIn(min = 70.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        song?.let { currentSong ->
            PlayerSongInfo(
                title = currentSong.title,
                artist = currentSong.displayArtist.takeIf { it.isNotBlank() } ?: currentSong.artist,
                artistId = currentSong.artistId,
                artists = currentSongArtists,
                expansionFractionProvider = expansionFractionProvider,
                textColor = textColor,
                artistTextColor = artistTextColor,
                gradientEdgeColor = gradientEdgeColor,
                playerViewModel = playerViewModel,
                onClickTitle = onClickTitle,
                onClickArtist = onClickArtist,
                morphTracker = morphTracker,
                rootCoordsHolder = rootCoordsHolder,
                fallbackScreenWidthPx = fallbackScreenWidthPx,
                miniReservedWidthPx = miniReservedWidthPx,
                aodSourceBounds = aodSourceBounds,
                aodTransitionProgressProvider = aodTransitionProgressProvider,
                modifier = Modifier
                    .weight(1f)
                    .align(Alignment.CenterVertically)
                    .padding(vertical = 4.dp)
                    .captureUntransformedBounds(rootCoordsHolder, songInfoBounds)
                    .graphicsLayer {
                        val s = morphTracker.evaluate(expansionFractionProvider())
                        val destX = if (songInfoBounds.hasBounds) songInfoBounds.offset.x else fallbackSongInfoLeftPx
                        val destY = if (songInfoBounds.hasBounds) songInfoBounds.offset.y else fallbackSongInfoTopPx
                        val scale = androidx.compose.ui.util.lerp(0.625f, 1f, s.sizeT)
                        transformOrigin = TransformOrigin(0f, 0f)
                        scaleX = scale
                        scaleY = scale
                        translationX = (miniSongInfoLeftPx - destX) * (1f - s.posT)
                        translationY = (miniSongInfoTopPx - destY) * (1f - s.posT)
                        alpha = if (aodTransitionProgressProvider() > 0.001f) 0f else 1f
                    }
            )
        }
        
        val stablePlayerState by playerViewModel.stablePlayerState.collectAsStateWithLifecycle()
        val isBuffering = stablePlayerState.isBuffering


        AnimatedVisibility(
            visible = isBuffering,
            enter = scaleIn(
                initialScale = 0.85f,
                animationSpec = tween(
                    durationMillis = 400,
                    delayMillis = 80,
                    easing = FastOutSlowInEasing
                )
            ) + fadeIn(
                animationSpec = tween(
                    durationMillis = 300,
                    delayMillis = 80
                )
            ),
            exit = scaleOut(
                targetScale = 0.85f,
                animationSpec = tween(
                    durationMillis = 300,
                    easing = FastOutSlowInEasing
                )
            ) + fadeOut(
                animationSpec = tween(
                    durationMillis = 200
                )
            )
        ) {
            Surface(
                shape = CircleShape,
                color = chipColor,
                modifier = Modifier.padding(end = 8.dp)
            ) {
                Box(
                    modifier = Modifier.padding(10.dp), 
                    contentAlignment = Alignment.Center
                ) {
                    LoadingIndicator(
                        modifier = Modifier.size(28.dp),
                        color = chipContentColor
                    )
                }
            }
        }

        if (showQueueButton) {
            // Landscape mode: [Lyrics] [Theme] [⋮] [Queue]
            Row(
                modifier = Modifier.graphicsLayer {
                    val aodFade = (1f - aodTransitionProgressProvider() * 2.2f).coerceIn(0f, 1f)
                    alpha = morphTracker.evaluate(expansionFractionProvider()).nonArtAlpha * aodFade
                },
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Lyrics button
                Box(
                    modifier = Modifier
                        .size(height = 42.dp, width = 44.dp)
                        .clip(
                            RoundedCornerShape(
                                topStart = 50.dp, topEnd = 6.dp,
                                bottomStart = 50.dp, bottomEnd = 6.dp
                            )
                        )
                        .background(chipColor)
                        .clickable { onClickLyrics() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(R.drawable.rounded_lyrics_24),
                        contentDescription = stringResource(R.string.presentation_batch_g_player_cd_lyrics),
                        tint = chipContentColor
                    )
                }
                // Share / More options button
                Box(
                    modifier = Modifier
                        .size(height = 42.dp, width = 44.dp)
                        .clip(
                            RoundedCornerShape(
                                topStart = 6.dp, topEnd = 6.dp,
                                bottomStart = 6.dp, bottomEnd = 6.dp
                            )
                        )
                        .background(chipColor)
                        .clickable { onClickShare() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(R.drawable.rounded_more_vert_24),
                        contentDescription = stringResource(R.string.cd_player_more_options),
                        tint = chipContentColor
                    )
                }
                // Queue button
                Box(
                    modifier = Modifier
                        .size(height = 42.dp, width = 44.dp)
                        .clip(
                            RoundedCornerShape(
                                topStart = 6.dp, topEnd = 50.dp,
                                bottomStart = 6.dp, bottomEnd = 50.dp
                            )
                        )
                        .background(chipColor)
                        .clickable { onClickQueue() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(R.drawable.rounded_queue_music_24),
                        contentDescription = stringResource(R.string.presentation_batch_g_player_cd_queue),
                        tint = chipContentColor
                    )
                }
            }
        } else {
            // Portrait Mode: [Lyrics] [⋮]
            val lyricsShape = RoundedCornerShape(
                topStart = 50.dp, topEnd = 6.dp,
                bottomStart = 50.dp, bottomEnd = 6.dp
            )
            val moreShape = RoundedCornerShape(
                topStart = 6.dp, topEnd = 50.dp,
                bottomStart = 6.dp, bottomEnd = 50.dp
            )

            Row(
                modifier = Modifier.graphicsLayer {
                    val aodFade = (1f - aodTransitionProgressProvider() * 2.2f).coerceIn(0f, 1f)
                    alpha = morphTracker.evaluate(expansionFractionProvider()).nonArtAlpha * aodFade
                },
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Default FilledIconButton
                FilledIconButton(
                    modifier = Modifier.size(width = 44.dp, height = 48.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = chipColor,
                        contentColor = chipContentColor
                    ),
                    shape = lyricsShape,
                    onClick = onClickLyrics,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.rounded_lyrics_24),
                        contentDescription = stringResource(R.string.presentation_batch_g_player_cd_lyrics)
                    )
                }
                FilledIconButton(
                    modifier = Modifier.size(width = 44.dp, height = 48.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = chipColor,
                        contentColor = chipContentColor
                    ),
                    shape = moreShape,
                    onClick = onClickShare,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.rounded_more_vert_24),
                        contentDescription = stringResource(R.string.cd_player_more_options)
                    )
                }
            }
        }
    }
}

private fun formatAudioMetaLabel(mimeType: String?, bitrate: Int?, sampleRate: Int?): String? {
    val formatLabel = mimeTypeToFormat(mimeType)
        .takeIf { it != "-" }
        ?.uppercase(Locale.getDefault())

    val parts = buildList {
        sampleRate?.takeIf { it > 0 }?.let { add(String.format(Locale.US, "%.1f kHz", it / 1000.0)) }
        bitrate?.takeIf { it > 0 }?.let { bitrateValue ->
            val kbpsLabel = "${bitrateValue / 1000} kbps"
            if (formatLabel != null) {
                add("$kbpsLabel \u2022 $formatLabel")
            } else {
                add(kbpsLabel)
            }
        } ?: formatLabel?.let { add(it) }
    }
    return parts.takeIf { it.isNotEmpty() }?.joinToString(" \u2022 ")
}

@Composable
private fun PlayerProgressBarSection(
    songId: String,
    currentPositionProvider: () -> Long,
    totalDurationValue: Long,
    songDurationHintMs: Long,
    audioMimeType: String?,
    audioBitrate: Int?,
    audioSampleRate: Int?,
    showAudioFileInfo: Boolean,
    onSeek: (Long) -> Unit,
    expansionFractionProvider: () -> Float,
    isPlayingProvider: () -> Boolean,
    currentSheetState: PlayerSheetState,
    activeTrackColor: Color,
    inactiveTrackColor: Color,
    thumbColor: Color,
    timeTextColor: Color,
    allowRealtimeUpdates: Boolean = true,
    isSheetDragGestureActive: Boolean = false,
    loadingTweaks: FullPlayerLoadingTweaks? = null,
    rootCoordsHolder: LayoutCoordsHolder? = null,
    aodSourceBounds: AodSourceBounds? = null,
    aodTransitionProgressProvider: () -> Float = { 0f },
    modifier: Modifier = Modifier
) {
    val progressBounds = aodSourceBounds?.progressBounds ?: remember { MorphElementBounds() }
    val progressSectionHorizontalInset = 0.dp
    val isVisible by remember(expansionFractionProvider) {
        derivedStateOf { expansionFractionProvider() > 0.01f }
    }
    val isExpanded by remember(currentSheetState, expansionFractionProvider) {
        derivedStateOf {
            currentSheetState == PlayerSheetState.EXPANDED && expansionFractionProvider() >= 0.995f
        }
    }
    val shouldRunRealtimeUpdates = allowRealtimeUpdates && isVisible

    val reportedDuration = totalDurationValue.coerceAtLeast(0L)
    val hintDuration = songDurationHintMs.coerceAtLeast(0L)
    val displayDurationValue = when {
        reportedDuration <= 0L && hintDuration <= 0L -> 0L
        reportedDuration <= 0L -> hintDuration
        hintDuration <= 0L -> reportedDuration
        kotlin.math.abs(reportedDuration - hintDuration) <= 1500L -> reportedDuration
        else -> minOf(reportedDuration, hintDuration)
    }
    val audioMetaLabel = remember(showAudioFileInfo, audioMimeType, audioBitrate, audioSampleRate) {
        if (showAudioFileInfo) {
            formatAudioMetaLabel(
                mimeType = audioMimeType,
                bitrate = audioBitrate,
                sampleRate = audioSampleRate
            )
        } else {
            null
        }
    }
    var displayAudioMetaLabel by remember(songId) { mutableStateOf<String?>(null) }
    LaunchedEffect(songId, audioMetaLabel, showAudioFileInfo) {
        if (!showAudioFileInfo) {
            displayAudioMetaLabel = null
        } else if (!audioMetaLabel.isNullOrBlank()) {
            displayAudioMetaLabel = audioMetaLabel
        } else {
            kotlinx.coroutines.delay(500)
            displayAudioMetaLabel = null
        }
    }
    val durationForCalc = displayDurationValue.coerceAtLeast(1L)
    
    // Pass isVisible to rememberSmoothProgress
    val (smoothProgressState, _) = rememberSmoothProgress(
        isPlayingProvider = isPlayingProvider,
        currentPositionProvider = currentPositionProvider,
        totalDuration = displayDurationValue,
        songId = songId,
        sampleWhilePlayingMs = if (isExpanded) 180L else 320L,
        sampleWhilePausedMs = 800L,
        isVisible = shouldRunRealtimeUpdates
    )

    var sliderDragValue by remember(songId) { mutableStateOf<Float?>(null) }
    // Held seek target (fraction) — mirrors PlayerSeekBar so the slider stays where the user
    // dropped it until real playback catches up. Fraction-based so it survives duration drift.
    var targetSeekFraction by remember(songId) { mutableFloatStateOf(-1f) }
    var lastSeekFinishedTime by remember(songId) { mutableLongStateOf(0L) }

    // Reset seek state on song change to avoid stale position from previous song.
    LaunchedEffect(songId) {
        sliderDragValue = null
        targetSeekFraction = -1f
        lastSeekFinishedTime = 0L
    }

    // Release the held target once smooth progress catches up (within 4%) or after a 1.5 s
    // safety net — same thresholds as the LyricsSheet PlayerSeekBar.
    LaunchedEffect(songId) {
        snapshotFlow { smoothProgressState.value }.collect { progress ->
            if (sliderDragValue != null) return@collect
            val target = targetSeekFraction
            if (target < 0f) return@collect
            val timeSinceSeek = System.currentTimeMillis() - lastSeekFinishedTime
            val diff = kotlin.math.abs(progress - target)
            if (timeSinceSeek > 1500L || diff < 0.04f) {
                targetSeekFraction = -1f
            }
        }
    }

    LaunchedEffect(lastSeekFinishedTime, targetSeekFraction) {
        if (targetSeekFraction >= 0f && lastSeekFinishedTime > 0L) {
            kotlinx.coroutines.delay(1500L)
            if (sliderDragValue == null) {
                targetSeekFraction = -1f
            }
        }
    }

    val interactionSource = remember { MutableInteractionSource() }
    val shouldAnimateWavyProgress by remember(shouldRunRealtimeUpdates, isPlayingProvider) {
        derivedStateOf { shouldRunRealtimeUpdates && isPlayingProvider() }
    }

    // Always drive the thumb from smoothed progress to avoid visual jumps from 500ms raw ticks.
    val animatedProgressState = remember(smoothProgressState) {
        derivedStateOf {
            when {
                sliderDragValue != null -> sliderDragValue!!
                targetSeekFraction >= 0f -> targetSeekFraction
                else -> smoothProgressState.value
            }
        }
    }

    // No LaunchedEffect/snapshotFlow needed anymore. 
    // smoothProgressState is already 60fps animated.

    val effectivePositionState = remember(durationForCalc, animatedProgressState, isVisible, displayDurationValue) {
        derivedStateOf {
             val progress = animatedProgressState.value
             (progress * durationForCalc).roundToLong().coerceIn(0L, displayDurationValue)
        }
    }

    val shouldDelay = loadingTweaks?.let { it.delayAll || it.delayProgressBar } ?: false

    val placeholderColor = LocalMaterialTheme.current.onPrimaryContainer.copy(alpha = 0.25f)
    val placeholderOnColor = LocalMaterialTheme.current.onPrimaryContainer.copy(alpha = 0.2f)

    DelayedContent(
        shouldDelay = shouldDelay,
        showPlaceholders = loadingTweaks?.showPlaceholders ?: false,
        applyPlaceholderDelayOnClose = loadingTweaks?.applyPlaceholdersOnClose ?: true,
        switchOnDragRelease = loadingTweaks?.switchOnDragRelease ?: false,
        isSheetDragGestureActive = isSheetDragGestureActive,
        sharedBoundsModifier = Modifier.fillMaxWidth().heightIn(min = 70.dp),
        expansionFractionProvider = expansionFractionProvider,
        isExpandedOverride = currentSheetState == PlayerSheetState.EXPANDED,
        normalStartThreshold = 0.08f,
        delayAppearThreshold = (loadingTweaks?.contentAppearThresholdPercent ?: 0) / 100f,
        delayCloseThreshold = 1f - ((loadingTweaks?.contentCloseThresholdPercent ?: 0) / 100f),
        placeholder = {
             if (loadingTweaks?.transparentPlaceholders == true) {
                 Box(Modifier.fillMaxWidth().heightIn(min = 70.dp))
             } else {
                 ProgressPlaceholder(
                     color = placeholderColor,
                     onColor = placeholderOnColor,
                     showAudioMetaChip = showAudioFileInfo && !displayAudioMetaLabel.isNullOrBlank()
                 )
             }
        }
    ) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .heightIn(min = 70.dp)
                .captureUntransformedBounds(rootCoordsHolder, progressBounds)
                .graphicsLayer {
                    val aodFade = (1f - aodTransitionProgressProvider() * 2.4f).coerceIn(0f, 1f)
                    alpha = aodFade
                }
        ) {
            // Isolated Slider Component
            androidx.compose.runtime.key(songId) {
                Box(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    EfficientSlider(
                        valueState = animatedProgressState,
                        onValueChange = { sliderDragValue = it },
                        onValueCommit = { finalValue ->
                            val targetMs = (finalValue * durationForCalc).roundToLong()
                            targetSeekFraction = finalValue
                            lastSeekFinishedTime = System.currentTimeMillis()
                            onSeek(targetMs)
                            sliderDragValue = null
                        },
                        thumbColor = thumbColor,
                        activeTrackColor = activeTrackColor,
                        inactiveTrackColor = inactiveTrackColor,
                        interactionSource = interactionSource,
                        isPlaying = shouldAnimateWavyProgress,
                        trackEdgePadding = progressSectionHorizontalInset
                    )
                }
            }

            // Isolated Time Labels
            EfficientTimeLabels(
                positionState = effectivePositionState,
                duration = displayDurationValue,
                isVisible = isVisible,
                textColor = timeTextColor,
                audioMetaLabel = displayAudioMetaLabel,
                horizontalTrackInset = progressSectionHorizontalInset
            )
        }
    }
}

@Composable
private fun EfficientSlider(
    valueState: androidx.compose.runtime.State<Float>,
    onValueChange: (Float) -> Unit,
    onValueCommit: (Float) -> Unit,
    thumbColor: Color,
    activeTrackColor: Color,
    inactiveTrackColor: Color,
    interactionSource: MutableInteractionSource,
    isPlaying: Boolean,
    trackEdgePadding: Dp
) {
    val haptics = LocalHapticFeedback.current
    val currentOnValueChange = rememberUpdatedState(onValueChange)
    val currentHaptics = rememberUpdatedState(haptics)
    val lastHapticStep = remember { intArrayOf(-1) }
    val onValueChangeWithHaptics = remember {
        { newValue: Float ->
            val quantized = (newValue.coerceIn(0f, 1f) * 20f).toInt()
            if (quantized != lastHapticStep[0]) {
                lastHapticStep[0] = quantized
                currentHaptics.value.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            }
            currentOnValueChange.value(newValue)
        }
    }

    WavySliderExpressive(
        value = valueState.value,
        onValueChange = onValueChangeWithHaptics,
        onValueCommit = onValueCommit,
        interactionSource = interactionSource,
        activeTrackColor = activeTrackColor,
        inactiveTrackColor = inactiveTrackColor,
        thumbColor = thumbColor,
        isPlaying = isPlaying,
        trackEdgePadding = trackEdgePadding,
        semanticsLabel = "Playback position",
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp, horizontal = 0.dp)
    )
}

@Composable
private fun EfficientTimeLabels(
    positionState: androidx.compose.runtime.State<Long>,
    duration: Long,
    isVisible: Boolean,
    textColor: Color,
    audioMetaLabel: String?,
    horizontalTrackInset: Dp
) {
    val coarsePositionMs by remember(isVisible, positionState) {
        derivedStateOf {
            if (!isVisible) 0L
            else (positionState.value.coerceAtLeast(0L) / 1000L) * 1000L
        }
    }
    val posStr by remember(isVisible, coarsePositionMs) {
        derivedStateOf { if (isVisible) formatDuration(coarsePositionMs) else "--:--" }
    }
    val durStr = remember(isVisible, duration) {
        if (isVisible) formatDuration(duration.coerceAtLeast(0L)) else "--:--"
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = horizontalTrackInset)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.Center),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                posStr,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                fontWeight = FontWeight.SemiBold,
                color = textColor
            )
            Text(
                durStr,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                fontWeight = FontWeight.SemiBold,
                color = textColor
            )
        }

        if (!audioMetaLabel.isNullOrBlank()) {
            Surface(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(horizontal = 58.dp),
                shape = RoundedCornerShape(999.dp),
                color = textColor.copy(alpha = 0.14f),
                contentColor = textColor.copy(alpha = 0.96f)
            ) {
                Text(
                    text = audioMetaLabel,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Medium,
                        fontSize = 11.sp
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                )
            }
        }
    }
}

@Composable
private fun DelayedContent(
    shouldDelay: Boolean,
    showPlaceholders: Boolean,
    applyPlaceholderDelayOnClose: Boolean,
    switchOnDragRelease: Boolean,
    isSheetDragGestureActive: Boolean,
    sharedBoundsModifier: Modifier = Modifier,
    expansionFractionProvider: () -> Float,
    isExpandedOverride: Boolean = false,
    normalStartThreshold: Float,
    delayAppearThreshold: Float,
    delayCloseThreshold: Float,
    placeholder: @Composable () -> Unit,
    content: @Composable () -> Unit
) {
    val appearThreshold = delayAppearThreshold.coerceIn(0f, 1f)
    val closeThreshold = delayCloseThreshold.coerceIn(0f, 1f)
    var isDelayGateOpen by remember(shouldDelay) { mutableStateOf(!shouldDelay) }

    LaunchedEffect(
        shouldDelay,
        appearThreshold,
        closeThreshold,
        applyPlaceholderDelayOnClose,
        switchOnDragRelease,
        isSheetDragGestureActive,
        isExpandedOverride,
        expansionFractionProvider
    ) {
        if (!shouldDelay) {
            isDelayGateOpen = true
            return@LaunchedEffect
        }

        if (switchOnDragRelease) {
            if (isSheetDragGestureActive) {
                return@LaunchedEffect
            }

            if (isExpandedOverride) {
                isDelayGateOpen = true
            } else {
                snapshotFlow { expansionFractionProvider().coerceIn(0f, 1f) }
                    .first { fraction -> fraction <= 0.001f }
                isDelayGateOpen = false
            }
            return@LaunchedEffect
        }

        var previousExpansionFraction = expansionFractionProvider().coerceIn(0f, 1f)
        var previousExpandedOverride = isExpandedOverride

        snapshotFlow {
            val rawExpansionFraction = expansionFractionProvider().coerceIn(0f, 1f)
            val effectiveExpansionFraction =
                if (isExpandedOverride && rawExpansionFraction >= 0.985f) 1f else rawExpansionFraction
            DelayedContentFrame(
                rawExpansionFraction = rawExpansionFraction,
                effectiveExpansionFraction = effectiveExpansionFraction,
                isExpandedOverride = isExpandedOverride
            )
        }.collect { frame ->
            val isCollapsingByFraction =
                frame.rawExpansionFraction < previousExpansionFraction - 0.001f
            val isExpandingByFraction =
                frame.rawExpansionFraction > previousExpansionFraction + 0.001f
            val justStartedCollapsing =
                previousExpandedOverride && !frame.isExpandedOverride
            val justStartedExpanding =
                !previousExpandedOverride && frame.isExpandedOverride
            val isCollapsing = isCollapsingByFraction || justStartedCollapsing
            val isExpanding = isExpandingByFraction || justStartedExpanding
            val isFullyExpanded =
                frame.isExpandedOverride && frame.effectiveExpansionFraction >= 0.985f

            if (frame.effectiveExpansionFraction <= 0.001f && !frame.isExpandedOverride) {
                isDelayGateOpen = false
            } else if (isFullyExpanded) {
                isDelayGateOpen = true
            } else if (isDelayGateOpen) {
                if (applyPlaceholderDelayOnClose &&
                    isCollapsing &&
                    frame.effectiveExpansionFraction <= closeThreshold
                ) {
                    isDelayGateOpen = false
                }
            } else if (
                frame.effectiveExpansionFraction >= appearThreshold &&
                    (!applyPlaceholderDelayOnClose || isExpanding || frame.isExpandedOverride)
            ) {
                isDelayGateOpen = true
            }

            previousExpansionFraction = frame.rawExpansionFraction
            previousExpandedOverride = frame.isExpandedOverride
        }
    }

    val baseAlphaProvider = remember(normalStartThreshold, expansionFractionProvider) {
        {
            ((expansionFractionProvider().coerceIn(0f, 1f) - normalStartThreshold) /
                (1f - normalStartThreshold).coerceAtLeast(0.001f))
                .coerceIn(0f, 1f)
        }
    }
    val contentBlendAlpha by animateFloatAsState(
        targetValue = if (isDelayGateOpen) 1f else 0f,
        animationSpec = if (isDelayGateOpen) {
            tween(durationMillis = 260, easing = FastOutSlowInEasing)
        } else {
            tween(durationMillis = 140, easing = FastOutSlowInEasing)
        },
        label = "DelayedContentBlendAlpha"
    )
    val placeholderBlendAlpha by animateFloatAsState(
        targetValue = if (isDelayGateOpen) 0f else 1f,
        animationSpec = if (isDelayGateOpen) {
            tween(durationMillis = 360, easing = FastOutSlowInEasing)
        } else {
            tween(durationMillis = 140, easing = FastOutSlowInEasing)
        },
        label = "DelayedPlaceholderBlendAlpha"
    )

    if (shouldDelay) {
        Box(modifier = sharedBoundsModifier) {
            val shouldComposeContent = isDelayGateOpen

            if (shouldComposeContent) {
                Box(
                    modifier = Modifier.graphicsLayer {
                        alpha = contentBlendAlpha * baseAlphaProvider()
                    }
                ) {
                    content()
                }
            }
            if (showPlaceholders && placeholderBlendAlpha > 0.001f && !isDelayGateOpen) {
                Box(
                    modifier = Modifier.graphicsLayer { alpha = placeholderBlendAlpha }
                ) {
                    placeholder()
                }
            }
        }
    } else {
        Box(
            modifier = if (normalStartThreshold > 0f) {
                sharedBoundsModifier.graphicsLayer { alpha = baseAlphaProvider() }
            } else {
                sharedBoundsModifier
            }
        ) {
            content()
        }
    }
}

private data class DelayedContentFrame(
    val rawExpansionFraction: Float,
    val effectiveExpansionFraction: Float,
    val isExpandedOverride: Boolean
)

@androidx.annotation.OptIn(UnstableApi::class)
@Composable
private fun PlayerSongInfo(
    title: String,
    artist: String,
    artistId: Long,
    artists: List<Artist>,
    expansionFractionProvider: () -> Float,
    textColor: Color,
    artistTextColor: Color,
    gradientEdgeColor: Color,
    playerViewModel: PlayerViewModel,
    onClickTitle: () -> Unit = {},
    onClickArtist: () -> Unit,
    morphTracker: ArtworkMorphTracker = remember { ArtworkMorphTracker() },
    rootCoordsHolder: LayoutCoordsHolder? = null,
    fallbackScreenWidthPx: Float = 0f,
    miniReservedWidthPx: Float = 0f,
    aodSourceBounds: AodSourceBounds? = null,
    aodTransitionProgressProvider: () -> Float = { 0f },
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val artistSpacerPx = remember(density) { with(density) { 4.dp.toPx() } }
    val coroutineScope = rememberCoroutineScope()
    var isNavigatingToArtist by remember { mutableStateOf(false) }
    val resolvedArtistId by remember(artists, artistId) {
        derivedStateOf { artists.firstOrNull { it.id != 0L && it.id != -1L }?.id ?: artistId }
    }
    val fallbackTitleMarquee = remember { MarqueeSyncState() }
    val fallbackArtistMarquee = remember { MarqueeSyncState() }
    val shouldRunMarquee = expansionFractionProvider() > 0.004f && aodTransitionProgressProvider() <= 0.001f
    val shouldRunMarqueeProvider = remember(expansionFractionProvider, aodTransitionProgressProvider) {
        { expansionFractionProvider() > 0.004f && aodTransitionProgressProvider() <= 0.001f }
    }
    val morphFractionProvider = remember(morphTracker, expansionFractionProvider) {
        { morphTracker.evaluate(expansionFractionProvider()).sizeT }
    }
    val miniAvailableWidthPxProvider = remember(aodSourceBounds, rootCoordsHolder, fallbackScreenWidthPx, miniReservedWidthPx) {
        {
            val measuredMiniW = aodSourceBounds?.miniSongInfoWidthPx ?: 0f
            if (measuredMiniW > 0f) {
                measuredMiniW
            } else {
                val sheetW = rootCoordsHolder?.widthPx?.takeIf { it > 0f } ?: fallbackScreenWidthPx
                (sheetW - miniReservedWidthPx).coerceAtLeast(60f)
            }
        }
    }
    val fullAvailableWidthPxProvider = remember(aodSourceBounds, rootCoordsHolder, fallbackScreenWidthPx) {
        {
            val boundW = aodSourceBounds?.songInfoBounds?.size?.width?.toFloat() ?: 0f
            val curRootW = rootCoordsHolder?.widthPx?.takeIf { it > 0f } ?: fallbackScreenWidthPx
            val deltaW = (fallbackScreenWidthPx - curRootW).coerceAtLeast(0f)
            if (boundW > 0f) boundW + deltaW else 0f
        }
    }
    val titleScaleProvider = remember(morphTracker, expansionFractionProvider) {
        { androidx.compose.ui.util.lerp(0.625f, 1f, morphTracker.evaluate(expansionFractionProvider()).sizeT) }
    }
    val artistScaleProvider = remember(morphTracker, expansionFractionProvider) {
        { androidx.compose.ui.util.lerp(13f / 15f, 1f, morphTracker.evaluate(expansionFractionProvider()).sizeT) }
    }

    val titleStyle = MaterialTheme.typography.headlineSmall.copy(
        fontWeight = FontWeight.ExtraBold,
        fontFamily = GoogleSansRounded,
        fontSize = 24.sp,
        lineHeight = 30.sp,
        letterSpacing = 0.sp,
        color = textColor
    )

    val artistStyle = MaterialTheme.typography.titleMedium.copy(
        fontFamily = GoogleSansRounded,
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.sp,
        color = artistTextColor
    )

    Column(
        horizontalAlignment = Alignment.Start,
        modifier = modifier.fillMaxWidth()
    ) {
        SharedPlayerMarqueeText(
            text = title,
            style = titleStyle,
            marqueeState = aodSourceBounds?.titleMarquee ?: fallbackTitleMarquee,
            shouldRunMarquee = shouldRunMarquee,
            shouldRunMarqueeProvider = shouldRunMarqueeProvider,
            morphFractionProvider = morphFractionProvider,
            miniAvailableWidthPxProvider = miniAvailableWidthPxProvider,
            fullAvailableWidthPxProvider = fullAvailableWidthPxProvider,
            localScaleProvider = titleScaleProvider,
            baseMiniScale = 0.625f,
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onClickTitle
                )
        )
        Spacer(modifier = Modifier.height(4.dp))

        SharedPlayerMarqueeText(
            text = artist,
            style = artistStyle,
            marqueeState = aodSourceBounds?.artistMarquee ?: fallbackArtistMarquee,
            shouldRunMarquee = shouldRunMarquee,
            shouldRunMarqueeProvider = shouldRunMarqueeProvider,
            morphFractionProvider = morphFractionProvider,
            miniAvailableWidthPxProvider = miniAvailableWidthPxProvider,
            fullAvailableWidthPxProvider = fullAvailableWidthPxProvider,
            localScaleProvider = artistScaleProvider,
            baseMiniScale = 13f / 15f,
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer {
                    val s = morphTracker.evaluate(expansionFractionProvider())
                    val artistScale = androidx.compose.ui.util.lerp(1.3866667f, 1f, s.sizeT)
                    transformOrigin = TransformOrigin(0f, 0f)
                    scaleX = artistScale
                    scaleY = artistScale
                    translationY = -artistSpacerPx * (1f - s.sizeT)
                }
                .combinedClickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {
                        if (isNavigatingToArtist) return@combinedClickable
                        coroutineScope.launch {
                            isNavigatingToArtist = true
                            try {
                                onClickArtist()
                            } finally {
                                isNavigatingToArtist = false
                            }
                        }
                    },
                    onLongClick = {
                        if (isNavigatingToArtist) return@combinedClickable
                        coroutineScope.launch {
                            isNavigatingToArtist = true
                            try {
                                playerViewModel.triggerArtistNavigationFromPlayer(
                                    artistIdStr = resolvedArtistId.toString(),
                                    fallbackArtistName = artist
                                )
                            } finally {
                                isNavigatingToArtist = false
                            }
                        }
                    }
                )
        )
    }
}

@Composable
private fun PlaceholderBox(
    modifier: Modifier,
    cornerRadius: Dp = 12.dp,
    color: Color
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(cornerRadius),
        color = color,
        tonalElevation = 0.dp
    ) {}
}

@Composable
private fun AlbumPlaceholder(
    height: Dp,
    color: Color,
    onColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(height),
        shape = RoundedCornerShape(18.dp),
        color = color,
        tonalElevation = 0.dp
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            Icon(
                modifier = Modifier.size(86.dp),
                painter = painterResource(R.drawable.pixelmusic_base_monochrome),
                contentDescription = null,
                tint = onColor
            )
        }
    }
}

@Composable
private fun MetadataPlaceholder(
    expansionFractionProvider: () -> Float,
    color: Color,
    onColor: Color,
    showQueueButtons: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 70.dp)
            .graphicsLayer {
                val expansionFraction = expansionFractionProvider().coerceIn(0f, 1f)
                alpha = expansionFraction.coerceIn(0f, 1f)
                translationY = (1f - expansionFraction.coerceIn(0f, 1f)) * 24f
            },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .align(Alignment.CenterVertically),
            verticalArrangement = Arrangement.spacedBy(6.dp) //2.dp
        ) {
            PlaceholderBox(
                modifier = Modifier
                    .fillMaxWidth(0.72f)
                    .height(27.dp), //30.dp
                cornerRadius = 8.dp,
                color = color
            )
            PlaceholderBox(
                modifier = Modifier
                    .fillMaxWidth(0.46f)
                    .height(17.dp), //20.dp
                cornerRadius = 8.dp,
                color = onColor
            )
        }

        if (showQueueButtons) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier.size(height = 42.dp, width = 50.dp),
                    shape = RoundedCornerShape(
                        topStart = 50.dp,
                        topEnd = 6.dp,
                        bottomStart = 50.dp,
                        bottomEnd = 6.dp
                    ),
                    color = onColor,
                    tonalElevation = 0.dp
                ) {}
                Surface(
                    modifier = Modifier.size(height = 42.dp, width = 50.dp),
                    shape = RoundedCornerShape(
                        topStart = 6.dp,
                        topEnd = 50.dp,
                        bottomStart = 6.dp,
                        bottomEnd = 50.dp
                    ),
                    color = onColor,
                    tonalElevation = 0.dp
                ) {}
            }
        } else {
            PlaceholderBox(
                modifier = Modifier.size(width = 48.dp, height = 48.dp),
                cornerRadius = 24.dp,
                color = onColor
            )
        }
    }
}

@Composable
private fun ProgressPlaceholder(
    color: Color,
    onColor: Color,
    showAudioMetaChip: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 70.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            PlaceholderBox(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp),
                cornerRadius = 3.dp,
                color = onColor.copy(alpha = 0.15f)
            )
            // Keep active segment in the layout tree but invisible to avoid visual noise.
            PlaceholderBox(
                modifier = Modifier
                    .fillMaxWidth(0.34f)
                    .height(6.dp)
                    .graphicsLayer { alpha = 0f },
                cornerRadius = 3.dp,
                color = color
            )
            // Keep thumb slot aligned but fully transparent.
            PlaceholderBox(
                modifier = Modifier
                    .padding(start = 92.dp)
                    .size(14.dp)
                    .graphicsLayer { alpha = 0f },
                cornerRadius = 7.dp,
                color = onColor
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 0.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.Center),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                PlaceholderBox(
                    modifier = Modifier
                        .width(34.dp)
                        .height(12.dp),
                    cornerRadius = 2.dp,
                    color = onColor
                )
                PlaceholderBox(
                    modifier = Modifier
                        .width(34.dp)
                        .height(12.dp),
                    cornerRadius = 2.dp,
                    color = onColor
                )
            }

            if (showAudioMetaChip) {
                PlaceholderBox(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .widthIn(min = 96.dp, max = 180.dp)
                        .height(18.dp),
                    cornerRadius = 999.dp,
                    color = onColor.copy(alpha = 0.15f)
                )
            }
        }
    }
}

@Composable
private fun ControlsPlaceholder(color: Color, onColor: Color) {
    val rowCorners = 60.dp

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .fillMaxWidth()
                .height(80.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                PlaceholderBox(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    cornerRadius = 60.dp,
                    color = onColor
                )
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    shape = RoundedCornerShape(26.dp),
                    color = color,
                    tonalElevation = 0.dp
                ) {}
                PlaceholderBox(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    cornerRadius = 60.dp,
                    color = onColor
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 66.dp, max = 86.dp)
            .padding(horizontal = 26.dp)
                .padding(bottom = 6.dp)
                .background(
                    color = onColor.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(rowCorners)
                ),
            contentAlignment = Alignment.Center
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(3) {
                    PlaceholderBox(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        cornerRadius = rowCorners,
                        color = onColor.copy(alpha = 0.1f)
                    )
                }
            }
        }
    }
}

private data class TransportButtonColors(
    val container: Color,
    val content: Color
)

private fun expressivePlayPauseButtonColors(colorScheme: ColorScheme): TransportButtonColors {
    return TransportButtonColors(
        container = colorScheme.tertiaryFixedDim,
        content = colorScheme.onTertiaryFixed
    )
}

private fun expressiveSkipButtonColors(colorScheme: ColorScheme): TransportButtonColors {
    return TransportButtonColors(
        container = colorScheme.secondaryFixedDim,
        content = colorScheme.onSecondaryFixed
    )
}

@Composable
private fun BottomToggleRow(
    modifier: Modifier,
    isShuffleEnabled: Boolean,
    isShuffleTransitionInProgress: Boolean,
    repeatMode: Int,
    isFavoriteProvider: () -> Boolean,
    onShuffleToggle: () -> Unit,
    onRepeatToggle: () -> Unit,
    onFavoriteToggle: () -> Unit,
    isFullScreenArt: Boolean = false
) {
    val isFavorite = isFavoriteProvider()
    val rowCorners = 60.dp
    val rowShape = RoundedCornerShape(rowCorners)
    val isDark = LocalPixelMusicDarkTheme.current

    val inactiveBg = LocalMaterialTheme.current.onSurface.copy(alpha = 0.07f)
    val inactiveContentColor = LocalMaterialTheme.current.onSurface

    val activeColor1 = LocalMaterialTheme.current.primary
    val activeContent1 = LocalMaterialTheme.current.onPrimary
    val activeColor2 = LocalMaterialTheme.current.secondary
    val activeContent2 = LocalMaterialTheme.current.onSecondary
    val activeColor3 = LocalMaterialTheme.current.tertiary
    val activeContent3 = LocalMaterialTheme.current.onTertiary

    val containerBg = if (isDark) {
        LocalMaterialTheme.current.surfaceContainer.copy(alpha = 0.5f)
    } else {
        Color.White.copy(alpha = 0.85f)
    }
    val containerModifier = modifier
        .clip(rowShape)
        .background(color = containerBg, shape = rowShape)

    Box(
        modifier = containerModifier
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(6.dp)
                .clip(
                    AbsoluteSmoothCornerShape(
                        cornerRadiusBL = rowCorners,
                        smoothnessAsPercentTR = 60,
                        cornerRadiusBR = rowCorners,
                        smoothnessAsPercentBL = 60,
                        cornerRadiusTL = rowCorners,
                        smoothnessAsPercentBR = 60,
                        cornerRadiusTR = rowCorners,
                        smoothnessAsPercentTL = 60
                    )
                )
                .background(Color.Transparent),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val commonModifier = Modifier.weight(1f)

            ToggleSegmentButton(
                modifier = commonModifier,
                active = isShuffleEnabled,
                enabled = !isShuffleTransitionInProgress,
                activeColor = activeColor1,
                activeCornerRadius = rowCorners,
                activeContentColor = activeContent1,
                inactiveColor = inactiveBg,
                inactiveContentColor = inactiveContentColor,
                onClick = onShuffleToggle,
                iconId = R.drawable.rounded_shuffle_24,
                contentDesc = "Aleatorio"
            )
            val repeatActive = repeatMode != Player.REPEAT_MODE_OFF
            val repeatIcon = when (repeatMode) {
                Player.REPEAT_MODE_ONE -> R.drawable.rounded_repeat_one_24
                Player.REPEAT_MODE_ALL -> R.drawable.rounded_repeat_24
                else -> R.drawable.rounded_repeat_24
            }
            ToggleSegmentButton(
                modifier = commonModifier,
                active = repeatActive,
                activeColor = activeColor2,
                activeCornerRadius = rowCorners,
                activeContentColor = activeContent2,
                inactiveColor = inactiveBg,
                inactiveContentColor = inactiveContentColor,
                onClick = onRepeatToggle,
                iconId = repeatIcon,
                contentDesc = "Repetir"
            )
            ToggleSegmentButton(
                modifier = commonModifier,
                active = isFavorite,
                activeColor = activeColor3,
                activeCornerRadius = rowCorners,
                activeContentColor = activeContent3,
                inactiveColor = inactiveBg,
                inactiveContentColor = inactiveContentColor,
                onClick = onFavoriteToggle,
                iconId = if (isFavorite) R.drawable.round_favorite_24 else R.drawable.rounded_favorite_24,
                contentDesc = "Favorito"
            )
        }
    }
}

@Composable
private fun SongActionRow(
    painter: Painter,
    title: String,
    subtitle: String? = null,
    iconTint: Color = LocalMaterialTheme.current.onSurfaceVariant,
    iconBgColor: Color = LocalMaterialTheme.current.surfaceVariant,
    trailingContent: (@Composable () -> Unit)? = null,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(iconBgColor),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painter,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(20.dp)
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontFamily = GoogleSansRounded,
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.bodyLarge,
                color = LocalMaterialTheme.current.onSurface
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    fontFamily = GoogleSansRounded,
                    fontWeight = FontWeight.Normal,
                    style = MaterialTheme.typography.bodySmall,
                    color = LocalMaterialTheme.current.onSurfaceVariant
                )
            }
        }
        if (trailingContent != null) {
            trailingContent()
        } else {
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(LocalMaterialTheme.current.surfaceContainerHigh),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                    contentDescription = null,
                    tint = LocalMaterialTheme.current.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
