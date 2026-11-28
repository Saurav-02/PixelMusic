package com.saurav.pixelmusic.presentation.components

import android.graphics.drawable.Animatable as DrawableAnimatable
import android.graphics.drawable.AnimatedImageDrawable
import android.os.Build
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.ColorUtils
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.request.ImageRequest
import com.saurav.pixelmusic.R
import com.saurav.pixelmusic.presentation.components.subcomps.MaterialYouVectorDrawable
import com.saurav.pixelmusic.ui.theme.LocalIsBlackAndWhiteTheme
import com.saurav.pixelmusic.utils.InAppUpdater
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import racra.compose.smooth_corner_rect_library.AbsoluteSmoothCornerShape
import java.util.Locale
import kotlin.math.roundToInt

private val MinimalSlowEasing = CubicBezierEasing(0.22f, 1.0f, 0.36f, 1.0f)
private val SoftGlideEasing = CubicBezierEasing(0.16f, 1.0f, 0.30f, 1.0f)

private fun Modifier.minimalAestheticReveal(progress: Float, slideDp: Float = 16f): Modifier {
    val p = progress.coerceIn(0f, 1f)
    val blurRadius = (1f - p) * 8f
    val blurMod = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && blurRadius > 0.3f) {
        Modifier.blur(blurRadius.dp)
    } else {
        Modifier
    }
    return this
        .then(blurMod)
        .graphicsLayer {
            alpha = p
            translationY = (1f - p) * slideDp.dp.toPx()
            val scale = 0.97f + (0.03f * p)
            scaleX = scale
            scaleY = scale
        }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun UpdateNotificationSheet(
    isUpdateAvailable: Boolean,
    versionName: String,
    changelog: String?,
    downloadUrl: String? = null,
    apkName: String? = null,
    apkSizeBytes: Long? = null,
    userName: String? = null,
    isTestBuild: Boolean = false,
    isDowngradeMode: Boolean = false,
    onDismiss: () -> Unit,
    onConfirmClick: () -> Unit = {},
    onMigrateClick: () -> Unit = {},
    onSnoozeClick: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val density = LocalDensity.current

    val sheetState = rememberBottomSheetState(
        initialValue = SheetValue.Hidden,
        enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded)
    )

    val downloadState by InAppUpdater.downloadState.collectAsStateWithLifecycle()
    val hasActiveDownload = downloadState !is InAppUpdater.GlobalDownloadState.Idle
    val effectiveUpdateMode = isUpdateAvailable || hasActiveDownload

    val resolvedDownloadUrl = remember(downloadUrl) {
        downloadUrl?.takeIf { it.isNotBlank() } ?: InAppUpdater.getResolvedDownloadUrl()
    }
    val deviceArch = remember { InAppUpdater.getDeviceArchitecture() }
    val cleanVersion = remember(versionName) {
        versionName.removePrefix("v").removePrefix("V").ifBlank { "Latest" }
    }
    val resolvedApkName = remember(apkName, resolvedDownloadUrl, versionName) {
        InAppUpdater.resolveApkFileName(
            downloadUrl = resolvedDownloadUrl,
            versionName = versionName,
            assetName = apkName ?: InAppUpdater.getResolvedApkName()
        )
    }
    val resolvedChangelog = remember(changelog) {
        changelog ?: InAppUpdater.getResolvedChangelog()
    }

    // Never auto-expand the What's New card when an update is available;
    // only expand when the user taps the soft dropdown pill
    var isChangelogExpanded by remember { mutableStateOf(false) }

    val isAlreadyDownloading = remember {
        downloadState is InAppUpdater.GlobalDownloadState.Downloading ||
            downloadState is InAppUpdater.GlobalDownloadState.Finished
    }

    val logoIntroScale = remember { Animatable(if (isAlreadyDownloading) 1f else 0.72f) }
    val logoIntroAlpha = remember { Animatable(if (isAlreadyDownloading) 1f else 0f) }
    val logoFlightProgress = remember { Animatable(if (isAlreadyDownloading) 1f else 0f) }

    val headerReveal = remember { Animatable(if (isAlreadyDownloading) 1f else 0f) }
    val pillsReveal = remember { Animatable(if (isAlreadyDownloading) 1f else 0f) }
    val apkCardReveal = remember { Animatable(if (isAlreadyDownloading) 1f else 0f) }
    val actionsReveal = remember { Animatable(if (isAlreadyDownloading) 1f else 0f) }

    LaunchedEffect(Unit) {
        if (!isAlreadyDownloading) {
            launch {
                logoIntroAlpha.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = 620, easing = SoftGlideEasing)
                )
            }
            launch {
                logoIntroScale.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = 720, easing = SoftGlideEasing)
                )
            }

            delay(560L)

            launch {
                logoFlightProgress.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = 1080, easing = MinimalSlowEasing)
                )
            }

            delay(540L)
            launch {
                headerReveal.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = 820, easing = MinimalSlowEasing)
                )
            }
            delay(95L)
            launch {
                pillsReveal.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = 840, easing = MinimalSlowEasing)
                )
            }
            delay(95L)
            launch {
                apkCardReveal.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = 860, easing = MinimalSlowEasing)
                )
            }
            delay(95L)
            launch {
                actionsReveal.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = 880, easing = MinimalSlowEasing)
                )
            }
        }
    }

    var sheetRootInWindow by remember { mutableStateOf(Offset.Zero) }
    var sheetWidthPx by remember { mutableFloatStateOf(0f) }
    var sheetHeightPx by remember { mutableFloatStateOf(0f) }
    var targetSlotCenterInRoot by remember { mutableStateOf<Offset?>(null) }

    val scrollState = rememberScrollState()

    SmoothModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        val smoothDismiss = LocalSmoothDismiss.current

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .onGloballyPositioned { coords ->
                    if (coords.isAttached) {
                        sheetRootInWindow = coords.positionInRoot()
                        sheetWidthPx = coords.size.width.toFloat()
                        sheetHeightPx = coords.size.height.toFloat()
                    }
                }
                .graphicsLayer {
                    compositingStrategy = CompositingStrategy.Offscreen
                }
                .drawWithContent {
                    drawContent()
                    val topFadeHeight = 28.dp.toPx().coerceAtMost(size.height * 0.15f)
                    val topStop = (topFadeHeight / size.height.coerceAtLeast(1f)).coerceIn(0f, 0.2f)
                    drawRect(
                        brush = Brush.verticalGradient(
                            0.0f to Color.Transparent,
                            topStop to Color.Black,
                            1.0f to Color.Black
                        ),
                        blendMode = BlendMode.DstIn
                    )
                }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState)
                    .padding(horizontal = 22.dp)
                    .padding(top = 14.dp, bottom = 22.dp)
                    .animateContentSize(
                        animationSpec = spring(
                            dampingRatio = 0.94f,
                            stiffness = Spring.StiffnessLow
                        )
                    )
            ) {
                // Top-Left Monochrome App Logo Slot
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .onGloballyPositioned { coords ->
                            if (coords.isAttached) {
                                val pos = coords.positionInRoot()
                                targetSlotCenterInRoot = Offset(
                                    x = pos.x + coords.size.width * 0.5f,
                                    y = pos.y + coords.size.height * 0.5f
                                )
                            }
                        }
                        .graphicsLayer {
                            alpha = if (logoFlightProgress.value >= 0.985f) 1f else 0f
                        }
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                painter = painterResource(R.drawable.pixelmusic_base_monochrome),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // 1. Greeting & Status Title ("Update available!" vs "Updated to latest version")
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .minimalAestheticReveal(headerReveal.value, slideDp = 14f)
                ) {
                    val greetingName = remember(userName) {
                        userName?.trim()?.takeIf { it.isNotEmpty() }?.lowercase(Locale.getDefault())
                    }
                    val headlineText = when {
                        isDowngradeMode -> "Downgrade to $cleanVersion"
                        effectiveUpdateMode && greetingName != null -> "Update available, $greetingName!"
                        effectiveUpdateMode -> "Update available!"
                        greetingName != null -> "Updated to latest version, $greetingName!"
                        else -> "Updated to latest version"
                    }

                    Text(
                        text = headlineText,
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontSize = 24.sp,
                            letterSpacing = (-0.3).sp
                        ),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = when {
                            isDowngradeMode -> "Install previous PixelMusic $cleanVersion over your current build"
                            effectiveUpdateMode -> "PixelMusic $cleanVersion is ready for your device"
                            else -> "You are running the latest PixelMusic $cleanVersion"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 2. Minimal, Soft, Aesthetic ABI & What's New Pills (No icons, no truncation, soft tonal fill)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .minimalAestheticReveal(pillsReveal.value, slideDp = 16f)
                ) {
                    val softPillShape = AbsoluteSmoothCornerShape(20.dp, 60)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Soft minimal ABI pill (no icon, soft surface fill)
                        Surface(
                            shape = softPillShape,
                            color = MaterialTheme.colorScheme.surfaceContainerHigh
                        ) {
                            Box(
                                modifier = Modifier
                                    .height(44.dp)
                                    .padding(horizontal = 18.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = deviceArch,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Soft minimal "What's new" dropdown pill (no sparkle icon, no truncation)
                        val arrowRotation by animateFloatAsState(
                            targetValue = if (isChangelogExpanded) 180f else 0f,
                            animationSpec = tween(durationMillis = 560, easing = MinimalSlowEasing),
                            label = "ChangelogChevronRotation"
                        )
                        val dropdownContainerColor by animateColorAsState(
                            targetValue = if (isChangelogExpanded) {
                                MaterialTheme.colorScheme.surfaceContainerHighest
                            } else {
                                MaterialTheme.colorScheme.surfaceContainerHigh
                            },
                            animationSpec = tween(durationMillis = 460, easing = MinimalSlowEasing),
                            label = "DropdownPillColor"
                        )

                        Surface(
                            shape = softPillShape,
                            color = dropdownContainerColor,
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .clip(softPillShape)
                                .clickable { isChangelogExpanded = !isChangelogExpanded }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 18.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "What's new",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1
                                )

                                Icon(
                                    imageVector = Icons.Rounded.KeyboardArrowDown,
                                    contentDescription = if (isChangelogExpanded) "Hide changelog" else "Show changelog",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier
                                        .size(20.dp)
                                        .graphicsLayer { rotationZ = arrowRotation }
                                )
                            }
                        }
                    }

                    // Expandable Changelog Card
                    AnimatedVisibility(
                        visible = isChangelogExpanded,
                        enter = expandVertically(
                            expandFrom = Alignment.Top,
                            animationSpec = tween(durationMillis = 640, easing = MinimalSlowEasing)
                        ) + fadeIn(
                            animationSpec = tween(durationMillis = 520, easing = SoftGlideEasing)
                        ),
                        exit = shrinkVertically(
                            shrinkTowards = Alignment.Top,
                            animationSpec = tween(durationMillis = 500, easing = MinimalSlowEasing)
                        ) + fadeOut(
                            animationSpec = tween(durationMillis = 340, easing = SoftGlideEasing)
                        )
                    ) {
                        Surface(
                            shape = AbsoluteSmoothCornerShape(24.dp, 60),
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 12.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(18.dp)
                            ) {
                                Text(
                                    text = "What changed on $cleanVersion",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(max = if (effectiveUpdateMode) 260.dp else 380.dp)
                                        .graphicsLayer {
                                            compositingStrategy = CompositingStrategy.Offscreen
                                        }
                                        .drawWithContent {
                                            drawContent()
                                            val fadeH = 18.dp.toPx().coerceAtMost(size.height * 0.15f)
                                            val stop = (fadeH / size.height.coerceAtLeast(1f)).coerceIn(0f, 0.2f)
                                            drawRect(
                                                brush = Brush.verticalGradient(
                                                    0f to Color.Transparent,
                                                    stop to Color.Black,
                                                    (1f - stop) to Color.Black,
                                                    1f to Color.Transparent
                                                ),
                                                blendMode = BlendMode.DstIn
                                            )
                                        }
                                        .verticalScroll(rememberScrollState())
                                        .padding(vertical = 4.dp)
                                ) {
                                    RichChangelogContent(changelog = resolvedChangelog)
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // 3. "To Be Installed -" APK Card (Only shown when an update is available or actively downloading)
                if (effectiveUpdateMode) {
                    val liveTotalBytes = when (val s = downloadState) {
                        is InAppUpdater.GlobalDownloadState.Downloading -> s.totalBytes
                        is InAppUpdater.GlobalDownloadState.Finished -> s.apkFile.length()
                        else -> 0L
                    }
                    val effectiveBytes = remember(liveTotalBytes, apkSizeBytes) {
                        when {
                            liveTotalBytes > 0L -> liveTotalBytes
                            apkSizeBytes != null && apkSizeBytes > 0L -> apkSizeBytes
                            else -> InAppUpdater.getResolvedApkSizeBytes() ?: 0L
                        }
                    }
                    val sizeLabel = remember(effectiveBytes) {
                        if (effectiveBytes > 0L) {
                            String.format(Locale.US, "%.1f MB", effectiveBytes / (1024f * 1024f))
                        } else {
                            "APK Package"
                        }
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .minimalAestheticReveal(apkCardReveal.value, slideDp = 18f)
                    ) {
                        Text(
                            text = "To Be Installed -",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp)
                        )

                        Surface(
                            shape = AbsoluteSmoothCornerShape(22.dp, 60),
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Surface(
                                    shape = AbsoluteSmoothCornerShape(14.dp, 60),
                                    color = MaterialTheme.colorScheme.surfaceContainerHighest,
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            painter = painterResource(R.drawable.pixelmusic_base_monochrome),
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.size(26.dp)
                                        )
                                    }
                                }

                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    Text(
                                        text = resolvedApkName,
                                        style = MaterialTheme.typography.labelLarge.copy(
                                            fontSize = 13.sp,
                                            lineHeight = 17.sp
                                        ),
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 2,
                                        softWrap = true
                                    )
                                    Text(
                                        text = "${context.packageName} · $sizeLabel",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))
                }

                // 4. Action Area:
                // - When effectiveUpdateMode is true: shows Download / Progress / Install, Migrate (if test), Open the web, Snooze
                // - When effectiveUpdateMode is false (already updated, viewing changelog): shows no download/web/migrate buttons, just a clean Done pill
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .minimalAestheticReveal(actionsReveal.value, slideDp = 20f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (effectiveUpdateMode) {
                        ExpressiveUpdateActionArea(
                            versionName = versionName,
                            downloadUrl = resolvedDownloadUrl,
                            apkName = resolvedApkName,
                            changelog = resolvedChangelog,
                            downloadState = downloadState,
                            isDowngradeMode = isDowngradeMode,
                            onFallbackConfirm = {
                                onConfirmClick()
                                smoothDismiss(null)
                            }
                        )

                        if (isTestBuild) {
                            val migrateShape = AbsoluteSmoothCornerShape(28.dp, 60)
                            Surface(
                                shape = migrateShape,
                                color = MaterialTheme.colorScheme.errorContainer,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp)
                                    .clip(migrateShape)
                                    .clickable {
                                        onMigrateClick()
                                        smoothDismiss(null)
                                    }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = "Migrate to Stable Release",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                }
                            }
                        }

                        val pillShape = AbsoluteSmoothCornerShape(28.dp, 60)

                        Surface(
                            shape = pillShape,
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .clip(pillShape)
                                .clickable {
                                    try {
                                        uriHandler.openUri("https://pixelmusic.pages.dev")
                                    } catch (_: Exception) {}
                                }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "Open the web",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        if (onSnoozeClick != null) {
                            Surface(
                                shape = pillShape,
                                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp)
                                    .clip(pillShape)
                                    .clickable {
                                        onSnoozeClick()
                                        smoothDismiss(null)
                                    }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = "no thanks remind me in 24 hour",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    } else {
                        val pillShape = AbsoluteSmoothCornerShape(28.dp, 60)
                        Surface(
                            shape = pillShape,
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp)
                                .clip(pillShape)
                                .clickable { smoothDismiss(null) }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "Close",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }

            // Minimal, Slow, Aesthetic Floating Monochrome Logo (Center -> Top-Left)
            if (logoFlightProgress.value < 0.985f) {
                val flight = logoFlightProgress.value
                val startSizePx = with(density) { 112.dp.toPx() }
                val endSizePx = with(density) { 64.dp.toPx() }
                val currentSizePx = startSizePx + (endSizePx - startSizePx) * flight
                val currentSizeDp = with(density) { currentSizePx.toDp() }

                val startCenterX = if (sheetWidthPx > 0f) sheetWidthPx * 0.5f else with(density) { 180.dp.toPx() }
                val startCenterY = if (sheetHeightPx > 0f) {
                    (sheetHeightPx * 0.34f).coerceAtLeast(with(density) { 130.dp.toPx() })
                } else {
                    with(density) { 150.dp.toPx() }
                }

                val targetLocalCenter = targetSlotCenterInRoot?.let { targetInRoot ->
                    Offset(
                        x = targetInRoot.x - sheetRootInWindow.x,
                        y = targetInRoot.y - sheetRootInWindow.y
                    )
                } ?: Offset(
                    x = with(density) { (22.dp + 32.dp).toPx() },
                    y = with(density) { (14.dp + 32.dp).toPx() }
                )

                val currentCenterX = startCenterX + (targetLocalCenter.x - startCenterX) * flight
                val currentCenterY = startCenterY + (targetLocalCenter.y - startCenterY) * flight

                val topLeftX = (currentCenterX - currentSizePx * 0.5f).roundToInt()
                val topLeftY = (currentCenterY - currentSizePx * 0.5f).roundToInt()

                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier
                        .offset { IntOffset(topLeftX, topLeftY) }
                        .size(currentSizeDp)
                        .graphicsLayer {
                            alpha = logoIntroAlpha.value
                            val s = logoIntroScale.value
                            scaleX = s
                            scaleY = s
                        }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        val iconSizeDp = with(density) { (currentSizePx * 0.5f).toDp() }
                        Icon(
                            painter = painterResource(R.drawable.pixelmusic_base_monochrome),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(iconSizeDp)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ExpressiveUpdateActionArea(
    versionName: String,
    downloadUrl: String?,
    apkName: String?,
    changelog: String?,
    downloadState: InAppUpdater.GlobalDownloadState,
    isDowngradeMode: Boolean = false,
    onFallbackConfirm: () -> Unit
) {
    val context = LocalContext.current
    val uiPhase = remember(downloadState) {
        when (downloadState) {
            is InAppUpdater.GlobalDownloadState.Idle -> "IDLE"
            is InAppUpdater.GlobalDownloadState.Downloading -> "DOWNLOADING"
            is InAppUpdater.GlobalDownloadState.Finished -> "FINISHED"
            is InAppUpdater.GlobalDownloadState.Error -> "ERROR"
        }
    }

    AnimatedContent(
        targetState = uiPhase,
        transitionSpec = {
            (fadeIn(tween(520, easing = MinimalSlowEasing)) +
                scaleIn(initialScale = 0.97f, animationSpec = tween(520, easing = MinimalSlowEasing)))
                .togetherWith(
                    fadeOut(tween(320, easing = SoftGlideEasing)) +
                        scaleOut(targetScale = 0.97f, animationSpec = tween(320, easing = SoftGlideEasing))
                )
        },
        label = "ExpressiveUpdateActionState"
    ) { phase ->
        when (phase) {
            "DOWNLOADING" -> {
                val state = downloadState as? InAppUpdater.GlobalDownloadState.Downloading
                val rawProgress = (state?.progress ?: 0f).coerceIn(0f, 1f)
                val isPaused = state?.isPaused ?: false
                val animatedProgress by animateFloatAsState(
                    targetValue = rawProgress,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioNoBouncy,
                        stiffness = Spring.StiffnessVeryLow
                    ),
                    label = "ExpressiveDownloadProgress"
                )

                val pillShape = AbsoluteSmoothCornerShape(28.dp, 60)
                val percentInt = (animatedProgress * 100f).roundToInt().coerceIn(0, 100)

                // Constant single fill color (never changes color when pausing/resuming)
                val constantFillColor = MaterialTheme.colorScheme.surfaceContainerHighest

                // Single horizontal line: [ Big Single-Color Fill Progress Pill ] [ Pause Circle ] [ Cancel Circle ]
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(58.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(pillShape)
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    ) {
                        val fillFraction = animatedProgress.coerceIn(0f, 1f)
                        if (fillFraction > 0.005f) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth(fraction = fillFraction)
                                    .background(constantFillColor)
                            )
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 20.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (isPaused) {
                                    "Paused · $percentInt%"
                                } else {
                                    "Downloading... $percentInt%"
                                },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // Pause / Resume Circle Button (Constant soft fill, smooth icon transition)
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        modifier = Modifier
                            .size(58.dp)
                            .clip(CircleShape)
                            .clickable {
                                if (isPaused) {
                                    InAppUpdater.resumeDownload(context.applicationContext)
                                } else {
                                    InAppUpdater.pauseDownload()
                                }
                            }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Crossfade(
                                targetState = isPaused,
                                animationSpec = tween(durationMillis = 280, easing = MinimalSlowEasing),
                                label = "PauseResumeIconCrossfade"
                            ) { paused ->
                                if (paused) {
                                    Icon(
                                        painter = painterResource(R.drawable.rounded_play_arrow_filled_24),
                                        contentDescription = "Resume",
                                        tint = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.size(22.dp)
                                    )
                                } else {
                                    Icon(
                                        painter = painterResource(R.drawable.rounded_pause_filled_24),
                                        contentDescription = "Pause",
                                        tint = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Cancel Circle Button
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.errorContainer,
                        modifier = Modifier
                            .size(58.dp)
                            .clip(CircleShape)
                            .clickable {
                                InAppUpdater.cancelDownload(context.applicationContext)
                            }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = "Cancel download",
                                tint = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
            }

            "FINISHED" -> {
                val state = downloadState as? InAppUpdater.GlobalDownloadState.Finished
                val pillShape = AbsoluteSmoothCornerShape(28.dp, 60)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(58.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            state?.apkFile?.let { file ->
                                InAppUpdater.installApk(context, file)
                            }
                        },
                        shape = pillShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.rounded_done_all_24),
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = if (isDowngradeMode) "Install Previous Version" else "Install Update",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.errorContainer,
                        modifier = Modifier
                            .size(58.dp)
                            .clip(CircleShape)
                            .clickable {
                                InAppUpdater.deleteApk(context.applicationContext)
                            }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                painter = painterResource(R.drawable.rounded_delete_outline_24),
                                contentDescription = "Delete downloaded APK",
                                tint = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
            }

            "ERROR" -> {
                val state = downloadState as? InAppUpdater.GlobalDownloadState.Error
                val pillShape = AbsoluteSmoothCornerShape(28.dp, 60)

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = AbsoluteSmoothCornerShape(18.dp, 60),
                        color = MaterialTheme.colorScheme.errorContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.rounded_error_outline_24),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = state?.message ?: "Download failed. Please try again.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }

                    Button(
                        onClick = {
                            if (!downloadUrl.isNullOrBlank()) {
                                InAppUpdater.startOrResumeDownload(
                                    context = context.applicationContext,
                                    url = downloadUrl,
                                    versionName = versionName,
                                    apkName = apkName,
                                    changelog = changelog
                                )
                            } else {
                                onFallbackConfirm()
                            }
                        },
                        shape = pillShape,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(58.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Retry Download",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            else -> {
                val isBwTheme = LocalIsBlackAndWhiteTheme.current
                val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
                val pillShape = AbsoluteSmoothCornerShape(28.dp, 60)

                val primaryButtonColor = when {
                    isBwTheme -> if (isDark) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.primary
                    isDark -> {
                        val hsl = FloatArray(3)
                        ColorUtils.colorToHSL(MaterialTheme.colorScheme.primary.toArgb(), hsl)
                        hsl[2] = (hsl[2] * 0.45f).coerceIn(0.22f, 0.40f)
                        Color(ColorUtils.HSLToColor(hsl))
                    }
                    else -> MaterialTheme.colorScheme.primary
                }
                val buttonContentColor = if (isBwTheme && !isDark) MaterialTheme.colorScheme.onPrimary else Color.White

                Button(
                    onClick = {
                        if (!downloadUrl.isNullOrBlank()) {
                            InAppUpdater.createNotificationChannel(context.applicationContext)
                            InAppUpdater.startOrResumeDownload(
                                context = context.applicationContext,
                                url = downloadUrl,
                                versionName = versionName,
                                apkName = apkName,
                                changelog = changelog
                            )
                        } else {
                            onFallbackConfirm()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(58.dp),
                    shape = pillShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = primaryButtonColor,
                        contentColor = buttonContentColor
                    ),
                    elevation = ButtonDefaults.buttonElevation()
                ) {
                    Icon(
                        painter = painterResource(R.drawable.rounded_download_24),
                        contentDescription = null,
                        tint = buttonContentColor,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = if (isDowngradeMode) "Download Previous Version" else "Download Update",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = buttonContentColor
                    )
                }
            }
        }
    }
}

@Composable
fun RichChangelogContent(
    changelog: String?,
    modifier: Modifier = Modifier
) {
    val items = remember(changelog) { parseChangelog(changelog) }

    if (items.isEmpty()) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .clip(RoundedCornerShape(18.dp))
            ) {
                MaterialYouVectorDrawable(
                    modifier = Modifier.fillMaxSize(),
                    drawableResId = R.drawable.welcome_art
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "This release focuses on bug fixes and performance polish.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
        return
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items.forEach { item ->
            when (item) {
                is ChangelogItem.Notice -> NoticeCard(item)
                is ChangelogItem.LinkButton -> LinkButtonView(item)
                is ChangelogItem.VersionHeader -> VersionHeaderView(item)
                is ChangelogItem.SectionHeader -> SectionHeaderView(item)
                is ChangelogItem.Bullet -> BulletItemView(item)
                is ChangelogItem.Image -> ImageItemView(item)
                is ChangelogItem.Paragraph -> ParagraphView(item)
                is ChangelogItem.Divider -> HorizontalDivider(
                    modifier = Modifier.padding(vertical = 6.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)
                )
            }
        }
    }
}

@Composable
private fun NoticeCard(notice: ChangelogItem.Notice) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val tertiaryColor = MaterialTheme.colorScheme.tertiary
    val codeBg = MaterialTheme.colorScheme.surfaceContainerHighest
    val highlightBg = MaterialTheme.colorScheme.primaryContainer

    Surface(
        shape = AbsoluteSmoothCornerShape(16.dp, 60),
        color = MaterialTheme.colorScheme.errorContainer,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.error.copy(alpha = 0.2f),
                    modifier = Modifier.size(28.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(text = notice.icon, fontSize = 14.sp)
                    }
                }
                Text(
                    text = notice.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            notice.lines.forEach { line ->
                Text(
                    text = parseRichMarkdown(line, primaryColor, tertiaryColor, codeBg, highlightBg),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.padding(vertical = 2.dp)
                )
            }
        }
    }
}

@Composable
private fun VersionHeaderView(header: ChangelogItem.VersionHeader) {
    val (containerColor, contentColor, icon) = when (header.badgeColorType) {
        BadgeType.PRIMARY -> Triple(
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.onPrimaryContainer,
            "🌟"
        )
        BadgeType.SECONDARY -> Triple(
            MaterialTheme.colorScheme.secondaryContainer,
            MaterialTheme.colorScheme.onSecondaryContainer,
            "🚀"
        )
        BadgeType.TERTIARY -> Triple(
            MaterialTheme.colorScheme.tertiaryContainer,
            MaterialTheme.colorScheme.onTertiaryContainer,
            "🧪"
        )
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Surface(
            shape = CircleShape,
            color = containerColor
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(text = icon, fontSize = 14.sp)
                Text(
                    text = header.cleanName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = contentColor
                )
            }
        }
    }
}

@Composable
private fun LinkButtonView(button: ChangelogItem.LinkButton) {
    val uriHandler = LocalUriHandler.current
    val buttonShape = AbsoluteSmoothCornerShape(18.dp, 60)

    Button(
        onClick = {
            try {
                uriHandler.openUri(button.url)
            } catch (_: Exception) {}
        },
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp),
        shape = buttonShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
        ),
        elevation = ButtonDefaults.buttonElevation(0.dp, 0.dp, 0.dp, 0.dp, 0.dp),
        contentPadding = PaddingValues(horizontal = 16.dp)
    ) {
        Icon(
            painter = painterResource(R.drawable.rounded_open_in_new_24),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = button.title,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun SectionHeaderView(header: ChangelogItem.SectionHeader) {
    Text(
        text = header.title,
        style = if (header.level <= 2) MaterialTheme.typography.titleMedium else MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.padding(top = 6.dp, bottom = 2.dp)
    )
}

@Composable
private fun BulletItemView(bullet: ChangelogItem.Bullet) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val tertiaryColor = MaterialTheme.colorScheme.tertiary
    val codeBg = MaterialTheme.colorScheme.surfaceContainerHighest
    val highlightBg = MaterialTheme.colorScheme.primaryContainer

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (!bullet.emoji.isNullOrBlank()) {
            Text(
                text = bullet.emoji,
                fontSize = 15.sp,
                modifier = Modifier.padding(top = 1.dp)
            )
        } else {
            Box(
                modifier = Modifier
                    .padding(top = 8.dp, start = 2.dp, end = 2.dp)
                    .size(6.dp)
                    .background(
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        shape = CircleShape
                    )
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            if (!bullet.title.isNullOrBlank()) {
                Text(
                    text = parseRichMarkdown(bullet.title, primaryColor, tertiaryColor, codeBg, highlightBg),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            if (bullet.body.isNotBlank() && bullet.body != bullet.title) {
                Text(
                    text = parseRichMarkdown(bullet.body, primaryColor, tertiaryColor, codeBg, highlightBg),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (bullet.tags.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val uriHandler = LocalUriHandler.current
                    bullet.tags.forEach { contributor ->
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            modifier = Modifier.clickable {
                                uriHandler.openUri("https://t.me/$contributor")
                            }
                        ) {
                            Text(
                                text = "@$contributor",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

fun normalizeMediaUrl(rawUrl: String): String {
    var trimmed = rawUrl.trim()
    val ghRegex = Regex("""^https?://github\.com/([^/]+)/([^/]+)/(?:blob|raw)/([^/]+)/(.*)""")
    val ghMatch = ghRegex.find(trimmed)
    if (ghMatch != null) {
        val owner = ghMatch.groupValues[1]
        val repo = ghMatch.groupValues[2]
        val branch = ghMatch.groupValues[3]
        val path = ghMatch.groupValues[4]
        trimmed = "https://raw.githubusercontent.com/$owner/$repo/$branch/$path"
    }
    return trimmed
}

@Composable
private fun ImageItemView(img: ChangelogItem.Image) {
    val context = LocalContext.current
    val normalizedUrl = remember(img.url) { normalizeMediaUrl(img.url) }
    var isLoading by remember { mutableStateOf(true) }
    var isError by remember { mutableStateOf(false) }

    val gifImageLoader = remember(context) {
        coil.ImageLoader.Builder(context)
            .components {
                if (Build.VERSION.SDK_INT >= 28) {
                    add(ImageDecoderDecoder.Factory())
                } else {
                    add(GifDecoder.Factory())
                }
            }
            .respectCacheHeaders(false)
            .build()
    }

    val imageRequest = remember(normalizedUrl, context) {
        ImageRequest.Builder(context)
            .data(normalizedUrl)
            .apply {
                if (Build.VERSION.SDK_INT >= 28) {
                    decoderFactory(ImageDecoderDecoder.Factory())
                } else {
                    decoderFactory(GifDecoder.Factory())
                }
            }
            .crossfade(true)
            .build()
    }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(16.dp))
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 100.dp, max = 300.dp),
                contentAlignment = Alignment.Center
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(28.dp),
                        strokeWidth = 3.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                if (isError) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(20.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Could not load preview",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }

                AsyncImage(
                    model = imageRequest,
                    imageLoader = gifImageLoader,
                    contentDescription = img.alt ?: "Changelog Preview",
                    onLoading = {
                        isLoading = true
                        isError = false
                    },
                    onSuccess = { state ->
                        isLoading = false
                        isError = false
                        val drawable = state.result.drawable
                        if (Build.VERSION.SDK_INT >= 28 && drawable is AnimatedImageDrawable) {
                            drawable.repeatCount = AnimatedImageDrawable.REPEAT_INFINITE
                            drawable.start()
                        } else if (drawable is DrawableAnimatable) {
                            drawable.start()
                        }
                    },
                    onError = {
                        isLoading = false
                        isError = true
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 100.dp, max = 300.dp),
                    contentScale = ContentScale.FillWidth
                )
            }

            if (!img.alt.isNullOrBlank()) {
                Text(
                    text = img.alt,
                    style = MaterialTheme.typography.labelSmall,
                    fontStyle = FontStyle.Italic,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun ParagraphView(para: ChangelogItem.Paragraph) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val tertiaryColor = MaterialTheme.colorScheme.tertiary
    val codeBg = MaterialTheme.colorScheme.surfaceContainerHighest
    val highlightBg = MaterialTheme.colorScheme.primaryContainer

    Text(
        text = parseRichMarkdown(para.text, primaryColor, tertiaryColor, codeBg, highlightBg),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(vertical = 2.dp)
    )
}

private fun parseRichMarkdown(
    text: String,
    primaryColor: Color,
    tertiaryColor: Color,
    codeBg: Color,
    highlightBg: Color
): AnnotatedString = buildAnnotatedString {
    val pattern = Regex(
        """\[([^\]]+)\]\((https?://[^\s)]+)\)|\[?@([a-zA-Z0-9_-]+)\]?|\*\*([^*]+)\*\*|`([^`]+)`|==([^=]+)==|\*([^*]+)\*"""
    )
    var currentIndex = 0
    pattern.findAll(text).forEach { match ->
        if (match.range.first > currentIndex) {
            append(text.substring(currentIndex, match.range.first))
        }
        val groups = match.groups
        when {
            groups[1] != null && groups[2] != null -> {
                val label = groups[1]!!.value
                val url = groups[2]!!.value
                withLink(
                    LinkAnnotation.Url(
                        url = url,
                        styles = TextLinkStyles(
                            style = SpanStyle(
                                color = primaryColor,
                                textDecoration = TextDecoration.Underline,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                    )
                ) {
                    append(label)
                }
            }
            groups[3] != null -> {
                val user = groups[3]!!.value
                withLink(
                    LinkAnnotation.Url(
                        url = "https://t.me/$user",
                        styles = TextLinkStyles(
                            style = SpanStyle(
                                color = tertiaryColor,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    )
                ) {
                    append("@$user")
                }
            }
            groups[4] != null -> {
                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                    append(groups[4]!!.value)
                }
            }
            groups[5] != null -> {
                withStyle(
                    SpanStyle(
                        fontFamily = FontFamily.Monospace,
                        background = codeBg,
                        color = primaryColor,
                        fontWeight = FontWeight.Medium
                    )
                ) {
                    append(" ${groups[5]!!.value} ")
                }
            }
            groups[6] != null -> {
                withStyle(
                    SpanStyle(
                        background = highlightBg,
                        color = primaryColor,
                        fontWeight = FontWeight.SemiBold
                    )
                ) {
                    append(" ${groups[6]!!.value} ")
                }
            }
            groups[7] != null -> {
                withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                    append(groups[7]!!.value)
                }
            }
        }
        currentIndex = match.range.last + 1
    }
    if (currentIndex < text.length) {
        append(text.substring(currentIndex))
    }
}

private enum class BadgeType {
    PRIMARY,
    SECONDARY,
    TERTIARY
}

private sealed class ChangelogItem {
    data class Notice(val icon: String, val title: String, val lines: List<String>) : ChangelogItem()
    data class LinkButton(
        val title: String,
        val url: String,
        val subtitle: String? = null,
        val icon: String = "🚀"
    ) : ChangelogItem()
    data class VersionHeader(val raw: String, val cleanName: String, val badgeColorType: BadgeType) : ChangelogItem()
    data class SectionHeader(val title: String, val level: Int) : ChangelogItem()
    data class Bullet(
        val emoji: String?,
        val title: String?,
        val body: String,
        val tags: List<String>
    ) : ChangelogItem()
    data class Image(val url: String, val alt: String?) : ChangelogItem()
    data class Paragraph(val text: String) : ChangelogItem()
    object Divider : ChangelogItem()
}

private fun parseChangelog(rawText: String?): List<ChangelogItem> {
    if (rawText.isNullOrBlank()) return emptyList()
    val items = mutableListOf<ChangelogItem>()
    val lines = rawText.lines()
    var i = 0

    while (i < lines.size) {
        val line = lines[i].trim()

        if (line.isEmpty()) {
            i++
            continue
        }

        if (line.startsWith("⚠️") || line.startsWith("🚨") || line.startsWith("💡") ||
            line.startsWith("📌") || line.contains("IMPORTANT NOTICE", ignoreCase = true) ||
            line.contains("NOTICE:", ignoreCase = true) || line.startsWith("[notice]", ignoreCase = true)
        ) {
            val icon = when {
                line.startsWith("🚨") -> "🚨"
                line.startsWith("💡") -> "💡"
                line.startsWith("📌") -> "📌"
                else -> "⚠️"
            }
            val titleClean = line.replace(Regex("^[⚠️🚨💡📌\\s*#]+"), "")
                .replace(Regex("\\*\\*"), "")
                .trim()
            val noticeLines = mutableListOf<String>()
            i++
            while (i < lines.size) {
                val nextLine = lines[i].trim()
                if (nextLine.startsWith("---") || nextLine.startsWith("___") ||
                    isVersionHeaderLine(nextLine) || nextLine.startsWith("# ")
                ) {
                    break
                }
                if (nextLine.isNotEmpty()) {
                    noticeLines.add(nextLine)
                }
                i++
            }
            items.add(ChangelogItem.Notice(icon = icon, title = titleClean, lines = noticeLines))
            continue
        }

        if (line.matches(Regex("^-{3,}|_{3,}|\\*{3,}$"))) {
            if (items.isNotEmpty()) {
                items.add(ChangelogItem.Divider)
            }
            i++
            continue
        }

        val rawUrlMatch = Regex("""(https?://[^\s)]+)""").find(line)
        if (rawUrlMatch != null) {
            val lowerLine = line.lowercase()
            if (lowerLine.contains("pixelmusic.pages.dev") || lowerLine.contains("official") || lowerLine.contains("apk page") || lowerLine.contains("download page") || lowerLine.contains("website")) {
                // Skip duplicate official website links inside What's New (the sheet already has the "Open the web" pill)
                i++
                continue
            }
        }

        val mdImageMatch = Regex("""^!\[(.*?)\]\((https?://[^\s)]+)\)""").find(line)
        val htmlImageMatch = Regex("""^<img\s+[^>]*src=["'](https?://[^"']+)["'][^>]*>""", RegexOption.IGNORE_CASE).find(line)
        val directMediaMatch = Regex("""^(https?://[^\s]+?\.(?:png|jpg|jpeg|webp|gif|svg)(?:\?[^\s]*)?)$""", RegexOption.IGNORE_CASE).find(line)
        val mediaUrlMatch = Regex("""^(https?://[^\s]+(?:/assets/[^\s]+|giphy\.com|tenor\.com|imgur\.com)[^\s]*)$""", RegexOption.IGNORE_CASE).find(line)

        if (mdImageMatch != null) {
            val alt = mdImageMatch.groupValues[1]
            val url = mdImageMatch.groupValues[2]
            items.add(ChangelogItem.Image(url = url, alt = alt.ifBlank { null }))
            i++
            continue
        } else if (htmlImageMatch != null) {
            val url = htmlImageMatch.groupValues[1]
            items.add(ChangelogItem.Image(url = url, alt = null))
            i++
            continue
        } else if (directMediaMatch != null) {
            val url = directMediaMatch.groupValues[1]
            items.add(ChangelogItem.Image(url = url, alt = null))
            i++
            continue
        } else if (mediaUrlMatch != null) {
            val url = mediaUrlMatch.groupValues[1]
            items.add(ChangelogItem.Image(url = url, alt = null))
            i++
            continue
        }

        if (isVersionHeaderLine(line)) {
            val cleanName = line
                .replace(Regex("^[-*#\\s]+|[-*#\\s]+$"), "")
                .trim()
            val badgeType = when {
                cleanName.contains("Final", ignoreCase = true) || cleanName.contains("Stable", ignoreCase = true) -> BadgeType.PRIMARY
                cleanName.contains("Sub release", ignoreCase = true) || cleanName.contains("Patch", ignoreCase = true) -> BadgeType.SECONDARY
                cleanName.contains("Beta", ignoreCase = true) || cleanName.contains("Alpha", ignoreCase = true) || cleanName.contains("test", ignoreCase = true) -> BadgeType.TERTIARY
                else -> BadgeType.PRIMARY
            }
            items.add(ChangelogItem.VersionHeader(raw = line, cleanName = cleanName, badgeColorType = badgeType))
            i++
            continue
        }

        if (line.startsWith("#")) {
            val level = line.takeWhile { it == '#' }.length
            val title = line.drop(level).trim().replace(Regex("\\*\\*"), "")
            items.add(ChangelogItem.SectionHeader(title = title, level = level))
            i++
            continue
        }

        val bulletMatch = Regex("""^(\*|-|•|\d+\.)\s+(.*)""").find(line)
        if (bulletMatch != null) {
            val rawContent = bulletMatch.groupValues[2].trim()

            val emojiMatch = Regex("""^([\p{So}\p{Sk}\p{Sm}\p{Sc}\uD83C-\uDBFF\uDC00-\uDFFF\u2600-\u27BF\u2300-\u23FF\u2B50\uFE0F]+)\s*""").find(rawContent)
            val emoji = emojiMatch?.groupValues?.get(1)?.trim()
            val afterEmoji = if (emojiMatch != null) rawContent.substring(emojiMatch.range.last + 1).trim() else rawContent

            val titleMatch = Regex("""^\*\*([^*]+)\*\*[:\-]?\s*(.*)""").find(afterEmoji)
            val title = titleMatch?.groupValues?.get(1)?.trim()
            val body = if (titleMatch != null) titleMatch.groupValues[2].trim() else afterEmoji

            val contributorRegex = Regex("""\[@([a-zA-Z0-9_\-]+)\]""")
            val contributors = contributorRegex.findAll(body).map { it.groupValues[1] }.toList()
            val bodyClean = body.replace(contributorRegex, "").trim()

            items.add(
                ChangelogItem.Bullet(
                    emoji = emoji,
                    title = title,
                    body = bodyClean.ifBlank { title ?: "" },
                    tags = contributors
                )
            )
            i++
            continue
        }

        items.add(ChangelogItem.Paragraph(text = line))
        i++
    }

    return items
}

private fun isVersionHeaderLine(line: String): Boolean {
    val trimmed = line.trim()
    val stripped = trimmed.replace(Regex("^[-*#\\s]+|[-*#\\s]+$"), "").trim()
    if (stripped.isEmpty()) return false
    val lower = stripped.lowercase()
    return lower.startsWith("sub release") ||
           lower.startsWith("final release") ||
           lower.startsWith("release") ||
           lower.startsWith("version") ||
           lower.startsWith("pixel music beta") ||
           lower.startsWith("pixel music alpha") ||
           lower.startsWith("beta-") ||
           lower.startsWith("v4.") ||
           lower.startsWith("v3.") ||
           lower.startsWith("v2.") ||
           lower.startsWith("v1.") ||
           lower.startsWith("v0.")
}
