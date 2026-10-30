package com.Nightjar.Th3GradeLibraryKMP

import com.Nightjar.Th3GradeLibraryKMP.theme.expressiveButtonMorph
import com.Nightjar.Th3GradeLibraryKMP.theme.bounceClick
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.zIndex
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.ui.draw.blur
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import com.Nightjar.Th3GradeLibraryKMP.data.BookItem
import com.Nightjar.Th3GradeLibraryKMP.data.AllItems
import com.Nightjar.Th3GradeLibraryKMP.data.PlatformActionHandler
import com.Nightjar.Th3GradeLibraryKMP.network.SyncEngine
import com.Nightjar.Th3GradeLibraryKMP.theme.Th3GradeTheme
import com.Nightjar.Th3GradeLibraryKMP.theme.liquidGlass
import com.Nightjar.Th3GradeLibraryKMP.theme.parseHexColor
import com.Nightjar.Th3GradeLibraryKMP.ui.*
import com.Nightjar.Th3GradeLibraryKMP.navigation.*
import androidx.lifecycle.viewmodel.compose.viewModel
import com.Nightjar.Th3GradeLibraryKMP.viewmodel.AppViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.foundation.Image
import org.jetbrains.compose.resources.painterResource
import androidx.compose.ui.graphics.graphicsLayer
import com.Nightjar.Th3GradeLibraryKMP.generated.resources.Res
import com.Nightjar.Th3GradeLibraryKMP.generated.resources.*
import kotlin.math.roundToInt

