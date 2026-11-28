package com.saurav.pixelmusic.utils

import coil.size.Dimension
import coil.size.Size
import com.saurav.pixelmusic.data.preferences.AlbumArtQuality

object ThumbnailUrlUtils {

    const val DEFAULT_ORIGINAL_SIZE = 720

    private val GOOGLE_EQUALS_SIZE_REGEX = Regex("=[ws]\\d+.*")
    private val GOOGLE_SLASH_SIZE_REGEX = Regex("/(?:w\\d+-h\\d+|s\\d+)(?:-[a-zA-Z0-9]+)*$")
    private val YT_VIDEO_ID_REGEX = Regex("(?:/vi(?:_webp)?/)([a-zA-Z0-9_-]{6,20})")

    private fun hasGoogleSlashSizeSegment(urlWithoutEquals: String): Boolean {
        val afterScheme = urlWithoutEquals.substringAfter("://", "")
        return afterScheme.count { it == '/' } >= 2 &&
            GOOGLE_SLASH_SIZE_REGEX.containsMatchIn(urlWithoutEquals)
    }

    private fun isYouTubeThumbnailHost(url: String): Boolean {
        return url.contains("ytimg.com", ignoreCase = true) ||
            url.contains("img.youtube.com", ignoreCase = true)
    }

    private fun isGoogleContentHost(url: String): Boolean {
        return url.contains("googleusercontent.com", ignoreCase = true) ||
            url.contains("ggpht.com", ignoreCase = true)
    }

    fun extractYouTubeVideoIdFromArtworkUrl(url: String?): String? {
        if (url.isNullOrBlank()) return null
        val clean = url.substringBefore("?")
        YT_VIDEO_ID_REGEX.find(clean)?.groupValues?.getOrNull(1)?.takeIf { it.isNotBlank() }?.let {
            return it
        }
        if (clean.contains("/vi/") || clean.contains("/vi_webp/")) {
            val id = clean.substringAfter("/vi/").substringAfter("/vi_webp/").substringBefore("/")
            if (id.isNotBlank() && !id.contains("http")) return id
        }
        return null
    }

    /**
     * Resolves the effective quality based on network status and user preferences.
     */
    fun getEffectiveQuality(
        isMetered: Boolean,
        qualityWifi: AlbumArtQuality,
        qualityMobile: AlbumArtQuality,
        performanceMode: Boolean
    ): AlbumArtQuality {
        return if (isMetered) qualityMobile else qualityWifi
    }

    private fun resolveQualityTag(url: String, explicitQuality: AlbumArtQuality?): Int {
        if (explicitQuality != null) return explicitQuality.maxSize
        return when {
            url.contains("=w360") || url.contains("=s360") || url.contains("/w360") ||
                url.contains("hqdefault") || url.contains("mqdefault") -> AlbumArtQuality.LOW.maxSize
            url.contains("=w720") || url.contains("=s720") || url.contains("/w720") ||
                url.contains("maxresdefault") || url.contains("sddefault") || url.contains("hq720") -> AlbumArtQuality.HIGH.maxSize
            else -> com.saurav.pixelmusic.presentation.components.SmartImageCache.getEffectiveQuality().maxSize
        }
    }

    /**
     * Extracts a stable, quality-aware canonical cache key for an artwork URL.
     * Ensures that Mini Player, Now Playing (Default, Immersive, Extended, Complex),
     * Lockscreen, Notification, AOD, Palette color extraction, and widgets all share
     * the exact same disk & memory cache entry for a given track at the selected quality,
     * downloading artwork only once per song while respecting 360p vs 720p settings.
     */
    fun getCanonicalArtworkDiskCacheKey(
        url: String?,
        quality: AlbumArtQuality? = null
    ): String? {
        if (url.isNullOrBlank()) return null
        if (LocalArtworkUri.isLocalArtworkUri(url) ||
            url.startsWith("content://") ||
            url.startsWith("file://") ||
            url.startsWith("android.resource://")
        ) {
            return url
        }

        val qTag = resolveQualityTag(url, quality)

        // 1. Google User Content / ggpht (Album covers & artist images)
        if (isGoogleContentHost(url)) {
            val clean = url.substringBefore("?")
            val withoutEquals = if (GOOGLE_EQUALS_SIZE_REGEX.containsMatchIn(clean)) {
                clean.replace(GOOGLE_EQUALS_SIZE_REGEX, "")
            } else if (clean.substringAfterLast("/").contains("=")) {
                clean.substringBeforeLast("=")
            } else {
                clean
            }
            val baseKey = if (hasGoogleSlashSizeSegment(withoutEquals)) {
                withoutEquals.replace(GOOGLE_SLASH_SIZE_REGEX, "")
            } else {
                withoutEquals
            }
            return "${baseKey}_q$qTag"
        }

        // 2. YouTube Video Thumbnails (ytimg.com / img.youtube.com)
        if (isYouTubeThumbnailHost(url)) {
            val videoId = extractYouTubeVideoIdFromArtworkUrl(url)
            if (!videoId.isNullOrBlank()) {
                return "yt_thumb_${videoId}_q$qTag"
            }
            return "${url.substringBefore("?")}_q$qTag"
        }

        return "${url.substringBefore("?")}_q$qTag"
    }

