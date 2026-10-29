package com.Nightjar.Th3GradeLibraryKMP.network

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import com.Nightjar.Th3GradeLibraryKMP.data.BookItem
import com.Nightjar.Th3GradeLibraryKMP.data.BookStorageEngine
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.ConcurrentHashMap

object BookDownloadManager {
    private const val CHANNEL_ID = "book_downloads_channel"
    private const val NOTIFICATION_ID = 4040

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val activeJobs = ConcurrentHashMap<String, Job>()

    private val _itemStates = MutableStateFlow<Map<String, ItemDownloadState>>(emptyMap())
    val itemStates = _itemStates.asStateFlow()

    private val _isBatchDownloading = MutableStateFlow(false)
    val isBatchDownloading = _isBatchDownloading.asStateFlow()

    private val _batchProgress = MutableStateFlow(0f)
    val batchProgress = _batchProgress.asStateFlow()

    private val _activeNoticeMessage = MutableStateFlow<String?>(null)
    val activeNoticeMessage = _activeNoticeMessage.asStateFlow()

    private fun ensureNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "تحميل الكتب والملازم",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "إشعارات تقدم تحميل الكتب والملازم سحابياً"
                setShowBadge(false)
            }
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(channel)
        }
    }

    private fun updateNotification(context: Context, title: String, text: String, progress: Int, isIndeterminate: Boolean = false) {
        try {
            ensureNotificationChannel(context)
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val builder = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.stat_sys_download)
                .setContentTitle(title)
                .setContentText(text)
                .setOngoing(progress in 0..99)
                .setOnlyAlertOnce(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setProgress(100, progress, isIndeterminate)

            nm.notify(NOTIFICATION_ID, builder.build())
        } catch (_: Exception) {}
    }

    fun dismissNotification(context: Context) {
        try {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.cancel(NOTIFICATION_ID)
        } catch (_: Exception) {}
    }

    fun getItemState(itemId: String): ItemDownloadState {
        return _itemStates.value[itemId] ?: ItemDownloadState(status = DownloadStatus.IDLE)
    }

    fun downloadItem(context: Context, item: BookItem) {
        if (BookStorageEngine.isDownloaded(context, item)) {
            updateState(item.id, ItemDownloadState(status = DownloadStatus.COMPLETED, progress = 1f))
            return
        }

        activeJobs[item.id]?.cancel()

        val job = scope.launch {
            try {
                updateState(item.id, ItemDownloadState(status = DownloadStatus.DOWNLOADING, progress = 0f))
                _activeNoticeMessage.value = "جاري التحميل من سحابة الكتب والملازم يرجى الانتظار قليلاً لأول مرة فقط ويكون كل شيء أوفلاين..."

                val storageDir = BookStorageEngine.getStorageDir(context)
                val tempTarget = File(storageDir, "temp_${item.id}_${System.currentTimeMillis()}.tmp")
                val permanentFile = BookStorageEngine.getPermanentFile(context, item)

                val streamUrl = CloudCatalogManager.getStreamUrl(item)
                if (streamUrl.isBlank() || !streamUrl.startsWith("http")) {
                    throw Exception("رابط التحميل غير متوفر")
                }

                updateNotification(
                    context,
                    title = "جاري تحميل: ${item.title}",
                    text = "جاري التحميل من سحابة الكتب والملازم يرجى الانتظار قليلاً...",
                    progress = 0
                )

                val conn = URL(streamUrl).openConnection() as HttpURLConnection
                conn.connectTimeout = 20000
                conn.readTimeout = 35000
                conn.setRequestProperty("User-Agent", "Mozilla/5.0")
                conn.connect()

                val contentType = conn.contentType ?: ""
                if (conn.responseCode in 200..299 && !contentType.contains("text/html")) {
                    val metaSize = CloudCatalogManager.getMeta(item)?.file_size ?: -1L
                    val totalBytes = if (conn.contentLengthLong > 0) conn.contentLengthLong else metaSize

                    conn.inputStream.use { input ->
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
                                        context,
                                        title = "جاري تحميل: ${item.title} (${(progress * 100).toInt()}%)",
                                        text = "جاري التحميل من سحابة الكتب والملازم...",
                                        progress = (progress * 100).toInt()
                                    )
                                }
                            }
                        }
                    }

                    if (tempTarget.exists() && tempTarget.length() > 1024L) {
                        tempTarget.renameTo(permanentFile)
                        updateState(item.id, ItemDownloadState(status = DownloadStatus.COMPLETED, progress = 1f))
                        updateNotification(
                            context,
                            title = "اكتمل تحميل: ${item.title}",
                            text = "أصبح الكتاب متاحاً الآن أوفلاين بدون إنترنت",
                            progress = 100
                        )
                    } else {
                        throw Exception("فشل اكتمال تنزيل الملف")
                    }
                } else {
                    throw Exception("تعذر البث من السحابة، يرجى المحاولة لاحقاً")
                }
            } catch (ce: CancellationException) {
                updateState(item.id, ItemDownloadState(status = DownloadStatus.PAUSED))
            } catch (e: Exception) {
                e.printStackTrace()
                updateState(item.id, ItemDownloadState(status = DownloadStatus.FAILED, error = e.message))
            } finally {
                activeJobs.remove(item.id)
                if (activeJobs.isEmpty() && !_isBatchDownloading.value) {
                    _activeNoticeMessage.value = null
                }
            }
        }
        activeJobs[item.id] = job
    }

    fun pauseDownload(item: BookItem) {
        activeJobs[item.id]?.cancel()
        updateState(item.id, ItemDownloadState(status = DownloadStatus.PAUSED))
    }

    fun cancelDownload(item: BookItem) {
        activeJobs[item.id]?.cancel()
        updateState(item.id, ItemDownloadState(status = DownloadStatus.IDLE, progress = 0f))
    }

    fun downloadAll(context: Context, items: List<BookItem>) {
        if (_isBatchDownloading.value) return
        _isBatchDownloading.value = true
        _activeNoticeMessage.value = "جاري تحميل الملفات لأول مرة وبعدها ستكون الملفات أوفلاين بدون أي إنترنت"

        scope.launch {
            try {
                val undownloaded = items.filter { !BookStorageEngine.isDownloaded(context, it) }
                val totalCount = undownloaded.size
                var completedCount = 0

                for (item in undownloaded) {
                    if (!_isBatchDownloading.value) break
                    downloadItem(context, item)
                    activeJobs[item.id]?.join()
                    completedCount++
                    _batchProgress.value = (completedCount.toFloat() / totalCount.toFloat()).coerceIn(0f, 1f)
                }

                updateNotification(
                    context,
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

    fun cancelAll() {
        _isBatchDownloading.value = false
        activeJobs.values.forEach { it.cancel() }
        activeJobs.clear()
        _activeNoticeMessage.value = null
    }

    private fun updateState(itemId: String, state: ItemDownloadState) {
        val current = _itemStates.value.toMutableMap()
        current[itemId] = state
        _itemStates.value = current
    }
}
