package com.silverpixelism.hyotok.ui

import android.view.MotionEvent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.silverpixelism.hyotok.webrtc.SignalingClient
import com.silverpixelism.hyotok.webrtc.WebRTCClient
import org.webrtc.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StreamViewerScreen(
    pairingCode: String,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var statusMessage by remember { mutableStateOf("연결 초기화 중...") }

    // WebRTC & Signaling
    val signalingClient = remember { SignalingClient(pairingCode) }
    val eglBaseContext = remember { EglBase.create().eglBaseContext } 
    val webRTCClient = remember { WebRTCClient(context, eglBaseContext, signalingClient) }

    // SurfaceViewRenderer for remote video - SIMPLE SETUP
    val remoteVideoView = remember { 
        SurfaceViewRenderer(context).apply {
            setMirror(false)
            init(eglBaseContext, null)
            setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FIT)
            setEnableHardwareScaler(true)
            // Touch for control
            setOnTouchListener { v, event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                        val x = event.x / v.width
                        val y = event.y / v.height
                        signalingClient.sendTouch(x, y)
                        true
                    }
                    else -> false
                }
            }
        }
    }

    // Handle Android back button
    BackHandler {
        Toast.makeText(context, "연결을 종료합니다", Toast.LENGTH_SHORT).show()
        onBack()
    }

    // Show initial Toast
    LaunchedEffect(Unit) {
        Toast.makeText(context, "화면을 터치하여 제어하세요", Toast.LENGTH_LONG).show()
    }

    DisposableEffect(Unit) {
        // Setup Signaling
        signalingClient.onError = { 
            statusMessage = "Error: $it"
        }
        signalingClient.onOfferReceived = { sdp ->
            statusMessage = "Offer 수신됨"
            webRTCClient.answerOffer(sdp)
        }
        signalingClient.onIceCandidateReceived = { webRTCClient.addIceCandidate(it) }

        // Setup WebRTC
        webRTCClient.onError = { statusMessage = "WebRTC Error: $it" }
        
        // Setup PeerConnection Observer
        webRTCClient.createPeerConnection(object : PeerConnection.Observer {
            override fun onIceCandidate(p0: IceCandidate?) {
                p0?.let { signalingClient.sendIceCandidate(it) }
            }
            override fun onDataChannel(p0: DataChannel?) {}
            override fun onIceConnectionReceivingChange(p0: Boolean) {}
            override fun onIceConnectionChange(p0: PeerConnection.IceConnectionState?) {
                if (p0 == PeerConnection.IceConnectionState.CONNECTED) {
                    statusMessage = "연결됨!"
                }
            }
            override fun onIceGatheringChange(p0: PeerConnection.IceGatheringState?) {}
            override fun onAddStream(p0: MediaStream?) {
                p0?.videoTracks?.firstOrNull()?.addSink(remoteVideoView)
            }
            override fun onSignalingChange(p0: PeerConnection.SignalingState?) {}
            override fun onIceCandidatesRemoved(p0: Array<out IceCandidate>?) {}
            override fun onRemoveStream(p0: MediaStream?) {}
            override fun onRenegotiationNeeded() {}
            override fun onAddTrack(p0: RtpReceiver?, p1: Array<out MediaStream>?) {}
        })
        
        // Start Audio Capture
        webRTCClient.startAudioCapture()
        
        // Start Listening
        signalingClient.startListening()
        
        onDispose {
            signalingClient.cleanup()
            webRTCClient.close()
            remoteVideoView.release()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("화면 공유") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "뒤로가기")
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            AndroidView(
                factory = { remoteVideoView },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
