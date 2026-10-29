package com.Nightjar.Th3GradeLibraryKMP.network

import com.Nightjar.Th3GradeLibraryKMP.data.BookItem
import kotlinx.coroutines.flow.StateFlow

expect object BookDownloadManager {
    val itemStates: StateFlow<Map<String, ItemDownloadState>>
    val isBatchDownloading: StateFlow<Boolean>
    val batchProgress: StateFlow<Float>
    val activeNoticeMessage: StateFlow<String?>

    fun getItemState(itemId: String): ItemDownloadState
    fun downloadItem(context: Any, item: BookItem)
    fun pauseDownload(item: BookItem)
    fun cancelDownload(item: BookItem)
    fun downloadAll(context: Any, items: List<BookItem>)
    fun cancelAll()
}
