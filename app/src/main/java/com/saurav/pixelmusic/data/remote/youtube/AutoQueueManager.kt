package com.saurav.pixelmusic.data.remote.youtube

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import com.saurav.pixelmusic.data.remote.youtube.UmihiHelper.printe
import com.saurav.pixelmusic.data.remote.youtube.UmihiHelper.printd
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.runBlocking
import kotlin.math.absoluteValue

import saurav.shru.pixelmusic.innertube.YouTube
import saurav.shru.pixelmusic.innertube.models.WatchEndpoint
import com.saurav.pixelmusic.data.model.Song
import com.saurav.pixelmusic.data.database.MusicDao
import com.saurav.pixelmusic.data.database.RelatedSongMap
import com.saurav.pixelmusic.data.database.SongEntity
import com.saurav.pixelmusic.data.database.toSong
import com.saurav.pixelmusic.data.database.AlbumEntity
import com.saurav.pixelmusic.data.database.ArtistEntity
import com.saurav.pixelmusic.data.database.SongArtistCrossRef
import com.saurav.pixelmusic.data.database.SourceType
import com.saurav.pixelmusic.data.model.ArtistRef
import com.saurav.pixelmusic.utils.MediaItemBuilder
import com.saurav.pixelmusic.presentation.viewmodel.ConnectivityStateHolder

/**
 * AutoQueueManager — Radio Mode (ArchiveTune 2026 Engine)
 *
 * Maintains a constant upcoming playback queue of 40–50 songs.
 * Automatically appends related songs when the remaining count falls below 40.
 * Supports online related tracks (YouTube Music next) and offline hybrid local fallback.
 */
object AutoQueueManager {

    private const val TARGET_QUEUE_SIZE = 45 // Targets exactly 45 upcoming songs
    private const val MAX_HISTORY = 60  // Reduced: avoid over-filtering all candidates
    private const val DECAY_LAMBDA = 1.15e-9 // 7-day half-life decay parameter

    private var fetchJob: Job? = null
    private var lastFetchedVideoId: String? = null
    private var continuationToken: String? = null
    private var currentWatchEndpoint: WatchEndpoint? = null
    private val addedVideoIds = mutableSetOf<String>()

    // Memory cache mapping local/offline song IDs to matched YouTube video IDs
    private val localToYoutubeIdMap = mutableMapOf<String, String>()

    enum class Mood { CHILL, UPBEAT, DEFAULT }
    private val sessionPlayHistory = mutableListOf<String>()
    private val CHILL_GENRES = setOf("classical", "lofi", "acoustic", "ambient", "jazz", "piano", "chill", "blues", "slow")
    private val UPBEAT_GENRES = setOf("rock", "metal", "dance", "electronic", "edm", "pop", "workout", "rap", "hip hop", "house", "techno", "party")
    
    private var scope: CoroutineScope? = null
    private var contextRef: Context? = null
    private var datastoreRepository: DatastoreRepository? = null
    private var playerRef: Player? = null
    private var musicDaoRef: MusicDao? = null
    private var engagementDaoRef: com.saurav.pixelmusic.data.database.EngagementDao? = null
    // BUG 5 FIX: Called after items are added to the player so DualPlayerEngine can
    // refresh its internal queue snapshot immediately (not waiting for TIMELINE_CHANGED
    // during an active crossfade).
    private var onQueueItemsAddedCallback: (() -> Unit)? = null

