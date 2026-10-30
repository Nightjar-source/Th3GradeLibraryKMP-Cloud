package com.Nightjar.Th3GradeLibraryKMP.network

import com.Nightjar.Th3GradeLibraryKMP.data.BookItem
import com.Nightjar.Th3GradeLibraryKMP.data.AppDataStore
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class DynamicBookItem(
    val id: String,
    val title: String,
    val cover_res: String = "",
    val cover_url: String = "",
    val pdf_path: String = "",
    val is_note: Boolean = false,
    val author: String = "",
    val color_start: String = "#2563eb",
    val color_end: String = "#1d4ed8",
    val msg_id: Long = 0,
    val file_id: String = "",
    val file_size: Long = 0,
    val stream_url: String = "",
    val telegram_channel_url: String = "",
    val telegram_deep_link: String = ""
) {
    fun toBookItem(): BookItem {
        return BookItem(
            id = id,
            title = title,
            coverResName = cover_res,
            pdfPath = pdf_path.ifBlank { "files/books/$id.pdf" },
            isNote = is_note,
            author = author,
            colorStart = color_start,
            colorEnd = color_end
        )
    }
}

@Serializable
data class CloudBookMeta(
    val msg_id: Long,
    val fallback_filename: String = "",
    val title: String = "",
    val file_id: String? = null,
    val file_size: Long? = null,
    val stream_url: String? = null,
    val telegram_channel_url: String? = null,
    val telegram_deep_link: String? = null,
    val direct_url: String? = null,
    val cover_url: String? = null
)

@Serializable
data class CloudManifest(
    val version: Int = 1,
    val grade: String = "الثالث متوسط",
    val channel: String = "@bookiraqi",
    val channel_id: Long = -1001329888197L,
    val books: Map<String, CloudBookMeta> = emptyMap(),
    val notes: Map<String, CloudBookMeta> = emptyMap(),
    val books_list: List<DynamicBookItem> = emptyList(),
    val notes_list: List<DynamicBookItem> = emptyList()
)

object CloudCatalogManager {
    private val json = Json { 
        ignoreUnknownKeys = true 
        isLenient = true
        encodeDefaults = true
    }
    
    private val httpClient = HttpClient()

    // 100% Free CDNs for real-time third-party catalog updates without new APK releases
    private const val GITHUB_MANIFEST_RAW = 
        "https://raw.githubusercontent.com/Nightjar-source/Th3GradeLibraryKMP-Cloud/main/cloud_manifest.json"
    private const val JSDELIVR_MANIFEST_CDN = 
        "https://cdn.jsdelivr.net/gh/Nightjar-source/Th3GradeLibraryKMP-Cloud@main/cloud_manifest.json"

    private const val CACHE_KEY_MANIFEST = "cached_cloud_manifest_json"

    private val _manifest = MutableStateFlow(loadInitialManifest())
    val manifest = _manifest.asStateFlow()

    private val _dynamicBooks = MutableStateFlow<List<BookItem>>(emptyList())
    val dynamicBooks = _dynamicBooks.asStateFlow()

    private val _dynamicNotes = MutableStateFlow<List<BookItem>>(emptyList())
    val dynamicNotes = _dynamicNotes.asStateFlow()

    init {
        updateDynamicLists(_manifest.value)
    }

    private fun loadInitialManifest(): CloudManifest {
        try {
            val cachedJson = AppDataStore.getString(CACHE_KEY_MANIFEST, "")
            if (cachedJson.isNotBlank()) {
                val parsed = json.decodeFromString<CloudManifest>(cachedJson)
                if (parsed.books_list.isNotEmpty() || parsed.books.isNotEmpty()) {
                    return parsed
                }
            }
        } catch (_: Exception) {}

        return CloudManifest(version = 1)
    }

    private fun updateDynamicLists(m: CloudManifest) {
        if (m.books_list.isNotEmpty()) {
            _dynamicBooks.value = m.books_list.map { it.toBookItem() }
        }
        if (m.notes_list.isNotEmpty()) {
            _dynamicNotes.value = m.notes_list.map { it.toBookItem() }
        }
    }

    suspend fun refreshManifest() {
        withContext(Dispatchers.IO) {
            try {
                val urls = listOf(GITHUB_MANIFEST_RAW, JSDELIVR_MANIFEST_CDN)
                for (url in urls) {
                    try {
                        val response = httpClient.get(url)
                        val text = response.bodyAsText()
                        if (text.isNotBlank() && text.contains("grade")) {
                            val parsed = json.decodeFromString<CloudManifest>(text)
                            if (parsed.books.isNotEmpty() || parsed.books_list.isNotEmpty()) {
                                _manifest.value = parsed
                                updateDynamicLists(parsed)
                                try {
                                    AppDataStore.putString(CACHE_KEY_MANIFEST, text)
                                } catch (_: Exception) {}
                                return@withContext
                            }
                        }
                    } catch (e: Exception) {
                        // Fallback to next CDN URL
                    }
                }
            } catch (e: Exception) {
                // Silently preserve existing cached manifest
            }
        }
    }

    fun getMeta(item: BookItem): CloudBookMeta? {
        val current = _manifest.value
        return if (item.isNote) {
            current.notes[item.id]
        } else {
            current.books[item.id]
        }
    }

    fun getStreamUrl(item: BookItem): String {
        val meta = getMeta(item)
        val stream = meta?.stream_url ?: meta?.direct_url
        if (!stream.isNullOrBlank() && !stream.contains("t.me")) return stream

        // If file <= 20MB and file_id exists, use free Cloudflare worker stream
        val fileId = meta?.file_id
        val fileSize = meta?.file_size ?: 0L
        if (fileSize in 1..(20 * 1024 * 1024) && !fileId.isNullOrBlank()) {
            return "https://th3books-gateway.karrar1iq.workers.dev/file/$fileId"
        }

        // Direct GitHub Release CDN Fallback
        val filename = meta?.fallback_filename
        if (!filename.isNullOrBlank()) {
            return "https://github.com/Nightjar-source/Th3GradeLibraryKMP-Cloud/releases/download/v2.0.0-cloud/$filename"
        }

        return ""
    }

    fun getTelegramChannelLink(item: BookItem): String {
        val meta = getMeta(item)
        val msgId = meta?.msg_id ?: 1L
        return "https://t.me/bookiraqi/$msgId"
    }

    fun getTelegramDeepLink(item: BookItem): String {
        val meta = getMeta(item)
        val msgId = meta?.msg_id ?: 1L
        return "tg://resolve?domain=bookiraqi&post=$msgId"
    }
}
