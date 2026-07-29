package com.unshoo.pixelmusic.data.worker

import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.media.MediaScannerConnection
import android.os.Environment
import android.os.Build
import android.os.Trace // Import Trace
import android.provider.MediaStore
import android.util.Log
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.hilt.work.HiltWorker
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.WorkManager
import com.unshoo.pixelmusic.data.remote.youtube.YoutubePlaylistDataSource
import com.unshoo.pixelmusic.data.remote.youtube.YoutubeRequestHelper
import unshoo.ianshulyadav.pixelmusic.innertube.YouTube
import com.unshoo.pixelmusic.data.model.youtube.Playlist
import com.unshoo.pixelmusic.data.model.youtube.PlaylistInfo
import com.unshoo.pixelmusic.data.database.AlbumEntity
import com.unshoo.pixelmusic.data.database.ArtistEntity
import com.unshoo.pixelmusic.data.database.MusicDao
import com.unshoo.pixelmusic.data.database.SongArtistCrossRef
import com.unshoo.pixelmusic.data.database.SongEntity
import com.unshoo.pixelmusic.data.database.SourceType
import com.unshoo.pixelmusic.data.database.FavoritesDao
import com.unshoo.pixelmusic.data.database.FavoritesEntity
import com.unshoo.pixelmusic.data.database.resolveAlbumArtUri
import com.unshoo.pixelmusic.data.database.serializeArtistRefs
import com.unshoo.pixelmusic.data.model.ArtistRef
import com.unshoo.pixelmusic.data.media.AudioMetadataReader
import com.unshoo.pixelmusic.data.model.Song
import com.unshoo.pixelmusic.data.preferences.UserPreferencesRepository
import com.unshoo.pixelmusic.data.preferences.PlaylistPreferencesRepository
import com.unshoo.pixelmusic.data.repository.LyricsRepository
import com.unshoo.pixelmusic.utils.AlbumArtCacheManager
import com.unshoo.pixelmusic.utils.AlbumArtUtils
import com.unshoo.pixelmusic.utils.AudioMetaUtils.getAudioMetadata
import com.unshoo.pixelmusic.utils.DirectoryRuleResolver
import com.unshoo.pixelmusic.utils.LocalArtworkUri
import com.unshoo.pixelmusic.utils.buildLocalAudioSelection
import com.unshoo.pixelmusic.utils.normalizeMetadataTextOrEmpty
import com.unshoo.pixelmusic.utils.splitArtistsByDelimiters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.absoluteValue // Added
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import timber.log.Timber

enum class SyncMode {
    INCREMENTAL,
    FULL,
    REBUILD
}

