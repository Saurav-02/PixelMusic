package com.saurav.pixelmusic.data.remote.lyrics_providers.apple

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

class AppleTokenManager(private val client: OkHttpClient) {
    private var cachedToken: String? = null
    private val mutex = Mutex()

    suspend fun getToken(): String = withContext(Dispatchers.IO) {
        mutex.withLock {
            cachedToken?.let { return@withContext it }

            try {
                val mainPageRequest = Request.Builder()
                    .url("https://beta.music.apple.com")
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .build()

                val mainPageResponse = client.newCall(mainPageRequest).execute()
                val mainPageBody = mainPageResponse.body?.string() ?: throw Exception("Empty response from Apple Music")

                val indexJsRegex = Regex("""/assets/index~[^/]+\.js""")
                val indexJsMatch = indexJsRegex.find(mainPageBody)
                    ?: throw Exception("Could not find index script URL")

                val indexJsUri = indexJsMatch.value
                val jsRequest = Request.Builder()
                    .url("https://beta.music.apple.com$indexJsUri")
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .build()

                val jsResponse = client.newCall(jsRequest).execute()
                val jsBody = jsResponse.body?.string() ?: throw Exception("Empty JS response")

                val tokenRegex = Regex("""eyJh([^"]*)""")
                val tokenMatch = tokenRegex.find(jsBody)
                    ?: throw Exception("Could not find Apple token")

                val token = tokenMatch.value
                cachedToken = token
                token
            } catch (e: Exception) {
                throw Exception("Error fetching Apple Music token: ${e.message}", e)
            }
        }
    }

    fun clearToken() {
        cachedToken = null
    }
}

