package com.saurav.pixelmusic.presentation.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.saurav.pixelmusic.data.lastfm.LastFM
import com.saurav.pixelmusic.data.model.Song
import com.saurav.pixelmusic.data.model.Playlist
import com.saurav.pixelmusic.data.preferences.PlaylistPreferencesRepository
import com.saurav.pixelmusic.data.preferences.UserPreferencesRepository
import com.saurav.pixelmusic.data.repository.MusicRepository
import com.saurav.pixelmusic.data.remote.youtube.toNativeSong
import saurav.shru.pixelmusic.innertube.YouTube
import saurav.shru.pixelmusic.innertube.models.SongItem
import saurav.shru.pixelmusic.innertube.models.filterVideo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.serialization.json.*
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import java.util.UUID
import kotlin.math.absoluteValue
import timber.log.Timber

data class LastFmTrack(
    val name: String,
    val artist: String,
    val imageUrl: String = ""
)

data class SmartMixUiState(
    val isLastfmLoggedIn: Boolean = false,
    val username: String = "",
    val isGenerating: Boolean = false,
    val generationProgress: String = "",
    val selectedMode: String? = null,
    val trackCount: Int = 25,
    val timePeriod: String = "overall", // overall, 12month, 6month, 3month, 1month, 7day
    val seedTrackName: String = "",
    val seedArtistName: String = "",
    val seedArtistInput: String = "",
    val tagInput: String = "",
    val isSearchingSeed: Boolean = false,
    val searchTrackResults: List<LastFmTrack> = emptyList(),
    val searchArtistResults: List<String> = emptyList(),
    val topTracksForSeed: List<LastFmTrack> = emptyList(),
    val topArtistsForSeed: List<String> = emptyList(),
    val isSearchingTopSeeds: Boolean = false,
    val toastMessage: String? = null,
    val generatedPlaylistId: String? = null
)

