package com.saurav.pixelmusic.data.remote.spotify

/**
 * Spotify Web API credentials (Client Credentials flow) used for playlist link import.
 *
 * Create a free app at https://developer.spotify.com/dashboard (no redirect URI needed
 * for this flow) and paste the Client ID + Client Secret below.
 *
 * Note: these ship inside the APK, so treat them as public. If they ever get abused,
 * regenerate them from the Spotify dashboard.
 */
object SpotifyCredentials {
    const val CLIENT_ID = ""
    const val CLIENT_SECRET = ""

    val isConfigured: Boolean
        get() = CLIENT_ID.isNotBlank() && CLIENT_SECRET.isNotBlank()
}
