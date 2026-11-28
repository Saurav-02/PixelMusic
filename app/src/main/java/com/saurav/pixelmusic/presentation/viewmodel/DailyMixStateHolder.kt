package com.saurav.pixelmusic.presentation.viewmodel

import android.content.Context
import com.saurav.pixelmusic.data.DailyMixManager
import com.saurav.pixelmusic.data.database.EngagementDao
import com.saurav.pixelmusic.data.model.Song
import com.saurav.pixelmusic.data.preferences.UserPreferencesRepository
import com.saurav.pixelmusic.data.remote.youtube.toNativeSong
import com.saurav.pixelmusic.data.repository.MusicRepository
import com.saurav.pixelmusic.data.stats.PlaybackStatsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import saurav.shru.pixelmusic.innertube.YouTube
import saurav.shru.pixelmusic.innertube.models.SongItem
import saurav.shru.pixelmusic.innertube.models.WatchEndpoint
import saurav.shru.pixelmusic.innertube.models.filterDuration
import saurav.shru.pixelmusic.innertube.models.filterExplicit
import saurav.shru.pixelmusic.innertube.models.filterVideo
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

private const val MIX_CACHE_PREFS = "daily_your_mix_cache"
private const val KEY_DAILY_MIX_JSON = "daily_mix_json"
private const val KEY_YOUR_MIX_JSON = "your_mix_json"

/**
 * Manages Daily Mix and Your Mix state.
 * Extracted from PlayerViewModel to improve modularity.
 *
 * Responsibilities:
 * - Generate and update daily/your mixes (blending local library, history, and related YouTube tracks)
 * - Persist and restore mix state
 * - Check if mix needs updating based on day change
 */
