/*
 * ArchiveTune (2026)
 * © Chartreux Westia — github.com/ianshulyadav
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */



package saurav.shru.pixelmusic.innertube

import saurav.shru.pixelmusic.innertube.models.YouTubeClient
import saurav.shru.pixelmusic.innertube.utils.parseCookieString
import saurav.shru.pixelmusic.innertube.utils.sha1
import java.util.Locale

data class PlaybackAuthState(
    val cookie: String? = null,
    val visitorData: String? = null,
    val dataSyncId: String? = null,
    val poToken: String? = null,
    val poTokenGvs: String? = null,
    val poTokenPlayer: String? = null,
    val webClientPoTokenEnabled: Boolean = true,
) {
    val hasLoginCookie: Boolean
        get() {
            val currentCookie = cookie ?: return false
            return "SAPISID" in parseCookieString(currentCookie)
        }

    val hasPlaybackLoginContext: Boolean
        get() = hasLoginCookie && !dataSyncId.isNullOrBlank()

    val sessionId: String?
        get() = if (hasPlaybackLoginContext) dataSyncId else visitorData

    val fingerprint: String
        get() = sha1(
            listOf(
                cookie.orEmpty(),
                visitorData.orEmpty(),
                dataSyncId.orEmpty(),
                poToken.orEmpty(),
                poTokenGvs.orEmpty(),
                poTokenPlayer.orEmpty(),
                webClientPoTokenEnabled.toString(),
            ).joinToString(separator = "\u0000")
        )

    fun normalized(): PlaybackAuthState =
        copy(
            cookie = cookie.normalizeAuthValue(),
            visitorData = visitorData.normalizeAuthValue(),
            dataSyncId = dataSyncId.normalizeDataSyncId(),
            poToken = poToken.normalizeAuthValue(),
            poTokenGvs = poTokenGvs.normalizeAuthValue(),
            poTokenPlayer = poTokenPlayer.normalizeAuthValue(),
        )

    fun resolvePlayerPoToken(
        client: YouTubeClient,
        explicitPoToken: String? = null,
    ): String? {
        val explicit = explicitPoToken.normalizeAuthValue()
        if (explicit != null) return explicit
        if (!webClientPoTokenEnabled) return null
        if (!needsServiceIntegrity(client)) return null
        val token = poTokenPlayer ?: poToken
        if (token.isNullOrBlank()) {
            val id = sessionId ?: java.util.UUID.randomUUID().toString()
            return saurav.shru.pixelmusic.innertube.utils.PoTokenGenerator.generateSessionToken(id)
        }
        return token
    }

    fun resolveGvsPoToken(client: YouTubeClient? = null): String? {
        if (client != null && !needsServiceIntegrity(client)) return null
        if (!webClientPoTokenEnabled) return null
        val token = poTokenGvs ?: poToken
        if (token.isNullOrBlank()) {
            val id = sessionId ?: java.util.UUID.randomUUID().toString()
            return saurav.shru.pixelmusic.innertube.utils.PoTokenGenerator.generateSessionToken(id)
        }
        return token
    }

    companion object {
        val EMPTY = PlaybackAuthState()

        internal fun needsServiceIntegrity(client: YouTubeClient): Boolean {
            val name = client.clientName.uppercase(Locale.US)
            return name == "WEB" ||
                name == "WEB_REMIX" ||
                name == "WEB_CREATOR" ||
                name == "MWEB" ||
                name == "WEB_EMBEDDED_PLAYER" ||
                name == "TVHTML5" ||
                name == "TVHTML5_SIMPLY_EMBEDDED_PLAYER" ||
                name == "TVHTML5_SIMPLY"
        }
    }
}

private fun String?.normalizeAuthValue(): String? {
    val trimmed = this?.trim()
    return trimmed?.takeIf { it.isNotEmpty() && !it.equals("null", ignoreCase = true) }
}

private fun String?.normalizeDataSyncId(): String? {
    val normalized = this.normalizeAuthValue() ?: return null
    return normalized.takeIf { !it.contains("||") }
        ?: normalized.takeIf { it.endsWith("||") }?.substringBefore("||")
        ?: normalized.substringAfter("||")
}
