package com.saurav.pixelmusic.presentation.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.saurav.pixelmusic.data.model.Song
import com.saurav.pixelmusic.data.remote.youtube.toNativeSong
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import timber.log.Timber
import saurav.shru.pixelmusic.innertube.YouTube
import com.saurav.pixelmusic.data.preferences.UserPreferencesRepository
import com.saurav.pixelmusic.data.preferences.QuickPicks
import com.saurav.pixelmusic.data.repository.MusicRepository
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

    init {
        // Immediately populate from cache so the UI shows something on relaunch
        loadFromCache()
        viewModelScope.launch {
            userPreferencesRepository.discoverFlow.collect { _ ->
                if (_selectedCategory.value == "All") {
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

    // Added 'forceRefresh' parameter
    private fun loadQuickPicks(category: String, forceRefresh: Boolean = false) {
        viewModelScope.launch {
            // --- NEW CODE: STOP AUTO-REFRESH ---
            // If we already have cached data and it's not a manual refresh, do nothing!
            if (!forceRefresh && _quickPicks.value.isNotEmpty()) {
                return@launch
            }
            // -----------------------------------

            if (_quickPicks.value.isEmpty()) {
                _isLoading.value = true
            }
            try {
                if (category == "All") {
                    val discover = userPreferencesRepository.discoverFlow.first()
                    when (discover) {
                        QuickPicks.DONT_SHOW -> {
                            _quickPicks.value = emptyList()
                        }
                        QuickPicks.QUICK_PICKS -> {
                            // Try enhanced 5-bucket algorithm first
                            val songs = loadEnhancedQuickPicks()
                            if (songs.isNotEmpty()) {
                                _quickPicks.value = songs
                                saveToCache(songs, _categories.value)
                            }
                        }
                        QuickPicks.LAST_LISTEN -> {
                            val lastPlayed = withContext(Dispatchers.IO) {
                                musicRepository.getLastPlayedSong()
                            }
                            val related = if (lastPlayed != null) {
                                withContext(Dispatchers.IO) {
                                    val id = lastPlayed.id.toLongOrNull()
                                    if (id != null) musicRepository.getRelatedSongs(id, 50) else emptyList()
                                }
                            } else emptyList()

                            if (related.isNotEmpty()) {
                                val songs = related.shuffled().take(20)
                                _quickPicks.value = songs
                                saveToCache(songs, _categories.value)
                            } else {
                                val songs = loadEnhancedQuickPicks()
                                if (songs.isNotEmpty()) {
                                    _quickPicks.value = songs
                                    saveToCache(songs, _categories.value)
                                }
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

    // ─────────────────────────────────────────────────────────────────────────
    // 5-bucket parallel algorithm — all buckets fetched concurrently
    // Final output: flat shuffled list of ~20 songs
    // ─────────────────────────────────────────────────────────────────────────

    private suspend fun loadEnhancedQuickPicks(): List<Song> = coroutineScope {
        val pureYtMusicOnly = userPreferencesRepository.pureYtMusicOnlyFlow.first()

        // 1. Gather historical seeds from user's local/online history and favorites
        val localHistoryList = try {
            playbackStatsRepository.loadPlaybackHistory(limit = 40)
        } catch (e: Exception) {
            emptyList()
        }

        val localFavoritesList = try {
            musicRepository.getFavoriteSongsOnce(com.saurav.pixelmusic.data.model.StorageFilter.ALL)
        } catch (e: Exception) {
            emptyList()
        }

        val ytHistoryList = try {
            YouTube.musicHistory().getOrNull()?.sections?.flatMap { it.songs } ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }

        val subscribedIds = userPreferencesRepository.subscribedArtistIdsFlow.first().toList()

        // Extract seed songs
        val seedSongs = mutableListOf<Pair<String, String>>()
        
        // Add from online YT Music history
        ytHistoryList.forEach { songItem ->
            if (!songItem.id.isNullOrBlank()) {
                seedSongs.add(songItem.id to (songItem.artists.firstOrNull()?.name ?: ""))
            }
        }
        
        // Add from local playback history
        localHistoryList.forEach { historyEntry ->
            if (!historyEntry.songId.isNullOrBlank() && (historyEntry.songId.startsWith("youtube:") || historyEntry.songId.length == 11)) {
                val cleanId = historyEntry.songId.removePrefix("youtube:")
                if (cleanId.isNotBlank()) {
                    seedSongs.add(cleanId to (historyEntry.artist ?: ""))
                }
            }
        }
        
        // Add from local favorites
        localFavoritesList.forEach { favoriteSong ->
            val yId = favoriteSong.youtubeId
            if (!yId.isNullOrBlank()) {
                seedSongs.add(yId to favoriteSong.artist)
            }
        }

        // Deduplicate seeds and select randomly for freshness on every load/refresh
        val uniqueSeedSongs = seedSongs.distinctBy { it.first }.shuffled()

        // Extract seed artists
        val seedArtistNames = mutableListOf<String>()
        val seedArtistIds = mutableListOf<String>()

        subscribedIds.forEach { seedArtistIds.add(it) }
        localHistoryList.mapNotNull { it.artist }.filter { it.isNotBlank() }.forEach { seedArtistNames.add(it) }
        localFavoritesList.map { it.artist }.filter { it.isNotBlank() }.forEach { seedArtistNames.add(it) }
        ytHistoryList.flatMap { it.artists }.forEach {
            if (!it.id.isNullOrBlank()) seedArtistIds.add(it.id)
            if (it.name.isNotBlank()) seedArtistNames.add(it.name)
        }

        val uniqueSeedArtistNames = seedArtistNames.distinct().shuffled()
        val uniqueSeedArtistIds = seedArtistIds.distinct().shuffled()

        // 2. Query different recommendation endpoints concurrently
        // Bucket A: Song Mix Radios (fetch related radio mix for up to 3 distinct seed songs)
        val songMixesDeferred = uniqueSeedSongs.take(3).map { (videoId, _) ->
            async(Dispatchers.IO) {
                try {
                    val radioResult = YouTube.next(
                        WatchEndpoint(playlistId = "RDAMVM$videoId", videoId = videoId)
                    ).getOrNull()
                    radioResult?.items?.filterIsInstance<SongItem>()?.filterVideo(pureYtMusicOnly) ?: emptyList()
                } catch (e: Exception) {
                    Timber.tag("QuickPicks").w(e, "Song mix radio fetch failed for videoId: $videoId")
                    emptyList()
                }
            }
        }

        // Bucket B: Similar Artist Radios (fetch similar artist radio for up to 2 subscribed/frequent artists)
        val artistRadiosDeferred = uniqueSeedArtistIds.take(2).map { artistId ->
            async(Dispatchers.IO) {
                try {
                    val artistPage = YouTube.artist(artistId).getOrNull()
                    val radioEndpoint = artistPage?.artist?.radioEndpoint
                    if (radioEndpoint != null) {
                        YouTube.next(radioEndpoint).getOrNull()?.items?.filterIsInstance<SongItem>()?.filterVideo(pureYtMusicOnly) ?: emptyList()
                    } else {
                        // Fallback to top song's mix radio
                        val songsSection = artistPage?.sections?.find {
                            it.title.contains("songs", ignoreCase = true) ||
                            it.title.contains("popular", ignoreCase = true)
                        }
                        val firstSong = songsSection?.items?.filterIsInstance<SongItem>()?.firstOrNull()
                        if (firstSong != null) {
                            YouTube.next(
                                WatchEndpoint(playlistId = "RDAMVM${firstSong.id}", videoId = firstSong.id)
                            ).getOrNull()?.items?.filterIsInstance<SongItem>()?.filterVideo(pureYtMusicOnly) ?: emptyList()
                        } else emptyList()
                    }
                } catch (e: Exception) {
                    Timber.tag("QuickPicks").w(e, "Artist radio fetch failed for artistId: $artistId")
                    emptyList()
                }
            }
        }

        // If artist IDs are not available but we have artist names, search and fetch their radio
        val artistNameRadiosDeferred = if (uniqueSeedArtistIds.isEmpty() && uniqueSeedArtistNames.isNotEmpty()) {
            uniqueSeedArtistNames.take(2).map { artistName ->
                async(Dispatchers.IO) {
                    try {
                        val searchResult = YouTube.search(artistName, YouTube.SearchFilter.FILTER_ARTIST).getOrNull()
                        val artistItem = searchResult?.items?.find { it is ArtistItem } as? ArtistItem
                        val artistId = artistItem?.id
                        if (artistId != null) {
                            val artistPage = YouTube.artist(artistId).getOrNull()
                            val radioEndpoint = artistPage?.artist?.radioEndpoint
                            if (radioEndpoint != null) {
                                YouTube.next(radioEndpoint).getOrNull()?.items?.filterIsInstance<SongItem>()?.filterVideo(pureYtMusicOnly) ?: emptyList()
                            } else {
                                val songsSection = artistPage?.sections?.find {
                                    it.title.contains("songs", ignoreCase = true) ||
                                    it.title.contains("popular", ignoreCase = true)
                                }
                                val firstSong = songsSection?.items?.filterIsInstance<SongItem>()?.firstOrNull()
                                if (firstSong != null) {
                                    YouTube.next(
                                        WatchEndpoint(playlistId = "RDAMVM${firstSong.id}", videoId = firstSong.id)
                                    ).getOrNull()?.items?.filterIsInstance<SongItem>()?.filterVideo(pureYtMusicOnly) ?: emptyList()
                                } else emptyList()
                            }
                        } else emptyList()
                    } catch (e: Exception) {
                        Timber.tag("QuickPicks").w(e, "Artist name search/radio failed for: $artistName")
                        emptyList()
                    }
                }
            }
        } else emptyList()

        // Bucket C: YouTube Music personalized Home page sections
        val ytHomeRecommendationsDeferred = async(Dispatchers.IO) {
            try {
                val homePage = YouTube.home().getOrNull() ?: return@async emptyList<SongItem>()
                val chipTitles = homePage.chips?.map { it.title } ?: emptyList()
                if (chipTitles.isNotEmpty()) {
                    _categories.value = listOf("All") + chipTitles
                }
                homePage.sections
                    .flatMap { section -> section.items.filterIsInstance<SongItem>() }
                    .filterVideo(pureYtMusicOnly)
            } catch (e: Exception) {
                Timber.tag("QuickPicks").w(e, "Personalized home section fetch failed")
                emptyList()
            }
        }

        // Bucket D: User's online YT Music History
        val ytHistoryDeferred = async(Dispatchers.IO) {
            try {
                YouTube.musicHistory().getOrNull()?.sections?.flatMap { it.songs }?.filterVideo(pureYtMusicOnly) ?: emptyList()
            } catch (e: Exception) {
                emptyList()
            }
        }

        // Bucket E: Local Top Played/Popular Songs
        val localPopularDeferred = async(Dispatchers.IO) {
            try {
                val topEngagements = engagementDao.getTopPlayedSongs(15)
                if (topEngagements.isNotEmpty()) {
                    val songIds = topEngagements.map { it.songId }
                    musicRepository.getSongsByIdsOnce(songIds)
                } else {
                    musicRepository.getRandomSongs(10)
                }
            } catch (e: Exception) {
                emptyList()
            }
        }

        // Await all async requests
        val songMixSongs = songMixesDeferred.flatMap { it.await() }
        val artistRadioSongs = artistRadiosDeferred.flatMap { it.await() }
        val artistNameRadioSongs = artistNameRadiosDeferred.flatMap { it.await() }
        val homeRecSongs = ytHomeRecommendationsDeferred.await()
        val onlineHistorySongs = ytHistoryDeferred.await()
        val localPopularSongs = localPopularDeferred.await()

        // 3. Blend, map, and de-duplicate
        val combinedCandidates = mutableListOf<Song>()
        (songMixSongs + artistRadioSongs + artistNameRadioSongs + homeRecSongs + onlineHistorySongs)
            .map { it.toNativeSong() }
            .let { combinedCandidates.addAll(it) }
        combinedCandidates.addAll(localPopularSongs)

        var deduplicated = combinedCandidates.distinctBy { song ->
            song.youtubeId?.takeIf { it.isNotBlank() } ?: "${song.title.lowercase()}|${song.artist.lowercase()}"
        }

        // 4. Fallback: If not enough personalized items (e.g. fresh install/no history), fetch location-based trending charts/releases
        if (deduplicated.size < 20) {
            val countryCode = userPreferencesRepository.contentCountryFlow.first().uppercase()
            
            val chartsDeferred = async(Dispatchers.IO) {
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
            }

            val newReleasesDeferred = async(Dispatchers.IO) {
                try {
                    val albums = YouTube.newReleaseAlbums().getOrNull() ?: emptyList()
                    val selectedAlbums = albums.shuffled().take(8)
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
            }

            val fallbackCharts = chartsDeferred.await()
            val fallbackNewReleases = newReleasesDeferred.await()

            deduplicated = (deduplicated + fallbackCharts + fallbackNewReleases)
                .distinctBy { song ->
                    song.youtubeId?.takeIf { it.isNotBlank() } ?: "${song.title.lowercase()}|${song.artist.lowercase()}"
                }
        }

        // Shuffle completely to make it dynamic on every view/refresh
        deduplicated.shuffled().take(20)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Category-specific fetch (genre chips from YouTube home)
    // ─────────────────────────────────────────────────────────────────────────

    private suspend fun loadCategoryQuickPicks(category: String) {
        val pureYtMusicOnly = userPreferencesRepository.pureYtMusicOnlyFlow.first()
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
                    .filterVideo(pureYtMusicOnly)
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
