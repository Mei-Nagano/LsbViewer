package sb.linux.client.domain.linkpreview

data class LinkPreviewInfo(
    val url: String,
    val title: String,
    val description: String,
    val iconUrl: String,
)

interface LinkPreviewRepository {
    suspend fun load(url: String): LinkPreviewInfo
    fun clearCache()
}
