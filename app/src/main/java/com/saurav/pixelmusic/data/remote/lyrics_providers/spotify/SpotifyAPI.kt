package com.saurav.pixelmusic.data.remote.lyrics_providers.spotify

import android.util.Log
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.saurav.pixelmusic.data.remote.lyrics_providers.model.SongInfo
import com.saurav.pixelmusic.data.remote.lyrics_providers.util.EmptyQueryException
import com.saurav.pixelmusic.data.remote.lyrics_providers.util.NoTrackFoundException
import dev.turingcomplete.kotlinonetimepassword.HmacAlgorithm
import dev.turingcomplete.kotlinonetimepassword.TimeBasedOneTimePasswordConfig
import dev.turingcomplete.kotlinonetimepassword.TimeBasedOneTimePasswordGenerator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.concurrent.TimeUnit

// --- Spotify Data Models ---
data class ServerTimeResponse(val serverTime: Long)
data class WebPlayerTokenResponse(val accessToken: String)
data class TrackSearchResult(val data: SpotifySearchData)
data class SpotifySearchData(val searchV2: SpotifySearchV2)
data class SpotifySearchV2(val tracksV2: SpotifyTracksV2)
data class SpotifyTracksV2(val items: List<SpotifyTrackItem>)
data class SpotifyTrackItem(val item: SpotifyItemWrapper)
data class SpotifyItemWrapper(val data: SpotifyTrackData)
data class SpotifyTrackData(val id: String, val name: String, val artists: SpotifyArtists, val albumOfTrack: SpotifyAlbum)
data class SpotifyArtists(val items: List<SpotifyArtistItem>)
data class SpotifyArtistItem(val profile: SpotifyProfile)
data class SpotifyProfile(val name: String)
data class SpotifyAlbum(val coverArt: SpotifyCoverArt)
data class SpotifyCoverArt(val sources: List<SpotifyImageSource>)
data class SpotifyImageSource(val url: String)
data class SyncedLinesResponse(val lyrics: String?)

