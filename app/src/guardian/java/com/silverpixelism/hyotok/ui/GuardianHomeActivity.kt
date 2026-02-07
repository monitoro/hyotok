package com.silverpixelism.hyotok.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.background
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.silverpixelism.hyotok.webrtc.SignalingClient
import com.silverpixelism.hyotok.webrtc.WebRTCClient
import org.webrtc.SurfaceViewRenderer
import com.silverpixelism.hyotok.ui.theme.HyoTalkTheme

class GuardianHomeActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            HyoTalkTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    GuardianHomeScreen()
                }
            }
        }
    }
}

@Composable
fun GuardianHomeScreen() {
    val context = androidx.compose.ui.platform.LocalContext.current
    var pairingCode by remember { mutableStateOf("") }
    var isConnected by remember { mutableStateOf(false) }
    val signalingClient = remember { mutableStateOf<com.silverpixelism.hyotok.webrtc.SignalingClient?>(null) }
    
    Column(
        modifier = Modifier.fillMaxSize().background(androidx.compose.ui.graphics.Color(0xFF1A1F36))
    ) {
        // Header - Always visible
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(androidx.compose.ui.graphics.Color(0xFF1A1F36))
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "효도폰(자녀용)",
                style = MaterialTheme.typography.titleLarge,
                color = androidx.compose.ui.graphics.Color.White
            )
            
            if (isConnected) {
                // Disconnect button only
                androidx.compose.material3.TextButton(
                    onClick = {
                        signalingClient.value?.sendDisconnect()
                        signalingClient.value?.cleanup()
                        isConnected = false
                    }
                ) {
                    Text("연결 끊기", color = androidx.compose.ui.graphics.Color.Red)
                }
            }
        }
        
        if (!isConnected) {
            // Connection UI
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                androidx.compose.material3.TextField(
                    value = pairingCode,
                    onValueChange = { pairingCode = it },
                    label = { Text("부모님 폰의 연결 코드 6자리") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
           
                Spacer(modifier = Modifier.height(24.dp))
           
                Button(
                    onClick = {
                        if (pairingCode.isNotEmpty()) {
                            val database = com.google.firebase.database.FirebaseDatabase.getInstance()
                            database.getReference("users").child(pairingCode).setValue("connected")
                                .addOnSuccessListener {
                                    android.widget.Toast.makeText(context, "연결 성공!", android.widget.Toast.LENGTH_SHORT).show()
                                    isConnected = true
                                }
                                .addOnFailureListener { e ->
                                    android.widget.Toast.makeText(context, "연결 실패: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
                                }
                        } else {
                            android.widget.Toast.makeText(context, "코드를 입력하세요", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
                ) {
                    Text("연결하기", style = MaterialTheme.typography.titleMedium)
                }
            }
        } else {
            // Full screen video - NO extra padding, NO labels
            GuardianVideoScreen(
                pairingCode = pairingCode,
                onDisconnect = { isConnected = false },
                onSignalingClient = { signalingClient.value = it }
            )
        }
    }
}

@Composable
fun GuardianVideoScreen(
    pairingCode: String, 
    onDisconnect: () -> Unit,
    onSignalingClient: (com.silverpixelism.hyotok.webrtc.SignalingClient) -> Unit = {}
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val eglBaseContext = remember { org.webrtc.EglBase.create().eglBaseContext }
    
    // Status State
    var connectionStatus by remember { mutableStateOf("초기화 중...") }
    
    val signalingClient = remember { com.silverpixelism.hyotok.webrtc.SignalingClient(pairingCode) }
    val webRTCClient = remember { com.silverpixelism.hyotok.webrtc.WebRTCClient(context, eglBaseContext, signalingClient) }
    
    // Remote screen size state for coordinate correction
    var remoteWidth by remember { mutableFloatStateOf(0f) }
    var remoteHeight by remember { mutableFloatStateOf(0f) }

    androidx.compose.runtime.DisposableEffect(pairingCode) {
        val database = com.google.firebase.database.FirebaseDatabase.getInstance()
        val sizeRef = database.getReference("sessions").child(pairingCode).child("screenSize")
        val listener = object : com.google.firebase.database.ValueEventListener {
             override fun onDataChange(snapshot: com.google.firebase.database.DataSnapshot) {
                 val w = snapshot.child("width").getValue(Int::class.java)
                 val h = snapshot.child("height").getValue(Int::class.java)
                 if (w != null && h != null) {
                     remoteWidth = w.toFloat()
                     remoteHeight = h.toFloat()
                 }
             }
             override fun onCancelled(error: com.google.firebase.database.DatabaseError) {}
        }
        sizeRef.addValueEventListener(listener)
        onDispose {
            sizeRef.removeEventListener(listener)
        }
    }
    
    // SurfaceViewRenderer reference for cleanup
    var surfaceViewRenderer by remember { mutableStateOf<org.webrtc.SurfaceViewRenderer?>(null) }

    // Expose signalingClient to parent
    LaunchedEffect(signalingClient) {
        onSignalingClient(signalingClient)
    }

    // Cleanup on dispose
    androidx.compose.runtime.DisposableEffect(Unit) {
        onDispose {
            signalingClient.cleanup()
            webRTCClient.close()
            surfaceViewRenderer?.release()
        }
    }

    LaunchedEffect(Unit) {
        // Wiring Signaling
        connectionStatus = "신호 대기 중..."
        signalingClient.onOfferReceived = { offerSdp ->
             connectionStatus = "신호 수신! 연결 시도 중..."
             webRTCClient.answerOffer(offerSdp)
        }
        signalingClient.onIceCandidateReceived = { candidate ->
             webRTCClient.addIceCandidate(candidate)
        }
        
        // Start Listening AFTER wiring
        signalingClient.startListening()
    }

    // Full screen video - NO TopAppBar, NO extra padding
    androidx.compose.foundation.layout.Box(
        modifier = Modifier
            .fillMaxSize()
            .background(androidx.compose.ui.graphics.Color.Black)
    ) {
        // Video View - Full screen with touch detection
        androidx.compose.ui.viewinterop.AndroidView(
            factory = { ctx ->
                org.webrtc.SurfaceViewRenderer(ctx).apply {
                    surfaceViewRenderer = this
                    init(eglBaseContext, null)
                    setScalingType(org.webrtc.RendererCommon.ScalingType.SCALE_ASPECT_FIT)
                    setMirror(false)
                    setEnableHardwareScaler(true)
                    setZOrderMediaOverlay(false)
                    
                    // Touch listener - send coordinates to Firebase for pointer display
                    val database = com.google.firebase.database.FirebaseDatabase.getInstance()
                    val pointerRef = database.getReference("pointer").child(pairingCode)
                    
                    var lastUpdateTime = 0L
                    
                    setOnTouchListener { v, event ->
                        when (event.action) {
                            android.view.MotionEvent.ACTION_DOWN,
                            android.view.MotionEvent.ACTION_MOVE -> {
                                val now = System.currentTimeMillis()
                                // Throttle updates to every 30ms for stability (~33fps)
                                if (now - lastUpdateTime > 30) {
                                    val viewWidth = v.width.toFloat()
                                    val viewHeight = v.height.toFloat()
                                    
                                    var normalizedX = 0f
                                    var normalizedY = 0f
                                    var isValid = false
                                    
                                    if (remoteWidth > 0 && remoteHeight > 0) {
                                        // Calculate Aspect Fit rect
                                        val remoteRatio = remoteWidth / remoteHeight
                                        val viewRatio = viewWidth / viewHeight
                                        
                                        var renderWidth = viewWidth
                                        var renderHeight = viewHeight
                                        var offsetX = 0f
                                        var offsetY = 0f
                                        
                                        if (viewRatio > remoteRatio) {
                                            // View is wider than video -> fit height, pillarbox (left/right bars)
                                            renderWidth = viewHeight * remoteRatio
                                            offsetX = (viewWidth - renderWidth) / 2f
                                        } else {
                                            // View is taller than video -> fit width, letterbox (top/bottom bars)
                                            renderHeight = viewWidth / remoteRatio
                                            offsetY = (viewHeight - renderHeight) / 2f
                                        }
                                        
                                        // Touch coordinate relative to video rect
                                        val touchX = event.x - offsetX
                                        val touchY = event.y - offsetY
                                        
                                        // Check bounds
                                        if (touchX >= 0 && touchX <= renderWidth && touchY >= 0 && touchY <= renderHeight) {
                                             normalizedX = touchX / renderWidth
                                             normalizedY = touchY / renderHeight
                                             isValid = true
                                        }
                                    } else {
                                        // Fallback: simple normalization if no remote size info
                                        normalizedX = event.x / viewWidth
                                        normalizedY = event.y / viewHeight
                                        isValid = true
                                    }
                                    
                                    if (isValid) {
                                        pointerRef.setValue(mapOf("x" to normalizedX, "y" to normalizedY))
                                        lastUpdateTime = now
                                    }
                                }
                                true
                            }
                            else -> false
                        }
                    }
                    
                    // Start connection
                    webRTCClient.createPeerConnection(object : org.webrtc.PeerConnection.Observer {
                         override fun onIceCandidate(candidate: org.webrtc.IceCandidate?) {
                             candidate?.let { signalingClient.sendIceCandidate(it) }
                         }
                         override fun onAddStream(stream: org.webrtc.MediaStream?) {
                             connectionStatus = "화면 수신 중!"
                             stream?.videoTracks?.get(0)?.addSink(this@apply)
                         }
                         override fun onIceConnectionChange(newState: org.webrtc.PeerConnection.IceConnectionState?) {
                             if (newState == org.webrtc.PeerConnection.IceConnectionState.CONNECTED) {
                                 connectionStatus = "연결됨"
                             } else if (newState == org.webrtc.PeerConnection.IceConnectionState.DISCONNECTED ||
                                        newState == org.webrtc.PeerConnection.IceConnectionState.FAILED) {
                                 connectionStatus = "연결 끊김"
                             }
                         }
                         override fun onDataChannel(p0: org.webrtc.DataChannel?) {}
                         override fun onIceConnectionReceivingChange(p0: Boolean) {}
                         override fun onIceGatheringChange(p0: org.webrtc.PeerConnection.IceGatheringState?) {}
                         override fun onRemoveStream(p0: org.webrtc.MediaStream?) {
                             connectionStatus = "화면 공유 종료됨"
                         }
                         override fun onSignalingChange(p0: org.webrtc.PeerConnection.SignalingState?) {}
                         override fun onRenegotiationNeeded() {}
                         override fun onAddTrack(p0: org.webrtc.RtpReceiver?, p1: Array<out org.webrtc.MediaStream>?) {}
                         override fun onIceCandidatesRemoved(p0: Array<out org.webrtc.IceCandidate>?) {}
                    })
                }
            },
            modifier = Modifier.fillMaxSize()
        )
        
        // Status Overlay at bottom (only show when not connected)
        if (connectionStatus != "연결됨") {
            androidx.compose.foundation.layout.Box(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                contentAlignment = Alignment.BottomCenter
            ) {
                Text(
                    text = connectionStatus,
                    style = MaterialTheme.typography.titleMedium,
                    color = androidx.compose.ui.graphics.Color.White,
                    modifier = Modifier.background(
                        androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.5f), 
                        androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
                    ).padding(8.dp)
                )
            }
        }
    }
}
