package sb.linux.client.data

import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.Headers
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import org.chromium.net.CronetEngine
import org.chromium.net.CronetException
import org.chromium.net.ExperimentalCronetEngine
import org.chromium.net.UploadDataProviders
import org.chromium.net.UrlRequest
import org.chromium.net.UrlResponseInfo
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.net.Inet4Address
import java.nio.ByteBuffer
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import java.util.concurrent.ExecutionException
import java.util.concurrent.atomic.AtomicBoolean
import okio.buffer

internal object CronetTransport {
    private const val MAX_ENGINES = 6
    internal class Entry(val engine: CronetEngine, var users: Int = 0, var retired: Boolean = false)
    private val engines = LinkedHashMap<String, Entry>(8, 0.75f, true)
    private val callbackPool = Executors.newFixedThreadPool(4) { runnable ->
        Thread(runnable, "cronet-callback").apply { isDaemon = true }
    }
    private val maintainer = Executors.newSingleThreadScheduledExecutor { runnable ->
        Thread(runnable, "cronet-maintain").apply { isDaemon = true }
    }

    val callbackExecutor: Executor get() = callbackPool

    internal class Lease(val engine: CronetEngine, private val release: () -> Unit) : AutoCloseable {
        private val closed = AtomicBoolean()
        override fun close() { if (closed.compareAndSet(false, true)) release() }
    }

    @Synchronized fun acquire(host: String, port: Int, ip: String, policy: String): Lease {
        val key = "$policy|$host|$port|$ip"
        val entry = engines[key] ?: run {
            val context = AppNetwork.appContext() ?: throw IOException("网络组件尚未初始化")
            // ExperimentalCronetEngine.Builder 才有 setExperimentalOptions（143 起
            // 普通 Builder 上该方法已被移到实验接口）
            val engine = ExperimentalCronetEngine.Builder(context)
                .enableHttp2(true)
                .enableQuic(true)
                // 让 Cronet 对该域名直接尝试 QUIC（否则需先经 TCP Alt-Svc 学习，而 TCP 已被阻断）
                .addQuicHint(host, port, port)
                // 系统 DNS 已被污染，强制 MAP 到 DoH 解析出的真实 IP。
                // 注意格式：HostResolverRules 的值是嵌套对象（字段名 host_resolver_rules），
                // 传纯字符串会被 Chromium 以 config params 错误忽略
                .setExperimentalOptions(
                    cronetResolverOptions(host, ip)
                )
                .build()
            Entry(engine).also { engines[key] = it }
        }
        entry.users++
        while (engines.size > MAX_ENGINES) engines.remove(engines.keys.first())?.let(::retire)
        return Lease(entry.engine) { synchronized(this) {
            entry.users--
            if (entry.retired && entry.users == 0) shutdown(entry.engine)
        } }
    }

    @Synchronized fun invalidate() {
        engines.values.forEach(::retire)
        engines.clear()
    }

    private fun retire(entry: Entry) {
        entry.retired = true
        if (entry.users == 0) shutdown(entry.engine)
    }

    private fun shutdown(engine: CronetEngine, retries: Int = 0) {
        // Cronet 不允许在回调线程 shutdown；最后一个回调返回前短暂重试，不阻塞清理队列。
        maintainer.schedule({
            try { engine.shutdown() }
            catch (_: Exception) { if (retries < 10) shutdown(engine, retries + 1) }
        }, 200, TimeUnit.MILLISECONDS)
    }
}

/**
 * UrlRequest.Callback 桥接：把异步回调收敛为一个 [CronetResult] Future。
 * 重定向不交给 Cronet 内部跟随（它没有 Cookie 存储，登录会丢会话），
 * 而是记录 30x 响应后 cancel，由外层逐跳重建请求。
 */
internal class CronetCallbackBridge(
    private val future: CompletableFuture<CronetResult>,
    private val url: HttpUrl,
    private val cookieJar: CookieJar,
    private val release: () -> Unit = {},
) : UrlRequest.Callback() {
    private val body = ByteArrayOutputStream()

    override fun onRedirectReceived(request: UrlRequest, info: UrlResponseInfo, newLocationUrl: String) {
        saveCookies(info)
        future.complete(
            CronetResult(
                code = info.httpStatusCode,
                statusText = info.httpStatusText.orEmpty(),
                headers = toOkHeaders(info),
                body = ByteArray(0),
                location = newLocationUrl,
                negotiatedProtocol = info.negotiatedProtocol,
            )
        )
        // future 已完成，后续 onCanceled 不会再覆盖结果
        request.cancel()
    }

    override fun onResponseStarted(request: UrlRequest, info: UrlResponseInfo) {
        saveCookies(info)
        request.read(ByteBuffer.allocateDirect(16 * 1024))
    }

    override fun onReadCompleted(request: UrlRequest, info: UrlResponseInfo, byteBuffer: ByteBuffer) {
        try {
            val chunk = cronetReadChunk(byteBuffer)
            if (body.size().toLong() + chunk.size > CRONET_RESPONSE_LIMIT) throw IOException("Response too large")
            body.write(chunk)
            request.read(byteBuffer)
        } catch (e: Exception) { future.completeExceptionally(e); request.cancel() }
    }

    override fun onSucceeded(request: UrlRequest, info: UrlResponseInfo) {
        future.complete(
            CronetResult(
                code = info.httpStatusCode,
                statusText = info.httpStatusText.orEmpty(),
                headers = toOkHeaders(info),
                body = body.toByteArray(),
                location = null,
                negotiatedProtocol = info.negotiatedProtocol,
            )
        )
        release()
    }

    override fun onFailed(request: UrlRequest, info: UrlResponseInfo?, error: CronetException) {
        future.completeExceptionally(IOException("${error.javaClass.simpleName}: ${error.message}", error))
        release()
    }

    override fun onCanceled(request: UrlRequest, info: UrlResponseInfo?) {
        future.completeExceptionally(IOException("canceled"))
        release()
    }

    private fun saveCookies(info: UrlResponseInfo) {
        if (cookieJar == CookieJar.NO_COOKIES) return
        val cookies = info.allHeadersAsList
            .filter { it.key.equals("Set-Cookie", true) }
            .mapNotNull { Cookie.parse(url, it.value) }
        if (cookies.isNotEmpty()) cookieJar.saveFromResponse(url, cookies)
    }

    private fun toOkHeaders(info: UrlResponseInfo): Headers = Headers.Builder().apply {
        info.allHeadersAsList.forEach { add(it.key, it.value) }
    }.build()
}

/**
 * 通用客户端的兜底请求头。论坛和图片客户端把回退拦截器放在各自请求头之后，
 * 因此真实 UA/Accept/缓存策略会保留；此处只填补没有显式设置的项。
 */
internal fun withDefaultBrowserHeaders(request: Request): Request {
    val builder = request.newBuilder()
    if (request.header("User-Agent") == null) builder.header("User-Agent", LsbClient.UA)
    if (request.header("Accept") == null) {
        builder.header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,*/*;q=0.8")
    }
    if (request.header("Accept-Language") == null) {
        builder.header("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8")
    }
    return builder.build()
}