@HiltViewModel
class SmartMixViewModel @Inject constructor(
    private val musicRepository: MusicRepository,
    private val playlistPreferencesRepository: PlaylistPreferencesRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val dailyMixStateHolder: DailyMixStateHolder,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(SmartMixUiState())
    
    // Daily Mix Generation States for Last.fm Recommendation Generator
    private val _dailyMixGenGenerating = MutableStateFlow(false)
    val dailyMixGenGenerating: StateFlow<Boolean> = _dailyMixGenGenerating.asStateFlow()

    private val _dailyMixGenProgress = MutableStateFlow<String?>(null)
    val dailyMixGenProgress: StateFlow<String?> = _dailyMixGenProgress.asStateFlow()

    private val _dailyMixGenError = MutableStateFlow<String?>(null)
    val dailyMixGenError: StateFlow<String?> = _dailyMixGenError.asStateFlow()

    private val _dailyMixGenSuccess = MutableStateFlow(false)
    val dailyMixGenSuccess: StateFlow<Boolean> = _dailyMixGenSuccess.asStateFlow()
    val uiState: StateFlow<SmartMixUiState> = _uiState.asStateFlow()

    private val json = Json { 
        isLenient = true
        ignoreUnknownKeys = true 
    }

    init {
        viewModelScope.launch {
            userPreferencesRepository.lastfmSessionFlow.collect { session ->
                _uiState.update { it.copy(isLastfmLoggedIn = session.isNotEmpty()) }
            }
        }
        viewModelScope.launch {
            userPreferencesRepository.lastfmUsernameFlow.collect { username ->
                _uiState.update { it.copy(username = username) }
            }
        }
    }

    fun setSelectedMode(mode: String?) {
        _uiState.update { it.copy(selectedMode = mode) }
    }

    fun setTrackCount(count: Int) {
        _uiState.update { it.copy(trackCount = count) }
    }

    fun setTimePeriod(period: String) {
        _uiState.update { it.copy(timePeriod = period) }
    }

    fun setSeedTrackName(name: String) {
        _uiState.update { it.copy(seedTrackName = name) }
    }

    fun setSeedArtistName(name: String) {
        _uiState.update { it.copy(seedArtistName = name) }
    }

    fun setSeedArtistInput(name: String) {
        _uiState.update { it.copy(seedArtistInput = name) }
    }

    fun setTagInput(tag: String) {
        _uiState.update { it.copy(tagInput = tag) }
    }

    fun clearToast() {
        _uiState.update { it.copy(toastMessage = null) }
    }

    fun clearGeneratedPlaylistId() {
        _uiState.update { it.copy(generatedPlaylistId = null) }
    }

    // --- Search Seed Methods ---

    fun searchSeedTrack() {
        val track = _uiState.value.seedTrackName.trim()
        val artist = _uiState.value.seedArtistName.trim()
        if (track.isEmpty()) {
            _uiState.update { it.copy(toastMessage = "Enter track name to search") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isSearchingSeed = true, searchTrackResults = emptyList()) }
            try {
                val params = mutableMapOf("track" to track, "limit" to "15")
                if (artist.isNotEmpty()) params["artist"] = artist
                val res = LastFM.get("track.search", params).getOrThrow()
                val jsonEl = json.parseToJsonElement(res)
                val tracks = parseLastFmTracks(jsonEl)
                _uiState.update { it.copy(searchTrackResults = tracks, isSearchingSeed = false) }
            } catch (e: Exception) {
                Timber.e(e, "Track search failed")
                _uiState.update { it.copy(isSearchingSeed = false, toastMessage = "Search failed: ${e.localizedMessage}") }
            }
        }
    }

    fun searchSeedArtist() {
        val artist = _uiState.value.seedArtistInput.trim()
        if (artist.isEmpty()) {
            _uiState.update { it.copy(toastMessage = "Enter artist name to search") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isSearchingSeed = true, searchArtistResults = emptyList()) }
            try {
                val params = mapOf("artist" to artist, "limit" to "15")
                val res = LastFM.get("artist.search", params).getOrThrow()
                val jsonEl = json.parseToJsonElement(res)
                val artists = parseLastFmArtists(jsonEl)
                _uiState.update { it.copy(searchArtistResults = artists, isSearchingSeed = false) }
            } catch (e: Exception) {
                Timber.e(e, "Artist search failed")
                _uiState.update { it.copy(isSearchingSeed = false, toastMessage = "Search failed: ${e.localizedMessage}") }
            }
        }
    }

    fun loadTopTracksForSeed() {
        val user = _uiState.value.username
        if (user.isEmpty()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSearchingTopSeeds = true, topTracksForSeed = emptyList()) }
            try {
                val res = LastFM.get("user.gettoptracks", mapOf("user" to user, "limit" to "20", "period" to "overall")).getOrThrow()
                val jsonEl = json.parseToJsonElement(res)
                val tracks = parseLastFmTracks(jsonEl)
                _uiState.update { it.copy(topTracksForSeed = tracks, isSearchingTopSeeds = false) }
            } catch (e: Exception) {
                Timber.e(e, "Load top tracks failed")
                _uiState.update { it.copy(isSearchingTopSeeds = false, toastMessage = "Failed to load top tracks: ${e.localizedMessage}") }
            }
        }
    }

    fun loadTopArtistsForSeed() {
        val user = _uiState.value.username
        if (user.isEmpty()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSearchingTopSeeds = true, topArtistsForSeed = emptyList()) }
            try {
                val res = LastFM.get("user.gettopartists", mapOf("user" to user, "limit" to "20", "period" to "overall")).getOrThrow()
                val jsonEl = json.parseToJsonElement(res)
                val artists = parseLastFmTopArtists(jsonEl)
                _uiState.update { it.copy(topArtistsForSeed = artists, isSearchingTopSeeds = false) }
            } catch (e: Exception) {
                Timber.e(e, "Load top artists failed")
                _uiState.update { it.copy(isSearchingTopSeeds = false, toastMessage = "Failed to load top artists: ${e.localizedMessage}") }
            }
        }
    }

    // --- Generation Method ---

    fun generatePlaylist() {
        val state = _uiState.value
        val mode = state.selectedMode ?: return
        val user = state.username
        val limit = state.trackCount

        if (user.isEmpty()) {
            _uiState.update { it.copy(toastMessage = "Link your Last.fm account first") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isGenerating = true, generationProgress = "Connecting to Last.fm...") }
            try {
                // Step 1: Query Last.fm for tracks metadata
                val lastFmTracks = withContext(Dispatchers.IO) {
                    when (mode) {
                        "top" -> fetchTopTracks(user, limit, state.timePeriod)
                        "library" -> fetchTopTracks(user, limit, "overall")
                        "recent" -> fetchRecentTracks(user, limit)
                        "similar-tracks" -> {
                            if (state.seedTrackName.isBlank() || state.seedArtistName.isBlank()) {
                                throw IllegalArgumentException("Enter a seed track and artist")
                            }
                            fetchSimilarTracks(state.seedTrackName, state.seedArtistName, limit)
                        }
                        "similar-artists" -> {
                            if (state.seedArtistInput.isBlank()) {
                                throw IllegalArgumentException("Enter a seed artist")
                            }
                            fetchSimilarArtistTracks(state.seedArtistInput, limit)
                        }
                        "tag" -> {
                            if (state.tagInput.isBlank()) {
                                throw IllegalArgumentException("Enter a genre/tag")
                            }
                            fetchTagTracks(state.tagInput, limit)
                        }
                        "mix" -> fetchMix(user, limit)
                        "recommendations" -> fetchRecommendations(user, limit)
                        else -> emptyList()
                    }
                }

                if (lastFmTracks.isEmpty()) {
                    throw Exception("No tracks found on Last.fm for your selections.")
                }

                // Step 2: Resolve tracks to YouTube Music IDs
                val basePlaylistName = generatePlaylistName(mode, state)
                val allPlaylists = playlistPreferencesRepository.userPlaylistsFlow.first()
                var existingPlaylist = allPlaylists.find { it.name.equals(basePlaylistName, ignoreCase = true) }
                var finalPlaylistName = basePlaylistName

                // If it exists but is NOT AI-generated, resolve name collision by adding a suffix
                if (existingPlaylist != null && !existingPlaylist.isAiGenerated) {
                    var suffixCounter = 1
                    while (allPlaylists.any { it.name.equals("$basePlaylistName ($suffixCounter)", ignoreCase = true) }) {
                        suffixCounter++
                    }
                    finalPlaylistName = "$basePlaylistName ($suffixCounter)"
                    existingPlaylist = null
                }

                val existingSongIds = existingPlaylist?.songIds?.map { it.removePrefix("youtube_") }?.toSet().orEmpty()

                _uiState.update { it.copy(generationProgress = "Resolving streamable tracks on YouTube Music (0/${lastFmTracks.size})...") }

                val resolvedSongs = mutableListOf<Song>()
                val semaphore = Semaphore(3) // parallel resolution with concurrency limit
                val deferreds = lastFmTracks.mapIndexed { index, track ->
                    async(Dispatchers.IO) {
                        semaphore.withPermit {
                            val song = resolveTrackToYoutubeSong(track)
                            if (song != null) {
                                synchronized(resolvedSongs) {
                                    val ytId = song.youtubeId
                                    if (ytId != null && 
                                        !existingSongIds.contains(ytId) && 
                                        resolvedSongs.none { it.youtubeId == ytId }) {
                                        resolvedSongs.add(song)
                                    }
                                    _uiState.update { it.copy(generationProgress = "Resolving streamable tracks on YouTube Music (${resolvedSongs.size}/${lastFmTracks.size})...") }
                                }
                            }
                        }
                    }
                }
                deferreds.awaitAll()

                val finalSongs = resolvedSongs.take(limit)

                if (finalSongs.isEmpty()) {
                    throw Exception("Could not find playable matches on YouTube Music for any generated tracks.")
                }

                // Step 3: Insert songs into Room database
                _uiState.update { it.copy(generationProgress = "Saving tracks to local database...") }
                withContext(Dispatchers.IO) {
                    musicRepository.insertYoutubeSongs(finalSongs)
                }

                // Step 4: Create playlist in Room database
                _uiState.update { it.copy(generationProgress = "Creating your mix playlist...") }
                val songIds = finalSongs.map { "youtube_${it.youtubeId}" }
                
                val coverImage = lastFmTracks.firstOrNull { it.imageUrl.isNotEmpty() }?.imageUrl

                val newPlaylist = withContext(Dispatchers.IO) {
                    if (existingPlaylist != null && existingPlaylist.isAiGenerated) {
                        val updated = existingPlaylist.copy(
                            songIds = songIds,
                            coverImageUri = coverImage,
                            lastModified = System.currentTimeMillis()
                        )
                        playlistPreferencesRepository.updatePlaylist(updated)
                        updated
                    } else {
                        playlistPreferencesRepository.createPlaylist(
                            name = finalPlaylistName,
                            songIds = songIds,
                            isAiGenerated = true,
                            coverImageUri = coverImage,
                            source = "LASTFM_MIX"
                        )
                    }
                }

                _uiState.update {
                    it.copy(
                        isGenerating = false,
                        generatedPlaylistId = newPlaylist.id,
                        toastMessage = "Smart Mix created successfully!"
                    )
                }
            } catch (e: Exception) {
                Timber.e(e, "Mix generation failed")
                _uiState.update {
                    it.copy(
                        isGenerating = false,
                        toastMessage = "Generation failed: ${e.localizedMessage}"
                    )
                }
            }
        }
    }

    private suspend fun resolveTrackToYoutubeSong(track: LastFmTrack): Song? {
        return try {
            val query = "${track.name} ${track.artist}"
            val result = YouTube.search(query, YouTube.SearchFilter.FILTER_SONG).getOrNull()
            val songsList = result?.items?.filterIsInstance<SongItem>()?.filterVideo(true).orEmpty()
            
            // Prefer ATV (official audio tracks)
            val songItem = songsList.firstOrNull {
                val musicVideoType = it.endpoint?.watchEndpointMusicSupportedConfigs?.watchEndpointMusicConfig?.musicVideoType
                musicVideoType == "MUSIC_VIDEO_TYPE_ATV"
            } ?: songsList.firstOrNull() ?: return null

            val native = songItem.toNativeSong()
            native.copy(
                title = if (track.name.isNotBlank()) track.name else native.title,
                artist = if (track.artist.isNotBlank()) track.artist else native.artist,
                album = native.album.takeIf { it.isNotBlank() && it != "YouTube Music" } ?: "YouTube Music"
            )
        } catch (e: Exception) {
            null
        }
    }

    private fun generatePlaylistName(mode: String, state: SmartMixUiState): String {
        return when (mode) {
            "top" -> {
                when (state.timePeriod) {
                    "overall" -> "My Era"
                    "12month" -> "Yearly Vibe"
                    "6month" -> "6M Vibe Check"
                    "3month" -> "3M Vibe Check"
                    "1month" -> "Monthly Vibe"
                    "7day" -> "Weekly Vibe"
                    else -> "My Vibe"
                }
            }
            "library" -> "My Vault"
            "recent" -> "On Repeat"
            "similar-tracks" -> {
                val shortName = if (state.seedTrackName.length > 15) state.seedTrackName.take(15).trim() + "..." else state.seedTrackName.trim()
                "$shortName Vibes"
            }
            "similar-artists" -> {
                val shortName = if (state.seedArtistInput.length > 15) state.seedArtistInput.take(15).trim() + "..." else state.seedArtistInput.trim()
                "Like $shortName"
            }
            "tag" -> {
                val shortName = if (state.tagInput.length > 15) state.tagInput.take(15).trim() + "..." else state.tagInput.trim()
                "${shortName.replaceFirstChar { it.uppercase() }} Vibe"
            }
            "mix" -> "Algorhythm"
            "recommendations" -> "For You"
            else -> "Smart Vibe"
        }
    }

    // --- Last.fm Track Fetch Helpers ---

    private suspend fun fetchTopTracks(user: String, limit: Int, period: String): List<LastFmTrack> {
        val page = (1..3).random().toString()
        val fetchLimit = (limit * 3).coerceAtMost(100)
        val res = LastFM.get(
            "user.gettoptracks",
            mapOf("user" to user, "limit" to fetchLimit.toString(), "period" to period, "page" to page)
        ).getOrThrow()
        return parseLastFmTracks(json.parseToJsonElement(res)).shuffled()
    }

    private suspend fun fetchRecentTracks(user: String, limit: Int): List<LastFmTrack> {
        val fetchLimit = (limit * 3).coerceAtMost(100)
        val res = LastFM.get(
            "user.getrecenttracks",
            mapOf("user" to user, "limit" to fetchLimit.toString())
        ).getOrThrow()
        return parseLastFmTracks(json.parseToJsonElement(res))
    }

    private suspend fun fetchSimilarTracks(track: String, artist: String, limit: Int): List<LastFmTrack> {
        val res = LastFM.get(
            "track.getsimilar",
            mapOf("track" to track, "artist" to artist, "limit" to (limit * 4).coerceAtMost(200).toString())
        ).getOrThrow()
        return parseLastFmTracks(json.parseToJsonElement(res)).shuffled()
    }

    private suspend fun fetchSimilarArtistTracks(artist: String, limit: Int): List<LastFmTrack> {
        val res = LastFM.get("artist.getsimilar", mapOf("artist" to artist, "limit" to "20")).getOrThrow()
        val artists = parseLastFmArtists(json.parseToJsonElement(res)).shuffled().take(8)
        val list = mutableListOf<LastFmTrack>()
        for (a in artists) {
            try {
                val page = (1..4).random().toString()
                val r = LastFM.get(
                    "artist.gettoptracks",
                    mapOf("artist" to a, "limit" to (limit / 5).coerceAtLeast(4).toString(), "page" to page)
                ).getOrThrow()
                list.addAll(parseLastFmTracks(json.parseToJsonElement(r)))
            } catch (e: Exception) {
                // Ignore single artist fetch errors
            }
        }
        return list.shuffled()
    }

    private suspend fun fetchTagTracks(tag: String, limit: Int): List<LastFmTrack> {
        val page = (1..8).random().toString()
        val res = LastFM.get(
            "tag.gettoptracks",
            mapOf("tag" to tag, "limit" to (limit * 3).coerceAtMost(100).toString(), "page" to page)
        ).getOrThrow()
        return parseLastFmTracks(json.parseToJsonElement(res)).shuffled()
    }

    private suspend fun fetchMix(user: String, total: Int): List<LastFmTrack> {
        val weighted = mutableListOf<Pair<LastFmTrack, Int>>() // track to weight

        // Bucket A (weight 3) - Recent plays similarity
        try {
            val res = LastFM.get("user.getrecenttracks", mapOf("user" to user, "limit" to "50")).getOrThrow()
            val recent = parseLastFmTracks(json.parseToJsonElement(res))
            val recentSeeds = recent.shuffled().take(6)
            recentSeeds.forEach { weighted.add(it to 3) }

            for (seed in recentSeeds.take(4)) {
                try {
                    val simRes = LastFM.get(
                        "track.getsimilar",
                        mapOf("track" to seed.name, "artist" to seed.artist, "limit" to (total / 6).coerceAtLeast(5).toString())
                    ).getOrThrow()
                    parseLastFmTracks(json.parseToJsonElement(simRes)).forEach { weighted.add(it to 3) }
                } catch (e: Exception) {}
            }
        } catch (e: Exception) {}

        // Bucket B (weight 2) - Top tracks period
        try {
            val period = listOf("1month", "3month", "6month", "12month").random()
            val res = LastFM.get("user.gettoptracks", mapOf("user" to user, "limit" to "30", "period" to period)).getOrThrow()
            parseLastFmTracks(json.parseToJsonElement(res)).forEach { weighted.add(it to 2) }
        } catch (e: Exception) {}

        // Bucket B2 (weight 2) - Similar artists of top artists
        var topArtists = emptyList<String>()
        try {
            val period = listOf("overall", "12month", "6month").random()
            val res = LastFM.get("user.gettopartists", mapOf("user" to user, "limit" to "30", "period" to period)).getOrThrow()
            topArtists = parseLastFmTopArtists(json.parseToJsonElement(res))
        } catch (e: Exception) {}

        for (artist in topArtists.shuffled().take(3)) {
            try {
                val simRes = LastFM.get("artist.getsimilar", mapOf("artist" to artist, "limit" to "12")).getOrThrow()
                val simPool = parseLastFmArtists(json.parseToJsonElement(simRes)).shuffled().take(3)
                for (sa in simPool) {
                    try {
                        val page = (1..4).random().toString()
                        val topTracksRes = LastFM.get(
                            "artist.gettoptracks",
                            mapOf("artist" to sa, "limit" to (total / 12).coerceAtLeast(4).toString(), "page" to page)
                        ).getOrThrow()
                        parseLastFmTracks(json.parseToJsonElement(topTracksRes)).forEach { weighted.add(it to 2) }
                    } catch (e: Exception) {}
                }
            } catch (e: Exception) {}
        }

        // Bucket C (weight 1) - Genre tags discovery
        try {
            val tagsRes = LastFM.get("user.gettoptags", mapOf("user" to user, "limit" to "8")).getOrThrow()
            val tags = parseLastFmTags(json.parseToJsonElement(tagsRes))
            if (tags.isNotEmpty()) {
                val tag = tags.take(5).random()
                val page = (1..8).random().toString()
                val tracksRes = LastFM.get(
                    "tag.gettoptracks",
                    mapOf("tag" to tag, "limit" to (total * 0.4).toInt().coerceAtLeast(5).toString(), "page" to page)
                ).getOrThrow()
                parseLastFmTracks(json.parseToJsonElement(tracksRes)).forEach { weighted.add(it to 1) }
            }
        } catch (e: Exception) {}

        // Deduplicate - keep highest weight
        val deduped = mutableMapOf<String, Pair<LastFmTrack, Int>>()
        for ((track, weight) in weighted) {
            val k = "${track.name}|${track.artist}".lowercase()
            val existing = deduped[k]
            if (existing == null || weight > existing.second) {
                deduped[k] = track to weight
            }
        }

        // Merge, sort by weight tier, shuffle within tier
        val merged = listOf(3, 2, 1).flatMap { w ->
            deduped.values.filter { it.second == w }.map { it.first }.shuffled()
        }

        // Artist diversity - max 3 tracks per artist
        val artistCount = mutableMapOf<String, Int>()
        return merged.filter { track ->
            val ak = track.artist.lowercase()
            val count = artistCount[ak] ?: 0
            artistCount[ak] = count + 1
            count < 3
        }.take(total)
    }

    private suspend fun fetchRecommendations(user: String, total: Int): List<LastFmTrack> {
        // Build taste profile
        val topTrackKeys = mutableSetOf<String>()
        val topTrackSeeds = mutableListOf<LastFmTrack>()
        val recentTrackKeys = mutableSetOf<String>()
        val recentTrackSeeds = mutableListOf<LastFmTrack>()
        val topArtistNames = mutableSetOf<String>()
        val recentArtists = mutableSetOf<String>()
        val topTags = mutableListOf<String>()

        coroutineScope {
            val d1 = async { runCatching { LastFM.get("user.gettoptracks", mapOf("user" to user, "limit" to "50", "period" to "overall")).getOrNull() } }
            val d2 = async { runCatching { LastFM.get("user.getrecenttracks", mapOf("user" to user, "limit" to "50")).getOrNull() } }
            val d3 = async { runCatching { LastFM.get("user.gettopartists", mapOf("user" to user, "limit" to "30", "period" to "overall")).getOrNull() } }
            val d4 = async { runCatching { LastFM.get("user.gettoptags", mapOf("user" to user, "limit" to "15")).getOrNull() } }

            val r1 = d1.await().getOrNull()
            val r2 = d2.await().getOrNull()
            val r3 = d3.await().getOrNull()
            val r4 = d4.await().getOrNull()

            if (r1 != null) {
                parseLastFmTracks(json.parseToJsonElement(r1)).forEach { t ->
                    val k = "${t.name}|${t.artist}".lowercase()
                    topTrackKeys.add(k)
                    topTrackSeeds.add(t)
                }
            }
            if (r2 != null) {
                parseLastFmTracks(json.parseToJsonElement(r2)).forEach { t ->
                    val k = "${t.name}|${t.artist}".lowercase()
                    val ak = t.artist.lowercase()
                    recentTrackKeys.add(k)
                    recentTrackSeeds.add(t)
                    recentArtists.add(ak)
                }
            }
            if (r3 != null) {
                parseLastFmTopArtists(json.parseToJsonElement(r3)).forEach { a ->
                    topArtistNames.add(a.lowercase())
                }
            }
            if (r4 != null) {
                parseLastFmTags(json.parseToJsonElement(r4)).forEach { tag ->
                    topTags.add(tag.lowercase())
                }
            }
        }

        val weighted = java.util.Collections.synchronizedList(mutableListOf<Pair<LastFmTrack, Int>>()) // thread-safe: concurrent launches write to this

        // Parallel candidate pool retrieval
        coroutineScope {
            // Bucket A (weight 4) - similar to recent seeds
            launch {
                val seeds = recentTrackSeeds.shuffled().take(6)
                seeds.forEach { seed ->
                    try {
                        val res = LastFM.get("track.getsimilar", mapOf("track" to seed.name, "artist" to seed.artist, "limit" to "30")).getOrNull()
                        if (res != null) {
                            parseLastFmTracks(json.parseToJsonElement(res)).forEach { weighted.add(it to 4) }
                        }
                    } catch (e: Exception) {}
                }
            }
            // Bucket B (weight 3) - similar to top seeds
            launch {
                val seeds = topTrackSeeds.shuffled().take(5)
                seeds.forEach { seed ->
                    try {
                        val res = LastFM.get("track.getsimilar", mapOf("track" to seed.name, "artist" to seed.artist, "limit" to "25")).getOrNull()
                        if (res != null) {
                            parseLastFmTracks(json.parseToJsonElement(res)).forEach { weighted.add(it to 3) }
                        }
                    } catch (e: Exception) {}
                }
            }
            // Bucket C (weight 2) - top tracks from similar artists
            launch {
                val artists = topArtistNames.shuffled().take(6)
                artists.forEach { art ->
                    try {
                        val resSim = LastFM.get("artist.getsimilar", mapOf("artist" to art, "limit" to "15")).getOrNull()
                        if (resSim != null) {
                            val simList = parseLastFmArtists(json.parseToJsonElement(resSim)).shuffled().take(4)
                            simList.forEach { sa ->
                                try {
                                    val page = (1..4).random().toString()
                                    val topTracksRes = LastFM.get("artist.gettoptracks", mapOf("artist" to sa, "limit" to "10", "page" to page)).getOrNull()
                                    if (topTracksRes != null) {
                                        parseLastFmTracks(json.parseToJsonElement(topTracksRes)).forEach { weighted.add(it to 2) }
                                    }
                                } catch (e: Exception) {}
                            }
                        }
                    } catch (e: Exception) {}
                }
            }
            // Bucket D (weight 1) - genre discoveries
            if (topTags.isNotEmpty()) {
                launch {
                    val tags = topTags.shuffled().take(4)
                    tags.forEach { tag ->
                        try {
                            val page = (1..8).random().toString()
                            val limitVal = (total * 0.35).toInt().coerceAtLeast(5).toString()
                            val res = LastFM.get("tag.gettoptracks", mapOf("tag" to tag, "limit" to limitVal, "page" to page)).getOrNull()
                            if (res != null) {
                                parseLastFmTracks(json.parseToJsonElement(res)).forEach { weighted.add(it to 1) }
                            }
                        } catch (e: Exception) {}
                    }
                }
            }
        }

        // Deduplicate
        val deduped = mutableMapOf<String, Pair<LastFmTrack, Int>>()
        for ((track, weight) in weighted) {
            val k = "${track.name}|${track.artist}".lowercase()
            val existing = deduped[k]
            if (existing == null || weight > existing.second) {
                deduped[k] = track to weight
            }
        }

        // Scoring candidates
        val scored = deduped.values.map { (track, weight) ->
            val score = scoreTrack(track, weight, recentTrackKeys, topArtistNames, recentArtists, topTrackKeys)
            Triple(track, weight, score)
        }

        // Filter and balance
        // Strict pass: score >= 30
        var filtered = scored.filter { it.third >= 30 }
        // Relax if thin
        if (filtered.size < (total * 0.65).toInt()) {
            filtered = scored.filter { it.third >= 10 }
        }
        // Supplement if still thin
        if (filtered.size < (total * 0.5).toInt()) {
            val filteredKeys = filtered.map { "${it.first.name}|${it.first.artist}".lowercase() }.toSet()
            val supplements = scored.filter { it.third != -1 && "${it.first.name}|it.first.artist".lowercase() !in filteredKeys }
                .sortedByDescending { it.third }
            filtered = filtered + supplements
        }

        filtered = filtered.sortedByDescending { it.third }

        // 60/40 balance of familiar (weight >= 3) vs discovery (weight <= 2)
        val familiarTarget = (total * 0.60).toInt()
        val discoveryTarget = (total * 0.40).toInt()
        val familiarPool = filtered.filter { it.second >= 3 }
        val discoveryPool = filtered.filter { it.second <= 2 }

        var balanced = familiarPool.take(familiarTarget) + discoveryPool.take(discoveryTarget)
        if (balanced.size < total) {
            val balancedKeys = balanced.map { "${it.first.name}|${it.first.artist}".lowercase() }.toSet()
            val extras = filtered.filter { "${it.first.name}|${it.first.artist}".lowercase() !in balancedKeys }
            balanced = balanced + extras
        }

        // Artist diversity - max 2 tracks per artist
        val artistCount = mutableMapOf<String, Int>()
        return balanced.map { it.first }.filter { track ->
            val ak = track.artist.lowercase()
            val count = artistCount[ak] ?: 0
            artistCount[ak] = count + 1
            count < 2
        }.take(total)
    }

    private fun scoreTrack(
        track: LastFmTrack,
        bucketWeight: Int,
        recentTrackKeys: Set<String>,
        topArtistNames: Set<String>,
        recentArtists: Set<String>,
        topTrackKeys: Set<String>
    ): Int {
        val trackKey = "${track.name}|${track.artist}".lowercase()
        val artistKey = track.artist.lowercase()

        // Hard reject: recently heard
        if (recentTrackKeys.contains(trackKey)) return -1

        var score = 0

        // Bucket confidence
        when (bucketWeight) {
            4 -> score += 40
            3 -> score += 30
            2 -> score += 20
        }

        // Artist familiarity
        if (topArtistNames.contains(artistKey)) score += 50
        if (recentArtists.contains(artistKey)) score += 30

        // Known favourite penalty
        if (topTrackKeys.contains(trackKey)) score -= 15

        return score.coerceAtLeast(0)
    }

    // --- JSON Parsers ---

    private fun parseLastFmTracks(rootElement: JsonElement): List<LastFmTrack> {
        return try {
            val root = rootElement.jsonObject
            val trackArray = when {
                "toptracks" in root -> root["toptracks"]?.jsonObject?.get("track")
                "recenttracks" in root -> root["recenttracks"]?.jsonObject?.get("track")
                "similartracks" in root -> root["similartracks"]?.jsonObject?.get("track")
                "tracks" in root -> root["tracks"]?.jsonObject?.get("track")
                "results" in root -> root["results"]?.jsonObject?.get("trackmatches")?.jsonObject?.get("track")
                else -> null
            } ?: return emptyList()

            val elements = if (trackArray is JsonArray) {
                trackArray
            } else {
                buildJsonArray { add(trackArray) }
            }

            elements.mapNotNull { element ->
                val obj = element.jsonObject
                val name = obj["name"]?.jsonPrimitive?.content ?: return@mapNotNull null
                val artistElement = obj["artist"]
                val artist = when (artistElement) {
                    is JsonPrimitive -> artistElement.content
                    is JsonObject -> artistElement["name"]?.jsonPrimitive?.content
                        ?: artistElement["#text"]?.jsonPrimitive?.content
                        ?: ""
                    else -> ""
                }

                var imageUrl = ""
                val imageArray = obj["image"] as? JsonArray
                if (imageArray != null) {
                    val sizes = listOf("extralarge", "large", "medium")
                    for (size in sizes) {
                        val imgObj = imageArray.mapNotNull { it.jsonObject }.firstOrNull { it["size"]?.jsonPrimitive?.content == size }
                        val text = imgObj?.get("#text")?.jsonPrimitive?.content
                        if (!text.isNullOrBlank() && !text.contains("2a96cbd8b46e442fc41c2b86b821562f")) {
                            imageUrl = text
                            break
                        }
                    }
                    if (imageUrl.isEmpty()) {
                        val fallbackImg = imageArray.mapNotNull { it.jsonObject }.firstOrNull()?.get("#text")?.jsonPrimitive?.content
                        if (!fallbackImg.isNullOrBlank() && !fallbackImg.contains("2a96cbd8b46e442fc41c2b86b821562f")) {
                            imageUrl = fallbackImg
                        }
                    }
                }

                LastFmTrack(name, artist, imageUrl)
            }
        } catch (e: Exception) {
            Timber.e(e, "Error parsing tracks")
            emptyList()
        }
    }

    private fun parseLastFmArtists(rootElement: JsonElement): List<String> {
        return try {
            val root = rootElement.jsonObject
            val artistArray = root["similarartists"]?.jsonObject?.get("artist")
                ?: root["results"]?.jsonObject?.get("artistmatches")?.jsonObject?.get("artist")
                ?: return emptyList()
            val elements = if (artistArray is JsonArray) {
                artistArray
            } else {
                buildJsonArray { add(artistArray) }
            }
            elements.mapNotNull { it.jsonObject["name"]?.jsonPrimitive?.content }
        } catch (e: Exception) {
            Timber.e(e, "Error parsing artists")
            emptyList()
        }
    }

    private fun parseLastFmTags(rootElement: JsonElement): List<String> {
        return try {
            val root = rootElement.jsonObject
            val tagArray = root["toptags"]?.jsonObject?.get("tag") ?: return emptyList()
            val elements = if (tagArray is JsonArray) {
                tagArray
            } else {
                buildJsonArray { add(tagArray) }
            }
            elements.mapNotNull { it.jsonObject["name"]?.jsonPrimitive?.content }
        } catch (e: Exception) {
            Timber.e(e, "Error parsing tags")
            emptyList()
        }
    }

    private fun parseLastFmTopArtists(rootElement: JsonElement): List<String> {
        return try {
            val root = rootElement.jsonObject
            val artistArray = root["topartists"]?.jsonObject?.get("artist") ?: return emptyList()
            val elements = if (artistArray is JsonArray) {
                artistArray
            } else {
                buildJsonArray { add(artistArray) }
            }
            elements.mapNotNull { it.jsonObject["name"]?.jsonPrimitive?.content }
        } catch (e: Exception) {
            Timber.e(e, "Error parsing top artists")
            emptyList()
        }
    }

    // Dynamic coroutine helper
    private suspend fun <T> coroutineScope(block: suspend kotlinx.coroutines.CoroutineScope.() -> T): T {
        return kotlinx.coroutines.coroutineScope(block)
    }

    fun dismissDailyMixGenSheet() {
        _dailyMixGenError.value = null
        _dailyMixGenSuccess.value = false
        _dailyMixGenGenerating.value = false
        _dailyMixGenProgress.value = null
    }

    fun generateDailyMixLastFm(
        mode: String,
        trackCount: Int,
        timePeriod: String = "overall",
        seedTrackName: String = "",
        seedArtistName: String = "",
        seedArtistInput: String = "",
        tagInput: String = ""
    ) {
        val user = _uiState.value.username
        if (user.isEmpty()) {
            _dailyMixGenError.value = "Link your Last.fm account first in settings."
            return
        }

        viewModelScope.launch {
            _dailyMixGenGenerating.value = true
            _dailyMixGenError.value = null
            _dailyMixGenSuccess.value = false
            _dailyMixGenProgress.value = "Connecting to Last.fm..."

            try {
                // Step 1: Query Last.fm for tracks metadata
                val lastFmTracks = withContext(Dispatchers.IO) {
                    when (mode) {
                        "top" -> fetchTopTracks(user, trackCount, timePeriod)
                        "library" -> fetchTopTracks(user, trackCount, "overall")
                        "recent" -> fetchRecentTracks(user, trackCount)
                        "similar-tracks" -> {
                            if (seedTrackName.isBlank() || seedArtistName.isBlank()) {
                                throw IllegalArgumentException("Enter a seed track and artist")
                            }
                            fetchSimilarTracks(seedTrackName, seedArtistName, trackCount)
                        }
                        "similar-artists" -> {
                            if (seedArtistInput.isBlank()) {
                                throw IllegalArgumentException("Enter a seed artist")
                            }
                            fetchSimilarArtistTracks(seedArtistInput, trackCount)
                        }
                        "tag" -> {
                            if (tagInput.isBlank()) {
                                throw IllegalArgumentException("Enter a genre/tag")
                            }
                            fetchTagTracks(tagInput, trackCount)
                        }
                        "mix" -> fetchMix(user, trackCount)
                        "recommendations" -> fetchRecommendations(user, trackCount)
                        else -> emptyList()
                    }
                }

                if (lastFmTracks.isEmpty()) {
                    throw Exception("No tracks found on Last.fm for your selections.")
                }

                _dailyMixGenProgress.value = "Resolving tracks on YouTube Music (0/${lastFmTracks.size})..."

                // Step 2: Resolve tracks to YouTube Music IDs
                val resolvedSongs = mutableListOf<Song>()
                val semaphore = Semaphore(3) // parallel resolution with concurrency limit
                val deferreds = lastFmTracks.mapIndexed { index, track ->
                    async(Dispatchers.IO) {
                        semaphore.withPermit {
                            val song = resolveTrackToYoutubeSong(track)
                            if (song != null) {
                                synchronized(resolvedSongs) {
                                    resolvedSongs.add(song)
                                    _dailyMixGenProgress.value = "Resolving tracks on YouTube Music (${resolvedSongs.size}/${lastFmTracks.size})..."
                                }
                            }
                        }
                    }
                }
                deferreds.awaitAll()

                if (resolvedSongs.isEmpty()) {
                    throw Exception("Could not find playable matches on YouTube Music.")
                }

                // Step 3: Insert songs into Room database
                _dailyMixGenProgress.value = "Saving tracks to local database..."
                withContext(Dispatchers.IO) {
                    musicRepository.insertYoutubeSongs(resolvedSongs)
                }

                // Step 4: ONLY set Daily Mix songs
                _dailyMixGenProgress.value = "Updating Daily Mix songs..."
                dailyMixStateHolder.setDailyMixSongs(resolvedSongs)
                
                _dailyMixGenSuccess.value = true
                _dailyMixGenProgress.value = "Success! Daily Mix updated."
            } catch (e: Exception) {
                Timber.e(e, "Daily Mix Last.fm generation failed")
                _dailyMixGenError.value = e.localizedMessage ?: "Unknown error"
            } finally {
                _dailyMixGenGenerating.value = false
            }
        }
    }
}
