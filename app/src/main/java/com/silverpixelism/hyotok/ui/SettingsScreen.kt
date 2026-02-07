package com.silverpixelism.hyotok.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.rounded.*
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.silverpixelism.hyotok.data.AppPreferences
import com.silverpixelism.hyotok.data.AppRepository
import com.silverpixelism.hyotok.data.AppInfo
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// New Theme Colors for Settings
val SettingsBgColor = Color(0xFFF2F4F8) // Light Gray-Blue
val SettingsSurfaceColor = Color.White
val SettingsPrimaryColor = Color(0xFF6C5CE7) // Soft Purple
val SettingsTextColor = Color(0xFF2D3436)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { AppPreferences(context) }
    val appRepository = remember { AppRepository(context) }
    
    // State
    var emergencyContact by remember { mutableStateOf(prefs.getEmergencyContact()) }
    var fullScreenShare by remember { mutableStateOf(prefs.isFullScreenShareEnabled()) }
    var childContacts by remember { mutableStateOf(prefs.getChildContacts()) }
    var hapticEnabled by remember { mutableStateOf(prefs.isHapticEnabled()) }
    var familyChatUrl by remember { mutableStateOf(prefs.getFamilyChatUrl()) }

    // User Info State
    var userName by remember { mutableStateOf(prefs.getUserName()) }
    var userPhoneNumber by remember { mutableStateOf(prefs.getUserPhoneNumber()) }
    var showUserName by remember { mutableStateOf(prefs.isUserNameVisible()) }
    var showUserPhoneNumber by remember { mutableStateOf(prefs.isUserPhoneNumberVisible()) }
    
    // App Selection
    var showAppSelectionDialog by remember { mutableStateOf(false) }
    var installedApps by remember { mutableStateOf(emptyList<AppInfo>()) }
    var selectedApps by remember { mutableStateOf(prefs.getHomeApps().toSet()) }

    LaunchedEffect(showAppSelectionDialog) {
        if (showAppSelectionDialog && installedApps.isEmpty()) {
            installedApps = withContext(Dispatchers.IO) {
                appRepository.getInstalledApps()
            }
        }
    }
    
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

    // App Selection Dialog (Custom Grid UI)
    if (showAppSelectionDialog) {
        Dialog(onDismissRequest = { showAppSelectionDialog = false }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF2D3436)), // Dark Background
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(600.dp)
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "홈 화면 앱 추가",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    
                    if (installedApps.isEmpty()) {
                        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = Color.White)
                        }
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(3),
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(installedApps) { app ->
                                val isSelected = selectedApps.contains(app.packageName)
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .clickable {
                                            selectedApps = if (isSelected) {
                                                selectedApps - app.packageName
                                            } else {
                                                selectedApps + app.packageName
                                            }
                                        }
                                        .background(
                                            if (isSelected) SettingsPrimaryColor.copy(alpha = 0.3f) else Color.Transparent,
                                            RoundedCornerShape(8.dp)
                                        )
                                        .padding(8.dp)
                                ) {
                                    Box {
                                        Image(
                                            painter = rememberDrawablePainter(app.icon),
                                            contentDescription = null,
                                            modifier = Modifier.size(48.dp)
                                        )
                                        if (isSelected) {
                                            Icon(
                                                Icons.Rounded.CheckCircle,
                                                contentDescription = null,
                                                tint = SettingsPrimaryColor,
                                                modifier = Modifier.align(Alignment.TopEnd).background(Color.White, CircleShape)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = app.name,
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        textAlign = TextAlign.Center,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showAppSelectionDialog = false }) {
                            Text("취소", color = Color.White.copy(alpha = 0.7f))
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                prefs.saveHomeApps(selectedApps.toList())
                                showAppSelectionDialog = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = SettingsPrimaryColor)
                        ) {
                            Text("저장 (${selectedApps.size}개)", color = Color.White)
                        }
                    }
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("부모님 앱 설정", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "뒤로")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = SettingsBgColor,
                    titleContentColor = SettingsTextColor,
                    navigationIconContentColor = SettingsTextColor
                )
            )
        },
        containerColor = SettingsBgColor
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // 0. 내 정보 설정 (New)
            SettingsCard(title = "👤 내 정보 설정") {
                SettingInputItem(
                    title = "내 이름 (표시용)",
                    value = userName,
                    onValueChange = { 
                        userName = it
                        prefs.saveUserName(it)
                    },
                    placeholder = "이름을 입력하세요 (예: 김망고)"
                )
                SettingSwitchItem(
                    title = "홈 화면에 이름 표시",
                    description = "홈 화면 상단에 이름을 표시합니다.",
                    checked = showUserName,
                    onCheckedChange = { 
                        showUserName = it
                        prefs.setUserNameVisible(it)
                    }
                )
                Divider(color = Color.LightGray.copy(alpha = 0.3f))
                SettingInputItem(
                    title = "내 전화번호 (표시용)",
                    value = userPhoneNumber,
                    onValueChange = { 
                        userPhoneNumber = it
                        prefs.saveUserPhoneNumber(it)
                    },
                    placeholder = "전화번호를 입력하세요"
                )
                SettingSwitchItem(
                    title = "홈 화면에 전화번호 표시",
                    description = "홈 화면 상단에 전화번호를 표시합니다.",
                    checked = showUserPhoneNumber,
                    onCheckedChange = { 
                        showUserPhoneNumber = it
                        prefs.setUserPhoneNumberVisible(it)
                    }
                )
                if (!showUserName && !showUserPhoneNumber) {
                     Text(
                        text = "💡 이름과 전화번호를 모두 숨기면 날씨 인사말이 표시됩니다.",
                        color = SettingsPrimaryColor,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }

            // 1. 화면 및 소리
            SettingsCard(title = "📱 화면 및 소리") {
                SettingSwitchItem(
                    title = "햅틱 피드백",
                    description = "버튼을 누를 때 진동을 느낍니다.",
                    checked = hapticEnabled,
                    onCheckedChange = { 
                        hapticEnabled = it 
                        prefs.setHapticEnabled(it)
                    }
                )
                Divider(color = Color.LightGray.copy(alpha = 0.3f))
                SettingSwitchItem(
                    title = "화면 공유 자동 전체화면",
                    description = "화면 공유 시 자동으로 전체화면으로 표시합니다.",
                    checked = fullScreenShare,
                    onCheckedChange = { 
                        fullScreenShare = it
                        prefs.setFullScreenShareEnabled(it)
                    }
                )
            }

            // 2. 홈 화면 구성
            SettingsCard(title = "🏠 홈 화면 구성") {
                SettingTextItem(
                    title = "홈 화면 앱추가",
                    description = "${selectedApps.size}개의 앱이 선택됨",
                    onClick = { showAppSelectionDialog = true }
                )
                Divider(color = Color.LightGray.copy(alpha = 0.3f))
                 SettingInputItem(
                    title = "가족 단톡방 링크",
                    value = familyChatUrl,
                    onValueChange = { 
                        familyChatUrl = it
                        prefs.saveFamilyChatUrl(it)
                    },
                    placeholder = "카카오톡 오픈채팅방 링크 입력"
                )
            }

            // 3. 가족 연결
            SettingsCard(title = "👨‍👩‍👧‍👦 가족 연결") {
                SettingInputItem(
                    title = "긴급 연락처 (119 또는 보호자)",
                    value = emergencyContact,
                    onValueChange = { 
                        emergencyContact = it
                        prefs.saveEmergencyContact(it)
                    },
                    placeholder = "010-1234-5678"
                )
                
                Divider(color = Color.LightGray.copy(alpha = 0.3f))
                
                Text(
                    text = "자녀 연락처 목록",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = SettingsTextColor,
                    modifier = Modifier.padding(16.dp)
                )
                
                if (childContacts.isEmpty()) {
                    Text(
                        "등록된 자녀가 없습니다",
                        color = Color.Gray,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        fontSize = 14.sp
                    )
                } else {
                    childContacts.forEachIndexed { index, contact ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(contact.first, color = SettingsTextColor, fontWeight = FontWeight.Medium)
                                Text(contact.second, color = Color.Gray, fontSize = 13.sp)
                            }
                            IconButton(onClick = {
                                val updated = childContacts.toMutableList().apply { removeAt(index) }
                                childContacts = updated
                                prefs.saveChildContacts(updated)
                            }) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "삭제",
                                    tint = Color.Red.copy(alpha = 0.7f),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        if (index < childContacts.size - 1) {
                             Divider(color = Color.LightGray.copy(alpha = 0.3f), modifier = Modifier.padding(horizontal = 16.dp))
                        }
                    }
                }
                
                Button(
                    onClick = { showAddContactDialog = true },
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SettingsPrimaryColor),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("자녀 연락처 추가", color = Color.White)
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun SettingsCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column {
        Text(
            text = title,
            color = SettingsTextColor,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
        )
        Card(
            colors = CardDefaults.cardColors(containerColor = SettingsSurfaceColor),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                content()
            }
        }
    }
}

