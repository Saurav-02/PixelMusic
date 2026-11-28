package com.saurav.pixelmusic.presentation.screens.audiolab

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.saurav.pixelmusic.R
import com.saurav.pixelmusic.audiolab.ReverbPresets
import com.saurav.pixelmusic.audiolab.ReverbSettings
import com.saurav.pixelmusic.audiolab.audio.AudioTap
import com.saurav.pixelmusic.presentation.viewmodel.AudioLabViewModel
import kotlin.math.exp
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt

@Composable
internal fun ReverbCard(viewModel: AudioLabViewModel) {
    val reverb by viewModel.reverb.collectAsState()
    EffectCard(
        picture = { ReverbPicture(reverb) },
        header = {
            LabToggleRow(stringResource(R.string.fx_reverb_title), stringResource(R.string.fx_reverb_sub), reverb.enabled, { on -> viewModel.updateReverb { it.copy(enabled = on) } })
        },
        expanded = reverb.enabled,
        options = {
            LabChips(
                chips = ReverbPresets.All.map { LabChip(it.id, reverbPresetName(it)) } +
                    (if (reverb.presetId == null) listOf(LabChip("custom", stringResource(R.string.fx_reverb_custom))) else emptyList()),
                selectedKey = reverb.presetId ?: "custom",
                onSelect = { id -> ReverbPresets.All.firstOrNull { it.id == id }?.let { p -> viewModel.updateReverb { it.withPreset(p) } } },
            )
            Spacer(Modifier.height(10.dp))
            LabPanel {
                LabSlider(stringResource(R.string.fx_mix), pct(reverb.mix), reverb.mix, { v -> viewModel.updateReverb { it.copy(mix = v, presetId = null) } }, 0f..1f)
                LabSlider(stringResource(R.string.fx_room_size), pct(reverb.roomSize), reverb.roomSize, { v -> viewModel.updateReverb { it.copy(roomSize = v, presetId = null) } }, 0f..1f)
                LabSlider(stringResource(R.string.fx_damping), pct(reverb.damping), reverb.damping, { v -> viewModel.updateReverb { it.copy(damping = v, presetId = null) } }, 0f..1f)
                LabSlider(stringResource(R.string.fx_wet), pct(reverb.wet), reverb.wet, { v -> viewModel.updateReverb { it.copy(wet = v, presetId = null) } }, 0f..1f)
                FineTuneLabel()
                LabSlider(stringResource(R.string.fx_dry), pct(reverb.dry), reverb.dry, { v -> viewModel.updateReverb { it.copy(dry = v, presetId = null) } }, 0f..1f)
                LabSlider(stringResource(R.string.fx_width), pct(reverb.width), reverb.width, { v -> viewModel.updateReverb { it.copy(width = v, presetId = null) } }, 0f..1f)
                LabSlider(stringResource(R.string.fx_pre_delay), "${reverb.preDelayMs.roundToInt()} ms", reverb.preDelayMs,
                    { v -> viewModel.updateReverb { it.copy(preDelayMs = v.roundToInt().toFloat(), presetId = null) } }, 0f..120f)
                LabSlider(stringResource(R.string.fx_low_cut), "${reverb.lowCutHz.roundToInt()} Hz", log10(reverb.lowCutHz),
                    { v -> viewModel.updateReverb { it.copy(lowCutHz = 10f.pow(v).roundToInt().toFloat(), presetId = null) } },
                    log10(20f)..log10(1000f))
            }
        },
    )
}

@Composable
private fun reverbPresetName(preset: ReverbPresets.Preset): String = when (preset.id) {
    "room" -> stringResource(R.string.fx_reverb_room)
    "studio" -> stringResource(R.string.fx_reverb_studio)
    "bathroom" -> stringResource(R.string.fx_reverb_bathroom)
    "plate" -> stringResource(R.string.fx_reverb_plate)
    "hall" -> stringResource(R.string.fx_reverb_hall)
    "cathedral" -> stringResource(R.string.fx_reverb_cathedral)
    "stadium" -> stringResource(R.string.fx_reverb_stadium)
    "dream" -> stringResource(R.string.fx_reverb_dream)
    else -> preset.name
}

@Composable
private fun FineTuneLabel() {
    Text(
        stringResource(R.string.fx_fine_tune),
        fontFamily = InterFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 11.sp, letterSpacing = 1.3.sp,
        color = LabColors.Secondary, modifier = Modifier.padding(top = 8.dp, bottom = 2.dp),
    )
}

private class RoomWaves {
    val born = FloatArray(14) { -100f }
    val strength = FloatArray(14)
    var next = 0
    var nextEmit = 0f
    var lastEmit = -10f

