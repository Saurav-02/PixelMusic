package com.saurav.pixelmusic.audiolab

import com.saurav.pixelmusic.audiolab.audio.SpatialMode
import com.saurav.pixelmusic.audiolab.stage.StageSettings
import kotlinx.serialization.Serializable

/** A saved whole-chain Audio FX preset: every effect on the Audio FX screen captured under one name. */
@Serializable
data class FxPreset(
    val id: String,
    val name: String,
    val reverb: ReverbSettings = ReverbSettings(),
    val eightD: EightDSettings = EightDSettings(),
    val loudness: LoudnessSettings = LoudnessSettings(),
    val spatial: SpatialSettings = SpatialSettings(),
    val playback: PlaybackSettings = PlaybackSettings(),
    val volume: VolumeSettings = VolumeSettings(),
    // Saved without per-device speaker overrides so phone output settings stay intact when switching presets
    val stage: StageSettings = StageSettings(),
)

object FxPresets {
    const val OffId = "builtin_fx_off"

    private val hallReverb = ReverbPresets.All.firstOrNull { it.id == "hall" } ?: ReverbPresets.Default

    val BuiltIn: List<FxPreset> = listOf(
        FxPreset(
            id = OffId,
            name = "Off",
        ),
        FxPreset(
            id = "builtin_fx_punchy",
            name = "Punchy",
            loudness = LoudnessSettings(enabled = true, boostDb = 4f, compression = 0.3f),
        ),
        FxPreset(
            id = "builtin_fx_concert",
            name = "Concert Hall",
            reverb = ReverbSettings(enabled = true).withPreset(hallReverb),
            loudness = LoudnessSettings(enabled = true, boostDb = 2f, compression = 0.15f),
        ),
        FxPreset(
            id = "builtin_fx_studio",
            name = "Studio",
            spatial = SpatialSettings(enabled = true, mode = SpatialMode.Speakers, speakerAngle = 30f, amount = 0.85f),
        ),
        FxPreset(
            id = "builtin_fx_night",
            name = "Night",
            loudness = LoudnessSettings(enabled = true, boostDb = 3f, compression = 0.85f),
        ),
        FxPreset(
            id = "builtin_fx_8d",
            name = "8D Surround",
            eightD = EightDSettings(enabled = true, rotationSpeedHz = 0.15f, depth = 0.85f, reverbLevel = 0.4f),
        ),
    )

    fun isBuiltIn(id: String?): Boolean = id != null && BuiltIn.any { it.id == id }
}
