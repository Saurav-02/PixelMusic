@file:Suppress("EXPERIMENTAL_API_USAGE", "EXPERIMENTAL_API_USAGE_ERROR", "EXPERIMENTAL_IS_NOT_ENABLED")
package com.saurav.pixelmusic.presentation.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.LocalOverscrollFactory
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.ui.graphics.toArgb
import com.saurav.pixelmusic.presentation.components.subcomps.SineWaveLine
import com.saurav.pixelmusic.ui.theme.ExpTitleTypography
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import dev.shreyaspatil.capturable.capturable
import dev.shreyaspatil.capturable.controller.rememberCaptureController
import coil.size.Size
import com.saurav.pixelmusic.R
import com.saurav.pixelmusic.data.model.Song
import com.saurav.pixelmusic.ui.theme.GoogleSansRounded
import com.saurav.pixelmusic.ui.theme.DarkColorScheme
import com.saurav.pixelmusic.ui.theme.LightColorScheme
import com.saurav.pixelmusic.presentation.viewmodel.ThemeStateHolder
import com.saurav.pixelmusic.presentation.viewmodel.ColorSchemePair
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LinearWavyProgressIndicator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import racra.compose.smooth_corner_rect_library.AbsoluteSmoothCornerShape
import java.io.File
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalDensity

private const val GITHUB_LINK = "https://pixelmusic.pages.dev"
private const val INSTAGRAM_PACKAGE = "com.instagram.android"

/**
 * Spotify-inspired dynamic background themes for the 9:16 shared card
 */
enum class ShareThemeStyle(val displayName: String) {
    EXPRESSIVE_LIGHT("Material Light"),
    EXPRESSIVE_DARK("Material Dark"),
    BLURRED_ARTWORK("Blurred Light"),
    ARTWORK_CLEAN("Full Artwork"),
    MIDNIGHT_AURA("Midnight Aura")
}

private data class ExpressiveShapeDef(
    val name: String,
    val radiusAt: (Double) -> Double
)

private val EXPRESSIVE_SHAPES = listOf(
    ExpressiveShapeDef("Circle") { 1.0 },
    ExpressiveShapeDef("Square") { th ->
        val c = abs(cos(th))
        val s = abs(sin(th))
        1.0 / (c.pow(4.0) + s.pow(4.0)).pow(0.25)
    },
    ExpressiveShapeDef("Slanted") { th ->
        val shifted = th - 0.22 * sin(2.0 * th)
        val c = abs(cos(shifted))
        val s = abs(sin(shifted))
        1.0 / (c.pow(3.8) + s.pow(3.8)).pow(0.26)
    },
    ExpressiveShapeDef("Arch") { th ->
        if (sin(th) < 0.0) 1.0 else {
            val c = abs(cos(th)).coerceAtLeast(0.01)
            val s = sin(th).coerceAtLeast(0.01)
            min(1.0 / c, 1.0 / s).coerceAtMost(1.18)
        }
    },
    ExpressiveShapeDef("Fan") { th ->
        if (cos(th) > -0.2 && sin(th) < 0.2) 1.05 else 0.72 + 0.28 * cos(th - PI / 4.0)
    },
    ExpressiveShapeDef("Arrow") { th ->
        val a = th + PI / 2.0
        0.88 + 0.32 * cos(a) - 0.16 * cos(2.0 * a)
    },
    ExpressiveShapeDef("Semicircle") { th ->
        if (sin(th) <= 0.0) 1.0 else 0.65 + 0.35 * (1.0 - sin(th))
    },
    ExpressiveShapeDef("Oval") { th ->
        1.0 / sqrt((cos(th) / 0.82).pow(2.0) + (sin(th) / 1.0).pow(2.0))
    },
    ExpressiveShapeDef("Pill") { th ->
        val c = abs(cos(th))
        val s = abs(sin(th))
        1.0 / ((c / 0.72).pow(5.5) + (s / 1.0).pow(5.5)).pow(1.0 / 5.5)
    },
    ExpressiveShapeDef("Triangle") { th ->
        0.84 + 0.26 * cos(3.0 * th + PI / 2.0)
    },
    ExpressiveShapeDef("Diamond") { th ->
        val c = abs(cos(th))
        val s = abs(sin(th))
        1.0 / (c.pow(1.4) + s.pow(1.4)).pow(1.0 / 1.4)
    },
    ExpressiveShapeDef("Hexagon") { th ->
        0.88 + 0.14 * cos(6.0 * th)
    },
    ExpressiveShapeDef("Pentagon") { th ->
        0.86 + 0.16 * cos(5.0 * th + PI / 2.0)
    },
    ExpressiveShapeDef("Gem") { th ->
        0.88 + 0.12 * cos(8.0 * th + PI / 8.0)
    },
    ExpressiveShapeDef("Very Sunny") { th ->
        0.88 + 0.12 * cos(16.0 * th)
    },
    ExpressiveShapeDef("Sunny") { th ->
        0.86 + 0.14 * cos(8.0 * th)
    },
    ExpressiveShapeDef("4-sided Cookie") { th ->
        0.85 + 0.15 * cos(4.0 * th)
    },
    ExpressiveShapeDef("6-sided Cookie") { th ->
        0.86 + 0.14 * cos(6.0 * th)
    },
    ExpressiveShapeDef("7-sided Cookie") { th ->
        0.87 + 0.13 * cos(7.0 * th)
    },
    ExpressiveShapeDef("9-sided Cookie") { th ->
        0.88 + 0.12 * cos(9.0 * th)
    },
    ExpressiveShapeDef("12-sided Cookie") { th ->
        0.90 + 0.10 * cos(12.0 * th)
    },
    ExpressiveShapeDef("Ghost-ish") { th ->
        if (sin(th) < 0.0) 1.0 else 0.84 + 0.16 * cos(8.0 * th)
    },
    ExpressiveShapeDef("4-leaf Clover") { th ->
        0.80 + 0.20 * abs(cos(2.0 * th))
    },
    ExpressiveShapeDef("8-leaf Clover") { th ->
        0.84 + 0.16 * abs(cos(4.0 * th))
    },
    ExpressiveShapeDef("Burst") { th ->
        0.80 + 0.20 * cos(10.0 * th)
    }
)

private class MorphingExpressiveShape(
    private val from: (Double) -> Double,
    private val to: (Double) -> Double,
    private val progress: Float,
    private val steps: Int = 120
) : Shape {
    override fun createOutline(
        size: androidx.compose.ui.geometry.Size,
        layoutDirection: androidx.compose.ui.unit.LayoutDirection,
        density: androidx.compose.ui.unit.Density
    ): Outline {
        val count = steps.coerceAtLeast(60)
        val twoPi = 2.0 * PI
        val clampedT = progress.coerceIn(0f, 1f).toDouble()

        val radii = DoubleArray(count)
        var maxR = 0.001

        for (i in 0 until count) {
            val theta = (twoPi * i) / count
            val rFrom = from(theta)
            val rTo = to(theta)
            val r = (1.0 - clampedT) * rFrom + clampedT * rTo
            radii[i] = r
            if (r > maxR) maxR = r
        }

        val baseRadius = min(size.width, size.height) * 0.485f
        val centerX = size.width * 0.5f
        val centerY = size.height * 0.5f

        val path = Path().apply {
            for (i in 0 until count) {
                val theta = (twoPi * i) / count
                val normalizedR = (baseRadius * (radii[i] / maxR)).toFloat()
                val x = centerX + (normalizedR * cos(theta)).toFloat()
                val y = centerY + (normalizedR * sin(theta)).toFloat()
                if (i == 0) moveTo(x, y) else lineTo(x, y)
            }
            close()
        }

        return Outline.Generic(path)
    }
}

/**
 * Utility to clean raw LRC lyric timestamps and metadata headers
 */
