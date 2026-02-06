package com.silverpixelism.hyotok.service

import android.content.Context
import android.content.Intent
import android.os.BatteryManager
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import android.util.Log

class SafetyWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val batteryManager = applicationContext.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        val batteryLevel = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)

        Log.d("SafetyWorker", "Current battery level: $batteryLevel%")

        if (batteryLevel < 20) {
            // In a real app, send this data to Firebase or Child's device
            Log.w("SafetyWorker", "Low battery! Notify child.")
            // Using a broadcast or just simulation for now
        }
        
        // Simulating activity check (e.g., screen on/off stats)
        
        return Result.success()
    }
}
