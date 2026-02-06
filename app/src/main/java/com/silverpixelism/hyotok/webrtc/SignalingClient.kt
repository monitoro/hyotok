package com.silverpixelism.hyotok.webrtc

import android.util.Log
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase

class SignalingClient(private val pairingCode: String = "test_code") {
    private val database = FirebaseDatabase.getInstance()
    private val signalingRef = database.getReference("signaling").child(pairingCode)
    
    // Store listener reference for cleanup
    private var childEventListener: ChildEventListener? = null
    
    var onOfferReceived: ((String) -> Unit)? = null
    var onAnswerReceived: ((String) -> Unit)? = null
    var onIceCandidateReceived: ((org.webrtc.IceCandidate) -> Unit)? = null
    var onError: ((String) -> Unit)? = null
    var onTouchReceived: ((Float, Float) -> Unit)? = null
    var onDisconnectReceived: (() -> Unit)? = null  // NEW: disconnect callback

    fun startListening() {
        // Remove any existing listener first
        stopListening()
        listenForSignals()
    }

    private fun listenForSignals() {
        childEventListener = object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                handleSnapshot(snapshot)
            }
            
            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                handleSnapshot(snapshot)
            }
            
            override fun onChildRemoved(snapshot: DataSnapshot) {
                // If disconnect signal is removed, it means the other side disconnected
                if (snapshot.key == "disconnect") {
                    // Ignore removal
                } else if (snapshot.key == "offer" || snapshot.key == "answer") {
                    // Connection data removed - treat as disconnect
                    Log.d("SignalingClient", "Signaling data removed: ${snapshot.key}")
                }
            }
            
            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
            
            override fun onCancelled(error: DatabaseError) {
                Log.e("SignalingClient", "Database error: ${error.message}")
                onError?.invoke("Database error: ${error.message}")
            }
        }
        
        signalingRef.addChildEventListener(childEventListener!!)
    }
    
    private fun handleSnapshot(snapshot: DataSnapshot) {
        when (snapshot.key) {
            "offer" -> {
                val offer = snapshot.getValue(String::class.java)
                offer?.let { onOfferReceived?.invoke(it) }
            }
            "answer" -> {
                val answer = snapshot.getValue(String::class.java)
                answer?.let { onAnswerReceived?.invoke(it) }
            }
            "candidates" -> {
                for (candidateSnap in snapshot.children) {
                    handleCandidateSnapshot(candidateSnap)
                }
            }
            "touch" -> {
                val x = snapshot.child("x").getValue(Float::class.java)
                val y = snapshot.child("y").getValue(Float::class.java)
                if (x != null && y != null) {
                    onTouchReceived?.invoke(x, y)
                }
            }
            "disconnect" -> {
                val disconnected = snapshot.getValue(Boolean::class.java)
                if (disconnected == true) {
                    Log.d("SignalingClient", "Disconnect signal received")
                    onDisconnectReceived?.invoke()
                }
            }
        }
    }

    fun sendTouch(x: Float, y: Float) {
        val touchMap = mapOf("x" to x, "y" to y)
        signalingRef.child("touch").setValue(touchMap)
    }

    fun sendOffer(sdp: String) {
        signalingRef.child("offer").setValue(sdp)
            .addOnFailureListener {
                onError?.invoke("Failed to send offer: ${it.message}")
            }
    }

    fun sendAnswer(sdp: String) {
        signalingRef.child("answer").setValue(sdp)
    }

    fun sendIceCandidate(candidate: org.webrtc.IceCandidate) {
        val candidateMap = mapOf(
            "sdpMid" to candidate.sdpMid,
            "sdpMLineIndex" to candidate.sdpMLineIndex,
            "candidate" to candidate.sdp
        )
        signalingRef.child("candidates").push().setValue(candidateMap)
    }
    
    // NEW: Send disconnect signal before cleanup
    fun sendDisconnect() {
        signalingRef.child("disconnect").setValue(true)
    }

    private fun handleCandidateSnapshot(snapshot: DataSnapshot) {
        val sdpMid = snapshot.child("sdpMid").getValue(String::class.java)
        val sdpMLineIndex = snapshot.child("sdpMLineIndex").getValue(Int::class.java)
        val candidateSd = snapshot.child("candidate").getValue(String::class.java)
        
        if (sdpMid != null && sdpMLineIndex != null && candidateSd != null) {
            val iceCandidate = org.webrtc.IceCandidate(sdpMid, sdpMLineIndex, candidateSd)
            onIceCandidateReceived?.invoke(iceCandidate)
        }
    }
    
    // Stop listening to Firebase
    fun stopListening() {
        childEventListener?.let {
            signalingRef.removeEventListener(it)
            childEventListener = null
        }
    }
    
    // Cleanup signaling data for fresh reconnection
    fun cleanup() {
        stopListening()
        signalingRef.removeValue()
    }
}

