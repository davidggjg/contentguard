package com.contentguard.utils

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build

object AppScanner {

    private val SKIP_PACKAGES = setOf(
        "android",
        "com.android.systemui",
        "com.android.shell",
        "com.android.providers.settings",
        "com.android.providers.contacts",
        "com.android.providers.media",
        "com.android.providers.telephony",
        "com.google.android.gms",
        "com.google.android.gsf",
    )

    fun getInstalledApps(context: Context): List<Pair<String, String>> {
        val pm = context.packageManager

        // אנדרואיד 13+ דורש flags מיוחד
        val apps = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.getInstalledApplications(PackageManager.ApplicationInfoFlags.of(0L))
        } else {
            pm.getInstalledApplications(0)
        }

        return apps
            .filter { app ->
                val notSkipped = app.packageName !in SKIP_PACKAGES
                val notSelf = app.packageName != context.packageName
                // כולל אפליקציות משתמש + אפליקציות מערכת שיש להן launcher
                val isUserApp = (app.flags and ApplicationInfo.FLAG_SYSTEM) == 0
                val hasLauncher = pm.getLaunchIntentForPackage(app.packageName) != null
                notSkipped && notSelf && (isUserApp || hasLauncher)
            }
            .map { app ->
                val name = try {
                    pm.getApplicationLabel(app).toString()
                } catch (e: Exception) {
                    app.packageName
                }
                Pair(app.packageName, name)
            }
            .sortedBy { it.second }
    }
}
