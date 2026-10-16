package com.saurav.pixelmusic.presentation.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.saurav.pixelmusic.data.model.Playlist
import com.saurav.pixelmusic.data.preferences.PlaylistPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import saurav.shru.pixelmusic.innertube.YouTube
import saurav.shru.pixelmusic.innertube.models.AlbumItem
import saurav.shru.pixelmusic.innertube.models.ArtistItem
import saurav.shru.pixelmusic.innertube.models.PlaylistItem
import saurav.shru.pixelmusic.innertube.models.SongItem
import saurav.shru.pixelmusic.innertube.models.YTItem
import saurav.shru.pixelmusic.innertube.pages.ChartsPage
import saurav.shru.pixelmusic.innertube.pages.ExplorePage
import saurav.shru.pixelmusic.innertube.pages.HomePage
import timber.log.Timber
import javax.inject.Inject

data class ExploreUiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val isContinuationLoading: Boolean = false,
    val homePageSections: List<HomePage.Section> = emptyList(),
    val homePageContinuation: String? = null,
    val newReleaseAlbums: List<AlbumItem> = emptyList(),
    val chartsPage: ChartsPage? = null,
    val error: String? = null,
    val selectedFilter: String = "All",
    val recentMixes: List<Playlist> = emptyList(),

    // ── Mood chips & Moods/Genres catalog ──────────────────────────────
    val selectedMood: String? = null,
    val moodSections: List<HomePage.Section> = emptyList(),
    val isMoodLoading: Boolean = false,
    val moodsAndGenresSections: List<YouTube.MoodsAndGenresSection> = emptyList(),

    // ── Charts & Region selector ───────────────────────────────────────
    val isChartsLoading: Boolean = false,
    val selectedChartCountryName: String = "India",
    val selectedChartCountryCode: String? = "IN",

    // ── Extra Curated Showcase Shelves ─────────────────────────────────
    val videoSpotlightItems: List<SongItem> = emptyList(),
    val podcastPlaylists: List<PlaylistItem> = emptyList()
)

