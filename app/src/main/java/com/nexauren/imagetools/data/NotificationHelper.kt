package com.nexauren.imagetools.data

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build

object AppNotificationSettings {
    private const val PREFS = "image_tools_notifications"
    private const val KEY_ENABLED = "enabled"

    fun isEnabled(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(KEY_ENABLED, false)

    fun setEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_ENABLED, enabled)
            .apply()
    }
}

object AppNotificationHelper {
    private const val CHANNEL_ID = "image_tools_general"
    private const val CHANNEL_NAME = "Image Tools"

    fun ensureChannel(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Updates and important activity from Image Tools."
            }
        )
    }

    fun hasPermission(context: Context): Boolean {
        return Build.VERSION.SDK_INT < 33 ||
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
    }

    fun show(context: Context, title: String, message: String) {
        if (!AppNotificationSettings.isEnabled(context) || !hasPermission(context)) return

        ensureChannel(context)

        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        val notification = android.app.Notification.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(message)
            .setAutoCancel(true)
            .setCategory(android.app.Notification.CATEGORY_STATUS)
            .build()

        manager.notify((System.currentTimeMillis() and 0x7FFFFFFF).toInt(), notification)
    }
}
