package com.unshoo.pixelmusic.utils

import com.unshoo.pixelmusic.utils.PixelLogger.Category
import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException

class PixelHttpLoggingInterceptor(
    private val name: String = "http",
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val startNs = System.nanoTime()

        PixelLogger.d(
            Category.NETWORK, name,
            "→ ${request.method} ${request.url}"
        )
        if (request.body != null) {
            PixelLogger.d(Category.NETWORK, name, "  body: ${request.body!!.contentLength()} bytes")
        }

        return try {
            val response = chain.proceed(request)
            val tookMs = (System.nanoTime() - startNs) / 1_000_000

            PixelLogger.d(
                Category.NETWORK, name,
                "← ${response.code} ${request.url.host}${request.url.encodedPath} " +
                    "(${tookMs}ms, ${response.body?.contentLength() ?: -1} bytes)"
            )

            if (!response.isSuccessful) {
                PixelLogger.w(
                    Category.NETWORK, name,
                    "Non-2xx: ${response.code} ${response.message} @ ${request.url}"
                )
            }
            response
        } catch (e: IOException) {
            val tookMs = (System.nanoTime() - startNs) / 1_000_000
            PixelLogger.e(
                Category.NETWORK, name,
                "× ${request.method} ${request.url} failed after ${tookMs}ms: ${e.message}",
                e
            )
            throw e
        }
    }
}