    /**
     * Returns a shared canonical MemoryCache key string.
     * When [targetSize] matches the active quality's full target size (or Size.ORIGINAL),
     * returns [canonicalDiskKey] directly so MiniPlayer, Now Playing, Lock Screen,
     * Notification, and Palette Color Extraction share the exact same decoded Bitmap in RAM.
     */
    fun getCanonicalMemoryCacheKey(
        canonicalDiskKey: String,
        targetSize: Size,
        quality: AlbumArtQuality = com.saurav.pixelmusic.presentation.components.SmartImageCache.getEffectiveQuality()
    ): String {
        if (targetSize == Size.ORIGINAL) return canonicalDiskKey
        val width = (targetSize.width as? Dimension.Pixels)?.px
        val height = (targetSize.height as? Dimension.Pixels)?.px
        if (width != null && height != null) {
            if (width >= quality.maxSize && height >= quality.maxSize) {
                return canonicalDiskKey
            }
            return "${canonicalDiskKey}_${width}x${height}"
        }
        return canonicalDiskKey
    }

    /**
     * Transforms an image/artwork URL to match the specified [quality] setting.
     * Guarantees a single, canonical URL representation across all app components
     * (Home, Explore, Search, Now Playing, Notification, Lockscreen, AOD, Widgets, etc.)
     * so that Coil downloads each artwork once and reuses the disk cache everywhere.
     */
    fun optimizeArtworkUrl(url: String?, quality: AlbumArtQuality): String? {
        if (url.isNullOrBlank()) return url
        if (LocalArtworkUri.isLocalArtworkUri(url) ||
            url.startsWith("content://") ||
            url.startsWith("file://") ||
            url.startsWith("android.resource://")
        ) {
            return url
        }

        val transformed = if (url.startsWith("//")) "https:$url" else url

        // 1. Google User Content / ggpht (Album covers & artist images)
        if (isGoogleContentHost(transformed)) {
            val clean = transformed.substringBefore("?")
            val targetPx = quality.maxSize
            return when {
                GOOGLE_EQUALS_SIZE_REGEX.containsMatchIn(clean) ->
                    clean.replace(GOOGLE_EQUALS_SIZE_REGEX, "=w$targetPx-h$targetPx-l90-rj")
                hasGoogleSlashSizeSegment(clean) ->
                    clean.replace(GOOGLE_SLASH_SIZE_REGEX, "/w$targetPx-h$targetPx-l90-rj")
                clean.substringAfterLast("/").contains("=") ->
                    clean.substringBeforeLast("=") + "=w$targetPx-h$targetPx-l90-rj"
                else -> "$clean=w$targetPx-h$targetPx-l90-rj"
            }
        }

        // 2. YouTube Video Thumbnails (ytimg.com / img.youtube.com)
        if (isYouTubeThumbnailHost(transformed)) {
            val videoId = extractYouTubeVideoIdFromArtworkUrl(transformed) ?: return transformed
            val ytRes = when (quality) {
                AlbumArtQuality.LOW -> "hqdefault" // 480x360 (360p)
                AlbumArtQuality.HIGH -> "maxresdefault" // 1280x720 (720p)
            }
            return "https://i.ytimg.com/vi/$videoId/$ytRes.jpg"
        }

        return transformed
    }

    /**
     * Ordered list of fallback URLs in case maxresdefault.jpg returns HTTP 404.
     * Tries sddefault.jpg (640x480) first, then hqdefault.jpg (480x360).
     */
    fun getFallbackArtworkUrls(url: String?): List<String> {
        if (url.isNullOrBlank()) return emptyList()
        return when {
            url.contains("maxresdefault") -> listOf(
                url.replace("maxresdefault", "sddefault"),
                url.replace("maxresdefault", "hqdefault")
            )
            url.contains("sddefault") -> listOf(
                url.replace("sddefault", "hqdefault")
            )
            else -> emptyList()
        }
    }

    /**
     * Fallback URL in case maxresdefault.jpg or sddefault.jpg returns HTTP 404
     */
    fun getFallbackArtworkUrl(url: String?): String? {
        return getFallbackArtworkUrls(url).firstOrNull()
    }
}
