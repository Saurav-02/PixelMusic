package com.saurav.pixelmusic.presentation.viewmodel

import android.util.LruCache
import com.saurav.pixelmusic.data.model.Album
import com.saurav.pixelmusic.data.model.Artist
import com.saurav.pixelmusic.data.model.Playlist
import com.saurav.pixelmusic.data.model.SearchFilterType
import com.saurav.pixelmusic.data.model.SearchHistoryItem
import com.saurav.pixelmusic.data.model.SearchResultItem
import com.saurav.pixelmusic.data.remote.youtube.toNativeSong
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.async
import timber.log.Timber
import saurav.shru.pixelmusic.innertube.YouTube
import saurav.shru.pixelmusic.innertube.models.AlbumItem
import saurav.shru.pixelmusic.innertube.models.ArtistItem
import saurav.shru.pixelmusic.innertube.models.PlaylistItem
import saurav.shru.pixelmusic.innertube.models.SongItem
import saurav.shru.pixelmusic.innertube.models.filterVideo
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.FlowPreview

import com.saurav.pixelmusic.data.preferences.UserPreferencesRepository
import com.saurav.pixelmusic.data.preferences.SearchSource
import com.saurav.pixelmusic.data.repository.MusicRepository
import kotlinx.coroutines.flow.first

/**
 * Pure YouTube Music search — all local library search removed.
 * Queries go directly to YouTube Music API; results are cached in LRU.
 */
