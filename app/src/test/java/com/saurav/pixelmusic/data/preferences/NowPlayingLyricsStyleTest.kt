package com.saurav.pixelmusic.data.preferences

import org.junit.Assert.assertEquals
import org.junit.Test

class NowPlayingLyricsStyleTest {

    @Test
    fun allExpectedEnumValuesExist() {
        val expected = listOf(
            "HIDDEN",
            "KARAOKE",
            "WORD_BY_WORD",
            "SINGLE_WORD",
            "BLUR_FOCUS",
            "GRADIENT_SWEEP",
            "SLIDE_FADE"
        )
        val actual = NowPlayingLyricsStyle.values().map { it.name }
        assertEquals(expected, actual)
    }

    @Test
    fun valueOf_resolvesEveryStyleCorrectly() {
        assertEquals(NowPlayingLyricsStyle.HIDDEN, NowPlayingLyricsStyle.valueOf("HIDDEN"))
        assertEquals(NowPlayingLyricsStyle.KARAOKE, NowPlayingLyricsStyle.valueOf("KARAOKE"))
        assertEquals(NowPlayingLyricsStyle.WORD_BY_WORD, NowPlayingLyricsStyle.valueOf("WORD_BY_WORD"))
        assertEquals(NowPlayingLyricsStyle.SINGLE_WORD, NowPlayingLyricsStyle.valueOf("SINGLE_WORD"))
        assertEquals(NowPlayingLyricsStyle.BLUR_FOCUS, NowPlayingLyricsStyle.valueOf("BLUR_FOCUS"))
        assertEquals(NowPlayingLyricsStyle.GRADIENT_SWEEP, NowPlayingLyricsStyle.valueOf("GRADIENT_SWEEP"))
        assertEquals(NowPlayingLyricsStyle.SLIDE_FADE, NowPlayingLyricsStyle.valueOf("SLIDE_FADE"))
    }
}
