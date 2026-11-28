package com.saurav.pixelmusic.data.diagnostics

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.Locale

/**
 * Structured, privacy-safe performance & diagnostic report for PixelMusic.
 *
 * Includes automated health scoring, bottleneck classification, live UI frame-time
 * probing, memory/heap/thermal telemetry, audio pipeline latency & decoder checks,
 * Coil cache metrics, and opt-in lag-marker capture.
 *
 * Privacy guarantee: contains only aggregates, counts, device capabilities, and
 * technical format metadata — never file paths, song titles, or artist names.
 */
@Serializable
data class PerformanceReport(
    val schemaVersion: Int = SCHEMA_VERSION,
    val generatedAtIso: String,
    val analysis: PerformanceAnalysisSection,
    val device: DeviceRuntimeSection,
    val app: AppGraphicsSection,
    val library: LibraryHiResSection,
    val audio: AudioPlaybackSection,
    val cache: CacheSection,
    val uiPerformance: UiPerformanceSection,
    val advancedDiagnostics: AdvancedDiagnosticsSection = AdvancedDiagnosticsSection(),
    val notes: List<String> = DEFAULT_NOTES
) {
    fun toJson(): String = PRETTY_JSON.encodeToString(this)

    fun toPlainText(): String = buildString {
        appendLine("PixelMusic Performance & Diagnostics Report (schema v$schemaVersion)")
        appendLine("Generated: $generatedAtIso")
        appendLine()

        section("EXECUTIVE ANALYSIS")
        kv("Health score", "${analysis.healthScore}/100 (${analysis.grade})")
        kv("Verdict", analysis.summary)
        if (analysis.findings.isNotEmpty()) {
            appendLine("  Findings:")
            analysis.findings.forEach { finding ->
                appendLine("    [${finding.severity}] (${finding.category}) ${finding.title}")
                appendLine("      Detail: ${finding.detail}")
                if (finding.recommendation.isNotBlank()) {
                    appendLine("      Action: ${finding.recommendation}")
                }
            }
        }

        section("DEVICE & RUNTIME")
        kv("Manufacturer", device.manufacturer)
        kv("Model", "${device.model} (${device.device})")
        kv("Brand", device.brand)
        kv("Hardware / SoC", device.hardware)
        kv("Android", "${device.androidVersion} (SDK ${device.sdkInt})")
        kv("ABIs", device.supportedAbis.joinToString(", "))
        kv("CPU cores", device.cpuCores.toString())
        kv("Display refresh rate", "${fmt1(device.displayRefreshRateHz)} Hz")
        kv("Thermal status", device.thermalStatus)
        kv("Power save mode", device.isPowerSaveMode.toString())
        kv("RAM class", "${device.memoryClassMb} MB")
        kv(
            "System RAM",
            "${bytes(device.availableRamBytes)} free / ${bytes(device.totalRamBytes)} total (${fmt1(device.ramUsedPercent)}% used)"
        )
        kv("Low-RAM device / Low memory", "${device.isLowRamDevice} / ${device.isSystemLowMemory}")
        kv(
            "JVM Heap",
            "${bytes(device.jvmHeapUsedBytes)} / ${bytes(device.jvmHeapMaxBytes)} (${fmt1(device.jvmHeapUsedPercent)}%)"
        )
        kv("Native Heap allocated", bytes(device.nativeHeapAllocatedBytes))

        section("APP & GRAPHICS")
        kv("Version", "${app.versionName} (${app.versionCode})")
        kv("Build type", app.buildType)
        kv("Application id", app.applicationId)
        kv("Media3 / ExoPlayer", app.media3Version)
        kv("Hardware blur (API 31+)", app.supportsHardwareBlur.toString())
        kv("AGSL shaders (API 33+)", app.supportsAgslShaders.toString())
        kv("Spatializer (API 32+)", app.supportsSpatializer.toString())
        kv("Motion blur enabled", app.motionBlurEnabled.toString())
        kv("Animated lyrics blur", app.animatedLyricsBlurEnabled.toString())
        kv("Performance mode", app.performanceModeEnabled.toString())
        kv("Album art quality", app.albumArtQuality)
        kv("Player design style", app.playerDesignStyle)

        section("UI FRAME PROBE")
        kv("Target frame budget", "${fmt2(uiPerformance.targetFrameBudgetMs)} ms (${fmt1(uiPerformance.refreshRateHz)} Hz)")
        if (uiPerformance.sampledFrames > 0) {
            kv("Sampled frames", uiPerformance.sampledFrames.toString())
            kv("Avg / P95 / Worst frame", "${fmt2(uiPerformance.avgFrameMs)} / ${fmt2(uiPerformance.p95FrameMs)} / ${fmt2(uiPerformance.worstFrameMs)} ms")
            kv("Jank frames", "${uiPerformance.jankFrames} (${fmt1(uiPerformance.jankPercent)}%)")
        } else {
            kv("Probe status", "not run")
        }

        section("LIBRARY & HI-RES")
        kv("Total songs", library.totalSongs.toString())
        kv("Local / Cloud songs", "${library.localSongs} / ${library.cloudSongs}")
        kv("Unavailable local files", library.unavailableLocalFiles.toString())
        kv("Local music storage", "${bytes(library.localMusicBytes)} (${bytes(library.deviceAvailableBytes)} device free)")
        kv("Max bitrate", library.maxBitrate?.let { "${it / 1000} kbps" } ?: UNKNOWN)
        kv("Sample rate range", sampleRateRange(library.minSampleRate, library.maxSampleRate))
        kv("Est. file size min/avg/max", estSizes(library))
        kv("Hi-res (>48 kHz)", library.hiResCount.toString())
        kv("Ultra-hi-res (>=176.4 kHz)", library.ultraHiResCount.toString())
        kv("Resampled (>native output)", library.resampledCount.toString())
        kv("Lossless-codec files", library.losslessCodecCount.toString())
        kv("Likely expensive to decode", library.likelyExpensiveToDecodeCount.toString())
        kv("Unsupported format tracks", library.unsupportedFormatCount.toString())
        if (library.unsupportedFormats.isNotEmpty()) {
            kv("Unsupported MIME types", library.unsupportedFormats.joinToString(", "))
        }
        if (library.mimeCounts.isNotEmpty()) {
            appendLine("  MIME/type counts:")
            library.mimeCounts.entries.sortedByDescending { it.value }.forEach { (mime, count) ->
                appendLine("    $mime: $count")
            }
        }

        section("AUDIO ENGINE & PLAYBACK")
        kv("Native output", "${audio.nativeOutputSampleRate} Hz / ${audio.nativeFramesPerBuffer} frames (~${fmt2(audio.nativeBufferLatencyMs)} ms)")
        kv("Low-latency / Pro Audio / PCM Float", "${audio.isLowLatencySupported} / ${audio.isProAudioSupported} / ${audio.isPcmFloatSupported}")
        kv("Installed decoders", "${audio.hardwareCodecCount} hardware, ${audio.softwareCodecCount} software")
        if (audio.outputRoutes.isNotEmpty()) {
            kv("Active output routes", audio.outputRoutes.joinToString(", "))
        }
        if (audio.offloadSupportedFormats.isNotEmpty()) {
            kv("Offload-capable formats", audio.offloadSupportedFormats.joinToString(", "))
        }
        kv("Current MIME", audio.currentMime ?: NOT_PLAYING)
        kv("Sample rate", audio.sampleRate?.let { "$it Hz" } ?: NOT_PLAYING)
        kv("Bitrate", audio.bitrate?.let { "${it / 1000} kbps" } ?: NOT_PLAYING)
        kv("Channels", audio.channelCount?.toString() ?: NOT_PLAYING)
        kv("PCM encoding", audio.pcmEncoding?.let(::pcmEncodingLabel) ?: NOT_PLAYING)
        kv("Active decoder", audio.decoderName ?: UNKNOWN)
        kv("Decoder class", audio.decoderHardware?.let { if (it) "hardware" else "software" } ?: UNKNOWN)
        kv("Audio offload enabled", audio.audioOffloadEnabled.toString())
        kv("Hi-Fi mode", audio.hiFiModeEnabled.toString())
        kv("Crossfade", if (audio.crossfadeEnabled) "on (${audio.crossfadeDurationMs} ms)" else "off")
        kv("Equalizer", audio.equalizerType)
        kv("AudioLab DSP / EQ active", audio.audioLabEffectsActive.toString())
        kv("ReplayGain", if (audio.replayGainEnabled) "on (${if (audio.replayGainAlbumMode) "album" else "track"})" else "off")

        section("IMAGE & CACHE")
        kv("Coil memory cache", "${bytes(cache.coilMemoryUsedBytes)} / ${bytes(cache.coilMemoryMaxBytes)} (${fmt1(cache.coilMemoryFillPercent)}%)")
        kv("Coil disk cache", "${bytes(cache.coilDiskUsedBytes)} / ${bytes(cache.coilDiskMaxBytes)} (${fmt1(cache.coilDiskFillPercent)}%)")
        kv("App cache directory", bytes(cache.appCacheDirBytes))

        if (advancedDiagnostics.enabled || advancedDiagnostics.events.isNotEmpty()) {
            section("LIVE DIAGNOSTICS & LAG MARKS")
            kv("Enabled", advancedDiagnostics.enabled.toString())
            kv("Started", advancedDiagnostics.sessionStartedIso ?: NOT_OBSERVED)
            kv("Expires", advancedDiagnostics.expiresAtIso ?: NOT_OBSERVED)
            kv("Events retained / dropped", "${advancedDiagnostics.eventCount} / ${advancedDiagnostics.droppedEventCount}")
            advancedDiagnostics.events.forEach { event ->
                val details = event.details.entries.joinToString(", ") { (k, v) -> "$k=$v" }
                appendLine(
                    buildString {
                        append("  [+${event.elapsedRealtimeMs} ms] ${event.type}/${event.name}")
                        if (details.isNotBlank()) append(" ($details)")
                    }
                )
            }
        }

        if (notes.isNotEmpty()) {
            section("NOTES")
            notes.forEach { appendLine("  - $it") }
        }
    }

    fun toFullShareableReport(): String = buildString {
        append(toPlainText())
        appendLine()
        appendLine()
        appendLine("--- JSON ---")
        append(toJson())
    }

    private fun StringBuilder.section(title: String) {
        appendLine()
        appendLine("== $title ==")
    }

    private fun StringBuilder.kv(key: String, value: String) {
        appendLine("  $key: $value")
    }

    companion object {
        const val SCHEMA_VERSION = 3
        private const val UNKNOWN = "unknown"
        private const val NOT_OBSERVED = "not observed"
        private const val NOT_PLAYING = "not playing"

        private val PRETTY_JSON = Json {
            prettyPrint = true
            encodeDefaults = true
        }

        val DEFAULT_NOTES = listOf(
            "Health score (0-100) evaluates system RAM pressure, JVM heap, thermal throttling, live UI frame jank, audio resampling/decoder path, and library integrity.",
            "Hi-res = sample rate > 48 kHz; ultra-hi-res = sample rate >= 176.4 kHz.",
            "UI frame probe measures real Choreographer frame intervals over ~1.5s against the display's native refresh budget.",
            "This report contains no file paths, song titles, or artist names and is safe to share."
        )

        fun bytes(value: Long): String = when {
            value >= 1_000_000_000 -> "%.2f GB".format(Locale.ROOT, value / 1_000_000_000.0)
            value >= 1_000_000 -> "%.2f MB".format(Locale.ROOT, value / 1_000_000.0)
            value >= 1_000 -> "%.1f KB".format(Locale.ROOT, value / 1_000.0)
            else -> "$value B"
        }

        fun pcmEncodingLabel(encoding: Int): String = when (encoding) {
            2 -> "16-bit"
            3 -> "8-bit"
            4 -> "32-bit float"
            21 -> "24-bit"
            22 -> "32-bit"
            536870912 -> "16-bit big-endian"
            else -> "encoding=$encoding"
        }

        private fun fmt1(value: Double): String = "%.1f".format(Locale.ROOT, value)
        private fun fmt2(value: Double): String = "%.2f".format(Locale.ROOT, value)

        private fun sampleRateRange(min: Int?, max: Int?): String = when {
            min == null && max == null -> UNKNOWN
            min == max -> "${max ?: UNKNOWN} Hz"
            else -> "${min ?: "?"}-${max ?: "?"} Hz"
        }

        private fun estSizes(library: LibraryHiResSection): String {
            val min = library.estFileSizeMinBytes
            val avg = library.estFileSizeAvgBytes
            val max = library.estFileSizeMaxBytes
            if (min == null && avg == null && max == null) return UNKNOWN
            return "${min?.let(::bytes) ?: "?"} / ${avg?.let { bytes(it.toLong()) } ?: "?"} / ${max?.let(::bytes) ?: "?"}"
        }
    }
}

