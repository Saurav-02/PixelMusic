package com.saurav.pixelmusic.utils

import com.google.common.truth.Truth.assertThat
import com.saurav.pixelmusic.data.preferences.AlbumArtQuality
import org.junit.Test

class ThumbnailUrlUtilsTest {

    @Test
    fun optimizeArtworkUrl_googleUserContent_highQuality() {
        val input = "https://lh3.googleusercontent.com/xyz=w400-h400"
        val output = ThumbnailUrlUtils.optimizeArtworkUrl(input, AlbumArtQuality.HIGH)
        assertThat(output).isEqualTo("https://lh3.googleusercontent.com/xyz=w720-h720-l90-rj")
    }

    @Test
    fun optimizeArtworkUrl_googleUserContent_lowQuality() {
        val input = "https://lh3.googleusercontent.com/xyz"
        val output = ThumbnailUrlUtils.optimizeArtworkUrl(input, AlbumArtQuality.LOW)
        assertThat(output).isEqualTo("https://lh3.googleusercontent.com/xyz=w360-h360-l90-rj")
    }

    @Test
    fun optimizeArtworkUrl_youtubeThumbnail_standardReplacements() {
        val inputHq = "https://i.ytimg.com/vi/dQw4w9WgXcQ/hqdefault.jpg"
        assertThat(ThumbnailUrlUtils.optimizeArtworkUrl(inputHq, AlbumArtQuality.HIGH))
            .isEqualTo("https://i.ytimg.com/vi/dQw4w9WgXcQ/maxresdefault.jpg")

        val inputMax = "https://i.ytimg.com/vi/dQw4w9WgXcQ/maxresdefault.jpg"
        assertThat(ThumbnailUrlUtils.optimizeArtworkUrl(inputMax, AlbumArtQuality.LOW))
            .isEqualTo("https://i.ytimg.com/vi/dQw4w9WgXcQ/hqdefault.jpg")

        assertThat(ThumbnailUrlUtils.optimizeArtworkUrl(inputMax, AlbumArtQuality.HIGH))
            .isEqualTo("https://i.ytimg.com/vi/dQw4w9WgXcQ/maxresdefault.jpg")
    }

    @Test
    fun optimizeArtworkUrl_localArtwork_untouched() {
        val localArt = "pixelmusic_local_art://song/42"
        assertThat(ThumbnailUrlUtils.optimizeArtworkUrl(localArt, AlbumArtQuality.HIGH))
            .isEqualTo(localArt)

        val contentArt = "content://media/external/audio/albumart/1"
        assertThat(ThumbnailUrlUtils.optimizeArtworkUrl(contentArt, AlbumArtQuality.LOW))
            .isEqualTo(contentArt)
    }

    @Test
    fun getEffectiveQuality_respectsPreferencesAndNetwork() {
        // Performance mode forces LOW
        assertThat(
            ThumbnailUrlUtils.getEffectiveQuality(
                isMetered = false,
                qualityWifi = AlbumArtQuality.HIGH,
                qualityMobile = AlbumArtQuality.LOW,
                performanceMode = true
            )
        ).isEqualTo(AlbumArtQuality.LOW)

        // Metered network uses mobile quality
        assertThat(
            ThumbnailUrlUtils.getEffectiveQuality(
                isMetered = true,
                qualityWifi = AlbumArtQuality.HIGH,
                qualityMobile = AlbumArtQuality.LOW,
                performanceMode = false
            )
        ).isEqualTo(AlbumArtQuality.LOW)

        // Unmetered network uses wifi quality
        assertThat(
            ThumbnailUrlUtils.getEffectiveQuality(
                isMetered = false,
                qualityWifi = AlbumArtQuality.HIGH,
                qualityMobile = AlbumArtQuality.LOW,
                performanceMode = false
            )
        ).isEqualTo(AlbumArtQuality.HIGH)
    }

