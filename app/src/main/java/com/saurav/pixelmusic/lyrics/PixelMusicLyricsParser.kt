package com.saurav.pixelmusic.lyrics

import com.saurav.pixelmusic.data.model.Lyrics
import com.saurav.pixelmusic.data.model.SyncedLine
import com.saurav.pixelmusic.data.model.SyncedWord

/**
 * Parses PixelMusic's rich LRC formats into PixelMusic's [Lyrics] model:
 * - Standard LRC: `[mm:ss.xx]text`
 * - Enhanced syllable/word LRC: `[mm:ss.xxx]<mm:ss.xxx>word <mm:ss.xxx>word` (YouLyPlus / eLRC)
 * - Word-span sidecar lines: `<word:startSec:endSec|word2:startSec:endSec>` (BetterLyrics / Paxsenix / DirectLyrics)
 * - Duet agent & background vocal tags: `{agent:v1}`, `{agent:v2}`, `{bg}`
 */
object PixelMusicLyricsParser {

    private val LineTagRegex = Regex("""^\[(\d{1,3}):(\d{2})(?:[.:](\d{1,3}))?](.*)$""")
    private val InlineWordRegex = Regex("""<(\d{1,3}):(\d{2})(?:[.:](\d{1,3}))?>([^<]*)""")
    private val AgentRegex = Regex("""^\{agent:([^}]+)\}""")
    private const val BgPrefix = "{bg}"

    fun parse(raw: String, providerName: String? = null, areFromRemote: Boolean = true): Lyrics {
        val rawLines = raw.replace("\r\n", "\n").lines()
        val synced = mutableListOf<SyncedLine>()
        val plain = mutableListOf<String>()

        var i = 0
        while (i < rawLines.size) {
            val trimmed = rawLines[i].trim()
            if (trimmed.isEmpty()) {
                i++
                continue
            }
            // Skip orphan word-span lines
            if (trimmed.startsWith("<") && trimmed.endsWith(">") && trimmed.contains("|")) {
                i++
                continue
            }

            val lineMatch = LineTagRegex.matchEntire(trimmed)
            if (lineMatch != null) {
                val timeMs = parseTimestampMs(
                    lineMatch.groupValues[1],
                    lineMatch.groupValues[2],
                    lineMatch.groupValues[3],
                )
                var body = lineMatch.groupValues[4].trim()
                var isBg = false
                var agent: String? = null

                if (body.startsWith(BgPrefix)) {
                    isBg = true
                    body = body.removePrefix(BgPrefix).trim()
                } else {
                    val agentMatch = AgentRegex.find(body)
                    if (agentMatch != null) {
                        agent = agentMatch.groupValues[1].trim()
                        body = body.removeRange(agentMatch.range).trim()
                    }
                }

                // Check for inline <mm:ss.xxx>word timing
                var words: List<SyncedWord>? = null
                val inlineMatches = InlineWordRegex.findAll(body).toList()
                val cleanText = if (inlineMatches.isNotEmpty()) {
                    val parsedWords = mutableListOf<SyncedWord>()
                    val sb = StringBuilder()
                    inlineMatches.forEachIndexed { idx, m ->
                        val wStart = parseTimestampMs(m.groupValues[1], m.groupValues[2], m.groupValues[3])
                        val wEnd = inlineMatches.getOrNull(idx + 1)?.let { next ->
                            parseTimestampMs(next.groupValues[1], next.groupValues[2], next.groupValues[3])
                        }
                        val piece = m.groupValues[4]
                        if (piece.isNotEmpty()) {
                            sb.append(piece)
                            val cleaned = piece.trim()
                            if (cleaned.isNotEmpty()) {
                                parsedWords.add(
                                    SyncedWord(
                                        time = wStart,
                                        word = cleaned,
                                        startsNewWord = idx == 0 || piece.startsWith(" ") || inlineMatches[idx - 1].groupValues[4].endsWith(" "),
                                        endTime = wEnd,
                                    )
                                )
                            }
                        }
                    }
                    if (parsedWords.isNotEmpty()) words = parsedWords
                    sb.toString().replace(Regex("\\s+"), " ").trim()
                } else {
                    body
                }

                // Check next line for PixelMusic `<word:startSec:endSec|...>` sidecar
                val nextLine = rawLines.getOrNull(i + 1)?.trim()
                if (nextLine != null && nextLine.startsWith("<") && nextLine.endsWith(">") && nextLine.contains(":")) {
                    val sidecarWords = parseSidecarWords(nextLine.substring(1, nextLine.length - 1))
                    if (sidecarWords.isNotEmpty()) {
                        words = sidecarWords
                        i++
                    }
                }

                if (cleanText.isNotEmpty()) {
                    synced.add(
                        SyncedLine(
                            time = timeMs,
                            line = cleanText,
                            words = words,
                            agent = agent,
                            isBackground = isBg,
                        )
                    )
                    plain.add(cleanText)
                }
            } else if (!trimmed.startsWith("[") || !trimmed.endsWith("]")) {
                // Plain text line (ignore metadata tags like [ar:...], [ti:...])
                plain.add(trimmed)
            }
            i++
        }

        val isFlatSameTime = synced.size >= 2 &&
            synced.none { !it.words.isNullOrEmpty() } &&
            synced.all { it.time == synced.first().time }
        val sortedSynced = if (isFlatSameTime) null else synced.sortedBy { it.time }.takeIf { it.isNotEmpty() }
        val finalPlain = plain.takeIf { it.isNotEmpty() }
        return Lyrics(
            plain = finalPlain,
            synced = sortedSynced,
            areFromRemote = areFromRemote,
            providerName = providerName,
        )
    }

