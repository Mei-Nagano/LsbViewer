package sb.linux.client.ui.session

import androidx.lifecycle.viewModelScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import sb.linux.client.domain.account.LoginState
import sb.linux.client.domain.account.SessionSnapshot

/** Clears account-owned caches and mirrors when identity changes or the user logs out. */
internal class SessionAccountLifecycleCoordinator(private val session: Session) {
    var identityVersion by androidx.compose.runtime.mutableLongStateOf(0L)
        private set
    fun applySnapshot(previous: LoginState, snapshot: SessionSnapshot) {
        if (!snapshot.login.loggedIn) {
            session.resetCheckinUi()
        }
        if (previous.loggedIn != snapshot.login.loggedIn || previous.userId != snapshot.login.userId) {
            identityVersion++
            clearAccountFeeds()
            session.topicPageCacheRepository.clearAll()
            session.accountSessionRepository.invalidateCsrf()
            session.checkinSummaryRepository.clear()
            session.resetKeywordFilterUi()
            if (snapshot.login.loggedIn) session.refreshKeywordFilter()
        }
    }

    fun onLoggedOut() {
        identityVersion++
        clearAccountFeeds()
        session.account.resetAfterLogout()
        session.topicPageCacheRepository.clearAll()
        session.accountSessionRepository.invalidateCsrf()
        session.resetKeywordFilterUi()
        session.resetCheckinUi()
        session.checkinSummaryRepository.clear()
        session.viewModelScope.launch { session.accountSessionRepository.logout() }
    }

    private fun clearAccountFeeds() {
        session.homeBrowse.homeTabs.drop(8).forEach { it.reset() }
        session.homeBrowse.followingView = sb.linux.client.domain.community.FollowingView.TOPICS
        if (session.homeBrowse.homeCombo == 0 && session.homeBrowse.homeTabIndex >= 4)
            session.homeBrowse.homeTabIndex = 0
    }
}
