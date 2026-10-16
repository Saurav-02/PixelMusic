/*
 * ArchiveTune (2026)
 * © Chartreux Westia — github.com/ianshulyadav
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */





package saurav.shru.pixelmusic.innertube.models.body

import saurav.shru.pixelmusic.innertube.models.Context
import kotlinx.serialization.Serializable

@Serializable
data class EditPlaylistBody(
    val context: Context,
    val playlistId: String,
    val actions: List<Action>
)

@Serializable
data class Action(
    val action: String,
    val addedVideoId: String? = null,
    val addedFullListId: String? = null,
    val setVideoId: String? = null,
    val movedSetVideoIdSuccessor: String? = null,
    val removedVideoId: String? = null,
    val playlistName: String? = null
)
