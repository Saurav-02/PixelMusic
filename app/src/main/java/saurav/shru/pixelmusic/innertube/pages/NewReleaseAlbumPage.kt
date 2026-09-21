/*
 * ArchiveTune (2026)
 * © Chartreux Westia — github.com/ianshulyadav
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */





package saurav.shru.pixelmusic.innertube.pages

import saurav.shru.pixelmusic.innertube.models.AlbumItem
import saurav.shru.pixelmusic.innertube.models.AlbumReleaseType
import saurav.shru.pixelmusic.innertube.models.Artist
import saurav.shru.pixelmusic.innertube.models.MusicTwoRowItemRenderer
import saurav.shru.pixelmusic.innertube.models.oddElements
import saurav.shru.pixelmusic.innertube.models.splitBySeparator

object NewReleaseAlbumPage {
    fun fromMusicTwoRowItemRenderer(renderer: MusicTwoRowItemRenderer): AlbumItem? {
        val subtitleRuns = renderer.subtitle?.runs ?: return null
        val subtitleGroups = subtitleRuns.splitBySeparator()

        return AlbumItem(
            browseId = renderer.navigationEndpoint.browseEndpoint?.browseId ?: return null,
            playlistId =
                renderer.thumbnailOverlay
                    ?.musicItemThumbnailOverlayRenderer
                    ?.content
                    ?.musicPlayButtonRenderer
                    ?.playNavigationEndpoint
                    ?.watchPlaylistEndpoint
                    ?.playlistId ?: return null,
            title =
                renderer.title.runs
                    ?.firstOrNull()
                    ?.text ?: return null,
            artists =
                subtitleGroups.getOrNull(1)?.oddElements()?.map {
                    Artist(
                        name = it.text,
                        id = it.navigationEndpoint?.browseEndpoint?.browseId,
                    )
                } ?: return null,
            year =
                subtitleRuns
                    .lastOrNull()
                    ?.text
                    ?.toIntOrNull(),
            releaseType = AlbumReleaseType.fromLabel(
                subtitleGroups.firstOrNull()?.joinToString(separator = "") { it.text }
            ),
            thumbnail = renderer.thumbnailRenderer.musicThumbnailRenderer?.getThumbnailUrl() ?: return null,
            explicit =
                renderer.subtitleBadges?.find {
                    it.musicInlineBadgeRenderer?.icon?.iconType == "MUSIC_EXPLICIT_BADGE"
                } != null,
        )
    }
}
