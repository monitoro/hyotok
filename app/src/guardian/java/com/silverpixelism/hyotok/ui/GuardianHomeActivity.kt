package com.silverpixelism.hyotok.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.background
import androidx.compose.runtime.*
import com.silverpixelism.hyotok.webrtc.SignalingClient
import com.silverpixelism.hyotok.webrtc.WebRTCClient
import org.webrtc.SurfaceViewRenderer
import com.silverpixelism.hyotok.ui.theme.HyoTalkTheme
import androidx.compose.ui.unit.dp

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
    // We need to keep clients open
    val signalingClient = remember { com.silverpixelism.hyotok.webrtc.SignalingClient("default") } // Needs dynamic update
    
    // Simplification: We'll re-instantiate signaling client when connecting with code.
    // Ideally use a ViewModel.
    
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp)
    ) {
        Text(
            text = "HyoTalk Guardian",
            style = MaterialTheme.typography.headlineLarge,
            modifier = Modifier.padding(bottom = 32.dp)
        )
        
        if (!isConnected) {
             androidx.compose.material3.TextField(
                 value = pairingCode,
                 onValueChange = { pairingCode = it },
                 label = { Text("부모님 폰의 연결 코드 6자리") },
                 modifier = Modifier.fillMaxWidth()
             )
        
             Button(
                 onClick = {
                     // Connect Logic
                     val signaling = com.silverpixelism.hyotok.webrtc.SignalingClient(pairingCode)
                     // Initialize WebRTC
                     // Ideally start an Activity or switch composable to "VideoScreen"
                     isConnected = true 
                 },
                 modifier = Modifier.padding(top = 24.dp).fillMaxWidth()
             ) {
                 Text("연결하기")
             }
        } else {
            // Video Render Screen
            GuardianVideoScreen(
                pairingCode = pairingCode,
                onDisconnect = { isConnected = false }
            )
        }
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun GuardianVideoScreen(pairingCode: String, onDisconnect: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val eglBaseContext = remember { org.webrtc.EglBase.create().eglBaseContext }
    
    // Status State
    var connectionStatus by remember { mutableStateOf("초기화 중...") }
    
    val signalingClient = remember { com.silverpixelism.hyotok.webrtc.SignalingClient(pairingCode) }
    val webRTCClient = remember { com.silverpixelism.hyotok.webrtc.WebRTCClient(context, eglBaseContext, signalingClient) }
    
    // SurfaceViewRenderer reference for cleanup
    var surfaceViewRenderer by remember { mutableStateOf<org.webrtc.SurfaceViewRenderer?>(null) }

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

    androidx.compose.material3.Scaffold(
        topBar = {
            androidx.compose.material3.TopAppBar(
                title = { Text("화면 보기", color = androidx.compose.ui.graphics.Color.White) },
                colors = androidx.compose.material3.TopAppBarDefaults.topAppBarColors(
                    containerColor = androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.7f)
                ),
                actions = {
                    androidx.compose.material3.TextButton(
                        onClick = {
                            // Send disconnect signal to Parent app FIRST
                            signalingClient.sendDisconnect()
                            signalingClient.cleanup()
                            webRTCClient.close()
                            onDisconnect()
                        }
                    ) {
                        Text("연결 끊기", color = androidx.compose.ui.graphics.Color.Red)
                    }
                }
            )
        },
        containerColor = androidx.compose.ui.graphics.Color.Black
    ) { padding ->
        androidx.compose.foundation.layout.Box(
            modifier = Modifier.fillMaxSize().padding(padding)
        ) {
            // Video View
            androidx.compose.ui.viewinterop.AndroidView(
                factory = { ctx ->
                    org.webrtc.SurfaceViewRenderer(ctx).apply {
                        surfaceViewRenderer = this
                        init(eglBaseContext, null)
                        setScalingType(org.webrtc.RendererCommon.ScalingType.SCALE_ASPECT_FIT)
                        setMirror(false)
                        setEnableHardwareScaler(true)
                        setZOrderMediaOverlay(false) // Ensure video doesn't cover UI
                        
                        // Touch control - send touch coordinates to parent phone
                        setOnTouchListener { v, event ->
                            when (event.action) {
                                android.view.MotionEvent.ACTION_DOWN,
                                android.view.MotionEvent.ACTION_MOVE -> {
                                    // Normalize coordinates to 0.0 ~ 1.0
                                    val normalizedX = event.x / v.width
                                    val normalizedY = event.y / v.height
                                    signalingClient.sendTouch(normalizedX, normalizedY)
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
            
            // Status Overlay at bottom
            androidx.compose.foundation.layout.Box(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                contentAlignment = androidx.compose.ui.Alignment.BottomCenter
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
