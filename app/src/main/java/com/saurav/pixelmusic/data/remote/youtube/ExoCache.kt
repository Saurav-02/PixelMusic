package com.saurav.pixelmusic.data.remote.youtube

import android.content.Context
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.cache.Cache
import androidx.media3.datasource.cache.CacheEvictor
import androidx.media3.datasource.cache.CacheSpan
import androidx.media3.datasource.cache.SimpleCache
import com.saurav.pixelmusic.data.preferences.UserPreferencesRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.util.TreeSet

@UnstableApi
class DynamicLruCacheEvictor(
    private var maxBytes: Long
) : CacheEvictor {
    private var currentSize: Long = 0
    private val leastRecentlyUsed = TreeSet<CacheSpan> { span1, span2 ->
        val lastTouchTimestampDelta = span1.lastTouchTimestamp - span2.lastTouchTimestamp
        if (lastTouchTimestampDelta == 0L) {
            span1.compareTo(span2)
        } else {
            if (lastTouchTimestampDelta < 0) -1 else 1
        }
    }

    override fun requiresCacheSpanTouches() = true
    override fun onCacheInitialized() {}

    override fun onStartFile(cache: Cache, cacheKey: String, position: Long, length: Long) {
        if (length != -1L) { // FIXED: Replaced C.LENGTH_UNSET with explicit Long to fix compiler error
            evictCache(cache, length)
        }
    }

    override fun onSpanAdded(cache: Cache, span: CacheSpan) {
        leastRecentlyUsed.add(span)
        currentSize += span.length
        evictCache(cache, 0)
    }

    override fun onSpanRemoved(cache: Cache, span: CacheSpan) {
        leastRecentlyUsed.remove(span)
        currentSize -= span.length
    }

    override fun onSpanTouched(cache: Cache, oldSpan: CacheSpan, newSpan: CacheSpan) {
        onSpanRemoved(cache, oldSpan)
        onSpanAdded(cache, newSpan)
    }

    fun updateMaxBytes(cache: Cache?, newMaxBytes: Long) {
        maxBytes = newMaxBytes
        if (cache != null) {
            evictCache(cache, 0) // Triggers immediate cleanup of older files if limit shrank
        }
    }

    private fun evictCache(cache: Cache, requiredSpace: Long) {
        while (currentSize + requiredSpace > maxBytes && leastRecentlyUsed.isNotEmpty()) {
            try {
                cache.removeSpan(leastRecentlyUsed.first())
            } catch (e: Exception) {
                // Ignore removal errors
            }
        }
    }
}

@UnstableApi
class ExoCache(
    private val context: Context,
    private val userPreferencesRepository: UserPreferencesRepository
) {
    // Starts with a 2GB default; the flow instantly overrides this on boot
    private val evictor = DynamicLruCacheEvictor(2048L * 1024 * 1024)
    private val cacheDir = File(context.cacheDir, Constants.ExoPlayer.Cache.NAME)
    
    val cache: SimpleCache by lazy {
        val simpleCache = SimpleCache(
            cacheDir,
            evictor,
            databaseProvider
        )

        // Listen to the slider! Updates the evictor dynamically when changed
        CoroutineScope(Dispatchers.IO).launch {
            userPreferencesRepository.storageLimitMbFlow.collect { limitMb ->
                val newLimitBytes = if (limitMb <= 0) Long.MAX_VALUE else limitMb.toLong() * 1024 * 1024
                evictor.updateMaxBytes(simpleCache, newLimitBytes)
            }
        }

        simpleCache
    }

    fun clearAllCache() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // FIXED: Dropped the 'isInitialized' lateinit check. 
                // Calling `cache` here safely initializes it so we can iterate and wipe the spans.
                val keysToClear = cache.keys.toList()
                keysToClear.forEach { key ->
                    cache.getCachedSpans(key).forEach { span ->
                        cache.removeSpan(span)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                // Fallback aggressive wipe if the engine fails to delete the spans
                SimpleCache.delete(cacheDir, databaseProvider)
            }
        }
    }

    fun release() {
        try {
            cache.release()
        } catch (e: Exception) {
            // Safely ignore if the cache was never initialized to begin with
        }
    }

    private val databaseProvider by lazy { StandaloneDatabaseProvider(context) }
}