@Singleton
class DailyMixStateHolder @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dailyMixManager: DailyMixManager,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val musicRepository: MusicRepository,
    private val playbackStatsRepository: PlaybackStatsRepository,
    private val engagementDao: EngagementDao
) {
    private var scope: CoroutineScope? = null
    private var updateJob: Job? = null
    private val mixCachePrefs by lazy { context.getSharedPreferences(MIX_CACHE_PREFS, Context.MODE_PRIVATE) }

    private val _dailyMixSongs = MutableStateFlow<ImmutableList<Song>>(persistentListOf())
    val dailyMixSongs: StateFlow<ImmutableList<Song>> = _dailyMixSongs.asStateFlow()

    private val _yourMixSongs = MutableStateFlow<ImmutableList<Song>>(persistentListOf())
    val yourMixSongs: StateFlow<ImmutableList<Song>> = _yourMixSongs.asStateFlow()

    /**
     * Initialize with coroutine scope from ViewModel.
     */
    fun initialize(coroutineScope: CoroutineScope) {
        scope = coroutineScope
    }

    /**
     * Remove a song from the daily mix.
     */
    fun removeFromDailyMix(songId: String) {
        _dailyMixSongs.update { currentList ->
            currentList.filterNot { it.id == songId }.toImmutableList()
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

    private suspend fun buildEnrichedCandidatePool(
        localSongs: List<Song>,
        forceRefresh: Boolean
    ): List<Song> = coroutineScope {
        val isLocalMode = userPreferencesRepository.localModeEnabledFlow.first()
        if (isLocalMode) {
            return@coroutineScope localSongs.filter { song ->
                (!song.contentUriString.startsWith("youtube://", ignoreCase = true) &&
                    !song.contentUriString.startsWith("http://", ignoreCase = true) &&
                    !song.contentUriString.startsWith("https://", ignoreCase = true)) ||
                    (song.path.isNotBlank() && java.io.File(song.path).exists())
            }
        }
        val pureYtMusicOnly = userPreferencesRepository.pureYtMusicOnlyFlow.first()
        val hideExplicit = userPreferencesRepository.hideExplicitFlow.first()
        val hideVideo = userPreferencesRepository.hideVideoFlow.first()
        val minDurationSec = userPreferencesRepository.getMinSongDuration() / 1000
        val shouldFilterVideos = pureYtMusicOnly || hideVideo

        val mostPlayed = runCatching { playbackStatsRepository.loadMostPlayedTracks(limit = 30) }.getOrDefault(emptyList())
        val recentHistory = runCatching { playbackStatsRepository.loadPlaybackHistory(limit = 30) }.getOrDefault(emptyList())
        val topEngagements = runCatching { engagementDao.getTopPlayedSongs(25) }.getOrDefault(emptyList())

        val localByKey = mutableMapOf<String, Song>()
        localSongs.forEach { s ->
            localByKey[s.id] = s
            val yId = s.youtubeId ?: extractCleanYoutubeId(s.contentUriString)
            if (!yId.isNullOrBlank()) {
                localByKey[yId] = s
                localByKey["youtube_$yId"] = s
            }
        }

        val historySongs = mutableListOf<Song>()
        (mostPlayed + recentHistory).forEach { entry ->
            val cleanId = extractCleanYoutubeId(entry.songId)
            val existing = localByKey[entry.songId] ?: (if (cleanId != null) localByKey[cleanId] else null)
            if (existing != null) {
                historySongs.add(existing)
            } else if (cleanId != null && !entry.title.isNullOrBlank()) {
                historySongs.add(
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

        val seedVideoIds = mutableListOf<String>()
        historySongs.forEach { s ->
            val yId = s.youtubeId ?: extractCleanYoutubeId(s.id) ?: extractCleanYoutubeId(s.contentUriString)
            if (!yId.isNullOrBlank()) seedVideoIds.add(yId)
        }
        topEngagements.forEach { e ->
            val yId = extractCleanYoutubeId(e.songId) ?: localByKey[e.songId]?.youtubeId
            if (!yId.isNullOrBlank()) seedVideoIds.add(yId)
        }
        localSongs.filter { it.isFavorite }.forEach { s ->
            val yId = s.youtubeId ?: extractCleanYoutubeId(s.contentUriString)
            if (!yId.isNullOrBlank()) seedVideoIds.add(yId)
        }

        val distinctSeeds = seedVideoIds.distinct().let { seeds ->
            if (forceRefresh && seeds.size > 1) seeds.take(12).shuffled() else seeds
        }

        // Fetch related YouTube radio songs from up to 3 seeds + YT Home recommendations
        val radioSongsDeferred = distinctSeeds.take(3).map { videoId ->
            async(Dispatchers.IO) {
                runCatching {
                    val nextRes = YouTube.next(WatchEndpoint(videoId = videoId, playlistId = "RDAMVM$videoId")).getOrNull()
                    val radioItems = nextRes?.items
                        ?.filterIsInstance<SongItem>()
                        ?.filter { it.id != videoId }
                        ?.filterExplicit(hideExplicit)
                        ?.filterVideo(shouldFilterVideos)
                        ?.filterDuration(minDurationSec)
                        .orEmpty()
                    val relatedItems = nextRes?.relatedEndpoint?.let { ep ->
                        YouTube.related(ep).getOrNull()?.songs
                            ?.filterExplicit(hideExplicit)
                            ?.filterVideo(shouldFilterVideos)
                            ?.filterDuration(minDurationSec)
                    }.orEmpty()
                    val combined = (radioItems + relatedItems).map { it.toNativeSong() }
                    if (forceRefresh) combined.shuffled() else combined
                }.getOrDefault(emptyList())
            }
        }

        val ytHomeDeferred = async(Dispatchers.IO) {
            runCatching {
                YouTube.home().getOrNull()?.sections
                    ?.flatMap { it.items.filterIsInstance<SongItem>() }
                    ?.filterExplicit(hideExplicit)
                    ?.filterVideo(shouldFilterVideos)
                    ?.filterDuration(minDurationSec)
                    ?.map { it.toNativeSong() }
                    ?.let { if (forceRefresh) it.shuffled() else it }
                    .orEmpty()
            }.getOrDefault(emptyList())
        }

        val discoveredSongs = radioSongsDeferred.flatMap { it.await() } + ytHomeDeferred.await()

        (discoveredSongs + historySongs + localSongs)
            .distinctBy { song ->
                song.youtubeId?.takeIf { it.isNotBlank() }
                    ?: extractCleanYoutubeId(song.contentUriString)
                    ?: "${song.title.lowercase()}|${song.artist.lowercase()}"
            }
    }

    /**
     * Update the daily mix and your mix with local library + related discovery songs.
     */
    fun updateDailyMix(
        favoriteSongIdsFlow: kotlinx.coroutines.flow.Flow<Set<String>>,
        forceRefresh: Boolean = false
    ) {
        updateJob?.cancel()
        updateJob = scope?.launch(Dispatchers.IO) {
            val localSongs = musicRepository.getAllSongsOnce()
            val candidatePool = buildEnrichedCandidatePool(localSongs, forceRefresh)
            if (candidatePool.isNotEmpty()) {
                val favoriteIds = favoriteSongIdsFlow.first()

                val previousYourMixKeys = if (forceRefresh) {
                    _yourMixSongs.value.take(12).map { s ->
                        s.youtubeId?.takeIf { it.isNotBlank() } ?: s.id
                    }.toSet()
                } else emptySet()

                val previousDailyMixKeys = if (forceRefresh) {
                    _dailyMixSongs.value.take(12).map { s ->
                        s.youtubeId?.takeIf { it.isNotBlank() } ?: s.id
                    }.toSet()
                } else emptySet()

                // Generate Your Mix (rotate out previous top collage tracks on manual refresh when enough candidates exist)
                val yourMixPool = if (forceRefresh && candidatePool.size > 12) {
                    val (prev, fresh) = candidatePool.partition { s ->
                        (s.youtubeId?.takeIf { it.isNotBlank() } ?: s.id) in previousYourMixKeys
                    }
                    if (fresh.size >= 6) fresh + prev else candidatePool.shuffled()
                } else candidatePool

                val yourMix = dailyMixManager.generateYourMix(
                    allSongs = yourMixPool,
                    favoriteSongIds = favoriteIds,
                    forceRefresh = forceRefresh
                ).let { generated ->
                    if (forceRefresh && generated.size > 6) {
                        val (prev, fresh) = generated.partition { s ->
                            (s.youtubeId?.takeIf { it.isNotBlank() } ?: s.id) in previousYourMixKeys
                        }
                        if (fresh.size >= 6) fresh + prev else generated.shuffled()
                    } else generated
                }
                _yourMixSongs.value = yourMix.toImmutableList()
                userPreferencesRepository.saveYourMixSongIds(yourMix.map { it.id })
                saveSongsToCache(KEY_YOUR_MIX_JSON, yourMix)

                // Generate Daily Mix (distinct ordering from Your Mix)
                val topYourMixIds = yourMix.take(6).map { it.id }.toSet()
                val dailyMixPool = candidatePool.filterNot { it.id in topYourMixIds }.ifEmpty { candidatePool }
                val mix = dailyMixManager.generateDailyMix(
                    allSongs = dailyMixPool,
                    favoriteSongIds = favoriteIds,
                    forceRefresh = forceRefresh
                ).let { generated ->
                    if (forceRefresh && generated.size > 4) {
                        val (prev, fresh) = generated.partition { s ->
                            (s.youtubeId?.takeIf { it.isNotBlank() } ?: s.id) in previousDailyMixKeys
                        }
                        if (fresh.size >= 4) fresh + prev else generated.shuffled()
                    } else generated
                }
                _dailyMixSongs.value = mix.toImmutableList()
                userPreferencesRepository.saveDailyMixSongIds(mix.map { it.id })
                saveSongsToCache(KEY_DAILY_MIX_JSON, mix)
            } else {
                _yourMixSongs.value = persistentListOf()
            }
        }
    }

    private fun saveSongsToCache(key: String, songs: List<Song>) {
        runCatching {
            val arr = JSONArray()
            songs.forEach { song ->
                arr.put(JSONObject().apply {
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
                })
            }
            mixCachePrefs.edit().putString(key, arr.toString()).apply()
        }
    }

    private fun loadSongsFromCache(key: String): List<Song> {
        return runCatching {
            val raw = mixCachePrefs.getString(key, null) ?: return emptyList()
            val arr = JSONArray(raw)
            buildList {
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    add(
                        Song(
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
                    )
                }
            }
        }.getOrDefault(emptyList())
    }

    /**
     * Load persisted daily mix from storage using cached JSON or direct DB queries by IDs.
     */
    fun loadPersistedDailyMix() {
        // Load Daily Mix
        scope?.launch {
            if (_dailyMixSongs.value.isEmpty()) {
                val cached = withContext(Dispatchers.IO) { loadSongsFromCache(KEY_DAILY_MIX_JSON) }
                if (cached.isNotEmpty()) {
                    _dailyMixSongs.value = cached.toImmutableList()
                } else {
                    val dailyMixIds = userPreferencesRepository.dailyMixSongIdsFlow.first()
                    if (dailyMixIds.isNotEmpty()) {
                        val songs = withContext(Dispatchers.IO) {
                            musicRepository.getSongsByIdsOnce(dailyMixIds)
                        }
                        if (songs.isNotEmpty()) {
                            val songMap = mutableMapOf<String, Song>()
                            songs.forEach { s ->
                                songMap[s.id] = s
                                s.youtubeId?.let { yId -> songMap["youtube_$yId"] = s }
                            }
                            val orderedSongs = dailyMixIds.mapNotNull { songMap[it] }
                            _dailyMixSongs.value = orderedSongs.toImmutableList()
                        }
                    }
                }
            }
        }

        // Load Your Mix
        scope?.launch {
            if (_yourMixSongs.value.isEmpty()) {
                val cached = withContext(Dispatchers.IO) { loadSongsFromCache(KEY_YOUR_MIX_JSON) }
                if (cached.isNotEmpty()) {
                    _yourMixSongs.value = cached.toImmutableList()
                } else {
                    val yourMixIds = userPreferencesRepository.yourMixSongIdsFlow.first()
                    if (yourMixIds.isNotEmpty()) {
                        val songs = withContext(Dispatchers.IO) {
                            musicRepository.getSongsByIdsOnce(yourMixIds)
                        }
                        if (songs.isNotEmpty()) {
                            val songMap = mutableMapOf<String, Song>()
                            songs.forEach { s ->
                                songMap[s.id] = s
                                s.youtubeId?.let { yId -> songMap["youtube_$yId"] = s }
                            }
                            val orderedSongs = yourMixIds.mapNotNull { songMap[it] }
                            _yourMixSongs.value = orderedSongs.toImmutableList()
                        }
                    }
                }
            }
        }
    }

    /**
     * Force update the daily mix regardless of day.
     */
    fun forceUpdate(favoriteSongIdsFlow: kotlinx.coroutines.flow.Flow<Set<String>>) {
        scope?.launch {
            updateDailyMix(favoriteSongIdsFlow, forceRefresh = true)
            userPreferencesRepository.saveLastDailyMixUpdateTimestamp(System.currentTimeMillis())
        }
    }

    /**
     * Check if daily mix needs updating (new day) and update if so.
     */
    fun checkAndUpdateIfNeeded(favoriteSongIdsFlow: kotlinx.coroutines.flow.Flow<Set<String>>) {
        scope?.launch {
            val lastUpdate = userPreferencesRepository.lastDailyMixUpdateFlow.first()
            val today = Calendar.getInstance().get(Calendar.DAY_OF_YEAR)
            val lastUpdateDay = Calendar.getInstance().apply {
                timeInMillis = lastUpdate
            }.get(Calendar.DAY_OF_YEAR)

            if (today != lastUpdateDay) {
                updateDailyMix(favoriteSongIdsFlow)
                userPreferencesRepository.saveLastDailyMixUpdateTimestamp(System.currentTimeMillis())
            }
        }
    }

    /**
     * Set the daily mix songs directly (used for AI-generated mixes).
     */
    fun setDailyMixSongs(songs: List<Song>) {
        _dailyMixSongs.value = songs.toImmutableList()
        scope?.launch {
            userPreferencesRepository.saveDailyMixSongIds(songs.map { it.id })
            userPreferencesRepository.saveLastDailyMixUpdateTimestamp(System.currentTimeMillis())
        }
    }

    /**
     * Get a candidate pool for AI playlist generation.
     */
    suspend fun getCandidatePool(
        allSongs: List<Song>,
        favoriteIds: Set<String>,
        maxSize: Int = 100
    ): List<Song> {
        return dailyMixManager.generateDailyMix(allSongs, favoriteIds, maxSize)
    }

    fun onCleared() {
        updateJob?.cancel()
        scope = null
    }
}
