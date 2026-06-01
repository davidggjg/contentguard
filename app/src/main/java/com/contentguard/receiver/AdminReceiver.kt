package com.contentguard.receiver

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import com.contentguard.utils.PrefsManager

/**
 * AdminReceiver – מנהל המכשיר.
 *
 * למה זה חשוב?
 * ─────────────
 * כשהמשתמש מאשר לאפליקציה להיות "Device Administrator",
 * אנדרואיד מונע מחיקה רגילה שלה.
 * כדי למחוק, צריך לעבור:
 *   הגדרות → אבטחה → מנהלי מכשיר → ביטול → ואז מחיקה
 *
 * אנחנו מנצלים את onDisableRequested כדי:
 * 1. לרשום את זמן בקשת הביטול
 * 2. להפעיל טיימר של 48 שעות
 * 3. להודיע למשתמש שיצטרך לחכות
 */
class AdminReceiver : DeviceAdminReceiver() {

    companion object {
        private const val TAG = "AdminReceiver"
    }

    /**
     * מופעל כשהמשתמש מנסה לבטל את הרשאות הניהול.
     * זו ההזדמנות שלנו להפעיל את מנגנון ה-Delay.
     *
     * שים לב: לא ניתן לחסום את הביטול לחלוטין –
     * רק להגדיר הודעה שתוצג למשתמש לפני הביטול.
     */
    override fun onDisableRequested(context: Context, intent: Intent): CharSequence {
        Log.d(TAG, "המשתמש ביקש לבטל הרשאות ניהול")

        // רושמים את הזמן שבו הוגשה בקשת הביטול
        PrefsManager(context).setDisableRequestedTime(System.currentTimeMillis())

        // ההודעה הזו תוצג למשתמש לפני הביטול
        return "⚠️ שים לב: ביטול ההגנה יכנס לתוקף בעוד 48 שעות. " +
               "אם תחרט – פשוט פתח את ContentGuard ולחץ 'בטל בקשה'."
    }

    /**
     * מופעל ברגע שהרשאות ניהול אושרו – האפליקציה מוגנת עכשיו!
     */
    override fun onEnabled(context: Context, intent: Intent) {
        Log.d(TAG, "הרשאות ניהול אושרו – האפליקציה מוגנת")
        Toast.makeText(
            context,
            "✅ ContentGuard מוגן כעת מפני הסרה",
            Toast.LENGTH_LONG
        ).show()
    }

    /**
     * מופעל אחרי שהרשאות ניהול בוטלו.
     * כאן נוכל לרשום את האירוע ולשלוח התראה.
     */
    override fun onDisabled(context: Context, intent: Intent) {
        Log.w(TAG, "הרשאות ניהול בוטלו")
        PrefsManager(context).setDisableRequestedTime(0L) // איפוס
    }
}
