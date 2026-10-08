package com.nexauren.imagetools.data

import android.content.Context

object SubscriptionStore {
    private const val PREFS = "image_tools_subscription"
    private const val LEGACY_KEY_ID = "paypal_subscription_id"
    private const val KEY_PREFIX = "paypal_subscription_id_"

    private fun keyForUser(userUid: String): String = KEY_PREFIX + userUid

    fun get(context: Context, userUid: String?): String? {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

        if (!userUid.isNullOrBlank()) {
            prefs.getString(keyForUser(userUid), null)
                ?.takeIf { it.isNotBlank() }
                ?.let { return it }
        }

        // Compatibility with builds that stored one global subscription id.
        // The backend still verifies PayPal ownership before cancelling.
        return prefs.getString(LEGACY_KEY_ID, null)?.takeIf { it.isNotBlank() }
    }

    fun save(context: Context, userUid: String?, subscriptionId: String) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val editor = prefs.edit()

        if (!userUid.isNullOrBlank()) {
            editor.putString(keyForUser(userUid), subscriptionId)
            editor.remove(LEGACY_KEY_ID)
        } else {
            editor.putString(LEGACY_KEY_ID, subscriptionId)
        }

        editor.apply()
    }

    fun clear(context: Context, userUid: String?) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val editor = prefs.edit()

        if (!userUid.isNullOrBlank()) {
            editor.remove(keyForUser(userUid))
        }
        editor.remove(LEGACY_KEY_ID)
        editor.apply()
    }
}
