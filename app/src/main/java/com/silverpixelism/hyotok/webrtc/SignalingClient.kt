package com.silverpixelism.hyotok.webrtc

import android.util.Log
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class SignalingClient(private val pairingCode: String = "test_code") { // Default code for now
    private val database = FirebaseDatabase.getInstance()
    private val signalingRef = database.getReference("signaling").child(pairingCode)
    
    var onOfferReceived: ((String) -> Unit)? = null
    var onAnswerReceived: ((String) -> Unit)? = null
    var onIceCandidateReceived: ((org.webrtc.IceCandidate) -> Unit)? = null

    init {
        listenForSignals()
    }

    private fun listenForSignals() {
        signalingRef.addChildEventListener(object : com.google.firebase.database.ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                val key = snapshot.key
                if (key == "offer") {
                    val offer = snapshot.getValue(String::class.java)
                    offer?.let { onOfferReceived?.invoke(it) }
                } else if (key == "answer") {
                    val answer = snapshot.getValue(String::class.java)
                    answer?.let { onAnswerReceived?.invoke(it) }
                } else if (key == "candidates") {
                     // Iterate through existing candidates or catch new ones
                     for (candidateSnap in snapshot.children) {
                         handleCandidateSnapshot(candidateSnap)
                     }
                }
            }

            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                // If candidates are added to the list
                if (snapshot.key == "candidates") {
                    // Logic to find new ones is tricky with onChildChanged on the parent node
                    // Better to put a listener on "candidates" reference itself if using push()
                }
            }
            override fun onChildRemoved(snapshot: DataSnapshot) {}
            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onCancelled(error: DatabaseError) {
                Log.e("SignalingClient", "Database error: ${error.message}")
            }
        })
    }

    fun sendOffer(sdp: String) {
        signalingRef.child("offer").setValue(sdp)
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
        // Store as a list/push
        signalingRef.child("candidates").push().setValue(candidateMap)
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
}
