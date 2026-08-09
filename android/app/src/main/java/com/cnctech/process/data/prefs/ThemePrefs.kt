package com.cnctech.process.data.prefs

import android.content.Context
import com.cnctech.process.ui.theme.ThemeVariant

object ThemePrefs {
    private const val PREFS = "cnc_prefs"
    private const val KEY = "theme_variant"

    fun get(context: Context): ThemeVariant {
        val name = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, null)
        return name?.let { runCatching { ThemeVariant.valueOf(it) }.getOrNull() } ?: ThemeVariant.Light
    }

    fun set(context: Context, variant: ThemeVariant) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY, variant.name)
            .apply()
    }
}
