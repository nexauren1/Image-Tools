package com.nexauren.imagetools.data

import android.content.Context

data class Recipe(
    val name: String,
    val toolId: String,
    val config: Map<String, String>
)

object RecipeStore {
    private const val PREFS = "image_tools_recipes"
    private const val KEY = "recipes"
    private const val PENDING_KEY = "pending_recipe"

    fun list(context: Context): List<Recipe> {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getStringSet(KEY, emptySet())
            .orEmpty()
            .mapNotNull { decode(it) }
            .sortedBy { it.name.lowercase() }
    }

    fun save(context: Context, recipe: Recipe) {
        val encoded = encode(recipe)
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val set = prefs.getStringSet(KEY, emptySet()).orEmpty().toMutableSet()
        set.removeIf { decode(it)?.name == recipe.name }
        set.add(encoded)
        prefs.edit().putStringSet(KEY, set).apply()
    }

    fun setPending(context: Context, recipe: Recipe) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(PENDING_KEY, encode(recipe))
            .apply()
    }

    fun consumePending(context: Context): Recipe? {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val encoded = prefs.getString(PENDING_KEY, null) ?: return null
        prefs.edit().remove(PENDING_KEY).apply()
        return decode(encoded)
    }

    fun delete(context: Context, name: String) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val set = prefs.getStringSet(KEY, emptySet()).orEmpty().filterNot { decode(it)?.name == name }.toSet()
        prefs.edit().putStringSet(KEY, set).apply()
    }

    private fun encode(recipe: Recipe): String =
        listOf(
            recipe.name.replace("|", " "),
            recipe.toolId,
            recipe.config.entries.joinToString(",") { it.key.replace(",", " ") + "=" + it.value.replace(",", " ") }
        ).joinToString("|")

    private fun decode(value: String): Recipe? {
        val p = value.split("|", limit = 3)
        if (p.size < 3) return null
        val cfg = p[2].split(",").mapNotNull {
            val i = it.indexOf("=")
            if (i < 0) null else it.substring(0, i) to it.substring(i + 1)
        }.toMap()
        return Recipe(p[0], p[1], cfg)
    }
}
