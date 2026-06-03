package com.contentguard.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.contentguard.R
import com.contentguard.utils.ApiManager
import com.contentguard.utils.PrefsManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ActivationActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs = PrefsManager(this)

        // אם כבר מופעל – עבור ישירות למסך הראשי
        if (prefs.isActivated()) {
            // משך הגדרות עדכניות ברקע
            val deviceId = prefs.getDeviceId()
            if (deviceId != null) {
                CoroutineScope(Dispatchers.IO).launch {
                    val settings = ApiManager.fetchSettings(deviceId)
                    if (settings != null) {
                        prefs.setBlockedDomains(settings.blockedDomains)
                        prefs.setBlockedApps(settings.blockedApps)
                        prefs.setBlockLevel(settings.blockLevel)
                    }
                }
            }
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }

        setContentView(R.layout.activity_activation)

        val etCode = findViewById<EditText>(R.id.etActivationCode)
        val btnActivate = findViewById<Button>(R.id.btnActivate)
        val tvStatus = findViewById<TextView>(R.id.tvActivationStatus)

        btnActivate.setOnClickListener {
            val code = etCode.text.toString().trim().uppercase()
            if (code.length != 6) {
                Toast.makeText(this, "קוד הפעלה חייב להיות 6 תווים", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            btnActivate.isEnabled = false
            tvStatus.text = "מתחבר לשרת..."

            CoroutineScope(Dispatchers.IO).launch {
                val settings = ApiManager.activate(code)
                withContext(Dispatchers.Main) {
                    if (settings != null) {
                        prefs.setDeviceId(settings.deviceId)
                        prefs.setActivationCode(code)
                        prefs.setBlockedDomains(settings.blockedDomains)
                        prefs.setBlockedApps(settings.blockedApps)
                        prefs.setBlockLevel(settings.blockLevel)
                        prefs.setActivated(true)

                        tvStatus.text = "✅ הופעל בהצלחה!"
                        Toast.makeText(
                            this@ActivationActivity,
                            "ברוך הבא! ${settings.deviceName}",
                            Toast.LENGTH_LONG
                        ).show()

                        startActivity(Intent(this@ActivationActivity, MainActivity::class.java))
                        finish()
                    } else {
                        tvStatus.text = ""
                        btnActivate.isEnabled = true
                        Toast.makeText(
                            this@ActivationActivity,
                            "קוד שגוי – נסה שוב",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }
    }
}
