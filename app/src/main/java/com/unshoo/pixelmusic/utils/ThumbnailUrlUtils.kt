package com.unshoo.pixelmusic.utils

import com.unshoo.pixelmusic.data.preferences.AlbumArtQuality

object ThumbnailUrlUtils {

    const val DEFAULT_ORIGINAL_SIZE = 1200

    /**
     * Resolves the effective quality based on network status and user preferences.
     */
    fun getEffectiveQuality(
        isMetered: Boolean,
        qualityWifi: AlbumArtQuality,
        qualityMobile: AlbumArtQuality,
        performanceMode: Boolean
    ): AlbumArtQuality {
        return when {
            performanceMode -> AlbumArtQuality.LOW
            isMetered -> qualityMobile
            else -> qualityWifi
        }
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

        var transformed = url

        // 1. Google User Content / ggpht (Album covers & artist images)
        if (transformed.contains("googleusercontent.com") || transformed.contains("ggpht.com")) {
            val targetPx = if (quality.maxSize > 0) quality.maxSize else DEFAULT_ORIGINAL_SIZE
            val sizeParamRegex = Regex("=[ws]\\d+.*")
            val slashSizeRegex = Regex("/[ws]\\d+.*")
            return when {
                sizeParamRegex.containsMatchIn(transformed) -> transformed.replace(sizeParamRegex, "=w$targetPx-h$targetPx-l90-rj")
                slashSizeRegex.containsMatchIn(transformed) -> transformed.replace(slashSizeRegex, "/w$targetPx-h$targetPx-l90-rj")
                transformed.contains("=") -> transformed.substringBeforeLast("=") + "=w$targetPx-h$targetPx-l90-rj"
                else -> "$transformed=w$targetPx-h$targetPx-l90-rj"
            }
        }

        // 2. YouTube Video Thumbnails (i.ytimg.com)
        if (transformed.contains("i.ytimg.com")) {
            val ytRes = when (quality) {
                AlbumArtQuality.LOW -> "mqdefault"
                AlbumArtQuality.MEDIUM -> "sddefault"
                AlbumArtQuality.HIGH -> "hqdefault"
                AlbumArtQuality.ORIGINAL -> "maxresdefault"
            }
            val filenameRegex = Regex("(maxresdefault|sddefault|hqdefault|mqdefault|default)\\.(jpg|webp)")
            if (filenameRegex.containsMatchIn(transformed)) {
                return transformed.replace(filenameRegex, "$ytRes.$2")
            }
            if (transformed.contains("/vi/") || transformed.contains("/vi_webp/")) {
                val videoId = transformed.substringAfter("/vi/").substringAfter("/vi_webp/").substringBefore("/")
                if (videoId.isNotBlank() && !videoId.contains("http")) {
                    val isWebp = transformed.contains("/vi_webp/")
                    val ext = if (isWebp) "webp" else "jpg"
                    return "https://i.ytimg.com/vi/$videoId/$ytRes.$ext"
                }
            }
            return transformed
        }

        return transformed
    }
}
