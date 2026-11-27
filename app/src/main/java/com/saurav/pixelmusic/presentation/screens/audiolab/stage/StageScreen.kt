package com.saurav.pixelmusic.presentation.screens.audiolab.stage

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import com.saurav.pixelmusic.R
import com.saurav.pixelmusic.audiolab.AudioOutputs
import com.saurav.pixelmusic.audiolab.audio.AudioTap
import com.saurav.pixelmusic.audiolab.stage.StageOutput
import com.saurav.pixelmusic.audiolab.stage.StagePresets
import com.saurav.pixelmusic.audiolab.stage.StageSettings
import com.saurav.pixelmusic.audiolab.stage.StageSpeakers
import com.saurav.pixelmusic.audiolab.stage.StageWalls
import com.saurav.pixelmusic.presentation.screens.audiolab.AudioLabPage
import com.saurav.pixelmusic.presentation.screens.audiolab.InterFontFamily
import com.saurav.pixelmusic.presentation.screens.audiolab.LabCard
import com.saurav.pixelmusic.presentation.screens.audiolab.LabChip
import com.saurav.pixelmusic.presentation.screens.audiolab.LabChips
import com.saurav.pixelmusic.presentation.screens.audiolab.LabColors
import com.saurav.pixelmusic.presentation.screens.audiolab.LabPanel
import com.saurav.pixelmusic.presentation.screens.audiolab.LabSectionLabel
import com.saurav.pixelmusic.presentation.screens.audiolab.LabSegmented
import com.saurav.pixelmusic.presentation.screens.audiolab.LabSlider
import com.saurav.pixelmusic.presentation.screens.audiolab.LabTextAction
import com.saurav.pixelmusic.presentation.screens.audiolab.LabTitle
import com.saurav.pixelmusic.presentation.screens.audiolab.pct
import com.saurav.pixelmusic.presentation.viewmodel.AudioLabViewModel
import kotlin.math.roundToInt

@Composable
fun StageScreen(
    navController: NavController,
    viewModel: AudioLabViewModel = hiltViewModel(),
) {
    val stage = viewModel.stage.collectAsState()
    var showAddSheet by remember { mutableStateOf(false) }
    var selected by rememberSaveable { mutableStateOf<Int?>(stage.value.objects.firstOrNull()?.id) }
    val shownSelection by remember {
        derivedStateOf { selected?.takeIf { it == ListenerKey || stage.value.objects.any { o -> o.id == it } } }
    }

    AudioLabPage(
        title = LabTitle(
            stringResource(R.string.stage_title_first),
            stringResource(R.string.stage_title_second),
            firstSize = 40.sp,
            secondSize = 84.sp,
            firstDim = true,
        ),
        onBack = { navController.navigateUp() },
    ) {
        StageRoom(
            stage = stage,
            selected = shownSelection,
            onSelect = { selected = it },
            onMove = { key, spot ->
                if (key == ListenerKey) viewModel.updateListener { it.copy(x = spot.x, y = spot.y, z = spot.z) }
                else viewModel.updateStageObject(key) { it.copy(x = spot.x, y = spot.y, z = spot.z) }
            },
            levels = viewModel.fx.stageLevels,
            isPlaying = { AudioTap.isLive },
            onAdd = { showAddSheet = true },
        )
        Spacer(Modifier.height(14.dp))
        StageControlsCard(viewModel, stage, shownSelection) { selected = it }
        Spacer(Modifier.height(14.dp))
        PresetsCard(viewModel, stage)
        Spacer(Modifier.height(14.dp))
        RoomCard(viewModel, stage)
        Spacer(Modifier.height(14.dp))
        OutputCard(viewModel, stage)
        Spacer(Modifier.height(14.dp))
        Footnote(stringResource(R.string.stage_surround_note))
        Footnote(stringResource(R.string.stage_credit))
    }
    if (showAddSheet) {
        StageAddBottomSheet(
            stage = stage,
            onDismiss = { showAddSheet = false },
            onAdd = { source -> viewModel.addStageObject(source)?.let { selected = it } },
        )
    }
}

@Composable
private fun PresetsCard(viewModel: AudioLabViewModel, stage: State<StageSettings>) {
    val presetId by remember { derivedStateOf { stage.value.presetId } }
    LabCard(padding = 14.dp) {
        LabSectionLabel(stringResource(R.string.stage_presets), Modifier.padding(horizontal = 6.dp))
        LabChips(
            chips = StagePresets.All.map { LabChip(it.id, stringResource(it.name)) },
            selectedKey = presetId,
            onSelect = viewModel::applyStagePreset,
        )
    }
}

