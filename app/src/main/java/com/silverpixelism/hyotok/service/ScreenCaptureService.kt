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
                
                val pairingCode = it.getStringExtra("pairingCode") ?: "test_code"

                // Send Screen Size to Firebase for coordinate normalization in Guardian App
                val windowManager = getSystemService(Context.WINDOW_SERVICE) as android.view.WindowManager
                val realMetrics = android.util.DisplayMetrics()
                @Suppress("DEPRECATION")
                windowManager.defaultDisplay.getRealMetrics(realMetrics)
                val screenWidth = realMetrics.widthPixels
                val screenHeight = realMetrics.heightPixels
                
                com.google.firebase.database.FirebaseDatabase.getInstance()
                    .getReference("sessions").child(pairingCode).child("screenSize")
                    .setValue(mapOf("width" to screenWidth, "height" to screenHeight))

                // Initialize clients here with the code
                if (!::signalingClient.isInitialized) {
                    signalingClient = SignalingClient(pairingCode)
                    signalingClient.onError = { errorMsg ->
                        updateNotification("Connection Error", errorMsg)
                    }
                }
                
                if (!::webRTCClient.isInitialized) {
                    webRTCClient = WebRTCClient(this, eglBase!!.eglBaseContext, signalingClient)
                    webRTCClient.onError = { errorMsg ->
                        updateNotification("WebRTC Error", errorMsg)
                    }
                }

                // 1. Start Capture (Create local track)
                webRTCClient.startScreenCapture(permissionIntent)
                webRTCClient.startAudioCapture() // Add Audio
                
                // 2. Create Peer Connection
                webRTCClient.createPeerConnection(object : org.webrtc.PeerConnection.Observer {
                     override fun onIceCandidate(candidate: org.webrtc.IceCandidate?) {
                         candidate?.let { signalingClient.sendIceCandidate(it) }
                     }
                     override fun onIceConnectionChange(newState: org.webrtc.PeerConnection.IceConnectionState?) {
                        android.util.Log.d("ScreenCaptureService", "ICE Connection state: $newState")
                        when (newState) {
                            org.webrtc.PeerConnection.IceConnectionState.DISCONNECTED,
                            org.webrtc.PeerConnection.IceConnectionState.FAILED,
                            org.webrtc.PeerConnection.IceConnectionState.CLOSED -> {
                                android.util.Log.d("ScreenCaptureService", "Connection lost, stopping service")
                                stopSelf()
                            }
                            else -> {}
                        }
                     }
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
                
                // 4. Setup Signaling Callbacks
                signalingClient.onAnswerReceived = { answerSdp ->
                    webRTCClient.onRemoteAnswer(answerSdp)
                }
                signalingClient.onIceCandidateReceived = { candidate ->
                    webRTCClient.addIceCandidate(candidate)
                }
                
                // Handle disconnect signal from Guardian app
                signalingClient.onDisconnectReceived = {
                    android.util.Log.d("ScreenCaptureService", "Disconnect signal received, stopping service")
                    stopSelf()
                }
                
                // Start Listening Now
                signalingClient.startListening()
                
                // 5. Send Offer
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

    private fun updateNotification(title: String, content: String) {
        val notification = NotificationCompat.Builder(this, "screen_capture_channel")
            .setContentTitle(title)
            .setContentText(content)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .build()
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(1, notification)
    }

    override fun onDestroy() {
        // Cleanup signaling data for fresh reconnection
        if (::signalingClient.isInitialized) {
            signalingClient.cleanup()
        }
        if (::webRTCClient.isInitialized) {
            webRTCClient.close()
        }
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
