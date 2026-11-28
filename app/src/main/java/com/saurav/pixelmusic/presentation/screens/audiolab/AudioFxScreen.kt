package com.saurav.pixelmusic.presentation.screens.audiolab

import android.content.Context
import android.media.AudioManager
import android.media.MediaCodecList
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import com.saurav.pixelmusic.R
import com.saurav.pixelmusic.audiolab.MaxSpeed
import com.saurav.pixelmusic.audiolab.MinSpeed
import com.saurav.pixelmusic.audiolab.PlaybackSettings
import com.saurav.pixelmusic.audiolab.ReverbPresets
import com.saurav.pixelmusic.audiolab.audio.ReplayGainMode
import com.saurav.pixelmusic.audiolab.audio.SpatialMode
import com.saurav.pixelmusic.presentation.navigation.Screen
import com.saurav.pixelmusic.presentation.screens.audiolab.stage.StageCard
import com.saurav.pixelmusic.presentation.viewmodel.AudioLabViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.roundToInt

@Composable
fun AudioFxScreen(
    navController: NavController,
    viewModel: AudioLabViewModel = hiltViewModel(),
) {
    ListenToAudio()
    val reverb by viewModel.reverb.collectAsState()
    val loudness by viewModel.loudness.collectAsState()
    val spatial by viewModel.spatial.collectAsState()
    val eightD by viewModel.eightD.collectAsState()
    val playback by viewModel.playback.collectAsState()
    val volume by viewModel.volume.collectAsState()
    val stage by viewModel.stage.collectAsState()
    val effects = activeEffects(reverb, loudness, spatial, eightD, playback, volume, stage)

    AudioLabPage(
        title = LabTitle(stringResource(R.string.fx_title_audio), "FX", firstSize = 40.sp, secondSize = 84.sp, firstDim = true),
        onBack = { navController.navigateUp() },
        art = { hidden -> FxCardStack(effects, hidden) },
    ) {
        FxPresetsSection(viewModel)
        Spacer(Modifier.height(18.dp))
        HighlightableLabItem(matchTitles = listOf("3D Sims")) {
            StageCard(viewModel) { navController.navigate(Screen.AudioLabStage.route) }
        }
        Spacer(Modifier.height(14.dp))
        HighlightableLabItem(matchTitles = listOf("Reverb")) {
            ReverbCard(viewModel)
        }
        Spacer(Modifier.height(14.dp))
        HighlightableLabItem(matchTitles = listOf("8D Audio")) {
            EightDAudioCard(viewModel)
        }
        Spacer(Modifier.height(14.dp))
        HighlightableLabItem(matchTitles = listOf("Loudness & Compression")) {
            LoudnessCard(viewModel)
        }
        Spacer(Modifier.height(14.dp))
        HighlightableLabItem(matchTitles = listOf("Spatial Audio & Crossfeed")) {
            SpatialCard(viewModel)
        }
        Spacer(Modifier.height(14.dp))
        HighlightableLabItem(matchTitles = listOf("Mono Audio")) {
            MonoCard(viewModel)
        }
        Spacer(Modifier.height(14.dp))
        HighlightableLabItem(matchTitles = listOf("Dolby Atmos & Surround")) {
            AtmosCard(viewModel = viewModel)
        }
        Spacer(Modifier.height(14.dp))
        HighlightableLabItem(matchTitles = listOf("Playback Speed & Pitch")) {
            SpeedCard(viewModel)
        }
        Spacer(Modifier.height(14.dp))
        HighlightableLabItem(matchTitles = listOf("Exponential Volume")) {
            VolumeCard(viewModel)
        }
    }
}

@Composable
internal fun EffectCard(
    picture: @Composable () -> Unit,
    header: @Composable () -> Unit,
    expanded: Boolean,
    options: @Composable ColumnScope.() -> Unit,
    footer: (@Composable ColumnScope.() -> Unit)? = null,
) {
    LabCard(padding = 14.dp) {
        LabStage(height = 132.dp) { picture() }
        Spacer(Modifier.height(10.dp))
        header()
        LabCollapsible(expanded) {
            Spacer(Modifier.height(10.dp))
            options()
        }
        footer?.invoke(this)
    }
}

internal fun pct(v: Float) = "${(v * 100).roundToInt()}%"

