package com.Nightjar.Th3GradeLibraryKMP.data

import android.content.Context
import java.io.File

actual object BookStorageEngine {
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

    actual fun isDownloaded(context: Any, item: BookItem): Boolean {
        val ctx = context as Context
        val file = getPermanentFile(ctx, item)
        if (file.exists() && file.length() > 1024L) {
            return true
        }
        if (hasBundledAsset(ctx, item)) {
            return true
        }
        return false
    }

    actual fun hasBundledAsset(context: Any, item: BookItem): Boolean {
        return try {
            val ctx = context as Context
            val possiblePaths = listOf(
                "composeResources/com.Nightjar.Th3GradeLibraryKMP.generated.resources/${item.pdfPath}",
                "composeResources/shared.generated.resources/${item.pdfPath}",
                item.pdfPath
            )
            val assetManager = ctx.assets
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

    actual fun deleteBook(context: Any, item: BookItem): Boolean {
        val ctx = context as Context
        val file = getPermanentFile(ctx, item)
        return if (file.exists()) file.delete() else false
    }
}
