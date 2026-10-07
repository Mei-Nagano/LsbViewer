package sb.linux.client.ui.session

import android.app.Application
import android.content.Context
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

/** Runs application-scoped preference, cache, session, and network recovery startup work. */
internal class SessionStartupCoordinator(private val session: Session) {
    fun start() {
        session.reloadPrefs()
        session.dataManagementRepository.clearAutoClearItems()
        preferences().edit().remove("notif_read_keys").apply()
        session.accountSessionRepository.setVerificationHandler(session::verifyWithUi)
        session.topicViewsRepository.reload()
        session.refreshSession()
        observeNetworkRecovery()
    }

    private fun observeNetworkRecovery() {
        session.viewModelScope.launch {
            session.networkRecoveryRepository.recoveryEvents.collect {
                session.account.recover(networkChanged = true)
                if (session.account.loginState.loggedIn && session.keywordFilter.pending) {
                    session.refreshKeywordFilter(force = true)
                }
            }
        }
    }

    private fun preferences() = session.getApplication<Application>()
        .getSharedPreferences("lsb_prefs", Context.MODE_PRIVATE)
}
