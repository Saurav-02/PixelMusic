package com.saurav.pixelmusic.presentation.screens.audiolab.stage

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.saurav.pixelmusic.R
import com.saurav.pixelmusic.audiolab.stage.StageSettings
import com.saurav.pixelmusic.audiolab.stage.StageSource
import com.saurav.pixelmusic.presentation.screens.audiolab.EffectCard
import com.saurav.pixelmusic.presentation.screens.audiolab.LabColors
import com.saurav.pixelmusic.presentation.screens.audiolab.LabTextAction
import com.saurav.pixelmusic.presentation.screens.audiolab.LabToggleRow
import com.saurav.pixelmusic.presentation.screens.audiolab.PictureCanvas
import com.saurav.pixelmusic.presentation.viewmodel.AudioLabViewModel
import kotlin.math.min
import kotlin.math.sin

@Composable
internal fun StageCard(viewModel: AudioLabViewModel, onOpen: () -> Unit) {
    val stage by viewModel.stage.collectAsState()
    EffectCard(
        picture = { Box(Modifier.fillMaxSize().clickable(onClick = onOpen)) { StagePicture(stage) } },
        header = {
            LabToggleRow(
                title = stringResource(R.string.stage_card_title),
                subtitle = stringResource(R.string.stage_card_sub),
                checked = stage.enabled,
                onCheckedChange = { on -> viewModel.updateStage { it.copy(enabled = on) } },
            )
        },
        expanded = false,
        options = {},
        footer = {
            Row(Modifier.fillMaxWidth()) {
                Spacer(Modifier.weight(1f))
                LabTextAction(stringResource(R.string.stage_open), R.drawable.ic_lab_arrow_forward, onClick = onOpen)
            }
        },
    )
}

@Composable
private fun StagePicture(settings: StageSettings) {
    val on by animateFloatAsState(if (settings.enabled) 1f else 0f, tween(400), label = "stageCardOn")
    val stageInk = LabColors.Primary
    PictureCanvas { t, clock ->
        val yaw = DefaultCameraYaw + sin(t * 0.35f) * 22f * (0.4f + 0.6f * on)
        val view = StageView(size, settings.box, yaw, DefaultCameraPitch + 6f)
        val level = min(1f, clock.level.level * 2.2f) * on
        val soloing = settings.objects.any { it.solo && !it.muted }
        val box = settings.box
        val marks = settings.objects.map { o ->
            StageMark(
                key = o.id,
                source = o.source,
                x = o.x,
                y = o.y,
                z = o.z,
                label = null,
                level = level,
                quiet = o.muted || (soloing && !o.solo),
                spread = if (o.source == StageSource.Ambience) o.width * min(3f, box.width * 0.35f) / (box.width / 2) else 0f,
            )
        }
        drawStage(view, marks, settings.listener, selected = null, on = on, compact = true, inkColor = stageInk)
    }
}
