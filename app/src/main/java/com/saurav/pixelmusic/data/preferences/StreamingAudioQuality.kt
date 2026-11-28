package com.saurav.pixelmusic.data.preferences

/**
 * Streaming audio quality levels for YouTube playback.
 * Controls the maximum bitrate ceiling when selecting stream formats.
 *
 * AUTO: Dynamically adapts bitrate based on current network speed and stability (Recommended).
 * On WiFi: user's chosen quality is honored.
 * On metered/mobile data: defaults to AUTO / LOW unless overridden.
 *
 * @property maxBitrateKbps Maximum bitrate ceiling in kbps (0 for unconstrained / auto)
 * @property label Human-readable label for Settings UI
 */
enum class StreamingAudioQuality(val maxBitrateKbps: Int, val label: String) {
    AUTO(0, "Auto — Dynamic (Recommended)"),
    LOW(64, "Low (64 kbps) — Saves data"),
    MEDIUM(128, "Medium (128 kbps) — Balanced"),
    HIGH(256, "High (256 kbps) — Best quality");

    companion object {
        fun fromName(name: String?): StreamingAudioQuality {
            return entries.find { it.name == name } ?: AUTO
        }
    }
}
