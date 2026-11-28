package com.saurav.pixelmusic.presentation.screens.audiolab.equalizer

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.saurav.pixelmusic.R
import com.saurav.pixelmusic.audiolab.AutoEqCorrection
import com.saurav.pixelmusic.audiolab.EqSettings
import com.saurav.pixelmusic.audiolab.audio.ResponseCurve
import com.saurav.pixelmusic.audiolab.autoeq.AutoEqEntry
import com.saurav.pixelmusic.audiolab.autoeq.AutoEqFitter
import com.saurav.pixelmusic.audiolab.autoeq.AutoEqMeasurement
import com.saurav.pixelmusic.audiolab.autoeq.AutoEqModel
import com.saurav.pixelmusic.audiolab.autoeq.AutoEqTuning
import com.saurav.pixelmusic.audiolab.autoeq.HeadphoneForm
import com.saurav.pixelmusic.presentation.screens.audiolab.InterFontFamily
import com.saurav.pixelmusic.presentation.screens.audiolab.LabCard
import com.saurav.pixelmusic.presentation.screens.audiolab.LabChip
import com.saurav.pixelmusic.presentation.screens.audiolab.LabChips
import com.saurav.pixelmusic.presentation.screens.audiolab.LabColors
import com.saurav.pixelmusic.presentation.screens.audiolab.LabSectionLabel
import com.saurav.pixelmusic.presentation.screens.audiolab.LabSlider
import com.saurav.pixelmusic.presentation.screens.audiolab.LabTextAction
import com.saurav.pixelmusic.presentation.screens.audiolab.LabToggleRow
import com.saurav.pixelmusic.presentation.screens.audiolab.damageBoundary
import com.saurav.pixelmusic.presentation.viewmodel.AudioLabViewModel
import com.saurav.pixelmusic.presentation.viewmodel.AutoEqIndex
import kotlinx.coroutines.launch
import kotlin.math.ceil
import kotlin.math.floor

private val OriginalColor = Color(0xFF6B9BF2)
private val CorrectedColor = Color(0xFFF28BA8)
private val TargetColor = Color(0xFF9A9AA0)

@Composable
fun AutoEqTab(viewModel: AudioLabViewModel, eq: EqSettings) {
    LaunchedEffect(Unit) { viewModel.openAutoEq() }
    val measurement by viewModel.measurement.collectAsState()
    val busy by viewModel.busy.collectAsState()
    val error by viewModel.error.collectAsState()
    val correction = eq.correction

    LabSectionLabel(stringResource(R.string.eqx_hp_correction))
    LabCard(padding = 12.dp) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.eqx_freq_response), fontFamily = InterFontFamily, fontWeight = FontWeight.Bold, fontSize = 12.sp,
                letterSpacing = 1.2.sp, color = LabColors.Secondary, modifier = Modifier.weight(1f))
            Legend(OriginalColor, stringResource(R.string.eqx_legend_original))
            Spacer(Modifier.width(10.dp))
            Legend(TargetColor, stringResource(R.string.eqx_legend_target))
            Spacer(Modifier.width(10.dp))
            Legend(CorrectedColor, stringResource(R.string.eqx_legend_corrected))
        }
        Spacer(Modifier.height(8.dp))
        ResponseGraph(measurement = measurement.takeIf { correction != null && it?.entry?.path == correction.path }, correction = correction)
    }

    if (correction != null) {
        Spacer(Modifier.height(12.dp))
        SelectedHeadphone(
            correction = correction,
            enabled = eq.correctionEnabled,
            fitting = busy == "fit",
            canTune = measurement?.entry?.path == correction.path,
            viewModel = viewModel,
        )
    }

    AnimatedVisibility(error != null, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
        Text(error.orEmpty(), fontFamily = InterFontFamily, fontSize = 13.sp, color = MaterialTheme.colorScheme.error,
            modifier = Modifier.padding(top = 10.dp, start = 6.dp))
    }

    Spacer(Modifier.height(14.dp))
    Database(viewModel = viewModel, selectedPath = correction?.path, loadingPath = busy)
}

