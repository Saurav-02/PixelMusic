package com.saurav.pixelmusic.audiolab

import com.saurav.pixelmusic.audiolab.audio.ReverbParams
import kotlinx.serialization.Serializable

@Serializable
data class ReverbSettings(
    val enabled: Boolean = false,
    val mix: Float = 0.5f,
    val roomSize: Float = 0.55f,
    val damping: Float = 0.45f,
    val wet: Float = 0.6f,
    val dry: Float = 1f,
    val width: Float = 1f,
    val preDelayMs: Float = 12f,
    val lowCutHz: Float = 180f,
    val presetId: String? = ReverbPresets.Default.id,
) {
    fun toParams() = ReverbParams(enabled, mix, roomSize, damping, wet, dry, width, preDelayMs, lowCutHz)

    fun withPreset(preset: ReverbPresets.Preset) = copy(
        mix = preset.mix,
        roomSize = preset.roomSize,
        damping = preset.damping,
        wet = preset.wet,
        dry = 1f,
        width = preset.width,
        preDelayMs = preset.preDelayMs,
        lowCutHz = preset.lowCutHz,
        presetId = preset.id,
    )
}

object ReverbPresets {
    class Preset(
        val id: String,
        val name: String,
        val mix: Float,
        val roomSize: Float,
        val damping: Float,
        val wet: Float,
        val width: Float,
        val preDelayMs: Float,
        val lowCutHz: Float,
    )

    val Default = Preset("room", "Room", 0.5f, 0.55f, 0.45f, 0.6f, 1f, 12f, 180f)

    val All = listOf(
        Default,
        Preset("studio", "Studio", 0.4f, 0.3f, 0.65f, 0.45f, 0.8f, 5f, 250f),
        Preset("bathroom", "Bathroom", 0.5f, 0.42f, 0.05f, 0.7f, 0.6f, 2f, 120f),
        Preset("plate", "Plate", 0.45f, 0.65f, 0.1f, 0.55f, 1f, 0f, 300f),
        Preset("hall", "Hall", 0.5f, 0.78f, 0.35f, 0.6f, 1f, 25f, 150f),
        Preset("cathedral", "Cathedral", 0.55f, 0.95f, 0.2f, 0.7f, 1f, 40f, 120f),
        Preset("stadium", "Stadium", 0.6f, 0.9f, 0.5f, 0.65f, 1f, 60f, 200f),
        Preset("dream", "Dreamy", 0.7f, 0.93f, 0.6f, 0.85f, 1f, 30f, 300f),
    )
}
