package com.cnctech.process.data.prefs

import android.content.Context

enum class LayoutMode {
    List,
    GridLarge,
    GridCompact,
}

object LayoutPrefs {
    private const val PREFS = "cnc_prefs"
    private const val KEY_CATALOG = "catalog_layout_mode"
    private const val KEY_PARTS = "parts_layout_mode"

    fun getCatalogLayout(context: Context): LayoutMode = get(context, KEY_CATALOG)

    fun setCatalogLayout(context: Context, mode: LayoutMode) = set(context, KEY_CATALOG, mode)

    fun getPartsLayout(context: Context): LayoutMode = get(context, KEY_PARTS)

    fun setPartsLayout(context: Context, mode: LayoutMode) = set(context, KEY_PARTS, mode)

    private fun get(context: Context, key: String): LayoutMode {
        val name = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(key, null)
        return name?.let { runCatching { LayoutMode.valueOf(it) }.getOrNull() } ?: LayoutMode.List
    }

    private fun set(context: Context, key: String, mode: LayoutMode) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(key, mode.name)
            .apply()
    }
}
