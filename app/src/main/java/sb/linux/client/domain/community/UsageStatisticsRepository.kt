package sb.linux.client.domain.community

data class UsageEvent(
    val type: String,
    val topicId: Long,
    val title: String,
    val value: Int,
    val at: Long,
)

data class SourceUsageTotals(
    val topics: Int,
    val replies: Int,
    val favorites: Int,
    val collections: Int,
)

/** Local activity and the account-scoped statistics synchronized from the forum. */
interface UsageStatisticsRepository {
    fun events(): List<UsageEvent>
    fun recordEvent(type: String, topicId: Long = 0, title: String = "", value: Int = 0)
    fun sourceTotals(userId: Long): SourceUsageTotals?
    fun points(): List<PointRow>
    fun aiSummary(): String
    fun saveAiSummary(value: String)
    fun clear()
    suspend fun sync()
}
