package com.saurav.pixelmusic.lyrics

import android.content.Context
import com.saurav.pixelmusic.data.model.Lyrics
import com.saurav.pixelmusic.lyrics.providers.betterlyrics.BetterLyrics
import com.saurav.pixelmusic.lyrics.providers.betterlyrics.TTMLParser
import com.saurav.pixelmusic.lyrics.providers.directlyrics.Deezer
import com.saurav.pixelmusic.lyrics.providers.directlyrics.LrcRed
import com.saurav.pixelmusic.lyrics.providers.directlyrics.NetEase
import com.saurav.pixelmusic.lyrics.providers.directlyrics.QQMusic
import com.saurav.pixelmusic.lyrics.providers.kugou.KuGou
import com.saurav.pixelmusic.lyrics.providers.lrclib.LrcLib
import com.saurav.pixelmusic.lyrics.providers.paxsenix.Paxsenix
import com.saurav.pixelmusic.lyrics.providers.simpmusic.SimpMusicLyrics
import com.saurav.pixelmusic.lyrics.providers.youlyplus.YouLyPlus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import timber.log.Timber
import java.util.Locale

data class PixelMusicLyricsResult(
    val lyrics: Lyrics,
    val rawLrc: String,
    val providerId: String,
    val providerDisplayName: String,
)

/**
 * Multi-provider lyrics engine ported from PixelMusic 8.4.4: queries BetterLyrics (Apple Music TTML),
 * Unison, YouLyPlus, Paxsenix, lrc.red, SimpMusic, LrcLib, Deezer, NetEase, QQ Music, and KuGou
 * in parallel and grades candidates by script, timing, features (duet > word timing > backing vocals),
 * and user provider order (or provider order first when LyricsOrderOverrideKey is enabled).
 */
object PixelMusicLyricsEngine {

    enum class SyncKind { NONE, PLAIN, LINE, WORD }

    enum class Script { LATIN, HAN, KANA, HANGUL, CYRILLIC, ARABIC, DEVANAGARI, THAI, GREEK, HEBREW, UNKNOWN }

    data class GradedCandidate(
        val result: PixelMusicLyricsResult,
        val order: Int,
        val sync: SyncKind,
        val script: Script,
        val language: String?,
        val hasDuet: Boolean,
        val hasBackground: Boolean,
        val isSyllableSplit: Boolean,
    )

    fun effectiveProviderOrder(orderString: String, preferredProviderId: String? = null): List<String> =
        PixelMusicLyricsPreferences.effectiveProviderOrder(orderString, preferredProviderId)

    fun getDisplayName(providerId: String): String = when (providerId) {
        "BetterLyrics" -> "Apple Music"
        "Unison" -> "Unison"
        "YouLyPlus" -> "YouLyPlus"
        "Paxsenix" -> "PaxSenix"
        "LrcRed" -> "lrc.red"
        "SimpMusic" -> "SimpMusic"
        "LrcLib" -> "LRCLIB"
        "Deezer" -> "Deezer"
        "NetEase" -> "NetEase"
        "QQMusic" -> "QQ Music"
        "Kugou" -> "KuGou"
        else -> providerId
    }

    suspend fun fetchBestLyrics(
        context: Context,
        title: String,
        artist: String,
        durationSec: Int,
        album: String? = null,
        youtubeId: String? = null,
    ): PixelMusicLyricsResult? = withContext(Dispatchers.IO) {
        val order = runCatching { PixelMusicLyricsPreferences.providerOrderFlow(context).first() }
            .getOrDefault(PixelMusicLyricsPreferences.DefaultProviderOrder)
        val orderFirst = runCatching { PixelMusicLyricsPreferences.orderOverrideFlow(context).first() }
            .getOrDefault(false)

        val candidates = coroutineScope {
            order.withIndex().map { (idx, providerId) ->
                async(Dispatchers.Default) {
                    val res = fetchFromProvider(
                        context = context,
                        providerId = providerId,
                        title = title,
                        artist = artist,
                        durationSec = durationSec,
                        album = album,
                        youtubeId = youtubeId,
                    ) ?: return@async null
                    gradeCandidate(res, idx)
                }
            }.awaitAll().filterNotNull()
        }

        withContext(Dispatchers.Default) {
            pickBest(
                title = title,
                artist = artist,
                album = album,
                candidates = candidates,
                providerCount = order.size,
                orderFirst = orderFirst,
            )?.result
        }
    }

