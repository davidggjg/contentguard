package com.contentguard.utils

import android.util.Log
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
                val response = conn.inputStream.bufferedReader().readText()
                parseSettings(response)
            } else {
                val error = conn.errorStream?.bufferedReader()?.readText()
                Log.e(TAG, "שגיאת שרת: ${conn.responseCode} – $error")
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
                val response = conn.inputStream.bufferedReader().readText()
                val json = JSONObject(response)
                val domains = json.optJSONArray("blocked_domains")
                val domainList = mutableListOf<String>()
                if (domains != null) {
                    for (i in 0 until domains.length()) {
                        domainList.add(domains.getString(i))
                    }
                }
                DeviceSettings(
                    deviceId = deviceId,
                    deviceName = "",
                    blockedDomains = domainList,
                    blockLevel = json.optString("block_level", "medium")
                )
            } else null
        } catch (e: Exception) {
            Log.e(TAG, "שגיאת עדכון: ${e.message}")
            null
        }
    }

    private fun parseSettings(json: String): DeviceSettings {
        val obj = JSONObject(json)
        val settings = obj.getJSONObject("settings")
        val domains = settings.optJSONArray("blocked_domains")
        val domainList = mutableListOf<String>()
        if (domains != null) {
            for (i in 0 until domains.length()) {
                domainList.add(domains.getString(i))
            }
        }
        return DeviceSettings(
            deviceId = obj.getString("device_id"),
            deviceName = obj.getString("device_name"),
            blockedDomains = domainList,
            blockLevel = settings.optString("block_level", "medium")
        )
    }
}
