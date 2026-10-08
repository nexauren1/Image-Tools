package com.nexauren.imagetools.data

import android.content.Context

object SubscriptionStore {
    private const val PREFS = "image_tools_subscription"
    private const val KEY_ID = "paypal_subscription_id"

    fun get(context: Context): String? =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_ID, null)
            ?.takeIf { it.isNotBlank() }

    fun save(context: Context, subscriptionId: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_ID, subscriptionId)
            .apply()
    }

    fun clear(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .remove(KEY_ID)
            .apply()
    }
}
