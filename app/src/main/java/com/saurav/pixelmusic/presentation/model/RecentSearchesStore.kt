package com.saurav.pixelmusic.presentation.model

import android.content.Context

/**
 * Lightweight persistent store for the last 5 settings-search queries.
 * Uses SharedPreferences because the payload is tiny and we want zero async complexity.
 */
object RecentSearchesStore {
    private const val PREFS_NAME = "settings_search_recent"
    private const val KEY = "queries"
    private const val SEPARATOR = "\u0001"
    private const val MAX = 5

    private fun prefs(ctx: Context) =
        ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun load(ctx: Context): List<String> {
        val raw = prefs(ctx).getString(KEY, null) ?: return emptyList()
        if (raw.isBlank()) return emptyList()
        return raw.split(SEPARATOR).filter { it.isNotBlank() }.take(MAX)
    }

    fun add(ctx: Context, query: String): List<String> {
        val q = query.trim()
        if (q.isEmpty()) return load(ctx)
        val current = load(ctx).toMutableList()
        current.removeAll { it.equals(q, ignoreCase = true) }
        current.add(0, q)
        while (current.size > MAX) current.removeAt(current.size - 1)
        prefs(ctx).edit().putString(KEY, current.joinToString(SEPARATOR)).apply()
        return current
    }

    fun clear(ctx: Context) {
        prefs(ctx).edit().remove(KEY).apply()
    }
}
