package com.contentguard.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * PrefsManager – ניהול הגדרות האפליקציה.
 *
 * משתמשים ב-EncryptedSharedPreferences כדי שלא יהיה אפשר
 * לערוך את ההגדרות ישירות בקבצים על המכשיר.
 */
class PrefsManager(context: Context) {

    companion object {
        private const val PREFS_FILE = "contentguard_prefs"
        private const val KEY_VPN_ENABLED = "vpn_enabled"
        private const val KEY_DISABLE_REQUESTED_TIME = "disable_requested_time"
        private const val KEY_PROTECTION_PIN_HASH = "protection_pin_hash"

        // 48 שעות במילישניות
        const val DELAY_MILLIS = 48L * 60 * 60 * 1000
    }

    // הגדרות מוצפנות – מאובטח יותר מ-SharedPreferences רגיל
    private val prefs: SharedPreferences by lazy {
        try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            EncryptedSharedPreferences.create(
                context,
                PREFS_FILE,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            // fallback לשימוש רגיל אם EncryptedSharedPreferences נכשל
            context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)
        }
    }

    // ─── VPN ────────────────────────────────────────────────────────────────

    fun isVpnEnabled(): Boolean = prefs.getBoolean(KEY_VPN_ENABLED, false)

    fun setVpnEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_VPN_ENABLED, enabled).apply()
    }

    // ─── מנגנון עיכוב הסרה ──────────────────────────────────────────────────

    /**
     * שומר מתי הוגשה בקשת ביטול הגנה.
     * @param time זמן ב-milliseconds, או 0 לאיפוס
     */
    fun setDisableRequestedTime(time: Long) {
        prefs.edit().putLong(KEY_DISABLE_REQUESTED_TIME, time).apply()
    }

    fun getDisableRequestedTime(): Long =
        prefs.getLong(KEY_DISABLE_REQUESTED_TIME, 0L)

    /**
     * האם חלפו 48 שעות מבקשת הביטול?
     */
    fun isDelayPassed(): Boolean {
        val requestTime = getDisableRequestedTime()
        if (requestTime == 0L) return false
        return System.currentTimeMillis() - requestTime >= DELAY_MILLIS
    }

    /**
     * כמה זמן נותר עד שהביטול יכנס לתוקף (במילישניות).
     */
    fun getRemainingDelayMillis(): Long {
        val requestTime = getDisableRequestedTime()
        if (requestTime == 0L) return DELAY_MILLIS
        val elapsed = System.currentTimeMillis() - requestTime
        return maxOf(0L, DELAY_MILLIS - elapsed)
    }

    // ─── סיסמת הגנה ─────────────────────────────────────────────────────────

    /**
     * שומר hash של הסיסמה (לא הסיסמה עצמה!).
     */
    fun setPinHash(pinHash: String) {
        prefs.edit().putString(KEY_PROTECTION_PIN_HASH, pinHash).apply()
    }

    fun getPinHash(): String? = prefs.getString(KEY_PROTECTION_PIN_HASH, null)

    fun hasPin(): Boolean = getPinHash() != null
}
