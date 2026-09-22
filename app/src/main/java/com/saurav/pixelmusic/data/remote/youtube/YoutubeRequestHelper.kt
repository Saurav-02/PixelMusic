package com.saurav.pixelmusic.data.remote.youtube

import com.saurav.pixelmusic.data.model.youtube.UmihiSettings
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

object YoutubeRequestHelper {
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    fun browse(browseId: String, settings: UmihiSettings): String {
        return requestWithContext(
            url = Constants.YoutubeApi.Browse.URL,
            idName = "browseId",
            id = browseId,
            settings = settings
        )
    }

    fun requestContinuation(continuationToken: String, settings: UmihiSettings): String {
        return requestWithContext(
            url = Constants.YoutubeApi.Browse.URL,
            idName = "continuation",
            id = continuationToken,
            settings = settings
        )
    }

    fun getPlayerInfo(videoId: String): String {
        return requestWithContext(
            url = Constants.YoutubeApi.PlayerInfo.URL,
            idName = "videoId",
            id = videoId
        )
    }

    fun search(query: String): String {
        return requestWithContext(
            url = Constants.YoutubeApi.Search.URL,
            idName = "query",
            id = query
        )
    }

    fun nextUp(videoId: String): String {
        return requestWithContext(
            url = Constants.YoutubeApi.Next.URL,
            idName = "videoId",
            id = videoId
        )
    }

    fun like(videoId: String, settings: UmihiSettings): String {
        return requestWithTarget(
            url = "https://www.youtube.com/youtubei/v1/like/like",
            videoId = videoId,
            settings = settings
        )
    }

    fun removeLike(videoId: String, settings: UmihiSettings): String {
        return requestWithTarget(
            url = "https://www.youtube.com/youtubei/v1/like/removelike",
            videoId = videoId,
            settings = settings
        )
    }

    private fun requestWithTarget(
        url: String,
        videoId: String,
        settings: UmihiSettings
    ): String {
        val baseBody = YoutubeAuthHelper.buildContextBody(null, null, settings)
        val body = buildJsonObject {
            baseBody.forEach { (key, value) ->
                put(key, value)
            }
            put("target", buildJsonObject {
                put("videoId", JsonPrimitive(videoId))
            })
        }

        val headers = YoutubeAuthHelper.getHeaders(settings.cookies)
        return executePost(url, body.toString(), headers)
    }

    private fun requestWithContext(
        url: String,
        idName: String,
        id: String,
        settings: UmihiSettings? = null
    ): String {
        val body = YoutubeAuthHelper.buildContextBody(idName, id, settings)
        val headers = if (settings != null) {
            YoutubeAuthHelper.getHeaders(settings.cookies)
        } else {
            mapOf(
                "User-Agent" to Constants.YoutubeApi.USER_AGENT,
                "Accept-Language" to "en-US,en;q=0.9",
                "Sec-Fetch-Mode" to "navigate"
            )
        }
        return executePost(url, body.toString(), headers)
    }

    private fun executePost(url: String, jsonBody: String, headers: Map<String, Any>): String {
        val requestBody = jsonBody.toRequestBody(jsonMediaType)
        val builder = okhttp3.Request.Builder()
            .url(url)
            .post(requestBody)

        headers.forEach { (key, value) ->
            builder.addHeader(key, value.toString())
        }

        val streamProxy = saurav.shru.pixelmusic.innertube.YouTube.streamProxy
        val client = if (streamProxy != null) {
            okhttp3.OkHttpClient.Builder()
                .connectionPool(okhttp3.ConnectionPool(10, 5, java.util.concurrent.TimeUnit.MINUTES))
                .proxy(streamProxy)
                .build()
        } else {
            YoutubeHelper.client
        }

        client.newCall(builder.build()).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("HTTP ${response.code}: ${response.message}")
            }
            return response.body?.string().orEmpty()
        }
    }
}
