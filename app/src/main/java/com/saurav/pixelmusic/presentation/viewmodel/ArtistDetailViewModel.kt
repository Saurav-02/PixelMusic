package com.saurav.pixelmusic.presentation.viewmodel

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.Immutable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.saurav.pixelmusic.R
import com.saurav.pixelmusic.data.model.Artist
import com.saurav.pixelmusic.data.model.Song
import com.saurav.pixelmusic.data.repository.ArtistImageRepository
import com.saurav.pixelmusic.data.repository.MusicRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.saurav.pixelmusic.data.preferences.UserPreferencesRepository
import com.saurav.pixelmusic.data.remote.youtube.toNativeSong
import saurav.shru.pixelmusic.innertube.YouTube as InnerTubeYouTube
import saurav.shru.pixelmusic.innertube.models.SongItem
import saurav.shru.pixelmusic.innertube.models.AlbumItem
import saurav.shru.pixelmusic.innertube.models.ArtistItem
import saurav.shru.pixelmusic.innertube.models.BrowseEndpoint
import saurav.shru.pixelmusic.innertube.pages.ArtistPage
import saurav.shru.pixelmusic.innertube.pages.SearchResult
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import javax.inject.Inject

/**
 * Holds the full UI state for ArtistDetailScreen.
 *
 * [effectiveImageUrl] is the resolved image to display (custom takes priority over Deezer).
 * It is updated after artist data loads and again whenever the user changes the custom image.
 */
data class ArtistDetailUiState(
    val artist: Artist? = null,
    val songs: List<Song> = emptyList(),
    val popularSongs: List<Song> = emptyList(),
    val albumSections: List<ArtistAlbumSection> = emptyList(),
    val singlesAndEPs: List<ArtistAlbumSection> = emptyList(),
    val effectiveImageUrl: String? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val isOnlineArtist: Boolean = false,
    val artistDescription: String? = null,
    val subscriberCount: String? = null,
    val browseId: String? = null,
    val albumsMoreEndpoint: BrowseEndpoint? = null,
    val singlesMoreEndpoint: BrowseEndpoint? = null,
    val songsMoreEndpoint: BrowseEndpoint? = null,
    val allItems: List<ArtistAlbumSection> = emptyList(),
    val isAllItemsLoading: Boolean = false,
    val allItemsContinuation: String? = null,
    val allItemsError: String? = null,
    val popularSongsAll: List<Song> = emptyList(),
    val isPopularSongsAllLoading: Boolean = false,
    val popularSongsAllContinuation: String? = null,
    val popularSongsAllError: String? = null
)

enum class ArtistSectionType { ALBUM, SINGLE_EP, SONGS }

@Immutable
data class ArtistAlbumSection(
    val albumId: Long,
    val title: String,
    val year: Int?,
    val albumArtUriString: String?,
    val songs: List<Song>,
    val browseId: String? = null,
    val sectionType: ArtistSectionType = ArtistSectionType.ALBUM
)

