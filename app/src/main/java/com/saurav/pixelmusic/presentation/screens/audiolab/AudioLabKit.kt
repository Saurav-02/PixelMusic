package com.saurav.pixelmusic.presentation.screens.audiolab

import com.saurav.pixelmusic.presentation.screens.HighlightPulseOverlay
import com.saurav.pixelmusic.presentation.screens.LocalSettingsHighlightTitle
import com.saurav.pixelmusic.presentation.screens.autoBringIntoViewWhenHighlighted

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.saurav.pixelmusic.audiolab.audio.AudioTap
import kotlinx.coroutines.flow.first

object LabColors {
    val PageBackdrop: Color
        @Composable get() = MaterialTheme.colorScheme.surfaceContainerLow
    val Sheet: Color
        @Composable get() = MaterialTheme.colorScheme.background
    val HubCard: Color
        @Composable get() = MaterialTheme.colorScheme.surfaceContainerHigh
    val Card: Color
        @Composable get() = MaterialTheme.colorScheme.surfaceContainer
    val Stage: Color
        @Composable get() = MaterialTheme.colorScheme.surfaceContainerLowest
    val StageLine: Color
        @Composable get() = MaterialTheme.colorScheme.outlineVariant
    val Row: Color
        @Composable get() = MaterialTheme.colorScheme.surfaceContainerHigh
    val RowSelected: Color
        @Composable get() = MaterialTheme.colorScheme.secondaryContainer
    val Chip: Color
        @Composable get() = MaterialTheme.colorScheme.surfaceContainerHigh
    val Accent: Color
        @Composable get() = MaterialTheme.colorScheme.primary
    val OnAccent: Color
        @Composable get() = MaterialTheme.colorScheme.onPrimary
    val Primary: Color
        @Composable get() = MaterialTheme.colorScheme.onSurface
    val Secondary: Color
        @Composable get() = MaterialTheme.colorScheme.onSurfaceVariant
    val Faint: Color
        @Composable get() = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
    val Ink: Color
        @Composable get() = MaterialTheme.colorScheme.onSurfaceVariant
    val Sketch: Color
        @Composable get() = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
    val SelectBg: Color
        @Composable get() = MaterialTheme.colorScheme.surfaceContainerLow
    val SelectEdge: Color
        @Composable get() = MaterialTheme.colorScheme.outlineVariant
    val SelectEdgeOpen: Color
        @Composable get() = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)

    val Nodes = listOf(
        Color(0xFFE05A82), Color(0xFFE0833E), Color(0xFFD4AD2B), Color(0xFF56B352),
        Color(0xFF33B39B), Color(0xFF3A96DE), Color(0xFF5E73E0), Color(0xFF935EE0),
        Color(0xFFC45EE0), Color(0xFFE04848),
    )

    fun node(index: Int) = Nodes[index.mod(Nodes.size)]
}

@Composable
internal fun HighlightableLabItem(
    matchTitles: List<String>,
    shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(26.dp),
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val highlightTitle = LocalSettingsHighlightTitle.current
    val highlighted = highlightTitle != null && matchTitles.any { it.equals(highlightTitle, ignoreCase = true) }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .autoBringIntoViewWhenHighlighted(highlighted)
    ) {
        content()
        if (highlighted) {
            HighlightPulseOverlay(shape = shape)
        }
    }
}

@Composable
fun LabCard(
    modifier: Modifier = Modifier,
    padding: Dp = 14.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(26.dp))
            .background(LabColors.Card)
            .padding(padding),
        content = content,
    )
}

@Composable
fun LabStage(
    modifier: Modifier = Modifier,
    height: Dp = 132.dp,
    content: @Composable androidx.compose.foundation.layout.BoxScope.() -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(18.dp))
            .background(LabColors.Stage)
            .border(1.dp, LabColors.StageLine, RoundedCornerShape(18.dp))
            .damageBoundary(),
    ) {
        content()
    }
}

