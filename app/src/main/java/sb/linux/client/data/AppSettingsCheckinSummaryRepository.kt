package sb.linux.client.data

import sb.linux.client.domain.account.CheckinSummary
import sb.linux.client.domain.account.CheckinSummaryRepository

/** Adapts the existing check-in preference keys without changing their format. */
internal class AppSettingsCheckinSummaryRepository(
    private val settings: AppSettings,
) : CheckinSummaryRepository {
    override fun today(): CheckinSummary? = settings.checkinSummaryToday()?.let { (streak, total) ->
        CheckinSummary(streak, total)
    }

    override fun markDone() = settings.markCheckinDone()

    override fun save(summary: CheckinSummary) =
        settings.saveCheckinSummary(summary.streak, summary.total)

    override fun clear() = settings.clearCheckinCache()
}
