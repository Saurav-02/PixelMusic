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
            // If we already have cached data and it's not a manual refresh, do nothing!
            if (!forceRefresh && _quickPicks.value.isNotEmpty()) {
                return@launch
            }

            _isLoading.value = true
            try {
                if (category == "All") {
                    val discover = userPreferencesRepository.discoverFlow.first()
                    when (discover) {
                        QuickPicks.DONT_SHOW -> {
                            _quickPicks.value = emptyList()
                        }
                        QuickPicks.QUICK_PICKS -> {
                            // Try enhanced 5-bucket algorithm first
                            val songs = loadEnhancedQuickPicks(isManualRefresh = forceRefresh)
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
                                val songs = loadEnhancedQuickPicks(isManualRefresh = forceRefresh)
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

        // 1. Load user's top played songs and playback history to accurately prioritize listening preferences
        val mostPlayedHistory = try {
            playbackStatsRepository.loadMostPlayedTracks(limit = 40)
        } catch (e: Exception) {
            emptyList()
        }

        val topEngagements = try {
            engagementDao.getTopPlayedSongs(30)
        } catch (e: Exception) {
            emptyList()
        }

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

        // 2. Resolve actual Song objects for user's most listened tracks
        val localTopSongIds = topEngagements.map { it.songId }
        val localTopSongs = try {
            if (localTopSongIds.isNotEmpty()) musicRepository.getSongsByIdsOnce(localTopSongIds) else emptyList()
        } catch (e: Exception) {
            emptyList()
        }
        val localTopSongsById = localTopSongs.associateBy { it.id }

        val userTopPlayedSongs = mutableListOf<Song>()
        // First add from mostPlayedHistory (covers both local and streamed YouTube songs with rich metadata)
        mostPlayedHistory.forEach { entry ->
            val local = localTopSongsById[entry.songId]
            if (local != null) {
                userTopPlayedSongs.add(local)
            } else {
                val cleanId = extractCleanYoutubeId(entry.songId)
                if (cleanId != null && !entry.title.isNullOrBlank()) {
                    userTopPlayedSongs.add(
                        Song(
                            id = "youtube_$cleanId",
                            title = entry.title,
                            artist = entry.artist.orEmpty(),
                            artistId = -(17_000_000_000_000L + kotlin.math.abs((entry.artist ?: "").lowercase().hashCode().toLong())),
                            album = "YouTube Music",
                            albumId = -(16_000_000_000_000L + kotlin.math.abs("YouTube Music".hashCode().toLong())),
                            albumArtist = entry.artist,
                            path = "",
                            contentUriString = "youtube://$cleanId",
                            albumArtUriString = entry.thumbnail,
                            duration = 0L,
                            genre = "YouTube",
                            mimeType = "audio/opus",
                            bitrate = 128000,
                            sampleRate = 44100,
                            youtubeId = cleanId
                        )
                    )
                }
            }
        }
        // Then add any remaining local top engagements
        localTopSongs.forEach { local ->
            if (userTopPlayedSongs.none { it.id == local.id }) {
                userTopPlayedSongs.add(local)
            }
        }

        // 3. Extract seed songs prioritized by actual listening frequency
        val seedSongs = mutableListOf<Pair<String, String>>()

        // Priority 1: User's most listened songs
        userTopPlayedSongs.forEach { song ->
            val yId = song.youtubeId ?: extractCleanYoutubeId(song.id)
            if (!yId.isNullOrBlank()) {
                seedSongs.add(yId to song.artist)
            }
        }

        // Priority 2: Online YT Music history
        ytHistoryList.forEach { songItem ->
            if (!songItem.id.isNullOrBlank()) {
                seedSongs.add(songItem.id to (songItem.artists.firstOrNull()?.name ?: ""))
            }
        }

        // Priority 3: Local playback history
        localHistoryList.forEach { historyEntry ->
            val cleanId = extractCleanYoutubeId(historyEntry.songId)
            if (cleanId != null) {
                seedSongs.add(cleanId to (historyEntry.artist ?: ""))
            }
        }

        // Priority 4: Favorites
        localFavoritesList.forEach { favoriteSong ->
            val yId = favoriteSong.youtubeId ?: extractCleanYoutubeId(favoriteSong.id)
            if (!yId.isNullOrBlank()) {
                seedSongs.add(yId to favoriteSong.artist)
            }
        }

        // Deduplicate seeds while preserving priority order (top listened tracks first)
        val prioritizedSeedSongs = seedSongs.distinctBy { it.first }

        // Extract seed artists prioritized by listening frequency
        val seedArtistNames = mutableListOf<String>()
        val seedArtistIds = mutableListOf<String>()

        subscribedIds.forEach { seedArtistIds.add(it) }
        userTopPlayedSongs.map { it.artist }.filter { it.isNotBlank() }.forEach { seedArtistNames.add(it) }
        mostPlayedHistory.mapNotNull { it.artist }.filter { it.isNotBlank() }.forEach { seedArtistNames.add(it) }
        localHistoryList.mapNotNull { it.artist }.filter { it.isNotBlank() }.forEach { seedArtistNames.add(it) }
        localFavoritesList.map { it.artist }.filter { it.isNotBlank() }.forEach { seedArtistNames.add(it) }
        ytHistoryList.flatMap { it.artists }.forEach {
            if (!it.id.isNullOrBlank()) seedArtistIds.add(it.id)
            if (it.name.isNotBlank()) seedArtistNames.add(it.name)
        }

        val prioritizedSeedArtistNames = seedArtistNames.distinct()
        val prioritizedSeedArtistIds = seedArtistIds.distinct()

        // 4. Query recommendation endpoints concurrently using prioritized seeds
        // Bucket A: Song Mix Radios (fetch tailored radio mixes using clean videoId and playlistId = RDAMVM$videoId)
        val seedPool = if (isManualRefresh && prioritizedSeedSongs.size > 3) {
            prioritizedSeedSongs.take(8).shuffled()
        } else {
            prioritizedSeedSongs
        }
        val songMixesDeferred = seedPool.take(3).map { (videoId, _) ->
            async(Dispatchers.IO) {
                try {
                    val radioResult = YouTube.next(
                        WatchEndpoint(videoId = videoId, playlistId = "RDAMVM$videoId")
                    ).getOrNull()
                    radioResult?.items?.filterIsInstance<SongItem>()?.filterExplicit(hideExplicit)?.filterVideo(shouldFilterVideos)?.filterDuration(minDurationSec) ?: emptyList()
                } catch (e: Exception) {
                    Timber.tag("QuickPicks").w(e, "Song mix radio fetch failed for videoId: $videoId")
                    emptyList()
                }
            }
        }

        // Bucket A.2: Related Endpoint Recommendations (seed from top listened song)
        val relatedEndpointDeferred = async(Dispatchers.IO) {
            try {
                val primarySeedVideoId = prioritizedSeedSongs.firstOrNull()?.first
                if (!primarySeedVideoId.isNullOrBlank()) {
                    val nextResult = YouTube.next(WatchEndpoint(videoId = primarySeedVideoId, playlistId = "RDAMVM$primarySeedVideoId")).getOrNull()
                    val relatedEndpoint = nextResult?.relatedEndpoint
                    if (relatedEndpoint != null) {
                        val relatedPage = YouTube.related(relatedEndpoint).getOrNull()
                        relatedPage?.songs?.filterExplicit(hideExplicit)?.filterVideo(shouldFilterVideos)?.filterDuration(minDurationSec) ?: emptyList()
                    } else emptyList()
                } else emptyList()
            } catch (e: Exception) {
                Timber.tag("QuickPicks").w(e, "Related endpoint fetch failed")
                emptyList()
            }
        }

        // Bucket B: Similar Artist Radios (fetch similar artist radio for top artists)
        val artistRadiosDeferred = prioritizedSeedArtistIds.take(2).map { artistId ->
            async(Dispatchers.IO) {
                try {
                    val artistPage = YouTube.artist(artistId).getOrNull()
                    val radioEndpoint = artistPage?.artist?.radioEndpoint
                    if (radioEndpoint != null) {
                        YouTube.next(radioEndpoint).getOrNull()?.items?.filterIsInstance<SongItem>()?.filterExplicit(hideExplicit)?.filterVideo(shouldFilterVideos)?.filterDuration(minDurationSec) ?: emptyList()
                    } else {
                        val songsSection = artistPage?.sections?.find {
                            it.title.contains("songs", ignoreCase = true) ||
                            it.title.contains("popular", ignoreCase = true)
                        }
                        val firstSong = songsSection?.items?.filterIsInstance<SongItem>()?.firstOrNull()
                        if (firstSong != null) {
                            YouTube.next(
                                WatchEndpoint(playlistId = "RDAMVM${firstSong.id}", videoId = firstSong.id)
                            ).getOrNull()?.items?.filterIsInstance<SongItem>()?.filterExplicit(hideExplicit)?.filterVideo(shouldFilterVideos)?.filterDuration(minDurationSec) ?: emptyList()
                        } else emptyList()
                    }
                } catch (e: Exception) {
                    Timber.tag("QuickPicks").w(e, "Artist radio fetch failed for artistId: $artistId")
                    emptyList()
                }
            }
        }

        val artistNameRadiosDeferred = if (prioritizedSeedArtistIds.isEmpty() && prioritizedSeedArtistNames.isNotEmpty()) {
            prioritizedSeedArtistNames.take(2).map { artistName ->
                async(Dispatchers.IO) {
                    try {
                        val searchResult = YouTube.search(artistName, YouTube.SearchFilter.FILTER_ARTIST).getOrNull()
                        val artistItem = searchResult?.items?.find { it is ArtistItem } as? ArtistItem
                        val artistId = artistItem?.id
                        if (artistId != null) {
                            val artistPage = YouTube.artist(artistId).getOrNull()
                            val radioEndpoint = artistPage?.artist?.radioEndpoint
                            if (radioEndpoint != null) {
                                YouTube.next(radioEndpoint).getOrNull()?.items?.filterIsInstance<SongItem>()?.filterExplicit(hideExplicit)?.filterVideo(shouldFilterVideos)?.filterDuration(minDurationSec) ?: emptyList()
                            } else {
                                val songsSection = artistPage?.sections?.find {
                                    it.title.contains("songs", ignoreCase = true) ||
                                    it.title.contains("popular", ignoreCase = true)
                                }
                                val firstSong = songsSection?.items?.filterIsInstance<SongItem>()?.firstOrNull()
                                if (firstSong != null) {
                                    YouTube.next(
                                        WatchEndpoint(playlistId = "RDAMVM${firstSong.id}", videoId = firstSong.id)
                                    ).getOrNull()?.items?.filterIsInstance<SongItem>()?.filterExplicit(hideExplicit)?.filterVideo(shouldFilterVideos)?.filterDuration(minDurationSec) ?: emptyList()
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
                    .filterExplicit(hideExplicit)
                    .filterVideo(shouldFilterVideos)
                    .filterDuration(minDurationSec)
            } catch (e: Exception) {
                Timber.tag("QuickPicks").w(e, "Personalized home section fetch failed")
                emptyList()
            }
        }

        // Bucket D: User's online YT Music History
        val ytHistoryDeferred = async(Dispatchers.IO) {
            try {
                YouTube.musicHistory().getOrNull()?.sections?.flatMap { it.songs }?.filterExplicit(hideExplicit)?.filterVideo(shouldFilterVideos)?.filterDuration(minDurationSec) ?: emptyList()
            } catch (e: Exception) {
                emptyList()
            }
        }

        // Await all async requests
        val songMixSongs = songMixesDeferred.flatMap { it.await() }
        val relatedSongs = relatedEndpointDeferred.await()
        val artistRadioSongs = artistRadiosDeferred.flatMap { it.await() }
        val artistNameRadioSongs = artistNameRadiosDeferred.flatMap { it.await() }
        val homeRecSongs = ytHomeRecommendationsDeferred.await()
        val onlineHistorySongs = ytHistoryDeferred.await()

        // 5. Structure and Prioritize Results:
        // Prioritize tracks the user listens to the most at the top, followed by tailored recommendations
        val filteredTopPlayed = userTopPlayedSongs
            .filter { song ->
                if (hideExplicit && song.title.contains("explicit", ignoreCase = true)) false
                else if (minDurationSec > 0 && song.duration > 0 && song.duration < minDurationSec * 1000L) false
                else true
            }
            .distinctBy { song ->
                song.youtubeId?.takeIf { it.isNotBlank() } ?: "${song.title.lowercase()}|${song.artist.lowercase()}"
            }

        val discoverySongs = (songMixSongs + relatedSongs + artistRadioSongs + artistNameRadioSongs + homeRecSongs + onlineHistorySongs)
            .map { it.toNativeSong() }
            .distinctBy { song ->
                song.youtubeId?.takeIf { it.isNotBlank() } ?: "${song.title.lowercase()}|${song.artist.lowercase()}"
            }
            .filterNot { rec ->
                filteredTopPlayed.any { top ->
                    (top.youtubeId != null && top.youtubeId == rec.youtubeId) ||
                    (top.title.equals(rec.title, ignoreCase = true) && top.artist.equals(rec.artist, ignoreCase = true))
                }
            }

        val blendedDiscovery = if (isManualRefresh) discoverySongs.shuffled() else discoverySongs

        // Blend: Keep most listened tracks prioritized at the beginning (up to 15 tracks),
        // followed by discovery tracks seeded directly by those preferences, up to 48 tracks.
        val prioritizedList = mutableListOf<Song>()
        val topPlayedToTake = if (isManualRefresh && filteredTopPlayed.size > 15) {
            filteredTopPlayed.take(25).shuffled().take(15)
        } else {
            filteredTopPlayed.take(15)
        }
        prioritizedList.addAll(topPlayedToTake)
        prioritizedList.addAll(blendedDiscovery.take(48 - prioritizedList.size))

        // 6. Fallback if still under 20 tracks (e.g. brand new install with no history)
        if (prioritizedList.size < 20) {
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
