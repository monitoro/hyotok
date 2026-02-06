package com.silverpixelism.hyotok.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.Intent
import android.graphics.Path
import android.os.Build
import android.util.DisplayMetrics
import android.util.Log
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent

/**
 * Accessibility Service for remote touch control.
 * This service can inject touch events on the parent's device
 * when receiving touch coordinates from the guardian app.
 */
class RemoteControlService : AccessibilityService() {

    companion object {
        private const val TAG = "RemoteControlService"
        
        // Singleton instance for external access
        var instance: RemoteControlService? = null
            private set
        
        fun isRunning(): Boolean = instance != null
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        Log.d(TAG, "Accessibility Service connected")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Not needed for gesture dispatch, but required to override
    }

    override fun onInterrupt() {
        Log.d(TAG, "Accessibility Service interrupted")
    }

    override fun onDestroy() {
        instance = null
        super.onDestroy()
    }

    /**
     * Perform a tap gesture at the given screen coordinates.
     * @param x X coordinate in pixels
     * @param y Y coordinate in pixels
     */
    fun performTap(x: Float, y: Float) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            Log.w(TAG, "Gesture dispatch requires API 24+")
            return
        }

        val path = Path().apply {
            moveTo(x, y)
        }

        val gestureBuilder = GestureDescription.Builder()
        gestureBuilder.addStroke(GestureDescription.StrokeDescription(path, 0, 100))

        val gesture = gestureBuilder.build()
        
        val result = dispatchGesture(gesture, object : GestureResultCallback() {
            override fun onCompleted(gestureDescription: GestureDescription?) {
                Log.d(TAG, "Tap completed at ($x, $y)")
            }

            override fun onCancelled(gestureDescription: GestureDescription?) {
                Log.w(TAG, "Tap cancelled at ($x, $y)")
            }
        }, null)

        if (!result) {
            Log.e(TAG, "Failed to dispatch tap gesture")
        }
    }

    /**
     * Perform a tap at normalized coordinates (0.0 - 1.0).
     * Converts to actual screen pixels internally.
     */
    fun performTapNormalized(normalizedX: Float, normalizedY: Float) {
        val windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        val displayMetrics = DisplayMetrics()
        
        @Suppress("DEPRECATION")
        windowManager.defaultDisplay.getRealMetrics(displayMetrics)
        
        val screenWidth = displayMetrics.widthPixels.toFloat()
        val screenHeight = displayMetrics.heightPixels.toFloat()
        
        val x = normalizedX * screenWidth
        val y = normalizedY * screenHeight
        
        Log.d(TAG, "Normalized ($normalizedX, $normalizedY) -> Pixels ($x, $y) on screen ${screenWidth}x${screenHeight}")
        
        performTap(x, y)
    }
}
