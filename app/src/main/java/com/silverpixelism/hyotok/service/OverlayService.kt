package com.silverpixelism.hyotok.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.compose.ui.platform.ComposeView
// import androidx.lifecycle.ViewTreeLifecycleOwner // Commented out to fix build

class OverlayService : Service() {
    
    override fun onCreate() {
        super.onCreate()
        // Stub implementation for build pass
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
