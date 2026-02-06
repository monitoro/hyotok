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
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.ComposeView
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
import com.silverpixelism.hyotok.webrtc.SignalingClient

class OverlayService : Service(), LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner {

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val store = ViewModelStore()
    private val savedStateRegistryController = SavedStateRegistryController.create(this)

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry
    override val viewModelStore: ViewModelStore get() = store

    private lateinit var windowManager: WindowManager
    private var overlayView: ComposeView? = null
    private lateinit var signalingClient: SignalingClient

    override fun onCreate() {
        super.onCreate()
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)

        val pairingCode = intent?.getStringExtra("pairingCode") ?: return START_NOT_STICKY
        
        if (!::signalingClient.isInitialized) {
            signalingClient = SignalingClient(pairingCode)
            
            // Handle disconnect signal
            signalingClient.onDisconnectReceived = {
                stopSelf()
            }
            
            setupOverlay()
            signalingClient.startListening()
        }

        return START_STICKY
    }

    private fun setupOverlay() {
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        )
        params.gravity = Gravity.TOP or Gravity.START

        overlayView = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@OverlayService)
            setViewTreeViewModelStoreOwner(this@OverlayService)
            setViewTreeSavedStateRegistryOwner(this@OverlayService)
            setContent {
                OverlayContent(signalingClient)
            }
        }

        windowManager.addView(overlayView, params)
    }

    override fun onDestroy() {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        viewModelStore.clear()
        if (overlayView != null) {
            windowManager.removeView(overlayView)
        }
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}

@Composable
fun OverlayContent(signalingClient: SignalingClient) {
    var pointerX by remember { mutableStateOf(-100f) }
    var pointerY by remember { mutableStateOf(-100f) }
    
    // Animate opacity for fade out
    val alphaAnim = remember { Animatable(0f) }

    DisposableEffect(Unit) {
        signalingClient.onTouchReceived = { x, y ->
            // Keep in mind x, y are normalized (0.0 - 1.0)
            pointerX = x
            pointerY = y
            
            // Perform actual tap via AccessibilityService
            RemoteControlService.instance?.performTapNormalized(x, y)
        }
        onDispose {}
    }
    
    // Trigger animation when coordinates change
    LaunchedEffect(pointerX, pointerY) {
        if (pointerX >= 0 && pointerY >= 0) {
            alphaAnim.snapTo(1f)
            alphaAnim.animateTo(0f, animationSpec = tween(1500))
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (alphaAnim.value > 0f) {
            PointerView(
                x = pointerX, 
                y = pointerY, 
                alpha = alphaAnim.value
            )
        }
    }
}

@Composable
fun PointerView(x: Float, y: Float, alpha: Float) {
    // Infinite Animation for ripple
    val infiniteTransition = rememberInfiniteTransition()
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        )
    )

    val pointerSize = 60.dp
    val density = androidx.compose.ui.platform.LocalDensity.current
    val pointerSizePx = with(density) { pointerSize.toPx() }

    // Use BoxWithConstraints to get actual available size
    androidx.compose.foundation.layout.BoxWithConstraints(
        modifier = Modifier.fillMaxSize()
    ) {
        val maxWidthPx = constraints.maxWidth.toFloat()
        val maxHeightPx = constraints.maxHeight.toFloat()
        
        // Calculate position (centered on touch point)
        val posX = (x * maxWidthPx - pointerSizePx / 2).toInt()
        val posY = (y * maxHeightPx - pointerSizePx / 2).toInt()

        Box(
            modifier = Modifier.offset { IntOffset(posX, posY) }
        ) {
            Canvas(modifier = Modifier.size(pointerSize)) {
                // Draw Ripple
                drawCircle(
                    color = Color.Red.copy(alpha = alpha * 0.5f),
                    radius = size.minDimension / 2 * scale,
                    style = Stroke(width = 4.dp.toPx())
                )
                // Draw Center
                drawCircle(
                    color = Color.Red.copy(alpha = alpha),
                    radius = size.minDimension / 4
                )
            }
        }
    }
}
