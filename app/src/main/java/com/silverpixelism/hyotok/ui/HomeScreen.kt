package com.silverpixelism.hyotok.ui

import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

// Color Palette from Mockup
val DarkNavy = Color(0xFF1A1F36) // Deep Blue Background
val BrightYellow = Color(0xFFFFD54F) // Button Yellow
val White = Color(0xFFFFFFFF)
val DarkText = Color(0xFF1E1E1E) // Text on Yellow
val GreyButton = Color(0xFF455A64) // Secondary Button? Or Help?

data class HomeGridItem(
    val title: String,
    val icon: ImageVector? = null,
    val backgroundColor: Color,
    val contentColor: Color,
    val onClick: () -> Unit
)

@Composable
fun HomeScreen(
    onNavigateToPairing: () -> Unit,
    onStartScreenShare: () -> Unit,
    onNavigateToSettings: () -> Unit = {}
) {
    val context = LocalContext.current
    var showDialog by remember { mutableStateOf(false) }

    // Child Selection Dialog
    var showChildDialog by remember { mutableStateOf(false) }
    
    if (showChildDialog) {
        AlertDialog(
            onDismissRequest = { showChildDialog = false },
            title = { Text("누구에게 연결할까요?") },
            text = {
                Column {
                    listOf("첫째 아들", "둘째 딸", "막내 아들").forEachIndexed { index, name ->
                        Button(
                            onClick = {
                                showChildDialog = false
                                // 1. Navigate to Pairing Screen (to show code/wait)
                                onNavigateToPairing()
                                
                                // 2. Launch Call (Dial)
                                // Mock Numbers
                                val number = when(index) {
                                    0 -> "010-1234-5678"
                                    1 -> "010-9876-5432"
                                    else -> "010-1111-2222"
                                }
                                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$number"))
                                context.startActivity(intent)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                        ) {
                            Text(name, fontSize = 20.sp)
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { showChildDialog = false }) {
                    Text("취소")
                }
            }
        )
    }

    // AI Assistant Logic
    if (showDialog) {
        val aiAssistant = remember { com.silverpixelism.hyotok.ai.AIAssistant("TODO_API_KEY") }
        val scope = rememberCoroutineScope()
        var aiResponse by remember { mutableStateOf("") }
        var isLoading by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("효톡 비서") },
            text = {
                Column {
                   if (isLoading) {
                       CircularProgressIndicator()
                       Spacer(modifier = Modifier.height(8.dp))
                       Text("생각 중입니다...")
                   } else {
                       Text(if (aiResponse.isEmpty()) "무엇을 도와드릴까요? (오늘의 운세, 날씨 등)" else aiResponse)
                   }
                }
            },
            confirmButton = {
                Button(onClick = {
                    showDialog = false
                    aiResponse = ""
                }) {
                    Text("닫기")
                }
            },
            dismissButton = {
                Button(onClick = {
                    isLoading = true
                    scope.launch {
                        try {
                            aiResponse = aiAssistant.generateResponse("어르신을 위한 긍정적이고 따뜻한 말씀 한마디 해줘")
                        } catch(e: Exception) {
                            aiResponse = "죄송합니다. 오류가 발생했습니다."
                        } finally {
                            isLoading = false
                        }
                    }
                }) {
                    Text("좋은 말씀 요청")
                }
            }
        )
    }

    // Grid Items
    val gridItems = listOf(
        HomeGridItem(
            title = "전화",
            icon = Icons.Rounded.Phone,
            backgroundColor = Color(0xFF4CAF50), // Green
            contentColor = White,
            onClick = {
                val intent = Intent(Intent.ACTION_DIAL)
                context.startActivity(intent)
            }
        ),
        HomeGridItem(
            title = "메세지",
            icon = Icons.Rounded.Email,
            backgroundColor = Color(0xFF2196F3), // Blue
            contentColor = White,
            onClick = {
                try {
                    val intent = Intent(Intent.ACTION_MAIN).apply {
                        addCategory(Intent.CATEGORY_APP_MESSAGING)
                    }
                    context.startActivity(intent)
                } catch (e: Exception) {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("sms:"))
                    context.startActivity(intent)
                }
            }
        ),
        HomeGridItem(
            title = "카톡",
            icon = Icons.Rounded.Email,
            backgroundColor = BrightYellow,
            contentColor = DarkText,
            onClick = {
                try {
                    val intent = context.packageManager.getLaunchIntentForPackage("com.kakao.talk")
                    if (intent != null) {
                        context.startActivity(intent)
                    }
                } catch (e: Exception) {}
            }
        ),
        HomeGridItem(
            title = "카메라",
            icon = Icons.Rounded.Face,
            backgroundColor = BrightYellow,
            contentColor = DarkText,
            onClick = {
                val intent = Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA)
                context.startActivity(intent)
            }
        ),
        HomeGridItem(
            title = "유튜브",
            icon = Icons.Rounded.PlayArrow,
            backgroundColor = BrightYellow,
            contentColor = DarkText,
            onClick = {
                try {
                    val intent = context.packageManager.getLaunchIntentForPackage("com.google.android.youtube")
                    if (intent != null) {
                        context.startActivity(intent)
                    } else {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com")))
                    }
                } catch (e: Exception) {}
            }
        ),
        HomeGridItem(
            title = "날씨",
            icon = Icons.Rounded.Info,
            backgroundColor = BrightYellow,
            contentColor = DarkText,
            onClick = {
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://search.naver.com/search.naver?query=날씨")))
            }
        ),
        HomeGridItem(
            title = "가족전화",
            icon = Icons.Rounded.Star,
            backgroundColor = Color(0xFFFF9800), // Orange
            contentColor = White,
            onClick = {
                // Open contacts favorites tab
                try {
                    val intent = Intent(Intent.ACTION_VIEW).apply {
                        data = android.provider.ContactsContract.Contacts.CONTENT_URI
                        putExtra("android.provider.extra.CONTENT_FILTER_TYPE", 1) // Favorites
                    }
                    context.startActivity(intent)
                } catch (e: Exception) {
                    val intent = Intent(Intent.ACTION_VIEW, android.provider.ContactsContract.Contacts.CONTENT_URI)
                    context.startActivity(intent)
                }
            }
        ),
        HomeGridItem(
            title = "자녀도움",
            icon = Icons.Rounded.Person,
            backgroundColor = Color(0xFF9C27B0), // Purple
            contentColor = White,
            onClick = {
                onNavigateToPairing()
            }
        )
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkNavy)
            .padding(16.dp)
    ) {
        // Top Bar (Settings)
        Box(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            contentAlignment = Alignment.CenterEnd
        ) {
            IconButton(
                onClick = { onNavigateToSettings() },
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.2f))
            ) {
                 Icon(Icons.Default.Settings, contentDescription = "설정", tint = Color.White)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Clock Header
        ClockHeader()

        Spacer(modifier = Modifier.height(32.dp))

        // Grid
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(gridItems) { item ->
                LargeAppButton(item)
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))

        // AI Assistant Button (Bottom)
        Button(
            onClick = { showDialog = true },
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp),
            shape = RoundedCornerShape(24.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E88E5)) // Blue for AI
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Rounded.Face, contentDescription = null, tint = White, modifier = Modifier.size(32.dp))
                Text("AI 비서 (무엇이든 물어보세요)", fontSize = 18.sp, color = White)
            }
        }
    }
}

