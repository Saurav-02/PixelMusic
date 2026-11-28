package com.saurav.pixelmusic.presentation.viewmodel

import android.app.ActivityManager
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioDeviceInfo
import android.media.AudioFormat
import android.media.AudioManager
import android.net.Uri
import android.hardware.display.DisplayManager
import android.os.Build
import android.os.Debug
import android.os.Environment
import android.os.PowerManager
import android.os.StatFs
import android.provider.OpenableColumns
import android.view.Display
import androidx.annotation.OptIn
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.util.UnstableApi
import coil.imageLoader
import com.saurav.pixelmusic.BuildConfig
import com.saurav.pixelmusic.audiolab.AudioLabStore
import com.saurav.pixelmusic.data.database.DeviceCapabilitySongRow
import com.saurav.pixelmusic.data.database.MusicDao
import com.saurav.pixelmusic.data.database.SourceType
import com.saurav.pixelmusic.data.diagnostics.AppGraphicsSection
import com.saurav.pixelmusic.data.diagnostics.AudioPlaybackSection
import com.saurav.pixelmusic.data.diagnostics.CacheSection
import com.saurav.pixelmusic.data.diagnostics.DeviceRuntimeSection
import com.saurav.pixelmusic.data.diagnostics.DiagnosticFinding
import com.saurav.pixelmusic.data.diagnostics.LibraryHiResSection
import com.saurav.pixelmusic.data.diagnostics.PerformanceAnalysisSection
import com.saurav.pixelmusic.data.diagnostics.PerformanceDiagnosticsTracker
import com.saurav.pixelmusic.data.diagnostics.PerformanceReport
import com.saurav.pixelmusic.data.diagnostics.UiPerformanceSection
import com.saurav.pixelmusic.data.preferences.UserPreferencesRepository
import com.saurav.pixelmusic.data.service.player.ActiveDecoderInfo
import com.saurav.pixelmusic.data.service.player.DualPlayerEngine
import com.saurav.pixelmusic.data.service.player.HiFiCapabilityChecker
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class CodecInfo(
    val name: String,
    val supportedTypes: List<String>,
    val isHardwareAccelerated: Boolean,
    val maxSupportedInstances: Int
)

data class AudioOutputInfo(
    val name: String,
    val category: AudioOutputCategory
)

enum class AudioOutputCategory {
    BuiltIn,
    Bluetooth,
    Usb,
    Wired,
    Cast,
    Other
}

data class AudioCapabilities(
    val outputSampleRate: Int,
    val outputFramesPerBuffer: Int,
    val isLowLatencySupported: Boolean,
    val isProAudioSupported: Boolean,
    val isPcmFloatSupported: Boolean,
    val offloadSupportedFormats: List<String>,
    val outputRoutes: List<AudioOutputInfo>,
    val supportedCodecs: List<CodecInfo>
)

data class FormatSupportInfo(
    val label: String,
    val mimeType: String,
    val isDecoderAvailable: Boolean,
    val isHardwareAccelerated: Boolean,
    val isOffloadSupported: Boolean,
    val librarySongCount: Int
)

data class LocalMusicStorageSummary(
    val localSongCount: Int,
    val cloudSongCount: Int,
    val knownLocalFileCount: Int,
    val unavailableLocalFileCount: Int,
    val localMusicBytes: Long,
    val deviceAvailableBytes: Long,
    val deviceTotalBytes: Long
) {
    val deviceUsedBytes: Long
        get() = (deviceTotalBytes - deviceAvailableBytes).coerceAtLeast(0L)

    val localMusicStorageFraction: Float
        get() = if (deviceTotalBytes <= 0L) {
            0f
        } else {
            (localMusicBytes.toDouble() / deviceTotalBytes.toDouble()).coerceIn(0.0, 1.0).toFloat()
        }

    val deviceUsedFraction: Float
        get() = if (deviceTotalBytes <= 0L) {
            0f
        } else {
            (deviceUsedBytes.toDouble() / deviceTotalBytes.toDouble()).coerceIn(0.0, 1.0).toFloat()
        }
}

data class PlaybackCompatibilitySummary(
    val supportedLibrarySongCount: Int,
    val unsupportedLibrarySongCount: Int,
    val unknownFormatSongCount: Int,
    val unsupportedFormats: List<String>,
    val localHiResSongCount: Int,
    val resampledLocalSongCount: Int,
    val maxLocalSampleRate: Int?,
    val maxLocalBitrate: Int?
)

data class MemorySummary(
    val availableRamBytes: Long,
    val totalRamBytes: Long,
    val memoryClassMb: Int,
    val isLowRamDevice: Boolean,
    val isSystemLowMemory: Boolean
)

data class ExoPlayerInfo(
    val version: String,
    val renderers: String,
    val decoderCounters: String
)

data class DeviceCapabilitiesState(
    val deviceInfo: Map<String, String> = emptyMap(),
    val audioCapabilities: AudioCapabilities? = null,
    val exoPlayerInfo: ExoPlayerInfo? = null,
    val storageSummary: LocalMusicStorageSummary? = null,
    val playbackCompatibility: PlaybackCompatibilitySummary? = null,
    val formatSupport: List<FormatSupportInfo> = emptyList(),
    val memorySummary: MemorySummary? = null,
    val decoderInfo: ActiveDecoderInfo? = null,
    val isLoading: Boolean = true,
    val performanceReport: PerformanceReport? = null,
    val isGeneratingReport: Boolean = false,
    val diagnosticsSessionEnabled: Boolean = PerformanceDiagnosticsTracker.isEnabled(),
    val diagnosticsEventCount: Int = PerformanceDiagnosticsTracker.eventCount()
)

private data class AudioFormatCandidate(
    val label: String,
    val mimeType: String,
    val offloadEncoding: Int?
)

