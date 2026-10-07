package com.nexauren.imagetools.data

import android.content.Context

data class HistoryEntry(
    val toolId: String,
    val label: String,
    val timestamp: Long,
    val inputCount: Int,
    val outputUri: String?
)

object HistoryStore {
    private const val PREFS = "image_tools_history"
    private const val KEY = "entries"
    private const val SEP = "||"

    fun list(context: Context): List<HistoryEntry> =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getStringSet(KEY, emptySet())
            .orEmpty()
            .mapNotNull { row ->
                val p = row.split(SEP)
                if (p.size < 5) null else HistoryEntry(
                    p[0], p[1], p[2].toLongOrNull() ?: 0L, p[3].toIntOrNull() ?: 1, p[4]
                )
            }
            .sortedByDescending { it.timestamp }

    fun add(context: Context, entry: HistoryEntry) {
        val current = list(context).toMutableList()
        current.removeAll { it.timestamp == entry.timestamp }
        current.add(0, entry)
        val saved = current.take(40).map {
            listOf(
                it.toolId,
                it.label.replace(SEP, " "),
                it.timestamp.toString(),
                it.inputCount.toString(),
                it.outputUri.orEmpty()
            ).joinToString(SEP)
        }.toSet()
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putStringSet(KEY, saved).apply()
    }

    fun clear(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().remove(KEY).apply()
    }
}