    /**
     * Returns true when [rawLrc] has real progressing timestamps (at least two distinct line
     * timestamps, or a single timed line), rejecting broken LRCs where every line is `[00:00.00]`.
     */
    fun hasUsableSyncedLyrics(rawLrc: String?): Boolean {
        if (rawLrc.isNullOrBlank()) return false
        var firstMs: Int? = null
        var count = 0
        for (rawLine in rawLrc.lineSequence()) {
            val match = LineTagRegex.matchEntire(rawLine.trim()) ?: continue
            if (match.groupValues[4].trim().isEmpty()) continue
            val ms = parseTimestampMs(
                match.groupValues[1],
                match.groupValues[2],
                match.groupValues[3],
            )
            count++
            if (firstMs == null) {
                firstMs = ms
            } else if (ms != firstMs) {
                return true
            }
        }
        return count == 1
    }

    private fun parseSidecarWords(inner: String): List<SyncedWord> {
        val result = mutableListOf<SyncedWord>()
        inner.split("|").forEach { token ->
            val lastColon = token.lastIndexOf(':')
            if (lastColon <= 0) return@forEach
            val prevColon = token.lastIndexOf(':', lastColon - 1)
            if (prevColon <= 0) return@forEach
            val wordText = token.substring(0, prevColon).trim()
            val startSec = token.substring(prevColon + 1, lastColon).toDoubleOrNull() ?: return@forEach
            val endSec = token.substring(lastColon + 1).toDoubleOrNull() ?: return@forEach
            if (wordText.isNotEmpty()) {
                result.add(
                    SyncedWord(
                        time = (startSec * 1000.0).toInt().coerceAtLeast(0),
                        word = wordText,
                        startsNewWord = true,
                        endTime = (endSec * 1000.0).toInt().coerceAtLeast((startSec * 1000.0).toInt()),
                    )
                )
            }
        }
        return result
    }

    private fun parseTimestampMs(minStr: String, secStr: String, fracStr: String): Int {
        val mins = minStr.toIntOrNull() ?: 0
        val secs = secStr.toIntOrNull() ?: 0
        val millis = when (fracStr.length) {
            0 -> 0
            1 -> (fracStr.toIntOrNull() ?: 0) * 100
            2 -> (fracStr.toIntOrNull() ?: 0) * 10
            else -> fracStr.take(3).toIntOrNull() ?: 0
        }
        return mins * 60_000 + secs * 1000 + millis
    }
}

/**
 * Lightweight on-device romanizer for Devanagari (Hindi), Cyrillic (Russian/Ukrainian/etc.),
 * and Japanese Kana so Romanization works offline without extra libraries.
 */
object PixelMusicRomanizer {