@Composable
private fun LoudnessCard(viewModel: AudioLabViewModel) {
    val loudness by viewModel.loudness.collectAsState()
    val normalize by viewModel.normalizeEnabled.collectAsState()
    val onNormalize: (Boolean) -> Unit = viewModel::setNormalizeEnabled
    EffectCard(
        picture = { LoudnessPicture(loudness, viewModel.fx) },
        header = {
            LabToggleRow(stringResource(R.string.fx_loudness_title), stringResource(R.string.fx_loudness_sub), loudness.enabled,
                { on -> viewModel.updateLoudness { it.copy(enabled = on) } })
        },
        expanded = loudness.enabled,
        options = {
            LabChips(
                chips = listOf(
                    LabChip("gentle", stringResource(R.string.fx_loudness_gentle)),
                    LabChip("punchy", stringResource(R.string.fx_loudness_punchy)),
                    LabChip("night", stringResource(R.string.fx_loudness_night)),
                    LabChip("max", stringResource(R.string.fx_loudness_max)),
                ),
                selectedKey = when {
                    loudness.boostDb == 2f && loudness.compression == 0.15f -> "gentle"
                    loudness.boostDb == 4f && loudness.compression == 0.3f -> "punchy"
                    loudness.boostDb == 3f && loudness.compression == 0.85f -> "night"
                    loudness.boostDb == 9f && loudness.compression == 0.5f -> "max"
                    else -> null
                },
                onSelect = { key ->
                    val (boost, comp) = when (key) {
                        "gentle" -> 2f to 0.15f
                        "night" -> 3f to 0.85f
                        "max" -> 9f to 0.5f
                        else -> 4f to 0.3f
                    }
                    viewModel.updateLoudness { it.copy(boostDb = boost, compression = comp) }
                },
            )
            Spacer(Modifier.height(10.dp))
            LabPanel {
                LabSlider(stringResource(R.string.fx_boost), "+${String.format("%.1f", loudness.boostDb)} dB", loudness.boostDb,
                    { v -> viewModel.updateLoudness { it.copy(boostDb = (v * 2).roundToInt() / 2f) } }, 0f..12f)
                LabSlider(stringResource(R.string.fx_compression), pct(loudness.compression), loudness.compression,
                    { v -> viewModel.updateLoudness { it.copy(compression = (v * 100).roundToInt() / 100f) } }, 0f..1f)
                Text(
                    stringResource(R.string.fx_compression_note),
                    fontFamily = InterFontFamily, fontSize = 12.sp, lineHeight = 16.sp, color = LabColors.Secondary,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        },
        footer = {
            Spacer(Modifier.height(6.dp))
            LabToggleRow(stringResource(R.string.fx_even_out), stringResource(R.string.fx_even_out_sub), normalize, onNormalize)
            val rg by viewModel.normalize.collectAsState()
            LabCollapsible(normalize) {
                Spacer(Modifier.height(4.dp))
                LabPanel {
                    Text(
                        stringResource(R.string.fx_replaygain),
                        fontFamily = InterFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 11.sp, letterSpacing = 1.3.sp,
                        color = LabColors.Secondary,
                    )
                    LabChips(
                        chips = listOf(LabChip("track", stringResource(R.string.fx_rg_track)), LabChip("album", stringResource(R.string.fx_rg_album))),
                        selectedKey = if (rg.replayGainMode == ReplayGainMode.Album) "album" else "track",
                        onSelect = { key ->
                            viewModel.updateNormalize {
                                it.copy(replayGainMode = if (key == "album") ReplayGainMode.Album else ReplayGainMode.Track)
                            }
                        },
                    )
                    LabSlider(stringResource(R.string.fx_preamp), (if (rg.preampDb > 0) "+" else "") + String.format("%.1f dB", rg.preampDb), rg.preampDb,
                        { v -> viewModel.updateNormalize { it.copy(preampDb = (v * 2).roundToInt() / 2f) } }, -6f..6f)
                    Text(
                        stringResource(R.string.fx_replaygain_note),
                        fontFamily = InterFontFamily, fontSize = 12.sp, lineHeight = 16.sp, color = LabColors.Secondary,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
        },
    )
}

private class CrossfeedLevel(val key: String, @StringRes val label: Int, val cutoff: Float, val feed: Float)

private val CrossfeedLevels = listOf(
    CrossfeedLevel("light", R.string.fx_xf_light, 700f, 4.5f),
    CrossfeedLevel("natural", R.string.fx_xf_natural, 700f, 6f),
    CrossfeedLevel("strong", R.string.fx_xf_strong, 650f, 9.5f),
)

@Composable
private fun SpatialCard(viewModel: AudioLabViewModel) {
    val spatial by viewModel.spatial.collectAsState()
    val stage by viewModel.stage.collectAsState()
    EffectCard(
        picture = { SpatialPicture(spatial) },
        header = {
            LabToggleRow(stringResource(R.string.fx_spatial_title), stringResource(R.string.fx_spatial_sub), spatial.enabled,
                { on -> viewModel.updateSpatial { it.copy(enabled = on) } })
        },
        expanded = spatial.enabled,
        options = {
            LabSegmented(
                options = listOf(stringResource(R.string.fx_mode_crossfeed), stringResource(R.string.fx_mode_speakers)),
                selectedIndex = if (spatial.mode == SpatialMode.Crossfeed) 0 else 1,
                onSelect = { i -> viewModel.updateSpatial { it.copy(mode = if (i == 0) SpatialMode.Crossfeed else SpatialMode.Speakers) } },
                height = 42.dp,
            )
            Spacer(Modifier.height(10.dp))
            if (spatial.mode == SpatialMode.Crossfeed) {
                LabChips(
                    chips = CrossfeedLevels.map { LabChip(it.key, stringResource(it.label)) },
                    selectedKey = CrossfeedLevels.firstOrNull { it.cutoff == spatial.cutoffHz && it.feed == spatial.feedDb }?.key,
                    onSelect = { key ->
                        CrossfeedLevels.firstOrNull { it.key == key }?.let { l ->
                            viewModel.updateSpatial { it.copy(cutoffHz = l.cutoff, feedDb = l.feed) }
                        }
                    },
                )
                Spacer(Modifier.height(10.dp))
                LabPanel {
                    LabSlider(stringResource(R.string.fx_head_shadow), "${spatial.cutoffHz.roundToInt()} Hz", spatial.cutoffHz,
                        { v -> viewModel.updateSpatial { it.copy(cutoffHz = (v / 10).roundToInt() * 10f) } }, 300f..1500f)
                    LabSlider(stringResource(R.string.fx_mode_crossfeed), "−${String.format("%.1f", spatial.feedDb)} dB", 16f - spatial.feedDb,
                        { v -> viewModel.updateSpatial { it.copy(feedDb = ((16f - v) * 2).roundToInt() / 2f) } }, 1f..15f)
                    LabSlider(stringResource(R.string.fx_amount), pct(spatial.amount), spatial.amount,
                        { v -> viewModel.updateSpatial { it.copy(amount = v) } }, 0f..1f)
                }
            } else {
                LabPanel {
                    LabSlider(stringResource(R.string.fx_speaker_angle), "±${spatial.speakerAngle.roundToInt()}°", spatial.speakerAngle,
                        { v -> viewModel.updateSpatial { it.copy(speakerAngle = v.roundToInt().toFloat()) } }, 10f..90f)
                    LabSlider(stringResource(R.string.fx_amount), pct(spatial.amount), spatial.amount,
                        { v -> viewModel.updateSpatial { it.copy(amount = v) } }, 0f..1f)
                    Text(
                        stringResource(R.string.fx_speakers_note),
                        fontFamily = InterFontFamily, fontSize = 12.sp, lineHeight = 16.sp, color = LabColors.Secondary,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
        },
        footer = if (stage.enabled && spatial.enabled) {
            {
                Text(
                    stringResource(R.string.stage_spatial_note),
                    fontFamily = InterFontFamily, fontSize = 12.sp, lineHeight = 16.sp, color = LabColors.Secondary,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp),
                )
            }
        } else null,
    )
}

@Composable
private fun MonoCard(viewModel: AudioLabViewModel) {
    val spatial by viewModel.spatial.collectAsState()
    LabCard(padding = 10.dp) {
        LabToggleRow(
            title = stringResource(R.string.fx_mono_title),
            subtitle = stringResource(R.string.fx_mono_sub),
            checked = spatial.mono,
            onCheckedChange = { on -> viewModel.updateSpatial { it.copy(mono = on) } },
            leadingDot = true,
        )
    }
}

@Composable
private fun AtmosCard(viewModel: AudioLabViewModel) {
    val spatial by viewModel.spatial.collectAsState()
    val context = LocalContext.current
    var device by remember { mutableStateOf<AtmosSupport?>(null) }
    LaunchedEffect(Unit) { device = withContext(Dispatchers.Default) { atmosSupport(context) } }

    EffectCard(
        picture = { AtmosPicture(enabled = spatial.surround, renderOnHeadphones = spatial.surround) },
        header = {
            LabToggleRow(
                title = stringResource(R.string.fx_atmos_title),
                subtitle = stringResource(R.string.fx_atmos_sub),
                checked = spatial.surround,
                onCheckedChange = { on -> viewModel.updateSpatial { it.copy(surround = on) } },
            )
        },
        expanded = false,
        options = {},
        footer = {
            Text(
                stringResource(R.string.atmos_streaming_note),
                fontFamily = InterFontFamily, fontSize = 12.sp, lineHeight = 16.sp, color = LabColors.Secondary,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp),
            )
            device?.let { support ->
                Spacer(Modifier.height(10.dp))
                Row(Modifier.padding(horizontal = 6.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    SupportPill(stringResource(R.string.fx_atmos_decoder), stringResource(support.decoder))
                    SupportPill(stringResource(R.string.fx_atmos_spatializer), stringResource(support.spatializer))
                }
            }
        },
    )
}

private class AtmosSupport(@StringRes val decoder: Int, @StringRes val spatializer: Int)

private fun atmosSupport(context: Context): AtmosSupport {
    val decoder = runCatching {
        MediaCodecList(MediaCodecList.ALL_CODECS).codecInfos.any { info ->
            !info.isEncoder && info.supportedTypes.any { it.equals("audio/eac3-joc", ignoreCase = true) }
        }
    }.getOrDefault(false)
    val spatializer = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S_V2) spatializerState(context) else R.string.fx_atmos_needs_android
    return AtmosSupport(if (decoder) R.string.fx_atmos_yes else R.string.fx_atmos_not_on_phone, spatializer)
}

@RequiresApi(Build.VERSION_CODES.S_V2)
@StringRes
private fun spatializerState(context: Context): Int = runCatching {
    val manager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    val spatializer = manager.spatializer
    when {
        !spatializer.isAvailable -> R.string.fx_state_not_available
        spatializer.isEnabled -> R.string.fx_state_on
        else -> R.string.fx_state_off
    }
}.getOrDefault(R.string.fx_state_unknown)

@Composable
private fun SupportPill(label: String, value: String) {
    Column {
        Text(label.uppercase(), fontFamily = InterFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 10.sp,
            letterSpacing = 1.sp, color = LabColors.Faint)
        Text(value, fontFamily = InterFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = LabColors.Secondary)
    }
}

private class SpeedPreset(val key: String, @StringRes val labelRes: Int?, val rawLabel: String?, val speed: Float, val preservePitch: Boolean, val withReverb: Boolean = false)

private val SpeedPresets = listOf(
    SpeedPreset("normal", R.string.fx_speed_normal, null, 1f, true),
    SpeedPreset("slowed", R.string.fx_speed_slowed, null, 0.85f, false),
    SpeedPreset("slowed_reverb", R.string.fx_speed_slowed_reverb, null, 0.85f, false, withReverb = true),
    SpeedPreset("nightcore", R.string.fx_speed_nightcore, null, 1.25f, false),
    SpeedPreset("0.75", null, "0.75×", 0.75f, true),
    SpeedPreset("1.25", null, "1.25×", 1.25f, true),
    SpeedPreset("1.5", null, "1.5×", 1.5f, true),
    SpeedPreset("2", null, "2×", 2f, true),
)

@Composable
private fun SpeedCard(viewModel: AudioLabViewModel) {
    val playback by viewModel.playback.collectAsState()
    val reverb by viewModel.reverb.collectAsState()
    EffectCard(
        picture = { SpeedPicture(playback) },
        header = {
            Row(Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.fx_speed_title), fontFamily = InterFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = LabColors.Primary)
                    Text(
                        stringResource(if (playback.preservePitch) R.string.fx_pitch_stays else R.string.fx_pitch_follows),
                        fontFamily = InterFontFamily, fontSize = 12.sp, color = LabColors.Secondary,
                    )
                }
                StepButton("−") { viewModel.updatePlayback { it.copy(speed = ((it.speed - 0.05f) * 100).roundToInt() / 100f) } }
                Text(
                    String.format("%.2f×", playback.speed),
                    fontFamily = InterFontFamily, fontWeight = FontWeight.Bold, fontSize = 17.sp, color = LabColors.Primary,
                    modifier = Modifier.padding(horizontal = 10.dp),
                )
                StepButton("+") { viewModel.updatePlayback { it.copy(speed = ((it.speed + 0.05f) * 100).roundToInt() / 100f) } }
            }
        },
        expanded = true,
        options = {
            LabChips(
                chips = SpeedPresets.map { LabChip(it.key, it.labelRes?.let { id -> stringResource(id) } ?: it.rawLabel.orEmpty()) },
                selectedKey = if (playback.speed == 0.85f && !playback.preservePitch) {
                    if (reverb.enabled) "slowed_reverb" else "slowed"
                } else {
                    SpeedPresets.firstOrNull { p ->
                        !p.withReverb && p.speed == playback.speed && p.preservePitch == playback.preservePitch &&
                            (!p.preservePitch || playback.pitchSemitones == 0f)
                    }?.key
                },
                onSelect = { key ->
                    val p = SpeedPresets.first { it.key == key }
                    viewModel.updatePlayback { it.copy(speed = p.speed, preservePitch = p.preservePitch, pitchSemitones = 0f) }
                    if (p.withReverb) {
                        ReverbPresets.All.firstOrNull { it.id == "hall" }?.let { hall ->
                            viewModel.updateReverb { it.withPreset(hall).copy(enabled = true) }
                        }
                    } else if (key == "slowed" && reverb.enabled) {
                        viewModel.updateReverb { it.copy(enabled = false) }
                    }
                },
            )
            Spacer(Modifier.height(10.dp))
            LabPanel {
                LabSlider(
                    stringResource(R.string.fx_speed), String.format("%.2f×", playback.speed), log10(playback.speed),
                    { v -> viewModel.updatePlayback { it.copy(speed = (10f.pow(v) * 100).roundToInt() / 100f) } },
                    log10(MinSpeed)..log10(MaxSpeed),
                )
                LabToggleRow(stringResource(R.string.fx_preserve_pitch), stringResource(R.string.fx_preserve_pitch_sub), playback.preservePitch,
                    { on -> viewModel.updatePlayback { it.copy(preservePitch = on) } })
                LabCollapsible(playback.preservePitch) {
                    LabSlider(
                        stringResource(R.string.fx_pitch),
                        stringResource(R.string.fx_semitones, (if (playback.pitchSemitones > 0) "+" else "") + playback.pitchSemitones.roundToInt()),
                        playback.pitchSemitones,
                        { v -> viewModel.updatePlayback { it.copy(pitchSemitones = v.roundToInt().toFloat()) } },
                        -12f..12f,
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                LabTextAction(stringResource(R.string.fx_reset), R.drawable.ic_lab_refresh, enabled = !playback.isNormal) {
                    viewModel.updatePlayback { PlaybackSettings() }
                }
            }
        },
    )
}

@Composable
private fun StepButton(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier.size(32.dp).clip(CircleShape).background(LabColors.Row).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, fontFamily = InterFontFamily, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = LabColors.Primary)
    }
}

private class CurveShape(val key: String, @StringRes val label: Int, val exponent: Float)

private val CurveShapes = listOf(
    CurveShape("gentle", R.string.fx_curve_gentle, 1.6f),
    CurveShape("exponential", R.string.fx_curve_exponential, 2.2f),
    CurveShape("steep", R.string.fx_curve_steep, 3.2f),
)

@Composable
private fun VolumeCard(viewModel: AudioLabViewModel) {
    val volume by viewModel.volume.collectAsState()
    EffectCard(
        picture = { VolumeCurvePicture(volume, 0.7f) },
        header = {
            LabToggleRow(stringResource(R.string.fx_volume_title), stringResource(R.string.fx_volume_sub), volume.enabled,
                { on -> viewModel.updateVolume { it.copy(enabled = on) } })
        },
        expanded = volume.enabled,
        options = {
            LabChips(
                chips = CurveShapes.map { LabChip(it.key, stringResource(it.label)) },
                selectedKey = CurveShapes.firstOrNull { it.exponent == volume.exponent }?.key,
                onSelect = { key -> CurveShapes.firstOrNull { it.key == key }?.let { s -> viewModel.updateVolume { it.copy(exponent = s.exponent) } } },
            )
            Spacer(Modifier.height(10.dp))
            LabPanel {
                LabSlider(stringResource(R.string.fx_curve), String.format("x^%.1f", volume.exponent), volume.exponent,
                    { v -> viewModel.updateVolume { it.copy(exponent = (v * 10).roundToInt() / 10f) } }, 1f..4f)
                LabSlider(stringResource(R.string.fx_volume_limit), pct(volume.limit), volume.limit,
                    { v -> viewModel.updateVolume { it.copy(limit = (v * 100).roundToInt() / 100f) } }, 0.2f..1f)
                Text(
                    stringResource(R.string.fx_volume_note),
                    fontFamily = InterFontFamily, fontSize = 12.sp, lineHeight = 16.sp, color = LabColors.Secondary,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        },
    )
}
