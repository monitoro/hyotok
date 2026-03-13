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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size // Add size import
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.background
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.CircularProgressIndicator // Add import
import androidx.compose.runtime.Composable
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.TouchApp // Use basic filled icon
import androidx.compose.material.icons.filled.BatteryChargingFull // Add Battery Icons
import androidx.compose.material.icons.filled.BatteryStd
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.foundation.clickable // Add clickable import
import com.silverpixelism.hyotok.webrtc.SignalingClient
import com.silverpixelism.hyotok.webrtc.WebRTCClient
import org.webrtc.SurfaceViewRenderer
import com.silverpixelism.hyotok.ui.theme.HyoTalkTheme
// Imports for AdMob Interstitial Ad removed

class GuardianHomeActivity : ComponentActivity() {



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize AdMob
        com.google.android.gms.ads.MobileAds.initialize(this) {}
        
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
    // Load saved pairing code
    val prefs = remember { com.silverpixelism.hyotok.data.AppPreferences(context) }
    var pairingCode by remember { mutableStateOf(prefs.getPairingCode() ?: "") }
    
    var isConnected by remember { mutableStateOf(false) }
    val signalingClient = remember { mutableStateOf<com.silverpixelism.hyotok.webrtc.SignalingClient?>(null) }
    
    // Safety Data States
    var batteryLevel by remember { mutableIntStateOf(-1) }
    var isCharging by remember { mutableStateOf(false) }
    var latitude by remember { mutableDoubleStateOf(0.0) }
    var longitude by remember { mutableDoubleStateOf(0.0) }
    var lastUpdated by remember { mutableLongStateOf(0L) }
    
    // Listen to Safety Data when pairingCode is available
    LaunchedEffect(pairingCode) {
        if (pairingCode.isNotEmpty()) {
            val database = com.google.firebase.database.FirebaseDatabase.getInstance()
            val safetyRef = database.getReference("safety").child(pairingCode)
            
            val listener = object : com.google.firebase.database.ValueEventListener {
                override fun onDataChange(snapshot: com.google.firebase.database.DataSnapshot) {
                    batteryLevel = snapshot.child("batteryLevel").getValue(Int::class.java) ?: -1
                    isCharging = snapshot.child("isCharging").getValue(Boolean::class.java) ?: false
                    latitude = snapshot.child("latitude").getValue(Double::class.java) ?: 0.0
                    longitude = snapshot.child("longitude").getValue(Double::class.java) ?: 0.0
                    lastUpdated = snapshot.child("lastUpdated").getValue(Long::class.java) ?: 0L
                }
                override fun onCancelled(error: com.google.firebase.database.DatabaseError) {}
            }
            safetyRef.addValueEventListener(listener)
        }
    }
    
