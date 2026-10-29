package com.Nightjar.Th3GradeLibraryKMP.data

expect object BookStorageEngine {
    fun isDownloaded(context: Any, item: BookItem): Boolean
    fun hasBundledAsset(context: Any, item: BookItem): Boolean
    fun deleteBook(context: Any, item: BookItem): Boolean
}
