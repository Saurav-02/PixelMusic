package com.saurav.pixelmusic.presentation.screens.audiolab

import android.os.Build
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.saurav.pixelmusic.R
import com.saurav.pixelmusic.presentation.components.MiniPlayerHeight
import com.saurav.pixelmusic.ui.blur.GlassState
import com.saurav.pixelmusic.ui.blur.ProgressiveHeaderBackdrop
import com.saurav.pixelmusic.ui.blur.glassRoot
import com.saurav.pixelmusic.ui.theme.PixelMusicStatusBarStyle
import kotlin.math.roundToInt

private val SheetCorner = 40.dp
private val ContentMaxWidth = 680.dp
private val SideMargin = 24.dp
private val BackSize = 32.dp

class LabTitle(
    val first: String,
    val second: String,
    val firstSize: TextUnit = 46.sp,
    val secondSize: TextUnit = 46.sp,
    val firstDim: Boolean = false,
    val secondDim: Boolean = false,
    val secondBadge: String? = null,
)

@Composable
fun AudioLabPage(
    title: LabTitle,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    art: (@Composable BoxScope.(hidden: State<Boolean>) -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val scroll = rememberScrollState()
    val pageBackdrop = LabColors.PageBackdrop
    val sheetColor = LabColors.Sheet
    PixelMusicStatusBarStyle(color = pageBackdrop)
    val statusTop = WindowInsets.stableStatusBars.asPaddingValues().calculateTopPadding()
    val bottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + MiniPlayerHeight

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(sheetColor),
    ) {
        val wide = maxWidth > ContentMaxWidth + SideMargin * 2
        val inset = if (wide) (maxWidth - ContentMaxWidth) / 2 + SideMargin else SideMargin
        val titleScale = (maxWidth / 400.dp).coerceIn(1f, 1.6f)
        val viewport = maxHeight

        SubcomposeLayout(Modifier.fillMaxSize()) { constraints ->
            val loose = constraints.copy(minWidth = 0, minHeight = 0, maxHeight = Constraints.Infinity)
            val headerHeight = subcompose("measure") {
                LabHeader(title, subtitle, inset, statusTop, titleScale)
            }.maxOfOrNull { it.measure(loose).height } ?: 0
            val headerDp = headerHeight.toDp()

            val backdrop = subcompose("backdrop") {
                LabBackdrop(scroll, headerHeight) {
                    if (art != null) {
                        val hidden = remember(headerHeight) { derivedStateOf { scroll.value >= headerHeight * 0.6f } }
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .fillMaxWidth(if (wide) 0.4f else 0.5f)
                                .height(headerDp + LabArtOverhang)
                                .padding(top = statusTop + 24.dp),
                        ) { art(hidden) }
                    }
                    LabHeader(title, subtitle, inset, statusTop, titleScale)
                }
            }.map { it.measure(constraints) }

            val body = subcompose("body") {
                val glassState = remember { GlassState() }
                Box(Modifier.fillMaxSize().graphicsLayer()) {
                    Column(
                        Modifier
                            .fillMaxSize()
                            .glassRoot(glassState, isActive = { !scroll.isScrollInProgress })
                            .verticalScroll(scroll)
                    ) {
                        Spacer(Modifier.height(headerDp))
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = viewport - headerDp + SheetCorner)
                                .clip(RoundedCornerShape(topStart = SheetCorner, topEnd = SheetCorner))
                                .background(sheetColor),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Column(
                                modifier = Modifier
                                    .widthIn(max = ContentMaxWidth)
                                    .fillMaxWidth()
                                    .padding(horizontal = SideMargin)
                                    .padding(top = 30.dp, bottom = bottomInset + 32.dp),
                                content = content,
                            )
                        }
                    }
                    TopScrim(scroll, headerHeight, statusTop, glassState)
                    LabBackButton(onBack, Modifier.padding(start = inset, top = statusTop + 14.dp))
                }
            }.map { it.measure(constraints) }

            layout(constraints.maxWidth, constraints.maxHeight) {
                backdrop.forEach { it.place(0, 0) }
                body.forEach { it.place(0, 0) }
            }
        }
    }
}

internal val LabArtOverhang = 64.dp

