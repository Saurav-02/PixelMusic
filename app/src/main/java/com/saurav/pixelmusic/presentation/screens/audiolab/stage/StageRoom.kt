package com.saurav.pixelmusic.presentation.screens.audiolab.stage

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.saurav.pixelmusic.R
import com.saurav.pixelmusic.audiolab.stage.StageSettings
import com.saurav.pixelmusic.audiolab.stage.StageSource
import com.saurav.pixelmusic.presentation.screens.audiolab.InterFontFamily
import com.saurav.pixelmusic.presentation.screens.audiolab.LabColors
import com.saurav.pixelmusic.presentation.screens.audiolab.damageBoundary
import com.saurav.pixelmusic.presentation.screens.audiolab.rememberLabTime
import com.saurav.pixelmusic.presentation.screens.audiolab.rememberOnScreen
import com.saurav.pixelmusic.presentation.screens.audiolab.rememberPicturesHeld
import com.saurav.pixelmusic.presentation.screens.audiolab.trackOnScreen
import kotlin.math.sqrt

internal class StageSpot(val x: Float, val y: Float, val z: Float)

private class LastFrame {
    var view: StageView? = null
}

@Composable
internal fun StageRoom(
    stage: State<StageSettings>,
    selected: Int?,
    onSelect: (Int) -> Unit,
    onMove: (key: Int, spot: StageSpot) -> Unit,
    levels: FloatArray,
    isPlaying: () -> Boolean,
    onAdd: () -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = 380.dp,
) {
    var yaw by rememberSaveable { mutableFloatStateOf(DefaultCameraYaw) }
    var pitch by rememberSaveable { mutableFloatStateOf(DefaultCameraPitch) }
    val on by animateFloatAsState(if (stage.value.enabled) 1f else 0f, tween(400), label = "stageOn")
    val labels = rememberStageLabels(stage)
    val frame = remember { LastFrame() }
    val shown = remember { FloatArray(levels.size) }
    val onScreen = rememberOnScreen()
    val held = rememberPicturesHeld(onScreen)
    val time = rememberLabTime(held, moving = { stage.value.enabled && isPlaying() })
    val currentSelected by rememberUpdatedState(selected)
    val currentOnSelect by rememberUpdatedState(onSelect)
    val currentOnMove by rememberUpdatedState(onMove)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(26.dp))
            .background(LabColors.Stage)
            .border(1.dp, LabColors.StageLine, RoundedCornerShape(26.dp))
            .damageBoundary(),
    ) {
        val stageInk = LabColors.Primary
        Canvas(
            Modifier
                .fillMaxSize()
                .trackOnScreen(onScreen)
                .pointerInput(Unit) {
                    var lastTap = 0L
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        val view = frame.view ?: return@awaitEachGesture
                        val hit = hitTest(view, stage.value, down.position, 28.dp.toPx())
                        if (hit != null) currentOnSelect(hit)
                        val lifted = hit ?: currentSelected
                        var dragging = false
                        var lifting = false
                        var grab: Pair<Float, Float>? = null
                        var lastCentroid = Offset.Zero
                        var liftFrom: StageSpot? = null
                        var liftY = 0f
                        while (true) {
                            val event = awaitPointerEvent()
                            val pressed = event.changes.filter { it.pressed }
                            if (pressed.isEmpty()) break
                            if (pressed.size >= 2 && lifted != null) {
                                val centroid = pressed.fold(Offset.Zero) { sum, c -> sum + c.position } / pressed.size.toFloat()
                                if (!lifting) {
                                    lifting = true
                                    lastCentroid = centroid
                                    liftFrom = spotOf(stage.value, lifted)
                                    liftY = liftFrom?.y ?: 0f
                                }
                                liftFrom?.let { from ->
                                    liftY = (liftY - (centroid.y - lastCentroid.y) / view.heightPixels(from.x, liftY, from.z)).coerceIn(0f, 1f)
                                    currentOnMove(lifted, StageSpot(from.x, liftY, from.z))
                                }
                                lastCentroid = centroid
                                event.changes.forEach { it.consume() }
                                continue
                            }
                            if (lifting) {
                                event.changes.forEach { it.consume() }
                                continue
                            }
                            val change = pressed.first()
                            if (!dragging && (change.position - down.position).getDistance() > viewConfiguration.touchSlop) dragging = true
                            if (!dragging) continue
                            val spot = hit?.let { spotOf(stage.value, it) }
                            if (hit != null && spot != null) {
                                val now = view.onLevel(change.position, spot.y)
                                if (now != null) {
                                    val offset = grab
                                        ?: (view.onLevel(down.position, spot.y)?.let { (spot.x - it.first) to (spot.z - it.second) } ?: (0f to 0f))
                                            .also { grab = it }
                                    currentOnMove(hit, StageSpot(
                                        (now.first + offset.first).coerceIn(-1f, 1f),
                                        spot.y,
                                        (now.second + offset.second).coerceIn(-1f, 1f),
                                    ))
                                }
                            } else {
                                val delta = change.positionChange()
                                yaw = (yaw - delta.x * 0.35f).let { if (it > 180f) it - 360f else if (it < -180f) it + 360f else it }
                                pitch = (pitch + delta.y * 0.3f).coerceIn(4f, 88f)
                            }
                            change.consume()
                        }
                        if (!dragging && !lifting && hit == null) {
                            if (down.uptimeMillis - lastTap < 320) {
                                yaw = DefaultCameraYaw
                                pitch = DefaultCameraPitch
                            }
                            lastTap = down.uptimeMillis
                        }
                    }
                },
        ) {
            time.value
            val s = stage.value
            val view = StageView(size, s.box, yaw, pitch)
            frame.view = view
            drawStage(view, marksOf(s, labels.value, levels, shown, s.enabled && isPlaying()), s.listener, selected, on, inkColor = stageInk)
        }

        Column(
            modifier = Modifier.align(Alignment.TopEnd).padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            RoomButton(R.drawable.ic_lab_refresh, stringResource(R.string.stage_reset_view)) {
                yaw = DefaultCameraYaw
                pitch = DefaultCameraPitch
            }
            RoomButton(R.drawable.ic_lab_add, stringResource(R.string.stage_add), onAdd)
        }
        Text(
            stringResource(R.string.stage_room_hint),
            fontFamily = InterFontFamily,
            fontSize = 11.sp,
            lineHeight = 14.sp,
            color = LabColors.Faint,
            textAlign = TextAlign.Center,
            modifier = Modifier.align(Alignment.BottomCenter).padding(horizontal = 18.dp, vertical = 12.dp),
        )
    }
}

