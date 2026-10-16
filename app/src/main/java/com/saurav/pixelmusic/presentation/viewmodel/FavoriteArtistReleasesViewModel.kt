package com.saurav.pixelmusic.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.saurav.pixelmusic.data.database.MusicDao
import com.saurav.pixelmusic.data.remote.youtube.toNativeSong
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import timber.log.Timber
import saurav.shru.pixelmusic.innertube.YouTube as InnerTubeYouTube
import saurav.shru.pixelmusic.innertube.models.YTItem
import javax.inject.Inject

@HiltViewModel
class FavoriteArtistReleasesViewModel @Inject constructor(
    private val musicDao: MusicDao
) : ViewModel() {

    private val _releases = MutableStateFlow<List<YTItem>>(emptyList())
    val releases: StateFlow<List<YTItem>> = _releases.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        loadReleases()
    }

    fun loadReleases() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val browseIds = withContext(Dispatchers.IO) {
                    val favJson = musicDao.getFavoriteSongsArtistsJson()
                    val mostPlayedJson = musicDao.getMostPlayedYoutubeArtistsJson()
                    val combinedJson = favJson + mostPlayedJson
                    
                    val ids = mutableListOf<String>()
                    combinedJson.forEach { json ->
                        try {
                            val arr = JSONArray(json)
                            for (i in 0 until arr.length()) {
                                val obj = arr.getJSONObject(i)
                                val id = obj.optString("id")
                                if (!id.isNullOrBlank() && id.startsWith("UC")) {
                                    ids.add(id)
                                }
                            }
                        } catch (e: Exception) {
                            Timber.e(e, "Error parsing artists_json in FavoriteArtistReleasesViewModel")
                        }
                    }
                    
                    // Count occurrences and get top 3 unique browseIds
                    ids.groupingBy { it }
                        .eachCount()
                        .entries
                        .sortedByDescending { it.value }
                        .map { it.key }
                        .take(3)
                }

                // Default popular artists fallback if user has no favorited/most played artists yet
                val effectiveBrowseIds = if (browseIds.isEmpty()) {
                    listOf(
                        "UCq-Fj5jknLsUf-MWSy4_brA", // Coldplay
                        "UCYvmuw-JdgEUTgZdGMs058A", // Taylor Swift
                        "UC0C-w0Yj5F07rYgUx50xHyw"  // The Weeknd
                    )
                } else {
                    browseIds
                }

                val allReleases = mutableListOf<YTItem>()
                withContext(Dispatchers.IO) {
                    effectiveBrowseIds.forEach { browseId ->
                        InnerTubeYouTube.artist(browseId).onSuccess { artistPage ->
                            artistPage.sections.forEach { section ->
                                if (section.title.contains("Albums", ignoreCase = true) ||
                                    section.title.contains("Singles", ignoreCase = true) ||
                                    section.title.contains("Latest release", ignoreCase = true) ||
                                    section.title.contains("Releases", ignoreCase = true)) {
                                    allReleases.addAll(section.items)
                                }
                            }
                        }.onFailure { e ->
                            Timber.e(e, "Failed to load releases for artist: $browseId")
                        }
                    }
                }

                // Remove duplicates and limit to 15 items
                _releases.value = allReleases.distinctBy { it.id }.take(15)
            } catch (e: Exception) {
                Timber.e(e, "Error in loadReleases")
            } finally {
                _isLoading.value = false
            }
        }
    }
}
