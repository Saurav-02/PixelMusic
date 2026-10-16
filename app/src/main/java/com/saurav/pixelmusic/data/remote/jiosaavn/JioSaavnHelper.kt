package com.saurav.pixelmusic.data.remote.jiosaavn

import android.util.LruCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Fallback stream resolver: maps a YouTube song (title/artist/duration) to a
 * JioSaavn direct stream URL. Best-effort only — returns null when no confident
 * match is found, so callers fall through to existing behavior.
 */
object JioSaavnHelper {

    /** Cache of youtubeId -> JioSaavn stream url (avoids repeat searches). */
    private val urlCache = LruCache<String, String>(200)

    /** Set to false to disable the fallback (e.g. from settings). */
    @Volatile
    var enabled: Boolean = true

    /**
     * Returns a playable JioSaavn URL for the given song metadata, or null.
     * @param preferKbps desired bitrate: 96, 160 or 320.
     */
    suspend fun getFallbackStreamUrl(
        youtubeId: String,
        title: String,
        artist: String,
        durationMs: Long,
        preferKbps: Int = 160
    ): String? = withContext(Dispatchers.IO) {
        if (!enabled) return@withContext null
        if (title.isBlank()) return@withContext null
        urlCache.get(youtubeId)?.let { return@withContext it }

        val query = listOf(title, artist).filter { it.isNotBlank() }.joinToString(" ")
        val candidates = JioSaavnApi.searchSongs(query, 10)
        if (candidates.isEmpty()) return@withContext null

        val best = pickBestMatch(candidates, title, artist, durationMs) ?: return@withContext null
        val decrypted = JioSaavnCrypto.decryptMediaUrl(best.encryptedMediaUrl) ?: return@withContext null
        val url = JioSaavnCrypto.withBitrate(decrypted, preferKbps)
        if (!url.startsWith("https://")) return@withContext null

        urlCache.put(youtubeId, url)
        url
    }

    fun clearCache() = urlCache.evictAll()

    /** Parses "m:ss", "h:mm:ss" or millis strings to milliseconds. */
    fun parseDurationToMs(durationStr: String): Long {
        if (durationStr.isBlank()) return 0L
        val parts = durationStr.split(":")
        return try {
            when (parts.size) {
                1 -> {
                    val raw = parts[0].toLong()
                    if (raw >= 1000L) raw else raw * 1000L
                }
                2 -> (parts[0].toLong() * 60L + parts[1].toLong()) * 1000L
                3 -> (parts[0].toLong() * 3600L + parts[1].toLong() * 60L + parts[2].toLong()) * 1000L
                else -> 0L
            }
        } catch (_: Exception) {
            0L
        }
    }

    private fun pickBestMatch(
        candidates: List<JioSaavnApi.SaavnTrack>,
        title: String,
        artist: String,
        durationMs: Long
    ): JioSaavnApi.SaavnTrack? {
        var best: JioSaavnApi.SaavnTrack? = null
        var bestScore = 0.55 // minimum confidence threshold
        for (c in candidates) {
            val score = scoreCandidate(c, title, artist, durationMs)
            if (score > bestScore) {
                bestScore = score
                best = c
            }
        }
        return best
    }

    private fun scoreCandidate(
        c: JioSaavnApi.SaavnTrack,
        title: String,
        artist: String,
        durationMs: Long
    ): Double {
        val titleScore = textSimilarity(c.title, title)
        val artistScore = if (artist.isBlank()) 0.5 else artistOverlap(c.artists, artist)
        val durationScore = if (durationMs <= 0 || c.durationSec <= 0) {
            0.5 // unknown duration: neutral
        } else {
            val diffSec = kotlin.math.abs(c.durationSec - durationMs / 1000.0)
            when {
                diffSec <= 3 -> 1.0
                diffSec <= 8 -> 0.8
                diffSec <= 15 -> 0.5
                else -> 0.0
            }
        }
        return titleScore * 0.55 + artistScore * 0.25 + durationScore * 0.20
    }

    private fun normalize(s: String): String =
        s.lowercase()
            .replace(Regex("[^a-z0-9 ]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()

    private fun textSimilarity(a: String, b: String): Double {
        val na = normalize(a)
        val nb = normalize(b)
        if (na.isEmpty() || nb.isEmpty()) return 0.0
        if (na == nb) return 1.0
        if (na.contains(nb) || nb.contains(na)) return 0.85
        val wa = na.split(" ").toSet()
        val wb = nb.split(" ").toSet()
        val inter = wa.intersect(wb).size.toDouble()
        val union = wa.union(wb).size.toDouble()
        return if (union == 0.0) 0.0 else inter / union
    }

    private fun artistOverlap(candidateArtists: String, queryArtist: String): Double {
        val ca = normalize(candidateArtists)
        val qa = normalize(queryArtist)
        if (ca.isEmpty() || qa.isEmpty()) return 0.5
        val qWords = qa.split(" ").filter { it.length > 2 }.toSet()
        if (qWords.isEmpty()) return 0.5
        val hits = qWords.count { ca.contains(it) }
        return hits.toDouble() / qWords.size
    }
}
