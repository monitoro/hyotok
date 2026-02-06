package com.silverpixelism.hyotok.ui

import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Notifications // Added
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.*
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import kotlinx.coroutines.launch

import androidx.compose.ui.unit.dp

data class AppItem(
    val name: String,
    val icon: ImageVector,
    val action: () -> Unit
)

@Composable
fun HomeScreen(
    onNavigateToPairing: () -> Unit,
    onStartScreenShare: () -> Unit
) {
    val context = LocalContext.current
    val aiAssistant = remember { com.silverpixelism.hyotok.ai.AIAssistant("TODO_API_KEY") }
    val scope = rememberCoroutineScope()
    var showDialog by remember { mutableStateOf(false) }
    var aiResponse by remember { mutableStateOf("") }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("효톡 비서") },
            text = { 
                Column {
                    Text(if (aiResponse.isEmpty()) "무엇을 도와드릴까요?" else aiResponse)
                    Button(onClick = {
                        scope.launch {
                            aiResponse = "생각 중..."
                            aiResponse = aiAssistant.generateResponse("어르신을 위한 따뜻한 말 한마디 해줘")
                        }
                    }) {
                        Text("오늘의 좋은 글 요청")
                    }
                }
            },
            confirmButton = {
                Button(onClick = { showDialog = false }) {
                    Text("닫기")
                }
            }
        )
    }

    // Load dynamic apps
    val pm = context.packageManager
    val repository = remember { com.silverpixelism.hyotok.data.AppRepository(context) }
    val preferences = remember { com.silverpixelism.hyotok.data.AppPreferences(context) }
    var dynamicApps by remember { mutableStateOf<List<AppItem>>(emptyList()) }
    
    LaunchedEffect(Unit) {
        val selectedPackages = preferences.getSelectedApps()
        val loadedApps = mutableListOf<AppItem>()
        
        for (pkg in selectedPackages) {
            try {
                val appInfo = pm.getApplicationInfo(pkg, 0)
                val label = pm.getApplicationLabel(appInfo).toString()
                // For simplicity, we use a default icon for dynamic apps in this iteration
                // Ideally we should convert Drawable to ImageVector or use Coil
                loadedApps.add(
                     AppItem(label, Icons.Default.Star) {
                          val launchIntent = pm.getLaunchIntentForPackage(pkg)
                          launchIntent?.let { context.startActivity(it) }
                     }
                )
            } catch (e: Exception) {
               // App might be uninstalled
            }
        }
        dynamicApps = loadedApps
    }

    val fixedApps = listOf(
        AppItem("전화", Icons.Default.Call) {
            val intent = Intent(Intent.ACTION_DIAL)
            context.startActivity(intent)
        },
        AppItem("화면 공유", Icons.Default.Star) {
             onStartScreenShare()
        },
        AppItem("효톡 비서", Icons.Default.Star) {
             showDialog = true
        },
        AppItem("카메라", Icons.Default.Star) {
            val intent = Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA)
            context.startActivity(intent)
        },
        AppItem("갤러리", Icons.Default.Star) {
            val intent = Intent(Intent.ACTION_VIEW, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            context.startActivity(intent)
        }
    )
    
    val allApps = fixedApps + dynamicApps

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 24.dp, top = 24.dp, bottom = 12.dp, end = 24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "효톡 홈",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.weight(1f)
            )
            androidx.compose.material3.IconButton(onClick = { onNavigateToPairing() }) { // Rename this callback or use separate one
                 Icon(Icons.Default.Settings, contentDescription = "Settings")
            }
        }

        // Main Grid Area
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            items(allApps) { app ->
                BigAppCard(app)
            }
        }
        
        // Bottom Quick Contact Area
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(16.dp)
        ) {
            Text(
                "빠른 연결",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                QuickContactButton(name = "큰 아들", color = androidx.compose.ui.graphics.Color(0xFFFFB74D)) {
                     // Action would go here
                     val intent = Intent(Intent.ACTION_DIAL)
                     // intent.data = Uri.parse("tel:01012345678") 
                     context.startActivity(intent)
                }
                QuickContactButton(name = "작은 딸", color = androidx.compose.ui.graphics.Color(0xFF81C784)) {
                     // Action
                     val intent = Intent(Intent.ACTION_DIAL)
                     context.startActivity(intent)
                }
            }
        }
    }
}

@Composable
fun BigAppCard(item: AppItem) {
    androidx.compose.material3.Card(
        modifier = Modifier
            .aspectRatio(1.1f) // Slightly rectangular
            .clickable { item.action() },
        shape = RoundedCornerShape(20.dp),
        colors = androidx.compose.material3.CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        ),
        elevation = androidx.compose.material3.CardDefaults.cardElevation(
            defaultElevation = 4.dp
        )
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = item.icon,
                contentDescription = item.name,
                modifier = Modifier.size(56.dp), // Slightly smaller icon
                tint = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Text(
                text = item.name,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

@Composable
fun QuickContactButton(name: String, color: androidx.compose.ui.graphics.Color, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(androidx.compose.foundation.shape.CircleShape)
                .background(color),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Call,
                contentDescription = "Call $name",
                tint = androidx.compose.ui.graphics.Color.White,
                modifier = Modifier.size(32.dp)
            )
        }
        Text(
            text = name,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}
