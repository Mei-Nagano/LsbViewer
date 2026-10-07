package sb.linux.client.domain.account

interface AccountSessionRepository {
    fun setVerificationHandler(handler: suspend (url: String, html: String) -> Boolean)
    fun invalidateCsrf()
    suspend fun logout()
    fun createRecovery(
        applySnapshot: (SessionSnapshot) -> Unit,
        settled: () -> Unit,
    ): AccountSessionRecovery
}

interface AccountSessionRecovery {
    val version: Long
    fun refresh()
    fun recover(networkChanged: Boolean = false)
    fun accept(snapshot: SessionSnapshot, expectedVersion: Long)
    fun stop()
}
