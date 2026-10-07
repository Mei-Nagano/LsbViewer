package sb.linux.client.data

import sb.linux.client.common.network.ForumUserAgent

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import org.jsoup.Jsoup
import org.json.JSONObject
import java.io.File

import sb.linux.client.common.error.LsbException
import sb.linux.client.domain.account.VerificationWebRepository
import sb.linux.client.domain.account.LoginVerification
import sb.linux.client.domain.topic.DanmakuItem

/**
 * linux.sb (bbs1.org) 客户端：OkHttp + 持久化 Cookie + 表单/JSON 提交。
 */
class LsbClient(
    private val context: Context,
    private val verificationWebRepository: VerificationWebRepository,
) {

    init { AppNetwork.init(context) }

    private val prefs = context.getSharedPreferences("lsb_session", Context.MODE_PRIVATE)

    private val cookieJar = SessionCookieJar(prefs)

    val http: OkHttpClient = AppNetwork.clientBuilder()
        .cookieJar(cookieJar)
        .apply { interceptors().removeAll { it is CronetFallbackInterceptor } }
        .followRedirects(true)
        // 显式 keep-alive：登录的 GET/POST 必须复用同一连接（同源 IP），
        // 否则服务端会判定"网络环境变化"拒绝提交
        .connectionPool(AppNetwork.connectionPool)
        .connectTimeout(java.time.Duration.ofSeconds(20))
        .readTimeout(java.time.Duration.ofSeconds(30))
        // 固定真实浏览器请求头：让防护侧看到与正常手机浏览器一致的请求特征
        .addInterceptor { chain ->
            val req = chain.request().newBuilder()
                .header("User-Agent", UA)
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,*/*;q=0.8")
                .header("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8")
                .apply {
                    if (chain.request().tag(ImageTraffic::class.java) == null) header("Cache-Control", "max-age=0")
                }
                .build()
            chain.proceed(req)
        }
        .addInterceptor(CronetFallbackInterceptor(cookieJar))
        .build()

    /** 全局验证页回调：设置后验证页会通过 UI 展示给用户 */
    @Volatile
    var verificationUi: (suspend (String, String) -> Boolean)? = null
    private val verificationMutex = Mutex()
    private val verificationCoordinator by lazy {
        LsbVerificationCoordinator(
            http, cookieJar, verificationMutex, { verificationUi },
            { ChallengePagePolicy.isUamChallenge(it) }, { ChallengePagePolicy.uamNonce(it) },
            { cookie, nonce -> ChallengePagePolicy.uamSum(cookie, nonce) }, ChallengePagePolicy.UAM_CHALLENGE_JS, UA,
            verificationWebRepository,
        )
    }
    private val requestExecutor by lazy {
        ClientRequestExecutor(
            http, UA,
            { code, html -> ChallengePagePolicy.needsVerification(code, html) },
            { html -> ChallengePagePolicy.isChallenge(html) },
            { url, html -> solveUam(url, html) },
            { html -> sniffCsrf(html) },
        )
    }
    private val authentication by lazy {
        LsbAuthenticationClient(
            http, cookieJar, prefs,
            { path -> get(path) }, { path, form -> postForm(path, form) },
            { csrf() }, { invalidateCsrf() },
        )
    }
    private val avatarLoader by lazy {
        ClientImageLoader(
            context,
            http,
            { url, html -> solveUam(url, html) },
            { ChallengePagePolicy.isValidImageBytes(it) },
            { ChallengePagePolicy.isChallenge(it) },
            UA,
        )
    }

    companion object {
        const val UA = ForumUserAgent.DEFAULT

        /** 源站 native-captcha PoW：SHA-256(prefix:nonce) 前缀含 N 个 0（与源站 plugins.js 一致） */
        fun solvePow(prefix: String, zeros: Int): String {
            val target = "0".repeat(zeros.coerceIn(2, 5))
            val md = java.security.MessageDigest.getInstance("SHA-256")
            for (i in 0 until 2_000_000) {
                val n = i.toString(16)
                md.reset()
                val digest = md.digest((prefix + ":" + n).toByteArray()).joinToString("") {
                    (it.toInt() and 0xff).toString(16).padStart(2, '0')
                }
                if (digest.startsWith(target)) return n
            }
            throw LsbException("工作量证明计算超时")
        }

    }

    // ---------------- Peekabo UAM 访问验证 ----------------

    /** 处理源站或浏览器验证，并串行化同一客户端的验证流程。 */
    private suspend fun solveUam(url: String, challengeHtml: String): Boolean =
        verificationCoordinator.solve(url, challengeHtml)

    fun importWebCookies(url: String) = verificationCoordinator.importWebCookies(url)

    fun exportWebCookies(): Boolean = verificationCoordinator.exportWebCookies()
    /** 尝试清空源站浏览足迹；返回是否找到可用的清空接口 */
    suspend fun clearFootprintHistory(): Boolean = withContext(Dispatchers.IO) {
        val csrf = runCatching { csrf() }.getOrNull() ?: return@withContext false
        val paths = listOf(
            "/unread_topic_notice_footprint_clear",
            "/unread_topic_notice_footprint/clear",
            "/clear_unread_topic_notice_footprint",
        )
        for (path in paths) {
            val resp = postForm(path, mapOf("_csrf" to csrf))
            if (resp.code == 200 && !resp.url.contains("form_error")) return@withContext true
        }
        false
    }

    // ---------------- 基础请求 ----------------

    class Resp(val url: String, val html: String, val code: Int)

    suspend fun get(path: String): Resp = requestExecutor.get(path)

    /** 请求首页触发一次 UAM，成功后头像/图片后续即可带上会话 Cookie */
    suspend fun ensureAccess() = requestExecutor.ensureAccess()

    fun peekAvatarBytes(url: String): ByteArray? = avatarLoader.peekAvatarBytes(url)

    suspend fun fetchBytes(url: String): ByteArray = avatarLoader.fetchBytes(url)

    suspend fun postForm(path: String, form: Map<String, String>): Resp = requestExecutor.postForm(path, form)

    suspend fun postFormPairs(path: String, pairs: List<Pair<String, String>>): Resp =
        requestExecutor.postFormPairs(path, pairs)

    suspend fun postAjax(path: String, form: Map<String, String>): JSONObject = requestExecutor.postAjax(path, form)

    suspend fun postMultipart(path: String, fields: Map<String, String>, fileField: String, file: File, mime: String): Resp =
        requestExecutor.postMultipart(path, fields, fileField, file, mime)

    internal suspend fun getAjaxJsonForRepository(path: String): JSONObject = requestExecutor.getAjaxJson(path)

    suspend fun getDonateFeed(topicId: Long): List<DanmakuItem> = requestExecutor.getDonateFeed(topicId)
    // ---------------- CSRF ----------------

    @Volatile
    private var csrfCache: String? = null

    /**
     * 丢弃已缓存的 CSRF 令牌。令牌与会话 Cookie 绑定，登录/登出/会话过期后旧令牌
     * 会被源站拒绝，必须在会话身份变化时调用（否则整个进程生命周期内一直用旧令牌）。
     */
    fun invalidateCsrf() {
        csrfCache = null
    }

    /**
     * 从任意响应 HTML 里顺手抓取 CSRF 令牌（每个 get/post 都会调）。
     * 源站几乎每个渲染页都带 `input[name=_csrf]`（回复框、抽奖参与、点赞…），
     * 抓到就更新缓存，这样 csrf() 基本不必再单独请求首页 —— 首页偶发被访问盾/CF
     * 拦成挑战页时不会再抛「无法获取 CSRF 令牌」把回复整个卡死。
     */
    private fun sniffCsrf(html: String) {
        // 先做廉价字符串判断：帖子页 HTML 很大，没有令牌就不必上正则/Jsoup
        if (html.length < 16 || !html.contains("_csrf")) return
        extractCsrf(html)?.let { csrfCache = it }
    }

    /**
     * 从 HTML 中提取 CSRF 令牌。容忍属性顺序、引号种类与多余空白 ——
     * 原先只认死板的 `name="_csrf" value="…"` 字面量，源站模板一改就整站提交失效。
     */
    private fun extractCsrf(html: String): String? {
        runCatching {
            val d = Jsoup.parse(html)
            d.select("input[name=_csrf]").forEach { el ->
                val v = el.attr("value").trim()
                if (v.isNotEmpty()) return v
            }
            d.selectFirst("meta[name=csrf-token]")?.attr("content")?.trim()
                ?.takeIf { it.isNotEmpty() }?.let { return it }
        }
        // Jsoup 解析失败（响应被截断等）时的正则兜底：name/value 任意顺序、单双引号皆可
        Regex("""name=["']_csrf["'][^>]*?value=["']([^"']+)["']""")
            .find(html)?.groupValues?.get(1)?.takeIf { it.isNotBlank() }?.let { return it }
        Regex("""value=["']([^"']+)["'][^>]*?name=["']_csrf["']""")
            .find(html)?.groupValues?.get(1)?.takeIf { it.isNotBlank() }?.let { return it }
        return null
    }

    /**
     * 取 CSRF 令牌：缓存 →（其他请求已顺手 sniff 到的令牌）→ 首页 → /profile。
     * 首页在被访问盾拦截时不含令牌，此时退到 /profile（登录态必有带令牌的表单）再试一次，
     * 而不是直接失败。
     */
    suspend fun csrf(forceRefresh: Boolean = false): String {
        if (!forceRefresh && csrfCache != null) return csrfCache!!
        for (path in listOf("/", "/profile")) {
            val html = runCatching { get(path).html }.getOrNull() ?: continue
            extractCsrf(html)?.let { csrfCache = it; return it }
        }
        // 兜底：本轮取不到，但之前 sniff 到过就继续用旧令牌（多半仍在同一会话内有效）
        return csrfCache ?: throw LsbException("无法获取 CSRF 令牌")
    }

    // ---------------- 登录 ----------------

    /** 登录、验证码及退出流程交给认证客户端编排。 */
    suspend fun fetchLoginCaptcha(): LoginVerification = authentication.fetchLoginCaptcha()

    suspend fun prepareVerification(verification: LoginVerification): LoginVerification =
        authentication.prepareVerification(verification)

    suspend fun login(
        username: String,
        password: String,
        captcha: LoginVerification,
        answer: String,
        onStatus: (String) -> Unit,
    ): String = authentication.login(username, password, captcha, answer, onStatus)

    suspend fun logout() = authentication.logout()

    fun hasAuthCookie(): Boolean = authentication.hasAuthCookie()
}
