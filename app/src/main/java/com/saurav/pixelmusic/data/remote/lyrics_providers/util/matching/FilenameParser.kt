package com.saurav.pixelmusic.data.remote.lyrics_providers.util.matching

import java.io.File

enum class MatchStrategy(val label: String) {
    TAGS("metadata tags"),
    FILENAME_ARTIST_TITLE("filename: Artist - Title"),
    FILENAME_PRIMARY_ARTIST("filename: primary artist"),
    FILENAME_TITLE_ARTIST("filename: Title - Artist"),
    FILENAME_LOOSE("filename: loosened"),
    FILENAME_TITLE_ONLY("filename: title only"),
    COVER_IDENTITY("cover identity"),
}

data class QueryCandidate(
    val title: String,
    val artist: String?,
    val strategy: MatchStrategy,
    val featuredArtists: List<String> = emptyList()
) {
    fun asSearchString(): String = listOfNotNull(artist?.takeIf { it.isNotBlank() }, title).joinToString(" ").trim()
}

object FilenameParser {
    private val collabSeparator = Regex(
        """\s+(?:x|×|&|vs\.?|feat\.?|ft\.?|featuring)\s+|,\s+""",
        RegexOption.IGNORE_CASE
    )

    private val versionSuffix = Regex(
        """\s*[(\[][^()\[\]]*\b(?:remix|version|edit|bootleg|flip|mix|cover|live|acoustic|instrumental|sped\s*up|slowed)\b[^()\[\]]*[)\]]\s*$""",
        RegexOption.IGNORE_CASE
    )

    private fun primaryArtist(artist: String): String =
        collabSeparator.split(artist).firstOrNull { it.isNotBlank() }?.trim() ?: artist

    private fun loosenTitle(title: String): String = versionSuffix.replace(title, "").trim()

    private fun collapse(s: String): String = s.lowercase().filter { it.isLetterOrDigit() }

    private fun splitFeaturedNames(value: String): List<String> = value
        .split(Regex("""\s*(?:,|&|x|and|\+)\s*""", RegexOption.IGNORE_CASE))
        .map { it.trim() }
        .filter { it.isNotBlank() }

    private fun extractFeatured(title: String): Pair<String, List<String>> {
        val names = mutableListOf<String>()
        var cleaned = TextMatch.parenFeatRegex.replace(title) { m ->
            names += splitFeaturedNames(m.groupValues[1]); " "
        }
        TextMatch.featRegex.find(cleaned)?.let { m ->
            names += splitFeaturedNames(m.groupValues[1])
            cleaned = cleaned.removeRange(m.range)
        }
        cleaned = cleaned.replace(Regex("""\s+"""), " ").trim().trim('-', '|', '/').trim()
        if (names.isEmpty()) return title to emptyList()
        return (cleaned.ifBlank { title }) to names
    }

    private val trailingJunkNumber = Regex("""\s+\d{2,4}$""")

    private fun stripTrailingJunkNumber(s: String): String = trailingJunkNumber.replace(s, "").trim()

    private val anyBracketClause = Regex("""\s*[(\[][^()\[\]]*(?:[)\]]|$)""")

    private fun stripAllBrackets(s: String): String =
        anyBracketClause.replace(s, " ").replace(Regex("""\s+"""), " ").trim().trim('-', '|', '/').trim()

    private fun candidate(title: String, artist: String?, strategy: MatchStrategy): QueryCandidate? {
        val cleanTitle = TextMatch.cleanTitleArtist(title)
        if (cleanTitle.isBlank()) return null
        val (finalTitle, titleFeat) = extractFeatured(cleanTitle)
        val (finalArtist, artistFeat) = artist?.let { TextMatch.cleanTitleArtist(it) }
            ?.takeIf { it.isNotBlank() }
            ?.let { extractFeatured(it) } ?: (null to emptyList())
        return QueryCandidate(finalTitle, finalArtist?.takeIf { it.isNotBlank() }, strategy, titleFeat + artistFeat)
    }