@HiltViewModel
class DeviceCapabilitiesViewModel @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val engine: DualPlayerEngine,
    private val musicDao: MusicDao,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val audioLabStore: AudioLabStore
) : ViewModel() {

    private val _state = MutableStateFlow(DeviceCapabilitiesState())
    val state = _state.asStateFlow()

    init {
        loadCapabilities()
    }

    private fun loadCapabilities() {
        viewModelScope.launch {
            val exoInfo = getExoPlayerInfo()
            val loadedState = withContext(Dispatchers.IO) {
                val deviceInfo = getDeviceInfo()
                val audioCaps = getAudioCapabilities()
                val libraryRows = musicDao.getDeviceCapabilitySongRows()
                val storage = getLocalMusicStorageSummary(libraryRows)
                val playback = getPlaybackCompatibilitySummary(libraryRows, audioCaps)
                val formatSupport = getFormatSupport(libraryRows, audioCaps)
                val memorySummary = getMemorySummary()
                val decoderInfo = engine.activeDecoderInfo.value

                DeviceCapabilitiesState(
                    deviceInfo = deviceInfo,
                    audioCapabilities = audioCaps,
                    exoPlayerInfo = exoInfo,
                    storageSummary = storage,
                    playbackCompatibility = playback,
                    formatSupport = formatSupport,
                    memorySummary = memorySummary,
                    decoderInfo = decoderInfo,
                    isLoading = false
                )
            }

            _state.value = loadedState
        }
    }

    private fun getDeviceInfo(): Map<String, String> {
        return mapOf(
            "Manufacturer" to Build.MANUFACTURER.replaceFirstChar { it.uppercase() },
            "Model" to Build.MODEL,
            "Brand" to Build.BRAND.replaceFirstChar { it.uppercase() },
            "Device" to Build.DEVICE,
            "Android Version" to Build.VERSION.RELEASE,
            "SDK Version" to Build.VERSION.SDK_INT.toString(),
            "Hardware" to Build.HARDWARE
        )
    }

    private fun getAudioCapabilities(): AudioCapabilities {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val sampleRate = audioManager.getProperty(AudioManager.PROPERTY_OUTPUT_SAMPLE_RATE)?.toIntOrNull() ?: 44_100
        val framesPerBuffer = audioManager.getProperty(AudioManager.PROPERTY_OUTPUT_FRAMES_PER_BUFFER)?.toIntOrNull() ?: 256
        val packageManager = context.packageManager
        val hasLowLatency = packageManager.hasSystemFeature(PackageManager.FEATURE_AUDIO_LOW_LATENCY)
        val hasProAudio = packageManager.hasSystemFeature(PackageManager.FEATURE_AUDIO_PRO)
        val offloadSupportedFormats = getOffloadSupportedFormats()
        val supportedCodecs = getSupportedAudioCodecs()

        return AudioCapabilities(
            outputSampleRate = sampleRate,
            outputFramesPerBuffer = framesPerBuffer,
            isLowLatencySupported = hasLowLatency,
            isProAudioSupported = hasProAudio,
            isPcmFloatSupported = HiFiCapabilityChecker.isSupported(),
            offloadSupportedFormats = offloadSupportedFormats,
            outputRoutes = getOutputRoutes(audioManager),
            supportedCodecs = supportedCodecs
        )
    }

    private fun getSupportedAudioCodecs(): List<CodecInfo> {
        val codecList = android.media.MediaCodecList(android.media.MediaCodecList.ALL_CODECS)
        val codecs = mutableListOf<CodecInfo>()
        val isSamsung = Build.MANUFACTURER.lowercase(Locale.US) == "samsung"

        for (codecInfo in codecList.codecInfos) {
            if (codecInfo.isEncoder) continue

            val types = codecInfo.supportedTypes
                .filter { it.startsWith("audio/") }
                .map { normalizeMimeType(it) }
                .distinct()
            if (types.isEmpty()) continue

            // On many Samsung devices, c2.sec.* codecs are high-performance hardware paths,
            // but the platform doesn't always flag them as hardwareAccelerated in the manifest.
            // We force report them as hardware in the UI if the name starts with c2.sec.
            val isHardware = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                codecInfo.isHardwareAccelerated || (isSamsung && codecInfo.name.startsWith("c2.sec."))
            } else {
                isSamsung && codecInfo.name.startsWith("c2.sec.")
            }

            val instances = try {
                codecInfo.getCapabilitiesForType(codecInfo.supportedTypes.first { it.startsWith("audio/") })
                    .maxSupportedInstances
            } catch (_: Exception) {
                -1
            }

            codecs.add(
                CodecInfo(
                    name = codecInfo.name,
                    supportedTypes = types,
                    isHardwareAccelerated = isHardware,
                    maxSupportedInstances = instances
                )
            )
        }
        return codecs.sortedBy { it.name }
    }

    private fun getLocalMusicStorageSummary(rows: List<DeviceCapabilitySongRow>): LocalMusicStorageSummary {
        val localRows = rows.filter { it.sourceType == SourceType.LOCAL }
        val cloudCount = rows.count { it.sourceType != SourceType.LOCAL }
        var totalBytes = 0L
        var knownFiles = 0
        var unavailableFiles = 0

        localRows.forEach { row ->
            val bytes = resolveLocalFileSize(row)
            if (bytes > 0L) {
                totalBytes += bytes
                knownFiles += 1
            } else {
                unavailableFiles += 1
            }
        }

        val storageStats = getDeviceStorageStats()

        return LocalMusicStorageSummary(
            localSongCount = localRows.size,
            cloudSongCount = cloudCount,
            knownLocalFileCount = knownFiles,
            unavailableLocalFileCount = unavailableFiles,
            localMusicBytes = totalBytes,
            deviceAvailableBytes = storageStats.first,
            deviceTotalBytes = storageStats.second
        )
    }

    private fun getPlaybackCompatibilitySummary(
        rows: List<DeviceCapabilitySongRow>,
        audioCapabilities: AudioCapabilities
    ): PlaybackCompatibilitySummary {
        val supportedTypes = audioCapabilities.supportedCodecs.flatMap { it.supportedTypes }.toSet()
        val localRows = rows.filter { it.sourceType == SourceType.LOCAL }
        var supportedCount = 0
        var unsupportedCount = 0
        var unknownCount = 0
        val unsupportedFormats = linkedSetOf<String>()

        rows.forEach { row ->
            val mimeType = row.mimeType?.let(::normalizeMimeType)
            when {
                mimeType.isNullOrBlank() -> unknownCount += 1
                isMimeTypeSupported(mimeType, supportedTypes) -> supportedCount += 1
                else -> {
                    unsupportedCount += 1
                    unsupportedFormats += mimeType
                }
            }
        }

        val localSampleRates = localRows.mapNotNull { it.sampleRate }.filter { it > 0 }
        val maxSampleRate = localSampleRates.maxOrNull()
        val maxBitrate = localRows.mapNotNull { it.bitrate }.filter { it > 0 }.maxOrNull()
        val outputRate = audioCapabilities.outputSampleRate.coerceAtLeast(1)
        val hiResSongCount = localSampleRates.count { it > 48_000 }
        val resampledSongCount = localSampleRates.count { it > outputRate }

        return PlaybackCompatibilitySummary(
            supportedLibrarySongCount = supportedCount,
            unsupportedLibrarySongCount = unsupportedCount,
            unknownFormatSongCount = unknownCount,
            unsupportedFormats = unsupportedFormats.toList(),
            localHiResSongCount = hiResSongCount,
            resampledLocalSongCount = resampledSongCount,
            maxLocalSampleRate = maxSampleRate,
            maxLocalBitrate = maxBitrate
        )
    }

    private fun getFormatSupport(
        rows: List<DeviceCapabilitySongRow>,
        audioCapabilities: AudioCapabilities
    ): List<FormatSupportInfo> {
        val supportedTypes = audioCapabilities.supportedCodecs.flatMap { it.supportedTypes }.toSet()
        val hardwareTypes = audioCapabilities.supportedCodecs
            .filter { it.isHardwareAccelerated }
            .flatMap { it.supportedTypes }
            .toSet()
        val offloadFormats = audioCapabilities.offloadSupportedFormats.toSet()
        val libraryCountsByMime = rows
            .mapNotNull { it.mimeType?.let(::normalizeMimeType) }
            .groupingBy { it }
            .eachCount()

        return audioFormatCandidates().map { candidate ->
            val acceptedMimes = compatibleMimeTypes(candidate.mimeType)
            FormatSupportInfo(
                label = candidate.label,
                mimeType = candidate.mimeType,
                isDecoderAvailable = acceptedMimes.any { isMimeTypeSupported(it, supportedTypes) },
                isHardwareAccelerated = acceptedMimes.any { isMimeTypeSupported(it, hardwareTypes) },
                isOffloadSupported = candidate.label in offloadFormats,
                librarySongCount = acceptedMimes.sumOf { libraryCountsByMime[it] ?: 0 }
            )
        }
    }

    private fun getMemorySummary(): MemorySummary {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memoryInfo)
        return MemorySummary(
            availableRamBytes = memoryInfo.availMem,
            totalRamBytes = memoryInfo.totalMem,
            memoryClassMb = activityManager.memoryClass,
            isLowRamDevice = activityManager.isLowRamDevice,
            isSystemLowMemory = memoryInfo.lowMemory
        )
    }

    private fun getOutputRoutes(audioManager: AudioManager): List<AudioOutputInfo> {
        return audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
            .map { device ->
                AudioOutputInfo(
                    name = device.productName?.toString()?.takeIf { it.isNotBlank() }
                        ?: device.type.toAudioOutputCategory().name,
                    category = device.type.toAudioOutputCategory()
                )
            }
            .distinctBy { it.category to it.name.lowercase(Locale.US) }
            .sortedBy { it.category.ordinal }
    }

    private fun getOffloadSupportedFormats(): List<String> {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return emptyList()

        val attributes = android.media.AudioAttributes.Builder()
            .setUsage(android.media.AudioAttributes.USAGE_MEDIA)
            .setContentType(android.media.AudioAttributes.CONTENT_TYPE_MUSIC)
            .build()

        return audioFormatCandidates()
            .mapNotNull { candidate ->
                val encoding = candidate.offloadEncoding ?: return@mapNotNull null
                val isSupported = runCatching {
                    val audioFormat = AudioFormat.Builder()
                        .setEncoding(encoding)
                        .setSampleRate(44_100)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                        .build()
                    AudioManager.isOffloadedPlaybackSupported(audioFormat, attributes)
                }.getOrDefault(false)

                if (isSupported) candidate.label else null
            }
    }

    private fun getDeviceStorageStats(): Pair<Long, Long> {
        return runCatching {
            val statFs = StatFs(Environment.getExternalStorageDirectory().path)
            statFs.availableBytes to statFs.totalBytes
        }.getOrElse { 0L to 0L }
    }

    private fun resolveLocalFileSize(row: DeviceCapabilitySongRow): Long {
        val pathSize = row.filePath
            .takeIf { it.isNotBlank() }
            ?.let { path ->
                runCatching {
                    val file = File(path)
                    if (file.exists() && file.isFile) file.length() else 0L
                }.getOrDefault(0L)
            }
            ?: 0L

        if (pathSize > 0L) return pathSize

        val contentSize = resolveContentUriSize(row.contentUriString)
        if (contentSize > 0L) return contentSize

        return estimateFileSizeFromMetadata(row)
    }

    private fun resolveContentUriSize(contentUriString: String): Long {
        val uri = runCatching { Uri.parse(contentUriString) }.getOrNull() ?: return 0L
        if (uri.scheme != "content") return 0L

        return runCatching {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.SIZE), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (sizeIndex >= 0 && !cursor.isNull(sizeIndex)) cursor.getLong(sizeIndex) else 0L
                } else {
                    0L
                }
            } ?: 0L
        }.getOrDefault(0L)
    }

    private fun estimateFileSizeFromMetadata(row: DeviceCapabilitySongRow): Long {
        val bitrate = row.bitrate?.takeIf { it > 0 }?.toLong() ?: return 0L
        val durationMs = row.duration.takeIf { it > 0 } ?: return 0L
        return (bitrate * durationMs / 8_000L).coerceAtLeast(0L)
    }

    @OptIn(UnstableApi::class)
    private fun getExoPlayerInfo(): ExoPlayerInfo {
        val player = engine.masterPlayer
        val version = androidx.media3.common.MediaLibraryInfo.VERSION
        val exoPlayer = player as? androidx.media3.exoplayer.ExoPlayer
        val renderers = "${exoPlayer?.rendererCount ?: 0}"

        return ExoPlayerInfo(
            version = version,
            renderers = renderers,
            decoderCounters = "N/A"
        )
    }

    fun toggleDiagnosticsSession(enabled: Boolean) {
        PerformanceDiagnosticsTracker.setEnabled(enabled)
        _state.update {
            it.copy(
                diagnosticsSessionEnabled = PerformanceDiagnosticsTracker.isEnabled(),
                diagnosticsEventCount = PerformanceDiagnosticsTracker.eventCount()
            )
        }
    }

    @OptIn(UnstableApi::class)
    fun markLagSpike(note: String = "Marked from Device capabilities screen") {
        val activeMime = runCatching {
            (engine.masterPlayer as? androidx.media3.exoplayer.ExoPlayer)?.audioFormat?.sampleMimeType
        }.getOrNull()
        val count = PerformanceDiagnosticsTracker.markLag(
            context = context,
            note = note,
            activeMime = activeMime
        )
        _state.update {
            it.copy(
                diagnosticsSessionEnabled = PerformanceDiagnosticsTracker.isEnabled(),
                diagnosticsEventCount = count
            )
        }
    }

    @OptIn(UnstableApi::class)
    fun generatePerformanceReport(onReady: (PerformanceReport) -> Unit = {}) {
        if (_state.value.isGeneratingReport) return
        viewModelScope.launch {
            _state.update { it.copy(isGeneratingReport = true) }
            try {
                val refreshRateHz = readDisplayRefreshRateHz()
                // Capture active playback format on Main thread (ExoPlayer thread contract)
                val exoPlayer = engine.masterPlayer as? androidx.media3.exoplayer.ExoPlayer
                val audioFormat = runCatching { exoPlayer?.audioFormat }.getOrNull()
                val currentMime = audioFormat?.sampleMimeType
                val currentSampleRate = audioFormat?.sampleRate?.takeIf { it > 0 }
                val currentBitrate = audioFormat?.bitrate?.takeIf { it > 0 }
                val currentChannels = audioFormat?.channelCount?.takeIf { it > 0 }
                val currentPcmEncoding = audioFormat?.pcmEncoding?.takeIf { it > 0 }
                val activeDecoder = engine.activeDecoderInfo.value

                // Run a short real Choreographer frame probe (1.5s) on Main thread
                val uiPerf = PerformanceDiagnosticsTracker.runFrameJankProbe(
                    refreshRateHz = refreshRateHz,
                    durationMs = 1_500L
                )

                val report = withContext(Dispatchers.IO) {
                    val audioCaps = _state.value.audioCapabilities ?: getAudioCapabilities()
                    val libraryRows = musicDao.getDeviceCapabilitySongRows()
                    val storage = _state.value.storageSummary ?: getLocalMusicStorageSummary(libraryRows)
                    val compat = _state.value.playbackCompatibility ?: getPlaybackCompatibilitySummary(libraryRows, audioCaps)
                    val mem = getMemorySummary()

                    val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
                    val thermalStatus = PerformanceDiagnosticsTracker.readThermalStatus(context)
                    val isPowerSave = pm?.isPowerSaveMode ?: false

                    val runtime = Runtime.getRuntime()
                    val jvmUsed = (runtime.totalMemory() - runtime.freeMemory()).coerceAtLeast(0L)
                    val jvmMax = runtime.maxMemory().coerceAtLeast(1L)
                    val jvmPct = (jvmUsed.toDouble() / jvmMax.toDouble()) * 100.0
                    val nativeHeap = Debug.getNativeHeapAllocatedSize().coerceAtLeast(0L)
                    val ramUsedPct = if (mem.totalRamBytes > 0L) {
                        ((mem.totalRamBytes - mem.availableRamBytes).coerceAtLeast(0L).toDouble() / mem.totalRamBytes.toDouble()) * 100.0
                    } else 0.0

                    val deviceSection = DeviceRuntimeSection(
                        manufacturer = Build.MANUFACTURER,
                        model = Build.MODEL,
                        brand = Build.BRAND,
                        device = Build.DEVICE,
                        hardware = Build.HARDWARE,
                        androidVersion = Build.VERSION.RELEASE,
                        sdkInt = Build.VERSION.SDK_INT,
                        supportedAbis = Build.SUPPORTED_ABIS.toList(),
                        cpuCores = runtime.availableProcessors(),
                        displayRefreshRateHz = refreshRateHz,
                        thermalStatus = thermalStatus,
                        isPowerSaveMode = isPowerSave,
                        memoryClassMb = mem.memoryClassMb,
                        isLowRamDevice = mem.isLowRamDevice,
                        isSystemLowMemory = mem.isSystemLowMemory,
                        totalRamBytes = mem.totalRamBytes,
                        availableRamBytes = mem.availableRamBytes,
                        ramUsedPercent = ramUsedPct,
                        jvmHeapUsedBytes = jvmUsed,
                        jvmHeapMaxBytes = jvmMax,
                        jvmHeapUsedPercent = jvmPct,
                        nativeHeapAllocatedBytes = nativeHeap
                    )

                    val motionBlur = runCatching { userPreferencesRepository.uiMotionBlurEnabledFlow.first() }.getOrDefault(false)
                    val performanceMode = runCatching { userPreferencesRepository.performanceModeEnabledFlow.first() }.getOrDefault(false)
                    val animatedLyricsBlur = runCatching { userPreferencesRepository.animatedLyricsBlurEnabledFlow.first() }.getOrDefault(false)
                    val albumArtQuality = runCatching { userPreferencesRepository.albumArtQualityFlow.first().name }.getOrDefault("HIGH")
                    val designStyle = runCatching { userPreferencesRepository.playerDesignStyleFlow.first().name }.getOrDefault("DEFAULT")
                    val offloadEnabled = runCatching { userPreferencesRepository.audioOffloadEnabledFlow.first() }.getOrDefault(false)
                    val hiFiEnabled = runCatching { userPreferencesRepository.hiFiModeEnabledFlow.first() }.getOrDefault(false)
                    val crossfadeEnabled = runCatching { userPreferencesRepository.isCrossfadeEnabledFlow.first() }.getOrDefault(false)
                    val crossfadeMs = runCatching { userPreferencesRepository.crossfadeDurationFlow.first() }.getOrDefault(2000)
                    val eqType = runCatching { userPreferencesRepository.equalizerTypeFlow.first().name }.getOrDefault("IN_APP")
                    val rgEnabled = runCatching { userPreferencesRepository.replayGainEnabledFlow.first() }.getOrDefault(false)
                    val rgAlbum = runCatching { userPreferencesRepository.replayGainUseAlbumGainFlow.first() }.getOrDefault(false)
                    val audioLabEffectsActive = runCatching { audioLabStore.shapesSound.first() }.getOrDefault(false)

                    val appSection = AppGraphicsSection(
                        versionName = BuildConfig.VERSION_NAME,
                        versionCode = BuildConfig.VERSION_CODE.toLong(),
                        buildType = BuildConfig.BUILD_TYPE,
                        applicationId = BuildConfig.APPLICATION_ID,
                        media3Version = androidx.media3.common.MediaLibraryInfo.VERSION,
                        supportsHardwareBlur = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S,
                        supportsAgslShaders = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU,
                        supportsSpatializer = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S_V2,
                        motionBlurEnabled = motionBlur,
                        playerDesignStyle = designStyle,
                        performanceModeEnabled = performanceMode,
                        animatedLyricsBlurEnabled = animatedLyricsBlur,
                        albumArtQuality = albumArtQuality
                    )

                    val mimeCounts = libraryRows
                        .mapNotNull { it.mimeType?.let(::normalizeMimeType) }
                        .groupingBy { it }
                        .eachCount()
                    val sampleRates = libraryRows.mapNotNull { it.sampleRate }.filter { it > 0 }
                    val bitrates = libraryRows.mapNotNull { it.bitrate }.filter { it > 0 }
                    val estSizes = libraryRows.map { estimateFileSizeFromMetadata(it) }.filter { it > 0L }
                    val losslessMimes = setOf("audio/flac", "audio/wav", "audio/alac", "audio/x-aiff")
                    val losslessCount = libraryRows.count { row ->
                        val m = row.mimeType?.let(::normalizeMimeType)
                        m != null && m in losslessMimes
                    }
                    val hiResCount = sampleRates.count { it > 48_000 }
                    val ultraHiResCount = sampleRates.count { it >= 176_400 }
                    val expensiveCount = libraryRows.count { row ->
                        val m = row.mimeType?.let(::normalizeMimeType)
                        val isLossless = m != null && m in losslessMimes
                        val isHiRes = (row.sampleRate ?: 0) > 48_000
                        isLossless || isHiRes
                    }

                    val librarySection = LibraryHiResSection(
                        totalSongs = libraryRows.size,
                        localSongs = storage.localSongCount,
                        cloudSongs = storage.cloudSongCount,
                        knownLocalFiles = storage.knownLocalFileCount,
                        unavailableLocalFiles = storage.unavailableLocalFileCount,
                        localMusicBytes = storage.localMusicBytes,
                        deviceAvailableBytes = storage.deviceAvailableBytes,
                        deviceTotalBytes = storage.deviceTotalBytes,
                        mimeCounts = mimeCounts,
                        maxBitrate = bitrates.maxOrNull(),
                        minSampleRate = sampleRates.minOrNull(),
                        maxSampleRate = sampleRates.maxOrNull(),
                        estFileSizeMinBytes = estSizes.minOrNull(),
                        estFileSizeAvgBytes = if (estSizes.isNotEmpty()) estSizes.average() else null,
                        estFileSizeMaxBytes = estSizes.maxOrNull(),
                        hiResCount = hiResCount,
                        ultraHiResCount = ultraHiResCount,
                        resampledCount = compat.resampledLocalSongCount,
                        losslessCodecCount = losslessCount,
                        likelyExpensiveToDecodeCount = expensiveCount,
                        unsupportedFormatCount = compat.unsupportedLibrarySongCount,
                        unsupportedFormats = compat.unsupportedFormats
                    )

                    val hwCodecs = audioCaps.supportedCodecs.count { it.isHardwareAccelerated }
                    val swCodecs = audioCaps.supportedCodecs.size - hwCodecs
                    val nativeLatencyMs = if (audioCaps.outputSampleRate > 0) {
                        (audioCaps.outputFramesPerBuffer.toDouble() / audioCaps.outputSampleRate.toDouble()) * 1000.0
                    } else 0.0

                    val audioSection = AudioPlaybackSection(
                        nativeOutputSampleRate = audioCaps.outputSampleRate,
                        nativeFramesPerBuffer = audioCaps.outputFramesPerBuffer,
                        nativeBufferLatencyMs = nativeLatencyMs,
                        isLowLatencySupported = audioCaps.isLowLatencySupported,
                        isProAudioSupported = audioCaps.isProAudioSupported,
                        isPcmFloatSupported = audioCaps.isPcmFloatSupported,
                        hardwareCodecCount = hwCodecs,
                        softwareCodecCount = swCodecs,
                        outputRoutes = audioCaps.outputRoutes.map { "${it.name} (${it.category.name})" },
                        offloadSupportedFormats = audioCaps.offloadSupportedFormats,
                        currentMime = currentMime,
                        sampleRate = currentSampleRate,
                        bitrate = currentBitrate,
                        channelCount = currentChannels,
                        pcmEncoding = currentPcmEncoding,
                        decoderName = activeDecoder?.name,
                        decoderHardware = activeDecoder?.isHardware,
                        audioOffloadEnabled = offloadEnabled,
                        hiFiModeEnabled = hiFiEnabled,
                        crossfadeEnabled = crossfadeEnabled,
                        crossfadeDurationMs = crossfadeMs,
                        equalizerType = eqType,
                        replayGainEnabled = rgEnabled,
                        replayGainAlbumMode = rgAlbum,
                        audioLabEffectsActive = audioLabEffectsActive
                    )

                    val memCache = context.imageLoader.memoryCache
                    val diskCache = context.imageLoader.diskCache
                    val memCacheUsed = memCache?.size?.toLong() ?: 0L
                    val memCacheMax = memCache?.maxSize?.toLong()?.coerceAtLeast(1L) ?: 1L
                    val diskCacheUsed = diskCache?.size ?: 0L
                    val diskCacheMax = diskCache?.maxSize?.coerceAtLeast(1L) ?: 1L
                    val cacheDirBytes = computeDirBytes(context.cacheDir)

                    val cacheSection = CacheSection(
                        coilMemoryUsedBytes = memCacheUsed,
                        coilMemoryMaxBytes = memCacheMax,
                        coilMemoryFillPercent = (memCacheUsed.toDouble() / memCacheMax.toDouble()) * 100.0,
                        coilDiskUsedBytes = diskCacheUsed,
                        coilDiskMaxBytes = diskCacheMax,
                        coilDiskFillPercent = (diskCacheUsed.toDouble() / diskCacheMax.toDouble()) * 100.0,
                        appCacheDirBytes = cacheDirBytes
                    )

                    val analysis = buildAutomatedAnalysis(
                        device = deviceSection,
                        app = appSection,
                        library = librarySection,
                        audio = audioSection,
                        cache = cacheSection,
                        uiPerf = uiPerf
                    )

                    PerformanceReport(
                        generatedAtIso = PerformanceDiagnosticsTracker.formatIsoUtc(System.currentTimeMillis()),
                        analysis = analysis,
                        device = deviceSection,
                        app = appSection,
                        library = librarySection,
                        audio = audioSection,
                        cache = cacheSection,
                        uiPerformance = uiPerf,
                        advancedDiagnostics = PerformanceDiagnosticsTracker.snapshot()
                    )
                }

                _state.update {
                    it.copy(
                        performanceReport = report,
                        isGeneratingReport = false,
                        diagnosticsSessionEnabled = PerformanceDiagnosticsTracker.isEnabled(),
                        diagnosticsEventCount = PerformanceDiagnosticsTracker.eventCount()
                    )
                }
                onReady(report)
            } catch (_: Throwable) {
                _state.update { it.copy(isGeneratingReport = false) }
            }
        }
    }

    private fun readDisplayRefreshRateHz(): Double {
        return runCatching {
            val dm = context.getSystemService(Context.DISPLAY_SERVICE) as? DisplayManager
            val display = dm?.getDisplay(Display.DEFAULT_DISPLAY)
            display?.refreshRate?.toDouble()?.takeIf { it > 0.0 } ?: 60.0
        }.getOrDefault(60.0)
    }

    private fun computeDirBytes(dir: File?): Long {
        if (dir == null || !dir.exists()) return 0L
        var total = 0L
        var visited = 0
        val stack = ArrayDeque<File>()
        stack.addLast(dir)
        while (stack.isNotEmpty() && visited < 4000) {
            val current = stack.removeLast()
            visited++
            if (current.isFile) {
                total += current.length()
            } else if (current.isDirectory) {
                current.listFiles()?.forEach { stack.addLast(it) }
            }
        }
        return total
    }

    private fun buildAutomatedAnalysis(
        device: DeviceRuntimeSection,
        app: AppGraphicsSection,
        library: LibraryHiResSection,
        audio: AudioPlaybackSection,
        cache: CacheSection,
        uiPerf: UiPerformanceSection
    ): PerformanceAnalysisSection {
        var score = 100
        val findings = mutableListOf<DiagnosticFinding>()

        // 1. Thermal & Power throttling
        when (device.thermalStatus) {
            "SEVERE", "CRITICAL", "EMERGENCY", "SHUTDOWN" -> {
                score -= 25
                val severeRec = when {
                    app.motionBlurEnabled -> "Let the device cool down; disable Motion Blur in Settings and avoid long video exports while charging."
                    !app.performanceModeEnabled -> "Let the device cool down; enable Performance Mode in Settings and avoid long video exports while charging."
                    else -> "Let the device cool down and avoid long video exports while charging (Motion Blur is off and Performance Mode is already active)."
                }
                findings += DiagnosticFinding(
                    severity = "CRITICAL",
                    category = "THERMAL",
                    title = "Heavy thermal throttling (${device.thermalStatus})",
                    detail = "Device CPU/GPU clocks are aggressively capped by the OS thermal daemon.",
                    recommendation = severeRec
                )
            }
            "MODERATE" -> {
                score -= 12
                val moderateRec = when {
                    app.motionBlurEnabled -> "Temporarily disable Motion Blur in Settings if UI animations feel slower."
                    app.animatedLyricsBlurEnabled && !app.performanceModeEnabled -> "Motion Blur is already off; disable Animated Lyrics Blur or enable Performance Mode in Settings if animations feel slower."
                    !app.performanceModeEnabled -> "Motion Blur is already off; enable Performance Mode in Settings or let the device cool down if animations feel slower."
                    else -> "Motion Blur is off and Performance Mode is already active — let the device cool down (especially while charging) for best smoothness."
                }
                findings += DiagnosticFinding(
                    severity = "WARNING",
                    category = "THERMAL",
                    title = "Moderate thermal throttling",
                    detail = "Elevated device temperature may increase frame times during fast list scrolls.",
                    recommendation = moderateRec
                )
            }
        }

        if (device.isPowerSaveMode) {
            score -= 8
            findings += DiagnosticFinding(
                severity = "INFO",
                category = "THERMAL",
                title = "Battery Saver is active",
                detail = "Android Power Save mode limits background threads and may cap display refresh rate.",
                recommendation = "Disable Battery Saver if testing peak animation smoothness."
            )
        }

        // 2. Memory & JVM Heap pressure
        if (device.isSystemLowMemory || device.ramUsedPercent >= 88.0) {
            score -= 18
            findings += DiagnosticFinding(
                severity = "WARNING",
                category = "MEMORY",
                title = "High system RAM pressure (${String.format(Locale.ROOT, "%.0f", device.ramUsedPercent)}% used)",
                detail = "Only ${PerformanceReport.bytes(device.availableRamBytes)} RAM free out of ${PerformanceReport.bytes(device.totalRamBytes)}.",
                recommendation = "Close background apps; low RAM causes aggressive image cache evictions and GC pauses."
            )
        }
        if (device.jvmHeapUsedPercent >= 80.0) {
            score -= 16
            val heapRec = if (app.albumArtQuality == "LOW") {
                "Frequent garbage collection can cause micro-stutters; restart the app to reclaim memory."
            } else {
                "Frequent garbage collection can cause micro-stutters; restart the app or lower Album Art Quality in Settings."
            }
            findings += DiagnosticFinding(
                severity = "WARNING",
                category = "MEMORY",
                title = "High JVM heap usage (${String.format(Locale.ROOT, "%.0f", device.jvmHeapUsedPercent)}%)",
                detail = "${PerformanceReport.bytes(device.jvmHeapUsedBytes)} used of ${PerformanceReport.bytes(device.jvmHeapMaxBytes)} max heap.",
                recommendation = heapRec
            )
        }

        // 3. UI Frame Probe jank
        if (uiPerf.sampledFrames >= 10) {
            when {
                uiPerf.jankPercent >= 15.0 || uiPerf.worstFrameMs >= uiPerf.targetFrameBudgetMs * 4.0 -> {
                    score -= 20
                    val uiRec = when {
                        app.motionBlurEnabled -> "Try turning off Motion Blur in Appearance settings to reduce GPU load."
                        !app.performanceModeEnabled && app.playerDesignStyle != "DEFAULT" -> "Try switching to the Default player design style or enabling Performance Mode to reduce GPU load."
                        !app.performanceModeEnabled -> "Enable Performance Mode in Settings and close background apps to reduce GPU/compositor load."
                        app.playerDesignStyle != "DEFAULT" -> "Performance Mode is already on; try switching to the Default player design style and closing background apps."
                        else -> "Motion Blur is off and Performance Mode is already active — close background apps or let the device cool down to reduce GPU/compositor load."
                    }
                    findings += DiagnosticFinding(
                        severity = "CRITICAL",
                        category = "UI",
                        title = "Elevated UI frame jank (${String.format(Locale.ROOT, "%.1f", uiPerf.jankPercent)}%)",
                        detail = "P95 frame ${String.format(Locale.ROOT, "%.1f", uiPerf.p95FrameMs)}ms, worst ${String.format(Locale.ROOT, "%.1f", uiPerf.worstFrameMs)}ms vs ${String.format(Locale.ROOT, "%.1f", uiPerf.targetFrameBudgetMs)}ms budget.",
                        recommendation = uiRec
                    )
                }
                uiPerf.jankPercent >= 5.0 -> {
                    score -= 8
                    findings += DiagnosticFinding(
                        severity = "WARNING",
                        category = "UI",
                        title = "Mild UI frame drops (${String.format(Locale.ROOT, "%.1f", uiPerf.jankPercent)}%)",
                        detail = "${uiPerf.jankFrames}/${uiPerf.sampledFrames} frames exceeded ${String.format(Locale.ROOT, "%.1f", uiPerf.targetFrameBudgetMs * 1.35)}ms.",
                        recommendation = "Normal during initial screen settle; re-run while scrolling if investigating lag."
                    )
                }
                else -> {
                    val pacingDetail = if (uiPerf.p95FrameMs <= uiPerf.targetFrameBudgetMs * 1.05) {
                        "P95 ${String.format(Locale.ROOT, "%.1f", uiPerf.p95FrameMs)}ms within ${String.format(Locale.ROOT, "%.1f", uiPerf.targetFrameBudgetMs)}ms budget at ${String.format(Locale.ROOT, "%.0f", uiPerf.refreshRateHz)} Hz."
                    } else {
                        "P95 ${String.format(Locale.ROOT, "%.1f", uiPerf.p95FrameMs)}ms (${String.format(Locale.ROOT, "%.1f", uiPerf.jankPercent)}% jank) at ${String.format(Locale.ROOT, "%.0f", uiPerf.refreshRateHz)} Hz (${String.format(Locale.ROOT, "%.1f", uiPerf.targetFrameBudgetMs)}ms budget)."
                    }
                    findings += DiagnosticFinding(
                        severity = "OK",
                        category = "UI",
                        title = "Silky frame pacing (${String.format(Locale.ROOT, "%.1f", uiPerf.avgFrameMs)}ms avg)",
                        detail = pacingDetail
                    )
                }
            }
        }

        // 4. Audio Pipeline & Resampling
        if (audio.decoderHardware == false && audio.decoderName != null) {
            val offloadSupported = audio.offloadSupportedFormats.isNotEmpty()
            val isSoftwareTypicalFormat = audio.currentMime in setOf("audio/opus", "audio/flac", "audio/vorbis", "audio/wav")
            val expectedSoftware = isSoftwareTypicalFormat ||
                audio.audioLabEffectsActive ||
                audio.crossfadeEnabled ||
                !offloadSupported
            if (!expectedSoftware) {
                score -= 5
            }
            val audioRec = when {
                !offloadSupported -> "Normal on this device (hardware audio offload is not advertised by the platform)."
                audio.audioOffloadEnabled && audio.audioLabEffectsActive -> "Audio Offload is already on, but active AudioLab EQ/DSP effects require software decoding."
                audio.audioOffloadEnabled && audio.crossfadeEnabled -> "Audio Offload is already on, but Crossfade requires software mixing."
                audio.audioOffloadEnabled -> "Audio Offload is already enabled; software decoding is normal for this track format."
                audio.audioLabEffectsActive -> "Expected while AudioLab EQ/DSP effects are active; bypass effects and enable Audio Offload in Settings for lower battery use."
                audio.crossfadeEnabled -> "Crossfade uses software mixing; disable Crossfade and enable Audio Offload in Settings for lower battery use."
                isSoftwareTypicalFormat -> "Normal for FLAC/Opus tracks; enable Audio Offload in Settings for lower battery use on supported formats (MP3/AAC)."
                else -> "Enable Audio Offload in Settings for lower battery use."
            }
            findings += DiagnosticFinding(
                severity = "INFO",
                category = "AUDIO",
                title = "Active track uses software decoder (${audio.decoderName})",
                detail = "Software decoding uses slightly more CPU than hardware offload.",
                recommendation = audioRec
            )
        }
        if (library.resampledCount > 0) {
            score -= 4
            val resampleRec = if (audio.hiFiModeEnabled) {
                "Hi-Fi mode is already enabled; connect a high-res USB DAC for bit-perfect playback."
            } else {
                "Enable Hi-Fi mode in Settings with a high-res USB DAC for bit-perfect playback."
            }
            findings += DiagnosticFinding(
                severity = "INFO",
                category = "AUDIO",
                title = "${library.resampledCount} local tracks exceed native ${audio.nativeOutputSampleRate} Hz output",
                detail = "Android mixer resamples these tracks unless routed through a USB DAC supporting higher rates.",
                recommendation = resampleRec
            )
        }
        if (library.unsupportedFormatCount > 0) {
            score -= 8
            findings += DiagnosticFinding(
                severity = "WARNING",
                category = "LIBRARY",
                title = "${library.unsupportedFormatCount} tracks use unsupported formats",
                detail = "Missing decoders for: ${library.unsupportedFormats.joinToString(", ")}.",
                recommendation = "Convert or re-download these files in FLAC, AAC, Opus, or MP3."
            )
        }
        if (library.unavailableLocalFiles > 0) {
            score -= 6
            findings += DiagnosticFinding(
                severity = "WARNING",
                category = "LIBRARY",
                title = "${library.unavailableLocalFiles} local files could not be accessed",
                detail = "Indexed files may have been moved, deleted, or blocked by scoped storage permissions.",
                recommendation = "Run a library rescan in Settings to clean up stale entries."
            )
        }

        // 5. Cache Health
        if (cache.appCacheDirBytes >= 500_000_000L) {
            score -= 4
            findings += DiagnosticFinding(
                severity = "INFO",
                category = "CACHE",
                title = "Large app cache (${PerformanceReport.bytes(cache.appCacheDirBytes)})",
                detail = "Coil disk cache holds ${PerformanceReport.bytes(cache.coilDiskUsedBytes)} of ${PerformanceReport.bytes(cache.coilDiskMaxBytes)}.",
                recommendation = "Clear cache in Settings if device storage is tight."
            )
        }

        val finalScore = score.coerceIn(0, 100)
        val grade = when {
            finalScore >= 90 -> "EXCELLENT"
            finalScore >= 75 -> "GOOD"
            finalScore >= 55 -> "FAIR"
            else -> "DEGRADED"
        }

        val warningOrCritCount = findings.count { it.severity == "WARNING" || it.severity == "CRITICAL" }
        val summary = when {
            warningOrCritCount == 0 -> "No performance bottlenecks detected — device, audio pipeline, and UI pacing look healthy."
            warningOrCritCount == 1 -> "1 potential bottleneck detected — review the highlighted finding below."
            else -> "$warningOrCritCount potential bottlenecks detected — review the prioritized findings below."
        }

        val severityRank = mapOf("CRITICAL" to 0, "WARNING" to 1, "INFO" to 2, "OK" to 3)
        val sortedFindings = findings.sortedBy { severityRank[it.severity] ?: 4 }

        return PerformanceAnalysisSection(
            healthScore = finalScore,
            grade = grade,
            summary = summary,
            findings = sortedFindings
        )
    }
}

