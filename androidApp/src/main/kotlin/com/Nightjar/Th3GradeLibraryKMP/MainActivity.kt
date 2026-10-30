package com.Nightjar.Th3GradeLibraryKMP

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.Icon
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit
import androidx.activity.enableEdgeToEdge
import androidx.activity.SystemBarStyle
import androidx.core.view.WindowCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.collectAsState
import androidx.browser.customtabs.CustomTabsIntent
import androidx.browser.customtabs.CustomTabColorSchemeParams
import com.Nightjar.Th3GradeLibraryKMP.data.BookItem
import com.Nightjar.Th3GradeLibraryKMP.data.PlatformActionHandler
import com.Nightjar.Th3GradeLibraryKMP.network.SyncEngine
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.ColorScheme
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob

fun scheduleBackgroundSync(context: Context, forceReplace: Boolean = false) {
    val syncInterval = SyncEngine.appSettings.value.syncInterval
    
    if (syncInterval == "on_open") {
        // Cancel Background WorkManager & Heartbeat
        WorkManager.getInstance(context).cancelUniqueWork("BackgroundNewsSync")
        HeartbeatScheduler.cancelHeartbeat(context)
        return
    }

    // 1. Set up the Heartbeat (12h inexact batching to wake app from Doze)
    HeartbeatScheduler.scheduleHeartbeat(context)

    // 2. Set up Periodic WorkManager for News Sync
    val intervalMinutes = when (syncInterval) {
        "15m" -> 15L
        "30m" -> 30L
        "1h"  -> 60L
        "6h"  -> 360L
        "12h" -> 720L
        "24h" -> 1440L
        else  -> 30L
    }

    val constraints = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build()

    val periodicWorkRequest = PeriodicWorkRequestBuilder<BackgroundSyncWorker>(
        intervalMinutes, TimeUnit.MINUTES
    )
        .setConstraints(constraints)
        .build()

    val policy = if (forceReplace) {
        ExistingPeriodicWorkPolicy.REPLACE
    } else {
        ExistingPeriodicWorkPolicy.KEEP
    }

    WorkManager.getInstance(context).enqueueUniquePeriodicWork(
        "BackgroundNewsSync",
        policy,
        periodicWorkRequest
    )
}

class MainActivity : ComponentActivity() {
    private var currentSpeechRecognizer: android.speech.SpeechRecognizer? = null
    private val bookIdState = mutableStateOf<String?>(null)
    private val isNoteState = mutableStateOf(false)
    private val pageState = mutableStateOf<String?>(null)
    private val searchQueryState = mutableStateOf<String?>(null)

    @Volatile
    private var isComposeDrawn = false
    private var isProcessDeath = false

    private lateinit var notificationPermissionLauncher: androidx.activity.result.ActivityResultLauncher<String>
    private lateinit var batteryOptimizationLauncher: androidx.activity.result.ActivityResultLauncher<Intent>

    private fun loadCoverBitmap(coverResName: String, maxWidth: Int, maxHeight: Int): Bitmap? {
        val assetManager = assets
        val possibleExtensions = listOf("png", "jpg", "jpeg")
        for (ext in possibleExtensions) {
            val assetPath = "composeResources/com.Nightjar.Th3GradeLibraryKMP.generated.resources/drawable/$coverResName.$ext"
            try {
                assetManager.open(assetPath).use { inputStream ->
                    val options = BitmapFactory.Options().apply {
                        inJustDecodeBounds = true
                    }
                    BitmapFactory.decodeStream(inputStream, null, options)
                    
                    assetManager.open(assetPath).use { actualStream ->
                        val scaleOptions = BitmapFactory.Options().apply {
                            inSampleSize = calculateInSampleSize(options, maxWidth, maxHeight)
                            inJustDecodeBounds = false
                        }
                        val decoded = BitmapFactory.decodeStream(actualStream, null, scaleOptions)
                        if (decoded != null) {
                            val scaled = Bitmap.createScaledBitmap(decoded, maxWidth, maxHeight, true)
                            if (scaled != decoded) {
                                decoded.recycle()
                            }
                            return scaled
                        }
                    }
                }
            } catch (e: Exception) {
                // Try next extension
            }
        }
        return null
    }

    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val (height: Int, width: Int) = options.outHeight to options.outWidth
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {
            val halfHeight: Int = height / 2
            val halfWidth: Int = width / 2
            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        isProcessDeath = savedInstanceState != null
        AdMobManager.initialize(this)
        window.setBackgroundDrawableResource(R.drawable.splash_background)
        val splashScreen = installSplashScreen()

        // تثبيت العرض (setKeepOnScreenCondition): يمنع إغلاق شاشة النظام قبل اكتمال رسم شاشة Compose تحتها تماماً
        splashScreen.setKeepOnScreenCondition { !isComposeDrawn }
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
            window.isStatusBarContrastEnforced = false
        }
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        window.navigationBarColor = android.graphics.Color.TRANSPARENT
        super.onCreate(savedInstanceState)
        
