package com.theveloper.pixelplay.data.model

import kotlinx.serialization.Serializable

@Serializable
data class SyncData(
    val songs: List<SyncSongMetadata>,
    val playlists: List<SyncPlaylistMetadata>
)