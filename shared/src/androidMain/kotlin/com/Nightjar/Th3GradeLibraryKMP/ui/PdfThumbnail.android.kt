package com.Nightjar.Th3GradeLibraryKMP.ui

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.util.LruCache
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import com.Nightjar.Th3GradeLibraryKMP.data.BookItem
import com.Nightjar.Th3GradeLibraryKMP.data.BookStorageEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// Strictly bounded LRU Cache in memory (max 30 thumbnails ~ 15MB max) with automatic memory eviction
private val thumbnailLruCache = object : LruCache<String, ImageBitmap>(30) {}

@Composable
actual fun rememberPdfCoverThumbnail(item: BookItem): ImageBitmap? {
    val context = LocalContext.current
    var bitmap by remember(item.id) { mutableStateOf<ImageBitmap?>(null) }

    LaunchedEffect(item.id) {
        withContext(Dispatchers.IO) {
            try {
                val file = BookStorageEngine.getPermanentFile(context, item)
                if (file.exists() && file.length() > 1024L) {
                    val cacheKey = "${item.id}_${file.lastModified()}"
                    val cached = synchronized(thumbnailLruCache) { thumbnailLruCache.get(cacheKey) }
                    if (cached != null) {
                        withContext(Dispatchers.Main) {
                            bitmap = cached
                        }
                        return@withContext
                    }

                    ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)?.use { pfd ->
                        PdfRenderer(pfd).use { renderer ->
                            if (renderer.pageCount > 0) {
                                renderer.openPage(0).use { page ->
                                    val width = 360
                                    val height = (360 * page.height / page.width).coerceAtLeast(480)
                                    val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                                    page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                                    val imgBmp = bmp.asImageBitmap()
                                    synchronized(thumbnailLruCache) {
                                        thumbnailLruCache.put(cacheKey, imgBmp)
                                    }
                                    withContext(Dispatchers.Main) {
                                        bitmap = imgBmp
                                    }
                                }
                            }
                        }
                    }
                }
            } catch (_: Exception) {}
        }
    }
    return bitmap
}
