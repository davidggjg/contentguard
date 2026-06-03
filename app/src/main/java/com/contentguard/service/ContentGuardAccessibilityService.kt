package com.contentguard.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import android.util.Log
import com.contentguard.utils.PrefsManager

/**
 * ContentGuardAccessibilityService
 *
 * עוקב אחרי כל אפליקציה שנפתחת.
 * אם האפליקציה חסומה – מחזיר למסך הבית.
 */
class ContentGuardAccessibilityService : AccessibilityService() {

    companion object {
        private const val TAG = "CGAccessibility"
    }

    private lateinit var prefs: PrefsManager

    override fun onServiceConnected() {
        prefs = PrefsManager(this)

        // הגדרת אילו אירועים לקבל
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

        // לא חוסמים את עצמנו
        if (packageName == this.packageName) return
        if (packageName == "com.contentguard.debug") return

        // בודק אם האפליקציה חסומה
        val blockedApps = prefs.getBlockedApps()
        if (blockedApps.contains(packageName)) {
            Log.d(TAG, "חוסם אפליקציה: $packageName")
            blockApp()
        }
    }

    private fun blockApp() {
        // חוזר למסך הבית
        val homeIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        startActivity(homeIntent)
    }

    override fun onInterrupt() {
        Log.d(TAG, "Accessibility Service הופסק")
    }
}
