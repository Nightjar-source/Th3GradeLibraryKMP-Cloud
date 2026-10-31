package com.Nightjar.Th3GradeLibraryKMP.network

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import com.Nightjar.Th3GradeLibraryKMP.data.BookItem
import com.Nightjar.Th3GradeLibraryKMP.data.BookStorageEngine
import com.Nightjar.Th3GradeLibraryKMP.ui.ToastManager
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.ConcurrentHashMap

actual object BookDownloadManager {
    private const val CHANNEL_ID = "book_downloads_channel"
    private const val NOTIFICATION_ID = 4040

    const val ACTION_PAUSE = "com.Nightjar.Th3GradeLibraryKMP.DOWNLOAD_PAUSE"
    const val ACTION_RESUME = "com.Nightjar.Th3GradeLibraryKMP.DOWNLOAD_RESUME"
    const val ACTION_CANCEL = "com.Nightjar.Th3GradeLibraryKMP.DOWNLOAD_CANCEL"
    const val EXTRA_ITEM_ID = "extra_download_item_id"

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val activeJobs = ConcurrentHashMap<String, Job>()
    private val activeItems = ConcurrentHashMap<String, BookItem>()
    private val tempFiles = ConcurrentHashMap<String, File>()

    private val _itemStates = MutableStateFlow<Map<String, ItemDownloadState>>(emptyMap())
    actual val itemStates: StateFlow<Map<String, ItemDownloadState>> = _itemStates.asStateFlow()

    private val _isBatchDownloading = MutableStateFlow(false)
    actual val isBatchDownloading: StateFlow<Boolean> = _isBatchDownloading.asStateFlow()

    private val _batchProgress = MutableStateFlow(0f)
    actual val batchProgress: StateFlow<Float> = _batchProgress.asStateFlow()

    private val _activeNoticeMessage = MutableStateFlow<String?>(null)
    actual val activeNoticeMessage: StateFlow<String?> = _activeNoticeMessage.asStateFlow()

    private var appContext: Context? = null
    private var isReceiverRegistered = false

    private val downloadReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val itemId = intent?.getStringExtra(EXTRA_ITEM_ID) ?: return
            val item = activeItems[itemId] ?: com.Nightjar.Th3GradeLibraryKMP.data.AllItems.getItemById(itemId) ?: return
            when (intent.action) {
                ACTION_PAUSE -> pauseDownload(item)
                ACTION_RESUME -> context?.let { downloadItem(it, item) }
                ACTION_CANCEL -> cancelDownload(item)
            }
        }
    }

    private fun registerReceiverIfNeeded(context: Context) {
        if (!isReceiverRegistered) {
            try {
                val filter = IntentFilter().apply {
                    addAction(ACTION_PAUSE)
                    addAction(ACTION_RESUME)
                    addAction(ACTION_CANCEL)
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    context.applicationContext.registerReceiver(downloadReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
                } else {
                    context.applicationContext.registerReceiver(downloadReceiver, filter)
                }
                isReceiverRegistered = true
            } catch (_: Exception) {}
        }
    }

    private fun isNetworkAvailable(context: Context): Boolean {
        return try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val nw = cm.activeNetwork ?: return false
                val actNw = cm.getNetworkCapabilities(nw) ?: return false
                actNw.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            } else {
                @Suppress("DEPRECATION")
                val ni = cm.activeNetworkInfo ?: return false
                @Suppress("DEPRECATION")
                ni.isConnected
            }
        } catch (_: Exception) {
            true // Fallback to allowing attempt
        }
    }

    private fun ensureNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "تحميل الكتب والملازم",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "إشعارات تقدم وتحكم تحميل الكتب والملازم سحابياً"
                setShowBadge(false)
            }
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(channel)
        }
    }

    private fun createTapPendingIntent(context: Context): PendingIntent {
        val launchIntent = context.packageManager.getLaunchIntentForPackage(context.packageName) ?: Intent()
        launchIntent.action = "th3books.intent.action.OPEN_LIBRARY"
        launchIntent.data = Uri.parse("th3books://library")
        launchIntent.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE else PendingIntent.FLAG_UPDATE_CURRENT
        return PendingIntent.getActivity(context, 100, launchIntent, flags)
    }

    private fun createActionPendingIntent(context: Context, action: String, itemId: String, requestCode: Int): PendingIntent {
        val intent = Intent(action).apply {
            setPackage(context.packageName)
            putExtra(EXTRA_ITEM_ID, itemId)
        }
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE else PendingIntent.FLAG_UPDATE_CURRENT
        return PendingIntent.getBroadcast(context, requestCode, intent, flags)
    }

    private fun updateNotification(
        context: Context,
        title: String,
        text: String,
        progress: Int,
        itemId: String? = null,
        isPaused: Boolean = false,
        isIndeterminate: Boolean = false
    ) {
        try {
            ensureNotificationChannel(context)
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val isDownloading = progress in 0..99 && !isPaused

            val icon = when {
                progress >= 100 -> android.R.drawable.stat_sys_download_done
                isPaused -> android.R.drawable.ic_media_pause
                progress < 0 -> android.R.drawable.stat_notify_error
                else -> android.R.drawable.stat_sys_download
            }

            val builder = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(icon)
                .setContentTitle(title)
                .setContentText(text)
                .setContentIntent(createTapPendingIntent(context))
                .setOngoing(isDownloading)
                .setAutoCancel(true)
                .setOnlyAlertOnce(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setCategory(NotificationCompat.CATEGORY_PROGRESS)
                .setSubText(if (isDownloading && progress >= 0) "$progress%" else null)
                .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)

            if (isDownloading) {
                builder.setProgress(100, progress, isIndeterminate)
            } else {
                builder.setProgress(0, 0, false)
            }

            // Action buttons
            if (itemId != null) {
                if (isDownloading) {
                    val pauseIntent = createActionPendingIntent(context, ACTION_PAUSE, itemId, 201)
                    val cancelIntent = createActionPendingIntent(context, ACTION_CANCEL, itemId, 202)
                    builder.addAction(android.R.drawable.ic_media_pause, "إيقاف مؤقت", pauseIntent)
                    builder.addAction(android.R.drawable.ic_menu_close_clear_cancel, "إلغاء", cancelIntent)
                } else if (isPaused) {
                    val resumeIntent = createActionPendingIntent(context, ACTION_RESUME, itemId, 203)
                    val cancelIntent = createActionPendingIntent(context, ACTION_CANCEL, itemId, 204)
                    builder.addAction(android.R.drawable.ic_media_play, "استئناف", resumeIntent)
                    builder.addAction(android.R.drawable.ic_menu_close_clear_cancel, "إلغاء", cancelIntent)
                }
            }

            nm.notify(NOTIFICATION_ID, builder.build())

            // Auto dismiss notification when 100% complete after 3 seconds
            if (progress >= 100) {
                scope.launch {
                    delay(3000)
                    if (activeJobs.isEmpty()) {
                        nm.cancel(NOTIFICATION_ID)
                    }
                }
            }
        } catch (_: Exception) {}
    }

    actual fun getItemState(itemId: String): ItemDownloadState {
        return _itemStates.value[itemId] ?: ItemDownloadState(status = DownloadStatus.IDLE)
    }

    actual fun downloadItem(context: Any, item: BookItem) {
        val ctx = context as Context
        appContext = ctx.applicationContext
        registerReceiverIfNeeded(ctx)

        // Offline network check
        if (!isNetworkAvailable(ctx)) {
            ToastManager.showToast("تعذر بدء التحميل، يرجى التحقق من اتصالك بالإنترنت والمحاولة مجدداً")
            return
        }

        if (BookStorageEngine.isDownloaded(ctx, item)) {
            updateState(item.id, ItemDownloadState(status = DownloadStatus.COMPLETED, progress = 1f))
            return
        }

        activeJobs[item.id]?.cancel()
        activeItems[item.id] = item

        val job = scope.launch {
            val storageDir = BookStorageEngine.getStorageDir(ctx)
            val tempTarget = File(storageDir, "temp_${item.id}_${System.currentTimeMillis()}.tmp")
            tempFiles[item.id] = tempTarget
            val permanentFile = BookStorageEngine.getPermanentFile(ctx, item)

            try {
                updateState(item.id, ItemDownloadState(status = DownloadStatus.DOWNLOADING, progress = 0f))
                _activeNoticeMessage.value = "جاري التحميل من سحابة الكتب والملازم يرجى الانتظار قليلاً لأول مرة فقط ويكون كل شيء أوفلاين..."

                val candidateUrls = CloudCatalogManager.getStreamCandidateUrls(item)
                if (candidateUrls.isEmpty()) {
                    throw Exception("رابط التحميل غير متوفر")
                }

                var downloadSuccess = false
                var lastErrorMessage = "فشل الاتصال بجميع الخوادم"

                // Triple failover with 2 round-robin retry cycles
                retryLoop@ for (cycle in 1..2) {
                    for (candidateUrl in candidateUrls) {
                        ensureActive()
                        try {
                            updateNotification(
                                ctx,
                                title = "جاري تحميل: ${item.title}",
                                text = "جاري الاتصال بالسحابة فائقة السرعة...",
                                progress = 0,
                                itemId = item.id
                            )

                            var currentUrl = candidateUrl
                            var redirectCount = 0
                            var conn: HttpURLConnection? = null

                            // Follow HTTP redirects cleanly (up to 5)
                            while (redirectCount < 5) {
                                conn = URL(currentUrl).openConnection() as HttpURLConnection
                                conn.connectTimeout = 20000
                                conn.readTimeout = 35000
                                conn.instanceFollowRedirects = true
                                conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Android; Mobile)")

                                val status = conn.responseCode
                                if (status in listOf(HttpURLConnection.HTTP_MOVED_PERM, HttpURLConnection.HTTP_MOVED_TEMP, 307, 308)) {
                                    val newUrl = conn.getHeaderField("Location")
                                    if (!newUrl.isNullOrBlank()) {
                                        currentUrl = newUrl
                                        redirectCount++
                                        conn.disconnect()
                                        continue
                                    }
                                }
                                break
                            }

                            val activeConn = conn ?: throw Exception("تعذر فتح الاتصال")
                            val contentType = activeConn.contentType ?: ""
                            val responseCode = activeConn.responseCode

                            if (responseCode in 200..299 && !contentType.contains("text/html")) {
                                val metaSize = CloudCatalogManager.getMeta(item)?.file_size ?: -1L
                                val totalBytes = if (activeConn.contentLengthLong > 0) activeConn.contentLengthLong else metaSize

                                activeConn.inputStream.use { input ->
                                    FileOutputStream(tempTarget).use { output ->
                                        val buffer = ByteArray(64 * 1024)
                                        var bytesCopied = 0L
                                        var bytesRead: Int
                                        var lastReportTime = System.currentTimeMillis()

                                        while (input.read(buffer).also { bytesRead = it } != -1) {
                                            ensureActive()
                                            output.write(buffer, 0, bytesRead)
                                            bytesCopied += bytesRead

                                            val progress = if (totalBytes > 0) (bytesCopied.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f) else 0f
                                            val now = System.currentTimeMillis()
                                            if (now - lastReportTime > 250) {
                                                lastReportTime = now
                                                updateState(item.id, ItemDownloadState(
                                                    status = DownloadStatus.DOWNLOADING,
                                                    progress = progress,
                                                    bytesDownloaded = bytesCopied,
                                                    totalBytes = totalBytes
                                                ))
                                                updateNotification(
                                                    ctx,
                                                    title = "جاري تحميل: ${item.title} (${(progress * 100).toInt()}%)",
                                                    text = "جاري التحميل بأقصى سرعة...",
                                                    progress = (progress * 100).toInt(),
                                                    itemId = item.id
                                                )
                                            }
                                        }
                                    }
                                }

                                if (tempTarget.exists() && tempTarget.length() > 1024L) {
                                    // Smart clean update: delete old file before atomic rename
                                    if (permanentFile.exists()) {
                                        permanentFile.delete()
                                    }
                                    tempTarget.renameTo(permanentFile)
                                    updateState(item.id, ItemDownloadState(status = DownloadStatus.COMPLETED, progress = 1f))
                                    updateNotification(
                                        ctx,
                                        title = "اكتمل تحميل: ${item.title}",
                                        text = "أصبح الكتاب متاحاً الآن أوفلاين بدون إنترنت",
                                        progress = 100,
                                        itemId = item.id
                                    )
                                    downloadSuccess = true
                                    break@retryLoop
                                } else {
                                    throw Exception("الملف المحمل غير مكتمل")
                                }
                            } else {
                                throw Exception("استجابة غير صالحة من الخادم: $responseCode")
                            }
                        } catch (ce: CancellationException) {
                            throw ce
                        } catch (e: Exception) {
                            lastErrorMessage = e.message ?: "فشل الخادم"
                            // Transient pause before next candidate
                            delay(1000)
                        }
                    }
                }

                if (!downloadSuccess) {
                    throw Exception(lastErrorMessage)
                }

            } catch (ce: CancellationException) {
                updateState(item.id, ItemDownloadState(status = DownloadStatus.PAUSED))
                updateNotification(
                    ctx,
                    title = "تم إيقاف تحميل: ${item.title}",
                    text = "تم إيقاف عملية التحميل مؤقتاً",
                    progress = -1,
                    itemId = item.id,
                    isPaused = true
                )
            } catch (e: Exception) {
                tempTarget.delete()
                updateState(item.id, ItemDownloadState(status = DownloadStatus.FAILED, error = e.message))
                updateNotification(
                    ctx,
                    title = "فشل تحميل: ${item.title}",
                    text = "تعذر التنزيل من السحابة، يرجى التحقق من الاتصال والمحاولة لاحقاً",
                    progress = -1,
                    itemId = item.id
                )
            } finally {
                activeJobs.remove(item.id)
                tempFiles.remove(item.id)
                if (activeJobs.isEmpty() && !_isBatchDownloading.value) {
                    _activeNoticeMessage.value = null
                }
            }
        }
        activeJobs[item.id] = job
    }

    actual fun pauseDownload(item: BookItem) {
        activeJobs[item.id]?.cancel()
        updateState(item.id, ItemDownloadState(status = DownloadStatus.PAUSED))
    }

    actual fun cancelDownload(item: BookItem) {
        activeJobs[item.id]?.cancel()
        tempFiles[item.id]?.delete()
        tempFiles.remove(item.id)
        activeItems.remove(item.id)
        updateState(item.id, ItemDownloadState(status = DownloadStatus.IDLE, progress = 0f))
        appContext?.let { ctx ->
            val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (activeJobs.isEmpty()) {
                nm.cancel(NOTIFICATION_ID)
            }
        }
    }

    actual fun downloadAll(context: Any, items: List<BookItem>) {
        val ctx = context as Context
        appContext = ctx.applicationContext
        registerReceiverIfNeeded(ctx)

        if (!isNetworkAvailable(ctx)) {
            ToastManager.showToast("تعذر بدء التحميل، يرجى التحقق من اتصالك بالإنترنت والمحاولة مجدداً")
            return
        }

        if (_isBatchDownloading.value) return
        _isBatchDownloading.value = true
        _activeNoticeMessage.value = "جاري تحميل الملفات لأول مرة وبعدها ستكون الملفات أوفلاين بدون أي إنترنت"

        scope.launch {
            try {
                val undownloaded = items.filter { !BookStorageEngine.isDownloaded(ctx, it) }
                val totalCount = undownloaded.size
                var completedCount = 0

                for (item in undownloaded) {
                    if (!_isBatchDownloading.value) break
                    downloadItem(ctx, item)
                    activeJobs[item.id]?.join()
                    completedCount++
                    _batchProgress.value = (completedCount.toFloat() / totalCount.toFloat()).coerceIn(0f, 1f)
                }

                updateNotification(
                    ctx,
                    title = "اكتملت جميع التحميلات",
                    text = "جميع الكتب والملازم الآن جاهزة أوفلاين بدون أي إنترنت!",
                    progress = 100
                )
            } finally {
                _isBatchDownloading.value = false
                _activeNoticeMessage.value = null
            }
        }
    }

    actual fun cancelAll() {
        _isBatchDownloading.value = false
        activeJobs.values.forEach { it.cancel() }
        activeJobs.clear()
        tempFiles.values.forEach { it.delete() }
        tempFiles.clear()
        activeItems.clear()
        _activeNoticeMessage.value = null
        appContext?.let { ctx ->
            val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.cancel(NOTIFICATION_ID)
        }
    }

    private fun updateState(itemId: String, state: ItemDownloadState) {
        val current = _itemStates.value.toMutableMap()
        current[itemId] = state
        _itemStates.value = current
    }
}
