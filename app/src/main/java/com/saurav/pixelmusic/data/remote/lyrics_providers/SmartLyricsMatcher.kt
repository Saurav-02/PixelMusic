package com.saurav.pixelmusic.data.remote.lyrics_providers

import android.util.Log
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull
import com.saurav.pixelmusic.data.remote.lyrics_providers.model.SongInfo
import com.saurav.pixelmusic.data.remote.lyrics_providers.util.Providers
import com.saurav.pixelmusic.data.remote.lyrics_providers.util.matching.*
import okhttp3.OkHttpClient

private const val TAG = "SmartLyricsMatcher"

data class ScoredHit(
    val provider: Providers,
    val strategy: MatchStrategy,
    val result: ProviderResult,
    val confidence: ConfidenceBreakdown,
    val inlineLyrics: String?,   
    val neteaseId: Long?,        
) {
    val tier get() = confidence.tier
}

data class MatchConfig(
    val providerOrder: List<Providers> = listOf(
        Providers.LRCLIB,
        Providers.NETEASE,
        Providers.APPLE,
        Providers.SPOTIFY,
        Providers.QQMUSIC,
        Providers.BETTERLYRICS,
        Providers.LYRICSPLUS
    ),
    val maxRetries: Int = 2,
    val requestDelayMs: Long = 100,
    val maxCandidatesPerProvider: Int = 3,
    val retryBaseDelayMs: Long = 300,
    val providerTimeoutMs: Long = 4_000,
)

class SmartLyricsMatcher(private val client: OkHttpClient) {
    private val providerService = LyricsProviderService(client)

    suspend fun search(
        local: LocalTrack,
        candidates: List<QueryCandidate>,
        config: MatchConfig = MatchConfig(),
        log: (String) -> Unit = { Log.i(TAG, it) },
    ): List<ScoredHit> = coroutineScope {
        val displayName = listOfNotNull(local.artist, local.title).joinToString(" - ").ifBlank { "<unknown>" }
        log("[match] \"$displayName\"  dur=${local.durationSec?.let { "${it.toInt()}s" } ?: "?"}  candidates=${candidates.size}")

        // -------------------------------------------------------------
        // FAST-PATH: Try LRCLib on the best 2 candidates first (< 200ms)
        // -------------------------------------------------------------
        val fastCandidates = candidates.take(2)
        for (cand in fastCandidates) {
            val query = cand.asSearchString()
            if (query.isNotBlank()) {
                val lrcHits = searchLrcLib(query, local, cand, config)
                val instantHit = lrcHits.firstOrNull { 
                    it.tier == MatchTier.AUTO_ACCEPT && !it.inlineLyrics.isNullOrBlank() 
                }
                if (instantHit != null) {
                    log("  [LRCLib Fast-Path Hit] ${instantHit.result.title} (${instantHit.confidence.percent()}%) -> Instant Return")
                    return@coroutineScope listOf(instantHit)
                }
            }
        }

        // -------------------------------------------------------------
        // FALLBACK: If LRCLib didn't find an exact match, query all providers concurrently
        // -------------------------------------------------------------
        log("  [Fast-Path missed] Querying full provider ladder in parallel...")

        val channel = kotlinx.coroutines.channels.Channel<List<ScoredHit>>(config.providerOrder.size)
        val jobs = config.providerOrder.map { provider ->
            async {
                val res = withTimeoutOrNull(config.providerTimeoutMs) {
                    searchProvider(provider, local, candidates, config, log)
                } ?: emptyList<ScoredHit>().also { log("  [${provider.name}] skipped/timeout") }
                channel.send(res)
            }
        }

        val hits = LinkedHashMap<String, ScoredHit>()
        var best = 0.0
        repeat(config.providerOrder.size) {
            val providerHits = channel.receive()
            for (hit in providerHits) {
                val key = "${hit.provider}|${hit.result.title}|${hit.result.artist}|${hit.result.durationSec}"
                val existing = hits[key]
                if (existing == null || hit.confidence.score > existing.confidence.score) hits[key] = hit
                if (hit.tier != MatchTier.REJECT && hit.confidence.score > best) {
                    best = hit.confidence.score
                    log("  [${hit.provider.name}] ${hit.strategy.label} -> ${hit.result.title} (${hit.confidence.percent()}%)")
                }
            }
            if (best >= ConfidenceScorer.AUTO_ACCEPT_THRESHOLD) {
                jobs.forEach { if (!it.isCompleted) it.cancel() }
                channel.close()
                return@repeat
            }
        }

        hits.values.sortedByDescending { it.confidence.score }
    }

