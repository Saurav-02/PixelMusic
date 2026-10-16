package com.saurav.pixelmusic.data.remote.spotify

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Credentials
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

class SpotifyNotConfiguredException :
    Exception("Spotify import isn't set up yet (missing API credentials)")

/**
 * Spotify Web API client with automatic public embed scraper fallback for importing
 * public playlists and albums from pasted links without requiring developer API credentials.
 */
class SpotifyPlaylistClient(
    private val http: OkHttpClient,
    private val clientId: String = SpotifyCredentials.CLIENT_ID,
    private val clientSecret: String = SpotifyCredentials.CLIENT_SECRET,
) {
    data class Track(val title: String, val artist: String, val durationMs: Long)

    private var cachedToken: String? = null
    private var tokenExpiresAtMs: Long = 0L

    private suspend fun accessToken(): String = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        cachedToken?.takeIf { now < tokenExpiresAtMs - 60_000 }?.let { return@withContext it }
        if (clientId.isBlank() || clientSecret.isBlank()) throw SpotifyNotConfiguredException()
        val body = FormBody.Builder().add("grant_type", "client_credentials").build()
        val request = Request.Builder()
            .url("https://accounts.spotify.com/api/token")
            .header("Authorization", Credentials.basic(clientId, clientSecret))
            .post(body)
            .build()
        http.newCall(request).execute().use { response ->
            val json = JSONObject(response.body.string())
            if (!response.isSuccessful) {
                throw Exception("Spotify auth failed (HTTP ${response.code})")
            }
            cachedToken = json.getString("access_token")
            tokenExpiresAtMs = now + json.getLong("expires_in") * 1000
            cachedToken!!
        }
    }

    /**
     * Returns (playlist/album name, tracks in order).
     * Attempts official Web API if credentials configured, otherwise uses embed scraper.
     */
    suspend fun fetchPlaylist(playlistId: String, isAlbum: Boolean = false): Pair<String, List<Track>> =
        withContext(Dispatchers.IO) {
            if (SpotifyCredentials.isConfigured) {
                try {
                    return@withContext fetchFromApi(playlistId, isAlbum)
                } catch (_: Exception) {
                    // Fall back to embed scraper if API fails
                }
            }
            fetchFromEmbed(playlistId, isAlbum)
        }

    private suspend fun fetchFromApi(id: String, isAlbum: Boolean): Pair<String, List<Track>> =
        withContext(Dispatchers.IO) {
            val token = accessToken()

            fun get(url: String): JSONObject {
                val request = Request.Builder()
                    .url(url)
                    .header("Authorization", "Bearer $token")
                    .build()
                http.newCall(request).execute().use { response ->
                    val body = response.body.string()
                    if (!response.isSuccessful) {
                        throw Exception("Spotify request failed (HTTP ${response.code})")
                    }
                    return JSONObject(body)
                }
            }

            val type = if (isAlbum) "albums" else "playlists"
            val name = get("https://api.spotify.com/v1/$type/$id?fields=name")
                .optString("name", if (isAlbum) "Spotify Album" else "Spotify Playlist")

            val tracks = mutableListOf<Track>()
            var url: String? = if (isAlbum) {
                "https://api.spotify.com/v1/albums/$id/tracks?limit=50"
            } else {
                "https://api.spotify.com/v1/playlists/$id/tracks?limit=50&fields=items(track(name,artists(name),duration_ms)),next"
            }

            while (url != null) {
                val page = get(url)
                val items = page.optJSONArray("items") ?: break
                for (i in 0 until items.length()) {
                    val obj = items.optJSONObject(i) ?: continue
                    val track = if (isAlbum) obj else (obj.optJSONObject("track") ?: continue)
                    val artistsJson = track.optJSONArray("artists")
                    val artist = buildList {
                        for (a in 0 until (artistsJson?.length() ?: 0)) {
                            artistsJson?.optJSONObject(a)?.optString("name")
                                ?.takeIf { it.isNotBlank() }?.let { add(it) }
                        }
                    }.joinToString(", ")
                    tracks += Track(
                        title = track.optString("name"),
                        artist = artist,
                        durationMs = track.optLong("duration_ms"),
                    )
                }
                url = page.optString("next").takeIf { it.isNotBlank() }
            }
            name to tracks
        }

    private fun fetchFromEmbed(id: String, isAlbum: Boolean): Pair<String, List<Track>> {
        val type = if (isAlbum) "album" else "playlist"
        val request = Request.Builder()
            .url("https://open.spotify.com/embed/$type/$id")
            .header("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36")
            .header("Accept-Language", "en-US,en;q=0.9")
            .build()

        val html = http.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw Exception("Could not open Spotify $type (HTTP ${response.code})")
            }
            response.body.string()
        }

        val scriptMatch = Regex("""<script id="__NEXT_DATA__" type="application/json">([^<]+)</script>""").find(html)
        if (scriptMatch != null) {
            val json = JSONObject(scriptMatch.groupValues[1])
            val entity = json.optJSONObject("props")
                ?.optJSONObject("pageProps")
                ?.optJSONObject("state")
                ?.optJSONObject("data")
                ?.optJSONObject("entity")

            if (entity != null) {
                val name = entity.optString("name", if (isAlbum) "Spotify Album" else "Spotify Playlist")
                val trackList = entity.optJSONArray("trackList")
                val tracks = mutableListOf<Track>()
                if (trackList != null) {
                    for (i in 0 until trackList.length()) {
                        val item = trackList.optJSONObject(i) ?: continue
                        val title = item.optString("title").ifBlank { item.optString("name") }
                        val subtitle = item.optString("subtitle")
                        val duration = item.optLong("duration", item.optLong("duration_ms", 0L))
                        if (title.isNotBlank()) {
                            tracks.add(Track(title = title, artist = subtitle, durationMs = duration))
                        }
                    }
                }
                if (tracks.isNotEmpty()) {
                    return name to tracks
                }
            }
        }

        throw Exception("Could not find playable tracks in this Spotify link. Please ensure the playlist is public.")
    }
}
