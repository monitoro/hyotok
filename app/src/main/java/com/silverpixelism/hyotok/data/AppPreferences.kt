package com.silverpixelism.hyotok.data

import android.content.Context
import android.content.SharedPreferences

class AppPreferences(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("hyotok_prefs", Context.MODE_PRIVATE)

    fun getSelectedApps(): Set<String> {
        return prefs.getStringSet("selected_apps", emptySet()) ?: emptySet()
    }

    fun saveSelectedApps(packageNames: Set<String>) {
        prefs.edit().putStringSet("selected_apps", packageNames).apply()
    }
}
