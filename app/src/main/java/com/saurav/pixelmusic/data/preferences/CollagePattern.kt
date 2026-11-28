package com.saurav.pixelmusic.data.preferences

enum class CollagePattern(
    val storageKey: String,
    val label: String
) {
    FLUID_BLOB("fluid_blob", "Fluid Blob Overlay");

    companion object {
        val default: CollagePattern = FLUID_BLOB

        fun fromStorageKey(value: String?): CollagePattern = default
    }
}
