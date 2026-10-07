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

/** 明确标记即使已进入发送阶段也可安全重放的请求，例如只签发临时题目的 CAP challenge。 */
internal object CronetReplayable

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
    return !mayHaveSent || request.method in setOf("GET", "HEAD") ||
        request.tag(CronetReplayable::class.java) != null
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
        // 用户明确启用代理时不能用 Cronet 直连回退，否则会绕过代理泄漏连接。
        if (AppNetwork.proxyConfig().enabled) throw failure
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
                    // OkHttp 允许无类型的空 POST，Cronet 对任何 upload data 都强制要求
                    // Content-Type。CAP challenge 正是空 POST，回退时补默认类型但不改变正文。
                    builder.addHeader("Content-Type", body.contentType()?.toString() ?: "application/octet-stream")
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