    suspend fun fetchFromProvider(
        context: Context,
        providerId: String,
        title: String,
        artist: String,
        durationSec: Int,
        album: String? = null,
        youtubeId: String? = null,
    ): PixelMusicLyricsResult? = withContext(Dispatchers.IO) {
        val raw = withTimeoutOrNull(ProviderTimeoutMs) {
            runCatching {
                when (providerId) {
                    "BetterLyrics" -> BetterLyrics.getLyrics(title, artist, durationSec, album).getOrNull()
                    "Unison" -> UnisonLyricsProvider.getLyrics(
                        id = youtubeId.orEmpty(),
                        title = title,
                        artist = artist,
                        duration = durationSec,
                        album = album,
                    ).getOrNull()
                    "YouLyPlus" -> YouLyPlus.getLyrics(title, artist, durationSec, album, id = youtubeId).getOrNull()
                    "Paxsenix" -> {
                        Paxsenix.init(context)
                        Paxsenix.getLyrics(title, artist, durationSec, album).getOrNull()
                    }
                    "LrcRed" -> LrcRed.getTtml(title, artist, durationSec).mapCatching { ttml ->
                        val lines = TTMLParser.parseTTML(ttml)
                        check(lines.isNotEmpty())
                        TTMLParser.toLRC(lines)
                    }.getOrNull()
                    "SimpMusic" -> {
                        val vid = youtubeId?.takeIf { it.length == 11 } ?: return@runCatching null
                        SimpMusicLyrics.getLyrics(vid, durationSec).getOrNull()
                    }
                    "LrcLib" -> LrcLib.getLyrics(title, artist, durationSec, album).getOrNull()
                    "Deezer" -> Deezer.getLyrics(title, artist, durationSec).getOrNull()
                    "NetEase" -> NetEase.getLyrics(title, artist, durationSec).getOrNull()
                    "QQMusic" -> QQMusic.getLyrics(title, artist, durationSec).getOrNull()
                    "Kugou" -> KuGou.getLyrics(title, artist, durationSec, album).getOrNull()
                    else -> null
                }
            }.onFailure { e ->
                Timber.tag("PixelMusicLyrics").d(e, "Provider %s missed for %s", providerId, title)
            }.getOrNull()
        }?.takeIf { it.isNotBlank() } ?: return@withContext null

        val displayName = getDisplayName(providerId)
        val parsed = PixelMusicLyricsParser.parse(raw, providerName = displayName, areFromRemote = true)
        if (parsed.synced.isNullOrEmpty() && parsed.plain.isNullOrEmpty()) return@withContext null

        PixelMusicLyricsResult(
            lyrics = parsed,
            rawLrc = raw,
            providerId = providerId,
            providerDisplayName = displayName,
        )
    }

    suspend fun searchAllProviders(
        context: Context,
        title: String,
        artist: String,
        durationSec: Int,
        album: String? = null,
        youtubeId: String? = null,
    ): List<PixelMusicLyricsResult> = coroutineScope {
        val order = runCatching { PixelMusicLyricsPreferences.providerOrderFlow(context).first() }
            .getOrDefault(PixelMusicLyricsPreferences.DefaultProviderOrder)

        order.map { providerId ->
            async(Dispatchers.IO) {
                fetchFromProvider(context, providerId, title, artist, durationSec, album, youtubeId)
            }
        }.awaitAll().filterNotNull()
    }

