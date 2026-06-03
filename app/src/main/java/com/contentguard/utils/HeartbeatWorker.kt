package com.contentguard.utils

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class HeartbeatWorker(context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val prefs = PrefsManager(applicationContext)
        val deviceId = prefs.getDeviceId() ?: return@withContext Result.failure()

        // שולח heartbeat
        ApiManager.sendHeartbeat(deviceId)

        // מושך הגדרות עדכניות – כולל אפליקציות חסומות
        val settings = ApiManager.fetchSettings(deviceId)
        if (settings != null) {
            prefs.setBlockedDomains(settings.blockedDomains)
            prefs.setBlockLevel(settings.blockLevel)
            prefs.setBlockedApps(settings.blockedApps) // ← חדש!
        }

        // שולח רשימת אפליקציות מותקנות
        val apps = AppScanner.getInstalledApps(applicationContext)
        ApiManager.sendInstalledApps(deviceId, apps)

        Result.success()
    }
    }
