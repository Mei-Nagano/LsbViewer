package sb.linux.client.domain.account

data class CheckinSummary(val streak: Int, val total: Int)

/** Stores the current account's daily check-in marker and cached summary. */
interface CheckinSummaryRepository {
    fun today(): CheckinSummary?
    fun markDone()
    fun save(summary: CheckinSummary)
    fun clear()
}
