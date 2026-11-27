package com.saurav.pixelmusic.presentation.screens.audiolab.stage

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import com.saurav.pixelmusic.audiolab.stage.StageBox
import com.saurav.pixelmusic.audiolab.stage.StageSource
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

internal const val DefaultCameraYaw = -28f
internal const val DefaultCameraPitch = 24f
private const val CameraDistance = 3.4f

internal fun sourceColor(source: StageSource): Color = when (source) {
    StageSource.Vocal -> Color(0xFFF2D46B)
    StageSource.Center -> Color(0xFFF2A46B)
    StageSource.Left -> Color(0xFF6BBAF2)
    StageSource.Right -> Color(0xFFF28BA8)
    StageSource.Ambience -> Color(0xFF6BD9C4)
    StageSource.Sub -> Color(0xFFB98CF2)
}

internal class Projected(val point: Offset, val depth: Float, val scale: Float)

internal class StageView(size: Size, box: StageBox, yawDegrees: Float, pitchDegrees: Float) {
    private val unit = 1f / max(box.width, max(box.depth, box.height)) * 2f
    val halfX = box.width / 2 * unit
    val halfY = box.height / 2 * unit
    val halfZ = box.depth / 2 * unit

    private val yawCos = cos(Math.toRadians(yawDegrees.toDouble())).toFloat()
    private val yawSin = sin(Math.toRadians(yawDegrees.toDouble())).toFloat()
    private val pitchCos = cos(Math.toRadians(pitchDegrees.toDouble())).toFloat()
    private val pitchSin = sin(Math.toRadians(pitchDegrees.toDouble())).toFloat()
    private val center = Offset(size.width / 2, size.height / 2)

    private val focal: Float = run {
        val radius = sqrt(halfX * halfX + halfY * halfY + halfZ * halfZ)
        min(size.width, size.height) / 2 * 1.08f * sqrt(CameraDistance * CameraDistance - radius * radius) / radius
    }

    val floorSquash = abs(pitchSin)

    fun pixels(at: Projected, worldSize: Float) = focal * worldSize / at.depth

    fun worldX(x: Float) = x * halfX
    fun worldY(y: Float) = (y - 0.5f) * 2 * halfY
    fun worldZ(z: Float) = z * halfZ

    fun project(x: Float, y: Float, z: Float): Projected = projectWorld(worldX(x), worldY(y), worldZ(z))

    fun projectWorld(wx: Float, wy: Float, wz: Float): Projected {
        val x1 = wx * yawCos + wz * yawSin
        val z1 = -wx * yawSin + wz * yawCos
        val y2 = wy * pitchCos + z1 * pitchSin
        val z2 = -wy * pitchSin + z1 * pitchCos
        val depth = z2 + CameraDistance
        val k = focal / depth
        return Projected(Offset(center.x + x1 * k, center.y - y2 * k), depth, CameraDistance / depth)
    }

    fun onLevel(point: Offset, y: Float): Pair<Float, Float>? {
        val planeY = worldY(y)
        val eye = toWorld(0f, 0f, -CameraDistance)
        val dir = toWorld((point.x - center.x) / focal, -(point.y - center.y) / focal, 1f)
        if (abs(dir[1]) < 1e-4f) return null
        val t = (planeY - eye[1]) / dir[1]
        if (t <= 0f) return null
        return (eye[0] + dir[0] * t) / halfX to (eye[2] + dir[2] * t) / halfZ
    }

    fun heightPixels(x: Float, y: Float, z: Float): Float {
        val a = project(x, y, z).point
        val b = project(x, (y + 0.5f), z).point
        return max(1f, (a - b).getDistance() * 2)
    }

    private fun toWorld(cx: Float, cy: Float, cz: Float): FloatArray {
        val y1 = cy * pitchCos - cz * pitchSin
        val z1 = cy * pitchSin + cz * pitchCos
        val wx = cx * yawCos - z1 * yawSin
        val wz = cx * yawSin + z1 * yawCos
        return floatArrayOf(wx, y1, wz)
    }
}