private fun Int.toAudioOutputCategory(): AudioOutputCategory {
    return when (this) {
        AudioDeviceInfo.TYPE_BUILTIN_EARPIECE,
        AudioDeviceInfo.TYPE_BUILTIN_SPEAKER,
        AudioDeviceInfo.TYPE_BUILTIN_SPEAKER_SAFE -> AudioOutputCategory.BuiltIn
        AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
        AudioDeviceInfo.TYPE_BLUETOOTH_SCO,
        AudioDeviceInfo.TYPE_BLE_HEADSET,
        AudioDeviceInfo.TYPE_BLE_SPEAKER,
        AudioDeviceInfo.TYPE_BLE_BROADCAST -> AudioOutputCategory.Bluetooth
        AudioDeviceInfo.TYPE_USB_ACCESSORY,
        AudioDeviceInfo.TYPE_USB_DEVICE,
        AudioDeviceInfo.TYPE_USB_HEADSET -> AudioOutputCategory.Usb
        AudioDeviceInfo.TYPE_WIRED_HEADPHONES,
        AudioDeviceInfo.TYPE_WIRED_HEADSET,
        AudioDeviceInfo.TYPE_LINE_ANALOG,
        AudioDeviceInfo.TYPE_LINE_DIGITAL -> AudioOutputCategory.Wired
        AudioDeviceInfo.TYPE_HDMI,
        AudioDeviceInfo.TYPE_HDMI_ARC,
        AudioDeviceInfo.TYPE_HDMI_EARC -> AudioOutputCategory.Cast
        else -> AudioOutputCategory.Other
    }
}

