package com.saurav.pixelmusic.data.remote.lyrics_providers.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class SongInfo(
    var songName: String?,
    var artistName: String? = null,
    var songLink: String? = null,
    var albumCoverLink: String? = null,
    var lrcLibID: Int? = null, 
    var qqPayload: String? = null, 
    var neteaseID: Long? = null, 
    var appleID: Long? = null, 
    var musixmatchID: Long? = null, 
    var hasSyncedLyrics: Boolean? = null, 
    var hasUnsyncedLyrics: Boolean? = null, 
    var syncedLyrics: String? = null, 
    var unsyncedLyrics: String? = null, 
    var availableLanguages: List<String> = emptyList(), 
    var originalLanguage: String? = null, 
    var currentLanguage: String? = null, 
) : Parcelable