@Composable
private fun Legend(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(7.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(4.dp))
        Text(label, fontFamily = InterFontFamily, fontSize = 11.sp, color = LabColors.Secondary)
    }
}

@Composable
private fun ResponseGraph(measurement: AutoEqMeasurement?, correction: AutoEqCorrection?) {
    val measurer = rememberTextMeasurer(cacheSize = 32)
    val style = TextStyle(fontFamily = InterFontFamily, fontSize = 10.sp, color = LabColors.Faint)
    val gridInk = LabColors.Primary
    val appear by animateFloatAsState(if (measurement != null) 1f else 0f, tween(600), label = "appear")

    val curves = remember(measurement, correction) {
        if (measurement == null || correction == null) return@remember null
        val f = measurement.frequencies
        val response = ResponseCurve(f).response(correction.filters)
        val target = AutoEqFitter.tunedTarget(measurement, correction.tuning)
        val corrected = DoubleArray(f.size) { measurement.raw[it] + response[it] }
        fun mid(a: DoubleArray) = f.indices.filter { f[it] in 300.0..3000.0 }.map { a[it] }.average()
        val offRaw = mid(target) - mid(measurement.raw)
        val offCorrected = mid(target) - mid(corrected)
        val raw = DoubleArray(f.size) { measurement.raw[it] + offRaw }
        val cor = DoubleArray(f.size) { corrected[it] + offCorrected }
        val all = raw + target + cor
        val lo = floor((all.minOrNull() ?: -10.0) / 5) * 5 - 5
        val hi = ceil((all.maxOrNull() ?: 10.0) / 5) * 5 + 5
        Triple(Triple(raw, target, cor), lo, hi)
    }

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(230.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(LabColors.Stage)
            .border(1.dp, LabColors.StageLine, RoundedCornerShape(18.dp))
            .damageBoundary(),
    ) {
        val data = curves
        val lo = data?.second ?: -15.0
        val hi = data?.third ?: 15.0
        val range = (hi - lo) / 2
        val centre = (hi + lo) / 2
        val plot = Plot(left = 30.dp.toPx(), top = 10.dp.toPx(), right = size.width - 10.dp.toPx(),
            bottom = size.height - 24.dp.toPx(), rangeDb = range)
        val step = if (range > 20) 10.0 else 5.0
        val labels = generateSequence(ceil(lo / step) * step) { it + step }.takeWhile { it <= hi }
            .map { (it - centre) to it.toInt().toString() }.toList()
        drawGrid(plot, measurer, style, labels, inkColor = gridInk)
        if (data == null) return@Canvas
        val (raw, target, cor) = data.first
        val f = measurement!!.frequencies
        fun shifted(a: DoubleArray) = DoubleArray(a.size) { a[it] - centre }
        clipRect(plot.left, plot.top, plot.left + (plot.right - plot.left) * appear, plot.bottom) {
            drawPath(curvePath(plot, shifted(target), f), TargetColor, style = Stroke(1.6.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(5.dp.toPx(), 4.dp.toPx()))))
            drawPath(curvePath(plot, shifted(raw), f), OriginalColor, style = Stroke(1.8.dp.toPx(), cap = StrokeCap.Round))
            drawPath(curvePath(plot, shifted(cor), f), CorrectedColor, style = Stroke(2.2.dp.toPx(), cap = StrokeCap.Round))
        }
    }
    if (measurement == null) {
        Text(
            if (correction == null) stringResource(R.string.eqx_pick_headphones) else stringResource(R.string.eqx_loading_model, correction.model),
            fontFamily = InterFontFamily, fontSize = 12.sp, color = LabColors.Faint,
            modifier = Modifier.padding(top = 10.dp, start = 6.dp),
        )
    }
}

