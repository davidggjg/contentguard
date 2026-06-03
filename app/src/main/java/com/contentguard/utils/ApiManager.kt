package com.contentguard.utils

import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object ApiManager {

    private const val TAG = "ApiManager"
    private const val BASE_URL = "https://contentguard-web-vxiw.vercel.app/api"

    data class DeviceSettings(
        val deviceId: String,
        val deviceName: String,
        val blockedDomains: List<String>,
        val blockedApps: List<String>,
        val blockLevel: String
    )

    fun activate(activationCode: String): DeviceSettings? {
        return try {
            val url = URL("$BASE_URL/devices")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json")
            conn.doOutput = true
            val body = JSONObject().put("activation_code", activationCode).toString()
            conn.outputStream.write(body.toByteArray())
            if (conn.responseCode == 200) {
                parseSettings(conn.inputStream.bufferedReader().readText())
            } else {
                Log.e(TAG, "שגיאה: ${conn.responseCode} – ${conn.errorStream?.bufferedReader()?.readText()}")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "שגיאת חיבור: ${e.message}", e)
            null
        }
    }

    fun fetchSettings(deviceId: String): DeviceSettings? {
        return try {
            val url = URL("$BASE_URL/devices?device_id=$deviceId")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            if (conn.responseCode == 200) {
                val json = JSONObject(conn.inputStream.bufferedReader().readText())
                DeviceSettings(
                    deviceId = deviceId,
                    deviceName = "",
                    blockedDomains = jsonArrayToList(json.optJSONArray("blocked_domains")),
                    blockedApps = jsonArrayToList(json.optJSONArray("blocked_apps")),
                    blockLevel = json.optString("block_level", "medium")
                )
            } else null
        } catch (e: Exception) {
            Log.e(TAG, "שגיאת עדכון: ${e.message}")
            null
        }
    }

    // שולח heartbeat לשרת – מסמן שהמכשיר מחובר
    fun sendHeartbeat(deviceId: String) {
        try {
            val url = URL("$BASE_URL/heartbeat")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json")
            conn.doOutput = true
            val body = JSONObject().put("device_id", deviceId).toString()
            conn.outputStream.write(body.toByteArray())
            conn.responseCode // מפעיל את הבקשה
        } catch (e: Exception) {
            Log.e(TAG, "שגיאת heartbeat: ${e.message}")
        }
    }

    // שולח רשימת אפליקציות מותקנות לשרת
    fun sendInstalledApps(deviceId: String, apps: List<Pair<String, String>>) {
        try {
            val url = URL("$BASE_URL/apps")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json")
            conn.doOutput = true
            val appsArray = JSONArray()
            apps.forEach { (packageName, appName) ->
                appsArray.put(JSONObject().put("package", packageName).put("name", appName))
            }
            val body = JSONObject()
                .put("device_id", deviceId)
                .put("apps", appsArray)
                .toString()
            conn.outputStream.write(body.toByteArray())
            conn.responseCode
        } catch (e: Exception) {
            Log.e(TAG, "שגיאת apps: ${e.message}")
        }
    }

    private fun parseSettings(json: String): DeviceSettings {
        val obj = JSONObject(json)
        val settings = obj.getJSONObject("settings")
        return DeviceSettings(
            deviceId = obj.getString("device_id"),
            deviceName = obj.getString("device_name"),
            blockedDomains = jsonArrayToList(settings.optJSONArray("blocked_domains")),
            blockedApps = jsonArrayToList(settings.optJSONArray("blocked_apps")),
            blockLevel = settings.optString("block_level", "medium")
        )
    }

    private fun jsonArrayToList(arr: JSONArray?): List<String> {
        if (arr == null) return emptyList()
        return (0 until arr.length()).map { arr.getString(it) }
    }
}