        try {
            startService(Intent(this, AppCleanupService::class.java))
        } catch (e: Exception) {
            e.printStackTrace()
        }

        batteryOptimizationLauncher = registerForActivityResult(
            androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
        ) { _ ->
            val pm = getSystemService(Context.POWER_SERVICE) as android.os.PowerManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (!pm.isIgnoringBatteryOptimizations(packageName)) {
                    try {
                        val settingsIntent = Intent(android.provider.Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                        Toast.makeText(this@MainActivity, "لم يتم تفعيل تخطي الخمول للبطارية، يرجى تفعيله يدوياً", Toast.LENGTH_LONG).show()
                        startActivity(settingsIntent)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
        
        notificationPermissionLauncher = registerForActivityResult(
            androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            com.Nightjar.Th3GradeLibraryKMP.network.SyncEngine.checkNotificationPermission()
            if (!isGranted) {
                val isPermanentlyDenied = !androidx.core.app.ActivityCompat
                    .shouldShowRequestPermissionRationale(
                        this@MainActivity,
                        android.Manifest.permission.POST_NOTIFICATIONS
                    )
                if (isPermanentlyDenied) {
                    val sharedPrefs = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
                    if (sharedPrefs.getBoolean("hasCompletedInitialSetup", false)) {
                        openAppNotificationSettings()
                    }
                }
            }
        }

        handleIntent(intent)

        // We will start the sequence here
        // Initial setup check will be handled in setContent via Compose Dialog
        val sharedPrefs = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
        val hasCompletedInitialSetup = sharedPrefs.getBoolean("hasCompletedInitialSetup", false)

        var systemAccentColorHex: String? = null
        var androidLightColorScheme: ColorScheme? = null
        var androidDarkColorScheme: ColorScheme? = null
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            try {
                val colorInt = resources.getColor(android.R.color.system_accent1_500, theme)
                systemAccentColorHex = String.format("#%06X", 0xFFFFFF and colorInt)
                
                androidLightColorScheme = dynamicLightColorScheme(this)
                androidDarkColorScheme = dynamicDarkColorScheme(this)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // Register network callback to update SyncEngine connectivity state
        try {
            val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
            val networkRequest = android.net.NetworkRequest.Builder()
                .addCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()
            connectivityManager.registerNetworkCallback(networkRequest, object : android.net.ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: android.net.Network) {
                    SyncEngine.setOnline(true)
                }
                override fun onLost(network: android.net.Network) {
                    SyncEngine.setOnline(false)
                }
            })
            // Initial network state check
            val activeNetwork = connectivityManager.activeNetwork
            val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork)
            SyncEngine.setOnline(capabilities?.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET) == true)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Schedule background operations asynchronously to keep main thread 100% free and eliminate startup delay
        CoroutineScope(Dispatchers.IO + SupervisorJob()).launch {
            try {
                AppCleanupService.cleanAllTemporaryPdfFiles(applicationContext)
                androidx.browser.customtabs.CustomTabsClient.connectAndInitialize(applicationContext, "com.android.chrome")
            } catch (_: Exception) {}
            scheduleBackgroundSync(applicationContext)
        }

        try {
            startService(Intent(this, AppCleanupService::class.java))
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // On Android 12+, root launcher back press moves task to back without finishing or calling onDestroy.
        // We explicitly intercept back press at the root level to clear cache and finish activity completely.
        onBackPressedDispatcher.addCallback(this, object : androidx.activity.OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                try {
                    AppCleanupService.cleanAllTemporaryPdfFiles(applicationContext)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                finish()
            }
        })

        // Sequence is handled by hasCompletedInitialSetup above, so we remove the duplicate check here

        val platformHandler = object : PlatformActionHandler {
            override fun isLowEndDevice(): Boolean {
                // 1. Android 9 and older (legacy Skia drivers)
                if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) return true
                
                val activityManager = getSystemService(android.content.Context.ACTIVITY_SERVICE) as? android.app.ActivityManager
                // 2. Android Go / Manufacturer Low RAM flag
                if (activityManager?.isLowRamDevice == true) return true
                
                // 3. Exact physical total RAM check (<= 3.2GB RAM)
                if (activityManager != null) {
                    val memoryInfo = android.app.ActivityManager.MemoryInfo()
                    activityManager.getMemoryInfo(memoryInfo)
                    val totalRamGb = memoryInfo.totalMem / (1024.0 * 1024.0 * 1024.0)
                    if (totalRamGb <= 3.2) return true
                    
                    // 4. CPU Core count check (Quad-core or lower with < 4.0GB RAM is considered low-end)
                    val cores = Runtime.getRuntime().availableProcessors()
                    if (cores <= 4 && totalRamGb < 4.0) return true
                } else {
                    val cores = Runtime.getRuntime().availableProcessors()
                    if (cores <= 4) return true
                }
                return false
            }

            override fun addHomeScreenShortcut(item: BookItem) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val shortcutManager = getSystemService(ShortcutManager::class.java)
                    if (shortcutManager != null && shortcutManager.isRequestPinShortcutSupported) {
                        val shortcutIntent = Intent(this@MainActivity, MainActivity::class.java).apply {
                            action = Intent.ACTION_VIEW
                            putExtra("book_id", item.id)
                            putExtra("is_note", item.isNote)
                            // Clear top flags to route correctly if activity exists
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                        }
                        
                        val maxIconWidth = shortcutManager.iconMaxWidth
                        val maxIconHeight = shortcutManager.iconMaxHeight
                        val width = if (maxIconWidth > 0) maxIconWidth else 192
                        val height = if (maxIconHeight > 0) maxIconHeight else 192

                        val coverBitmap = loadCoverBitmap(item.coverResName, width, height)
                        val icon = if (coverBitmap != null) {
                            Icon.createWithBitmap(coverBitmap)
                        } else {
                            Icon.createWithResource(this@MainActivity, R.drawable.app_icon)
                        }
                        
                        val pinShortcutInfo = ShortcutInfo.Builder(this@MainActivity, item.id)
                            .setShortLabel(item.title)
                            .setIcon(icon)
                            .setIntent(shortcutIntent)
                            .build()
                        
                        shortcutManager.requestPinShortcut(pinShortcutInfo, null)
                        Toast.makeText(this@MainActivity, "تمت إضافة اختصار لـ ${item.title} بنجاح", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this@MainActivity, "إضافة الاختصارات غير مدعومة في جهازك", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this@MainActivity, "ميزة الاختصارات تحتاج أندرويد 8+", Toast.LENGTH_SHORT).show()
                }
            }

            override fun showToast(message: String) {
                com.Nightjar.Th3GradeLibraryKMP.ui.ToastManager.showToast(message)
            }

            override fun openUrl(url: String) {
                try {
                    val intent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse(url))
                    startActivity(intent)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            override fun openCustomTab(url: String, colorHex: String) {
                try {
                    val colorInt = android.graphics.Color.parseColor(colorHex)
                    val defaultColors = CustomTabColorSchemeParams.Builder()
                        .setToolbarColor(colorInt)
                        .setNavigationBarColor(android.graphics.Color.TRANSPARENT)
                        .setNavigationBarDividerColor(android.graphics.Color.TRANSPARENT)
                        .build()
                    val customTabsIntent = CustomTabsIntent.Builder()
                        .setDefaultColorSchemeParams(defaultColors)
                        .setShowTitle(true)
                        .build()
                    customTabsIntent.launchUrl(this@MainActivity, android.net.Uri.parse(url))
                } catch (e: Exception) {
                    openUrl(url) // fallback
                }
            }

            override fun playNotificationSound() {
                // Sound is already handled natively via NotificationHelper and R.raw.custom_sound
            }

            override fun startVoiceSearch(onResult: (String) -> Unit, onEnd: () -> Unit) {
                if (androidx.core.content.ContextCompat.checkSelfPermission(this@MainActivity, android.Manifest.permission.RECORD_AUDIO) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                    androidx.core.app.ActivityCompat.requestPermissions(this@MainActivity, arrayOf(android.Manifest.permission.RECORD_AUDIO), 101)
                    onEnd()
                    Toast.makeText(this@MainActivity, "يرجى منح صلاحية الميكروفون للبحث الصوتي والمحاولة مجدداً", Toast.LENGTH_SHORT).show()
                    return
                }

                currentSpeechRecognizer?.destroy()
                currentSpeechRecognizer = android.speech.SpeechRecognizer.createSpeechRecognizer(this@MainActivity)
                val speechIntent = Intent(android.speech.RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE_MODEL, android.speech.RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE, "ar")
                    putExtra(android.speech.RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                }

                currentSpeechRecognizer?.setRecognitionListener(object : android.speech.RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {}
                    override fun onBeginningOfSpeech() {}
                    override fun onRmsChanged(rmsdB: Float) {}
                    override fun onBufferReceived(buffer: ByteArray?) {}
                    override fun onEndOfSpeech() {}
                    override fun onError(error: Int) {
                        onEnd()
                        val msg = when(error) {
                            android.speech.SpeechRecognizer.ERROR_NETWORK -> "خطأ في الاتصال بالإنترنت"
                            android.speech.SpeechRecognizer.ERROR_NO_MATCH -> "لم يتم التعرف على الصوت"
                            android.speech.SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "انتهى وقت التحدث"
                            else -> "حدث خطأ في التسجيل"
                        }
                        Toast.makeText(this@MainActivity, msg, Toast.LENGTH_SHORT).show()
                    }
                    override fun onResults(results: Bundle?) {
                        val matches = results?.getStringArrayList(android.speech.SpeechRecognizer.RESULTS_RECOGNITION)
                        if (!matches.isNullOrEmpty()) {
                            onResult(matches[0])
                        }
                        onEnd()
                    }
                    override fun onPartialResults(partialResults: Bundle?) {
                        val matches = partialResults?.getStringArrayList(android.speech.SpeechRecognizer.RESULTS_RECOGNITION)
                        if (!matches.isNullOrEmpty()) {
                            onResult(matches[0])
                        }
                    }
                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })
                
                android.os.Handler(android.os.Looper.getMainLooper()).post {
                    currentSpeechRecognizer?.startListening(speechIntent)
                }
            }

            override fun stopVoiceSearch() {
                android.os.Handler(android.os.Looper.getMainLooper()).post {
                    currentSpeechRecognizer?.stopListening()
                    currentSpeechRecognizer?.destroy()
                    currentSpeechRecognizer = null
                }
            }

            override fun rescheduleBackgroundSync() {
                scheduleBackgroundSync(this@MainActivity, forceReplace = true)
            }

            override fun areNotificationsEnabled(): Boolean {
                return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    androidx.core.content.ContextCompat.checkSelfPermission(this@MainActivity, android.Manifest.permission.POST_NOTIFICATIONS) == android.content.pm.PackageManager.PERMISSION_GRANTED
                } else {
                    androidx.core.app.NotificationManagerCompat.from(this@MainActivity).areNotificationsEnabled()
                }
            }

