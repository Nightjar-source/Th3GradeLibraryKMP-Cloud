@file:OptIn(org.jetbrains.compose.resources.ExperimentalResourceApi::class)
package com.Nightjar.Th3GradeLibraryKMP.ui

import com.Nightjar.Th3GradeLibraryKMP.theme.bounceClick
import com.Nightjar.Th3GradeLibraryKMP.theme.expressiveButtonMorph

import android.os.Build
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.zIndex
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.core.view.WindowCompat
import com.Nightjar.Th3GradeLibraryKMP.data.AllItems
import androidx.core.view.WindowInsetsCompat
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.Nightjar.Th3GradeLibraryKMP.data.BookItem
import com.Nightjar.Th3GradeLibraryKMP.generated.resources.Res
import org.jetbrains.compose.resources.painterResource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
import kotlinx.coroutines.isActive
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.currentCoroutineContext
import java.io.File
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.ui.input.key.*
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.foundation.focusable
import com.Nightjar.Th3GradeLibraryKMP.LocalSharedTransitionScope
import com.Nightjar.Th3GradeLibraryKMP.LocalAnimatedVisibilityScope
import androidx.compose.foundation.gestures.*
import androidx.compose.ui.input.pointer.positionChanged
import android.util.LruCache
import kotlin.math.abs
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import com.Nightjar.Th3GradeLibraryKMP.theme.contrastingTextColor

val pdfMutex = Mutex()
private val pendingCacheCleanups = java.util.concurrent.ConcurrentHashMap<String, kotlinx.coroutines.Job>()
private var activeViewingBookId: String? = null