@OptIn(ExperimentalAnimationApi::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun App(
    platformActionHandler: PlatformActionHandler,
    systemAccentColor: String? = null,
    dynamicLightColorScheme: androidx.compose.material3.ColorScheme? = null,
    dynamicDarkColorScheme: androidx.compose.material3.ColorScheme? = null,
    initialBookId: String? = null,
    initialIsNote: Boolean = false,
    initialPage: String? = null,
    initialSearchQuery: String? = null,
    isProcessDeath: Boolean = false,
    onIntentConsumed: () -> Unit = {},
    bottomBannerAd: @Composable (() -> Unit)? = null
) {
    val appSettings by SyncEngine.appSettings.collectAsState()
    
    LaunchedEffect(platformActionHandler) {
        SyncEngine.platformActionHandler = platformActionHandler
    }

    val isDarkTheme = when (appSettings.theme) {
        "light" -> false
        "dark" -> true
        else -> androidx.compose.foundation.isSystemInDarkTheme()
    }

    val seedColorHex = if (appSettings.useMaterialYou && systemAccentColor != null) {
        systemAccentColor
    } else {
        appSettings.primaryColor
    }

    Th3GradeTheme(
        darkTheme = isDarkTheme,
        pureBlackMode = appSettings.pureBlackMode,
        pureWhiteMode = appSettings.pureWhiteMode,
        seedColorHex = seedColorHex,
        dynamicColorScheme = if (appSettings.useMaterialYou) {
            if (isDarkTheme) dynamicDarkColorScheme else dynamicLightColorScheme
        } else null
    ) {
        CompositionLocalProvider(
            LocalLayoutDirection provides LayoutDirection.Rtl,
            com.Nightjar.Th3GradeLibraryKMP.theme.LocalIsLowEndDevice provides (platformActionHandler?.isLowEndDevice() ?: false)
        ) {
            val isDirectShortcutLaunch = initialBookId != null || (initialPage != null && initialPage != "home")
            var showSplash by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(!isDirectShortcutLaunch) }

            androidx.compose.animation.AnimatedContent(
                targetState = showSplash,
                transitionSpec = {
                    (androidx.compose.animation.fadeIn(animationSpec = androidx.compose.animation.core.tween(500)) +
                     androidx.compose.animation.scaleIn(initialScale = 0.95f, animationSpec = androidx.compose.animation.core.spring(0.8f, 100f))) togetherWith
                    (androidx.compose.animation.fadeOut(animationSpec = androidx.compose.animation.core.tween(250)) +
                     androidx.compose.animation.scaleOut(targetScale = 1.02f, animationSpec = androidx.compose.animation.core.tween(250)))
                },
                label = "SplashTransition"
            ) { isSplash ->
                if (isSplash) {
                    com.Nightjar.Th3GradeLibraryKMP.ui.AnimatedSplashScreen(
                        onAnimationFinished = { 
                            showSplash = false 
                        }
                    )
                } else {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        AppContent(
                            platformActionHandler = platformActionHandler,
                            isDark = isDarkTheme,
                            initialBookId = initialBookId,
                            initialIsNote = initialIsNote,
                            initialPage = initialPage,
                            initialSearchQuery = initialSearchQuery,
                            isProcessDeath = isProcessDeath,
                            onIntentConsumed = onIntentConsumed,
                            bottomBannerAd = bottomBannerAd
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalAnimationApi::class, ExperimentalSharedTransitionApi::class)
val LocalSharedTransitionScope = compositionLocalOf<SharedTransitionScope?> { null }

@OptIn(ExperimentalSharedTransitionApi::class)
val LocalAnimatedVisibilityScope = compositionLocalOf<AnimatedVisibilityScope?> { null }

val LocalStableStatusBarHeight = staticCompositionLocalOf { 24.dp }

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun AppContent(
    platformActionHandler: PlatformActionHandler,
    isDark: Boolean,
    initialBookId: String?,
    initialIsNote: Boolean,
    initialPage: String?,
    initialSearchQuery: String?,
    isProcessDeath: Boolean = false,
    onIntentConsumed: () -> Unit,
    bottomBannerAd: @Composable (() -> Unit)? = null
) {
    val appSettings by SyncEngine.appSettings.collectAsState()
    val toastMessage by ToastManager.toastMessage.collectAsState()
    val scope = rememberCoroutineScope()
    val saveableStateHolder = androidx.compose.runtime.saveable.rememberSaveableStateHolder()

    
    // Hoisted scroll/list states for preserving positions across navigation
    val newsLazyGridState = androidx.compose.foundation.lazy.grid.rememberLazyGridState()
    val libraryHomeScrollState = androidx.compose.foundation.rememberScrollState()
    val initialBookIndex = remember(initialBookId, initialIsNote) {
        if (initialBookId != null && !initialIsNote) {
            AllItems.books.indexOfFirst { it.id == initialBookId }.takeIf { it >= 0 } ?: 0
        } else 0
    }
    val initialNoteIndex = remember(initialBookId, initialIsNote) {
        if (initialBookId != null && initialIsNote) {
            AllItems.notes.indexOfFirst { it.id == initialBookId }.takeIf { it >= 0 } ?: 0
        } else 0
    }
    val libraryBooksLazyGridState = androidx.compose.foundation.lazy.grid.rememberLazyGridState(initialFirstVisibleItemIndex = initialBookIndex)
    val libraryNotesLazyGridState = androidx.compose.foundation.lazy.grid.rememberLazyGridState(initialFirstVisibleItemIndex = initialNoteIndex)
    val bookmarksLazyGridState = androidx.compose.foundation.lazy.grid.rememberLazyGridState()
    val shortcutsLazyGridState = androidx.compose.foundation.lazy.grid.rememberLazyGridState()
    val aboutScrollState = androidx.compose.foundation.rememberScrollState()

    val viewModel: AppViewModel = viewModel()

    LaunchedEffect(isProcessDeath, initialPage, initialSearchQuery, initialBookId, initialIsNote) {
        viewModel.initLaunchState(
            isProcessDeath = isProcessDeath,
            initialPage = initialPage,
            initialSearchQuery = initialSearchQuery,
            initialBookId = initialBookId,
            initialIsNote = initialIsNote
        )
        onIntentConsumed()
    }

    val pageHistory = viewModel.pageHistory
    var page by viewModel::page
    val screenNavCounter by viewModel::screenNavCounter
    var isReturningFromPdf by viewModel::isReturningFromPdf

    val density = androidx.compose.ui.platform.LocalDensity.current
    val bottomBarHeightPx = with(density) { 200.dp.toPx() } // Safety height to slide completely off screen under nav bars
    var isBottomBarVisible by remember { mutableStateOf(true) }
    val bottomBarOffsetHeightPx by animateFloatAsState(
        targetValue = if (isBottomBarVisible) 0f else bottomBarHeightPx,
        animationSpec = tween(350, easing = FastOutSlowInEasing),
        label = "bottomBarOffset"
    )

    var canCurrentPageScroll by remember { mutableStateOf(false) }
    var accumulatedScrollY by remember { mutableStateOf(0f) }
    val nestedScrollConnection = remember(canCurrentPageScroll) {
        object : androidx.compose.ui.input.nestedscroll.NestedScrollConnection {
            override fun onPostScroll(
                consumed: androidx.compose.ui.geometry.Offset,
                available: androidx.compose.ui.geometry.Offset,
                source: androidx.compose.ui.input.nestedscroll.NestedScrollSource
            ): androidx.compose.ui.geometry.Offset {
                if (!canCurrentPageScroll || page == "home") {
                    isBottomBarVisible = true
                    accumulatedScrollY = 0f
                    return androidx.compose.ui.geometry.Offset.Zero
                }

                val delta = consumed.y
                if (delta == 0f) return androidx.compose.ui.geometry.Offset.Zero
                
                accumulatedScrollY += delta
                if (isBottomBarVisible) {
                    if (accumulatedScrollY < 0) {
                        if (accumulatedScrollY < -150f) {
                            isBottomBarVisible = false
                            accumulatedScrollY = 0f
                        }
                    } else {
                        accumulatedScrollY = 0f
                    }
                } else {
                    if (accumulatedScrollY > 0) {
                        if (accumulatedScrollY > 100f) {
                            isBottomBarVisible = true
                            accumulatedScrollY = 0f
                        }
                    } else {
                        accumulatedScrollY = 0f
                    }
                }
                
                return androidx.compose.ui.geometry.Offset.Zero
            }
        }
    }
    var isNavigatingBack by viewModel::isNavigatingBack

    fun popPage(): Boolean = viewModel.popPage()

    // Connect properties to SyncEngine (Unified Persistent Storage)
    val isGridView by SyncEngine.isGridView.collectAsState()
    val selectedCategory by SyncEngine.selectedCategory.collectAsState()

    var selectedItem by viewModel::selectedItem

    LaunchedEffect(page, selectedCategory, selectedItem) {
        isBottomBarVisible = true
        accumulatedScrollY = 0f
        canCurrentPageScroll = false
    }

    LaunchedEffect(isDark, page) {
        if (page != "pdf") {
            platformActionHandler.updateSystemBars(
                isLightStatusBars = !isDark,
                isLightNavigationBars = !isDark
            )
        }
    }
    var isSearchOpen by viewModel::isSearchOpen
    var searchQuery by viewModel::searchQuery

    var showOnboarding by remember { mutableStateOf(!appSettings.isOnboardingCompleted) }

    // Modal states hoisted in AppViewModel
    var isDrawerOpen by viewModel::isDrawerOpen
    var isAppSettingsOpen by viewModel::isAppSettingsOpen
    var isNewsSettingsOpen by viewModel::isNewsSettingsOpen
    var showClearCacheDialog by viewModel::showClearCacheDialog

    fun navigateTo(newPage: String) = viewModel.navigateTo(newPage)

    // Bookmarks list from SyncEngine (Persistent Cache)
    val savedItemIds by SyncEngine.savedBookmarks.collectAsState()
    val isTransitionActive = com.Nightjar.Th3GradeLibraryKMP.ui.TransitionTargetTracker.isTransitionActive
    val activeTargetId = com.Nightjar.Th3GradeLibraryKMP.ui.TransitionTargetTracker.activeTargetId

    val savedItems = remember(savedItemIds, isTransitionActive, isReturningFromPdf, activeTargetId) {
        val savedBookIds = savedItemIds.map { it.split(":")[0] }.toMutableSet()
        if ((isTransitionActive || isReturningFromPdf) && activeTargetId != null && activeTargetId.startsWith("bookmark_")) {
            val activeBookId = activeTargetId.substringAfter("bookmark_").substringBeforeLast("_")
            if (activeBookId.isNotEmpty()) savedBookIds.add(activeBookId)
        }
        (AllItems.books + AllItems.notes).filter { it.id in savedBookIds }
    }
    var pdfInitialPage by viewModel::pdfInitialPage

    // Centralized Unified Back Navigation Handler for ALL Platforms (Android, Desktop, Web, iOS)
    fun handleTopBarBack() {
        if (showClearCacheDialog) {
            showClearCacheDialog = false
        } else if (isDrawerOpen) {
            isDrawerOpen = false
        } else if (isNewsSettingsOpen) {
            isNewsSettingsOpen = false
        } else if (isAppSettingsOpen) {
            isAppSettingsOpen = false
        } else if (selectedItem != null) {
            isNavigatingBack = true
            viewModel.clearSelectedItem()
        } else if (page == "list") {
            isNavigatingBack = true
            navigateTo("home")
        } else {
            isNavigatingBack = true
            if (!popPage()) {
                navigateTo("home")
            }
        }
    }

    // Platform Back Handlers
    val isSettingsOpen = isAppSettingsOpen || isNewsSettingsOpen
    val backEnabled = showClearCacheDialog || isDrawerOpen || isSettingsOpen || page != "home" || selectedItem != null || pageHistory.value.size > 1
    PlatformBackHandler(enabled = backEnabled) {
        handleTopBarBack()
    }

    // Handle deep link / shortcut / notification parameters reactively
    LaunchedEffect(initialBookId, initialIsNote, initialPage, initialSearchQuery) {
        var consumed = false
        if (initialBookId != null) {
            val target = AllItems.books.firstOrNull { it.id == initialBookId }
                ?: AllItems.notes.firstOrNull { it.id == initialBookId }
            if (target != null) {
                SyncEngine.saveSelectedCategory(if (target.isNote) "notes" else "books")
                val list = if (target.isNote) AllItems.notes else AllItems.books
                val targetIdx = list.indexOfFirst { it.id == target.id }
                
                if (targetIdx >= 0) {
                    val state = if (target.isNote) libraryNotesLazyGridState else libraryBooksLazyGridState
                    scope.launch {
                        try {
                            state.scrollToItem(targetIdx)
                        } catch (_: Exception) {}
                    }
                }
                
                // Open directly into PDF reader with shared element transition
                com.Nightjar.Th3GradeLibraryKMP.ui.TransitionTargetTracker.activeTargetId = "card_${target.id}"
                com.Nightjar.Th3GradeLibraryKMP.ui.TransitionTargetTracker.isTransitionActive = true
                viewModel.pdfSourcePage = "list"
                viewModel.pageHistory.value = listOf("home", "list", "pdf")
                viewModel.selectItem(target, SyncEngine.lastReadPages.value[target.id])
                consumed = true
            }
        } else if (initialPage != null) {
            navigateTo(initialPage)
            consumed = true
        }
        if (initialSearchQuery != null) {
            searchQuery = initialSearchQuery
            isSearchOpen = true
            consumed = true
        }
        if (consumed) {
            onIntentConsumed()
        }
    }

    // Auto-fetch on start ONLY if the setting is "on_open" (sync on open)
    LaunchedEffect(appSettings.syncInterval) {
        if (appSettings.syncInterval == "on_open") {
            if (appSettings.lastSync > 0L) {
                scope.launch(kotlinx.coroutines.Dispatchers.Default) {
                    SyncEngine.fetchAndSync(forced = false)
                }
            }
        }
        // Removed the while(true) loop that was causing continuous UI updates.
        // Background sync is handled properly by WorkManager according to the user's interval.
    }

    // First time install auto-fetch & notification permission: trigger only after onboarding finish
    LaunchedEffect(showOnboarding) {
        if (!showOnboarding) {
            // Always sync when the app is fully opened by the user, regardless of background sync interval!
            scope.launch(kotlinx.coroutines.Dispatchers.Default) {
                SyncEngine.fetchAndSync(forced = true)
            }
        }
    }

    SharedTransitionLayout {
        CompositionLocalProvider(LocalSharedTransitionScope provides this@SharedTransitionLayout) {
            BoxWithConstraints(
                modifier = Modifier.fillMaxSize()
            ) {
        val screenWidthDp = with(androidx.compose.ui.platform.LocalDensity.current) { androidx.compose.ui.platform.LocalWindowInfo.current.containerSize.width.toDp() }
        val isWide = screenWidthDp >= 600.dp
        
        val currentNavBar = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
        val currentStatusBar = WindowInsets.statusBars.add(WindowInsets.captionBar).asPaddingValues().calculateTopPadding()
        
        val listBottomPadding = (if (isWide) 24.dp else 110.dp) + currentNavBar + (if (bottomBannerAd != null) 56.dp else 0.dp)

        CompositionLocalProvider(LocalStableStatusBarHeight provides currentStatusBar) {
        Box(
            modifier = Modifier
                .fillMaxSize()
        ) {
                // Background Orbs (Premium Liquid M3 Aesthetics) - Layer 0
                if (platformActionHandler?.isLowEndDevice() != true) {
                    Box(modifier = Modifier.fillMaxSize().zIndex(0f)) {
                    val orbSize = if (isWide) 200.dp else 300.dp
                    Box(
                        modifier = Modifier
                            .size(orbSize)
                            .align(Alignment.TopEnd)
                            .offset(x = (orbSize / 3), y = (-orbSize / 6))
                            .background(
                                Brush.radialGradient(
                                    listOf(
                                        parseHexColor(appSettings.primaryColor).copy(alpha = 0.15f),
                                        Color.Transparent
                                    )
                                )
                            )
                    )
                    Box(
                        modifier = Modifier
                            .size(orbSize)
                            .align(Alignment.BottomStart)
                            .offset(x = (-orbSize / 3), y = (orbSize / 3))
                            .background(
                                Brush.radialGradient(
                                    listOf(
                                        parseHexColor(appSettings.primaryColor).copy(alpha = 0.1f),
                                        Color.Transparent
                                    )
                                )
                            )
                    )
                }
                }

                // Page Content (Always matches full content area edge-to-edge)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .nestedScroll(nestedScrollConnection),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .zIndex(10f)
                    ) {
                        val screenEntranceSeenMap = remember { mutableMapOf<String, androidx.compose.runtime.snapshots.SnapshotStateMap<String, Boolean>>() }
                        AnimatedContent(
                            targetState = page,
                            modifier = Modifier.fillMaxSize(),
                            transitionSpec = {
                                val isToPdf = targetState == "pdf"
                                val isFromPdf = initialState == "pdf"

                                if (isToPdf) {
                                    (fadeIn(animationSpec = tween(durationMillis = 130, easing = androidx.compose.animation.core.LinearEasing))).togetherWith(
                                        fadeOut(animationSpec = tween(durationMillis = 130, easing = androidx.compose.animation.core.LinearEasing))
                                    ).apply {
                                        targetContentZIndex = 50f
                                    }
                                } else if (isFromPdf) {
                                    (fadeIn(animationSpec = tween(durationMillis = 150, delayMillis = 75, easing = androidx.compose.animation.core.LinearEasing))).togetherWith(
                                        fadeOut(animationSpec = tween(durationMillis = 150, delayMillis = 75, easing = androidx.compose.animation.core.LinearEasing))
                                    ).apply {
                                        targetContentZIndex = -1f
                                    }
                                } else {
                                    val pageOrder = listOf("home", "list", "news", "bookmarks", "shortcuts", "about")
                                    val initialIndex = pageOrder.indexOf(initialState).takeIf { it >= 0 } ?: 0
                                    val targetIndex = pageOrder.indexOf(targetState).takeIf { it >= 0 } ?: 0

                                    val fluidEasing = androidx.compose.animation.core.CubicBezierEasing(0.16f, 1f, 0.3f, 1f)
                                    val slideSpec = androidx.compose.animation.core.tween<androidx.compose.ui.unit.IntOffset>(
                                        durationMillis = 350,
                                        easing = fluidEasing
                                    )
                                    val fadeSpec = androidx.compose.animation.core.tween<Float>(
                                        durationMillis = 260,
                                        easing = androidx.compose.animation.core.FastOutSlowInEasing
                                    )
                                    val direction = if (isNavigatingBack || targetIndex < initialIndex) 1 else -1 // RTL: Forward is -1 (from left), Backward is 1 (from right)

                                    (slideInHorizontally(
                                        initialOffsetX = { fullWidth -> fullWidth * direction },
                                        animationSpec = slideSpec
                                    ) + fadeIn(animationSpec = fadeSpec)).togetherWith(
                                        slideOutHorizontally(
                                            targetOffsetX = { fullWidth -> -fullWidth * direction },
                                            animationSpec = slideSpec
                                        ) + fadeOut(animationSpec = fadeSpec)
                                    )
                                }
                            }
                        ) { targetPage ->
                            val currentTransition = this@AnimatedContent.transition
                            LaunchedEffect(currentTransition.currentState, currentTransition.targetState, targetPage) {
                                if (currentTransition.currentState == currentTransition.targetState &&
                                    page != "pdf"
                                ) {
                                    com.Nightjar.Th3GradeLibraryKMP.ui.TransitionTargetTracker.isTransitionActive = false
                                    com.Nightjar.Th3GradeLibraryKMP.ui.TransitionTargetTracker.activeTargetId = null
                                }
                            }
                            val pageContentPadding = PaddingValues(
                                end = if (isWide && targetPage in listOf("home", "list", "news", "bookmarks", "shortcuts", "about")) 90.dp else 0.dp
                            )
                            val innerHorizontalInsets = if (targetPage != "pdf") WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal) else WindowInsets(0, 0, 0, 0)
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .windowInsetsPadding(innerHorizontalInsets)
                                    .padding(pageContentPadding),
                                contentAlignment = Alignment.TopCenter
                            ) {
                                val seenKeysForScreen = remember(targetPage, screenNavCounter) {
                                    screenEntranceSeenMap.getOrPut("${targetPage}_$screenNavCounter") { androidx.compose.runtime.mutableStateMapOf() }
                                }
                                CompositionLocalProvider(
                                    LocalAnimatedVisibilityScope provides this@AnimatedContent,
                                    com.Nightjar.Th3GradeLibraryKMP.theme.LocalIsNavigatingBack provides isNavigatingBack,
                                    com.Nightjar.Th3GradeLibraryKMP.theme.LocalSeenEntranceItems provides seenKeysForScreen,
                                    com.Nightjar.Th3GradeLibraryKMP.theme.LocalScreenEntranceKey provides "${targetPage}_$screenNavCounter",
                                    com.Nightjar.Th3GradeLibraryKMP.theme.LocalIsReturningFromPdf provides isReturningFromPdf
                                ) { saveableStateHolder.SaveableStateProvider(targetPage) {
                                when (targetPage) {
                                    "home" -> RenderHomeDestination(
                                        isGridView = isGridView,
                                        searchQuery = searchQuery,
                                        onSearchQueryChange = { searchQuery = it },
                                        onNavigateToCategory = {
                                            SyncEngine.saveSelectedCategory(it)
                                            navigateTo("list")
                                        },
                                        onNavigateToPdf = {
                                            viewModel.selectItem(it, SyncEngine.lastReadPages.value[it.id])
                                        },
                                        isDark = isDark,
                                        onToggleViewMode = { SyncEngine.saveGridView(!isGridView) },
                                        isSearchOpen = isSearchOpen,
                                        onToggleSearch = { isSearchOpen = !isSearchOpen },
                                        onVoiceSearchTrigger = {
                                            platformActionHandler.showToast("جاري الاستماع للبحث الصوتي...")
                                        },
                                        bottomPadding = listBottomPadding,
                                        onScrollableStateChanged = { canCurrentPageScroll = it },
                                        scrollState = libraryHomeScrollState
                                    )
                                    "list" -> RenderListDestination(
                                        category = selectedCategory ?: "books",
                                        isGridView = isGridView,
                                        searchQuery = searchQuery,
                                        onSearchQueryChange = { searchQuery = it },
                                        onNavigateToCategory = {
                                            SyncEngine.saveSelectedCategory(it)
                                            navigateTo("list")
                                        },
                                        onNavigateToPdf = {
                                            viewModel.selectItem(it, SyncEngine.lastReadPages.value[it.id])
                                        },
                                        isDark = isDark,
                                        onToggleViewMode = { SyncEngine.saveGridView(!isGridView) },
                                        isSearchOpen = isSearchOpen,
                                        onToggleSearch = { isSearchOpen = !isSearchOpen },
                                        onVoiceSearchTrigger = {
                                            platformActionHandler.showToast("جاري الاستماع للبحث الصوتي...")
                                        },
                                        bottomPadding = listBottomPadding,
                                        onScrollableStateChanged = { canCurrentPageScroll = it },
                                        lazyGridState = if (selectedCategory == "books") libraryBooksLazyGridState else libraryNotesLazyGridState
                                    )
                                    "news" -> RenderNewsDestination(
                                        onOpenSettings = { isNewsSettingsOpen = true },
                                        onItemClick = { url, _ ->
                                            platformActionHandler.openCustomTab(url, appSettings.primaryColor)
                                        },
                                        isDark = isDark,
                                        bottomPadding = listBottomPadding,
                                        onScrollableStateChanged = { canCurrentPageScroll = it },
                                        lazyGridState = newsLazyGridState
                                    )
                                    "bookmarks" -> RenderBookmarksDestination(
                                        savedItems = savedItems,
                                        savedItemIds = savedItemIds,
                                        isTransitionActive = isTransitionActive,
                                        isReturningFromPdf = isReturningFromPdf,
                                        activeTargetId = activeTargetId,
                                        isDark = isDark,
                                        onNavigateToPdf = { item, pageIdx ->
                                            viewModel.selectItem(item, pageIdx, fromBookmarks = true)
                                        },
                                        bottomPadding = listBottomPadding,
                                        onScrollableStateChanged = { canCurrentPageScroll = it },
                                        lazyGridState = bookmarksLazyGridState
                                    )
                                    "shortcuts" -> RenderShortcutsDestination(
                                        platformActionHandler = platformActionHandler,
                                        isDark = isDark,
                                        bottomPadding = listBottomPadding,
                                        onScrollableStateChanged = { canCurrentPageScroll = it },
                                        lazyGridState = shortcutsLazyGridState
                                    )
                                    "about" -> RenderAboutDestination(
                                        onOpenUrl = { url ->
                                            platformActionHandler.showToast("تحويل إلى تليغرام...")
                                            platformActionHandler.openUrl(url)
                                        },
                                        isDark = isDark,
                                        bottomPadding = listBottomPadding,
                                        onScrollableStateChanged = { canCurrentPageScroll = it },
                                        scrollState = aboutScrollState
                                    )
                                    "pdf" -> {
                                        val currentItem = selectedItem
                                        if (currentItem != null) {
                                            RenderPdfDestination(
                                                item = currentItem,
                                                savedItemIds = savedItemIds,
                                                onToggleBookmark = { pageIdx ->
                                                    val key = "${currentItem.id}:$pageIdx"
                                                    val isNowSaved = SyncEngine.toggleBookmark(key)
                                                    if (!isNowSaved) {
                                                        platformActionHandler.showToast("تم حذف الصفحة ${pageIdx + 1} من المحفوظات")
                                                    } else {
                                                        platformActionHandler.showToast("تم حفظ الصفحة ${pageIdx + 1} في المحفوظات")
                                                    }
                                                },
                                                onClose = {
                                                    handleTopBarBack()
                                                },
                                                initialPage = pdfInitialPage,
                                                isDark = isDark,
                                                isFromBookmarks = viewModel.isFromBookmarks,
                                                sourcePage = viewModel.pdfSourcePage
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

                // --- Floating App Bar (Always visible except PDF, WebView, and Onboarding) ---
                androidx.compose.animation.AnimatedVisibility(
                    visible = !showOnboarding && page != "pdf" && page != "web",
                    enter = slideInVertically(animationSpec = spring(dampingRatio = 0.76f, stiffness = 280f)) { -it } + fadeIn(tween(260)),
                    exit = slideOutVertically(animationSpec = spring(dampingRatio = 0.76f, stiffness = 280f)) { -it } + fadeOut(tween(220)),
                    modifier = Modifier.align(Alignment.TopCenter).fillMaxWidth().zIndex(200f).then(
                        if (LocalSharedTransitionScope.current != null) {
                            with(LocalSharedTransitionScope.current!!) {
                                Modifier.renderInSharedTransitionScopeOverlay(zIndexInOverlay = 50f)
                            }
                        } else Modifier
                    )
                ) {
                    val topBarHeight = if (isWide) 42.dp else 56.dp // Strictly < 56dp portrait, 42dp landscape
                    val topBarPaddingV = if (isWide) 4.dp else 6.dp
                    Box(
                        modifier = Modifier
                            .windowInsetsPadding(WindowInsets.statusBars.add(WindowInsets.captionBar))
                            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
                            .padding(
                                start = 16.dp,
                                end = if (isWide) 96.dp else 16.dp,
                                top = topBarPaddingV,
                                bottom = topBarPaddingV
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            modifier = Modifier
                                .widthIn(max = 608.dp)
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // 1. Detached Floating Elements (Circle Back/Icon + Oval Title Pill)
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // A. Circular Detached Island: Back Button or App Icon
                                Box(
                                    modifier = Modifier
                                        .size(topBarHeight)
                                        .liquidGlass(isDark, borderRadius = topBarHeight / 2, alpha = 0.95f),
                                    contentAlignment = Alignment.Center
                                ) {
                                    androidx.compose.animation.AnimatedVisibility(
                                        visible = (page != "home"),
                                        enter = fadeIn(tween(220)) + scaleIn(initialScale = 0.75f, animationSpec = tween(220)),
                                        exit = fadeOut(tween(200)) + scaleOut(targetScale = 0.75f, animationSpec = tween(200))
                                    ) {
                                        IconButton(
                                            onClick = {
                                                handleTopBarBack()
                                            },
                                            modifier = Modifier.size(topBarHeight)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.ChevronRight,
                                                contentDescription = "Back",
                                                tint = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }

                                    androidx.compose.animation.AnimatedVisibility(
                                        visible = (page == "home"),
                                        enter = fadeIn(tween(220)) + scaleIn(initialScale = 0.75f, animationSpec = tween(220)),
                                        exit = fadeOut(tween(200)) + scaleOut(targetScale = 0.75f, animationSpec = tween(200))
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(topBarHeight - 8.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    Brush.radialGradient(
                                                        listOf(
                                                            parseHexColor(appSettings.primaryColor).copy(alpha = 0.5f),
                                                            Color.Transparent
                                                        )
                                                    )
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Image(
                                                painter = painterResource(com.Nightjar.Th3GradeLibraryKMP.generated.resources.Res.drawable.app_icon),
                                                contentDescription = "App Icon",
                                                modifier = Modifier.size(topBarHeight - 16.dp).clip(CircleShape)
                                            )
                                        }
                                    }
                                }

                                // B. Oval Detached Island: Screen Name Title
                                var savedTitle by remember { mutableStateOf("مكتبة الثالث متوسط") }
                                val appBarTitleRaw = when (page) {
                                    "news" -> "آخر الأخبار"
                                    "bookmarks" -> "المحفوظات"
                                    "shortcuts" -> "إضافة اختصار"
                                    "about" -> "حول التطبيق"
                                    "list" -> if (selectedCategory == "books") "الكتب الرسمية" else "الملازم الدراسية"
                                    else -> "مكتبة الثالث متوسط"
                                }
                                if (page != "pdf" && page != "error") {
                                    savedTitle = appBarTitleRaw
                                }

                                Box(
                                    modifier = Modifier
                                        .height(topBarHeight)
                                        .liquidGlass(isDark, borderRadius = topBarHeight / 2, alpha = 0.95f)
                                        .padding(horizontal = 14.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    AnimatedContent(
                                        targetState = savedTitle,
                                        transitionSpec = {
                                            (slideInVertically { height -> height / 2 } + fadeIn(tween(220)))
                                                .togetherWith(slideOutVertically { height -> -height / 2 } + fadeOut(tween(220)))
                                        },
                                        label = "titleTransition"
                                    ) { targetTitle ->
                                        Text(
                                            text = targetTitle,
                                            fontSize = if (isWide) 15.sp else 17.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = if (targetTitle == "مكتبة الثالث متوسط") com.Nightjar.Th3GradeLibraryKMP.theme.KufiReemFontFamily else null,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }

                            // 2. Detached Actions: Separate Floating Circular Buttons
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                AnimatedContent(
                                    targetState = page,
                                    transitionSpec = {
                                        (scaleIn(initialScale = 0.8f, animationSpec = spring(dampingRatio = 0.72f, stiffness = 260f)) + fadeIn(tween(220)))
                                            .togetherWith(scaleOut(targetScale = 0.8f, animationSpec = tween(180)) + fadeOut(tween(180)))
                                            .using(SizeTransform(clip = false))
                                    },
                                    label = "topBarActions"
                                ) { targetPage ->
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        if (targetPage == "list") {
                                            // Circular Detached Grid/List Toggle Button
                                            Box(
                                                modifier = Modifier
                                                    .size(topBarHeight)
                                                    .liquidGlass(isDark, borderRadius = topBarHeight / 2, alpha = 0.95f),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                IconButton(
                                                    onClick = { SyncEngine.saveGridView(!isGridView) },
                                                    modifier = Modifier.size(topBarHeight)
                                                ) {
                                                    androidx.compose.animation.AnimatedContent(
                                                        targetState = isGridView,
                                                        transitionSpec = {
                                                            (fadeIn(tween(200)) + scaleIn(initialScale = 0.8f)).togetherWith(fadeOut(tween(160)) + scaleOut(targetScale = 0.8f))
                                                        },
                                                        label = "gridToggle"
                                                    ) { isGrid ->
                                                        Icon(
                                                            imageVector = if (isGrid) Icons.Default.ViewList else Icons.Default.GridView,
                                                            contentDescription = "Toggle Grid/List view",
                                                            tint = MaterialTheme.colorScheme.onSurface
                                                        )
                                                    }
                                                }
                                            }

                                            // Circular Detached Search Button
                                            Box(
                                                modifier = Modifier
                                                    .size(topBarHeight)
                                                    .liquidGlass(isDark, borderRadius = topBarHeight / 2, alpha = 0.95f),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                IconButton(
                                                    onClick = { isSearchOpen = !isSearchOpen },
                                                    modifier = Modifier.size(topBarHeight)
                                                ) {
                                                    androidx.compose.animation.AnimatedContent(
                                                        targetState = isSearchOpen,
                                                        transitionSpec = {
                                                            (fadeIn(tween(200)) + scaleIn(initialScale = 0.8f)).togetherWith(fadeOut(tween(160)) + scaleOut(targetScale = 0.8f))
                                                        },
                                                        label = "searchToggle"
                                                    ) { isOpen ->
                                                        Icon(
                                                            imageVector = if (isOpen) Icons.Default.Close else Icons.Default.Search,
                                                            contentDescription = "Toggle Search",
                                                            tint = MaterialTheme.colorScheme.onSurface
                                                        )
                                                    }
                                                }
                                            }
                                        } else if (targetPage == "news") {
                                            // Circular Detached Settings Button
                                            Box(
                                                modifier = Modifier
                                                    .size(topBarHeight)
                                                    .liquidGlass(isDark, borderRadius = topBarHeight / 2, alpha = 0.95f),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                IconButton(
                                                    onClick = { isNewsSettingsOpen = true },
                                                    modifier = Modifier.size(topBarHeight)
                                                ) {
                                                    Icon(
                                                        Icons.Default.Settings,
                                                        contentDescription = "Settings",
                                                        tint = MaterialTheme.colorScheme.onSurface
                                                    )
                                                }
                                            }
                                        } else {
                                            Spacer(modifier = Modifier.width(0.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // --- Floating Search Bar (Aligned inside the Main Content Area) ---
                androidx.compose.animation.AnimatedVisibility(
                    visible = !showOnboarding && page == "list" && isSearchOpen,
                    enter = slideInVertically(
                        initialOffsetY = { -it / 2 },
                        animationSpec = spring(dampingRatio = 0.75f, stiffness = Spring.StiffnessLow)
                    ) + fadeIn(animationSpec = tween(300)),
                    exit = slideOutVertically(
                        targetOffsetY = { -it / 2 },
                        animationSpec = spring(dampingRatio = 0.75f, stiffness = Spring.StiffnessMediumLow)
                    ) + fadeOut(animationSpec = tween(200)),
                    modifier = Modifier.align(Alignment.TopCenter).zIndex(200f).then(
                        if (LocalSharedTransitionScope.current != null) {
                            with(LocalSharedTransitionScope.current!!) {
                                Modifier.renderInSharedTransitionScopeOverlay(zIndexInOverlay = 50f)
                            }
                        } else Modifier
                    )
                ) {
                    val searchBarHeight = if (isWide) 48.dp else 56.dp
                    val topPadding = if (isWide) 64.dp else 80.dp
                    val searchFontSize = if (isWide) 14.sp else 15.sp
                    var isListening by remember { mutableStateOf(false) }

                    var isClearingAnimation by remember { mutableStateOf(false) }
                    var displayClearText by remember { mutableStateOf("") }
                    val clearTextSlideOffset = remember { androidx.compose.animation.core.Animatable(0f) }
                    val clearTextSlideAlpha = remember { androidx.compose.animation.core.Animatable(1f) }
                    
                    DisposableEffect(Unit) {
                        onDispose {
                            if (isListening) {
                                platformActionHandler.stopVoiceSearch()
                                isListening = false
                            }
                        }
                    }
                    
                    val pulseAnim = animateFloatAsState(
                        targetValue = if (isListening) 1.2f else 1f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(500, easing = FastOutSlowInEasing),
                            repeatMode = RepeatMode.Reverse
                        )
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
                            .padding(
                                top = topPadding,
                                start = 16.dp,
                                end = if (isWide) 96.dp else 16.dp
                            ),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        Row(
                            modifier = Modifier
                                .widthIn(max = 608.dp)
                                .fillMaxWidth()
                                .defaultMinSize(minHeight = searchBarHeight)
                                .wrapContentHeight()
                                .liquidGlass(isDark, borderRadius = 28.dp, alpha = 0.95f)
                                .animateContentSize(spring(dampingRatio = 0.65f, stiffness = 180f))
                                .padding(horizontal = 16.dp, vertical = if (isWide) 4.dp else 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                modifier = Modifier.size(if (isWide) 20.dp else 24.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Box(
                                modifier = Modifier.weight(1f),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                BasicTextField(
                                    value = if (isListening) "جاري الاستماع..." else searchQuery,
                                    onValueChange = { if (!isListening && !isClearingAnimation) searchQuery = it },
                                    textStyle = LocalTextStyle.current.copy(
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontSize = searchFontSize
                                    ),
                                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                    readOnly = isListening || isClearingAnimation,
                                    decorationBox = { innerTextField ->
                                        Box(
                                            modifier = Modifier.fillMaxWidth(),
                                            contentAlignment = Alignment.CenterStart
                                        ) {
                                            androidx.compose.animation.AnimatedVisibility(
                                                visible = searchQuery.isEmpty() && !isListening && !isClearingAnimation,
                                                enter = androidx.compose.animation.fadeIn(animationSpec = tween(220, delayMillis = 150)),
                                                exit = androidx.compose.animation.fadeOut(animationSpec = tween(120))
                                            ) {
                                                Text(
                                                    text = "ابحث عن ملزمة أو كتاب...",
                                                    style = LocalTextStyle.current.copy(
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                                        fontSize = searchFontSize
                                                    ),
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                            if (!isClearingAnimation) {
                                                innerTextField()
                                            }
                                        }
                                    }
                                )
                                if (isClearingAnimation && displayClearText.isNotEmpty()) {
                                    Text(
                                        text = displayClearText,
                                        style = LocalTextStyle.current.copy(
                                            color = MaterialTheme.colorScheme.onSurface,
                                            fontSize = searchFontSize
                                        ),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.graphicsLayer {
                                            translationX = clearTextSlideOffset.value
                                            alpha = clearTextSlideAlpha.value
                                            scaleX = clearTextSlideAlpha.value
                                            scaleY = clearTextSlideAlpha.value
                                        }
                                    )
                                }
                            }
                            
                            IconButton(
                                onClick = {
                                    if (!isListening) {
                                        isListening = true
                                        platformActionHandler.startVoiceSearch(
                                            onResult = { text ->
                                                searchQuery = text
                                            },
                                            onEnd = {
                                                isListening = false
                                            }
                                        )
                                    } else {
                                        platformActionHandler.stopVoiceSearch()
                                        isListening = false
                                    }
                                },
                                modifier = Modifier
                                    .size(if (isWide) 36.dp else 40.dp)
                                    .graphicsLayer {
                                        val s = if (isListening) pulseAnim.value else 1f
                                        scaleX = s
                                        scaleY = s
                                    }
                                    .background(
                                        color = if (isListening) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else Color.Transparent,
                                        shape = CircleShape
                                    )
                            ) {
                                androidx.compose.animation.AnimatedContent(
                                    targetState = isListening,
                                    transitionSpec = {
                                        (androidx.compose.animation.scaleIn(initialScale = 0.7f) + androidx.compose.animation.fadeIn()) togetherWith
                                        (androidx.compose.animation.scaleOut(targetScale = 1.3f) + androidx.compose.animation.fadeOut())
                                    }
                                ) { listening ->
                                    Icon(
                                        imageVector = if (listening) Icons.Default.GraphicEq else Icons.Default.Mic,
                                        contentDescription = "Voice search",
                                        tint = if (listening) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(if (isWide) 20.dp else 24.dp)
                                    )
                                }
                            }

                            androidx.compose.animation.AnimatedVisibility(
                                visible = (searchQuery.isNotEmpty() || isClearingAnimation) && !isListening,
                                enter = androidx.compose.animation.fadeIn(animationSpec = tween(220)) + 
                                        androidx.compose.animation.scaleIn(initialScale = 0.8f, animationSpec = spring(dampingRatio = 0.7f, stiffness = 200f)) +
                                        androidx.compose.animation.expandHorizontally(expandFrom = Alignment.End, animationSpec = spring(dampingRatio = 0.8f, stiffness = 250f)),
                                exit = androidx.compose.animation.fadeOut(animationSpec = tween(200)) + 
                                       androidx.compose.animation.scaleOut(targetScale = 0.5f, animationSpec = tween(200)) + 
                                       androidx.compose.animation.slideOutVertically(targetOffsetY = { it / 2 }, animationSpec = tween(200)) +
                                       androidx.compose.animation.shrinkHorizontally(shrinkTowards = Alignment.End, animationSpec = tween(200))
                            ) {
                                val clearSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                                IconButton(
                                    onClick = {
                                        val textToClear = searchQuery
                                        if (textToClear.isNotEmpty() && !isClearingAnimation) {
                                            displayClearText = textToClear
                                            isClearingAnimation = true
                                            searchQuery = ""
                                            scope.launch {
                                                clearTextSlideOffset.snapTo(0f)
                                                clearTextSlideAlpha.snapTo(1f)
                                                launch {
                                                    clearTextSlideOffset.animateTo(
                                                        targetValue = 90f,
                                                        animationSpec = tween(280, easing = androidx.compose.animation.core.CubicBezierEasing(0.2f, 0f, 0.2f, 1f))
                                                    )
                                                }
                                                launch {
                                                    clearTextSlideAlpha.animateTo(
                                                        targetValue = 0f,
                                                        animationSpec = tween(220, easing = androidx.compose.animation.core.LinearEasing)
                                                    )
                                                }
                                                kotlinx.coroutines.delay(280)
                                                isClearingAnimation = false
                                                displayClearText = ""
                                                clearTextSlideOffset.snapTo(0f)
                                                clearTextSlideAlpha.snapTo(1f)
                                            }
                                        }
                                    },
                                    modifier = Modifier
                                        .size(if (isWide) 36.dp else 40.dp)
                                        .expressiveButtonMorph(
                                            restRadius = if (isWide) 18.dp else 20.dp,
                                            pressedRadius = 8.dp,
                                            interactionSource = clearSource
                                        ),
                                    interactionSource = clearSource
                                ) {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = "Clear search",
                                        modifier = Modifier.size(if (isWide) 18.dp else 22.dp)
                                    )
                                }
                            }
                        }
                    }
                }



        // --- Mobile Bottom Navigation Capsule (Only visible on small screens and main tabs) ---
        val mainTabs = listOf("home", "list", "news", "bookmarks", "shortcuts", "about")
        androidx.compose.animation.AnimatedVisibility(
            visible = !showOnboarding && page in mainTabs && !isWide,
            enter = slideInVertically(animationSpec = spring(dampingRatio = 0.76f, stiffness = 280f)) { it } + fadeIn(tween(260)),
            exit = slideOutVertically(animationSpec = spring(dampingRatio = 0.76f, stiffness = 280f)) { it } + fadeOut(tween(220)),
            modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().zIndex(200f).then(
                if (LocalSharedTransitionScope.current != null) {
                    with(LocalSharedTransitionScope.current!!) {
                        Modifier.renderInSharedTransitionScopeOverlay(zIndexInOverlay = 50f)
                    }
                } else Modifier
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom))
                    .offset { androidx.compose.ui.unit.IntOffset(x = 0, y = bottomBarOffsetHeightPx.roundToInt()) }
                    .padding(bottom = 16.dp, start = 16.dp, end = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                val mainNavItems = listOf(
                    Triple("المكتبة", Icons.Default.Home) {
                        navigateTo("home")
                        SyncEngine.saveSelectedCategory(null)
                    },
                    Triple("الأخبار", Icons.Default.Newspaper) { navigateTo("news") },
                    Triple("المحفوظات", Icons.Default.Star) { navigateTo("bookmarks") }
                )

                val isMainActive = listOf(page == "home" || page == "list", page == "news", page == "bookmarks")
                var lastValidIndex by remember { mutableStateOf(0) }
                val currentIndex = isMainActive.indexOfFirst { it }.takeIf { it >= 0 }
                if (currentIndex != null) {
                    lastValidIndex = currentIndex
                }
                val selectedIndex = lastValidIndex
                val isMenuButtonActive = isDrawerOpen || page == "about" || page == "shortcuts"

                Row(
                    modifier = Modifier
                        .widthIn(max = 480.dp)
                        .fillMaxWidth()
                        .height(if (isWide) 48.dp else 64.dp), // Strictly < 56dp portrait, 44dp landscape
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    // 1. Elongated Main Pill with 3 Tabs & Sliding Indicator
                    BoxWithConstraints(
                        modifier = Modifier
                            .weight(1f)
                            .height(if (isWide) 48.dp else 64.dp)
                            .border(
                                width = 1.dp,
                                brush = Brush.linearGradient(listOf(Color.White.copy(alpha = 0.25f), Color.White.copy(alpha = 0.05f))),
                                shape = RoundedCornerShape(26.dp)
                            )
                            .liquidGlass(isDark, borderRadius = 26.dp, alpha = 0.90f)
                            .padding(horizontal = 6.dp)
                    ) {
                        val contentWidth = maxWidth
                        val itemWidth = contentWidth / mainNavItems.size
                        var dragOffset by remember { mutableStateOf<Float?>(null) }
                        val itemWidthPx = with(androidx.compose.ui.platform.LocalDensity.current) { itemWidth.toPx() }

                        val indicatorOffset by animateDpAsState(
                            targetValue = if (dragOffset != null) with(androidx.compose.ui.platform.LocalDensity.current) { (dragOffset!! - (itemWidthPx / 2)).toDp() } else itemWidth * selectedIndex,
                            animationSpec = spring(dampingRatio = 0.74f, stiffness = Spring.StiffnessMediumLow)
                        )

                        Box(
                            modifier = Modifier
                                .offset(x = indicatorOffset)
                                .width(itemWidth)
                                .fillMaxHeight()
                                .padding(vertical = 8.dp, horizontal = 6.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .background(
                                    Brush.radialGradient(
                                        listOf(
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
                                            Color.Transparent
                                        )
                                    )
                                )
                        )

                        val layoutDirection = androidx.compose.ui.platform.LocalLayoutDirection.current
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .pointerInput(layoutDirection) {
                                    val isRtl = layoutDirection == androidx.compose.ui.unit.LayoutDirection.Rtl
                                    awaitPointerEventScope {
                                        while (true) {
                                            val event = awaitPointerEvent()
                                            val change = event.changes.firstOrNull()
                                            if (change != null) {
                                                if (change.pressed) {
                                                    val rawX = change.position.x
                                                    val directionalX = if (isRtl) size.width.toFloat() - rawX else rawX
                                                    dragOffset = directionalX.coerceIn(0f, size.width.toFloat())
                                                } else {
                                                    if (dragOffset != null) {
                                                        val idx = (dragOffset!! / itemWidthPx).toInt().coerceIn(0, mainNavItems.lastIndex)
                                                        mainNavItems[idx].third()
                                                        dragOffset = null
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                        ) {
                            mainNavItems.forEachIndexed { index, item ->
                                val isActive = isMainActive[index]
                                val tint by animateColorAsState(
                                    targetValue = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    animationSpec = tween(250)
                                )
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .clip(RoundedCornerShape(20.dp)),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = item.second,
                                        contentDescription = item.first,
                                        tint = tint,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = item.first,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = tint
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    // 2. Separate Squircle Menu Button ("القائمة")
                    val menuTint by animateColorAsState(
                        targetValue = if (isMenuButtonActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        animationSpec = tween(250)
                    )
                    Box(
                        modifier = Modifier
                            .width(56.dp)
                            .height(if (isWide) 48.dp else 64.dp)
                            .border(
                                width = 1.dp,
                                brush = Brush.linearGradient(listOf(Color.White.copy(alpha = 0.25f), Color.White.copy(alpha = 0.05f))),
                                shape = CircleShape
                            )
                            .liquidGlass(isDark, borderRadius = 26.dp, alpha = 0.90f)
                            .clip(CircleShape)
                            .bounceClick {
                                isDrawerOpen = !isDrawerOpen
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isMenuButtonActive) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(8.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(
                                        Brush.radialGradient(
                                            listOf(
                                                MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
                                                Color.Transparent
                                            )
                                        )
                                    )
                            )
                        }
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Menu,
                                contentDescription = "القائمة",
                                tint = menuTint,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "القائمة",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = menuTint
                            )
                        }
                    }
                }
            }
        }
        
        // --- Tablet/Wide Floating Side Navigation Capsule ---
        androidx.compose.animation.AnimatedVisibility(
            visible = !showOnboarding && page in mainTabs && isWide,
            enter = slideInHorizontally(animationSpec = spring(dampingRatio = 0.76f, stiffness = 280f)) { -it } + fadeIn(tween(260)),
            exit = slideOutHorizontally(animationSpec = spring(dampingRatio = 0.76f, stiffness = 280f)) { -it } + fadeOut(tween(220)),
            modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight().zIndex(200f).then(
                if (LocalSharedTransitionScope.current != null) {
                    with(LocalSharedTransitionScope.current!!) {
                        Modifier.renderInSharedTransitionScopeOverlay(zIndexInOverlay = 50f)
                    }
                } else Modifier
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .statusBarsPadding()
                    .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
                    .padding(end = 12.dp, top = 24.dp, bottom = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                val mainNavItems = listOf(
                    Triple("المكتبة", Icons.Default.Home) {
                        navigateTo("home")
                        SyncEngine.saveSelectedCategory(null)
                    },
                    Triple("الأخبار", Icons.Default.Newspaper) { navigateTo("news") },
                    Triple("المحفوظات", Icons.Default.Star) { navigateTo("bookmarks") }
                )

                val isMainActive = listOf(page == "home" || page == "list", page == "news", page == "bookmarks")
                var lastValidIndex by remember { mutableStateOf(0) }
                val currentIndex = isMainActive.indexOfFirst { it }.takeIf { it >= 0 }
                if (currentIndex != null) {
                    lastValidIndex = currentIndex
                }
                val selectedIndex = lastValidIndex
                val isMenuButtonActive = isDrawerOpen || page == "about" || page == "shortcuts"

                Column(
                    modifier = Modifier.fillMaxHeight(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    // 1. Vertical 3-tab pill
                    BoxWithConstraints(
                        modifier = Modifier
                            .heightIn(max = 330.dp)
                            .weight(1f, fill = false)
                            .width(72.dp)
                            .border(
                                width = 1.dp,
                                brush = Brush.linearGradient(listOf(Color.White.copy(alpha = 0.25f), Color.White.copy(alpha = 0.05f))),
                                shape = RoundedCornerShape(32.dp)
                            )
                            .liquidGlass(isDark, borderRadius = 32.dp, alpha = 0.90f)
                            .padding(vertical = 10.dp)
                    ) {
                        val contentHeight = maxHeight
                        val itemHeight = contentHeight / mainNavItems.size
                        var dragOffsetY by remember { mutableStateOf<Float?>(null) }
                        val itemHeightPx = with(androidx.compose.ui.platform.LocalDensity.current) { itemHeight.toPx() }

                        val indicatorOffset by animateDpAsState(
                            targetValue = if (dragOffsetY != null) with(androidx.compose.ui.platform.LocalDensity.current) { (dragOffsetY!! - (itemHeightPx / 2)).toDp() } else itemHeight * selectedIndex,
                            animationSpec = spring(dampingRatio = 0.74f, stiffness = Spring.StiffnessMediumLow)
                        )

                        Box(
                            modifier = Modifier
                                .offset(y = indicatorOffset)
                                .height(itemHeight)
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 8.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .background(
                                    Brush.radialGradient(
                                        listOf(
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
                                            Color.Transparent
                                        )
                                    )
                                )
                        )

                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .pointerInput(Unit) {
                                    awaitPointerEventScope {
                                        while (true) {
                                            val event = awaitPointerEvent()
                                            val change = event.changes.firstOrNull()
                                            if (change != null) {
                                                if (change.pressed) {
                                                    val rawY = change.position.y
                                                    dragOffsetY = rawY.coerceIn(0f, size.height.toFloat())
                                                } else {
                                                    if (dragOffsetY != null) {
                                                        val idx = (dragOffsetY!! / itemHeightPx).toInt().coerceIn(0, mainNavItems.lastIndex)
                                                        mainNavItems[idx].third()
                                                        dragOffsetY = null
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                        ) {
                            mainNavItems.forEachIndexed { index, item ->
                                val isActive = isMainActive[index]
                                val tint by animateColorAsState(
                                    targetValue = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    animationSpec = tween(250)
                                )
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(20.dp)),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = item.second,
                                        contentDescription = item.first,
                                        tint = tint,
                                        modifier = Modifier.size(26.dp)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = item.first,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = tint
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // 2. Separate Squircle Menu Button for Tablet ("القائمة")
                    val sideMenuTint by animateColorAsState(
                        targetValue = if (isMenuButtonActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        animationSpec = tween(250)
                    )
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .border(
                                width = 1.dp,
                                brush = Brush.linearGradient(listOf(Color.White.copy(alpha = 0.25f), Color.White.copy(alpha = 0.05f))),
                                shape = RoundedCornerShape(24.dp)
                            )
                            .liquidGlass(isDark, borderRadius = 24.dp, alpha = 0.90f)
                            .clip(RoundedCornerShape(24.dp))
                            .bounceClick {
                                isDrawerOpen = !isDrawerOpen
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isMenuButtonActive) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(8.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(
                                        Brush.radialGradient(
                                            listOf(
                                                MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
                                                Color.Transparent
                                            )
                                        )
                                    )
                            )
                        }
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Menu,
                                contentDescription = "القائمة",
                                tint = sideMenuTint,
                                modifier = Modifier.size(26.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "القائمة",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = sideMenuTint
                            )
                        }
                    }
                }
            }
        }

        // --- Slide Drawer ---
        AnimatedVisibility(
            visible = isDrawerOpen,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.zIndex(500f).then(
                if (LocalSharedTransitionScope.current != null) {
                    with(LocalSharedTransitionScope.current!!) {
                        Modifier.renderInSharedTransitionScopeOverlay(zIndexInOverlay = 100f)
                    }
                } else Modifier
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.4f))
                    .clickable { isDrawerOpen = false }
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(280.dp)
                        .clickable(enabled = false) {}
                        .align(Alignment.TopStart)
                        .clip(RoundedCornerShape(topEnd = 32.dp, bottomEnd = 32.dp))
                        .liquidGlass(isDark, borderRadius = 0.dp, alpha = 0.98f)
                        .animateEnterExit(
                            enter = slideInHorizontally { it },
                            exit = slideOutHorizontally { it }
                        )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .statusBarsPadding()
                            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
                            .padding(top = 24.dp, bottom = 12.dp)
                    ) {
                        // Top Header (now scrollable)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp)
                                .clip(RoundedCornerShape(24.dp))
                                .background(if (isDark) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f))
                                .border(
                                    width = 1.dp,
                                    color = if (isDark) Color.White.copy(alpha = 0.15f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.22f),
                                    shape = RoundedCornerShape(24.dp)
                                )
                                .padding(20.dp)
                        ) {
                            Column {
                                Box(
                                    modifier = Modifier
                                        .size(56.dp)
                                        .background(
                                            Brush.radialGradient(
                                                colors = listOf(
                                                    parseHexColor(appSettings.primaryColor).copy(alpha = 0.8f),
                                                    Color.Transparent
                                                )
                                            ),
                                            shape = CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Image(
                                        painter = painterResource(Res.drawable.app_icon),
                                        contentDescription = null,
                                        modifier = Modifier
                                            .size(44.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                    )
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    "مكتبة الثالث متوسط",
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                    fontFamily = com.Nightjar.Th3GradeLibraryKMP.theme.KufiReemFontFamily
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Middle Content
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp, vertical = 8.dp)
                        ) {
                            DrawerItem("المكتبة", Icons.Default.Home, page == "home" || page == "list", {
                                navigateTo("home")
                                SyncEngine.saveSelectedCategory(null)
                                isDrawerOpen = false
                            })
                            DrawerItem("الأخبار", Icons.Default.Newspaper, page == "news", {
                                navigateTo("news")
                                isDrawerOpen = false
                            })
                            DrawerItem("المحفوظات", Icons.Default.Star, page == "bookmarks", {
                                navigateTo("bookmarks")
                                isDrawerOpen = false
                            })
                            DrawerItem("إضافة اختصار", Icons.Default.AddCard, page == "shortcuts", {
                                navigateTo("shortcuts")
                                isDrawerOpen = false
                            })
                            DrawerItem("حول التطبيق", Icons.Default.Info, page == "about", {
                                navigateTo("about")
                                isDrawerOpen = false
                            })
                        }

                        Spacer(modifier = Modifier.weight(1f, fill = false))

                        // Bottom Content
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp, vertical = 8.dp)
                        ) {
                            DrawerItem("الإعدادات المظهرية", Icons.Default.Palette, isAppSettingsOpen, {
                                isAppSettingsOpen = true
                                isDrawerOpen = false
                            })
                        }
                    }
                }
            }
        }

        // Modals
        com.Nightjar.Th3GradeLibraryKMP.ui.AppSettingsModal(
            isOpen = isAppSettingsOpen,
            onClose = { isAppSettingsOpen = false },
            settings = appSettings,
            onUpdateSettings = { newSettings ->
                SyncEngine.saveSettings(newSettings)
                SyncEngine.platformActionHandler?.rescheduleBackgroundSync()
            }
        )

        com.Nightjar.Th3GradeLibraryKMP.ui.NewsSettingsModal(
            isOpen = isNewsSettingsOpen,
            onClose = { isNewsSettingsOpen = false },
            settings = appSettings,
            onUpdateSettings = { newSettings ->
                SyncEngine.saveSettings(newSettings)
                SyncEngine.platformActionHandler?.rescheduleBackgroundSync()
            },
            onRequestClearCache = {
                isNewsSettingsOpen = false
                showClearCacheDialog = true
            }
        )

        // Clear Cache Dialog Overlay (Material 3 Expressive System Dialog)
        androidx.compose.animation.AnimatedVisibility(
            visible = showClearCacheDialog,
            enter = androidx.compose.animation.fadeIn(),
            exit = androidx.compose.animation.fadeOut(),
            modifier = Modifier.zIndex(600f)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .windowInsetsPadding(WindowInsets.statusBars.add(WindowInsets.captionBar))
                    .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom))
                    .clickable(
                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                        indication = null
                    ) { showClearCacheDialog = false },
                contentAlignment = Alignment.Center
            ) {
                androidx.compose.material3.Surface(
                    shape = RoundedCornerShape(28.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 6.dp,
                    modifier = Modifier
                        .padding(32.dp)
                        .widthIn(max = 400.dp)
                        .clickable(enabled = false) {}
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
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = null,
                            tint = Color(0xFFDC2626), // Material 3 Expressive Vivid Red
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            "تأكيد الحذف النهائي",
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFDC2626),
                            fontSize = 20.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Justify
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "هل أنت متأكد من مسح جميع الأخبار المحفوظة؟ سيتم إفراغ الشاشة بالكامل.",
                            fontSize = 15.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Justify,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(28.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            com.Nightjar.Th3GradeLibraryKMP.theme.MorphingButton(
                                onClick = { showClearCacheDialog = false },
                                modifier = Modifier.weight(1f),
                                restRadius = 24.dp,
                                pressedRadius = 8.dp,
                                containerColor = Color.Transparent,
                                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                            ) {
                                Text(
                                    text = "إلغاء",
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Justify
                                )
                            }
                            com.Nightjar.Th3GradeLibraryKMP.theme.MorphingButton(
                                onClick = {
                                    showClearCacheDialog = false
                                    SyncEngine.clearNewsCache()
                                    platformActionHandler.clearDiskCache()
                                    platformActionHandler.showToast("تم مسح كاش التطبيق والملفات المؤقتة بنجاح ✓")
                                },
                                modifier = Modifier.weight(1f),
                                restRadius = 24.dp,
                                pressedRadius = 8.dp,
                                containerColor = Color(0xFFDC2626),
                                contentColor = Color.White
                            ) {
                                Text(
                                    text = "حذف الآن",
                                    fontWeight = FontWeight.Bold,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Justify
                                )
                            }
                        }
                    }
                }
            }
        }


        // --- Onboarding Screen ---
        androidx.compose.animation.AnimatedVisibility(
            visible = showOnboarding,
            enter = androidx.compose.animation.fadeIn(),
            exit = androidx.compose.animation.slideOutVertically(
                targetOffsetY = { it / 4 },
                animationSpec = spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessLow)
            ) + androidx.compose.animation.scaleOut(
                targetScale = 0.95f,
                animationSpec = spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessLow)
            ) + androidx.compose.animation.fadeOut(animationSpec = androidx.compose.animation.core.tween(500)),
            modifier = Modifier
                .fillMaxSize()
                .zIndex(200f)
                .clickable(
                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                    indication = null
                ) {}
        ) {
            com.Nightjar.Th3GradeLibraryKMP.ui.OnboardingScreen(
                platformActionHandler = platformActionHandler,
                onComplete = {
                    showOnboarding = false
                    SyncEngine.completeOnboarding()
                    // If they skip, ensure we still request notification permission (as requested by user)
                    platformActionHandler.requestNotificationPermission()
                }
            )
        }
        
        // Clear toast immediately on navigation to prevent lingering toasts from previous screens
        LaunchedEffect(page) {
            ToastManager.clearToast()
        }

        var lastNonNullToastMessage by remember { mutableStateOf<String?>(null) }
        if (toastMessage != null) {
            lastNonNullToastMessage = toastMessage
        }

        // Global Toast Message (Floating Dynamic Pill with animateBackOutDown)
        androidx.compose.animation.AnimatedVisibility(
            visible = toastMessage != null,
            enter = (androidx.compose.animation.slideInVertically(
                initialOffsetY = { (it * 0.5f).toInt() },
                animationSpec = spring(dampingRatio = 0.65f, stiffness = 300f)
            ) + androidx.compose.animation.scaleIn(
                initialScale = 0.6f,
                animationSpec = spring(dampingRatio = 0.65f, stiffness = 300f)
            ) + androidx.compose.animation.expandVertically(
                expandFrom = Alignment.Bottom,
                animationSpec = spring(dampingRatio = 0.65f, stiffness = 300f)
            ) + androidx.compose.animation.expandHorizontally(
                expandFrom = Alignment.CenterHorizontally,
                animationSpec = spring(dampingRatio = 0.65f, stiffness = 300f)
            ) + androidx.compose.animation.fadeIn(tween(200))),
            exit = (androidx.compose.animation.slideOutVertically(
                targetOffsetY = { (it * 0.8f).toInt() },
                animationSpec = spring(dampingRatio = 0.8f, stiffness = 400f)
            ) + androidx.compose.animation.scaleOut(
                targetScale = 0.4f,
                animationSpec = spring(dampingRatio = 0.8f, stiffness = 400f)
            ) + androidx.compose.animation.shrinkVertically(
                shrinkTowards = Alignment.Bottom,
                animationSpec = spring(dampingRatio = 0.8f, stiffness = 400f)
            ) + androidx.compose.animation.shrinkHorizontally(
                shrinkTowards = Alignment.CenterHorizontally,
                animationSpec = spring(dampingRatio = 0.8f, stiffness = 400f)
            ) + androidx.compose.animation.fadeOut(tween(150))),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom))
                .padding(bottom = if (page == "pdf") 96.dp else if (page == "web") 24.dp else if (isWide) 24.dp else 100.dp)
                .zIndex(250f)
        ) {
            lastNonNullToastMessage?.let { msg ->
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.inverseSurface,
                    border = androidx.compose.foundation.BorderStroke(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                    ),
                    shadowElevation = 0.dp,
                    modifier = Modifier
                        .wrapContentWidth()
                        .padding(horizontal = 16.dp)
                        .animateContentSize(
                            animationSpec = spring(
                                dampingRatio = 0.82f,
                                stiffness = 320f
                            ),
                            alignment = Alignment.Center
                        )
                        .clickable(
                            interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                            indication = null
                        ) { com.Nightjar.Th3GradeLibraryKMP.ui.ToastManager.clearToast() }
                        .pointerInput(Unit) {
                            detectHorizontalDragGestures(
                                onDragStart = { com.Nightjar.Th3GradeLibraryKMP.ui.ToastManager.clearToast() }
                            ) { change, _ -> change.consume() }
                        }
                ) {
                    androidx.compose.animation.AnimatedContent(
                        targetState = msg,
                        transitionSpec = {
                            (slideInVertically(spring(dampingRatio = 0.7f, stiffness = 250f)) { it / 2 } + fadeIn(tween(160)))
                                .togetherWith(slideOutVertically(tween(140)) { -it / 2 } + fadeOut(tween(140)))
                        },
                        contentAlignment = Alignment.Center,
                        label = "toastTextContent"
                    ) { targetMsg ->
                        Row(
                            modifier = Modifier
                                .padding(horizontal = 24.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.inversePrimary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = targetMsg,
                                color = MaterialTheme.colorScheme.inverseOnSurface,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
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
@Composable
fun RailNavItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    isActive: Boolean,
    onClick: () -> Unit
) {
    val bg = if (isActive) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else Color.Transparent
    val tint = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant

    Column(
        modifier = Modifier
            .size(64.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(bg)
            .clickable { onClick() },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = tint,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            color = tint
        )
    }
}

@Composable
fun BottomNavItem(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isActive: Boolean,
    onClick: () -> Unit
) {
    val tint = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant

    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(
                if (isActive) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                else Color.Transparent
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = tint,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = tint
        )
    }
}

@Composable
fun DrawerItem(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isActive: Boolean,
    onClick: () -> Unit
) {
    val bg = if (isActive) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else Color.Transparent
    val tint = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(bg)
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = label,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = tint
        )
    }
}





