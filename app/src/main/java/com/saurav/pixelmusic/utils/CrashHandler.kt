package com.saurav.pixelmusic.utils

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log
import kotlin.system.exitProcess
import com.saurav.pixelmusic.BuildConfig

/**
 * Dummy data class to prevent compilation errors if other files (like MainActivity) 
 * still reference the old crash log system.
 */
data class CrashLogData(
    val timestamp: Long,
    val formattedDate: String,
    val exceptionMessage: String,
    val stackTrace: String
) {
    fun getFullLog(): String = ""
}

/**
 * Automated Telegram/Email Crash Reporter.
 * Intercepts fatal crashes and instantly fires an intent to send the log.
 */
object CrashHandler : Thread.UncaughtExceptionHandler {

    private lateinit var appContext: Context
    private var defaultHandler: Thread.UncaughtExceptionHandler? = null
    private val isCrashing = java.util.concurrent.atomic.AtomicBoolean(false)

    // Your credentials
    private const val DEV_EMAIL = "vita47177@gmail.com"
    private const val TG_USERNAME = "Saurav124x"

    /**
     * Installs this crash handler.
     * Keep this exactly as is so your PixelMusicApplication.kt doesn't break!
     */
    fun install(context: Context) {
        val current = Thread.getDefaultUncaughtExceptionHandler()
        if (current is CrashHandler) return
        appContext = context.applicationContext
        defaultHandler = current
        Thread.setDefaultUncaughtExceptionHandler(this)
    }

    override fun uncaughtException(thread: Thread, throwable: Throwable) {
        if (!isCrashing.compareAndSet(false, true)) {
            defaultHandler?.uncaughtException(thread, throwable)
            return
        }

        runCatching {
            val stackTrace = Log.getStackTraceString(throwable)
            val crashReport = """
                🚨 PixelMusic Crash Report 🚨
                
                Device: ${Build.MANUFACTURER} ${Build.MODEL}
                Android Version: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})
                App Version: ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})
                
                What went wrong:
                ${throwable.message}
                
                Stacktrace:
                $stackTrace
            """.trimIndent()

            val crashFile = java.io.File(appContext.cacheDir, "last_crash.log")
            crashFile.writeText(crashReport)

            val truncatedReport = if (crashReport.length > 2000) crashReport.take(2000) + "\n...[truncated]" else crashReport
            val telegramUri = Uri.parse("tg://resolve?domain=$TG_USERNAME&text=${Uri.encode(truncatedReport)}")
            val telegramIntent = Intent(Intent.ACTION_VIEW, telegramUri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            }

            try {
                appContext.startActivity(telegramIntent)
            } catch (e: ActivityNotFoundException) {
                val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
                    data = Uri.parse("mailto:")
                    putExtra(Intent.EXTRA_EMAIL, arrayOf(DEV_EMAIL))
                    putExtra(Intent.EXTRA_SUBJECT, "PixelMusic Automated Crash Report")
                    putExtra(Intent.EXTRA_TEXT, truncatedReport)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                }
                try {
                    appContext.startActivity(emailIntent)
                } catch (ex: Exception) {
                    Log.e("CrashHandler", "No apps found to handle crash report.", ex)
                }
            }
        }

        defaultHandler?.uncaughtException(thread, throwable)
    }

    // --- DUMMY METHODS TO PREVENT COMPILER ERRORS ---
    // These ensure that if MainActivity is still checking for old crashes, 
    // it won't crash the compiler, but will always think there are no old logs.
    fun hasCrashLog(): Boolean = false
    fun getCrashLog(): CrashLogData? = null
    fun clearCrashLog() {}
}