object LyricCleaner {
    fun clean(rawLyrics: String?): List<String> {
        if (rawLyrics.isNullOrBlank()) return emptyList()
        val noMeta = rawLyrics.replace(Regex("(?m)^\\[[a-zA-Z]+:.*\\]\\r?\\n?"), "")
        val noTimestamps = noMeta.replace(Regex("\\[\\d{2}:\\d{2}(?:\\.\\d{1,3})?\\]"), "")
        return noTimestamps.lines()
            .map { it.trim().replace("\"", "").replace("“", "").replace("”", "").trim() }
            .filter { it.isNotEmpty() && !it.startsWith("[") && !it.startsWith("(") }
    }
}

@OptIn(
    ExperimentalMaterial3Api::class,
    androidx.compose.ui.ExperimentalComposeUiApi::class,
    ExperimentalMaterial3ExpressiveApi::class
)
@Composable
fun ShareBottomSheet(
    song: Song,
    onDismiss: () -> Unit,
    onAddToPlaylist: () -> Unit,
    colorScheme: ColorScheme = MaterialTheme.colorScheme,
    lyricsLines: List<String> = emptyList(),
    currentPlaybackPositionMs: Long = 0L
) {
    val context = LocalContext.current
    val appContext = context.applicationContext
    val entryPoint = remember(appContext) {
        EntryPointAccessors.fromApplication(appContext, ShareBottomSheetEntryPoint::class.java)
    }
    val themeStateHolder = entryPoint.themeStateHolder()
    val userPreferencesRepository = remember(entryPoint) {
        entryPoint.userPreferencesRepository()
    }
    val albumColorSchemeState by themeStateHolder.getAlbumColorSchemeFlow(song.albumArtUriString.orEmpty()).collectAsState()

    val isVideoEngineEnabled by userPreferencesRepository.videoSharingEngineEnabledFlow.collectAsState(initial = false)
    val persistedFormat by userPreferencesRepository.shareCardFormatFlow.collectAsState(initial = com.saurav.pixelmusic.data.preferences.ShareCardFormat.PHOTO)
    var selectedFormat by remember { mutableStateOf(com.saurav.pixelmusic.data.preferences.ShareCardFormat.PHOTO) }

    LaunchedEffect(persistedFormat, isVideoEngineEnabled) {
        selectedFormat = if (isVideoEngineEnabled) persistedFormat else com.saurav.pixelmusic.data.preferences.ShareCardFormat.PHOTO
    }

    val scope = rememberCoroutineScope()
    val applicationScope = remember { kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.SupervisorJob() + kotlinx.coroutines.Dispatchers.Main) }
    val haptic = LocalHapticFeedback.current
    val sheetState = rememberBottomSheetState(
        initialValue = SheetValue.Hidden,
        enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded)
    )

    var selectedCardMode by remember { mutableStateOf(0) }
    var useSolidLyricsCard by remember { mutableStateOf(false) }

    val cleanedLyrics = remember(song.lyrics, lyricsLines) {
        lyricsLines.ifEmpty { LyricCleaner.clean(song.lyrics) }
    }
    val hasLyrics = remember(cleanedLyrics) { cleanedLyrics.isNotEmpty() }
    val selectedLyrics = remember { mutableStateListOf<String>() }

    LaunchedEffect(cleanedLinesInit@ cleanedLyrics) {
        if (selectedLyrics.isEmpty() && cleanedLyrics.isNotEmpty()) {
            selectedLyrics.addAll(cleanedLyrics.take(3))
        }
    }

    var activeThemeStyle by remember { mutableStateOf(ShareThemeStyle.EXPRESSIVE_LIGHT) }
    var currentShapeIndex by remember { mutableIntStateOf(0) }
    var previousShapeIndex by remember { mutableIntStateOf(0) }
    val shapeMorphAnim = remember { Animatable(1f) }
    val shapeCoroutineScope = rememberCoroutineScope()

    val onAlbumArtTap: () -> Unit = {
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        previousShapeIndex = currentShapeIndex
        currentShapeIndex = (currentShapeIndex + 1) % EXPRESSIVE_SHAPES.size
        shapeCoroutineScope.launch {
            shapeMorphAnim.snapTo(0f)
            shapeMorphAnim.animateTo(1f, tween(450, easing = FastOutSlowInEasing))
        }
    }

    var isCapturing by remember { mutableStateOf(false) }
    var isGeneratingVideo by remember { mutableStateOf(false) }
    var videoProgress by remember { mutableStateOf(0f) }
    var videoStatus by remember { mutableStateOf("Preparing 30s video…") }
    var videoJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }
    val captureController = rememberCaptureController()

    val instagramInstalled = remember { isPackageInstalled(context, INSTAGRAM_PACKAGE) }

    val primaryColor = colorScheme.primary
    val onPrimaryColor = colorScheme.onPrimary
    val primaryContainerColor = colorScheme.primaryContainer
    val onPrimaryContainerColor = colorScheme.onPrimaryContainer
    val secondaryColor = colorScheme.secondary
    val tertiaryColor = colorScheme.tertiary
    val cardShape = AbsoluteSmoothCornerShape(
        cornerRadiusTR = 24.dp, smoothnessAsPercentBR = 60,
        cornerRadiusBR = 24.dp, smoothnessAsPercentTL = 60,
        cornerRadiusTL = 24.dp, smoothnessAsPercentBL = 60,
        cornerRadiusBL = 24.dp, smoothnessAsPercentTR = 60
    )

    fun processAndShare(
        actionPhoto: suspend (Bitmap) -> Unit,
        actionVideo: suspend (Bitmap) -> Unit
    ) {
        if (selectedFormat == com.saurav.pixelmusic.data.preferences.ShareCardFormat.PHOTO) {
            isCapturing = true
            applicationScope.launch {
                try {
                    val bitmap = captureController.captureAsync().await().asAndroidBitmap()
                    actionPhoto(bitmap)
                } catch (e: kotlinx.coroutines.CancellationException) {
                    com.saurav.pixelmusic.utils.PixelLogger.i(com.saurav.pixelmusic.utils.PixelLogger.Category.UI, "ShareBottomSheet", "Photo share cancelled")
                } catch (e: Exception) {
                    com.saurav.pixelmusic.utils.PixelLogger.e(com.saurav.pixelmusic.utils.PixelLogger.Category.UI, "ShareBottomSheet", "Failed to capture card", e)
                    Toast.makeText(context, "Failed to capture card", Toast.LENGTH_SHORT).show()
                } finally {
                    isCapturing = false
                }
            }
        } else {
            isGeneratingVideo = true
            videoProgress = 0f
            videoStatus = "Preparing card artwork…"
            videoJob = applicationScope.launch {
                try {
                    val bitmap = captureController.captureAsync().await().asAndroidBitmap()
                    actionVideo(bitmap)
                } catch (e: kotlinx.coroutines.CancellationException) {
                    com.saurav.pixelmusic.utils.PixelLogger.i(com.saurav.pixelmusic.utils.PixelLogger.Category.UI, "ShareBottomSheet", "Video generation cancelled")
                } catch (e: Exception) {
                    val errorMsg = e.localizedMessage ?: "Unknown error"
                    com.saurav.pixelmusic.utils.PixelLogger.e(com.saurav.pixelmusic.utils.PixelLogger.Category.UI, "ShareBottomSheet", "Video generation failed: $errorMsg", e)
                    Toast.makeText(context, context.getString(R.string.share_video_failed, errorMsg), Toast.LENGTH_LONG).show()
                } finally {
                    isGeneratingVideo = false
                    videoJob = null
                }
            }
        }
    }

    suspend fun saveBitmapToCache(bitmap: Bitmap): File = withContext(Dispatchers.IO) {
        val cacheDir = File(context.cacheDir, "share_cards").also { it.mkdirs() }
        val file = File(cacheDir, "pixelmusic_share_${System.currentTimeMillis()}.png")
        file.outputStream().use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        file
    }

    val sheetShape = remember {
        AbsoluteSmoothCornerShape(
            cornerRadiusTR = 28.dp, smoothnessAsPercentBR = 60,
            cornerRadiusBR = 0.dp, smoothnessAsPercentTL = 60,
            cornerRadiusTL = 28.dp, smoothnessAsPercentBL = 60,
            cornerRadiusBL = 0.dp, smoothnessAsPercentTR = 60
        )
    }
    SmoothModalBottomSheet(
        onDismissRequest = { if (!isGeneratingVideo) onDismiss() },
        sheetState = sheetState,
        containerColor = colorScheme.surfaceContainer,
        shape = sheetShape,
        dragHandle = null,
        contentWindowInsets = { WindowInsets(0, 0, 0, 0) }
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = MaterialTheme.typography,
            shapes = MaterialTheme.shapes
        ) {
            CompositionLocalProvider(LocalOverscrollFactory provides null) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(sheetShape)
                    .background(colorScheme.surfaceContainer)
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp, bottom = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .width(40.dp)
                            .height(4.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f))
                    )
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.share_sheet_title),
                    fontFamily = GoogleSansRounded,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
                Text(
                    text = song.title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 20.dp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(12.dp))

                if (hasLyrics) {
                    Row(
                        modifier = Modifier
                            .padding(horizontal = 20.dp)
                            .fillMaxWidth()
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf(
                            stringResource(R.string.share_card_tab_song),
                            stringResource(R.string.share_card_tab_lyrics)
                        ).forEachIndexed { index, label ->
                            val isSelected = selectedCardMode == index
                            val bgColor by animateColorAsState(
                                targetValue = if (isSelected) primaryColor else Color.Transparent,
                                animationSpec = tween(250),
                                label = "tabColor$index"
                            )
                            val textColor by animateColorAsState(
                                targetValue = if (isSelected) onPrimaryColor else MaterialTheme.colorScheme.onSurfaceVariant,
                                animationSpec = tween(250),
                                label = "tabTextColor$index"
                            )
                            val tabScale by animateFloatAsState(
                                targetValue = if (isSelected) 1.03f else 1f,
                                label = "tabScale$index"
                            )
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(40.dp)
                                    .graphicsLayer {
                                        scaleX = tabScale
                                        scaleY = tabScale
                                    }
                                    .clip(CircleShape)
                                    .background(bgColor)
                                    .clickable {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        selectedCardMode = index
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    fontFamily = GoogleSansRounded,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = textColor,
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                }

                // Share Format Switcher (Photo / 30s Video)
                if (isVideoEngineEnabled && com.saurav.pixelmusic.utils.AndroidVersionCompat.supportsVideoSharing()) {
                    Row(
                        modifier = Modifier
                            .padding(horizontal = 20.dp)
                            .fillMaxWidth()
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf(
                            Pair(com.saurav.pixelmusic.data.preferences.ShareCardFormat.PHOTO, stringResource(R.string.share_format_photo)),
                            Pair(com.saurav.pixelmusic.data.preferences.ShareCardFormat.VIDEO, stringResource(R.string.share_format_video))
                        ).forEach { (format, label) ->
                            val isSelected = selectedFormat == format
                            val bgColor by animateColorAsState(
                                targetValue = if (isSelected) primaryColor else Color.Transparent,
                                animationSpec = tween(250),
                                label = "formatBg_${format.name}"
                            )
                            val contentColor by animateColorAsState(
                                targetValue = if (isSelected) onPrimaryColor else MaterialTheme.colorScheme.onSurfaceVariant,
                                animationSpec = tween(250),
                                label = "formatText_${format.name}"
                            )
                            val formatScale by animateFloatAsState(
                                targetValue = if (isSelected) 1.02f else 1f,
                                label = "formatScale_${format.name}"
                            )
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(38.dp)
                                    .graphicsLayer {
                                        scaleX = formatScale
                                        scaleY = formatScale
                                    }
                                    .clip(CircleShape)
                                    .background(bgColor)
                                    .clickable {
                                        if (selectedFormat != format) {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            selectedFormat = format
                                            scope.launch {
                                                userPreferencesRepository.setShareCardFormat(format)
                                            }
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = if (format == com.saurav.pixelmusic.data.preferences.ShareCardFormat.PHOTO) Icons.Rounded.Image else Icons.Rounded.Videocam,
                                        contentDescription = null,
                                        tint = contentColor,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = label,
                                        fontFamily = GoogleSansRounded,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = contentColor,
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                }

                if (selectedCardMode == 1) {
                    Row(
                        modifier = Modifier
                            .padding(horizontal = 20.dp)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Card Style: ",
                            fontFamily = GoogleSansRounded,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.width(8.dp))
                        FilterChip(
                            selected = !useSolidLyricsCard,
                            onClick = { useSolidLyricsCard = false },
                            label = { Text("Glass Panel", fontFamily = GoogleSansRounded) },
                            shape = CircleShape,
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = primaryColor,
                                selectedLabelColor = onPrimaryColor
                            )
                        )
                        Spacer(Modifier.width(8.dp))
                        FilterChip(
                            selected = useSolidLyricsCard,
                            onClick = { useSolidLyricsCard = true },
                            label = { Text("Solid Color", fontFamily = GoogleSansRounded) },
                            shape = CircleShape,
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = primaryColor,
                                selectedLabelColor = onPrimaryColor
                            )
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    AnimatedContent(
                        targetState = Triple(selectedCardMode, selectedLyrics.toList(), useSolidLyricsCard),
                        transitionSpec = {
                            fadeIn(tween(200)) togetherWith fadeOut(tween(150))
                        },
                        label = "cardPreview"
                    ) { (mode, lyricsList, solidMode) ->
                        ShareableCard(
                            modifier = Modifier
                                .fillMaxWidth(0.82f)
                                .capturable(captureController),
                            song = song,
                            isLyricsMode = mode == 1,
                            selectedLyrics = lyricsList,
                            themeStyle = activeThemeStyle,
                            colorScheme = colorScheme,
                            cardShape = cardShape,
                            albumColorScheme = albumColorSchemeState,
                            useSolidLyricsCard = solidMode,
                            isVideoMode = selectedFormat == com.saurav.pixelmusic.data.preferences.ShareCardFormat.VIDEO,
                            currentPositionMs = currentPlaybackPositionMs,
                            currentShapeIndex = currentShapeIndex,
                            previousShapeIndex = previousShapeIndex,
                            shapeMorphProgress = shapeMorphAnim.value,
                            onAlbumArtTap = onAlbumArtTap
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Card Theme",
                        fontFamily = GoogleSansRounded,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ShareThemeStyle.values().forEach { style ->
                            val isSelected = activeThemeStyle == style
                            val outlineColor = if (isSelected) primaryColor else Color.Transparent
                            val borderWidth = if (isSelected) 2.dp else 0.dp
                            val swatchScale by animateFloatAsState(
                                targetValue = if (isSelected) 1.15f else 1f,
                                label = "swatchScale_${style.name}"
                            )

                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .graphicsLayer {
                                        scaleX = swatchScale
                                        scaleY = swatchScale
                                    }
                                    .clip(CircleShape)
                                    .border(borderWidth, outlineColor, CircleShape)
                                    .padding(if (isSelected) 3.dp else 0.dp)
                                    .clip(CircleShape)
                                    .clickable {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        activeThemeStyle = style
                                    }
                            ) {
                                when (style) {
                                    ShareThemeStyle.EXPRESSIVE_LIGHT -> {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .background(
                                                    brush = Brush.linearGradient(
                                                        colors = listOf(
                                                            colorScheme.primaryContainer,
                                                            colorScheme.surfaceContainerHigh,
                                                            colorScheme.surfaceContainerLowest
                                                        )
                                                    )
                                                )
                                        )
                                    }
                                    ShareThemeStyle.EXPRESSIVE_DARK -> {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .background(
                                                    brush = Brush.linearGradient(
                                                        colors = listOf(
                                                            Color(0xFF2C2C2C),
                                                            Color(0xFF1E1E1E),
                                                            Color(0xFF121212)
                                                        )
                                                    )
                                                )
                                        )
                                    }
                                    ShareThemeStyle.BLURRED_ARTWORK -> {
                                        Box(modifier = Modifier.fillMaxSize()) {
                                            SmartImage(
                                                model = song.albumArtUriString,
                                                contentDescription = null,
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.fillMaxSize().blur(4.dp)
                                            )
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .background(Color.White.copy(alpha = 0.55f))
                                            )
                                        }
                                    }
                                    ShareThemeStyle.ARTWORK_CLEAN -> {
                                        SmartImage(
                                            model = song.albumArtUriString,
                                            contentDescription = null,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                    ShareThemeStyle.MIDNIGHT_AURA -> {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .background(Color(0xFF0C0C0C))
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(24.dp)
                                                    .align(Alignment.Center)
                                                    .background(
                                                        brush = Brush.radialGradient(
                                                            colors = listOf(
                                                                primaryColor.copy(alpha = 0.85f),
                                                                secondaryColor.copy(alpha = 0.45f),
                                                                Color.Transparent
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
                }
                Spacer(Modifier.height(12.dp))

                if (selectedCardMode == 1 && cleanedLyrics.isNotEmpty()) {
                    LyricLineSelector(
                        lines = cleanedLyrics,
                        selectedLines = selectedLyrics,
                        onToggleLine = { line ->
                            if (selectedLyrics.contains(line)) {
                                selectedLyrics.remove(line)
                            } else {
                                if (selectedLyrics.size < 5) {
                                    selectedLyrics.add(line)
                                } else {
                                    Toast.makeText(context, "Maximum 5 lines allowed", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        primaryColor = primaryColor,
                        haptic = haptic
                    )
                    Spacer(Modifier.height(16.dp))
                }

                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally)
                ) {
                    if (instagramInstalled) {
                        item {
                            ShareActionChip(
                                icon = {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_instagram),
                                        contentDescription = null,
                                        modifier = Modifier.size(22.dp),
                                        tint = Color.Unspecified
                                    )
                                },
                                label = stringResource(R.string.share_action_instagram),
                                containerColor = Color(0xFFE1306C).copy(alpha = 0.12f),
                                contentColor = Color(0xFFE1306C),
                                onClick = {
                                    processAndShare(
                                        actionPhoto = { bitmap ->
                                            val file = saveBitmapToCache(bitmap)
                                            val uri = FileProvider.getUriForFile(
                                                context,
                                                "${context.packageName}.fileprovider",
                                                file
                                            )
                                            val (topColor, bottomColor) = getGradientColors(activeThemeStyle, colorScheme, primaryColor, secondaryColor)
                                            shareToInstagramStory(
                                                context = context,
                                                mediaUri = uri,
                                                isVideo = false,
                                                topColorHex = topColor.toInstagramHex(),
                                                bottomColorHex = bottomColor.toInstagramHex()
                                            )
                                        },
                                        actionVideo = { bitmap ->
                                            val videoFile = com.saurav.pixelmusic.utils.ShareVideoGenerator.generateVideo(
                                                context = context,
                                                cardBitmap = bitmap,
                                                song = song,
                                                currentPositionMs = currentPlaybackPositionMs,
                                                desiredDurationSec = 30,
                                                onProgress = { p, s -> videoProgress = p; videoStatus = s }
                                            )
                                            val uri = FileProvider.getUriForFile(
                                                context,
                                                "${context.packageName}.fileprovider",
                                                videoFile
                                            )
                                            val (topColor, bottomColor) = getGradientColors(activeThemeStyle, colorScheme, primaryColor, secondaryColor)
                                            shareToInstagramStory(
                                                context = context,
                                                mediaUri = uri,
                                                isVideo = true,
                                                topColorHex = topColor.toInstagramHex(),
                                                bottomColorHex = bottomColor.toInstagramHex()
                                            )
                                        }
                                    )
                                }
                            )
                        }
                    }

                    item {
                        val isVideoMode = selectedFormat == com.saurav.pixelmusic.data.preferences.ShareCardFormat.VIDEO
                        ShareActionChip(
                            icon = {
                                Icon(
                                    imageVector = if (isVideoMode) Icons.Rounded.VideoFile else Icons.Rounded.Download,
                                    contentDescription = null,
                                    modifier = Modifier.size(22.dp)
                                )
                            },
                            label = if (isVideoMode) stringResource(R.string.share_action_download_video) else stringResource(R.string.share_action_download_card),
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            onClick = {
                                processAndShare(
                                    actionPhoto = { bitmap ->
                                        scope.launch {
                                            withContext(Dispatchers.IO) {
                                                try {
                                                    val resolver = context.contentResolver
                                                    val contentValues = android.content.ContentValues().apply {
                                                        put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, "PixelMusic_${song.title.take(20)}_${System.currentTimeMillis()}.png")
                                                        put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "image/png")
                                                        put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, android.os.Environment.DIRECTORY_PICTURES + "/PixelMusic")
                                                    }

                                                    val uri = resolver.insert(android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                                                        ?: throw Exception("Failed to create MediaStore entry")

                                                    resolver.openOutputStream(uri)?.use { out ->
                                                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                                                    }

                                                    withContext(Dispatchers.Main) {
                                                        Toast.makeText(context, context.getString(R.string.share_card_saved), Toast.LENGTH_SHORT).show()
                                                    }
                                                } catch (e: Exception) {
                                                    withContext(Dispatchers.Main) {
                                                        Toast.makeText(context, "Failed to save card", Toast.LENGTH_SHORT).show()
                                                    }
                                                }
                                            }
                                        }
                                    },
                                    actionVideo = { bitmap ->
                                        val videoFile = com.saurav.pixelmusic.utils.ShareVideoGenerator.generateVideo(
                                            context = context,
                                            cardBitmap = bitmap,
                                            song = song,
                                            currentPositionMs = currentPlaybackPositionMs,
                                            desiredDurationSec = 30,
                                            onProgress = { p, s -> videoProgress = p; videoStatus = s }
                                        )
                                        withContext(Dispatchers.IO) {
                                            try {
                                                val resolver = context.contentResolver
                                                val contentValues = android.content.ContentValues().apply {
                                                    put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, "PixelMusic_${song.title.take(20)}_${System.currentTimeMillis()}.mp4")
                                                    put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
                                                    put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, android.os.Environment.DIRECTORY_MOVIES + "/PixelMusic")
                                                }

                                                val uri = resolver.insert(android.provider.MediaStore.Video.Media.EXTERNAL_CONTENT_URI, contentValues)
                                                    ?: throw Exception("Failed to create MediaStore video entry")

                                                resolver.openOutputStream(uri)?.use { out ->
                                                    videoFile.inputStream().use { input ->
                                                        input.copyTo(out)
                                                    }
                                                }

                                                withContext(Dispatchers.Main) {
                                                    Toast.makeText(context, context.getString(R.string.share_video_saved), Toast.LENGTH_SHORT).show()
                                                }
                                            } catch (e: Exception) {
                                                withContext(Dispatchers.Main) {
                                                    Toast.makeText(context, "Failed to save video", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        }
                                    }
                                )
                            }
                        )
                    }

                    if (!song.youtubeId.isNullOrEmpty()) {
                        item {
                            ShareActionChip(
                                icon = {
                                    Icon(
                                        imageVector = Icons.Rounded.MusicNote,
                                        contentDescription = null,
                                        modifier = Modifier.size(22.dp)
                                    )
                                },
                                label = "YT Music Link",
                                containerColor = Color(0xFFFF0000).copy(alpha = 0.12f),
                                contentColor = Color(0xFFFF0000),
                                onClick = {
                                    val ytMusicLink = "https://music.youtube.com/watch?v=${song.youtubeId}"
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    clipboard.setPrimaryClip(ClipData.newPlainText("YouTube Music Link", ytMusicLink))
                                    Toast.makeText(context, "YouTube Music link copied", Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                    }

                    item {
                        val isVideoMode = selectedFormat == com.saurav.pixelmusic.data.preferences.ShareCardFormat.VIDEO
                        ShareActionChip(
                            icon = {
                                Icon(
                                    imageVector = Icons.Rounded.Share,
                                    contentDescription = null,
                                    modifier = Modifier.size(22.dp)
                                )
                            },
                            label = if (isVideoMode) stringResource(R.string.share_action_share_video) else stringResource(R.string.share_action_more_apps),
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                            onClick = {
                                processAndShare(
                                    actionPhoto = { bitmap ->
                                        val file = saveBitmapToCache(bitmap)
                                        val uri = FileProvider.getUriForFile(
                                            context,
                                            "${context.packageName}.fileprovider",
                                            file
                                        )
                                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                            type = "image/png"
                                            putExtra(Intent.EXTRA_STREAM, uri)
                                            putExtra(
                                                Intent.EXTRA_TEXT,
                                                "${song.title} • ${song.displayArtist}\n🎵 $GITHUB_LINK"
                                            )
                                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        }
                                        context.startActivity(
                                            Intent.createChooser(shareIntent, context.getString(R.string.share_sheet_chooser_title))
                                        )
                                    },
                                    actionVideo = { bitmap ->
                                        val videoFile = com.saurav.pixelmusic.utils.ShareVideoGenerator.generateVideo(
                                            context = context,
                                            cardBitmap = bitmap,
                                            song = song,
                                            currentPositionMs = currentPlaybackPositionMs,
                                            desiredDurationSec = 30,
                                            onProgress = { p, s -> videoProgress = p; videoStatus = s }
                                        )
                                        val uri = FileProvider.getUriForFile(
                                            context,
                                            "${context.packageName}.fileprovider",
                                            videoFile
                                        )
                                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                            type = "video/mp4"
                                            putExtra(Intent.EXTRA_STREAM, uri)
                                            putExtra(
                                                Intent.EXTRA_TEXT,
                                                "${song.title} • ${song.displayArtist}\n🎵 $GITHUB_LINK"
                                            )
                                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        }
                                        context.startActivity(
                                            Intent.createChooser(shareIntent, context.getString(R.string.share_sheet_chooser_title))
                                        )
                                    }
                                )
                            }
                        )
                    }
                }


                Spacer(modifier = Modifier.navigationBarsPadding())
            }
        }
    }
    }

    if (isCapturing) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.35f)),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }
    }

    if (isGeneratingVideo) {
        val videoSheetState = rememberBottomSheetState(
            initialValue = SheetValue.Hidden,
            enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded)
        )
        val videoSheetShape = remember {
            AbsoluteSmoothCornerShape(
                cornerRadiusTR = 28.dp, smoothnessAsPercentTR = 60,
                cornerRadiusTL = 28.dp, smoothnessAsPercentTL = 60,
                cornerRadiusBR = 0.dp, smoothnessAsPercentBR = 60,
                cornerRadiusBL = 0.dp, smoothnessAsPercentBL = 60
            )
        }
        SmoothModalBottomSheet(
            onDismissRequest = {
                videoJob?.cancel()
                isGeneratingVideo = false
            },
            sheetState = videoSheetState,
            containerColor = colorScheme.surfaceContainerLow,
            shape = videoSheetShape,
            dragHandle = { BottomSheetDefaults.DragHandle() },
            contentWindowInsets = { WindowInsets(0, 0, 0, 0) }
        ) {
            MaterialTheme(
                colorScheme = colorScheme,
                typography = MaterialTheme.typography,
                shapes = MaterialTheme.shapes
            ) {
                VideoGenerationProgressSheetContent(
                    song = song,
                    currentPlaybackPositionMs = currentPlaybackPositionMs,
                    videoProgress = videoProgress,
                    videoStatus = videoStatus,
                    primaryColor = primaryColor,
                    onCancel = {
                        videoJob?.cancel()
                        isGeneratingVideo = false
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun VideoGenerationProgressSheetContent(
    song: Song,
    currentPlaybackPositionMs: Long,
    videoProgress: Float,
    videoStatus: String,
    primaryColor: Color,
    onCancel: () -> Unit
) {
    val smoothDismiss = LocalSmoothDismiss.current
    val colors = MaterialTheme.colorScheme
    val animatedProgress by animateFloatAsState(
        targetValue = videoProgress.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 320, easing = FastOutSlowInEasing),
        label = "videoProgressAnim"
    )

    val clipWindowLabel = remember(song.duration, currentPlaybackPositionMs) {
        val desiredClipMs = 30_000L
        val totalMs = song.duration.coerceAtLeast(desiredClipMs)
        val clipLen = minOf(desiredClipMs, totalMs)
        val maxStart = (totalMs - clipLen).coerceAtLeast(0L)
        val startMs = if (currentPlaybackPositionMs > 0L) {
            currentPlaybackPositionMs.coerceIn(0L, maxStart)
        } else if (totalMs > clipLen * 2) {
            (totalMs * 0.25f).toLong().coerceIn(0L, maxStart)
        } else {
            0L
        }
        val endMs = startMs + clipLen
        fun fmt(ms: Long): String {
            val totalSec = (ms / 1000L).toInt().coerceAtLeast(0)
            return String.format(java.util.Locale.US, "%02d:%02d", totalSec / 60, totalSec % 60)
        }
        "${fmt(startMs)} – ${fmt(endMs)}"
    }

    val currentStageIndex = when {
        animatedProgress < 0.36f -> 0
        animatedProgress < 0.42f -> 1
        else -> 2
    }

    val topCardShape = remember {
        AbsoluteSmoothCornerShape(
            cornerRadiusTL = 22.dp, smoothnessAsPercentTL = 60,
            cornerRadiusTR = 22.dp, smoothnessAsPercentTR = 60,
            cornerRadiusBL = 8.dp, smoothnessAsPercentBL = 60,
            cornerRadiusBR = 8.dp, smoothnessAsPercentBR = 60
        )
    }
    val bottomCardShape = remember {
        AbsoluteSmoothCornerShape(
            cornerRadiusTL = 8.dp, smoothnessAsPercentTL = 60,
            cornerRadiusTR = 8.dp, smoothnessAsPercentTR = 60,
            cornerRadiusBL = 22.dp, smoothnessAsPercentBL = 60,
            cornerRadiusBR = 22.dp, smoothnessAsPercentBR = 60
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(top = 2.dp, bottom = 24.dp)
            .navigationBarsPadding(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header Row: Title on Left, Solid Tonal Percentage Pill on Right
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = stringResource(R.string.share_video_progress_title),
                    fontFamily = GoogleSansRounded,
                    fontWeight = FontWeight.ExtraBold,
                    style = ExpTitleTypography.headlineSmall,
                    color = colors.onSurface
                )
                Text(
                    text = "30s Story Clip • 720p MP4",
                    fontFamily = GoogleSansRounded,
                    fontWeight = FontWeight.Medium,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant
                )
            }

            Surface(
                shape = CircleShape,
                color = colors.primaryContainer
            ) {
                Text(
                    text = "${(animatedProgress * 100).toInt()}%",
                    fontFamily = GoogleSansRounded,
                    fontWeight = FontWeight.ExtraBold,
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.onPrimaryContainer,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        }

        // Connected Material 3 Expressive Segmented Cards (Zero Borders)
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Top Segment: Song Preview & 30s Clip Timestamp Window
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = topCardShape,
                color = colors.surfaceContainerHigh
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier.size(50.dp),
                        contentAlignment = Alignment.BottomEnd
                    ) {
                        SmartImage(
                            model = song.albumArtUriString,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(AbsoluteSmoothCornerShape(14.dp, 60))
                        )
                        Surface(
                            shape = CircleShape,
                            color = primaryColor,
                            modifier = Modifier.size(20.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Rounded.Videocam,
                                    contentDescription = null,
                                    tint = colors.onPrimary,
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }
                    }

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = song.title,
                            fontFamily = GoogleSansRounded,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleSmall,
                            color = colors.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = song.displayArtist.ifBlank { song.artist },
                            fontFamily = GoogleSansRounded,
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Surface(
                        shape = CircleShape,
                        color = colors.surfaceContainerHighest
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Schedule,
                                contentDescription = null,
                                tint = colors.onSurfaceVariant,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = clipWindowLabel,
                                fontFamily = GoogleSansRounded,
                                fontWeight = FontWeight.SemiBold,
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Bottom Segment: Live Pipeline Status, Single Wavy Progress Bar & Stage Pills
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = bottomCardShape,
                color = colors.surfaceContainerHigh
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = colors.secondaryContainer,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = when (currentStageIndex) {
                                        0 -> Icons.Rounded.GraphicEq
                                        1 -> Icons.Rounded.AutoAwesome
                                        else -> Icons.Rounded.MovieFilter
                                    },
                                    contentDescription = null,
                                    tint = colors.onSecondaryContainer,
                                    modifier = Modifier.size(17.dp)
                                )
                            }
                        }

                        AnimatedContent(
                            targetState = videoStatus,
                            transitionSpec = { fadeIn(tween(180)) togetherWith fadeOut(tween(150)) },
                            label = "videoStatusAnimation",
                            modifier = Modifier.weight(1f)
                        ) { status ->
                            Text(
                                text = status,
                                fontFamily = GoogleSansRounded,
                                fontWeight = FontWeight.SemiBold,
                                style = MaterialTheme.typography.bodyMedium,
                                color = colors.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    if (animatedProgress > 0.01f) {
                        LinearWavyProgressIndicator(
                            progress = { animatedProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(10.dp)
                                .clip(RoundedCornerShape(50)),
                            color = primaryColor,
                            trackColor = colors.surfaceContainerHighest
                        )
                    } else {
                        LinearWavyProgressIndicator(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(10.dp)
                                .clip(RoundedCornerShape(50)),
                            color = primaryColor,
                            trackColor = colors.surfaceContainerHighest
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(
                            "1. Audio Stream",
                            "2. Card Frame",
                            "3. MP4 Encode"
                        ).forEachIndexed { index, label ->
                            val isReached = currentStageIndex >= index
                            val isCurrent = currentStageIndex == index
                            val pillBg by animateColorAsState(
                                targetValue = when {
                                    isCurrent -> colors.primaryContainer
                                    isReached -> colors.secondaryContainer
                                    else -> colors.surfaceContainerHighest
                                },
                                animationSpec = tween(220),
                                label = "stagePillBg_$index"
                            )
                            val pillContent by animateColorAsState(
                                targetValue = when {
                                    isCurrent -> colors.onPrimaryContainer
                                    isReached -> colors.onSecondaryContainer
                                    else -> colors.onSurfaceVariant.copy(alpha = 0.75f)
                                },
                                animationSpec = tween(220),
                                label = "stagePillContent_$index"
                            )

                            Surface(
                                modifier = Modifier.weight(1f),
                                shape = CircleShape,
                                color = pillBg
                            ) {
                                Text(
                                    text = label,
                                    fontFamily = GoogleSansRounded,
                                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = pillContent,
                                    textAlign = TextAlign.Center,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Clean Tonal Cancel Button
        FilledTonalButton(
            onClick = {
                smoothDismiss {
                    onCancel()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = AbsoluteSmoothCornerShape(18.dp, 60),
            colors = ButtonDefaults.filledTonalButtonColors(
                containerColor = colors.surfaceContainerHighest,
                contentColor = colors.onSurface
            )
        ) {
            Icon(
                imageVector = Icons.Rounded.Close,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Cancel",
                fontFamily = GoogleSansRounded,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}

@Composable
private fun ShareableCard(
    modifier: Modifier = Modifier,
    song: Song,
    isLyricsMode: Boolean,
    selectedLyrics: List<String>,
    themeStyle: ShareThemeStyle,
    colorScheme: ColorScheme,
    cardShape: Shape,
    albumColorScheme: ColorSchemePair?,
    useSolidLyricsCard: Boolean = false,
    isVideoMode: Boolean = false,
    currentPositionMs: Long = 0L,
    currentShapeIndex: Int = 0,
    previousShapeIndex: Int = 0,
    shapeMorphProgress: Float = 1f,
    onAlbumArtTap: () -> Unit = {}
) {
    val cardRatio = 9f / 16f
    val darkScheme = albumColorScheme?.dark ?: DarkColorScheme
    val lightScheme = albumColorScheme?.light ?: LightColorScheme

    val primaryColor = darkScheme.primary
    val secondaryColor = darkScheme.secondary

    Box(
        modifier = modifier
            .aspectRatio(cardRatio)
            .shadow(elevation = 16.dp, shape = cardShape, clip = true)
            .clip(cardShape)
    ) {
        when (themeStyle) {
            ShareThemeStyle.EXPRESSIVE_LIGHT -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    lightScheme.surfaceContainerLowest,
                                    lightScheme.surfaceContainerLow
                                )
                            )
                        )
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                brush = Brush.radialGradient(
                                    colors = listOf(lightScheme.primaryContainer.copy(alpha = 0.45f), Color.Transparent),
                                    radius = 800f
                                )
                            )
                    )
                }
            }
            ShareThemeStyle.EXPRESSIVE_DARK -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    darkScheme.surfaceContainerLowest,
                                    darkScheme.surfaceContainerLow
                                )
                            )
                        )
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                brush = Brush.radialGradient(
                                    colors = listOf(darkScheme.primaryContainer.copy(alpha = 0.35f), Color.Transparent),
                                    radius = 800f
                                )
                            )
                    )
                }
            }
            ShareThemeStyle.BLURRED_ARTWORK -> {
                Box(modifier = Modifier.fillMaxSize()) {
                    SmartImage(
                        model = com.saurav.pixelmusic.data.remote.youtube.YoutubeHelper.getHighResThumbnailUrl(song.albumArtUriString) ?: song.albumArtUriString,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        targetSize = Size.ORIGINAL,
                        modifier = Modifier
                            .fillMaxSize()
                            .blur(24.dp)
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        Color.White.copy(alpha = 0.65f),
                                        Color.White.copy(alpha = 0.82f)
                                    )
                                )
                            )
                    )
                }
            }
            ShareThemeStyle.ARTWORK_CLEAN -> {
                Box(modifier = Modifier.fillMaxSize()) {
                    SmartImage(
                        model = com.saurav.pixelmusic.data.remote.youtube.YoutubeHelper.getHighResThumbnailUrl(song.albumArtUriString) ?: song.albumArtUriString,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        targetSize = Size.ORIGINAL,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                brush = Brush.verticalGradient(
                                    0.0f to Color.Transparent,
                                    0.45f to Color.Black.copy(alpha = 0.25f),
                                    0.75f to Color.Black.copy(alpha = 0.65f),
                                    1.0f to Color.Black.copy(alpha = 0.88f)
                                )
                            )
                    )
                }
            }
            ShareThemeStyle.MIDNIGHT_AURA -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF0A0A0A))
                ) {
                    Box(
                        modifier = Modifier
                            .size(500.dp)
                            .align(Alignment.Center)
                            .background(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        primaryColor.copy(alpha = 0.55f),
                                        secondaryColor.copy(alpha = 0.25f),
                                        Color.Transparent
                                    ),
                                    radius = 700f
                                )
                            )
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.weight(0.35f))

            if (!isLyricsMode) {
                val innerCardContainerColor = when (themeStyle) {
                    ShareThemeStyle.EXPRESSIVE_LIGHT -> lightScheme.surfaceContainerHigh
                    ShareThemeStyle.EXPRESSIVE_DARK -> darkScheme.surfaceContainerHigh
                    ShareThemeStyle.BLURRED_ARTWORK -> Color.White.copy(alpha = 0.90f)
                    ShareThemeStyle.ARTWORK_CLEAN -> Color.Black.copy(alpha = 0.44f)
                    ShareThemeStyle.MIDNIGHT_AURA -> darkScheme.surfaceContainerHigh.copy(alpha = 0.95f)
                }
                val innerCardBorder = when (themeStyle) {
                    ShareThemeStyle.EXPRESSIVE_LIGHT -> BorderStroke(1.dp, lightScheme.outlineVariant.copy(alpha = 0.45f))
                    ShareThemeStyle.EXPRESSIVE_DARK -> BorderStroke(1.dp, darkScheme.outlineVariant.copy(alpha = 0.35f))
                    ShareThemeStyle.BLURRED_ARTWORK -> BorderStroke(1.dp, Color.Black.copy(alpha = 0.08f))
                    ShareThemeStyle.ARTWORK_CLEAN -> BorderStroke(1.dp, Color.White.copy(alpha = 0.25f))
                    ShareThemeStyle.MIDNIGHT_AURA -> BorderStroke(1.dp, primaryColor.copy(alpha = 0.35f))
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth(0.88f)
                        .shadow(
                            elevation = 16.dp,
                            shape = AbsoluteSmoothCornerShape(26.dp, 60),
                            ambientColor = if (themeStyle == ShareThemeStyle.EXPRESSIVE_LIGHT) Color.Black.copy(alpha = 0.15f) else primaryColor.copy(alpha = 0.4f),
                            spotColor = if (themeStyle == ShareThemeStyle.EXPRESSIVE_LIGHT) Color.Black.copy(alpha = 0.25f) else primaryColor.copy(alpha = 0.65f)
                        ),
                    shape = AbsoluteSmoothCornerShape(26.dp, 60),
                    colors = CardDefaults.cardColors(
                        containerColor = innerCardContainerColor
                    ),
                    border = innerCardBorder
                ) {
                    SongMiniCard(
                        song = song,
                        cardTheme = themeStyle,
                        lightScheme = lightScheme,
                        darkScheme = darkScheme,
                        shapeMorphProgress = shapeMorphProgress,
                        previousShapeIndex = previousShapeIndex,
                        currentShapeIndex = currentShapeIndex,
                        onAlbumArtTap = onAlbumArtTap
                    )
                }
            } else {
                val containerColor = if (useSolidLyricsCard) {
                    lightScheme.primaryContainer.copy(alpha = 0.9f)
                } else {
                    Color.White.copy(alpha = 0.18f)
                }
                val borderColor = if (useSolidLyricsCard) {
                    Color.White.copy(alpha = 0.45f)
                } else {
                    Color.White.copy(alpha = 0.18f)
                }
                val borderStrokeWidth = if (useSolidLyricsCard) 1.2.dp else 1.dp

                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.92f)
                        .clip(RoundedCornerShape(18.dp))
                        .background(containerColor)
                        .border(
                            width = borderStrokeWidth,
                            color = borderColor,
                            shape = RoundedCornerShape(18.dp)
                        )
                        .padding(14.dp)
                ) {
                    LyricsGlassPanel(
                        song = song,
                        selectedLyrics = selectedLyrics,
                        isSolid = useSolidLyricsCard,
                        lightScheme = lightScheme
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current
            val isDarkTheme = themeStyle != ShareThemeStyle.EXPRESSIVE_LIGHT && themeStyle != ShareThemeStyle.BLURRED_ARTWORK
            val pillBg = if (isDarkTheme) Color.Black.copy(alpha = 0.42f) else Color.White.copy(alpha = 0.88f)
            val pillBorder = if (isDarkTheme) Color.White.copy(alpha = 0.16f) else Color.Black.copy(alpha = 0.10f)
            val pillText = if (isDarkTheme) Color.White else Color(0xFF1E1E1E)

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(pillBg)
                        .border(1.dp, pillBorder, RoundedCornerShape(16.dp))
                        .clickable {
                            try { uriHandler.openUri(GITHUB_LINK) } catch (e: Exception) { }
                        }
                        .padding(horizontal = 14.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.MusicNote,
                        contentDescription = null,
                        tint = pillText,
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = "PixelMusic",
                        fontFamily = GoogleSansRounded,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = pillText
                    )
                    Icon(
                        imageVector = Icons.Rounded.ChevronRight,
                        contentDescription = null,
                        tint = pillText.copy(alpha = 0.7f),
                        modifier = Modifier.size(13.dp)
                    )
                }
                Text(
                    text = "pixelmusic.pages.dev",
                    fontFamily = GoogleSansRounded,
                    fontWeight = FontWeight.Medium,
                    fontSize = 8.sp,
                    color = pillText.copy(alpha = 0.45f),
                    textAlign = TextAlign.Center
                )
            }

            Spacer(Modifier.weight(0.75f))
        }
    }
}

