package com.saurav.pixelmusic.presentation.screens.audiolab

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.saurav.pixelmusic.R
import com.saurav.pixelmusic.audiolab.FxPreset
import com.saurav.pixelmusic.audiolab.FxPresets
import com.saurav.pixelmusic.presentation.viewmodel.AudioLabViewModel

@Composable
internal fun FxPresetsSection(viewModel: AudioLabViewModel) {
    val presets by viewModel.fxPresets.collectAsState()
    val pickedId by viewModel.fxPresetId.collectAsState()
    var creating by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<FxPreset?>(null) }

    Row(Modifier.fillMaxWidth().height(34.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(
            stringResource(R.string.fx_presets),
            fontFamily = InterFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp,
            letterSpacing = (-0.02).em,
            color = LabColors.Primary,
            modifier = Modifier.weight(1f),
        )
        pickedId?.let { id ->
            val picked = presets.firstOrNull { it.id == id }
            if (picked != null) {
                LabTextAction(stringResource(R.string.fx_preset_edit), R.drawable.rounded_edit_24) {
                    editing = picked
                }
            }
        }
    }
    Spacer(Modifier.height(10.dp))
    val allChips = remember(presets) {
        FxPresets.BuiltIn.map { LabChip(it.id, it.name) } + presets.map { LabChip(it.id, it.name) }
    }
    LabChips(
        chips = allChips,
        selectedKey = pickedId,
        onSelect = { id ->
            if (id == pickedId && !FxPresets.isBuiltIn(id)) {
                viewModel.releaseFxPreset()
            } else {
                viewModel.applyFxPreset(id)
            }
        },
        onLongPress = { id ->
            if (!FxPresets.isBuiltIn(id)) {
                editing = presets.firstOrNull { it.id == id }
            }
        },
        leading = {
            AddFxPresetButton { creating = true }
        },
    )

    if (creating) {
        TextFieldDialog(
            title = { Text(stringResource(R.string.fx_preset_save)) },
            placeholder = { Text(stringResource(R.string.fx_preset_save_hint)) },
            onDone = viewModel::saveFxPreset,
            onDismiss = { creating = false },
        )
    }

    editing?.let { preset ->
        EditFxPresetDialog(
            preset = preset,
            onRename = { newName ->
                viewModel.renameFxPreset(preset.id, newName)
                editing = null
            },
            onDelete = {
                viewModel.deleteFxPreset(preset.id)
                editing = null
            },
            onDismiss = { editing = null },
        )
    }
}

@Composable
private fun AddFxPresetButton(onClick: () -> Unit) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(38.dp)
            .clip(CircleShape)
            .background(LabColors.Chip)
            .clickable(remember { MutableInteractionSource() }, RemoteFocus, role = Role.Button, onClick = onClick),
    ) {
        Icon(
            painterResource(R.drawable.ic_lab_add),
            contentDescription = stringResource(R.string.fx_preset_save),
            tint = LabColors.Primary,
            modifier = Modifier.size(18.dp),
        )
    }
}

@Composable
private fun EditFxPresetDialog(
    preset: FxPreset,
    onRename: (String) -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember(preset.id) { mutableStateOf(preset.name) }
    val cs = MaterialTheme.colorScheme
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = cs.surfaceContainerHigh,
        titleContentColor = cs.onSurface,
        textContentColor = cs.onSurface,
        title = { Text(stringResource(R.string.fx_preset_rename), fontFamily = InterFontFamily, fontWeight = FontWeight.SemiBold) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = cs.onSurface,
                    unfocusedTextColor = cs.onSurface,
                    focusedBorderColor = cs.primary,
                    unfocusedBorderColor = cs.outline,
                    cursorColor = cs.primary,
                ),
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val trimmed = name.trim()
                    if (trimmed.isNotEmpty()) onRename(trimmed)
                }
            ) {
                Text("Save", color = cs.primary)
            }
        },
        dismissButton = {
            Row {
                TextButton(onClick = onDelete) {
                    Text("Delete", color = cs.error)
                }
                TextButton(onClick = onDismiss) {
                    Text("Cancel", color = cs.onSurfaceVariant)
                }
            }
        },
    )
}