    private fun gradeCandidate(res: PixelMusicLyricsResult, order: Int): GradedCandidate? {
        val synced = res.lyrics.synced
        val sync = when {
            !synced.isNullOrEmpty() -> {
                val isFlatSameTime = synced.size >= 2 &&
                    synced.none { !it.words.isNullOrEmpty() } &&
                    synced.all { it.time == synced.first().time }
                if (isFlatSameTime) {
                    SyncKind.PLAIN
                } else {
                    val wordSynced = synced.count { (it.words?.size ?: 0) > 1 }
                    if (wordSynced * 2 >= synced.size && wordSynced > 0) SyncKind.WORD else SyncKind.LINE
                }
            }
            !res.lyrics.plain.isNullOrEmpty() -> SyncKind.PLAIN
            else -> SyncKind.NONE
        }
        if (sync == SyncKind.NONE) return null

        val script = lyricsScript(res.rawLrc)
        val language = latinLanguage(res.rawLrc, script)
        val hasDuet = synced != null && synced.filter { !it.isBackground }
            .map { it.agent ?: "v1" }
            .distinct()
            .size > 1
        val hasBackground = res.rawLrc.contains("{bg}") || (synced?.any { it.isBackground } == true)
        val syllableSplit = sync == SyncKind.WORD && isSyllableSplit(synced)

        return GradedCandidate(
            result = res,
            order = order,
            sync = sync,
            script = script,
            language = language,
            hasDuet = hasDuet,
            hasBackground = hasBackground,
            isSyllableSplit = syllableSplit,
        )
    }

    private fun pickBest(
        title: String,
        artist: String,
        album: String?,
        candidates: List<GradedCandidate>,
        providerCount: Int,
        orderFirst: Boolean,
    ): GradedCandidate? {
        if (candidates.isEmpty()) return null

        val metadataScript = dominantScript(listOfNotNull(title, artist, album).joinToString(" "))
        val referenceScript = metadataScript.takeIf(::isNative)?.let { script ->
            if (script == Script.HAN && candidates.any { it.script == Script.KANA }) Script.KANA else script
        }

        val chineseSong = isChineseSong(referenceScript, candidates)
        val hindiSong = referenceScript == Script.DEVANAGARI ||
            candidates.any { it.script == Script.DEVANAGARI || it.language == "hi-Latn" }
        val songLanguage = songLanguage(referenceScript, candidates)
        val pool = eligible(candidates, chineseSong, songLanguage)
        if (pool.isEmpty()) return null

        val languages = languageKeys(pool)
        val majority = if (referenceScript == null) majorityLanguage(languages, minVotes = 2) else null

        val trustedNative: Set<Script> = if (referenceScript != null || majority != null) {
            emptySet()
        } else {
            pool.map { it.script }
                .filter(::isNative)
                .groupingBy { it }
                .eachCount()
                .filter { (script, count) ->
                    count >= 2 || script == Script.KANA || script == Script.HANGUL
                }
                .keys
        }

        return pool.withIndex().maxByOrNull { (index, c) ->
            val hindiMatch = hindiSong && (
                c.script == Script.DEVANAGARI ||
                    c.language == "hi-Latn" ||
                    (c.script == Script.LATIN && c.language == null)
                )
            val scriptScore = when {
                hindiMatch -> 3
                referenceScript != null -> when {
                    c.script == referenceScript -> 3
                    sameLanguageFamily(c.script, referenceScript) -> 2
                    c.script == Script.UNKNOWN -> 1
                    else -> 0
                }
                majority != null -> when {
                    languages[index] == majority -> 3
                    c.script == Script.UNKNOWN -> 1
                    else -> 0
                }
                trustedNative.isNotEmpty() -> when {
                    c.script in trustedNative -> 2
                    c.script == Script.UNKNOWN -> 1
                    else -> 0
                }
                isNative(c.script) -> 0
                else -> 1
            }
            val timedScore = if (c.sync >= SyncKind.LINE) 1 else 0
            val base = scriptScore * ScriptWeight + timedScore * TimedWeight

            if (orderFirst) return@maxByOrNull base + providerCount - c.order

            val preferredBonus = if (hindiMatch && c.result.providerId in PreferredHindiProviders) HindiBonus else 0
            val orderScore = if (chineseSong && c.result.providerId in ChineseSources) {
                providerCount * 2 - c.order
            } else {
                providerCount - c.order
            }

            base + preferredBonus + featureScore(c) * FeatureWeight + orderScore
        }?.value
    }

