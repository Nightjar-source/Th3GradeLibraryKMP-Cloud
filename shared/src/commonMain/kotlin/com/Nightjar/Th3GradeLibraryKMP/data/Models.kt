package com.Nightjar.Th3GradeLibraryKMP.data

import kotlinx.serialization.Serializable

@Serializable
data class BookItem(
    val id: String,
    val title: String,
    val coverResName: String,
    val pdfPath: String, // e.g. "files/books/ajtmaa.pdf"
    val isNote: Boolean,
    val author: String,
    val colorStart: String,
    val colorEnd: String
)

@Serializable
data class NewsItem(
    val id: String,
    val title: String,
    val link: String,
    val contentSnippet: String?,
    val pubDate: String,
    val imageUrl: String?,
    val formattedDate: String = ""
)

@Serializable
data class AppSettings(
    val theme: String = "system", // system, light, dark
    val pureBlackMode: Boolean = false,
    val pureWhiteMode: Boolean = false,
    val useMaterialYou: Boolean = false,
    val primaryColor: String = "#2563eb",
    val pdfScrollDirection: String = "vertical", // vertical, horizontal
    val syncInterval: String = "30m",   // on_open, 30m, 1h, 12h, 24h
    val cacheClearInterval: String = "6m", // 7d, 1m, 3m, 6m, never
    val notificationSound: Boolean = false,
    val lastSync: Long = 0,
    val isOnboardingCompleted: Boolean = false,
    val autoDownloadOnUpdate: Boolean = false
)

object AllItems {
    fun getItemById(id: String): BookItem? {
        val dynamicList = com.Nightjar.Th3GradeLibraryKMP.network.CloudCatalogManager.dynamicBooks.value + com.Nightjar.Th3GradeLibraryKMP.network.CloudCatalogManager.dynamicNotes.value
        return dynamicList.firstOrNull { it.id == id } ?: (books + notes).firstOrNull { it.id == id }
    }

    val dynamicBooks: List<BookItem>
        get() {
            val list = com.Nightjar.Th3GradeLibraryKMP.network.CloudCatalogManager.dynamicBooks.value
            return if (list.isNotEmpty()) list else staticBooks
        }

    val dynamicNotes: List<BookItem>
        get() {
            val list = com.Nightjar.Th3GradeLibraryKMP.network.CloudCatalogManager.dynamicNotes.value
            return if (list.isNotEmpty()) list else staticNotes
        }

    val books: List<BookItem>
        get() = dynamicBooks

    val notes: List<BookItem>
        get() = dynamicNotes

    val staticBooks = listOf(
        BookItem("b1", "كتاب الاجتماعيات", "ajh", "files/books/ajtmaa.pdf", false, "المنهج الوزاري المعتمد 2027", "#f59e0b", "#d97706"),
        BookItem("b2", "كتاب الاحياء", "ahya", "files/books/ahya.pdf", false, "المنهج الوزاري المعتمد 2027", "#10b981", "#059669"),
        BookItem("b3", "كتاب الانكليزي (النشاط)", "englishactivity", "files/books/englishactivity.pdf", false, "المنهج الوزاري المعتمد 2027", "#3b82f6", "#1d4ed8"),
        BookItem("b4", "كتاب الانكليزي (الطالب)", "english", "files/books/english.pdf", false, "المنهج الوزاري المعتمد 2027", "#6366f1", "#4338ca"),
        BookItem("b5", "كتاب الإسلامية", "aslm", "files/books/isalmic.pdf", false, "المنهج الوزاري المعتمد 2027", "#059669", "#047857"),
        BookItem("b6", "كتاب الرياضيات", "math", "files/books/reata.pdf", false, "المنهج الوزاري المعتمد 2027", "#2563eb", "#1d4ed8"),
        BookItem("b7", "كتاب الفيزياء", "fizya", "files/books/fuzyaaa.pdf", false, "المنهج الوزاري المعتمد 2027", "#8b5cf6", "#6d28d9"),
        BookItem("b8", "كتاب الكيمياء", "kimai", "files/books/kimya.pdf", false, "المنهج الوزاري المعتمد 2027", "#06b6d4", "#0891b2"),
        BookItem("b9", "كتاب اللغة العربية جزء1", "arbi", "files/books/rabi1.pdf", false, "المنهج الوزاري المعتمد 2027", "#f43f5e", "#e11d48"),
        BookItem("b10", "كتاب اللغة العربية2", "arabi2", "files/books/2arab.pdf", false, "المنهج الوزاري المعتمد 2027", "#ec4899", "#be185d")
    )

