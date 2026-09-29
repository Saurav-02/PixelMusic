package com.saurav.pixelmusic.data.session

/** UI state machine for the Listen Together bottom sheet. */
sealed interface ListenTogetherUiState {
    /** No active session. */
    data object Idle : ListenTogetherUiState

    /** Creating a session (host). */
    data object Creating : ListenTogetherUiState

    /** Joining a session (guest). */
    data object Joining : ListenTogetherUiState

    /**
     * Hosting a session.
     * @param code the 6-letter room code guests enter to join.
     * @param members everyone in the room, host first, with liveness.
     */
    data class Hosting(
        val code: String,
        val members: List<SessionMember> = emptyList()
    ) : ListenTogetherUiState

/**
 * A session participant. [isLive] is heartbeat-driven: true while the
 * member's last heartbeat is fresh, false when it goes stale.
 */
data class SessionMember(
    val name: String,
    val lastSeenMs: Long,
    val isLive: Boolean = true
)

    /** Guest in someone else's session. @param hostName display name of the host. */
    data class Guest(val hostName: String) : ListenTogetherUiState

    /** Something went wrong; [message] is user-facing. */
    data class Error(val message: String) : ListenTogetherUiState
}

/**
 * A host playback snapshot, mirrored through Firebase Realtime Database.
 * Serialized as plain maps — no reflection, R8-safe.
 */
data class SessionTrack(
    val videoId: String = "",
    val title: String = "",
    val artist: String = "",
    val artworkUrl: String = "",
    val isPlaying: Boolean = false,
    val positionMs: Long = 0L,
    val updatedAtMs: Long = 0L
) {
    companion object {
        @Suppress("UNCHECKED_CAST")
        fun fromMap(map: Map<String, Any?>): SessionTrack = SessionTrack(
            videoId = map["videoId"] as? String ?: "",
            title = map["title"] as? String ?: "",
            artist = map["artist"] as? String ?: "",
            artworkUrl = map["artworkUrl"] as? String ?: "",
            isPlaying = map["isPlaying"] as? Boolean ?: false,
            positionMs = (map["positionMs"] as? Number)?.toLong() ?: 0L,
            updatedAtMs = (map["updatedAtMs"] as? Number)?.toLong() ?: 0L
        )
    }

    fun toMap(): Map<String, Any?> = mapOf(
        "videoId" to videoId,
        "title" to title,
        "artist" to artist,
        "artworkUrl" to artworkUrl,
        "isPlaying" to isPlaying,
        "positionMs" to positionMs,
        "updatedAtMs" to updatedAtMs
    )
}
