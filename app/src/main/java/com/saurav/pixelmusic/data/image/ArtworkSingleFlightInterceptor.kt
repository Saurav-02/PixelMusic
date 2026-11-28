package com.saurav.pixelmusic.data.image

import android.net.Uri
import coil.intercept.Interceptor
import coil.request.ErrorResult
import coil.request.ImageResult
import com.saurav.pixelmusic.presentation.components.SmartImageCache
import com.saurav.pixelmusic.utils.LocalArtworkUri
import com.saurav.pixelmusic.utils.ThumbnailUrlUtils
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap

/**
 * Ensures that a song's album art is downloaded at most ONCE across all app surfaces
 * (MiniPlayer, Now Playing, Lock Screen, Notification, AOD, Widget, Palette extractor, Preloader)
 * by:
 * 1. Normalizing all remote artwork URLs to the active [AlbumArtQuality] setting (e.g. 720p)
 *    and assigning a single canonical diskCacheKey per song + quality tier.
 * 2. Coalescing concurrent requests for the same canonical artwork key with a per-key [Mutex]
 *    so simultaneous requests (when a track starts playing) wait for the single network download
 *    and then read directly from Coil's MemoryCache / DiskCache.
 * 3. Transparently falling back (`maxresdefault` -> `sddefault` -> `hqdefault`) if a high-res
 *    YouTube thumbnail returns HTTP 404, while storing the result under the same canonical key.
 */
class ArtworkSingleFlightInterceptor : Interceptor {

    private val keyLocks = ConcurrentHashMap<String, Mutex>()

    private fun lockFor(key: String): Mutex {
        if (keyLocks.size > 512) {
            val iterator = keyLocks.entries.iterator()
            while (iterator.hasNext()) {
                val entry = iterator.next()
                if (!entry.value.isLocked) {
                    iterator.remove()
                }
            }
        }
        return keyLocks.computeIfAbsent(key) { Mutex() }
    }

    override suspend fun intercept(chain: Interceptor.Chain): ImageResult {
        val originalRequest = chain.request
        val rawData = originalRequest.data
        val rawUrl = when (rawData) {
            is String -> rawData
            is Uri -> rawData.toString()
            else -> null
        }

        if (rawUrl.isNullOrBlank() ||
            LocalArtworkUri.isLocalArtworkUri(rawUrl) ||
            (!rawUrl.startsWith("http://") && !rawUrl.startsWith("https://") && !rawUrl.startsWith("//"))
        ) {
            return chain.proceed(originalRequest)
        }

        val effectiveQuality = SmartImageCache.getEffectiveQuality()
        val optimizedUrl = ThumbnailUrlUtils.optimizeArtworkUrl(rawUrl, effectiveQuality) ?: rawUrl
        val canonicalKey = ThumbnailUrlUtils.getCanonicalArtworkDiskCacheKey(optimizedUrl, effectiveQuality)
            ?: optimizedUrl

        val normalizedRequest = originalRequest.newBuilder()
            .data(optimizedUrl)
            .diskCacheKey(canonicalKey)
            .build()

        val mutex = lockFor(canonicalKey)
        return mutex.withLock {
            var result = chain.proceed(normalizedRequest)
            if (result is ErrorResult) {
                val fallbacks = ThumbnailUrlUtils.getFallbackArtworkUrls(optimizedUrl)
                for (fallbackUrl in fallbacks) {
                    val fallbackRequest = normalizedRequest.newBuilder()
                        .data(fallbackUrl)
                        .diskCacheKey(canonicalKey)
                        .build()
                    result = chain.proceed(fallbackRequest)
                    if (result !is ErrorResult) {
                        break
                    }
                }
            }
            result
        }
    }
}
