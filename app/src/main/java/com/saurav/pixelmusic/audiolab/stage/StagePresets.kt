package com.saurav.pixelmusic.audiolab.stage

import androidx.annotation.StringRes
import com.saurav.pixelmusic.R

/** ready-made rooms: where everything sits, where you sit, and the room around it */
object StagePresets {
    const val DefaultId = "studio"

    class Preset(
        val id: String,
        @StringRes val name: Int,
        val objects: List<StageObject>,
        val listener: StageListener,
        val roomSize: Float,
        val walls: StageWalls,
        val roomAmount: Float,
    )

    private class Spot(val x: Float, val y: Float, val z: Float)

    private fun at(x: Float, y: Float, z: Float) = Spot(x, y, z)

    private fun layout(vocal: Spot, center: Spot, left: Spot, right: Spot, ambience: Spot, width: Float, subs: List<Spot>): List<StageObject> {
        val main = listOf(
            StageSource.Vocal to vocal, StageSource.Center to center, StageSource.Left to left,
            StageSource.Right to right, StageSource.Ambience to ambience,
        ).mapIndexed { id, (source, spot) -> StageObject(id, source, spot.x, spot.y, spot.z, width = width) }
        return main + subs.mapIndexed { i, spot -> StageObject(main.size + i, StageSource.Sub, spot.x, spot.y, spot.z) }
    }

    val Default = Preset(
        id = DefaultId,
        name = R.string.stage_preset_studio,
        objects = layout(
            vocal = at(0f, 0.42f, 0.45f), center = at(0f, 0.4f, 0.52f),
            left = at(-0.62f, 0.4f, 0.42f), right = at(0.62f, 0.4f, 0.42f),
            ambience = at(0f, 0.75f, -0.7f), width = 0.6f, subs = listOf(at(0f, 0.06f, 0.55f)),
        ),
        listener = StageListener(0f, 0.39f, -0.2f),
        roomSize = 0.18f,
        walls = StageWalls.Wood,
        roomAmount = 0.35f,
    )

    val All = listOf(
        Default,
        Preset(
            id = "living",
            name = R.string.stage_preset_living,
            objects = layout(
                vocal = at(0f, 0.38f, 0.6f), center = at(0f, 0.3f, 0.7f),
                left = at(-0.8f, 0.36f, 0.6f), right = at(0.8f, 0.36f, 0.6f),
                ambience = at(0f, 0.6f, -0.85f), width = 0.9f, subs = listOf(at(-0.85f, 0.05f, 0.85f)),
            ),
            listener = StageListener(0f, 0.35f, -0.15f),
            roomSize = 0.3f,
            walls = StageWalls.Soft,
            roomAmount = 0.45f,
        ),
        Preset(
            id = "front_row",
            name = R.string.stage_preset_front_row,
            objects = layout(
                vocal = at(0f, 0.3f, 0.55f), center = at(0f, 0.25f, 0.75f),
                left = at(-0.45f, 0.28f, 0.6f), right = at(0.45f, 0.28f, 0.6f),
                ambience = at(0f, 0.6f, -0.8f), width = 1f, subs = listOf(at(-0.6f, 0.04f, 0.7f), at(0.6f, 0.04f, 0.7f)),
            ),
            listener = StageListener(0f, 0.21f, -0.3f),
            roomSize = 0.6f,
            walls = StageWalls.Wood,
            roomAmount = 0.55f,
        ),
        Preset(
            id = "on_stage",
            name = R.string.stage_preset_on_stage,
            objects = layout(
                vocal = at(0f, 0.3f, 0.25f), center = at(0f, 0.25f, -0.45f),
                left = at(-0.7f, 0.28f, 0.05f), right = at(0.7f, 0.28f, 0.05f),
                ambience = at(0f, 0.5f, 0.9f), width = 1f, subs = listOf(at(0f, 0.04f, -0.5f)),
            ),
            listener = StageListener(0f, 0.27f, 0f),
            roomSize = 0.45f,
            walls = StageWalls.Wood,
            roomAmount = 0.4f,
        ),
        Preset(
            id = "around",
            name = R.string.stage_preset_around,
            objects = layout(
                vocal = at(0f, 0.38f, 0.5f), center = at(0f, 0.9f, 0.1f),
                left = at(-0.75f, 0.38f, -0.35f), right = at(0.75f, 0.38f, -0.35f),
                ambience = at(0f, 0.85f, -0.6f), width = 0.8f, subs = listOf(at(0f, 0.05f, 0.6f)),
            ),
            listener = StageListener(0f, 0.35f, 0f),
            roomSize = 0.3f,
            walls = StageWalls.Soft,
            roomAmount = 0.4f,
        ),
        Preset(
            id = "hall",
            name = R.string.stage_preset_hall,
            objects = layout(
                vocal = at(0f, 0.2f, 0.55f), center = at(0f, 0.18f, 0.7f),
                left = at(-0.4f, 0.2f, 0.6f), right = at(0.4f, 0.2f, 0.6f),
                ambience = at(0f, 0.6f, -0.2f), width = 1f, subs = listOf(at(0f, 0.02f, 0.6f)),
            ),
            listener = StageListener(0f, 0.15f, -0.35f),
            roomSize = 0.9f,
            walls = StageWalls.Concrete,
            roomAmount = 0.7f,
        ),
    )

    fun apply(settings: StageSettings, preset: Preset) = settings.copy(
        presetId = preset.id,
        objects = preset.objects,
        listener = preset.listener,
        roomSize = preset.roomSize,
        walls = preset.walls,
        roomAmount = preset.roomAmount,
    )
}
