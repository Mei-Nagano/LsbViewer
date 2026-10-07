package sb.linux.client.ui.session

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import sb.linux.client.domain.account.AccountSessionRecovery
import sb.linux.client.domain.account.AccountSessionRepository
import sb.linux.client.domain.account.LoginState
import sb.linux.client.domain.account.SessionSnapshot

/** Owns authenticated identity, unread count, and the source-session recovery lifecycle. */
internal class AccountSession(
    repository: AccountSessionRepository,
    private val onSnapshot: (LoginState, SessionSnapshot) -> Unit,
) {
    var loginState by mutableStateOf(LoginState())
        private set
    var booting by mutableStateOf(true)
        private set
    var notifUnreadCount by mutableIntStateOf(0)
        private set

    private val recovery: AccountSessionRecovery = repository.createRecovery(
        applySnapshot = ::applySnapshot,
        settled = { booting = false },
    )

    val version: Long get() = recovery.version

    fun refresh() = recovery.refresh()

    fun recover(networkChanged: Boolean = false) = recovery.recover(networkChanged)

    fun accept(snapshot: SessionSnapshot, expectedVersion: Long) =
        recovery.accept(snapshot, expectedVersion)

    fun stop() = recovery.stop()

    fun resetAfterLogout() {
        recovery.stop()
        loginState = LoginState()
        notifUnreadCount = 0
    }

    fun updateUnread(count: Int) {
        notifUnreadCount = count.coerceAtLeast(0)
    }

    private fun applySnapshot(snapshot: SessionSnapshot) {
        val previous = loginState
        loginState = snapshot.login
        notifUnreadCount = snapshot.unread
        onSnapshot(previous, snapshot)
    }
}