    private suspend fun searchProvider(
        provider: Providers,
        local: LocalTrack,
        candidates: List<QueryCandidate>,
        config: MatchConfig,
        log: (String) -> Unit,
    ): List<ScoredHit> {
        val providerHits = LinkedHashMap<String, ScoredHit>()
        var providerBest = 0.0
        val artistGuesses = artistGuessesFor(local, candidates)

        for (cand in candidates) {
            val query = cand.asSearchString()
            if (query.isBlank()) continue

            val found = when (provider) {
                Providers.LRCLIB -> searchLrcLib(query, local, cand, config)
                Providers.NETEASE -> searchNetease(query, local, cand, config)
                Providers.LYRICSPLUS, Providers.BETTERLYRICS -> searchDirect(provider, local, cand)
                else -> searchGeneric(provider, local, cand, config)
            }

            for (raw in found) {
                val hit = if (wrongSingerVetoed(artistGuesses, raw.result.artist, raw.confidence))
                    raw.copy(confidence = raw.confidence.copy(tier = MatchTier.REJECT))
                else raw

                val key = "${hit.provider}|${hit.result.title}|${hit.result.artist}|${hit.result.durationSec}"
                val existing = providerHits[key]
                if (existing == null || hit.confidence.score > existing.confidence.score) providerHits[key] = hit
                if (hit.tier != MatchTier.REJECT && hit.confidence.score > providerBest) providerBest = hit.confidence.score
            }
            if (providerBest >= ConfidenceScorer.AUTO_ACCEPT_THRESHOLD) break
            delay(config.requestDelayMs)
        }
        return providerHits.values.toList()
    }

    private suspend fun searchLrcLib(
        query: String, local: LocalTrack, cand: QueryCandidate, config: MatchConfig
    ): List<ScoredHit> {
        val results = runCatching { providerService.lrcLibAPI.searchCandidates(query) }.getOrDefault(emptyList())

        return results
            .filter { !it.syncedLyrics.isNullOrBlank() }
            .map { r ->
                val pr = ProviderResult(r.name, r.artistName, r.duration, r.albumName, true)
                ScoredHit(Providers.LRCLIB, cand.strategy, pr, scoreHitAgainstViews(local, pr, cand), r.syncedLyrics, null)
            }
            .sortedByDescending { it.confidence.score }
            .take(config.maxCandidatesPerProvider)
    }

    private suspend fun searchNetease(
        query: String, local: LocalTrack, cand: QueryCandidate, config: MatchConfig
    ): List<ScoredHit> {
        val songs = runCatching { providerService.neteaseAPI.searchCandidates(query, config.maxCandidatesPerProvider) }.getOrDefault(emptyList())

        return songs.map { s ->
            val pr = ProviderResult(
                title = s.name,
                artist = s.artists?.joinToString(", ") { it.name } ?: "",
                durationSec = s.duration?.let { it / 1000.0 },
                album = s.album?.name,
                hasSyncedLyrics = true,
            )
            ScoredHit(Providers.NETEASE, cand.strategy, pr, scoreHitAgainstViews(local, pr, cand), null, s.id)
        }
    }

