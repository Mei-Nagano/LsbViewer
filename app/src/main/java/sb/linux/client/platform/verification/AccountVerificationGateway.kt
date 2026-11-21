package sb.linux.client.platform.verification

import sb.linux.client.domain.account.AccountAuthenticationRepository
import sb.linux.client.domain.account.CapVerificationNetworkRepository
import sb.linux.client.domain.account.LoginVerification

/** 共用登录、回复和账号表单的验证网络与会话。 */
internal class AccountVerificationGateway(
    private val authentication: AccountAuthenticationRepository,
    private val network: CapVerificationNetworkRepository,
    private val resources: sb.linux.client.domain.topic.SourceTopicResourceRepository,
) {
    suspend fun prepare(verification: LoginVerification.Cap): LoginVerification.Cap =
        authentication.prepareVerification(verification)

    fun exportCookies(): Boolean = authentication.exportWebCookies()

    suspend fun recoverVerification(challenge: sb.linux.client.domain.topic.SourceResourceVerificationRequired): Boolean =
        resources.recoverVerification(challenge)

    fun loadPowResource(request: android.webkit.WebResourceRequest): android.webkit.WebResourceResponse? {
        val url = request.url
        // 未接管的脚本、跨域验证依赖和 POST 保留 WebView 的正常请求语义。
        // 它们在页面加载前已应用 DoH/代理设置，不应伪造 403。
        val shared = !request.isForMainFrame && request.method == "GET" && url.scheme == "https" &&
            sb.linux.client.common.network.Endpoints.isForumHost(url.host) &&
            (url.path?.startsWith("/app/") == true || url.path == "/human_verification_refresh")
        if (!shared) return null
        val response = resources.load(url.toString(), request.requestHeaders + ("Cache-Control" to "no-cache"))
        return android.webkit.WebResourceResponse(response.mimeType, response.encoding, response.status,
            response.reason, response.headers, response.body)
    }

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