    /**
     * Returns true if [text] contains any letter outside Latin Unicode blocks
     * (e.g. Japanese Kanji/Kana, Korean Hangul, Chinese, Devanagari, Arabic, Cyrillic, Thai, etc.).
     */
    fun hasNonLatinScript(text: String): Boolean = text.any { ch ->
        if (!Character.isLetter(ch)) return@any false
        val block = Character.UnicodeBlock.of(ch) ?: return@any true
        when (block) {
            Character.UnicodeBlock.BASIC_LATIN,
            Character.UnicodeBlock.LATIN_1_SUPPLEMENT,
            Character.UnicodeBlock.LATIN_EXTENDED_A,
            Character.UnicodeBlock.LATIN_EXTENDED_B,
            Character.UnicodeBlock.LATIN_EXTENDED_ADDITIONAL -> false
            Character.UnicodeBlock.HALFWIDTH_AND_FULLWIDTH_FORMS ->
                ch !in '\uFF21'..'\uFF3A' && ch !in '\uFF41'..'\uFF5A'
            else -> true
        }
    }

    fun needsRomanization(text: String): Boolean = text.any { ch ->
        val block = Character.UnicodeBlock.of(ch)
        block == Character.UnicodeBlock.DEVANAGARI ||
            block == Character.UnicodeBlock.CYRILLIC ||
            block == Character.UnicodeBlock.HIRAGANA ||
            block == Character.UnicodeBlock.KATAKANA
    }

    fun romanize(text: String): String? {
        if (!needsRomanization(text)) return null
        val sb = StringBuilder(text.length * 2)
        var i = 0
        while (i < text.length) {
            val ch = text[i]
            val next = text.getOrNull(i + 1)
            when {
                ch in '\u0900'..'\u097F' -> {
                    val cons = devanagariConsonants[ch]
                    if (cons != null) {
                        sb.append(cons)
                        when {
                            next == '\u094D' -> i++ // halant suppresses inherent 'a'
                            next != null && next in devanagariMatras -> {
                                sb.append(devanagariMatras[next])
                                i++
                            }
                            next != null && next in '\u0900'..'\u097F' -> sb.append('a')
                        }
                    } else {
                        sb.append(devanagariVowels[ch] ?: devanagariMatras[ch] ?: ch)
                    }
                }
                ch in cyrillicMap -> sb.append(cyrillicMap[ch])
                ch == 'っ' || ch == 'ッ' -> {
                    val nextRom = next?.let { n ->
                        val nextNext = text.getOrNull(i + 2)
                        val digraph = nextNext?.let { kanaDigraphs["$n$it"] }
                        digraph ?: kanaMap[n]
                    }
                    if (!nextRom.isNullOrEmpty()) {
                        if (nextRom.startsWith("ch")) sb.append('t')
                        else if (nextRom[0] !in "aeiou") sb.append(nextRom[0])
                    }
                }
                next != null && "$ch$next" in kanaDigraphs -> {
                    sb.append(kanaDigraphs["$ch$next"])
                    i++
                }
                ch in kanaMap -> sb.append(kanaMap[ch])
                else -> sb.append(ch)
            }
            i++
        }
        val out = sb.toString().replace(Regex("\\s+"), " ").trim()
        return out.takeIf { it.isNotBlank() && it != text.trim() }
    }

    private val devanagariVowels = mapOf(
        'अ' to "a", 'आ' to "aa", 'इ' to "i", 'ई' to "ee", 'उ' to "u", 'ऊ' to "oo",
        'ऋ' to "ri", 'ए' to "e", 'ऐ' to "ai", 'ओ' to "o", 'औ' to "au",
        'ं' to "n", 'ँ' to "n", 'ः' to "h", '।' to ".",
    )

    private val devanagariMatras = mapOf(
        'ा' to "aa", 'ि' to "i", 'ी' to "ee", 'ु' to "u", 'ू' to "oo",
        'ृ' to "ri", 'े' to "e", 'ै' to "ai", 'ो' to "o", 'ौ' to "au",
        'ं' to "n", 'ँ' to "n", 'ः' to "h",
    )

