package com.saurav.pixelmusic.data.remote.youtube

import android.content.Context
import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.CacheWriter
import com.saurav.pixelmusic.data.model.youtube.Song
import com.saurav.pixelmusic.data.remote.youtube.UmihiHelper.printd
import com.saurav.pixelmusic.data.remote.youtube.UmihiHelper.printe
import com.saurav.pixelmusic.data.service.player.DualPlayerEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@OptIn(UnstableApi::class)
object QueuePreloadManager {

    private var preloadJob: Job? = null
    private var watcherJob: Job? = null
    private var scope: CoroutineScope? = null
    private var appContext: Context? = null
    private var datastoreRepository: DatastoreRepository? = null
    private var playerRef: Player? = null
    private var exoCache: ExoCache? = null
    private var engineRef: DualPlayerEngine? = null
    
    // Track the last index we preloaded so we don't duplicate work
    private var lastPreloadedIndex: Int = -1

    private val playerListener = object : Player.Listener {
        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            startProgressWatcher()
        }
    }

    fun attach(
        player: Player,
        context: Context,
        datastoreRepo: DatastoreRepository,
        coroutineScope: CoroutineScope,
        exoCacheInstance: ExoCache,
        engine: DualPlayerEngine? = null
    ) {
        scope = coroutineScope
        appContext = context.applicationContext
        datastoreRepository = datastoreRepo
        playerRef = player
        exoCache = exoCacheInstance
        engineRef = engine
        player.addListener(playerListener)
        startProgressWatcher()
        printd("QueuePreloadManager attached")
    }

    fun detach(player: Player?) {
        player?.removeListener(playerListener)
        playerRef = null
        watcherJob?.cancel()
        preloadJob?.cancel()
        scope = null
        appContext = null
        datastoreRepository = null
        exoCache = null
    }

    fun updatePlayer(newPlayer: Player) {
        val oldPlayer = playerRef
        if (oldPlayer !== newPlayer) {
            oldPlayer?.removeListener(playerListener)
            playerRef = newPlayer
            newPlayer.addListener(playerListener)
            startProgressWatcher()
            printd("QueuePreloadManager player updated")
        }
    }

    fun onControllerReady(player: Player) {
        updatePlayer(player)
    }

    private fun startProgressWatcher() {
        val currentScope = scope ?: return
        watcherJob?.cancel()
        watcherJob = currentScope.launch(Dispatchers.Default) {
            while (isActive) {
                // Safely read ExoPlayer state on the Main thread to prevent crashes
                val playerState = withContext(Dispatchers.Main) {
                    val player = playerRef
                    if (player != null) {
                        Triple(player.duration, player.currentPosition, player.currentMediaItemIndex)
                    } else null
                }
                
                if (playerState == null) break
                
                val (duration, position, currentIndex) = playerState
                
                // Trigger preloading when 50% completed AND we haven't processed this index yet
                if (duration > 0 && position >= duration / 2 && currentIndex != lastPreloadedIndex) {
                    lastPreloadedIndex = currentIndex
                    triggerPreload()
                    break // Stop watching until the next track transition restarts it
                }
                delay(1000)
            }
        }
    }

    private fun triggerPreload() {
        val currentScope = scope ?: return
        val player = playerRef ?: return
        val ctx = appContext ?: return

        preloadJob?.cancel()
        preloadJob = currentScope.launch(Dispatchers.IO) {
            val settings = datastoreRepository?.settings?.first() ?: return@launch
            if (!settings.preloadQueueEnabled) return@launch

            val playerState = withContext(Dispatchers.Main) {
                if (playerRef == null) null
                else Pair(player.currentMediaItemIndex, player.mediaItemCount)
            } ?: return@launch

            val (currentIndex, totalCount) = playerState
            
            // Limit preloading to user preferences (defaults to 2 if updated)
            val indicesAhead = 
                (currentIndex + 1)..(currentIndex + settings.preloadQueueSize).coerceAtMost(totalCount - 1)

            for (i in indicesAhead) {
                val mediaItem = withContext(Dispatchers.Main) {
                    if (playerRef != null && i < player.mediaItemCount) player.getMediaItemAt(i) else null
                } ?: continue

                val videoId = mediaItem.mediaId
                if (videoId.isBlank()) continue

                val song = Song(
                    youtubeId = videoId,
                    title = mediaItem.mediaMetadata.title?.toString() ?: "",
                    artist = mediaItem.mediaMetadata.artist?.toString() ?: "",
                    thumbnailHref = mediaItem.mediaMetadata.artworkUri?.toString().orEmpty()
                )

                var streamUrl: String? = null
                try {
                    // Safe call to the new NewPipe Extractor setup
                    streamUrl = YoutubeHelper.getSongPlayerUrl(ctx, song, allowLocal = false)
                    printd("QueuePreloadManager: preloaded stream URL for $videoId")
                } catch (e: Exception) {
                    printe("QueuePreloadManager: failed to preload stream for $videoId: ${e.message}")
                }

                if (!streamUrl.isNullOrBlank() && streamUrl.startsWith("http")) {
                    prefetchAudioBytes(ctx, videoId, streamUrl)
                }

                val thumbnailUrl = song.thumbnailHref
                if (thumbnailUrl.isNotBlank()) {
                    try {
                        val optimizedUrl = com.saurav.pixelmusic.utils.ThumbnailUrlUtils.optimizeArtworkUrl(
                            thumbnailUrl,
                            com.saurav.pixelmusic.presentation.components.SmartImageCache.getEffectiveQuality()
                        )
                        if (!optimizedUrl.isNullOrBlank()) {
                            val request = coil.request.ImageRequest.Builder(ctx)
                                .data(optimizedUrl)
                                .diskCacheKey(optimizedUrl)
                                .build()
                            coil.Coil.imageLoader(ctx).enqueue(request)
                            printd("QueuePreloadManager: enqueued thumbnail preload for $videoId")
                        }
                    } catch (e: Exception) {
                        printe("QueuePreloadManager: failed to preload thumbnail for $videoId: ${e.message}")
                    }
                }
                delay(500)
            }
        }
    }

    private suspend fun prefetchAudioBytes(ctx: Context, videoId: String, streamUrl: String) {
        val cache = exoCache?.cache ?: return
        try {
            val uri = Uri.parse(streamUrl)
            val baseDataSourceFactory = DefaultDataSource.Factory(ctx)
            val cacheDataSourceFactory = CacheDataSource.Factory()
                .setCache(cache)
                .setUpstreamDataSourceFactory(baseDataSourceFactory)
                .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)

            val dataSource = cacheDataSourceFactory.createDataSource()
            val dataSpec = DataSpec.Builder()
                .setUri(uri)
                .setPosition(0)
                .setLength(512 * 1024)
                .build()

            val parentJob = kotlin.coroutines.coroutineContext[kotlinx.coroutines.Job]
            val progressListener = CacheWriter.ProgressListener { _, _, _ ->
                if (parentJob != null && !parentJob.isActive) {
                    throw InterruptedException("Prefetch canceled")
                }
            }

            val cacheWriter = CacheWriter(dataSource, dataSpec, null, progressListener)
            withContext(Dispatchers.IO) { cacheWriter.cache() }
        } catch (e: Exception) {
            if (e !is InterruptedException) {
                printe("QueuePreloadManager: failed to prefetch audio bytes for $videoId: ${e.message}")
            }
        }
    }
}
