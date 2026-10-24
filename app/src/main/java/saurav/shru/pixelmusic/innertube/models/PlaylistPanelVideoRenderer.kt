/*
 * ArchiveTune (2026)
 * © Chartreux Westia — github.com/ianshulyadav
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */





package saurav.shru.pixelmusic.innertube.models

import kotlinx.serialization.Serializable

@Serializable
data class PlaylistPanelVideoRenderer(
    val title: Runs? = null,
    val lengthText: Runs? = null,
    val longBylineText: Runs? = null,
    val shortBylineText: Runs? = null,
    val badges: List<Badges>? = null,
    val videoId: String? = null,
    val playlistSetVideoId: String? = null,
    val selected: Boolean = false,
    val thumbnail: Thumbnails? = null,
    val unplayableText: Runs? = null,
    val menu: Menu? = null,
    val navigationEndpoint: NavigationEndpoint? = null,
)
