package sb.linux.client.data

import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.Headers
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
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
import java.util.zip.GZIPInputStream
import okio.buffer

/**
 * TCP/TLS 失败后的 Cronet（QUIC / HTTP3）回退传输层。
 *
 * QUIC 是否可用由当前线路与服务端决定，不把它当成保证连通的代理。
 * 仅安全重放或确定尚未进入发送阶段的请求可以回退。
 *
 * DNS：Cronet 自带解析走系统 DNS（污染结果会让 QUIC 握手证书校验失败），
 * 因此通过 HostResolverRules 把目标域名 MAP 到本应用 DoH 已解析出的真实 IP。
 * 每次重定向重新解析目标。引擎按网络策略、域名、端口和 IP 隔离。
 */
internal class CronetAttempt { @Volatile var mayHaveSent = false }

internal const val CRONET_RESPONSE_LIMIT = 32L * 1024 * 1024

internal fun cronetAddressCandidates(addresses: List<java.net.InetAddress>): List<java.net.InetAddress> =
    (listOfNotNull(addresses.firstOrNull { it is Inet4Address }, addresses.firstOrNull { it is java.net.Inet6Address }) + addresses)
        .distinctBy { it.hostAddress }.take(3)

internal fun cronetReadChunk(buffer: ByteBuffer): ByteArray {
    buffer.flip()
    return ByteArray(buffer.remaining()).also { buffer.get(it); buffer.clear() }
}

internal fun canRetryWithCronet(request: Request, error: IOException, mayHaveSent: Boolean): Boolean {
    if (request.url.scheme != "https" || request.body?.isOneShot() == true || request.body?.isDuplex() == true) return false
    if (generateSequence<Throwable>(error) { it.cause }.take(16).any {
        it is javax.net.ssl.SSLPeerUnverifiedException || it is java.security.cert.CertificateException
    }) return false
    return !mayHaveSent || request.method in setOf("GET", "HEAD")
}

internal fun cronetResolverOptions(host: String, ip: String): String {
    require(("https://$host/").toHttpUrlOrNull()?.host == host)
    require(ip.isNotBlank() && ip.all { it in "0123456789abcdefABCDEF:." })
    val address = if (':' in ip) "[$ip]" else ip
    require(("https://$address/").toHttpUrlOrNull() != null)
    return "{\"HostResolverRules\":{\"host_resolver_rules\":\"MAP $host $address\"}}"
}

/** 保留请求句柄并轮询上层取消状态；所有异常路径均取消底层请求。 */
internal fun <T> awaitCronetResult(
    future: CompletableFuture<T>, canceled: () -> Boolean, cancel: () -> Unit, deadline: Long,
): T {
    var completed = false
    try {
        while (true) {
            if (canceled()) throw IOException("Canceled")
            val left = deadline - System.nanoTime()
            if (left <= 0) throw java.net.SocketTimeoutException("Timed out")
            try {
                return future.get(minOf(left, TimeUnit.MILLISECONDS.toNanos(150)), TimeUnit.NANOSECONDS)
                    .also { completed = true }
            } catch (_: TimeoutException) { /* 检查取消与总期限 */ }
            catch (e: ExecutionException) { throw (e.cause as? IOException ?: IOException("Cronet request failed", e.cause)) }
        }
    } catch (e: InterruptedException) {
        Thread.currentThread().interrupt()
        throw java.io.InterruptedIOException("Interrupted").apply { initCause(e) }
    } finally { if (!completed) cancel() }
}

