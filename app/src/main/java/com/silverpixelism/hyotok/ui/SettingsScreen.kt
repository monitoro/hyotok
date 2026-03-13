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
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.rounded.*
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
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

    // Favorite Apps Selection (Top 6 apps)
    var showFavoriteAppsDialog by remember { mutableStateOf(false) }
    var favoriteApps by remember { mutableStateOf(prefs.getFavoriteApps()) }

    // 홈화면 아이콘 글자 크기
    var iconFontSize by remember { mutableStateOf(prefs.getIconFontSize()) }

    LaunchedEffect(showAppSelectionDialog, showFavoriteAppsDialog) {
        if ((showAppSelectionDialog || showFavoriteAppsDialog) && installedApps.isEmpty()) {
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
    // Favorite Apps Edit Dialog
    if (showFavoriteAppsDialog) {
        Dialog(onDismissRequest = { showFavoriteAppsDialog = false }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF2D3436)),
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
                        text = "즐겨찾기 6개 메뉴 변경",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Text(
                        text = "홈 화면 상단의 6개 아이콘을 변경합니다.\n최대 6개까지만 추가 가능합니다.",
                        color = Color.LightGray,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    // 1. Current Selected Favorites List
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(0.5f)
                            .background(Color.DarkGray, RoundedCornerShape(8.dp))
                            .padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(favoriteApps.size) { index ->
                            val appKey = favoriteApps[index]
                            val label = when (appKey) {
                                "##PHONE##" -> "전화 (기본)"
                                "##MESSAGE##" -> "메시지 (기본)"
                                "##CONTACTS##" -> "연락처 (기본)"
                                "##VOICE_AI##" -> "음성AI (기본)"
                                else -> installedApps.find { it.packageName == appKey }?.name ?: "알 수 없는 앱 ($appKey)"
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFF3B4245), RoundedCornerShape(8.dp))
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${index + 1}. $label",
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    modifier = Modifier.weight(1f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )

                                Row {
                                    IconButton(
                                        onClick = {
                                            if (index > 0) {
                                                val mList = favoriteApps.toMutableList()
                                                val temp = mList[index]
                                                mList[index] = mList[index - 1]
                                                mList[index - 1] = temp
                                                favoriteApps = mList
                                            }
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Rounded.KeyboardArrowUp, null, tint = if (index > 0) Color.White else Color.Gray)
                                    }
                                    IconButton(
                                        onClick = {
                                            if (index < favoriteApps.size - 1) {
                                                val mList = favoriteApps.toMutableList()
                                                val temp = mList[index]
                                                mList[index] = mList[index + 1]
                                                mList[index + 1] = temp
                                                favoriteApps = mList
                                            }
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Rounded.KeyboardArrowDown, null, tint = if (index < favoriteApps.size - 1) Color.White else Color.Gray)
                                    }
                                    IconButton(
                                        onClick = {
                                            val mList = favoriteApps.toMutableList()
                                            mList.removeAt(index)
                                            favoriteApps = mList
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Default.Delete, null, tint = Color.Red.copy(alpha = 0.8f))
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Text("추가할 앱 선택", color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))

                    // 2. All Apps List to Add from
                    if (installedApps.isEmpty()) {
                        Box(modifier = Modifier.weight(0.5f), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = Color.White)
                        }
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(3),
                            modifier = Modifier.weight(0.5f),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Add default special apps manually to the list for selection
                            val specialApps = listOf(
                                Pair("##PHONE##", "전화 (기본)"),
                                Pair("##MESSAGE##", "메시지 (기본)"),
                                Pair("##CONTACTS##", "연락처 (기본)"),
                                Pair("##VOICE_AI##", "음성AI (기본)")
                            )

                            // 1. Special Apps Section
                            items(specialApps.size) { index ->
                                val pair = specialApps[index]
                                val isSelected = favoriteApps.contains(pair.first)

                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .clickable {
                                            if (!isSelected && favoriteApps.size < 6) {
                                                favoriteApps = favoriteApps + pair.first
                                            }
                                        }
                                        .background(
                                            if (isSelected) SettingsPrimaryColor.copy(alpha = 0.3f) else Color.Transparent,
                                            RoundedCornerShape(8.dp)
                                        )
                                        .padding(8.dp)
                                ) {
                                    Box {
                                        Icon(
                                            when (pair.first) {
                                                "##PHONE##" -> Icons.Default.Call
                                                "##MESSAGE##" -> Icons.Rounded.Chat
                                                "##CONTACTS##" -> Icons.Rounded.Person
                                                else -> Icons.Rounded.Mic
                                            },
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(48.dp).padding(8.dp)
                                        )
                                        if (isSelected) {
                                            Icon(
                                                Icons.Rounded.CheckCircle,
                                                null,
                                                tint = SettingsPrimaryColor,
                                                modifier = Modifier.align(Alignment.TopEnd).background(Color.White, CircleShape)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = pair.second,
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        textAlign = TextAlign.Center,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            // 2. Installed Apps Section
                            items(installedApps.size) { index ->
                                val app = installedApps[index]
                                val isSelected = favoriteApps.contains(app.packageName)
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .clickable {
                                            if (!isSelected && favoriteApps.size < 6) {
                                                favoriteApps = favoriteApps + app.packageName
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
                        TextButton(onClick = { showFavoriteAppsDialog = false }) {
                            Text("취소", color = Color.White.copy(alpha = 0.7f))
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                prefs.saveFavoriteApps(favoriteApps)
                                showFavoriteAppsDialog = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = SettingsPrimaryColor)
                        ) {
                            Text("저장", color = Color.White)
                        }
                    }
                }
            }
        }
    }

    // App Selection Dialog (홈화면 앱추가 - 하단 영역)
    if (showAppSelectionDialog) {
        Dialog(onDismissRequest = { showAppSelectionDialog = false }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF2D3436)),
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
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Text(
                        text = "홈 화면 하단에 표시할 앱을 선택하세요.",
                        color = Color.LightGray,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
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
                            items(installedApps.size) { index ->
                                val app = installedApps[index]
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
            // 0. 내 정보 설정 (New) - 컴팩트형 다크 테마 반영
            DarkSettingsCard(title = "👤 내 정보 설정") {
                DarkSettingInputItem(
                    title = "내 이름 (표시용)",
                    value = userName,
                    onValueChange = { 
                        userName = it
                        prefs.saveUserName(it)
                    },
                    placeholder = "예: 김망고"
                )
                DarkSettingSwitchItem(
                    title = "홈 화면에 이름 표시",
                    description = "상단 인사말에 이름을 보여줍니다.",
                    checked = showUserName,
                    onCheckedChange = { 
                        showUserName = it
                        prefs.setUserNameVisible(it)
                    }
                )
                Divider(color = Color.DarkGray, modifier = Modifier.padding(horizontal = 12.dp))
                DarkSettingInputItem(
                    title = "내 전화번호",
                    value = userPhoneNumber,
                    onValueChange = { 
                        userPhoneNumber = it
                        prefs.saveUserPhoneNumber(it)
                    },
                    placeholder = "전화번호 입력"
                )
                DarkSettingSwitchItem(
                    title = "홈 화면에 번호 표시",
                    description = "상단 인사말에 번호를 보여줍니다.",
                    checked = showUserPhoneNumber,
                    onCheckedChange = { 
                        showUserPhoneNumber = it
                        prefs.setUserPhoneNumberVisible(it)
                    }
                )
                if (!showUserName && !showUserPhoneNumber) {
                     Text(
                        text = "💡 설정이 비활성화되면 날씨 인사말만 나타납니다.",
                        color = Color(0xFFA5B4FC), // Light Indigo
                        fontSize = 11.sp,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
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
                    title = "즐겨찾기 메뉴 6개 변경",
                    description = "상단 6개 기본 메뉴를 바꾸거나 순서를 편집합니다.",
                    onClick = {
                        favoriteApps = prefs.getFavoriteApps()
                        showFavoriteAppsDialog = true
                    }
                )
                Divider(color = Color.LightGray.copy(alpha = 0.3f))
                SettingTextItem(
                    title = "홈 화면 앱추가",
                    description = "${selectedApps.size}개의 앱이 선택됨 (하단영역)",
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
                Divider(color = Color.LightGray.copy(alpha = 0.3f))
                // 홈화면 아이콘 글자 크기 슬라이더
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("아이콘 글자 크기", fontWeight = FontWeight.Medium, color = SettingsTextColor, fontSize = 16.sp)
                            Text("현재: ${iconFontSize.toInt()}sp", color = Color.Gray, fontSize = 13.sp)
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("12", fontSize = 12.sp, color = Color.Gray)
                        androidx.compose.material3.Slider(
                            value = iconFontSize,
                            onValueChange = { 
                                val newSize = kotlin.math.round(it)
                                iconFontSize = newSize
                            },
                            onValueChangeFinished = {
                                prefs.saveIconFontSize(iconFontSize)
                            },
                            valueRange = 12f..22f,
                            steps = 9,
                            modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
                        )
                        Text("22", fontSize = 12.sp, color = Color.Gray)
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun ChildContactItem(contact: Pair<String, String>, index: Int, showDivider: Boolean, onRemove: () -> Unit) {
    Column {
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
            IconButton(onClick = onRemove) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "삭제",
                    tint = Color.Red.copy(alpha = 0.7f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        if (showDivider) {
             Divider(color = Color.LightGray.copy(alpha = 0.3f), modifier = Modifier.padding(horizontal = 16.dp))
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

@Composable
fun DarkSettingsCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column {
        Text(
            text = title,
            color = SettingsTextColor,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
        )
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF2D3436)),
            shape = RoundedCornerShape(12.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                content()
            }
        }
    }
}

@Composable
fun DarkSettingInputItem(
    title: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String
) {
    Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)) {
        Text(title, fontWeight = FontWeight.Medium, color = Color.White, fontSize = 13.sp)
        Spacer(modifier = Modifier.height(4.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text(placeholder, fontSize = 12.sp, color = Color.Gray) },
            modifier = Modifier.fillMaxWidth().height(52.dp),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = SettingsPrimaryColor,
                unfocusedBorderColor = Color.DarkGray,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent
            ),
            shape = RoundedCornerShape(8.dp),
            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 13.sp)
        )
    }
}

@Composable
fun DarkSettingSwitchItem(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Medium, color = Color.White, fontSize = 13.sp)
            Spacer(modifier = Modifier.height(2.dp))
            Text(description, color = Color.LightGray, fontSize = 11.sp, lineHeight = 14.sp)
        }
        Switch(
            checked = checked,
            onCheckedChange = null,
            modifier = Modifier.scale(0.85f),
            colors = SwitchDefaults.colors(
                checkedThumbColor = SettingsPrimaryColor,
                checkedTrackColor = SettingsPrimaryColor.copy(alpha = 0.5f),
                uncheckedThumbColor = Color.LightGray,
                uncheckedTrackColor = Color.DarkGray
            )
        )
    }
}
