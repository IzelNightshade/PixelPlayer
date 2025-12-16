package com.theveloper.pixelplay.data.model

import kotlinx.serialization.Serializable

@Serializable
data class SyncSongMetadata(
    val id: String,
    val title: String,
    val artist: String,
    val fileHash: String,
    val lastModified: Long
)