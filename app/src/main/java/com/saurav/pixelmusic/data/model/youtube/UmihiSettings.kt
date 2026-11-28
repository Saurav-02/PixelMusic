package com.saurav.pixelmusic.data.model.youtube

data class UmihiSettings(
    val updateChannel: UpdateChannel = UpdateChannel.Stable,
    val cookies: Cookies = Cookies(),
    val dataSyncId: String = "",
    val showPodcastPlaylist: Boolean = true,
    val useSpecialLanguage: Boolean = false,
    val useAudioOffload: Boolean = false,
    val keepScreenOn: Boolean = false,
    val useAnimatedLyrics: Boolean = true,
    val animatedLyricsBlurEnabled: Boolean = true,
    val useImmersiveLyrics: Boolean = true,
    val lyricsAutoHideDelay: Int = 4,
    val showPlayerFileInfo: Boolean = false,
    val playerThemePreference: String = "ALBUM_ART",
    val colorPalettePreference: String = "SAGE",
    val lyricsMiniPlayerPosition: String = "TOP",
    val lyricsMiniPlayerAlignment: String = "LEFT",
    val useImmersiveLyricsStatusBar: Boolean = true,
    val autoQueueEnabled: Boolean = true,
    val avoidRepetitiveSongs: Boolean = false,
    val preloadQueueEnabled: Boolean = true,
    val preloadQueueSize: Int = 5,
) {
    enum class UpdateChannel {
        Stable, Beta
    }
}
