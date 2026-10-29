package com.Nightjar.Th3GradeLibraryKMP.data

import android.content.Context
import java.io.File

object BookStorageEngine {
    private const val STORAGE_DIR_NAME = "books_storage"

    fun getStorageDir(context: Context): File {
        val dir = File(context.filesDir, STORAGE_DIR_NAME)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    fun getPermanentFile(context: Context, item: BookItem): File {
        return File(getStorageDir(context), "pdf_${item.id}.pdf")
    }

    /**
     * Checks if the book/note is downloaded in permanent user storage,
     * or if it exists directly as a bundled asset inside the APK.
     */
    fun isDownloaded(context: Context, item: BookItem): Boolean {
        // 1. Check permanent user storage in filesDir
        val file = getPermanentFile(context, item)
        if (file.exists() && file.length() > 1024L) {
            return true
        }

        // 2. Check bundled asset (for backward compatibility if developer bundles offline assets)
        if (hasBundledAsset(context, item)) {
            return true
        }

        return false
    }

    /**
     * Checks if the asset is bundled directly into the APK assets.
     */
    fun hasBundledAsset(context: Context, item: BookItem): Boolean {
        return try {
            val possiblePaths = listOf(
                "composeResources/com.Nightjar.Th3GradeLibraryKMP.generated.resources/${item.pdfPath}",
                "composeResources/shared.generated.resources/${item.pdfPath}",
                item.pdfPath
            )
            val assetManager = context.assets
            for (path in possiblePaths) {
                try {
                    assetManager.open(path).use { return true }
                } catch (_: Exception) {}
            }
            false
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Deletes a downloaded book from permanent storage.
     */
    fun deleteBook(context: Context, item: BookItem): Boolean {
        val file = getPermanentFile(context, item)
        return if (file.exists()) file.delete() else false
    }

    /**
     * Returns total bytes stored in books_storage.
     */
    fun getTotalStorageUsed(context: Context): Long {
        val dir = getStorageDir(context)
        return dir.listFiles()?.filter { it.extension == "pdf" }?.sumOf { it.length() } ?: 0L
    }
}
