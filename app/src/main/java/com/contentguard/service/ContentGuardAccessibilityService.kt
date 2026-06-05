package com.contentguard.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Intent
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
    private var blockRunnable: Runnable? = null

    override fun onServiceConnected() {
        prefs = PrefsManager(this)
        serviceInfo = serviceInfo.apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or
                         AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            notificationTimeout = 50
        }
        Log.d(TAG, "Accessibility Service מחובר")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return

        val packageName = event.packageName?.toString() ?: return

        // לא חוסמים מערכת ואת עצמנו
        if (packageName == this.packageName) return
        if (packageName == "com.contentguard.debug") return
        if (packageName == "com.android.systemui") return
        if (packageName == "android") return
        if (packageName == "com.android.launcher3") return
        if (packageName.contains("launcher")) return

        val now = System.currentTimeMillis()
        if (packageName == lastBlockedPackage && now - lastBlockedTime < 1000) return

        val blockedApps = prefs.getBlockedApps()
        if (blockedApps.contains(packageName)) {
            lastBlockedPackage = packageName
            lastBlockedTime = now

            Log.d(TAG, "חוסם: $packageName")

            // חוזר למסך הבית מיד
            goHome()

            // חוזר שוב אחרי 300ms למקרה שאנדרואיד החזיר לאפליקציה
            blockRunnable?.let { handler.removeCallbacks(it) }
            blockRunnable = Runnable { goHome() }
            handler.postDelayed(blockRunnable!!, 300)
            handler.postDelayed({ goHome() }, 600)
            handler.postDelayed({ goHome() }, 1000)
        }
    }

    private fun goHome() {
        performGlobalAction(GLOBAL_ACTION_HOME)
    }

    override fun onInterrupt() {
        Log.d(TAG, "Accessibility Service הופסק")
    }

    override fun onDestroy() {
        blockRunnable?.let { handler.removeCallbacks(it) }
        super.onDestroy()
    }
}