@Composable
private fun SongMiniCard(
    song: Song,
    cardTheme: ShareThemeStyle,
    lightScheme: ColorScheme,
    darkScheme: ColorScheme,
    shapeMorphProgress: Float,
    previousShapeIndex: Int,
    currentShapeIndex: Int,
    onAlbumArtTap: () -> Unit
) {
    val titleColor = when (cardTheme) {
        ShareThemeStyle.EXPRESSIVE_LIGHT -> lightScheme.onSurface
        ShareThemeStyle.EXPRESSIVE_DARK -> darkScheme.onSurface
        ShareThemeStyle.BLURRED_ARTWORK -> Color(0xFF1C1B1F)
        ShareThemeStyle.ARTWORK_CLEAN -> Color.White
        ShareThemeStyle.MIDNIGHT_AURA -> Color.White
    }
    val artistColor = when (cardTheme) {
        ShareThemeStyle.EXPRESSIVE_LIGHT -> lightScheme.onSurfaceVariant
        ShareThemeStyle.EXPRESSIVE_DARK -> darkScheme.onSurfaceVariant
        ShareThemeStyle.BLURRED_ARTWORK -> Color(0xFF49454F)
        ShareThemeStyle.ARTWORK_CLEAN -> Color.White.copy(alpha = 0.75f)
        ShareThemeStyle.MIDNIGHT_AURA -> Color.White.copy(alpha = 0.75f)
    }

    val currentShape = remember(previousShapeIndex, currentShapeIndex, shapeMorphProgress) {
        MorphingExpressiveShape(
            from = EXPRESSIVE_SHAPES[previousShapeIndex].radiusAt,
            to = EXPRESSIVE_SHAPES[currentShapeIndex].radiusAt,
            progress = shapeMorphProgress
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .aspectRatio(1f)
                .clip(currentShape)
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() },
                    onClick = onAlbumArtTap
                ),
            contentAlignment = Alignment.Center
        ) {
            SmartImage(
                model = com.saurav.pixelmusic.data.remote.youtube.YoutubeHelper.getHighResThumbnailUrl(song.albumArtUriString) ?: song.albumArtUriString,
                contentDescription = "Tap to change shape",
                contentScale = ContentScale.Crop,
                targetSize = Size.ORIGINAL,
                modifier = Modifier.fillMaxSize()
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(
                text = song.title,
                fontFamily = GoogleSansRounded,
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp,
                lineHeight = 21.sp,
                color = titleColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
            Text(
                text = song.displayArtist,
                fontFamily = GoogleSansRounded,
                fontWeight = FontWeight.Medium,
                fontSize = 12.sp,
                lineHeight = 15.sp,
                color = artistColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun LyricsGlassPanel(
    song: Song,
    selectedLyrics: List<String>,
    isSolid: Boolean = false,
    lightScheme: ColorScheme = LightColorScheme
) {
    val textColor = if (isSolid) lightScheme.onPrimaryContainer else Color.White
    val artistColor = if (isSolid) lightScheme.onPrimaryContainer.copy(alpha = 0.65f) else Color.White.copy(alpha = 0.65f)
    val dividerColor = if (isSolid) lightScheme.onPrimaryContainer.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.15f)
    val activeProgressColor = if (isSolid) lightScheme.onPrimaryContainer else Color.White
    val progressTrackColor = if (isSolid) lightScheme.onPrimaryContainer.copy(alpha = 0.22f) else Color.White.copy(alpha = 0.22f)

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            SmartImage(
                model = song.albumArtUriString,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(44.dp)
                    .shadow(4.dp, RoundedCornerShape(8.dp), clip = true)
                    .clip(RoundedCornerShape(8.dp))
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = song.title,
                    fontFamily = GoogleSansRounded,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    lineHeight = 17.sp,
                    color = textColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = song.displayArtist,
                    fontFamily = GoogleSansRounded,
                    fontWeight = FontWeight.Medium,
                    fontSize = 10.sp,
                    lineHeight = 13.sp,
                    color = artistColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Spacer(Modifier.height(12.dp))
        HorizontalDivider(
            color = dividerColor,
            thickness = 1.dp
        )
        Spacer(Modifier.height(14.dp))

        if (selectedLyrics.isEmpty()) {
            Text(
                text = "Select lyrics below to share...",
                fontFamily = GoogleSansRounded,
                fontWeight = FontWeight.Medium,
                fontSize = 13.sp,
                color = textColor.copy(alpha = 0.38f),
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 20.dp)
            )
        } else {
            val fontSize = when (selectedLyrics.size) {
                1    -> 22.sp
                2    -> 18.sp
                3    -> 15.sp
                4    -> 13.sp
                else -> 11.sp
            }
            val lineHeight = when (selectedLyrics.size) {
                1    -> 28.sp
                2    -> 24.sp
                3    -> 20.sp
                4    -> 17.sp
                else -> 15.sp
            }
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                selectedLyrics.forEach { line ->
                    Text(
                        text = line.replace("\"", "").replace("“", "").replace("”", "").trim(),
                        fontFamily = GoogleSansRounded,
                        fontWeight = FontWeight.Bold,
                        fontSize = fontSize,
                        lineHeight = lineHeight,
                        color = textColor,
                        textAlign = TextAlign.Start,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        Spacer(Modifier.height(18.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(3.dp)
                .clip(CircleShape)
                .background(progressTrackColor)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.42f)
                    .fillMaxHeight()
                    .background(activeProgressColor)
            )
        }
    }
}

@Composable
private fun LyricLineSelector(
    lines: List<String>,
    selectedLines: List<String>,
    onToggleLine: (String) -> Unit,
    primaryColor: Color,
    haptic: androidx.compose.ui.hapticfeedback.HapticFeedback
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
    ) {
        Text(
            text = "Tap to select lyrics (Max 5 lines)",
            fontFamily = GoogleSansRounded,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(vertical = 12.dp)
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(lines) { line ->
                    val isSelected = selectedLines.contains(line)
                    val bgSelectedColor = if (isSelected) primaryColor.copy(alpha = 0.15f) else Color.Transparent
                    val borderSelectedColor = if (isSelected) primaryColor else Color.Transparent
                    val textWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    val textColor = if (isSelected) primaryColor else MaterialTheme.colorScheme.onSurface

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(bgSelectedColor)
                            .border(1.dp, borderSelectedColor, RoundedCornerShape(10.dp))
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onToggleLine(line)
                            }
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = line,
                            fontFamily = GoogleSansRounded,
                            fontWeight = textWeight,
                            fontSize = 15.sp,
                            color = textColor,
                            modifier = Modifier.weight(1f)
                        )
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Rounded.CheckCircle,
                                contentDescription = "Selected",
                                tint = primaryColor,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ShareActionChip(
    icon: @Composable () -> Unit,
    label: String,
    containerColor: Color,
    contentColor: Color,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(containerColor)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            CompositionLocalProvider(LocalContentColor provides contentColor) {
                icon()
            }
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            maxLines = 2,
            modifier = Modifier.width(64.dp)
        )
    }
}

@Composable
private fun ShareListItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    ListItem(
        modifier = Modifier
            .padding(horizontal = 20.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        colors = ListItemDefaults.colors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.5f)
        ),
        headlineContent = {
            Text(
                text = title,
                fontFamily = GoogleSansRounded,
                fontWeight = FontWeight.Medium,
                style = MaterialTheme.typography.bodyLarge
            )
        },
        supportingContent = {
            Text(
                text = subtitle,
                fontFamily = GoogleSansRounded,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        leadingContent = {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    )
}

private fun isPackageInstalled(context: Context, packageName: String): Boolean {
    return try {
        context.packageManager.getPackageInfo(packageName, 0)
        true
    } catch (e: PackageManager.NameNotFoundException) {
        false
    }
}

private fun Color.toInstagramHex(): String {
    return String.format("#%06X", 0xFFFFFF and this.toArgb())
}

private fun getGradientColors(
    style: ShareThemeStyle,
    colorScheme: ColorScheme,
    primaryColor: Color,
    secondaryColor: Color
): Pair<Color, Color> {
    val topColor = when (style) {
        ShareThemeStyle.EXPRESSIVE_LIGHT -> colorScheme.primaryContainer
        ShareThemeStyle.EXPRESSIVE_DARK -> Color(0xFF1E1E1E)
        ShareThemeStyle.BLURRED_ARTWORK -> Color(0xFFF5F5F5)
        ShareThemeStyle.ARTWORK_CLEAN -> Color(0xFF141414)
        ShareThemeStyle.MIDNIGHT_AURA -> primaryColor
    }
    val bottomColor = when (style) {
        ShareThemeStyle.EXPRESSIVE_LIGHT -> colorScheme.surfaceContainerLow
        ShareThemeStyle.EXPRESSIVE_DARK -> Color(0xFF0F0F0F)
        ShareThemeStyle.BLURRED_ARTWORK -> Color(0xFFE8E8E8)
        ShareThemeStyle.ARTWORK_CLEAN -> Color(0xFF080808)
        ShareThemeStyle.MIDNIGHT_AURA -> Color(0xFF0A0A0A)
    }
    return Pair(topColor, bottomColor)
}

private fun shareToInstagramStory(
    context: Context,
    mediaUri: android.net.Uri,
    isVideo: Boolean = false,
    topColorHex: String? = null,
    bottomColorHex: String? = null
) {
    context.grantUriPermission(INSTAGRAM_PACKAGE, mediaUri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
    val mimeType = if (isVideo) "video/mp4" else "image/png"

    val intent = Intent("com.instagram.share.ADD_TO_STORY").apply {
        if (isVideo) {
            // Videos must be set as background asset via setDataAndType; interactive_asset_uri is only for image stickers
            setDataAndType(mediaUri, mimeType)
        } else {
            type = mimeType
            putExtra("interactive_asset_uri", mediaUri)
            if (topColorHex != null) {
                putExtra("top_background_color", topColorHex)
            }
            if (bottomColorHex != null) {
                putExtra("bottom_background_color", bottomColorHex)
            }
        }
        putExtra("content_url", GITHUB_LINK)
        putExtra("source_application", "1703718787517231")
        `package` = INSTAGRAM_PACKAGE
        
        clipData = ClipData.newRawUri("", mediaUri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    
    try {
        context.startActivity(intent)
    } catch (e: Exception) {
        val fallback = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, mediaUri)
            `package` = INSTAGRAM_PACKAGE
            clipData = ClipData.newRawUri("", mediaUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        try {
            context.startActivity(fallback)
        } catch (ex: Exception) {
            Toast.makeText(context, "Instagram not available", Toast.LENGTH_SHORT).show()
        }
    }
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface ShareBottomSheetEntryPoint {
    fun themeStateHolder(): ThemeStateHolder
    fun userPreferencesRepository(): com.saurav.pixelmusic.data.preferences.UserPreferencesRepository
}