package com.Nightjar.Th3GradeLibraryKMP.network

import com.Nightjar.Th3GradeLibraryKMP.data.AppDataStore
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import com.Nightjar.Th3GradeLibraryKMP.data.NewsItem
import com.Nightjar.Th3GradeLibraryKMP.data.AppSettings
import com.Nightjar.Th3GradeLibraryKMP.data.PlatformActionHandler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.prof18.rssparser.RssParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object SyncEngine {
    private val settings = AppDataStore
    private val json = Json { ignoreUnknownKeys = true }
    private val client = HttpClient()

    var platformActionHandler: PlatformActionHandler? = null

    private val _notificationsEnabled = MutableStateFlow(true)
    val notificationsEnabled = _notificationsEnabled.asStateFlow()

    private val _isOnline = MutableStateFlow(true)
    val isOnline = _isOnline.asStateFlow()

    fun setOnline(online: Boolean) {
        _isOnline.value = online
    }

    fun checkNotificationPermission() {
        _notificationsEnabled.value = platformActionHandler?.areNotificationsEnabled() ?: true
    }

    private val _newsList = MutableStateFlow<List<NewsItem>>(emptyList())
    val newsList = _newsList.asStateFlow()

    private val _appSettings = MutableStateFlow(AppSettings())
    val appSettings = _appSettings.asStateFlow()

    private val _syncing = MutableStateFlow(false)
    val syncing = _syncing.asStateFlow()

    // -1 means no fetch yet, 0 means "no new", >0 means count of new items
    private val _lastFetchedCount = MutableStateFlow(-1)
    val lastFetchedCount = _lastFetchedCount.asStateFlow()

    private val _savedBookmarks = MutableStateFlow<Set<String>>(emptySet())
    val savedBookmarks = _savedBookmarks.asStateFlow()

    private val _lastReadPages = MutableStateFlow<Map<String, Int>>(emptyMap())
    val lastReadPages = _lastReadPages.asStateFlow()

    private val _expandedBookmarks = MutableStateFlow<Set<String>>(emptySet())
    val expandedBookmarks = _expandedBookmarks.asStateFlow()

    private val _isGridView = MutableStateFlow(true)
    val isGridView = _isGridView.asStateFlow()

    private val _selectedCategory = MutableStateFlow<String?>(null)
    val selectedCategory = _selectedCategory.asStateFlow()

    private val _readNewsIds = MutableStateFlow<Set<String>>(emptySet())
    val readNewsIds = _readNewsIds.asStateFlow()

    private val _isDataLoaded = MutableStateFlow(false)
    val isDataLoaded = _isDataLoaded.asStateFlow()

    init {
        loadLocalSettings()
        loadLocalNews()
        loadBookmarks()
        loadLastReadPages()
        loadExpandedBookmarks()
        loadGridView()
        loadSelectedCategory()
        loadReadNews()
        _isDataLoaded.value = true
    }

    fun reloadFromPersistence() {
        loadLocalSettings()
        loadLocalNews()
        loadBookmarks()
        loadLastReadPages()
        loadExpandedBookmarks()
        loadGridView()
        loadReadNews()
        _isDataLoaded.value = true
    }

    private fun loadReadNews() {
        try {
            val readStr = settings.getString("read_news_ids", "")
            if (readStr.isNotEmpty()) {
                if (readStr.trim().startsWith("[")) {
                    _readNewsIds.value = json.decodeFromString<List<String>>(readStr).toSet()
                } else {
                    _readNewsIds.value = readStr.split(",").filter { it.isNotEmpty() }.toSet()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun markNewsAsRead(id: String) {
        if (id.isEmpty()) return
        val current = _readNewsIds.value.toMutableSet()
        if (!current.contains(id)) {
            current.add(id)
            _readNewsIds.value = current
            try {
                settings.putString("read_news_ids", json.encodeToString(current.toList()))
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun loadExpandedBookmarks() {
        try {
            val expandedStr = settings.getString("expanded_bookmarks", "")
            if (expandedStr.isNotEmpty()) {
                _expandedBookmarks.value = expandedStr.split(",").toSet()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun toggleExpandedBookmark(key: String) {
        val current = _expandedBookmarks.value.toMutableSet()
        if (current.contains(key)) {
            current.remove(key)
        } else {
            current.add(key)
        }
        _expandedBookmarks.value = current
        settings.putString("expanded_bookmarks", current.joinToString(","))
    }

    private fun loadGridView() {
        _isGridView.value = settings.getBoolean("is_grid_view", true)
    }

    fun saveGridView(isGrid: Boolean) {
        _isGridView.value = isGrid
        settings.putBoolean("is_grid_view", isGrid)
    }

    private fun loadSelectedCategory() {
        // Always start fresh on the main Library page on fresh app launches
        _selectedCategory.value = null
    }

    fun saveSelectedCategory(cat: String?) {
        _selectedCategory.value = cat
        settings.putString("selected_category", cat ?: "")
    }

    private fun loadLastReadPages() {
        try {
            val lastPagesStr = settings.getString("last_read_pages", "")
            if (lastPagesStr.isNotEmpty()) {
                val map = mutableMapOf<String, Int>()
                lastPagesStr.split(",").forEach {
                    val parts = it.split(":")
                    if (parts.size == 2) {
                        map[parts[0]] = parts[1].toIntOrNull() ?: 0
                    }
                }
                _lastReadPages.value = map
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun saveLastReadPage(pdfId: String, pageIndex: Int) {
        val current = _lastReadPages.value.toMutableMap()
        current[pdfId] = pageIndex
        _lastReadPages.value = current
        val serialized = current.entries.joinToString(",") { "${it.key}:${it.value}" }
        settings.putString("last_read_pages", serialized)
    }

    private fun loadBookmarks() {
        try {
            val bookmarksStr = settings.getString("saved_bookmarks", "")
            if (bookmarksStr.isNotEmpty()) {
                _savedBookmarks.value = bookmarksStr.split(",").toSet()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun toggleBookmark(key: String): Boolean {
        val current = _savedBookmarks.value.toMutableSet()
        val isSaved = if (current.contains(key)) {
            current.remove(key)
            false
        } else {
            current.add(key)
            true
        }
        _savedBookmarks.value = current
        settings.putString("saved_bookmarks", current.joinToString(","))
        return isSaved
    }

    private fun loadLocalSettings() {
        try {
            val theme = settings.getString("theme", "system")
            val useMaterialYou = settings.getBoolean("useMaterialYou", false)
            val primaryColor = settings.getString("primaryColor", "#2563eb")
            val syncInterval = settings.getString("syncInterval", "30m")
            val cacheClearInterval = settings.getString("cacheClearInterval", "6m")
            val notificationSound = settings.getBoolean("notificationSound", false)
            val lastSync = settings.getLong("lastSync", 0L)
            val pureWhiteMode = settings.getBoolean("pureWhiteMode", false)
            val pdfScrollDirection = settings.getString("pdfScrollDirection", "vertical")

            val isOnboardingCompleted = settings.getBoolean("isOnboardingCompleted", false)

            _appSettings.value = AppSettings(
                theme = theme,
                pureBlackMode = settings.getBoolean("pureBlackMode", false),
                pureWhiteMode = pureWhiteMode,
                useMaterialYou = useMaterialYou,
                primaryColor = primaryColor,
                pdfScrollDirection = pdfScrollDirection,
                syncInterval = syncInterval,
                cacheClearInterval = cacheClearInterval,
                notificationSound = notificationSound,
                lastSync = lastSync,
                isOnboardingCompleted = isOnboardingCompleted
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun saveSettings(newSettings: AppSettings) {
        _appSettings.value = newSettings
        settings.putString("theme", newSettings.theme)
        settings.putBoolean("pureBlackMode", newSettings.pureBlackMode)
        settings.putBoolean("pureWhiteMode", newSettings.pureWhiteMode)
        settings.putBoolean("useMaterialYou", newSettings.useMaterialYou)
        settings.putString("primaryColor", newSettings.primaryColor)
        settings.putString("pdfScrollDirection", newSettings.pdfScrollDirection)
        settings.putString("syncInterval", newSettings.syncInterval)
        settings.putString("cacheClearInterval", newSettings.cacheClearInterval)
        settings.putBoolean("notificationSound", newSettings.notificationSound)
        settings.putLong("lastSync", newSettings.lastSync)
        settings.putBoolean("isOnboardingCompleted", newSettings.isOnboardingCompleted)
        platformActionHandler?.rescheduleBackgroundSync()
    }

    fun completeOnboarding() {
        val current = _appSettings.value
        val updated = current.copy(isOnboardingCompleted = true)
        saveSettings(updated)
    }

    private fun loadLocalNews() {
        try {
            val cachedNews = settings.getString("app_news_cache", "")
            if (cachedNews.isNotEmpty()) {
                val list = json.decodeFromString<List<NewsItem>>(cachedNews).map {
                    if (it.formattedDate.isEmpty() && it.pubDate.isNotEmpty()) {
                        it.copy(formattedDate = formatNewsDate(it.pubDate))
                    } else it
                }
                _newsList.value = list
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun extractImageFromContent(content: String): String? {
        val regex = Regex("<img[^>]+src\\s*=\\s*['\"]([^'\"]+)['\"][^>]*>")
        val match = regex.find(content)
        return match?.groups?.get(1)?.value
    }

    private fun saveLocalNews(list: List<NewsItem>) {
        _newsList.value = list
        try {
            settings.putString("app_news_cache", json.encodeToString(list))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun extractDomain(url: String): String {
        return try {
            val withoutProtocol = if (url.contains("://")) {
                url.substringAfter("://")
            } else {
                url
            }
            withoutProtocol.substringBefore("/")
        } catch (e: Exception) {
            ""
        }
    }

    fun clearLastFetchedCount() {
        _lastFetchedCount.value = -1
    }

    fun formatNewsDate(pubDate: String): String = try {
        val dParts = pubDate.split(" ")
        if (dParts.size >= 5) {
            val day = dParts[1]
            val month = when (dParts[2].lowercase()) {
                "jan" -> "1"; "feb" -> "2"; "mar" -> "3"; "apr" -> "4"
                "may" -> "5"; "jun" -> "6"; "jul" -> "7"; "aug" -> "8"
                "sep" -> "9"; "oct" -> "10"; "nov" -> "11"; "dec" -> "12"
                else -> "1"
            }
            val year = dParts[3]
            val timeParts = dParts[4].split(":")
            var hour = timeParts[0].toIntOrNull() ?: 12
            val min = timeParts[1]
            
            // تعديل التوقيت ليكون بتوقيت العراق (+3)
            hour += 3
            if (hour >= 24) hour -= 24
            
            val amPm = if (hour >= 12) "م" else "ص"
            if (hour > 12) hour -= 12
            if (hour == 0) hour = 12
            "$day-$month-$year  $hour:$min $amPm"
        } else pubDate
    } catch (e: Exception) { pubDate }

    suspend fun fetchAndSync(forced: Boolean = false): Int = withContext(Dispatchers.Default) {
        if (_syncing.value) return@withContext 0 // Sync Lock: exit immediately if a sync is already in progress
        val now = ClockSystem.currentTimeMillis()
        val lastSync = _appSettings.value.lastSync
        val timeSinceSync = now - lastSync

        val intervalMs = when (_appSettings.value.syncInterval) {
            "on_open" -> 0L
            "15m"     -> 15 * 60 * 1000L
            "30m"     -> 30 * 60 * 1000L
            "1h"      -> 60 * 60 * 1000L
            "6h"      -> 6 * 60 * 60 * 1000L
            "12h"     -> 12 * 60 * 60 * 1000L
            "24h"     -> 24 * 60 * 60 * 1000L
            else      -> 30 * 60 * 1000L // default 30m
        }

        // 1. We removed the 10-minute barrier in favor of the Dead Man's Switch architecture.
        // The Alarm will be dynamically pushed forward by WorkManager upon success.

        _syncing.value = true
        _lastFetchedCount.value = -1 // Reset fetch result state before starting

        // Refresh dynamic cloud catalog metadata asynchronously from GitHub/CDN
        try {
            CloudCatalogManager.refreshManifest()
        } catch (_: Exception) {}
        try {
            val rssParser = RssParser()
            val originalUrl = "https://feeds.feedburner.com/karraraliraqii/gkhbdxas1m0"
            val proxyList = listOf(
                "https://api.allorigins.win/raw?url=$originalUrl",
                "https://corsproxy.io/?url=$originalUrl",
                "https://api.codetabs.com/v1/proxy/?quest=$originalUrl",
                "https://thingproxy.freeboard.io/fetch/$originalUrl",
                "https://api.cors.lol/?url=$originalUrl",
                "https://yacdn.org/proxy/$originalUrl",
                "https://cors.eu.org/$originalUrl"
            )
            
            suspend fun fetchWithFallbacks(): com.prof18.rssparser.model.RssChannel {
                var lastException: Throwable? = null
                
                // المحاولة الأولى: الرابط الأصلي بمهلة طويلة (60 ثانية)
                try {
                    return kotlinx.coroutines.withTimeout(60000L) {
                        val channel = rssParser.getRssChannel(originalUrl)
                        if (channel.items.isEmpty()) throw Exception("Empty channel returned by original URL")
                        channel
                    }
                } catch (e: kotlinx.coroutines.CancellationException) {
                    if (e !is kotlinx.coroutines.TimeoutCancellationException) throw e
                    lastException = e
                } catch (e: Exception) {
                    lastException = e
                } catch (e: Throwable) {
                    lastException = e
                }

                // محاولة جلب البيانات عبر البروكسيات كحالة طوارئ بمهلة قصيرة (6 ثواني)
                for (url in proxyList) {
                    try {
                        return kotlinx.coroutines.withTimeout(6000L) {
                            val channel = rssParser.getRssChannel(url)
                            if (channel.items.isEmpty()) {
                                throw Exception("Empty channel returned by proxy")
                            }
                            channel
                        }
                    } catch (e: kotlinx.coroutines.CancellationException) {
                        // إذا تم إلغاء الكوروتين بالكامل (مثلاً خروج المستخدم)، ارمِ الخطأ فوراً
                        if (e !is kotlinx.coroutines.TimeoutCancellationException) throw e
                        lastException = e
                    } catch (e: Exception) {
                        // حظر الشبكة، مشاكل SSL، وأخطاء الخوادم مثل:
                        // 404 (Not Found), 500 (Internal Server Error)
                        // 502 (Bad Gateway), 503 (Service Unavailable)
                        // سيتم اصطيادها جميعاً هنا وتخطيها للذهاب إلى البروكسي التالي
                        lastException = e
                    } catch (e: Throwable) {
                        // أي أخطاء أخرى غير متوقعة
                        lastException = e
                    }
                }
                throw Exception("فشلت جميع البروكسيات في جلب البيانات (الأخطاء المحتملة: 404, 500, 502, أو Timeout). آخر خطأ: ${lastException?.message}")
            }
            
            val channel = fetchWithFallbacks()

            val fetchedItems = channel.items.map { item ->
                val pubDate = item.pubDate ?: ""
                NewsItem(
                    id = item.guid ?: item.link ?: "",
                    title = item.title ?: "",
                    link = item.link ?: "",
                    contentSnippet = item.description ?: "",
                    pubDate = pubDate,
                    imageUrl = item.image ?: extractImageFromContent(item.content ?: item.description ?: ""),
                    formattedDate = formatNewsDate(pubDate)
                )
            }
            
            if (fetchedItems.isNotEmpty()) {
                // ── Dual Boundary Algorithm (Top & Bottom Links/Times) ──
                val topLink = settings.getString("Top_Link", "")
                val topTime = settings.getString("Top_Time", "")
                val bottomLink = settings.getString("Bottom_Link", "")
                val bottomTime = settings.getString("Bottom_Time", "")

                val newUniqueItems = mutableListOf<NewsItem>()

                // Detect domain change to treat as a fresh run
                val firstItemLink = fetchedItems.firstOrNull()?.link ?: ""
                val domainChanged = topLink.isNotEmpty() && firstItemLink.isNotEmpty() && 
                        extractDomain(topLink) != extractDomain(firstItemLink)

                val isCacheCleared = _newsList.value.isEmpty() && topLink.isNotEmpty()

                if (topLink.isEmpty() || domainChanged || isCacheCleared) {
                    // First run or domain changed: take latest 15 only
                    val validItems = fetchedItems.take(15)
                    newUniqueItems.addAll(validItems)
                    
                    if (validItems.isNotEmpty()) {
                        settings.putString("Top_Link", validItems.first().link)
                        settings.putString("Top_Time", validItems.first().pubDate)
                        settings.putString("Bottom_Link", validItems.last().link)
                        settings.putString("Bottom_Time", validItems.last().pubDate)
                    }
                } else {
                    val collectedItems = mutableListOf<NewsItem>()
                    for (item in fetchedItems) {
                        // Found exact Top boundary -> Everything below is old
                        if (item.link == topLink && item.pubDate == topTime) break 
                        // Top broken, found Bottom boundary -> Everything below is definitely old
                        if (item.link == bottomLink && item.pubDate == bottomTime) break

                        collectedItems.add(item)
                    }

                    // Filter to find TRULY new or updated items
                    val trulyNewOrUpdated = collectedItems.filter { fetchedItem ->
                        val localItem = _newsList.value.find { it.id == fetchedItem.id }
                        // New if not in local DB. Updated if in local DB but time changed.
                        localItem == null || localItem.pubDate != fetchedItem.pubDate
                    }

                    newUniqueItems.addAll(trulyNewOrUpdated)

                    if (newUniqueItems.isNotEmpty()) {
                        // Dynamic sliding window: Old Top becomes New Bottom, Absolute Newest becomes New Top
                        val absoluteNewest = fetchedItems.first()
                        val isFirstRunOrCleared = topLink.isEmpty() || isCacheCleared || domainChanged
                        val newBottomLink = if (isFirstRunOrCleared) newUniqueItems.last().link else topLink
                        val newBottomTime = if (isFirstRunOrCleared) newUniqueItems.last().pubDate else topTime
                        
                        settings.putString("Bottom_Link", newBottomLink)
                        settings.putString("Bottom_Time", newBottomTime)
                        settings.putString("Top_Link", absoluteNewest.link)
                        settings.putString("Top_Time", absoluteNewest.pubDate)
                    }
                }

                if (newUniqueItems.isNotEmpty()) {

                    // If any of the new/updated items were previously marked as read, mark them as unread again
                    val currentReadIds = _readNewsIds.value.toMutableSet()
                    var readIdsChanged = false
                    newUniqueItems.forEach { newItem ->
                        val oldItem = _newsList.value.find { it.id == newItem.id }
                        if (oldItem != null && oldItem.pubDate != newItem.pubDate) {
                            if (currentReadIds.remove(newItem.id)) {
                                readIdsChanged = true
                            }
                        }
                    }
                    if (readIdsChanged) {
                        _readNewsIds.value = currentReadIds
                        try {
                            settings.putString("read_news_ids", json.encodeToString(currentReadIds.toList()))
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }

                    // Merge new items on top, de-duplicate by id
                    val combined = (newUniqueItems + _newsList.value).distinctBy { it.id }

                    // Apply cache-clear limit (date-based cap)
                    val cacheClear = _appSettings.value.cacheClearInterval
                    val limitDays = when (cacheClear) {
                        "7d"   -> 7
                        "1m"   -> 30
                        "3m"   -> 90
                        "6m"   -> 180
                        "never" -> Int.MAX_VALUE
                        else   -> 180 // default 6 months
                    }
                    // Cap list to limitDays worth of news (approx 3 news/day)
                    val maxItems = if (limitDays == Int.MAX_VALUE) 200 else (limitDays * 3).coerceAtLeast(15)
                    val filteredList = if (combined.size > maxItems) combined.take(maxItems) else combined

                    saveLocalNews(filteredList)
                    _lastFetchedCount.value = newUniqueItems.size
                    if (_appSettings.value.notificationSound && platformActionHandler?.areNotificationsEnabled() == true) {
                        platformActionHandler?.playNotificationSound()
                    }
                    platformActionHandler?.showNewsNotification(newUniqueItems.map { it.title }, newUniqueItems.size)
                    saveSettings(_appSettings.value.copy(lastSync = now))
                    return@withContext newUniqueItems.size
                } else {
                    // Fetch succeeded but no new items were found.
                    // We MUST update lastSync here so the 5-minute skip rule applies, preventing
                    // the backup alarm from immediately repeating the same network request after WorkManager.
                    saveSettings(_appSettings.value.copy(lastSync = now))
                    _lastFetchedCount.value = 0
                    return@withContext 0
                }
            } else {
                // Feeds might be empty initially, still counts as a successful connection check
                saveSettings(_appSettings.value.copy(lastSync = now))
            }
            return@withContext 0
        } catch (e: Throwable) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            e.printStackTrace()
            _lastFetchedCount.value = -2 // signal error
            return@withContext -2
        } finally {
            _syncing.value = false
        }
    }

    fun deleteNewsItem(id: String) {
        // Remove from UI list
        val updated = _newsList.value.filter { it.id != id }
        saveLocalNews(updated)
    }

    fun clearNewsCache() {
        saveLocalNews(emptyList())
        settings.putString("Top_Link", "")
        settings.putString("Top_Time", "")
        settings.putString("Bottom_Link", "")
        settings.putString("Bottom_Time", "")
    }

    fun restoreNewsItem(item: NewsItem, index: Int) {
        val current = _newsList.value.toMutableList()
        if (index in 0..current.size) {
            current.add(index, item)
        } else {
            current.add(item)
        }
        saveLocalNews(current)
    }
}

object ClockSystem {
    fun currentTimeMillis(): Long {
        // Native currentTimeMillis is equivalent across Kotlin/JS (Date.now()) and Kotlin/JVM (System.currentTimeMillis())
        // but we write a safe cross-platform wrapper using system-specific or epoch wrappers.
        // In Kotlin commonMain, we can get current time using standard expect/actual or System class if supported,
        // but Kotlin's Clock or simple platform wrapper is best.
        // Actually, we can use a small actual function for System.currentTimeMillis() or Wasm Date.now()!
        return PlatformClock.currentTimeMillis()
    }
}

expect object PlatformClock {
    fun currentTimeMillis(): Long
}
