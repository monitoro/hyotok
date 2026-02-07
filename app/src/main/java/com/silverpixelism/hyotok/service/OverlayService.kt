package com.silverpixelism.hyotok.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.delay

// Trail point with timestamp for fade effect
data class TrailPoint(val x: Float, val y: Float, val timestamp: Long)

class OverlayService : Service(), LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner {

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val store = ViewModelStore()
    private val savedStateRegistryController = SavedStateRegistryController.create(this)

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry
    override val viewModelStore: ViewModelStore get() = store

    private lateinit var windowManager: WindowManager
    private var overlayView: ComposeView? = null
    private var pairingCode: String = ""
    private var pointerListener: ValueEventListener? = null

    // Trail points for drag effect
    private val _trailPoints = mutableStateListOf<TrailPoint>()

    override fun onCreate() {
        super.onCreate()
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)

        pairingCode = intent?.getStringExtra("pairingCode") ?: return START_NOT_STICKY
        
        if (overlayView == null) {
            setupOverlay()
            startListeningForPointer()
        }

        return START_STICKY
    }

    private fun setupOverlay() {
        // Get REAL screen metrics including navigation bar
        val realMetrics = android.util.DisplayMetrics()
        @Suppress("DEPRECATION")
        windowManager.defaultDisplay.getRealMetrics(realMetrics)
        val screenWidth = realMetrics.widthPixels.toFloat()
        val screenHeight = realMetrics.heightPixels.toFloat()
        
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        )
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            params.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }
        
        params.gravity = Gravity.TOP or Gravity.START

        overlayView = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@OverlayService)
            setViewTreeViewModelStoreOwner(this@OverlayService)
            setViewTreeSavedStateRegistryOwner(this@OverlayService)
            setContent {
                OverlayContent(_trailPoints, screenWidth, screenHeight)
            }
        }

        windowManager.addView(overlayView, params)
    }

    private fun startListeningForPointer() {
        val database = FirebaseDatabase.getInstance()
        val pointerRef = database.getReference("pointer").child(pairingCode)
        
        var lastSoundTime = 0L
        
        pointerListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val x = snapshot.child("x").getValue(Float::class.java) ?: return
                val y = snapshot.child("y").getValue(Float::class.java) ?: return
                
                // Add new trail point
                val now = System.currentTimeMillis()
                _trailPoints.add(TrailPoint(x, y, now))
                
                // Play sound effect (throttled to every 300ms)
                if (now - lastSoundTime > 300) {
                    try {
                        val audioManager = getSystemService(Context.AUDIO_SERVICE) as android.media.AudioManager
                        audioManager.playSoundEffect(android.media.AudioManager.FX_KEY_CLICK)
                    } catch (e: Exception) {
                        android.util.Log.e("OverlayService", "Sound error: ${e.message}")
                    }
                    lastSoundTime = now
                }
                
                // Keep only recent points (last 1.5 seconds)
                val cutoff = now - 1500
                _trailPoints.removeAll { it.timestamp < cutoff }
            }
            
            override fun onCancelled(error: DatabaseError) {
                android.util.Log.e("OverlayService", "Pointer listener cancelled: ${error.message}")
            }
        }
        
        pointerRef.addValueEventListener(pointerListener!!)
    }

    override fun onDestroy() {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        viewModelStore.clear()
        
        pointerListener?.let {
            FirebaseDatabase.getInstance()
                .getReference("pointer").child(pairingCode)
                .removeEventListener(it)
        }
        
        if (overlayView != null) {
            windowManager.removeView(overlayView)
            overlayView = null
        }
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}

@Composable
fun OverlayContent(trailPoints: List<TrailPoint>, screenWidth: Float, screenHeight: Float) {
    var currentTime by remember { mutableLongStateOf(System.currentTimeMillis()) }
    
    // Update time every 50ms to animate fade
    LaunchedEffect(Unit) {
        while (true) {
            currentTime = System.currentTimeMillis()
            delay(50)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            if (trailPoints.isEmpty()) return@Canvas

            val now = currentTime
            val trailDuration = 1200L // 1.2초 잔상

            // Draw fading circles for trail effect (잔상)
            trailPoints.forEach { point ->
                val age = now - point.timestamp
                if (age < trailDuration && age > 100) { // 100ms 이후부터 잔상 표시
                    val alpha = (1f - (age.toFloat() / trailDuration)) * 0.6f
                    val scale = 1f - (age.toFloat() / trailDuration) * 0.7f
                    
                    val screenX = point.x * screenWidth
                    val screenY = point.y * screenHeight
                    val baseRadius = 18.dp.toPx()
                    
                    // Trail circle (잔상 원)
                    drawCircle(
                        color = Color.Red.copy(alpha = alpha),
                        radius = baseRadius * scale,
                        center = Offset(screenX, screenY)
                    )
                }
            }

            // Draw current pointer (현재 포인터)
            if (trailPoints.isNotEmpty()) {
                val lastPoint = trailPoints.last()
                val age = now - lastPoint.timestamp
                if (age < 800) { // 0.8초간 포인터 표시
                    val screenX = lastPoint.x * screenWidth
                    val screenY = lastPoint.y * screenHeight
                    val pointerRadius = 20.dp.toPx()
                    
                    // Ripple effect (물결 효과)
                    val rippleAlpha = 0.5f - (age.toFloat() / 800f) * 0.4f
                    val rippleScale = 1f + (age.toFloat() / 800f) * 0.8f
                    drawCircle(
                        color = Color.Red.copy(alpha = rippleAlpha),
                        radius = pointerRadius * rippleScale * 1.5f,
                        center = Offset(screenX, screenY),
                        style = Stroke(width = 4.dp.toPx())
                    )
                    
                    // Main pointer (메인 포인터)
                    drawCircle(
                        color = Color.Red.copy(alpha = 0.9f),
                        radius = pointerRadius,
                        center = Offset(screenX, screenY)
                    )
                    
                    // Center dot (중앙 점)
                    drawCircle(
                        color = Color.White,
                        radius = pointerRadius * 0.3f,
                        center = Offset(screenX, screenY)
                    )
                }
            }
        }
    }
}