@Serializable
data class PerformanceAnalysisSection(
    val healthScore: Int,
    val grade: String,
    val summary: String,
    val findings: List<DiagnosticFinding>
)

@Serializable
data class DiagnosticFinding(
    val severity: String,
    val category: String,
    val title: String,
    val detail: String,
    val recommendation: String = ""
)

@Serializable
data class DeviceRuntimeSection(
    val manufacturer: String,
    val model: String,
    val brand: String,
    val device: String,
    val hardware: String,
    val androidVersion: String,
    val sdkInt: Int,
    val supportedAbis: List<String>,
    val cpuCores: Int,
    val displayRefreshRateHz: Double,
    val thermalStatus: String,
    val isPowerSaveMode: Boolean,
    val memoryClassMb: Int,
    val isLowRamDevice: Boolean,
    val isSystemLowMemory: Boolean,
    val totalRamBytes: Long,
    val availableRamBytes: Long,
    val ramUsedPercent: Double,
    val jvmHeapUsedBytes: Long,
    val jvmHeapMaxBytes: Long,
    val jvmHeapUsedPercent: Double,
    val nativeHeapAllocatedBytes: Long
)

@Serializable
data class AppGraphicsSection(
    val versionName: String,
    val versionCode: Long,
    val buildType: String,
    val applicationId: String,
    val media3Version: String,
    val supportsHardwareBlur: Boolean,
    val supportsAgslShaders: Boolean,
    val supportsSpatializer: Boolean,
    val motionBlurEnabled: Boolean,
    val playerDesignStyle: String,
    val performanceModeEnabled: Boolean = false,
    val animatedLyricsBlurEnabled: Boolean = false,
    val albumArtQuality: String = "HIGH"
)

