package sb.linux.client.data

import android.content.SharedPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import sb.linux.client.common.error.LsbException
import sb.linux.client.data.parser.LoginVerificationParser
import sb.linux.client.domain.account.LoginVerification
import sb.linux.client.domain.account.SessionSnapshot
import sb.linux.client.domain.account.fields

internal class LsbAuthenticationClient(
    private val http: OkHttpClient,
    private val cookieJar: SessionCookieJar,
    private val prefs: SharedPreferences,
    private val get: suspend (String) -> LsbClient.Resp,
    private val postForm: suspend (String, Map<String, String>) -> LsbClient.Resp,
    private val csrf: suspend () -> String,
    private val invalidateCsrf: () -> Unit,
) {    /** 拉取登录页并识别当前验证码协议。登录前先确保通过访问验证。 */
    suspend fun fetchLoginCaptcha(): LoginVerification = withContext(Dispatchers.IO) {
        // 登录页同样被访问盾/CF 保护：先触发一次验证，避免登录页 302 到挑战页
        get("/")
        val page = get("/login")
        val parsed = LoginVerificationParser.parse(page.html, page.url)
            ?: throw LsbException(
                // 已登录时源站把 /login 302 到首页（最终 URL 不再含 /login），无验证码组件可解析：
                // 给出明确提示而非笼统的"解析失败"（访客页也有 .user-name"访客"，不能用作登录态判定）
                if (!page.url.contains("/login")) "已是登录状态，无需重复登录（请先退出登录）"
                else "未找到受支持的人机验证组件"
            )
        val verification = if (parsed is sb.linux.client.domain.account.LoginVerification.Cap) {
            CapVerificationAssets.load(http, parsed)
        } else {
            parsed
        }
        // 登录页偶尔不给 _csrf（例如页面由组件渲染）：提交前回退到会话现取的令牌，
        // 不能因此丢掉整个验证码组件。
        if (verification.csrf.isBlank()) {
            val token = runCatching { csrf() }.getOrDefault("")
            when (verification) {
                is LoginVerification.Pow -> verification.copy(csrf = token)
                is sb.linux.client.domain.account.LoginVerification.Native ->
                    verification.copy(csrf = token)
                is sb.linux.client.domain.account.LoginVerification.Cap ->
                    verification.copy(csrf = token)
            }
        } else {
            verification
        }
    }

    /** 为任意页面中的 CAP 组件准备本地脚本与 WASM；旧版验证无需处理。 */
    suspend fun prepareVerification(verification: LoginVerification): LoginVerification =
        withContext(Dispatchers.IO) {
            if (verification is sb.linux.client.domain.account.LoginVerification.Cap && verification.widgetScript.isBlank()) {
                CapVerificationAssets.load(http, verification)
            } else {
                verification
            }
        }

    /**
     * 登录：旧版验证码由用户填写答案并自动计算 PoW；CAP 使用组件产生的一次性令牌。
     * @param captcha 先前 fetchLoginCaptcha() 获取的验证码信息。
     * @param answer 旧版验证码答案、CAP 令牌或源站 PoW 组件返回的 challenge/proof。
     */
    suspend fun login(
        username: String,
        password: String,
        captcha: LoginVerification,
        answer: String,
        onStatus: (String) -> Unit,
    ): SessionSnapshot = withContext(Dispatchers.IO) {
        val fields = when (captcha) {
            is LoginVerification.Pow -> {
                onStatus("正在提交登录…")
                mapOf("_csrf" to captcha.csrf, "username" to username, "password" to password) + captcha.fields(answer)
            }
            is sb.linux.client.domain.account.LoginVerification.Native -> {
                onStatus("正在计算工作量证明…")
                val startedAt = System.currentTimeMillis()
                val pow = LsbClient.solvePow(captcha.powPrefix, captcha.powZeros)
                val elapsed = System.currentTimeMillis() - startedAt
                if (elapsed < 5200) {
                    onStatus("安全校验中…")
                    delay(5200 - elapsed)
                }
                mapOf(
                    "_csrf" to captcha.csrf,
                    "username" to username,
                    "password" to password,
                    "native_captcha_answer" to answer.trim(),
                    "native_captcha_token" to captcha.token,
                    "native_captcha_pow" to pow,
                    "native_captcha_company" to "",
                )
            }
            is sb.linux.client.domain.account.LoginVerification.Cap -> {
                onStatus("正在提交登录…")
                mapOf(
                    "_csrf" to captcha.csrf,
                    "username" to username,
                    "password" to password,
                    captcha.fieldName to answer.trim(),
                )
            }
        }
        val resp = postForm("/login", fields)
        if (resp.code !in 200..299 || resp.url.contains("form_error") ||
            resp.url.substringBefore('?').trimEnd('/').endsWith("/login")) {
            throw LsbException(HtmlTextParser.extractError(resp.html).ifBlank { "登录被拒绝，验证码可能已失效" })
        }
        // 重定向本身不是认证证据；邮箱登录同样以源站确认的账号为准。
        invalidateCsrf()
        onStatus("正在确认登录状态…")
        val snapshot = sessionSnapshot(resp.html, resp.code)?.takeIf { it.login.loggedIn }
            ?: get("/").let { sessionSnapshot(it.html, it.code) }
        if (snapshot?.login?.loggedIn != true) {
            throw LsbException(if (snapshot != null) "源站仍返回未登录状态，请重新登录"
                else "无法确认登录状态，请稍后刷新首页")
        }
        snapshot
    }

    suspend fun logout() {
        try {
            val csrf = csrf()
            postForm("/logout", mapOf("_csrf" to csrf))
        } catch (_: Exception) {
        }
        invalidateCsrf()
        // 必须同时清内存：Cookie Jar 现以内存为权威存储，只清 prefs 会残留会话
        // Cookie，退出登录后仍是登录态
        cookieJar.clear()
    }

}
