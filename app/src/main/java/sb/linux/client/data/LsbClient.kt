package sb.linux.client.data

import android.content.Context
import android.util.Base64
import android.webkit.CookieManager
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.withContext
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.FormBody
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import org.jsoup.Jsoup
import org.json.JSONObject
import java.io.File
import java.security.MessageDigest

class LsbException(message: String) : Exception(message)

/**
 * linux.sb (bbs1.org) 客户端：OkHttp + 持久化 Cookie + 表单/JSON 提交。
 */
class LsbClient(private val context: Context) {

    private val prefs = context.getSharedPreferences("lsb_session", Context.MODE_PRIVATE)

    private val cookieJar = object : CookieJar {
        fun store(): MutableMap<String, Cookie> {
            val map = mutableMapOf<String, Cookie>()
            val set = prefs.getStringSet("cookies", emptySet()) ?: emptySet()
            for (s in set) {
                val p = s.split("\u0001")
                if (p.size == 5 && p[4].toLong() > System.currentTimeMillis()) {
                    map[p[0]] = Cookie.Builder()
                        .name(p[0]).value(p[1])
                        .domain(p[2]).path(p[3])
                        .expiresAt(p[4].toLong())
                        .build()
                }
            }
            return map
        }

        fun value(name: String): String? = store()[name]?.value

        fun saveRawCookies(url: String, cookieHeader: String) {
            val map = store()
            val httpUrl = url.toHttpUrl()
            for (pair in cookieHeader.split(";")) {
                val eq = pair.indexOf('=')
                if (eq <= 0) continue
                val name = pair.substring(0, eq).trim()
                val value = pair.substring(eq + 1).trim()
                if (name.isBlank()) continue
                map[name] = Cookie.Builder()
                    .name(name).value(value)
                    .domain(httpUrl.host).path("/")
                    .expiresAt(System.currentTimeMillis() + 6 * 60 * 60 * 1000)
                    .build()
            }
            persist(map)
        }

        private fun persist(map: MutableMap<String, Cookie>) {
            prefs.edit().putStringSet(
                "cookies",
                map.values.filter { it.persistent }.map {
                    "${it.name}\u0001${it.value}\u0001${it.domain}\u0001${it.path}\u0001${it.expiresAt}"
                }.toSet()
            ).apply()
        }

        override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
            val map = store()
            for (c in cookies) map[c.name] = c
            persist(map)
        }

