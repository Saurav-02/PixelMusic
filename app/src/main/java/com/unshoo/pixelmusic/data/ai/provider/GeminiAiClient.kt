package com.unshoo.pixelmusic.data.ai.provider

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.time.Duration

/**
 * Gemini AI provider implemented against the public Gemini REST API
 * (generativelanguage.googleapis.com).
 *
 * Deliberately avoids every Gemini SDK artifact so the app is not coupled
 * to any specific Ktor version. The previous
 * `NoClassDefFoundError: io.ktor.client.plugins.HttpTimeout` crash was
 * caused by the legacy SDK being compiled against Ktor 2.x while this
 * project uses Ktor 3.x. Talking to the REST API directly removes that
 * entire class of failure.
 */
class GeminiAiClient(private val apiKey: String) : AiClient {

    companion object {
        private const val DEFAULT_GEMINI_MODEL = "gemini-3-flash-preview"
        private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta"
        private val JSON_MEDIA = "application/json; charset=utf-8".toMediaType()
    }

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(Duration.ofSeconds(15))
            .readTimeout(Duration.ofSeconds(60))
            .writeTimeout(Duration.ofSeconds(60))
            .build()
    }

    override suspend fun generateContent(
        model: String,
        systemPrompt: String,
        prompt: String,
        temperature: Float
    ): String = withContext(Dispatchers.IO) {
        val resolvedModel = model.ifBlank { DEFAULT_GEMINI_MODEL }

        val userParts = buildJsonArray {
            add(buildJsonObject { put("text", prompt) })
        }
        val userContent = buildJsonObject {
            put("role", "user")
            put("parts", userParts)
        }
        val contents = buildJsonArray { add(userContent) }

        val payloadObj = buildJsonObject {
            put("contents", contents)
            if (systemPrompt.isNotBlank()) {
                val sysParts = buildJsonArray {
                    add(buildJsonObject { put("text", systemPrompt) })
                }
                put("systemInstruction", buildJsonObject { put("parts", sysParts) })
            }
            put("generationConfig", buildJsonObject {
                put("temperature", temperature)
                put("topK", 64)
                put("topP", 0.95)
            })
        }

        val payload = payloadObj.toString()
        val url = "$BASE_URL/models/$resolvedModel:generateContent?key=$apiKey"

        val request = Request.Builder()
            .url(url)
            .post(payload.toRequestBody(JSON_MEDIA))
            .build()

        httpClient.newCall(request).execute().use { response ->
            val body = response.body?.string().orEmpty()

            if (!response.isSuccessful) {
                throw AiProviderSupport.createException(
                    providerName = "Gemini",
                    statusCode = response.code,
                    transportMessage = "HTTP ${response.code}",
                    responseBody = body,
                    requestedModel = resolvedModel
                )
            }

            val text = extractText(body)
            if (text.isNullOrBlank()) {
                throw AiProviderSupport.createException(
                    providerName = "Gemini",
                    statusCode = response.code,
                    transportMessage = "Gemini returned an empty response. The model may have filtered the content.",
                    responseBody = body,
                    requestedModel = resolvedModel
                )
            }
            text
        }
    }

    override suspend fun countTokens(
        model: String,
        systemPrompt: String,
        prompt: String
    ): Int = withContext(Dispatchers.IO) {
        val combined = buildString {
            if (systemPrompt.isNotBlank()) {
                append(systemPrompt)
                append("\n\n")
            }
            append(prompt)
        }

        val fallback = (combined.length / 4).coerceAtLeast(1)

        try {
            val resolvedModel = model.ifBlank { DEFAULT_GEMINI_MODEL }

            val parts = buildJsonArray {
                add(buildJsonObject { put("text", combined) })
            }
            val content = buildJsonObject { put("parts", parts) }
            val contents = buildJsonArray { add(content) }
            val payloadObj = buildJsonObject { put("contents", contents) }

            val url = "$BASE_URL/models/$resolvedModel:countTokens?key=$apiKey"
            val request = Request.Builder()
                .url(url)
                .post(payloadObj.toString().toRequestBody(JSON_MEDIA))
                .build()

            httpClient.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()
                if (!response.isSuccessful) return@withContext fallback

                val tokens = runCatching {
                    (json.parseToJsonElement(body) as? JsonObject)
                        ?.get("totalTokens")
                        ?.jsonPrimitive
                        ?.intOrNull
                }.getOrNull()

                tokens ?: fallback
            }
        } catch (_: Exception) {
            fallback
        }
    }

    override suspend fun getAvailableModels(apiKey: String): List<String> =
        withContext(Dispatchers.IO) {
            try {
                val url = "$BASE_URL/models?key=$apiKey"
                val request = Request.Builder().url(url).get().build()

                httpClient.newCall(request).execute().use { response ->
                    val body = response.body?.string().orEmpty()
                    if (!response.isSuccessful) return@withContext getDefaultModels()
                    parseModelsFromResponse(body).ifEmpty { getDefaultModels() }
                }
            } catch (_: Exception) {
                getDefaultModels()
            }
        }

    override suspend fun validateApiKey(apiKey: String): Boolean =
        withContext(Dispatchers.IO) {
            try {
                val url = "$BASE_URL/models?key=$apiKey"
                val request = Request.Builder().url(url).get().build()
                httpClient.newCall(request).execute().use { it.isSuccessful }
            } catch (_: Exception) {
                false
            }
        }

    override fun getDefaultModel(): String = DEFAULT_GEMINI_MODEL

    private fun extractText(responseBody: String): String? {
        val root = runCatching {
            json.parseToJsonElement(responseBody) as? JsonObject
        }.getOrNull() ?: return null

        val candidates = root["candidates"] as? JsonArray ?: return null
        val first = candidates.firstOrNull() as? JsonObject ?: return null
        val content = first["content"] as? JsonObject ?: return null
        val parts = content["parts"] as? JsonArray ?: return null

        val builder = StringBuilder()
        for (element in parts) {
            val text = (element as? JsonObject)
                ?.get("text")
                ?.jsonPrimitive
                ?.contentOrNull
            if (!text.isNullOrEmpty()) builder.append(text)
        }
        return builder.toString().ifBlank { null }
    }

    private fun parseModelsFromResponse(jsonResponse: String): List<String> {
        return runCatching {
            val root = json.parseToJsonElement(jsonResponse) as? JsonObject ?: return emptyList()
            val models = root["models"] as? JsonArray ?: return emptyList()

            val result = mutableListOf<String>()
            for (element in models) {
                val name = (element as? JsonObject)
                    ?.get("name")
                    ?.jsonPrimitive
                    ?.contentOrNull
                    ?: continue
                val short = name.removePrefix("models/")
                if (short.startsWith("gemini", ignoreCase = true) &&
                    !short.contains("embedding", ignoreCase = true)
                ) {
                    result.add(short)
                }
            }
            result
        }.getOrDefault(emptyList())
    }

    private fun getDefaultModels(): List<String> = listOf(
        "gemini-3-flash-preview",
        "gemini-3.1-pro-preview",
        "gemini-3.1-flash-lite-preview",
        "gemini-flash-latest",
        "gemini-2.5-flash",
        "gemini-2.5-pro",
        "gemini-2.0-flash"
    )
}
