package com.saurav.pixelmusic.audiolab

import android.content.Context
import androidx.core.content.edit
import com.saurav.pixelmusic.audiolab.audio.Biquad
import com.saurav.pixelmusic.audiolab.audio.EightDParams
import com.saurav.pixelmusic.audiolab.audio.LoudnessParams
import com.saurav.pixelmusic.audiolab.audio.PeakLimiter
import com.saurav.pixelmusic.audiolab.audio.ReplayGainMode
import com.saurav.pixelmusic.audiolab.audio.ReplayGainParams
import com.saurav.pixelmusic.audiolab.audio.ResponseCurve
import com.saurav.pixelmusic.audiolab.audio.SpatialMode
import com.saurav.pixelmusic.audiolab.audio.SpatialParams
import com.saurav.pixelmusic.audiolab.audio.VolumeCurve
import com.saurav.pixelmusic.audiolab.autoeq.AutoEqTuning
import com.saurav.pixelmusic.audiolab.autoeq.HeadphoneForm
import com.saurav.pixelmusic.audiolab.data.EQProfileRepository
import com.saurav.pixelmusic.audiolab.data.FilterType
import com.saurav.pixelmusic.audiolab.data.ParametricEQ
import com.saurav.pixelmusic.audiolab.data.ParametricEQBand
import com.saurav.pixelmusic.audiolab.stage.StageNative
import com.saurav.pixelmusic.audiolab.stage.StageOutput
import com.saurav.pixelmusic.audiolab.stage.StageSettings
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max
import kotlin.math.pow

/** what sonic, the player's speed stage, can do */
const val MinSpeed = 0.1f
const val MaxSpeed = 8f

/** a headphone correction picked from the autoeq database, with the filters it came to */
@Serializable
data class AutoEqCorrection(
    val model: String,
    val source: String,
    val rig: String? = null,
    val path: String,
    val form: HeadphoneForm = HeadphoneForm.OverEar,
    val tuning: AutoEqTuning = AutoEqTuning(),
    val filters: List<ParametricEQBand>,
    val preamp: Double,
)

@Serializable
data class EqPreset(
    val id: String,
    val name: String,
    val nodes: List<ParametricEQBand>,
    val preamp: Double = 0.0,
)

@Serializable
data class EqSettings(
    val enabled: Boolean = false,
    /** the user's own curve: the graph's nodes and the bands' sliders are the same list */
    val nodes: List<ParametricEQBand> = EqPresets.flatNodes(),
    val preamp: Double = 0.0,
    /** pulls the level down by the part of the boost the limiter can't catch, so nothing clips */
    val autoPreamp: Boolean = true,
    /** the preset the curve still matches; null once it's been edited */
    val presetId: String? = EqPresets.FlatId,
    val correction: AutoEqCorrection? = null,
    val correctionEnabled: Boolean = true,
)

@Serializable
data class LoudnessSettings(
    val enabled: Boolean = false,
    val boostDb: Float = 4f,
    val compression: Float = 0.3f,
) {
    fun toParams() = LoudnessParams(enabled, boostDb, compression)
}

@Serializable
data class SpatialSettings(
    val enabled: Boolean = false,
    val mode: SpatialMode = SpatialMode.Crossfeed,
    val cutoffHz: Float = 700f,
    val feedDb: Float = 4.5f,
    val speakerAngle: Float = 30f,
    val amount: Float = 1f,
    /** dolby atmos / surround rendered binaurally by pixelmusic rather than by the device */
    val surround: Boolean = false,
    /** both ears hear the whole mix */
    val mono: Boolean = false,
) {
    fun toParams() = SpatialParams(enabled, mode, cutoffHz, feedDb, speakerAngle, amount, surround, mono)
}

/** "even out songs" for files that carry replaygain */
@Serializable
data class NormalizeSettings(
    val replayGainMode: ReplayGainMode = ReplayGainMode.Track,
    val preampDb: Float = 3f,
) {
    fun toParams() = ReplayGainParams(replayGainMode, preampDb)
}

@Serializable
data class PlaybackSettings(
    val speed: Float = 1f,
    /** off: pitch follows speed, like a record played fast or slow */
    val preservePitch: Boolean = true,
    /** semitones, while pitch is preserved */
    val pitchSemitones: Float = 0f,
) {
    val pitch: Float
        get() = if (preservePitch) 2f.pow(pitchSemitones / 12f) else speed

    val isNormal: Boolean get() = speed == 1f && pitch == 1f
}