@Serializable
data class LibraryHiResSection(
    val totalSongs: Int,
    val localSongs: Int,
    val cloudSongs: Int,
    val knownLocalFiles: Int,
    val unavailableLocalFiles: Int,
    val localMusicBytes: Long,
    val deviceAvailableBytes: Long,
    val deviceTotalBytes: Long,
    val mimeCounts: Map<String, Int>,
    val maxBitrate: Int?,
    val minSampleRate: Int?,
    val maxSampleRate: Int?,
    val estFileSizeMinBytes: Long?,
    val estFileSizeAvgBytes: Double?,
    val estFileSizeMaxBytes: Long?,
    val hiResCount: Int,
    val ultraHiResCount: Int,
    val resampledCount: Int,
    val losslessCodecCount: Int,
    val likelyExpensiveToDecodeCount: Int,
    val unsupportedFormatCount: Int,
    val unsupportedFormats: List<String>
)

@Serializable
data class AudioPlaybackSection(
    val nativeOutputSampleRate: Int,
    val nativeFramesPerBuffer: Int,
    val nativeBufferLatencyMs: Double,
    val isLowLatencySupported: Boolean,
    val isProAudioSupported: Boolean,
    val isPcmFloatSupported: Boolean,
    val hardwareCodecCount: Int,
    val softwareCodecCount: Int,
    val outputRoutes: List<String>,
    val offloadSupportedFormats: List<String>,
    val currentMime: String?,
    val sampleRate: Int?,
    val bitrate: Int?,
    val channelCount: Int?,
    val pcmEncoding: Int?,
    val decoderName: String?,
    val decoderHardware: Boolean?,
    val audioOffloadEnabled: Boolean,
    val hiFiModeEnabled: Boolean,
    val crossfadeEnabled: Boolean,
    val crossfadeDurationMs: Int,
    val equalizerType: String,
    val replayGainEnabled: Boolean,
    val replayGainAlbumMode: Boolean,
    val audioLabEffectsActive: Boolean = false
)

@Serializable
data class CacheSection(
    val coilMemoryUsedBytes: Long,
    val coilMemoryMaxBytes: Long,
    val coilMemoryFillPercent: Double,
    val coilDiskUsedBytes: Long,
    val coilDiskMaxBytes: Long,
    val coilDiskFillPercent: Double,
    val appCacheDirBytes: Long
)

@Serializable
data class UiPerformanceSection(
    val refreshRateHz: Double,
    val targetFrameBudgetMs: Double,
    val sampledFrames: Int,
    val avgFrameMs: Double,
    val p95FrameMs: Double,
    val worstFrameMs: Double,
    val jankFrames: Int,
    val jankPercent: Double
)

@Serializable
data class AdvancedDiagnosticsSection(
    val enabled: Boolean = false,
    val sessionStartedIso: String? = null,
    val expiresAtIso: String? = null,
    val eventCount: Int = 0,
    val droppedEventCount: Long = 0,
    val events: List<AdvancedDiagnosticEventEntry> = emptyList()
)

@Serializable
data class AdvancedDiagnosticEventEntry(
    val elapsedRealtimeMs: Long,
    val type: String,
    val name: String,
    val details: Map<String, String> = emptyMap()
)
