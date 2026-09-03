package sb.linux.client.data

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jsoup.Jsoup

internal data class SessionSnapshot(val login: LoginState, val unread: Int = 0)

/** 无用户卡不等于退出：错误页/验证页/残缺 HTML 都不能作为登出证据。 */
internal fun sessionSnapshot(html: String, code: Int): SessionSnapshot? {
    if (code !in 200..299) return null
    val parsed = HtmlParser.parseMySidebar(html)
    if (parsed.loggedIn) return SessionSnapshot(parsed, HtmlParser.parseNotifyBadgeCount(html))
    val doc = Jsoup.parse(html, Endpoints.BASE)
    val guest = doc.select(".user-card a[href], form[action]").any { el ->
        val target = if (el.tagName() == "form") el.attr("action") else el.attr("href")
        val path = runCatching { java.net.URI(target).path?.trimEnd('/') }.getOrNull()
        path == "/login" && (el.tagName() != "form" || el.selectFirst("input[type=password]") != null)
    }
    return if (guest) SessionSnapshot(LoginState()) else null
}

/** 在拥有者的主线程调用；只协调恢复，不保存密码、不自动提交登录表单。 */
internal class SessionRecovery(
    private val scope: CoroutineScope,
    private val hasCookie: () -> Boolean,
    private val fetch: suspend () -> SessionSnapshot?,
    private val apply: (SessionSnapshot) -> Unit,
    private val settled: () -> Unit = {},
    private val retryDelays: List<Long> = listOf(1000, 3000),
) {
    var version: Long = 0
        private set
    private var job: Job? = null
    private var stopped = false
    private var needsRecovery = true

    fun refresh() {
        version++
        job?.cancel()
        stopped = false
        needsRecovery = true
        start()
    }

    fun recover(networkChanged: Boolean = false) {
        if (stopped) return
        if (networkChanged) needsRecovery = true
        if (needsRecovery && hasCookie() && job?.isActive != true) start()
    }

    fun accept(snapshot: SessionSnapshot, expectedVersion: Long) {
        if (stopped || version != expectedVersion) return
        version++
        job?.cancel()
        needsRecovery = false
        apply(snapshot)
        settled()
    }

    fun stop() {
        stopped = true
        needsRecovery = false
        version++
        job?.cancel()
        settled()
    }

    private fun start() {
        val requestVersion = version
        job = scope.launch(start = CoroutineStart.LAZY) {
            try {
                if (!hasCookie()) {
                    if (version == requestVersion && !stopped) {
                        needsRecovery = false
                        version++
                        apply(SessionSnapshot(LoginState()))
                        settled()
                    }
                    return@launch
                }
                for (attempt in 0..retryDelays.size) {
                    if (attempt > 0) delay(retryDelays[attempt - 1])
                    val snapshot = try { fetch() }
                    catch (e: CancellationException) { throw e }
                    catch (_: Exception) { null }
                    if (requestVersion != version || stopped) return@launch
                    settled()
                    if (snapshot != null) {
                        needsRecovery = false
                        version++
                        apply(snapshot)
                        return@launch
                    }
                    // 网络失败时不改变上次确认的身份；有限重试后等待下一次恢复事件。
                }
            } finally { if (version == requestVersion) settled() }
        }.also { it.start() }
    }
}
