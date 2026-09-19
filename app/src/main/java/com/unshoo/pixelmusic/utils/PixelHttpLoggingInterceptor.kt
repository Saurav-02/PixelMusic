package com.unshoo.pixelmusic.utils

import com.unshoo.pixelmusic.utils.PixelLogger.Category
import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException

class PixelHttpLoggingInterceptor(
    private val name: String = "http",
    /** Return true to log, false to skip silently. */
    private val urlFilter: (String) -> Boolean = { true },
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val url = request.url.toString()

        if (!urlFilter(url)) return chain.proceed(request)

        val startNs = System.nanoTime()

        PixelLogger.d(Category.NETWORK, name, "→ ${request.method} $url")
        request.body?.let { body ->
            PixelLogger.d(Category.NETWORK, name, "  req body ${body.contentLength()} bytes")
        }

        return try {
            val response = chain.proceed(request)
            val tookMs = (System.nanoTime() - startNs) / 1_000_000
            val cl = response.body?.contentLength() ?: -1L

            PixelLogger.d(
                Category.NETWORK, name,
                "← ${response.code} ${request.url.host} " +
                    "(${tookMs}ms, ${if (cl >= 0) "$cl bytes" else "unknown size"})"
            )

            if (!response.isSuccessful) {
                PixelLogger.w(
                    Category.NETWORK, name,
                    "Non-2xx: ${response.code} ${response.message} @ $url"
                )
            }
            response
        } catch (e: IOException) {
            val tookMs = (System.nanoTime() - startNs) / 1_000_000
            PixelLogger.e(
                Category.NETWORK, name,
                "× ${request.method} $url failed after ${tookMs}ms: ${e.message}",
                e
            )
            throw e
        }
    }
}
