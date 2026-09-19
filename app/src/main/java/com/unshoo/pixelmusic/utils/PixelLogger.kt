package com.unshoo.pixelmusic.utils

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean

object PixelLogger {

    enum class Category(val shortName: String) {
        NETWORK("NET"),
        PLAYER("PLR"),
        QUEUE("QUE"),
        LYRICS("LYR"),
        AUTH("AUT"),
        SYNC("SYN"),
        DB("DB "),
        LIFE("LIF"),
        UI("UI "),
        MISC("MSC")
    }

    data class Entry(
        val timestampMs: Long,
        val level: Char,           // D, I, W, E
        val category: Category,
        val tag: String,
        val message: String,
        val throwable: Throwable? = null,
    ) {
        fun format(): String {
            val ts = TS_FORMAT.format(Date(timestampMs))
            val head = "$ts $level/${category.shortName} $tag: $message"
            return if (throwable == null) head
            else "$head\n${Log.getStackTraceString(throwable)}"
        }
    }

    private const val MAX_BUFFER = 2000
    private const val MAX_FILE_BYTES = 2L * 1024L * 1024L
    private const val MAX_ROTATED_FILES = 5

    private val TS_FORMAT = SimpleDateFormat("HH:mm:ss.SSS", Locale.US)

    private val _enabled = MutableStateFlow(false)
    val enabled: StateFlow<Boolean> = _enabled.asStateFlow()

    private val _entries = MutableStateFlow<List<Entry>>(emptyList())
    val entries: StateFlow<List<Entry>> = _entries.asStateFlow()

    private val buffer = ArrayDeque<Entry>(MAX_BUFFER)
    private val writeChannel = Channel<Entry>(capacity = Channel.UNLIMITED)
    private val ioScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val started = AtomicBoolean(false)

    private var logDir: File? = null
    private var currentLogFile: File? = null

/** Call once from Application.onCreate() */
fun init(context: Context) {
    if (!started.compareAndSet(false, true)) return

    // Unconditional probes. Bypass the enabled flag, bypass any level filter.
    // If any of these five reach LogFox, we know the process is alive and
    // which levels the OS / LogFox are letting through.
    Log.v("PM-BOOT", "init: v probe")
    Log.d("PM-BOOT", "init: d probe")
    Log.i("PM-BOOT", "init: i probe")
    Log.w("PM-BOOT", "init: w probe")
    Log.e("PM-BOOT", "init: e probe")

    val dir = File(context.filesDir, "logs").apply { mkdirs() }
    logDir = dir
    currentLogFile = File(dir, "pixelmusic.log")
    ioScope.launch { drainToFile() }
}

    fun setEnabled(value: Boolean) {
        _enabled.value = value
        if (value) i(Category.MISC, "Logger", "Enabled")
    }

    fun clear() {
        buffer.clear()
        _entries.value = emptyList()
        ioScope.launch {
            currentLogFile?.delete()
            currentLogFile = logDir?.let { File(it, "pixelmusic.log") }
        }
    }

    fun snapshot(): List<Entry> = _entries.value

    fun exportText(): String = buildString {
        snapshot().forEach { appendLine(it.format()) }
    }

    // ---- public logging API ----

    fun d(category: Category, tag: String, message: String) =
        log('D', category, tag, message, null)

    fun i(category: Category, tag: String, message: String) =
        log('I', category, tag, message, null)

    fun w(category: Category, tag: String, message: String, throwable: Throwable? = null) =
        log('W', category, tag, message, throwable)

    fun e(category: Category, tag: String, message: String, throwable: Throwable? = null) =
        log('E', category, tag, message, throwable)

    private fun log(
        level: Char,
        category: Category,
        tag: String,
        message: String,
        throwable: Throwable?,
    ) {
        val enabledNow = _enabled.value
        // Always mirror to Logcat when enabled — never when disabled
        // so a released build with logging off is zero-cost.
        if (!enabledNow) return

        val entry = Entry(
            timestampMs = System.currentTimeMillis(),
            level = level,
            category = category,
            tag = tag,
            message = message,
            throwable = throwable,
        )

// Android drops tags longer than 23 chars on some OEM ROMs.
// Format: PM-<3-char category>/<up to 12 chars of tag>  = max 20 chars.
val logTag = "PM-${category.shortName}/${tag.take(12)}"
when (level) {
    'D' -> Log.d(logTag, message)
    'I' -> Log.i(logTag, message)
    'W' -> Log.w(logTag, message, throwable)
    'E' -> Log.e(logTag, message, throwable)
}

        // Ring buffer
        synchronized(buffer) {
            if (buffer.size >= MAX_BUFFER) buffer.removeFirst()
            buffer.addLast(entry)
            _entries.value = buffer.toList()
        }

        // File sink
        writeChannel.trySend(entry)
    }

    private suspend fun drainToFile() {
        for (entry in writeChannel) {
            try {
                val file = currentLogFile ?: continue
                if (file.length() >= MAX_FILE_BYTES) rotate()
                file.appendText(entry.format() + "\n")
            } catch (_: Throwable) {
                // never crash the logger
            }
        }
    }

    private suspend fun rotate() = withContext(Dispatchers.IO) {
        val dir = logDir ?: return@withContext
        val current = currentLogFile ?: return@withContext
        // Shift pixelmusic.4.log -> pixelmusic.5.log ... pixelmusic.log -> pixelmusic.1.log
        for (i in MAX_ROTATED_FILES - 1 downTo 1) {
            val from = File(dir, if (i == 1) "pixelmusic.log" else "pixelmusic.$i.log")
            val to = File(dir, "pixelmusic.${i + 1}.log")
            if (from.exists()) from.renameTo(to)
        }
        val oldest = File(dir, "pixelmusic.${MAX_ROTATED_FILES}.log")
        if (oldest.exists()) oldest.delete()
        currentLogFile = File(dir, "pixelmusic.log")
    }

    fun logFilePath(): String? = currentLogFile?.absolutePath
}
