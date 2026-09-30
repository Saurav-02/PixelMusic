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
     * @param hostName display name of the host (the local user).
     * @param members everyone in the room, host first, with liveness.
     */
    data class Hosting(
        val code: String,
        val hostName: String,
        val members: List<SessionMember> = emptyList()
    ) : ListenTogetherUiState

    /**
     * Guest in someone else's session.
     * @param hostName display name of the host.
     * @param code the 6-letter room code, shown so guests can share it too.
     * @param members everyone in the room, host first, with liveness.
     */
    data class Guest(
        val hostName: String,
        val code: String,
        val members: List<SessionMember> = emptyList()
    ) : ListenTogetherUiState

    /** Something went wrong; [message] is user-facing. */
    data class Error(val message: String) : ListenTogetherUiState
}

/**
 * A session participant. [isLive] is heartbeat-driven: true while the
 * member's last heartbeat is fresh, false when it goes stale.
 */
data class SessionMember(
    val name: String,
    val lastSeenMs: Long,
    val isLive: Boolean = true,
    /** True for entries written by older app versions (plain name, no heartbeat). */
    val isLegacy: Boolean = false,
    val photoUrl: String? = null,
    /** Sync protocol version (2 = understands coordinated buffering). */
    val proto: Int = 0
)

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
    val updatedAtMs: Long = 0L,
    /**
     * Monotonic revision, bumped by the host on every publish. Guests
     * ignore snapshots that do not advance it (stale/reordered writes).
     */
    val revision: Long = 0L,
    /**
     * True while the host runs a coordinated-buffering round: guests
     * should preload this track, hold it paused and report ready, then
     * start together when a snapshot arrives with buffering=false.
     */
    val buffering: Boolean = false
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
            updatedAtMs = (map["updatedAtMs"] as? Number)?.toLong() ?: 0L,
            revision = (map["revision"] as? Number)?.toLong() ?: 0L,
            buffering = map["buffering"] as? Boolean ?: false
        )
    }

    fun toMap(): Map<String, Any?> = mapOf(
        "videoId" to videoId,
        "title" to title,
        "artist" to artist,
        "artworkUrl" to artworkUrl,
        "isPlaying" to isPlaying,
        "positionMs" to positionMs,
        "updatedAtMs" to updatedAtMs,
        "revision" to revision,
        "buffering" to buffering
    )
}

/** An emoji reaction someone sent to the room. */
data class ReactionEvent(
    val key: String,
    val emoji: String,
    val from: String,
    val ts: Long,
    val isLoved: Boolean = false
)

/** A preset message someone sent to the room. */
data class ChatMessage(
    val key: String,
    val text: String,
    val from: String,
    val ts: Long
)