    fun emit(t: Float, s: Float) {
        born[next] = t
        strength[next] = s
        next = (next + 1) % born.size
        lastEmit = t
    }
}

@Composable
private fun ReverbPicture(settings: ReverbSettings) {
    val room by animateFloatAsState(settings.roomSize, spring(dampingRatio = 0.8f, stiffness = 120f), label = "room")
    val on by animateFloatAsState(if (settings.enabled) 1f else 0f, tween(400), label = "on")
    val waves = remember { RoomWaves() }
    val ink = LabColors.Primary
    val accent = LabColors.Accent
    PictureCanvas { t, clock ->
        val w = size.width
        val h = size.height
        val cy = h / 2f
        val speakerX = w * 0.15f
        val backX = w * 0.05f
        val wallX = w * (0.52f + 0.4f * room)
        val speed = w * 0.42f

        val live = AudioTap.isLive
        val punch = clock.level.punch
        if (t >= waves.nextEmit || (live && punch > 0.55f && t - waves.lastEmit > 0.28f)) {
            waves.emit(t, if (live) 0.55f + 0.45f * min(1f, clock.level.level * 3f + punch) else 0.75f)
            waves.nextEmit = t + if (live) 0.9f else 1.3f
        }

        val reflectivity = (0.35f + 0.55f * settings.roomSize) * (1f - 0.55f * settings.damping)
        val echoAlpha = on * (0.35f + 0.65f * settings.wet) * (0.4f + 0.6f * settings.mix)
        var flash = 0f
        val span = wallX - backX
        for (i in waves.born.indices) {
            val age = t - waves.born[i]
            if (age < 0f || age > 8f) continue
            val d = age * speed
            val first = wallX - speakerX
            if (d <= first || on < 0.01f) {
                val x = speakerX + d
                if (x > w + 20f) continue
                val a = waves.strength[i] * exp(-d / (w * 1.6f))
                drawWave(x, cy, h, d, a * (if (d > first) 1f - on else 1f), rightward = true, softness = 0f, color = accent)
                continue
            }
            val past = d - first
            val bounces = 1 + (past / span).toInt()
            val inLeg = past % span
            val rightward = bounces % 2 == 0
            val x = if (rightward) backX + inLeg else wallX - inLeg
            val a = waves.strength[i] * reflectivity.pow(bounces) * exp(-d / (w * 2.2f)) * echoAlpha
            if (a < 0.02f) continue
            if (inLeg < span * 0.08f && !rightward) flash = max(flash, a * (1f - inLeg / (span * 0.08f)))
            drawWave(x, cy, h, d, a, rightward, softness = settings.damping, color = accent)
        }

        val wallH = h * 0.62f
        drawRoundRect(
            ink.copy(alpha = (0.18f + 0.55f * on + 0.6f * flash).coerceAtMost(1f)),
            Offset(wallX, cy - wallH / 2), Size(5.dp.toPx(), wallH), CornerRadius(3.dp.toPx()),
        )
        drawSpeaker(Offset(speakerX, cy), 1f + clock.level.punch * 0.12f, color = ink)
    }
}

private fun DrawScope.drawWave(x: Float, cy: Float, h: Float, travelled: Float, alpha: Float, rightward: Boolean, softness: Float, color: Color) {
    if (alpha <= 0.005f) return
    val reach = min(h * 0.42f, 14.dp.toPx() + travelled * 0.18f)
    val bulge = reach * 0.42f * if (rightward) 1f else -1f
    val path = Path().apply {
        moveTo(x - bulge * 0.2f, cy - reach)
        quadraticTo(x + bulge, cy, x - bulge * 0.2f, cy + reach)
    }
    if (softness > 0.15f) {
        drawPath(path, color.copy(alpha = alpha * 0.25f * softness), style = Stroke(7.dp.toPx() * softness, cap = StrokeCap.Round))
    }
    drawPath(path, color.copy(alpha = alpha.coerceAtMost(1f)), style = Stroke((2.2f - softness).dp.toPx(), cap = StrokeCap.Round))
}

private fun DrawScope.drawSpeaker(center: Offset, scale: Float, color: Color) {
    val s = 9.dp.toPx() * scale
    val body = Path().apply {
        moveTo(center.x - s * 1.1f, center.y - s * 0.45f)
        lineTo(center.x - s * 0.4f, center.y - s * 0.45f)
        lineTo(center.x + s * 0.5f, center.y - s * 1.1f)
        lineTo(center.x + s * 0.5f, center.y + s * 1.1f)
        lineTo(center.x - s * 0.4f, center.y + s * 0.45f)
        lineTo(center.x - s * 1.1f, center.y + s * 0.45f)
        close()
    }
    drawPath(body, color.copy(alpha = 0.92f), style = Stroke(1.6.dp.toPx()))
}