@Singleton
class SearchStateHolder @Inject constructor(
    @param:dagger.hilt.android.qualifiers.ApplicationContext private val appContext: android.content.Context,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val musicRepository: MusicRepository
) {
    companion object {
        const val SEARCH_DEBOUNCE_MS = 80L
        const val SEARCH_CACHE_SIZE = 100
        val albumIdMap = java.util.concurrent.ConcurrentHashMap<Long, String>()
    }

    private val searchResultCache = LruCache<String, ImmutableList<SearchResultItem>>(SEARCH_CACHE_SIZE)

    private data class SearchRequest(val query: String, val requestId: Long)

    private val _searchResults = MutableStateFlow<ImmutableList<SearchResultItem>>(persistentListOf())
    val searchResults = _searchResults.asStateFlow()

    private val _selectedSearchFilter = MutableStateFlow(SearchFilterType.ALL)
    val selectedSearchFilter = _selectedSearchFilter.asStateFlow()

    private val _searchHistory = MutableStateFlow<ImmutableList<SearchHistoryItem>>(persistentListOf())
    val searchHistory = _searchHistory.asStateFlow()

    private val searchRequests = MutableSharedFlow<SearchRequest>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    private val latestSearchRequestId = AtomicLong(0L)

    private var scope: CoroutineScope? = null
    private var searchJob: Job? = null

    private var lastContinuationToken: String? = null
    private var activeSearchQuery: String? = null
    private var activeFilterType: SearchFilterType = SearchFilterType.ALL
    private var isLoadingMore = false

    private fun getLongestPrefixMatch(query: String): ImmutableList<SearchResultItem>? {
        if (query.isBlank()) return null
        var longestPrefix: String? = null
        var longestCached: ImmutableList<SearchResultItem>? = null
        
        val snapshot = searchResultCache.snapshot()
        for (key in snapshot.keys) {
            if (query.startsWith(key, ignoreCase = true)) {
                if (longestPrefix == null || key.length > longestPrefix.length) {
                    longestPrefix = key
                    longestCached = snapshot[key]
                }
            }
        }
        return longestCached
    }

    fun initialize(scope: CoroutineScope) {
        this.scope = scope
        observeSearchRequests()
    }

    @OptIn(FlowPreview::class)
    private fun observeSearchRequests() {
        searchJob?.cancel()
        searchJob = scope?.launch {
            searchRequests
                .debounce(SEARCH_DEBOUNCE_MS)
                .collectLatest { request ->
                    val query = request.query
                    if (query.isBlank()) {
                        _searchResults.value = persistentListOf()
                        return@collectLatest
                    }

                    val source = userPreferencesRepository.searchSourceFlow.first()
                    if (source == SearchSource.LOCAL) {
                        try {
                            val results = musicRepository.searchAllOnce(query, _selectedSearchFilter.value)
                            if (request.requestId == latestSearchRequestId.get()) {
                                _searchResults.value = results.toImmutableList()
                            }
                        } catch (_: CancellationException) {
                        } catch (e: Exception) {
                            if (request.requestId == latestSearchRequestId.get()) {
                                Timber.e(e, "Local search error: $query")
                            }
                        }
                        return@collectLatest
                    }

                    // Instantly show cache if available, otherwise fallback to longest prefix match
                    val cached = searchResultCache.get(query) ?: getLongestPrefixMatch(query)
                    if (cached != null) {
                        _searchResults.value = cached
                    }

                    try {
                        val results = withContext(Dispatchers.IO) {
                            searchYouTube(query, _selectedSearchFilter.value)
                        }
                        if (request.requestId == latestSearchRequestId.get()) {
                            val immutable = results.toImmutableList()
                            _searchResults.value = immutable
                            searchResultCache.put(query, immutable)

                            // Pre-fetch top song stream URL
                            scope?.launch(Dispatchers.IO) {
                                try {
                                    val topSong = immutable.filterIsInstance<SearchResultItem.SongItem>().firstOrNull()
                                    if (topSong?.song?.youtubeId != null) {
                                        val ytSong = com.saurav.pixelmusic.data.model.youtube.Song(
                                            youtubeId = topSong.song.youtubeId,
                                            title = topSong.song.title,
                                            artist = topSong.song.artist,
                                            thumbnailHref = topSong.song.albumArtUriString ?: ""
                                        )
                                        com.saurav.pixelmusic.data.remote.youtube.YoutubeHelper.getSongPlayerUrl(
                                            context = appContext, song = ytSong, allowLocal = false
                                        )
                                    }
                                } catch (_: Exception) {}
                            }
                        }
                    } catch (_: CancellationException) {
                    } catch (e: Exception) {
                        if (request.requestId == latestSearchRequestId.get()) {
                            Timber.e(e, "YouTube search error: $query")
                        }
                    }
                }
        }
    }

    fun loadMoreSearch() {
        val token = lastContinuationToken
        val query = activeSearchQuery
        val filter = activeFilterType
        if (token == null || query == null || isLoadingMore) return

        isLoadingMore = true
        scope?.launch(Dispatchers.IO) {
            try {
                val result = YouTube.searchContinuation(token).getOrNull()
                if (result != null) {
                    val pureYtMusicOnly = userPreferencesRepository.pureYtMusicOnlyFlow.first()
                    val newItems = mutableListOf<SearchResultItem>()
                    result.items.forEach { item ->
                        when (item) {
                            is SongItem -> {
                                val musicVideoType = item.endpoint?.watchEndpointMusicSupportedConfigs?.watchEndpointMusicConfig?.musicVideoType
                                val isMusicVideo = musicVideoType == "MUSIC_VIDEO_TYPE_OMV" || musicVideoType == "MUSIC_VIDEO_TYPE_UGC"
                                if (!pureYtMusicOnly || !isMusicVideo) {
                                    newItems.add(SearchResultItem.SongItem(item.toNativeSong()))
                                }
                            }
                            is ArtistItem -> newItems.add(SearchResultItem.ArtistItem(
                                Artist(id = ytArtistId(item.title), name = item.title, songCount = 0, imageUrl = item.thumbnail, channelId = item.id)
                            ))
                            is AlbumItem -> {
                                val longId = ytAlbumId(item.title)
                                albumIdMap[longId] = item.browseId
                                newItems.add(SearchResultItem.AlbumItem(
                                    Album(id = longId, title = item.title,
                                        artist = item.artists?.joinToString { it.name }.orEmpty(),
                                        year = item.year ?: 0, dateAdded = System.currentTimeMillis(),
                                        albumArtUriString = item.thumbnail, songCount = 0)
                                ))
                            }
                            is PlaylistItem -> newItems.add(SearchResultItem.PlaylistItem(
                                Playlist(id = item.id, name = item.title, songIds = emptyList(), coverImageUri = item.thumbnail, source = "YOUTUBE")
                            ))
                        }
                    }
                    if (newItems.isNotEmpty()) {
                        val currentList = _searchResults.value
                        val updatedList = (currentList + newItems).toImmutableList()
                        _searchResults.value = updatedList
                        lastContinuationToken = result.continuation
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Error loading more search results")
            } finally {
                isLoadingMore = false
            }
        }
    }

    private suspend fun searchYouTube(query: String, filter: SearchFilterType): List<SearchResultItem> {
        val pureYtMusicOnly = userPreferencesRepository.pureYtMusicOnlyFlow.first()
        val items = mutableListOf<SearchResultItem>()
        
        activeSearchQuery = query
        activeFilterType = filter
        lastContinuationToken = null

        when (filter) {
            SearchFilterType.ALL -> coroutineScope {
                val songsDeferred = async { YouTube.search(query, YouTube.SearchFilter.FILTER_SONG).getOrNull() }
                val artistsDeferred = async { YouTube.search(query, YouTube.SearchFilter.FILTER_ARTIST).getOrNull() }
                val albumsDeferred = async { YouTube.search(query, YouTube.SearchFilter.FILTER_ALBUM).getOrNull() }

                val songsResult = songsDeferred.await()
                val artistsResult = artistsDeferred.await()
                val albumsResult = albumsDeferred.await()

                lastContinuationToken = songsResult?.continuation

                val popularSongs = mutableListOf<SearchResultItem>()
                val mixedItems = mutableListOf<SearchResultItem>()

                val songsList = songsResult?.items?.filterIsInstance<SongItem>()?.filterVideo(pureYtMusicOnly).orEmpty()
                val artistsList = artistsResult?.items?.filterIsInstance<ArtistItem>().orEmpty()
                val albumsList = albumsResult?.items?.filterIsInstance<AlbumItem>().orEmpty()

                // Separate ATV tracks (Popular Songs)
                val (atvSongs, nonAtvSongs) = songsList.partition {
                    val musicVideoType = it.endpoint?.watchEndpointMusicSupportedConfigs?.watchEndpointMusicConfig?.musicVideoType
                    musicVideoType == "MUSIC_VIDEO_TYPE_ATV"
                }

                atvSongs.forEach { popularSongs.add(SearchResultItem.SongItem(it.toNativeSong())) }

                // Mix remaining categories in interleaving order (1 Artist, 1 Album, 1 non-ATV Song)
                val artistIterator = artistsList.iterator()
                val albumIterator = albumsList.iterator()
                val songIterator = nonAtvSongs.iterator()

                while (artistIterator.hasNext() || albumIterator.hasNext() || songIterator.hasNext()) {
                    if (artistIterator.hasNext()) {
                        val artist = artistIterator.next()
                        mixedItems.add(SearchResultItem.ArtistItem(
                            Artist(id = ytArtistId(artist.title), name = artist.title, songCount = 0, imageUrl = artist.thumbnail, channelId = artist.id)
                        ))
                    }
                    if (albumIterator.hasNext()) {
                        val album = albumIterator.next()
                        val longId = ytAlbumId(album.title)
                        albumIdMap[longId] = album.browseId
                        mixedItems.add(SearchResultItem.AlbumItem(
                            Album(id = longId, title = album.title,
                                artist = album.artists?.joinToString { it.name }.orEmpty(),
                                year = album.year ?: 0, dateAdded = System.currentTimeMillis(),
                                albumArtUriString = album.thumbnail, songCount = 0)
                        ))
                    }
                    if (songIterator.hasNext()) {
                        val song = songIterator.next()
                        mixedItems.add(SearchResultItem.SongItem(song.toNativeSong()))
                    }
                }

                items.addAll(popularSongs)
                items.addAll(mixedItems)
            }
            SearchFilterType.SONGS -> {
                val result = YouTube.search(query, YouTube.SearchFilter.FILTER_SONG).getOrNull()
                lastContinuationToken = result?.continuation
                result?.items?.filterIsInstance<SongItem>()?.filterVideo(pureYtMusicOnly)?.forEach { items.add(SearchResultItem.SongItem(it.toNativeSong())) }
            }
            SearchFilterType.ARTISTS -> {
                val result = YouTube.search(query, YouTube.SearchFilter.FILTER_ARTIST).getOrNull()
                lastContinuationToken = result?.continuation
                result?.items?.filterIsInstance<ArtistItem>()?.forEach { a ->
                    items.add(SearchResultItem.ArtistItem(Artist(id = ytArtistId(a.title), name = a.title, songCount = 0, imageUrl = a.thumbnail, channelId = a.id)))
                }
            }
            SearchFilterType.ALBUMS -> {
                val result = YouTube.search(query, YouTube.SearchFilter.FILTER_ALBUM).getOrNull()
                lastContinuationToken = result?.continuation
                result?.items?.filterIsInstance<AlbumItem>()?.forEach { a ->
                    val longId = ytAlbumId(a.title)
                    albumIdMap[longId] = a.browseId
                    items.add(SearchResultItem.AlbumItem(Album(id = longId, title = a.title,
                        artist = a.artists?.joinToString { it.name }.orEmpty(), year = a.year ?: 0,
                        dateAdded = System.currentTimeMillis(), albumArtUriString = a.thumbnail, songCount = 0)))
                }
            }
            SearchFilterType.PLAYLISTS -> {
                val result = YouTube.search(query, YouTube.SearchFilter.FILTER_FEATURED_PLAYLIST).getOrNull()
                lastContinuationToken = result?.continuation
                result?.items?.filterIsInstance<PlaylistItem>()?.forEach { p ->
                    items.add(SearchResultItem.PlaylistItem(Playlist(id = p.id, name = p.title, songIds = emptyList(), coverImageUri = p.thumbnail, source = "YOUTUBE")))
                }
            }
            SearchFilterType.VIDEOS -> {
                if (!pureYtMusicOnly) {
                    val result = YouTube.search(query, YouTube.SearchFilter.FILTER_VIDEO).getOrNull()
                    lastContinuationToken = result?.continuation
                    result?.items?.filterIsInstance<SongItem>()?.forEach { items.add(SearchResultItem.SongItem(it.toNativeSong())) }
                }
            }
        }
        return items
    }

    fun updateSearchFilter(filterType: SearchFilterType) {
        _selectedSearchFilter.value = filterType
    }

    fun performSearch(query: String) {
        val requestId = latestSearchRequestId.incrementAndGet()
        if (query.trim().isBlank()) {
            _searchResults.value = persistentListOf()
        }
        searchRequests.tryEmit(SearchRequest(query.trim(), requestId))
    }

    fun loadSearchHistory(limit: Int = 15) {
        scope?.launch {
            val history = musicRepository.getRecentSearchHistory(limit)
            _searchHistory.value = history.toImmutableList()
        }
    }

    fun onSearchQuerySubmitted(query: String) {
        scope?.launch {
            if (query.isNotBlank()) {
                musicRepository.addSearchHistoryItem(query)
                loadSearchHistory()
            }
        }
    }

    fun deleteSearchHistoryItem(query: String) {
        scope?.launch {
            musicRepository.deleteSearchHistoryItemByQuery(query)
            loadSearchHistory()
        }
    }

    fun clearSearchHistory() {
        scope?.launch {
            musicRepository.clearSearchHistory()
            _searchHistory.value = persistentListOf()
        }
    }

    fun onCleared() {
        searchJob?.cancel()
        scope = null
    }

    private fun ytArtistId(name: String): Long =
        -(17_000_000_000_000L + kotlin.math.abs(name.lowercase().hashCode().toLong()))

    private fun ytAlbumId(name: String): Long =
        -(16_000_000_000_000L + kotlin.math.abs(name.lowercase().hashCode().toLong()))
}
