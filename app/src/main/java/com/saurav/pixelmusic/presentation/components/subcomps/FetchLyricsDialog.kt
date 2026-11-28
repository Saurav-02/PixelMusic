package com.saurav.pixelmusic.presentation.components.subcomps

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Search
import androidx.compose.ui.res.painterResource
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.saurav.pixelmusic.R
import com.saurav.pixelmusic.data.model.Song
import com.saurav.pixelmusic.data.repository.LyricsSearchResult
import com.saurav.pixelmusic.presentation.viewmodel.LyricsSearchUiState
import com.saurav.pixelmusic.utils.ProviderText
import com.saurav.pixelmusic.utils.shapes.RoundedStarShape

private fun cleanSongTitleForLyrics(rawTitle: String): String {
    if (rawTitle.isBlank()) return ""
    val cleaned = rawTitle
        .replace(Regex("""\s*[\(\[][^\)\]]*[\)\]]"""), " ")
        .replace(Regex("""\s*[\(\[][^\)\]]*$"""), " ")
        .replace(Regex("""\s+[-|]\s+(?:from\b|official\b|lyrical\b|full\s+video\b|video\b|audio\b).*$""", RegexOption.IGNORE_CASE), " ")
        .replace(Regex("""\s+\b(?:feat\.?|ft\.?|featuring)\b.*$""", RegexOption.IGNORE_CASE), " ")
        .replace(Regex("""\s+"""), " ")
        .trim()
        .trim('-', '|', '/', '~')
        .trim()
    return cleaned.ifBlank { rawTitle.trim() }
}

private fun cleanArtistForLyrics(rawArtist: String?): String {
    if (rawArtist.isNullOrBlank() || rawArtist.equals("<unknown>", ignoreCase = true)) return ""
    val withoutTopic = rawArtist.replace(Regex("""\s*-\s*Topic$""", RegexOption.IGNORE_CASE), "").trim()
    val primary = withoutTopic
        .split(Regex("""\s*(?:,|&|\bfeat\.?|\bft\.?|\bfeaturing\b|\bx\b|\band\b)\s*""", RegexOption.IGNORE_CASE))
        .firstOrNull { it.isNotBlank() }
        ?.trim()
        ?: withoutTopic
    return primary
}

private fun formatSongTildeArtist(title: String, artist: String?): String {
    val cleanT = title.trim()
    val cleanA = artist?.trim()?.takeIf { it.isNotBlank() && !it.equals("<unknown>", ignoreCase = true) }
    return if (cleanA != null) "$cleanT ~ $cleanA" else cleanT
}

private fun parseSongTildeArtist(input: String): Pair<String, String?> {
    val trimmed = input.trim()
    if (trimmed.contains("~")) {
        val songPart = trimmed.substringBefore("~").trim()
        val artistPart = trimmed.substringAfter("~").trim().takeIf { it.isNotBlank() }
        return songPart.ifBlank { trimmed } to artistPart
    }
    if (trimmed.contains(" - ")) {
        val songPart = trimmed.substringBefore(" - ").trim()
        val artistPart = trimmed.substringAfter(" - ").trim().takeIf { it.isNotBlank() }
        if (songPart.isNotBlank() && artistPart != null) {
            return songPart to artistPart
        }
    }
    return trimmed to null
}

