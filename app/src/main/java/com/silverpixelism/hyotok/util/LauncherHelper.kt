package com.silverpixelism.hyotok.util

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings

object LauncherHelper {
    fun isDefaultLauncher(context: Context): Boolean {
        val intent = Intent(Intent.ACTION_MAIN)
        intent.addCategory(Intent.CATEGORY_HOME)
        val resolveInfo = context.packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
        val currentLauncherPackage = resolveInfo?.activityInfo?.packageName
        return currentLauncherPackage == context.packageName
    }

    fun openHomeSettings(context: Context) {
        val intent = Intent(Settings.ACTION_HOME_SETTINGS)
        // Fallback or specific handling could be added here if needed
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            // Some devices might not support this directly, fallback to app settings
            val settingsIntent = Intent(Settings.ACTION_SETTINGS)
            context.startActivity(settingsIntent)
        }
    }
}
