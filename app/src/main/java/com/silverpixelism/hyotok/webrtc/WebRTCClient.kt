package com.silverpixelism.hyotok.webrtc

import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjection // Added
import org.webrtc.*

class WebRTCClient(
    private val context: Context,
    private val eglBaseContext: EglBase.Context,
    private val signalingClient: SignalingClient
) {
    private val peerConnectionFactory: PeerConnectionFactory
    private var peerConnection: PeerConnection? = null
    private var localVideoTrack: VideoTrack? = null
    private var localAudioTrack: AudioTrack? = null
    val rootEglBase: EglBase = EglBase.create(eglBaseContext)
    
    var onError: ((String) -> Unit)? = null

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
        videoCapturer.startCapture(720, 1280, 30) // Resolution can be adjusted

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

    // Call this to setup PeerConnection and add tracks
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
