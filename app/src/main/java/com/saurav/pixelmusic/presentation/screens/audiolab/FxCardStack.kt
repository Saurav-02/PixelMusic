package com.saurav.pixelmusic.presentation.screens.audiolab

import androidx.annotation.StringRes
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.saurav.pixelmusic.R
import com.saurav.pixelmusic.audiolab.EightDSettings
import com.saurav.pixelmusic.audiolab.LoudnessSettings
import com.saurav.pixelmusic.audiolab.PlaybackSettings
import com.saurav.pixelmusic.audiolab.ReverbSettings
import com.saurav.pixelmusic.audiolab.SpatialSettings
import com.saurav.pixelmusic.audiolab.VolumeSettings
import com.saurav.pixelmusic.audiolab.stage.StageSettings
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first

class ActiveEffect(val key: String, val label: String)

internal fun activeEffects(
    reverb: ReverbSettings,
    loudness: LoudnessSettings,
    spatial: SpatialSettings,
    eightD: EightDSettings,
    playback: PlaybackSettings,
    volume: VolumeSettings,
    stage: StageSettings,
): List<ActiveEffect> = buildList {
    if (stage.enabled) add(ActiveEffect("stage", "3D Sims"))
    if (reverb.enabled) add(ActiveEffect("reverb", "Reverb"))
    if (loudness.enabled) add(ActiveEffect("loudness", "Loudness"))
    if (spatial.enabled) add(ActiveEffect("spatial", "Spatial"))
    if (eightD.enabled) add(ActiveEffect("eight_d", "8D Audio"))
    if (spatial.surround) add(ActiveEffect("surround", "Surround"))
    if (spatial.mono) add(ActiveEffect("mono", "Mono"))
    if (!playback.isNormal) add(ActiveEffect("playback", "Playback"))
    if (volume.enabled) add(ActiveEffect("volume", "Volume"))
}

@StringRes
internal fun effectLabelRes(key: String): Int = when (key) {
    "reverb" -> R.string.lab_effect_reverb
    "loudness" -> R.string.lab_effect_loudness
    "spatial" -> R.string.lab_effect_spatial
    "eight_d" -> R.string.eight_d_title
    "surround" -> R.string.lab_effect_surround
    "mono" -> R.string.lab_effect_mono
    "playback" -> R.string.lab_effect_playback
    "stage" -> R.string.lab_effect_stage
    else -> R.string.lab_effect_volume
}

private const val ShownCards = 3
private const val FrontSeconds = 3_200L
private const val IdleKey = "idle"

private class StackCard(val key: String, val label: String?) {
    var leaving by mutableStateOf(false)
    var lastDepth = 0
}

@Composable
fun BoxScope.FxCardStack(effects: List<ActiveEffect>, hidden: State<Boolean>) {
    val spectrum = rememberLabSpectrum(10)
    val cards = remember { mutableStateListOf<StackCard>() }
    val wanted = if (effects.isEmpty()) listOf(IdleKey) else effects.map { it.key }
    LaunchedEffect(wanted) {
        val labels = effects.associate { it.key to it.label }
        val staying = wanted.map { key ->
            cards.firstOrNull { it.key == key }?.also { it.leaving = false } ?: StackCard(key, labels[key])
        }
        val going = cards.filter { it.key !in wanted }.onEach { it.leaving = true }
        cards.clear()
        cards.addAll(staying + going)
    }

    val staying = cards.filterNot { it.leaving }
    val count = staying.size
    var front by remember { mutableIntStateOf(0) }
    LaunchedEffect(count) {
        while (count > 1) {
            delay(FrontSeconds)
            snapshotFlow { hidden.value }.first { !it }
            front++
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .damageBoundary()
            .clickable(remember { MutableInteractionSource() }, indication = RemoteFocus, enabled = count > 1) { front++ },
    ) {
        if (count > ShownCards) {
            Text(
                stringResource(R.string.lab_fx_and_other, count - ShownCards),
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                color = LabCardInk,
                modifier = Modifier.align(Alignment.BottomStart).padding(start = 26.dp, bottom = LabCardHeight + 46.dp),
            )
        }
        cards.forEach { card ->
            key(card.key) {
                val index = staying.indexOf(card)
                val depth = if (index >= 0) (index - front).mod(count) else card.lastDepth
                card.lastDepth = depth
                StackedCard(
                    card = card,
                    depth = depth,
                    position = index + 1,
                    count = count,
                    spectrum = spectrum,
                    hidden = hidden,
                    onGone = { cards.remove(card) },
                )
            }
        }
    }
}

@Composable
private fun BoxScope.StackedCard(
    card: StackCard,
    depth: Int,
    position: Int,
    count: Int,
    spectrum: LabSpectrum,
    hidden: State<Boolean>,
    onGone: () -> Unit,
) {
    val place by animateFloatAsState(depth.toFloat(), spring(dampingRatio = 0.82f, stiffness = 170f), label = "fan")
    val rise = remember { Animatable(1f) }
    LaunchedEffect(card.leaving) {
        rise.animateTo(if (card.leaving) 1f else 0f, spring(dampingRatio = 0.86f, stiffness = 120f))
        if (card.leaving) onGone()
    }

    LabHeaderCardFrame(
        modifier = Modifier
            .align(Alignment.BottomStart)
            .padding(start = LabCardStart, bottom = LabCardBottom)
            .zIndex(-place)
            .graphicsLayer {
                translationX = 20.dp.toPx() * place
                translationY = -10.dp.toPx() * place + rise.value * (LabCardHeight + 80.dp).toPx()
                rotationZ = -10f + 5f * place
                val scale = 1f - 0.04f * place
                scaleX = scale
                scaleY = scale
                alpha = (ShownCards - place).coerceIn(0f, 1f)
            },
    ) {
        if (depth >= ShownCards && place >= ShownCards - 0.01f) return@LabHeaderCardFrame
        val label = card.label?.let { stringResource(effectLabelRes(card.key)) }
        LabCardFace(
            modifier = Modifier.graphicsLayer { alpha = 1f - place.coerceIn(0f, 1f) },
            topStart = label?.let { stringResource(R.string.lab_fx_on) },
            topEnd = label?.let { stringResource(R.string.lab_fx_position, position, count) },
            label = label,
        ) { barsModifier ->
            LabBars(spectrum = spectrum, hidden = hidden, modifier = barsModifier, color = LabCardBars, fill = 0.42f)
        }
        if (label != null) {
            Text(
                label.uppercase(),
                fontFamily = FontFamily.Monospace,
                fontSize = 9.sp,
                letterSpacing = 0.08.em,
                color = LabCardInk,
                maxLines = 1,
                softWrap = false,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 18.dp, end = 6.dp)
                    .graphicsLayer { alpha = place.coerceIn(0f, 1f) }
                    .readingDown(),
            )
        }
    }
}

private fun Modifier.readingDown(): Modifier = layout { measurable, _ ->
    val text = measurable.measure(Constraints())
    layout(text.height, text.width) {
        text.placeWithLayer(
            x = -(text.width - text.height) / 2,
            y = (text.width - text.height) / 2,
        ) { rotationZ = 90f }
    }
}