class CronetFallbackInterceptor(
    private val cookieJar: CookieJar = CookieJar.NO_COOKIES,
    private val dns: okhttp3.Dns = AppNetwork.dns,
    private val policyKey: () -> String = AppNetwork::policyKey,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val attempt = CronetAttempt()
        val request = chain.request().newBuilder().tag(CronetAttempt::class.java, attempt).build()
        val failure = try {
            return chain.proceed(request)
        } catch (e: IOException) {
            e
        }
        // 已取消的调用不重试；仅 https 可能走 QUIC（http 无绕过意义，直抛原错误）
        if (chain.call().isCanceled() || !canRetryWithCronet(request, failure, attempt.mayHaveSent)) throw failure
        return try {
            executeWithCronet(request, chain.call())
        } catch (e: Exception) {
            // 不记录完整 URL、查询参数或服务端异常详情（其中可能包含 token）。
            if (e is InterruptedException) Thread.currentThread().interrupt()
            failure.addSuppressed(e)
            throw failure // Cronet 也失败时保留原始错误，界面语义不变
        }
    }

    private fun executeWithCronet(request: Request, call: okhttp3.Call): Response {
        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(30)
        var current = withDefaultBrowserHeaders(request)
        var redirects = 0
        while (true) {
            if (call.isCanceled()) throw IOException("Canceled")
            // 重定向可能更换域名；不能让新域名落回 Cronet 的系统 DNS。
            val policy = policyKey()
            val addresses = dns.lookup(current.url.host).sortedBy { it !is Inet4Address }
            var result: CronetResult? = null
            var last: Exception? = null
            for (address in cronetAddressCandidates(addresses)) {
                if (call.isCanceled() || System.nanoTime() >= deadline) throw IOException("Canceled or timed out")
                val lease = CronetTransport.acquire(current.url.host, current.url.port, address.hostAddress!!, policy)
                try {
                    result = executeOnce(lease, current, call, deadline)
                    break
                } catch (e: Exception) {
                    last = e
                    // 写操作已经交给 Cronet 后不再换 IP 重发，以防重复提交。
                    if (current.method !in setOf("GET", "HEAD")) throw e
                }
            }
            val response = result ?: throw IOException("Cronet unavailable", last)
            if (response.location == null) {
                return response.toResponse(current)
            }
            if (++redirects > MAX_REDIRECTS) throw IOException("Cronet 回退重定向次数超限")
            current = response.toFollowRequest(current)
        }
    }

    private fun executeOnce(lease: CronetTransport.Lease, request: Request, call: okhttp3.Call, deadline: Long): CronetResult {
        val url = request.url
        val future = CompletableFuture<CronetResult>()
        var nativeRequest: UrlRequest? = null
        var started = false
        try {
            val builder = lease.engine.newUrlRequestBuilder(
                url.toString(),
                CronetCallbackBridge(future, url, cookieJar, lease::close),
                CronetTransport.callbackExecutor
            )
            builder.setHttpMethod(request.method)
            request.headers.forEach { (name, value) ->
                // Host/Content-Length/Connection 由 Cronet 自行计算，重复传入会报错
                if (name.equals("Host", true) || name.equals("Content-Length", true) ||
                    name.equals("Connection", true) || name.equals("Accept-Encoding", true) ||
                    name.equals("Transfer-Encoding", true)
                ) return@forEach
                builder.addHeader(name, value)
            }
            if (cookieJar != CookieJar.NO_COOKIES && request.header("Cookie") == null) {
                cookieJar.loadForRequest(url).takeIf { it.isNotEmpty() }?.let { cookies ->
                    builder.addHeader("Cookie", cookies.joinToString("; ") { "${it.name}=${it.value}" })
                }
            }
            // Cronet 管理压缩协商并透明解码，避免重复解压或声明其不支持的编码。
            request.body?.let { body ->
                if (body.isOneShot() || body.isDuplex() || body.contentLength() > MAX_BUFFER_BYTES) throw IOException("Request cannot be replayed")
                val buffer = okio.Buffer()
                val limited = object : okio.ForwardingSink(buffer) {
                    override fun write(source: okio.Buffer, byteCount: Long) {
                        if (buffer.size + byteCount > MAX_BUFFER_BYTES) throw IOException("Request too large")
                        super.write(source, byteCount)
                    }
                }
                val sink = limited.buffer()
                body.writeTo(sink); sink.flush()
                val bytes = buffer.readByteArray()
                builder.setUploadDataProvider(UploadDataProviders.create(bytes), CronetTransport.callbackExecutor)
                if (request.header("Content-Type") == null) {
                    body.contentType()?.let { builder.addHeader("Content-Type", it.toString()) }
                }
            }
            nativeRequest = builder.build()
            nativeRequest.start()
            started = true
            return awaitCronetResult(future, call::isCanceled, { nativeRequest.cancel() },
                minOf(deadline, System.nanoTime() + TimeUnit.SECONDS.toNanos(10)))
        } catch (e: Exception) {
            nativeRequest?.cancel()
            throw e
        } finally {
            // 已启动的请求由终态回调释放；重定向 cancel 的回调也需走完才能关闭引擎。
            if (!started) lease.close()
        }
    }

    companion object {
        private const val MAX_REDIRECTS = 10
        private const val MAX_BUFFER_BYTES = 16L * 1024 * 1024
    }
}