@HiltViewModel
class ExploreViewModel @Inject constructor(
    private val playbackStatsRepository: com.saurav.pixelmusic.data.stats.PlaybackStatsRepository,
    private val userPreferencesRepository: com.saurav.pixelmusic.data.preferences.UserPreferencesRepository,
    private val playlistPreferencesRepository: PlaylistPreferencesRepository,
    private val musicDao: com.saurav.pixelmusic.data.database.MusicDao,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(ExploreUiState())
    val uiState: StateFlow<ExploreUiState> = _uiState.asStateFlow()

    private var stage2Job: kotlinx.coroutines.Job? = null
    private var stage3Job: kotlinx.coroutines.Job? = null

    /** Simple in-memory cache so re-tapping a mood or chart region doesn't refetch. */
    private val moodCache = mutableMapOf<String, List<HomePage.Section>>()
    private val chartsCache = mutableMapOf<String, ChartsPage>()
    private var cachedMoodsAndGenres: List<YouTube.MoodsAndGenresSection>? = null
    private var lastCacheTimestamp: Long = 0L

    private val gson by lazy {
        com.google.gson.GsonBuilder()
            .registerTypeAdapter(YTItem::class.java, YTItemTypeAdapter())
            .create()
    }

    private val cacheFile by lazy {
        java.io.File(context.cacheDir, "explore_cache.json")
    }

    private val explorePrefs by lazy { context.getSharedPreferences("explore_guest_cache", Context.MODE_PRIVATE) }

    init {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                restoreFromCache()
            }
            loadDataInternal(forceRefresh = false)
        }
        viewModelScope.launch {
            playlistPreferencesRepository.userPlaylistsFlow.collect { playlists ->
                val mixes = playlists.filter { it.source == "LASTFM_MIX" }
                    .sortedByDescending { it.lastModified }
                _uiState.update { it.copy(recentMixes = mixes) }
            }
        }
    }

    private fun restoreFromCache() {
        try {
            if (!cacheFile.exists()) return
            val json = cacheFile.readText()
            val cachedData = gson.fromJson(json, ExploreCacheModel::class.java) ?: return
            if (cachedData.cacheVersion != ExploreCacheModel.CURRENT_CACHE_VERSION) return

            val safeSections = cachedData.sections?.filterNotNull().orEmpty()
            val safeAlbums = cachedData.albums?.filterNotNull().orEmpty()
            lastCacheTimestamp = cachedData.timestamp

            if (cachedData.charts != null) {
                chartsCache["IN"] = cachedData.charts
            }

            _uiState.update { current ->
                current.copy(
                    homePageSections = safeSections,
                    newReleaseAlbums = safeAlbums,
                    chartsPage = cachedData.charts,
                    homePageContinuation = cachedData.continuation,
                    isLoading = false
                )
            }
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            Timber.e(e, "Failed to restore explore data from cache")
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    private fun persistToCache(state: ExploreUiState) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val now = System.currentTimeMillis()
                lastCacheTimestamp = now
                val cache = ExploreCacheModel(
                    sections = state.homePageSections,
                    albums = state.newReleaseAlbums,
                    charts = state.chartsPage,
                    continuation = state.homePageContinuation,
                    timestamp = now,
                    cacheVersion = ExploreCacheModel.CURRENT_CACHE_VERSION
                )
                val json = gson.toJson(cache)
                cacheFile.writeText(json)
            } catch (e: Exception) {
                Timber.e(e, "Failed to persist explore data to cache")
            }
        }
    }

    fun loadData(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            loadDataInternal(forceRefresh)
        }
    }

    private fun deduplicateSections(sections: List<HomePage.Section>): List<HomePage.Section> {
        val seen = mutableSetOf<String>()
        return sections.filter { section ->
            val key = section.title.trim().lowercase()
            section.items.isNotEmpty() && key.isNotBlank() && seen.add(key)
        }
    }

    private suspend fun loadDataInternal(forceRefresh: Boolean) {
        stage2Job?.cancel()
        stage3Job?.cancel()

        val currentVal = _uiState.value
        val hasCachedData = currentVal.homePageSections.isNotEmpty() ||
                currentVal.newReleaseAlbums.isNotEmpty() ||
                currentVal.chartsPage != null

        val isCacheFresh = hasCachedData &&
                (System.currentTimeMillis() - lastCacheTimestamp) < 10 * 60 * 1000L &&
                currentVal.homePageSections.size >= 8 &&
                currentVal.moodsAndGenresSections.isNotEmpty()

        if (forceRefresh) {
            _uiState.update { it.copy(isRefreshing = true, error = null) }
        } else if (isCacheFresh) {
            return
        } else {
            // Stale-while-revalidate: keep existing cached UI visible while silently enriching in background
            _uiState.update { it.copy(isLoading = !hasCachedData, error = null) }
        }

        try {
            val history = withContext(Dispatchers.IO) {
                playbackStatsRepository.loadPlaybackHistory(limit = 35)
            }
            val candidateArtistId = withContext(Dispatchers.IO) {
                userPreferencesRepository.subscribedArtistIdsFlow.first().firstOrNull()
            }

            val userActivityQuery = if (history.isNotEmpty()) {
                val artistCounts = history.mapNotNull { it.artist }.groupingBy { it }.eachCount()
                artistCounts.maxByOrNull { it.value }?.key ?: "Bollywood"
            } else {
                "Bollywood"
            }

            val hasLogin = YouTube.hasLoginCookie()

            var home: HomePage? = null
            var explore: ExplorePage? = null
            var charts: ChartsPage? = null
            var newReleasesResult: List<AlbumItem>? = null

            coroutineScope {
                // 1. Fetch Home Page + automatically chain up to 2 continuation pages for rich content depth
                launch(Dispatchers.IO) {
                    try {
                        val h = YouTube.home().getOrNull()
                        home = h
                        if (h != null) {
                            val combinedSections = h.sections.toMutableList()
                            var nextToken = h.continuation

                            _uiState.update { currentState ->
                                currentState.copy(
                                    isLoading = false,
                                    isRefreshing = false,
                                    homePageSections = deduplicateSections(combinedSections + currentState.homePageSections),
                                    homePageContinuation = nextToken
                                )
                            }

                            // Auto-preload up to 2 extra continuation pages so Explore is rich immediately
                            var preloadCount = 0
                            while (nextToken != null && preloadCount < 2) {
                                preloadCount++
                                val contPage = YouTube.home(continuation = nextToken).getOrNull() ?: break
                                if (contPage.sections.isNotEmpty()) {
                                    combinedSections.addAll(contPage.sections)
                                }
                                nextToken = contPage.continuation
                                _uiState.update { currentState ->
                                    currentState.copy(
                                        homePageSections = deduplicateSections(currentState.homePageSections + contPage.sections),
                                        homePageContinuation = nextToken
                                    )
                                }
                            }
                        }
                    } catch (e: Exception) {
                        Timber.e(e, "Failed to load home sections in Stage 1")
                    }
                }

                // 2. Fetch Charts
                launch(Dispatchers.IO) {
                    try {
                        val regionCode = _uiState.value.selectedChartCountryCode
                        val c = YouTube.getChartsPage(regionCode).getOrNull()
                        charts = c
                        if (c != null) {
                            chartsCache[regionCode ?: "GLOBAL"] = c
                            _uiState.update { currentState ->
                                currentState.copy(
                                    isLoading = false,
                                    isRefreshing = false,
                                    chartsPage = c
                                )
                            }
                        }
                    } catch (e: Exception) {
                        Timber.e(e, "Failed to load charts in Stage 1")
                    }
                }

                // 3. Fetch Official New Release Albums
                launch(Dispatchers.IO) {
                    try {
                        val r = YouTube.newReleaseAlbums().getOrNull()
                        newReleasesResult = r
                        if (!r.isNullOrEmpty()) {
                            _uiState.update { currentState ->
                                val merged = (r + currentState.newReleaseAlbums).distinctBy { it.browseId }
                                currentState.copy(
                                    isLoading = false,
                                    isRefreshing = false,
                                    newReleaseAlbums = merged
                                )
                            }
                        }
                    } catch (e: Exception) {
                        Timber.e(e, "Failed to load new release albums in Stage 1")
                    }
                }

                // 4. Fetch Explore Page (prioritizing favorite artists)
                launch(Dispatchers.IO) {
                    try {
                        val exp = YouTube.explore().getOrNull()
                        explore = exp
                        if (exp != null) {
                            val artistRows = try { musicDao.getArtistsByPlayCount() } catch (e: Exception) { emptyList() }

                            val artistsMap: MutableMap<Int, String> = mutableMapOf()
                            val favouriteArtistsMap: MutableMap<Int, String> = mutableMapOf()
                            var favIndex = 0
                            for ((index, row) in artistRows.withIndex()) {
                                artistsMap[index] = row.channelId
                                if (row.isFavourite == 1) {
                                    favouriteArtistsMap[favIndex] = row.channelId
                                    favIndex++
                                }
                            }

                            val sortedReleases = exp.newReleaseAlbums
                                .sortedBy { album ->
                                    val artistIds = album.artists.orEmpty().mapNotNull { it.id }
                                    artistIds.firstNotNullOfOrNull { artistId ->
                                        if (artistId in favouriteArtistsMap.values) {
                                            favouriteArtistsMap.entries.firstOrNull { it.value == artistId }?.key
                                        } else {
                                            artistsMap.entries.firstOrNull { it.value == artistId }?.key
                                        }
                                    } ?: Int.MAX_VALUE
                                }

                            _uiState.update { currentState ->
                                val merged = (sortedReleases + currentState.newReleaseAlbums).distinctBy { it.browseId }
                                currentState.copy(
                                    isLoading = false,
                                    isRefreshing = false,
                                    newReleaseAlbums = merged
                                )
                            }
                        }
                    } catch (e: Exception) {
                        Timber.e(e, "Failed to load explore albums in Stage 1")
                    }
                }

                // 5. Fetch Official Moods & Genres Catalog
                launch(Dispatchers.IO) {
                    try {
                        val mg = YouTube.moodsAndGenres().getOrNull()
                        if (!mg.isNullOrEmpty()) {
                            cachedMoodsAndGenres = mg
                            _uiState.update { currentState ->
                                currentState.copy(moodsAndGenresSections = mg)
                            }
                        }
                    } catch (e: Exception) {
                        Timber.e(e, "Failed to load moods and genres in Stage 1")
                    }
                }
            }

            if (home == null && explore == null && charts == null && newReleasesResult == null) {
                val stillHasCached = _uiState.value.homePageSections.isNotEmpty() ||
                        _uiState.value.newReleaseAlbums.isNotEmpty() ||
                        _uiState.value.chartsPage != null
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isRefreshing = false,
                        error = if (!stillHasCached) "Failed to fetch explore data from YouTube Music. Please check your connection." else null
                    )
                }
                return
            }

            _uiState.update { it.copy(isLoading = false, isRefreshing = false) }

            // Stage 2: Deep personalized & editorial shelves
            stage2Job = viewModelScope.launch(Dispatchers.IO) {
                try {
                    coroutineScope {
                        val likedAlbumsDeferred = if (hasLogin) {
                            async { YouTube.library("FEmusic_liked_albums").getOrNull()?.items?.filterIsInstance<AlbumItem>() ?: emptyList() }
                        } else null

                        val likedArtistsDeferred = if (hasLogin) {
                            async { YouTube.library("FEmusic_liked_artists").getOrNull()?.items?.filterIsInstance<ArtistItem>() ?: emptyList() }
                        } else null

                        val recentActivityDeferred = if (hasLogin) {
                            async { YouTube.libraryRecentActivity().getOrNull()?.items ?: emptyList() }
                        } else null

                        val personalPlaylistsDeferred = if (hasLogin) {
                            async { YouTube.library("FEmusic_liked_playlists").getOrNull()?.items?.filterIsInstance<PlaylistItem>() ?: emptyList() }
                        } else null

                        val communityPlaylistsDeferred = async {
                            YouTube.search(
                                query = "$userActivityQuery playlist",
                                filter = YouTube.SearchFilter.FILTER_COMMUNITY_PLAYLIST
                            ).getOrNull()
                        }

                        val videoSpotlightDeferred = async {
                            YouTube.search(
                                query = "$userActivityQuery official music video",
                                filter = YouTube.SearchFilter.FILTER_VIDEO
                            ).getOrNull()?.items?.filterIsInstance<SongItem>().orEmpty()
                        }

                        val podcastsDeferred = async {
                            YouTube.search(
                                query = "music podcast",
                                filter = YouTube.SearchFilter.FILTER_FEATURED_PLAYLIST
                            ).getOrNull()?.items?.filterIsInstance<PlaylistItem>().orEmpty()
                        }

                        val deepCutsDeferred = async {
                            YouTube.search(
                                query = "$userActivityQuery hits",
                                filter = YouTube.SearchFilter.FILTER_SONG
                            ).getOrNull()?.items?.filterIsInstance<SongItem>().orEmpty()
                        }

                        val cachedArtistBrowseId = explorePrefs.getString("artist_id_${userActivityQuery}", null)
                        val resolvedArtistId = candidateArtistId ?: cachedArtistBrowseId

                        val similarArtistPageDeferred = if (resolvedArtistId != null) {
                            async { YouTube.artist(resolvedArtistId).getOrNull() }
                        } else null

                        val searchArtistPageDeferred = if (resolvedArtistId == null && userActivityQuery != "Bollywood") {
                            async {
                                val searchResult = YouTube.search(userActivityQuery, YouTube.SearchFilter.FILTER_ARTIST).getOrNull()
                                val artistItem = searchResult?.items?.find { it is ArtistItem } as? ArtistItem
                                val id = artistItem?.id
                                if (id != null) {
                                    explorePrefs.edit().putString("artist_id_${userActivityQuery}", id).apply()
                                    val artistPage = YouTube.artist(id).getOrNull()
                                    Pair(artistItem.title, artistPage)
                                } else null
                            }
                        } else null

                        val likedAlbums = likedAlbumsDeferred?.await() ?: emptyList()
                        val likedArtists = likedArtistsDeferred?.await() ?: emptyList()
                        val recentActivityItems = recentActivityDeferred?.await() ?: emptyList()
                        val personalPlaylists = personalPlaylistsDeferred?.await() ?: emptyList()
                        val communityPlaylistsResult = communityPlaylistsDeferred.await()
                        val videoSpotlight = videoSpotlightDeferred.await()
                        val podcasts = podcastsDeferred.await()
                        val deepCuts = deepCutsDeferred.await()

                        var similarSection: HomePage.Section? = null
                        var artistNameForSection = ""

                        if (similarArtistPageDeferred != null) {
                            val artistPage = similarArtistPageDeferred.await()
                            if (artistPage != null) {
                                artistNameForSection = artistPage.artist.title
                                val rawSimilarSection = artistPage.sections.find {
                                    it.title.contains("fans", ignoreCase = true) ||
                                            it.title.contains("similar", ignoreCase = true) ||
                                            it.title.contains("like", ignoreCase = true)
                                }
                                if (rawSimilarSection != null && rawSimilarSection.items.isNotEmpty()) {
                                    similarSection = HomePage.Section(
                                        title = "Similar to $artistNameForSection",
                                        label = "Based on your activity",
                                        thumbnail = null,
                                        endpoint = null,
                                        items = rawSimilarSection.items.filterIsInstance<ArtistItem>()
                                    )
                                }
                            }
                        } else if (searchArtistPageDeferred != null) {
                            val pair = searchArtistPageDeferred.await()
                            if (pair != null) {
                                artistNameForSection = pair.first
                                val artistPage = pair.second
                                if (artistPage != null) {
                                    val rawSimilarSection = artistPage.sections.find {
                                        it.title.contains("fans", ignoreCase = true) ||
                                                it.title.contains("similar", ignoreCase = true) ||
                                                it.title.contains("like", ignoreCase = true)
                                    }
                                    if (rawSimilarSection != null && rawSimilarSection.items.isNotEmpty()) {
                                        similarSection = HomePage.Section(
                                            title = "Similar to $artistNameForSection",
                                            label = "Based on your activity",
                                            thumbnail = null,
                                            endpoint = null,
                                            items = rawSimilarSection.items.filterIsInstance<ArtistItem>()
                                        )
                                    }
                                }
                            }
                        }

                        val communityPlaylists = communityPlaylistsResult?.items?.filterIsInstance<PlaylistItem>() ?: emptyList()

                        _uiState.update { currentState ->
                            val updatedSections = currentState.homePageSections.toMutableList()

                            if (deepCuts.isNotEmpty()) {
                                updatedSections.add(
                                    0,
                                    HomePage.Section(
                                        title = "Trending for $userActivityQuery",
                                        label = "Curated Tracks",
                                        thumbnail = null,
                                        endpoint = null,
                                        items = deepCuts
                                    )
                                )
                            }

                            if (personalPlaylists.isNotEmpty()) {
                                updatedSections.add(
                                    0,
                                    HomePage.Section(
                                        title = "Your Playlists",
                                        label = "From your YouTube Music Account",
                                        thumbnail = null,
                                        endpoint = null,
                                        items = personalPlaylists
                                    )
                                )
                            } else if (communityPlaylists.isNotEmpty()) {
                                updatedSections.add(
                                    HomePage.Section(
                                        title = "Community Mixes • $userActivityQuery",
                                        label = "Curated by listeners",
                                        thumbnail = null,
                                        endpoint = null,
                                        items = communityPlaylists
                                    )
                                )
                            }

                            if (recentActivityItems.isNotEmpty()) {
                                updatedSections.add(
                                    0,
                                    HomePage.Section(
                                        title = "Recently Played (YouTube)",
                                        label = "From your YouTube Music Account",
                                        thumbnail = null,
                                        endpoint = null,
                                        items = recentActivityItems
                                    )
                                )
                            }

                            if (similarSection != null) {
                                updatedSections.add(0, similarSection)
                            }

                            if (likedAlbums.isNotEmpty()) {
                                updatedSections.add(
                                    HomePage.Section(
                                        title = "Your Liked Albums",
                                        label = "From your YouTube Music Account",
                                        thumbnail = null,
                                        endpoint = null,
                                        items = likedAlbums
                                    )
                                )
                            }

                            if (likedArtists.isNotEmpty()) {
                                updatedSections.add(
                                    HomePage.Section(
                                        title = "Your Favorite Artists",
                                        label = "From your YouTube Music Account",
                                        thumbnail = null,
                                        endpoint = null,
                                        items = likedArtists
                                    )
                                )
                            }

                            currentState.copy(
                                homePageSections = deduplicateSections(updatedSections),
                                videoSpotlightItems = if (videoSpotlight.isNotEmpty()) videoSpotlight else currentState.videoSpotlightItems,
                                podcastPlaylists = if (podcasts.isNotEmpty()) podcasts else currentState.podcastPlaylists
                            )
                        }
                    }
                } catch (e: Exception) {
                    Timber.e(e, "Error loading Stage 2 Explore data")
                }
            }

            stage3Job = viewModelScope.launch(Dispatchers.IO) {
                try {
                    kotlinx.coroutines.delay(2500)
                    persistToCache(_uiState.value)
                } catch (e: Exception) {
                    Timber.e(e, "Error persisting Stage 3 Explore data")
                }
            }

        } catch (e: Exception) {
            Timber.e(e, "Error loading Explore screen data")
            _uiState.update {
                it.copy(
                    isLoading = false,
                    isRefreshing = false,
                    error = e.localizedMessage ?: "Unknown error occurred"
                )
            }
        }
    }

    fun loadMore() {
        val currentState = _uiState.value
        val continuation = currentState.homePageContinuation

        if (currentState.isContinuationLoading || continuation == null) {
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isContinuationLoading = true) }

            try {
                val result = withContext(Dispatchers.IO) {
                    YouTube.home(continuation = continuation).getOrNull()
                }

                if (result == null) {
                    _uiState.update { it.copy(isContinuationLoading = false) }
                    return@launch
                }

                if (result.sections.isEmpty()) {
                    _uiState.update {
                        it.copy(
                            isContinuationLoading = false,
                            homePageContinuation = result.continuation
                        )
                    }
                    if (result.continuation != null) {
                        loadMore()
                    }
                    return@launch
                }

                val existingTitles = currentState.homePageSections.map { it.title.trim().lowercase() }.toSet()
                val uniqueNewSections = result.sections.filter { newSection ->
                    newSection.items.isNotEmpty() && newSection.title.trim().lowercase() !in existingTitles
                }

                if (uniqueNewSections.isEmpty()) {
                    _uiState.update {
                        it.copy(
                            isContinuationLoading = false,
                            homePageContinuation = result.continuation
                        )
                    }
                    if (result.continuation != null) {
                        loadMore()
                    }
                    return@launch
                }

                _uiState.update {
                    val newState = it.copy(
                        isContinuationLoading = false,
                        homePageSections = deduplicateSections(it.homePageSections + uniqueNewSections),
                        homePageContinuation = result.continuation
                    )
                    persistToCache(newState)
                    newState
                }
            } catch (e: Exception) {
                Timber.e(e, "Error loading more Explore screen sections")
                _uiState.update { it.copy(isContinuationLoading = false) }
            }
        }
    }

    fun setSelectedFilter(filter: String) {
        _uiState.update { it.copy(selectedFilter = filter, selectedMood = null, moodSections = emptyList()) }
        if (filter == "Charts" && _uiState.value.chartsPage?.sections.isNullOrEmpty()) {
            retryCharts()
        }
    }

    fun setChartCountry(countryName: String, countryCode: String?) {
        val cacheKey = countryCode ?: "GLOBAL"
        val cached = chartsCache[cacheKey]
        if (cached != null && cached.sections.isNotEmpty()) {
            _uiState.update {
                it.copy(
                    selectedChartCountryName = countryName,
                    selectedChartCountryCode = countryCode,
                    chartsPage = cached,
                    isChartsLoading = false
                )
            }
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    selectedChartCountryName = countryName,
                    selectedChartCountryCode = countryCode,
                    isChartsLoading = true
                )
            }
            try {
                val c = withContext(Dispatchers.IO) { YouTube.getChartsPage(countryCode).getOrNull() }
                if (c != null && c.sections.isNotEmpty()) {
                    chartsCache[cacheKey] = c
                }
                _uiState.update {
                    it.copy(
                        isChartsLoading = false,
                        chartsPage = c ?: it.chartsPage
                    )
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to switch chart country to $countryName")
                _uiState.update { it.copy(isChartsLoading = false) }
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    // MOODS
    // ─────────────────────────────────────────────────────────────────────

    fun setSelectedMood(mood: String?) {
        if (mood == null) {
            _uiState.update { it.copy(selectedMood = null, moodSections = emptyList(), isMoodLoading = false) }
            return
        }
        if (_uiState.value.selectedMood == mood) {
            setSelectedMood(null)
            return
        }
        val cached = moodCache[mood]
        if (cached != null && cached.isNotEmpty()) {
            _uiState.update { it.copy(selectedMood = mood, moodSections = cached, isMoodLoading = false) }
            return
        }
        loadMoodInternal(mood)
    }

    private fun loadMoodInternal(mood: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(selectedMood = mood, isMoodLoading = true, moodSections = emptyList()) }
            try {
                val sections = withContext(Dispatchers.IO) {
                    if (mood.equals("Podcasts", ignoreCase = true)) {
                        coroutineScope {
                            val showsDef = async {
                                YouTube.search("podcast", YouTube.SearchFilter.FILTER_FEATURED_PLAYLIST)
                                    .getOrNull()?.items?.filterIsInstance<PlaylistItem>().orEmpty()
                            }
                            val episodesDef = async {
                                YouTube.search("podcast episode", YouTube.SearchFilter.FILTER_VIDEO)
                                    .getOrNull()?.items?.filterIsInstance<SongItem>().orEmpty()
                            }
                            val communityDef = async {
                                YouTube.search("top podcasts", YouTube.SearchFilter.FILTER_COMMUNITY_PLAYLIST)
                                    .getOrNull()?.items?.filterIsInstance<PlaylistItem>().orEmpty()
                            }
                            val shows = showsDef.await()
                            val episodes = episodesDef.await()
                            val comm = communityDef.await()
                            buildList {
                                if (shows.isNotEmpty()) add(HomePage.Section("Featured Podcast Shows", null, null, null, shows))
                                if (episodes.isNotEmpty()) add(HomePage.Section("Latest Podcast Episodes", null, null, null, episodes))
                                if (comm.isNotEmpty()) add(HomePage.Section("Community Audio Series", null, null, null, comm))
                            }
                        }
                    } else {
                        val allMoods = cachedMoodsAndGenres ?: run {
                            val fetched = YouTube.moodsAndGenres().getOrNull()
                            if (!fetched.isNullOrEmpty()) {
                                cachedMoodsAndGenres = fetched
                            }
                            fetched
                        }

                        val normalizedTarget = mood.lowercase()
                        val matchedItem = allMoods?.flatMap { it.items }?.firstOrNull {
                            val t = it.title.lowercase()
                            t == normalizedTarget ||
                                    (normalizedTarget == "energy" && t.startsWith("energ")) ||
                                    (normalizedTarget == "relax" && (t.contains("chill") || t.contains("relax"))) ||
                                    t.contains(normalizedTarget)
                        }

                        val browseResult = if (matchedItem != null && matchedItem.browseId.isNotBlank()) {
                            YouTube.browse(matchedItem.browseId, matchedItem.params).getOrNull()
                        } else null

                        val browsedSections = browseResult?.items?.mapNotNull { shelf ->
                            if (shelf.items.isEmpty()) null
                            else HomePage.Section(
                                title = shelf.title ?: "$mood Highlights",
                                label = null,
                                thumbnail = null,
                                endpoint = null,
                                items = shelf.items
                            )
                        }.orEmpty()

                        if (browsedSections.size >= 2) {
                            browsedSections
                        } else {
                            coroutineScope {
                                val featuredDef = async {
                                    YouTube.search("$mood playlist", YouTube.SearchFilter.FILTER_FEATURED_PLAYLIST)
                                        .getOrNull()?.items?.filterIsInstance<PlaylistItem>().orEmpty()
                                }
                                val songsDef = async {
                                    YouTube.search("$mood hits", YouTube.SearchFilter.FILTER_SONG)
                                        .getOrNull()?.items?.filterIsInstance<SongItem>().orEmpty()
                                }
                                val communityDef = async {
                                    YouTube.search("$mood mix", YouTube.SearchFilter.FILTER_COMMUNITY_PLAYLIST)
                                        .getOrNull()?.items?.filterIsInstance<PlaylistItem>().orEmpty()
                                }
                                val featured = featuredDef.await()
                                val songs = songsDef.await()
                                val community = communityDef.await()
                                buildList {
                                    addAll(browsedSections)
                                    if (songs.isNotEmpty()) add(HomePage.Section("$mood Top Tracks", null, null, null, songs))
                                    if (featured.isNotEmpty()) add(HomePage.Section("$mood Featured Playlists", null, null, null, featured))
                                    if (community.isNotEmpty()) add(HomePage.Section("$mood Community Mixes", null, null, null, community))
                                }
                            }
                        }
                    }
                }
                val cleanSections = deduplicateSections(sections)
                if (cleanSections.isNotEmpty()) {
                    moodCache[mood] = cleanSections
                }
                _uiState.update { it.copy(isMoodLoading = false, moodSections = cleanSections) }
            } catch (e: Exception) {
                Timber.e(e, "Failed to load mood: $mood")
                _uiState.update { it.copy(isMoodLoading = false, moodSections = emptyList()) }
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    // CHARTS RETRY
    // ─────────────────────────────────────────────────────────────────────

    fun retryCharts() {
        if (_uiState.value.isChartsLoading) return
        viewModelScope.launch {
            _uiState.update { it.copy(isChartsLoading = true) }
            try {
                val code = _uiState.value.selectedChartCountryCode
                val c = withContext(Dispatchers.IO) { YouTube.getChartsPage(code).getOrNull() }
                if (c != null && c.sections.isNotEmpty()) {
                    chartsCache[code ?: "GLOBAL"] = c
                }
                _uiState.update {
                    it.copy(
                        isChartsLoading = false,
                        chartsPage = c ?: it.chartsPage
                    )
                }
            } catch (e: Exception) {
                Timber.e(e, "Charts retry failed")
                _uiState.update { it.copy(isChartsLoading = false) }
            }
        }
    }
}

@androidx.annotation.Keep
data class ExploreCacheModel(
    val sections: List<HomePage.Section>? = null,
    val albums: List<AlbumItem>? = null,
    val charts: ChartsPage? = null,
    val continuation: String? = null,
    val timestamp: Long = 0L,
    val cacheVersion: Int = 0
) {
    companion object {
        const val CURRENT_CACHE_VERSION = 2
    }
}

private class YTItemTypeAdapter : com.google.gson.JsonSerializer<YTItem>, com.google.gson.JsonDeserializer<YTItem> {
    override fun serialize(src: YTItem, typeOfSrc: java.lang.reflect.Type, context: com.google.gson.JsonSerializationContext): com.google.gson.JsonElement {
        val obj = context.serialize(src).asJsonObject
        val typeName = when (src) {
            is SongItem -> "SongItem"
            is AlbumItem -> "AlbumItem"
            is PlaylistItem -> "PlaylistItem"
            is ArtistItem -> "ArtistItem"
        }
        obj.addProperty("type", typeName)
        return obj
    }

    override fun deserialize(json: com.google.gson.JsonElement, typeOfT: java.lang.reflect.Type, context: com.google.gson.JsonDeserializationContext): YTItem {
        val obj = json.asJsonObject
        val type = obj.get("type").asString
        val clazz = when (type) {
            "SongItem" -> SongItem::class.java
            "AlbumItem" -> AlbumItem::class.java
            "PlaylistItem" -> PlaylistItem::class.java
            "ArtistItem" -> ArtistItem::class.java
            else -> throw com.google.gson.JsonParseException("Unknown type: $type")
        }
        return context.deserialize(obj, clazz)
    }
}