    private val devanagariConsonants: Map<Char, String> = mapOf(
        'क' to "k", 'ख' to "kh", 'ग' to "g", 'घ' to "gh", 'ङ' to "ng",
        'च' to "ch", 'छ' to "chh", 'ज' to "j", 'झ' to "jh", 'ञ' to "ny",
        'ट' to "t", 'ठ' to "th", 'ड' to "d", 'ढ' to "dh", 'ण' to "n",
        'त' to "t", 'थ' to "th", 'द' to "d", 'ध' to "dh", 'न' to "n",
        'प' to "p", 'फ' to "ph", 'ब' to "b", 'भ' to "bh", 'म' to "m",
        'य' to "y", 'र' to "r", 'ल' to "l", 'व' to "v",
        'श' to "sh", 'ष' to "sh", 'स' to "s", 'ह' to "h",
        '\u0958' to "q", '\u0959' to "kh", '\u095A' to "gh", '\u095B' to "z",
        '\u095C' to "r", '\u095D' to "rh", '\u095E' to "f",
    )

    private val cyrillicMap = mapOf(
        'А' to "A", 'а' to "a", 'Б' to "B", 'б' to "b", 'В' to "V", 'в' to "v",
        'Г' to "G", 'г' to "g", 'Д' to "D", 'д' to "d", 'Е' to "E", 'е' to "e",
        'Ё' to "Yo", 'ё' to "yo", 'Ж' to "Zh", 'ж' to "zh", 'З' to "Z", 'з' to "z",
        'И' to "I", 'и' to "i", 'Й' to "Y", 'й' to "y", 'К' to "K", 'к' to "k",
        'Л' to "L", 'л' to "l", 'М' to "M", 'м' to "m", 'Н' to "N", 'н' to "n",
        'О' to "O", 'о' to "o", 'П' to "P", 'п' to "p", 'Р' to "R", 'р' to "r",
        'С' to "S", 'с' to "s", 'Т' to "T", 'т' to "t", 'У' to "U", 'у' to "u",
        'Ф' to "F", 'ф' to "f", 'Х' to "Kh", 'х' to "kh", 'Ц' to "Ts", 'ц' to "ts",
        'Ч' to "Ch", 'ч' to "ch", 'Ш' to "Sh", 'ш' to "sh", 'Щ' to "Shch", 'щ' to "shch",
        'Ы' to "Y", 'ы' to "y", 'Э' to "E", 'э' to "e", 'Ю' to "Yu", 'ю' to "yu",
        'Я' to "Ya", 'я' to "ya", 'ь' to "", 'ъ' to "", 'Ь' to "", 'Ъ' to "",
    )

    private val kanaMap = mapOf(
        'あ' to "a", 'い' to "i", 'う' to "u", 'え' to "e", 'お' to "o",
        'か' to "ka", 'き' to "ki", 'く' to "ku", 'け' to "ke", 'こ' to "ko",
        'さ' to "sa", 'し' to "shi", 'す' to "su", 'せ' to "se", 'そ' to "so",
        'た' to "ta", 'ち' to "chi", 'つ' to "tsu", 'て' to "te", 'と' to "to",
        'な' to "na", 'に' to "ni", 'ぬ' to "nu", 'ね' to "ne", 'の' to "no",
        'は' to "ha", 'ひ' to "hi", 'ふ' to "fu", 'へ' to "he", 'ほ' to "ho",
        'ま' to "ma", 'み' to "mi", 'む' to "mu", 'め' to "me", 'も' to "mo",
        'や' to "ya", 'ゆ' to "yu", 'よ' to "yo",
        'ら' to "ra", 'り' to "ri", 'る' to "ru", 'れ' to "re", 'ろ' to "ro",
        'わ' to "wa", 'を' to "wo", 'ん' to "n",
        'が' to "ga", 'ぎ' to "gi", 'ぐ' to "gu", 'げ' to "ge", 'ご' to "go",
        'ざ' to "za", 'じ' to "ji", 'ず' to "zu", 'ぜ' to "ze", 'ぞ' to "zo",
        'だ' to "da", 'ぢ' to "ji", 'づ' to "zu", 'で' to "de", 'ど' to "do",
        'ば' to "ba", 'び' to "bi", 'ぶ' to "bu", 'べ' to "be", 'ぼ' to "bo",
        'ぱ' to "pa", 'ぴ' to "pi", 'ぷ' to "pu", 'ぺ' to "pe", 'ぽ' to "po",
        'ア' to "a", 'イ' to "i", 'ウ' to "u", 'エ' to "e", 'オ' to "o",
        'カ' to "ka", 'キ' to "ki", 'ク' to "ku", 'ケ' to "ke", 'コ' to "ko",
        'サ' to "sa", 'シ' to "shi", 'ス' to "su", 'セ' to "se", 'ソ' to "so",
        'タ' to "ta", 'チ' to "chi", 'ツ' to "tsu", 'テ' to "te", 'ト' to "to",
        'ナ' to "na", 'ニ' to "ni", 'ヌ' to "nu", 'ネ' to "ne", 'ノ' to "no",
        'ハ' to "ha", 'ヒ' to "hi", 'フ' to "fu", 'ヘ' to "he", 'ホ' to "ho",
        'マ' to "ma", 'ミ' to "mi", 'ム' to "mu", 'メ' to "me", 'モ' to "mo",
        'ヤ' to "ya", 'ユ' to "yu", 'ヨ' to "yo",
        'ラ' to "ra", 'リ' to "ri", 'ル' to "ru", 'レ' to "re", 'ロ' to "ro",
        'ワ' to "wa", 'ヲ' to "wo", 'ン' to "n", 'ー' to "-",
        'ぁ' to "a", 'ぃ' to "i", 'ぅ' to "u", 'ぇ' to "e", 'ぉ' to "o",
        'ゃ' to "ya", 'ゅ' to "yu", 'ょ' to "yo", 'ゎ' to "wa",
        'ァ' to "a", 'ィ' to "i", 'ゥ' to "u", 'ェ' to "e", 'ォ' to "o",
        'ャ' to "ya", 'ュ' to "yu", 'ョ' to "yo", 'ヮ' to "wa", 'ヴ' to "vu",
    )

