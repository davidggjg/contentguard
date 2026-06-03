package com.contentguard.utils

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager

object AppScanner {

    // אפליקציות מערכת שלא צריך להציג
    private val SKIP_PACKAGES = setOf(
        "android",
        "com.android.systemui",
        "com.android.settings",
        "com.android.phone",
        "com.android.shell",
        "com.android.providers.settings",
        "com.android.providers.contacts",
        "com.android.providers.media",
        "com.android.providers.telephony",
        "com.google.android.gms",
        "com.google.android.gsf",
        "com.samsung.android.providers.context",
    )

    fun getInstalledApps(context: Context): List<Pair<String, String>> {
        val pm = context.packageManager

        return pm.getInstalledApplications(PackageManager.GET_META_DATA)
            .filter { app ->
                // כולל אפליקציות משתמש + אפליקציות מערכת שיש להן ממשק
                val isUserApp = (app.flags and ApplicationInfo.FLAG_SYSTEM) == 0
                val hasLauncher = pm.getLaunchIntentForPackage(app.packageName) != null
                val notSkipped = app.packageName !in SKIP_PACKAGES
                val notSelf = app.packageName != context.packageName

                (isUserApp || hasLauncher) && notSkipped && notSelf
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
