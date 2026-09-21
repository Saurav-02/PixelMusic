package com.saurav.pixelmusic.data.remote.lyrics_providers.util

import java.util.Locale

fun Int.toLrcTimestamp(): String {
    val totalSeconds = this / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    val millis = this % 1000
    return String.format(Locale.US, "%02d:%02d.%03d", minutes, seconds, millis)
}