@Composable
private fun SelectedHeadphone(
    correction: AutoEqCorrection,
    enabled: Boolean,
    fitting: Boolean,
    canTune: Boolean,
    viewModel: AudioLabViewModel,
) {
    LabCard(padding = 16.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            FormGlyph(correction.form)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(correction.model, fontFamily = InterFontFamily, fontWeight = FontWeight.Bold, fontSize = 17.sp,
                    color = LabColors.Primary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    listOfNotNull(stringResource(R.string.eqx_by_source, correction.source), correction.rig, pluralStringResource(R.plurals.eqx_filters_count, correction.filters.size, correction.filters.size)).joinToString(" · "),
                    fontFamily = InterFontFamily, fontSize = 12.sp, color = LabColors.Secondary, maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
            }
            if (fitting) {
                CircularProgressIndicator(color = LabColors.Accent, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(10.dp))
            }
        }
        Spacer(Modifier.height(8.dp))
        LabToggleRow(
            title = stringResource(R.string.eqx_apply_correction),
            subtitle = stringResource(R.string.eqx_apply_correction_desc),
            checked = enabled,
            onCheckedChange = viewModel::setCorrectionEnabled,
        )
        Spacer(Modifier.height(10.dp))

        val tuning = correction.tuning
        Column(Modifier.graphicsLayer { alpha = if (canTune) 1f else 0.4f }) {
            LabSectionLabel(stringResource(R.string.eqx_filter_bands))
            LabChips(
                chips = AutoEqTuning.BandChoices.map { LabChip(it.toString(), stringResource(R.string.eqx_bands_count, it)) },
                selectedKey = tuning.bands.toString(),
                onSelect = { key -> if (canTune) viewModel.tune { it.copy(bands = key.toInt()) } },
            )
            Spacer(Modifier.height(12.dp))
            LabSectionLabel(stringResource(R.string.eqx_correct_up_to))
            LabChips(
                chips = AutoEqTuning.MaxHzChoices.map { LabChip(it.toString(), "${it / 1000} kHz") },
                selectedKey = tuning.maxHz.toString(),
                onSelect = { key -> if (canTune) viewModel.tune { it.copy(maxHz = key.toInt()) } },
            )
            Spacer(Modifier.height(12.dp))
            LabSlider(
                label = stringResource(R.string.eqx_bass),
                valueText = (if (tuning.bassDb > 0) "+" else "") + String.format("%.0f dB", tuning.bassDb),
                value = tuning.bassDb.toFloat(),
                onValueChange = { v -> if (canTune) viewModel.tune { it.copy(bassDb = Math.round(v).toDouble()) } },
                valueRange = -6f..6f,
                enabled = canTune,
            )
            Spacer(Modifier.height(6.dp))
            LabSlider(
                label = stringResource(R.string.eqx_tilt),
                valueText = (if (tuning.tiltDbPerOctave > 0) "+" else "") + String.format("%.1f dB/oct", tuning.tiltDbPerOctave),
                value = tuning.tiltDbPerOctave.toFloat(),
                onValueChange = { v -> if (canTune) viewModel.tune { it.copy(tiltDbPerOctave = Math.round(v * 10) / 10.0) } },
                valueRange = -1f..1f,
                enabled = canTune,
            )
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (!tuning.isDefault) LabTextAction(stringResource(R.string.eqx_use_autoeq_fit), R.drawable.ic_lab_refresh, enabled = canTune) { viewModel.tune { AutoEqTuning() } }
            Spacer(Modifier.weight(1f))
            LabTextAction(stringResource(R.string.eqx_remove), R.drawable.ic_lab_close) { viewModel.clearCorrection() }
        }
    }
}

@Composable
private fun FormGlyph(form: HeadphoneForm) {
    val cs = MaterialTheme.colorScheme
    Box(
        modifier = Modifier.size(40.dp).clip(CircleShape).background(cs.secondaryContainer),
        contentAlignment = Alignment.Center,
    ) {
        Icon(painterResource(R.drawable.ic_lab_headset), null, tint = cs.onSecondaryContainer, modifier = Modifier.size(20.dp))
        if (form != HeadphoneForm.OverEar) {
            Text(if (form == HeadphoneForm.InEar) "IEM" else "BUD", fontFamily = InterFontFamily, fontWeight = FontWeight.Bold,
                fontSize = 7.sp, color = cs.onSecondaryContainer, modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 2.dp))
        }
    }
}

