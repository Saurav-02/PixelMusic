package com.saurav.pixelmusic.audiolab.stage

import kotlinx.serialization.Serializable
import kotlin.math.pow

@Serializable
enum class StageSource { Vocal, Center, Left, Right, Ambience, Sub }

@Serializable
data class StageObject(
    val id: Int,
    val source: StageSource,
    val x: Float,
    val y: Float,
    val z: Float,
    val levelDb: Float = 0f,
    val muted: Boolean = false,
    val solo: Boolean = false,
    val width: Float = 0.6f,
)

@Serializable
data class StageListener(val x: Float = 0f, val y: Float = 0.4f, val z: Float = 0f, val yaw: Float = 0f)

@Serializable
enum class StageWalls(val absorption: Float, val damping: Float) {
    Soft(0.45f, 0.7f),
    Wood(0.22f, 0.45f),
    Concrete(0.08f, 0.15f),
}

@Serializable
enum class StageSpeakers { Off, Stereo, Keep3D }

data class StageBox(val width: Float, val depth: Float, val height: Float)

@Serializable
data class StageSettings(
    val enabled: Boolean = false,
    val presetId: String? = StagePresets.DefaultId,
    val objects: List<StageObject> = StagePresets.Default.objects,
    val listener: StageListener = StagePresets.Default.listener,
    val roomSize: Float = StagePresets.Default.roomSize,
    val walls: StageWalls = StagePresets.Default.walls,
    val roomAmount: Float = StagePresets.Default.roomAmount,
    val levelDb: Float = 0f,
    val onSpeakers: StageSpeakers = StageSpeakers.Stereo,
    val outputKinds: Map<String, Boolean> = emptyMap(),
) {
    val box: StageBox
        get() {
            val width = 3.5f * (22f / 3.5f).pow(roomSize)
            return StageBox(width, width * 1.3f, 2.6f + 6.4f * roomSize.pow(1.5f))
        }

    fun playsOn(speaker: Boolean): Boolean = enabled && !(speaker && onSpeakers == StageSpeakers.Off)

    fun toParams(speaker: Boolean): StageParams {
        val values = FloatArray(StageNative.ParamCount)
        val box = box
        val wet = playsOn(speaker)
        values[StageNative.Enabled] = if (wet) 1f else 0f
        values[StageNative.Mode] = if (speaker && onSpeakers == StageSpeakers.Stereo) StageNative.ModeSpeakers else StageNative.ModeHeadphones
        values[StageNative.RoomWidth] = box.width
        values[StageNative.RoomDepth] = box.depth
        values[StageNative.RoomHeight] = box.height
        values[StageNative.Absorption] = walls.absorption
        values[StageNative.Damping] = walls.damping
        values[StageNative.RoomAmount] = roomAmount
        values[StageNative.ListenerX] = listener.x * box.width / 2
        values[StageNative.ListenerY] = listener.y * box.height
        values[StageNative.ListenerZ] = listener.z * box.depth / 2
        values[StageNative.ListenerYaw] = listener.yaw
        values[StageNative.Master] = dbToGain(levelDb)

        val soloing = objects.any { it.solo && !it.muted }
        fun gainOf(o: StageObject) = if (o.muted || (soloing && !o.solo)) 0f else dbToGain(o.levelDb)
        fun put(slot: Int, o: StageObject, gain: Float) {
            val at = StageNative.Objects + slot * StageNative.ObjectStride
            values[at] = o.x * box.width / 2
            values[at + 1] = o.y * box.height
            values[at + 2] = o.z * box.depth / 2
            values[at + 3] = gain
            values[at + 4] = o.width
        }
        StageSource.entries.forEach { source ->
            val copies = objects.filter { it.source == source }.take(StageNative.Copies)
            copies.forEachIndexed { i, o -> put(source.ordinal * StageNative.Copies + i, o, gainOf(o) / copies.size) }
        }
        return StageParams(wet, values)
    }
}

class StageParams(val enabled: Boolean, val values: FloatArray) {
    companion object {
        val Off = StageParams(false, FloatArray(StageNative.ParamCount))
    }
}

internal fun dbToGain(db: Float): Float = 10f.pow(db / 20f)
