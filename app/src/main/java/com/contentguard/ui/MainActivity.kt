package com.contentguard.ui

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.VpnService
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.contentguard.R
import com.contentguard.receiver.AdminReceiver
import com.contentguard.service.BlockerVpnService
import com.contentguard.utils.PrefsManager
import java.util.concurrent.TimeUnit

/**
 * MainActivity – המסך הראשי של ContentGuard.
 *
 * מה יש כאן:
 * 1. כפתור להפעלת/כיבוי ה-VPN
 * 2. כפתור לאישור הרשאות Device Admin
 * 3. תצוגת מצב ההגנה
 * 4. טיימר לביטול (אם הוגשה בקשה)
 */
class MainActivity : AppCompatActivity() {

    private lateinit var prefs: PrefsManager
    private lateinit var devicePolicyManager: DevicePolicyManager
    private lateinit var adminComponentName: ComponentName

    // Views
    private lateinit var tvStatus: TextView
    private lateinit var tvDelayTimer: TextView
    private lateinit var btnToggleVpn: Button
    private lateinit var btnAdminProtection: Button
    private lateinit var btnCancelRequest: Button

    // Activity Result Launchers (הדרך המודרנית לבקש הרשאות)
    private val vpnPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            startVpnService()
        } else {
            showToast("הרשאת VPN נדחתה")
        }
    }

    private val adminPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            showToast("✅ הגנת הסרה הופעלה!")
        }
        updateUI()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        prefs = PrefsManager(this)
        devicePolicyManager = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        adminComponentName = ComponentName(this, AdminReceiver::class.java)

        initViews()
        setupClickListeners()
        updateUI()
    }

    override fun onResume() {
        super.onResume()
        updateUI() // מעדכן את המצב כל פעם שחוזרים למסך
    }

    private fun initViews() {
        tvStatus = findViewById(R.id.tvStatus)
        tvDelayTimer = findViewById(R.id.tvDelayTimer)
        btnToggleVpn = findViewById(R.id.btnToggleVpn)
        btnAdminProtection = findViewById(R.id.btnAdminProtection)
        btnCancelRequest = findViewById(R.id.btnCancelRequest)
    }

    private fun setupClickListeners() {

        // הפעלה/כיבוי VPN
        btnToggleVpn.setOnClickListener {
            if (prefs.isVpnEnabled()) {
                showDisableConfirmation()
            } else {
                requestVpnPermission()
            }
        }

        // הפעלת הגנת Admin
        btnAdminProtection.setOnClickListener {
            if (isAdminActive()) {
                showToast("הגנה כבר מופעלת ✅")
            } else {
                requestAdminPermission()
            }
        }

        // ביטול בקשת הסרה (אם הוגשה בטעות)
        btnCancelRequest.setOnClickListener {
            prefs.setDisableRequestedTime(0L)
            showToast("בקשת הביטול בוטלה ✅")
            updateUI()
        }
    }

    // ─── VPN ────────────────────────────────────────────────────────────────

    private fun requestVpnPermission() {
        val intent = VpnService.prepare(this)
        if (intent != null) {
            // צריך לבקש הרשאה מהמשתמש
            vpnPermissionLauncher.launch(intent)
        } else {
            // הרשאה כבר קיימת
            startVpnService()
        }
    }

    private fun startVpnService() {
        val intent = Intent(this, BlockerVpnService::class.java)
        startForegroundService(intent)
        prefs.setVpnEnabled(true)
        updateUI()
        showToast("🛡️ הגנה הופעלה!")
    }

    private fun stopVpnService() {
        val intent = Intent(this, BlockerVpnService::class.java)
        stopService(intent)
        prefs.setVpnEnabled(false)
        updateUI()
    }

    /**
     * מציג דיאלוג אישור לפני כיבוי – עוד שכבת הגנה מפני דחף רגעי.
     */
    private fun showDisableConfirmation() {
        AlertDialog.Builder(this)
            .setTitle("לכבות את ההגנה?")
            .setMessage(
                "האם אתה בטוח שתרצה לכבות את הגנת הרשת?\n\n" +
                "💡 טיפ: אם יש משהו שתרצה לגשת אליו – " +
                "אולי כדאי להמתין כמה דקות ולראות אם הדחף עובר."
            )
            .setPositiveButton("כן, כבה") { _, _ -> stopVpnService() }
            .setNegativeButton("ביטול", null)
            .show()
    }

    // ─── Device Admin ────────────────────────────────────────────────────────

    private fun isAdminActive(): Boolean {
        return devicePolicyManager.isAdminActive(adminComponentName)
    }

    private fun requestAdminPermission() {
        val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
            putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, adminComponentName)
            putExtra(
                DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                "הגנה זו מונעת מחיקה מקרית של ContentGuard. " +
                "כדי להסיר, יש לבטל הרשאות אלו תחילה."
            )
        }
        adminPermissionLauncher.launch(intent)
    }

    // ─── UI Update ───────────────────────────────────────────────────────────

    private fun updateUI() {
        val vpnActive = prefs.isVpnEnabled()
        val adminActive = isAdminActive()
        val pendingRequest = prefs.getDisableRequestedTime() > 0

        // סטטוס כללי
        tvStatus.text = when {
            vpnActive && adminActive -> "🛡️ הגנה מלאה פעילה"
            vpnActive -> "⚡ VPN פעיל (ללא הגנת הסרה)"
            adminActive -> "🔒 הגנת הסרה פעילה (VPN כבוי)"
            else -> "⚠️ הגנה לא פעילה"
        }

        // כפתור VPN
        btnToggleVpn.text = if (vpnActive) "כבה הגנה" else "הפעל הגנה"

        // כפתור Admin
        btnAdminProtection.text = if (adminActive) "✅ מוגן מהסרה" else "הפעל הגנת הסרה"
        btnAdminProtection.isEnabled = !adminActive

        // טיימר ביטול
        if (pendingRequest) {
            val remaining = prefs.getRemainingDelayMillis()
            val hours = TimeUnit.MILLISECONDS.toHours(remaining)
            val minutes = TimeUnit.MILLISECONDS.toMinutes(remaining) % 60
            tvDelayTimer.text = "⏳ ביטול בעוד: ${hours}ש׳ ${minutes}ד׳"
            btnCancelRequest.visibility = android.view.View.VISIBLE
        } else {
            tvDelayTimer.text = ""
            btnCancelRequest.visibility = android.view.View.GONE
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
