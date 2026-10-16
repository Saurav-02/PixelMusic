/*
 * ArchiveTune (2026)
 * © Chartreux Westia — github.com/ianshulyadav
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */





package saurav.shru.pixelmusic.innertube.pages

import saurav.shru.pixelmusic.innertube.models.MusicResponsiveListItemRenderer.FlexColumn
import saurav.shru.pixelmusic.innertube.models.Run

object PageHelper {
    fun extractRuns(columns: List<FlexColumn>, typeLike: String): List<Run> {
        val filteredRuns = mutableListOf<Run>()
        for (column in columns) {
            val runs = column.musicResponsiveListItemFlexColumnRenderer.text?.runs
                ?: continue

            for (run in runs) {
                val typeStr = run.navigationEndpoint?.watchEndpoint?.watchEndpointMusicSupportedConfigs?.watchEndpointMusicConfig?.musicVideoType
                    ?: run.navigationEndpoint?.browseEndpoint?.browseEndpointContextSupportedConfigs?.browseEndpointContextMusicConfig?.pageType
                    ?: continue

                if (typeLike in typeStr) {
                    filteredRuns.add(run)
                }
            }
        }
        return filteredRuns
    }
}