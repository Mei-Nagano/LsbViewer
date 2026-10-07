package sb.linux.client.domain.account

data class VerificationCookie(
    val domain: String,
    val path: String,
    val name: String,
    val value: String,
)

data class UamVerificationWebRequest(
    val url: String,
    val userAgent: String,
    val challengeScript: String,
    val cookies: List<VerificationCookie>,
)

data class UamVerificationWebResult(
    val passed: Boolean,
    val cookies: String?,
)

/** Android WebView and CookieManager boundary used by the network verification coordinator. */
interface VerificationWebRepository {
    suspend fun solveUam(request: UamVerificationWebRequest): UamVerificationWebResult
    fun exportCookies(cookies: List<VerificationCookie>): Boolean
    fun importCookies(url: String): String?
}