    private val kanaDigraphs = mapOf(
        "きゃ" to "kya", "きゅ" to "kyu", "きょ" to "kyo",
        "しゃ" to "sha", "しゅ" to "shu", "しょ" to "sho",
        "ちゃ" to "cha", "ちゅ" to "chu", "ちょ" to "cho",
        "にゃ" to "nya", "にゅ" to "nyu", "にょ" to "nyo",
        "ひゃ" to "hya", "ひゅ" to "hyu", "ひょ" to "hyo",
        "みゃ" to "mya", "みゅ" to "myu", "みょ" to "myo",
        "りゃ" to "rya", "りゅ" to "ryu", "りょ" to "ryo",
        "ぎゃ" to "gya", "ぎゅ" to "gyu", "ぎょ" to "gyo",
        "じゃ" to "ja", "じゅ" to "ju", "じょ" to "jo",
        "びゃ" to "bya", "びゅ" to "byu", "びょ" to "byo",
        "ぴゃ" to "pya", "ぴゅ" to "pyu", "ぴょ" to "pyo",
        "キャ" to "kya", "キュ" to "kyu", "キョ" to "kyo",
        "シャ" to "sha", "シュ" to "shu", "ショ" to "sho",
        "チャ" to "cha", "チュ" to "chu", "チョ" to "cho",
        "ニャ" to "nya", "ニュ" to "nyu", "ニョ" to "nyo",
        "ヒャ" to "hya", "ヒュ" to "hyu", "ヒョ" to "hyo",
        "ミャ" to "mya", "ミュ" to "myu", "ミョ" to "myo",
        "リャ" to "rya", "リュ" to "ryu", "リョ" to "ryo",
        "ギャ" to "gya", "ギュ" to "gyu", "ギョ" to "gyo",
        "ジャ" to "ja", "ジュ" to "ju", "ジョ" to "jo",
        "ビャ" to "bya", "ビュ" to "byu", "ビョ" to "byo",
        "ピャ" to "pya", "ピュ" to "pyu", "ピョ" to "pyo",
        "ティ" to "ti", "ディ" to "di", "トゥ" to "tu", "ドゥ" to "du",
        "ファ" to "fa", "フィ" to "fi", "フェ" to "fe", "フォ" to "fo",
        "ウィ" to "wi", "ウェ" to "we", "ウォ" to "wo",
        "ヴァ" to "va", "ヴィ" to "vi", "ヴェ" to "ve", "ヴォ" to "vo",
        "シェ" to "she", "ジェ" to "je", "チェ" to "che", "ツァ" to "tsa",
    )
}
