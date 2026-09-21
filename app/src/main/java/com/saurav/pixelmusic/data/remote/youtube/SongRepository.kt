package com.saurav.pixelmusic.data.remote.youtube

import com.saurav.pixelmusic.data.model.youtube.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

class SongRepository {
    private val songDataSource = SongDataSource()

    fun search(query: String): Flow<ApiResult<List<Song>>> {
        return flow {
            try {
                emit(ApiResult.Loading)
                emit(ApiResult.Success(songDataSource.search(query)))
            } catch (e: Exception) {
                emit(ApiResult.Error(e))
            }
        }.flowOn(Dispatchers.IO)
    }

    fun getSongInfo(songId: String): Flow<ApiResult<Song>> {
        return flow {
            try {
                emit(ApiResult.Loading)
                emit(ApiResult.Success(songDataSource.getSongInfo(songId)))
            } catch (e: Exception) {
                emit(ApiResult.Error(e))
            }
        }.flowOn(Dispatchers.IO)
    }

    fun getRelatedSongs(videoId: String): Flow<ApiResult<List<Song>>> {
        return flow {
            try {
                emit(ApiResult.Loading)
                emit(ApiResult.Success(songDataSource.getRelatedSongs(videoId)))
            } catch (e: Exception) {
                emit(ApiResult.Error(e))
            }
        }.flowOn(Dispatchers.IO)
    }
}
