package sb.linux.client.ui.session


import sb.linux.client.domain.account.CheckinInfo

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

internal class SessionCheckinCoordinator(
    private val session: Session,
    private val scope: CoroutineScope,
) {
    fun apply(info: CheckinInfo) = session.applyCheckinSnapshot(info)

    fun load(force: Boolean) {
        if (!force) {
            session.checkinSummaryRepository.today()?.let { summary ->
                apply(
                    CheckinInfo(
                        streak = summary.streak,
                        total = summary.total,
                        canCheckin = false,
                        checkedToday = true,
                    )
                )
                return
            }
        }
        scope.launch {
            try {
                session.checkinRepository.load()
            } catch (_: Exception) {
            }
        }
    }

    fun onCheckinDone() {
        session.markCheckinComplete()
        session.checkinSummaryRepository.markDone()
        load(force = false)
    }
}