@Composable
private fun Database(viewModel: AudioLabViewModel, selectedPath: String?, loadingPath: String?) {
    val index by viewModel.index.collectAsState()
    val results by viewModel.results.collectAsState()
    val query by viewModel.query.collectAsState()
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    var expanded by remember { mutableStateOf<String?>(null) }

    LabCard(padding = 14.dp) {
        Row(Modifier.padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.eqx_database), fontFamily = InterFontFamily, fontWeight = FontWeight.Bold, fontSize = 17.sp, color = LabColors.Primary)
                Text(stringResource(R.string.eqx_database_desc), fontFamily = InterFontFamily, fontSize = 12.sp, color = LabColors.Secondary)
            }
            val ready = index as? AutoEqIndex.Ready
            if (ready != null) {
                Text(
                    stringResource(R.string.eqx_models_count, String.format("%,d", ready.models.size)),
                    fontFamily = InterFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = LabColors.Secondary,
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        SearchField(query = query, onQueryChange = {
            viewModel.query.value = it
            scope.launch { listState.scrollToItem(0) }
        })
        Spacer(Modifier.height(10.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(420.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(LabColors.Stage)
                .border(1.dp, LabColors.StageLine, RoundedCornerShape(18.dp)),
        ) {
            when (val state = index) {
                AutoEqIndex.Loading -> Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    repeat(7) { ShimmerRow(it) }
                }
                is AutoEqIndex.Failed -> Column(
                    Modifier.fillMaxSize().padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(state.message, fontFamily = InterFontFamily, fontSize = 14.sp, color = LabColors.Secondary)
                    Spacer(Modifier.height(12.dp))
                    LabTextAction(stringResource(R.string.eqx_try_again), R.drawable.ic_lab_refresh) { viewModel.loadIndex(refresh = true) }
                }
                is AutoEqIndex.Ready -> {
                    if (results.isEmpty()) {
                        Text(stringResource(R.string.eqx_no_match, query), fontFamily = InterFontFamily, fontSize = 14.sp,
                            color = LabColors.Secondary, modifier = Modifier.align(Alignment.Center))
                    }
                    LazyColumn(state = listState, modifier = Modifier.fillMaxSize().padding(end = 22.dp)) {
                        items(results, key = { it.name }) { model ->
                            ModelRow(
                                model = model,
                                expanded = expanded == model.name,
                                selectedPath = selectedPath,
                                loadingPath = loadingPath,
                                onClick = {
                                    if (model.entries.size == 1) viewModel.select(model.entries.first())
                                    else expanded = if (expanded == model.name) null else model.name
                                },
                                onPick = viewModel::select,
                            )
                        }
                    }
                    if (query.isBlank()) {
                        AlphabetRail(
                            modifier = Modifier.align(Alignment.CenterEnd),
                            onLetter = { letter ->
                                val target = results.indexOfFirst { m ->
                                    val c = m.name.firstOrNull()?.uppercaseChar() ?: '#'
                                    if (letter == '#') !c.isLetter() else c >= letter
                                }
                                if (target >= 0) scope.launch { listState.scrollToItem(target) }
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchField(query: String, onQueryChange: (String) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(LabColors.Row)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.weight(1f)) {
            if (query.isEmpty()) {
                Text(stringResource(R.string.eqx_search_hint), fontFamily = InterFontFamily, fontSize = 15.sp, color = LabColors.Faint)
            }
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                singleLine = true,
                textStyle = TextStyle(fontFamily = InterFontFamily, fontSize = 15.sp, color = LabColors.Primary),
                cursorBrush = SolidColor(LabColors.Accent),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (query.isNotEmpty()) {
            Icon(
                painterResource(R.drawable.ic_lab_close), stringResource(R.string.eqx_clear), tint = LabColors.Secondary,
                modifier = Modifier.size(18.dp).clip(CircleShape).clickable { onQueryChange("") },
            )
        }
    }
}

@Composable
private fun ModelRow(
    model: AutoEqModel,
    expanded: Boolean,
    selectedPath: String?,
    loadingPath: String?,
    onClick: () -> Unit,
    onPick: (AutoEqEntry) -> Unit,
) {
    val picked = model.entries.any { it.path == selectedPath }
    Column(Modifier.fillMaxWidth().animateContentSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 14.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(painterResource(R.drawable.ic_lab_headset), null, tint = LabColors.Secondary, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(model.name, fontFamily = InterFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 15.sp,
                    color = LabColors.Primary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    if (model.entries.size == 1) model.entries.first().sourceLabel else stringResource(R.string.eqx_profiles_count, model.entries.size),
                    fontFamily = InterFontFamily, fontSize = 12.sp, color = LabColors.Secondary, maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
            }
            when {
                model.entries.size == 1 && loadingPath == model.entries.first().path ->
                    CircularProgressIndicator(color = LabColors.Accent, strokeWidth = 2.dp, modifier = Modifier.size(16.dp))
                picked && model.entries.size == 1 ->
                    Icon(painterResource(R.drawable.ic_lab_check), null, tint = LabColors.Accent, modifier = Modifier.size(18.dp))
                model.entries.size > 1 -> {
                    val turn by animateFloatAsState(if (expanded) 90f else 0f, tween(200), label = "chevron")
                    Icon(painterResource(R.drawable.ic_lab_navigate_next), null, tint = LabColors.Faint,
                        modifier = Modifier.size(18.dp).graphicsLayer { rotationZ = turn })
                }
            }
        }
        if (expanded) {
            model.entries.forEach { entry ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 44.dp, end = 10.dp, bottom = 4.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (entry.path == selectedPath) LabColors.RowSelected else LabColors.Row)
                        .clickable { onPick(entry) }
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(entry.sourceLabel, fontFamily = InterFontFamily, fontSize = 13.sp, color = LabColors.Primary,
                        modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    when {
                        loadingPath == entry.path ->
                            CircularProgressIndicator(color = LabColors.Accent, strokeWidth = 2.dp, modifier = Modifier.size(14.dp))
                        entry.path == selectedPath ->
                            Icon(painterResource(R.drawable.ic_lab_check), null, tint = LabColors.Accent, modifier = Modifier.size(16.dp))
                    }
                }
            }
            Spacer(Modifier.height(6.dp))
        }
    }
}

@Composable
private fun AlphabetRail(modifier: Modifier, onLetter: (Char) -> Unit) {
    val letters = ('A'..'Z').toList() + '#'
    var railHeight by remember { mutableStateOf(1) }
    Column(
        modifier = modifier
            .fillMaxHeight()
            .width(22.dp)
            .padding(vertical = 10.dp)
            .onSizeChanged { railHeight = it.height.coerceAtLeast(1) }
            .pointerInput(Unit) {
                fun pick(y: Float) = onLetter(letters[((y / railHeight) * letters.size).toInt().coerceIn(0, letters.size - 1)])
                detectTapGestures { pick(it.y) }
            }
            .pointerInput(Unit) {
                detectVerticalDragGestures { change, _ ->
                    change.consume()
                    onLetter(letters[((change.position.y / railHeight) * letters.size).toInt().coerceIn(0, letters.size - 1)])
                }
            },
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        letters.forEach {
            Text(it.toString(), fontFamily = InterFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 9.sp, color = LabColors.Faint)
        }
    }
}

@Composable
private fun ShimmerRow(index: Int) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(18.dp).clip(CircleShape).background(LabColors.Row))
        Spacer(Modifier.width(12.dp))
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Box(Modifier.height(12.dp).width((120 + (index * 37) % 90).dp).clip(RoundedCornerShape(6.dp)).background(LabColors.Row))
            Box(Modifier.height(9.dp).width(70.dp).clip(RoundedCornerShape(5.dp)).background(LabColors.Chip))
        }
    }
}
