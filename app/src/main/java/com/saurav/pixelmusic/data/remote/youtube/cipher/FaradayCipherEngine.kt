package com.saurav.pixelmusic.data.remote.youtube.cipher

import io.ktor.client.HttpClient
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import io.ktor.http.Url
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class FaradayCipherEngine(
    private val httpClient: HttpClient,
    private val jsThread: CoroutineDispatcher,
    private val logger: InnerTubeLogger = InnerTubeLogger.NONE,
) {
    data class PlayerInfo(
        val playerId: String,
        val signatureTimestamp: Int,
    )

    data class DecodeResult(
        val signatures: Map<String, String>,
        val nParameters: Map<String, String>,
    )

    private val repository = InMemoryPlayerConfigRepository(PLAYER_CONFIG_URL)
    private val store = RemotePlayerConfigStore(httpClient, repository, logger)

    private val operationMutex = Mutex()
    private val mutex = Mutex()
    private var cachedPlayerId: String? = null
    private var cachedPlayerIdAtMs: Long = 0L
    private var cachedSolver: CachedSolver? = null

    private class CachedSolver(
        val playerId: String,
        val configEpoch: Long,
        val solver: ZemerCipherSolver,
    )

    suspend fun playerInfo(): PlayerInfo? = operationMutex.withLock { playerInfoLocked() }

    private suspend fun playerInfoLocked(): PlayerInfo? {
        val playerId = currentPlayerId() ?: return null
        val timestamp = store.getSignatureTimestamp(playerUrlFor(playerId)) ?: return null
        return PlayerInfo(playerId, timestamp)
    }

    suspend fun decode(
        playerId: String,
        signatures: List<String>,
        nParameters: List<String>,
    ): DecodeResult = operationMutex.withLock { decodeLocked(playerId, signatures, nParameters) }

    private suspend fun decodeLocked(
        playerId: String,
        signatures: List<String>,
        nParameters: List<String>,
    ): DecodeResult {
        if (signatures.isEmpty() && nParameters.isEmpty()) return DecodeResult(emptyMap(), emptyMap())
        val solver = solverFor(playerId) ?: return DecodeResult(emptyMap(), emptyMap())
        return DecodeResult(
            signatures =
                signatures.distinct().mapNotNull { challenge ->
                    solver.solveSignature(challenge)?.let { challenge to it }
                }.toMap(),
            nParameters =
                nParameters.distinct().mapNotNull { challenge ->
                    solver.solveN(challenge)?.let { challenge to it }
                }.toMap(),
        )
    }

    suspend fun invalidate() = operationMutex.withLock { invalidateLocked() }

    private suspend fun invalidateLocked() {
        mutex.withLock {
            cachedPlayerId = null
            cachedPlayerIdAtMs = 0L
            cachedSolver?.solver?.dispose()
            cachedSolver = null
        }
        runCatching { store.refreshAfterStreamRejection() }
    }

    private suspend fun currentPlayerId(): String? {
        val now = System.currentTimeMillis()
        mutex.withLock {
            cachedPlayerId?.takeIf { now - cachedPlayerIdAtMs < PLAYER_ID_TTL_MS }?.let { return it }
        }
        val fetched = fetchPlayerId() ?: return null
        mutex.withLock {
            cachedPlayerId = fetched
            cachedPlayerIdAtMs = System.currentTimeMillis()
        }
        return fetched
    }

    private suspend fun fetchPlayerId(): String? =
        try {
            val response = httpClient.getTextWithoutRedirects(Url(IFRAME_API_URL), MAX_IFRAME_BYTES) {
                header(HttpHeaders.UserAgent, USER_AGENT)
                header(HttpHeaders.Accept, "*/*")
            }
            val body = response.body
            if (body == null) {
                logger.w(TAG, "iframe_api returned HTTP ${response.status.value}")
                null
            } else {
                PLAYER_HASH_REGEX.find(body)?.groupValues?.getOrNull(1)
                    ?: run {
                        logger.w(TAG, "player hash not found in iframe_api")
                        null
                    }
            }
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            logger.w(TAG, "iframe_api fetch failed", details = mapOf("exceptionType" to error.logType()))
            null
        }

    private suspend fun solverFor(playerId: String): ZemerCipherSolver? {
        if (!PLAYER_ID_REGEX.matches(playerId)) return null
        val playerUrl = playerUrlFor(playerId)
        var config = store.getConfig(playerUrl)
        if (config == null) {
            store.forceRefresh(missingHash = playerId)
            config = store.getConfig(playerUrl)
        }
        if (config == null) {
            logger.d(TAG, "player not in remote table", details = mapOf("player" to playerId))
            return null
        }
        val epoch = store.configEpoch
        mutex.withLock {
            cachedSolver
                ?.takeIf { it.playerId == playerId && it.configEpoch == epoch }
                ?.let { return it.solver }
        }
        return try {
            val playerCode = downloadPlayerCode(playerUrl) ?: return null
            val created = ZemerCipherSolver.create(playerCode, config, jsThread)
            val replaced =
                mutex.withLock {
                    val previous = cachedSolver
                    cachedSolver = CachedSolver(playerId, epoch, created)
                    previous
                }
            replaced?.solver?.dispose()
            created
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            logger.w(
                TAG,
                "solver build failed",
                details = mapOf("player" to playerId, "exceptionType" to error.logType()),
            )
            null
        }
    }

    private suspend fun downloadPlayerCode(playerUrl: String): String? =
        try {
            val response = httpClient.getTextWithoutRedirects(Url(playerUrl), MAX_PLAYER_SCRIPT_BYTES) {
                header(HttpHeaders.UserAgent, USER_AGENT)
                header(HttpHeaders.Accept, "*/*")
                header("Referer", "https://www.youtube.com/")
            }
            response.body ?: run {
                logger.w(TAG, "player script HTTP ${response.status.value}")
                null
            }
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            logger.w(TAG, "player script fetch failed", details = mapOf("exceptionType" to error.logType()))
            null
        }

    private fun Throwable.logType(): String = this::class.simpleName ?: "Exception"

    companion object {
        private const val TAG = "FaradayCipherEngine"
        const val PLAYER_CONFIG_URL: String =
            "https://raw.githubusercontent.com/MetrolistGroup/faraday/master/registry/player_configs.json"
        private const val IFRAME_API_URL = "https://www.youtube.com/iframe_api"
        private const val USER_AGENT = "okhttp/5.4.0"
        private const val MAX_IFRAME_BYTES = 512 * 1024
        private const val MAX_PLAYER_SCRIPT_BYTES = 8 * 1024 * 1024
        private const val PLAYER_ID_TTL_MS = 30 * 60 * 1000L
        private val PLAYER_HASH_REGEX = Regex("""player\\?/([a-z0-9]{8})\\?/""")
        private val PLAYER_ID_REGEX = Regex("^[A-Za-z0-9_-]{4,32}$")

        internal fun playerUrlFor(playerId: String): String =
            "https://www.youtube.com/s/player/$playerId/player_ias.vflset/en_GB/base.js"
    }

    internal class InMemoryPlayerConfigRepository(
        private val url: String,
    ) : PlayerConfigRepository {
        override val enabled: Boolean = true
        override val sourceUrl: String = url
        override val defaultSourceUrl: String = url
        override var cachedJson: String = ""
        override var cachedAtMs: Long = 0L
        override var cachedSourceUrl: String = ""
        override var cachedEtag: String = ""
    }
}

