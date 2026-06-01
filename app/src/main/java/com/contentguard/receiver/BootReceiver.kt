package com.contentguard.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.contentguard.service.BlockerVpnService
import com.contentguard.utils.PrefsManager

/**
 * BootReceiver – מאזין לאתחול המכשיר.
 *
 * בלי זה, ה-VPN לא יפעל אחרי שהמשתמש מכבה ומדליק את הטלפון.
 * עם זה – ה-VPN מופעל אוטומטית עם כל הפעלה.
 */
class BootReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "BootReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.d(TAG, "המכשיר אותחל – בודק אם להפעיל VPN")

            val prefs = PrefsManager(context)

            // מפעיל את ה-VPN רק אם הוא היה פעיל לפני הכיבוי
            if (prefs.isVpnEnabled()) {
                Log.d(TAG, "מפעיל VPN אוטומטית לאחר אתחול")
                val vpnIntent = Intent(context, BlockerVpnService::class.java)
                context.startForegroundService(vpnIntent)
            }
        }
    }
}
