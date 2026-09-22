package com.saurav.pixelmusic

import com.saurav.pixelmusic.utils.PixelLogger
import timber.log.Timber

/**
 * A unified Timber Tree that routes all Timber log calls into PixelLogger and Logcat.
 * Controlled by the universal logging toggle (PixelLogger.isEnabled()).
 * When logging is turned OFF: suppresses ALL logs (zero output, zero overhead).
 * When logging is turned ON: captures ALL log types (VERBOSE, DEBUG, INFO, WARN, ERROR, ASSERT).
 */
class ReleaseTree : Timber.Tree() {

    override fun isLoggable(tag: String?, priority: Int): Boolean {
        // Universal toggle: when off, absolutely zero logs are emitted
        return PixelLogger.isEnabled()
    }

    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        if (!PixelLogger.isEnabled()) return
        PixelLogger.log(priority, tag, message, t)
    }
}
