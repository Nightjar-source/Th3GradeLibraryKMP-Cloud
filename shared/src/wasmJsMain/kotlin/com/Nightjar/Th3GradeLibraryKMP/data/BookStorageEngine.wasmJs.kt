package com.Nightjar.Th3GradeLibraryKMP.data

actual object BookStorageEngine {
    actual fun isDownloaded(context: Any, item: BookItem): Boolean = true
    actual fun hasBundledAsset(context: Any, item: BookItem): Boolean = false
    actual fun deleteBook(context: Any, item: BookItem): Boolean = false
}
