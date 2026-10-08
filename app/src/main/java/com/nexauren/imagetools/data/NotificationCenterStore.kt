package com.nexauren.imagetools.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class AppNotification(
    val id: Long,
    val title: String,
    val message: String,
    val createdAt: Long,
    val read: Boolean
)

object NotificationCenterStore {
    private const val PREFS = "image_tools_notification_center"
    private const val KEY_PREFIX = "items_"

    private fun key(uid: String?): String =
        KEY_PREFIX + (uid?.takeIf { it.isNotBlank() } ?: "guest")

    fun list(context: Context, uid: String?): List<AppNotification> {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(key(uid), null)
            ?: return emptyList()

        return runCatching {
            val array = JSONArray(raw)
            buildList(array.length()) {
                for (index in 0 until array.length()) {
                    val item = array.getJSONObject(index)
                    add(
                        AppNotification(
                            id = item.optLong("id"),
                            title = item.optString("title"),
                            message = item.optString("message"),
                            createdAt = item.optLong("createdAt"),
                            read = item.optBoolean("read")
                        )
                    )
                }
            }.sortedByDescending { it.createdAt }
        }.getOrDefault(emptyList())
    }

    fun unreadCount(context: Context, uid: String?): Int =
        list(context, uid).count { !it.read }

    fun add(
        context: Context,
        uid: String?,
        title: String,
        message: String
    ) {
        val now = System.currentTimeMillis()
        val items = list(context, uid).toMutableList()
        items.add(
            0,
            AppNotification(
                id = now,
                title = title,
                message = message,
                createdAt = now,
                read = false
            )
        )
        save(context, uid, items.take(50))
    }

    fun markRead(context: Context, uid: String?, id: Long) {
        save(
            context,
            uid,
            list(context, uid).map { notification ->
                if (notification.id == id) notification.copy(read = true) else notification
            }
        )
    }

    fun markAllRead(context: Context, uid: String?) {
        save(
            context,
            uid,
            list(context, uid).map { it.copy(read = true) }
        )
    }

    fun clear(context: Context, uid: String?) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .remove(key(uid))
            .apply()
    }

    private fun save(
        context: Context,
        uid: String?,
        items: List<AppNotification>
    ) {
        val array = JSONArray()
        items.forEach { item ->
            array.put(
                JSONObject()
                    .put("id", item.id)
                    .put("title", item.title)
                    .put("message", item.message)
                    .put("createdAt", item.createdAt)
                    .put("read", item.read)
            )
        }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(key(uid), array.toString())
            .apply()
    }
}
