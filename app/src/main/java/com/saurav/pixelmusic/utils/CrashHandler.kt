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

    // Your credentials
    private const val DEV_EMAIL = "vita47177@gmail.com"
    private const val TG_USERNAME = "Saurav124x"

    /**
     * Installs this crash handler.
     * Keep this exactly as is so your PixelMusicApplication.kt doesn't break!
     */
    fun install(context: Context) {
        appContext = context.applicationContext
        defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler(this)
    }

    override fun uncaughtException(thread: Thread, throwable: Throwable) {
        // 1. Extract the exact reason for the crash
        val stackTrace = Log.getStackTraceString(throwable)
        
        // 2. Build the pre-filled report
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

        // 3. Attempt to launch Telegram directly to your username
        val telegramUri = Uri.parse("tg://resolve?domain=$TG_USERNAME&text=${Uri.encode(crashReport)}")
        val telegramIntent = Intent(Intent.ACTION_VIEW, telegramUri).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }

        try {
            appContext.startActivity(telegramIntent)
        } catch (e: ActivityNotFoundException) {
            // 4. Fallback to Gmail/Email if Telegram is not installed
            val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:") // Blank mailto forces only email apps to open
                putExtra(Intent.EXTRA_EMAIL, arrayOf(DEV_EMAIL)) // Pass the email here instead
                putExtra(Intent.EXTRA_SUBJECT, "PixelMusic Automated Crash Report")
                putExtra(Intent.EXTRA_TEXT, crashReport)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            }
            try {
                appContext.startActivity(emailIntent)
            } catch (ex: ActivityNotFoundException) {
                // Failsafe if absolutely no messaging apps are installed
                Log.e("CrashHandler", "No apps found to handle crash report.")
            }
        }

        // 5. Let the system kill the dead app process cleanly
        defaultHandler?.uncaughtException(thread, throwable)
        exitProcess(1)
    }

    // --- DUMMY METHODS TO PREVENT COMPILER ERRORS ---
    // These ensure that if MainActivity is still checking for old crashes, 
    // it won't crash the compiler, but will always think there are no old logs.
    fun hasCrashLog(): Boolean = false
    fun getCrashLog(): CrashLogData? = null
    fun clearCrashLog() {}
}
