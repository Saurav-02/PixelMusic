package com.saurav.pixelmusic.utils

import com.google.common.truth.Truth.assertThat
import com.saurav.pixelmusic.data.preferences.AlbumArtQuality
import org.junit.Test

class ThumbnailUrlUtilsTest {

    @Test
    fun optimizeArtworkUrl_googleUserContent_originalQuality() {
        val input = "https://lh3.googleusercontent.com/xyz=w120-h120-l90-rj"
        val output = ThumbnailUrlUtils.optimizeArtworkUrl(input, AlbumArtQuality.ORIGINAL)
        assertThat(output).isEqualTo("https://lh3.googleusercontent.com/xyz=w1200-h1200-l90-rj")
    }

    @Test
    fun optimizeArtworkUrl_googleUserContent_highQuality() {
        val input = "https://lh3.googleusercontent.com/xyz=w400-h400"
        val output = ThumbnailUrlUtils.optimizeArtworkUrl(input, AlbumArtQuality.HIGH)
        assertThat(output).isEqualTo("https://lh3.googleusercontent.com/xyz=w800-h800-l90-rj")
    }

    @Test
    fun optimizeArtworkUrl_googleUserContent_mediumQuality() {
        val input = "https://lh3.googleusercontent.com/xyz=s500"
        val output = ThumbnailUrlUtils.optimizeArtworkUrl(input, AlbumArtQuality.MEDIUM)
        assertThat(output).isEqualTo("https://lh3.googleusercontent.com/xyz=w512-h512-l90-rj")
    }

    @Test
    fun optimizeArtworkUrl_googleUserContent_lowQuality() {
        val input = "https://lh3.googleusercontent.com/xyz"
        val output = ThumbnailUrlUtils.optimizeArtworkUrl(input, AlbumArtQuality.LOW)
        assertThat(output).isEqualTo("https://lh3.googleusercontent.com/xyz=w256-h256-l90-rj")
    }

    @Test
    fun optimizeArtworkUrl_youtubeThumbnail_standardReplacements() {
        val inputHq = "https://i.ytimg.com/vi/dQw4w9WgXcQ/hqdefault.jpg"
        assertThat(ThumbnailUrlUtils.optimizeArtworkUrl(inputHq, AlbumArtQuality.ORIGINAL))
            .isEqualTo("https://i.ytimg.com/vi/dQw4w9WgXcQ/maxresdefault.jpg")

        val inputMax = "https://i.ytimg.com/vi/dQw4w9WgXcQ/maxresdefault.jpg"
        assertThat(ThumbnailUrlUtils.optimizeArtworkUrl(inputMax, AlbumArtQuality.LOW))
            .isEqualTo("https://i.ytimg.com/vi/dQw4w9WgXcQ/mqdefault.jpg")

        assertThat(ThumbnailUrlUtils.optimizeArtworkUrl(inputMax, AlbumArtQuality.MEDIUM))
            .isEqualTo("https://i.ytimg.com/vi/dQw4w9WgXcQ/sddefault.jpg")

        assertThat(ThumbnailUrlUtils.optimizeArtworkUrl(inputMax, AlbumArtQuality.HIGH))
            .isEqualTo("https://i.ytimg.com/vi/dQw4w9WgXcQ/hqdefault.jpg")
    }

    @Test
    fun optimizeArtworkUrl_localArtwork_untouched() {
        val localArt = "pixelmusic_local_art://song/42"
        assertThat(ThumbnailUrlUtils.optimizeArtworkUrl(localArt, AlbumArtQuality.ORIGINAL))
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
                qualityWifi = AlbumArtQuality.ORIGINAL,
                qualityMobile = AlbumArtQuality.MEDIUM,
                performanceMode = true
            )
        ).isEqualTo(AlbumArtQuality.LOW)

        // Metered network uses mobile quality
        assertThat(
            ThumbnailUrlUtils.getEffectiveQuality(
                isMetered = true,
                qualityWifi = AlbumArtQuality.ORIGINAL,
                qualityMobile = AlbumArtQuality.MEDIUM,
                performanceMode = false
            )
        ).isEqualTo(AlbumArtQuality.MEDIUM)

        // Unmetered network uses wifi quality
        assertThat(
            ThumbnailUrlUtils.getEffectiveQuality(
                isMetered = false,
                qualityWifi = AlbumArtQuality.ORIGINAL,
                qualityMobile = AlbumArtQuality.MEDIUM,
                performanceMode = false
            )
        ).isEqualTo(AlbumArtQuality.ORIGINAL)
    }
}
