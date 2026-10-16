package com.saurav.pixelmusic.data.remote.lyrics_providers.util.matching

import java.util.Locale

object TextMatch {

    private val noiseBrackets = Regex(
        """[(\[]\s*(?:""" +
            "mp3[_ ]?\\d{2,3}k?|" +
            "\\d{2,3}\\s*kbps|" +
            "\\d{3,4}p?|" +
            "official\\s*(?:music\\s*)?(?:video|audio|lyric\\s*video|visualizer)?|" +
            "[^)\\]]*\\bvideo\\b[^)\\]]*|" + 
            "lyric[s]?\\s*video|lyric[s]?|" +
            "video\\s*oficial|audio|visualizer|" +
            "hd|hq|4k|remaster(?:ed)?(?:\\s*\\d{4})?|explicit(?:\\s*version)?|" +
            "closed\\s*captioned|wshh\\s*exclusive|getovarijante|" +
            "prod\\.?[^)\\]]*" +
            """)\s*[)\]]""",
        RegexOption.IGNORE_CASE
    )

    val featRegex = Regex("""[\s(\[]*(?:feat\.?|ft\.?|featuring)\s+([^()\[\]]+?)\s*[)\]]?$""", RegexOption.IGNORE_CASE)

    val parenFeatRegex = Regex("""[(\[]\s*(?:feat\.?|ft\.?|featuring)\s+([^()\[\]]+?)\s*[)\]]""", RegexOption.IGNORE_CASE)

    private val junkArtists = setOf(
        "unknown", "unknown artist", "<unknown>", "various artists", "va", "n/a", "not available", "artist",
    )

    fun isJunkArtist(raw: String?): Boolean =
        raw.isNullOrBlank() || raw.trim().lowercase(Locale.ROOT) in junkArtists

    private val leadingTrackNo = Regex("""^\s*\d{1,2}\s*[.)\-_]\s*""")

    private val garbledChars = Regex("""\?{2,}|\uFFFD+""")

    private val channelSuffix = Regex(
        """\s*(?:-\s*Topic|TV|Official(?:\s+(?:Music|Channel))?|Music|Records|Media)\s*$""",
        RegexOption.IGNORE_CASE
    )

    fun stripChannelSuffix(raw: String): String {
        var s = raw.trim()
        repeat(2) { s = channelSuffix.replace(s, "").trim(' ', '-', '_') }
        return s
    }

    fun cleanTitleArtist(raw: String): String {
        var s = raw
        s = noiseBrackets.replace(s, " ")
        s = leadingTrackNo.replace(s, "")
        s = garbledChars.replace(s, " ")
        s = s.replace(Regex("""["“”„«»]"""), " ")
        s = s.replace('_', ' ')
        s = s.replace(Regex("""[\s]+"""), " ").trim()
        s = s.trim(' ', '-', '|', '/', '(', ')', '[', ']', '.', ',')
        return s.trim()
    }

    private val cyrillicMap = mapOf(
        'а' to "a", 'б' to "b", 'в' to "v", 'г' to "g", 'д' to "d", 'ђ' to "dj", 'е' to "e", 'ё' to "e",
        'ж' to "z", 'з' to "z", 'и' to "i", 'й' to "j", 'ј' to "j", 'к' to "k", 'л' to "l", 'љ' to "lj",
        'м' to "m", 'н' to "n", 'њ' to "nj", 'о' to "o", 'п' to "p", 'р' to "r", 'с' to "s", 'т' to "t",
        'ћ' to "c", 'у' to "u", 'ф' to "f", 'х' to "h", 'ц' to "c", 'ч' to "c", 'џ' to "dz", 'ш' to "s",
        'щ' to "sh", 'ъ' to "", 'ы' to "y", 'ь' to "", 'э' to "e", 'ю' to "ju", 'я' to "ja",
    )

    private fun transliterateCyrillic(s: String): String =
        if (s.none { it in 'Ѐ'..'ӿ' }) s
        else buildString { for (c in s) append(cyrillicMap[c] ?: c.toString()) }

    fun normalizeForCompare(raw: String?): String {
        if (raw.isNullOrBlank()) return ""
        var s = raw.lowercase(Locale.ROOT)
        s = transliterateCyrillic(s)
        s = noiseBrackets.replace(s, " ")
        s = s.replace('_', ' ')
        s = featRegex.replace(s, " ")
        s = java.text.Normalizer.normalize(s, java.text.Normalizer.Form.NFD)
            .replace(Regex("""\p{Mn}+"""), "")
        s = s.replace(Regex("""[^a-z0-9 ]"""), " ")
        s = s.replace(Regex("""\s+"""), " ").trim()
        return s
    }

    private fun levenshtein(a: String, b: String): Int {
        if (a == b) return 0
        if (a.isEmpty()) return b.length
        if (b.isEmpty()) return a.length
        var prev = IntArray(b.length + 1) { it }
        var curr = IntArray(b.length + 1)
        for (i in 1..a.length) {
            curr[0] = i
            for (j in 1..b.length) {
                val cost = if (a[i - 1] == b[j - 1]) 0 else 1
                curr[j] = minOf(curr[j - 1] + 1, prev[j] + 1, prev[j - 1] + cost)
            }
            val tmp = prev; prev = curr; curr = tmp
        }
        return prev[b.length]
    }

    private fun levRatio(a: String, b: String): Double {
        if (a.isEmpty() && b.isEmpty()) return 1.0
        val maxLen = maxOf(a.length, b.length)
        if (maxLen == 0) return 1.0
        return 1.0 - levenshtein(a, b).toDouble() / maxLen
    }

    private fun tokenOverlap(a: String, b: String): Double {
        val ta = a.split(' ').filter { it.isNotBlank() }.toSet()
        val tb = b.split(' ').filter { it.isNotBlank() }.toSet()
        if (ta.isEmpty() || tb.isEmpty()) return 0.0
        val inter = ta.intersect(tb).size.toDouble()
        return inter / ta.union(tb).size.toDouble()
    }

    private fun containsPhrase(longer: String, shorter: String): Boolean {
        if (longer.isEmpty() || shorter.isEmpty()) return false
        val shorterTokens = shorter.split(' ').filter { it.isNotBlank() }
        if (shorterTokens.size < 2 && shorter.length < 7) return false
        return (" $longer ").contains(" $shorter ")
    }

    fun similarity(a: String?, b: String?): Double {
        val na = normalizeForCompare(a)
        val nb = normalizeForCompare(b)
        if (na.isEmpty() || nb.isEmpty()) return 0.0
        if (na == nb) return 1.0
        val containment = if (containsPhrase(na, nb) || containsPhrase(nb, na)) 0.92 else 0.0
        val blended = 0.6 * levRatio(na, nb) + 0.4 * tokenOverlap(na, nb)
        return maxOf(blended, containment)
    }
}