@HiltWorker
class SyncWorker
@AssistedInject
constructor(
        @Assisted appContext: Context,
        @Assisted workerParams: WorkerParameters,
        private val musicDao: MusicDao,
        private val userPreferencesRepository: UserPreferencesRepository,
        private val lyricsRepository: LyricsRepository,
        private val playlistPreferencesRepository: PlaylistPreferencesRepository,
        private val youtubeDatastoreRepository: com.unshoo.pixelmusic.data.remote.youtube.DatastoreRepository,
        private val favoritesDao: FavoritesDao
) : CoroutineWorker(appContext, workerParams) {

    private val contentResolver: ContentResolver = appContext.contentResolver
    private var minSongDurationMs: Int = 10000
    private var minTracksPerAlbum: Int = 1

    override suspend fun doWork(): Result =
            withContext(Dispatchers.IO) {
                Trace.beginSection("SyncWorker.doWork")
                try {
                    val syncModeName =
                            inputData.getString(INPUT_SYNC_MODE) ?: SyncMode.INCREMENTAL.name
                    val syncMode = SyncMode.valueOf(syncModeName)
                    val forceMetadata = inputData.getBoolean(INPUT_FORCE_METADATA, false)

                    Timber.tag(TAG)
                        .i("Starting MediaStore synchronization (Mode: $syncMode, ForceMetadata: $forceMetadata)...")
                    val startTime = System.currentTimeMillis()

                    val artistDelimiters = userPreferencesRepository.artistDelimitersFlow.first()
                    val artistWordDelimiters = userPreferencesRepository.artistWordDelimitersFlow.first()
                    val extractArtistsFromTitle = userPreferencesRepository.extractArtistsFromTitleFlow.first()
                    val groupByAlbumArtist =
                            userPreferencesRepository.groupByAlbumArtistFlow.first()
                    val rescanRequired =
                            userPreferencesRepository.artistSettingsRescanRequiredFlow.first()
                    val directoryRulesVersion =
                        userPreferencesRepository.getDirectoryRulesVersion()
                    val lastAppliedDirectoryRulesVersion =
                        userPreferencesRepository.getLastAppliedDirectoryRulesVersion()
                    val directoryRulesChanged =
                        directoryRulesVersion != lastAppliedDirectoryRulesVersion

                    // Feature: Directory Filtering
                    val allowedDirs = userPreferencesRepository.allowedDirectoriesFlow.first()
                    val blockedDirs = userPreferencesRepository.blockedDirectoriesFlow.first()
                    val directoryResolver = DirectoryRuleResolver(allowedDirs, blockedDirs)
                    
                    var lastSyncTimestamp = userPreferencesRepository.getLastSyncTimestamp()

                    // Smart Duration Filtering
                    minSongDurationMs = userPreferencesRepository.getMinSongDuration()
                    minTracksPerAlbum = userPreferencesRepository.minTracksPerAlbumFlow.first()

                    Timber.tag(TAG)
                        .d(
                            "Artist delimiters=$artistDelimiters, groupByAlbumArtist=$groupByAlbumArtist, " +
                                "rescanRequired=$rescanRequired, directoryRulesChanged=$directoryRulesChanged " +
                                "(current=$directoryRulesVersion, applied=$lastAppliedDirectoryRulesVersion)"
                        )

                    // --- MEDIA SCAN PHASE ---
                    // OPT #8: Filesystem walk cooldown.
                    // triggerMediaScanForNewFiles() calls File.walkTopDown() on the external storage
                    // root, which can touch thousands of inodes and cause noticeable I/O.
                    // For INCREMENTAL syncs we skip it if we already ran within the last hour
                    // (the MediaStore daemon itself picks up new files quickly via inotify).
                    // FULL and REBUILD always run it unconditionally.
                    // Media scan for un-indexed files is slow. Run only if forced, or once every 12 hours.
                    val mediaScanCooldownMs = 12L * 60L * 60L * 1000L // 12 hours
                    val timeSinceLastScan = System.currentTimeMillis() - lastSyncTimestamp
                    val forceFilesystemScan = forceMetadata
                    val shouldRunMediaScan = forceFilesystemScan || 
                            (lastSyncTimestamp > 0L && timeSinceLastScan >= mediaScanCooldownMs)
                    if (shouldRunMediaScan) {
                        triggerMediaScanForNewFiles(directoryResolver)
                    } else {
                        Timber.tag(TAG).d(
                            "Skipping filesystem walk — forceMetadata=$forceMetadata, lastSyncTimestamp=${lastSyncTimestamp}"
                        )
                    }

                    // --- DELETION PHASE ---
                    // Detect and remove deleted songs efficiently using ID comparison
                    // We do this for INCREMENTAL and FULL modes. REBUILD clears everything anyway.
                    if (syncMode != SyncMode.REBUILD) {
                        // Only compare MediaStore-backed songs; cloud sources are excluded.
                        val localSongIds = musicDao.getAllMediaStoreSongIds().toHashSet()
                        val mediaStoreIds = fetchMediaStoreIds(directoryResolver)

                        // Identify IDs that are in local DB but not in MediaStore
                        val deletedIds = localSongIds - mediaStoreIds

                        if (deletedIds.isNotEmpty()) {
                            Timber.tag(TAG)
                                .i("Found ${deletedIds.size} deleted songs. Removing from database...")
                            // Chunk deletions to avoid SQLite variable limit (default 999)
                            val batchSize = 500
                            deletedIds.chunked(batchSize).forEach { chunk ->
                                musicDao.deleteSongsByIds(chunk.toList())
                                musicDao.deleteCrossRefsBySongIds(chunk.toList())
                            }
                        } else {
                            Timber.tag(TAG).d("No deleted songs found.")
                        }
                    }

                    // --- FETCH PHASE ---
                    // Determine what to fetch based on mode
                    val isFreshInstall = musicDao.getSongCount().first() == 0

                    // If REBUILD or FULL or RescanRequired or Fresh Install -> Fetch EVERYTHING
                    // (timestamp = 0)
                    // If INCREMENTAL -> Fetch only changes since lastSyncTimestamp
                    val fetchTimestamp =
                            if (syncMode == SyncMode.INCREMENTAL &&
                                            !rescanRequired &&
                                            !directoryRulesChanged &&
                                            !isFreshInstall
                            ) {
                                lastSyncTimestamp /
                                        1000 // Convert to seconds for MediaStore comparison
                            } else {
                                0L
                            }

                    Timber.tag(TAG)
                        .i("Fetching music from MediaStore (since: $fetchTimestamp seconds)...")

                    // Update every 50 songs or ~5% of library
                    val progressBatchSize = 50

                    val songsToInsert =
                            fetchMusicFromMediaStore(
                                    fetchTimestamp,
                                    forceMetadata,
                                    directoryResolver,
                                    syncMode == SyncMode.REBUILD,
                                    progressBatchSize
                            ) { current, total, phaseOrdinal ->
                                setProgress(
                                        workDataOf(
                                                PROGRESS_CURRENT to current,
                                                PROGRESS_TOTAL to total,
                                                PROGRESS_PHASE to phaseOrdinal
                                        )
                                )
                            }

                    Timber.tag(TAG)
                        .i("Fetched ${songsToInsert.size} new/modified songs from MediaStore.")

                    // --- PROCESSING PHASE ---
                    val anySongsFetched = songsToInsert.isNotEmpty()
                    if (anySongsFetched) {

                        val allExistingArtists =
                                if (syncMode == SyncMode.REBUILD) {
                                    emptyList()
                                } else {
                                    musicDao.getAllArtistsListRaw()
                                }
                        val allExistingAlbums =
                                if (syncMode == SyncMode.REBUILD) {
                                    emptyList()
                                } else {
                                    musicDao.getAllAlbumsList(emptyList(), false, 0)
                                }

                        val existingArtistMetadata =
                                allExistingArtists.associate { it.id to (it.imageUrl to it.customImageUri) }
                        
                        // Load all existing artist IDs to ensure stability across incremental syncs
                        val existingArtistIdMap = allExistingArtists.associate { it.name to it.id }.toMutableMap()
                        val maxArtistId = musicDao.getMaxArtistId() ?: 0L

                        Timber.tag(TAG)
                            .i("Processing ${songsToInsert.size} songs for upsert. Hash: ${songsToInsert.hashCode()}")

                        val (correctedSongs, albums, artists, crossRefs) =
                                preProcessAndDeduplicateWithMultiArtist(
                                        songs = songsToInsert,
                                        artistDelimiters = artistDelimiters,
                                        wordDelimiters = artistWordDelimiters,
                                        extractFromTitle = extractArtistsFromTitle,
                                        groupByAlbumArtist = groupByAlbumArtist,
                                        existingArtistMetadata = existingArtistMetadata,
                                        existingAlbums = allExistingAlbums,
                                        existingArtistIdMap = existingArtistIdMap,
                                        initialMaxArtistId = maxArtistId
                                )

                        // Use incrementalSyncMusicData for all modes except REBUILD
                        // Even for FULL sync, we can just upsert the values
                        if (syncMode == SyncMode.REBUILD) {
                            // Keep clear + insert in one transaction to avoid partial clears
                            // if this worker gets cancelled/replaced mid-rebuild.
                            musicDao.rebuildMusicDataWithCrossRefs(
                                    correctedSongs,
                                    albums,
                                    artists,
                                    crossRefs
                            )
                        } else {
                            // incrementalSyncMusicData handles upserts efficiently
                            // processing deleted songs was already handled at the start
                            musicDao.incrementalSyncMusicData(
                                    songs = correctedSongs,
                                    albums = albums,
                                    artists = artists,
                                    crossRefs = crossRefs,
                                    deletedSongIds = emptyList() // Already handled
                            )
                        }

                        // Clear the rescan required flag
                        userPreferencesRepository.clearArtistSettingsRescanRequired()

                        userPreferencesRepository.markDirectoryRulesVersionApplied(
                            directoryRulesVersion
                        )
                    }

                    // Always update last sync timestamp if we reached this point successfully,
                    // even if no new songs were found in MediaStore. This prevents the sync
                    // from being re-triggered on every app launch when the library is already up to date.
                    userPreferencesRepository.setLastSyncTimestamp(System.currentTimeMillis())

                    val endTime = System.currentTimeMillis()
                    Timber.tag(TAG)
                        .i("Synchronization finished successfully in ${endTime - startTime}ms.")

                    // Count total songs for the output
                    val totalSongs = musicDao.getSongCount().first()

                    // --- LRC SCANNING PHASE ---
                    val autoScanLrc = userPreferencesRepository.autoScanLrcFilesFlow.first()
                    if (autoScanLrc) {
                        Timber.tag(TAG)
                            .i("Auto-scan LRC files enabled. Starting scan phase in chunks...")

                        // Get ALL media store song IDs to scan in manageable chunks
                        val mediaStoreSongIds = musicDao.getAllMediaStoreSongIds()
                        val totalToScan = mediaStoreSongIds.size
                        var totalScannedCount = 0

                        mediaStoreSongIds.chunked(1000).forEach { idBatch ->
                            val batchEntities = musicDao.getSongsByIdsListSimple(idBatch)
                            val batchSongs =
                                    batchEntities.map { entity ->
                                        Song(
                                                id = entity.id.toString(),
                                                title = entity.title,
                                                artist = entity.artistName,
                                                artistId = entity.artistId,
                                                album = entity.albumName,
                                                albumId = entity.albumId,
                                                path = entity.filePath,
                                                contentUriString = entity.contentUriString,
                                                albumArtUriString = entity.albumArtUriString,
                                                duration = entity.duration,
                                                lyrics = entity.lyrics,
                                                dateAdded = entity.dateAdded,
                                                trackNumber = entity.trackNumber,
                                                year = entity.year,
                                                mimeType = entity.mimeType,
                                                bitrate = entity.bitrate,
                                                sampleRate = entity.sampleRate
                                        )
                                    }

                            val batchScannedCount =
                                    lyricsRepository.scanAndAssignLocalLrcFiles(batchSongs) {
                                            current,
                                            total ->
                                        // Progress within the current batch
                                        val overallCurrent = totalScannedCount + current
                                        setProgress(
                                                workDataOf(
                                                        PROGRESS_CURRENT to overallCurrent,
                                                        PROGRESS_TOTAL to totalToScan,
                                                        PROGRESS_PHASE to
                                                                SyncProgress.SyncPhase.SCANNING_LRC
                                                                        .ordinal
                                                )
                                        )
                                    }
                            totalScannedCount += idBatch.size
                            Log.d(
                                    TAG,
                                    "LRC Scan: Processed batch of ${idBatch.size}, total assigned so far: $batchScannedCount"
                            )
                        }

                        Log.i(TAG, "LRC Scan finished for $totalToScan songs.")
                    }

                    // Clean orphaned album art cache files
                    setProgress(
                        workDataOf(
                            PROGRESS_PHASE to SyncProgress.SyncPhase.CLEANING_CACHE.ordinal
                        )
                    )
                    val allSongIds = musicDao.getAllSongIds().toSet()
                    AlbumArtCacheManager.cleanOrphanedCacheFiles(applicationContext, allSongIds)

                    // Sync cloud songs into the unified songs table.
                    // OPT #7: Guard each source to avoid opening Room transactions when
                    // the user hasn't configured that cloud provider.
                    val isYoutubeConnected = youtubeDatastoreRepository.cookies.first().toRawCookie().isNotEmpty()
                    
                    val needsActiveCloudSync = isYoutubeConnected

                    if (needsActiveCloudSync) {
                        setProgress(
                            workDataOf(
                                PROGRESS_PHASE to SyncProgress.SyncPhase.SYNCING_CLOUD.ordinal
                            )
                        )
                    }

                    if (isYoutubeConnected) {
                        syncYoutubeData()
                    } else {
                        Log.d(TAG, "Skipping YouTube sync — not logged in.")
                    }

                    // Recalculate total
                    val finalTotalSongs = musicDao.getSongCount().first()

                    Result.success(workDataOf(OUTPUT_TOTAL_SONGS to finalTotalSongs.toLong()))
                } catch (e: Exception) {
                    Log.e(TAG, "Error during MediaStore synchronization", e)
                    Result.failure()
                } finally {
                    Trace.endSection() // End SyncWorker.doWork
                }
            }

    /** Data class to hold the result of multi-artist preprocessing. */
    private data class MultiArtistProcessResult(
        val songs: List<SongEntity>,
        val albums: List<AlbumEntity>,
        val artists: List<ArtistEntity>,
        val crossRefs: List<SongArtistCrossRef>
    )

    /**
     * Process songs with multi-artist support. Splits artist names by delimiters and creates proper
     * cross-references.
     */
    private fun preProcessAndDeduplicateWithMultiArtist(
            songs: List<SongEntity>,
            artistDelimiters: List<String>,
            wordDelimiters: List<String> = emptyList(),
            extractFromTitle: Boolean = true,
            groupByAlbumArtist: Boolean,
            existingArtistMetadata: Map<Long, Pair<String?, String?>>,
            existingAlbums: List<AlbumEntity>,
            existingArtistIdMap: MutableMap<String, Long>,
            initialMaxArtistId: Long
    ): MultiArtistProcessResult {
        
        val nextArtistId = AtomicLong(initialMaxArtistId + 1)
        val artistNameToId = existingArtistIdMap // Re-use the map passed in
        
        val allCrossRefs = mutableListOf<SongArtistCrossRef>()
        val artistTrackCounts = mutableMapOf<Long, Int>()
        val albumMap = mutableMapOf<AlbumGroupingKey, Long>()
        val artistSplitCache = mutableMapOf<String, List<String>>()
        val correctedSongs = ArrayList<SongEntity>(songs.size)

        existingAlbums
            .sortedBy { it.id }
            .forEach { album ->
                buildAlbumGroupingKeys(album).forEach { key ->
                    albumMap.putIfAbsent(key, album.id)
                }
            }

        songs.forEach { song ->
            val rawArtistName = song.artistName
            val songArtistNameTrimmed = rawArtistName.trim()

            // Split artist field by character + word delimiters
            val allArtistsForSong =
                    artistSplitCache.getOrPut("$rawArtistName\u0000${song.title}\u0000$extractFromTitle") {
                        collectArtistNames(
                            rawArtistName = rawArtistName,
                            title = song.title,
                            artistDelimiters = artistDelimiters,
                            wordDelimiters = wordDelimiters,
                            extractFromTitle = extractFromTitle
                        )
                    }

            allArtistsForSong.forEach { artistName ->
                val normalizedName = artistName.trim()
                if (normalizedName.isNotEmpty() && !artistNameToId.containsKey(normalizedName)) {
                     val id = nextArtistId.getAndIncrement()
                     artistNameToId[normalizedName] = id
                }
            }

            val primaryArtistName =
                    allArtistsForSong.firstOrNull()?.trim()?.takeIf { it.isNotEmpty() }
                            ?: songArtistNameTrimmed
            val primaryArtistId = artistNameToId[primaryArtistName] ?: song.artistId

            allArtistsForSong.forEachIndexed { index, artistName ->
                val normalizedName = artistName.trim()
                val artistId = artistNameToId[normalizedName]
                if (artistId != null) {
                    val isPrimary = (index == 0) // First artist is primary
                    allCrossRefs.add(
                            SongArtistCrossRef(
                                    songId = song.id,
                                    artistId = artistId,
                                    isPrimary = isPrimary
                            )
                    )
                    artistTrackCounts[artistId] = (artistTrackCounts[artistId] ?: 0) + 1
                }
            }

            // --- Album Logic ---
            val albumKey = buildAlbumGroupingKey(song)
            val finalAlbumId = albumMap.getOrPut(albumKey) { song.albumId }

            // Build serialized artists JSON for efficient loading without JOINs
            val artistRefsForJson = allArtistsForSong.mapIndexed { index, name ->
                val normalizedName = name.trim()
                val artistId = artistNameToId[normalizedName] ?: 0L
                ArtistRef(id = artistId, name = normalizedName, isPrimary = index == 0)
            }.filter { it.name.isNotEmpty() }

            correctedSongs.add(
                    song.copy(
                            artistId = primaryArtistId,
                            artistName = rawArtistName, // Preserving full artist string for display
                            albumId = finalAlbumId,
                            artistsJson = serializeArtistRefs(artistRefsForJson)
                    )
            )
        }

        // Build Entities
        val artistEntities = artistNameToId.map { (name, id) ->
            val count = artistTrackCounts[id] ?: 0
            val metadata = existingArtistMetadata[id]
            ArtistEntity(
                id = id,
                name = name,
                trackCount = count,
                imageUrl = metadata?.first,
                customImageUri = metadata?.second
            )
        }
        
        // Re-calculate Album Entities from the corrected songs to ensure we have valid metadata (Art, Year)
        // which isn't available in the simple albumMap (which only has ID)
        val albumEntities = correctedSongs.groupBy { it.albumId }.map { (catAlbumId, songsInAlbum) ->
             val firstSong = songsInAlbum.first()
             val representativeAlbumArt = songsInAlbum.firstNotNullOfOrNull { it.albumArtUriString }
             val determinedAlbumArtist = chooseAlbumDisplayArtist(
                 songs = songsInAlbum,
                 preferAlbumArtist = groupByAlbumArtist
             )
             val determinedAlbumArtistId = resolveAlbumDisplayArtistId(
                 displayArtist = determinedAlbumArtist,
                 songs = songsInAlbum,
                 artistNameToId = artistNameToId,
                 artistDelimiters = artistDelimiters,
                 wordDelimiters = wordDelimiters
             )
             val metadataAlbumArtist = songsInAlbum
                 .mapNotNull { song ->
                     song.albumArtist?.takeIf { it.isNotBlank() }
                 }
                 .groupingBy { it }
                 .eachCount()
                 .maxByOrNull { it.value }
                 ?.key

             AlbumEntity(
                 id = catAlbumId,
                 title = firstSong.albumName,
                 artistName = determinedAlbumArtist,
                 artistId = determinedAlbumArtistId,
                 albumArtUriString = representativeAlbumArt,
                 songCount = songsInAlbum.size,
                 dateAdded = firstSong.dateAdded,
                 year = firstSong.year,
                 albumArtist = metadataAlbumArtist
             )
        }

        return MultiArtistProcessResult(
                songs = correctedSongs, // Corrected songs have the right Album IDs now
                albums = albumEntities,
                artists = artistEntities,
                crossRefs = allCrossRefs
        )
    }

    /**
     * Fetches a map of Song ID -> Genre Name using the MediaStore.Audio.Genres table. This is
     * necessary because the GENRE column in MediaStore.Audio.Media is not reliably available or
     * populated on all Android versions (especially pre-API 30).
     * 
     * Optimized: 
     * 1. Caches results for 1 hour to avoid refetching on incremental syncs
     * 2. Fetches all genres first, then queries members in parallel with controlled concurrency
     */
    private suspend fun fetchGenreMap(forceRefresh: Boolean = false): Map<Long, String> = coroutineScope {
        // Optimization: Skip genre map on Android 11+ because we can query GENRE directly.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return@coroutineScope emptyMap()
        }

        // Check cache first (valid for 1 hour)
        val now = System.currentTimeMillis()
        val cacheAge = now - genreMapCacheTimestamp
        if (!forceRefresh && genreMapCache.isNotEmpty() && cacheAge < GENRE_CACHE_TTL_MS) {
            Log.d(TAG, "Using cached genre map (${genreMapCache.size} entries, age: ${cacheAge/1000}s)")
            return@coroutineScope genreMapCache
        }
        
        val genreMap = ConcurrentHashMap<Long, String>()
        val genreProjection = arrayOf(MediaStore.Audio.Genres._ID, MediaStore.Audio.Genres.NAME)
        
        // Semaphore to limit concurrent queries (avoid overwhelming ContentResolver)
        val querySemaphore = Semaphore(4)

        try {
            // Step 1: Fetch all genres (single query)
            val genres = mutableListOf<Pair<Long, String>>()
            
            contentResolver.query(
                            MediaStore.Audio.Genres.EXTERNAL_CONTENT_URI,
                            genreProjection,
                            null,
                            null,
                            null
                    )
                    ?.use { cursor ->
                        val idCol = cursor.getColumnIndex(MediaStore.Audio.Genres._ID)
                        val nameCol = cursor.getColumnIndex(MediaStore.Audio.Genres.NAME)

                        if (idCol >= 0 && nameCol >= 0) {
                            while (cursor.moveToNext()) {
                                val genreId = cursor.getLong(idCol)
                                val genreName = cursor.getString(nameCol)

                                if (!genreName.isNullOrBlank() &&
                                                !genreName.equals("unknown", ignoreCase = true)
                                ) {
                                    genres.add(genreId to genreName)
                                }
                            }
                        }
                    }
            
            // Step 2: Fetch members for each genre in parallel (controlled concurrency)
            genres.map { (genreId, genreName) ->
                async(Dispatchers.IO) {
                    querySemaphore.withPermit {
                        val membersUri =
                                MediaStore.Audio.Genres.Members.getContentUri(
                                        "external",
                                        genreId
                                )
                        val membersProjection =
                                arrayOf(MediaStore.Audio.Genres.Members.AUDIO_ID)

                        contentResolver.query(
                                        membersUri,
                                        membersProjection,
                                        null,
                                        null,
                                        null
                                )
                                ?.use { membersCursor ->
                                    val audioIdCol =
                                            membersCursor.getColumnIndex(
                                                    MediaStore.Audio.Genres.Members.AUDIO_ID
                                            )
                                    if (audioIdCol >= 0) {
                                        while (membersCursor.moveToNext()) {
                                            val audioId = membersCursor.getLong(audioIdCol)
                                            // If a song has multiple genres, the last one processed wins.
                                            // This is acceptable as a primary genre for display.
                                            genreMap[audioId] = genreName
                                        }
                                    }
                                }
                    }
                }
            }.awaitAll()
            
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching genre map", e)
        }
        
        // Update cache
        if (genreMap.isNotEmpty()) {
            genreMapCache = genreMap.toMap()
            genreMapCacheTimestamp = System.currentTimeMillis()
            Log.d(TAG, "Genre map cache updated with ${genreMap.size} entries")
        }
        
        genreMap
    }

    /** Raw data extracted from cursor - lightweight class for fast iteration */
    private data class RawSongData(
            val id: Long,
            val albumId: Long,
            val artistId: Long,
            val filePath: String,
            val mimeType: String?,
            val title: String,
            val artist: String,
            val album: String,
            val albumArtist: String?,
            val duration: Long,
            val trackNumber: Int,
            val discNumber: Int?,
            val year: Int,
            val dateModified: Long,
            val genre: String?
    )

    private fun isSongUnchanged(raw: RawSongData, existing: SongEntity?): Boolean {
        if (existing == null) return false

        val lastSlash = raw.filePath.lastIndexOf('/')
        val parentDir = if (lastSlash > 0) raw.filePath.substring(0, lastSlash) else ""
        val existingDateModifiedSeconds = TimeUnit.MILLISECONDS.toSeconds(existing.dateAdded)

        return existing.filePath == raw.filePath &&
            existing.parentDirectoryPath == parentDir &&
            existing.title == raw.title &&
            existing.artistName == raw.artist &&
            existing.albumName == raw.album &&
            existing.albumId == raw.albumId &&
            existing.artistId == raw.artistId &&
            existing.duration == raw.duration &&
            existing.trackNumber == raw.trackNumber &&
            existing.discNumber == raw.discNumber &&
            existing.year == raw.year &&
            existingDateModifiedSeconds == raw.dateModified
    }

    private suspend fun fetchMusicFromMediaStore(
            sinceTimestamp: Long, // Seconds
            forceMetadata: Boolean,
            directoryResolver: DirectoryRuleResolver,
            isRebuild: Boolean,
            progressBatchSize: Int,
            onProgress: suspend (current: Int, total: Int, phaseOrdinal: Int) -> Unit
    ): List<SongEntity> {
        Trace.beginSection("SyncWorker.fetchMusicFromMediaStore")

        val deepScan = forceMetadata
        val genreMap = fetchGenreMap() // Load genres upfront

        val projectionList = mutableListOf(
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.ARTIST_ID,
                MediaStore.Audio.Media.ALBUM,
                MediaStore.Audio.Media.ALBUM_ID,
                MediaStore.Audio.Media.ALBUM_ARTIST,
                MediaStore.Audio.Media.DURATION,
                MediaStore.Audio.Media.DATA,
                MediaStore.Audio.Media.MIME_TYPE,
                MediaStore.Audio.Media.TRACK,
                MediaStore.Audio.Media.YEAR,
                MediaStore.Audio.Media.DATE_MODIFIED
        )

        // API 30+ supports GENRE in the main audio table
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            projectionList.add(MediaStore.Audio.Media.GENRE)
        }

        val projection = projectionList.toTypedArray()

        val (baseSelection, baseArgs) = buildLocalAudioSelection(minSongDurationMs)
        val selectionBuilder = StringBuilder(baseSelection)
        val selectionArgsList = baseArgs.toMutableList()

        // Incremental selection
        if (sinceTimestamp > 0) {
            selectionBuilder.append(
                    " AND (${MediaStore.Audio.Media.DATE_MODIFIED} > ? OR ${MediaStore.Audio.Media.DATE_ADDED} > ?)"
            )
            selectionArgsList.add(sinceTimestamp.toString())
            selectionArgsList.add(sinceTimestamp.toString())
        }

        val selection = selectionBuilder.toString()
        val selectionArgs = selectionArgsList.toTypedArray()

        // Phase 1: Fast cursor iteration to collect raw data
        val rawDataList = mutableListOf<RawSongData>()

        contentResolver.query(
                        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                        projection,
                        selection,
                        selectionArgs,
                        null
                )
                ?.use { cursor ->
                    val totalCount = cursor.count
                    onProgress(0, totalCount, SyncProgress.SyncPhase.FETCHING_MEDIASTORE.ordinal)

                    val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                    val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                    val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                    val artistIdCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST_ID)
                    val albumCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                    val albumIdCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
                    val albumArtistCol = cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_ARTIST)
                    val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                    val dataCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
                    val mimeTypeCol = cursor.getColumnIndex(MediaStore.Audio.Media.MIME_TYPE)
                    val trackCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TRACK)
                    val yearCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.YEAR)
                    val dateModifiedCol =
                            cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_MODIFIED)
                    val genreCol = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        cursor.getColumnIndex(MediaStore.Audio.Media.GENRE)
                    } else -1

                    while (cursor.moveToNext()) {
                        try {
                            val data = cursor.getString(dataCol) ?: continue
                            val lastSlash = data.lastIndexOf('/')
                            if (lastSlash > 0) {
                                val normalizedParent = data.substring(0, lastSlash)
                                if (directoryResolver.isBlocked(normalizedParent)) {
                                    continue
                                }
                            }
                        } catch (e: Exception) {
                            // Proceed on error
                        }

                        rawDataList.add(
                                RawSongData(
                                        id = cursor.getLong(idCol),
                                        albumId = cursor.getLong(albumIdCol),
                                        artistId = cursor.getLong(artistIdCol),
                                        filePath = cursor.getString(dataCol) ?: "",
                                        mimeType = if (mimeTypeCol >= 0) cursor.getString(mimeTypeCol) else null,
                                        title =
                                                cursor.getString(titleCol)
                                                        .normalizeMetadataTextOrEmpty()
                                                        .ifEmpty { "Unknown Title" },
                                        artist =
                                                cursor.getString(artistCol)
                                                        .normalizeMetadataTextOrEmpty()
                                                        .ifEmpty { "Unknown Artist" },
                                        album =
                                                cursor.getString(albumCol)
                                                        .normalizeMetadataTextOrEmpty()
                                                        .ifEmpty { "Unknown Album" },
                                        albumArtist =
                                                if (albumArtistCol >= 0)
                                                        cursor.getString(albumArtistCol)
                                                                ?.normalizeMetadataTextOrEmpty()
                                                                ?.takeIf { it.isNotBlank() }
                                                else null,
                                        duration = cursor.getLong(durationCol),
                                        trackNumber = cursor.getInt(trackCol) % 1000,
                                        discNumber = (cursor.getInt(trackCol) / 1000).takeIf { it > 0 },
                                        year = cursor.getInt(yearCol),
                                        dateModified = cursor.getLong(dateModifiedCol),
                                        genre = if (genreCol >= 0) cursor.getString(genreCol) else null
                                )
                        )
                    }
                }

        if (rawDataList.isEmpty()) {
            Log.i(TAG, "MediaStore cursor produced 0 raw songs after directory filtering")
            Trace.endSection()
            return emptyList()
        }

        // Phase 2: Identify changed songs and merge with existing data in chunks
        val artistDelimiters = userPreferencesRepository.artistDelimitersFlow.first()
        val artistWordDelimiters = userPreferencesRepository.artistWordDelimitersFlow.first()
        val rawSongCount = rawDataList.size
        val songsToProcess = if (isRebuild) {
             rawDataList.toList()
        } else {
            // Find existing data for these songs to avoid unnecessary reprocessing
            // and to preserve user edits.
            val results = mutableListOf<RawSongData>()
            
            rawDataList.chunked(500).forEach { batch ->
                val ids = batch.map { it.id }
                val existingMap = musicDao.getSongsByIdsListSimple(ids).associateBy { it.id }
                
                batch.forEach { raw ->
                    val existing = existingMap[raw.id]
                    if (!isSongUnchanged(raw, existing)) {
                        results.add(raw)
                    }
                }
            }
            results
        }

        // rawDataList is no longer needed — release its memory before the processing phase,
        // which may allocate large existingMap objects and metadata ByteArrays.
        rawDataList.clear()

        val totalCount = songsToProcess.size
        Log.i(
            TAG,
            "MediaStore raw=$rawSongCount, songsToProcess=$totalCount, isRebuild=$isRebuild"
        )
        if (totalCount == 0) {
            Trace.endSection()
            return emptyList()
        }

        // Phase 3: Parallel processing of songs with metadata merging
        onProgress(0, totalCount, SyncProgress.SyncPhase.PROCESSING_FILES.ordinal)
        val processedCount = AtomicInteger(0)
        val concurrencyLimit = 4 // Reduced concurrency to save memory
        val semaphore = Semaphore(concurrencyLimit)

        // Process batches sequentially so each batch's existingMap can be GC'd before the next
        // batch is loaded. The semaphore still limits concurrency within each batch.
        val songs = mutableListOf<SongEntity>()
        for (batch in songsToProcess.chunked(200)) {
            val ids = batch.map { it.id }
            val existingMap = if (isRebuild) emptyMap() else musicDao.getSongsByIdsListSimple(ids).associateBy { it.id }
            val batchResults = coroutineScope {
                batch.map { raw ->
                    async {
                        semaphore.withPermit {
                            val localSong = existingMap[raw.id]
                            val mediaStoreSong =
                                processSongData(
                                    raw = raw,
                                    genreMap = genreMap,
                                    deepScan = deepScan,
                                    forceAlbumArtRefresh = deepScan || localSong != null
                                )

                            val song = if (localSong != null) {
                                // Preserve user-edited fields
                                mediaStoreSong.copy(
                                    dateAdded = localSong.dateAdded,
                                    lyrics = localSong.lyrics,
                                    title = if (localSong.title.isNotBlank() && localSong.title != mediaStoreSong.title) localSong.title else mediaStoreSong.title,
                                    artistName = choosePreferredArtistName(
                                        localArtistName = localSong.artistName,
                                        mediaStoreArtistName = mediaStoreSong.artistName,
                                        artistDelimiters = artistDelimiters,
                                        wordDelimiters = artistWordDelimiters
                                    ),
                                    albumName = if (localSong.albumName.isNotBlank() && localSong.albumName != mediaStoreSong.albumName) localSong.albumName else mediaStoreSong.albumName,
                                    genre = localSong.genre ?: mediaStoreSong.genre,
                                    trackNumber = if (localSong.trackNumber != 0) localSong.trackNumber else mediaStoreSong.trackNumber,
                                    discNumber = localSong.discNumber ?: mediaStoreSong.discNumber,
                                    albumArtUriString = mediaStoreSong.albumArtUriString
                                )
                            } else {
                                mediaStoreSong
                            }

                            val count = processedCount.incrementAndGet()
                            if (count % progressBatchSize == 0 || count == totalCount) {
                                onProgress(count, totalCount, SyncProgress.SyncPhase.PROCESSING_FILES.ordinal)
                            }
                            song
                        }
                    }
                }.awaitAll()
            }
            songs.addAll(batchResults)
        }

        Trace.endSection()
        return songs
    }

    /**
     * Checks if a metadata field from MediaStore is a default/unknown placeholder.
     * MediaStore uses `<unknown>` for unreadable fields, and our normalization
     * may fall back to `"Unknown Artist"` / `"Unknown Album"` etc.
     */
    private fun isDefaultMetadata(value: String): Boolean {
        val lower = value.trim().lowercase()
        return lower.isEmpty() ||
            lower == "<unknown>" ||
            lower == "unknown" ||
            lower == "unknown artist" ||
            lower == "unknown album"
    }

    /**
     * Process a single song's raw data into a SongEntity. This is the CPU/IO intensive work that
     * benefits from parallelization.
     */
    private suspend fun processSongData(
            raw: RawSongData,
            genreMap: Map<Long, String>,
            deepScan: Boolean,
            forceAlbumArtRefresh: Boolean
    ): SongEntity {
        val parentDir = java.io.File(raw.filePath).parent ?: ""
        val contentUriString =
                ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, raw.id)
                        .toString()

        var albumArtUriString =
                AlbumArtUtils.getAlbumArtUri(
                        applicationContext,
                        raw.filePath,
                        raw.id,
                        forceAlbumArtRefresh
                )
        val audioMetadata =
                if (deepScan) getAudioMetadata(musicDao, raw.id, raw.filePath, true) else null

        var title = raw.title
        var artist = raw.artist
        var album = raw.album
        var albumArtist = resolveAlbumArtist(
            rawAlbumArtist = raw.albumArtist,
            metadataAlbumArtist = null
        )
        var trackNumber = raw.trackNumber
        var discNumber = raw.discNumber
        var year = raw.year
        var genre: String? = genreMap[raw.id] ?: raw.genre // Use mapped genre as default, or direct genre from main cursor

        val shouldAugmentMetadata =
                deepScan ||
                        raw.filePath.endsWith(".wav", true) ||
                        raw.filePath.endsWith(".opus", true) ||
                        raw.filePath.endsWith(".ogg", true) ||
                        raw.filePath.endsWith(".oga", true) ||
                        raw.filePath.endsWith(".aiff", true) ||
                        // Fallback: if MediaStore returned default/missing metadata,
                        // try TagLib+JAudioTagger to read actual tags from the file.
                        // MediaStore uses "<unknown>" for unreadable fields;
                        // our normalization may produce "Unknown Artist"/"Unknown Album".
                        isDefaultMetadata(raw.artist) ||
                        isDefaultMetadata(raw.album)

        if (shouldAugmentMetadata) {
            val file = java.io.File(raw.filePath)
            if (file.exists()) {
                try {
                    AudioMetadataReader.read(file)?.let { meta ->
                        if (!meta.title.isNullOrBlank()) title = meta.title
                        if (!meta.artist.isNullOrBlank()) artist = meta.artist
                        if (!meta.album.isNullOrBlank()) album = meta.album
                        albumArtist = resolveAlbumArtist(
                            rawAlbumArtist = albumArtist,
                            metadataAlbumArtist = meta.albumArtist
                        )
                        if (!meta.genre.isNullOrBlank()) genre = meta.genre
                        if (meta.trackNumber != null) trackNumber = meta.trackNumber
                        if (meta.discNumber != null) discNumber = meta.discNumber
                        if (meta.year != null) year = meta.year

                        meta.artwork?.let { art ->
                            albumArtUriString = LocalArtworkUri.buildSongUri(raw.id)
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to read metadata via TagLib for ${raw.filePath}", e)
                }
            }
        }

        return SongEntity(
                id = raw.id,
                title = title,
                artistName = artist,
                artistId = raw.artistId,
                albumArtist = albumArtist,
                albumName = album,
                albumId = raw.albumId,
                contentUriString = contentUriString,
                albumArtUriString = albumArtUriString,
                duration = raw.duration,
                genre = genre?.takeIf { it.isNotBlank() } ?: "Local Music",
                filePath = raw.filePath,
                parentDirectoryPath = parentDir,
                trackNumber = trackNumber,
                discNumber = discNumber,
                year = year,
                dateAdded =
                        raw.dateModified.let { seconds ->
                            if (seconds > 0) TimeUnit.SECONDS.toMillis(seconds)
                            else System.currentTimeMillis()
                        },
                mimeType = audioMetadata?.mimeType ?: raw.mimeType,
                sampleRate = audioMetadata?.sampleRate,
                bitrate = audioMetadata?.bitrate,
                sourceType = SourceType.LOCAL
        )
    }

    /**
     * Triggers a media scan ONLY for new files that are not yet in MediaStore.
     * This is a fast, incremental scan optimized for pull-to-refresh.
     * It compares filesystem files with MediaStore entries and only scans the difference.
     */
    private suspend fun triggerMediaScanForNewFiles(directoryResolver: DirectoryRuleResolver) {
        withContext(Dispatchers.IO) {
            val externalRoot = Environment.getExternalStorageDirectory()
            val allowedSet =
                userPreferencesRepository.allowedDirectoriesFlow.first().map { File(it) }.toSet()
            Log.i(TAG, "Starting media scan for new files. Explicit includes: ${allowedSet.size}")

            // Get all file paths currently in MediaStore
            val mediaStorePaths = fetchMediaStoreFilePaths()
            Log.d(TAG, "MediaStore has ${mediaStorePaths.size} known files")

            val scanRoots =
                collectPreferredScanRoots(
                    externalRoot = externalRoot,
                    mediaStorePaths = mediaStorePaths,
                    explicitAllowedRoots = allowedSet,
                    directoryResolver = directoryResolver
                )

            if (scanRoots.isEmpty()) {
                Log.d(TAG, "No eligible roots found for media scan")
                return@withContext
            }

            // Collect audio files from filesystem that are NOT in MediaStore
            val audioExtensions =
                    setOf("mp3", "flac", "m4a", "wav", "ogg", "opus", "aac", "wma", "aiff")
            val newFilesToScan = linkedSetOf<String>()

            scanRoots.forEach { root ->
                root.walkTopDown()
                    .onEnter { dir ->
                        val name = dir.name
                        if (dir.isHidden || name.startsWith(".")) return@onEnter false
                        val path = dir.absolutePath
                        if (directoryResolver.isBlocked(path)) return@onEnter false

                        if (File(dir, ".nomedia").exists()) {
                            val isAllowed = allowedSet.any { allowed -> 
                                allowed.absolutePath == path || allowed.absolutePath.startsWith("$path/")
                            }
                            if (!isAllowed) return@onEnter false
                        }

                        // Default Skip Rules (System Folders)
                        val isSystemFolder = (name == "Android" || name == "data" || name == "obb")
                        if (isSystemFolder) {
                            // Check if this specific folder is Explicitly Allowed or is a Parent of an allowed folder
                            // e.g. if Allowed is "Android/media", we MUST enter "Android".
                            // e.g. if Allowed is "Android" (root), we MUST enter "Android".
                            val isAllowed = allowedSet.any { allowed -> 
                                allowed.absolutePath == path || allowed.absolutePath.startsWith("$path/")
                            }
                            
                            if (!isAllowed) {
                                // Apply strict skipping for Android/data and Android/obb if not allowed
                                val parent = dir.parentFile
                                if (name == "Android" && parent?.absolutePath == Environment.getExternalStorageDirectory().absolutePath) return@onEnter false
                                if (parent?.name == "Android" && (name == "data" || name == "obb")) return@onEnter false
                            }
                        }
                        true
                    }
                    .filter { it.isFile && it.extension.lowercase() in audioExtensions }
                    .filter { it.absolutePath !in mediaStorePaths } // Only new files
                    .forEach { newFilesToScan.add(it.absolutePath) }
            }

            if (newFilesToScan.isEmpty()) {
                Log.d(TAG, "No new audio files found - MediaStore is up to date")
                return@withContext
            }

            Log.i(TAG, "Found ${newFilesToScan.size} NEW audio files to scan")

            // Scan only the new files
            val latch = CountDownLatch(1)
            var scannedCount = 0

            MediaScannerConnection.scanFile(
                applicationContext, 
                newFilesToScan.toTypedArray(),
                null
            ) { _, _ ->
                scannedCount++
                if (scannedCount >= newFilesToScan.size) {
                    latch.countDown()
                }
            }

            // Wait for scan to complete (max 15 seconds)
            val completed = latch.await(15, TimeUnit.SECONDS)
            if (!completed) {
                Log.w(TAG, "Media scan timeout after scanning $scannedCount/${newFilesToScan.size} files")
            } else {
                Log.i(TAG, "Media scan completed for ${newFilesToScan.size} new files")
            }
        }
    }

    private fun collectPreferredScanRoots(
        externalRoot: File,
        mediaStorePaths: Set<String>,
        explicitAllowedRoots: Set<File>,
        directoryResolver: DirectoryRuleResolver
    ): List<File> {
        val candidates = linkedSetOf<File>()

        // 1) Explicitly allowed roots always get priority.
        explicitAllowedRoots.forEach { candidates.add(it) }

        // 2) Common user-facing media directories.
        listOf(
            Environment.DIRECTORY_MUSIC,
            Environment.DIRECTORY_DOWNLOADS,
            Environment.DIRECTORY_PODCASTS,
            Environment.DIRECTORY_AUDIOBOOKS,
            Environment.DIRECTORY_RINGTONES,
            Environment.DIRECTORY_NOTIFICATIONS
        ).forEach { dirName ->
            candidates.add(Environment.getExternalStoragePublicDirectory(dirName))
        }

        // 3) Top-level buckets already used by current MediaStore entries.
        mediaStorePaths.forEach { mediaPath ->
            val parent = File(mediaPath).parentFile ?: return@forEach
            val parentPath = parent.absolutePath
            if (parentPath.startsWith("${externalRoot.absolutePath}/")) {
                val relative = parentPath.removePrefix(externalRoot.absolutePath).trimStart('/')
                if (relative.isNotEmpty()) {
                    val topLevel = relative.substringBefore('/')
                    if (topLevel.isNotEmpty()) {
                        candidates.add(File(externalRoot, topLevel))
                    }
                }
            } else {
                candidates.add(parent)
            }
        }

        val existingRoots =
            candidates
                .map { it.absoluteFile }
                .distinctBy { it.absolutePath }
                .filter { it.exists() && it.isDirectory }
                .filterNot { directoryResolver.isBlocked(it.absolutePath) }

        if (existingRoots.isEmpty()) return emptyList()
        return pruneNestedRoots(existingRoots)
    }

    private fun pruneNestedRoots(roots: List<File>): List<File> {
        val normalizedRoots = roots.distinctBy { it.absolutePath.trimEnd('/') }
        val kept = mutableListOf<File>()
        normalizedRoots
            .sortedBy { it.absolutePath.length }
            .forEach { candidate ->
                val candidatePath = candidate.absolutePath.trimEnd('/')
                val coveredByParent = kept.any { root ->
                    val rootPath = root.absolutePath.trimEnd('/')
                    candidatePath == rootPath || candidatePath.startsWith("$rootPath/")
                }
                if (!coveredByParent) {
                    kept.add(candidate)
                }
            }
        return kept
    }

    /**
     * Fetches all IDs currently available in MediaStore to identify deleted songs.
     */
    private fun fetchMediaStoreIds(directoryResolver: DirectoryRuleResolver): Set<Long> {
        val ids = mutableSetOf<Long>()
        val projection = arrayOf(MediaStore.Audio.Media._ID, MediaStore.Audio.Media.DATA)
        val (selection, selectionArgs) = buildLocalAudioSelection(minSongDurationMs)

        contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            null
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val dataCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
            while (cursor.moveToNext()) {
                val data = cursor.getString(dataCol)
                if (data != null) {
                    val parentPath = File(data).parent
                    if (parentPath != null && directoryResolver.isBlocked(File(parentPath).absolutePath)) {
                        continue
                    }
                }
                ids.add(cursor.getLong(idCol))
            }
        }
        return ids
    }

    /**
     * Fetches all file paths currently known to MediaStore.
     * Used to identify new files that need scanning.
     */
    private fun fetchMediaStoreFilePaths(): Set<String> {
        val paths = HashSet<String>()
        val projection = arrayOf(MediaStore.Audio.Media.DATA)
        val (selection, selectionArgs) = buildLocalAudioSelection(minSongDurationMs)
        
        contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            null
        )?.use { cursor ->
            val dataCol = cursor.getColumnIndex(MediaStore.Audio.Media.DATA)
            if (dataCol >= 0) {
                while (cursor.moveToNext()) {
                    cursor.getString(dataCol)?.let { paths.add(it) }
                }
            }
        }
        return paths
    }

    companion object {
        const val WORK_NAME = "com.unshoo.pixelmusic.data.worker.SyncWorker"
        private const val TAG = "SyncWorker"
        const val INPUT_FORCE_METADATA = "input_force_metadata"
        const val INPUT_SYNC_MODE = "input_sync_mode"

        // Progress reporting constants
        const val PROGRESS_CURRENT = "progress_current"
        const val PROGRESS_TOTAL = "progress_total"
        const val PROGRESS_PHASE = "progress_phase"
        const val OUTPUT_TOTAL_SONGS = "output_total_songs"
        private const val NETEASE_SONG_ID_OFFSET = 3_000_000_000_000L
        private const val NETEASE_ALBUM_ID_OFFSET = 4_000_000_000_000L
        private const val NETEASE_ARTIST_ID_OFFSET = 5_000_000_000_000L
        private const val NETEASE_PARENT_DIRECTORY = "/Cloud/Netease"
        private const val NETEASE_GENRE = "Netease Cloud"

        private const val YOUTUBE_SONG_ID_OFFSET = 15_000_000_000_000L
        private const val YOUTUBE_ALBUM_ID_OFFSET = 16_000_000_000_000L
        private const val YOUTUBE_ARTIST_ID_OFFSET = 17_000_000_000_000L
        private const val YOUTUBE_PARENT_DIRECTORY = "/Cloud/YouTube"
        private const val YOUTUBE_GENRE = "YouTube"

        // Genre cache - shared across worker instances to avoid refetching on incremental syncs
        private const val GENRE_CACHE_TTL_MS = 60 * 60 * 1000L // 1 hour
        @Volatile private var genreMapCache: Map<Long, String> = emptyMap()
        @Volatile private var genreMapCacheTimestamp: Long = 0L
        
        fun invalidateGenreCache() {
            genreMapCache = emptyMap()
            genreMapCacheTimestamp = 0L
            Log.d(TAG, "Genre cache invalidated")
        }

        fun startUpSyncWork(deepScan: Boolean = false) =
                OneTimeWorkRequestBuilder<SyncWorker>()
                        .setInputData(
                                workDataOf(
                                        INPUT_FORCE_METADATA to deepScan,
                                        INPUT_SYNC_MODE to SyncMode.INCREMENTAL.name
                                )
                        )
                        .build()

        fun incrementalSyncWork() =
                OneTimeWorkRequestBuilder<SyncWorker>()
                        .setInputData(workDataOf(INPUT_SYNC_MODE to SyncMode.INCREMENTAL.name))
                        .build()

        // Full rescans and rebuilds do heavy bulk writes to Room + the album art cache.
        // Requiring non-critical storage prevents partial/corrupt syncs when the device is
        // nearly full. Not applied to incremental/startup sync so the library still appears
        // immediately when the user opens the app.
        private val heavySyncConstraints: Constraints =
                Constraints.Builder()
                        .setRequiresStorageNotLow(true)
                        .build()

        fun fullSyncWork(deepScan: Boolean = false) =
                OneTimeWorkRequestBuilder<SyncWorker>()
                        .setInputData(
                                workDataOf(
                                        INPUT_SYNC_MODE to SyncMode.FULL.name,
                                        INPUT_FORCE_METADATA to deepScan
                                )
                        )
                        .setConstraints(heavySyncConstraints)
                        .build()

        fun rebuildDatabaseWork() =
                OneTimeWorkRequestBuilder<SyncWorker>()
                        .setInputData(workDataOf(INPUT_SYNC_MODE to SyncMode.REBUILD.name))
                        .setConstraints(heavySyncConstraints)
                        .build()
    }
                
    private suspend fun syncYoutubeData() {
        Log.i(TAG, "Syncing YouTube songs to main database (Unified Mode)...")
        try {
            val settings = youtubeDatastoreRepository.settings.first()
            val appDatabase = com.unshoo.pixelmusic.data.database.youtube.AppDatabase.getInstance(applicationContext)
            val isMobile = isMobileNetwork(applicationContext)
            val localPlaylistsExist = appDatabase.playlistRepository().getAll().isNotEmpty()

            // 1. Fetch remote user-created playlists (delta sync — only insert new songs)
            var remotePlaylistsSuccess = false
            try {
                val remotePlaylists = YoutubePlaylistDataSource().retrieveAll(settings)
                remotePlaylists.forEach { playlistInfo ->
                    val existingPlaylist = appDatabase.playlistRepository().getPlaylistById(playlistInfo.id)
                    val existingSongCount = existingPlaylist?.info?.lastSyncSongCount ?: 0

                    val emptyPlaylist = Playlist(playlistInfo, emptyList())
                    val fullPlaylist = YoutubePlaylistDataSource().retrieveOne(emptyPlaylist, settings)

                    // Delta check: only process if song count changed or first sync
                    if (fullPlaylist.songs.size != existingSongCount || existingPlaylist == null) {
                        // Use preserving insert — keeps existing downloaded songs intact
                        appDatabase.playlistRepository().insertPlaylistWithSongsPreserving(
                            fullPlaylist,
                            appDatabase.songRepository()
                        )
                        Log.i(TAG, "Delta synced playlist '${playlistInfo.title}': ${fullPlaylist.songs.size} songs (was $existingSongCount)")
                    } else {
                        Log.d(TAG, "Skipping playlist '${playlistInfo.title}' — no changes (${existingSongCount} songs)")
                    }
                    // NOTE: Auto-download removed. Users download via playlist options menu (Component 24).
                }
                remotePlaylistsSuccess = true
            } catch (e: Exception) {
                Log.e(TAG, "Failed to fetch remote YouTube playlists", e)
            }

            if (!remotePlaylistsSuccess && settings.cookies.raw.isNotBlank()) {
                Log.w(TAG, "SyncWorker: Remote YouTube playlist fetch failed, but user is logged in. Aborting YouTube sync to prevent wiping local synchronized library.")
                return
            }

            // 2. Fetch and sync Liked Songs playlist ("LM") — delta sync and mapping removed
            val remoteLikedSongsList: List<com.unshoo.pixelmusic.data.model.youtube.Song> = emptyList()

            // 3. Fetch missing genres (rate limited: 10 per sync cycle)
            if (!isMobile) {
                try {
                    val songsWithoutGenre = appDatabase.songRepository().getSongsWithoutGenre()
                    val targetSongs = songsWithoutGenre
                        .sortedByDescending { it.downloadTimestamp }
                        .take(10)

                    targetSongs.forEach { song ->
                        val fetchedGenre = com.unshoo.pixelmusic.data.remote.youtube.YoutubeHelper.extractGenre(song.youtubeId)
                        if (fetchedGenre != null) {
                            appDatabase.songRepository().updateGenre(song.youtubeId, fetchedGenre)
                            val unifiedId = toUnifiedYoutubeSongId(song.youtubeId)
                            musicDao.updateSongGenre(unifiedId, fetchedGenre)
                            Log.i(TAG, "Fetched genre '$fetchedGenre' for YouTube song '${song.title}'")
                        }
                        kotlinx.coroutines.delay(500)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to fetch and update YouTube genres", e)
                }
            } else {
                Log.i(TAG, "SyncWorker: Mobile data active. Skipping missing genres network fetch.")
            }

            val allPlaylists = playlistPreferencesRepository.getPlaylistsOnce()
            val localPlaylistSongIds = allPlaylists
                .filter { it.source == "LOCAL" || it.source == "SMART" }
                .flatMap { it.songIds }
                .mapNotNull { it.toLongOrNull() }
                .toSet()

            val youtubePlaylists = appDatabase.playlistRepository().getAll()
            val downloadedSongs = appDatabase.songRepository().getDownloadedSongs()

            val existingUnifiedYoutubeIds = musicDao.getAllYoutubeSongIds()

            if (youtubePlaylists.isEmpty() && downloadedSongs.isEmpty() && remoteLikedSongsList.isEmpty()) {
                if (existingUnifiedYoutubeIds.isNotEmpty()) {
                    val toDelete = existingUnifiedYoutubeIds.filter { it !in localPlaylistSongIds }
                    if (toDelete.isNotEmpty()) {
                        musicDao.deleteSongsAndRelatedData(toDelete)
                    }
                }
                // Also delete all YouTube playlists in the main DB
                allPlaylists.filter { it.source == "YOUTUBE" }.forEach {
                    playlistPreferencesRepository.deletePlaylist(it.id)
                }
                Log.d(TAG, "No YouTube songs/playlists to sync.")
                return
            }

            // Combine all unique songs
            val allUniqueSongs = mutableMapOf<String, com.unshoo.pixelmusic.data.model.youtube.Song>()
            downloadedSongs.forEach { allUniqueSongs[it.youtubeId] = it }
            youtubePlaylists.forEach { playlist ->
                playlist.songs.forEach { allUniqueSongs[it.youtubeId] = it }
            }
            remoteLikedSongsList.forEach { allUniqueSongs[it.youtubeId] = it }

            val localFavorites = favoritesDao.getFavoriteSongIdsOnce().toSet()

            val songsToInsert = ArrayList<SongEntity>(allUniqueSongs.size)
            val artistsToInsert = LinkedHashMap<Long, ArtistEntity>()
            val albumsToInsert = LinkedHashMap<Long, AlbumEntity>()
            val crossRefsToInsert = mutableListOf<SongArtistCrossRef>()

            allUniqueSongs.values.forEach { ySong ->
                val songId = toUnifiedYoutubeSongId(ySong.youtubeId)
                val artistNames = parseYoutubeArtistNames(ySong.artist)
                val primaryArtistName = artistNames.firstOrNull() ?: "Unknown Artist"
                val primaryArtistId = toUnifiedYoutubeArtistId(primaryArtistName)

                artistNames.forEachIndexed { index, artistName ->
                    val artistId = toUnifiedYoutubeArtistId(artistName)
                    artistsToInsert.putIfAbsent(
                        artistId,
                        ArtistEntity(
                            id = artistId,
                            name = artistName,
                            trackCount = 0,
                            imageUrl = null
                        )
                    )
                    crossRefsToInsert.add(
                        SongArtistCrossRef(
                            songId = songId,
                            artistId = artistId,
                            isPrimary = index == 0
                        )
                    )
                }

                val albumId = toUnifiedYoutubeAlbumId("YouTube Music")
                val albumName = "YouTube Music"
                albumsToInsert.putIfAbsent(
                    albumId,
                    AlbumEntity(
                        id = albumId,
                        title = albumName,
                        artistName = primaryArtistName,
                        artistId = primaryArtistId,
                        songCount = 0,
                        dateAdded = System.currentTimeMillis(),
                        year = 0,
                        albumArtUriString = com.unshoo.pixelmusic.data.remote.youtube.upgradeThumbnailUrlToHighQuality(ySong.thumbnailPath ?: ySong.thumbnailHref)
                    )
                )

                // Build artists JSON
                val youtubeArtistRefs = artistNames.mapIndexed { idx, name ->
                    ArtistRef(
                        id = toUnifiedYoutubeArtistId(name),
                        name = name,
                        isPrimary = idx == 0
                    )
                }

                val durationMs = parseDurationStringToMillis(ySong.duration)

                songsToInsert.add(
                    SongEntity(
                        id = songId,
                        title = ySong.title,
                        artistName = ySong.artist.ifBlank { primaryArtistName },
                        artistId = primaryArtistId,
                        albumArtist = null,
                        albumName = albumName,
                        albumId = albumId,
                        contentUriString = "youtube://${ySong.youtubeId}",
                        albumArtUriString = com.unshoo.pixelmusic.data.remote.youtube.upgradeThumbnailUrlToHighQuality(ySong.thumbnailPath ?: ySong.thumbnailHref),
                        duration = durationMs,
                        genre = ySong.genre?.takeIf { it.isNotBlank() } ?: YOUTUBE_GENRE,
                        filePath = ySong.audioFilePath ?: "",
                        parentDirectoryPath = if (!ySong.audioFilePath.isNullOrBlank()) {
                            java.io.File(ySong.audioFilePath).parent ?: YOUTUBE_PARENT_DIRECTORY
                        } else {
                            YOUTUBE_PARENT_DIRECTORY
                        },
                        isFavorite = songId in localFavorites,
                        lyrics = null,
                        trackNumber = 0,
                        year = 0,
                        dateAdded = System.currentTimeMillis(),
                        mimeType = "audio/opus",
                        bitrate = null,
                        sampleRate = null,
                        telegramChatId = null,
                        telegramFileId = null,
                        artistsJson = serializeArtistRefs(youtubeArtistRefs),
                        sourceType = SourceType.YOUTUBE
                    )
                )
            }

            val albumCounts = songsToInsert.groupingBy { it.albumId }.eachCount()
            val finalAlbums = albumsToInsert.values.map { album ->
                album.copy(songCount = albumCounts[album.id] ?: 0)
            }

            val currentUnifiedSongIds = songsToInsert.map { it.id }.toSet()
            val deletedUnifiedSongIds = existingUnifiedYoutubeIds.filter { 
                it !in currentUnifiedSongIds && it !in localPlaylistSongIds 
            }

            musicDao.incrementalSyncMusicData(
                songs = songsToInsert,
                albums = finalAlbums,
                artists = artistsToInsert.values.toList(),
                crossRefs = crossRefsToInsert,
                deletedSongIds = deletedUnifiedSongIds
            )

            // Sync YouTube Playlists to user preferences/database
            val syncedPlaylistIds = youtubePlaylists.map { it.info.id }.toSet()

            // Delete orphaned synced playlists
            allPlaylists.filter { it.source == "YOUTUBE" && it.id !in syncedPlaylistIds }.forEach {
                playlistPreferencesRepository.deletePlaylist(it.id)
            }

            // Upsert actual YouTube playlists
            youtubePlaylists.forEach { playlist ->
                val playlistSongIds = playlist.songs.map { "youtube_${it.youtubeId}" }

                // Check if already exists in main database to avoid overwriting user edits or custom playlist attributes
                val existing = allPlaylists.find { it.id == playlist.info.id }
                if (existing == null) {
                    playlistPreferencesRepository.createPlaylist(
                        name = playlist.info.title,
                        songIds = playlistSongIds,
                        coverImageUri = com.unshoo.pixelmusic.data.remote.youtube.upgradeThumbnailUrlToHighQuality(playlist.info.coverPath ?: playlist.info.coverHref),
                        customId = playlist.info.id,
                        source = "YOUTUBE"
                    )
                } else {
                    playlistPreferencesRepository.updatePlaylist(
                        existing.copy(
                            name = playlist.info.title,
                            songIds = playlistSongIds,
                            coverImageUri = com.unshoo.pixelmusic.data.remote.youtube.upgradeThumbnailUrlToHighQuality(playlist.info.coverPath ?: playlist.info.coverHref)
                        )
                    )
                }
            }

            // 4. Two-way Playlist Sync (Sync local created playlists to YouTube)
            try {
                if (settings.cookies.raw.isNotBlank()) {
                    val remotePlaylists = YoutubePlaylistDataSource().retrieveAll(settings)
                    val localPlaylists = allPlaylists.filter { it.source == "LOCAL" || it.source == "SMART" }
                    
                    localPlaylists.forEach { localPlaylist ->
                        val playlistSongIds = localPlaylist.songIds.map { id ->
                            when {
                                id.startsWith("youtube_") -> toUnifiedYoutubeSongId(id.removePrefix("youtube_"))
                                else -> id.toLongOrNull()
                            }
                        }.filterNotNull()
                        if (playlistSongIds.isNotEmpty()) {
                            val playlistSongs = musicDao.getSongsByIdsListSimple(playlistSongIds)
                            val localYoutubeVideoIds = playlistSongs
                                .filter { it.sourceType == SourceType.YOUTUBE }
                                .map { it.contentUriString.removePrefix("youtube://") }
                                .filter { it.isNotBlank() }
                            
                            if (localYoutubeVideoIds.isNotEmpty()) {
                                val matchingRemote = remotePlaylists.find { it.title.equals(localPlaylist.name, ignoreCase = true) }
                                if (matchingRemote == null) {
                                    Log.i(TAG, "Creating remote YouTube playlist '${localPlaylist.name}' with ${localYoutubeVideoIds.size} songs...")
                                    try {
                                        val result = YouTube.createPlaylist(localPlaylist.name)
                                        if (result.isSuccess) {
                                            val remotePlaylistId = result.getOrThrow()
                                            localYoutubeVideoIds.forEach { videoId ->
                                                YouTube.addToPlaylist(remotePlaylistId, videoId)
                                            }
                                            Log.i(TAG, "Successfully created remote YouTube playlist: $remotePlaylistId")
                                        } else {
                                            Log.e(TAG, "Failed to create remote YouTube playlist", result.exceptionOrNull())
                                        }
                                    } catch (e: Exception) {
                                        Log.e(TAG, "Failed to create remote YouTube playlist", e)
                                    }
                                } else {
                                    // Fetch current remote playlist songs to avoid adding duplicates
                                    try {
                                        val emptyPlaylist = com.unshoo.pixelmusic.data.model.youtube.Playlist(matchingRemote, emptyList())
                                        val fullPlaylist = YoutubePlaylistDataSource().retrieveOne(emptyPlaylist, settings)
                                        val remoteSongVideoIds = fullPlaylist.songs.map { it.youtubeId }.toSet()
                                        val missingVideoIds = localYoutubeVideoIds.filter { it !in remoteSongVideoIds }
                                        
                                        if (missingVideoIds.isNotEmpty()) {
                                            Log.i(TAG, "Adding ${missingVideoIds.size} missing songs to remote YouTube playlist '${localPlaylist.name}'...")
                                            missingVideoIds.forEach { videoId ->
                                                YouTube.addToPlaylist(matchingRemote.id, videoId)
                                            }
                                        }

                                        // Sync deletions back to YouTube
                                        val deletedVideoIds = remoteSongVideoIds - localYoutubeVideoIds.toSet()
                                        if (deletedVideoIds.isNotEmpty()) {
                                            Log.i(TAG, "Removing ${deletedVideoIds.size} deleted songs from remote YouTube playlist '${localPlaylist.name}'...")
                                            deletedVideoIds.forEach { videoId ->
                                                val setVideoIds = YouTube.playlistEntrySetVideoIds(matchingRemote.id, videoId).getOrNull() ?: emptyList()
                                                setVideoIds.forEach { setVideoId ->
                                                    YouTube.removeFromPlaylist(matchingRemote.id, videoId, setVideoId)
                                                }
                                            }
                                        }
                                    } catch (e: Exception) {
                                        Log.e(TAG, "Failed to sync songs to existing remote YouTube playlist", e)
                                    }
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to perform two-way YouTube playlist sync", e)
            }

            // 5. Sync YouTube subscribed artists
            try {
                if (settings.cookies.raw.isNotBlank()) {
                    val artistsJson = com.unshoo.pixelmusic.data.remote.youtube.YoutubeRequestHelper.browse("FEmusic_library_corpus_artists", settings)
                    val remoteArtists = com.unshoo.pixelmusic.data.remote.youtube.YoutubeHelper.extractAccountArtists(artistsJson, settings)
                    
                    val localArtistIdsToSubscribe = mutableListOf<String>()
                    val artistEntitiesToInsert = mutableListOf<ArtistEntity>()
                    remoteArtists.forEach { remoteArtist ->
                        val channelId = remoteArtist.id
                        val artistId = toUnifiedYoutubeArtistId(remoteArtist.name)
                        
                        // Insert artist into db
                        val artistEntity = ArtistEntity(
                            id = artistId,
                            name = remoteArtist.name,
                            trackCount = 0,
                            imageUrl = remoteArtist.thumbnailUrl,
                            channelId = channelId
                        )
                        artistEntitiesToInsert.add(artistEntity)
                        
                        localArtistIdsToSubscribe.add(channelId)
                        localArtistIdsToSubscribe.add(artistId.toString())
                    }
                    
                    if (artistEntitiesToInsert.isNotEmpty()) {
                        musicDao.insertArtists(artistEntitiesToInsert)
                    }
                    
                    // Batch subscribe in preferences
                    if (localArtistIdsToSubscribe.isNotEmpty()) {
                        val currentSubscribed = userPreferencesRepository.subscribedArtistIdsFlow.first()
                        val newSet = currentSubscribed + localArtistIdsToSubscribe
                        newSet.forEach { id ->
                            userPreferencesRepository.subscribeArtist(id, true)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to sync YouTube subscribed artists", e)
            }

            Log.i(TAG, "Synced ${songsToInsert.size} YouTube songs and ${youtubePlaylists.size} playlists.")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to sync YouTube data", e)
        }
    }

    private fun parseYoutubeArtistNames(rawArtist: String): List<String> {
        if (rawArtist.isBlank()) return listOf("Unknown Artist")
        // Split on common separators. Include " and " (with word-boundary spaces) to handle
        // natural-language artist lists like "Armaan Khan and Sameer Khan".
        val parsed = rawArtist
            .split(Regex("\\s*[,/&;+、]\\s*|\\s+and\\s+", RegexOption.IGNORE_CASE))
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()
        return if (parsed.isEmpty()) listOf("Unknown Artist") else parsed
    }

    private fun toUnifiedYoutubeSongId(youtubeId: String): Long {
        return -(YOUTUBE_SONG_ID_OFFSET + youtubeId.hashCode().toLong().absoluteValue)
    }

    private fun toUnifiedYoutubeAlbumId(albumName: String): Long {
        return -(YOUTUBE_ALBUM_ID_OFFSET + albumName.lowercase().hashCode().toLong().absoluteValue)
    }

    private fun toUnifiedYoutubeArtistId(artistName: String): Long {
        return -(YOUTUBE_ARTIST_ID_OFFSET + artistName.lowercase().hashCode().toLong().absoluteValue)
    }

    private fun parseDurationStringToMillis(durationStr: String): Long {
        if (durationStr.isBlank()) return 0L
        val parts = durationStr.split(":")
        var seconds = 0L
        try {
            when (parts.size) {
                1 -> {
                    val raw = parts[0].toLongOrNull() ?: 0L
                    seconds = if (raw >= 1000L) raw / 1000L else raw
                }
                2 -> seconds = (parts[0].toLongOrNull() ?: 0L) * 60 + (parts[1].toLongOrNull() ?: 0L)
                3 -> seconds = (parts[0].toLongOrNull() ?: 0L) * 3600 + (parts[1].toLongOrNull() ?: 0L) * 60 + (parts[2].toLongOrNull() ?: 0L)
            }
        } catch (e: Exception) {
            // ignore
        }
        return seconds * 1000
    }

    private fun isMobileNetwork(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
        val activeNetwork = cm.activeNetwork ?: return false
        val capabilities = cm.getNetworkCapabilities(activeNetwork) ?: return false
        return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
    }
}
