package sb.linux.client.data

import android.content.SharedPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import sb.linux.client.common.error.LsbException
import sb.linux.client.data.parser.LoginVerificationParser
import sb.linux.client.domain.account.LoginVerification

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
        // 已登录时源站把 /login 302 到首页（最终 URL 不再含 /login），无验证码组件可解析：
        // 给出明确提示而非笼统的"解析失败"（访客页也有 .user-name"访客"，不能用作登录态判定）
        if (!page.url.contains("/login")) {
            throw LsbException("已是登录状态，无需重复登录（请先退出登录）")
        }
        val verification = LoginVerificationParser.parse(page.html, page.url)
            ?: throw LsbException("未找到受支持的人机验证组件")
        if (verification is sb.linux.client.domain.account.LoginVerification.Cap) {
            CapVerificationAssets.load(http, verification)
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
     * @param answer 旧版验证码答案或 CAP 令牌。
     */
    suspend fun login(
        username: String,
        password: String,
        captcha: LoginVerification,
        answer: String,
        onStatus: (String) -> Unit,
    ): String = withContext(Dispatchers.IO) {
        val fields = when (captcha) {
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
        if (resp.url.contains("form_error") || resp.url.substringBefore('?').endsWith("/login")) {
            throw LsbException(HtmlTextParser.extractError(resp.html).ifBlank { "登录被拒绝，验证码可能已失效" })
        }
        // 成功判定：实测源站登录成功 302 → 首页，失败（密码错/验证码错）→ /form_error，非 form_error 即成功。
        // 此前用 contains(username) 兜底判定：邮箱登录时页面只显示用户名而非邮箱，导致实际登录
        // 成功却误报"用户名或密码错误"（cookie 已写入，再次刷新验证码还会因已登录 302 首页而静默失败）
        // 登录成功后会话 Cookie 已换新，旧 CSRF 令牌随之失效，必须丢弃缓存——否则
        // 登录前取过令牌的话（搜索/签到等路径都会取），之后发帖回帖会一直拿游客期
        // 的旧令牌去提交并被源站拒绝，且整个进程生命周期内都无法恢复
        invalidateCsrf()
        "登录成功"
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

    /**
     * 登录态判定：优先查 Cookie Jar 内存视图（会话 Cookie 形式的登录也能识别），
     * 再回落到持久化记录的前缀匹配。
     */
    fun hasAuthCookie(): Boolean {
        val set = prefs.getStringSet("cookies", emptySet()) ?: emptySet()
        return cookieJar.value("bbs_auth") != null || set.any { it.startsWith("bbs_auth\u0001") }
    }}
