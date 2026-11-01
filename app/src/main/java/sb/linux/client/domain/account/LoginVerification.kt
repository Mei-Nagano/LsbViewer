package sb.linux.client.domain.account

sealed interface LoginVerification {
    val csrf: String

    data class Native(
        override val csrf: String,
        val question: String,
        val questionHtml: String = "",
        val token: String,
        val powPrefix: String,
        val powZeros: Int,
    ) : LoginVerification

    data class Cap(
        override val csrf: String,
        val pageUrl: String,
        val endpoint: String,
        val scriptUrl: String,
        val wasmUrl: String = "",
        val fieldName: String = "cap-token",
        val widgetScript: String = "",
        val wasmDataUrl: String = "",
        /** 源站用 ES module 加载组件脚本时，内联脚本也必须按模块执行。 */
        val scriptModule: Boolean = false,
    ) : LoginVerification
}

data class SavedLoginCredentials(
    val username: String,
    val password: String,
    val rememberPassword: Boolean,
)

interface AccountAuthenticationRepository {
    fun savedCredentials(): SavedLoginCredentials
    suspend fun loadLoginChallenge(): LoginVerification
    suspend fun prepareVerification(verification: LoginVerification.Cap): LoginVerification.Cap
    fun exportWebCookies(): Boolean
    fun importWebCookies(url: String)
    suspend fun login(
        username: String,
        password: String,
        challenge: LoginVerification,
        answer: String,
        rememberPassword: Boolean,
        onStatus: (String) -> Unit,
    ): String
}