@Composable
fun LabToggleRow(
    title: String,
    subtitle: String?,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingDot: Boolean = false,
) {
    val hideDescriptions = false
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(enabled = enabled) { onCheckedChange(!checked) }
            .padding(horizontal = 6.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (leadingDot) {
            val dot by animateColorAsState(if (checked) LabColors.Accent else LabColors.Secondary.copy(alpha = 0.4f), tween(200), label = "dot")
            Box(Modifier.size(20.dp).clip(CircleShape).background(dot))
            Spacer(Modifier.width(12.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
                color = if (enabled) LabColors.Primary else LabColors.Faint,
            )
            if (subtitle != null && !hideDescriptions) {
                Text(
                    text = subtitle,
                    fontFamily = InterFontFamily,
                    fontSize = 12.sp,
                    lineHeight = 15.sp,
                    color = if (enabled) LabColors.Secondary else LabColors.Faint,
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        AppSwitch(checked = checked, onCheckedChange = onCheckedChange, enabled = enabled)
    }
}

@Composable
fun LabSlider(
    label: String,
    valueText: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    steps: Int = 0,
) {
    Column(modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = label,
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                color = if (enabled) LabColors.Primary else LabColors.Faint,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = valueText,
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                color = if (enabled) LabColors.Primary else LabColors.Faint,
            )
        }
        DefaultPlayerSeekBarSlider(
            value = value.coerceIn(valueRange.start, valueRange.endInclusive),
            onValueChange = onValueChange,
            valueRange = valueRange,
            steps = steps,
            enabled = enabled,
        )
    }
}

@Composable
fun LabPanel(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(LabColors.Row)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        content = content,
    )
}

@Composable
fun LabCollapsible(visible: Boolean, content: @Composable ColumnScope.() -> Unit) {
    AnimatedVisibility(
        visible = visible,
        enter = expandVertically(spring(dampingRatio = 0.9f, stiffness = Spring.StiffnessMediumLow)) + fadeIn(tween(220, 60)),
        exit = shrinkVertically(spring(dampingRatio = 1f, stiffness = Spring.StiffnessMedium)) + fadeOut(tween(140)),
    ) {
        Column(Modifier.fillMaxWidth()) { content() }
    }
}

@Composable
fun LabSectionLabel(text: String, modifier: Modifier = Modifier, actions: @Composable RowScope.() -> Unit = {}) {
    Row(modifier.fillMaxWidth().padding(top = 4.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = text.uppercase(),
            fontFamily = InterFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 12.sp,
            letterSpacing = 1.4.sp,
            color = LabColors.Secondary,
            modifier = Modifier.weight(1f),
        )
        actions()
    }
}

@Composable
fun LabTextAction(text: String, iconRes: Int? = null, enabled: Boolean = true, busy: Boolean = false, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(enabled = enabled && !busy, onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (iconRes != null) {
            val turn = if (busy) rememberInfiniteTransition(label = "busy")
                .animateFloat(0f, 360f, infiniteRepeatable(tween(800, easing = LinearEasing)), label = "turn") else null
            Icon(
                painterResource(iconRes), null, tint = if (enabled) LabColors.Accent else LabColors.Faint,
                modifier = Modifier.size(16.dp).graphicsLayer { rotationZ = turn?.value ?: 0f },
            )
            Spacer(Modifier.width(6.dp))
        }
        Text(text, fontFamily = InterFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 14.sp,
            color = if (enabled) LabColors.Accent else LabColors.Faint)
    }
}

data class LabChip(val key: String, val label: String, val dot: Color? = null)

@Composable
fun LabChips(
    chips: List<LabChip>,
    selectedKey: String?,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
    onLongPress: ((String) -> Unit)? = null,
    leading: @Composable RowScope.() -> Unit = {},
    trailing: @Composable RowScope.() -> Unit = {},
) {
    Row(
        modifier = modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        leading()
        chips.forEach { chip ->
            val selected = chip.key == selectedKey
            val bg by animateColorAsState(if (selected) LabColors.Accent else LabColors.Chip, tween(180), label = "chipBg")
            val fg by animateColorAsState(if (selected) LabColors.OnAccent else LabColors.Primary, tween(180), label = "chipFg")
            val interaction = remember { MutableInteractionSource() }
            Row(
                modifier = Modifier
                    .height(38.dp)
                    .clip(RoundedCornerShape(19.dp))
                    .background(bg)
                    .then(
                        if (onLongPress != null) Modifier.combinedClickableCompat(
                            interaction, onClick = { onSelect(chip.key) }, onLongClick = { onLongPress(chip.key) },
                        ) else Modifier.clickable(interaction, null) { onSelect(chip.key) }
                    )
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (chip.dot != null) {
                    Box(Modifier.size(8.dp).clip(CircleShape).background(chip.dot))
                    Spacer(Modifier.width(8.dp))
                }
                Text(chip.label, fontFamily = InterFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = fg, maxLines = 1)
            }
        }
        trailing()
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
private fun Modifier.combinedClickableCompat(
    interaction: MutableInteractionSource,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
): Modifier = this.then(
    Modifier.combinedClickable(interactionSource = interaction, indication = RemoteFocus, onClick = onClick, onLongClick = onLongClick)
)

@Composable
fun LabSegmented(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = 48.dp,
) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(height / 2))
            .background(LabColors.Card)
            .border(1.dp, LabColors.StageLine, RoundedCornerShape(height / 2))
            .padding(4.dp),
    ) {
        val segment = maxWidth / options.size
        val offset by animateDpAsState(segment * selectedIndex, spring(dampingRatio = 0.82f, stiffness = 520f), label = "segment")
        Box(
            Modifier
                .offset(x = offset)
                .width(segment)
                .fillMaxHeight()
                .clip(RoundedCornerShape(height / 2))
                .background(LabColors.Accent)
        )
        Row(Modifier.fillMaxWidth().fillMaxHeight()) {
            options.forEachIndexed { index, label ->
                val color by animateColorAsState(if (index == selectedIndex) LabColors.OnAccent else LabColors.Primary, tween(160), label = "segText")
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(height / 2))
                        .clickable(remember { MutableInteractionSource() }, null) { onSelect(index) },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(label, fontFamily = InterFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = color,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

@Composable
fun LabGlyph(iconRes: Int, modifier: Modifier = Modifier, size: Dp = 32.dp) {
    Box(
        modifier = modifier.size(size).clip(CircleShape).background(MaterialTheme.colorScheme.secondaryContainer),
        contentAlignment = Alignment.Center,
    ) {
        Icon(painterResource(iconRes), null, tint = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.size(size * 0.55f))
    }
}

fun Modifier.dimmedWhen(off: Boolean, alpha: Float): Modifier = graphicsLayer { this.alpha = if (off) alpha else 1f }

@Composable
fun animatedDim(off: Boolean): Float =
    animateFloatAsState(if (off) 0.45f else 1f, tween(220), label = "dim").value

@Composable
fun rememberPicturesHeld(onScreen: State<Boolean>): State<Boolean> {
    return remember(onScreen) {
        derivedStateOf { !onScreen.value }
    }
}

@Composable
fun rememberLabTime(held: State<Boolean>, moving: () -> Boolean = { true }): State<Float> {
    val time = remember { mutableFloatStateOf(0f) }
    val currentMoving by androidx.compose.runtime.rememberUpdatedState(moving)
    LaunchedEffect(held) {
        var last = 0L
        while (true) {
            if (held.value) {
                snapshotFlow { held.value }.first { !it }
                last = 0L
            }
            if (!currentMoving()) {
                kotlinx.coroutines.delay(250)
                last = 0L
                continue
            }
            withFrameNanos { now ->
                if (last != 0L) time.floatValue += ((now - last) / 1e9f).coerceAtMost(0.05f)
                last = now
            }
        }
    }
    return time
}

@Composable
fun Modifier.trackOnScreen(state: MutableState<Boolean>): Modifier {
    val view = LocalView.current
    return onGloballyPositioned { coords ->
        val bounds = coords.boundsInWindow()
        val visible = bounds.height > 0f && bounds.bottom > 0f && bounds.top < view.height
        if (visible != state.value) state.value = visible
    }
}

@Composable
fun ListenToAudio() {
    DisposableEffect(Unit) {
        AudioTap.listen()
        onDispose { AudioTap.stopListening() }
    }
}

class LevelFollower {
    private val scratch = FloatArray(1024)
    var level = 0f
        private set
    var punch = 0f
        private set
    private var slow = 0f

    fun update(dt: Float) {
        val now = AudioTap.level(scratch)
        level += (now - level) * (if (now > level) 0.5f else 0.12f)
        slow += (now - slow) * 0.03f
        val p = ((now - slow * 1.25f) * 6f).coerceIn(0f, 1f)
        punch = if (p > punch) p else (punch - dt * 3f).coerceAtLeast(0f)
    }
}

@Composable
fun rememberOnScreen(): MutableState<Boolean> = remember { mutableStateOf(true) }

@Composable
fun dpToPx(dp: Dp): Float = with(LocalDensity.current) { dp.toPx() }

@Composable
fun LabSelect(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier,
    mutedIndex: Int = 0,
    maxListHeight: Dp = 264.dp,
) {
    var open by rememberSaveable { mutableStateOf(false) }
    val chevron by animateFloatAsState(if (open) 180f else 0f, spring(dampingRatio = 0.8f, stiffness = 420f), label = "chevron")
    val selectBg = LabColors.SelectBg
    val selectEdge = LabColors.SelectEdge
    val selectEdgeOpen = LabColors.SelectEdgeOpen
    val edge by animateColorAsState(if (open) selectEdgeOpen else selectEdge, tween(180), label = "selectEdge")
    val listScroll = rememberScrollState()
    val rowHeight = with(LocalDensity.current) { LabSelectRowHeight.roundToPx() }
    val bringIntoView = remember { BringIntoViewRequester() }

    BackHandler(enabled = open) { open = false }
    LaunchedEffect(open) {
        if (!open) return@LaunchedEffect
        listScroll.scrollTo(((selectedIndex - 2) * rowHeight).coerceAtLeast(0))
        kotlinx.coroutines.delay(LabSelectUnrollMs.toLong())
        bringIntoView.bringIntoView()
    }

    Column(
        modifier
            .fillMaxWidth()
            .bringIntoViewRequester(bringIntoView)
            .clip(LabSelectShape)
            .background(selectBg)
            .border(1.dp, edge, LabSelectShape),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .clickable { open = !open }
                .padding(start = 16.dp, end = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = options.getOrNull(selectedIndex).orEmpty(),
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
                color = if (selectedIndex == mutedIndex) LabColors.Secondary else LabColors.Primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Icon(
                painter = painterResource(com.saurav.pixelmusic.R.drawable.ic_lab_expand_more),
                contentDescription = contentDescription,
                tint = if (open) LabColors.Accent else LabColors.Secondary,
                modifier = Modifier.size(22.dp).graphicsLayer { rotationZ = chevron },
            )
        }
        AnimatedVisibility(
            visible = open,
            enter = expandVertically(tween(LabSelectUnrollMs, easing = FastOutSlowInEasing), expandFrom = Alignment.Top) + fadeIn(tween(160, 40)),
            exit = shrinkVertically(tween(200, easing = FastOutSlowInEasing), shrinkTowards = Alignment.Top) + fadeOut(tween(120)),
        ) {
            Column {
                Box(Modifier.fillMaxWidth().padding(horizontal = 12.dp).height(1.dp).background(selectEdge))
                Column(
                    Modifier
                        .heightIn(max = maxListHeight)
                        .verticalScroll(listScroll)
                        .padding(6.dp),
                ) {
                    options.forEachIndexed { index, label ->
                        LabSelectRow(
                            label = label,
                            picked = index == selectedIndex,
                            onClick = {
                                onSelect(index)
                                open = false
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LabSelectRow(label: String, picked: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(LabSelectRowHeight)
            .clip(RoundedCornerShape(10.dp))
            .background(if (picked) LabColors.Row else Color.Transparent)
            .selectable(selected = picked, role = Role.RadioButton, onClick = onClick)
            .padding(start = 10.dp, end = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            fontFamily = InterFontFamily,
            fontWeight = if (picked) FontWeight.SemiBold else FontWeight.Medium,
            fontSize = 15.sp,
            color = if (picked) LabColors.Primary else LabColors.Secondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        if (picked) {
            Box(Modifier.size(20.dp).clip(CircleShape).background(LabColors.Accent), contentAlignment = Alignment.Center) {
                Icon(painterResource(com.saurav.pixelmusic.R.drawable.ic_lab_check), contentDescription = null, tint = LabColors.OnAccent, modifier = Modifier.size(14.dp))
            }
        }
    }
}

private const val LabSelectUnrollMs = 240
private val LabSelectRowHeight = 44.dp
private val LabSelectShape = RoundedCornerShape(16.dp)