    val staticNotes = listOf(
        BookItem("n1", "املاء الانكليزي", "aaaaaaaaen", "files/notes/enalaaaa.pdf", true, "مدرس المادة", "#6366f1", "#3b82f6"),
        BookItem("n2", "ملزمة اجتماعيات - عباس العامري", "aj", "files/notes/ajtma.pdf", true, "أ. عباس العامري", "#f59e0b", "#d97706"),
        BookItem("n3", "ملزمة أحياء قديمة - احمد الجاف", "ahyaaa", "files/notes/ah.pdf", true, "أ. احمد الجاف", "#10b981", "#059669"),
        BookItem("n4", "ملزمة اجتماعيات - إيمان الصالحي", "ajjj", "files/notes/ajtm.pdf", true, "أ. إيمان الصالحي", "#f59e0b", "#d97706"),
        BookItem("n5", "ملزمة عربي - رفل الزبيدي", "arabrrrr", "files/notes/arabiraval.pdf", true, "أ. رفل الزبيدي", "#f43f5e", "#e11d48"),
        BookItem("n6", "ملزمة عربي - هشام المعموري", "arrrr", "files/notes/arabihish.pdf", true, "أ. هشام المعموري", "#ec4899", "#be185d"),
        BookItem("n7", "ملزمة اسلامية - ساجد العكيلي", "am", "files/notes/asm.pdf", true, "أ. ساجد العكيلي", "#059669", "#047857"),
        BookItem("n8", "النموذجية بالاسلامية - على الزاملي", "eem", "files/notes/aiiiaslaim.pdf", true, "أ. علي الزاملي", "#10b981", "#059669"),
        BookItem("n9", "ملزمة تعاريف واسقاطات الانكليزي", "emns", "files/notes/ennnnnnnnn.pdf", true, "مدرس المادة", "#3b82f6", "#1d4ed8"),
        BookItem("n10", "ملزمة انكليزي - علي الجبوري", "enk", "files/notes/end.pdf", true, "أ. علي الجبوري", "#6366f1", "#4338ca"),
        BookItem("n11", "وزاريات قواعد الانكليزي", "enn", "files/notes/ennnnnnndkdll.pdf", true, "مدرس المادة", "#3b82f6", "#1d4ed8"),
        BookItem("n12", "ملزمة قطع الانكليزي - علي الجبوري", "nnns", "files/notes/ettttttt.pdf", true, "أ. علي الجبوري", "#3b82f6", "#1d4ed8"),
        BookItem("n13", "ملزمة فيزياء - علي الذهبي", "fffk", "files/notes/fizyaaaa.pdf", true, "أ. علي الذهبي", "#8b5cf6", "#6d28d9"),
        BookItem("n14", "ملزمة فيزياء - دانيار الجاف", "fhhh", "files/notes/ffffffffff.pdf", true, "أ. دانيار الجاف", "#8b5cf6", "#6d28d9"),
        BookItem("n15", "ملزمة فيزياء - علي السوداني", "fiiii", "files/notes/ffflflfkfkfkdkkd.pdf", true, "أ. علي السوداني", "#8b5cf6", "#6d28d9"),
        BookItem("n16", "ملزمة العربي جزء 1 - قاسم مرتضى", "kss1", "files/notes/marab1.pdf", true, "أ. قاسم مرتضى", "#f43f5e", "#e11d48"),
        BookItem("n17", "ملزمة العربي جزء 2 - قاسم مرتضى", "kss", "files/notes/rivvvvvv2.pdf", true, "أ. قاسم مرتضى", "#ec4899", "#be185d"),
        BookItem("n18", "الذهبية في الرياضيات", "m", "files/notes/mathlkdkdkdkdkd.pdf", true, "مدرس المادة", "#2563eb", "#1d4ed8"),
        BookItem("n19", "ملزمة رياضيات 1 - علي صادق", "mam", "files/notes/mathsa.pdf", true, "أ. علي صادق", "#2563eb", "#1d4ed8"),
        BookItem("n20", "ملزمة رياضيات 2 - علي صادق", "mam2", "files/notes/mam2.pdf", true, "أ. علي صادق", "#2563eb", "#1d4ed8"),
        BookItem("n21", "ملزمة قصص الانكليزي", "wnn", "files/notes/ennndndndnndnd.pdf", true, "مدرس المادة", "#3b82f6", "#1d4ed8"),
        BookItem("n22", "ملزمة الاسلامية 2027 سندس حارس", "sammam", "files/notes/miss2023.pdf", true, "أ. سندس حارس", "#059669", "#047857"),
        BookItem("n23", "ملزمة معاكسات الانكليزي", "ksk", "files/notes/enwwwwww.pdf", true, "مدرس المادة", "#3b82f6", "#1d4ed8"),
        BookItem("n24", "ملزمة كيمياء دانيار الجاف", "kaa", "files/notes/kkkkksksksksksks.pdf", true, "أ. دانيار الجاف", "#06b6d4", "#0891b2"),
        BookItem("n25", "ملزمة الكيمياء - حسين حمزة", "kki", "files/notes/kkkkkkkkkk.pdf", true, "أ. حسين حمزة", "#06b6d4", "#0891b2"),
        BookItem("n26", "ملزمة كيمياء - هاشم الغرباوي", "kkak", "files/notes/kiiii.pdf", true, "أ. هاشم الغرباوي", "#06b6d4", "#0891b2"),
        BookItem("n27", "ملزمة كيمياء - مهند السوداني", "kio", "files/notes/kimmamama.pdf", true, "أ. مهند السوداني", "#06b6d4", "#0891b2"),
        BookItem("n28", "ملزمة رياضيات ثالث متوسط1-- رشيد عبدالله", "marrahiied", "files/notes/marrahiied.pdf", true, "أ. رشيد عبدالله", "#2563eb", "#1d4ed8"),
        BookItem("n29", "ملزمة رياضيات ثالث متوسط2- رشيد عبدالله", "marrahiied2", "files/notes/marrahiied2.pdf", true, "أ. رشيد عبدالله", "#2563eb", "#1d4ed8"),
        BookItem("n30", "ملزمة الاحياء جعفر محمد 2027", "ayajaaa", "files/notes/ayajaaa.pdf", true, "أ. جعفر محمد 2027", "#10b981", "#059669"),
        BookItem("n31", "ملزمة الرياضيات 1--- خالد وليد 2027", "mathkhal", "files/notes/mathkhal.pdf", true, "أ. خالد وليد 2027", "#2563eb", "#1d4ed8"),
        BookItem("n32", "ملزمة الرياضيات 2--- خالد وليد 2027", "mathkhal2", "files/notes/mathkhal2.pdf", true, "أ. خالد وليد 2027", "#2563eb", "#1d4ed8")
    )
}
