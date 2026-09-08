package sb.linux.client.data

import android.webkit.JavascriptInterface
import okhttp3.Call
import okhttp3.Callback
import okhttp3.ConnectionPool
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONObject
import java.io.Closeable
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

/** 将 CAP 的动态接口请求转交给支持 DoH 的 OkHttp，并严格限制到当前验证码端点。 */
internal class CapNetworkBridge(
    private val client: LsbClient,
    endpoint: String,
    private val pageUrl: String,
    private val evaluateJavascript: (String) -> Unit,
) : Closeable {
    private val endpointUrl = endpoint.toHttpUrlOrNull()
    private val pageOrigin = pageUrl.toHttpUrlOrNull()?.let {
        val port = if ((it.isHttps && it.port == 443) || (!it.isHttps && it.port == 80)) "" else ":${it.port}"
        "${it.scheme}://${it.host}$port"
    }.orEmpty()
    private val calls = ConcurrentHashMap.newKeySet<Call>()
    private val closed = AtomicBoolean(false)
    private val solver = Executors.newFixedThreadPool(8)
    private val requestScheduler: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor()
    // CAP 在 DoH/移动网络下偶尔会复用已经失效的 HTTP/2 连接；独立的短连接 HTTP/1.1
    // 通道仍沿用 client 的 DoH、代理、Cookie 和 UA 策略，但不污染站内长连接池。
    private val capHttp = client.http.newBuilder()
        .protocols(listOf(Protocol.HTTP_1_1))
        .connectionPool(ConnectionPool(0, 1, TimeUnit.SECONDS))
        .retryOnConnectionFailure(true)
        .build()

    @JavascriptInterface
    fun request(requestId: String, url: String, method: String, headersJson: String, body: String) {
        if (closed.get()) return
        val target = allowedTarget(url)
        if (target == null || method.uppercase() != "POST") {
            complete(requestId, errorResult("验证码请求被拒绝"))
            return
        }
        val contentType = runCatching {
            JSONObject(headersJson).optString("content-type").toMediaTypeOrNull()
        }.getOrNull()
        val request = Request.Builder()
            .url(target)
            .header("Accept", "application/json")
            .apply {
                if (pageOrigin.isNotBlank()) header("Origin", pageOrigin)
                if (pageUrl.isNotBlank()) header("Referer", pageUrl)
            }
            .post(body.toRequestBody(contentType))
            // challenge 只签发临时计算题，不会消费凭据；TCP 被重置后允许统一网络层
            // 经 DoH 固定解析结果切换到 Cronet/HTTP3。redeem 会消费令牌，禁止重放。
            .apply {
                if (target.encodedPath.endsWith("/challenge")) {
                    tag(CronetReplayable::class.java, CronetReplayable)
                }
            }
            .build()
        enqueueRequest(requestId, request, target.encodedPath.endsWith("/challenge"))
    }

    /** challenge 没有副作用，连接建立失败时可以重新请求；redeem 只允许提交一次。 */
    private fun enqueueRequest(
        requestId: String,
        request: Request,
        retryable: Boolean,
        attempt: Int = 0,
    ) {
        if (closed.get()) return
        val call = capHttp.newCall(request)
        calls += call
        call.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                calls -= call
                if (closed.get()) return
                if (retryable && attempt < MAX_CHALLENGE_RETRIES) {
                    val delay = RETRY_DELAYS_MS[attempt]
                    requestScheduler.schedule(
                        { enqueueRequest(requestId, request, retryable, attempt + 1) },
                        delay,
                        TimeUnit.MILLISECONDS,
                    )
                } else {
                    complete(
                        requestId,
                        errorResult(
                            "验证码服务连接失败",
                            networkFailureDetail(e),
                        ),
                    )
                }
            }

            override fun onResponse(call: Call, response: Response) {
                calls -= call
                response.use {
                    val responseBody = runCatching { it.body?.string().orEmpty() }.getOrDefault("")
                    complete(
                        requestId,
                        JSONObject()
                            .put("status", it.code)
                            .put("body", responseBody)
                            .put("contentType", it.header("Content-Type", "application/json")),
                    )
                }
            }
        })
    }

    /** Android WebView 的 Blob Worker/WASM 兼容性不稳定，Pow 在原生线程中完成后回传给代理 Worker。 */
    @JavascriptInterface
    fun solvePow(requestId: String, salt: String, target: String) {
        if (closed.get() || salt.length > 256 || target.length !in 1..64 || !target.matches(Regex("[0-9a-fA-F]+"))) {
            completeWorker(requestId, JSONObject().put("error", "invalid challenge"))
            return
        }
        try {
            solver.execute {
                try {
                    completeWorker(
                        requestId,
                        JSONObject().put("found", true).put("nonce", CapChallengeSolver.solvePow(salt, target)),
                    )
                } catch (_: Exception) {
                    completeWorker(requestId, JSONObject().put("error", "native solver failed"))
                }
            }
        } catch (_: Exception) {
            completeWorker(requestId, JSONObject().put("error", "native solver unavailable"))
        }
    }

    /** CAP 新版 format=2 的 RSW 挑战：与组件 Worker 保持相同的 x^(2^t) mod N 运算。 */
    @JavascriptInterface
    fun solveRsw(requestId: String, modulus: String, value: String, iterations: Int) {
        if (closed.get() || iterations < 0 || iterations > 10_000 ||
            !modulus.matches(Regex("[0-9a-fA-F]+")) || !value.matches(Regex("[0-9a-fA-F]+"))
        ) {
            completeWorker(requestId, JSONObject().put("error", "invalid challenge"))
            return
        }
        try {
            solver.execute {
                try {
                    completeWorker(
                        requestId,
                        JSONObject().put("found", true).put("y", CapChallengeSolver.solveRsw(modulus, value, iterations)),
                    )
                } catch (_: Exception) {
                    completeWorker(requestId, JSONObject().put("error", "native solver failed"))
                }
            }
        } catch (_: Exception) {
            completeWorker(requestId, JSONObject().put("error", "native solver unavailable"))
        }
    }

    private fun completeWorker(requestId: String, result: JSONObject) {
        complete(requestId, result, "__lsbCapWorkerComplete")
    }

    private fun allowedTarget(source: String): HttpUrl? {
        val endpoint = endpointUrl ?: return null
        val target = source.toHttpUrlOrNull() ?: return null
        val basePath = endpoint.encodedPath.trimEnd('/') + "/"
        val operation = target.encodedPath.removePrefix(basePath)
        return target.takeIf {
            it.isHttps && it.host == endpoint.host && it.port == endpoint.port &&
                it.username.isEmpty() && it.password.isEmpty() && it.fragment == null &&
                operation in setOf("challenge", "redeem")
        }
    }

    private fun errorResult(message: String, detail: String = ""): JSONObject = JSONObject()
        .put("error", message)
        .apply { if (detail.isNotBlank()) put("detail", detail) }

    private fun networkFailureDetail(error: IOException): String {
        var cause: Throwable = error
        while (cause.cause != null && cause.cause !== cause) cause = cause.cause!!
        val message = cause.message.orEmpty().replace(Regex("https?://\\S+"), "远端地址")
            .take(160)
        return cause.javaClass.simpleName + message.takeIf(String::isNotBlank)?.let { ": $it" }.orEmpty()
    }

    private fun complete(requestId: String, result: JSONObject) {
        if (closed.get()) return
        complete(requestId, result, "__lsbCapComplete")
    }

    private fun complete(requestId: String, result: JSONObject, callback: String) {
        evaluateJavascript(
            "window.$callback&&window.$callback(" +
                "${JSONObject.quote(requestId)},${result});",
        )
    }

    override fun close() {
        if (!closed.compareAndSet(false, true)) return
        calls.toTypedArray().forEach(Call::cancel)
        calls.clear()
        solver.shutdownNow()
        requestScheduler.shutdownNow()
    }

    companion object {
        const val NAME = "LsbCapNetwork"
        private const val MAX_CHALLENGE_RETRIES = 2
        private val RETRY_DELAYS_MS = longArrayOf(250, 750)
    }
}
