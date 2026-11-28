package com.saurav.pixelmusic.lyrics

import android.content.Context
import androidx.datastore.preferences.core.stringPreferencesKey
import com.saurav.pixelmusic.data.ai.provider.AiClientFactory
import com.saurav.pixelmusic.data.ai.provider.AiProvider
import com.saurav.pixelmusic.data.ai.provider.AiProviderSupport
import com.saurav.pixelmusic.data.database.AiUsageDao
import com.saurav.pixelmusic.data.database.AiUsageEntity
import com.saurav.pixelmusic.data.preferences.dataStore
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import timber.log.Timber

@EntryPoint
@InstallIn(SingletonComponent::class)
interface AiLyricsTranslatorEntryPoint {
    fun aiUsageDao(): AiUsageDao
}

/**
 * Translates synced lyric lines using the user's configured AI provider and API key
 * (from AI Settings), logging token usage to [AiUsageDao]. Returns null on missing key
 * or failure so callers can fall back cleanly to [GoogleTranslateEngine].
 */
object AiLyricsTranslator {

    private const val ChunkSize = 40
    private val AiProviderKey = stringPreferencesKey("ai_provider")
    private val NumberedLineRegex = Regex("""^\s*\[(\d+)]\s*(.*)$""")

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }
    private val clientFactory = AiClientFactory()

    private val cacheLock = Any()
    private val cache = object : LinkedHashMap<String, List<String>>(64, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, List<String>>?): Boolean = size > 64
    }

    suspend fun translate(
        context: Context,
        lines: List<String>,
        targetLanguageName: String,
    ): List<String>? = withContext(Dispatchers.IO) {
        if (lines.isEmpty() || targetLanguageName.isBlank()) return@withContext null

        val prefs = runCatching { context.dataStore.data.first() }.getOrNull() ?: return@withContext null
        val selectedProvider = AiProvider.fromString(prefs[AiProviderKey] ?: AiProvider.GEMINI.name)
        val providerChain = AiProviderSupport.buildProviderChain(selectedProvider)

        val resolvedProvider = providerChain.firstOrNull { provider ->
            prefs[stringPreferencesKey("${provider.name.lowercase()}_api_key")].orEmpty().trim().isNotBlank()
        } ?: return@withContext null

        val apiKey = prefs[stringPreferencesKey("${resolvedProvider.name.lowercase()}_api_key")].orEmpty().trim()
        val savedModel = prefs[stringPreferencesKey("${resolvedProvider.name.lowercase()}_model")].orEmpty().trim()
        val client = runCatching { clientFactory.createClient(resolvedProvider, apiKey) }.getOrNull() ?: return@withContext null
        val primaryModel = savedModel.ifBlank { client.getDefaultModel() }

        val cacheKey = "${resolvedProvider.name}\u0000$primaryModel\u0000$targetLanguageName\u0000${lines.joinToString("\u0001")}"
        synchronized(cacheLock) { cache[cacheKey] }?.let { return@withContext it }

        val systemPrompt = """
            You are a song lyrics translator. Translate each numbered lyric line naturally into $targetLanguageName while preserving the emotional tone and meaning.
            Keep the exact [index] prefix on every line, output one line per input index in order, and do not add any extra commentary or markdown formatting.
        """.trimIndent()

        val usageDao = runCatching {
            EntryPointAccessors.fromApplication(
                context.applicationContext,
                AiLyricsTranslatorEntryPoint::class.java
            ).aiUsageDao()
        }.getOrNull()

        val out = ArrayList<String>(lines.size)
        var usedModel = primaryModel
        var totalPromptChars = 0
        var totalOutputChars = 0

        for (chunk in lines.chunked(ChunkSize)) {
            val numberedInput = chunk.mapIndexed { idx, line -> "[$idx] $line" }.joinToString("\n")
            val prompt = "Translate these ${chunk.size} lyric lines into $targetLanguageName:\n$numberedInput"

            val raw = runCatching {
                client.generateContent(
                    model = usedModel,
                    systemPrompt = systemPrompt,
                    prompt = prompt,
                    temperature = 0.3f,
                )
            }.getOrElse { primaryError ->
                val defaultModel = client.getDefaultModel()
                if (usedModel != defaultModel) {
                    Timber.tag("AiLyricsTranslator").w(primaryError, "Model %s failed, retrying with %s", usedModel, defaultModel)
                    usedModel = defaultModel
                    runCatching {
                        client.generateContent(
                            model = usedModel,
                            systemPrompt = systemPrompt,
                            prompt = prompt,
                            temperature = 0.3f,
                        )
                    }.onFailure { fallbackError ->
                        Timber.tag("AiLyricsTranslator").w(fallbackError, "AI lyrics translation failed on fallback model %s", usedModel)
                    }.getOrNull()
                } else {
                    Timber.tag("AiLyricsTranslator").w(primaryError, "AI lyrics translation failed on %s", usedModel)
                    null
                }
            } ?: return@withContext null

            totalPromptChars += systemPrompt.length + prompt.length
            totalOutputChars += raw.length

            val parsed = parseTranslatedLines(raw, chunk) ?: run {
                Timber.tag("AiLyricsTranslator").w("Failed to parse AI lyrics response for %d lines", chunk.size)
                return@withContext null
            }
            out += parsed
        }

        if (usageDao != null && totalOutputChars > 0) {
            runCatching {
                usageDao.insertUsage(
                    AiUsageEntity(
                        timestamp = System.currentTimeMillis(),
                        provider = resolvedProvider.displayName,
                        model = usedModel,
                        promptType = "LYRICS_TRANSLATION",
                        promptTokens = (totalPromptChars / 4).coerceAtLeast(1),
                        outputTokens = (totalOutputChars / 4).coerceAtLeast(1),
                        thoughtTokens = 0,
                    )
                )
            }.onFailure { error ->
                Timber.tag("AiLyricsTranslator").e(error, "Failed to log AI lyrics translation usage")
            }
        }

        if (out.size != lines.size) return@withContext null
        synchronized(cacheLock) { cache[cacheKey] = out }
        out
    }

    private fun parseTranslatedLines(raw: String, originalChunk: List<String>): List<String>? {
        val cleaned = raw.trim()
            .removePrefix("```json")
            .removePrefix("```")
            .removeSuffix("```")
            .trim()

        // 1. Primary: numbered `[index] translated line` format
        val byIndex = HashMap<Int, String>(originalChunk.size)
        for (line in cleaned.lines()) {
            val match = NumberedLineRegex.matchEntire(line) ?: continue
            val idx = match.groupValues[1].toIntOrNull() ?: continue
            if (idx in originalChunk.indices) {
                val text = match.groupValues[2].trim()
                if (text.isNotEmpty()) byIndex[idx] = text
            }
        }
        val minRequired = ((originalChunk.size * 3) + 3) / 4 // at least 75% matched
        if (byIndex.size >= minRequired) {
            return originalChunk.indices.map { idx -> byIndex[idx] ?: originalChunk[idx] }
        }

        // 2. Secondary: JSON array fallback if the model returned JSON anyway
        val jsonParsed = runCatching {
            val start = cleaned.indexOf('[')
            val end = cleaned.lastIndexOf(']')
            if (start < 0 || end <= start) return@runCatching null
            val array = json.parseToJsonElement(cleaned.substring(start, end + 1)).jsonArray
            if (array.size != originalChunk.size) return@runCatching null
            array.map { item ->
                (item as? JsonPrimitive)?.takeIf { it.isString }?.content?.trim() ?: return@runCatching null
            }
        }.getOrNull()
        if (jsonParsed != null) return jsonParsed

        // 3. Tertiary: plain line-by-line match if exact line count
        val plainLines = cleaned.lines().map { it.trim() }.filter { it.isNotEmpty() }
        if (plainLines.size == originalChunk.size) return plainLines

        return null
    }
}
