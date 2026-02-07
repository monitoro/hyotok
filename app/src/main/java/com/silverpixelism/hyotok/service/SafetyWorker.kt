package com.silverpixelism.hyotok.service

import android.content.Context
import android.content.Intent
import android.os.BatteryManager
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import android.util.Log

import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.firebase.database.FirebaseDatabase
import com.silverpixelism.hyotok.data.AppPreferences
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SafetyWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val context = applicationContext
        val prefs = AppPreferences(context)
        val pairingCode = prefs.getPairingCode()

        if (pairingCode == null) {
            Log.w("SafetyWorker", "Pairing code not found. Skipping safety check.")
            return Result.success()
        }

        // 1. Check Battery
        val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        val batteryLevel = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        val isCharging = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val status = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_STATUS)
            status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
        } else {
            false // Simplified for older versions
        }

        Log.d("SafetyWorker", "Battery: $batteryLevel%, Charging: $isCharging")

        // 2. Check Location (if permission granted)
        var latitude = 0.0
        var longitude = 0.0
        
        if (androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.ACCESS_COARSE_LOCATION
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            try {
                val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
                // Try to get last location first
                val lastLocation = fusedLocationClient.lastLocation.await()
                if (lastLocation != null) {
                    latitude = lastLocation.latitude
                    longitude = lastLocation.longitude
                } else {
                    // If no last location, request current location (high accuracy for freshness, but coarse perm limits it)
                    val cancellationTokenSource = CancellationTokenSource()
                    val currentLocation = fusedLocationClient.getCurrentLocation(
                        Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                        cancellationTokenSource.token
                    ).await()
                    
                    if (currentLocation != null) {
                        latitude = currentLocation.latitude
                        longitude = currentLocation.longitude
                    }
                }
                Log.d("SafetyWorker", "Location: $latitude, $longitude")
            } catch (e: Exception) {
                Log.e("SafetyWorker", "Failed to get location", e)
            }
        } else {
            Log.d("SafetyWorker", "Location permission not granted")
        }

        // 3. Upload to Firebase
        val database = FirebaseDatabase.getInstance()
        val safetyRef = database.getReference("safety").child(pairingCode)

        val updates = mapOf(
            "batteryLevel" to batteryLevel,
            "isCharging" to isCharging,
            "latitude" to latitude,
            "longitude" to longitude,
            "lastUpdated" to System.currentTimeMillis()
        )

        try {
            safetyRef.updateChildren(updates).await()
            Log.d("SafetyWorker", "Safety data uploaded successfully")
        } catch (e: Exception) {
            Log.e("SafetyWorker", "Failed to upload safety data", e)
            return Result.retry()
        }

        return Result.success()
    }
}
