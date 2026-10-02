package com.saurav.pixelmusic.presentation.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.coroutineScope
import com.saurav.pixelmusic.data.database.ArtistPlayCountRow
import timber.log.Timber
import saurav.shru.pixelmusic.innertube.YouTube
import saurav.shru.pixelmusic.innertube.models.YTItem
import saurav.shru.pixelmusic.innertube.models.SongItem
import saurav.shru.pixelmusic.innertube.models.AlbumItem
import saurav.shru.pixelmusic.innertube.models.PlaylistItem
import saurav.shru.pixelmusic.innertube.models.ArtistItem
import saurav.shru.pixelmusic.innertube.pages.ExplorePage
import saurav.shru.pixelmusic.innertube.pages.HomePage
import saurav.shru.pixelmusic.innertube.pages.ChartsPage
import javax.inject.Inject
import kotlinx.coroutines.async
import com.saurav.pixelmusic.data.model.Playlist
import com.saurav.pixelmusic.data.preferences.PlaylistPreferencesRepository

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

    // ── New for mood chips ──────────────────────────────────────────────
    val selectedMood: String? = null,
    val moodSections: List<HomePage.Section> = emptyList(),
    val isMoodLoading: Boolean = false,

    // ── New for charts retry ───────────────────────────────────────────
    val isChartsLoading: Boolean = false
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

    /** Simple in-memory cache so re-tapping a mood doesn't refetch. */
    private val moodCache = mutableMapOf<String, List<HomePage.Section>>()

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
            val cachedData = gson.fromJson(json, ExploreCacheModel::class.java)

            if (cachedData == null) return

            // Check version if necessary
            if (cachedData.cacheVersion != ExploreCacheModel.CURRENT_CACHE_VERSION) return

            // Safely extract lists to prevent NullPointerExceptions
            val safeSections = cachedData.sections?.filterNotNull().orEmpty()
            val safeAlbums = cachedData.albums?.filterNotNull().orEmpty()
            
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
            timber.log.Timber.e(e, "Failed to restore explore data from cache")
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    private fun persistToCache(state: ExploreUiState) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val cache = ExploreCacheModel(
                    sections = state.homePageSections,
                    albums = state.newReleaseAlbums,
                    charts = state.chartsPage,
                    continuation = state.homePageContinuation,
                    timestamp = System.currentTimeMillis(),
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

    private suspend fun loadDataInternal(forceRefresh: Boolean) {
        stage2Job?.cancel()
        stage3Job?.cancel()

        if (forceRefresh) {
            _uiState.update { it.copy(isRefreshing = true, error = null) }
        } else {
            val hasCachedData = _uiState.value.homePageSections.isNotEmpty() ||
                    _uiState.value.newReleaseAlbums.isNotEmpty() ||
                    _uiState.value.chartsPage != null

            if (hasCachedData) {
                return
            }

            _uiState.update { it.copy(isLoading = !hasCachedData, error = null) }
        }
        try {
            val history = withContext(Dispatchers.IO) {
                playbackStatsRepository.loadPlaybackHistory(limit = 30)
            }
            val candidateArtistId = withContext(Dispatchers.IO) {
                userPreferencesRepository.subscribedArtistIdsFlow.first().firstOrNull()
            }

            val dbArtists = withContext(Dispatchers.IO) {
                try {
                    musicDao.getAllArtistsListRaw()
                } catch (e: Exception) {
                    emptyList()
                }
            }
            val libraryArtistChannelIds = dbArtists
                .mapNotNull { it.channelId }
                .filter { it.isNotBlank() }
                .distinct()

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
                launch(Dispatchers.IO) {
                    try {
                        val h = YouTube.home().getOrNull()
                        home = h
                        if (h != null) {
                            _uiState.update { currentState ->
                                currentState.copy(
                                    isLoading = false,
                                    isRefreshing = false,
                                    homePageSections = h.sections,
                                    homePageContinuation = h.continuation
                                )
                            }
                        }
                    } catch (e: Exception) {
                        Timber.e(e, "Failed to load home sections in Stage 1")
                    }
                }
                launch(Dispatchers.IO) {
                    try {
                        val c = YouTube.getChartsPage().getOrNull()
                        charts = c
                        if (c != null) {
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
            }

            if (home == null && explore == null && charts == null && newReleasesResult == null) {
                val hasCachedData = _uiState.value.homePageSections.isNotEmpty() ||
                        _uiState.value.newReleaseAlbums.isNotEmpty() ||
                        _uiState.value.chartsPage != null
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isRefreshing = false,
                        error = if (!hasCachedData) "Failed to fetch explore data from YouTube Music. Please check your connection." else null
                    )
                }
                return
            }

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
                            val updatedSections = (home?.sections ?: currentState.homePageSections).toMutableList()

                            if (personalPlaylists.isNotEmpty()) {
                                updatedSections.add(0, HomePage.Section(
                                    title = "Your Playlists",
                                    label = "From your YouTube Music Account",
                                    thumbnail = null,
                                    endpoint = null,
                                    items = personalPlaylists
                                ))
                            } else if (communityPlaylists.isNotEmpty()) {
                                updatedSections.add(HomePage.Section(
                                    title = "Community Playlists",
                                    label = "Based on your activity for $userActivityQuery",
                                    thumbnail = null,
                                    endpoint = null,
                                    items = communityPlaylists
                                ))
                            }

                            if (recentActivityItems.isNotEmpty()) {
                                updatedSections.add(0, HomePage.Section(
                                    title = "Recently Played (YouTube)",
                                    label = "From your YouTube Music Account",
                                    thumbnail = null,
                                    endpoint = null,
                                    items = recentActivityItems
                                ))
                            }

                            if (similarSection != null) {
                                updatedSections.add(0, similarSection)
                            }

                            if (likedAlbums.isNotEmpty()) {
                                updatedSections.add(HomePage.Section(
                                    title = "Your Liked Albums",
                                    label = "From your YouTube Music Account",
                                    thumbnail = null,
                                    endpoint = null,
                                    items = likedAlbums
                                ))
                            }

                            if (likedArtists.isNotEmpty()) {
                                updatedSections.add(HomePage.Section(
                                    title = "Your Favorite Artists",
                                    label = "From your YouTube Music Account",
                                    thumbnail = null,
                                    endpoint = null,
                                    items = likedArtists
                                ))
                            }

                            currentState.copy(homePageSections = updatedSections)
                        }
                    }
                } catch (e: Exception) {
                    Timber.e(e, "Error loading Stage 2 Explore data")
                }
            }

            stage3Job = viewModelScope.launch(Dispatchers.IO) {
                try {
                    kotlinx.coroutines.delay(2000)
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

    Timber.d("loadMore() called - isContinuationLoading: ${currentState.isContinuationLoading}, continuation: ${continuation != null}")

    if (currentState.isContinuationLoading) {
        Timber.d("loadMore() skipped - already loading")
        return
    }

    if (continuation == null) {
        Timber.d("loadMore() skipped - no continuation token")
        return
    }

    viewModelScope.launch {
        _uiState.update { it.copy(isContinuationLoading = true) }

        try {
            val result = withContext(Dispatchers.IO) {
                YouTube.home(continuation = continuation).getOrNull()
            }

            Timber.d("loadMore() API result: ${result?.sections?.size ?: 0} sections, hasContinuation: ${result?.continuation != null}")

            if (result == null) {
                _uiState.update { it.copy(isContinuationLoading = false) }
                return@launch
            }

            if (result.sections.isEmpty()) {
                Timber.d("loadMore() - no sections in this page, trying next")
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

            val existingTitles = currentState.homePageSections.map { it.title }.toSet()
            val uniqueNewSections = result.sections.filter { newSection ->
                newSection.title !in existingTitles
            }

            Timber.d("loadMore() - ${uniqueNewSections.size} unique sections after deduplication")

            if (uniqueNewSections.isEmpty()) {
                Timber.d("loadMore() - all sections were duplicates, fetching next page")
                _uiState.update {
                    it.copy(
                        isContinuationLoading = false,
                        homePageContinuation = result.continuation
                    )
                }
                loadMore()
                return@launch
            }

            _uiState.update {
                val newState = it.copy(
                    isContinuationLoading = false,
                    homePageSections = it.homePageSections + uniqueNewSections,
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
        // If user taps Charts and we don't have them yet, fetch them
        if (filter == "Charts" && _uiState.value.chartsPage?.sections.isNullOrEmpty()) {
            retryCharts()
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
        // Toggle off if same mood tapped again
        if (_uiState.value.selectedMood == mood) {
            setSelectedMood(null)
            return
        }
        // Serve from cache if available
        val cached = moodCache[mood]
        if (cached != null) {
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
                    coroutineScope {
                        val songsDeferred = async {
                            YouTube.search(mood, YouTube.SearchFilter.FILTER_SONG).getOrNull()
                        }
                        val playlistsDeferred = async {
                            YouTube.search("$mood playlist", YouTube.SearchFilter.FILTER_COMMUNITY_PLAYLIST).getOrNull()
                        }
                        val albumsDeferred = async {
                            YouTube.search(mood, YouTube.SearchFilter.FILTER_ALBUM).getOrNull()
                        }
                        val artistsDeferred = async {
                            YouTube.search(mood, YouTube.SearchFilter.FILTER_ARTIST).getOrNull()
                        }

                        val songs = songsDeferred.await()?.items?.filterIsInstance<SongItem>().orEmpty()
                        val playlists = playlistsDeferred.await()?.items?.filterIsInstance<PlaylistItem>().orEmpty()
                        val albums = albumsDeferred.await()?.items?.filterIsInstance<AlbumItem>().orEmpty()
                        val artists = artistsDeferred.await()?.items?.filterIsInstance<ArtistItem>().orEmpty()

                        val result = mutableListOf<HomePage.Section>()

                        if (songs.isNotEmpty()) {
                            result.add(HomePage.Section(
                                title = "$mood Songs",
                                label = null,
                                thumbnail = null,
                                endpoint = null,
                                items = songs
                            ))
                        }
                        if (playlists.isNotEmpty()) {
                            result.add(HomePage.Section(
                                title = "$mood Playlists",
                                label = null,
                                thumbnail = null,
                                endpoint = null,
                                items = playlists
                            ))
                        }
                        if (albums.isNotEmpty()) {
                            result.add(HomePage.Section(
                                title = "$mood Albums",
                                label = null,
                                thumbnail = null,
                                endpoint = null,
                                items = albums
                            ))
                        }
                        if (artists.isNotEmpty()) {
                            result.add(HomePage.Section(
                                title = "$mood Artists",
                                label = null,
                                thumbnail = null,
                                endpoint = null,
                                items = artists
                            ))
                        }
                        result
                    }
                }
                moodCache[mood] = sections
                _uiState.update { it.copy(isMoodLoading = false, moodSections = sections) }
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
        if (!_uiState.value.chartsPage?.sections.isNullOrEmpty()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isChartsLoading = true) }
            try {
                val c = withContext(Dispatchers.IO) { YouTube.getChartsPage().getOrNull() }
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
        // FIX: R8 obfuscates class names in release builds. 
        // We must manually map them so the offline cache doesn't break.
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
