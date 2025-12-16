package com.theveloper.pixelplay.services

import android.content.Context
import com.theveloper.pixelplay.data.database.MusicDao
import com.theveloper.pixelplay.data.model.SyncData
import com.theveloper.pixelplay.data.model.SyncPlaylistMetadata
import com.theveloper.pixelplay.utils.FileUtils
import fi.iki.elonen.NanoHTTPD
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

class PixelPlayHttpServer(
    private val context: Context,
    private val musicDao: MusicDao,
    port: Int = 8080
) : NanoHTTPD(port) {

    private var syncDataJson: String = "{}"
    private val fileMap = mutableMapOf<String, String>() // id to path

    fun updateSyncData(syncData: SyncData, paths: Map<String, String>) {
        syncDataJson = Json.encodeToString(syncData)
        fileMap.clear()
        fileMap.putAll(paths)
    }

    override fun serve(session: IHTTPSession): Response {
        return when (session.uri) {
            "/metadata" -> handleMetadataRequest()
            else -> if (session.uri.startsWith("/file/")) {
                handleFileRequest(session.uri.substring(6))
            } else {
                newFixedLengthResponse(Response.Status.NOT_FOUND, MIME_PLAINTEXT, "Not Found")
            }
        }
    }

    private fun handleMetadataRequest(): Response {
        return newFixedLengthResponse(Response.Status.OK, "application/json", syncDataJson)
    }

    private fun handleFileRequest(songId: String): Response {
        val path = fileMap[songId] ?: return newFixedLengthResponse(Response.Status.NOT_FOUND, MIME_PLAINTEXT, "File not found")
        val file = File(path)
        if (!file.exists()) return newFixedLengthResponse(Response.Status.NOT_FOUND, MIME_PLAINTEXT, "File not found")
        return newFixedLengthResponse(Response.Status.OK, "application/octet-stream", file.inputStream(), file.length())
    }
}