package com.silverpixelism.hyotok.ui

import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.LocationServices
import com.silverpixelism.hyotok.data.WeatherRepository
import com.silverpixelism.hyotok.data.AppPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.Chat
import androidx.compose.material.icons.rounded.VolunteerActivism
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.draw.shadow
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.silverpixelism.hyotok.data.AppRepository
import com.silverpixelism.hyotok.data.ContactInfo

@Composable
fun ClockWidget() {
    val context = LocalContext.current
    var currentTime by remember { mutableStateOf(System.currentTimeMillis()) }
    var weatherInfo by remember { mutableStateOf<Pair<String, String>?>(null) } // "맑음", "24°C"
    var weatherIcon by remember { mutableStateOf("❓") }
    
    // Time Update
    LaunchedEffect(Unit) {
        while(true) {
            currentTime = System.currentTimeMillis()
            delay(1000)
        }
    }

    // Weather Update (Location Based)
    LaunchedEffect(Unit) {
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch {
                        val weather = WeatherRepository.getCurrentWeather(location.latitude, location.longitude)
                        withContext(Dispatchers.Main) {
                            if (weather != null) {
                                val (desc, icon) = WeatherRepository.getWeatherInfo(weather.weatherCode)
                                weatherIcon = icon
                                weatherInfo = desc to "${weather.temperature}°C"
                            }
                        }
                    }
                }
            }
        }
    }
    
    val timeFormat = SimpleDateFormat("a h:mm", Locale.KOREAN)
    val dateFormat = SimpleDateFormat("yyyy년 M월 d일 EEEE", Locale.KOREAN) // Added Year
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp)
            .shadow(8.dp, RoundedCornerShape(32.dp))
            .clip(RoundedCornerShape(32.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(Color(0xFF6C5CE7), Color(0xFFA29BFE))
                )
            )
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = timeFormat.format(Date(currentTime)),
                fontSize = 48.sp, // Reduced font size (52 -> 48)
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = dateFormat.format(Date(currentTime)),
                fontSize = 16.sp, // Reduced font size (18 -> 16)
                color = Color.White.copy(alpha = 0.9f)
            )
            Spacer(modifier = Modifier.height(12.dp)) // Increased spacer
            
            // Weather Info
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = weatherIcon, fontSize = 20.sp)
                Spacer(modifier = Modifier.width(8.dp))
                if (weatherInfo != null) {
                    Text(
                        text = "${weatherInfo!!.first} ${weatherInfo!!.second}",
                        fontSize = 16.sp,
                        color = Color.White.copy(alpha = 0.95f),
                        fontWeight = FontWeight.Medium
                    )
                } else {
                    Text(
                        text = "날씨 정보 없음", // Or "위치 권한 필요"
                        fontSize = 14.sp,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}


// Trendy Color Palette (Pastel & Vibrant)
val BackgroundColor = Color(0xFFF8F9FA) // Soft White/Grey
val SurfaceColor = Color(0xFFFFFFFF)
val TextPrimary = Color(0xFF2D3436)
val TextSecondary = Color(0xFF636E72)

// Legacy Colors (for SettingsScreen compatibility)
val DarkNavy = Color(0xFF1A1F36)
val BrightYellow = Color(0xFFFFD54F)

// App Colors
val ColorPhone = Color(0xFF00B894) // Mint Green
val ColorMessage = Color(0xFF0984E3) // Electron Blue
val ColorKakao = Color(0xFFFEE500) // Kakao Yellow
val ColorCamera = Color(0xFFFF7675) // Pinkish Red
val ColorYoutube = Color(0xFFD63031) // YouTube Red
val ColorGallery = Color(0xFF6C5CE7) // Purple
val ColorHelp = Color(0xFF2D3436) // Dark Grey/Black for contrast
val ColorFamily = Color(0xFFFF9F43) // Orange

// Data class for Grid Items
data class HomeGridItem(
    val title: String,
    val icon: ImageVector? = null,
    val backgroundColor: Color,
    val contentColor: Color,
    val packageName: String? = null,
    val onClick: () -> Unit
)

@Composable
fun HomeScreen(
    onNavigateToPairing: () -> Unit,
    onStartScreenShare: () -> Unit,
    onNavigateToSettings: () -> Unit = {}
) {
    val context = LocalContext.current
    val repository = remember { AppRepository(context) }
    
    // Permission State
    val contactsPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        // Refresh contacts if granted
    }
    
    // Contacts State
    var favoriteContacts by remember { mutableStateOf<List<ContactInfo>>(emptyList()) }
    
    LaunchedEffect(Unit) {
        try {
            favoriteContacts = repository.getFavoriteContacts()
        } catch (e: Exception) {
            // Permission might not be granted yet
        }
    }
    
    // Refresh contacts periodically or on resume logic conceptually
    DisposableEffect(Unit) {
        val observer = object : androidx.lifecycle.DefaultLifecycleObserver {
            override fun onResume(owner: androidx.lifecycle.LifecycleOwner) {
                try {
                     favoriteContacts = repository.getFavoriteContacts()
                } catch(e: Exception) {}
            }
        }
        val lifecycle = (context as? androidx.activity.ComponentActivity)?.lifecycle
        lifecycle?.addObserver(observer)
        onDispose {
            lifecycle?.removeObserver(observer)
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundColor)
            .padding(horizontal = 24.dp),
        contentPadding = PaddingValues(top = 24.dp, bottom = 48.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp) // Reduced spacing
    ) {
        // 1. Header Section
        item {
            HeaderSection(onNavigateToSettings)
        }
        
        // 2. Search Section Removed
        
        // 3. Clock Widget
        item {
            ClockWidget()
        }
        
        // 4. Favorites (Horizontal Scroll)
        item {
            FavoritesSection(
                contacts = favoriteContacts,
                onRequirePermission = { contactsPermissionLauncher.launch(android.Manifest.permission.READ_CONTACTS) },
                onContactClick = { contact ->
                    if (!contact.phoneNumber.isNullOrEmpty()) {
                        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${contact.phoneNumber}"))
                        context.startActivity(intent)
                    }
                }
            )
        }
        
        // 5. Basic App Grid (Top)
        item {
            BasicAppsGrid(context)
        }
        
        // 6. Action Buttons (Middle)
        item {
            ActionButtonsSection(context, onNavigateToPairing)
        }
        
        // 7. Extended App Grid (Bottom - User Added Apps)
        item {
            ExtendedAppsGrid(context)
        }
        
        // Bottom Action Section Removed (Moved to Middle)
    }
}

