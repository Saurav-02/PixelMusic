package com.saurav.pixelmusic.utils.potoken

import android.content.Context
import android.webkit.CookieManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import timber.log.Timber

class PoTokenGenerator(context: Context) {
    private val TAG = "PoTokenGenerator"
    private val applicationContext = context.applicationContext

    private val webViewSupported by lazy { runCatching { CookieManager.getInstance() }.isSuccess }
    private var webViewBadImpl = false // whether the system has a bad WebView implementation

    private val webPoTokenGenLock = Mutex()
    private var webPoTokenSessionId: String? = null
    private var webPoTokenStreamingPot: String? = null
    private var webPoTokenGenerator: PoTokenWebView? = null

    suspend fun getWebClientPoToken(videoId: String, sessionId: String): PoTokenResult? {
        val effectiveSessionId = sessionId.takeIf { it.isNotBlank() }
            ?: saurav.shru.pixelmusic.innertube.YouTube.visitorData.takeIf { !it.isNullOrBlank() }
            ?: "CgtPQVo1TndqTldzWSjE0bC3Bg%3D%3D"
        Timber.tag(TAG).d("WebView state: supported=$webViewSupported, badImpl=$webViewBadImpl")
        if (!webViewSupported || webViewBadImpl) {
            Timber.tag(TAG).d("WebView not available: supported=$webViewSupported, badImpl=$webViewBadImpl")
            return null
        }

        return try {
            withTimeout(POTOKEN_TIMEOUT_MS) {
                getWebClientPoToken(videoId, effectiveSessionId, forceRecreate = false)
            }
        } catch (e: TimeoutCancellationException) {
            Timber.tag(TAG).w("poToken generation timed out after ${POTOKEN_TIMEOUT_MS}ms; proceeding without PoToken")
            clearGenerator()
            null
        } catch (e: CancellationException) {
            throw e
        } catch (e: BadWebViewException) {
            Timber.tag(TAG).e("Could not obtain PO token because WebView is unavailable")
            webViewBadImpl = true
            null
        } catch (e: Exception) {
            Timber.tag(TAG).e("PO token generation failed type=${e::class.simpleName ?: "unknown"}")
            throw e
        }
    }

    suspend fun close() {
        clearGenerator()
    }

    private suspend fun clearGenerator() {
        webPoTokenGenLock.withLock {
            try {
                withContext(Dispatchers.Main) {
                    webPoTokenGenerator?.close()
                }
            } catch (error: Exception) {
                Timber.tag(TAG).e("PO token WebView cleanup failed type=${error::class.simpleName ?: "unknown"}")
            }
            webPoTokenGenerator = null
            webPoTokenStreamingPot = null
            webPoTokenSessionId = null
        }
    }

    private companion object {
        const val POTOKEN_TIMEOUT_MS = 8_000L
    }

    private suspend fun getWebClientPoToken(videoId: String, sessionId: String, forceRecreate: Boolean): PoTokenResult {
        val (poTokenGenerator, streamingPot, hasBeenRecreated) =
            webPoTokenGenLock.withLock {
                val shouldRecreate =
                    forceRecreate || webPoTokenGenerator == null || webPoTokenGenerator!!.isExpired ||
                        webPoTokenGenerator!!.isDead ||
                        webPoTokenSessionId != sessionId

                if (shouldRecreate) {
                    Timber.tag(TAG).d("Creating new PoTokenWebView (forceRecreate=$forceRecreate)")

                    withContext(Dispatchers.Main) {
                        webPoTokenGenerator?.close()
                    }

                    webPoTokenGenerator = null
                    webPoTokenStreamingPot = null
                    webPoTokenSessionId = null

                    val newGenerator = PoTokenWebView.getNewPoTokenGenerator(applicationContext)

                    val newStreamingPot = try {
                        newGenerator.generatePoToken(sessionId)
                    } catch (t: Throwable) {
                        try {
                            newGenerator.close()
                        } catch (error: Exception) {
                            Timber.tag(TAG).e("PO token WebView cleanup failed type=${error::class.simpleName ?: "unknown"}")
                        }
                        throw t
                    }

                    webPoTokenGenerator = newGenerator
                    webPoTokenStreamingPot = newStreamingPot
                    webPoTokenSessionId = sessionId

                    Triple(newGenerator, newStreamingPot, true)
                } else {
                    Triple(webPoTokenGenerator!!, webPoTokenStreamingPot!!, false)
                }
            }

        val playerPot = try {
            poTokenGenerator.generatePoToken(videoId)
        } catch (t: Throwable) {
            Timber.tag(TAG).e("Exception while generating player poToken: ${t::class.simpleName}")

            if (t !is BadWebViewException && !hasBeenRecreated) {
                Timber.tag(TAG).d("Retrying poToken generation with a new WebView")
                return getWebClientPoToken(videoId, sessionId, forceRecreate = true)
            } else {
                throw t
            }
        }

        return PoTokenResult(
            playerRequestPoToken = playerPot,
            streamingDataPoToken = streamingPot,
        )
    }
}
