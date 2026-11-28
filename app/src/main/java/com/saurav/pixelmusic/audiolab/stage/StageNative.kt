package com.saurav.pixelmusic.audiolab.stage

import java.nio.ByteBuffer

/** the jni half of 3d sims (app/src/main/cpp/stage); handles are only ever passed back here, on the playback thread */
internal object StageNative {

    /** loads the library the first time the room is wanted, so nobody else pays for it */
    val loaded: Boolean by lazy { runCatching { System.loadLibrary("pixelmusicstage") }.isSuccess }

    const val Enabled = 0
    const val Mode = 1
    const val RoomWidth = 2
    const val RoomDepth = 3
    const val RoomHeight = 4
    const val Absorption = 5
    const val Damping = 6
    const val RoomAmount = 7
    const val ListenerX = 8
    const val ListenerY = 9
    const val ListenerZ = 10
    const val ListenerYaw = 11
    const val Master = 12
    const val Objects = 13
    const val ObjectStride = 5
    const val Copies = 4
    const val Kinds = 6
    const val ObjectCount = Kinds * Copies
    const val ParamCount = Objects + ObjectCount * ObjectStride

    const val ModeHeadphones = 0f
    const val ModeSpeakers = 1f

    external fun loadHrtf(data: ByteArray): Boolean
    external fun create(sampleRate: Int, channels: Int): Long
    external fun setParams(handle: Long, params: FloatArray)
    external fun process(handle: Long, input: ByteBuffer, inputOffset: Int, output: ByteBuffer, frames: Int): Boolean
    external fun levels(handle: Long, out: FloatArray)
    external fun clear(handle: Long)
    external fun destroy(handle: Long)
}
