package com.saurav.pixelmusic.data.diagnostics

import android.content.Context
import android.os.Build
import android.os.Debug
import android.os.PowerManager
import android.os.SystemClock
import android.view.Choreographer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.coroutines.resume

/**
 * Lightweight, opt-in diagnostics & frame-jank probe manager.
 * Zero continuous background overhead when disabled.
 */
object PerformanceDiagnosticsTracker {
    private const val MAX_EVENTS = 100
    private const val SESSION_TTL_MS = 24L * 60L * 60L * 1000L

    private val lock = Any()
    private var enabled = false
    private var sessionStartedEpochMs: Long? = null
    private var expiresAtEpochMs: Long? = null
    private var droppedCount = 0L
    private val events = ArrayDeque<AdvancedDiagnosticEventEntry>(MAX_EVENTS)

    fun isEnabled(): Boolean = synchronized(lock) {
        expireIfNeededLocked()
        enabled
    }

    fun eventCount(): Int = synchronized(lock) {
        expireIfNeededLocked()
        events.size
    }

    fun setEnabled(enable: Boolean) {
        synchronized(lock) {
            if (enable) {
                val now = System.currentTimeMillis()
                enabled = true
                sessionStartedEpochMs = now
                expiresAtEpochMs = now + SESSION_TTL_MS
                recordLocked(
                    type = "session",
                    name = "diagnostics_enabled",
                    details = mapOf("ttlHours" to "24")
                )
            } else {
                enabled = false
                sessionStartedEpochMs = null
                expiresAtEpochMs = null
                events.clear()
                droppedCount = 0L
            }
        }
    }

    fun markLag(
        context: Context,
        note: String,
        activeMime: String? = null
    ): Int {
        val runtime = Runtime.getRuntime()
        val heapUsedMb = ((runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024)).coerceAtLeast(0L)
        val heapMaxMb = (runtime.maxMemory() / (1024 * 1024)).coerceAtLeast(1L)
        val nativeMb = (Debug.getNativeHeapAllocatedSize() / (1024 * 1024)).coerceAtLeast(0L)
        val thermal = readThermalStatus(context)

        val details = buildMap {
            if (note.isNotBlank()) put("note", note)
            put("jvmHeapMb", "${heapUsedMb}/${heapMaxMb}")
            put("nativeHeapMb", nativeMb.toString())
            put("thermal", thermal)
            if (!activeMime.isNullOrBlank()) put("mime", activeMime)
        }

        return synchronized(lock) {
            if (!enabled) {
                val now = System.currentTimeMillis()
                enabled = true
                sessionStartedEpochMs = now
                expiresAtEpochMs = now + SESSION_TTL_MS
            }
            recordLocked(
                type = "user_mark",
                name = "lag_mark",
                details = details
            )
            events.size
        }
    }

    fun snapshot(): AdvancedDiagnosticsSection = synchronized(lock) {
        expireIfNeededLocked()
        AdvancedDiagnosticsSection(
            enabled = enabled,
            sessionStartedIso = sessionStartedEpochMs?.let(::formatIsoUtc),
            expiresAtIso = expiresAtEpochMs?.let(::formatIsoUtc),
            eventCount = events.size,
            droppedEventCount = droppedCount,
            events = events.toList()
        )
    }

    private fun expireIfNeededLocked() {
        val exp = expiresAtEpochMs ?: return
        if (System.currentTimeMillis() >= exp) {
            enabled = false
            sessionStartedEpochMs = null
            expiresAtEpochMs = null
            events.clear()
            droppedCount = 0L
        }
    }

    private fun recordLocked(
        type: String,
        name: String,
        details: Map<String, String>
    ) {
        if (events.size >= MAX_EVENTS) {
            events.removeFirst()
            droppedCount += 1L
        }
        events.addLast(
            AdvancedDiagnosticEventEntry(
                elapsedRealtimeMs = SystemClock.elapsedRealtime(),
                type = type,
                name = name,
                details = details
            )
        )
    }