@Composable
private fun RoomButton(icon: Int, description: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(34.dp)
            .clip(CircleShape)
            .background(LabColors.Row)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(painterResource(icon), description, tint = LabColors.Primary, modifier = Modifier.size(17.dp))
    }
}

internal fun labelsOf(objects: List<Pair<Int, StageSource>>, names: Map<StageSource, String>, numbered: String): Map<Int, String> {
    val seen = IntArray(StageSource.entries.size)
    return objects.associate { (id, source) ->
        val n = ++seen[source.ordinal]
        id to if (n == 1) names.getValue(source) else String.format(numbered, names.getValue(source), n)
    }
}

@Composable
private fun rememberStageLabels(stage: State<StageSettings>): State<Map<Int, TextLayoutResult>> {
    val names = StageSource.entries.associateWith { stringResource(sourceName(it)) }
    val numbered = stringResource(R.string.stage_source_numbered)
    val measurer = rememberTextMeasurer()
    val keys by remember { derivedStateOf { stage.value.objects.map { it.id to it.source } } }
    val style = TextStyle(fontFamily = InterFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 11.sp)
    return remember(keys, names, numbered) {
        derivedStateOf { labelsOf(keys, names, numbered).mapValues { (_, text) -> measurer.measure(text, style) } }
    }
}

internal fun sourceName(source: StageSource): Int = when (source) {
    StageSource.Vocal -> R.string.stage_source_vocal
    StageSource.Center -> R.string.stage_source_center
    StageSource.Left -> R.string.stage_source_left
    StageSource.Right -> R.string.stage_source_right
    StageSource.Ambience -> R.string.stage_source_ambience
    StageSource.Sub -> R.string.stage_source_sub
}

internal fun marksOf(
    settings: StageSettings,
    labels: Map<Int, TextLayoutResult>,
    levels: FloatArray,
    shown: FloatArray,
    playing: Boolean,
): List<StageMark> {
    val soloing = settings.objects.any { it.solo && !it.muted }
    val stepped = BooleanArray(shown.size)
    val box = settings.box
    return settings.objects.map { o ->
        val slot = o.source.ordinal.takeIf { it in levels.indices } ?: -1
        val level = if (slot >= 0) {
            if (!stepped[slot]) {
                val target = if (playing) sqrt(levels[slot].coerceIn(0f, 1f)) else 0f
                shown[slot] += (target - shown[slot]) * if (target > shown[slot]) 0.45f else 0.12f
                stepped[slot] = true
            }
            shown[slot]
        } else 0f
        StageMark(
            key = o.id,
            source = o.source,
            x = o.x,
            y = o.y,
            z = o.z,
            label = labels[o.id],
            level = level,
            quiet = o.muted || (soloing && !o.solo),
            spread = if (o.source == StageSource.Ambience) o.width * minOf(3f, box.width * 0.35f) / (box.width / 2) else 0f,
        )
    }
}

internal fun spotOf(settings: StageSettings, key: Int): StageSpot? =
    if (key == ListenerKey) settings.listener.let { StageSpot(it.x, it.y, it.z) }
    else settings.objects.firstOrNull { it.id == key }?.let { StageSpot(it.x, it.y, it.z) }

private fun hitTest(view: StageView, settings: StageSettings, point: Offset, reach: Float): Int? {
    var best: Int? = null
    var bestDistance = reach
    fun check(key: Int, x: Float, y: Float, z: Float) {
        val distance = (view.project(x, y, z).point - point).getDistance()
        if (distance < bestDistance) {
            bestDistance = distance
            best = key
        }
    }
    settings.objects.forEach { check(it.id, it.x, it.y, it.z) }
    settings.listener.let { check(ListenerKey, it.x, it.y, it.z) }
    return best
}