    private val playerListener = object : Player.Listener {
        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            mediaItem?.let { item ->
                scope?.launch(Dispatchers.IO) {
                    val genre = try {
                        val metadataGenre = item.mediaMetadata.genre?.toString()
                        if (!metadataGenre.isNullOrBlank()) {
                            metadataGenre
                        } else {
                            val songId = item.mediaId
                            val dao = musicDaoRef
                            val longId = songId.toLongOrNull()
                            if (longId != null) {
                                dao?.getSongByIdOnce(longId)?.genre
                            } else if (songId.startsWith("youtube_")) {
                                val videoId = songId.substringAfter("youtube_")
                                val dbId = getDatabaseIdForYoutubeId(videoId)
                                dao?.getSongByIdOnce(dbId)?.genre
                            } else {
                                null
                            }
                        }
                    } catch (e: Exception) {
                        null
                    }
                    
                    if (!genre.isNullOrBlank()) {
                        synchronized(sessionPlayHistory) {
                            sessionPlayHistory.add(genre)
                            if (sessionPlayHistory.size > 5) {
                                sessionPlayHistory.removeAt(0)
                            }
                        }
                        printd("AutoQueueManager: Active session play history: $sessionPlayHistory (Mood = ${getActiveSessionMood()})")
                    }
                }
            }
            checkAndRefillQueue()
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            if (playbackState == Player.STATE_ENDED) {
                checkAndRefillQueue()
            }
        }
    }

    fun attach(
        player: Player,
        context: Context,
        datastoreRepo: DatastoreRepository,
        coroutineScope: CoroutineScope,
        musicDao: MusicDao,
        engagementDao: com.saurav.pixelmusic.data.database.EngagementDao,
        onQueueItemsAdded: (() -> Unit)? = null
    ) {
        scope = coroutineScope
        contextRef = context.applicationContext
        datastoreRepository = datastoreRepo
        playerRef = player
        musicDaoRef = musicDao
        engagementDaoRef = engagementDao
        onQueueItemsAddedCallback = onQueueItemsAdded
        player.addListener(playerListener)
        printd("AutoQueueManager attached")
    }

    fun updatePlayer(newPlayer: Player) {
        val oldPlayer = playerRef
        if (oldPlayer !== newPlayer) {
            oldPlayer?.removeListener(playerListener)
            playerRef = newPlayer
            newPlayer.addListener(playerListener)
            printd("AutoQueueManager player updated")
        }
    }

    fun detach(player: Player?) {
        player?.removeListener(playerListener)
        playerRef = null
        fetchJob?.cancel()
        scope = null
        contextRef = null
        datastoreRepository = null
        musicDaoRef = null
        engagementDaoRef = null
    }

    fun reset() {
        lastFetchedVideoId = null
        continuationToken = null
        currentWatchEndpoint = null
        synchronized(addedVideoIds) {
            addedVideoIds.clear()
        }
        synchronized(sessionPlayHistory) {
            sessionPlayHistory.clear()
        }
        fetchJob?.cancel()
        fetchJob = null
    }

    /**
     * Called when auto-queue is re-enabled (toggle OFF→ON).
     * Fully resets state and re-seeds from the currently playing song so that
     * a fresh related-songs queue is built from scratch.
     */
    fun resetAndReseedFromCurrentSong() {
        reset()
        val player = playerRef ?: return
        val currentScope = scope ?: return
        currentScope.launch(Dispatchers.IO) {
            val settings = datastoreRepository?.settings?.first() ?: return@launch
            if (!settings.autoQueueEnabled) return@launch
            val (currentId, videoId) = withContext(Dispatchers.Main) {
                val item = player.currentMediaItem ?: return@withContext null
                val mediaId = item.mediaId
                val playbackUri = item.localConfiguration?.uri?.toString()
                val metaUri = item.mediaMetadata.extras?.getString("com.saurav.pixelmusic.external.CONTENT_URI")
                val contentUri = metaUri ?: playbackUri
                val vid = when {
                    mediaId.startsWith("youtube_") -> mediaId.substringAfter("youtube_")
                    contentUri?.startsWith("youtube://") == true -> contentUri.removePrefix("youtube://")
                    else -> null
                }
                Pair(mediaId, vid)
            } ?: return@launch

            // Resolve YouTube ID for local songs via DB
            val resolvedVideoId = videoId ?: run {
                val longId = currentId.toLongOrNull()
                if (longId != null) {
                    val dbSong = musicDaoRef?.getSongByIdOnce(longId)
                    dbSong?.contentUriString?.removePrefix("youtube://")?.takeIf {
                        dbSong.contentUriString.startsWith("youtube://")
                    }
                } else null
            }

            val seedId = resolvedVideoId ?: currentId
            synchronized(addedVideoIds) { addedVideoIds.add(seedId) }
            lastFetchedVideoId = seedId

            if (resolvedVideoId != null) {
                // Online song — create a fresh endpoint and pre-fetch first batch
                val endpoint = WatchEndpoint(videoId = resolvedVideoId, playlistId = "RDAMVM$resolvedVideoId")
                currentWatchEndpoint = endpoint
                continuationToken = null
            }
            // Trigger immediate refill
            fetchJob = currentScope.launch(Dispatchers.IO) {
                refillQueueLoop(currentId, forceRefresh = false)
            }
        }
    }

    fun seed(endpoint: WatchEndpoint, continuation: String?, videoId: String) {
        lastFetchedVideoId = videoId
        continuationToken = continuation
        currentWatchEndpoint = endpoint
        synchronized(addedVideoIds) {
            addedVideoIds.clear()
            addedVideoIds.add(videoId)
        }
    }

    fun registerSkip(songId: String) {
        val cleanId = extractYtId(songId) ?: songId
        val ctx = contextRef ?: return
        try {
            val sharedPrefs = ctx.getSharedPreferences("auto_queue_skips", Context.MODE_PRIVATE)
            val currentCount = sharedPrefs.getInt(cleanId + "_skip_count", 0)
            val now = System.currentTimeMillis()
            sharedPrefs.edit()
                .putLong(cleanId + "_last_skip_time", now)
                .putInt(cleanId + "_skip_count", currentCount + 1)
                .apply()
            printd("AutoQueueManager: Registered skip for $cleanId (count = ${currentCount + 1})")
        } catch (e: Exception) {
            printe("AutoQueueManager: Error saving skip to SharedPreferences: ${e.message}")
        }
    }

    private suspend fun getActiveSkippedSongIds(): Set<String> {
        val ctx = contextRef ?: return emptySet()
        val activeIds = mutableSetOf<String>()
        try {
            val sharedPrefs = ctx.getSharedPreferences("auto_queue_skips", Context.MODE_PRIVATE)
            val allEntries = sharedPrefs.all
            val now = System.currentTimeMillis()
            val FOUR_HOURS_MS = 4 * 60 * 60 * 1000L
            val editor = sharedPrefs.edit()
            var modified = false

            // Extract all unique song IDs from keys ending with _last_skip_time
            val skippedSongs = allEntries.keys
                .filter { it.endsWith("_last_skip_time") }
                .map { it.removeSuffix("_last_skip_time") }

            for (songId in skippedSongs) {
                val lastSkipTime = sharedPrefs.getLong(songId + "_last_skip_time", 0L)
                if (now - lastSkipTime < FOUR_HOURS_MS) {
                    val skipCount = sharedPrefs.getInt(songId + "_skip_count", 0)

                    // Retrieve database playCount without runBlocking (we are already in a suspend context)
                    val playCount = try {
                        val dbSongId = if (songId.toLongOrNull() == null && !songId.startsWith("youtube_")) {
                            getDatabaseIdForYoutubeId(songId).toString()
                        } else {
                            songId
                        }
                        val p1 = engagementDaoRef?.getPlayCount(songId) ?: 0
                        val p2 = engagementDaoRef?.getPlayCount(dbSongId) ?: 0
                        kotlin.math.max(p1, p2)
                    } catch (e: Exception) {
                        0
                    }

                    if (playCount > 3 && skipCount < 2) {
                        // High-play favorite bypassed for single skip
                        printd("AutoQueueManager: Skip bypass triggered for favorite $songId (plays = $playCount, skips = $skipCount)")
                    } else {
                        activeIds.add(songId)
                    }
                } else {
                    editor.remove(songId + "_last_skip_time")
                    editor.remove(songId + "_skip_count")
                    modified = true
                }
            }
            if (modified) {
                editor.apply()
            }
        } catch (e: Exception) {
            printe("AutoQueueManager: Error reading skips from SharedPreferences: ${e.message}")
        }
        return activeIds
    }

    private fun getActiveSessionMood(): Mood {
        if (sessionPlayHistory.isEmpty()) return Mood.DEFAULT
        var chillCount = 0
        var upbeatCount = 0
        for (genre in sessionPlayHistory) {
            val norm = genre.lowercase().trim()
            if (CHILL_GENRES.any { norm.contains(it) }) chillCount++
            else if (UPBEAT_GENRES.any { norm.contains(it) }) upbeatCount++
        }
        return when {
            chillCount > upbeatCount && chillCount >= 2 -> Mood.CHILL
            upbeatCount > chillCount && upbeatCount >= 2 -> Mood.UPBEAT
            else -> Mood.DEFAULT
        }
    }


    private fun checkAndRefillQueue() {
        forceRefill(forceRefresh = false)
    }

    fun forceRefill(forceRefresh: Boolean) {
        val currentScope = scope ?: return
        val player = playerRef ?: return

        currentScope.launch(Dispatchers.IO) {
            val settings = datastoreRepository?.settings?.first() ?: return@launch
            if (!settings.autoQueueEnabled) return@launch

            val playerState = withContext(Dispatchers.Main) {
                if (playerRef == null) null
                else {
                    val currentIndex = player.currentMediaItemIndex
                    val totalCount = player.mediaItemCount
                    val remaining = totalCount - currentIndex - 1
                    val currentId = player.currentMediaItem?.mediaId
                    Triple(remaining, currentId, totalCount)
                }
            } ?: return@launch

            val (remaining, currentId, _) = playerState
            if (currentId == null) return@launch

            if (forceRefresh) {
                fetchJob?.cancel()
                // When force-refreshing, partially prune addedVideoIds to keep
                // only the current song — this prevents the set from blocking
                // all future recommendations after a toggle or manual refill.
                synchronized(addedVideoIds) {
                    val currentClean = normalizeSongId(currentId)
                    addedVideoIds.retainAll { isSameSong(it, currentClean) }
                    addedVideoIds.add(currentClean)
                }
                continuationToken = null
            } else {
                if (fetchJob?.isActive == true) return@launch
            }

            fetchJob = launch(Dispatchers.IO) {
                refillQueueLoop(currentId, forceRefresh)
            }
        }
    }

    private suspend fun getYoutubeVideoId(songId: String): String? {
        if (songId.startsWith("youtube_")) {
            return songId.substringAfter("youtube_")
        }
        if (songId.startsWith("youtube://")) {
            return songId.substringAfter("youtube://")
        }
        val cached = synchronized(localToYoutubeIdMap) {
            localToYoutubeIdMap[songId]
        }
        if (cached != null) return cached

        val longId = songId.toLongOrNull()
        if (longId != null && longId < 0) {
            val songEntity = musicDaoRef?.getSongByIdOnce(longId)
            if (songEntity?.contentUriString?.startsWith("youtube://") == true) {
                val vidId = songEntity.contentUriString.removePrefix("youtube://")
                synchronized(localToYoutubeIdMap) {
                    localToYoutubeIdMap[songId] = vidId
                }
                return vidId
            }
        }
        return null
    }

    private suspend fun getDbSongByIdString(idStr: String): Song? {
        val dao = musicDaoRef ?: return null
        val longId = idStr.toLongOrNull()
        if (longId != null) {
            return dao.getSongByIdOnce(longId)?.toSong()
        }
        if (idStr.startsWith("youtube_")) {
            val videoId = idStr.substringAfter("youtube_")
            val dbId = getDatabaseIdForYoutubeId(videoId)
            return dao.getSongByIdOnce(dbId)?.toSong()
        }
        return null
    }

    private val commonWords = setOf(
        "the", "and", "you", "for", "with", "from", "this", "that", "feat", "ft",
        "remix", "version", "original", "mix", "audio", "video", "official", "music",
        "song", "lyric", "lyrics", "acoustic", "live", "cover", "remastered", "remaster"
    )

    private fun getTitleSimilarityScore(title1: String, title2: String): Double {
        val t1 = title1.lowercase().replace(Regex("[^a-zA-Z0-9 ]"), " ")
        val t2 = title2.lowercase().replace(Regex("[^a-zA-Z0-9 ]"), " ")
        val words1 = t1.split(" ").filter { it.length > 2 && it !in commonWords }
        val words2 = t2.split(" ").filter { it.length > 2 && it !in commonWords }
        if (words1.isEmpty() || words2.isEmpty()) return 0.0
        val intersection = words1.toSet().intersect(words2.toSet())
        return intersection.size * 3.0
    }

    private fun getSongSimilarityScore(
        s1Title: String, s1Artist: String, s1Genre: String?,
        s2Title: String, s2Artist: String, s2Genre: String?
    ): Double {
        var score = 0.0
        
        // Artist similarity
        val a1 = s1Artist.lowercase().trim()
        val a2 = s2Artist.lowercase().trim()
        if (a1 == a2) {
            score += 10.0
        } else if (a1.contains(a2) || a2.contains(a1)) {
            score += 6.0
        }
        
        // Title keyword similarity
        score += getTitleSimilarityScore(s1Title, s2Title)
        
        // Genre similarity (exclude generic YouTube genre placeholders)
        val g1 = s1Genre?.lowercase()?.trim().orEmpty()
        val g2 = s2Genre?.lowercase()?.trim().orEmpty()
        if (g1.isNotEmpty() && g2.isNotEmpty() && 
            g1 != "youtube" && g1 != "youtube music" && 
            g2 != "youtube" && g2 != "youtube music") {
            if (g1 == g2) {
                score += 4.0
            } else if (g1.contains(g2) || g2.contains(g1)) {
                score += 2.0
            }
        }
        
        return score
    }

    private fun isSameSong(id1: String, id2: String): Boolean {
        if (id1 == id2) return true
        val clean1 = normalizeSongId(id1)
        val clean2 = normalizeSongId(id2)
        if (clean1 == clean2) return true

        val long1 = clean1.toLongOrNull()
        val long2 = clean2.toLongOrNull()

        // 1. YouTube DB mapping comparison
        if (long1 != null && long1 < 0) {
            val raw2 = clean2.removePrefix("youtube_").removePrefix("youtube://")
            if (raw2.length == 11 && getDatabaseIdForYoutubeId(raw2) == long1) {
                return true
            }
        }
        if (long2 != null && long2 < 0) {
            val raw1 = clean1.removePrefix("youtube_").removePrefix("youtube://")
            if (raw1.length == 11 && getDatabaseIdForYoutubeId(raw1) == long2) {
                return true
            }
        }

        // 2. Resolve cached YouTube IDs if mapped
        val (ytId1, ytId2) = synchronized(localToYoutubeIdMap) {
            Pair(
                localToYoutubeIdMap[id1] ?: localToYoutubeIdMap[clean1],
                localToYoutubeIdMap[id2] ?: localToYoutubeIdMap[clean2]
            )
        }
        if (ytId1 != null && ytId2 != null && ytId1 == ytId2) return true
        
        // If one is already a YouTube ID, check against resolved YT ID of the other
        val raw1 = clean1.removePrefix("youtube_").removePrefix("youtube://")
        val raw2 = clean2.removePrefix("youtube_").removePrefix("youtube://")
        if (raw1.length == 11 && raw1 == ytId2) return true
        if (raw2.length == 11 && raw2 == ytId1) return true

        return false
    }

    private fun normalizeSongId(id: String): String {
        return when {
            id.startsWith("youtube_") -> id.substringAfter("youtube_")
            id.startsWith("youtube://") -> id.substringAfter("youtube://")
            else -> id
        }
    }

    private suspend fun addToAddedVideoIds(songId: String) {
        val cleanId = getYoutubeVideoId(songId) ?: songId
        synchronized(addedVideoIds) {
            addedVideoIds.add(cleanId)
            if (addedVideoIds.size > MAX_HISTORY) {
                val excess = addedVideoIds.size - MAX_HISTORY
                val toRemove = addedVideoIds.take(excess)
                addedVideoIds.removeAll(toRemove.toSet())
            }
        }
    }

    private fun extractYtId(id: String): String? {
        if (id.startsWith("youtube_")) return id.substringAfter("youtube_")
        if (id.startsWith("youtube://")) return id.substringAfter("youtube://")
        val longVal = id.toLongOrNull()
        if (longVal != null) {
            return null
        }
        return id
    }

    private suspend fun getContextualFamiliarSongs(
        currentSong: SongEntity?,
        currentQueueIds: Set<String>,
        avoidIds: Set<String>
    ): List<Song> {
        val dao = musicDaoRef ?: return emptyList()
        val engagementDao = engagementDaoRef

        val favoriteSongs = try {
            dao.getFavoriteSongsList(emptyList(), false, 0).map { it.toSong() }
        } catch (e: Exception) {
            emptyList()
        }

        val playedMultipleTimesSongs = mutableListOf<Song>()
        val engagementsMap = if (engagementDao != null) {
            val engagements = try {
                engagementDao.getAllEngagements()
            } catch (e: Exception) {
                emptyList()
            }
            val idsPlayedMultiple = engagements.filter { it.playCount >= 2 }.map { it.songId }.toSet()
            for (idStr in idsPlayedMultiple) {
                val dbSong = getDbSongByIdString(idStr)
                if (dbSong != null) {
                    playedMultipleTimesSongs.add(dbSong)
                }
            }
            engagements.associateBy { it.songId }
        } else {
            emptyMap()
        }

        val combined = (favoriteSongs + playedMultipleTimesSongs).distinctBy { it.id }

        val now = System.currentTimeMillis()
        fun calculateAffinityScore(song: Song): Double {
            val songIdStr = song.id
            val rawId = extractYtId(songIdStr) ?: songIdStr
            val eng = engagementsMap[songIdStr] ?: engagementsMap[rawId]
            var score = 0.0
            if (song.isFavorite) score += 5.0
            if (eng != null) {
                val timeDiffMs = (now - eng.lastPlayedTimestamp).coerceAtLeast(0L)
                val decay = kotlin.math.exp(-DECAY_LAMBDA * timeDiffMs)
                score += eng.playCount * decay
            }
            return score
        }

        val sortedCandidates = combined.sortedByDescending { calculateAffinityScore(it) }

        val currentGenre = currentSong?.genre
        val currentArtist = currentSong?.artistName
        val currentArtistId = currentSong?.artistId

        val contextualMatches = sortedCandidates.filter { song ->
            val songIdStr = song.id
            val isAlreadyInQueue = currentQueueIds.any { isSameSong(it, songIdStr) }
            val isAvoid = avoidIds.any { isSameSong(it, songIdStr) }

            val matchesGenre = currentGenre != null && song.genre != null && 
                               song.genre.equals(currentGenre, ignoreCase = true) && 
                               !song.genre.equals("YouTube", ignoreCase = true) &&
                               !song.genre.equals("YouTube Music", ignoreCase = true)
            val matchesArtist = (currentArtist != null && song.artist.equals(currentArtist, ignoreCase = true)) || 
                                (currentArtistId != null && currentArtistId != -1L && song.artistId == currentArtistId)

            !isAlreadyInQueue && !isAvoid && (matchesGenre || matchesArtist)
        }

        return contextualMatches.take(15)
    }

    suspend fun buildMixQueue(seedSong: Song, onlineRelated: List<Song>): List<Song> {
        if (onlineRelated.isNotEmpty()) {
            return (listOf(seedSong) + onlineRelated).distinctBy { it.youtubeId ?: it.id }
        }
        val dao = musicDaoRef ?: return (listOf(seedSong) + onlineRelated).distinctBy { it.id }
        val engagementDao = engagementDaoRef
        
        val highlyRotatedIds = mutableSetOf<String>()
        val engagements = try {
            engagementDao?.getAllEngagements()
        } catch (e: Exception) {
            null
        }
        if (engagements != null) {
            for (eng in engagements) {
                if (eng.playCount > 40) {
                    highlyRotatedIds.add(eng.songId)
                }
            }
        }

        val recentlyPlayedIds = mutableSetOf<String>()
        val recents = try {
            engagementDao?.getRecentlyPlayedSongs(100)
        } catch (e: Exception) {
            null
        }
        if (recents != null) {
            for (eng in recents) {
                recentlyPlayedIds.add(eng.songId)
            }
        }

        val settings = datastoreRepository?.settings?.first() ?: return (listOf(seedSong) + onlineRelated).distinctBy { it.id }
        val activeSkips = getActiveSkippedSongIds()
        val avoidIds = if (settings.avoidRepetitiveSongs) {
            highlyRotatedIds + recentlyPlayedIds + activeSkips
        } else {
            highlyRotatedIds + activeSkips
        }

        // Batch resolve title & artist keys for all avoid IDs to ensure strict title/artist deduplication!
        val avoidLongIds = avoidIds.mapNotNull { id ->
            id.toLongOrNull() ?: getDatabaseIdForYoutubeId(normalizeSongId(id))
        }
        val avoidSongs = if (avoidLongIds.isNotEmpty()) {
            dao.getSongsByIdsListSimple(avoidLongIds)
        } else {
            emptyList()
        }
        val avoidKeys = avoidSongs.mapNotNull { s ->
            val title = s.title.lowercase().trim()
            val artist = s.artistName.lowercase().trim()
            if (title.isNotEmpty() && artist.isNotEmpty()) "$title|$artist" else null
        }.toSet()

        val seedTitleKey = seedSong.title.lowercase().trim()
        val seedArtistKey = seedSong.artist.lowercase().trim()
        val currentQueueKeys = if (seedTitleKey.isNotEmpty() && seedArtistKey.isNotEmpty()) {
            setOf("$seedTitleKey|$seedArtistKey")
        } else {
            emptySet()
        }
        val currentQueueIds = setOf(seedSong.id)

        // 1. Resolve current playing song information
        val seedLongId = seedSong.id.toLongOrNull()
        val currentSongEntity = if (seedLongId != null) dao.getSongByIdOnce(seedLongId) else null
        
        var resolvedGenre = currentSongEntity?.genre ?: seedSong.genre
        if (resolvedGenre.isNullOrBlank() && seedSong.title.isNotBlank() && seedSong.artist.isNotBlank()) {
            val dbMatch = dao.getSongsByArtistName(seedSong.artist, 1).firstOrNull()
            if (dbMatch != null) {
                resolvedGenre = dbMatch.genre
            }
        }

        // 2. Related candidates from online/offline
        val resolvedVideoId = seedSong.youtubeId ?: seedSong.id.substringAfter("youtube_")
        
        val discovered = if (onlineRelated.isNotEmpty()) {
            onlineRelated
        } else {
            fetchLocalRelated(seedSong.id, currentQueueIds)
        }

        // 3. Extract same-artist and same-genre local pools
        val sameArtistSongs = if (currentSongEntity?.artistName != null || seedSong.artist.isNotBlank()) {
            val artistToQuery = currentSongEntity?.artistName ?: seedSong.artist
            dao.getSongsByArtistName(artistToQuery, 20).map { it.toSong() }
        } else {
            emptyList()
        }

        val sameGenreSongs = if (!resolvedGenre.isNullOrBlank() && !resolvedGenre.equals("YouTube", ignoreCase = true)) {
            dao.getSongsByGenre(resolvedGenre, seedLongId ?: 0L, 50).map { it.toSong() }
        } else {
            emptyList()
        }

        // 4. Extract familiar contextual songs (favorites or playCount >= 2 matching genre/artist)
        val familiarSongs = getContextualFamiliarSongs(currentSongEntity, currentQueueIds, avoidIds)

        // 5. Interleave pools with strict capping (max 2 per artist)
        val finalSongsToAdd = mutableListOf<Song>()
        finalSongsToAdd.add(seedSong) // seed song is first!

        val addedArtists = mutableMapOf<String, Int>()
        addedArtists[seedSong.artist.lowercase().trim()] = 1

        val addedKeys = mutableSetOf<String>()
        if (seedTitleKey.isNotEmpty() && seedArtistKey.isNotEmpty()) {
            addedKeys.add("$seedTitleKey|$seedArtistKey")
        }

        // Helper to check artist limits and session mood to ensure acoustic consistency & diversity
        val activeMood = getActiveSessionMood()
        fun canAddSong(song: Song): Boolean {
            val songIdStr = song.id
            val isInQueue = currentQueueIds.any { isSameSong(it, songIdStr) }
            val isAvoid = avoidIds.any { isSameSong(it, songIdStr) }
            val isAlreadyAdded = finalSongsToAdd.any { isSameSong(it.id, songIdStr) }
            if (isInQueue || isAvoid || isAlreadyAdded) return false

            // Deduplicate by Title + Artist to prevent duplicates (e.g. local copy vs youtube copy)
            val cleanTitle = song.title.lowercase().trim()
            val cleanArtist = song.artist.lowercase().trim()
            if (cleanTitle.isNotEmpty() && cleanArtist.isNotEmpty()) {
                val key = "$cleanTitle|$cleanArtist"
                if (currentQueueKeys.contains(key) || avoidKeys.contains(key) || addedKeys.contains(key)) {
                    return false
                }
            }

            // Active Mood Protection
            val songGenre = song.genre?.lowercase()?.trim().orEmpty()
            if (activeMood == Mood.CHILL) {
                if (UPBEAT_GENRES.any { songGenre.contains(it) }) return false
            } else if (activeMood == Mood.UPBEAT) {
                if (CHILL_GENRES.any { songGenre.contains(it) }) return false
            }

            val artistKey = song.artist.lowercase().trim()
            val artistCount = addedArtists[artistKey] ?: 0
            return artistCount < 2 // Max 2 songs per artist in the added batch
        }

        // Separate same-genre into popular and discovery (playCount = 0)
        val discoveryCandidates = sameGenreSongs.filter { song ->
            val playCount = engagementDao?.getPlayCount(song.id) ?: 0
            playCount == 0 && !song.isFavorite
        }.filter { canAddSong(it) }.shuffled().toMutableList()

        val popularGenreCandidates = sameGenreSongs.filter { song ->
            val playCount = engagementDao?.getPlayCount(song.id) ?: 0
            playCount > 0 || song.isFavorite
        }.filter { canAddSong(it) }.shuffled().toMutableList()

        val sameArtistCandidates = sameArtistSongs.filter { canAddSong(it) }.shuffled().toMutableList()
        val relatedCandidates = discovered.filter { canAddSong(it) }.toMutableList()
        val familiarCandidates = familiarSongs.filter { canAddSong(it) }.toMutableList()

        var addedCount = 1 // we added the seed song
        val targetBatchSize = 50 // Mix of 50 songs

        while (addedCount < targetBatchSize) {
            var addedInThisRound = false

            // 1. YouTube / Local Related gets HIGHEST PRIORITY (First Priority - Vibe Match)
            // We pull up to 2 related songs to anchor the vibe
            for (i in 0 until 2) {
                if (relatedCandidates.isNotEmpty()) {
                    val s = relatedCandidates.removeAt(0)
                    if (canAddSong(s)) {
                        finalSongsToAdd.add(s)
                        addedArtists[s.artist.lowercase().trim()] = (addedArtists[s.artist.lowercase().trim()] ?: 0) + 1
                        val cleanTitle = s.title.lowercase().trim()
                        val cleanArtist = s.artist.lowercase().trim()
                        if (cleanTitle.isNotEmpty() && cleanArtist.isNotEmpty()) {
                            addedKeys.add("$cleanTitle|$cleanArtist")
                        }
                        addedCount++
                        addedInThisRound = true
                    }
                }
            }

            if (addedCount >= targetBatchSize) break

            // 2. Discovery Candidates (Never played before - New Discoveries)
            for (i in 0 until 2) {
                if (discoveryCandidates.isNotEmpty()) {
                    val s = discoveryCandidates.removeAt(0)
                    if (canAddSong(s)) {
                        finalSongsToAdd.add(s)
                        addedArtists[s.artist.lowercase().trim()] = (addedArtists[s.artist.lowercase().trim()] ?: 0) + 1
                        val cleanTitle = s.title.lowercase().trim()
                        val cleanArtist = s.artist.lowercase().trim()
                        if (cleanTitle.isNotEmpty() && cleanArtist.isNotEmpty()) {
                            addedKeys.add("$cleanTitle|$cleanArtist")
                        }
                        addedCount++
                        addedInThisRound = true
                    }
                }
            }

            if (addedCount >= targetBatchSize) break

            // 3. Same Artist / Vibe Exploration
            if (sameArtistCandidates.isNotEmpty()) {
                val s = sameArtistCandidates.removeAt(0)
                if (canAddSong(s)) {
                    finalSongsToAdd.add(s)
                    addedArtists[s.artist.lowercase().trim()] = (addedArtists[s.artist.lowercase().trim()] ?: 0) + 1
                    val cleanTitle = s.title.lowercase().trim()
                    val cleanArtist = s.artist.lowercase().trim()
                    if (cleanTitle.isNotEmpty() && cleanArtist.isNotEmpty()) {
                        addedKeys.add("$cleanTitle|$cleanArtist")
                    }
                    addedCount++
                    addedInThisRound = true
                }
            }

            if (addedCount >= targetBatchSize) break

            // 4. Same Genre Popular Exploration
            if (popularGenreCandidates.isNotEmpty()) {
                val s = popularGenreCandidates.removeAt(0)
                if (canAddSong(s)) {
                    finalSongsToAdd.add(s)
                    addedArtists[s.artist.lowercase().trim()] = (addedArtists[s.artist.lowercase().trim()] ?: 0) + 1
                    val cleanTitle = s.title.lowercase().trim()
                    val cleanArtist = s.artist.lowercase().trim()
                    if (cleanTitle.isNotEmpty() && cleanArtist.isNotEmpty()) {
                        addedKeys.add("$cleanTitle|$cleanArtist")
                    }
                    addedCount++
                    addedInThisRound = true
                }
            }

            if (addedCount >= targetBatchSize) break

            // 5. Familiar Contextual (Favorites/Popular matching context - lower priority)
            if (familiarCandidates.isNotEmpty()) {
                val s = familiarCandidates.removeAt(0)
                if (canAddSong(s)) {
                    finalSongsToAdd.add(s)
                    addedArtists[s.artist.lowercase().trim()] = (addedArtists[s.artist.lowercase().trim()] ?: 0) + 1
                    val cleanTitle = s.title.lowercase().trim()
                    val cleanArtist = s.artist.lowercase().trim()
                    if (cleanTitle.isNotEmpty() && cleanArtist.isNotEmpty()) {
                        addedKeys.add("$cleanTitle|$cleanArtist")
                    }
                    addedCount++
                    addedInThisRound = true
                }
            }

            if (!addedInThisRound) break
        }

        // Fallback: If we couldn't build at least targetBatchSize songs, relax constraints
        if (finalSongsToAdd.size < targetBatchSize) {
            val remainingCandidates = (discovered + sameGenreSongs + familiarSongs).distinctBy { it.id }
            for (s in remainingCandidates) {
                val songIdStr = s.id
                val isInQueue = currentQueueIds.any { isSameSong(it, songIdStr) }
                val isAlreadyAdded = finalSongsToAdd.any { isSameSong(it.id, songIdStr) }
                val isAvoid = avoidIds.any { isSameSong(it, songIdStr) }
                if (!isInQueue && !isAlreadyAdded && !isAvoid) {
                    val cleanTitle = s.title.lowercase().trim()
                    val cleanArtist = s.artist.lowercase().trim()
                    val isDuplicateTitleArtist = cleanTitle.isNotEmpty() && cleanArtist.isNotEmpty() && 
                        (currentQueueKeys.contains("$cleanTitle|$cleanArtist") || avoidKeys.contains("$cleanTitle|$cleanArtist") || addedKeys.contains("$cleanTitle|$cleanArtist"))
                    
                    if (!isDuplicateTitleArtist) {
                        finalSongsToAdd.add(s)
                        if (cleanTitle.isNotEmpty() && cleanArtist.isNotEmpty()) {
                            addedKeys.add("$cleanTitle|$cleanArtist")
                        }
                        if (finalSongsToAdd.size >= targetBatchSize) break
                    }
                }
            }
        }

        return finalSongsToAdd
    }

    private suspend fun refillQueueLoop(currentId: String, forceRefresh: Boolean) {
        val player = playerRef ?: return
        val dao = musicDaoRef ?: return
        val context = contextRef ?: return
        val engagementDao = engagementDaoRef

        val entryPoint = try {
            dagger.hilt.android.EntryPointAccessors.fromApplication(
                context,
                YoutubeHelperEntryPoint::class.java
            )
        } catch (e: Exception) {
            null
        }
        val connectivityStateHolder = entryPoint?.connectivityStateHolder()
        connectivityStateHolder?.initialize()

        // 1. Identify if current song is YouTube or local/offline
        val currentMediaItem = withContext(Dispatchers.Main) { player.currentMediaItem }
        val playbackUriStr = currentMediaItem?.localConfiguration?.uri?.toString()
        val metadataUriStr = currentMediaItem?.mediaMetadata?.extras?.getString("com.saurav.pixelmusic.external.CONTENT_URI")
        val contentUriStr = metadataUriStr ?: playbackUriStr

        var rawVideoId: String? = if (currentId.startsWith("youtube_")) {
            currentId.substringAfter("youtube_")
        } else if (contentUriStr?.startsWith("youtube://") == true) {
            contentUriStr.removePrefix("youtube://")
        } else {
            null
        }

        if (rawVideoId == null) {
            val songId = currentId.toLongOrNull()
            if (songId != null) {
                val dbSong = dao.getSongByIdOnce(songId)
                if (dbSong?.contentUriString?.startsWith("youtube://") == true) {
                    rawVideoId = dbSong.contentUriString.removePrefix("youtube://")
                }
            }
        }
        val isLocal = rawVideoId == null
        val resolvedVideoId = rawVideoId ?: ""

        // NOTE: On forceRefresh, forceRefill() already pruned addedVideoIds and cleared
        // continuationToken. We just update lastFetchedVideoId and set currentWatchEndpoint
        // only if it was not already set by resetAndReseedFromCurrentSong().
        val activeId = if (isLocal) currentId else resolvedVideoId
        if (forceRefresh) {
            lastFetchedVideoId = activeId
            // Only reset endpoint if not already seeded by resetAndReseedFromCurrentSong()
            if (currentWatchEndpoint == null && !isLocal && resolvedVideoId.isNotBlank()) {
                currentWatchEndpoint = WatchEndpoint(videoId = resolvedVideoId, playlistId = "RDAMVM$resolvedVideoId")
            }
            synchronized(addedVideoIds) {
                // Add current song — forceRefill() already pruned the set
                addedVideoIds.add(activeId)
            }
        } else {
            if (lastFetchedVideoId == null) {
                lastFetchedVideoId = activeId
                synchronized(addedVideoIds) {
                    addedVideoIds.add(activeId)
                }
            }
        }

        var loopCount = 0
        var emptyFetchCount = 0 // Count of consecutive loop iterations that added 0 songs
        while (true) {
            val playerState = withContext(Dispatchers.Main) {
                if (playerRef == null) null
                else {
                    val currentIndex = player.currentMediaItemIndex
                    val totalCount = player.mediaItemCount
                    val remaining = totalCount - currentIndex - 1
                    Pair(remaining, totalCount)
                }
            } ?: break

            val (remaining, totalCount) = playerState
            if (remaining >= TARGET_QUEUE_SIZE) {
                printd("AutoQueueManager: Queue is full. Current remaining: $remaining (>= $TARGET_QUEUE_SIZE)")
                break
            }

            // Hard cap on total iterations; also break after 3 consecutive empty fetches
            if (loopCount >= 15 || emptyFetchCount >= 3) {
                printd("AutoQueueManager: Breaking — loopCount=$loopCount, emptyFetchCount=$emptyFetchCount")
                break
            }
            loopCount++

            printd("AutoQueueManager: Refilling queue. Remaining: $remaining, Target: $TARGET_QUEUE_SIZE, Loop: $loopCount")

            val currentQueueIds = withContext(Dispatchers.Main) {
                if (playerRef == null) emptySet()
                else (0 until player.mediaItemCount).mapNotNull { player.getMediaItemAt(it).mediaId }.toSet()
            }

            val highlyRotatedIds = mutableSetOf<String>()
            val engagements = try {
                engagementDao?.getAllEngagements()
            } catch (e: Exception) {
                null
            }
            if (engagements != null) {
                for (eng in engagements) {
                    if (eng.playCount > 40) {
                        highlyRotatedIds.add(eng.songId)
                    }
                }
            }

            val recentlyPlayedIds = mutableSetOf<String>()
            val recents = try {
                engagementDao?.getRecentlyPlayedSongs(100)
            } catch (e: Exception) {
                null
            }
            if (recents != null) {
                for (eng in recents) {
                    recentlyPlayedIds.add(eng.songId)
                }
            }

            val settings = datastoreRepository?.settings?.first() ?: return
            val activeSkips = getActiveSkippedSongIds()
            val avoidIds = if (settings.avoidRepetitiveSongs) {
                highlyRotatedIds + recentlyPlayedIds + activeSkips
            } else {
                highlyRotatedIds + activeSkips
            }

            // Batch resolve title & artist keys for all avoid IDs to ensure strict title/artist deduplication!
            val avoidLongIds = avoidIds.mapNotNull { id ->
                id.toLongOrNull() ?: getDatabaseIdForYoutubeId(normalizeSongId(id))
            }
            val avoidSongs = if (avoidLongIds.isNotEmpty()) {
                dao.getSongsByIdsListSimple(avoidLongIds)
            } else {
                emptyList()
            }
            val avoidKeys = avoidSongs.mapNotNull { s ->
                val title = s.title.lowercase().trim()
                val artist = s.artistName.lowercase().trim()
                if (title.isNotEmpty() && artist.isNotEmpty()) "$title|$artist" else null
            }.toSet()

            val currentQueueKeys = withContext(Dispatchers.Main) {
                if (playerRef == null) emptySet()
                else (0 until player.mediaItemCount).mapNotNull { index ->
                    val item = player.getMediaItemAt(index)
                    val title = item.mediaMetadata.title?.toString()?.lowercase()?.trim() ?: ""
                    val artist = item.mediaMetadata.artist?.toString()?.lowercase()?.trim() ?: ""
                    if (title.isNotEmpty() && artist.isNotEmpty()) "$title|$artist" else null
                }.toSet()
            }

            val songsToAdd = mutableListOf<Song>()

            // 1. Resolve current playing song information
            val currentSongLongId = currentId.toLongOrNull()
            val currentSongEntity = if (currentSongLongId != null) dao.getSongByIdOnce(currentSongLongId) else null
            
            val currentMediaItem = withContext(Dispatchers.Main) { player.currentMediaItem }
            val currentTitle = currentMediaItem?.mediaMetadata?.title?.toString().orEmpty()
            val currentArtist = currentMediaItem?.mediaMetadata?.artist?.toString().orEmpty()
            
            var resolvedGenre = currentSongEntity?.genre
            if (resolvedGenre.isNullOrBlank() && currentTitle.isNotBlank() && currentArtist.isNotBlank()) {
                val dbMatch = dao.getSongsByArtistName(currentArtist, 1).firstOrNull()
                if (dbMatch != null) {
                    resolvedGenre = dbMatch.genre
                }
            }

            // 2. Discover related tracks (first priority is online YouTube Music for online tracks, and local related for offline tracks)
            val isOnline = connectivityStateHolder?.isOnline?.value ?: true
            var discovered = emptyList<Song>()
            
            if (isOnline && !isLocal && resolvedVideoId.isNotBlank()) {
                val related = fetchOnlineRelated(resolvedVideoId)
                if (related.isNotEmpty()) {
                    saveRelatedSongsToDb(resolvedVideoId, related, player)
                    discovered = related
                    val mediaItems = related.map { MediaItemBuilder.build(it) }
                    withContext(Dispatchers.Main) {
                        player.addMediaItems(mediaItems)
                        onQueueItemsAddedCallback?.invoke()
                    }
                    printd("AutoQueueManager: Appended ${mediaItems.size} online mix radio songs directly.")
                    continue
                } else {
                    discovered = fetchLocalRelated(currentId, currentQueueIds)
                }
            } else {
                discovered = fetchLocalRelated(currentId, currentQueueIds)
            }

            // 3. Extract same-artist and same-genre local pools
            val sameArtistSongs = if (currentSongEntity?.artistName != null || currentArtist.isNotBlank()) {
                val artistToQuery = currentSongEntity?.artistName ?: currentArtist
                dao.getSongsByArtistName(artistToQuery, 20).map { it.toSong() }
            } else {
                emptyList()
            }

            val sameGenreSongs = if (!resolvedGenre.isNullOrBlank() && !resolvedGenre.equals("YouTube", ignoreCase = true)) {
                dao.getSongsByGenre(resolvedGenre, currentSongLongId ?: 0L, 50).map { it.toSong() }
            } else {
                emptyList()
            }

            // 4. Extract familiar contextual songs (favorites or playCount >= 2 matching genre/artist)
            val familiarSongs = getContextualFamiliarSongs(currentSongEntity, currentQueueIds, avoidIds)

            // 5. Interleave pools with strict capping (max 2 per artist)
            val finalSongsToAdd = mutableListOf<Song>()
            val addedArtists = mutableMapOf<String, Int>()
            val addedKeys = mutableSetOf<String>()

            // Helper to check artist limits and session mood to ensure acoustic consistency & diversity
            val activeMood = getActiveSessionMood()
            fun canAddSong(song: Song): Boolean {
                val songIdStr = song.id
                val isInQueue = currentQueueIds.any { isSameSong(it, songIdStr) }
                val isAvoid = avoidIds.any { isSameSong(it, songIdStr) }
                val isAlreadyAdded = finalSongsToAdd.any { isSameSong(it.id, songIdStr) }
                val isAlreadyInAddedVideoIds = synchronized(addedVideoIds) {
                    addedVideoIds.any { isSameSong(it, songIdStr) }
                }
                if (isInQueue || isAvoid || isAlreadyAdded || isAlreadyInAddedVideoIds) return false

                // Deduplicate by Title + Artist to prevent duplicates (e.g. local copy vs youtube copy)
                val cleanTitle = song.title.lowercase().trim()
                val cleanArtist = song.artist.lowercase().trim()
                if (cleanTitle.isNotEmpty() && cleanArtist.isNotEmpty()) {
                    val key = "$cleanTitle|$cleanArtist"
                    if (currentQueueKeys.contains(key) || avoidKeys.contains(key) || addedKeys.contains(key)) {
                        return false
                    }
                }

                // Active Mood Protection
                val songGenre = song.genre?.lowercase()?.trim().orEmpty()
                if (activeMood == Mood.CHILL) {
                    if (UPBEAT_GENRES.any { songGenre.contains(it) }) return false
                } else if (activeMood == Mood.UPBEAT) {
                    if (CHILL_GENRES.any { songGenre.contains(it) }) return false
                }

                val artistKey = song.artist.lowercase().trim()
                val artistCount = addedArtists[artistKey] ?: 0
                return artistCount < 2 // Max 2 songs per artist in the added batch
            }

            // Separate same-genre into popular and discovery (playCount = 0)
            val discoveryCandidates = sameGenreSongs.filter { song ->
                val playCount = engagementDao?.getPlayCount(song.id) ?: 0
                playCount == 0 && !song.isFavorite
            }.filter { canAddSong(it) }.shuffled().toMutableList()

            val popularGenreCandidates = sameGenreSongs.filter { song ->
                val playCount = engagementDao?.getPlayCount(song.id) ?: 0
                playCount > 0 || song.isFavorite
            }.filter { canAddSong(it) }.shuffled().toMutableList()

            val sameArtistCandidates = sameArtistSongs.filter { canAddSong(it) }.shuffled().toMutableList()
            val relatedCandidates = discovered.filter { canAddSong(it) }.toMutableList()
            val familiarCandidates = familiarSongs.filter { canAddSong(it) }.toMutableList()

            var addedCount = 0
            val targetBatchSize = 12

            while (addedCount < targetBatchSize) {
                var addedInThisRound = false

                // 1. YouTube / Local Related gets HIGHEST PRIORITY (First Priority - Vibe Match)
                // We pull up to 2 related songs to anchor the vibe
                for (i in 0 until 2) {
                    if (relatedCandidates.isNotEmpty()) {
                        val s = relatedCandidates.removeAt(0)
                        if (canAddSong(s)) {
                            finalSongsToAdd.add(s)
                            addToAddedVideoIds(s.id)
                            addedArtists[s.artist.lowercase().trim()] = (addedArtists[s.artist.lowercase().trim()] ?: 0) + 1
                            val cleanTitle = s.title.lowercase().trim()
                            val cleanArtist = s.artist.lowercase().trim()
                            if (cleanTitle.isNotEmpty() && cleanArtist.isNotEmpty()) {
                                addedKeys.add("$cleanTitle|$cleanArtist")
                            }
                            addedCount++
                            addedInThisRound = true
                        }
                    }
                }

                if (addedCount >= targetBatchSize) break

                // 2. Discovery Candidates (Never played before - New Discoveries)
                // We pull up to 2 songs to encourage exploration of new music
                for (i in 0 until 2) {
                    if (discoveryCandidates.isNotEmpty()) {
                        val s = discoveryCandidates.removeAt(0)
                        if (canAddSong(s)) {
                            finalSongsToAdd.add(s)
                            addToAddedVideoIds(s.id)
                            addedArtists[s.artist.lowercase().trim()] = (addedArtists[s.artist.lowercase().trim()] ?: 0) + 1
                            val cleanTitle = s.title.lowercase().trim()
                            val cleanArtist = s.artist.lowercase().trim()
                            if (cleanTitle.isNotEmpty() && cleanArtist.isNotEmpty()) {
                                addedKeys.add("$cleanTitle|$cleanArtist")
                            }
                            addedCount++
                            addedInThisRound = true
                        }
                    }
                }

                if (addedCount >= targetBatchSize) break

                // 3. Same Artist / Vibe Exploration
                if (sameArtistCandidates.isNotEmpty()) {
                    val s = sameArtistCandidates.removeAt(0)
                    if (canAddSong(s)) {
                        finalSongsToAdd.add(s)
                        addToAddedVideoIds(s.id)
                        addedArtists[s.artist.lowercase().trim()] = (addedArtists[s.artist.lowercase().trim()] ?: 0) + 1
                        val cleanTitle = s.title.lowercase().trim()
                        val cleanArtist = s.artist.lowercase().trim()
                        if (cleanTitle.isNotEmpty() && cleanArtist.isNotEmpty()) {
                            addedKeys.add("$cleanTitle|$cleanArtist")
                        }
                        addedCount++
                        addedInThisRound = true
                    }
                }

                if (addedCount >= targetBatchSize) break

                // 4. Same Genre Popular Exploration
                if (popularGenreCandidates.isNotEmpty()) {
                    val s = popularGenreCandidates.removeAt(0)
                    if (canAddSong(s)) {
                        finalSongsToAdd.add(s)
                        addToAddedVideoIds(s.id)
                        addedArtists[s.artist.lowercase().trim()] = (addedArtists[s.artist.lowercase().trim()] ?: 0) + 1
                        val cleanTitle = s.title.lowercase().trim()
                        val cleanArtist = s.artist.lowercase().trim()
                        if (cleanTitle.isNotEmpty() && cleanArtist.isNotEmpty()) {
                            addedKeys.add("$cleanTitle|$cleanArtist")
                        }
                        addedCount++
                        addedInThisRound = true
                    }
                }

                if (addedCount >= targetBatchSize) break

                // 5. Familiar Contextual (Favorites/Popular matching context - lower priority)
                if (familiarCandidates.isNotEmpty()) {
                    val s = familiarCandidates.removeAt(0)
                    if (canAddSong(s)) {
                        finalSongsToAdd.add(s)
                        addToAddedVideoIds(s.id)
                        addedArtists[s.artist.lowercase().trim()] = (addedArtists[s.artist.lowercase().trim()] ?: 0) + 1
                        val cleanTitle = s.title.lowercase().trim()
                        val cleanArtist = s.artist.lowercase().trim()
                        if (cleanTitle.isNotEmpty() && cleanArtist.isNotEmpty()) {
                            addedKeys.add("$cleanTitle|$cleanArtist")
                        }
                        addedCount++
                        addedInThisRound = true
                    }
                }

                if (!addedInThisRound) break
            }

            // Fallback: If we couldn't build at least 6 songs due to strict limits, relax constraints but STILL enforce avoidIds, Title/Artist duplicates, and queue checks!
            if (finalSongsToAdd.size < 6) {
                val remainingCandidates = (discovered + sameGenreSongs + familiarSongs).distinctBy { it.id }
                for (s in remainingCandidates) {
                    val songIdStr = s.id
                    val isInQueue = currentQueueIds.any { isSameSong(it, songIdStr) }
                    val isAlreadyAdded = finalSongsToAdd.any { isSameSong(it.id, songIdStr) }
                    val isAvoid = avoidIds.any { isSameSong(it, songIdStr) }
                    val isAlreadyInAddedVideoIds = synchronized(addedVideoIds) {
                        addedVideoIds.any { isSameSong(it, songIdStr) }
                    }
                    if (!isInQueue && !isAlreadyAdded && !isAvoid && !isAlreadyInAddedVideoIds) {
                        val cleanTitle = s.title.lowercase().trim()
                        val cleanArtist = s.artist.lowercase().trim()
                        val isDuplicateTitleArtist = cleanTitle.isNotEmpty() && cleanArtist.isNotEmpty() && 
                            (currentQueueKeys.contains("$cleanTitle|$cleanArtist") || avoidKeys.contains("$cleanTitle|$cleanArtist") || addedKeys.contains("$cleanTitle|$cleanArtist"))
                        
                        if (!isDuplicateTitleArtist) {
                            finalSongsToAdd.add(s)
                            addToAddedVideoIds(s.id)
                            if (cleanTitle.isNotEmpty() && cleanArtist.isNotEmpty()) {
                                addedKeys.add("$cleanTitle|$cleanArtist")
                            }
                            if (finalSongsToAdd.size >= 8) break
                        }
                    }
                }
            }

            if (finalSongsToAdd.isEmpty()) {
                printd("AutoQueueManager: No songs to add this loop — emptyFetchCount=$emptyFetchCount")
                emptyFetchCount++
                continue // Count empty loops; break handled at loop top
            }
            emptyFetchCount = 0 // Reset on successful add

            val mediaItems = finalSongsToAdd.map { MediaItemBuilder.build(it) }
            withContext(Dispatchers.Main) {
                player.addMediaItems(mediaItems)
                // Notify the engine to refresh its queue snapshot immediately.
                onQueueItemsAddedCallback?.invoke()
            }
            printd("AutoQueueManager: Appended ${mediaItems.size} songs to queue")
        }
    }

    private suspend fun fetchOnlineRelated(videoId: String): List<Song> {
        try {
            val endpoint = currentWatchEndpoint ?: WatchEndpoint(videoId = videoId, playlistId = "RDAMVM$videoId")
            val result = YouTube.next(endpoint = endpoint, continuation = continuationToken, followAutomixPreview = true)
            
            var fetchedSongs = emptyList<Song>()
            result.onSuccess { nextResult ->
                continuationToken = nextResult.continuation
                currentWatchEndpoint = nextResult.endpoint
                
                val addedVideoIdsLocal = synchronized(addedVideoIds) {
                    addedVideoIds.toSet()
                }
                val filteredItems = nextResult.items
                    .filter { it.id !in addedVideoIdsLocal }

                if (filteredItems.isEmpty()) {
                    // All items in this continuation batch are already added.
                    // If we also have no continuation left, reset addedVideoIds
                    // (keeping only current song) and try a fresh endpoint so we
                    // don't get permanently stuck returning 0 songs.
                    if (nextResult.continuation == null) {
                        printd("AutoQueueManager: Continuation exhausted and all items filtered — resetting addedVideoIds for fresh fetch")
                        // Compute retained set without holding the lock during isSameSong evaluation
                        val retainedSet = synchronized(addedVideoIds) {
                            addedVideoIds.filter { isSameSong(it, videoId) }.toMutableSet()
                        }
                        retainedSet.add(videoId)
                        synchronized(addedVideoIds) {
                            addedVideoIds.clear()
                            addedVideoIds.addAll(retainedSet)
                        }
                        continuationToken = null
                        currentWatchEndpoint = WatchEndpoint(videoId = videoId, playlistId = "RDAMVM$videoId")
                    } else {
                        // More continuation available — just return empty to try next page
                        printd("AutoQueueManager: All fetched items already added, will try next continuation")
                    }
                    return@onSuccess
                }

                // Add each to tracking set — must use loop, not forEach, because addToAddedVideoIds is suspend
                for (item in filteredItems) {
                    addToAddedVideoIds(item.id)
                }
                fetchedSongs = filteredItems.map { it.toNativeSong() }
            }.onFailure { e ->
                printe("AutoQueueManager: Failed to fetch related online: ${e.message}")
                // On network failure, reset endpoint to allow retry next time
                continuationToken = null
                currentWatchEndpoint = null
            }
            return fetchedSongs
        } catch (e: Exception) {
            printe("AutoQueueManager: Exception fetching online related songs: ${e.message}")
            continuationToken = null
            currentWatchEndpoint = null
            return emptyList()
        }
    }

    private suspend fun fetchLocalRelated(songIdStr: String, currentQueueIds: Set<String>): List<Song> {
        val dao = musicDaoRef ?: return emptyList()
        val engagementDao = engagementDaoRef
        try {
            val songId = songIdStr.toLongOrNull()
            val currentSong = if (songId != null) dao.getSongByIdOnce(songId) else null
            
            var filtered = emptyList<SongEntity>()

            val engagementsMap = if (engagementDao != null) {
                try {
                    engagementDao.getAllEngagements().associateBy { it.songId }
                } catch (e: Exception) {
                    emptyMap()
                }
            } else {
                emptyMap()
            }
            val now = System.currentTimeMillis()

            if (currentSong != null) {
                val relatedEntities = dao.getLocalRelatedSongs(
                    songId = currentSong.id,
                    artistId = currentSong.artistId,
                    albumId = currentSong.albumId,
                    genre = currentSong.genre,
                    limit = 60
                )

                val mappedRelatedIds = try {
                    dao.getRelatedSongs(currentSong.id, 100).map { it.id }.toSet()
                } catch (e: Exception) {
                    emptySet()
                }

                fun calculateRelevanceAndDecayScore(entity: SongEntity): Double {
                    var relevance = getSongSimilarityScore(
                        currentSong.title, currentSong.artistName, currentSong.genre,
                        entity.title, entity.artistName, entity.genre
                    )
                    if (entity.albumId == currentSong.albumId) relevance += 5.0
                    if (mappedRelatedIds.contains(entity.id)) relevance += 20.0

                    val songIdStr = entity.id.toString()
                    val rawId = extractYtId(songIdStr) ?: songIdStr
                    val eng = engagementsMap[songIdStr] ?: engagementsMap[rawId]
                    
                    var popularityScore = 0.0
                    if (entity.isFavorite) popularityScore += 2.0
                    if (eng != null) {
                        val timeDiffMs = (now - eng.lastPlayedTimestamp).coerceAtLeast(0L)
                        val decay = kotlin.math.exp(-DECAY_LAMBDA * timeDiffMs)
                        popularityScore += (eng.playCount.coerceAtMost(10) * 0.5) * decay
                    }
                    return relevance + popularityScore
                }

                val sortedRelated = relatedEntities.sortedByDescending { calculateRelevanceAndDecayScore(it) }

                filtered = sortedRelated.filter { entity ->
                    val entityIdStr = entity.id.toString()
                    val isInQueue = currentQueueIds.any { isSameSong(it, entityIdStr) }
                    val isAlreadyAdded = synchronized(addedVideoIds) {
                        addedVideoIds.any { isSameSong(it, entityIdStr) }
                    }
                    !isInQueue && !isAlreadyAdded
                }
            }
            
            // Scarce local related songs fallback improvement!
            if (filtered.size < 12 && currentSong != null) {
                val artistSongs = dao.getSongsByArtistName(currentSong.artistName, 30)
                val genreSongs = if (!currentSong.genre.isNullOrBlank() && !currentSong.genre.equals("YouTube", ignoreCase = true)) {
                    dao.getSongsByGenre(currentSong.genre, currentSong.id, 30)
                } else {
                    emptyList()
                }
                
                val allLocalSongs = dao.getAllSongsList()
                val fallbackCandidates = (artistSongs + genreSongs + allLocalSongs).distinctBy { it.id }
                
                fun calculateFallbackScore(entity: SongEntity): Double {
                    var relevance = getSongSimilarityScore(
                        currentSong.title, currentSong.artistName, currentSong.genre,
                        entity.title, entity.artistName, entity.genre
                    )
                    
                    val songIdStr = entity.id.toString()
                    val rawId = extractYtId(songIdStr) ?: songIdStr
                    val eng = engagementsMap[songIdStr] ?: engagementsMap[rawId]
                    
                    var popularityScore = 0.0
                    if (entity.isFavorite) popularityScore += 2.0
                    if (eng != null) {
                        val timeDiffMs = (now - eng.lastPlayedTimestamp).coerceAtLeast(0L)
                        val decay = kotlin.math.exp(-DECAY_LAMBDA * timeDiffMs)
                        popularityScore += (eng.playCount.coerceAtMost(10) * 0.5) * decay
                    }
                    return relevance + popularityScore
                }
                
                val extraLocal = fallbackCandidates.filter { entity ->
                    val entityIdStr = entity.id.toString()
                    val isInQueue = currentQueueIds.any { isSameSong(it, entityIdStr) }
                    val isAlreadyAdded = synchronized(addedVideoIds) {
                        addedVideoIds.any { isSameSong(it, entityIdStr) }
                    }
                    val isCurrent = entity.id == currentSong.id
                    !isInQueue && !isAlreadyAdded && !isCurrent
                }.sortedByDescending { calculateFallbackScore(it) }
                 .take(30)
                 
                filtered = (filtered + extraLocal).distinctBy { it.id }
            }
            
            // NOTE: Do NOT add all local candidates to addedVideoIds here.
            // Only the songs actually selected by refillQueueLoop's interleave logic
            // (via addToAddedVideoIds in the batch loop) should be tracked.
            // Adding ALL fetched local songs here causes premature exhaustion of candidates.
            return filtered.map { it.toSong() }
        } catch (e: Exception) {
            printe("AutoQueueManager: Exception fetching local related songs: ${e.message}")
            return emptyList()
        }
    }

    private suspend fun saveRelatedSongsToDb(sourceVideoId: String, relatedSongs: List<Song>, player: Player) {
        val dao = musicDaoRef ?: return

        try {
            val sourceLongId = getDatabaseIdForYoutubeId(sourceVideoId)
            
            val songEntities = mutableListOf<SongEntity>()
            val albumEntities = mutableListOf<AlbumEntity>()
            val artistEntities = mutableListOf<ArtistEntity>()
            val crossRefs = mutableListOf<SongArtistCrossRef>()
            val relatedMaps = mutableListOf<RelatedSongMap>()

            withContext(Dispatchers.IO) {
                // Check if source song exists in DB, if not, insert it first!
                val exists = dao.getSongByIdOnce(sourceLongId) != null
                if (!exists) {
                    val (currentMediaItem, playerDuration) = withContext(Dispatchers.Main) {
                        Pair(player.currentMediaItem, player.duration)
                    }
                    if (currentMediaItem != null) {
                        val title = currentMediaItem.mediaMetadata.title?.toString() ?: ""
                        val artist = currentMediaItem.mediaMetadata.artist?.toString() ?: ""
                        val artistLongId = artist.hashCode().toLong()
                        val album = currentMediaItem.mediaMetadata.albumTitle?.toString() ?: "YouTube Music"
                        val albumLongId = album.hashCode().toLong()
                        
                        val sourceArtist = ArtistEntity(id = artistLongId, name = artist, trackCount = 1, imageUrl = null)
                        val sourceAlbum = AlbumEntity(
                            id = albumLongId,
                            title = album,
                            artistName = artist,
                            artistId = artistLongId,
                            albumArtUriString = upgradeThumbnailUrlToHighQuality(currentMediaItem.mediaMetadata.artworkUri?.toString()),
                            songCount = 1,
                            dateAdded = System.currentTimeMillis(),
                            year = 0,
                            albumArtist = artist
                        )
                        val sourceSong = SongEntity(
                            id = sourceLongId,
                            title = title,
                            artistName = artist,
                            artistId = artistLongId,
                            albumArtist = artist,
                            albumName = album,
                            albumId = albumLongId,
                            contentUriString = "youtube://$sourceVideoId",
                            albumArtUriString = upgradeThumbnailUrlToHighQuality(currentMediaItem.mediaMetadata.artworkUri?.toString()),
                            duration = playerDuration.coerceAtLeast(0L),
                            genre = "YouTube",
                            filePath = "",
                            parentDirectoryPath = "/Cloud/YouTube",
                            isFavorite = false,
                            lyrics = null,
                            trackNumber = 0,
                            discNumber = null,
                            year = 0,
                            dateAdded = System.currentTimeMillis(),
                            mimeType = "audio/opus",
                            bitrate = 128,
                            sampleRate = 44100,
                            sourceType = SourceType.YOUTUBE
                        )
                        val sourceCrossRef = SongArtistCrossRef(songId = sourceLongId, artistId = artistLongId, isPrimary = true)
                        
                        dao.insertArtists(listOf(sourceArtist))
                        dao.insertAlbums(listOf(sourceAlbum))
                        dao.insertSongs(listOf(sourceSong))
                        dao.insertSongArtistCrossRefs(listOf(sourceCrossRef))
                    }
                }

                relatedSongs.forEach { song ->
                    val songVideoId = song.youtubeId ?: song.id.substringAfter("youtube_")
                    val songLongId = getDatabaseIdForYoutubeId(songVideoId)
                    val artistLongId = song.artistId
                    val albumLongId = song.albumId

                    artistEntities.add(
                        ArtistEntity(
                            id = artistLongId,
                            name = song.artist,
                            trackCount = 1,
                            imageUrl = null
                        )
                    )

                    albumEntities.add(
                        AlbumEntity(
                            id = albumLongId,
                            title = song.album,
                            artistName = song.artist,
                            artistId = artistLongId,
                            albumArtUriString = upgradeThumbnailUrlToHighQuality(song.albumArtUriString),
                            songCount = 1,
                            dateAdded = System.currentTimeMillis(),
                            year = 0,
                            albumArtist = song.artist
                        )
                    )

                    songEntities.add(
                        SongEntity(
                            id = songLongId,
                            title = song.title,
                            artistName = song.artist,
                            artistId = artistLongId,
                            albumArtist = song.artist,
                            albumName = song.album,
                            albumId = albumLongId,
                            contentUriString = song.contentUriString,
                            albumArtUriString = upgradeThumbnailUrlToHighQuality(song.albumArtUriString),
                            duration = song.duration,
                            genre = song.genre,
                            filePath = "",
                            parentDirectoryPath = "/Cloud/YouTube",
                            isFavorite = false,
                            lyrics = null,
                            trackNumber = 0,
                            discNumber = null,
                            year = 0,
                            dateAdded = System.currentTimeMillis(),
                            mimeType = "audio/opus",
                            bitrate = 128,
                            sampleRate = 44100,
                            sourceType = SourceType.YOUTUBE
                        )
                    )

                    crossRefs.add(
                        SongArtistCrossRef(
                            songId = songLongId,
                            artistId = artistLongId,
                            isPrimary = true
                        )
                    )

                    relatedMaps.add(
                        RelatedSongMap(
                            songId = sourceLongId,
                            relatedSongId = songLongId
                        )
                    )
                }

                // Strictly insert artist/album first due to Foreign Key constraints referenced in SongEntity
                dao.insertArtists(artistEntities.distinctBy { it.id })
                dao.insertAlbums(albumEntities.distinctBy { it.id })
                dao.insertSongs(songEntities.distinctBy { it.id })
                dao.insertSongArtistCrossRefs(crossRefs.distinct())
                dao.insertRelatedSongMaps(relatedMaps.distinct())
            }
        } catch (e: Exception) {
            printe("AutoQueueManager: Error saving related songs to DB: ${e.message}")
        }
    }

    private fun getDatabaseIdForYoutubeId(youtubeId: String): Long {
        val YOUTUBE_SONG_ID_OFFSET = 15_000_000_000_000L
        return -(YOUTUBE_SONG_ID_OFFSET + youtubeId.hashCode().toLong().absoluteValue)
    }
}
