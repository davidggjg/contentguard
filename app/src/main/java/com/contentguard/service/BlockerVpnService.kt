package com.contentguard.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import android.util.Log
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.DatagramChannel

class BlockerVpnService : VpnService() {

    companion object {
        private const val TAG = "BlockerVpnService"
        private const val NOTIFICATION_ID = 1
        private const val CHANNEL_ID = "vpn_channel"

        // DNS של CleanBrowsing – חוסם תוכן מבוגרים
        private const val CLEAN_DNS_1 = "185.228.168.10"
        private const val CLEAN_DNS_2 = "185.228.169.11"
    }

    private var vpnInterface: ParcelFileDescriptor? = null
    private var isRunning = false
    private var tunnel: DatagramChannel? = null

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "שירות ה-VPN נוצר")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification())
        startVpn()
        return START_STICKY
    }

    private fun startVpn() {
        try {
            // פותחים channel לשרת DNS החיצוני
            tunnel = DatagramChannel.open()
            tunnel?.connect(InetSocketAddress(CLEAN_DNS_1, 53))

            // חשוב: מגנים על ה-channel כדי שהתעבורה שלו לא תעבור דרך ה-VPN עצמו
            protect(tunnel!!.socket())

            vpnInterface = Builder()
                .addAddress("10.0.0.2", 32)

                // רק תעבורת DNS עוברת דרכנו (port 53)
                // שאר התעבורה עוברת ישירות דרך הרשת הרגילה
                .addRoute(CLEAN_DNS_1, 32)
                .addRoute(CLEAN_DNS_2, 32)

                // מחליפים את שרת ה-DNS בשרת המסנן
                .addDnsServer(CLEAN_DNS_1)
                .addDnsServer(CLEAN_DNS_2)

                .setSession("ContentGuard")
                .setMtu(1500)
                .establish()

            isRunning = true
            Log.d(TAG, "VPN הופעל – DNS מוחלף לשרת מסנן")

        } catch (e: Exception) {
            Log.e(TAG, "שגיאה בהפעלת VPN: ${e.message}", e)
            stopSelf()
        }
    }

    override fun onDestroy() {
        isRunning = false
        tunnel?.close()
        vpnInterface?.close()
        vpnInterface = null
        Log.d(TAG, "שירות ה-VPN נסגר")
        super.onDestroy()
    }

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
            .setContentText("DNS מסנן פעיל")
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .build()
    }
}