        override fun loadForRequest(url: HttpUrl): List<Cookie> =
            store().values.filter { it.matches(url) }
    }

    val http: OkHttpClient = OkHttpClient.Builder()
        .cookieJar(cookieJar)
        .followRedirects(true)
        // 显式 keep-alive：登录的 GET/POST 必须复用同一连接（同源 IP），
        // 否则服务端会判定"网络环境变化"拒绝提交
        .connectionPool(okhttp3.ConnectionPool(5, 10, java.util.concurrent.TimeUnit.MINUTES))
        .connectTimeout(java.time.Duration.ofSeconds(20))
        .readTimeout(java.time.Duration.ofSeconds(30))
        // 固定真实浏览器请求头：让防护侧看到与正常手机浏览器一致的请求特征
        .addInterceptor { chain ->
            val req = chain.request().newBuilder()
                .header("User-Agent", UA)
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,*/*;q=0.8")
                .header("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8")
                .header("Cache-Control", "max-age=0")
                .build()
            chain.proceed(req)
        }
        .build()

    /** 全局验证页回调：设置后验证页会通过 UI 展示给用户 */
    @Volatile
    var verificationUi: (suspend (String, String) -> Boolean)? = null
    private val verificationMutex = Mutex()
    private val avatarMemoryCache = java.util.concurrent.ConcurrentHashMap<String, ByteArray>()
    // 头像并发去重：同一 URL 的并发请求共享首个请求的结果（列表页同一头像出现多次时只发一次网络请求）
    private val avatarInFlight = java.util.concurrent.ConcurrentHashMap<String, CompletableDeferred<ByteArray>>()
    private val avatarCacheDir: File by lazy {
        File(context.cacheDir, "lsb_avatar_cache").apply { mkdirs() }
    }

    companion object {
        const val UA =
            "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"

        private const val UAM_CHALLENGE_JS =
            "(function(){try{var t=(document.title||'');var b=(document.body?document.body.innerText:'');" +
                "var el=!!(document.querySelector('#challenge-form')||document.querySelector('.ui-uam-box')||" +
                "document.querySelector('.cf-browser-verification')||document.querySelector('#cf-challenge-running')||" +
                "document.querySelector('.challenge-container')||document.querySelector('[class*=\"turnstile\"]')||" +
                "document.querySelector('iframe[src*=\"challenges.cloudflare.com\"]'));" +
                "return (t.indexOf('Checking your Browser')>=0||t.indexOf('Just a moment')>=0||" +
                "t.indexOf('Attention Required')>=0||t.indexOf('Verifying your request')>=0||" +
                "b.indexOf('SECURITY_VERIFICATION')>=0||b.indexOf('X-FL-UA-Step')>=0||el);" +
                "}catch(e){return false;}})()"

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

        /** 源站 Peekabo UAM 验证页特征（与页面 JS 一致） */
        private fun isUamChallenge(html: String): Boolean =
            listOf(
                "X-FL-UA-Step",
                "fl_ua_p",
                "SECURITY_VERIFICATION",
                "UAM_CHECKING",
                "Checking your Browser",
                "Verifying your request",
                "Protected By",
                "Peekabo",
            ).any { html.contains(it) }

        /** Cloudflare 人机验证 / JS 挑战特征（常以 403/503 返回） */
        private fun isCloudflareChallenge(html: String): Boolean =
            listOf(
                "challenge-platform",
                "challenges.cloudflare.com",
                "cf-chl-",
                "cf-browser-verification",
                "__cf_chl",
                "cf-turnstile",
                "turnstile",
                "Just a moment",
                "Attention Required",
                "cdn-cgi/challenge",
                "CF-Chl-Client",
            ).any { html.contains(it, ignoreCase = true) }

        private fun isChallenge(html: String): Boolean = isUamChallenge(html) || isCloudflareChallenge(html)

        /** SVG 字节嗅探：<svg 开头，或 <?xml 声明且前 1KB 内含 <svg（与 Coil SvgDecoder 的嗅探规则一致） */
        private fun isSvgBytes(b: ByteArray): Boolean {
            val head = String(b, 0, minOf(1024, b.size), Charsets.UTF_8).trimStart()
            return head.startsWith("<svg") || (head.startsWith("<?xml") && head.contains("<svg"))
        }

        /** 已知二进制图片魔数：JPEG / PNG / GIF / WebP */
        private fun isBinaryImageMagic(b: ByteArray): Boolean {
            fun u(i: Int) = b[i].toInt() and 0xFF
            return (b.size >= 3 && u(0) == 0xFF && u(1) == 0xD8 && u(2) == 0xFF) ||
                (b.size >= 4 && u(0) == 0x89 && u(1) == 0x50 && u(2) == 0x4E && u(3) == 0x47) ||
                (b.size >= 4 && u(0) == 0x47 && u(1) == 0x49 && u(2) == 0x46 && u(3) == 0x38) ||
                (b.size >= 12 && u(0) == 0x52 && u(1) == 0x49 && u(2) == 0x46 && u(3) == 0x46 &&
                    u(8) == 0x57 && u(9) == 0x45 && u(10) == 0x42 && u(11) == 0x50)
        }

        /** 有效图片字节：以魔数/内容为准，不信任 Content-Type（验证盾可能返回伪装成图片类型的 HTML） */
        fun isValidImageBytes(b: ByteArray): Boolean =
            b.isNotEmpty() && (isBinaryImageMagic(b) || (b[0] == '<'.code.toByte() && isSvgBytes(b)))

        /**
         * 空白或疑似被源站/Cloudflare 套盾的响应，也需要触发验证页展示。
         * 关键：CF 盾通常返回 403/503，不能只按 code==200 判断。
         */
        private fun needsVerification(code: Int, html: String): Boolean =
            html.isBlank() || isChallenge(html) ||
                (code == 403 || code == 503 || code == 429) && html.isNotBlank()

        private fun uamNonce(html: String): Int =
            Regex("""var nonce\s*=\s*(\d+)""").find(html)?.groupValues?.get(1)?.toIntOrNull()
                ?: throw LsbException("验证页缺少 nonce")

        /** 复刻源站 UAM JS：对 cookie 中字母数字字符按位置加权求和 */
        private fun uamSum(cookie: String, nonce: Int): Long {
            var sum = 12345L
            for (i in cookie.indices) {
                val ch = cookie[i]
                if (ch.isLetterOrDigit()) sum += ch.code.toLong() * (nonce + i)
            }
            return sum
        }
    }

    // ---------------- Peekabo UAM 访问验证 ----------------

    private suspend fun solveUam(url: String, challengeHtml: String) = verificationMutex.withLock {
        // 1) 源站 Peekabo UAM：可应用层直接结算（SHA-256 PoW + 加权和 POST）。
        //    用 WebView 弹倒计时页反而会秒数卡死/一直转圈，这里先静默求解。
        if (isUamChallenge(challengeHtml)) {
            try {
                solveUamManually(url, challengeHtml)
                return@withLock
            } catch (_: Exception) {
                // 手动求解失败（如缺 fl_ua_p cookie）→ 回退到 WebView/UI
            }
        }
        // 2) Cloudflare Turnstile / 手动求解失败：交给 WebView + 交互对话框。
        //    不预置 OkHttp cookie 到 WebView，验证通过后 importWebCookies 统一回传。
        val ui = verificationUi
        if (ui != null) {
            val ok = runCatching { ui(url, challengeHtml) }.getOrDefault(false)
            if (ok) return@withLock
        }
        // 3) 未配置 UI：隐藏 WebView 兜底。
        if (ui == null && solveUamWithWebView(url)) return@withLock
    }

    /** 验证页通过后，把 WebView 中的 Cookie 同步回 OkHttp */
    fun importWebCookies(url: String) {
        val cookies = CookieManager.getInstance().getCookie(url)
        if (!cookies.isNullOrBlank()) cookieJar.saveRawCookies(url, cookies)
    }

    /** 把 OkHttp 已保存的登录会话 Cookie 注入 WebView，应用内浏览器即可保持登录（如私信对话） */
    fun exportWebCookies(): Boolean {
        return try {
            val map = cookieJar.store()
            if (map.isEmpty()) return false
            val jar = CookieManager.getInstance()
            jar.setAcceptCookie(true)
            for (c in map.values) {
                val domain = c.domain.trimStart('.')
                val path = c.path.ifBlank { "/" }
                val pair = "${c.name}=${c.value}"
                runCatching { jar.setCookie("https://$domain$path", pair) }
                runCatching { jar.setCookie("http://$domain$path", pair) }
            }
            if (android.os.Build.VERSION.SDK_INT >= 21) runCatching { jar.flush() }
            true
        } catch (_: Exception) { false }
    }

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

    private suspend fun solveUamManually(url: String, challengeHtml: String) {
        var html = challengeHtml
        for (attempt in 0 until 3) {
            val nonce = uamNonce(html)
            val cookie = cookieJar.value("fl_ua_p")
                ?: throw LsbException("验证恢复失败：缺少 UA Cookie")
            val sum = uamSum(cookie, nonce)
            val req = Request.Builder().url(url)
                .header("User-Agent", UA)
                .header("X-FL-UA-Step", "prev")
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

    private suspend fun solveUamWithWebView(url: String): Boolean = withContext(Dispatchers.Main) {
        var webView: WebView? = null
        try {
            val wv = WebView(context.applicationContext)
            webView = wv
            wv.settings.javaScriptEnabled = true
            wv.settings.domStorageEnabled = true
            wv.settings.cacheMode = WebSettings.LOAD_DEFAULT
            // CF 的 cf_clearance 与 UA 强绑定：必须与 OkHttp 使用同一 UA，否则验证完 OkHttp 仍被拒
            wv.settings.userAgentString = UA
            CookieManager.getInstance().setAcceptCookie(true)
            wv.webViewClient = WebViewClient()
            wv.loadUrl(url)

            val passed = withTimeoutOrNull(30_000) { waitForUamPass(wv) } ?: false
            val cookies = CookieManager.getInstance().getCookie(url)
            if (!cookies.isNullOrBlank()) {
                cookieJar.saveRawCookies(url, cookies)
            }
            passed
        } catch (_: Exception) {
            false
        } finally {
            webView?.stopLoading()
            webView?.destroy()
        }
    }

    private suspend fun waitForUamPass(webView: WebView): Boolean {
        for (i in 0 until 25) {
            delay(1000)
            val raw = CompletableDeferred<String>()
            runCatching {
                webView.evaluateJavascript(UAM_CHALLENGE_JS) { value -> raw.complete(value ?: "true") }
            }.onFailure { raw.complete("true") }
            val value = withTimeoutOrNull(2000) { raw.await() } ?: "true"
            if (!value.contains("true", ignoreCase = true)) return true
            if (i == 12) runCatching { webView.reload() }
        }
        return false
    }

    // ---------------- 基础请求 ----------------

    class Resp(val url: String, val html: String, val code: Int)

    suspend fun get(path: String): Resp = withContext(Dispatchers.IO) {
        val req = Request.Builder().url(Endpoints.abs(path))
            .header("User-Agent", UA)
            .build()
        repeat(3) {
            http.newCall(req).execute().use { r ->
                val resp = Resp(r.request.url.toString(), r.body?.string() ?: "", r.code)
                if (needsVerification(resp.code, resp.html)) {
                    solveUam(resp.url, resp.html)
                } else {
                    return@withContext resp
                }
            }
        }
        throw LsbException("访问验证恢复失败")
    }

    /** 请求首页触发一次 UAM，成功后头像/图片后续即可带上会话 Cookie */
    suspend fun ensureAccess() {
        get("/")
    }

    /**
     * 拉取图片原始字节（源站头像等静态资源被 /FL/CC/VALIDATOR 重定向验证盾保护，
     * OkHttp 自动跟随 302 验证链）。
     * 三重防护：并发去重（同 URL 只发一次请求）、魔数校验（防验证页 HTML 混入缓存）、
     * 磁盘缓存读取时再校验（历史坏缓存自动删除自愈，不再永久显示失败）。
     */
    suspend fun fetchBytes(url: String): ByteArray = withContext(Dispatchers.IO) {
        avatarMemoryCache[url]?.let { return@withContext it }
        // v2 前缀：读取时校验魔数；旧版（未校验内容即写入）残留的验证页 HTML 等坏文件在此自愈删除
        val cacheFile = File(avatarCacheDir, "v2_${url.hashCode().toString(16)}.dat")
        if (cacheFile.exists()) {
            val cached = runCatching { cacheFile.readBytes() }.getOrDefault(ByteArray(0))
            if (isValidImageBytes(cached)) {
                avatarMemoryCache[url] = cached
                return@withContext cached
            }
            runCatching { cacheFile.delete() } // 坏缓存：删除后重新拉取
        }
        // 并发去重：首个请求完成前，同 URL 的后续请求共享其结果
        val mine = CompletableDeferred<ByteArray>()
        val existing = avatarInFlight.putIfAbsent(url, mine)
        if (existing != null) return@withContext existing.await()
        try {
            val bytes = fetchImageBytes(url)
            avatarMemoryCache[url] = bytes
            runCatching { cacheFile.writeBytes(bytes) }
            mine.complete(bytes)
            bytes
        } catch (e: Exception) {
            // 首个请求被取消不能连累等待者：等待者收到普通异常后仍可走 URL 兜底
            mine.completeExceptionally(
                if (e is kotlinx.coroutines.CancellationException) LsbException("图片请求中断") else e
            )
            throw e
        } finally {
            avatarInFlight.remove(url, mine)
        }
    }

    /** 图片网络拉取：魔数校验为准（不信任 Content-Type）；?v= 参数 504 时去参重试；验证页触发 solveUam 后重试 */
    private suspend fun fetchImageBytes(url: String): ByteArray {
        // 去参 URL 优先：?v= 时间戳只是缓存穿透参数，去掉不影响内容，还规避网关对带参 URL 的 504
        val candidates = if (url.contains('?')) listOf(url.substringBefore('?'), url) else listOf(url)

        var result: ByteArray? = null
        for (candidate in candidates) {
            val req = Request.Builder().url(candidate)
                .header("User-Agent", UA)
                .header("Accept", "image/avif,image/webp,image/svg+xml,image/*,*/*;q=0.8")
                .build()
            run attempt@{
                repeat(3) {
                    http.newCall(req).execute().use { r ->
                        val b = r.body?.bytes() ?: ByteArray(0)
                        if (isValidImageBytes(b)) {
                            result = b
                            return@attempt
                        }
                        // 内容不是图片：仅真正命中验证特征时才触发验证恢复
                        // （504 空体等网关错误不该弹验证页，直接重试/换候选）
                        val html = String(b, Charsets.UTF_8)
                        if (isChallenge(html)) {
                            runCatching { solveUam(r.request.url.toString(), html) }
                        }
                    }
                }
            }
            if (result != null) break
        }
        return result ?: throw LsbException("图片加载失败")
    }

    suspend fun postForm(path: String, form: Map<String, String>): Resp = withContext(Dispatchers.IO) {
        val b = FormBody.Builder()
        form.forEach { (k, v) -> b.add(k, v) }
        val req = Request.Builder().url(Endpoints.abs(path))
            .header("User-Agent", UA)
            .post(b.build())
            .build()
        repeat(3) {
            http.newCall(req).execute().use { r ->
                val resp = Resp(r.request.url.toString(), r.body?.string() ?: "", r.code)
                if (needsVerification(resp.code, resp.html)) {
                    solveUam(resp.url, resp.html)
                } else {
                    return@withContext resp
                }
            }
        }
        throw LsbException("访问失败")
    }

    /** AJAX 提交，返回 JSON 文本 */
    suspend fun postAjax(path: String, form: Map<String, String>): JSONObject = withContext(Dispatchers.IO) {
        val b = FormBody.Builder()
        form.forEach { (k, v) -> b.add(k, v) }
        val req = Request.Builder().url(Endpoints.abs(path))
            .header("User-Agent", UA)
            .header("X-Requested-With", "XMLHttpRequest")
            .header("Accept", "application/json")
            .post(b.build())
            .build()
        repeat(3) {
            http.newCall(req).execute().use { r ->
                val text = r.body?.string() ?: ""
                if (isChallenge(text) || r.code == 403 || r.code == 503) {
                    solveUam(r.request.url.toString(), text)
                } else {
                    return@withContext JSONObject(if (text.isBlank()) "{}" else text)
                }
            }
        }
        throw LsbException("接口访问失败")
    }

    /** multipart 表单提交（头像上传等文件场景） */
    suspend fun postMultipart(
        path: String,
        fields: Map<String, String>,
        fileField: String,
        file: File,
        mime: String,
    ): Resp = withContext(Dispatchers.IO) {
        val b = okhttp3.MultipartBody.Builder().setType(okhttp3.MultipartBody.FORM)
        fields.forEach { (k, v) -> b.addFormDataPart(k, v) }
        val media = mime.toMediaTypeOrNull() ?: "image/jpeg".toMediaType()
        b.addFormDataPart(fileField, file.name, okhttp3.RequestBody.create(media, file))
        val req = Request.Builder().url(Endpoints.abs(path))
            .header("User-Agent", UA)
            .post(b.build())
            .build()
        repeat(3) {
            http.newCall(req).execute().use { r ->
                val resp = Resp(r.request.url.toString(), r.body?.string() ?: "", r.code)
                if (needsVerification(resp.code, resp.html)) {
                    solveUam(resp.url, resp.html)
                } else {
                    return@withContext resp
                }
            }
        }
        throw LsbException("上传访问失败")
    }

    /** AJAX GET，返回 JSON 文本（失败/HTML 时抛 LsbException） */
    private suspend fun getAjaxJson(path: String): JSONObject = withContext(Dispatchers.IO) {
        val req = Request.Builder().url(Endpoints.abs(path))
            .header("User-Agent", UA)
            .header("X-Requested-With", "XMLHttpRequest")
            .header("Accept", "application/json")
            .build()
        repeat(3) {
            http.newCall(req).execute().use { r ->
                val text = r.body?.string() ?: ""
                val t = text.trim()
                if (isChallenge(text)) {
                    solveUam(r.request.url.toString(), text)
                } else {
                    if (t.startsWith("{") || t.startsWith("[")) return@withContext JSONObject(t)
                    throw LsbException("接口未返回 JSON")
                }
            }
        }
        throw LsbException("接口访问失败")
    }

    // ---------------- 源站屏蔽词（首页关键词过滤） ----------------

    data class KeywordFilter(
        val presets: List<String> = emptyList(),
        val custom: List<String> = emptyList(),
        val users: List<String> = emptyList(),
        val availablePresets: List<String> = emptyList(), // 源站下发的可选预设词
    )

    /**
     * 按源站 plugins.js 的做法解析：
     * GET 返回 {ok:1, exists:1, settings:{presets:[], custom:[], users:[]}}；
     * 规则：normalize(trim+lowercase)，custom 最多 20 词、users 最多 5 个。
     */
    private fun parseKeywordFilter(json: JSONObject): KeywordFilter {
        val s = json.optJSONObject("settings")
        fun arr(o: JSONObject, k: String, limit: Int): List<String> = runCatching {
            val a = o.optJSONArray(k) ?: return emptyList()
            (0 until a.length()).mapNotNull { i ->
                when (val v = a.get(i)) {
                    is String -> v.trim().lowercase().ifBlank { null }
                    else -> v.toString().trim().lowercase().ifBlank { null }
                }
            }.distinct().take(limit)
        }.getOrDefault(emptyList())
        return if (s != null) KeywordFilter(
            presets = arr(s, "presets", 20),
            custom = arr(s, "custom", 20),
            users = arr(s, "users", 5),
        ) else KeywordFilter()
    }

    /** 拉取源站屏蔽词设置（需登录，接口 302 到 /login 视为未登录） */
    suspend fun getKeywordFilter(): KeywordFilter {
        return try {
            val json = getAjaxJson("/home_keyword_filter_settings")
            if (json.optInt("ok", 0) != 1 && !json.has("settings"))
                throw LsbException(json.optString("message").ifBlank { "读取失败" })
            parseKeywordFilter(json)
        } catch (e: LsbException) {
            throw LsbException("请先登录后使用源站屏蔽词")
        }
    }

    /** 保存源站屏蔽词设置（需登录）：POST 字段为 _csrf + settings(JSON 字符串)，与源站一致 */
    suspend fun saveKeywordFilter(f: KeywordFilter): KeywordFilter {
        val csrf = csrf()
        val settings = JSONObject().put("presets", org.json.JSONArray(f.presets))
            .put("custom", org.json.JSONArray(f.custom))
            .put("users", org.json.JSONArray(f.users))
        val json = postAjax(
            "/home_keyword_filter_settings", mapOf(
                "_csrf" to csrf,
                "settings" to settings.toString(),
            )
        )
        if (json.optInt("ok", 0) != 1) {
            throw LsbException(json.optString("message").ifBlank { "保存失败" })
        }
        return parseKeywordFilter(json)
    }

    // ---------------- 打赏弹幕 ----------------

    /** 拉取帖子打赏弹幕（源站 /donate_feed AJAX 接口） */
    suspend fun getDonateFeed(topicId: Long): List<DanmakuItem> = withContext(Dispatchers.IO) {
        runCatching {
            val req = Request.Builder()
                .url(Endpoints.abs("/donate_feed?topic_id=$topicId&last_id=0&bootstrap=1"))
                .header("User-Agent", UA)
                .header("X-Requested-With", "XMLHttpRequest")
                .header("Accept", "application/json")
                .build()
            http.newCall(req).execute().use { r ->
                val text = r.body?.string() ?: ""
                val json = JSONObject(if (text.isBlank()) "{}" else text)
                HtmlParser.parseDonateBarrage(json.optString("area_html"))
            }
        }.getOrDefault(emptyList())
    }

    // ---------------- CSRF ----------------

    @Volatile
    private var csrfCache: String? = null

    suspend fun csrf(forceRefresh: Boolean = false): String {
        if (!forceRefresh && csrfCache != null) return csrfCache!!
        val html = get("/").html
        val m = Regex("""name="_csrf" value="([^"]+)"""").find(html)
        csrfCache = m?.groupValues?.get(1)
        return csrfCache ?: throw LsbException("无法获取 CSRF 令牌")
    }

    // ---------------- 登录 ----------------

    /** 登录页验证码信息：题目由用户作答，PoW 由客户端自动计算 */
    data class LoginCaptcha(
        val csrf: String,
        val question: String,
        val questionHtml: String = "",
        val token: String,
        val powPrefix: String,
        val powZeros: Int,
    )

    /** 拉取登录页，提取人机验证题目（供用户作答）。登录前先确保通过访问验证。 */
    suspend fun fetchLoginCaptcha(): LoginCaptcha = withContext(Dispatchers.IO) {
        // 登录页同样被访问盾/CF 保护：先触发一次验证，避免登录页 302 到挑战页
        get("/")
        val page = get("/login")
        val d = Jsoup.parse(page.html, Endpoints.BASE)
        // 已登录时源站把 /login 302 到首页（最终 URL 不再含 /login），无验证码组件可解析：
        // 给出明确提示而非笼统的"解析失败"（访客页也有 .user-name"访客"，不能用作登录态判定）
        if (!page.url.contains("/login")) {
            throw LsbException("已是登录状态，无需重复登录（请先退出登录）")
        }
        val widget = d.selectFirst("[data-native-captcha]")
        val csrf = d.selectFirst("input[name=_csrf]")?.attr("value")
            ?: Regex("""name="_csrf" value="([^"]+)"""").find(page.html)?.groupValues?.get(1)
            ?: throw LsbException("登录页解析失败")
        val token = d.selectFirst("input[name=native_captcha_token]")?.attr("value")
            ?: Regex("""name="native_captcha_token" value="([^"]+)"""").find(page.html)?.groupValues?.get(1)
            ?: throw LsbException("未找到人机验证组件")
        val questionEl = widget?.selectFirst(".native-captcha-question")
            ?: d.selectFirst(".native-captcha-question")
        val question = questionEl?.text()?.trim()
            ?: Regex("""class="native-captcha-question">([^<]+)<""").find(page.html)?.groupValues?.get(1)?.trim()
            ?: throw LsbException("验证码题目缺失")
        LoginCaptcha(
            csrf = csrf,
            question = question,
            questionHtml = questionEl?.outerHtml() ?: "",
            token = token,
            powPrefix = widget?.attr("data-pow-prefix") ?: "",
            powZeros = widget?.attr("data-pow-zeroes")?.toIntOrNull() ?: 3,
        )
    }

    /**
     * 登录：用户填写的验证码答案 + 自动 PoW → 等待反刷间隔 → 提交。
     * @param captcha 先前 fetchLoginCaptcha() 获取的验证码信息
     * @param answer 用户填写的计算结果
     */
    suspend fun login(
        username: String,
        password: String,
        captcha: LoginCaptcha,
        answer: String,
        onStatus: (String) -> Unit,
    ): String = withContext(Dispatchers.IO) {
        onStatus("正在计算工作量证明…")
        val t0 = System.currentTimeMillis()
        val pow = solvePow(captcha.powPrefix, captcha.powZeros)

        // 服务器要求验证码加载后再提交（防脚本）
        val elapsed = System.currentTimeMillis() - t0
        if (elapsed < 5200) {
            onStatus("安全校验中…")
            Thread.sleep(5200 - elapsed)
        }

        val resp = postForm(
            "/login", mapOf(
                "_csrf" to captcha.csrf,
                "username" to username,
                "password" to password,
                "native_captcha_answer" to answer.trim(),
                "native_captcha_token" to captcha.token,
                "native_captcha_pow" to pow,
                "native_captcha_company" to "",
            )
        )
        if (resp.url.contains("form_error")) {
            throw LsbException(HtmlParser.extractError(resp.html).ifBlank { "登录被拒绝，验证码答案可能有误" })
        }
        // 成功判定：实测源站登录成功 302 → 首页，失败（密码错/验证码错）→ /form_error，非 form_error 即成功。
        // 此前用 contains(username) 兜底判定：邮箱登录时页面只显示用户名而非邮箱，导致实际登录
        // 成功却误报"用户名或密码错误"（cookie 已写入，再次刷新验证码还会因已登录 302 首页而静默失败）
        "登录成功"
    }

    suspend fun logout() {
        try {
            val csrf = csrf()
            postForm("/logout", mapOf("_csrf" to csrf))
        } catch (_: Exception) {
        }
        csrfCache = null
        prefs.edit().putStringSet("cookies", emptySet()).apply()
    }

    fun hasAuthCookie(): Boolean {
        val set = prefs.getStringSet("cookies", emptySet()) ?: emptySet()
        return set.any { it.startsWith("bbs_auth\u0001") }
    }
}
