package com.nexauren.imagetools.data

import android.content.Context

object FavoritesStore {
    private const val PREFS = "image_tools_favorites"
    private const val KEY_IDS = "ids"

    fun list(context: Context): Set<String> =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getStringSet(KEY_IDS, emptySet())
            ?.toSet()
            ?: emptySet()

    fun isFavorite(context: Context, id: String): Boolean =
        list(context).contains(id)

    fun toggle(context: Context, id: String): Boolean {
        val current = list(context).toMutableSet()
        val added = current.add(id)
        if (!added) current.remove(id)

        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putStringSet(KEY_IDS, current)
            .apply()

        return added
    }
}