@Composable
private fun LabBackdrop(scroll: ScrollState, headerHeight: Int, content: @Composable BoxScope.() -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                val covered = (scroll.value / headerHeight.toFloat().coerceAtLeast(1f)).coerceIn(0f, 1f)
                val scale = 1f - 0.07f * covered
                scaleX = scale
                scaleY = scale
                transformOrigin = TransformOrigin(0.5f, 0f)
                alpha = 1f - 0.4f * covered
                shape = RoundedCornerShape((30f * covered).dp)
                clip = covered > 0f
                renderEffect = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    BlurEffectCache.element(18.dp.toPx() * covered, android.graphics.Shader.TileMode.DECAL)
                } else {
                    null
                }
            }
            .background(LabColors.PageBackdrop)
            .dotGrid(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)),
        content = content,
    )
}

@Composable
fun LabSheetTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        letterSpacing = (-0.02).em,
        color = LabColors.Secondary,
        modifier = modifier.padding(start = 4.dp, top = 6.dp, bottom = 12.dp),
    )
}

@Composable
fun LabGroup(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(30.dp))
            .background(LabColors.Card)
            .padding(12.dp),
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp),
        content = content,
    )
}

@Composable
private fun LabHeader(
    title: LabTitle,
    subtitle: String?,
    inset: Dp,
    statusTop: Dp,
    scale: Float,
) {
    val fontScale = LocalDensity.current.fontScale
    val keep = fontScale.coerceAtMost(1.15f) / fontScale * scale
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = inset, end = inset, top = statusTop + 14.dp + BackSize + 18.dp, bottom = SheetCorner + 26.dp),
    ) {
        Text(
            title.first,
            fontFamily = InterFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = title.firstSize * keep,
            lineHeight = title.firstSize * keep * 0.95f,
            letterSpacing = (-0.035).em,
            color = if (title.firstDim) LabColors.Secondary else LabColors.Primary,
            maxLines = 1,
            overflow = TextOverflow.Clip,
            softWrap = false,
        )
        if (title.second.isNotEmpty()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    title.second,
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = title.secondSize * keep,
                    lineHeight = title.secondSize * keep * 0.92f,
                    letterSpacing = (-0.04).em,
                    color = if (title.secondDim) LabColors.Secondary else LabColors.Primary,
                    maxLines = 1,
                    overflow = TextOverflow.Clip,
                    softWrap = false,
                    modifier = Modifier.offset(y = (-2).dp),
                )
                title.secondBadge?.let { badge ->
                    Spacer(Modifier.width(12.dp))
                    LabBadge(badge)
                }
            }
        }
        if (subtitle != null) {
            Spacer(Modifier.height(8.dp))
            Text(
                subtitle.uppercase(),
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                letterSpacing = 0.06.em,
                color = LabColors.Secondary,
                maxLines = 2,
                modifier = Modifier.widthIn(max = 220.dp),
            )
        }
    }
}

@Composable
private fun LabBadge(text: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.tertiaryContainer)
            .border(1.5.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.6f), RoundedCornerShape(50))
            .padding(horizontal = 12.dp, vertical = 3.dp),
    ) {
        Text(
            text,
            fontFamily = InterFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 13.sp,
            letterSpacing = 0.08.em,
            color = MaterialTheme.colorScheme.onTertiaryContainer,
            maxLines = 1,
        )
    }
}

@Composable
private fun TopScrim(scroll: ScrollState, headerHeight: Int, statusTop: Dp, glassState: GlassState) {
    val sheetColor = LabColors.Sheet
    val density = LocalDensity.current
    val progress = remember(scroll.value, headerHeight, statusTop, density) {
        with(density) {
            val reach = headerHeight - (statusTop + 64.dp).toPx()
            ((scroll.value - reach) / 48.dp.toPx()).coerceIn(0f, 1f)
        }
    }
    ProgressiveHeaderBackdrop(
        alpha = progress,
        scrimColor = sheetColor,
        glassState = glassState,
        modifier = Modifier
            .fillMaxWidth()
            .height(statusTop + 88.dp),
    )
}

@Composable
internal fun LabBackButton(onBack: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(BackSize)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            .clickable(onClick = onBack),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_lab_arrow_back),
            contentDescription = stringResource(R.string.lab_back),
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(15.dp),
        )
    }
}

private fun Modifier.dotGrid(dotColor: Color): Modifier = drawWithCache {
    val step = 12.dp.toPx().roundToInt().coerceAtLeast(2)
    val tile = ImageBitmap(step, step)
    val paint = Paint().apply {
        color = dotColor
        isAntiAlias = true
    }
    androidx.compose.ui.graphics.Canvas(tile).drawCircle(Offset(step / 2f, step / 2f), 0.9.dp.toPx(), paint)
    val brush = ShaderBrush(ImageShader(tile, TileMode.Repeated, TileMode.Repeated))
    onDrawBehind { drawRect(brush) }
}
