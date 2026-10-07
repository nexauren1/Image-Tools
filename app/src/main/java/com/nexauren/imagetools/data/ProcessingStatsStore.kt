package com.nexauren.imagetools.data

import android.content.Context

object ProcessingStatsStore {
    private const val PREFS = "image_tools_stats"
    private const val PROCESSED = "processed"
    private const val EXPORTED = "exported"

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun processed(context: Context): Int = prefs(context).getInt(PROCESSED, 0)
    fun exported(context: Context): Int = prefs(context).getInt(EXPORTED, 0)

    fun recordProcessed(context: Context, count: Int = 1) {
        if (count <= 0) return
        val p = prefs(context)
        p.edit().putInt(PROCESSED, p.getInt(PROCESSED, 0) + count).apply()
    }

    fun recordExported(context: Context, count: Int = 1) {
        if (count <= 0) return
        val p = prefs(context)
        p.edit().putInt(EXPORTED, p.getInt(EXPORTED, 0) + count).apply()
    }
}
