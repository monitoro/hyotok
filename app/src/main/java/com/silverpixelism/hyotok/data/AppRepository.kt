package com.silverpixelism.hyotok.data

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable

data class AppInfo(
    val name: String,
    val packageName: String,
    val icon: Drawable
)

class AppRepository(private val context: Context) {
    
    fun getInstalledApps(): List<AppInfo> {
        val pm = context.packageManager
        val apps = mutableListOf<AppInfo>()
        
        val intent = android.content.Intent(android.content.Intent.ACTION_MAIN, null)
        intent.addCategory(android.content.Intent.CATEGORY_LAUNCHER)
        
        val unresolvedApps = pm.queryIntentActivities(intent, 0)
        
        for (resolveInfo in unresolvedApps) {
            val packageName = resolveInfo.activityInfo.packageName
            // Exclude our own app
            if (packageName != context.packageName) {
               val name = resolveInfo.loadLabel(pm).toString()
               val icon = resolveInfo.loadIcon(pm)
               apps.add(AppInfo(name, packageName, icon))
            }
        }
        
        return apps.sortedBy { it.name }
    }
}
