package com.contentguard.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent
import android.util.Log
import com.contentguard.utils.PrefsManager

class ContentGuardAccessibilityService : AccessibilityService() {

    companion object {
        private const val TAG = "CGAccessibility"
    }

    private lateinit var prefs: PrefsManager
    private val handler = Handler(Looper.getMainLooper())
    private var lastBlockedPackage = ""
    private var lastBlockedTime = 0L

    override fun onServiceConnected() {
        prefs = PrefsManager(this)
        serviceInfo = serviceInfo.apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            notificationTimeout = 50
        }
        Log.d(TAG, "Accessibility Service מחובר")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return

        val packageName = event.packageName?.toString() ?: return

        if (packageName == this.packageName) return
        if (packageName == "com.contentguard.debug") return
        if (packageName == "com.android.systemui") return
        if (packageName == "android") return
        if (packageName.contains("launcher")) return

        // אם המכשיר נעול – חוסם הכל
        if (prefs.isLocked()) {
            goHome()
            return
        }

        val now = System.currentTimeMillis()
        if (packageName == lastBlockedPackage && now - lastBlockedTime < 1000) return

        val blockedApps = prefs.getBlockedApps()
        if (blockedApps.contains(packageName)) {
            lastBlockedPackage = packageName
            lastBlockedTime = now
            Log.d(TAG, "חוסם: $packageName")
            goHome()
            handler.postDelayed({ goHome() }, 300)
            handler.postDelayed({ goHome() }, 700)
        }
    }

    private fun goHome() {
        performGlobalAction(GLOBAL_ACTION_HOME)
    }

    override fun onInterrupt() {
        Log.d(TAG, "הופסק")
    }
}