@Composable
fun ClockHeader() {
    var currentTime by remember { mutableStateOf(System.currentTimeMillis()) }
    
    LaunchedEffect(Unit) {
        while(true) {
            currentTime = System.currentTimeMillis()
            delay(1000) // Update every second
        }
    }
    
    // Format: 오전 12:33
    // Format: 2026년 2월 3일 화요일
    val timeFormat = SimpleDateFormat("a hh:mm", Locale.KOREAN)
    val dateFormat = SimpleDateFormat("yyyy년 M월 d일 EEEE", Locale.KOREAN)
    
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = timeFormat.format(Date(currentTime)),
            style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.Bold),
            color = White,
            fontSize = 48.sp
        )
        Text(
            text = dateFormat.format(Date(currentTime)),
            style = MaterialTheme.typography.titleMedium,
            color = White.copy(alpha = 0.8f),
            fontSize = 18.sp
        )
    }
}

@Composable
fun LargeAppButton(item: HomeGridItem) {
    Card(
        modifier = Modifier
            .aspectRatio(1.8f) // Wider, less tall
            .clickable { item.onClick() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = item.backgroundColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                if (item.icon != null) {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = null,
                        modifier = Modifier.size(40.dp), // Reduced slightly
                        tint = item.contentColor
                    )
                    Spacer(modifier = Modifier.height(4.dp)) // Reduced spacing
                } else if (item.title == "?") {
                    // Special case for '?' to look big
                     Text(
                        text = item.title,
                        fontSize = 48.sp, // Reduced slightly
                        fontWeight = FontWeight.Bold,
                        color = item.contentColor
                    )
                }
                
                if (item.title != "?") {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                        color = item.contentColor,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}
