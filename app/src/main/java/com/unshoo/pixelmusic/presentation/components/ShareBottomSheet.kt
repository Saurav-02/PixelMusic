@file:Suppress("EXPERIMENTAL_API_USAGE", "EXPERIMENTAL_API_USAGE_ERROR", "EXPERIMENTAL_IS_NOT_ENABLED")
package com.unshoo.pixelmusic.presentation.components

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
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
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
import com.unshoo.pixelmusic.R
import com.unshoo.pixelmusic.data.model.Song
import com.unshoo.pixelmusic.ui.theme.GoogleSansRounded
import com.unshoo.pixelmusic.ui.theme.DarkColorScheme
import com.unshoo.pixelmusic.ui.theme.LightColorScheme
import com.unshoo.pixelmusic.presentation.viewmodel.ThemeStateHolder
import com.unshoo.pixelmusic.presentation.viewmodel.ColorSchemePair
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

private const val GITHUB_LINK = "https://sauravbr.github.io/PixelMusic/"
private const val INSTAGRAM_PACKAGE = "com.instagram.android"

/**
 * Spotify-inspired dynamic background themes for the 9:16 shared card
 */
enum class ShareThemeStyle(val displayName: String) {
    DYNAMIC_PALETTE("Dynamic Player"),
    SOOTHING_GRADIENT("Gradient"),
    BLURRED_ARTWORK("Artwork Blur"),
    MIDNIGHT_MINIMAL("Midnight"),
    VIBRANT_GLOW("Vibrant Accent")
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

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.ui.ExperimentalComposeUiApi::class)
@Composable
fun ShareBottomSheet(
    song: Song,
    onDismiss: () -> Unit,
    onAddToPlaylist: () -> Unit,
    colorScheme: ColorScheme = MaterialTheme.colorScheme,
    lyricsLines: List<String> = emptyList()
) {
    val context = LocalContext.current
    val appContext = context.applicationContext
    val entryPoint = remember(appContext) {
        EntryPointAccessors.fromApplication(appContext, ShareBottomSheetEntryPoint::class.java)
    }
    val themeStateHolder = entryPoint.themeStateHolder()
    val albumColorSchemeState by themeStateHolder.getAlbumColorSchemeFlow(song.albumArtUriString.orEmpty()).collectAsState()

    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

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

    var activeThemeStyle by remember { mutableStateOf(ShareThemeStyle.DYNAMIC_PALETTE) }
    var isCapturing by remember { mutableStateOf(false) }
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

    fun captureAndShare(action: suspend (Bitmap) -> Unit) {
        isCapturing = true
        scope.launch {
            try {
                val bitmap = captureController.captureAsync().await().asAndroidBitmap()
                action(bitmap)
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to capture card", Toast.LENGTH_SHORT).show()
            } finally {
                isCapturing = false
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
    ModalBottomSheet(
        onDismissRequest = onDismiss,
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
                                .fillMaxWidth(0.70f)
                                .capturable(captureController),
                            song = song,
                            isLyricsMode = mode == 1,
                            selectedLyrics = lyricsList,
                            themeStyle = activeThemeStyle,
                            colorScheme = colorScheme,
                            cardShape = cardShape,
                            albumColorScheme = albumColorSchemeState,
                            useSolidLyricsCard = solidMode
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
                                    ShareThemeStyle.DYNAMIC_PALETTE -> {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .background(
                                                    brush = Brush.linearGradient(
                                                        colors = listOf(
                                                            colorScheme.primaryContainer,
                                                            colorScheme.secondaryContainer.copy(alpha = 0.6f),
                                                            colorScheme.surfaceContainerLow
                                                        )
                                                    )
                                                )
                                        )
                                    }
                                    ShareThemeStyle.BLURRED_ARTWORK -> {
                                        SmartImage(
                                            model = song.albumArtUriString,
                                            contentDescription = null,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                    ShareThemeStyle.SOOTHING_GRADIENT -> {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .background(
                                                    brush = Brush.linearGradient(
                                                        colors = listOf(
                                                            primaryColor.copy(alpha = 0.85f),
                                                            tertiaryColor.copy(alpha = 0.7f),
                                                            colorScheme.surfaceContainerHighest
                                                        )
                                                    )
                                                )
                                        )
                                    }
                                    ShareThemeStyle.MIDNIGHT_MINIMAL -> {
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
                                                            colors = listOf(primaryColor.copy(alpha = 0.6f), Color.Transparent)
                                                        )
                                                    )
                                            )
                                        }
                                    }
                                    ShareThemeStyle.VIBRANT_GLOW -> {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .background(
                                                    brush = Brush.linearGradient(
                                                        colors = listOf(
                                                            primaryColor,
                                                            secondaryColor,
                                                            tertiaryColor
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
                                    captureAndShare { bitmap ->
                                        val file = saveBitmapToCache(bitmap)
                                        val uri = FileProvider.getUriForFile(
                                            context,
                                            "${context.packageName}.fileprovider",
                                            file
                                        )
                                        val topColor = when (activeThemeStyle) {
                                            ShareThemeStyle.DYNAMIC_PALETTE -> colorScheme.primaryContainer
                                            ShareThemeStyle.SOOTHING_GRADIENT -> primaryColor
                                            ShareThemeStyle.BLURRED_ARTWORK -> primaryColor.copy(alpha = 0.5f)
                                            ShareThemeStyle.MIDNIGHT_MINIMAL -> Color(0xFF0A0A0A)
                                            ShareThemeStyle.VIBRANT_GLOW -> primaryColor
                                        }
                                        val bottomColor = when (activeThemeStyle) {
                                            ShareThemeStyle.DYNAMIC_PALETTE -> colorScheme.surfaceContainerLow
                                            ShareThemeStyle.SOOTHING_GRADIENT -> colorScheme.surfaceContainerHighest
                                            ShareThemeStyle.BLURRED_ARTWORK -> Color(0xFF141414)
                                            ShareThemeStyle.MIDNIGHT_MINIMAL -> Color(0xFF0A0A0A)
                                            ShareThemeStyle.VIBRANT_GLOW -> secondaryColor
                                        }
                                        shareToInstagramStory(
                                            context = context,
                                            imageUri = uri,
                                            topColorHex = topColor.toInstagramHex(),
                                            bottomColorHex = bottomColor.toInstagramHex()
                                        )
                                    }
                                }
                            )
                        }
                    }

                    item {
                        ShareActionChip(
                            icon = {
                                Icon(
                                    imageVector = Icons.Rounded.Download,
                                    contentDescription = null,
                                    modifier = Modifier.size(22.dp)
                                )
                            },
                            label = stringResource(R.string.share_action_download_card),
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            onClick = {
                                captureAndShare { bitmap ->
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
                                }
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
                        ShareActionChip(
                            icon = {
                                Icon(
                                    imageVector = Icons.Rounded.Share,
                                    contentDescription = null,
                                    modifier = Modifier.size(22.dp)
                                )
                            },
                            label = stringResource(R.string.share_action_more_apps),
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                            onClick = {
                                captureAndShare { bitmap ->
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
                                            "${song.title}\n🎵 $GITHUB_LINK"
                                        )
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(
                                        Intent.createChooser(shareIntent, context.getString(R.string.share_sheet_chooser_title))
                                    )
                                }
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
                .background(Color.Black.copy(alpha = 0.3f)),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
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
    useSolidLyricsCard: Boolean = false
) {
    val cardRatio = 9f / 16f
    val darkScheme = albumColorScheme?.dark ?: DarkColorScheme
    val lightScheme = albumColorScheme?.light ?: LightColorScheme

    val primaryColor = darkScheme.primary
    val secondaryColor = darkScheme.secondary
    val tertiaryColor = darkScheme.tertiary
    val surfaceContainerLow = darkScheme.surfaceContainerLow
    val surfaceContainerLowest = darkScheme.surfaceContainerLowest

    Box(
        modifier = modifier
            .aspectRatio(cardRatio)
            .shadow(elevation = 16.dp, shape = cardShape, clip = true)
            .clip(cardShape)
    ) {
        val primaryContainer = darkScheme.primaryContainer
        val secondaryContainer = darkScheme.secondaryContainer

        when (themeStyle) {
            ShareThemeStyle.DYNAMIC_PALETTE -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(primaryContainer, surfaceContainerLowest)
                            )
                        )
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                brush = Brush.radialGradient(
                                    colors = listOf(primaryColor.copy(alpha = 0.45f), Color.Transparent),
                                    radius = 700f
                                )
                            )
                    )
                }
            }
            ShareThemeStyle.SOOTHING_GRADIENT -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(primaryContainer, secondaryContainer, surfaceContainerLowest)
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
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.25f))
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        primaryContainer.copy(alpha = 0.65f),
                                        surfaceContainerLowest.copy(alpha = 0.85f)
                                    )
                                )
                            )
                    )
                }
            }
            ShareThemeStyle.MIDNIGHT_MINIMAL -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(surfaceContainerLowest)
                ) {
                    Box(
                        modifier = Modifier
                            .size(420.dp)
                            .align(Alignment.TopStart)
                            .offset(x = (-120).dp, y = (-60).dp)
                            .background(
                                brush = Brush.radialGradient(
                                    colors = listOf(primaryColor.copy(alpha = 0.18f), Color.Transparent)
                                )
                            )
                    )
                }
            }
            ShareThemeStyle.VIBRANT_GLOW -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(primaryContainer, surfaceContainerLowest)
                            )
                        )
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                brush = Brush.linearGradient(
                                    colors = listOf(
                                        primaryColor.copy(alpha = 0.38f),
                                        secondaryColor.copy(alpha = 0.28f),
                                        Color.Transparent
                                    )
                                )
                            )
                    )
                }
            }
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.45f)),
                        radius = 1200f
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.weight(1f))

            if (!isLyricsMode) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight()
                        .drawWithContent {
                            val centerOffset = Offset(size.width / 2f, size.height / 2f)
                            val bloomRadius = size.width * 0.55f
                            drawCircle(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        lightScheme.primaryContainer.copy(alpha = 0.22f),
                                        Color.Transparent
                                    ),
                                    center = centerOffset,
                                    radius = bloomRadius
                                ),
                                radius = bloomRadius,
                                center = centerOffset
                            )
                            drawContent()
                        },
                    contentAlignment = Alignment.Center
                ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth(0.88f)
                        .shadow(
                            elevation = 20.dp,
                            shape = RoundedCornerShape(12.dp),
                            ambientColor = primaryColor.copy(alpha = 0.4f),
                            spotColor = primaryColor.copy(alpha = 0.65f)
                        ),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = lightScheme.primaryContainer.copy(alpha = 0.9f)
                    ),
                    border = BorderStroke(1.2.dp, Color.White.copy(alpha = 0.45f))
                ) {
                    SongMiniCard(song = song, lightScheme = lightScheme)
                }
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

            Spacer(Modifier.height(12.dp))

            val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.Black.copy(alpha = 0.35f))
                        .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(16.dp))
                        .clickable {
                            try { uriHandler.openUri(GITHUB_LINK) } catch (e: Exception) { }
                        }
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Link,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(12.dp)
                    )
                    Text(
                        text = "Made with love by saurav",
                        fontFamily = GoogleSansRounded,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = Color.White
                    )
                    Icon(
                        imageVector = Icons.Rounded.ChevronRight,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.size(13.dp)
                    )
                }
                Text(
                    text = "sauravbr.github.io",
                    fontFamily = GoogleSansRounded,
                    fontWeight = FontWeight.Medium,
                    fontSize = 8.sp,
                    color = Color.White.copy(alpha = 0.38f),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun SongMiniCard(
    song: Song,
    lightScheme: ColorScheme
) {
    val formattedDuration = remember(song.duration) {
        val totalSecs = song.duration / 1000
        val mins = totalSecs / 60
        val secs = totalSecs % 60
        String.format("%02d:%02d", mins, secs)
    }
    val formattedProgress = remember(song.duration) {
        val progressSecs = (song.duration * 0.4f / 1000).toLong()
        val mins = progressSecs / 60
        val secs = progressSecs % 60
        String.format("%02d:%02d", mins, secs)
    }

    val density = LocalDensity.current
    val stroke = remember(density) {
        Stroke(width = with(density) { 3.dp.toPx() }, cap = StrokeCap.Round)
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.Start
    ) {
        SmartImage(
            model = song.albumArtUriString,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(
                    RoundedCornerShape(
                        topStart = 12.dp,
                        topEnd = 12.dp,
                        bottomStart = 0.dp,
                        bottomEnd = 0.dp
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp)
                .padding(top = 10.dp, bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = song.title,
                fontFamily = GoogleSansRounded,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                lineHeight = 18.sp,
                color = lightScheme.onPrimaryContainer,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = song.displayArtist,
                fontFamily = GoogleSansRounded,
                fontWeight = FontWeight.Medium,
                fontSize = 11.sp,
                lineHeight = 14.sp,
                color = lightScheme.onPrimaryContainer.copy(alpha = 0.55f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = formattedProgress,
                    fontFamily = GoogleSansRounded,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 8.sp,
                    color = lightScheme.onPrimaryContainer.copy(alpha = 0.6f)
                )
                LinearWavyProgressIndicator(
                    progress = { 0.4f },
                    modifier = Modifier
                        .weight(1f)
                        .height(12.dp),
                    color = lightScheme.primary,
                    trackColor = lightScheme.primary.copy(alpha = 0.22f),
                    stroke = stroke,
                    trackStroke = stroke,
                    wavelength = 12.dp,
                    amplitude = { 0.5f },
                    waveSpeed = 4.dp
                )
                Text(
                    text = formattedDuration,
                    fontFamily = GoogleSansRounded,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 8.sp,
                    color = lightScheme.onPrimaryContainer.copy(alpha = 0.6f)
                )
            }
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

private fun shareToInstagramStory(
    context: Context,
    imageUri: android.net.Uri,
    topColorHex: String? = null,
    bottomColorHex: String? = null
) {
    context.grantUriPermission(INSTAGRAM_PACKAGE, imageUri, Intent.FLAG_GRANT_READ_URI_PERMISSION)

    val intent = Intent("com.instagram.share.ADD_TO_STORY").apply {
        type = "image/png"
        putExtra("interactive_asset_uri", imageUri)
        putExtra("content_url", GITHUB_LINK)
        putExtra("source_application", "1703718787517231")
        
        if (topColorHex != null) {
            putExtra("top_background_color", topColorHex)
        }
        if (bottomColorHex != null) {
            putExtra("bottom_background_color", bottomColorHex)
        }
        `package` = INSTAGRAM_PACKAGE
        
        clipData = ClipData.newRawUri("", imageUri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    
    try {
        context.startActivity(intent)
    } catch (e: Exception) {
        val fallback = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, imageUri)
            `package` = INSTAGRAM_PACKAGE
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
}