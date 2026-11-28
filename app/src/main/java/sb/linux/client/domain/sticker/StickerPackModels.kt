package sb.linux.client.domain.sticker

enum class StickerScope(val queryValue: String, val label: String) {
    SYSTEM("system", "站点表情"),
    RECENT("recent", "最近使用"),
    MINE("mine", "我的表情");

    companion object {
        fun fromQuery(value: String?): StickerScope? = entries.firstOrNull { it.queryValue == value }
    }
}

/** 源站 [data-sticker-pack-picker] 按钮携带的表情选择器协议。 */
data class StickerPickerConfig(
    val pickerUrl: String = "",
    val recordUrl: String = "",
    val manageUrl: String = "",
    val forumId: String = "",
    val allowedForums: List<String> = emptyList(),
    val userId: String = "",
    val context: String = "",
    val hideMine: Boolean = false,
    val csrf: String = "",
) {
    val enabled: Boolean get() = pickerUrl.isNotBlank()
    val isDirectMessages: Boolean get() = context == "direct_messages"

    fun availableScopes(): List<StickerScope> = if (hideMine) {
        listOf(StickerScope.SYSTEM, StickerScope.RECENT)
    } else {
        listOf(StickerScope.SYSTEM, StickerScope.RECENT, StickerScope.MINE)
    }
}

/** 源站 [data-sticker-pack-collect] 携带的灯箱收藏表情协议。 */
data class StickerCollectConfig(
    val collectUrl: String = "",
    val csrf: String = "",
) {
    val enabled: Boolean get() = collectUrl.isNotBlank() && csrf.isNotBlank()
}

data class StickerItem(
    val id: String,
    val title: String,
    val url: String,
    val width: Int = 64,
    val markdown: String,
)

data class StickerPage(
    val items: List<StickerItem>,
    val more: Boolean,
)

data class StickerCollectResult(
    val ok: Boolean,
    val message: String,
)

interface StickerPackRepository {
    fun savedScope(userId: String): StickerScope?
    fun saveScope(userId: String, scope: StickerScope)
    suspend fun loadPage(config: StickerPickerConfig, scope: StickerScope, query: String, page: Int): StickerPage
    suspend fun recordUsage(config: StickerPickerConfig, stickerId: String)
    suspend fun collectImage(config: StickerCollectConfig, imageUrl: String, title: String): StickerCollectResult
}
