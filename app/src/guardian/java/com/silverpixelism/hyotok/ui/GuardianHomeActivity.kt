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
            GuardianVideoScreen(pairingCode)
        }
    }
}

@Composable
fun GuardianVideoScreen(pairingCode: String) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val eglBaseContext = remember { org.webrtc.EglBase.create().eglBaseContext }
    val signalingClient = remember { com.silverpixelism.hyotok.webrtc.SignalingClient(pairingCode) }
    val webRTCClient = remember { com.silverpixelism.hyotok.webrtc.WebRTCClient(context, eglBaseContext, signalingClient) }

    // setup View
    androidx.compose.ui.viewinterop.AndroidView(
        factory = { ctx ->
            org.webrtc.SurfaceViewRenderer(ctx).apply {
                init(eglBaseContext, null)
                setScalingType(org.webrtc.RendererCommon.ScalingType.SCALE_ASPECT_FIT)
                setMirror(false)
                setEnableHardwareScaler(true)
                
                // Start connection
                webRTCClient.createPeerConnection(object : org.webrtc.PeerConnection.Observer {
                     override fun onIceCandidate(candidate: org.webrtc.IceCandidate?) {
                         candidate?.let { signalingClient.sendIceCandidate(it) }
                     }
                     override fun onAddStream(stream: org.webrtc.MediaStream?) {
                         stream?.videoTracks?.get(0)?.addSink(this@apply)
                     }
                     override fun onIceConnectionChange(newState: org.webrtc.PeerConnection.IceConnectionState?) {}
                     override fun onDataChannel(p0: org.webrtc.DataChannel?) {}
                     override fun onIceConnectionReceivingChange(p0: Boolean) {}
                     override fun onIceGatheringChange(p0: org.webrtc.PeerConnection.IceGatheringState?) {}
                     override fun onRemoveStream(p0: org.webrtc.MediaStream?) {}
                     override fun onSignalingChange(p0: org.webrtc.PeerConnection.SignalingState?) {}
                     override fun onRenegotiationNeeded() {}
                     override fun onAddTrack(p0: org.webrtc.RtpReceiver?, p1: Array<out org.webrtc.MediaStream>?) {}
                     override fun onIceCandidatesRemoved(p0: Array<out org.webrtc.IceCandidate>?) {}
                })
                
                // Wiring Signaling
                signalingClient.onOfferReceived = { offerSdp ->
                     webRTCClient.answerOffer(offerSdp)
                }
                signalingClient.onIceCandidateReceived = { candidate ->
                     webRTCClient.addIceCandidate(candidate)
                }
            }
        },
        modifier = Modifier.fillMaxSize()
    )
}