private fun audioFormatCandidates(): List<AudioFormatCandidate> {
    return buildList {
        add(AudioFormatCandidate("MP3", "audio/mpeg", AudioFormat.ENCODING_MP3))
        add(AudioFormatCandidate("AAC", "audio/mp4a-latm", AudioFormat.ENCODING_AAC_LC))
        add(AudioFormatCandidate("FLAC", "audio/flac", null))
        add(AudioFormatCandidate("Vorbis", "audio/vorbis", null))
        add(AudioFormatCandidate("WAV", "audio/wav", AudioFormat.ENCODING_PCM_16BIT))
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            add(AudioFormatCandidate("Opus", "audio/opus", AudioFormat.ENCODING_OPUS))
        } else {
            add(AudioFormatCandidate("Opus", "audio/opus", null))
        }
        add(AudioFormatCandidate("WebM", "audio/webm", null))
        add(AudioFormatCandidate("ALAC", "audio/alac", null))
        add(AudioFormatCandidate("AIFF", "audio/x-aiff", null))
        add(AudioFormatCandidate("AC3", "audio/ac3", null))
        add(AudioFormatCandidate("DTS", "audio/vnd.dts", null))
        add(AudioFormatCandidate("AMR-NB", "audio/3gpp", null))
        add(AudioFormatCandidate("AMR-WB", "audio/amr-wb", null))
        add(AudioFormatCandidate("WMA", "audio/x-ms-wma", null))
        add(AudioFormatCandidate("EVRC", "audio/evrc", null))
        add(AudioFormatCandidate("QCELP", "audio/qcelp", null))
        add(AudioFormatCandidate("IMA-ADPCM", "audio/x-ima-adpcm", null))
    }
}

