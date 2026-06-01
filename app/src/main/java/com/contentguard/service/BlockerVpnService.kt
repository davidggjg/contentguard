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
import java.nio.ByteBuffer

class BlockerVpnService : VpnService() {

    companion object {
        private const val TAG = "BlockerVpnService"
        private const val NOTIFICATION_ID = 1
        private const val CHANNEL_ID = "vpn_channel"
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
        return START_STICKY
    }

    private fun startVpn() {
        try {
            vpnInterface = Builder()
                .addAddress("10.0.0.2", 32)
                .addRoute("0.0.0.0", 0)
                .addDnsServer(CLEAN_DNS_1)
                .addDnsServer(CLEAN_DNS_2)
                .setSession("ContentGuard")
                .setMtu(1500)
                .establish()

            isRunning = true
            Log.d(TAG, "VPN הופעל בהצלחה")
            startPacketProcessing()

        } catch (e: Exception) {
            Log.e(TAG, "שגיאה בהפעלת VPN: ${e.message}", e)
        }
    }

    private fun startPacketProcessing() {
        Thread {
            val buffer = ByteBuffer.allocate(32767)
            val inputStream = FileInputStream(vpnInterface!!.fileDescriptor)
            val outputStream = FileOutputStream(vpnInterface!!.fileDescriptor)

            while (isRunning) {
                try {
                    val length = inputStream.read(buffer.array())
                    if (length <= 0) continue

                    buffer.limit(length)

                    val blocked = shouldBlockPacket(buffer)
                    if (!blocked) {
                        outputStream.write(buffer.array(), 0, length)
                    }

                    buffer.clear()
                } catch (e: Exception) {
                    if (isRunning) {
                        Log.e(TAG, "שגיאה בעיבוד חבילה: ${e.message}")
                    }
                }
            }
        }.start()
    }

    private fun shouldBlockPacket(packet: ByteBuffer): Boolean {
        return false
    }

    override fun onDestroy() {
        isRunning = false
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
            .setContentText("הגנת הרשת מופעלת")
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .build()
    }
}
