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
fun ClockWidget(
    weatherInfo: Pair<String, String>?,
    weatherIcon: String
) {
    val context = LocalContext.current
    var currentTime by remember { mutableStateOf(System.currentTimeMillis()) }
    
    // Time Update
    LaunchedEffect(Unit) {
        while(true) {
            currentTime = System.currentTimeMillis()
            delay(1000)
        }
    }
    
    val timeFormat = SimpleDateFormat("a h:mm", Locale.KOREAN)
    val dateFormat = SimpleDateFormat("yyyy년 M월 d일 EEEE", Locale.KOREAN) // Added Year
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight() // Allow height to expand
            .shadow(8.dp, RoundedCornerShape(32.dp))
            .clip(RoundedCornerShape(32.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(Color(0xFF6C5CE7), Color(0xFFA29BFE))
                )
            )
            .padding(vertical = 24.dp, horizontal = 20.dp), // Increased vertical padding
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = timeFormat.format(Date(currentTime)),
                fontSize = 42.sp, // Reduced font size (48 -> 42)
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = dateFormat.format(Date(currentTime)),
                fontSize = 14.sp, // Reduced font size (16 -> 14)
                color = Color.White.copy(alpha = 0.9f)
            )
            Spacer(modifier = Modifier.height(16.dp)) // Increased spacer for separation
            
            // Weather Info
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = weatherIcon, fontSize = 24.sp)
                Spacer(modifier = Modifier.width(8.dp))
                if (weatherInfo != null) {
                    Text(
                        text = "${weatherInfo.first} ${weatherInfo.second}",
                        fontSize = 16.sp, // 18 -> 16
                        color = Color.White.copy(alpha = 0.95f),
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                } else {
                    Text(
                        text = "날씨 불러오는 중...", // More descriptive
                        fontSize = 14.sp,
                        color = Color.White.copy(alpha = 0.8f)
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
    
    // Photo Permission State
    val photoPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
           // Permission granted, user can try clicking again
           android.widget.Toast.makeText(context, "권한이 허용되었습니다. 다시 버튼을 눌러주세요.", android.widget.Toast.LENGTH_SHORT).show()
        } else {
           android.widget.Toast.makeText(context, "사진을 보내려면 권한이 필요합니다.", android.widget.Toast.LENGTH_SHORT).show()
        }
    }
    
    // Contacts State
    var favoriteContacts by remember { mutableStateOf<List<ContactInfo>>(emptyList()) }
    
    val lifecycleOwner = androidx.compose.ui.platform.LocalLifecycleOwner.current

    // Weather State (Unified)
    var weatherInfo by remember { mutableStateOf<Pair<String, String>?>(null) }
    var weatherIcon by remember { mutableStateOf("❓") }
    var weatherGreeting by remember { mutableStateOf("즐거운 하루 보내세요! 😊") }
    
    // Fetch Weather Logic
    val fetchWeather = remember {
        {
            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                LocationServices.getFusedLocationProviderClient(context).lastLocation.addOnSuccessListener { location ->
                    if (location != null) {
                        kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch {
                            try {
                                val weather = WeatherRepository.getCurrentWeather(location.latitude, location.longitude)
                                withContext(Dispatchers.Main) {
                                    if (weather != null) {
                                        val (desc, icon) = WeatherRepository.getWeatherInfo(weather.weatherCode)
                                        weatherIcon = icon
                                        weatherInfo = desc to "${weather.temperature}°C"
                                        
                                        // Update Greeting based on weather
                                        weatherGreeting = when {
                                            weather.weatherCode == 0 -> "맑은 날씨입니다. 산책 어떠세요? ☀️"
                                            weather.weatherCode in 1..3 -> "구름이 조금 있네요. 편안한 하루 보내세요! ⛅"
                                            weather.weatherCode in 51..67 || weather.weatherCode in 80..82 -> "비 소식이 있어요. 우산 챙기세요! ☔"
                                            weather.weatherCode in 71..77 || weather.weatherCode in 85..86 -> "눈이 오네요. 미끄럼 조심하세요! ❄️"
                                            weather.temperature < 0 -> "날씨가 많이 춥습니다. 따뜻하게 입으세요! 🧣"
                                            weather.temperature > 30 -> "무더운 날씨입니다. 물 자주 드세요! 💧"
                                            else -> "오늘도 기분 좋은 하루 되세요! 🍀"
                                        }
                                    }
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    }
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        try {
            favoriteContacts = repository.getFavoriteContacts()
        } catch (e: Exception) {
            // Permission might not be granted yet
        }
        fetchWeather()
    }
    
    // Refresh contacts & Weather periodically or on resume logic conceptually
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                try {
                     favoriteContacts = repository.getFavoriteContacts()
                     fetchWeather() // Refresh Weather on Resume
                } catch(e: Exception) {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
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
            HeaderSection(onNavigateToSettings, weatherGreeting)
        }
        
        // 2. Search Section Removed
        
        // 3. Clock Widget
        item {
            ClockWidget(weatherInfo, weatherIcon)
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
            ActionButtonsSection(context, onNavigateToPairing, photoPermissionLauncher)
        }
        
        // 7. Extended App Grid (Bottom - User Added Apps)
        item {
            ExtendedAppsGrid(context)
        }
        
        // Bottom Action Section Removed (Moved to Middle)
    }
}

@Composable
fun HeaderSection(onSettingsClick: () -> Unit, weatherGreeting: String) {
    val context = LocalContext.current
    val lifecycleOwner = androidx.compose.ui.platform.LocalLifecycleOwner.current
    val prefs = remember { AppPreferences(context) }
    val showUserName = prefs.isUserNameVisible()
    val showUserPhoneNumber = prefs.isUserPhoneNumberVisible()
    val userName = prefs.getUserName()
    val userPhoneNumber = prefs.getUserPhoneNumber()

    // Default Launcher Check
    var isDefaultLauncher by remember { mutableStateOf(true) } // Assume true initially to avoid flicker
    
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                isDefaultLauncher = com.silverpixelism.hyotok.util.LauncherHelper.isDefaultLauncher(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            if (showUserName || showUserPhoneNumber) {
                if (showUserName && userName.isNotEmpty()) {
                    Text(
                        text = userName,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }
                if (showUserPhoneNumber && userPhoneNumber.isNotEmpty()) {
                    Text(
                        text = userPhoneNumber,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = ColorPhone, // Mint color for phone number
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            } else {
                 Text(
                    text = weatherGreeting,
                    fontSize = 20.sp, // Slightly larger for greeting
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    lineHeight = 28.sp
                 )
            }
        }
        IconButton(
            onClick = onSettingsClick,
            modifier = Modifier
                .size(48.dp)
        ) {
            Icon(
                Icons.Filled.Settings, 
                contentDescription = "설정", 
                tint = Color(0xFFBDC3C7), // 은색/회색 계열로 은은하게
                modifier = Modifier.size(32.dp)
            )
        }
    }

    // Default Launcher Banner
    if (!isDefaultLauncher) {
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = { com.silverpixelism.hyotok.util.LauncherHelper.openHomeSettings(context) },
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .shadow(4.dp, RoundedCornerShape(16.dp)),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFCC00)) // Warning Yellow
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Home, contentDescription = null, tint = Color.Black)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "기본 홈 화면으로 설정하기 (터치)",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
            }
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
fun ActionButtonsSection(
    context: android.content.Context, 
    onNavigateToPairing: () -> Unit,
    permissionLauncher: androidx.activity.result.ActivityResultLauncher<String>
) {
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
            // Send Photo Button
             Button(
                onClick = {
                    val permission = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                        android.Manifest.permission.READ_MEDIA_IMAGES
                    } else {
                        android.Manifest.permission.READ_EXTERNAL_STORAGE
                    }
                    
                    if (androidx.core.content.ContextCompat.checkSelfPermission(context, permission) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                        // Get Latest Photo
                        kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch {
                            val collection = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                                MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
                            } else {
                                MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                            }
                            
                            val projection = arrayOf(MediaStore.Images.Media._ID)
                            val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"
                            
                            context.contentResolver.query(
                                collection,
                                projection,
                                null,
                                null,
                                sortOrder
                            )?.use { cursor ->
                                if (cursor.moveToFirst()) {
                                    val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                                    val id = cursor.getLong(idColumn)
                                    val contentUri = android.content.ContentUris.withAppendedId(collection, id)
                                    
                                    val intent = Intent(Intent.ACTION_SEND).apply {
                                        type = "image/*"
                                        putExtra(Intent.EXTRA_STREAM, contentUri)
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    
                                    withContext(Dispatchers.Main) {
                                        context.startActivity(Intent.createChooser(intent, "가족에게 사진 보내기"))
                                    }
                                } else {
                                    withContext(Dispatchers.Main) {
                                        android.widget.Toast.makeText(context, "최근 사진을 찾을 수 없습니다.", android.widget.Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        }
                    } else {
                        permissionLauncher.launch(permission)
                    }
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
                    val intent = Intent(Intent.ACTION_VIEW).apply {
                        type = "image/*"
                    }
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