    private fun featureScore(c: GradedCandidate): Int {
        val wordTimed = c.sync == SyncKind.WORD && !c.isSyllableSplit
        return (if (c.hasDuet) DuetPoints else 0) +
            (if (wordTimed) WordPoints else 0) +
            (if (c.hasBackground) BackgroundPoints else 0)
    }

    private fun languageKeys(candidates: List<GradedCandidate>): List<String> {
        val keys = candidates.map { c ->
            if (c.script == Script.LATIN) {
                c.language?.let { LatinPrefix + it } ?: Script.LATIN.name
            } else {
                c.script.name
            }
        }
        val commonLatin = keys.filter { it.startsWith(LatinPrefix) }
            .groupingBy { it }
            .eachCount()
            .maxByOrNull { it.value }
            ?.key
        return keys.map { if (it == Script.LATIN.name && commonLatin != null) commonLatin else it }
    }

    private fun majorityLanguage(keys: List<String>, minVotes: Int): String? {
        val ranked = keys.filter { it != Script.UNKNOWN.name }
            .groupingBy { it }
            .eachCount()
            .entries
            .sortedByDescending { it.value }
        val top = ranked.firstOrNull() ?: return null
        val runnerUp = ranked.getOrNull(1)?.value ?: 0
        return top.key.takeIf { top.value >= minVotes && top.value > runnerUp }
    }

    private fun isChineseSong(referenceScript: Script?, candidates: List<GradedCandidate>): Boolean =
        if (referenceScript != null) {
            referenceScript == Script.HAN
        } else {
            val others = candidates.filter { it.result.providerId !in ChineseSources }
            candidates.none { it.script == Script.KANA } &&
                majorityLanguage(languageKeys(others), minVotes = 1) == Script.HAN.name
        }

    private fun eligible(
        candidates: List<GradedCandidate>,
        chineseSong: Boolean,
        songLanguage: String?,
    ): List<GradedCandidate> {
        val (chinese, main) = candidates.partition { it.result.providerId in ChineseSources }
        val wordTimedOriginals = chinese.filter { c ->
            c.sync == SyncKind.WORD && c.script != Script.HAN &&
                songLanguage != null && languageKey(c, songLanguage) == songLanguage
        }
        return (if (chineseSong) main + chinese else main + wordTimedOriginals)
            .ifEmpty { chinese.filter { it.script != Script.HAN } }
            .ifEmpty { candidates }
    }

    private fun songLanguage(referenceScript: Script?, candidates: List<GradedCandidate>): String? {
        if (referenceScript != null) return referenceScript.name
        val others = candidates.filter { it.result.providerId !in ChineseSources }
        return majorityLanguage(languageKeys(others), minVotes = 1)
    }

    private fun languageKey(candidate: GradedCandidate, songLanguage: String): String = when {
        candidate.script == Script.LATIN ->
            candidate.language?.let { LatinPrefix + it }
                ?: songLanguage.takeIf { it.startsWith(LatinPrefix) || it == Script.LATIN.name }
                ?: Script.LATIN.name
        candidate.script == Script.KANA && songLanguage == Script.HAN.name -> songLanguage
        else -> candidate.script.name
    }

    private fun isNative(script: Script): Boolean =
        script != Script.LATIN && script != Script.UNKNOWN

    private fun sameLanguageFamily(a: Script, b: Script): Boolean {
        val cjk = setOf(Script.HAN, Script.KANA)
        return a in cjk && b in cjk
    }

    private fun isSyllableSplit(lines: List<com.saurav.pixelmusic.data.model.SyncedLine>?): Boolean {
        if (lines == null || lines.size < 4) return false
        var checked = 0
        var flagged = 0
        for (entry in lines) {
            val text = entry.line.trim()
            val letters = text.filter(Char::isLetter)
            if (letters.isEmpty()) continue
            val latin = letters.count { it.code < 0x250 }
            if (latin.toDouble() / letters.length < 0.9) continue
            val tokens = text.split(Regex("\\s+")).filter { it.any(Char::isLetter) }
            if (tokens.size < 4) continue
            checked++
            val lengths = tokens.map { it.count(Char::isLetter) }
            val shortRatio = lengths.count { it <= 3 }.toDouble() / lengths.size
            val mean = lengths.sum().toDouble() / lengths.size
            if (shortRatio >= 0.8 && mean < 2.8) flagged++
        }
        return checked >= 4 && flagged.toDouble() / checked >= 0.65
    }

