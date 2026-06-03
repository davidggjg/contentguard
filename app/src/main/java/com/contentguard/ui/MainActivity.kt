package com.contentguard.ui

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.VpnService
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.contentguard.R
import com.contentguard.receiver.AdminReceiver
import com.contentguard.service.BlockerVpnService
import com.contentguard.service.ContentGuardAccessibilityService
import com.contentguard.utils.ApiManager
import com.contentguard.utils.AppScanner
import com.contentguard.utils.HeartbeatWorker
import com.contentguard.utils.PrefsManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {

    private lateinit var prefs: PrefsManager
    private lateinit var devicePolicyManager: DevicePolicyManager
    private lateinit var adminComponentName: ComponentName

    private lateinit var tvStatus: TextView
    private lateinit var tvDelayTimer: TextView
    private lateinit var btnToggleVpn: Button
    private lateinit var btnAdminProtection: Button
    private lateinit var btnAccessibility: Button
    private lateinit var btnCancelRequest: Button

    private val vpnPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) startVpnService()
        else showToast("הרשאת VPN נדחתה")
    }

    private val adminPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { updateUI() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        prefs = PrefsManager(this)
        devicePolicyManager = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        adminComponentName = ComponentName(this, AdminReceiver::class.java)

        initViews()
        setupClickListeners()
        updateUI()
        startHeartbeat()
        syncAppsNow()
    }

    override fun onResume() {
        super.onResume()
        updateUI()
    }

    private fun initViews() {
        tvStatus = findViewById(R.id.tvStatus)
        tvDelayTimer = findViewById(R.id.tvDelayTimer)
        btnToggleVpn = findViewById(R.id.btnToggleVpn)
        btnAdminProtection = findViewById(R.id.btnAdminProtection)
        btnAccessibility = findViewById(R.id.btnAccessibility)
        btnCancelRequest = findViewById(R.id.btnCancelRequest)
    }

    private fun setupClickListeners() {
        btnToggleVpn.setOnClickListener {
            if (prefs.isVpnEnabled()) showDisableConfirmation()
            else requestVpnPermission()
        }
        btnAdminProtection.setOnClickListener {
            if (!isAdminActive()) requestAdminPermission()
            else showToast("הגנה כבר מופעלת ✅")
        }
        btnAccessibility.setOnClickListener {
            if (!isAccessibilityEnabled()) {
                // פותח את הגדרות הנגישות
                startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                showToast("חפש ContentGuard והפעל")
            } else {
                showToast("חסימת אפליקציות פעילה ✅")
            }
        }
        btnCancelRequest.setOnClickListener {
            prefs.setDisableRequestedTime(0L)
            showToast("בקשת הביטול בוטלה ✅")
            updateUI()
        }
    }

    private fun syncAppsNow() {
        val deviceId = prefs.getDeviceId() ?: return
        CoroutineScope(Dispatchers.IO).launch {
            val apps = AppScanner.getInstalledApps(applicationContext)
            ApiManager.sendInstalledApps(deviceId, apps)
            ApiManager.sendHeartbeat(deviceId)
        }
    }

    private fun startHeartbeat() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        val request = PeriodicWorkRequestBuilder<HeartbeatWorker>(15, TimeUnit.MINUTES)
            .setConstraints(constraints)
            .build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "heartbeat", ExistingPeriodicWorkPolicy.KEEP, request
        )
    }

    private fun requestVpnPermission() {
        val intent = VpnService.prepare(this)
        if (intent != null) vpnPermissionLauncher.launch(intent)
        else startVpnService()
    }

    private fun startVpnService() {
        startForegroundService(Intent(this, BlockerVpnService::class.java))
        prefs.setVpnEnabled(true)
        updateUI()
        showToast("🛡️ הגנה הופעלה!")
    }

    private fun stopVpnService() {
        stopService(Intent(this, BlockerVpnService::class.java))
        prefs.setVpnEnabled(false)
        updateUI()
    }

    private fun showDisableConfirmation() {
        AlertDialog.Builder(this)
            .setTitle("לכבות את ההגנה?")
            .setMessage("האם אתה בטוח?")
            .setPositiveButton("כן, כבה") { _, _ -> stopVpnService() }
            .setNegativeButton("ביטול", null)
            .show()
    }

    private fun isAdminActive() = devicePolicyManager.isAdminActive(adminComponentName)

    private fun isAccessibilityEnabled(): Boolean {
        val service = "${packageName}/${ContentGuardAccessibilityService::class.java.canonicalName}"
        val enabled = Settings.Secure.getString(
            contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        return enabled.contains(service)
    }

    private fun requestAdminPermission() {
        val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
            putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, adminComponentName)
            putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, "הגנה מפני הסרה")
        }
        adminPermissionLauncher.launch(intent)
    }

    private fun updateUI() {
        val vpnActive = prefs.isVpnEnabled()
        val adminActive = isAdminActive()
        val accessibilityActive = isAccessibilityEnabled()
        val pendingRequest = prefs.getDisableRequestedTime() > 0

        tvStatus.text = when {
            vpnActive && adminActive && accessibilityActive -> "🛡️ הגנה מלאה פעילה"
            vpnActive && adminActive -> "⚡ VPN + Admin פעיל (ללא חסימת אפליקציות)"
            vpnActive -> "⚡ VPN פעיל בלבד"
            else -> "⚠️ הגנה לא פעילה"
        }

        btnToggleVpn.text = if (vpnActive) "כבה הגנה" else "הפעל הגנה"
        btnAdminProtection.text = if (adminActive) "✅ מוגן מהסרה" else "הפעל הגנת הסרה"
        btnAdminProtection.isEnabled = !adminActive
        btnAccessibility.text = if (accessibilityActive) "✅ חסימת אפליקציות פעילה" else "הפעל חסימת אפליקציות"

        if (pendingRequest) {
            val remaining = prefs.getRemainingDelayMillis()
            val hours = remaining / 3600000
            val minutes = (remaining % 3600000) / 60000
            tvDelayTimer.text = "⏳ ביטול בעוד: ${hours}ש׳ ${minutes}ד׳"
            btnCancelRequest.visibility = android.view.View.VISIBLE
        } else {
            tvDelayTimer.text = ""
            btnCancelRequest.visibility = android.view.View.GONE
        }
    }

    private fun showToast(message: String) =
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
}
