/*
 * ArchiveTune (2026)
 * © Chartreux Westia — github.com/ianshulyadav
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */





package saurav.shru.pixelmusic.innertube.models

import kotlinx.serialization.Serializable

@Serializable
data class PlaylistPanelRenderer(
    val title: String? = null,
    val titleText: Runs? = null,
    val shortBylineText: Runs? = null,
    val contents: List<Content> = emptyList(),
    val isInfinite: Boolean? = null,
    val numItemsToShow: Int? = null,
    val playlistId: String? = null,
    val continuations: List<Continuation>? = null,
) {
    @Serializable
    data class Content(
        val playlistPanelVideoRenderer: PlaylistPanelVideoRenderer?,
        val automixPreviewVideoRenderer: AutomixPreviewVideoRenderer?,
    )
}
