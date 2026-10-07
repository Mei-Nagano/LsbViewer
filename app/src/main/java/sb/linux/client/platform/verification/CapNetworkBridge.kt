package sb.linux.client.platform.verification

import android.webkit.JavascriptInterface
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import org.json.JSONObject
import java.io.Closeable
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import sb.linux.client.domain.account.CapVerificationNetworkRepository
import sb.linux.client.domain.account.CapVerificationNetworkRequest
import sb.linux.client.domain.account.CapVerificationNetworkResponse

/** 将 CAP 的动态接口请求转交给支持 DoH 的 OkHttp，并严格限制到当前验证码端点。 */
internal class CapNetworkBridge(
    private val network: CapVerificationNetworkRepository,
    endpoint: String,
    private val pageUrl: String,
    private val evaluateJavascript: (String) -> Unit,
) : Closeable {
    private val endpointUrl = endpoint.toHttpUrlOrNull()
    private val pageOrigin = pageUrl.toHttpUrlOrNull()?.let {
        val port = if ((it.isHttps && it.port == 443) || (!it.isHttps && it.port == 80)) "" else ":${it.port}"
        "${it.scheme}://${it.host}$port"
    }.orEmpty()
    private val calls = ConcurrentHashMap.newKeySet<Closeable>()
    private val closed = AtomicBoolean(false)
    private val solver = Executors.newFixedThreadPool(8)
    private val requestScheduler: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor()
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
        val replayable = target.encodedPath.endsWith("/challenge")
        val headers = buildMap {
            put("Accept", "application/json")
            if (pageOrigin.isNotBlank()) put("Origin", pageOrigin)
            if (pageUrl.isNotBlank()) put("Referer", pageUrl)
        }
        val networkRequest = CapVerificationNetworkRequest(
            url = target.toString(),
            headers = headers,
            contentType = contentType?.toString(),
            body = body,
            replayable = replayable,
        )
        enqueueRequest(requestId, networkRequest, replayable)
    }

    /** challenge 没有副作用，连接建立失败时可以重新请求；redeem 只允许提交一次。 */
    private fun enqueueRequest(
        requestId: String,
        request: CapVerificationNetworkRequest,
        retryable: Boolean,
        attempt: Int = 0,
    ) {
        if (closed.get()) return
        val callLock = Any()
        var activeCall: Closeable? = null
        var completed = false
        val call = network.enqueue(request) { result ->
            synchronized(callLock) {
                completed = true
                activeCall?.let(calls::remove)
            }
            if (closed.get()) return@enqueue
            result.fold(
                onSuccess = { completeResponse(requestId, it) },
                onFailure = { handleFailure(requestId, request, retryable, attempt, it) },
            )
        }
        synchronized(callLock) {
            activeCall = call
            if (!completed) calls += call
        }
        if (closed.get()) {
            calls -= call
            call.close()
        }
    }

    private fun completeResponse(requestId: String, response: CapVerificationNetworkResponse) {
        complete(
            requestId,
            JSONObject()
                .put("status", response.status)
                .put("body", response.body)
                .put("contentType", response.contentType),
        )
    }

    private fun handleFailure(
        requestId: String,
        request: CapVerificationNetworkRequest,
        retryable: Boolean,
        attempt: Int,
        error: Throwable,
    ) {
        if (closed.get()) return
        if (retryable && attempt < MAX_CHALLENGE_RETRIES) {
            requestScheduler.schedule(
                { enqueueRequest(requestId, request, retryable, attempt + 1) },
                RETRY_DELAYS_MS[attempt],
                TimeUnit.MILLISECONDS,
            )
        } else {
            complete(requestId, errorResult("验证码服务连接失败", networkFailureDetail(error)))
        }
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

    private fun networkFailureDetail(error: Throwable): String {
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
        calls.toTypedArray().forEach { runCatching { it.close() } }
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
