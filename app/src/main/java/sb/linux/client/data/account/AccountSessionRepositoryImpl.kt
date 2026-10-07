package sb.linux.client.data.account

import kotlinx.coroutines.CoroutineScope
import sb.linux.client.data.LsbClient
import sb.linux.client.data.SessionRecovery
import sb.linux.client.domain.account.AccountSessionRecovery
import sb.linux.client.domain.account.AccountSessionRepository
import sb.linux.client.domain.account.SessionSnapshot

internal class AccountSessionRepositoryImpl(
    private val scope: CoroutineScope,
    private val client: LsbClient,
) : AccountSessionRepository {
    override fun setVerificationHandler(handler: suspend (url: String, html: String) -> Boolean) {
        client.verificationUi = handler
    }

    override fun invalidateCsrf() = client.invalidateCsrf()

    override suspend fun logout() = client.logout()

    override fun createRecovery(
        applySnapshot: (SessionSnapshot) -> Unit,
        settled: () -> Unit,
    ): AccountSessionRecovery = SessionRecovery(scope, client, applySnapshot, settled)
}
