package com.silverpixelism.hyotok.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import android.widget.Toast
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import com.google.firebase.database.FirebaseDatabase

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PairingScreen(
    onBack: () -> Unit,
    onSaveCode: (String) -> Unit,
    onStartShare: () -> Unit,
    onChildConnected: (String) -> Unit = {}
) {
    var pairingCode by remember { mutableStateOf("") }
    var inputCode by remember { mutableStateOf("") }
    var statusMessage by remember { mutableStateOf("") }
    var isChildConnected by remember { mutableStateOf(false) }

    val database = FirebaseDatabase.getInstance()
    val usersRef = database.getReference("users")
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("자녀 연결") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Parent Section
            Text(
                text = "내 연결 코드 (부모님)",
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = if (pairingCode.isNotEmpty()) pairingCode else "코드 생성 버튼을 누르세요",
                style = MaterialTheme.typography.displayMedium,
                modifier = Modifier.padding(16.dp)
            )

            Button(
                onClick = {
                    val newCode = (100000..999999).random().toString()
                    pairingCode = newCode
                    usersRef.child(newCode).setValue("ready")
                        .addOnSuccessListener { 
                            statusMessage = "코드가 생성되었습니다. 자녀의 접속을 기다리는 중..."
                            onSaveCode(newCode)
                            
                            // Listen for connection
                            usersRef.child(newCode).addValueEventListener(object : com.google.firebase.database.ValueEventListener {
                                override fun onDataChange(snapshot: com.google.firebase.database.DataSnapshot) {
                                    val status = snapshot.getValue(String::class.java)
                                    android.util.Log.d("PairingScreen", "Firebase status changed: $status")
                                    android.widget.Toast.makeText(context, "상태: $status", android.widget.Toast.LENGTH_SHORT).show()
                                    
                                    if (status == "connected") {
                                        statusMessage = "자녀와 연결되었습니다! 화면 공유를 시작합니다."
                                        android.util.Log.d("PairingScreen", "Calling onStartShare()")
                                        onStartShare()
                                    }
                                }
                                override fun onCancelled(error: com.google.firebase.database.DatabaseError) {
                                    android.util.Log.e("PairingScreen", "Firebase error: ${error.message}")
                                }
                            })
                        }
                        .addOnFailureListener { statusMessage = "생성 실패: ${it.message}" }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("새 코드 생성")
            }

            if (pairingCode.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                        val shareIntent = android.content.Intent().apply {
                            action = android.content.Intent.ACTION_SEND
                            putExtra(android.content.Intent.EXTRA_TEXT, "효도폰 연결 코드: [$pairingCode] \n\n이 코드를 자녀 앱에 입력해주세요.")
                            type = "text/plain"
                        }
                        context.startActivity(android.content.Intent.createChooser(shareIntent, "코드 공유하기"))
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = androidx.compose.ui.graphics.Color(0xFFFAE300), // Kakao Yellow like
                        contentColor = androidx.compose.ui.graphics.Color.Black
                    )
                ) {
                    Text("카카오톡/문자로 코드 보내기")
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
            Spacer(modifier = Modifier.height(16.dp))

            if (statusMessage.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(text = statusMessage, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}
