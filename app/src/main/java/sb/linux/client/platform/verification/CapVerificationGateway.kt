package sb.linux.client.platform.verification

import sb.linux.client.domain.account.AccountAuthenticationRepository
import sb.linux.client.domain.account.CapVerificationNetworkRepository
import sb.linux.client.domain.account.LoginVerification

/** Keeps CAP preparation and its WebView network bridge behind one platform boundary. */
internal class CapVerificationGateway(
    private val authentication: AccountAuthenticationRepository,
    private val network: CapVerificationNetworkRepository,
) {
    suspend fun prepare(verification: LoginVerification.Cap): LoginVerification.Cap =
        authentication.prepareVerification(verification)

    fun exportCookies(): Boolean = authentication.exportWebCookies()

    fun createBridge(
        verification: LoginVerification.Cap,
        evaluateJavascript: (String) -> Unit,
    ): CapNetworkBridge = CapNetworkBridge(
        network = network,
        endpoint = verification.endpoint,
        pageUrl = verification.pageUrl,
        evaluateJavascript = evaluateJavascript,
    )
}
