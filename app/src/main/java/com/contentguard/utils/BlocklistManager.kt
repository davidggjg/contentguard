package com.contentguard.utils

import android.content.Context
import android.util.Log

/**
 * BlocklistManager – מנהל רשימת האתרים החסומים.
 *
 * בגרסה הנוכחית: רשימה סטטית מובנית.
 *
 * TODO לגרסה הבאה:
 * - הורדת רשימה עדכנית מ-CleanBrowsing / StevenBlack / AdGuard
 * - שמירה מקומית ב-Room Database
 * - עדכון אוטומטי שבועי
 */
class BlocklistManager(private val context: Context) {

    companion object {
        private const val TAG = "BlocklistManager"

        /**
         * רשימה שחורה בסיסית.
         * הרשימה המלאה (מיליוני דומיינים) תורד מהאינטרנט.
         *
         * מקורות מומלצים (קוד פתוח):
         * - https://raw.githubusercontent.com/StevenBlack/hosts/master/alternates/porn/hosts
         * - https://cleanbrowsing.org/filters
         */
        private val BASIC_BLOCKLIST = setOf(
            // דוגמאות – הרשימה האמיתית הרבה יותר ארוכה
            "example-blocked-site.com",
            "another-blocked.net"
            // TODO: הוסף כאן דומיינים ספציפיים שתרצה לחסום
        )
    }

    /**
     * בודק אם דומיין מסוים חסום.
     *
     * @param domain שם הדומיין, לדוגמה: "google.com"
     * @return true אם יש לחסום, false אם מותר
     */
    fun isBlocked(domain: String): Boolean {
        val cleanDomain = domain.lowercase().trim()

        // בדיקה ישירה
        if (BASIC_BLOCKLIST.contains(cleanDomain)) {
            Log.d(TAG, "חסום: $cleanDomain")
            return true
        }

        // בדיקת תת-דומיין (www.example.com → example.com)
        val rootDomain = extractRootDomain(cleanDomain)
        if (rootDomain != cleanDomain && BASIC_BLOCKLIST.contains(rootDomain)) {
            Log.d(TAG, "חסום (תת-דומיין): $cleanDomain → $rootDomain")
            return true
        }

        return false
    }

    /**
     * מחלץ את הדומיין הראשי מכתובת מלאה.
     * לדוגמה: "www.example.co.uk" → "example.co.uk"
     */
    private fun extractRootDomain(domain: String): String {
        val parts = domain.split(".")
        return if (parts.size >= 2) {
            parts.takeLast(2).joinToString(".")
        } else {
            domain
        }
    }

    /**
     * TODO: מורידה רשימת חסימה עדכנית מהאינטרנט.
     * לקרוא פעם בשבוע ברקע.
     */
    suspend fun downloadUpdatedBlocklist() {
        // TODO: מימוש עם OkHttp או Retrofit
        // URL: https://raw.githubusercontent.com/StevenBlack/hosts/master/alternates/porn/hosts
        Log.d(TAG, "TODO: הורדת רשימה מעודכנת")
    }
}
