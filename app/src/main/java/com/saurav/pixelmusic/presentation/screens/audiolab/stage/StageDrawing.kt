package com.saurav.pixelmusic.presentation.screens.audiolab.stage

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.drawText
import androidx.compose.ui.unit.dp
import com.saurav.pixelmusic.audiolab.stage.StageListener
import com.saurav.pixelmusic.audiolab.stage.StageSource
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

internal const val ListenerKey = -1
private const val HeadSize = 0.085f

internal class StageMark(
    val key: Int,
    val source: StageSource,
    val x: Float,
    val y: Float,
    val z: Float,
    val label: TextLayoutResult?,
    val level: Float,
    val quiet: Boolean,
    val spread: Float = 0f,
)

private val Edges = arrayOf(
    intArrayOf(0, 1), intArrayOf(1, 3), intArrayOf(3, 2), intArrayOf(2, 0),
    intArrayOf(4, 5), intArrayOf(5, 7), intArrayOf(7, 6), intArrayOf(6, 4),
    intArrayOf(0, 4), intArrayOf(1, 5), intArrayOf(2, 6), intArrayOf(3, 7),
)

internal fun DrawScope.drawStage(
    view: StageView,
    marks: List<StageMark>,
    listener: StageListener,
    selected: Int?,
    on: Float,
    compact: Boolean = false,
    inkColor: Color = Color.White,
) {
    drawRoomBox(view, on, inkColor)
    if (selected != null && !compact) {
        val mark = marks.firstOrNull { it.key == selected }
        if (mark != null) drawGuides(view, mark.x, mark.z, sourceColor(mark.source))
        else if (selected == ListenerKey) drawGuides(view, listener.x, listener.z, inkColor)
    }
    drawFacing(view, listener, on, inkColor)

    val headDepth = view.project(listener.x, listener.y, listener.z).depth
    val ordered = marks.sortedByDescending { view.project(it.x, it.y, it.z).depth }
    var headDrawn = false
    for (mark in ordered) {
        if (!headDrawn && view.project(mark.x, mark.y, mark.z).depth < headDepth) {
            drawHead(view, listener, selected == ListenerKey, on, compact, inkColor)
            headDrawn = true
        }
        drawMark(view, mark, selected == mark.key, on, compact, inkColor)
    }
    if (!headDrawn) drawHead(view, listener, selected == ListenerKey, on, compact, inkColor)
}

private fun DrawScope.drawRoomBox(view: StageView, on: Float, inkColor: Color) {
    val grid = inkColor.copy(alpha = 0.05f + 0.04f * on)
    for (i in 0..4) {
        val t = -1f + i * 0.5f
        drawLine(grid, view.project(t, 0f, -1f).point, view.project(t, 0f, 1f).point, 1.dp.toPx())
        drawLine(grid, view.project(-1f, 0f, t).point, view.project(1f, 0f, t).point, 1.dp.toPx())
    }
    val corners = Array(8) { i ->
        view.project(if (i and 1 == 0) -1f else 1f, if (i and 4 == 0) 0f else 1f, if (i and 2 == 0) -1f else 1f)
    }
    val farthest = corners.maxOf { it.depth }
    for ((a, b) in Edges) {
        val back = corners[a].depth == farthest || corners[b].depth == farthest
        val alpha = (if (back) 0.18f else 0.34f) * (0.6f + 0.4f * on)
        drawLine(inkColor.copy(alpha = alpha), corners[a].point, corners[b].point, 1.dp.toPx(), cap = StrokeCap.Round)
    }
}

private fun DrawScope.drawGuides(view: StageView, x: Float, z: Float, color: Color) {
    val line = color.copy(alpha = 0.25f)
    val dash = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 4.dp.toPx()))
    drawLine(line, view.project(-1f, 0f, z).point, view.project(1f, 0f, z).point, 1.dp.toPx(), pathEffect = dash)
    drawLine(line, view.project(x, 0f, -1f).point, view.project(x, 0f, 1f).point, 1.dp.toPx(), pathEffect = dash)
}

private fun DrawScope.drawFacing(view: StageView, listener: StageListener, on: Float, inkColor: Color) {
    val base = Math.toRadians(listener.yaw.toDouble())
    val reach = 0.55f
    val path = Path()
    val start = view.projectWorld(view.worldX(listener.x), view.worldY(0f), view.worldZ(listener.z)).point
    path.moveTo(start.x, start.y)
    for (step in -4..4) {
        val angle = base + Math.toRadians(step * 6.0)
        val wx = view.worldX(listener.x) + sin(angle).toFloat() * reach
        val wz = view.worldZ(listener.z) + cos(angle).toFloat() * reach
        val p = view.projectWorld(wx, view.worldY(0f), wz).point
        path.lineTo(p.x, p.y)
    }
    path.close()
    drawPath(path, Brush.radialGradient(
        listOf(inkColor.copy(alpha = 0.14f * (0.5f + 0.5f * on)), Color.Transparent),
        center = start,
        radius = view.pixels(view.project(listener.x, 0f, listener.z), reach) + 1f,
    ))
}

private fun DrawScope.drawStem(view: StageView, x: Float, y: Float, z: Float, color: Color, alpha: Float) {
    val top = view.project(x, y, z)
    val floor = view.project(x, 0f, z)
    val rx = 8.dp.toPx() * floor.scale
    val ry = rx * (0.2f + 0.7f * view.floorSquash)
    drawOval(color.copy(alpha = 0.2f * alpha), Offset(floor.point.x - rx, floor.point.y - ry), Size(rx * 2, ry * 2))
    drawLine(
        color.copy(alpha = 0.32f * alpha), floor.point, top.point, 1.dp.toPx(),
        pathEffect = PathEffect.dashPathEffect(floatArrayOf(3.dp.toPx(), 3.dp.toPx())),
    )
}