fun cancelAllPendingCacheCleanups() {
    try {
        pendingCacheCleanups.values.forEach { it.cancel() }
        pendingCacheCleanups.clear()
    } catch (_: Exception) {}
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalSharedTransitionApi::class)
@Composable
actual fun PdfViewerScreen(
    item: BookItem,
    isSaved: (Int) -> Boolean,
    onToggleSave: (Int) -> Unit,
    onClose: () -> Unit,
    initialPage: Int?,
    isDark: Boolean,
    isFromBookmarks: Boolean,
    sourcePage: String
) {
    val context = LocalContext.current
    val appSettings by com.Nightjar.Th3GradeLibraryKMP.network.SyncEngine.appSettings.collectAsState()
    val isSystemDark = androidx.compose.foundation.isSystemInDarkTheme()
    val isAppDark = isDark || when (appSettings.theme) {
        "dark" -> true
        "light" -> false
        else -> isSystemDark
    }

    var tempFile by remember(item) { mutableStateOf<File?>(null) }
    var pdfRenderer by remember { mutableStateOf<PdfRenderer?>(null) }
    var loadingError by remember(item) { mutableStateOf<String?>(null) }
    var downloadProgress by remember(item) { mutableStateOf(0f) }
    var isDownloading by remember(item) { mutableStateOf(false) }
    var orientation by remember(appSettings.pdfScrollDirection) { mutableStateOf(appSettings.pdfScrollDirection) }
    var isZoomed by remember(item) { mutableStateOf(false) }

    // Find and cast Activity context to toggle system status bars
    val activity = remember(context) {
        var currentContext = context
        var act: android.app.Activity? = null
        while (currentContext is android.content.ContextWrapper) {
            if (currentContext is android.app.Activity) {
                act = currentContext
                break
            }
            currentContext = currentContext.baseContext
        }
        act
    }

    var isCurrentPageTopLight by remember { mutableStateOf(!isAppDark) }
    var isCurrentPageBottomLight by remember { mutableStateOf(!isAppDark) }
    val pageLuminanceMap = remember { mutableStateMapOf<Int, Pair<Boolean, Boolean>>() }

    // Guaranteed cleanup on exit: restore system bars to match active app theme contrast
    DisposableEffect(activity, isAppDark) {
        onDispose {
            activity?.let { act ->
                val window = act.window
                window.statusBarColor = android.graphics.Color.TRANSPARENT
                window.navigationBarColor = android.graphics.Color.TRANSPARENT
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    window.isNavigationBarContrastEnforced = false
                    window.isStatusBarContrastEnforced = false
                }
                val controller = WindowCompat.getInsetsController(window, window.decorView)
                controller.show(WindowInsetsCompat.Type.statusBars())
                controller.isAppearanceLightStatusBars = !isAppDark
                controller.isAppearanceLightNavigationBars = !isAppDark
            }
        }
    }

    LaunchedEffect(isZoomed, isCurrentPageTopLight, isCurrentPageBottomLight, activity) {
        activity?.let { act ->
            val window = act.window
            window.statusBarColor = android.graphics.Color.TRANSPARENT
            window.navigationBarColor = android.graphics.Color.TRANSPARENT
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                window.isNavigationBarContrastEnforced = false
                window.isStatusBarContrastEnforced = false
            }
            val controller = WindowCompat.getInsetsController(window, window.decorView)
            
            // Dynamic contrast: adapt status bar and 3-button navigation bar to real-time content
            controller.isAppearanceLightStatusBars = isCurrentPageTopLight
            controller.isAppearanceLightNavigationBars = isCurrentPageBottomLight

            if (isZoomed) {
                controller.systemBarsBehavior = androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                controller.hide(WindowInsetsCompat.Type.statusBars())
            } else {
                controller.show(WindowInsetsCompat.Type.statusBars())
            }
        }
    }

    androidx.activity.compose.BackHandler(enabled = true) {
        onClose()
    }

    val scope = rememberCoroutineScope()
    // Load last read page index directly from unified SyncEngine database
    val savedPage = remember(item) { com.Nightjar.Th3GradeLibraryKMP.network.SyncEngine.lastReadPages.value[item.id] ?: 0 }

    // Load file and instantiate PdfRenderer
    LaunchedEffect(item) {
        activeViewingBookId = item.id
        pendingCacheCleanups.remove("pdf_${item.id}.pdf")?.cancel()
        try {
            val file = File(context.cacheDir, "pdf_${item.id}.pdf")
            if (!file.exists() || file.length() == 0L) {
                withContext(Dispatchers.IO) {
                    val tempTarget = File(context.cacheDir, "pdf_${item.id}_${System.currentTimeMillis()}.tmp")
                    var streamSuccess = false

                    // Tier 1: Try bundled assets (for backward compatibility if asset exists)
                    val assetManager = context.assets
                    val possiblePaths = listOf(
                        "composeResources/com.Nightjar.Th3GradeLibraryKMP.generated.resources/${item.pdfPath}",
                        "composeResources/shared.generated.resources/${item.pdfPath}",
                        item.pdfPath
                    )
                    
                    for (path in possiblePaths) {
                        try {
                            assetManager.open(path).use { input ->
                                java.io.FileOutputStream(tempTarget).use { output ->
                                    input.copyTo(output, 32 * 1024)
                                }
                            }
                            if (tempTarget.exists() && tempTarget.length() > 0L) {
                                streamSuccess = true
                                break
                            }
                        } catch (_: Exception) {}
                    }

                    // Tier 2: Stream on-demand from Cloudflare Gateway / Telegram CDN
                    if (!streamSuccess) {
                        try {
                            val streamUrl = com.Nightjar.Th3GradeLibraryKMP.network.CloudCatalogManager.getStreamUrl(item)
                            if (streamUrl.isNotBlank() && streamUrl.startsWith("http")) {
                                isDownloading = true
                                val connection = java.net.URL(streamUrl).openConnection() as java.net.HttpURLConnection
                                connection.connectTimeout = 15000
                                connection.readTimeout = 30000
                                connection.setRequestProperty("User-Agent", "Mozilla/5.0")
                                connection.connect()

                                if (connection.responseCode in 200..299) {
                                    val totalBytes = connection.contentLengthLong.takeIf { it > 0 } ?: (item.id.let { 
                                        com.Nightjar.Th3GradeLibraryKMP.network.CloudCatalogManager.getMeta(item)?.file_size 
                                    } ?: -1L)

                                    connection.inputStream.use { input ->
                                        java.io.FileOutputStream(tempTarget).use { output ->
                                            val buffer = ByteArray(64 * 1024)
                                            var bytesCopied = 0L
                                            var bytesRead: Int
                                            while (input.read(buffer).also { bytesRead = it } != -1) {
                                                ensureActive()
                                                output.write(buffer, 0, bytesRead)
                                                bytesCopied += bytesRead
                                                if (totalBytes > 0) {
                                                    downloadProgress = (bytesCopied.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f)
                                                }
                                            }
                                        }
                                    }
                                    if (tempTarget.exists() && tempTarget.length() > 0L) {
                                        streamSuccess = true
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            if (e is kotlinx.coroutines.CancellationException) throw e
                            e.printStackTrace()
                        } finally {
                            isDownloading = false
                        }
                    }

                    // Tier 3: Res.readBytes fallback (if packed)
                    if (!streamSuccess) {
                        try {
                            val bytes = Res.readBytes(item.pdfPath)
                            tempTarget.writeBytes(bytes)
                            if (tempTarget.exists() && tempTarget.length() > 0L) {
                                streamSuccess = true
                            }
                        } catch (_: Exception) {}
                    }

                    if (streamSuccess && tempTarget.exists() && tempTarget.length() > 0L) {
                        tempTarget.renameTo(file)
                    } else {
                        throw Exception("تعذر تنزيل الملف، يرجى التأكد من اتصال الإنترنت.")
                    }
                }
            }
            tempFile = file

            var pfd: ParcelFileDescriptor? = null
            try {
                if (!isActive) return@LaunchedEffect
                pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
                if (!isActive) {
                    pfd?.close()
                    return@LaunchedEffect
                }
                pdfRenderer = PdfRenderer(pfd)
            } catch (initEx: Exception) {
                pfd?.close()
                throw initEx
            }
        } catch (e: Exception) {
            e.printStackTrace()
            loadingError = "فشل في تحميل الملف: ${e.message}"
        }
    }

    DisposableEffect(item, activity) {
        onDispose {
            if (activeViewingBookId == item.id) {
                activeViewingBookId = null
            }
            try {
                val rendererToClose = pdfRenderer
                pdfRenderer = null
                val fileToDelete = tempFile
                val bookId = item.id
                
                // 1. Safe renderer release: Close renderer after reverse sharedBounds shrink animation completes (600ms)
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        kotlinx.coroutines.delay(600)
                        pdfMutex.withLock {
                            try {
                                rendererToClose?.close()
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                // 2. Official 2-Minute Grace Timer (120 seconds):
                // Retains extracted PDF file and page cache in RAM for 2 minutes.
                // If user re-opens this book within 2 minutes, launch is instantaneous with zero disk re-copy!
                // If 2 minutes elapse without re-opening, cleanly deletes file and evicts cache.
                val cleanupJob = CoroutineScope(Dispatchers.IO).launch {
                    try {
                        kotlinx.coroutines.delay(120_000L) // 2-Minute official timer
                        if (activeViewingBookId != bookId) {
                            if (fileToDelete != null && fileToDelete.exists()) {
                                fileToDelete.delete()
                            }
                            PdfBitmapCache.evictBook(bookId)
                            pendingCacheCleanups.remove("pdf_$bookId.pdf")
                        }
                    } catch (_: kotlinx.coroutines.CancellationException) {
                        // User reopened the book within 2 minutes! Cleanup aborted safely.
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                pendingCacheCleanups["pdf_$bookId.pdf"] = cleanupJob

                activity?.let { act ->
                    val window = act.window
                    window.statusBarColor = android.graphics.Color.TRANSPARENT
                    window.navigationBarColor = android.graphics.Color.TRANSPARENT
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        window.isNavigationBarContrastEnforced = false
                        window.isStatusBarContrastEnforced = false
                    }
                    val controller = WindowCompat.getInsetsController(window, window.decorView)
                    controller.show(WindowInsetsCompat.Type.statusBars())
                    controller.isAppearanceLightStatusBars = !isAppDark
                    controller.isAppearanceLightNavigationBars = !isAppDark
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    val sharedTransitionScope = LocalSharedTransitionScope.current
    val animatedVisibilityScope = LocalAnimatedVisibilityScope.current
    val targetInitialPage = initialPage ?: if (savedPage > 0) savedPage else 0
    val isInitiallyCached = remember(item.id, targetInitialPage) {
        PdfBitmapCache.cache.get("pdf_${item.id}_page_$targetInitialPage") != null
    }
    var isFirstPageReady by remember(item.id) { mutableStateOf(isInitiallyCached) }

    val isGridMode = com.Nightjar.Th3GradeLibraryKMP.network.SyncEngine.isGridView.collectAsState().value
    val isNavigatingOut = animatedVisibilityScope?.transition?.targetState != androidx.compose.animation.EnterExitState.Visible
    val isFromBookmarksEffective = isFromBookmarks || sourcePage == "bookmarks"
    val isBookmarkStillPresent = initialPage?.let { isSaved(it) } ?: false
    val isBookmarkDeletedOnExit = isFromBookmarksEffective && !isBookmarkStillPresent

    val dynamicTargetCornerRadius = when {
        isBookmarkDeletedOnExit -> 40.dp // Smooth Egg/Pill Capsule Morph on Deletion
        isFromBookmarksEffective -> 14.dp
        isGridMode -> 20.dp
        else -> 28.dp
    }
    val cornerRadius by if (animatedVisibilityScope != null) {
        animatedVisibilityScope.transition.animateDp(
            transitionSpec = { spring(dampingRatio = 0.76f, stiffness = 220f) },
            targetValueByState = { state ->
                when (state) {
                    androidx.compose.animation.EnterExitState.Visible -> 0.dp
                    else -> dynamicTargetCornerRadius
                }
            },
            label = "corner"
        )
    } else remember { mutableStateOf(0.dp) }
    val safeCornerRadius = cornerRadius.coerceAtLeast(0.dp)

    val hasValidTarget = when {
        isFromBookmarksEffective -> (initialPage != null)
        else -> true
    }

    val sharedKey = when {
        isFromBookmarksEffective && initialPage != null -> "bookmark_${item.id}_$initialPage"
        else -> "card_${item.id}"
    }

    val sharedBoundsModifier = if (sharedTransitionScope != null && animatedVisibilityScope != null && hasValidTarget) {
        with(sharedTransitionScope) {
            Modifier.sharedBounds(
                sharedContentState = rememberSharedContentState(key = sharedKey),
                animatedVisibilityScope = animatedVisibilityScope,
                enter = androidx.compose.animation.fadeIn(
                    animationSpec = androidx.compose.animation.core.tween(
                        durationMillis = 130,
                        easing = androidx.compose.animation.core.LinearEasing
                    )
                ),
                exit = androidx.compose.animation.fadeOut(
                    animationSpec = androidx.compose.animation.core.tween(
                        durationMillis = 150,
                        delayMillis = 75,
                        easing = androidx.compose.animation.core.LinearEasing
                    )
                ),
                renderInOverlayDuringTransition = true,
                clipInOverlayDuringTransition = OverlayClip(androidx.compose.foundation.shape.RoundedCornerShape(safeCornerRadius)),
                boundsTransform = { _, _ ->
                    spring(dampingRatio = 0.82f, stiffness = 280f)
                },
                resizeMode = SharedTransitionScope.ResizeMode.RemeasureToBounds
            )
        }
    } else if (animatedVisibilityScope != null) {
        // Liquid Dynamic Island sink-down exit ONLY when bookmark is deleted or when opening from a shortcut/news
        with(animatedVisibilityScope) {
            Modifier.animateEnterExit(
                enter = androidx.compose.animation.fadeIn(androidx.compose.animation.core.tween(220)) + androidx.compose.animation.scaleIn(initialScale = 0.92f),
                exit = androidx.compose.animation.fadeOut(androidx.compose.animation.core.tween(220, delayMillis = 60)) + 
                       androidx.compose.animation.scaleOut(
                           targetScale = 0.28f, 
                           animationSpec = spring(dampingRatio = 0.78f, stiffness = Spring.StiffnessMediumLow)
                       ) + 
                       androidx.compose.animation.slideOutVertically(
                           targetOffsetY = { it },
                           animationSpec = spring(dampingRatio = 0.78f, stiffness = Spring.StiffnessMediumLow)
                       )
            )
        }
    } else Modifier

    val rootModifier = Modifier
        .fillMaxSize()
        .then(sharedBoundsModifier)
        .clip(androidx.compose.foundation.shape.RoundedCornerShape(safeCornerRadius))
        .background(MaterialTheme.colorScheme.background)

    Box(modifier = rootModifier) {
        if (loadingError != null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(Icons.Default.Error, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(64.dp))
                Spacer(modifier = Modifier.height(16.dp))
                Text(loadingError!!, color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(modifier = Modifier.height(24.dp))
                Button(onClick = onClose) {
                    Text("رجوع")
                }
            }
        } else if (pdfRenderer == null) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(24.dp)
                ) {
                    com.Nightjar.Th3GradeLibraryKMP.ui.ExpressiveLoadingIndicator(
                        modifier = Modifier.size(56.dp),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    if (isDownloading) {
                        val percent = (downloadProgress * 100).toInt()
                        Text(
                            text = if (percent > 0) "جاري التحميل من سحابة الكتب: $percent%" else "جاري بدء التحميل السحابي السريع...",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        LinearProgressIndicator(
                            progress = { downloadProgress },
                            modifier = Modifier
                                .width(220.dp)
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                        )
                    } else {
                        Text(
                            text = "جاري تهيئة وعرض الصفحات...",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        } else {
            val pageCount = pdfRenderer!!.pageCount
                val targetInitialPage = initialPage ?: if (pageCount > 0) savedPage.coerceAtMost(pageCount - 1) else 0
                androidx.compose.runtime.key(item.id, targetInitialPage) {
                    val pagerState = rememberPagerState(initialPage = targetInitialPage.coerceAtMost(maxOf(0, pageCount - 1))) { pageCount }

                    LaunchedEffect(targetInitialPage) {
                        if (pagerState.currentPage != targetInitialPage && targetInitialPage in 0 until pageCount) {
                            pagerState.scrollToPage(targetInitialPage)
                        }
                    }

                    LaunchedEffect(isNavigatingOut) {
                        if (isNavigatingOut && isFromBookmarksEffective && targetInitialPage in 0 until pageCount) {
                            if (pagerState.currentPage != targetInitialPage) {
                                pagerState.scrollToPage(targetInitialPage)
                            }
                        }
                    }

                    LaunchedEffect(pagerState.currentPage) {
                        if (pageCount > 0) {
                            com.Nightjar.Th3GradeLibraryKMP.network.SyncEngine.saveLastReadPage(item.id, pagerState.currentPage)
                        }
                    }

                    val currentPage = pagerState.currentPage
                    val currentLum = pageLuminanceMap[currentPage]

                    // Intelligently sync bars to current page contrast or reader background
                    LaunchedEffect(currentPage, currentLum, isAppDark) {
                        if (currentLum != null) {
                            isCurrentPageTopLight = currentLum.first
                            isCurrentPageBottomLight = currentLum.second
                        } else {
                            isCurrentPageTopLight = !isAppDark
                            isCurrentPageBottomLight = !isAppDark
                        }
                    }

                    var showJumpDialog by remember { mutableStateOf(false) }
                    var jumpPageInput by remember { mutableStateOf("") }
                    val coroutineScope = rememberCoroutineScope()
                    val focusRequester = remember { FocusRequester() }

                    LaunchedEffect(Unit) {
                        try {
                            focusRequester.requestFocus()
                        } catch (_: Exception) {}
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.background)
                            .focusRequester(focusRequester)
                            .focusable()
                            .onKeyEvent { keyEvent ->
                                if (keyEvent.type == KeyEventType.KeyDown) {
                                    when (keyEvent.key) {
                                        Key.DirectionRight, Key.PageDown, Key.Spacebar -> {
                                            if (pagerState.currentPage < pageCount - 1) {
                                                coroutineScope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                                                true
                                            } else false
                                        }
                                        Key.DirectionLeft, Key.PageUp -> {
                                            if (pagerState.currentPage > 0) {
                                                coroutineScope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) }
                                                true
                                            } else false
                                        }
                                        Key.Escape -> {
                                            onClose()
                                            true
                                        }
                                        else -> false
                                    }
                                } else false
                            }
                    ) {
                        // Render page content
                        Box(modifier = Modifier.fillMaxSize()) {
                            if (orientation == "vertical") {
                                VerticalPager(
                                    state = pagerState,
                                    beyondViewportPageCount = 1,
                                    modifier = Modifier.fillMaxSize(),
                                    contentPadding = PaddingValues(0.dp),
                                    pageSpacing = 0.dp
                                ) { pageIndex ->
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        val currentRenderer = pdfRenderer
                                        if (currentRenderer != null) {
                                            AndroidPdfPage(
                                                renderer = currentRenderer,
                                                pageIndex = pageIndex,
                                                bookId = item.id,
                                                scrollDirection = orientation,
                                                isAppDark = isAppDark,
                                                onZoomChanged = { isZoomed = it },
                                                onPageReady = {
                                                    if (pageIndex == pagerState.currentPage) {
                                                        isFirstPageReady = true
                                                    }
                                                },
                                                onLuminanceCalculated = { topLight, bottomLight ->
                                                    pageLuminanceMap[pageIndex] = (topLight to bottomLight)
                                                    if (pageIndex == pagerState.currentPage) {
                                                        isCurrentPageTopLight = topLight
                                                        isCurrentPageBottomLight = bottomLight
                                                    }
                                                }
                                            )
                                        }
                                    }
                                }
                            } else {
                                HorizontalPager(
                                    state = pagerState,
                                    beyondViewportPageCount = 1,
                                    modifier = Modifier.fillMaxSize(),
                                    contentPadding = PaddingValues(0.dp),
                                    pageSpacing = 0.dp
                                ) { pageIndex ->
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        val currentRenderer = pdfRenderer
                                        if (currentRenderer != null) {
                                            AndroidPdfPage(
                                                renderer = currentRenderer,
                                                pageIndex = pageIndex,
                                                bookId = item.id,
                                                scrollDirection = orientation,
                                                isAppDark = isAppDark,
                                                onZoomChanged = { isZoomed = it },
                                                onPageReady = {
                                                    if (pageIndex == pagerState.currentPage) {
                                                        isFirstPageReady = true
                                                    }
                                                },
                                                onLuminanceCalculated = { topLight, bottomLight ->
                                                    pageLuminanceMap[pageIndex] = (topLight to bottomLight)
                                                    if (pageIndex == pagerState.currentPage) {
                                                        isCurrentPageTopLight = topLight
                                                        isCurrentPageBottomLight = bottomLight
                                                    }
                                                },
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        }
                                    }
                                }
                            }
                        }

                    // Completely Floating Top control bar
                    AnimatedVisibility(
                        visible = !isZoomed,
                        enter = fadeIn() + slideInVertically { -it },
                        exit = fadeOut() + slideOutVertically { -it },
                        modifier = Modifier.align(Alignment.TopCenter)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .windowInsetsPadding(WindowInsets.statusBars.add(WindowInsets.captionBar))
                                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = onClose,
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f))
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Close", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }

                            Text(
                                text = item.title,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f).padding(horizontal = 16.dp)
                            )

                            // Toolbar actions
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                val isPageSaved = isSaved(pagerState.currentPage)
                                IconButton(
                                    onClick = { onToggleSave(pagerState.currentPage) },
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(if (isPageSaved) Color(0x33F59E0B) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f))
                                        .bounceClick { onToggleSave(pagerState.currentPage) }
                                ) {
                                    androidx.compose.animation.AnimatedContent(
                                        targetState = isPageSaved,
                                        transitionSpec = { androidx.compose.animation.fadeIn(androidx.compose.animation.core.tween(300)) + androidx.compose.animation.scaleIn(initialScale = 0.5f) togetherWith androidx.compose.animation.fadeOut(androidx.compose.animation.core.tween(300)) + androidx.compose.animation.scaleOut(targetScale = 0.5f) }
                                    ) { saved ->
                                        Icon(
                                            imageVector = if (saved) Icons.Default.Star else Icons.Default.StarBorder,
                                            contentDescription = "Bookmark",
                                            tint = if (saved) Color(0xFFF59E0B) else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                IconButton(
                                    onClick = {
                                        val newOrientation = if (orientation == "vertical") "horizontal" else "vertical"
                                        orientation = newOrientation
                                        com.Nightjar.Th3GradeLibraryKMP.network.SyncEngine.saveSettings(appSettings.copy(pdfScrollDirection = newOrientation))
                                    },
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.8f))
                                        .bounceClick {
                                            val newOrientation = if (orientation == "vertical") "horizontal" else "vertical"
                                            orientation = newOrientation
                                            com.Nightjar.Th3GradeLibraryKMP.network.SyncEngine.saveSettings(appSettings.copy(pdfScrollDirection = newOrientation))
                                        }
                                ) {
                                    androidx.compose.animation.AnimatedContent(
                                        targetState = orientation,
                                        transitionSpec = { androidx.compose.animation.fadeIn(androidx.compose.animation.core.tween(300)) + androidx.compose.animation.scaleIn(initialScale = 0.5f) togetherWith androidx.compose.animation.fadeOut(androidx.compose.animation.core.tween(300)) + androidx.compose.animation.scaleOut(targetScale = 0.5f) }
                                    ) { currentOrientation ->
                                        Icon(
                                            imageVector = if (currentOrientation == "vertical") Icons.Default.SwapVert else Icons.Default.SwapHoriz,
                                            contentDescription = "Change Scroll Direction",
                                            tint = MaterialTheme.colorScheme.onPrimary
                                        )
                                    }
                                }
                            }
                        }
                    }
                
                    // Floating Page Indicator capsule at bottom center
                    AnimatedVisibility(
                        visible = !isZoomed,
                        enter = fadeIn() + slideInVertically { it },
                        exit = fadeOut() + slideOutVertically { it },
                        modifier = Modifier.align(Alignment.BottomCenter)
                    ) {
                        Card(
                            modifier = Modifier
                                .navigationBarsPadding()
                                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
                                .padding(bottom = 20.dp)
                                .shadow(12.dp, shape = CircleShape),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f)
                            ),
                            shape = CircleShape
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                val currentPage = pagerState.currentPage + 1

                                IconButton(
                                    onClick = {
                                        scope.launch {
                                            val target = pagerState.targetPage
                                            if (target > 0) {
                                                pagerState.animateScrollToPage(target - 1)
                                            }
                                        }
                                    },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(Icons.Default.ChevronLeft, contentDescription = "Previous Page", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                }

                                Box(
                                    modifier = Modifier
                                        .animateContentSize(
                                            animationSpec = spring(dampingRatio = 0.82f, stiffness = 320f)
                                        )
                                        .clip(CircleShape)
                                        .clickable { showJumpDialog = true }
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        androidx.compose.animation.AnimatedContent(
                                            targetState = currentPage,
                                            transitionSpec = {
                                                (androidx.compose.animation.slideInVertically { it / 2 } + androidx.compose.animation.fadeIn()) togetherWith
                                                (androidx.compose.animation.slideOutVertically { -it / 2 } + androidx.compose.animation.fadeOut())
                                            }
                                        ) { page ->
                                            Text(
                                                text = "$page",
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp
                                            )
                                        }
                                        Text(
                                            text = " / $pageCount",
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        )
                                    }
                                }

                                IconButton(
                                    onClick = {
                                        scope.launch {
                                            val target = pagerState.targetPage
                                            if (target < pageCount - 1) {
                                                pagerState.animateScrollToPage(target + 1)
                                            }
                                        }
                                    },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(Icons.Default.ChevronRight, contentDescription = "Next Page", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }

                    // Animated Overlay for Jump Dialog
                    androidx.compose.animation.AnimatedVisibility(
                        visible = showJumpDialog,
                        enter = fadeIn(),
                        exit = fadeOut(),
                        modifier = Modifier.align(Alignment.Center)
                    ) {
                        var jumpErrorText by remember { mutableStateOf<String?>(null) }
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.5f))
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) { 
                                    showJumpDialog = false
                                    jumpPageInput = ""
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            androidx.compose.material3.Surface(
                                shape = RoundedCornerShape(28.dp),
                                color = MaterialTheme.colorScheme.surface,
                                tonalElevation = 6.dp,
                                modifier = Modifier
                                    .widthIn(max = 400.dp)
                                    .fillMaxWidth(0.85f)
                                    .clickable(enabled = false) {}
                                    .shadow(24.dp, RoundedCornerShape(28.dp))
                                    .animateEnterExit(
                                        enter = androidx.compose.animation.scaleIn(
                                            initialScale = 0.8f,
                                            animationSpec = spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessLow)
                                        ) + androidx.compose.animation.fadeIn(),
                                        exit = androidx.compose.animation.scaleOut(
                                            targetScale = 0.9f,
                                            animationSpec = spring(dampingRatio = 0.75f, stiffness = Spring.StiffnessLow)
                                        ) + androidx.compose.animation.fadeOut()
                                    )
                            ) {
                                Column(
                                    modifier = Modifier.padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "إلى أي صفحة تريد الانتقال؟",
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Justify
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    OutlinedTextField(
                                        value = jumpPageInput,
                                        onValueChange = { 
                                            jumpPageInput = it 
                                            jumpErrorText = null // Clear error on edit
                                        },
                                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                                        singleLine = true,
                                        placeholder = { Text("مثال: 15", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)) },
                                        colors = TextFieldDefaults.colors(
                                            focusedContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
                                            unfocusedContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
                                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                            focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                                            unfocusedIndicatorColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                                        ),
                                        modifier = Modifier.fillMaxWidth(0.9f),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    if (jumpErrorText != null) {
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = jumpErrorText!!,
                                            color = MaterialTheme.colorScheme.error,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(24.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        com.Nightjar.Th3GradeLibraryKMP.theme.MorphingButton(
                                            onClick = { 
                                                showJumpDialog = false 
                                                jumpPageInput = ""
                                            },
                                            modifier = Modifier.weight(1f),
                                            restRadius = 24.dp,
                                            pressedRadius = 8.dp,
                                            containerColor = Color.Transparent,
                                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                                        ) {
                                            Text(
                                                "إلغاء",
                                                textAlign = TextAlign.Justify
                                            )
                                        }
                                        com.Nightjar.Th3GradeLibraryKMP.theme.MorphingButton(
                                            onClick = {
                                                val p = jumpPageInput.toIntOrNull()
                                                if (p == null) {
                                                    jumpErrorText = "الرجاء إدخال رقم صحيح"
                                                } else if (p !in 1..pageCount) {
                                                    jumpErrorText = "رقم الصفحة غير موجود في هذا الملف (1 - $pageCount)"
                                                } else {
                                                    scope.launch { pagerState.animateScrollToPage(p - 1) }
                                                    showJumpDialog = false
                                                    jumpPageInput = ""
                                                    jumpErrorText = null
                                                }
                                            },
                                            modifier = Modifier.weight(1f),
                                            restRadius = 24.dp,
                                            pressedRadius = 8.dp,
                                            containerColor = MaterialTheme.colorScheme.primary,
                                            contentColor = MaterialTheme.colorScheme.primary.contrastingTextColor()
                                        ) {
                                            Text(
                                                "انتقال سريع",
                                                fontWeight = FontWeight.Bold,
                                                textAlign = TextAlign.Justify
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// Singleton Cache for PDF Pages to prevent lag during scrolling
object PdfBitmapCache {
    private val maxMemory = (Runtime.getRuntime().maxMemory() / 1024).toInt()
    private val cores = Runtime.getRuntime().availableProcessors()
    // Dynamic cache allocation based on hardware capabilities:
    // Flagships with 8+ cores & large heap (>= 512MB) get 1/4th of RAM for maximum performance with NO limits!
    // Mid-range devices get 1/8th of RAM.
    // Budget devices (Galaxy J7, RAM < 3GB, small heap) get 1/12th of RAM to prevent OOM.
    private val cacheDivisor = when {
        maxMemory >= 512 * 1024 && cores >= 8 -> 4
        maxMemory >= 256 * 1024 -> 8
        else -> 12
    }
    private val cacheSize = maxMemory / cacheDivisor
    val cache = object : LruCache<String, Bitmap>(cacheSize) {
        override fun sizeOf(key: String, bitmap: Bitmap): Int {
            return bitmap.byteCount / 1024
        }
    }

    fun evictBook(bookId: String) {
        try {
            val prefix = "pdf_${bookId}_"
            val snapshot = cache.snapshot()
            for (key in snapshot.keys) {
                if (key.startsWith(prefix)) {
                    cache.remove(key)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

fun clearPdfCache(cacheDir: File) {
    try {
        cacheDir.listFiles()?.forEach { file ->
            if (file.name.startsWith("pdf_") && (file.name.endsWith(".pdf") || file.name.endsWith(".tmp"))) {
                file.delete()
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

suspend fun androidx.compose.ui.input.pointer.PointerInputScope.detectZoomPanGestures(
    onGesture: (pan: Offset, zoom: Float) -> Boolean
) {
    awaitEachGesture {
        var zoom = 1f
        var pan = Offset.Zero
        var pastTouchSlop = false
        val touchSlop = viewConfiguration.touchSlop

        awaitFirstDown(requireUnconsumed = false)
        do {
            val event = awaitPointerEvent()
            val canceled = event.changes.any { it.isConsumed }
            if (!canceled) {
                val zoomChange = event.calculateZoom()
                val panChange = event.calculatePan()

                if (!pastTouchSlop) {
                    zoom *= zoomChange
                    pan += panChange

                    val centroidSize = event.calculateCentroidSize(useCurrent = false)
                    val zoomMotion = abs(1 - zoom) * centroidSize
                    val panMotion = pan.getDistance()

                    if (zoomMotion > touchSlop || panMotion > touchSlop) {
                        pastTouchSlop = true
                    }
                }

                if (pastTouchSlop) {
                    val consume = onGesture(panChange, zoomChange)
                    if (consume) {
                        event.changes.forEach {
                            if (it.positionChanged()) {
                                it.consume()
                            }
                        }
                    }
                }
            }
        } while (!canceled && event.changes.any { it.pressed })
    }
}

fun sampleBitmapLuminance(bitmap: Bitmap): Pair<Boolean, Boolean> {
    return try {
        if (bitmap.width <= 0 || bitmap.height <= 0) return true to true
        val w = bitmap.width
        val h = bitmap.height

        val topY = 24.coerceAtMost(h - 1)
        val topP1 = bitmap.getPixel((w * 0.15f).toInt().coerceIn(0, w - 1), topY)
        val topP2 = bitmap.getPixel(w / 2, topY)
        val topP3 = bitmap.getPixel((w * 0.85f).toInt().coerceIn(0, w - 1), topY)
        val avgTopLum = (androidx.core.graphics.ColorUtils.calculateLuminance(topP1) +
                         androidx.core.graphics.ColorUtils.calculateLuminance(topP2) +
                         androidx.core.graphics.ColorUtils.calculateLuminance(topP3)) / 3.0

        val botY = (h - 24).coerceIn(0, h - 1)
        val botP1 = bitmap.getPixel((w * 0.15f).toInt().coerceIn(0, w - 1), botY)
        val botP2 = bitmap.getPixel(w / 2, botY)
        val botP3 = bitmap.getPixel((w * 0.85f).toInt().coerceIn(0, w - 1), botY)
        val avgBotLum = (androidx.core.graphics.ColorUtils.calculateLuminance(botP1) +
                         androidx.core.graphics.ColorUtils.calculateLuminance(botP2) +
                         androidx.core.graphics.ColorUtils.calculateLuminance(botP3)) / 3.0

        (avgTopLum > 0.5) to (avgBotLum > 0.5)
    } catch (_: Exception) {
        true to true
    }
}

@Composable
fun AndroidPdfPage(
    renderer: PdfRenderer,
    pageIndex: Int,
    bookId: String = "",
    scrollDirection: String = "vertical",
    isAppDark: Boolean = false,
    onZoomChanged: (Boolean) -> Unit = {},
    onPageReady: () -> Unit = {},
    onLuminanceCalculated: (isTopLight: Boolean, isBottomLight: Boolean) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    val cacheKey = "pdf_${if (bookId.isNotEmpty()) bookId else renderer.hashCode().toString()}_page_$pageIndex"
    var bitmap by remember(pageIndex, bookId) { mutableStateOf<Bitmap?>(PdfBitmapCache.cache.get(cacheKey)) }
    
    // Zoom and pan states - reset when page changes
    var scale by remember(pageIndex) { mutableStateOf(1f) }
    var offset by remember(pageIndex) { mutableStateOf(Offset.Zero) }

    val scope = rememberCoroutineScope()

    LaunchedEffect(scale) {
        onZoomChanged(scale > 1.05f)
    }

    LaunchedEffect(bitmap, pageIndex) {
        if (bitmap != null) {
            onPageReady()

            // Asynchronous prefetch of next page on capable devices to eliminate scrolling lag
            val cores = Runtime.getRuntime().availableProcessors()
            val maxMemoryMb = (Runtime.getRuntime().maxMemory() / (1024 * 1024)).toInt()
            if (cores >= 6 && maxMemoryMb >= 384 && pageIndex + 1 < renderer.pageCount) {
                val nextKey = "pdf_${if (bookId.isNotEmpty()) bookId else renderer.hashCode().toString()}_page_${pageIndex + 1}"
                if (PdfBitmapCache.cache.get(nextKey) == null) {
                    val prefetchDispatcher = if (cores >= 8) Dispatchers.Default else Dispatchers.IO
                    scope.launch(prefetchDispatcher) {
                        try {
                            pdfMutex.withLock {
                                currentCoroutineContext().ensureActive()
                                val nextPage = renderer.openPage(pageIndex + 1)
                                try {
                                    val prefetchScale = if (maxMemoryMb >= 512 && cores >= 8) 2.2f else 1.5f
                                    val nextW = (nextPage.width * prefetchScale).toInt()
                                    val nextH = (nextPage.height * prefetchScale).toInt()
                                    val nextBmp = Bitmap.createBitmap(nextW, nextH, Bitmap.Config.ARGB_8888)
                                    val canvas = android.graphics.Canvas(nextBmp)
                                    canvas.drawColor(android.graphics.Color.WHITE)
                                    nextPage.render(nextBmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                                    PdfBitmapCache.cache.put(nextKey, nextBmp)
                                } finally {
                                    try { nextPage.close() } catch (_: Exception) {}
                                }
                            }
                        } catch (_: Exception) {}
                    }
                }
            }
        }
    }

    LaunchedEffect(pageIndex) {
        if (bitmap == null) {
            val cores = Runtime.getRuntime().availableProcessors()
            val maxMemoryMb = (Runtime.getRuntime().maxMemory() / (1024 * 1024)).toInt()
            // Low-end devices (<= 4 cores): use Dispatchers.IO to prevent freezing Compose UI thread.
            // High-end devices (>= 6 cores): use Dispatchers.Default for maximum multi-core CPU throughput.
            val renderDispatcher = if (cores >= 6) Dispatchers.Default else Dispatchers.IO
            withContext(renderDispatcher) {
                try {
                    pdfMutex.withLock {
                        currentCoroutineContext().ensureActive()
                        val page = renderer.openPage(pageIndex)
                        try {
                            // Hardware-profile Adaptive Scaling Engine (CPU Cores + RAM Heap):
                            // Ultra-powerful flagships, DeX & tablets get unleashed 2.2f sharpness with zero bottleneck
                            val maxMemoryMb = (Runtime.getRuntime().maxMemory() / (1024 * 1024)).toInt()
                            val cores = Runtime.getRuntime().availableProcessors()
                            val scaleFactor = when {
                                maxMemoryMb >= 512 && cores >= 8 -> 2.2f // Ultra high-end flagships & tablets: UNLEASHED!
                                maxMemoryMb >= 384 && cores >= 6 -> 1.5f  // Upper mid-range
                                maxMemoryMb >= 256 && cores >= 4 -> 1.2f  // Mid-range
                                else -> 1.0f // Low-end devices (Galaxy J7, RAM < 3GB, small heap, <= 4 cores)
                            }
                            
                            val width = (page.width * scaleFactor).toInt()
                            val height = (page.height * scaleFactor).toInt()
                            
                            val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                            
                            val canvas = android.graphics.Canvas(bmp)
                            canvas.drawColor(android.graphics.Color.WHITE)

                            page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                            PdfBitmapCache.cache.put(cacheKey, bmp)
                            bitmap = bmp
                        } finally {
                            try {
                                page.close()
                            } catch (_: Exception) {}
                        }
                    }
                } catch (e: kotlinx.coroutines.CancellationException) {
                    throw e
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        } else {
            onPageReady()
        }
    }

    val currentBitmap = bitmap
    if (currentBitmap != null) {
        BoxWithConstraints(
            modifier = modifier
                .fillMaxSize()
                .clipToBounds(),
            contentAlignment = Alignment.Center
        ) {
            val containerWidth = constraints.maxWidth.toFloat()
            val containerHeight = constraints.maxHeight.toFloat()
            
            val bitmapWidth = currentBitmap.width.toFloat()
            val bitmapHeight = currentBitmap.height.toFloat()
            val imageRatio = bitmapWidth / bitmapHeight
            val containerRatio = containerWidth / containerHeight
            
            // Fitted dimensions to ensure the whole page is visible without cropping
            val w: Float
            val h: Float
            if (imageRatio > containerRatio) {
                w = containerWidth
                h = containerWidth / imageRatio
            } else {
                h = containerHeight
                w = containerHeight * imageRatio
            }

            val touchesEdges = (scrollDirection == "vertical") || (scale > 1.05f) || (h >= containerHeight - 80f)

            LaunchedEffect(currentBitmap, scale, scrollDirection, isAppDark, h, containerHeight) {
                try {
                    val (bmpTopLight, bmpBotLight) = sampleBitmapLuminance(currentBitmap)
                    val effectiveTopLight = if (touchesEdges) bmpTopLight else !isAppDark
                    val effectiveBottomLight = if (touchesEdges) bmpBotLight else !isAppDark
                    onLuminanceCalculated(effectiveTopLight, effectiveBottomLight)
                } catch (_: Exception) {}
            }

            Image(
                bitmap = currentBitmap.asImageBitmap(),
                contentDescription = "الصفحة ${pageIndex + 1}",
                contentScale = ContentScale.FillBounds,
                modifier = Modifier
                    .requiredSize(
                        width = with(androidx.compose.ui.platform.LocalDensity.current) { w.toDp() },
                        height = with(androidx.compose.ui.platform.LocalDensity.current) { h.toDp() }
                    )
                    .graphicsLayer(
                        scaleX = scale,
                        scaleY = scale,
                        translationX = offset.x,
                        translationY = offset.y
                    )
                    .pointerInput(pageIndex) { // Key on pageIndex to reset input detector
                        detectTapGestures(
                            onDoubleTap = {
                                val targetScale = if (scale > 1.05f) 1f else 2.5f
                                val targetOffsetX = 0f
                                val targetOffsetY = 0f
                                
                                val startScale = scale
                                val startOffset = offset
                                
                                scope.launch {
                                    animate(0f, 1f) { fraction, _ ->
                                        scale = startScale + fraction * (targetScale - startScale)
                                        offset = Offset(
                                            startOffset.x + fraction * (targetOffsetX - startOffset.x),
                                            startOffset.y + fraction * (targetOffsetY - startOffset.y)
                                        )
                                    }
                                }
                            }
                        )
                    }
                    .pointerInput(pageIndex) { // Key on pageIndex to reset input detector
                        detectZoomPanGestures { pan, zoom ->
                            scale = (scale * zoom).coerceIn(1f, 15f)
                            
                            val maxOffsetX = maxOf(0f, (w * scale - containerWidth) / 2f)
                            val maxOffsetY = maxOf(0f, (h * scale - containerHeight) / 2f)
                            
                            val newOffsetX = (offset.x + pan.x).coerceIn(-maxOffsetX, maxOffsetX)
                            val newOffsetY = (offset.y + pan.y).coerceIn(-maxOffsetY, maxOffsetY)
                            
                            offset = Offset(newOffsetX, newOffsetY)
                            
                            // Determine if we should consume the gesture
                            val atHorizontalEdge = (newOffsetX == -maxOffsetX && pan.x < 0) || (newOffsetX == maxOffsetX && pan.x > 0)
                            val atVerticalEdge = (newOffsetY == -maxOffsetY && pan.y < 0) || (newOffsetY == maxOffsetY && pan.y > 0)
                            
                            if (scrollDirection == "horizontal" && atHorizontalEdge && abs(pan.x) > abs(pan.y)) {
                                false
                            } else if (scrollDirection == "vertical" && atVerticalEdge && abs(pan.y) > abs(pan.x)) {
                                false
                            } else {
                                scale > 1f
                            }
                        }
                    }
            )
        }
    } else {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            com.Nightjar.Th3GradeLibraryKMP.ui.ExpressiveLoadingIndicator(
                modifier = Modifier.size(40.dp),
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}