@Composable
private fun RoomCard(viewModel: AudioLabViewModel, stage: State<StageSettings>) {
    val room by remember { derivedStateOf { RoomLook(stage.value) } }
    LabCard(padding = 14.dp) {
        LabSectionLabel(stringResource(R.string.stage_room), Modifier.padding(horizontal = 6.dp))
        LabSegmented(
            options = listOf(
                stringResource(R.string.stage_walls_soft),
                stringResource(R.string.stage_walls_wood),
                stringResource(R.string.stage_walls_concrete),
            ),
            selectedIndex = room.walls.ordinal,
            onSelect = { i -> viewModel.updateStage { it.copy(walls = StageWalls.entries[i], presetId = null) } },
            height = 42.dp,
        )
        Spacer(Modifier.height(10.dp))
        LabPanel {
            LabSlider(
                stringResource(R.string.stage_room_size),
                stringResource(R.string.stage_room_size_value, room.width.roundToInt().toString(), room.depth.roundToInt().toString()),
                room.size,
                { v -> viewModel.updateStage { it.copy(roomSize = v, presetId = null) } },
                0f..1f,
            )
            LabSlider(stringResource(R.string.stage_room_amount), pct(room.amount), room.amount,
                { v -> viewModel.updateStage { it.copy(roomAmount = v, presetId = null) } }, 0f..1f)
            LabSlider(stringResource(R.string.stage_output_level), dbText(room.level), room.level,
                { v -> viewModel.updateStage { it.copy(levelDb = (v * 2).roundToInt() / 2f) } }, -12f..6f)
        }
    }
}

private data class RoomLook(val size: Float, val width: Float, val depth: Float, val walls: StageWalls, val amount: Float, val level: Float) {
    constructor(s: StageSettings) : this(s.roomSize, s.box.width, s.box.depth, s.walls, s.roomAmount, s.levelDb)
}

@Composable
private fun OutputCard(viewModel: AudioLabViewModel, stage: State<StageSettings>) {
    val output by viewModel.stageOutput.collectAsState()
    val kinds by remember { derivedStateOf { stage.value.outputKinds } }
    val onSpeakers by remember { derivedStateOf { stage.value.onSpeakers } }
    val chosen = kinds[output.key]
    val speaker = chosen ?: output.guessedSpeaker

    LabCard(padding = 14.dp) {
        Column(Modifier.padding(horizontal = 6.dp)) {
            Text(stringResource(R.string.stage_output), fontFamily = InterFontFamily, fontSize = 12.sp, color = LabColors.Secondary)
            Text(outputName(output), fontFamily = InterFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 17.sp, color = LabColors.Primary)
        }
        Spacer(Modifier.height(10.dp))
        LabSegmented(
            options = listOf(stringResource(R.string.stage_kind_headphones), stringResource(R.string.stage_kind_speaker)),
            selectedIndex = if (speaker) 1 else 0,
            onSelect = { i -> viewModel.setOutputKind(output.key, i == 1) },
            height = 42.dp,
        )
        if (chosen == null) {
            Footnote(stringResource(R.string.stage_output_guessed))
        } else {
            Footnote(stringResource(R.string.stage_output_set))
            LabTextAction(stringResource(R.string.stage_output_reset)) { viewModel.setOutputKind(output.key, null) }
        }
        Spacer(Modifier.height(14.dp))
        LabSectionLabel(stringResource(R.string.stage_on_speakers), Modifier.padding(horizontal = 6.dp))
        LabSegmented(
            options = listOf(
                stringResource(R.string.stage_speakers_off),
                stringResource(R.string.stage_speakers_stereo),
                stringResource(R.string.stage_speakers_3d),
            ),
            selectedIndex = onSpeakers.ordinal,
            onSelect = { i -> viewModel.updateStage { it.copy(onSpeakers = StageSpeakers.entries[i]) } },
            height = 42.dp,
        )
        Footnote(stringResource(R.string.stage_speakers_note))
    }
}

@Composable
private fun outputName(output: StageOutput): String = output.name.ifBlank {
    stringResource(
        when (output.kind) {
            AudioOutputs.Kind.PHONE -> R.string.stage_output_phone
            AudioOutputs.Kind.WIRED -> R.string.stage_output_wired
            AudioOutputs.Kind.USB -> R.string.stage_output_usb
            AudioOutputs.Kind.BLUETOOTH -> R.string.stage_output_bluetooth
            AudioOutputs.Kind.HDMI -> R.string.stage_output_hdmi
        },
    )
}

@Composable
private fun Footnote(text: String) {
    Text(
        text,
        fontFamily = InterFontFamily,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        color = LabColors.Secondary,
        modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp),
    )
}
