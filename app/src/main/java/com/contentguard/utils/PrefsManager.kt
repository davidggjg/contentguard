package com.contentguard.utils

import android.content.Context
import android.content.SharedPreferences

class PrefsManager(context: Context) {

    companion object {
        private const val PREFS_FILE = "contentguard_prefs"
        private const val KEY_VPN_ENABLED = "vpn_enabled"
        private const val KEY_DISABLE_REQUESTED_TIME = "disable_requested_time"
        private const val KEY_DEVICE_ID = "device_id"
        private const val KEY_ACTIVATION_CODE = "activation_code"
        private const val KEY_BLOCKED_DOMAINS = "blocked_domains"
        private const val KEY_BLOCK_LEVEL = "block_level"
        private const val KEY_ACTIVATED = "activated"
        const val DELAY_MILLIS = 48L * 60 * 60 * 1000
    }

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)

    // VPN
    fun isVpnEnabled() = prefs.getBoolean(KEY_VPN_ENABLED, false)
    fun setVpnEnabled(enabled: Boolean) = prefs.edit().putBoolean(KEY_VPN_ENABLED, enabled).apply()

    // עיכוב הסרה
    fun setDisableRequestedTime(time: Long) = prefs.edit().putLong(KEY_DISABLE_REQUESTED_TIME, time).apply()
    fun getDisableRequestedTime() = prefs.getLong(KEY_DISABLE_REQUESTED_TIME, 0L)
    fun isDelayPassed(): Boolean {
        val t = getDisableRequestedTime()
        return t > 0 && System.currentTimeMillis() - t >= DELAY_MILLIS
    }
    fun getRemainingDelayMillis(): Long {
        val t = getDisableRequestedTime()
        return if (t == 0L) DELAY_MILLIS else maxOf(0L, DELAY_MILLIS - (System.currentTimeMillis() - t))
    }

    // חשבון
    fun isActivated() = prefs.getBoolean(KEY_ACTIVATED, false)
    fun setActivated(v: Boolean) = prefs.edit().putBoolean(KEY_ACTIVATED, v).apply()
    fun getDeviceId() = prefs.getString(KEY_DEVICE_ID, null)
    fun setDeviceId(id: String) = prefs.edit().putString(KEY_DEVICE_ID, id).apply()
    fun getActivationCode() = prefs.getString(KEY_ACTIVATION_CODE, null)
    fun setActivationCode(code: String) = prefs.edit().putString(KEY_ACTIVATION_CODE, code).apply()

    // הגדרות חסימה
    fun getBlockedDomains(): List<String> {
        val raw = prefs.getString(KEY_BLOCKED_DOMAINS, "") ?: ""
        return if (raw.isEmpty()) emptyList() else raw.split(",")
    }
    fun setBlockedDomains(domains: List<String>) =
        prefs.edit().putString(KEY_BLOCKED_DOMAINS, domains.joinToString(",")).apply()
    fun getBlockLevel() = prefs.getString(KEY_BLOCK_LEVEL, "medium") ?: "medium"
    fun setBlockLevel(level: String) = prefs.edit().putString(KEY_BLOCK_LEVEL, level).apply()
}
