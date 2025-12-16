package com.theveloper.pixelplay.services

import android.app.Service
import android.content.Context
import android.content.Intent
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.os.IBinder
import android.util.Log
import com.theveloper.pixelplay.data.model.SyncData
import com.theveloper.pixelplay.data.model.SyncPlaylistMetadata
import com.theveloper.pixelplay.data.preferences.UserPreferencesRepository
import com.theveloper.pixelplay.utils.FileUtils
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.io.File
import javax.inject.Inject

@AndroidEntryPoint
class LANDiscoveryService : Service() {

    @Inject
    lateinit var musicDao: MusicDao

    @Inject
    lateinit var userPreferencesRepository: UserPreferencesRepository

    private lateinit var nsdManager: NsdManager
    private lateinit var httpServer: PixelPlayHttpServer
    private val client = OkHttpClient()
    private val scope = CoroutineScope(Dispatchers.IO)
    private var localSyncData = SyncData(emptyList(), emptyList())

    private val SERVICE_TYPE = "_pixelplayer._tcp"
    private val SERVICE_NAME = "PixelPlayer_${android.os.Build.MODEL}"

    override fun onCreate() {
        super.onCreate()
        nsdManager = getSystemService(Context.NSD_SERVICE) as NsdManager
        httpServer = PixelPlayHttpServer(this, musicDao)
        try {
            httpServer.start()
            registerService()
            discoverServices()
            scope.launch {
                updateLocalSyncData()
            }
        } catch (e: IOException) {
            Log.e("LANDiscoveryService", "Failed to start server", e)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        nsdManager.stopServiceDiscovery(discoveryListener)
        nsdManager.unregisterService(registrationListener)
        httpServer.stop()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun registerService() {
        val serviceInfo = NsdServiceInfo().apply {
            serviceName = SERVICE_NAME
            serviceType = SERVICE_TYPE
            port = httpServer.listeningPort
        }
        nsdManager.registerService(serviceInfo, NsdManager.PROTOCOL_DNS_SD, registrationListener)
    }

    private val registrationListener = object : NsdManager.RegistrationListener {
        override fun onServiceRegistered(serviceInfo: NsdServiceInfo) {
            Log.d("LANDiscoveryService", "Service registered: ${serviceInfo.serviceName}")
        }

        override fun onRegistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
            Log.e("LANDiscoveryService", "Registration failed: $errorCode")
        }

        override fun onServiceUnregistered(serviceInfo: NsdServiceInfo) {}
        override fun onUnregistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {}
    }

    private fun discoverServices() {
        nsdManager.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, discoveryListener)
    }

    private val discoveryListener = object : NsdManager.DiscoveryListener {
        override fun onDiscoveryStarted(regType: String) {
            Log.d("LANDiscoveryService", "Discovery started")
        }

        override fun onServiceFound(serviceInfo: NsdServiceInfo) {
            if (serviceInfo.serviceName != SERVICE_NAME) {
                nsdManager.resolveService(serviceInfo, resolveListener)
            }
        }

        override fun onServiceLost(serviceInfo: NsdServiceInfo) {}
        override fun onDiscoveryStopped(serviceType: String) {}
        override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {}
        override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {}
    }

    private val resolveListener = object : NsdManager.ResolveListener {
        override fun onServiceResolved(serviceInfo: NsdServiceInfo) {
            Log.d("LANDiscoveryService", "Service resolved: ${serviceInfo.serviceName} at ${serviceInfo.host}:${serviceInfo.port}")
            scope.launch {
                fetchAndSyncMetadata(serviceInfo)
            }
        }

        override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
            Log.e("LANDiscoveryService", "Resolve failed: $errorCode")
        }
    }

    private suspend fun updateLocalSyncData() {
        val songs = musicDao.getSongs(emptyList(), false).first()
        val songMetadata = mutableListOf<SyncSongMetadata>()
        val paths = mutableMapOf<String, String>()
        for (song in songs) {
            val file = File(song.path)
            if (file.exists()) {
                val hash = FileUtils.computeSHA256(file)
                val lastMod = FileUtils.getLastModified(file)
                songMetadata.add(SyncSongMetadata(
                    id = song.id.toString(),
                    title = song.title,
                    artist = song.artistName,
                    fileHash = hash,
                    lastModified = lastMod
                ))
                paths[song.id.toString()] = song.path
            }
        }
        val playlists = userPreferencesRepository.userPlaylistsFlow.first()
        val playlistMetadata = playlists.map { playlist ->
            SyncPlaylistMetadata(
                id = playlist.id,
                name = playlist.name,
                songIds = playlist.songIds,
                lastModified = playlist.lastModified
            )
        }
        val syncData = SyncData(songMetadata, playlistMetadata)
        httpServer.updateSyncData(syncData, paths)
        localSyncData = syncData
    }

    private suspend fun fetchAndSyncMetadata(serviceInfo: NsdServiceInfo) {
        val url = "http://${serviceInfo.host.hostAddress}:${serviceInfo.port}/metadata"
        val request = Request.Builder().url(url).build()
        try {
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val json = response.body?.string() ?: return
                    val remoteSyncData = Json.decodeFromString<SyncData>(json)
                    syncSongs(serviceInfo, remoteSyncData.songs)
                    syncPlaylists(remoteSyncData.playlists)
                    Log.d("LANDiscoveryService", "Synced ${remoteSyncData.songs.size} songs and ${remoteSyncData.playlists.size} playlists")
                }
            }
        } catch (e: Exception) {
            Log.e("LANDiscoveryService", "Failed to fetch metadata", e)
        }
    }

    private suspend fun syncSongs(serviceInfo: NsdServiceInfo, remoteSongs: List<SyncSongMetadata>) {
        val localHashes = localSyncData.songs.map { it.fileHash }.toSet()
        val missing = remoteSongs.filter { it.fileHash !in localHashes }
        for (meta in missing) {
            downloadFile(serviceInfo, meta)
        }
    }

    private suspend fun syncPlaylists(remotePlaylists: List<SyncPlaylistMetadata>) {
        val currentPlaylists = userPreferencesRepository.userPlaylistsFlow.first().associateBy { it.id }.toMutableMap()
        for (remote in remotePlaylists) {
            val local = currentPlaylists[remote.id]
            if (local == null || remote.lastModified > local.lastModified) {
                // Update or add
                val playlist = Playlist(
                    id = remote.id,
                    name = remote.name,
                    songIds = remote.songIds,
                    lastModified = remote.lastModified
                )
                userPreferencesRepository.savePlaylist(playlist)
            }
        }
    }

    private suspend fun downloadFile(serviceInfo: NsdServiceInfo, meta: SyncSongMetadata) {
        val url = "http://${serviceInfo.host.hostAddress}:${serviceInfo.port}/file/${meta.id}"
        val request = Request.Builder().url(url).build()
        try {
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body ?: return
                    // Save to a sync directory
                    val syncDir = File(getExternalFilesDir(null), "sync")
                    syncDir.mkdirs()
                    val file = File(syncDir, "${meta.fileHash}.mp3") // assume mp3
                    file.outputStream().use { out ->
                        body.byteStream().copyTo(out)
                    }
                    Log.d("LANDiscoveryService", "Downloaded ${meta.title}")
                    // TODO: Add to DB
                }
            }
        } catch (e: Exception) {
            Log.e("LANDiscoveryService", "Failed to download ${meta.title}", e)
        }
    }
}