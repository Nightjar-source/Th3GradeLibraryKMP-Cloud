package com.Nightjar.Th3GradeLibraryKMP.network

import com.Nightjar.Th3GradeLibraryKMP.data.BookItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

actual object BookDownloadManager {
    private val _itemStates = MutableStateFlow<Map<String, ItemDownloadState>>(emptyMap())
    actual val itemStates: StateFlow<Map<String, ItemDownloadState>> = _itemStates.asStateFlow()

    private val _isBatchDownloading = MutableStateFlow(false)
    actual val isBatchDownloading: StateFlow<Boolean> = _isBatchDownloading.asStateFlow()

    private val _batchProgress = MutableStateFlow(0f)
    actual val batchProgress: StateFlow<Float> = _batchProgress.asStateFlow()

    private val _activeNoticeMessage = MutableStateFlow<String?>(null)
    actual val activeNoticeMessage: StateFlow<String?> = _activeNoticeMessage.asStateFlow()

    actual fun getItemState(itemId: String): ItemDownloadState = ItemDownloadState(status = DownloadStatus.COMPLETED)
    actual fun downloadItem(context: Any, item: BookItem) {}
    actual fun pauseDownload(item: BookItem) {}
    actual fun cancelDownload(item: BookItem) {}
    actual fun downloadAll(context: Any, items: List<BookItem>) {}
    actual fun cancelAll() {}
}