@Serializable
data class VolumeSettings(
    val enabled: Boolean = false,
    val exponent: Float = 2f,
    val limit: Float = 1f,
) {
    fun toCurve() = VolumeCurve(enabled, exponent, limit)
}

@Serializable
data class EightDSettings(
    val enabled: Boolean = false,
    val rotationSpeedHz: Float = 0.15f,
    val depth: Float = 0.85f,
    val reverbLevel: Float = 0.4f,
) {
    fun normalized() = copy(
        rotationSpeedHz = rotationSpeedHz.coerceIn(0.05f, 0.5f),
        depth = depth.coerceIn(0f, 1f),
        reverbLevel = reverbLevel.coerceIn(0f, 1f),
    )

    fun toParams() = EightDParams(enabled, rotationSpeedHz, depth, reverbLevel)
}

/** everything audio lab sets (equalizer and audio fx), each change goes to the players live and is saved a moment later, so a slider can be dragged freely */
@Singleton
class AudioLabStore @Inject constructor(
    @ApplicationContext private val context: Context,
    private val equalizerService: EqualizerService,
    private val audioFx: AudioFxService,
    private val legacyProfiles: EQProfileRepository,
) {
    private val prefs = context.getSharedPreferences("audio_lab", Context.MODE_PRIVATE)
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var saveJob: Job? = null
    private val headroomCurve = ResponseCurve(ResponseCurve.logSpaced(160))
    private val boostRoom = headroomCurve.frequencies.map(PeakLimiter::boostRoomDb).toDoubleArray()

    private val _eq = MutableStateFlow(load("eq") ?: EqSettings())
    val eq: StateFlow<EqSettings> = _eq.asStateFlow()

    private val _songPreset = MutableStateFlow<EqPreset?>(null)
    val songPreset: StateFlow<EqPreset?> = _songPreset.asStateFlow()

    private val _reverb = MutableStateFlow(load("reverb") ?: ReverbSettings())
    val reverb: StateFlow<ReverbSettings> = _reverb.asStateFlow()

    private val _presets = MutableStateFlow(
        loadWithSerializer("presets", kotlinx.serialization.builtins.ListSerializer(EqPreset.serializer())) ?: emptyList()
    )
    val presets: StateFlow<List<EqPreset>> = _presets.asStateFlow()

    private val _fxPresets = MutableStateFlow(
        loadWithSerializer("fx_presets", kotlinx.serialization.builtins.ListSerializer(FxPreset.serializer())) ?: emptyList()
    )
    val fxPresets: StateFlow<List<FxPreset>> = _fxPresets.asStateFlow()

    private val _fxPresetId = MutableStateFlow(
        prefs.getString("fx_preset_id", null)?.takeIf { id ->
            FxPresets.isBuiltIn(id) || _fxPresets.value.any { it.id == id }
        }
    )
    val fxPresetId: StateFlow<String?> = _fxPresetId.asStateFlow()

    private val _loudness = MutableStateFlow(load("loudness") ?: LoudnessSettings())
    val loudness: StateFlow<LoudnessSettings> = _loudness.asStateFlow()

    private val _spatial = MutableStateFlow(load("spatial") ?: SpatialSettings())
    val spatial: StateFlow<SpatialSettings> = _spatial.asStateFlow()

    private val _eightD = MutableStateFlow((load("eight_d") ?: EightDSettings()).normalized())
    val eightD: StateFlow<EightDSettings> = _eightD.asStateFlow()

    private val _playback = MutableStateFlow(load("playback") ?: PlaybackSettings())
    val playback: StateFlow<PlaybackSettings> = _playback.asStateFlow()

    private val _volume = MutableStateFlow(load("volume") ?: VolumeSettings())
    val volume: StateFlow<VolumeSettings> = _volume.asStateFlow()

    private val _normalize = MutableStateFlow(load("normalize") ?: NormalizeSettings())
    val normalize: StateFlow<NormalizeSettings> = _normalize.asStateFlow()

    private val _normalizeEnabled = MutableStateFlow(prefs.getBoolean("normalize_enabled", true))
    val normalizeEnabled: StateFlow<Boolean> = _normalizeEnabled.asStateFlow()

    fun setNormalizeEnabled(enabled: Boolean) {
        _normalizeEnabled.value = enabled
        audioFx.normalize = enabled
        prefs.edit { putBoolean("normalize_enabled", enabled) }
    }

    private val _stage = MutableStateFlow(load("stage") ?: StageSettings())
    val stage: StateFlow<StageSettings> = _stage.asStateFlow()

    private val _stageOutput = MutableStateFlow(StageOutput.PhoneSpeaker)
    val stageOutput: StateFlow<StageOutput> = _stageOutput.asStateFlow()

    @Volatile
    private var hrtfRequested = false

    val shapesSound: Flow<Boolean> = combine(
        combine(eq, reverb, loudness, spatial, songPreset) { eq, reverb, loudness, spatial, songPreset ->
            eq.enabled || songPreset != null || reverb.enabled || loudness.enabled || spatial.enabled || spatial.mono
        },
        stage,
        eightD,
    ) { others, stage, eightD -> others || stage.enabled || eightD.enabled }.distinctUntilChanged()

    init {
        migrateLegacyEqualizer()
        pushEq()
        pushFx()
    }

    fun updateReverb(transform: (ReverbSettings) -> ReverbSettings) {
        _reverb.update(transform)
        pushFx()
        saveIntoFxPreset()
        scheduleSave()
    }

    fun updateLoudness(transform: (LoudnessSettings) -> LoudnessSettings) {
        _loudness.update(transform)
        pushFx()
        saveIntoFxPreset()
        scheduleSave()
    }

    fun updateSpatial(transform: (SpatialSettings) -> SpatialSettings) {
        _spatial.update(transform)
        pushFx()
        saveIntoFxPreset()
        scheduleSave()
    }

    fun updateEightD(transform: (EightDSettings) -> EightDSettings) {
        _eightD.update { transform(it).normalized() }
        pushFx()
        saveIntoFxPreset()
        scheduleSave()
    }

    fun updateStage(transform: (StageSettings) -> StageSettings) {
        _stage.update(transform)
        pushFx()
        saveIntoFxPreset()
        scheduleSave()
    }

    fun setStageOutput(output: StageOutput) {
        if (_stageOutput.value == output) return
        _stageOutput.value = output
        pushFx()
    }

    fun updatePlayback(transform: (PlaybackSettings) -> PlaybackSettings) {
        _playback.update { transform(it).let { s -> s.copy(speed = s.speed.coerceIn(MinSpeed, MaxSpeed)) } }
        saveIntoFxPreset()
        scheduleSave()
    }

    fun updateNormalize(transform: (NormalizeSettings) -> NormalizeSettings) {
        _normalize.update(transform)
        pushFx()
        scheduleSave()
    }

    fun updateVolume(transform: (VolumeSettings) -> VolumeSettings) {
        _volume.update(transform)
        pushFx()
        saveIntoFxPreset()
        scheduleSave()
    }

    fun updateEq(transform: (EqSettings) -> EqSettings) {
        _eq.update(transform)
        pushEq()
        scheduleSave()
    }

    fun editEqCurve(transform: (EqSettings) -> EqSettings) {
        updateEq { s ->
            val next = transform(s)
            if (next == s) s else next.copy(presetId = s.presetId?.takeIf { id -> _presets.value.any { it.id == id } })
        }
        saveIntoEqPreset()
    }

    fun setEqPreamp(db: Double) {
        updateEq { it.copy(preamp = db) }
        saveIntoEqPreset()
    }

    fun savePreset(name: String) {
        val current = _eq.value
        val preset = EqPreset("user_${System.currentTimeMillis()}", name.trim(), current.nodes, current.preamp)
        _presets.update { it + preset }
        updateEq { it.copy(presetId = preset.id) }
    }

    fun renamePreset(id: String, name: String) {
        _presets.update { list -> list.map { if (it.id == id) it.copy(name = name.trim()) else it } }
        scheduleSave()
    }

    private fun saveIntoEqPreset() {
        val eq = _eq.value
        val id = eq.presetId ?: return
        _presets.update { list -> list.map { if (it.id == id) it.copy(nodes = eq.nodes, preamp = eq.preamp) else it } }
    }

    fun saveFxPreset(name: String) {
        val preset = currentFx("fx_${System.currentTimeMillis()}", name.trim())
        _fxPresets.update { it + preset }
        _fxPresetId.value = preset.id
        saveNow()
    }

    fun applyFxPreset(id: String) {
        val preset = _fxPresets.value.firstOrNull { it.id == id }
            ?: FxPresets.BuiltIn.firstOrNull { it.id == id }
            ?: return
        _reverb.value = preset.reverb
        _eightD.value = preset.eightD.normalized()
        _loudness.value = preset.loudness
        _spatial.value = preset.spatial
        _playback.value = preset.playback.let { it.copy(speed = it.speed.coerceIn(MinSpeed, MaxSpeed)) }
        _volume.value = preset.volume
        _stage.value = preset.stage.copy(outputKinds = _stage.value.outputKinds)
        if (_stage.value.enabled) loadHrtf()
        _fxPresetId.value = id
        pushFx()
        saveNow()
    }

    fun releaseFxPreset() {
        if (_fxPresetId.value == null) return
        _fxPresetId.value = null
        saveNow()
    }

    fun renameFxPreset(id: String, name: String) {
        if (FxPresets.isBuiltIn(id)) return
        _fxPresets.update { list -> list.map { if (it.id == id) it.copy(name = name.trim()) else it } }
        saveNow()
    }

    fun deleteFxPreset(id: String) {
        if (FxPresets.isBuiltIn(id)) return
        _fxPresets.update { list -> list.filterNot { it.id == id } }
        if (_fxPresetId.value == id) _fxPresetId.value = null
        saveNow()
    }

    private fun currentFx(id: String, name: String) = FxPreset(
        id = id,
        name = name,
        reverb = _reverb.value,
        eightD = _eightD.value,
        loudness = _loudness.value,
        spatial = _spatial.value,
        playback = _playback.value,
        volume = _volume.value,
        stage = _stage.value.copy(outputKinds = emptyMap()),
    )

    private fun saveIntoFxPreset() {
        val id = _fxPresetId.value ?: return
        if (FxPresets.isBuiltIn(id)) {
            _fxPresetId.value = null
            return
        }
        _fxPresets.update { list -> list.map { if (it.id == id) currentFx(it.id, it.name) else it } }
    }

    fun useSongPreset(preset: EqPreset?) {
        if (_songPreset.value == preset) return
        _songPreset.value = preset
        pushEq()
    }

    fun presetById(id: String): EqPreset? =
        _presets.value.firstOrNull { it.id == id }
            ?: EqPresets.BuiltIn.firstOrNull { it.id == id }?.let { EqPreset(it.id, it.name, it.nodes) }

    fun deletePreset(id: String) {
        _presets.update { list -> list.filterNot { it.id == id } }
        if (_eq.value.presetId == id) updateEq { it.copy(presetId = null) } else scheduleSave()
    }

    fun autoHeadroom(settings: EqSettings = _eq.value): Double = synchronized(headroomCurve) {
        val response = headroomCurve.response(audibleBands(settings))
        var over = 0.0
        for (i in response.indices) over = max(over, response[i] - boostRoom[i])
        -over
    }

    fun design(settings: EqSettings): ParametricEQ? {
        if (!settings.enabled) return null
        val bands = audibleBands(settings)
        val correction = settings.correction?.takeIf { settings.correctionEnabled }
        val headroom = when {
            settings.autoPreamp -> autoHeadroom(settings)
            correction != null -> correction.preamp
            else -> 0.0
        }
        val preamp = settings.preamp + headroom
        if (bands.isEmpty() && preamp == 0.0) return null
        return ParametricEQ(preamp = preamp, bands = bands)
    }

    private fun audibleBands(settings: EqSettings): List<ParametricEQBand> {
        val own = settings.nodes.filter(Biquad::isAudible)
        val correction = if (settings.correctionEnabled) settings.correction?.filters.orEmpty() else emptyList()
        return own + correction.filter(Biquad::isAudible)
    }

    private fun pushEq() {
        val song = _songPreset.value
        val playing = if (song == null) _eq.value else _eq.value.copy(enabled = true, nodes = song.nodes, preamp = song.preamp, presetId = song.id)
        equalizerService.apply(design(playing))
    }

    private fun pushFx() {
        audioFx.reverb = _reverb.value.toParams()
        audioFx.loudness = _loudness.value.toParams()
        audioFx.spatial = _spatial.value.toParams()
        val stage = _stage.value
        audioFx.stage = stage.toParams(speaker = _stageOutput.value.isSpeaker(stage))
        if (stage.enabled) loadHrtf()
        audioFx.eightD = _eightD.value.toParams()
        audioFx.replayGain = _normalize.value.toParams()
        audioFx.normalize = _normalizeEnabled.value
        val curve = _volume.value.toCurve()
        if (curve != audioFx.volumeCurve) audioFx.volumeCurve = curve
    }

    private fun saveNow() {
        saveJob?.cancel()
        saveJob = scope.launch { persistAll() }
    }

    private fun scheduleSave() {
        saveJob?.cancel()
        saveJob = scope.launch {
            delay(400)
            persistAll()
        }
    }

    private fun persistAll() {
        prefs.edit {
            putString("eq", json.encodeToString(EqSettings.serializer(), _eq.value))
            putString("reverb", json.encodeToString(ReverbSettings.serializer(), _reverb.value))
            putString("loudness", json.encodeToString(LoudnessSettings.serializer(), _loudness.value))
            putString("spatial", json.encodeToString(SpatialSettings.serializer(), _spatial.value))
            putString("eight_d", json.encodeToString(EightDSettings.serializer(), _eightD.value))
            putString("playback", json.encodeToString(PlaybackSettings.serializer(), _playback.value))
            putString("volume", json.encodeToString(VolumeSettings.serializer(), _volume.value))
            putString("normalize", json.encodeToString(NormalizeSettings.serializer(), _normalize.value))
            putString("stage", json.encodeToString(StageSettings.serializer(), _stage.value))
            putString(
                "presets",
                json.encodeToString(kotlinx.serialization.builtins.ListSerializer(EqPreset.serializer()), _presets.value),
            )
            putString(
                "fx_presets",
                json.encodeToString(kotlinx.serialization.builtins.ListSerializer(FxPreset.serializer()), _fxPresets.value),
            )
            val fxPresetId = _fxPresetId.value
            if (fxPresetId != null) putString("fx_preset_id", fxPresetId) else remove("fx_preset_id")
        }
    }

    private fun loadHrtf() {
        if (hrtfRequested) return
        hrtfRequested = true
        scope.launch {
            runCatching {
                StageNative.loaded && StageNative.loadHrtf(context.assets.open("stage_hrtf.bin").use { it.readBytes() })
            }.onFailure { Timber.tag("AudioLab").w(it, "Couldn't load the 3D Sims ear responses") }
        }
    }

    private inline fun <reified T> load(key: String): T? = prefs.getString(key, null)?.let {
        runCatching { json.decodeFromString<T>(it) }
            .onFailure { e -> Timber.tag("AudioLab").w(e, "Couldn't read saved $key") }
            .getOrNull()
    }

    private fun <T> loadWithSerializer(key: String, serializer: kotlinx.serialization.KSerializer<T>): T? =
        prefs.getString(key, null)?.let {
            runCatching { json.decodeFromString(serializer, it) }
                .onFailure { e -> Timber.tag("AudioLab").w(e, "Couldn't read saved $key") }
                .getOrNull()
        }

    private fun migrateLegacyEqualizer() {
        if (prefs.getBoolean("migrated_legacy_eq", false)) return
        runCatching {
            val legacyActive = legacyProfiles.getActiveProfile()
            if (prefs.getString("eq", null) == null && legacyActive != null && legacyActive.bands.isNotEmpty()) {
                _eq.value = EqSettings(
                    enabled = true,
                    nodes = legacyActive.bands,
                    preamp = legacyActive.preamp,
                    presetId = null,
                )
            }
            val saved = legacyProfiles.getSortedProfiles().filter { it.id != "musicfy_custom_eq" && it.bands.isNotEmpty() }
            if (saved.isNotEmpty()) {
                _presets.update { it + saved.map { p -> EqPreset("legacy_${p.id}", p.name, p.bands, p.preamp) } }
            }
        }.onFailure { Timber.tag("AudioLab").w(it, "Legacy equalizer migration failed") }
        prefs.edit { putBoolean("migrated_legacy_eq", true) }
        scheduleSave()
    }
}