@Composable
fun FetchLyricsDialog(
    uiState: LyricsSearchUiState,
    currentSong: Song?,
    onConfirm: (Boolean) -> Unit,
    onPickResult: (LyricsSearchResult) -> Unit,
    onManualSearch: (String, String?, Boolean) -> Unit,
    onDismiss: () -> Unit,
    onImport: () -> Unit,
    initialManualMode: Boolean = false
) {
    if (uiState is LyricsSearchUiState.Success) return

    var forcePickResults by rememberSaveable(initialManualMode) { mutableStateOf(initialManualMode) }
    var manualModeEnabled by rememberSaveable(initialManualMode) { mutableStateOf(initialManualMode) }
    var forceManualViewFromPicker by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(uiState) {
        if (uiState is LyricsSearchUiState.Loading) {
            forceManualViewFromPicker = false
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .padding(24.dp)
                .fillMaxWidth(),
            shape = RoundedCornerShape(32.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                when {
                    forceManualViewFromPicker -> {
                        IdleContent(
                            currentSong = currentSong,
                            manualModeEnabled = true,
                            onToggleManualMode = {
                                if (!it) forceManualViewFromPicker = false
                                manualModeEnabled = it
                            },
                            forcePickResults = forcePickResults,
                            onToggleForcePickResults = { forcePickResults = it },
                            onSearch = { onConfirm(forcePickResults) },
                            onManualSearch = { title, artist ->
                                forceManualViewFromPicker = false
                                onManualSearch(title, artist, !forcePickResults)
                            },
                            onImport = onImport,
                            onCancel = { forceManualViewFromPicker = false }
                        )
                    }
                    uiState is LyricsSearchUiState.Idle -> {
                        IdleContent(
                            currentSong = currentSong,
                            manualModeEnabled = manualModeEnabled,
                            onToggleManualMode = { manualModeEnabled = it },
                            forcePickResults = forcePickResults,
                            onToggleForcePickResults = { forcePickResults = it },
                            onSearch = { onConfirm(forcePickResults) },
                            onManualSearch = { title, artist ->
                                onManualSearch(title, artist, !forcePickResults)
                            },
                            onImport = onImport,
                            onCancel = onDismiss
                        )
                    }
                    uiState is LyricsSearchUiState.Loading -> {
                        LoadingContent()
                    }
                    uiState is LyricsSearchUiState.PickResult -> {
                        PickResultContent(
                            results = uiState.results,
                            onPickResult = onPickResult,
                            onOpenManualSearch = {
                                manualModeEnabled = true
                                forceManualViewFromPicker = true
                            },
                            onCancel = onDismiss
                        )
                    }
                    uiState is LyricsSearchUiState.NotFound -> {
                        NotFoundContent(
                            message = uiState.message,
                            initialTitle = currentSong?.title.orEmpty(),
                            initialArtist = currentSong?.displayArtist,
                            forcePickResults = forcePickResults,
                            onToggleForcePickResults = { forcePickResults = it },
                            onManualSearch = { title, artist ->
                                onManualSearch(title, artist, !forcePickResults)
                            },
                            onCancel = onDismiss
                        )
                    }
                    uiState is LyricsSearchUiState.Error -> {
                        ErrorContent(
                            message = uiState.message,
                            onRetryManualSearch = {
                                manualModeEnabled = true
                                forceManualViewFromPicker = true
                            },
                            onDismiss = onDismiss
                        )
                    }
                }
            }
        }
    }
}

// --------------------------------------------------------------------------
// Sub-componentes de Estados (Internal)
// --------------------------------------------------------------------------