@Composable
fun SettingSwitchItem(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) } // Clickable row support
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Medium, color = SettingsTextColor, fontSize = 16.sp)
            Spacer(modifier = Modifier.height(2.dp))
            Text(description, color = Color.Gray, fontSize = 13.sp)
        }
        Switch(
            checked = checked,
            onCheckedChange = null, // Handled by row click
            colors = SwitchDefaults.colors(
                checkedThumbColor = SettingsPrimaryColor,
                checkedTrackColor = SettingsPrimaryColor.copy(alpha = 0.3f)
            )
        )
    }
}

@Composable
fun SettingInputItem(
    title: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String
) {
    Column(modifier = Modifier.padding(16.dp)) {
        Text(title, fontWeight = FontWeight.Medium, color = SettingsTextColor, fontSize = 15.sp)
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text(placeholder, fontSize = 14.sp, color = Color.Gray) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = SettingsPrimaryColor,
                unfocusedBorderColor = Color.LightGray
            ),
            shape = RoundedCornerShape(8.dp)
        )
    }
}

@Composable
fun SettingTextItem(title: String, description: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Medium, color = SettingsTextColor, fontSize = 16.sp)
            Spacer(modifier = Modifier.height(2.dp))
            Text(description, color = Color.Gray, fontSize = 13.sp)
        }
    }
}
