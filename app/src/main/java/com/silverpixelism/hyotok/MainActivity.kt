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
import com.silverpixelism.hyotok.ui.SettingsScreen
import com.silverpixelism.hyotok.ui.theme.HyoTalkTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.navigation.NavType
import androidx.navigation.navArgument
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {

    private val mediaProjectionManager by lazy {
        getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
    }
    
    // State to trigger navigation after permission is granted
    private val _shouldNavigateToHome = mutableStateOf(false)

    private val startMediaProjection = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            startScreenCaptureService(result.resultCode, result.data!!)
            // Signal Compose UI to navigate back after permission granted
            _shouldNavigateToHome.value = true
        }
    }

    private fun startScreenCaptureService(resultCode: Int, data: Intent) {
        val prefs = com.silverpixelism.hyotok.data.AppPreferences(this)
        val pairingCode = prefs.getPairingCode() ?: "test_code"

        val intent = Intent(this, ScreenCaptureService::class.java).apply {
            putExtra("code", resultCode)
            putExtra("permissionIntent", data)
            putExtra("pairingCode", pairingCode)
        }
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }

        // Start Overlay Service for pointer display
        if (android.provider.Settings.canDrawOverlays(this)) {
            val overlayIntent = Intent(this, com.silverpixelism.hyotok.service.OverlayService::class.java).apply {
                putExtra("pairingCode", pairingCode)
            }
            startService(overlayIntent)
        }
    }

    private val overlayPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        // Overlay permission granted result
    }

    private val requestAudioPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        startMediaProjection.launch(mediaProjectionManager.createScreenCaptureIntent())
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
                    
                    // Auto-navigate to home when screen share permission is granted
                    val shouldNavigateHome by remember { _shouldNavigateToHome }
                    androidx.compose.runtime.LaunchedEffect(shouldNavigateHome) {
                        if (shouldNavigateHome) {
                            navController.popBackStack("home", inclusive = false)
                            _shouldNavigateToHome.value = false
                        }
                    }
                    
                    NavHost(navController = navController, startDestination = "home") {
                        composable("home") {
                            HomeScreen(
                                onNavigateToPairing = { navController.navigate("pair") },
                                onStartScreenShare = {
                                    if (androidx.core.content.ContextCompat.checkSelfPermission(
                                            this@MainActivity,
                                            android.Manifest.permission.RECORD_AUDIO
                                        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                                    ) {
                                        startMediaProjection.launch(mediaProjectionManager.createScreenCaptureIntent())
                                    } else {
                                        requestAudioPermissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
                                    }
                                },
                                onNavigateToSettings = { navController.navigate("settings") }
                            )
                        }
                        composable("settings") {
                            SettingsScreen(onBack = { navController.popBackStack() })
                        }
                        composable("pair") {
                            // Permission handling for Child
                            val context = androidx.compose.ui.platform.LocalContext.current
                            var pendingCode by remember { mutableStateOf<String?>(null) }
                            
                            val permissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
                                androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
                            ) { isGranted: Boolean ->
                                pendingCode?.let { code ->
                                    navController.navigate("stream_viewer/$code")
                                }
                                pendingCode = null
                            }

                            PairingScreen(
                                onBack = { navController.popBackStack() },
                                onSaveCode = { code ->
                                    val prefs = com.silverpixelism.hyotok.data.AppPreferences(this@MainActivity)
                                    prefs.savePairingCode(code)
                                },
                                onStartShare = {
                                    android.util.Log.d("MainActivity", "onStartShare called!")
                                    android.widget.Toast.makeText(this@MainActivity, "화면 공유 시작 요청", android.widget.Toast.LENGTH_SHORT).show()
                                    
                                    runOnUiThread {
                                        if (androidx.core.content.ContextCompat.checkSelfPermission(
                                                this@MainActivity,
                                                android.Manifest.permission.RECORD_AUDIO
                                            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                                        ) {
                                            android.util.Log.d("MainActivity", "Launching MediaProjection")
                                            startMediaProjection.launch(mediaProjectionManager.createScreenCaptureIntent())
                                        } else {
                                            android.util.Log.d("MainActivity", "Requesting Audio Permission")
                                            requestAudioPermissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
                                        }
                                    }
                                },
                                onChildConnected = { code ->
                                    // Navigate to StreamViewerScreen directly
                                    android.widget.Toast.makeText(this@MainActivity, "3. onChildConnected 호출됨! code=$code", android.widget.Toast.LENGTH_LONG).show()
                                    navController.navigate("stream_viewer/$code")
                                }
                            )
                        }
                        composable(
                            "stream_viewer/{code}",
                            arguments = listOf(androidx.navigation.navArgument("code") { type = androidx.navigation.NavType.StringType })
                        ) { backStackEntry ->
                            val code = backStackEntry.arguments?.getString("code") ?: ""
                            com.silverpixelism.hyotok.ui.StreamViewerScreen(
                                pairingCode = code,
                                onBack = { navController.popBackStack() }
                            )
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
