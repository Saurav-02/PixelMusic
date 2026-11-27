package com.saurav.pixelmusic.presentation.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.saurav.pixelmusic.data.model.Song
import com.saurav.pixelmusic.data.remote.youtube.toNativeSong
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONArray
import org.json.JSONObject
import timber.log.Timber
import saurav.shru.pixelmusic.innertube.YouTube
import com.saurav.pixelmusic.data.preferences.UserPreferencesRepository
import com.saurav.pixelmusic.data.preferences.QuickPicks
import com.saurav.pixelmusic.data.repository.MusicRepository
import saurav.shru.pixelmusic.innertube.models.filterDuration
import saurav.shru.pixelmusic.innertube.models.filterExplicit
import saurav.shru.pixelmusic.innertube.models.filterVideo
import saurav.shru.pixelmusic.innertube.models.SongItem
import saurav.shru.pixelmusic.innertube.models.AlbumItem
import saurav.shru.pixelmusic.innertube.models.WatchEndpoint
import javax.inject.Inject
import com.saurav.pixelmusic.data.stats.PlaybackStatsRepository
import saurav.shru.pixelmusic.innertube.models.ArtistItem

private const val PREFS_NAME = "quick_picks_cache"
private const val KEY_SONGS = "songs_json"
private const val KEY_CATEGORIES = "categories_json"
private const val KEY_CACHE_TIMESTAMP = "cache_timestamp"
// Cache valid for 4 hours (shorter than before so new releases appear faster)
private const val CACHE_MAX_AGE_MS = 4 * 60 * 60 * 1000L

