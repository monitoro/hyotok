package com.silverpixelism.hyotok.webrtc

import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjection
import android.util.Log
import org.json.JSONObject
import org.webrtc.*
import java.nio.ByteBuffer
import java.nio.charset.Charset

class WebRTCClient(
    private val context: Context,
    private val eglBaseContext: EglBase.Context,
    private val signalingClient: SignalingClient
) {
    companion object {
        private const val TAG = "WebRTCClient"
        private const val DATA_CHANNEL_NAME = "touch_channel"
    }

    private val peerConnectionFactory: PeerConnectionFactory
    private var peerConnection: PeerConnection? = null
    private var localVideoTrack: VideoTrack? = null
    private var localAudioTrack: AudioTrack? = null
    val rootEglBase: EglBase = EglBase.create(eglBaseContext)
    
    // DataChannel for touch events
    private var dataChannel: DataChannel? = null
    
    var onError: ((String) -> Unit)? = null
    
    // Callback for receiving touch data via DataChannel
    var onTouchReceived: ((Float, Float) -> Unit)? = null

    init {
        val options = PeerConnectionFactory.InitializationOptions.builder(context)
            .setEnableInternalTracer(true)
            .setFieldTrials("WebRTC-H264HighProfile/Enabled/")
            .createInitializationOptions()
        PeerConnectionFactory.initialize(options)

        val defaultVideoEncoderFactory = DefaultVideoEncoderFactory(rootEglBase.eglBaseContext, true, true)
        val defaultVideoDecoderFactory = DefaultVideoDecoderFactory(rootEglBase.eglBaseContext)

        peerConnectionFactory = PeerConnectionFactory.builder()
            .setOptions(PeerConnectionFactory.Options())
            .setVideoEncoderFactory(defaultVideoEncoderFactory)
            .setVideoDecoderFactory(defaultVideoDecoderFactory)
            .createPeerConnectionFactory()
    }

    fun startScreenCapture(permissionIntent: Intent) {
        val videoCapturer = createScreenCapturer(permissionIntent)
        
        val videoSource = peerConnectionFactory.createVideoSource(videoCapturer.isScreencast)
        videoCapturer.initialize(SurfaceTextureHelper.create("ScreenCaptureThread", rootEglBase.eglBaseContext), context, videoSource.capturerObserver)
        videoCapturer.startCapture(720, 1280, 30)

        localVideoTrack = peerConnectionFactory.createVideoTrack("ARDAMSv0", videoSource)
    }

    fun startAudioCapture() {
        val audioSource = peerConnectionFactory.createAudioSource(MediaConstraints())
        localAudioTrack = peerConnectionFactory.createAudioTrack("ARDAMSa0", audioSource)
    }

    private fun createScreenCapturer(intent: Intent): VideoCapturer {
        return ScreenCapturerAndroid(intent, object : MediaProjection.Callback() {
            override fun onStop() {
                super.onStop()
            }
        })
    }

    fun createPeerConnection(observer: PeerConnection.Observer) {
        val rtcConfig = PeerConnection.RTCConfiguration(
            listOf(
                PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer()
            )
        )
        peerConnection = peerConnectionFactory.createPeerConnection(rtcConfig, observer)
        localVideoTrack?.let {
            peerConnection?.addTrack(it, listOf("ARDAMS"))
        }
        localAudioTrack?.let {
            peerConnection?.addTrack(it, listOf("ARDAMS"))
        }
        
        // Create DataChannel (for screen sharing side - Parent app)
        createDataChannel()
    }
    
    /**
     * Create DataChannel for sending/receiving touch data
     */
    private fun createDataChannel() {
        val init = DataChannel.Init().apply {
            ordered = false  // Unordered for lower latency
            maxRetransmits = 0  // No retransmission for real-time data
        }
        
        dataChannel = peerConnection?.createDataChannel(DATA_CHANNEL_NAME, init)
        dataChannel?.registerObserver(createDataChannelObserver())
        Log.d(TAG, "DataChannel created: $DATA_CHANNEL_NAME")
    }
    
    /**
     * Setup DataChannel observer for receiving data
     */
    private fun createDataChannelObserver(): DataChannel.Observer {
        return object : DataChannel.Observer {
            override fun onBufferedAmountChange(previousAmount: Long) {}
            
            override fun onStateChange() {
                Log.d(TAG, "DataChannel state: ${dataChannel?.state()}")
            }
            
            override fun onMessage(buffer: DataChannel.Buffer) {
                if (!buffer.binary) {
                    val data = buffer.data
                    val bytes = ByteArray(data.remaining())
                    data.get(bytes)
                    val message = String(bytes, Charset.forName("UTF-8"))
                    handleTouchMessage(message)
                }
            }
        }
    }
    
    /**
     * Handle incoming touch message
     */
    private fun handleTouchMessage(message: String) {
        try {
            val json = JSONObject(message)
            if (json.getString("type") == "touch") {
                val x = json.getDouble("x").toFloat()
                val y = json.getDouble("y").toFloat()
                Log.d(TAG, "Touch received via DataChannel: ($x, $y)")
                onTouchReceived?.invoke(x, y)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing touch message: ${e.message}")
        }
    }
    
    /**
     * Send touch data via DataChannel (called from Guardian app)
     */
    fun sendTouchData(x: Float, y: Float) {
        val json = JSONObject().apply {
            put("type", "touch")
            put("x", x)
            put("y", y)
        }
        val message = json.toString()
        val buffer = ByteBuffer.wrap(message.toByteArray(Charset.forName("UTF-8")))
        val dataBuffer = DataChannel.Buffer(buffer, false)
        
        val success = dataChannel?.send(dataBuffer) ?: false
        if (!success) {
            Log.w(TAG, "Failed to send touch data, DataChannel state: ${dataChannel?.state()}")
        }
    }
    
    /**
     * Register observer for incoming DataChannel (for Guardian app receiving channel)
     */
    fun setOnDataChannelReceived(callback: (DataChannel) -> Unit) {
        // This will be called from PeerConnection.Observer.onDataChannel()
        // Store callback for external use if needed
    }
    
    /**
     * Setup received DataChannel (called when Guardian receives DataChannel from Parent)
     */
    fun setupReceivedDataChannel(channel: DataChannel) {
        dataChannel = channel
        dataChannel?.registerObserver(createDataChannelObserver())
        Log.d(TAG, "Received DataChannel setup complete")
    }
    
    fun sendOffer() {
        val constraints = MediaConstraints()
        peerConnection?.createOffer(object : SdpObserverAdapter() {
            override fun onCreateSuccess(desc: SessionDescription?) {
                desc?.let {
                    peerConnection?.setLocalDescription(object : SdpObserverAdapter() {}, it)
                    signalingClient.sendOffer(it.description)
                }
            }
        }, constraints)
    }
    
    fun answerOffer(offerSdp: String) {
        val constraints = MediaConstraints()
        peerConnection?.setRemoteDescription(object : SdpObserverAdapter() {}, SessionDescription(SessionDescription.Type.OFFER, offerSdp))
        
        peerConnection?.createAnswer(object : SdpObserverAdapter() {
            override fun onCreateSuccess(desc: SessionDescription?) {
                desc?.let {
                    peerConnection?.setLocalDescription(object : SdpObserverAdapter() {}, it)
                    signalingClient.sendAnswer(it.description)
                }
            }
        }, constraints)
    }
    
    fun onRemoteAnswer(answerSdp: String) {
        peerConnection?.setRemoteDescription(object : SdpObserverAdapter() {}, SessionDescription(SessionDescription.Type.ANSWER, answerSdp))
    }
    
    fun addIceCandidate(candidate: IceCandidate) {
        peerConnection?.addIceCandidate(candidate)
    }

    fun close() {
        dataChannel?.close()
        dataChannel = null
        peerConnection?.close()
    }
    
    open inner class SdpObserverAdapter : SdpObserver {
        override fun onCreateSuccess(p0: SessionDescription?) {}
        override fun onSetSuccess() {}
        override fun onCreateFailure(p0: String?) {
            onError?.invoke("SDP Create Failure: $p0")
        }
        override fun onSetFailure(p0: String?) {
            onError?.invoke("SDP Set Failure: $p0")
        }
    }
}