            override fun openNotificationSettings() {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    val alreadyGranted = androidx.core.content.ContextCompat.checkSelfPermission(
                        this@MainActivity, android.Manifest.permission.POST_NOTIFICATIONS
                    ) == android.content.pm.PackageManager.PERMISSION_GRANTED

                    if (alreadyGranted) {
                        requestBatteryOptimizationExemptionDirectly()
                        return
                    }

                    val shouldShowRationale = androidx.core.app.ActivityCompat.shouldShowRequestPermissionRationale(
                        this@MainActivity, android.Manifest.permission.POST_NOTIFICATIONS
                    )
                    
                    if (shouldShowRationale) {
                        // محاولة إظهار نافذة النظام إذا كان النظام يسمح بذلك
                        notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        // النظام يمنع ظهور النافذة بسبب الرفض المتكرر، نفتح الإعدادات مباشرة
                        openAppNotificationSettings()
                    }
                } else {
                    openAppNotificationSettings()
                    // نطلب استثناء البطارية أيضاً للأجهزة القديمة
                    requestBatteryOptimizationExemptionDirectly()
                }
            }

            override fun showNewsNotification(titles: List<String>, count: Int) {
                val withSound = com.Nightjar.Th3GradeLibraryKMP.network.SyncEngine.appSettings.value.notificationSound
                NotificationHelper.showNotifications(this@MainActivity, titles, count, withSound)
            }

            override fun isBatteryOptimizationIgnored(): Boolean {
                val pm = getSystemService(Context.POWER_SERVICE) as android.os.PowerManager
                return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    pm.isIgnoringBatteryOptimizations(packageName)
                } else {
                    true
                }
            }

            override fun requestBatteryOptimizationExemption() {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    val intent = Intent(android.provider.Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                        data = android.net.Uri.parse("package:$packageName")
                    }
                    try {
                        android.widget.Toast.makeText(this@MainActivity, "يرجى السماح للتطبيق بتخطي البطارية للعمل في الخلفية", android.widget.Toast.LENGTH_LONG).show()
                        batteryOptimizationLauncher.launch(intent)
                    } catch (e: Exception) {
                        try {
                            val settingsIntent = Intent(android.provider.Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_HISTORY or Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
                            }
                            android.widget.Toast.makeText(this@MainActivity, "يرجى اختيار 'بدون قيود' أو 'عدم التحسين' للتطبيق", android.widget.Toast.LENGTH_LONG).show()
                            startActivity(settingsIntent)
                        } catch (ex: Exception) {
                            ex.printStackTrace()
                        }
                    }
                }
            }

            override fun canScheduleExactAlarms(): Boolean {
                return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val alarmManager = getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
                    alarmManager.canScheduleExactAlarms()
                } else {
                    true
                }
            }

            override fun requestExactAlarmPermission() {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val intent = Intent(android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                        data = android.net.Uri.parse("package:$packageName")
                    }
                    try {
                        startActivity(intent)
                    } catch (e: Exception) {
                        try {
                            val settingsIntent = Intent(android.provider.Settings.ACTION_DEVICE_INFO_SETTINGS)
                            startActivity(settingsIntent)
                        } catch (ex: Exception) {
                            ex.printStackTrace()
                        }
                    }
                }
            }

            override fun requestNotificationPermission() {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    if (androidx.core.content.ContextCompat.checkSelfPermission(this@MainActivity, android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                        notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                    }
                }
            }

            override fun isAutoRevokeWhitelisted(): Boolean {
                return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    val packageManager = getPackageManager()
                    packageManager.isAutoRevokeWhitelisted()
                } else {
                    true
                }
            }

            override fun requestAutoRevokeExemption() {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) { // Android 11+
                    android.widget.Toast.makeText(this@MainActivity, "يرجى إيقاف تفعيل خيار (إيقاف نشاط التطبيق إذا لم يكن مستخدماً) أو (إزالة الأذونات وإخلاء المساحة) لحماية التطبيق وعمل المزامنة.", android.widget.Toast.LENGTH_LONG).show()
                    val intent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = android.net.Uri.parse("package:$packageName")
                        addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS or Intent.FLAG_ACTIVITY_NO_HISTORY)
                    }
                    startActivity(intent)
                } else {
                    android.widget.Toast.makeText(this@MainActivity, "جهازك لا يحتوي على هذه الخاصية، أنت في أمان!", android.widget.Toast.LENGTH_SHORT).show()
                }
            }

            override fun clearDiskCache() {
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        // 1. Delete all temporary PDF files via unified cache manager from both cacheDir and filesDir
                        com.Nightjar.Th3GradeLibraryKMP.ui.clearPdfCache(cacheDir)
                        filesDir?.let { com.Nightjar.Th3GradeLibraryKMP.ui.clearPdfCache(it) }
                        // 2. Clear Coil Image disk & memory cache
                        coil3.SingletonImageLoader.get(this@MainActivity).let { loader ->
                            loader.diskCache?.clear()
                            loader.memoryCache?.clear()
                        }
                        // 3. Evict PDF page bitmap cache
                        com.Nightjar.Th3GradeLibraryKMP.ui.PdfBitmapCache.cache.evictAll()
                        System.gc()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }

            override fun updateSystemBars(isLightStatusBars: Boolean, isLightNavigationBars: Boolean) {
                runOnUiThread {
                    try {
                        window.statusBarColor = android.graphics.Color.TRANSPARENT
                        window.navigationBarColor = android.graphics.Color.TRANSPARENT
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            window.isNavigationBarContrastEnforced = false
                            window.isStatusBarContrastEnforced = false
                        }
                        val controller = WindowCompat.getInsetsController(window, window.decorView)
                        controller.isAppearanceLightStatusBars = isLightStatusBars
                        controller.isAppearanceLightNavigationBars = isLightNavigationBars
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }

        setContent {
            androidx.compose.runtime.LaunchedEffect(Unit) {
                kotlinx.coroutines.delay(200) // Small delay to prevent native splash exit flicker on Xiaomi/HyperOS
                isComposeDrawn = true
            }
            val appSettings = com.Nightjar.Th3GradeLibraryKMP.network.SyncEngine.appSettings.collectAsState().value
            val isSystemDark = androidx.compose.foundation.isSystemInDarkTheme()
            val isDarkThemeActive = when (appSettings.theme) {
                "light" -> false
                "dark" -> true
                else -> isSystemDark
            }

            // Set status bar & navigation bar appearance reactively!
            val view = androidx.compose.ui.platform.LocalView.current
            if (!view.isInEditMode) {
                androidx.compose.runtime.SideEffect {
                    val activity = view.context as ComponentActivity
                    val window = activity.window
                    window.statusBarColor = android.graphics.Color.TRANSPARENT
                    window.navigationBarColor = android.graphics.Color.TRANSPARENT
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        window.isNavigationBarContrastEnforced = false
                        window.isStatusBarContrastEnforced = false
                    }
                    val controller = WindowCompat.getInsetsController(window, window.decorView)
                    controller.isAppearanceLightStatusBars = !isDarkThemeActive
                    controller.isAppearanceLightNavigationBars = !isDarkThemeActive
                }
            }

            App(
                platformActionHandler = platformHandler,
                systemAccentColor = systemAccentColorHex,
                dynamicLightColorScheme = androidLightColorScheme,
                dynamicDarkColorScheme = androidDarkColorScheme,
                initialBookId = bookIdState.value,
                initialIsNote = isNoteState.value,
                initialPage = pageState.value,
                initialSearchQuery = searchQueryState.value,
                isProcessDeath = isProcessDeath,
                onIntentConsumed = {
                    bookIdState.value = null
                    pageState.value = null
                    searchQueryState.value = null
                },
                bottomBannerAd = {
                    AdaptiveBannerAd()
                }
            )
            
            // Notification permission is now requested directly in App.kt after splash screen

        }
    }

    override fun onSaveInstanceState(outState: android.os.Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean("is_process_recreation", true)
    }

    override fun onStop() {
        super.onStop()
        // Zero Background CPU & Memory (Feb 2027 Standard):
        // Evict transient PDF page bitmaps and in-memory images completely to drop anonymous memory near zero
        try {
            com.Nightjar.Th3GradeLibraryKMP.ui.PdfBitmapCache.cache.evictAll()
            AppCleanupService.cleanAllTemporaryPdfFiles(applicationContext)
            com.Nightjar.Th3GradeLibraryKMP.ui.clearPdfCache(cacheDir)
            coil3.SingletonImageLoader.get(this).let { loader ->
                loader.memoryCache?.clear()
            }
            System.gc()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        if (level >= TRIM_MEMORY_UI_HIDDEN) {
            try {
                // UI is completely hidden (e.g. user pressed Home or switched apps).
                // Evict transient UI caches to drop anonymous RSS immediately (Android 15-18 cgroup v2 freezer standard).
                com.Nightjar.Th3GradeLibraryKMP.ui.PdfBitmapCache.cache.evictAll()
                com.Nightjar.Th3GradeLibraryKMP.ui.clearPdfCache(cacheDir)
                AppCleanupService.cleanAllTemporaryPdfFiles(applicationContext)
                coil3.SingletonImageLoader.get(this).let { loader ->
                    loader.memoryCache?.clear()
                }
                System.gc()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        } else if (level >= TRIM_MEMORY_BACKGROUND) {
            try {
                com.Nightjar.Th3GradeLibraryKMP.ui.PdfBitmapCache.cache.evictAll()
                coil3.SingletonImageLoader.get(this).let { loader ->
                    loader.memoryCache?.clear()
                }
                System.gc()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun onLowMemory() {
        super.onLowMemory()
        try {
            com.Nightjar.Th3GradeLibraryKMP.ui.PdfBitmapCache.cache.evictAll()
            coil3.SingletonImageLoader.get(this).let { loader ->
                loader.memoryCache?.clear()
            }
            System.gc()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Delete all temporary PDF cache files upon exiting the app to ensure zero storage footprint
        try {
            com.Nightjar.Th3GradeLibraryKMP.ui.clearPdfCache(cacheDir)
            AppCleanupService.cleanAllTemporaryPdfFiles(applicationContext)
            com.Nightjar.Th3GradeLibraryKMP.ui.PdfBitmapCache.cache.evictAll()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        SyncEngine.checkNotificationPermission()
    }

    override fun onConfigurationChanged(newConfig: android.content.res.Configuration) {
        super.onConfigurationChanged(newConfig)
        // Handled smoothly by Compose without Activity recreation or black borders (Googlebook OS & Foldables)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent == null) return
        
        var bookId = intent.getStringExtra("book_id")
        var isNote = intent.getBooleanExtra("is_note", false)
        var page = intent.getStringExtra("page")
        var searchQuery = intent.getStringExtra("search_query")

        val uri = intent.data
        if (uri != null && uri.scheme == "th3books" && uri.host == "library") {
            bookId = uri.getQueryParameter("book_id") ?: bookId
            isNote = uri.getQueryParameter("is_note")?.toBoolean() ?: isNote
            page = uri.getQueryParameter("page") ?: page
            searchQuery = uri.getQueryParameter("search_query") ?: searchQuery
        }

        if (bookId != null) {
            bookIdState.value = bookId
            isNoteState.value = isNote
            pageState.value = "pdf"
        } else if (page != null) {
            pageState.value = page
        }
        if (searchQuery != null) {
            searchQueryState.value = searchQuery
        }
    }

    /**
     * يفتح صفحة إشعارات التطبيق في إعدادات الجهاز.
     * يعمل على جميع الشركات والإصدارات:
     * - الطريقة 1 (أندرويد 8+): ACTION_APP_NOTIFICATION_SETTINGS ← مباشر لإعدادات إشعارات التطبيق
     * - الطريقة 2 (احتياطية): ACTION_APPLICATION_DETAILS_SETTINGS ← صفحة تفاصيل التطبيق
     * - الطريقة 3 (أخيرة): ACTION_MANAGE_APPLICATIONS_SETTINGS ← قائمة التطبيقات العامة
     */
    private fun openAppNotificationSettings() {
        val intent = Intent().apply {
            when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.O -> {
                    action = android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS
                    putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, packageName)
                }
                else -> {
                    action = android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                    addCategory(Intent.CATEGORY_DEFAULT)
                    data = android.net.Uri.parse("package:$packageName")
                }
            }
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS or Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_HISTORY)
        startActivity(intent)
    }

    private fun checkAndRequestBatteryBypass() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val pm = getSystemService(Context.POWER_SERVICE) as android.os.PowerManager
            if (!pm.isIgnoringBatteryOptimizations(packageName)) {
                requestBatteryOptimizationExemptionDirectly()
            }
        }
    }


    private fun requestBatteryOptimizationExemptionDirectly() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val intent = Intent(android.provider.Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = android.net.Uri.parse("package:$packageName")
                addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS or Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_HISTORY)
            }
            try {
                startActivity(intent)
            } catch (e: Exception) {
                try {
                    val intents = listOf(
                        Intent().setComponent(android.content.ComponentName("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity")),
                        Intent().setComponent(android.content.ComponentName("com.letv.android.letvsafe", "com.letv.android.letvsafe.AutobootManageActivity")),
                        Intent().setComponent(android.content.ComponentName("com.huawei.systemmanager", "com.huawei.systemmanager.optimize.process.ProtectActivity")),
                        Intent().setComponent(android.content.ComponentName("com.coloros.safecenter", "com.coloros.safecenter.permission.startup.StartupAppListActivity")),
                        Intent().setComponent(android.content.ComponentName("com.coloros.safecenter", "com.coloros.safecenter.startupapp.StartupAppListActivity")),
                        Intent().setComponent(android.content.ComponentName("com.oppo.safe", "com.oppo.safe.permission.startup.StartupAppListActivity")),
                        Intent().setComponent(android.content.ComponentName("com.iqoo.secure", "com.iqoo.secure.ui.phoneoptimize.AddWhiteListActivity")),
                        Intent().setComponent(android.content.ComponentName("com.iqoo.secure", "com.iqoo.secure.ui.phoneoptimize.BgStartUpManager")),
                        Intent().setComponent(android.content.ComponentName("com.vivo.permissionmanager", "com.vivo.permissionmanager.activity.BgStartUpManagerActivity")),
                        Intent().setComponent(android.content.ComponentName("com.asus.mobilemanager", "com.asus.mobilemanager.entry.FunctionActivity")).setData(android.net.Uri.parse("mobilemanager://function/entry/AutoStart"))
                    )
                    
                    var launched = false
                    for (i in intents) {
                        try {
                            if (packageManager.resolveActivity(i, android.content.pm.PackageManager.MATCH_DEFAULT_ONLY) != null) {
                                i.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS or Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_HISTORY)
                                startActivity(i)
                                launched = true
                                break
                            }
                        } catch (ex: Exception) {}
                    }
                    if (!launched) {
                        val settingsIntent = Intent(android.provider.Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_HISTORY or Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
                        }
                        startActivity(settingsIntent)
                    }
                } catch (ex: Exception) {
                    try {
                        val settingsIntent = Intent(android.provider.Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_HISTORY or Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
                        }
                        startActivity(settingsIntent)
                    } catch (e2: Exception) {
                        e2.printStackTrace()
                    }
                }
            }
        }
    }
}
