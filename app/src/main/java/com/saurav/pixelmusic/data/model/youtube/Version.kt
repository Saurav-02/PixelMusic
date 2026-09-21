package com.saurav.pixelmusic.data.model.youtube

import androidx.compose.runtime.Immutable
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.saurav.pixelmusic.data.remote.youtube.Constants
import kotlinx.serialization.Serializable

@Serializable
@Immutable
@Entity(tableName = Constants.Database.VERSIONS_TABLE)
data class Version(
    @PrimaryKey
    val name: String,
) {
    val isBeta: Boolean
        get() = name.contains(".")
}