    private suspend fun searchDirect(
        provider: Providers, local: LocalTrack, cand: QueryCandidate
    ): List<ScoredHit> {
        val artist = cand.artist?.takeIf { it.isNotBlank() } ?: local.artist ?: return emptyList()
        val durationSec = local.durationSec?.toInt()?.takeIf { it > 0 }

        val lyrics = runCatching {
            when (provider) {
                Providers.LYRICSPLUS -> providerService.lyricsPlusAPI.getSyncedLyrics(cand.title, artist, durationSec, local.album)
                else -> providerService.betterLyricsAPI.getSyncedLyrics(cand.title, artist, durationSec, local.album)
            }
        }.getOrNull()

        if (lyrics.isNullOrBlank()) return emptyList()

        val pr = ProviderResult(
            title = cand.title,
            artist = artist,
            durationSec = if (durationSec != null) local.durationSec else null,
            album = local.album,
            hasSyncedLyrics = true,
        )
        return listOf(ScoredHit(provider, cand.strategy, pr, scoreHitAgainstViews(local, pr, cand), lyrics, null))
    }

    private suspend fun searchGeneric(
        provider: Providers, local: LocalTrack, cand: QueryCandidate, config: MatchConfig
    ): List<ScoredHit> {
        val hits = LinkedHashMap<String, ScoredHit>()

        for (offset in 0 until config.maxCandidatesPerProvider) {
            val info = runCatching {
                providerService.getSongInfo(SongInfo(cand.title, cand.artist), offset, provider)
            }.getOrNull() ?: break

            val pr = ProviderResult(info.songName, info.artistName, null, null, true)
            val conf = scoreHitAgainstViews(local, pr, cand)
            val lyrics = if (conf.score >= ConfidenceScorer.REVIEW_THRESHOLD) {
                runCatching { providerService.getSyncedLyrics(info, provider) }.getOrNull()
            } else null

            if (!lyrics.isNullOrBlank()) {
                val hit = ScoredHit(provider, cand.strategy, pr, conf, lyrics, null)
                val key = "${hit.provider}|${hit.result.title}|${hit.result.artist}|${hit.result.durationSec}"
                val existing = hits[key]
                if (existing == null || hit.confidence.score > existing.confidence.score) hits[key] = hit
                if (hit.tier != MatchTier.REJECT && hit.confidence.score >= ConfidenceScorer.AUTO_ACCEPT_THRESHOLD) break
            }
        }
        return hits.values.sortedByDescending { it.confidence.score }
    }

    suspend fun fetchLyrics(hit: ScoredHit): String? {
        return when (hit.provider) {
            Providers.NETEASE -> hit.neteaseId?.let { id ->
                runCatching { providerService.neteaseAPI.getSyncedLyrics(id) }.getOrNull()
            }
            else -> hit.inlineLyrics
        }?.takeIf { it.isNotBlank() }
    }
}
internal fun scoreHitAgainstViews(local: LocalTrack, pr: ProviderResult, cand: QueryCandidate): ConfidenceBreakdown {
    var best = ConfidenceScorer.score(local, pr)
    val view = LocalTrack(cand.title, cand.artist, local.durationSec, local.album)
    if (view.title != local.title || view.artist != local.artist) {
        val viewConf = ConfidenceScorer.score(view, pr)
        val viewTrustable = !cand.artist.isNullOrBlank() || viewConf.durationMatched
        if (viewTrustable && viewConf.score > best.score) best = viewConf
    }
    return if (cand.strategy == MatchStrategy.FILENAME_LOOSE && best.tier == MatchTier.AUTO_ACCEPT)
        best.copy(score = 0.80, tier = MatchTier.REVIEW)
    else best
}

internal fun artistGuessesFor(local: LocalTrack, candidates: List<QueryCandidate>): List<String> =
    (listOfNotNull(local.artist) + candidates.mapNotNull { it.artist }).filter { it.isNotBlank() }.distinct()

internal fun wrongSingerVetoed(artistGuesses: List<String>, resultArtist: String?, conf: ConfidenceBreakdown): Boolean {
    if (conf.durationMatched) return false
    if (resultArtist.isNullOrBlank()) return false
    val guesses = artistGuesses.filter { TextMatch.normalizeForCompare(it).isNotEmpty() }
    if (guesses.isEmpty()) return false
    if (TextMatch.normalizeForCompare(resultArtist).isEmpty()) return true
    return guesses.maxOf { TextMatch.similarity(it, resultArtist) } < 0.50
}

            