    /**
     * Runs a short, real Choreographer frame-time probe on the Main thread (default ~1.5s)
     * to measure actual UI frame intervals, P95, worst frame, and jank percentage.
     */
    suspend fun runFrameJankProbe(
        refreshRateHz: Double,
        durationMs: Long = 1_500L
    ): UiPerformanceSection = withContext(Dispatchers.Main) {
        val safeRefresh = refreshRateHz.coerceIn(30.0, 240.0)
        val budgetMs = 1000.0 / safeRefresh
        val jankThresholdMs = budgetMs * 1.35

        val frameDeltasMs = suspendCancellableCoroutine<List<Double>> { cont ->
            val choreographer = Choreographer.getInstance()
            val samples = ArrayList<Double>(180)
            var lastNanos = -1L
            var warmupSkipped = 0
            val startNanos = System.nanoTime()
            val maxDurationNanos = durationMs * 1_000_000L

            val callback = object : Choreographer.FrameCallback {
                override fun doFrame(frameTimeNanos: Long) {
                    if (!cont.isActive) return
                    if (lastNanos > 0L) {
                        val deltaMs = (frameTimeNanos - lastNanos) / 1_000_000.0
                        if (deltaMs in 1.0..1000.0) {
                            if (warmupSkipped < 2) {
                                warmupSkipped++
                            } else {
                                samples.add(deltaMs)
                            }
                        }
                    }
                    lastNanos = frameTimeNanos
                    if (frameTimeNanos - startNanos >= maxDurationNanos || samples.size >= 240) {
                        cont.resume(samples)
                    } else {
                        choreographer.postFrameCallback(this)
                    }
                }
            }

            choreographer.postFrameCallback(callback)
            cont.invokeOnCancellation {
                choreographer.removeFrameCallback(callback)
            }
        }

        if (frameDeltasMs.isEmpty()) {
            return@withContext UiPerformanceSection(
                refreshRateHz = safeRefresh,
                targetFrameBudgetMs = budgetMs,
                sampledFrames = 0,
                avgFrameMs = 0.0,
                p95FrameMs = 0.0,
                worstFrameMs = 0.0,
                jankFrames = 0,
                jankPercent = 0.0
            )
        }

        val sorted = frameDeltasMs.sorted()
        val avg = frameDeltasMs.average()
        val p95Index = ((sorted.size - 1) * 0.95).toInt().coerceIn(0, sorted.lastIndex)
        val p95 = sorted[p95Index]
        val worst = sorted.last()
        val jankCount = frameDeltasMs.count { it > jankThresholdMs }
        val jankPct = (jankCount.toDouble() / frameDeltasMs.size.toDouble()) * 100.0

        UiPerformanceSection(
            refreshRateHz = safeRefresh,
            targetFrameBudgetMs = budgetMs,
            sampledFrames = frameDeltasMs.size,
            avgFrameMs = avg,
            p95FrameMs = p95,
            worstFrameMs = worst,
            jankFrames = jankCount,
            jankPercent = jankPct
        )
    }

    fun readThermalStatus(context: Context): String {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return "UNSUPPORTED"
        val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager ?: return "UNKNOWN"
        return when (pm.currentThermalStatus) {
            PowerManager.THERMAL_STATUS_NONE -> "NONE"
            PowerManager.THERMAL_STATUS_LIGHT -> "LIGHT"
            PowerManager.THERMAL_STATUS_MODERATE -> "MODERATE"
            PowerManager.THERMAL_STATUS_SEVERE -> "SEVERE"
            PowerManager.THERMAL_STATUS_CRITICAL -> "CRITICAL"
            PowerManager.THERMAL_STATUS_EMERGENCY -> "EMERGENCY"
            PowerManager.THERMAL_STATUS_SHUTDOWN -> "SHUTDOWN"
            else -> "UNKNOWN"
        }
    }

    fun formatIsoUtc(epochMs: Long): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
        sdf.timeZone = TimeZone.getTimeZone("UTC")
        return sdf.format(Date(epochMs))
    }
}
