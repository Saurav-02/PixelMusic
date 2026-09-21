package com.saurav.pixelmusic.data.remote.lyrics_providers

import com.saurav.pixelmusic.data.remote.lyrics_providers.apple.AppleAPI
import com.saurav.pixelmusic.data.remote.lyrics_providers.model.SongInfo
import com.saurav.pixelmusic.data.remote.lyrics_providers.others.*
import com.saurav.pixelmusic.data.remote.lyrics_providers.spotify.SpotifyAPI
import com.saurav.pixelmusic.data.remote.lyrics_providers.util.*
import com.saurav.pixelmusic.data.remote.lyrics_providers.util.Providers
import okhttp3.OkHttpClient

class LyricsProviderService(client: OkHttpClient) {
    
    val lrcLibAPI = LRCLibAPI(client)
    val neteaseAPI = NeteaseAPI(client)
    val spotifyAPI = SpotifyAPI(client)
    val qqMusicAPI = QQMusicAPI(client)
    val appleAPI = AppleAPI(client)
    val lyricsPlusAPI = LyricsPlusAPI(client)
    val betterLyricsAPI = BetterLyricsAPI(client)

    suspend fun getSongInfo(query: SongInfo, offset: Int = 0, provider: Providers): SongInfo? {
        return try {
            when (provider) {
                Providers.LRCLIB -> lrcLibAPI.getSongInfo(query, offset) ?: throw NoTrackFoundException()
                Providers.NETEASE -> neteaseAPI.getSongInfo(query, offset) ?: throw NoTrackFoundException()
                Providers.SPOTIFY -> spotifyAPI.getSongInfo(query, offset) ?: throw NoTrackFoundException()
                Providers.QQMUSIC -> qqMusicAPI.getSongInfo(query, offset) ?: throw NoTrackFoundException()
                Providers.APPLE -> appleAPI.getSongInfo(query, offset) ?: throw NoTrackFoundException()
                Providers.LYRICSPLUS, Providers.BETTERLYRICS ->
                    if (offset > 0) throw NoTrackFoundException()
                    else SongInfo(songName = query.songName, artistName = query.artistName)
            }
        } catch (e: Exception) {
            throw e
        }
    }

    suspend fun getSyncedLyrics(
        song: SongInfo,
        provider: Providers,
        includeTranslationNetEase: Boolean = false,
        includeRomanizationNetEase: Boolean = false
    ): String? {
        return when (provider) {
            Providers.LRCLIB -> lrcLibAPI.getSyncedLyrics(song.lrcLibID ?: 0)
            Providers.NETEASE -> neteaseAPI.getSyncedLyrics(
                song.neteaseID ?: 0L,
                includeTranslationNetEase,
                includeRomanizationNetEase
            )
            Providers.SPOTIFY -> spotifyAPI.getSyncedLyrics(song.songLink ?: "")
            Providers.QQMUSIC -> qqMusicAPI.getSyncedLyrics(song.qqPayload ?: "")
            Providers.APPLE -> appleAPI.getSyncedLyrics(song.appleID ?: 0L)
            Providers.LYRICSPLUS -> lyricsPlusAPI.getSyncedLyrics(song.songName.orEmpty(), song.artistName.orEmpty())
            Providers.BETTERLYRICS -> betterLyricsAPI.getSyncedLyrics(song.songName.orEmpty(), song.artistName.orEmpty())
        }
    }
}