    private fun scriptOf(ch: Char): Script? {
        if (!ch.isLetter()) return null
        return when (ch.code) {
            in 0x0041..0x024F -> Script.LATIN
            in 0x0370..0x03FF -> Script.GREEK
            in 0x0400..0x052F -> Script.CYRILLIC
            in 0x0590..0x05FF -> Script.HEBREW
            in 0x0600..0x06FF, in 0x0750..0x077F -> Script.ARABIC
            in 0x0900..0x097F -> Script.DEVANAGARI
            in 0x0E00..0x0E7F -> Script.THAI
            in 0x3040..0x30FF -> Script.KANA
            in 0xAC00..0xD7AF, in 0x1100..0x11FF -> Script.HANGUL
            in 0x3400..0x4DBF, in 0x4E00..0x9FFF, in 0xF900..0xFAFF -> Script.HAN
            else -> null
        }
    }

    private fun dominantScript(text: String): Script {
        val counts = HashMap<Script, Int>()
        var total = 0
        for (ch in text) {
            val s = scriptOf(ch) ?: continue
            counts[s] = (counts[s] ?: 0) + 1
            total++
        }
        if (total == 0) return Script.UNKNOWN
        if ((counts[Script.KANA] ?: 0) > 0) return Script.KANA
        if ((counts[Script.HANGUL] ?: 0) * 4 >= total) return Script.HANGUL
        return counts.maxByOrNull { it.value }?.key ?: Script.UNKNOWN
    }

    private fun lyricsScript(rawLyrics: String): Script {
        val stripped = rawLyrics
            .replace(MarkupRegex, " ")
        return dominantScript(stripped)
    }

    private fun latinLanguage(rawLyrics: String, script: Script): String? {
        if (script != Script.LATIN) return null
        val tokens = rawLyrics
            .replace(MarkupRegex, " ")
            .lowercase(Locale.ROOT)
            .split(NonWordRegex)
            .map { it.trim('\'', '’') }
            .filter { it.isNotEmpty() }
        if (tokens.size < 12) return null
        val ranked = LanguageMarkers
            .map { (language, markers) -> language to tokens.count { it in markers } }
            .sortedByDescending { it.second }
        val (best, hits) = ranked.first()
        val runnerUp = ranked.getOrNull(1)?.second ?: 0
        return best.takeIf { hits * 10 >= tokens.size && hits * 2 >= runnerUp * 3 }
    }

    private const val ProviderTimeoutMs = 14_000L
    private val PreferredHindiProviders = setOf("BetterLyrics", "YouLyPlus")
    private val ChineseSources = setOf("Kugou", "NetEase", "QQMusic")
    private const val LatinPrefix = "latin:"

    private const val DuetPoints = 4
    private const val WordPoints = 3
    private const val BackgroundPoints = 2

    private const val FeatureWeight = 1_000
    private const val HindiBonus = 10_000
    private const val TimedWeight = 100_000
    private const val ScriptWeight = 1_000_000

    private val MarkupRegex = Regex("""\[[^\]]*\]|<[^>]*>|\{[^}]*\}""")
    private val NonWordRegex = Regex("""[^\p{L}'’]+""")

