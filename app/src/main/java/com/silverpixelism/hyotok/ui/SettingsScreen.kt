package com.silverpixelism.hyotok.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.silverpixelism.hyotok.data.AppPreferences

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { AppPreferences(context) }
    
    // State
    var emergencyContact by remember { mutableStateOf(prefs.getEmergencyContact()) }
    var fullScreenShare by remember { mutableStateOf(prefs.isFullScreenShareEnabled()) }
    var childContacts by remember { mutableStateOf(prefs.getChildContacts()) }
    
    // Edit child contact dialog
    var showAddContactDialog by remember { mutableStateOf(false) }
    var newContactName by remember { mutableStateOf("") }
    var newContactPhone by remember { mutableStateOf("") }
    
    // Add Child Contact Dialog
    if (showAddContactDialog) {
        AlertDialog(
            onDismissRequest = { showAddContactDialog = false },
            title = { Text("자녀 연락처 추가") },
            text = {
                Column {
                    OutlinedTextField(
                        value = newContactName,
                        onValueChange = { newContactName = it },
                        label = { Text("이름 (예: 첫째 아들)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newContactPhone,
                        onValueChange = { newContactPhone = it },
                        label = { Text("전화번호") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (newContactName.isNotEmpty() && newContactPhone.isNotEmpty()) {
                        val updated = childContacts + Pair(newContactName, newContactPhone)
                        childContacts = updated
                        prefs.saveChildContacts(updated)
                        newContactName = ""
                        newContactPhone = ""
                        showAddContactDialog = false
                    }
                }) {
                    Text("추가")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddContactDialog = false }) {
                    Text("취소")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("설정", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "뒤로")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkNavy,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        containerColor = DarkNavy
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // 긴급 연락처 섹션
            SettingsSection(title = "📞 긴급 연락처") {
                OutlinedTextField(
                    value = emergencyContact,
                    onValueChange = { 
                        emergencyContact = it
                        prefs.saveEmergencyContact(it)
                    },
                    label = { Text("긴급 연락처 번호") },
                    placeholder = { Text("010-1234-5678") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = BrightYellow,
                        unfocusedBorderColor = Color.Gray,
                        focusedLabelColor = BrightYellow,
                        unfocusedLabelColor = Color.Gray
                    )
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 화면 공유 설정 섹션
            SettingsSection(title = "📱 화면 공유 설정") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF2A3050), RoundedCornerShape(12.dp))
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("자동 전체화면", color = Color.White, fontWeight = FontWeight.Medium)
                        Text(
                            "화면 공유 시 자동으로 전체화면으로 표시",
                            color = Color.Gray,
                            fontSize = 12.sp
                        )
                    }
                    Switch(
                        checked = fullScreenShare,
                        onCheckedChange = { 
                            fullScreenShare = it
                            prefs.setFullScreenShareEnabled(it)
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = BrightYellow,
                            checkedTrackColor = BrightYellow.copy(alpha = 0.5f)
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 자녀 연락처 섹션
            SettingsSection(title = "👨‍👧‍👦 자녀 연락처") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF2A3050), RoundedCornerShape(12.dp))
                        .padding(16.dp)
                ) {
                    if (childContacts.isEmpty()) {
                        Text(
                            "등록된 자녀가 없습니다",
                            color = Color.Gray,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    } else {
                        childContacts.forEachIndexed { index, contact ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(contact.first, color = Color.White, fontWeight = FontWeight.Medium)
                                    Text(contact.second, color = Color.Gray, fontSize = 14.sp)
                                }
                                IconButton(onClick = {
                                    val updated = childContacts.toMutableList().apply { removeAt(index) }
                                    childContacts = updated
                                    prefs.saveChildContacts(updated)
                                }) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = "삭제",
                                        tint = Color.Red.copy(alpha = 0.7f)
                                    )
                                }
                            }
                            if (index < childContacts.size - 1) {
                                Divider(color = Color.Gray.copy(alpha = 0.3f))
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    OutlinedButton(
                        onClick = { showAddContactDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = BrightYellow
                        )
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("자녀 추가")
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // 저장 확인 메시지
            Text(
                "💡 변경사항은 자동으로 저장됩니다",
                color = Color.Gray,
                fontSize = 12.sp,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }
    }
}

@Composable
fun SettingsSection(title: String, content: @Composable () -> Unit) {
    Column {
        Text(
            text = title,
            color = BrightYellow,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        content()
    }
}
