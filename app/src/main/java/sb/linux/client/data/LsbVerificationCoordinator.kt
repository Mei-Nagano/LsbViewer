package sb.linux.client.data

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import sb.linux.client.common.error.LsbException
import sb.linux.client.domain.account.UamVerificationWebRequest
import sb.linux.client.domain.account.VerificationCookie
import sb.linux.client.domain.account.VerificationWebRepository

internal class LsbVerificationCoordinator(
    private val http: OkHttpClient,
    private val cookieJar: SessionCookieJar,
    private val mutex: Mutex,
    private val verificationUi: () -> (suspend (String, String) -> Boolean)?,
    private val isUamChallenge: (String) -> Boolean,
    private val uamNonce: (String) -> Int,
    private val uamSum: (String, Int) -> Long,
    private val challengeScript: String,
    private val userAgent: String,
    private val webRepository: VerificationWebRepository,
) {
    /**
     * 处理验证页。
     * @return true = 已处理，调用方可重试请求；false = 用户取消或等待超时，调用方应
     *   立即放弃而不是再重试 —— 否则外层的 repeat(3) 会把同一个验证对话框接连弹三次。
     */
    suspend fun solve(url: String, challengeHtml: String): Boolean = mutex.withLock {
        // 1) 源站 Peekabo UAM：可应用层直接结算（SHA-256 PoW + 加权和 POST）。
        //    用 WebView 弹倒计时页反而会秒数卡死/一直转圈，这里先静默求解。
        if (isUamChallenge(challengeHtml)) {
            try {
                solveUamManually(url, challengeHtml)
                return@withLock true
            } catch (_: Exception) {
                // 手动求解失败（如缺 fl_ua_p cookie）→ 回退到 WebView/UI
            }
        }
        // 2) Cloudflare Turnstile / 手动求解失败：交给 WebView + 交互对话框。
        //    UI 会先把挑战响应保存的 Cookie 注入 WebView，完成后再统一回传新 Cookie。
        val ui = verificationUi()
        if (ui != null) {
            // 取消与超时都会拿到 false，如实上报由调用方终止重试
            return@withLock runCatching { ui(url, challengeHtml) }.getOrDefault(false)
        }
        // 3) 未配置 UI：隐藏 WebView 兜底。
        solveUamWithWebView(url)
    }

    /** 验证页通过后，把 WebView 中的 Cookie 同步回 OkHttp */
    fun importWebCookies(url: String) {
        val cookies = webRepository.importCookies(url)
        if (!cookies.isNullOrBlank()) cookieJar.saveRawCookies(url, cookies)
    }

    /** 把 OkHttp 已保存的登录会话 Cookie 注入 WebView，应用内浏览器即可保持登录（如私信对话） */
    fun exportWebCookies(): Boolean {
        val cookies = cookieJar.store().values.map { cookie ->
            VerificationCookie(cookie.domain, cookie.path, cookie.name, cookie.value)
        }
        return webRepository.exportCookies(cookies)
    }

    private suspend fun solveUamManually(url: String, challengeHtml: String) {
        var html = challengeHtml
        for (attempt in 0 until 3) {
            val nonce = uamNonce(html)
            val cookie = cookieJar.value("fl_ua_p")
                ?: throw LsbException("验证恢复失败：缺少 userAgent Cookie")
            val sum = uamSum(cookie, nonce)
            val req = Request.Builder().url(url)
                .header("User-Agent", userAgent)
                .header("X-FL-userAgent-Step", "prev")
                .header("Accept", "*/*")
                .post(
                    FormBody.Builder()
                        .add("sum", sum.toString())
                        .add("nonce", nonce.toString())
                        .build()
                )
                .build()
            http.newCall(req).execute().use { r ->
                val text = r.body?.string() ?: ""
                if (!isUamChallenge(text)) return
                html = text
            }
        }
        throw LsbException("访问验证恢复失败，请稍后重试")
    }

    private suspend fun solveUamWithWebView(url: String): Boolean {
        val cookies = cookieJar.store().values.map { cookie ->
            VerificationCookie(cookie.domain, cookie.path, cookie.name, cookie.value)
        }
        val result = webRepository.solveUam(
            UamVerificationWebRequest(url, userAgent, challengeScript, cookies),
        )
        result.cookies?.takeIf(String::isNotBlank)?.let { cookieJar.saveRawCookies(url, it) }
        return result.passed
    }

}
