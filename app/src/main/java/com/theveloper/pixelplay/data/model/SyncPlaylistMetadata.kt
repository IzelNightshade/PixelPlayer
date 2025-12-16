package com.theveloper.pixelplay.data.model

import kotlinx.serialization.Serializable

@Serializable
data class SyncPlaylistMetadata(
    val id: String,
    val name: String,
    val songIds: List<String>,
    val lastModified: Long
)