@Composable
fun HeaderSection(onSettingsClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "반갑습니다",
                fontSize = 16.sp,
                color = TextSecondary
            )
            Text(
                text = "김망고님", // TODO: Get from preferences
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        }
        IconButton(
            onClick = onSettingsClick,
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(Color.White)
                .shadow(4.dp, CircleShape)
        ) {
            Icon(Icons.Filled.Settings, contentDescription = "설정", tint = TextPrimary)
        }
    }
}



@Composable
fun FavoritesSection(
    contacts: List<ContactInfo>,
    onRequirePermission: () -> Unit,
    onContactClick: (ContactInfo) -> Unit
) {
    Column {
        Text(
            text = "즐겨찾는 연락처",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(horizontal = 4.dp)
        ) {
            if (contacts.isEmpty()) {
                item {
                    FavoriteItem(
                        name = "권한 허용",
                        color = Color(0xFFDFE6E9),
                        icon = Icons.Rounded.Add,
                        onClick = onRequirePermission
                    )
                }
            } else {
                items(contacts) { contact ->
                    FavoriteContactItem(contact, onContactClick)
                }
            }
        }
    }
}

@Composable
fun FavoriteContactItem(contact: ContactInfo, onClick: (ContactInfo) -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .shadow(4.dp, RoundedCornerShape(24.dp))
                .clip(RoundedCornerShape(24.dp))
                .background(Color.White)
                .clickable { onClick(contact) },
            contentAlignment = Alignment.Center
        ) {
            if (contact.photoUri != null) {
                Image(
                    painter = rememberAsyncImagePainter(contact.photoUri),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Icon(
                    Icons.Rounded.Person,
                    contentDescription = null,
                    tint = Color(0xFFB2BEC3),
                    modifier = Modifier.size(36.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = contact.name,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = TextPrimary,
            maxLines = 1
        )
    }
}

@Composable
fun FavoriteItem(name: String, color: Color, icon: ImageVector, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(72.dp) // Square
                .shadow(2.dp, RoundedCornerShape(24.dp))
                .clip(RoundedCornerShape(24.dp))
                .background(color)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(32.dp))
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(name, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
    }
}

@Composable
fun BasicAppsGrid(context: android.content.Context) {
    val prefs = remember { AppPreferences(context) }
    val hapticEnabled = prefs.isHapticEnabled()
    val familyChatUrl = prefs.getFamilyChatUrl()
    
    val performHaptic = {
        if (hapticEnabled) {
             val vibrator = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(android.content.Context.VIBRATOR_MANAGER_SERVICE) as android.os.VibratorManager
                vibratorManager.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(android.content.Context.VIBRATOR_SERVICE) as android.os.Vibrator
            }
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                vibrator.vibrate(android.os.VibrationEffect.createOneShot(50, android.os.VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(50)
            }
        }
    }

    val apps = mutableListOf<HomeGridItem>()

    // 1. Phone
    apps.add(HomeGridItem("전화", Icons.Rounded.Call, ColorPhone, Color.White, null) {
        performHaptic()
        context.startActivity(Intent(Intent.ACTION_DIAL))
    })

    // 2. Messages
    apps.add(HomeGridItem("메세지", Icons.Rounded.Email, ColorMessage, Color.White, null) {
        performHaptic()
        try {
            val intent = Intent(Intent.ACTION_MAIN).apply { addCategory(Intent.CATEGORY_APP_MESSAGING) }
            context.startActivity(intent)
        } catch (e: Exception) {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("sms:")))
        }
    })

    // 3. Family Phone (Address Book Group)
    apps.add(HomeGridItem("가족전화", Icons.Rounded.Star, ColorFamily, Color.White, null) {
         performHaptic()
         try {
             val intent = Intent(Intent.ACTION_VIEW).apply {
                data = android.provider.ContactsContract.Contacts.CONTENT_URI
                putExtra("android.provider.extra.CONTENT_FILTER_TYPE", 1)
             }
             context.startActivity(intent)
        } catch(e: Exception) {}
    })
    
     // 4. KakaoTalk
    apps.add(HomeGridItem("카카오톡", Icons.Rounded.Chat, ColorKakao, Color.Black, "com.kakao.talk") {
        performHaptic()
        try {
            val intent = context.packageManager.getLaunchIntentForPackage("com.kakao.talk")
            if (intent != null) context.startActivity(intent)
        } catch(e: Exception) {}
    })
    
    // 5. YouTube
    apps.add(HomeGridItem("유튜브", Icons.Rounded.PlayArrow, ColorYoutube, Color.White, "com.google.android.youtube") {
        performHaptic()
        try {
            val intent = context.packageManager.getLaunchIntentForPackage("com.google.android.youtube")
            if (intent != null) context.startActivity(intent)
        } catch(e: Exception) {}
    })
    
    // 6. Voice AI
    apps.add(HomeGridItem("음성AI", Icons.Rounded.Mic, Color(0xFF6C5CE7), Color.White, null) {
        performHaptic()
        try {
            // Try launching Google Assistant
             val intent = Intent(Intent.ACTION_VOICE_COMMAND)
             intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
             context.startActivity(intent)
        } catch(e: Exception) {
             android.widget.Toast.makeText(context, "음성 비서를 실행할 수 없습니다.", android.widget.Toast.LENGTH_SHORT).show()
        }
    })
    
    val rows = apps.chunked(3)
    
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        for (rowApps in rows) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                for (app in rowApps) {
                    AppIconCard(app, modifier = Modifier.weight(1f))
                }
                repeat(3 - rowApps.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
fun ExtendedAppsGrid(context: android.content.Context) {
    val prefs = remember { AppPreferences(context) }
    val hapticEnabled = prefs.isHapticEnabled()
    val homeAppPackages = prefs.getHomeApps()
    val pm = context.packageManager
    
    val performHaptic = {
        if (hapticEnabled) {
             val vibrator = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(android.content.Context.VIBRATOR_MANAGER_SERVICE) as android.os.VibratorManager
                vibratorManager.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(android.content.Context.VIBRATOR_SERVICE) as android.os.Vibrator
            }
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                vibrator.vibrate(android.os.VibrationEffect.createOneShot(50, android.os.VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(50)
            }
        }
    }

    val apps = mutableListOf<HomeGridItem>()
    
    for (packageName in homeAppPackages) {
        // Filter out basic apps that are already in BasicAppsGrid
        if (packageName == "com.android.dialer") continue
        if (packageName == "com.android.mms") continue
        if (packageName == "com.kakao.talk") continue // Already in Basic
        if (packageName == "com.google.android.youtube") continue // Already in Basic
        
        // Generic App Handling
        try {
            val appInfo = pm.getApplicationInfo(packageName, 0)
            val label = pm.getApplicationLabel(appInfo).toString()
            
            // Background Colors for some popular apps
            val bgColor = when(packageName) {
                "com.kakao.talk" -> ColorKakao
                "com.google.android.youtube" -> ColorYoutube
                "com.sec.android.app.camera" -> ColorCamera
                "com.sec.android.gallery3d" -> ColorGallery
                else -> Color.White
            }
            val contentColor = if(bgColor == ColorKakao) Color.Black else if (bgColor == Color.White) Color.Black else Color.White
            
            apps.add(HomeGridItem(
                title = label,
                icon = null,
                backgroundColor = bgColor,
                contentColor = contentColor,
                packageName = packageName,
                onClick = {
                    performHaptic()
                    val intent = pm.getLaunchIntentForPackage(packageName)
                    if (intent != null) context.startActivity(intent)
                }
            ))
        } catch (e: Exception) {
            // App not found
        }
    }
    
    if (apps.isNotEmpty()) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            // Title for Extended Apps
            Text(
                text = "자주 쓰는 앱",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
            )
            
            val rows = apps.chunked(3)
            for (rowApps in rows) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    for (app in rowApps) {
                        AppIconCard(app, modifier = Modifier.weight(1f))
                    }
                    repeat(3 - rowApps.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
fun AppIconCard(item: HomeGridItem, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .aspectRatio(1f)
                .padding(6.dp)
                .shadow(6.dp, RoundedCornerShape(24.dp))
                .clip(RoundedCornerShape(24.dp))
                .background(item.backgroundColor)
                .clickable { item.onClick() },
            contentAlignment = Alignment.Center
        ) {
            if (item.packageName != null) {
                 val context = LocalContext.current
                 val pm = context.packageManager
                 
                 var appIcon by remember(item.packageName) { mutableStateOf<android.graphics.drawable.Drawable?>(null) }
                 
                 DisposableEffect(item.packageName) {
                     try {
                         appIcon = pm.getApplicationIcon(item.packageName)
                     } catch(e: Exception) {
                         appIcon = null
                     }
                     onDispose {}
                 }

                 if (appIcon != null) {
                     Image(
                        painter = com.google.accompanist.drawablepainter.rememberDrawablePainter(appIcon!!),
                        contentDescription = null,
                        modifier = Modifier.size(48.dp)
                     )
                 } else {
                     Icon(Icons.Rounded.Android, contentDescription = null, tint = item.contentColor)
                 }
            } else {
                Icon(
                    imageVector = item.icon ?: Icons.Rounded.Info,
                    contentDescription = null,
                    tint = item.contentColor,
                    modifier = Modifier.size(32.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = item.title,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary,
            maxLines = 1,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
        )
    }
}

@Composable
fun ActionButtonsSection(context: android.content.Context, onNavigateToPairing: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Row 1: Help & Camera
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Help Button
            Button(
                onClick = onNavigateToPairing,
                modifier = Modifier
                    .weight(1f)
                    .height(80.dp)
                    .shadow(4.dp, RoundedCornerShape(24.dp)),
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(containerColor = ColorHelp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.VolunteerActivism, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("도움받기", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
            
            // Camera Button
            Button(
                onClick = {
                    val intent = Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA)
                    context.startActivity(intent)
                },
                modifier = Modifier
                    .weight(1f)
                    .height(80.dp)
                    .shadow(4.dp, RoundedCornerShape(24.dp)),
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(containerColor = ColorCamera)
            ) {
                 Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.CameraAlt, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("카메라", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
        
        // Row 2: Send Photo & Gallery
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Send Photo Button
             Button(
                onClick = {
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "image/*"
                    }
                    context.startActivity(Intent.createChooser(intent, "사진 공유"))
                },
                modifier = Modifier
                    .weight(1f)
                    .height(80.dp)
                    .shadow(4.dp, RoundedCornerShape(24.dp)),
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0984E3)) // Blue
            ) {
                 Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Send, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("사진보내기", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
            
            // Gallery Button
            Button(
                onClick = {
                    val intent = Intent(Intent.ACTION_VIEW, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                    context.startActivity(intent)
                },
                modifier = Modifier
                    .weight(1f)
                    .height(80.dp)
                    .shadow(4.dp, RoundedCornerShape(24.dp)),
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(containerColor = ColorGallery)
            ) {
                 Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.PhotoLibrary, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("사진첩", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }
}