    // Background Color
    val backgroundColor = androidx.compose.ui.graphics.Color(0xFF1A1F36)
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
            .statusBarsPadding()
    ) {
        // Header - Always visible
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(backgroundColor)
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
                        // Reset Connection
                        val database = com.google.firebase.database.FirebaseDatabase.getInstance()
                        database.getReference("users").child(pairingCode).setValue("disconnected")
                        
                        signalingClient.value?.sendDisconnect()
                        signalingClient.value?.cleanup()
                        signalingClient.value = null // Nullify
                        isConnected = false
                        
                        
                        // Interstitial Ad removed on disconnect
                    }
                ) {
                    Text("연결 끊기", color = androidx.compose.ui.graphics.Color.Red)
                }
            }
        }
        
        if (!isConnected) {
            // Connection UI - Split into Input (Top) and Tutorial (Bottom)
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 1. Input Section (Weight 0.35) - Top Area
                Column(
                    modifier = Modifier
                        .weight(0.35f)
                        .fillMaxWidth(),
                    verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center, // Center vertically in top area
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "부모님 폰과 연결하기",
                        style = MaterialTheme.typography.headlineSmall,
                        color = androidx.compose.ui.graphics.Color.White,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    
                    androidx.compose.material3.TextField(
                        value = pairingCode,
                        onValueChange = { 
                            pairingCode = it 
                            prefs.savePairingCode(it) // Auto save
                        },
                        label = { Text("부모님 폰의 연결 코드 6자리") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = androidx.compose.material3.TextFieldDefaults.colors(
                            focusedContainerColor = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.1f),
                            unfocusedContainerColor = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.1f),
                            focusedTextColor = androidx.compose.ui.graphics.Color.White,
                            unfocusedTextColor = androidx.compose.ui.graphics.Color.White,
                            focusedLabelColor = androidx.compose.ui.graphics.Color(0xFFA5B4FC), // Light Indigo
                            unfocusedLabelColor = androidx.compose.ui.graphics.Color.Gray
                        )
                    )
            
                    Spacer(modifier = Modifier.height(16.dp))
            
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
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                            containerColor = androidx.compose.ui.graphics.Color(0xFF4F46E5) // Indigo 600
                        )
                    ) {
                        Text("연결하기", style = MaterialTheme.typography.titleMedium, color = androidx.compose.ui.graphics.Color.White)
                    }
                }
                
                // Divider
                androidx.compose.material3.Divider(
                    color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.1f),
                    thickness = 1.dp
                )
                
                // 2. Info & Tutorial Section (Weight 0.55) - Middle Area
                Column(
                    modifier = Modifier
                        .weight(0.55f)
                        .fillMaxWidth()
                        .padding(top = 16.dp)
                ) {
                    // Safety Status Card - Always visible (shows waiting state if no data)
                     Text(
                        text = "부모님 현재 상태 (Beta)",
                        style = MaterialTheme.typography.titleMedium,
                        color = androidx.compose.ui.graphics.Color(0xFF63B3ED), // Light Blue
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    
                    androidx.compose.material3.Card(
                        colors = androidx.compose.material3.CardDefaults.cardColors(
                            containerColor = androidx.compose.ui.graphics.Color(0xFF2D3748)
                        ),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            if (batteryLevel != -1) {
                                // Data Available
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = if (isCharging) Icons.Default.BatteryChargingFull else Icons.Default.BatteryStd,
                                        contentDescription = "Battery",
                                        tint = if (batteryLevel < 20) androidx.compose.ui.graphics.Color.Red else androidx.compose.ui.graphics.Color.Green,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "배터리 ${batteryLevel}%" + if (isCharging) " (충전 중)" else "",
                                        color = androidx.compose.ui.graphics.Color.White,
                                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                                    )
                                }
                                
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.LocationOn,
                                        contentDescription = "Location",
                                        tint = androidx.compose.ui.graphics.Color.Yellow,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    if (latitude != 0.0) {
                                        Text(
                                            text = "위치 확인됨",
                                            color = androidx.compose.ui.graphics.Color.White,
                                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                                            modifier = Modifier.clickable {
                                                val uri = android.net.Uri.parse("geo:$latitude,$longitude?q=$latitude,$longitude(Parents)")
                                                val mapIntent = Intent(Intent.ACTION_VIEW, uri)
                                                mapIntent.setPackage("com.google.android.apps.maps")
                                                if (mapIntent.resolveActivity(context.packageManager) != null) {
                                                    context.startActivity(mapIntent)
                                                } else {
                                                    context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                                                }
                                            }
                                        )
                                        Text(
                                            text = " (지도 보기)",
                                            color = androidx.compose.ui.graphics.Color.Gray,
                                            fontSize = 12.sp
                                        )
                                    } else {
                                        Text(
                                            text = "위치 정보 없음",
                                            color = androidx.compose.ui.graphics.Color.Gray
                                        )
                                    }
                                }
                                
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                val timeDiff = (System.currentTimeMillis() - lastUpdated) / (1000 * 60)
                                Text(
                                    text = "마지막 업데이트: ${timeDiff}분 전",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = androidx.compose.ui.graphics.Color.Gray
                                )
                            } else {
                                // Waiting for Data
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
                                ) {
                                    androidx.compose.material3.CircularProgressIndicator(
                                        modifier = Modifier.size(24.dp),
                                        color = androidx.compose.ui.graphics.Color(0xFFA5B4FC),
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = "부모님 상태 수신 대기 중...",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = androidx.compose.ui.graphics.Color.Gray
                                    )
                                    Text(
                                        text = "(최대 15분 소요될 수 있습니다)",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = androidx.compose.ui.graphics.Color.DarkGray
                                    )
                                }
                            }
                        }
                    }
                
                    Text(
                        text = "부모님 폰 설정 가이드",
                        style = MaterialTheme.typography.titleMedium,
                        color = androidx.compose.ui.graphics.Color(0xFFA5B4FC), // Light Indigo
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    
                    androidx.compose.foundation.lazy.LazyColumn(
                        verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 24.dp)
                    ) {
                        item {
                            TutorialCard(
                                icon = Icons.Default.Home,
                                title = "1. 기본 홈 앱으로 설정 (필수)",
                                description = "앱 설치 후 최초 실행 시, 또는 스마트폰 '설정 > 애플리케이션 > 기본 앱 선택'에서 '효도폰'을 기본 홈 앱으로 설정해야 항상 이 화면이 유지됩니다."
                            )
                        }
                        item {
                            TutorialCard(
                                icon = Icons.Default.Contacts,
                                title = "2. 연락처 및 전화 권한 허용",
                                description = "홈 화면에서 가족에게 바로 전화를 걸기 위해, 최초 권한 요청 시 '연락처 접근' 및 '전화 걸기' 권한을 반드시 허용해주세요."
                            )
                        }
                        item {
                            TutorialCard(
                                icon = Icons.Default.PhotoCamera,
                                title = "3. 사진보내기 및 파일 권한",
                                description = "홈 화면 '사진보내기' 버튼으로 부모님이 최근에 찍은 사진을 가족에게 쉽게 보낼 수 있습니다. 이를 위해 최초 실행 시 '사진 및 동영상(또는 파일)' 시스템 접근 권한을 허용해주세요."
                            )
                        }
                        item {
                            TutorialCard(
                                icon = Icons.Default.Settings,
                                title = "4. 부모님 맞춤 환경 설정",
                                description = "부모폰 설정 메뉴(우측 상단 톱니바퀴)에서 앱 아이콘 글자 크기 조절, 하단 자주 쓰는 앱 추가, 상단 6개 즐겨찾기 메뉴 변경 등을 부모님께 맞춰 설정해 드릴 수 있습니다."
                            )
                        }
                        item {
                            TutorialCard(
                                icon = Icons.Default.TouchApp,
                                title = "5. 도움받기(원격지원) 기능",
                                description = "부모님이 홈 화면의 '도움받기' 버튼을 누르면 자녀에게 연결 요청이 옵니다. 스마트폰 화면을 함께 보면서 원격으로 도와드릴 수 있습니다."
                            )
                        }
                    }
                }

                
                // 3. AdMob Banner (Weight 0.1) - Bottom Area 제거됨
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
fun TutorialCard(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, description: String) {
    androidx.compose.material3.Card(
        colors = androidx.compose.material3.CardDefaults.cardColors(
            containerColor = androidx.compose.ui.graphics.Color(0xFF2D3748) // Dark Slate
        ),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = androidx.compose.ui.graphics.Color(0xFF63B3ED), // Light Blue
                modifier = Modifier.padding(top = 2.dp).size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                    color = androidx.compose.ui.graphics.Color.White
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = androidx.compose.ui.graphics.Color(0xFFCBD5E0), // Light Gray
                    lineHeight = 20.sp
                )
            }
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