private fun normalizeMimeType(mimeType: String): String {
    return when (val mime = mimeType.lowercase(Locale.US).substringBefore(";").trim()) {
        "audio/mp3", "audio/x-mp3", "audio/mpeg3" -> "audio/mpeg"
        "audio/x-wav", "audio/wave", "audio/vnd.wave" -> "audio/wav"
        "audio/aac", "audio/x-aac", "audio/m4a", "audio/mp4", "audio/aacp" -> "audio/mp4a-latm"
        "audio/x-flac" -> "audio/flac"
        "audio/ogg", "audio/x-vorbis", "application/ogg" -> "audio/vorbis"
        "audio/x-ms-wma", "audio/wma" -> "audio/x-ms-wma"
        "audio/x-aiff", "audio/aiff", "audio/aif", "audio/x-aifc" -> "audio/x-aiff"
        "audio/ac3", "audio/eac3", "audio/eac3-joc" -> "audio/ac3"
        "audio/vnd.dts", "audio/vnd.dts.hd" -> "audio/vnd.dts"
        "audio/3gpp", "audio/amr" -> "audio/3gpp"
        "audio/amr-wb" -> "audio/amr-wb"
        "audio/evrc", "audio/x-evrc" -> "audio/evrc"
        "audio/qcelp", "audio/x-qcelp" -> "audio/qcelp"
        "audio/x-ima-adpcm", "audio/ima-adpcm" -> "audio/x-ima-adpcm"
        "audio/webm", "video/webm" -> "audio/webm"
        "audio/x-matroska", "audio/matroska", "video/x-matroska" -> "audio/x-matroska"
        else -> mime
    }
}

