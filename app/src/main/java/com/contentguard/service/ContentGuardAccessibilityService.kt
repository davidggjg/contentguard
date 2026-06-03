package com.contentguard.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import android.util.Log
import com.contentguard.ui.BlockedActivity
import com.contentguard.utils.PrefsManager

class ContentGuardAccessibilityService : AccessibilityService() {

    companion object {
        private const val TAG = "CGAccessibility"
    }

    private lateinit var prefs: PrefsManager
    private var lastBlockedPackage = ""
    private var lastBlockedTime = 0L

    override fun onServiceConnected() {
        prefs = PrefsManager(this)
        serviceInfo = serviceInfo.apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            notificationTimeout = 100
        }
        Log.d(TAG, "Accessibility Service מחובר")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return

        val packageName = event.packageName?.toString() ?: return

        // לא חוסמים את עצמנו ואת מסך הנעילה
        if (packageName == this.packageName) return
        if (packageName == "com.contentguard.debug") return
        if (packageName == "com.android.systemui") return
        if (packageName == "android") return

        // מניעת לולאה – לא חוסמים שוב תוך 2 שניות
        val now = System.currentTimeMillis()
        if (packageName == lastBlockedPackage && now - lastBlockedTime < 2000) return

        val blockedApps = prefs.getBlockedApps()
        if (blockedApps.contains(packageName)) {
            lastBlockedPackage = packageName
            lastBlockedTime = now

            Log.d(TAG, "חוסם אפליקציה: $packageName")

            // מציג את מסך החסימה
            val intent = Intent(this, BlockedActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP
                putExtra("app_name", getAppName(packageName))
            }
            startActivity(intent)
        }
    }

    private fun getAppName(packageName: String): String {
        return try {
            val pm = packageManager
            val info = pm.getApplicationInfo(packageName, 0)
            pm.getApplicationLabel(info).toString()
        } catch (e: Exception) {
            packageName
        }
    }

    override fun onInterrupt() {
        Log.d(TAG, "Accessibility Service הופסק")
    }
}
