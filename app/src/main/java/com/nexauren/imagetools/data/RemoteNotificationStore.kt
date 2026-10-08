package com.nexauren.imagetools.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object RemoteNotificationStore {
    private const val FEED_URL =
        "https://raw.githubusercontent.com/nexauren1/Image-Tools/main/remote-notifications.json"

    suspend fun sync(context: Context, uid: String?): Int = withContext(Dispatchers.IO) {
        if (!AppNotificationSettings.isEnabled(context)) return@withContext 0

        var connection: HttpURLConnection? = null
        try {
            val url = URL(FEED_URL + "?t=" + System.currentTimeMillis())
            connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 5000
                readTimeout = 5000
                setRequestProperty("Accept", "application/json")
                setRequestProperty("Cache-Control", "no-cache")
            }

            if (connection.responseCode !in 200..299) return@withContext 0

            val body = connection.inputStream.bufferedReader().use { it.readText() }
            val root = JSONObject(body)
            val notifications = root.optJSONArray("notifications") ?: return@withContext 0
            var added = 0

            for (index in 0 until notifications.length()) {
                val item = notifications.optJSONObject(index) ?: continue
                val id = item.optString("id").trim()
                val title = item.optString("title").trim()
                val message = item.optString("message").trim()
                if (id.isBlank() || title.isBlank() || message.isBlank()) continue

                if (NotificationCenterStore.addRemote(
                        context = context,
                        uid = uid,
                        remoteId = id,
                        title = title,
                        message = message,
                        createdAt = item.optLong("createdAt")
                    )
                ) {
                    added++
                }
            }
            added
        } catch (_: Exception) {
            0
        } finally {
            connection?.disconnect()
        }
    }
}
