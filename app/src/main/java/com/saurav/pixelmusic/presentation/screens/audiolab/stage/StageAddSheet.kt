package com.saurav.pixelmusic.presentation.screens.audiolab.stage

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.saurav.pixelmusic.R
import com.saurav.pixelmusic.audiolab.stage.StageNative
import com.saurav.pixelmusic.audiolab.stage.StageSettings
import com.saurav.pixelmusic.audiolab.stage.StageSource
import com.saurav.pixelmusic.presentation.screens.audiolab.SheetLabel
import com.saurav.pixelmusic.presentation.screens.audiolab.SheetOptionRow
import com.saurav.pixelmusic.presentation.screens.audiolab.stableSystemBars

@Composable
internal fun StageAddBottomSheet(
    stage: State<StageSettings>,
    onDismiss: () -> Unit,
    onAdd: (StageSource) -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberBottomSheetState(initialValue = SheetValue.Hidden),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        StageAddPicker(
            stage = stage,
            onPick = { source ->
                onAdd(source)
                onDismiss()
            },
        )
    }
}

@Composable
private fun StageAddPicker(stage: State<StageSettings>, onPick: (StageSource) -> Unit) {
    val counts by remember { derivedStateOf { StageSource.entries.associateWith { s -> stage.value.objects.count { it.source == s } } } }
    val navBottom = WindowInsets.stableSystemBars.asPaddingValues().calculateBottomPadding()
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(bottom = 16.dp + navBottom),
    ) {
        SheetLabel(stringResource(R.string.stage_add_title), Modifier.padding(start = 4.dp, bottom = 4.dp))
        for (source in StageSource.entries) {
            val count = counts[source] ?: 0
            val full = count >= StageNative.Copies
            SheetOptionRow(
                title = stringResource(sourceName(source)),
                subtitle = if (full) stringResource(R.string.stage_add_full, StageNative.Copies)
                else stringResource(R.string.stage_add_count, count, StageNative.Copies),
                enabled = !full,
                onClick = { if (!full) onPick(source) },
                leading = { SourceDot(source) },
            )
        }
    }
}

@Composable
private fun SourceDot(source: StageSource) {
    Box(Modifier.size(28.dp), contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .size(14.dp)
                .clip(if (source == StageSource.Sub) RoundedCornerShape(4.dp) else CircleShape)
                .background(sourceColor(source)),
        )
    }
}