@Composable
private fun SongTildeArtistInputSection(
    rawTitle: String,
    rawArtist: String?,
    queryText: String,
    onQueryTextChange: (String) -> Unit,
    onTriggerSearch: () -> Unit
) {
    val cleanedTitle = remember(rawTitle) { cleanSongTitleForLyrics(rawTitle) }
    val cleanedArtist = remember(rawArtist) { cleanArtistForLyrics(rawArtist) }
    val cleanedCombo = remember(cleanedTitle, cleanedArtist) {
        formatSongTildeArtist(cleanedTitle, cleanedArtist)
    }
    val rawCombo = remember(rawTitle, rawArtist) {
        formatSongTildeArtist(rawTitle, rawArtist)
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedTextField(
            value = queryText,
            onValueChange = onQueryTextChange,
            label = { Text("Song ~ Artist") },
            placeholder = { Text("e.g. Jugraafiya ~ Udit Narayan") },
            singleLine = true,
            shape = RoundedCornerShape(18.dp),
            trailingIcon = {
                if (queryText.isNotEmpty()) {
                    IconButton(onClick = { onQueryTextChange("") }) {
                        Icon(
                            imageVector = Icons.Rounded.Clear,
                            contentDescription = "Clear"
                        )
                    }
                }
            },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(
                onSearch = {
                    if (queryText.isNotBlank()) onTriggerSearch()
                }
            ),
            modifier = Modifier.fillMaxWidth()
        )

        if (rawTitle.isNotBlank()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (cleanedCombo.isNotBlank() && cleanedCombo != queryText) {
                    AssistChip(
                        onClick = { onQueryTextChange(cleanedCombo) },
                        label = { Text("Cleaned", maxLines = 1) },
                        leadingIcon = {
                            Icon(
                                painter = painterResource(R.drawable.rounded_auto_awesome_24),
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    )
                }
                if (cleanedTitle.isNotBlank() && cleanedTitle != queryText) {
                    AssistChip(
                        onClick = { onQueryTextChange(cleanedTitle) },
                        label = { Text("Song only", maxLines = 1) }
                    )
                }
                if (rawCombo.isNotBlank() && rawCombo != queryText && rawCombo != cleanedCombo) {
                    AssistChip(
                        onClick = { onQueryTextChange(rawCombo) },
                        label = { Text("Original", maxLines = 1) }
                    )
                }
            }
        }
    }
}

@Composable
private fun IdleContent(
    currentSong: Song?,
    manualModeEnabled: Boolean,
    onToggleManualMode: (Boolean) -> Unit,
    forcePickResults: Boolean,
    onToggleForcePickResults: (Boolean) -> Unit,
    onSearch: () -> Unit,
    onManualSearch: (String, String?) -> Unit,
    onImport: () -> Unit,
    onCancel: () -> Unit
) {
    val rawTitle = currentSong?.title.orEmpty()
    val rawArtist = currentSong?.displayArtist
    val defaultCleanedQuery = remember(rawTitle, rawArtist) {
        formatSongTildeArtist(
            cleanSongTitleForLyrics(rawTitle),
            cleanArtistForLyrics(rawArtist)
        )
    }
    var manualQuery by rememberSaveable(rawTitle, rawArtist) {
        mutableStateOf(defaultCleanedQuery)
    }

    val triggerActiveSearch = {
        if (manualModeEnabled) {
            val (parsedTitle, parsedArtist) = parseSongTildeArtist(manualQuery)
            if (parsedTitle.isNotBlank()) {
                onManualSearch(parsedTitle, parsedArtist)
            }
        } else {
            onSearch()
        }
    }

    Box(
        modifier = Modifier
            .size(64.dp)
            .clip(RoundedStarShape(sides = 8, curve = 0.1, rotation = 0f))
            .background(MaterialTheme.colorScheme.secondaryContainer),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(if (manualModeEnabled) R.drawable.rounded_edit_24 else R.drawable.rounded_music_note_24),
            contentDescription = null,
            modifier = Modifier.size(34.dp),
            tint = MaterialTheme.colorScheme.onSecondaryContainer
        )
    }

    Spacer(modifier = Modifier.height(16.dp))

    if (currentSong != null) {
        Text(
            text = currentSong.title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = currentSong.displayArtist,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    } else {
        Text(
            text = stringResource(R.string.lyrics_not_found),
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center
        )
    }

    Spacer(modifier = Modifier.height(16.dp))

    // Manual Search (Song ~ Artist) Toggle Card
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Manual search (Song ~ Artist)",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Text(
                        text = "Type custom song ~ artist name for messy titles",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.75f)
                    )
                }
                Switch(
                    checked = manualModeEnabled,
                    onCheckedChange = onToggleManualMode,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                        checkedTrackColor = MaterialTheme.colorScheme.primary
                    )
                )
            }

            AnimatedVisibility(visible = manualModeEnabled) {
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    SongTildeArtistInputSection(
                        rawTitle = rawTitle,
                        rawArtist = rawArtist,
                        queryText = manualQuery,
                        onQueryTextChange = { manualQuery = it },
                        onTriggerSearch = triggerActiveSearch
                    )
                }
            }
        }
    }

    Spacer(modifier = Modifier.height(10.dp))

    // Show All Options Toggle Card
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.fetch_lyrics_show_options_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Text(
                    text = stringResource(R.string.fetch_lyrics_show_options_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.75f)
                )
            }
            Switch(
                checked = forcePickResults,
                onCheckedChange = onToggleForcePickResults,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                    checkedTrackColor = MaterialTheme.colorScheme.primary
                )
            )
        }
    }

    Spacer(modifier = Modifier.height(16.dp))

    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Button(
            onClick = triggerActiveSearch,
            enabled = !manualModeEnabled || manualQuery.isNotBlank(),
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = CircleShape
        ) {
            Icon(Icons.Rounded.Search, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (manualModeEnabled) "Search Manual Query" else stringResource(R.string.search),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Button(
            onClick = onImport,
            modifier = Modifier.fillMaxWidth().height(50.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.secondary,
                contentColor = MaterialTheme.colorScheme.onSecondary
            )
        ) {
            Icon(painter = painterResource(R.drawable.rounded_upload_file_24), contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.import_file))
        }

        TextButton(
            onClick = onCancel,
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = RoundedCornerShape(18.dp)
        ) {
            Text(stringResource(R.string.cancel), maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun LoadingContent() {
    Column(
        modifier = Modifier.padding(vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularWavyProgressIndicator(
            modifier = Modifier.size(56.dp)
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = stringResource(R.string.searching_lyrics),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun PickResultContent(
    results: List<LyricsSearchResult>,
    onPickResult: (LyricsSearchResult) -> Unit,
    onOpenManualSearch: () -> Unit,
    onCancel: () -> Unit
) {
    Text(
        text = stringResource(R.string.found_n_matches_format).format(results.size),
        style = MaterialTheme.typography.headlineSmall,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth()
    )

    Spacer(modifier = Modifier.height(20.dp))

    LazyColumn(
        modifier = Modifier.heightIn(max = 330.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(bottom = 8.dp)
    ) {
        items(results, key = { it.hashCode() }) { result ->
            ResultItemCard(result = result, onClick = { onPickResult(result) })
        }
    }

    Spacer(modifier = Modifier.height(14.dp))

    FilledTonalButton(
        onClick = onOpenManualSearch,
        modifier = Modifier.fillMaxWidth().height(48.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Icon(painter = painterResource(R.drawable.rounded_edit_24), contentDescription = null, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text("Search manually (Song ~ Artist)", maxLines = 1, overflow = TextOverflow.Ellipsis)
    }

    Spacer(modifier = Modifier.height(6.dp))

    TextButton(
        onClick = onCancel,
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onSurfaceVariant)
    ) {
        Text(stringResource(R.string.cancel), maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun ResultItemCard(
    result: LyricsSearchResult,
    onClick: () -> Unit
) {
    // Check if the parsed lyrics contain synced lines
    val hasSyncedLyrics = !result.lyrics.synced.isNullOrEmpty()
    
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        if (hasSyncedLyrics) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surfaceVariant
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(R.drawable.rounded_music_note_24),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = if (hasSyncedLyrics) MaterialTheme.colorScheme.onPrimaryContainer
                           else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = result.hit.result.title ?: "Unknown Title",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f, fill = false),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (hasSyncedLyrics) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ) {
                            Text(
                                text = stringResource(R.string.lyrics_synced_badge),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(
                            R.string.presentation_batch_g_list_song_artist_album,
                            result.hit.result.artist ?: "Unknown Artist",
                            result.hit.result.album ?: "Unknown Album"
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    // Show which provider found these lyrics!
                    Text(
                        text = result.hit.provider.name,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun NotFoundContent(
    initialTitle: String,
    initialArtist: String?,
    message: String,
    forcePickResults: Boolean = true,
    onToggleForcePickResults: (Boolean) -> Unit = {},
    onManualSearch: (String, String?) -> Unit,
    onCancel: () -> Unit
) {
    val defaultCleanedQuery = remember(initialTitle, initialArtist) {
        formatSongTildeArtist(
            cleanSongTitleForLyrics(initialTitle),
            cleanArtistForLyrics(initialArtist)
        )
    }
    var manualQuery by rememberSaveable(initialTitle, initialArtist) {
        mutableStateOf(defaultCleanedQuery)
    }

    val triggerManual = {
        val (parsedTitle, parsedArtist) = parseSongTildeArtist(manualQuery)
        if (parsedTitle.isNotBlank()) {
            onManualSearch(parsedTitle, parsedArtist)
        }
    }

    Box(
        modifier = Modifier
            .size(64.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.secondaryContainer),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(R.drawable.rounded_search_off_24),
            contentDescription = null,
            modifier = Modifier.size(34.dp),
            tint = MaterialTheme.colorScheme.onSecondaryContainer
        )
    }

    Spacer(Modifier.height(14.dp))

    Text(
        text = stringResource(R.string.lyrics_not_found),
        style = MaterialTheme.typography.headlineSmall
    )

    Spacer(Modifier.height(8.dp))

    Text(
        text = "Enter Song ~ Artist below to search across all lyric providers",
        style = MaterialTheme.typography.bodyMedium,
        textAlign = TextAlign.Center,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )

    Spacer(Modifier.height(14.dp))

    SongTildeArtistInputSection(
        rawTitle = initialTitle,
        rawArtist = initialArtist,
        queryText = manualQuery,
        onQueryTextChange = { manualQuery = it },
        onTriggerSearch = triggerManual
    )

    Spacer(Modifier.height(12.dp))

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.fetch_lyrics_show_options_title),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Text(
                    text = stringResource(R.string.fetch_lyrics_show_options_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.75f)
                )
            }
            Switch(
                checked = forcePickResults,
                onCheckedChange = onToggleForcePickResults,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                    checkedTrackColor = MaterialTheme.colorScheme.primary
                )
            )
        }
    }

    Spacer(Modifier.height(18.dp))

    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Button(
            onClick = triggerManual,
            enabled = manualQuery.isNotBlank(),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(18.dp)
        ) {
            Icon(Icons.Rounded.Search, null)
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.search))
        }

        TextButton(
            onClick = onCancel,
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = RoundedCornerShape(18.dp)
        ) {
            Text(stringResource(R.string.cancel), maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun ErrorContent(
    message: String,
    onRetryManualSearch: () -> Unit,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(72.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.errorContainer),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(R.drawable.rounded_error_outline_24),
            contentDescription = null,
            modifier = Modifier.size(36.dp),
            tint = MaterialTheme.colorScheme.onErrorContainer
        )
    }

    Spacer(modifier = Modifier.height(24.dp))

    Text(
        text = stringResource(R.string.error),
        style = MaterialTheme.typography.headlineSmall,
        color = MaterialTheme.colorScheme.error
    )

    Spacer(modifier = Modifier.height(8.dp))

    Text(
        text = message,
        style = MaterialTheme.typography.bodyLarge,
        textAlign = TextAlign.Center,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )

    Spacer(modifier = Modifier.height(24.dp))

    FilledTonalButton(
        onClick = onRetryManualSearch,
        modifier = Modifier.fillMaxWidth().height(48.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Icon(painter = painterResource(R.drawable.rounded_edit_24), contentDescription = null, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text("Search manually (Song ~ Artist)", maxLines = 1, overflow = TextOverflow.Ellipsis)
    }

    Spacer(modifier = Modifier.height(10.dp))

    Button(
        onClick = onDismiss,
        modifier = Modifier.fillMaxWidth().height(50.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.error,
            contentColor = MaterialTheme.colorScheme.onError
        ),
        shape = RoundedCornerShape(18.dp)
    ) {
        Text(stringResource(R.string.ok), maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}
