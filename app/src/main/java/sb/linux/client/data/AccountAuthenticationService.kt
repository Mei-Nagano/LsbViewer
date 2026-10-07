package sb.linux.client.data

import sb.linux.client.domain.account.AccountAuthenticationRepository
import sb.linux.client.domain.account.LoginVerification
import sb.linux.client.domain.account.SavedLoginCredentials

/** Coordinates source authentication with the locally selected credential policy. */
internal class AccountAuthenticationService(
    private val client: LsbClient,
    private val settings: AppSettings,
    private val refreshSession: () -> Unit,
) : AccountAuthenticationRepository {
    override fun savedCredentials() = SavedLoginCredentials(
        settings.savedUsername, settings.savedPassword, settings.rememberPassword,
    )

    override suspend fun loadLoginChallenge(): LoginVerification = client.fetchLoginCaptcha()

    override suspend fun prepareVerification(verification: LoginVerification.Cap): LoginVerification.Cap =
        client.prepareVerification(verification) as LoginVerification.Cap

    override fun exportWebCookies(): Boolean = client.exportWebCookies()

    override fun importWebCookies(url: String) = client.importWebCookies(url)

    override suspend fun login(
        username: String,
        password: String,
        challenge: LoginVerification,
        answer: String,
        rememberPassword: Boolean,
        onStatus: (String) -> Unit,
    ): String {
        val result = client.login(username, password, challenge, answer, onStatus)
        settings.rememberPassword = rememberPassword
        settings.savedUsername = if (rememberPassword) username else ""
        settings.savedPassword = if (rememberPassword) password else ""
        refreshSession()
        return result
    }
}
