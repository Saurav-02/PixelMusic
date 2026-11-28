package com.saurav.pixelmusic.lyrics.providers.directlyrics

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.UserAgent
import io.ktor.client.plugins.compression.ContentEncoding
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

internal val LyricsJson = Json {
    ignoreUnknownKeys = true
    coerceInputValues = true
    isLenient = true
}

internal val lyricsHttp by lazy {
    HttpClient(OkHttp) {
        expectSuccess = true
        install(HttpTimeout) {
            connectTimeoutMillis = 5_000
            requestTimeoutMillis = 10_000
        }
        install(UserAgent) { agent = "PixelMusic/1.0 (Android)" }
        install(ContentNegotiation) {
            json(LyricsJson)
            json(LyricsJson, ContentType.Text.Plain)
            json(LyricsJson, ContentType.Text.Html)
        }
        install(ContentEncoding) {
            gzip()
            deflate()
        }
    }
}