    @Test
    fun getCanonicalArtworkDiskCacheKey_sharesKeyAcrossResolutionsAndDistinguishesDifferentTracks() {
        val googleHigh = "https://lh3.googleusercontent.com/xyz=w720-h720-l90-rj"
        val googleLow = "https://lh3.googleusercontent.com/xyz=w360-h360-l90-rj"
        val googleSlash = "https://lh3.googleusercontent.com/xyz/w360-h360-l90-rj"
        val googleBase = "https://lh3.googleusercontent.com/xyz"
        val googleOtherTrack = "https://lh3.googleusercontent.com/abc_other_song=w720-h720-l90-rj"

        assertThat(ThumbnailUrlUtils.getCanonicalArtworkDiskCacheKey(googleHigh))
            .isEqualTo("https://lh3.googleusercontent.com/xyz")
        assertThat(ThumbnailUrlUtils.getCanonicalArtworkDiskCacheKey(googleHigh))
            .isEqualTo(ThumbnailUrlUtils.getCanonicalArtworkDiskCacheKey(googleLow))
        assertThat(ThumbnailUrlUtils.getCanonicalArtworkDiskCacheKey(googleHigh))
            .isEqualTo(ThumbnailUrlUtils.getCanonicalArtworkDiskCacheKey(googleSlash))
        assertThat(ThumbnailUrlUtils.getCanonicalArtworkDiskCacheKey(googleHigh))
            .isEqualTo(ThumbnailUrlUtils.getCanonicalArtworkDiskCacheKey(googleBase))
        assertThat(ThumbnailUrlUtils.getCanonicalArtworkDiskCacheKey(googleHigh))
            .isNotEqualTo(ThumbnailUrlUtils.getCanonicalArtworkDiskCacheKey(googleOtherTrack))

        val ytMax = "https://i.ytimg.com/vi/dQw4w9WgXcQ/maxresdefault.jpg"
        val ytHq = "https://i.ytimg.com/vi/dQw4w9WgXcQ/hqdefault.jpg"
        val ytWebp = "https://i.ytimg.com/vi_webp/dQw4w9WgXcQ/default.webp"
        val ytOther = "https://i.ytimg.com/vi/otherVideo1/maxresdefault.jpg"
        assertThat(ThumbnailUrlUtils.getCanonicalArtworkDiskCacheKey(ytMax))
            .isEqualTo(ThumbnailUrlUtils.getCanonicalArtworkDiskCacheKey(ytHq))
        assertThat(ThumbnailUrlUtils.getCanonicalArtworkDiskCacheKey(ytMax))
            .isEqualTo(ThumbnailUrlUtils.getCanonicalArtworkDiskCacheKey(ytWebp))
        assertThat(ThumbnailUrlUtils.getCanonicalArtworkDiskCacheKey(ytMax))
            .isEqualTo("yt_thumb_dQw4w9WgXcQ")
        assertThat(ThumbnailUrlUtils.getCanonicalArtworkDiskCacheKey(ytMax))
            .isNotEqualTo(ThumbnailUrlUtils.getCanonicalArtworkDiskCacheKey(ytOther))
    }

    @Test
    fun getCanonicalArtworkDiskCacheKey_dynamicSqpCollages_distinguishesMixes() {
        val mix1 = "https://i.ytimg.com/hqdefault.jpg?sqp=-oaymwE2COADEG5IVWytauq4Ep9CAgp9W1&rs=AOn4CL1"
        val mix2 = "https://i.ytimg.com/hqdefault.jpg?sqp=-oaymwE2CPYBEG5IVWytauq4Ep9CAgp9W2&rs=AOn4CL2"

        assertThat(ThumbnailUrlUtils.getCanonicalArtworkDiskCacheKey(mix1))
            .isNotEqualTo(ThumbnailUrlUtils.getCanonicalArtworkDiskCacheKey(mix2))
        assertThat(ThumbnailUrlUtils.getCanonicalArtworkDiskCacheKey(mix1))
            .isEqualTo(mix1)
    }

    @Test
    fun optimizeArtworkUrl_dynamicSqpCollages_untouched() {
        val mixUrl = "https://i.ytimg.com/hqdefault.jpg?sqp=-oaymwE2COADEG5IVWytauq4Ep9CAgp9W1&rs=AOn4CL1"
        assertThat(ThumbnailUrlUtils.optimizeArtworkUrl(mixUrl, AlbumArtQuality.HIGH))
            .isEqualTo(mixUrl)
    }
}
