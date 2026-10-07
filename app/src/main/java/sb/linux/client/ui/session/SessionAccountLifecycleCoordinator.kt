package sb.linux.client.ui.session

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import sb.linux.client.domain.account.LoginState
import sb.linux.client.domain.account.SessionSnapshot

/** Clears account-owned caches and mirrors when identity changes or the user logs out. */
internal class SessionAccountLifecycleCoordinator(private val session: Session) {
    fun applySnapshot(previous: LoginState, snapshot: SessionSnapshot) {
        if (!snapshot.login.loggedIn) {
            session.resetCheckinUi()
        }
        if (previous.loggedIn != snapshot.login.loggedIn || previous.userId != snapshot.login.userId) {
            session.topicPageCacheRepository.clearAll()
            session.accountSessionRepository.invalidateCsrf()
            session.checkinSummaryRepository.clear()
            session.resetKeywordFilterUi()
            if (snapshot.login.loggedIn) session.refreshKeywordFilter()
        }
    }

    fun onLoggedOut() {
        session.account.resetAfterLogout()
        session.topicPageCacheRepository.clearAll()
        session.accountSessionRepository.invalidateCsrf()
        session.resetKeywordFilterUi()
        session.resetCheckinUi()
        session.checkinSummaryRepository.clear()
        session.viewModelScope.launch { session.accountSessionRepository.logout() }
    }
}
