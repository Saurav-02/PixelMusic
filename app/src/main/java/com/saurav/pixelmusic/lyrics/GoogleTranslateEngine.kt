package com.saurav.pixelmusic.lyrics

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.put
import java.text.Normalizer

object GoogleTranslateEngine {

    private const val LineMarker = "\n@@@\n"
    private const val BatchCharBudget = 1_500
    private const val LineByLineParallelism = 4
    private const val BingAuthUrl = "https://edge.microsoft.com/translate/auth"
    private const val BingBaseUrl = "https://api-edge.cognitive.microsofttranslator.com"
    private const val BingTokenTtlMs = 8 * 60_000L

    private data class GoogleEndpoint(val url: String, val client: String)

    private val googleEndpoints = listOf(
        GoogleEndpoint("https://translate.googleapis.com/translate_a/single", "gtx"),
        GoogleEndpoint("https://clients5.google.com/translate_a/single", "dict-chrome-ex"),
        GoogleEndpoint("https://translate.google.com/translate_a/single", "gtx"),
    )

    private val DiacriticsRegex = Regex("\\p{Mn}+")

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }
    private val romanizeCacheLock = Any()
    private val romanizeCache = object : LinkedHashMap<String, List<String?>>(64, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, List<String?>>?): Boolean = size > 64
    }

    private val bingTokenMutex = Mutex()
    @Volatile private var cachedBingToken: String? = null
    @Volatile private var cachedBingTokenExpiresAtMs: Long = 0L

    private val client by lazy {
        HttpClient(OkHttp) {
            expectSuccess = false
            install(HttpTimeout) {
                requestTimeoutMillis = 15_000
                connectTimeoutMillis = 10_000
                socketTimeoutMillis = 15_000
            }
        }
    }

    suspend fun translate(
        lines: List<String>,
        targetLanguage: String,
        sourceLanguage: String = "auto",
    ): List<String>? = withContext(Dispatchers.IO) {
        if (lines.isEmpty() || targetLanguage.isBlank()) return@withContext null

        val out = ArrayList<String>(lines.size)
        var batch = ArrayList<String>()
        var budget = 0

        suspend fun flush(): Boolean {
            if (batch.isEmpty()) return true
            val translated = translateBatch(batch, targetLanguage, sourceLanguage)
                ?: bingTranslateBatch(batch, targetLanguage, sourceLanguage)
                ?: translateLineByLine(batch, targetLanguage, sourceLanguage)
                ?: return false
            out += translated
            batch = ArrayList()
            budget = 0
            return true
        }

        for (line in lines) {
            if (budget + line.length > BatchCharBudget && batch.isNotEmpty()) {
                if (!flush()) return@withContext null
            }
            batch.add(line)
            budget += line.length + LineMarker.length
        }
        if (!flush()) return@withContext null

        if (out.size == lines.size) out else null
    }

    private suspend fun translateLineByLine(
        lines: List<String>,
        targetLanguage: String,
        sourceLanguage: String,
    ): List<String>? = coroutineScope {
        val gate = Semaphore(LineByLineParallelism)
        lines.map { line ->
            async { gate.withPermit { translateBatch(listOf(line), targetLanguage, sourceLanguage)?.single() } }
        }.awaitAll().let { results ->
            results.mapIndexed { index, translated -> translated ?: lines[index] }
                .takeIf { results.any { it != null } }
        }
    }

    private suspend fun translateBatch(
        lines: List<String>,
        targetLanguage: String,
        sourceLanguage: String,
    ): List<String>? {
        val payload = lines.joinToString(LineMarker)
        for (endpoint in googleEndpoints) {
            val body = runCatching {
                val response = client.get(endpoint.url) {
                    parameter("client", endpoint.client)
                    parameter("sl", sourceLanguage)
                    parameter("tl", targetLanguage)
                    parameter("dt", "t")
                    parameter("q", payload)
                }
                if (response.status.value != 200) return@runCatching null
                response.bodyAsText()
            }.getOrNull() ?: continue

            val joined = extractGoogleText(body, forRomanization = false) ?: continue
            val parts = joined.split(Regex("\\s*[@＠]{3}\\s*")).map { it.trim() }
            if (parts.size == lines.size) return parts
        }
        return null
    }

    suspend fun romanize(
        lines: List<String>,
        sourceLanguage: String = "auto",
    ): List<String?>? = withContext(Dispatchers.IO) {
        if (lines.isEmpty()) return@withContext null

        val cacheKey = "$sourceLanguage\u0000${lines.joinToString("\u0001")}"
        synchronized(romanizeCacheLock) {
            romanizeCache[cacheKey]
        }?.let { return@withContext it }

        val out = ArrayList<String?>(lines.size)
        var batch = ArrayList<String>()
        var budget = 0

        suspend fun flush(): Boolean {
            if (batch.isEmpty()) return true
            val romanized = romanizeBatch(batch, sourceLanguage)
                ?: bingRomanizeBatch(batch)
                ?: romanizeLineByLine(batch, sourceLanguage)
                ?: return false
            out += romanized
            batch = ArrayList()
            budget = 0
            return true
        }

        for (line in lines) {
            if (budget + line.length > BatchCharBudget && batch.isNotEmpty()) {
                if (!flush()) return@withContext null
            }
            batch.add(line)
            budget += line.length + LineMarker.length
        }
        if (!flush()) return@withContext null

        if (out.size != lines.size) return@withContext null
        val cleaned = out.mapIndexed { idx, rom ->
            val plain = rom?.let { stripDiacritics(it) }?.trim()
            if (plain.isNullOrEmpty() || plain.equals(lines[idx].trim(), ignoreCase = true)) null else plain
        }
        if (cleaned.any { it != null }) {
            synchronized(romanizeCacheLock) {
                romanizeCache[cacheKey] = cleaned
            }
        }
        cleaned
    }

    private suspend fun romanizeLineByLine(
        lines: List<String>,
        sourceLanguage: String,
    ): List<String?>? = coroutineScope {
        val gate = Semaphore(LineByLineParallelism)
        val results = lines.map { line ->
            async { gate.withPermit { romanizeBatch(listOf(line), sourceLanguage)?.singleOrNull() } }
        }.awaitAll()
        results.takeIf { it.any { rom -> !rom.isNullOrBlank() } }
    }

    private suspend fun romanizeBatch(
        lines: List<String>,
        sourceLanguage: String,
    ): List<String?>? {
        val payload = lines.joinToString(LineMarker)
        for (endpoint in googleEndpoints) {
            val body = runCatching {
                val response = client.get(endpoint.url) {
                    parameter("client", endpoint.client)
                    parameter("sl", sourceLanguage)
                    parameter("tl", "en")
                    parameter("dt", "rm")
                    parameter("q", payload)
                }
                if (response.status.value != 200) return@runCatching null
                response.bodyAsText()
            }.getOrNull() ?: continue

            val joined = extractGoogleText(body, forRomanization = true) ?: continue
            val parts = joined.split(Regex("\\s*[@＠]{3}\\s*")).map { it.trim() }
            if (parts.size == lines.size) return parts
        }
        return null
    }

    private fun extractGoogleText(body: String, forRomanization: Boolean): String? = runCatching {
        val element = json.parseToJsonElement(body)
        when (element) {
            is JsonArray -> {
                val segments = element.getOrNull(0) as? JsonArray ?: return@runCatching null
                val idx = if (forRomanization) 3 else 0
                buildString {
                    for (segment in segments) {
                        val piece = (segment as? JsonArray)?.getOrNull(idx) as? JsonPrimitive
                        if (piece != null && piece.isString) append(piece.content)
                    }
                }
            }
            is JsonObject -> {
                val sentences = element["sentences"] as? JsonArray ?: return@runCatching null
                val key = if (forRomanization) "src_translit" else "trans"
                buildString {
                    for (sentence in sentences) {
                        val piece = (sentence as? JsonObject)?.get(key) as? JsonPrimitive
                        if (piece != null && piece.isString) append(piece.content)
                    }
                }
            }
            else -> null
        }
    }.getOrNull()?.takeIf { it.isNotBlank() }

    private suspend fun getBingToken(forceRefresh: Boolean = false): String? {
        val now = System.currentTimeMillis()
        if (!forceRefresh) {
            val token = cachedBingToken
            if (token != null && now < cachedBingTokenExpiresAtMs) return token
        }
        return bingTokenMutex.withLock {
            val refreshedNow = System.currentTimeMillis()
            if (!forceRefresh) {
                val token = cachedBingToken
                if (token != null && refreshedNow < cachedBingTokenExpiresAtMs) return@withLock token
            }
            val token = runCatching {
                val response = client.get(BingAuthUrl)
                if (response.status.value != 200) return@runCatching null
                response.bodyAsText().trim().takeIf { it.isNotEmpty() }
            }.getOrNull()
            if (token != null) {
                cachedBingToken = token
                cachedBingTokenExpiresAtMs = System.currentTimeMillis() + BingTokenTtlMs
            }
            token
        }
    }

    private fun toBingLangCode(code: String): String = when (code) {
        "zh-CN" -> "zh-Hans"
        "zh-TW" -> "zh-Hant"
        "ku" -> "kmr"
        "ckb" -> "ku"
        "tl" -> "fil"
        "no" -> "nb"
        "sr" -> "sr-Latn"
        else -> code
    }

    private suspend fun bingTranslateBatch(
        lines: List<String>,
        targetLanguage: String,
        sourceLanguage: String,
    ): List<String>? {
        val token = getBingToken() ?: return null
        val bingTarget = toBingLangCode(targetLanguage)
        val payload = JsonArray(lines.map { buildJsonObject { put("Text", it) } }).toString()
        val body = runCatching {
            val response = client.post("$BingBaseUrl/translate") {
                parameter("api-version", "3.0")
                parameter("to", bingTarget)
                if (sourceLanguage != "auto" && sourceLanguage.isNotBlank()) {
                    parameter("from", toBingLangCode(sourceLanguage))
                }
                header("Authorization", "Bearer $token")
                contentType(ContentType.Application.Json)
                setBody(payload)
            }
            if (response.status.value == 401 || response.status.value == 403) {
                cachedBingToken = null
                return@runCatching null
            }
            if (response.status.value != 200) return@runCatching null
            response.bodyAsText()
        }.getOrNull() ?: return null

        return runCatching {
            val root = json.parseToJsonElement(body).jsonArray
            if (root.size != lines.size) return@runCatching null
            root.mapIndexed { idx, item ->
                val translations = (item as? JsonObject)?.get("translations") as? JsonArray
                val text = ((translations?.getOrNull(0) as? JsonObject)?.get("text") as? JsonPrimitive)
                    ?.takeIf { it.isString }?.content?.trim()
                text?.takeIf { it.isNotEmpty() } ?: lines[idx]
            }
        }.getOrNull()
    }

    private suspend fun bingRomanizeBatch(lines: List<String>): List<String?>? {
        val (language, fromScript) = detectBingTranslitScript(lines) ?: return null
        val token = getBingToken() ?: return null
        val payload = JsonArray(lines.map { buildJsonObject { put("Text", it) } }).toString()
        val body = runCatching {
            val response = client.post("$BingBaseUrl/transliterate") {
                parameter("api-version", "3.0")
                parameter("language", language)
                parameter("fromScript", fromScript)
                parameter("toScript", "Latn")
                header("Authorization", "Bearer $token")
                contentType(ContentType.Application.Json)
                setBody(payload)
            }
            if (response.status.value == 401 || response.status.value == 403) {
                cachedBingToken = null
                return@runCatching null
            }
            if (response.status.value != 200) return@runCatching null
            response.bodyAsText()
        }.getOrNull() ?: return null

        return runCatching {
            val root = json.parseToJsonElement(body).jsonArray
            if (root.size != lines.size) return@runCatching null
            val mapped = root.map { item ->
                ((item as? JsonObject)?.get("text") as? JsonPrimitive)
                    ?.takeIf { it.isString }?.content?.trim()
            }
            mapped.takeIf { it.any { rom -> !rom.isNullOrBlank() } }
        }.getOrNull()
    }

    private fun detectBingTranslitScript(lines: List<String>): Pair<String, String>? {
        var hasKana = false
        var hasCjk = false
        var hasHangul = false
        var hasDevanagari = false
        var hasBengali = false
        var hasTamil = false
        var hasTelugu = false
        var hasKannada = false
        var hasMalayalam = false
        var hasGujarati = false
        var hasGurmukhi = false
        var hasArabic = false
        var hasCyrillic = false
        var hasGreek = false
        var hasThai = false
        var hasHebrew = false

        for (line in lines) {
            for (ch in line) {
                val block = Character.UnicodeBlock.of(ch) ?: continue
                when (block) {
                    Character.UnicodeBlock.HIRAGANA,
                    Character.UnicodeBlock.KATAKANA -> hasKana = true
                    Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS,
                    Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A,
                    Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS -> hasCjk = true
                    Character.UnicodeBlock.HANGUL_SYLLABLES,
                    Character.UnicodeBlock.HANGUL_JAMO -> hasHangul = true
                    Character.UnicodeBlock.DEVANAGARI -> hasDevanagari = true
                    Character.UnicodeBlock.BENGALI -> hasBengali = true
                    Character.UnicodeBlock.TAMIL -> hasTamil = true
                    Character.UnicodeBlock.TELUGU -> hasTelugu = true
                    Character.UnicodeBlock.KANNADA -> hasKannada = true
                    Character.UnicodeBlock.MALAYALAM -> hasMalayalam = true
                    Character.UnicodeBlock.GUJARATI -> hasGujarati = true
                    Character.UnicodeBlock.GURMUKHI -> hasGurmukhi = true
                    Character.UnicodeBlock.ARABIC -> hasArabic = true
                    Character.UnicodeBlock.CYRILLIC -> hasCyrillic = true
                    Character.UnicodeBlock.GREEK -> hasGreek = true
                    Character.UnicodeBlock.THAI -> hasThai = true
                    Character.UnicodeBlock.HEBREW -> hasHebrew = true
                    else -> Unit
                }
            }
        }

        return when {
            hasKana -> "ja" to "Jpan"
            hasHangul -> "ko" to "Kore"
            hasCjk -> "zh-Hans" to "Hans"
            hasDevanagari -> "hi" to "Deva"
            hasBengali -> "bn" to "Beng"
            hasTamil -> "ta" to "Taml"
            hasTelugu -> "te" to "Telu"
            hasKannada -> "kn" to "Knda"
            hasMalayalam -> "ml" to "Mlym"
            hasGujarati -> "gu" to "Gujr"
            hasGurmukhi -> "pa" to "Guru"
            hasArabic -> "ar" to "Arab"
            hasCyrillic -> "ru" to "Cyrl"
            hasGreek -> "el" to "Grek"
            hasThai -> "th" to "Thai"
            hasHebrew -> "he" to "Hebr"
            else -> null
        }
    }

    internal fun stripDiacritics(text: String): String {
        val decomposed = Normalizer.normalize(text, Normalizer.Form.NFD)
        return DiacriticsRegex.replace(decomposed, "")
            .replace('đ', 'd')
            .replace('Đ', 'D')
            .replace('ł', 'l')
            .replace('Ł', 'L')
            .replace('ø', 'o')
            .replace('Ø', 'O')
            .replace("ß", "ss")
            .replace("æ", "ae")
            .replace("Æ", "Ae")
            .replace("œ", "oe")
            .replace("Œ", "Oe")
            .replace('’', '\'')
            .replace('‘', '\'')
    }
}
