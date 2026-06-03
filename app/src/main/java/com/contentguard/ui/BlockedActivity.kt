package com.contentguard.ui

import android.app.Activity
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import com.contentguard.R

/**
 * מסך שמוצג כשמנסים לפתוח אפליקציה חסומה.
 */
class BlockedActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_blocked)

        val appName = intent.getStringExtra("app_name") ?: "אפליקציה זו"

        findViewById<TextView>(R.id.tvBlockedAppName).text = "\"$appName\" חסומה"
        findViewById<Button>(R.id.btnGoHome).setOnClickListener {
            finish()
        }
    }

    // מונע חזרה לאפליקציה החסומה עם כפתור Back
    override fun onBackPressed() {
        finish()
    }
}
