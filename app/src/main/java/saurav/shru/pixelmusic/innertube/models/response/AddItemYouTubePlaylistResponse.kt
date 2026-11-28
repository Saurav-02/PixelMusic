/*
 * ArchiveTune (2026)
 * © Chartreux Westia — github.com/ianshulyadav
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */





package saurav.shru.pixelmusic.innertube.models.response

import kotlinx.serialization.Serializable

@Serializable
data class AddItemYouTubePlaylistResponse(
    val status: String? = null,
    val playlistEditResults: List<PlaylistEditResult> = emptyList()
) {
    @Serializable
    data class PlaylistEditResult(
        val playlistEditVideoAddedResultData: PlaylistEditVideoAddedResultData? = null,
    ) {
        @Serializable
        data class PlaylistEditVideoAddedResultData(
            val setVideoId: String? = null,
            val videoId: String? = null
        )
    }
}
