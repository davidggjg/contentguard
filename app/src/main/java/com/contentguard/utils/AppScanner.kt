package com.contentguard.utils

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager

/**
 * סורק את כל האפליקציות המותקנות על המכשיר.
 */
object AppScanner {

    // אפליקציות מערכת שלא רוצים להציג
    private val SYSTEM_PACKAGES = setOf(
        "com.android.settings",
        "com.android.systemui",
        "android",
        "com.google.android.gms",
        "com.android.phone"
    )

    fun getInstalledApps(context: Context): List<Pair<String, String>> {
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        return pm.queryIntentActivities(intent, 0)
            .filter { it.activityInfo.packageName !in SYSTEM_PACKAGES }
            .map { resolveInfo ->
                val packageName = resolveInfo.activityInfo.packageName
                val appName = resolveInfo.loadLabel(pm).toString()
                Pair(packageName, appName)
            }
            .sortedBy { it.second }
    }
}