    fun candidates(tagTitle: String?, tagArtist: String?, filePath: String?): List<QueryCandidate> {
        val out = LinkedHashMap<String, QueryCandidate>()

        fun add(c: QueryCandidate?) {
            if (c == null) return
            val key = TextMatch.normalizeForCompare(c.title) + "|" + TextMatch.normalizeForCompare(c.artist)
            out.putIfAbsent(key, c)
        }

        fun addDashCandidates(raw: String) {
            val cleaned = TextMatch.cleanTitleArtist(raw)
            val dashParts = cleaned.split(Regex("""\s+-\s+"""), limit = 2).map { it.trim() }.filter { it.isNotBlank() }
            if (dashParts.size == 2) {
                val (artistPart, titlePart) = dashParts[0] to dashParts[1]
                add(candidate(titlePart, artistPart, MatchStrategy.FILENAME_ARTIST_TITLE)) 

                val primary = primaryArtist(artistPart)
                if (!primary.equals(artistPart, ignoreCase = true))
                    add(candidate(titlePart, primary, MatchStrategy.FILENAME_PRIMARY_ARTIST))

                val collabParts = collabSeparator.split(artistPart).map { it.trim() }.filter { it.isNotBlank() }
                if (collabParts.size > 1) collabParts.drop(1).take(2).forEach { part ->
                    add(candidate(titlePart, part, MatchStrategy.FILENAME_PRIMARY_ARTIST))
                }

                val loose = loosenTitle(titlePart)
                if (!loose.equals(titlePart, ignoreCase = true) && loose.isNotBlank())
                    add(candidate(loose, primary, MatchStrategy.FILENAME_LOOSE))

                val noJunkNo = stripTrailingJunkNumber(titlePart)
                if (!noJunkNo.equals(titlePart, ignoreCase = true) && noJunkNo.isNotBlank())
                    add(candidate(noJunkNo, artistPart, MatchStrategy.FILENAME_LOOSE))

                val noBrackets = stripAllBrackets(titlePart)
                if (!noBrackets.equals(titlePart, ignoreCase = true) && noBrackets.isNotBlank()) {
                    add(candidate(noBrackets, artistPart, MatchStrategy.FILENAME_LOOSE))
                    add(candidate(noBrackets, primary, MatchStrategy.FILENAME_LOOSE))
                }

                val collapsed = collapse(artistPart)
                if (collapsed.length >= 3 && collapsed != artistPart.lowercase())
                    add(candidate(titlePart, collapsed, MatchStrategy.FILENAME_PRIMARY_ARTIST))

                add(candidate(artistPart, titlePart, MatchStrategy.FILENAME_TITLE_ARTIST)) 
                add(candidate(titlePart, null, MatchStrategy.FILENAME_TITLE_ONLY))
            }
            add(candidate(cleaned, null, MatchStrategy.FILENAME_TITLE_ONLY))
            val loosePlain = loosenTitle(cleaned)
            if (!loosePlain.equals(cleaned, ignoreCase = true) && loosePlain.isNotBlank())
                add(candidate(loosePlain, null, MatchStrategy.FILENAME_TITLE_ONLY))
            val noJunkPlain = stripTrailingJunkNumber(cleaned)
            if (!noJunkPlain.equals(cleaned, ignoreCase = true) && noJunkPlain.isNotBlank())
                add(candidate(noJunkPlain, null, MatchStrategy.FILENAME_TITLE_ONLY))
            val noBracketsPlain = stripAllBrackets(cleaned)
            if (!noBracketsPlain.equals(cleaned, ignoreCase = true) && noBracketsPlain.isNotBlank())
                add(candidate(noBracketsPlain, null, MatchStrategy.FILENAME_TITLE_ONLY))
        }

        val title = tagTitle?.takeIf { it.isNotBlank() && it != "<unknown>" }
        val artist = tagArtist?.takeIf { !TextMatch.isJunkArtist(it) }
        if (title != null) {
            add(candidate(title, artist, MatchStrategy.TAGS))
            val channelless = artist?.let { TextMatch.stripChannelSuffix(it) }
            if (!channelless.isNullOrBlank() && !channelless.equals(artist, ignoreCase = true)) {
                add(candidate(title, channelless, MatchStrategy.TAGS))
            }

            if (artist != null) {
                val primary = primaryArtist(artist)
                if (!primary.equals(artist, ignoreCase = true) && primary.isNotBlank()) {
                    add(candidate(title, primary, MatchStrategy.FILENAME_PRIMARY_ARTIST))
                }

                val collabParts = collabSeparator.split(artist).map { it.trim() }.filter { it.isNotBlank() }
                if (collabParts.size > 1) {
                    for (part in collabParts.take(3)) {
                        add(candidate(title, part, MatchStrategy.FILENAME_PRIMARY_ARTIST))
                        if (part.contains(Regex("""\s+-\s+"""))) {
                            val hyphenated = part.replace(Regex("""\s+-\s+"""), "-").trim()
                            add(candidate(title, hyphenated, MatchStrategy.FILENAME_PRIMARY_ARTIST))
                        }
                    }
                } else if (artist.contains(Regex("""\s+-\s+"""))) {
                    val hyphenated = artist.replace(Regex("""\s+-\s+"""), "-").trim()
                    add(candidate(title, hyphenated, MatchStrategy.FILENAME_PRIMARY_ARTIST))
                }
            }

            val loose = loosenTitle(title)
            if (!loose.equals(title, ignoreCase = true) && loose.isNotBlank()) {
                add(candidate(loose, artist, MatchStrategy.FILENAME_LOOSE))
                artist?.let { add(candidate(loose, primaryArtist(it), MatchStrategy.FILENAME_LOOSE)) }
                add(candidate(loose, null, MatchStrategy.FILENAME_TITLE_ONLY))
            }

            val noBrackets = stripAllBrackets(title)
            if (!noBrackets.equals(title, ignoreCase = true) && noBrackets.isNotBlank()) {
                add(candidate(noBrackets, artist, MatchStrategy.FILENAME_LOOSE))
                artist?.let { add(candidate(noBrackets, primaryArtist(it), MatchStrategy.FILENAME_LOOSE)) }
                add(candidate(noBrackets, null, MatchStrategy.FILENAME_TITLE_ONLY))
            }

            add(candidate(title, null, MatchStrategy.FILENAME_TITLE_ONLY))

            if (title.contains(Regex("""\s+-\s+"""))) addDashCandidates(title)
        }

        val base = filePath?.let { File(it).nameWithoutExtension }
        if (base != null) addDashCandidates(base)

        return out.values.toList()
    }
}

