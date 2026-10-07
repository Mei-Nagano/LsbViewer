package sb.linux.client.domain.ai

interface AiSummaryHistoryRepository {
    fun save(topicId: Long, summary: String, title: String = "")
    fun cached(topicId: Long): String?
    fun history(): List<AiSummaryRecord>
    fun remove(record: AiSummaryRecord)
    fun setFavorite(record: AiSummaryRecord, favorite: Boolean)
    fun clearCachedSummaries()
    fun clear()
}