@HiltViewModel
class ArtistDetailViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val musicRepository: MusicRepository,
    private val artistImageRepository: ArtistImageRepository,
    val themeStateHolder: ThemeStateHolder,
    val userPreferencesRepository: UserPreferencesRepository,
    private val savedStateHandle: SavedStateHandle,
    private val musicDao: com.saurav.pixelmusic.data.database.MusicDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(ArtistDetailUiState())
    val uiState: StateFlow<ArtistDetailUiState> = _uiState.asStateFlow()

    private val _artistColorScheme = MutableStateFlow<ColorSchemePair?>(null)
    val artistColorScheme: StateFlow<ColorSchemePair?> = _artistColorScheme.asStateFlow()

    val isSubscribed: Flow<Boolean> = combine(
        savedStateHandle.getStateFlow<String?>("artistId", null),
        userPreferencesRepository.subscribedArtistIdsFlow
    ) { artistIdStr, subscribedIds ->
        artistIdStr != null && subscribedIds.contains(artistIdStr)
    }

    fun toggleSubscription() {
        val artistIdStr = savedStateHandle.get<String?>("artistId") ?: return
        viewModelScope.launch {
            val browseId = uiState.value.browseId ?: if (artistIdStr.toLongOrNull() == null) artistIdStr else null
            val currentSubscribed = userPreferencesRepository.subscribedArtistIdsFlow.first()
            val isCurrentlySubscribed = currentSubscribed.contains(artistIdStr)
            val subscribe = !isCurrentlySubscribed

            if (browseId != null) {
                try {
                    withContext(Dispatchers.IO) {
                        InnerTubeYouTube.subscribeChannel(browseId, subscribe)
                    }
                } catch (e: Exception) {
                    Log.e("ArtistDetailViewModel", "Failed to toggle remote subscription", e)
                }
            }

            userPreferencesRepository.subscribeArtist(artistIdStr, subscribe)
            if (browseId != null && browseId != artistIdStr) {
                userPreferencesRepository.subscribeArtist(browseId, subscribe)
            }

            if (subscribe) {
                val artist = uiState.value.artist
                if (artist != null) {
                    val finalArtistId = if (artist.id < 0) artist.id else -(17_000_000_000_000L + kotlin.math.abs(artist.name.lowercase().hashCode().toLong()))
                    val artistEntity = com.saurav.pixelmusic.data.database.ArtistEntity(
                        id = finalArtistId,
                        name = artist.name,
                        trackCount = artist.songCount,
                        imageUrl = artist.imageUrl ?: uiState.value.effectiveImageUrl,
                        customImageUri = artist.customImageUri,
                        channelId = browseId ?: artist.channelId
                    )
                    withContext(Dispatchers.IO) {
                        musicDao.insertArtists(listOf(artistEntity))
                    }
                }
            }
        }
    }

    init {
        savedStateHandle.getStateFlow<String?>("artistId", null)
            .onEach { idString ->
                if (idString != null) {
                    loadArtistData(idString)
                } else {
                    _uiState.update { it.copy(error = context.getString(R.string.artist_id_not_found), isLoading = false) }
                }
            }
            .launchIn(viewModelScope)
    }

    private var currentLoadJob: Job? = null

    private fun loadArtistData(artistIdStr: String) {
        currentLoadJob?.cancel()
        currentLoadJob = viewModelScope.launch {
            Log.d("ArtistDebug", "loadArtistData: idStr=$artistIdStr")
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val numericId = artistIdStr.toLongOrNull()
                var browseId: String? = null
                if (numericId == null || artistIdStr.startsWith("UC") || artistIdStr.startsWith("LA")) {
                    browseId = artistIdStr
                } else {
                    val localArtist = musicRepository.getArtistById(numericId).first()
                    if (localArtist != null) {
                        val primaryArtistName = localArtist.name.split(
                            ", ", " & ", " feat.", " feat ", " Feat.", " Feat ", " FT.", " FT ", " ft.", " ft "
                        ).firstOrNull()?.trim() ?: localArtist.name

                        val searchResult = withContext(Dispatchers.IO) {
                            InnerTubeYouTube.search(primaryArtistName, InnerTubeYouTube.SearchFilter.FILTER_ARTIST).getOrNull()
                        }
                        val artistItem = searchResult?.items?.find { it is ArtistItem } as? ArtistItem
                        browseId = artistItem?.id
                    }
                }

                if (browseId != null && (browseId.startsWith("UC") || browseId.startsWith("LA") || numericId == null)) {
                    val artistPageResult = withContext(Dispatchers.IO) {
                        InnerTubeYouTube.artist(browseId)
                    }

                    artistPageResult.onSuccess { artistPage ->
                        val artistItem = artistPage.artist

                        // ── Popular Songs: extract from the "Songs" section ──
                        val ytSongsSection = artistPage.sections.find {
                            it.title.contains("Songs", ignoreCase = true) ||
                            it.title.contains("Popular", ignoreCase = true)
                        }
                        
                        val ytPopularSongs = ytSongsSection?.items?.mapNotNull { item ->
                            (item as? SongItem)?.toNativeSong()
                        } ?: emptyList()

                        // ---> OUR FIX: ALWAYS fetch local/downloaded songs first! <---
                        val localSongs = if (numericId != null) {
                            runCatching { musicRepository.getSongsForArtist(numericId).first() }.getOrDefault(emptyList())
                        } else emptyList()

                        // Merge them: Your downloaded songs show up first, then YouTube's tracks. 
                        // distinctBy { it.id } ensures we don't show the exact same song twice!
                        val popularSongs = (localSongs + ytPopularSongs)
                            .distinctBy { it.id }
                            .take(10)

                        val localCount = withContext(Dispatchers.IO) {
                            runCatching {
                                musicDao.getLocalSongCountByArtistName(artistItem.title)
                            }.getOrDefault(0)
                        }

                        val artistModel = Artist(
                            id = browseId.hashCode().toLong(),
                            name = artistItem.title,
                            songCount = if (localCount > 0) localCount else popularSongs.size,
                            imageUrl = artistItem.thumbnail
                        )
                        

                        // ── Albums: sections titled "Albums" or "Releases" ──
                        fun AlbumItem.toAlbumSection(artistTitle: String, artistIdHash: Long): ArtistAlbumSection {
                            return ArtistAlbumSection(
                                albumId = this.browseId.hashCode().toLong(),
                                title = this.title,
                                year = this.year,
                                albumArtUriString = this.thumbnail,
                                browseId = this.browseId,
                                songs = emptyList(), // Albums shown as cards; songs loaded on album detail
                                sectionType = ArtistSectionType.ALBUM
                            )
                        }

                        val albumsSection = artistPage.sections.firstOrNull { section ->
                            section.title.contains("Albums", ignoreCase = true) ||
                            section.title.contains("Releases", ignoreCase = true)
                        }
                        val albumSections = albumsSection?.items?.mapNotNull { item ->
                            when (item) {
                                is AlbumItem -> item.toAlbumSection(artistItem.title, browseId.hashCode().toLong())
                                else -> null
                            }
                        }.orEmpty()

                        // ── Singles & EPs: sections titled "Singles", "EP", or "EPs" ──
                        val singlesSection = artistPage.sections.firstOrNull { section ->
                            section.title.contains("Single", ignoreCase = true) ||
                            section.title.contains("EP", ignoreCase = true)
                        }
                        val singlesAndEPs = singlesSection?.items?.mapNotNull { item ->
                            when (item) {
                                is AlbumItem -> ArtistAlbumSection(
                                    albumId = item.browseId.hashCode().toLong(),
                                    title = item.title,
                                    year = item.year,
                                    albumArtUriString = item.thumbnail,
                                    browseId = item.browseId,
                                    songs = emptyList(),
                                    sectionType = ArtistSectionType.SINGLE_EP
                                )
                                else -> null
                            }
                        }.orEmpty()

                        val effectiveImageUrl = artistItem.thumbnail
                        val newScheme = if (!effectiveImageUrl.isNullOrBlank()) {
                            try {
                                themeStateHolder.getOrGenerateColorScheme(effectiveImageUrl)
                            } catch (e: Exception) {
                                null
                            }
                        } else null

                        _artistColorScheme.value = newScheme
                        _uiState.value = ArtistDetailUiState(
                            artist = artistModel,
                            songs = popularSongs,
                            popularSongs = popularSongs,
                            albumSections = albumSections,
                            singlesAndEPs = singlesAndEPs,
                            effectiveImageUrl = effectiveImageUrl,
                            isLoading = false,
                            isOnlineArtist = true,
                            artistDescription = artistPage.description,
                            subscriberCount = artistItem.subscriberCountText,
                            browseId = browseId,
                            albumsMoreEndpoint = albumsSection?.moreEndpoint,
                            singlesMoreEndpoint = singlesSection?.moreEndpoint,
                            songsMoreEndpoint = ytSongsSection?.moreEndpoint
                        )
                    }.onFailure { e ->
                        _uiState.update {
                            it.copy(
                                error = context.getString(R.string.error_loading_artist, e.localizedMessage ?: ""),
                                isLoading = false
                            )
                        }
                    }
                } else {
                    val id = numericId ?: return@launch
                    val artistDetailsFlow = musicRepository.getArtistById(id)
                    val artistSongsFlow = musicRepository.getSongsForArtist(id)

                    combine(artistDetailsFlow, artistSongsFlow) { artist, songs ->
                        Log.d("ArtistDebug", "loadArtistData: id=$id found=${artist != null} songs=${songs.size}")
                        artist to songs
                    }
                        .catch { e ->
                            _uiState.update {
                                it.copy(
                                    error = context.getString(R.string.error_loading_artist, e.localizedMessage ?: ""),
                                    isLoading = false
                                )
                            }
                        }
                        .collect { (artist, songs) ->
                            if (artist == null) {
                                _uiState.update {
                                    it.copy(error = context.getString(R.string.could_not_find_artist), isLoading = false)
                                }
                                return@collect
                            }

                            val albumSections = buildAlbumSections(songs)
                            val orderedSongs = albumSections.flatMap { it.songs }

                            val effectiveUrl = try {
                                artistImageRepository.getEffectiveArtistImageUrl(
                                    artistId = artist.id,
                                    artistName = artist.name
                                )
                            } catch (e: Exception) {
                                Log.w("ArtistDebug", "Failed to resolve effective artist image: ${e.message}")
                                artist.effectiveImageUrl
                            }

                            val newScheme = if (!effectiveUrl.isNullOrBlank()) {
                                try {
                                    themeStateHolder.getOrGenerateColorScheme(effectiveUrl)
                                } catch (e: Exception) {
                                    Log.w("ArtistDebug", "Color scheme pre-warm failed: ${e.message}")
                                    null
                                }
                            } else null

                            _artistColorScheme.value = newScheme
                            _uiState.value = ArtistDetailUiState(
                                artist = artist.copy(
                                    imageUrl = if (artist.customImageUri.isNullOrBlank()) effectiveUrl else artist.imageUrl
                                ),
                                songs = orderedSongs,
                                popularSongs = emptyList(),
                                albumSections = albumSections,
                                singlesAndEPs = emptyList(),
                                effectiveImageUrl = effectiveUrl,
                                isLoading = false,
                                isOnlineArtist = false
                            )
                        }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        error = context.getString(R.string.error_loading_artist, e.localizedMessage ?: ""),
                        isLoading = false
                    )
                }
            }
        }
    }

    /**
     * Called from the UI when the user selects a custom image from the system photo picker.
     * Copies the image to internal storage, persists the path to DB, and triggers palette regeneration.
     */
    fun setCustomImage(sourceUri: Uri) {
        val artistId = _uiState.value.artist?.id ?: return
        viewModelScope.launch {
            try {
                val internalPath = artistImageRepository.setCustomArtistImage(context, artistId, sourceUri)
                if (!internalPath.isNullOrBlank()) {
                    val oldEffectiveUrl = _uiState.value.effectiveImageUrl

                    // Regenerate palette from the new image url — invalidate old and warm-up new
                    if (!oldEffectiveUrl.isNullOrBlank() && oldEffectiveUrl != internalPath) {
                        themeStateHolder.forceRegenerateColorScheme(oldEffectiveUrl)
                    }
                    val newScheme = try {
                        themeStateHolder.forceRegenerateColorScheme(internalPath)
                        themeStateHolder.getOrGenerateColorScheme(internalPath)
                    } catch (e: Exception) {
                        Log.w("ArtistDebug", "Failed to regenerate color scheme for custom image: ${e.message}")
                        null
                    }

                    _artistColorScheme.value = newScheme
                    _uiState.update { state ->
                        // Cache-busting: add timestamp to internalPath to force Coil to reload
                        val effectiveUrlWithBust = "$internalPath?t=${System.currentTimeMillis()}"
                        state.copy(
                            effectiveImageUrl = effectiveUrlWithBust,
                            artist = state.artist?.copy(customImageUri = internalPath)
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e("ArtistDebug", "Failed to set custom image: ${e.message}")
            }
        }
    }

    /**
     * Called when the user wants to revert to the Deezer-sourced image.
     */
    fun clearCustomImage() {
        val artist = _uiState.value.artist ?: return
        viewModelScope.launch {
            try {
                val oldEffectiveUrl = _uiState.value.effectiveImageUrl
                artistImageRepository.clearCustomArtistImage(context, artist.id)

                // Fall back to Deezer URL
                val deezerUrl = artistImageRepository.getArtistImageUrl(artist.name, artist.id)
                val newEffectiveUrl = deezerUrl.takeIf { !it.isNullOrBlank() }

                // Invalidate old custom image palette
                if (!oldEffectiveUrl.isNullOrBlank()) {
                    themeStateHolder.forceRegenerateColorScheme(oldEffectiveUrl)
                }

                val newScheme = if (!newEffectiveUrl.isNullOrBlank()) {
                    try {
                        themeStateHolder.getOrGenerateColorScheme(newEffectiveUrl)
                    } catch (e: Exception) {
                        Log.w("ArtistDebug", "Failed to regenerate palette after clear: ${e.message}")
                        null
                    }
                } else null

                _artistColorScheme.value = newScheme
                _uiState.update { state ->
                    state.copy(
                        effectiveImageUrl = newEffectiveUrl,
                        artist = state.artist?.copy(customImageUri = null, imageUrl = deezerUrl)
                    )
                }

            } catch (e: Exception) {
                Log.e("ArtistDebug", "Failed to clear custom image: ${e.message}")
            }
        }
    }

    fun removeSongFromAlbumSection(songId: String) {
        _uiState.update { currentState ->
            val updatedAlbumSections = currentState.albumSections.map { section ->
                val updatedSongs = section.songs.filterNot { it.id == songId }
                section.copy(songs = updatedSongs)
            }.filter { it.songs.isNotEmpty() }

            currentState.copy(
                albumSections = updatedAlbumSections,
                popularSongs = currentState.popularSongs.filterNot { it.id == songId },
                songs = currentState.songs.filterNot { it.id == songId }
            )
        }
    }

    private var allItemsJob: Job? = null

    fun loadAllItems(type: String) {
        val endpoint = if (type == "singles") {
            _uiState.value.singlesMoreEndpoint
        } else {
            _uiState.value.albumsMoreEndpoint
        }

        if (endpoint == null) {
            val initialItems = if (type == "singles") {
                _uiState.value.singlesAndEPs
            } else {
                _uiState.value.albumSections
            }
            _uiState.update { it.copy(allItems = initialItems, allItemsContinuation = null, isAllItemsLoading = false) }
            return
        }

        allItemsJob?.cancel()
        allItemsJob = viewModelScope.launch {
            _uiState.update { it.copy(isAllItemsLoading = true, allItems = emptyList(), allItemsContinuation = null, allItemsError = null) }
            try {
                val result = withContext(Dispatchers.IO) {
                    InnerTubeYouTube.artistItems(endpoint)
                }
                result.onSuccess { page ->
                    val mappedItems = page.items.mapNotNull { item ->
                        when (item) {
                            is AlbumItem -> ArtistAlbumSection(
                                albumId = item.browseId.hashCode().toLong(),
                                title = item.title,
                                year = item.year,
                                albumArtUriString = item.thumbnail,
                                browseId = item.browseId,
                                songs = emptyList(),
                                sectionType = if (type == "singles") ArtistSectionType.SINGLE_EP else ArtistSectionType.ALBUM
                            )
                            else -> null
                        }
                    }
                    _uiState.update {
                        it.copy(
                            allItems = mappedItems,
                            allItemsContinuation = page.continuation,
                            isAllItemsLoading = false
                        )
                    }
                }.onFailure { e ->
                    _uiState.update {
                        it.copy(
                            allItemsError = e.localizedMessage,
                            isAllItemsLoading = false
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        allItemsError = e.localizedMessage,
                        isAllItemsLoading = false
                    )
                }
            }
        }
    }

    fun loadMoreAllItems(type: String) {
        val continuation = _uiState.value.allItemsContinuation ?: return
        if (_uiState.value.isAllItemsLoading) return

        viewModelScope.launch {
            _uiState.update { it.copy(isAllItemsLoading = true) }
            try {
                val result = withContext(Dispatchers.IO) {
                    InnerTubeYouTube.artistItemsContinuation(continuation)
                }
                result.onSuccess { page ->
                    val mappedItems = page.items.mapNotNull { item ->
                        when (item) {
                            is AlbumItem -> ArtistAlbumSection(
                                albumId = item.browseId.hashCode().toLong(),
                                title = item.title,
                                year = item.year,
                                albumArtUriString = item.thumbnail,
                                browseId = item.browseId,
                                songs = emptyList(),
                                sectionType = if (type == "singles") ArtistSectionType.SINGLE_EP else ArtistSectionType.ALBUM
                            )
                            else -> null
                        }
                    }
                    _uiState.update {
                        it.copy(
                            allItems = it.allItems + mappedItems,
                            allItemsContinuation = page.continuation,
                            isAllItemsLoading = false
                        )
                    }
                }.onFailure { e ->
                    _uiState.update {
                        it.copy(
                            allItemsError = e.localizedMessage,
                            isAllItemsLoading = false
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        allItemsError = e.localizedMessage,
                        isAllItemsLoading = false
                    )
                }
            }
        }
    }

    private var popularSongsAllJob: Job? = null

    fun loadAllPopularSongs() {
        val endpoint = _uiState.value.songsMoreEndpoint
        if (endpoint == null) {
            _uiState.update { it.copy(popularSongsAll = it.popularSongs, popularSongsAllContinuation = null, isPopularSongsAllLoading = false) }
            return
        }

        popularSongsAllJob?.cancel()
        popularSongsAllJob = viewModelScope.launch {
            _uiState.update { it.copy(isPopularSongsAllLoading = true, popularSongsAll = emptyList(), popularSongsAllContinuation = null, popularSongsAllError = null) }
            try {
                val result = withContext(Dispatchers.IO) {
                    InnerTubeYouTube.artistItems(endpoint)
                }
                result.onSuccess { page ->
                    val mappedSongs = page.items.mapNotNull { item ->
                        (item as? SongItem)?.toNativeSong()
                    }
                    _uiState.update {
                        it.copy(
                            popularSongsAll = mappedSongs,
                            popularSongsAllContinuation = page.continuation,
                            isPopularSongsAllLoading = false
                        )
                    }
                }.onFailure { e ->
                    _uiState.update {
                        it.copy(
                            popularSongsAllError = e.localizedMessage,
                            isPopularSongsAllLoading = false
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        popularSongsAllError = e.localizedMessage,
                        isPopularSongsAllLoading = false
                    )
                }
            }
        }
    }

    fun loadMorePopularSongs() {
        val continuation = _uiState.value.popularSongsAllContinuation ?: return
        if (_uiState.value.isPopularSongsAllLoading) return

        viewModelScope.launch {
            _uiState.update { it.copy(isPopularSongsAllLoading = true) }
            try {
                val result = withContext(Dispatchers.IO) {
                    InnerTubeYouTube.artistItemsContinuation(continuation)
                }
                result.onSuccess { page ->
                    val mappedSongs = page.items.mapNotNull { item ->
                        (item as? SongItem)?.toNativeSong()
                    }
                    _uiState.update {
                        it.copy(
                            popularSongsAll = it.popularSongsAll + mappedSongs,
                            popularSongsAllContinuation = page.continuation,
                            isPopularSongsAllLoading = false
                        )
                    }
                }.onFailure { e ->
                    _uiState.update {
                        it.copy(
                            popularSongsAllError = e.localizedMessage,
                            isPopularSongsAllLoading = false
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        popularSongsAllError = e.localizedMessage,
                        isPopularSongsAllLoading = false
                    )
                }
            }
        }
    }
}

private val songDisplayComparator = compareBy<Song> { it.discNumber ?: 1 }
    .thenBy { if (it.trackNumber > 0) it.trackNumber else Int.MAX_VALUE }
    .thenBy { it.title.lowercase() }

private fun buildAlbumSections(songs: List<Song>): List<ArtistAlbumSection> {
    if (songs.isEmpty()) return emptyList()

    val sections = songs
        .groupBy { it.albumId to it.album }
        .map { (key, albumSongs) ->
            val sortedSongs = albumSongs.sortedWith(songDisplayComparator)
            val albumYear = albumSongs.mapNotNull { song -> song.year.takeIf { it > 0 } }.maxOrNull()
            val albumArtUri = albumSongs.firstNotNullOfOrNull { it.albumArtUriString }
            ArtistAlbumSection(
                albumId = key.first,
                title = (key.second.takeIf { it.isNotBlank() } ?: "Unknown Album"),
                year = albumYear,
                albumArtUriString = albumArtUri,
                songs = sortedSongs
            )
        }

    val (withYear, withoutYear) = sections.partition { it.year != null }
    val withYearSorted = withYear.sortedWith(
        compareByDescending<ArtistAlbumSection> { it.year ?: Int.MIN_VALUE }
            .thenBy { it.title.lowercase() }
    )
    val withoutYearSorted = withoutYear.sortedBy { it.title.lowercase() }

    return withYearSorted + withoutYearSorted
}
