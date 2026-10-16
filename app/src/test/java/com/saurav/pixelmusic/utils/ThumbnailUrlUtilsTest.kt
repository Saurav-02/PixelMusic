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
}