    private val LanguageMarkers: Map<String, Set<String>> = mapOf(
        "en" to setOf(
            "the", "and", "you", "i", "i'm", "my", "it's", "don't", "your", "that", "what", "with",
            "this", "be", "just", "can't", "won't", "when", "all", "for", "of", "we", "but", "now",
            "never", "you're", "i'll", "was", "are", "can", "know", "like", "want", "she", "he",
        ),
        "id" to setOf(
            "yang", "dan", "aku", "kau", "kamu", "ini", "itu", "tak", "tidak", "dengan", "untuk",
            "akan", "ada", "dalam", "hati", "saja", "bisa", "tapi", "jangan", "sudah", "lagi",
            "karena", "selalu", "engkau", "dia", "kita", "cinta", "ku", "mu", "dari", "pun",
            "pernah", "hanya", "semua", "takkan", "denganmu", "padamu", "diriku", "dirimu",
        ),
        "es" to setOf(
            "que", "y", "el", "los", "las", "yo", "pero", "porque", "quiero", "eres", "estoy",
            "cuando", "para", "del", "como", "más", "qué", "nada", "todo", "tú", "mí", "sí", "esta",
            "está", "contigo", "corazón", "vida", "siempre", "nunca", "puedo",
        ),
        "pt" to setOf(
            "você", "não", "eu", "meu", "minha", "é", "um", "pra", "tudo", "nao", "voce", "também",
            "mais", "quando", "sem", "isso", "ela", "ele", "só", "vou", "seu", "sua", "nós", "estou",
            "coração", "agora", "sempre", "nunca",
        ),
        "fr" to setOf(
            "je", "les", "et", "est", "pas", "pour", "dans", "moi", "toi", "mon", "c'est", "j'ai",
            "qui", "sur", "une", "des", "du", "au", "ne", "plus", "tout", "elle", "nous", "vous",
            "suis", "ça", "avec", "mais",
        ),
        "de" to setOf(
            "der", "die", "das", "und", "ich", "nicht", "ist", "ein", "eine", "mich", "mir", "dich",
            "dir", "wir", "mein", "auf", "mit", "wie", "nur", "auch", "sich", "noch", "bin", "bist",
            "kein", "immer", "zu",
        ),
        "it" to setOf(
            "il", "che", "non", "sono", "sei", "io", "per", "mio", "mia", "più", "cosa", "ancora",
            "questo", "della", "nel", "perché", "tutto", "anche", "gli", "voglio", "amore",
        ),
        "tl" to setOf(
            "ang", "ng", "sa", "ko", "mo", "na", "ka", "ako", "ikaw", "hindi", "lang", "mga",
            "kita", "ay", "naman", "pa", "sana", "akin", "ito", "iyong", "natin", "tayo", "puso",
        ),
        "nl" to setOf(
            "het", "een", "ik", "niet", "van", "dat", "mijn", "jij", "wij", "voor", "maar", "zijn",
            "wat", "wel", "nog", "ook", "geen", "heb", "ben",
        ),
        "tr" to setOf(
            "bir", "ve", "ben", "sen", "bu", "için", "çok", "gibi", "beni", "seni", "var", "yok",
            "değil", "olan", "her", "ama", "daha", "şey", "kadar",
        ),
        "vi" to setOf(
            "anh", "em", "và", "không", "là", "của", "một", "người", "yêu", "có", "những", "cho",
            "đã", "mình", "trong", "như", "tôi", "thì", "này",
        ),
        "ja-Latn" to setOf(
            "wa", "ga", "wo", "ni", "mo", "kimi", "boku", "watashi", "anata", "dake", "kara",
            "made", "kokoro", "demo", "itsumo", "nai", "koto", "toki", "naka", "sora",
        ),
        "ko-Latn" to setOf(
            "naega", "neol", "neoreul", "nan", "nae", "neon", "geu", "nal", "naneun", "neoui",
            "uri", "saranghae", "jigeum", "modeun", "ije", "geurae", "eonjena",
        ),
        "hi-Latn" to setOf(
            "hai", "hain", "mein", "main", "tera", "teri", "tere", "mera", "meri", "mere",
            "dil", "pyaar", "pyar", "ishq", "nahi", "nahin", "nhi", "bhi", "toh", "kya",
            "koi", "hum", "tum", "tujhe", "tujhko", "mujhe", "mujhko", "aur", "tha", "thi",
            "yeh", "woh", "kaise", "kyun", "kabhi", "apna", "apni", "apne", "saath", "zindagi",
        ),
    )
}
