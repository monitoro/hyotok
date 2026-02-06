package com.silverpixelism.hyotok.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
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
import com.google.firebase.database.FirebaseDatabase

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PairingScreen(
    onBack: () -> Unit
) {
    var pairingCode by remember { mutableStateOf("") }
    var inputCode by remember { mutableStateOf("") }
    var statusMessage by remember { mutableStateOf("") }

    val database = FirebaseDatabase.getInstance()
    val usersRef = database.getReference("users")

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
            Text(
                text = "내 연결 코드",
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
                        .addOnSuccessListener { statusMessage = "코드가 생성되었습니다." }
                        .addOnFailureListener { statusMessage = "생성 실패: ${it.message}" }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("새 코드 생성 (부모님용)")
            }

            Spacer(modifier = Modifier.height(32.dp))

            OutlinedTextField(
                value = inputCode,
                onValueChange = { inputCode = it },
                label = { Text("상대방 코드 입력") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = {
                    if (inputCode.isNotEmpty()) {
                        usersRef.child(inputCode).setValue("connected")
                            .addOnSuccessListener { statusMessage = "연결되었습니다!" }
                            .addOnFailureListener { statusMessage = "연결 실패: ${it.message}" }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("코드 연결 (자녀용)")
            }

            if (statusMessage.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(text = statusMessage, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}
