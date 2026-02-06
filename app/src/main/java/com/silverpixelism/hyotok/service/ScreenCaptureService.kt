package com.silverpixelism.hyotok.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.silverpixelism.hyotok.R
import com.silverpixelism.hyotok.webrtc.SignalingClient
import com.silverpixelism.hyotok.webrtc.WebRTCClient
import org.webrtc.EglBase

class ScreenCaptureService : Service() {

    private lateinit var webRTCClient: WebRTCClient
    private lateinit var signalingClient: SignalingClient
    private var eglBase: EglBase? = null

    override fun onCreate() {
        super.onCreate()
        startForegroundService()
        
        eglBase = EglBase.create()
        signalingClient = SignalingClient()
        webRTCClient = WebRTCClient(this, eglBase!!.eglBaseContext, signalingClient)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.let {
            if (it.hasExtra("permissionIntent")) {
                val permissionIntent = if (Build.VERSION.SDK_INT >= 33) {
                     it.getParcelableExtra("permissionIntent", Intent::class.java)!!
                } else {
                     @Suppress("DEPRECATION")
                     it.getParcelableExtra("permissionIntent")!!
                }
                
                // 1. Start Capture (Create local track)
                webRTCClient.startScreenCapture(permissionIntent)
                
                // 2. Create Peer Connection
                webRTCClient.createPeerConnection(object : org.webrtc.PeerConnection.Observer {
                     override fun onIceCandidate(candidate: org.webrtc.IceCandidate?) {
                         candidate?.let { signalingClient.sendIceCandidate(it) }
                     }
                     override fun onIceConnectionChange(newState: org.webrtc.PeerConnection.IceConnectionState?) {}
                     override fun onDataChannel(p0: org.webrtc.DataChannel?) {}
                     override fun onIceConnectionReceivingChange(p0: Boolean) {}
                     override fun onIceGatheringChange(p0: org.webrtc.PeerConnection.IceGatheringState?) {}
                     override fun onAddStream(p0: org.webrtc.MediaStream?) {}
                     override fun onRemoveStream(p0: org.webrtc.MediaStream?) {}
                     override fun onSignalingChange(p0: org.webrtc.PeerConnection.SignalingState?) {}
                     override fun onRenegotiationNeeded() {}
                     override fun onAddTrack(p0: org.webrtc.RtpReceiver?, p1: Array<out org.webrtc.MediaStream>?) {}
                     override fun onIceCandidatesRemoved(p0: Array<out org.webrtc.IceCandidate>?) {}
                })
                
                // 3. Setup Signaling Callbacks
                signalingClient.onAnswerReceived = { answerSdp ->
                    webRTCClient.onRemoteAnswer(answerSdp)
                }
                signalingClient.onIceCandidateReceived = { candidate ->
                    webRTCClient.addIceCandidate(candidate)
                }
                
                // 4. Send Offer
                webRTCClient.sendOffer()
            }
        }
        return START_STICKY
    }

    private fun startForegroundService() {
        val channelId = "screen_capture_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Screen Capture",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("화면 공유 중")
            .setContentText("현재 화면을 공유하고 있습니다.")
            .setSmallIcon(android.R.drawable.ic_dialog_info) // Using android default for now
            .build()
            
        startForeground(1, notification)
    }

    override fun onDestroy() {
        webRTCClient.close()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