class SpotifyAPI(
    private val client: OkHttpClient,
    private val gson: Gson = Gson()
) {
    private val webPlayerURL = "https://open.spotify.com/"
    private val baseURL = "https://api-partner.spotify.com/pathfinder/v1/query"
    private val lyricsURL = "https://paxsenix.alwaysdata.net/getLyricsSpotify.php"

    private var totpSecret: ByteArray? = null
    private var totpVer: Int = 0
    private var totpGenerator: TimeBasedOneTimePasswordGenerator? = null

    private val reqHeaders = mapOf(
        "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/127.0.0.0",
        "Origin" to "https://open.spotify.com",
        "Referer" to "https://open.spotify.com/"
    )

    private var spotifyToken = ""
    private var tokenTime: Long = 0

    private suspend fun initializeTOTP() {
        if (totpGenerator != null) return
        SpotifySecrets.refresh(client)
        val lastSecretData = SpotifySecrets.current().last()
        totpSecret = toSecret(lastSecretData.secret)
        totpVer = lastSecretData.version

        totpGenerator = TimeBasedOneTimePasswordGenerator(
            totpSecret!!,
            TimeBasedOneTimePasswordConfig(30L, TimeUnit.SECONDS, 6, HmacAlgorithm.SHA1)
        )
        Log.d("SpotifyAPI", "TOTP initialized with version: $totpVer")
    }

    private fun toSecret(data: List<Int>): ByteArray {
        val mappedData = data.mapIndexed { index, value -> value xor ((index % 33) + 9) }
        val dataString = mappedData.joinToString("")
        val hexData = dataString.toByteArray(StandardCharsets.UTF_8).joinToString("") { "%02x".format(it) }
        return hexData.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
    }

    private suspend fun getServerTime(): Long = withContext(Dispatchers.IO) {
        val request = Request.Builder().url(webPlayerURL + "api/server-time").apply {
            reqHeaders.forEach { (k, v) -> addHeader(k, v) }
        }.build()
        val response = client.newCall(request).execute()
        val body = response.body.string()
        gson.fromJson(body, ServerTimeResponse::class.java).serverTime * 1000
    }

    private suspend fun getTsAndTOTP(): Pair<Long, String> {
        if (totpGenerator == null) initializeTOTP()
        val serverTime = getServerTime()
        return Pair(serverTime, totpGenerator!!.generate(serverTime))
    }

    suspend fun refreshToken(force: Boolean = false) = withContext(Dispatchers.IO) {
        if (force || spotifyToken == "") {
            val totp = getTsAndTOTP()
            val url = (webPlayerURL + "api/token").toHttpUrlOrNull()?.newBuilder()
                ?.addQueryParameter("reason", "init")
                ?.addQueryParameter("productType", "mobile-web-player")
                ?.addQueryParameter("ts", totp.first.toString())
                ?.addQueryParameter("totp", totp.second)
                ?.addQueryParameter("totpVer", totpVer.toString())
                ?.build() ?: return@withContext

            val request = Request.Builder().url(url).apply {
                reqHeaders.forEach { (k, v) -> addHeader(k, v) }
            }.build()

            val response = client.newCall(request).execute()
            val body = response.body.string()
            
            val json = gson.fromJson(body, WebPlayerTokenResponse::class.java)
            spotifyToken = json.accessToken
            tokenTime = System.currentTimeMillis()
        }
    }

    suspend fun getSongInfo(query: SongInfo, offset: Int = 0): SongInfo = withContext(Dispatchers.IO) {
        if (System.currentTimeMillis() - tokenTime > 1800000) refreshToken()

        val searchTerm = URLEncoder.encode("${query.songName} ${query.artistName}".trim(), StandardCharsets.UTF_8.toString())
        if (searchTerm == "+" || searchTerm.isBlank()) throw EmptyQueryException()

        val variables = """{"searchTerm":"$searchTerm","offset":$offset,"limit":1,"numberOfTopResults":20,"includeAudiobooks":false}"""
        val extensions = """{"persistedQuery":{"version":1,"sha256Hash":"1d021289df50166c61630e02f002ec91182b518e56bcd681ac6b0640390c0245"}}"""

        val encodedVariables = URLEncoder.encode(variables, StandardCharsets.UTF_8.toString())
        val encodedExtensions = URLEncoder.encode(extensions, StandardCharsets.UTF_8.toString())

        val request = Request.Builder()
            .url("$baseURL?operationName=searchTracks&variables=$encodedVariables&extensions=$encodedExtensions")
            .apply {
                reqHeaders.forEach { (k, v) -> addHeader(k, v) }
                addHeader("Authorization", "Bearer $spotifyToken")
            }.build()

        val response = client.newCall(request).execute()
        val body = response.body.string()

        val json = gson.fromJson(body, TrackSearchResult::class.java)
        if (json.data.searchV2.tracksV2.items.isEmpty()) throw NoTrackFoundException()

        val track = json.data.searchV2.tracksV2.items[0].item.data
        val artists = track.artists.items.joinToString(", ") { it.profile.name }
        val albumArtURL = track.albumOfTrack.coverArt.sources.firstOrNull()?.url
        val spotifyURL = "https://open.spotify.com/track/${track.id}"

        SongInfo(track.name, artists, spotifyURL, albumArtURL)
    }

    suspend fun getSyncedLyrics(trackUrl: String): String? = withContext(Dispatchers.IO) {
        val url = lyricsURL.toHttpUrlOrNull()?.newBuilder()
            ?.addQueryParameter("url", trackUrl)
            ?.build() ?: return@withContext null

        val request = Request.Builder().url(url).build()
        try {
            val response = client.newCall(request).execute()
            val body = response.body.string()
            if (!response.isSuccessful || body.isBlank()) return@withContext null
            
            val json = gson.fromJson(body, SyncedLinesResponse::class.java)
            if (json.lyrics == "Not Found.") return@withContext null
            
            json.lyrics
        } catch (e: Exception) {
            null
        }
    }
}

