package sb.linux.client.domain.community

interface ReportRepository {
    suspend fun loadReport(reportId: Long, type: String): ReportForm

    suspend fun submitReport(
        reportId: Long,
        type: String,
        reason: String,
        details: String,
    ): String?
}