/**
 * 引用计数引擎池：配置变更立即停止复用旧引擎，在途请求正常结束后异步关闭。
 */
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

/** Cronet 单次请求的结果（30x 也算一次结果，location 非空表示需要外层跟随） */
internal class CronetResult(
    val code: Int,
    val statusText: String,
    val headers: Headers,
    val body: ByteArray,
    val location: String?,
    val negotiatedProtocol: String?,
) {
    fun toResponse(request: Request): Response {
        var responseHeaders = headers
        var bytes = body
        if (responseHeaders["Content-Encoding"]?.equals("gzip", true) == true) {
            // Cronet 会自动解压响应体但保留 Content-Encoding 头；用 gzip 魔数判断
            // 当前内容是否仍是压缩态，避免二次解压（ZipException）
            if (isGzip(bytes)) {
                bytes = GZIPInputStream(bytes.inputStream()).use { input ->
                    val output = ByteArrayOutputStream()
                    val chunk = ByteArray(8192)
                    while (true) {
                        val count = input.read(chunk)
                        if (count < 0) break
                        if (output.size().toLong() + count > CRONET_RESPONSE_LIMIT) throw IOException("Response too large")
                        output.write(chunk, 0, count)
                    }
                    output.toByteArray()
                }
            }
            responseHeaders = responseHeaders.newBuilder()
                .removeAll("Content-Encoding")
                .removeAll("Content-Length")
                .set("Content-Length", bytes.size.toString())
                .build()
        }
        // Brotli/deflate 等由 Cronet 透明解码；不能向下游继续宣称 body 还是压缩字节。
        responseHeaders = responseHeaders.newBuilder().removeAll("Content-Encoding")
            .removeAll("Transfer-Encoding").set("Content-Length", bytes.size.toString()).build()
        return Response.Builder()
            .request(request)
            .protocol(Protocol.HTTP_2)
            .code(code)
            .message(statusText.ifBlank { "Cronet" })
            .headers(responseHeaders)
            .body(bytes.toResponseBody(responseHeaders["Content-Type"]?.toMediaTypeOrNull()))
            .build()
    }

    fun toFollowRequest(previous: Request): Request {
        val newUrl = previous.url.resolve(location!!)
            ?: throw IOException("Cronet 回退遇到无效重定向地址: $location")
        if (newUrl.scheme != "https") throw IOException("Refusing HTTPS downgrade")
        val method = when {
            previous.method == "HEAD" -> "HEAD"
            code == 303 || (code in setOf(301, 302) && previous.method == "POST") -> "GET"
            else -> previous.method // 307/308 保留方法与请求体
        }
        val nextBody = if (method == "GET" || method == "HEAD") null else previous.body
        return previous.newBuilder().url(newUrl).method(method, nextBody).apply {
            if (nextBody == null) {
                removeHeader("Content-Type"); removeHeader("Content-Length"); removeHeader("Transfer-Encoding")
            }
            if (previous.url.host != newUrl.host || previous.url.port != newUrl.port || previous.url.scheme != newUrl.scheme) {
                removeHeader("Authorization"); removeHeader("Cookie"); removeHeader("Proxy-Authorization")
            }
        }.build()
    }
}

/** gzip 魔数（0x1f 0x8b）：区分「仍是压缩态」与「Cronet 已透明解压」 */
private fun isGzip(bytes: ByteArray): Boolean =
    bytes.size >= 2 && bytes[0] == 0x1f.toByte() && bytes[1] == 0x8b.toByte()

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
