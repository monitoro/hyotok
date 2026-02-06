package com.silverpixelism.hyotok

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.silverpixelism.hyotok.service.SafetyWorker
import com.silverpixelism.hyotok.service.ScreenCaptureService
import com.silverpixelism.hyotok.ui.AppSelectionScreen
import com.silverpixelism.hyotok.ui.HomeScreen
import com.silverpixelism.hyotok.ui.PairingScreen
import com.silverpixelism.hyotok.ui.theme.HyoTalkTheme
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {

    private val mediaProjectionManager by lazy {
        getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
    }

    private val startMediaProjection = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            startScreenCaptureService(result.resultCode, result.data!!)
        }
    }

    private fun startScreenCaptureService(resultCode: Int, data: Intent) {
        val intent = Intent(this, ScreenCaptureService::class.java).apply {
            putExtra("code", resultCode)
            putExtra("permissionIntent", data)
        }
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }

        // Also start Overlay Service
        if (android.provider.Settings.canDrawOverlays(this)) {
            startService(Intent(this, com.silverpixelism.hyotok.service.OverlayService::class.java))
        }
    }

    private val overlayPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        // Overlay permission granted result
    }

    private fun checkAndRequestOverlayPermission() {
        if (!android.provider.Settings.canDrawOverlays(this)) {
            val intent = Intent(android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                android.net.Uri.parse("package:$packageName"))
            overlayPermissionLauncher.launch(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        checkAndRequestOverlayPermission()

        // Safety Worker (Every 15 minutes)
        val safetyRequest = PeriodicWorkRequestBuilder<SafetyWorker>(15, TimeUnit.MINUTES).build()
        WorkManager.getInstance(this).enqueue(safetyRequest)

        setContent {
            HyoTalkTheme {
                Surface(
                    modifier = Modifier.fillMaxSize()
                ) {
                    val navController = rememberNavController()
                    NavHost(navController = navController, startDestination = "home") {
                        composable("home") {
                            HomeScreen(
                                onNavigateToPairing = { navController.navigate("settings") },
                                onStartScreenShare = {
                                    startMediaProjection.launch(mediaProjectionManager.createScreenCaptureIntent())
                                }
                            )
                        }
                        composable("pair") {
                            PairingScreen(onBack = { navController.popBackStack() })
                        }
                        composable("settings") {
                            // Temporary Settings Menu
                            Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                                Text("설정", style = MaterialTheme.typography.headlineMedium)
                                
                                Button(
                                    onClick = { navController.navigate("pair") },
                                    modifier = Modifier.padding(top = 16.dp)
                                ) {
                                    Text("자녀 연결")
                                }
                                
                                // Notification settings button removed
                                
                                Button(
                                    onClick = { navController.navigate("app_selection") },
                                    modifier = Modifier.padding(top = 8.dp)
                                ) {
                                    Text("홈 화면 앱 선택")
                                }
                                
                                Button(
                                    onClick = { navController.popBackStack() },
                                    modifier = Modifier.padding(top = 24.dp)
                                ) {
                                    Text("닫기")
                                }
                            }
                        }
                        composable("app_selection") {
                            AppSelectionScreen(onBack = { navController.popBackStack() })
                        }
                    }
                }
            }
        }
    }
}
