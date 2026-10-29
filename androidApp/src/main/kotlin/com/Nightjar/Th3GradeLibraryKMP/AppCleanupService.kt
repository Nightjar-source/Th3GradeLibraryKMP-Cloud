package com.Nightjar.Th3GradeLibraryKMP

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder

class AppCleanupService : Service() {
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_NOT_STICKY
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        cleanAllTemporaryPdfFiles(this)
        stopSelf()
    }

    companion object {
        fun cleanAllTemporaryPdfFiles(context: Context) {
            try {
                // Clear temporary cache directory and incomplete downloads only
                com.Nightjar.Th3GradeLibraryKMP.ui.clearPdfCache(context.cacheDir)
                context.cacheDir.listFiles()?.forEach { file ->
                    if (file.name.endsWith(".tmp") || (file.name.endsWith(".pdf") && file.name.startsWith("cache_"))) {
                        try { file.delete() } catch (_: Exception) {}
                    }
                }
                // Memory caches eviction
                com.Nightjar.Th3GradeLibraryKMP.ui.PdfBitmapCache.cache.evictAll()
                try {
                    coil3.SingletonImageLoader.get(context).memoryCache?.clear()
                } catch (_: Exception) {}
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