private fun compatibleMimeTypes(mimeType: String): Set<String> {
    val normalized = normalizeMimeType(mimeType)
    return when (normalized) {
        "audio/mpeg" -> setOf("audio/mpeg", "audio/mp3", "audio/x-mp3", "audio/mpeg3")
        "audio/mp4a-latm" -> setOf("audio/mp4a-latm", "audio/aac", "audio/x-aac", "audio/m4a", "audio/mp4", "audio/aacp")
        "audio/flac" -> setOf("audio/flac", "audio/x-flac")
        "audio/wav" -> setOf("audio/wav", "audio/x-wav", "audio/wave", "audio/vnd.wave", "audio/raw")
        "audio/vorbis" -> setOf("audio/vorbis", "audio/ogg", "audio/x-vorbis", "application/ogg")
        "audio/x-ms-wma" -> setOf("audio/x-ms-wma", "audio/wma")
        "audio/x-aiff" -> setOf("audio/x-aiff", "audio/aiff", "audio/aif", "audio/x-aifc")
        "audio/ac3" -> setOf("audio/ac3", "audio/eac3", "audio/eac3-joc")
        "audio/vnd.dts" -> setOf("audio/vnd.dts", "audio/vnd.dts.hd")
        "audio/3gpp" -> setOf("audio/3gpp", "audio/amr")
        "audio/amr-wb" -> setOf("audio/amr-wb")
        "audio/evrc" -> setOf("audio/evrc", "audio/x-evrc")
        "audio/qcelp" -> setOf("audio/qcelp", "audio/x-qcelp")
        "audio/x-ima-adpcm" -> setOf("audio/x-ima-adpcm", "audio/ima-adpcm", "audio/raw")
        "audio/webm" -> setOf("audio/webm", "audio/opus", "audio/vorbis")
        "audio/x-matroska" -> setOf("audio/x-matroska", "audio/opus", "audio/vorbis", "audio/mp4a-latm", "audio/flac")
        else -> setOf(normalized)
    }
}

private fun isMimeTypeSupported(
    mimeType: String,
    supportedTypes: Set<String>
): Boolean {
    return compatibleMimeTypes(mimeType).any { normalizeMimeType(it) in supportedTypes }
}
