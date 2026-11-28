package com.saurav.pixelmusic.audiolab

import com.saurav.pixelmusic.audiolab.audio.EightDParams
import com.saurav.pixelmusic.audiolab.audio.LoudnessParams
import com.saurav.pixelmusic.audiolab.audio.ReplayGainParams
import com.saurav.pixelmusic.audiolab.audio.ReverbParams
import com.saurav.pixelmusic.audiolab.audio.SpatialParams
import com.saurav.pixelmusic.audiolab.audio.VolumeCurve
import com.saurav.pixelmusic.audiolab.stage.StageNative
import com.saurav.pixelmusic.audiolab.stage.StageParams
import javax.inject.Inject
import javax.inject.Singleton

/** the audio fx settings every player's effects read at the start of each buffer */
@Singleton
class AudioFxService @Inject constructor() {
    @Volatile
    var eightD: EightDParams = EightDParams()

    @Volatile
    var reverb: ReverbParams = ReverbParams()

    @Volatile
    var loudness: LoudnessParams = LoudnessParams()

    @Volatile
    var spatial: SpatialParams = SpatialParams()

    /** 3d sims as the native room reads it; while it plays, spatial's own stereo modes step aside */
    @Volatile
    var stage: StageParams = StageParams.Off

    /** how loud each kind of sound in the room is right now (0..1, StageSource order), written by the playback thread for the room's dots */
    val stageLevels = FloatArray(StageNative.Kinds)

    /** replaced, never mutated: the sink notices a new curve by identity */
    @Volatile
    var volumeCurve: VolumeCurve = VolumeCurve()

    /** "even out songs": youtube's loudness numbers, or a file's own replaygain */
    @Volatile
    var normalize: Boolean = true

    @Volatile
    var replayGain: ReplayGainParams = ReplayGainParams()

    /** how much the loudness stage is pulling peaks down right now, in db (0 or less), for the meter */
    @Volatile
    var loudnessReductionDb: Float = 0f
}
