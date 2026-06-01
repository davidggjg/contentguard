package com.contentguard.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import android.util.Log
import com.contentguard.utils.BlocklistManager
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.InetAddress
import java.nio.ByteBuffer

/**
 * BlockerVpnService – לב האפליקציה.
 *
 * איך זה עובד בקצרה:
 * 1. יוצרים "ממשק רשת וירטואלי" על המכשיר (tun0)
 * 2. כל החבילות (packets) עוברות דרכנו
 * 3. אנחנו בודקים את כתובת ה-IP/דומיין
 * 4. אם חסום → זורקים את החבילה
 * 5. אם מותר → מעבירים הלאה לאינטרנט האמיתי
 *
 * הערה: זהו שלד – ה-DNS parsing המלא מצריך עוד קוד.
 * ראה docs/SETUP.md להרחבה.
 */
class BlockerVpnService : VpnService() {

    companion object {
        private const val TAG = "BlockerVpnService"
        private const val NOTIFICATION_ID = 1
        private const val CHANNEL_ID = "vpn_channel"

        // DNS של CleanBrowsing לסינון תוכן מבוגרים
        // adult-filter-dns.cleanbrowsing.org
        private const val CLEAN_DNS_1 = "185.228.168.10"
        private const val CLEAN_DNS_2 = "185.228.169.11"
    }

    private var vpnInterface: ParcelFileDescriptor? = null
    private var isRunning = false

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "שירות ה-VPN נוצר")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification())
        startVpn()
        return START_STICKY // אם המערכת הורגת את השירות, הוא מתחיל מחדש
    }

    /**
     * מקים את ממשק ה-VPN עם ה-DNS המסנן.
     */
    private fun startVpn() {
        try {
            vpnInterface = Builder()
                // כתובת IP וירטואלית למכשיר שלנו בתוך ה-VPN
                .addAddress("10.0.0.2", 32)

                // כל התעבורה עוברת דרכנו (0.0.0.0/0 = הכל)
                .addRoute("0.0.0.0", 0)

                // DNS מסנן – זה השינוי הקריטי!
                // במקום שרת ה-DNS של ספק האינטרנט,
                // נשתמש בשרת שחוסם תוכן מבוגרים.
                .addDnsServer(CLEAN_DNS_1)
                .addDnsServer(CLEAN_DNS_2)

                // שם שיופיע בהגדרות הרשת
                .setSession("ContentGuard")

                // MTU סטנדרטי
                .setMtu(1500)

                .establish()

            isRunning = true
            Log.d(TAG, "VPN הופעל בהצלחה עם DNS מסנן")

            // מתחילים לקרוא חבילות ברשת (Thread נפרד)
            startPacketProcessing()

        } catch (e: Exception) {
            Log.e(TAG, "שגיאה בהפעלת VPN: ${e.message}", e)
        }
    }

    /**
     * קורא חבילות רשת ומסנן אותן.
     *
     * כרגע: רק קריאה בסיסית.
     * TODO: להוסיף DNS packet parsing לחסימה לפי שם דומיין.
     */
    private fun startPacketProcessing() {
        Thread {
            val buffer = ByteBuffer.allocate(32767)
            val inputStream = FileInputStream(vpnInterface!!.fileDescriptor)
            val outputStream = FileOutputStream(vpnInterface!!.fileDescriptor)

            while (isRunning) {
                try {
                    // קריאת חבילה נכנסת
                    val length = inputStream.read(buffer.array())
                    if (length <= 0) continue

                    buffer.limit(length)

                    // TODO: כאן תוסיף פירוש של חבילת ה-DNS
                    // ותבדוק אם הדומיין המבוקש נמצא ברשימה השחורה
                    val blocked = shouldBlockPacket(buffer)

                    if (!blocked) {
                        // העבר את החבילה הלאה
                        outputStream.write(buffer.array(), 0, length)
                    }
                    // אם חסום – פשוט לא כותבים כלום (החבילה נזרקת)

                    buffer.clear()
                } catch (e: Exception) {
                    if (isRunning) {
                        Log.e(TAG, "שגיאה בעיבוד חבילה: ${e.message}")
                    }
                }
            }
        }.start()
    }

    /**
     * בודק אם יש לחסום חבילה.
     * כרגע – בדיקת IP בלבד.
     * שלב הבא: פענוח DNS query לבדיקת שם הדומיין.
     */
    private fun shouldBlockPacket(packet: ByteBuffer): Boolean {
        // TODO: מימוש מלא של DNS packet parsing
        // לדוגמה: חילוץ שם הדומיין והשוואה ל-BlocklistManager
        return false
    }

    override fun onDestroy() {
        isRunning = false
        vpnInterface?.close()
        vpnInterface = null
        Log.d(TAG, "שירות ה-VPN נסגר")
        super.onDestroy()
    }

    // ─── Notification (חובה ל-Foreground Service) ───────────────────────────

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "ContentGuard פעיל",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "מציין שהגנת הרשת פעילה"
            }
            getSystemService(NotificationManager::class.java)
                .createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        return Notification.Builder(this, CHANNEL_ID)
            .setContentTitle("🛡️ ContentGuard פעיל")
            .setContentText("הגנת הרשת מופעלת")
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .build()
    }
}