@HiltViewModel
class QuickPicksViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val musicRepository: MusicRepository,
    private val engagementDao: com.saurav.pixelmusic.data.database.EngagementDao,
    private val playbackStatsRepository: PlaybackStatsRepository
) : ViewModel() {

    private val _quickPicks = MutableStateFlow<List<Song>>(emptyList())
    val quickPicks: StateFlow<List<Song>> = _quickPicks.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _categories = MutableStateFlow<List<String>>(listOf("All"))
    val categories: StateFlow<List<String>> = _categories.asStateFlow()

    private val _selectedCategory = MutableStateFlow("All")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    private val prefs by lazy { context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE) }
    private var loadJob: Job? = null

    init {
        // Immediately populate from cache so the UI shows something on relaunch
        if (!UserPreferencesRepository.isLocalModeActive) {
            loadFromCache()
        }
        viewModelScope.launch {
            userPreferencesRepository.localModeEnabledFlow.collect { localMode ->
                if (localMode) {
                    loadJob?.cancel()
                    _isLoading.value = false
                    _quickPicks.value = emptyList()
                } else {
                    if (_quickPicks.value.isEmpty()) {
                        loadFromCache()
                    }
                    loadQuickPicks(_selectedCategory.value, forceRefresh = false)
                }
            }
        }
        viewModelScope.launch {
            userPreferencesRepository.discoverFlow.collect { _ ->
                if (_selectedCategory.value == "All" && !UserPreferencesRepository.isLocalModeActive) {
                    // Changed to prevent auto-refresh on app launch
                    loadQuickPicks("All", forceRefresh = false)
                }
            }
        }
    }

    fun setCategory(category: String) {
        if (_selectedCategory.value == category && !_isLoading.value) return
        _selectedCategory.value = category
        loadQuickPicks(category, forceRefresh = true) // Fetch new data when category changes
    }

    fun refresh() {
        loadQuickPicks(_selectedCategory.value, forceRefresh = true) // Manual swipe down
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Cache helpers
    // ─────────────────────────────────────────────────────────────────────────

    private fun loadFromCache() {
        try {
            prefs.getString(KEY_SONGS, null) ?: return
            val songsJson = prefs.getString(KEY_SONGS, null) ?: return
            val categoriesJson = prefs.getString(KEY_CATEGORIES, null)

            val songsArray = JSONArray(songsJson)
            val songs = mutableListOf<Song>()
            for (i in 0 until songsArray.length()) {
                songs.add(songFromJson(songsArray.getJSONObject(i)))
            }
            if (songs.isNotEmpty()) _quickPicks.value = songs

            if (categoriesJson != null) {
                val catArray = JSONArray(categoriesJson)
                val cats = mutableListOf<String>()
                for (i in 0 until catArray.length()) cats.add(catArray.getString(i))
                if (cats.isNotEmpty()) _categories.value = cats
            }
        } catch (e: Exception) {
            Timber.tag("QuickPicks").w(e, "Failed to load cache")
        }
    }

    private fun saveToCache(songs: List<Song>, categories: List<String>) {
        try {
            val songsArray = JSONArray()
            songs.forEach { songsArray.put(songToJson(it)) }
            val catArray = JSONArray()
            categories.forEach { catArray.put(it) }
            prefs.edit()
                .putString(KEY_SONGS, songsArray.toString())
                .putString(KEY_CATEGORIES, catArray.toString())
                .putLong(KEY_CACHE_TIMESTAMP, System.currentTimeMillis())
                .apply()
        } catch (e: Exception) {
            Timber.tag("QuickPicks").w(e, "Failed to save cache")
        }
    }

    private fun songToJson(song: Song): JSONObject = JSONObject().apply {
        put("id", song.id)
        put("title", song.title)
        put("artist", song.artist)
        put("artistId", song.artistId)
        put("album", song.album)
        put("albumId", song.albumId)
        put("albumArtist", song.albumArtist ?: "")
        put("path", song.path)
        put("contentUriString", song.contentUriString)
        put("albumArtUriString", song.albumArtUriString ?: "")
        put("duration", song.duration)
        put("genre", song.genre ?: "")
        put("mimeType", song.mimeType ?: "")
        put("bitrate", song.bitrate ?: 0)
        put("sampleRate", song.sampleRate ?: 0)
        put("youtubeId", song.youtubeId ?: "")
        put("albumBrowseId", song.albumBrowseId ?: "")
    }

    private fun songFromJson(obj: JSONObject): Song = Song(
        id = obj.optString("id"),
        title = obj.optString("title"),
        artist = obj.optString("artist", ""),
        artistId = obj.optLong("artistId", 0L),
        album = obj.optString("album", ""),
        albumId = obj.optLong("albumId", 0L),
        albumArtist = obj.optString("albumArtist").takeIf { it.isNotBlank() },
        path = obj.optString("path", ""),
        contentUriString = obj.optString("contentUriString", ""),
        albumArtUriString = obj.optString("albumArtUriString").takeIf { it.isNotBlank() },
        duration = obj.optLong("duration", 0L),
        genre = obj.optString("genre").takeIf { it.isNotBlank() },
        mimeType = obj.optString("mimeType").takeIf { it.isNotBlank() },
        bitrate = obj.optInt("bitrate", 0),
        sampleRate = obj.optInt("sampleRate", 0),
        youtubeId = obj.optString("youtubeId").takeIf { it.isNotBlank() },
        albumBrowseId = obj.optString("albumBrowseId").takeIf { it.isNotBlank() }
    )

    // ─────────────────────────────────────────────────────────────────────────
    // Main load entry point
    // ─────────────────────────────────────────────────────────────────────────

    // Added 'forceRefresh' parameter — manual refresh always pulls fresh personalized YouTube recommendations
    private fun loadQuickPicks(category: String, forceRefresh: Boolean = false) {
        if (UserPreferencesRepository.isLocalModeActive) {
            loadJob?.cancel()
            _isLoading.value = false
            _quickPicks.value = emptyList()
            return
        }
        if (!forceRefresh && _quickPicks.value.isNotEmpty()) {
            return
        }

        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _isLoading.value = true
            try {
                if (category == "All") {
                    val discover = userPreferencesRepository.discoverFlow.first()
                    when (discover) {
                        QuickPicks.DONT_SHOW -> {
                            _quickPicks.value = emptyList()
                        }
                        QuickPicks.QUICK_PICKS, QuickPicks.LAST_LISTEN -> {
                            val songs = loadEnhancedQuickPicks(isManualRefresh = forceRefresh)
                            if (songs.isNotEmpty()) {
                                _quickPicks.value = songs
                                saveToCache(songs, _categories.value)
                            }
                        }
                    }
                } else {
                    loadCategoryQuickPicks(category)
                }
            } catch (e: Exception) {
                Timber.tag("QuickPicks").e(e, "Error loading quick picks")
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun extractCleanYoutubeId(rawId: String?): String? {
        if (rawId.isNullOrBlank()) return null
        val cleaned = rawId.removePrefix("youtube://")
            .removePrefix("youtube_")
            .removePrefix("youtube:")
            .trim()
        return if (cleaned.length == 11 && cleaned.all { it.isLetterOrDigit() || it == '-' || it == '_' }) cleaned else null
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Listening preferences prioritization & 5-bucket recommendation algorithm
    // Prioritizes user's most listened tracks followed by tailored recommendations
    // ─────────────────────────────────────────────────────────────────────────

    private suspend fun loadEnhancedQuickPicks(isManualRefresh: Boolean = false): List<Song> = coroutineScope {
        val pureYtMusicOnly = userPreferencesRepository.pureYtMusicOnlyFlow.first()
        val hideExplicit = userPreferencesRepository.hideExplicitFlow.first()
        val hideVideo = userPreferencesRepository.hideVideoFlow.first()
        val minSongDurationMs = userPreferencesRepository.getMinSongDuration()
        val minDurationSec = minSongDurationMs / 1000
        val shouldFilterVideos = pureYtMusicOnly || hideVideo

        // 1. Load local listening signals concurrently on IO (avoid blocking on slow remote musicHistory)
        val mostPlayedDeferred = async(Dispatchers.IO) {
            runCatching { playbackStatsRepository.loadMostPlayedTracks(limit = 40) }.getOrDefault(emptyList())
        }
        val topEngagementsDeferred = async(Dispatchers.IO) {
            runCatching { engagementDao.getTopPlayedSongs(30) }.getOrDefault(emptyList())
        }
        val localHistoryDeferred = async(Dispatchers.IO) {
            runCatching { playbackStatsRepository.loadPlaybackHistory(limit = 40) }.getOrDefault(emptyList())
        }
        val localFavoritesDeferred = async(Dispatchers.IO) {
            runCatching { musicRepository.getFavoriteSongsOnce(com.saurav.pixelmusic.data.model.StorageFilter.ALL) }.getOrDefault(emptyList())
        }
        val subscribedIdsDeferred = async(Dispatchers.IO) {
            runCatching { userPreferencesRepository.subscribedArtistIdsFlow.first().toList() }.getOrDefault(emptyList())
        }
        // Fetch YouTube musicHistory concurrently with local DB queries under a tight timeout
        val ytHistoryDeferred = async(Dispatchers.IO) {
            withTimeoutOrNull(1_500L) {
                runCatching { YouTube.musicHistory().getOrNull()?.sections?.flatMap { it.songs } ?: emptyList() }.getOrDefault(emptyList())
            } ?: emptyList()
        }

        val mostPlayedHistory = mostPlayedDeferred.await()
        val topEngagements = topEngagementsDeferred.await()
        val localHistoryList = localHistoryDeferred.await()
        val localFavoritesList = localFavoritesDeferred.await()
        val subscribedIds = subscribedIdsDeferred.await()
        val ytHistoryList = ytHistoryDeferred.await()

        // 2. Resolve actual Song objects for user's most listened tracks (indexing by both Room ID and YouTube ID)
        val allLookupIds = (
            topEngagements.map { it.songId } +
            mostPlayedHistory.map { it.songId } +
            localHistoryList.map { it.songId }
        ).distinct()
        val localTopSongs = try {
            if (allLookupIds.isNotEmpty()) musicRepository.getSongsByIdsOnce(allLookupIds) else emptyList()
        } catch (e: Exception) {
            emptyList()
        }
        val localTopSongsById = mutableMapOf<String, Song>()
        localTopSongs.forEach { song ->
            localTopSongsById[song.id] = song
            val yId = song.youtubeId ?: extractCleanYoutubeId(song.contentUriString)
            if (!yId.isNullOrBlank()) {
                localTopSongsById[yId] = song
                localTopSongsById["youtube_$yId"] = song
                localTopSongsById["youtube://$yId"] = song
            }
        }

        val userTopPlayedSongs = mutableListOf<Song>()
        // First add from mostPlayedHistory (covers both local and streamed YouTube songs with rich metadata)
        mostPlayedHistory.forEach { entry ->
            val cleanEntryId = extractCleanYoutubeId(entry.songId)
            val local = localTopSongsById[entry.songId]
                ?: (if (cleanEntryId != null) localTopSongsById[cleanEntryId] ?: localTopSongsById["youtube_$cleanEntryId"] else null)
            if (local != null) {
                userTopPlayedSongs.add(local)
            } else if (cleanEntryId != null && !entry.title.isNullOrBlank()) {
                userTopPlayedSongs.add(
                    Song(
                        id = "youtube_$cleanEntryId",
                        title = entry.title,
                        artist = entry.artist.orEmpty(),
                        artistId = -(17_000_000_000_000L + kotlin.math.abs((entry.artist ?: "").lowercase().hashCode().toLong())),
                        album = "YouTube Music",
                        albumId = -(16_000_000_000_000L + kotlin.math.abs("YouTube Music".hashCode().toLong())),
                        albumArtist = entry.artist,
                        path = "",
                        contentUriString = "youtube://$cleanEntryId",
                        albumArtUriString = entry.thumbnail,
                        duration = 0L,
                        genre = "YouTube",
                        mimeType = "audio/opus",
                        bitrate = 128000,
                        sampleRate = 44100,
                        youtubeId = cleanEntryId
                    )
                )
            }
        }
        // Then add any remaining local top engagements
        topEngagements.forEach { engagement ->
            val local = localTopSongsById[engagement.songId]
            if (local != null && userTopPlayedSongs.none { it.id == local.id }) {
                userTopPlayedSongs.add(local)
            }
        }

        // 3. Extract seed songs prioritized by actual listening frequency
        val seedSongs = mutableListOf<Pair<String, String>>()

        // Priority 1: User's most listened songs
        userTopPlayedSongs.forEach { song ->
            val yId = song.youtubeId ?: extractCleanYoutubeId(song.id) ?: extractCleanYoutubeId(song.contentUriString)
            if (!yId.isNullOrBlank()) {
                seedSongs.add(yId to song.artist)
            }
        }

        // Priority 2: Local playback history (resolving both direct YouTube IDs and Room-mapped songs)
        localHistoryList.forEach { historyEntry ->
            val cleanId = extractCleanYoutubeId(historyEntry.songId)
                ?: localTopSongsById[historyEntry.songId]?.let { it.youtubeId ?: extractCleanYoutubeId(it.contentUriString) }
            if (!cleanId.isNullOrBlank()) {
                seedSongs.add(cleanId to (historyEntry.artist ?: ""))
            }
        }

        // Priority 3: Online YT Music history
        ytHistoryList.forEach { songItem ->
            if (!songItem.id.isNullOrBlank()) {
                seedSongs.add(songItem.id to (songItem.artists.firstOrNull()?.name ?: ""))
            }
        }

        // Priority 4: Favorites
        localFavoritesList.forEach { favoriteSong ->
            val yId = favoriteSong.youtubeId ?: extractCleanYoutubeId(favoriteSong.id) ?: extractCleanYoutubeId(favoriteSong.contentUriString)
            if (!yId.isNullOrBlank()) {
                seedSongs.add(yId to favoriteSong.artist)
            }
        }

        // Fallback seed resolution for local-only tracks without YouTube IDs (capped timeout so refresh stays fast)
        if (seedSongs.distinctBy { it.first }.size < 2) {
            val localSeedCandidates = (userTopPlayedSongs + localFavoritesList)
                .filter { it.title.isNotBlank() && it.artist.isNotBlank() && !it.artist.equals("<unknown>", ignoreCase = true) }
                .distinctBy { "${it.title.lowercase()}|${it.artist.lowercase()}" }
                .let { if (isManualRefresh) it.take(6).shuffled().take(2) else it.take(2) }

            val resolvedLocalSeeds = localSeedCandidates.map { song ->
                async(Dispatchers.IO) {
                    withTimeoutOrNull(2_000L) {
                        try {
                            val query = "${song.title} ${song.artist}".trim()
                            val searchRes = YouTube.search(query, YouTube.SearchFilter.FILTER_SONG).getOrNull()
                            val matchedSong = searchRes?.items?.filterIsInstance<SongItem>()?.firstOrNull()
                            if (matchedSong != null && matchedSong.id.isNotBlank()) {
                                matchedSong.id to song.artist
                            } else null
                        } catch (e: Exception) {
                            null
                        }
                    }
                }
            }.mapNotNull { it.await() }
            seedSongs.addAll(resolvedLocalSeeds)
        }

        // Deduplicate seeds while preserving priority order (top listened tracks first)
        val prioritizedSeedSongs = seedSongs.distinctBy { it.first }
        val seedVideoIdSet = prioritizedSeedSongs.map { it.first }.toSet()

        // Extract seed artists prioritized by listening frequency
        val seedArtistNames = mutableListOf<String>()
        val seedArtistIds = mutableListOf<String>()

        subscribedIds.forEach { seedArtistIds.add(it) }
        userTopPlayedSongs.map { it.artist }.filter { it.isNotBlank() && !it.equals("<unknown>", ignoreCase = true) }.forEach { seedArtistNames.add(it) }
        mostPlayedHistory.mapNotNull { it.artist }.filter { it.isNotBlank() && !it.equals("<unknown>", ignoreCase = true) }.forEach { seedArtistNames.add(it) }
        localHistoryList.mapNotNull { it.artist }.filter { it.isNotBlank() && !it.equals("<unknown>", ignoreCase = true) }.forEach { seedArtistNames.add(it) }
        localFavoritesList.map { it.artist }.filter { it.isNotBlank() && !it.equals("<unknown>", ignoreCase = true) }.forEach { seedArtistNames.add(it) }
        ytHistoryList.flatMap { it.artists }.forEach {
            if (!it.id.isNullOrBlank()) seedArtistIds.add(it.id)
            if (it.name.isNotBlank()) seedArtistNames.add(it.name)
        }

        val prioritizedSeedArtistNames = seedArtistNames.distinct()
        val prioritizedSeedArtistIds = seedArtistIds.distinct()

        // 4. Query recommendation endpoints concurrently using prioritized seeds
        // On manual refresh, rotate through a broader pool of seeds so each refresh fetches fresh related mixes
        val seedPool = if (isManualRefresh && prioritizedSeedSongs.size > 1) {
            prioritizedSeedSongs.take(12).shuffled()
        } else {
            prioritizedSeedSongs
        }

        // Pull fresh personalized recommendations directly from YouTube in parallel
        // (personalized Home + top seed Song Mix Radios + Related tab reusing the first radio's endpoint)
        val requestTimeoutMs = if (isManualRefresh) 3_500L else 5_000L

        // Bucket A + A.2: Song Mix Radios (reusing relatedEndpoint from the primary seed so we never duplicate YouTube.next)
        val songMixesDeferred = seedPool.take(3).mapIndexed { idx, (videoId, _) ->
            async(Dispatchers.IO) {
                withTimeoutOrNull(requestTimeoutMs) {
                    try {
                        val radioResult = YouTube.next(
                            WatchEndpoint(videoId = videoId, playlistId = "RDAMVM$videoId")
                        ).getOrNull()
                        val radioSongs = radioResult?.items
                            ?.filterIsInstance<SongItem>()
                            ?.filter { it.id != videoId }
                            ?.filterExplicit(hideExplicit)
                            ?.filterVideo(shouldFilterVideos)
                            ?.filterDuration(minDurationSec)
                            ?.let { if (isManualRefresh) it.shuffled() else it }
                            ?: emptyList()

                        val relatedEndpoint = radioResult?.relatedEndpoint
                        val relatedSongs = if (idx == 0 && relatedEndpoint != null) {
                            withTimeoutOrNull(1_800L) {
                                YouTube.related(relatedEndpoint).getOrNull()?.songs
                                    ?.filterExplicit(hideExplicit)
                                    ?.filterVideo(shouldFilterVideos)
                                    ?.filterDuration(minDurationSec)
                                    ?.let { if (isManualRefresh) it.shuffled() else it }
                            } ?: emptyList()
                        } else emptyList()

                        radioSongs to relatedSongs
                    } catch (e: Exception) {
                        Timber.tag("QuickPicks").w(e, "Song mix radio fetch failed for videoId: $videoId")
                        emptyList<SongItem>() to emptyList<SongItem>()
                    }
                } ?: (emptyList<SongItem>() to emptyList<SongItem>())
            }
        }

        // Bucket B: Top Subscribed / Listened Artist Radio (1 parallel request with timeout)
        val artistIdPool = if (isManualRefresh && prioritizedSeedArtistIds.size > 1) {
            prioritizedSeedArtistIds.take(6).shuffled()
        } else {
            prioritizedSeedArtistIds
        }
        val artistRadiosDeferred = artistIdPool.take(1).map { artistId ->
            async(Dispatchers.IO) {
                withTimeoutOrNull(requestTimeoutMs) {
                    try {
                        val artistPage = YouTube.artist(artistId).getOrNull()
                        val radioEndpoint = artistPage?.artist?.radioEndpoint
                        if (radioEndpoint != null) {
                            YouTube.next(radioEndpoint).getOrNull()?.items?.filterIsInstance<SongItem>()?.filterExplicit(hideExplicit)?.filterVideo(shouldFilterVideos)?.filterDuration(minDurationSec) ?: emptyList()
                        } else {
                            val songsSection = artistPage?.sections?.find {
                                it.isSongsSection ||
                                it.title.contains("songs", ignoreCase = true) ||
                                it.title.contains("popular", ignoreCase = true)
                            } ?: artistPage?.sections?.firstOrNull { section ->
                                section.items.any { it is SongItem }
                            }
                            songsSection?.items?.filterIsInstance<SongItem>()?.filterExplicit(hideExplicit)?.filterVideo(shouldFilterVideos)?.filterDuration(minDurationSec) ?: emptyList()
                        }
                    } catch (e: Exception) {
                        Timber.tag("QuickPicks").w(e, "Artist radio fetch failed for artistId: $artistId")
                        emptyList()
                    }
                } ?: emptyList()
            }
        }

        // Bucket C: YouTube Music personalized Home page recommendations (directly pulled on every refresh)
        val ytHomeRecommendationsDeferred = async(Dispatchers.IO) {
            withTimeoutOrNull(requestTimeoutMs) {
                try {
                    val homePage = YouTube.home().getOrNull() ?: return@withTimeoutOrNull emptyList<SongItem>()
                    val chipTitles = homePage.chips?.map { it.title } ?: emptyList()
                    if (chipTitles.isNotEmpty()) {
                        _categories.value = listOf("All") + chipTitles
                    }
                    homePage.sections
                        .flatMap { section -> section.items.filterIsInstance<SongItem>() }
                        .filterExplicit(hideExplicit)
                        .filterVideo(shouldFilterVideos)
                        .filterDuration(minDurationSec)
                } catch (e: Exception) {
                    Timber.tag("QuickPicks").w(e, "Personalized home section fetch failed")
                    emptyList()
                }
            } ?: emptyList()
        }

        // Await all parallel YouTube recommendation sources and interleave buckets
        val mixAndRelated = songMixesDeferred.map { it.await() }
        val songMixLists = mixAndRelated.map { it.first }
        val relatedLists = mixAndRelated.map { it.second }.filter { it.isNotEmpty() }
        val artistRadioLists = artistRadiosDeferred.map { it.await() }
        val homeRecSongs = ytHomeRecommendationsDeferred.await().let { if (isManualRefresh) it.shuffled() else it }

        val interleavedDiscoveryItems = mutableListOf<SongItem>()
        val allDiscoveryBuckets: List<List<SongItem>> =
            listOf(homeRecSongs) + songMixLists + relatedLists + artistRadioLists
        val maxBucketLen = allDiscoveryBuckets.maxOfOrNull { it.size } ?: 0
        for (idx in 0 until maxBucketLen) {
            for (bucket in allDiscoveryBuckets) {
                bucket.getOrNull(idx)?.let { interleavedDiscoveryItems.add(it) }
            }
        }

        // 5. Structure and Blend Results:
        // Lead with related/discovery tracks seeded from the user's listening history,
        // lightly weaving in a few familiar favorites, and rotate out previously shown tracks on manual refresh.
        val previousSongKeys = if (isManualRefresh) {
            _quickPicks.value.map { song ->
                song.youtubeId?.takeIf { it.isNotBlank() } ?: "${song.title.lowercase()}|${song.artist.lowercase()}"
            }.toSet()
        } else emptySet()

        val filteredTopPlayed = userTopPlayedSongs
            .filter { song ->
                if (hideExplicit && song.title.contains("explicit", ignoreCase = true)) false
                else if (minDurationSec > 0 && song.duration > 0 && song.duration < minDurationSec * 1000L) false
                else true
            }
            .distinctBy { song ->
                song.youtubeId?.takeIf { it.isNotBlank() } ?: "${song.title.lowercase()}|${song.artist.lowercase()}"
            }

        val discoverySongs = interleavedDiscoveryItems
            .map { it.toNativeSong() }
            .distinctBy { song ->
                song.youtubeId?.takeIf { it.isNotBlank() } ?: "${song.title.lowercase()}|${song.artist.lowercase()}"
            }
            .filterNot { rec ->
                val recYtId = rec.youtubeId
                (recYtId != null && recYtId in seedVideoIdSet && filteredTopPlayed.any { it.youtubeId == recYtId }) ||
                filteredTopPlayed.any { top ->
                    (top.youtubeId != null && top.youtubeId == recYtId) ||
                    (top.title.equals(rec.title, ignoreCase = true) && top.artist.equals(rec.artist, ignoreCase = true))
                }
            }

        val orderedDiscovery = if (isManualRefresh) {
            val (previouslyShown, freshDiscovery) = discoverySongs.partition { song ->
                val key = song.youtubeId?.takeIf { it.isNotBlank() } ?: "${song.title.lowercase()}|${song.artist.lowercase()}"
                key in previousSongKeys
            }
            freshDiscovery.shuffled() + previouslyShown.shuffled()
        } else {
            discoverySongs
        }

        val familiarPool = if (isManualRefresh) {
            val (prevTop, freshTop) = filteredTopPlayed.partition { song ->
                val key = song.youtubeId?.takeIf { it.isNotBlank() } ?: "${song.title.lowercase()}|${song.artist.lowercase()}"
                key in previousSongKeys
            }
            (freshTop.shuffled() + prevTop.shuffled()).take(4)
        } else {
            filteredTopPlayed.shuffled().take(4)
        }

        val prioritizedList = mutableListOf<Song>()
        if (orderedDiscovery.isNotEmpty()) {
            var famIdx = 0
            orderedDiscovery.forEachIndexed { index, discSong ->
                if (prioritizedList.size < 48) {
                    prioritizedList.add(discSong)
                }
                // Sprinkle at most 1 familiar track every 4 discovery tracks
                if ((index + 1) % 4 == 0 && famIdx < familiarPool.size && prioritizedList.size < 48) {
                    prioritizedList.add(familiarPool[famIdx++])
                }
            }
            while (famIdx < familiarPool.size && prioritizedList.size < 48) {
                prioritizedList.add(familiarPool[famIdx++])
            }
        } else {
            prioritizedList.addAll(if (isManualRefresh) filteredTopPlayed.shuffled().take(15) else filteredTopPlayed.take(15))
        }

        // 6. Fallback if still under 15 tracks (e.g. brand new install with no history)
        if (prioritizedList.size < 15) {
            val countryCode = userPreferencesRepository.contentCountryFlow.first().uppercase()
            val fallbackTimeoutMs = 3_000L

            val chartsDeferred = async(Dispatchers.IO) {
                withTimeoutOrNull(fallbackTimeoutMs) {
                    try {
                        val charts = YouTube.getChartsPage(countryCode).getOrNull()
                            ?: YouTube.getChartsPage().getOrNull()
                        charts?.sections
                            ?.flatMap { it.items }
                            ?.filterIsInstance<SongItem>()
                            ?.filterVideo(pureYtMusicOnly)
                            ?.map { it.toNativeSong() } ?: emptyList()
                    } catch (e: Exception) {
                        emptyList()
                    }
                } ?: emptyList()
            }

            val newReleasesDeferred = async(Dispatchers.IO) {
                withTimeoutOrNull(fallbackTimeoutMs) {
                    try {
                        val albums = YouTube.newReleaseAlbums().getOrNull() ?: emptyList()
                        val selectedAlbums = albums.shuffled().take(3)
                        val songsPool = coroutineScope {
                            selectedAlbums.map { album ->
                                async {
                                    try {
                                        YouTube.album(album.browseId).getOrNull()?.songs ?: emptyList()
                                    } catch (e: Exception) {
                                        emptyList()
                                    }
                                }
                            }.flatMap { it.await() }
                        }
                        songsPool.filter { item ->
                            if (pureYtMusicOnly) {
                                val mvType = item.endpoint?.watchEndpointMusicSupportedConfigs
                                    ?.watchEndpointMusicConfig?.musicVideoType
                                mvType == "MUSIC_VIDEO_TYPE_ATV" || mvType == null
                            } else true
                        }
                        .map { it.toNativeSong() }
                    } catch (e: Exception) {
                        emptyList()
                    }
                } ?: emptyList()
            }

            val fallbackCharts = chartsDeferred.await()
            val fallbackNewReleases = newReleasesDeferred.await()

            (fallbackCharts + fallbackNewReleases).forEach { fb ->
                if (prioritizedList.size < 36 && prioritizedList.none {
                    it.youtubeId == fb.youtubeId || (it.title.equals(fb.title, ignoreCase = true) && it.artist.equals(fb.artist, ignoreCase = true))
                }) {
                    prioritizedList.add(fb)
                }
            }
        }

        prioritizedList.take(48)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Category-specific fetch (genre chips from YouTube home)
    // ─────────────────────────────────────────────────────────────────────────

    private suspend fun loadCategoryQuickPicks(category: String) {
        val pureYtMusicOnly = userPreferencesRepository.pureYtMusicOnlyFlow.first()
        val hideExplicit = userPreferencesRepository.hideExplicitFlow.first()
        val hideVideo = userPreferencesRepository.hideVideoFlow.first()
        val minSongDurationMs = userPreferencesRepository.getMinSongDuration()
        val minDurationSec = minSongDurationMs / 1000
        val shouldFilterVideos = pureYtMusicOnly || hideVideo
        val songs = withContext(Dispatchers.IO) {
            try {
                val defaultHome = YouTube.home().getOrNull() ?: return@withContext emptyList<Song>()
                val matchingChip = defaultHome.chips?.firstOrNull {
                    it.title.equals(category, ignoreCase = true)
                }
                val targetHome = if (matchingChip?.endpoint?.params != null) {
                    YouTube.home(params = matchingChip.endpoint.params).getOrNull() ?: defaultHome
                } else defaultHome

                targetHome.sections
                    .filter {
                        !it.title.contains("listen again", ignoreCase = true) &&
                        !it.title.contains("recently played", ignoreCase = true)
                    }
                    .flatMap { it.items.filterIsInstance<SongItem>() }
                    .filterExplicit(hideExplicit)
                    .filterVideo(shouldFilterVideos)
                    .filterDuration(minDurationSec)
                    .distinctBy { it.id }
                    .take(25)
                    .map { it.toNativeSong() }
                    .shuffled()
            } catch (e: Exception) {
                Timber.tag("QuickPicks").e(e, "Category fetch failed: $category")
                emptyList()
            }
        }
        if (songs.isNotEmpty()) {
            _quickPicks.value = songs
        }
    }
}
