package com.theveloper.pixelplay.services

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.delay

@HiltWorker
class LanSyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        // Start the LAN discovery service
        val intent = android.content.Intent(applicationContext, LANDiscoveryService::class.java)
        applicationContext.startService(intent)
        // Keep running for some time, or just trigger
        delay(60000) // Run for 1 minute
        applicationContext.stopService(intent)
        return Result.success()
    }
}