package com.saurav.pixelmusic.utils

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
        val level: Char,           // V, D, I, W, E, A
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

    private const val MAX_BUFFER = 5000
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

        val dir = File(context.filesDir, "logs").apply { mkdirs() }
        logDir = dir
        currentLogFile = File(dir, "pixelmusic.log")
        ioScope.launch { drainToFile() }
    }

    fun isEnabled(): Boolean = _enabled.value

    fun setEnabled(value: Boolean) {
        val previous = _enabled.value
        _enabled.value = value
        if (previous != value) {
            if (value) {
                Log.i("PM-BOOT", "Universal Logging Enabled (All log levels active)")
                i(Category.MISC, "Logger", "Universal logging system enabled")
            } else {
                Log.i("PM-BOOT", "Universal Logging Disabled (All logs silenced)")
            }
        }
    }

    fun clear() {
        synchronized(buffer) {
            buffer.clear()
            _entries.value = emptyList()
        }
        ioScope.launch {
            currentLogFile?.delete()
            currentLogFile = logDir?.let { File(it, "pixelmusic.log") }
        }
    }

    fun snapshot(): List<Entry> = synchronized(buffer) { buffer.toList() }

    fun exportText(): String = buildString {
        snapshot().forEach { appendLine(it.format()) }
    }

    // ---- Public logging API ----

    fun v(category: Category, tag: String, message: String, throwable: Throwable? = null) =
        log('V', category, tag, message, throwable)

    fun d(category: Category, tag: String, message: String, throwable: Throwable? = null) =
        log('D', category, tag, message, throwable)

    fun i(category: Category, tag: String, message: String, throwable: Throwable? = null) =
        log('I', category, tag, message, throwable)

    fun w(category: Category, tag: String, message: String, throwable: Throwable? = null) =
        log('W', category, tag, message, throwable)

    fun e(category: Category, tag: String, message: String, throwable: Throwable? = null) =
        log('E', category, tag, message, throwable)

    fun wtf(category: Category, tag: String, message: String, throwable: Throwable? = null) =
        log('A', category, tag, message, throwable)

    fun log(priority: Int, tag: String?, message: String, throwable: Throwable? = null) {
        if (!isEnabled()) return
        val level = when (priority) {
            Log.VERBOSE -> 'V'
            Log.DEBUG -> 'D'
            Log.INFO -> 'I'
            Log.WARN -> 'W'
            Log.ERROR -> 'E'
            Log.ASSERT -> 'A'
            else -> 'D'
        }
        val category = inferCategory(tag)
        log(level, category, tag ?: "PixelMusic", message, throwable)
    }

    fun inferCategory(tag: String?): Category {
        if (tag == null) return Category.MISC
        val lower = tag.lowercase()
        return when {
            lower.contains("net") || lower.contains("http") || lower.contains("api") || lower.contains("yt") || lower.contains("down") -> Category.NETWORK
            lower.contains("play") || lower.contains("exo") || lower.contains("audio") || lower.contains("service") || lower.contains("dual") -> Category.PLAYER
            lower.contains("queue") -> Category.QUEUE
            lower.contains("lyric") -> Category.LYRICS
            lower.contains("auth") || lower.contains("login") || lower.contains("token") -> Category.AUTH
            lower.contains("sync") || lower.contains("work") -> Category.SYNC
            lower.contains("db") || lower.contains("room") || lower.contains("dao") || lower.contains("repo") -> Category.DB
            lower.contains("life") || lower.contains("activity") || lower.contains("app") -> Category.LIFE
            lower.contains("ui") || lower.contains("screen") || lower.contains("view") || lower.contains("compose") || lower.contains("sheet") -> Category.UI
            else -> Category.MISC
        }
    }

    private fun log(
        level: Char,
        category: Category,
        tag: String,
        message: String,
        throwable: Throwable?,
    ) {
        if (!_enabled.value) return

        val entry = Entry(
            timestampMs = System.currentTimeMillis(),
            level = level,
            category = category,
            tag = tag,
            message = message,
            throwable = throwable,
        )

        val logTag = "PM-${category.shortName}/${tag.take(12)}"
        when (level) {
            'V' -> Log.v(logTag, message, throwable)
            'D' -> Log.d(logTag, message, throwable)
            'I' -> Log.i(logTag, message, throwable)
            'W' -> Log.w(logTag, message, throwable)
            'E' -> Log.e(logTag, message, throwable)
            'A' -> Log.wtf(logTag, message, throwable)
        }

        synchronized(buffer) {
            if (buffer.size >= MAX_BUFFER) buffer.removeFirst()
            buffer.addLast(entry)
            _entries.value = buffer.toList()
        }

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
        for (i in MAX_ROTATED_FILES - 1 downTo 1) {
            val from = File(dir, if (i == 1) "pixelmusic.log" else "pixelmusic.$i.log")
            val to = File(dir, if (i == 1) "pixelmusic.2.log" else "pixelmusic.${i + 1}.log")
            if (from.exists()) from.renameTo(to)
        }
        val oldest = File(dir, "pixelmusic.${MAX_ROTATED_FILES}.log")
        if (oldest.exists()) oldest.delete()
        currentLogFile = File(dir, "pixelmusic.log")
    }

    fun logFilePath(): String? = currentLogFile?.absolutePath
}