object EqPresets {
    const val FlatId = "flat"

    val Frequencies = doubleArrayOf(31.0, 62.0, 125.0, 250.0, 500.0, 1000.0, 2000.0, 4000.0, 8000.0, 16000.0)
    private const val OctaveQ = 1.41

    class Preset(val id: String, val name: String, val gains: DoubleArray) {
        val nodes: List<ParametricEQBand> get() = nodesFor(gains)
    }

    fun nodesFor(gains: DoubleArray): List<ParametricEQBand> = Frequencies.mapIndexed { i, f ->
        ParametricEQBand(frequency = f, gain = gains.getOrElse(i) { 0.0 }, q = OctaveQ, filterType = FilterType.PK)
    }

    fun flatNodes(): List<ParametricEQBand> = nodesFor(DoubleArray(Frequencies.size))

    val BuiltIn = listOf(
        Preset(FlatId, "Flat", doubleArrayOf(0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0)),
        Preset("acoustic", "Acoustic", doubleArrayOf(4.5, 4.5, 3.5, 1.0, 1.5, 1.5, 3.0, 3.5, 3.0, 1.5)),
        Preset("bass_boost", "Bass Booster", doubleArrayOf(5.5, 4.5, 3.5, 2.5, 1.2, 0.0, 0.0, 0.0, 0.0, 0.0)),
        Preset("bass_cut", "Bass Reducer", doubleArrayOf(-5.5, -4.5, -3.5, -2.5, -1.2, 0.0, 0.0, 0.0, 0.0, 0.0)),
        Preset("classical", "Classical", doubleArrayOf(4.5, 3.5, 3.0, 2.5, -1.5, -1.5, 0.0, 2.0, 3.0, 3.5)),
        Preset("dance", "Dance", doubleArrayOf(3.5, 6.5, 5.0, 0.0, 2.0, 3.5, 5.0, 4.5, 3.5, 0.0)),
        Preset("deep", "Deep", doubleArrayOf(5.0, 3.5, 1.5, 1.0, 3.0, 2.5, 1.5, -2.0, -3.5, -4.5)),
        Preset("electronic", "Electronic", doubleArrayOf(4.5, 4.0, 1.5, 0.0, -2.0, 2.0, 1.0, 1.5, 4.5, 5.0)),
        Preset("hiphop", "Hip-Hop", doubleArrayOf(5.0, 4.0, 1.5, 3.0, -1.0, -1.0, 1.5, -0.5, 2.0, 3.0)),
        Preset("jazz", "Jazz", doubleArrayOf(4.0, 3.0, 1.5, 2.0, -1.5, -1.5, 0.0, 1.5, 3.0, 3.5)),
        Preset("loudness", "Loudness", doubleArrayOf(6.0, 4.0, 0.0, 0.0, -2.0, 0.0, -1.0, -5.0, 5.0, 1.0)),
        Preset("lounge", "Lounge", doubleArrayOf(-3.0, -1.5, -0.5, 1.5, 4.0, 2.5, 0.0, -1.5, 2.0, 1.0)),
        Preset("piano", "Piano", doubleArrayOf(3.0, 2.0, 0.0, 2.5, 3.0, 1.5, 3.5, 4.5, 3.0, 3.5)),
        Preset("pop", "Pop", doubleArrayOf(-1.5, -1.0, 0.0, 2.0, 4.0, 4.0, 2.0, 0.0, -1.0, -1.5)),
        Preset("rnb", "R&B", doubleArrayOf(2.5, 7.0, 5.5, 1.5, -2.5, -1.5, 2.5, 3.0, 3.0, 3.5)),
        Preset("rock", "Rock", doubleArrayOf(5.0, 4.0, 3.0, 1.5, -0.5, -1.0, 0.5, 2.5, 3.5, 4.5)),
        Preset("small_speakers", "Small Speakers", doubleArrayOf(5.5, 4.0, 3.5, 2.5, 1.0, 0.0, -1.0, -2.0, -3.5, -4.0)),
        Preset("spoken", "Spoken Word", doubleArrayOf(-3.5, -0.5, 0.0, 0.5, 3.5, 4.5, 5.0, 4.0, 2.5, 0.0)),
        Preset("treble_boost", "Treble Booster", doubleArrayOf(0.0, 0.0, 0.0, 0.0, 0.0, 1.0, 2.5, 3.5, 4.5, 5.5)),
        Preset("treble_cut", "Treble Reducer", doubleArrayOf(0.0, 0.0, 0.0, 0.0, 0.0, -1.0, -2.5, -3.5, -4.5, -5.5)),
        Preset("vocal", "Vocal Booster", doubleArrayOf(-1.5, -3.0, -3.0, 1.5, 3.5, 3.5, 3.0, 1.5, 0.0, -1.5)),
    )
}