private fun DrawScope.drawMark(view: StageView, mark: StageMark, selected: Boolean, on: Float, compact: Boolean, inkColor: Color) {
    val color = sourceColor(mark.source)
    val alpha = (if (mark.quiet) 0.35f else 1f) * (0.55f + 0.45f * on)
    val p = view.project(mark.x, mark.y, mark.z)
    drawStem(view, mark.x, mark.y, mark.z, color, alpha)

    if (mark.spread > 0f) {
        val left = view.project((mark.x - mark.spread).coerceIn(-1f, 1f), mark.y, mark.z).point
        val right = view.project((mark.x + mark.spread).coerceIn(-1f, 1f), mark.y, mark.z).point
        drawLine(color.copy(alpha = 0.3f * alpha), left, right, 1.5.dp.toPx(), cap = StrokeCap.Round)
        drawCircle(color.copy(alpha = 0.5f * alpha), 2.5.dp.toPx() * p.scale, left)
        drawCircle(color.copy(alpha = 0.5f * alpha), 2.5.dp.toPx() * p.scale, right)
    }

    val base = (if (compact) 3.5f else 6f).dp.toPx() * p.scale
    val r = base * (1f + 0.6f * mark.level * on)
    val glow = r * (2.4f + 1.4f * mark.level * on)
    drawCircle(
        Brush.radialGradient(listOf(color.copy(alpha = 0.42f * alpha), Color.Transparent), center = p.point, radius = glow),
        radius = glow,
        center = p.point,
    )
    if (mark.source == StageSource.Sub) {
        drawRoundRect(color.copy(alpha = alpha), Offset(p.point.x - r, p.point.y - r), Size(r * 2, r * 2), CornerRadius(r * 0.4f))
    } else {
        drawCircle(color.copy(alpha = alpha), r, p.point)
    }
    drawCircle(Color.White.copy(alpha = 0.55f * alpha), r * 0.32f, p.point + Offset(-r * 0.3f, -r * 0.3f))
    if (selected) drawCircle(inkColor.copy(alpha = 0.9f), r + 4.dp.toPx(), p.point, style = Stroke(1.5.dp.toPx()))
    if (!compact && mark.label != null) {
        drawText(
            mark.label,
            color = inkColor.copy(alpha = 0.85f * alpha),
            topLeft = Offset(p.point.x - mark.label.size.width / 2f, p.point.y + r + 5.dp.toPx()),
        )
    }
}

private class HeadPart(val point: Offset, val depth: Float, val radius: Float, val color: Color)

private fun DrawScope.drawHead(view: StageView, listener: StageListener, selected: Boolean, on: Float, compact: Boolean, inkColor: Color) {
    val alpha = 0.7f + 0.3f * on
    drawStem(view, listener.x, listener.y, listener.z, inkColor, alpha)
    val cx = view.worldX(listener.x)
    val cy = view.worldY(listener.y)
    val cz = view.worldZ(listener.z)
    val center = view.projectWorld(cx, cy, cz)
    val size = if (compact) HeadSize * 0.8f else HeadSize
    val radius = max(view.pixels(center, size), (if (compact) 5 else 9).dp.toPx())
    val yaw = Math.toRadians(listener.yaw.toDouble())
    val fx = sin(yaw).toFloat()
    val fz = cos(yaw).toFloat()
    val rx = fz
    val rz = -fx

    fun part(forward: Float, right: Float, up: Float, partRadius: Float, color: Color): HeadPart {
        val p = view.projectWorld(
            cx + (fx * forward + rx * right) * size,
            cy + up * size,
            cz + (fz * forward + rz * right) * size,
        )
        return HeadPart(p.point, p.depth, radius * partRadius, color)
    }

    val cup = Color(0xFFE5E5EA).copy(alpha = alpha)
    val parts = mutableListOf(
        part(0f, -1.02f, 0f, 0.36f, cup),
        part(0f, 1.02f, 0f, 0.36f, cup),
        part(1.02f, 0f, -0.05f, 0.2f, Color(0xFFF2C6B4).copy(alpha = alpha)),
    )
    if (!compact) {
        parts += part(0.86f, -0.36f, 0.22f, 0.09f, Color(0xFF1C1C1E))
        parts += part(0.86f, 0.36f, 0.22f, 0.09f, Color(0xFF1C1C1E))
    }
    val (behind, front) = parts.partition { it.depth > center.depth }
    behind.forEach { drawCircle(it.color, it.radius, it.point) }
    drawCircle(
        Brush.radialGradient(
            listOf(Color(0xFFF7F7F9).copy(alpha = alpha), Color(0xFF8E8E96).copy(alpha = alpha)),
            center = center.point + Offset(-radius * 0.35f, -radius * 0.4f),
            radius = radius * 1.7f,
        ),
        radius = radius,
        center = center.point,
    )
    front.forEach { drawCircle(it.color, it.radius, it.point) }

    val left = part(0f, -1.02f, 0.25f, 0f, cup).point
    val right = part(0f, 1.02f, 0.25f, 0f, cup).point
    val top = part(0f, 0f, 1.25f, 0f, cup).point
    val control = top * 2f - (left + right) / 2f
    drawPath(
        Path().apply {
            moveTo(left.x, left.y)
            quadraticTo(control.x, control.y, right.x, right.y)
        },
        inkColor.copy(alpha = 0.9f * alpha),
        style = Stroke(max(1.5.dp.toPx(), radius * 0.16f), cap = StrokeCap.Round),
    )
    if (selected) drawCircle(inkColor.copy(alpha = 0.9f), radius + 6.dp.toPx(), center.point, style = Stroke(1.5.dp.toPx()))
}
