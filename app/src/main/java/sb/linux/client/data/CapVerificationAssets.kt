package sb.linux.client.data

import android.util.Base64
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request

import sb.linux.client.common.error.LsbException

/** 通过应用统一网络层下载 CAP 运行资源，避免 WebView 在 DoH 下自行解析域名。 */
internal object CapVerificationAssets {
    private const val MAX_SCRIPT_BYTES = 512 * 1024
    private const val MAX_WASM_BYTES = 2 * 1024 * 1024

    fun load(client: OkHttpClient, verification: LoginVerification.Cap): LoginVerification.Cap {
        val endpoint = verification.endpoint.toHttpUrlOrNull()
            ?: throw LsbException("验证码服务地址无效")
        val script = download(client, endpoint, verification.scriptUrl, MAX_SCRIPT_BYTES)
            .toString(Charsets.UTF_8)
        if (script.isBlank()) throw LsbException("验证码组件内容为空")

        val wasmDataUrl = verification.wasmUrl.takeIf(String::isNotBlank)?.let { url ->
            val bytes = download(client, endpoint, url, MAX_WASM_BYTES)
            "data:application/wasm;base64,${Base64.encodeToString(bytes, Base64.NO_WRAP)}"
        }.orEmpty()
        return verification.copy(widgetScript = script, wasmDataUrl = wasmDataUrl)
    }

    private fun download(
        client: OkHttpClient,
        endpoint: HttpUrl,
        source: String,
        maxBytes: Int,
    ): ByteArray {
        val url = source.toHttpUrlOrNull()?.takeIf {
            it.isHttps && it.host == endpoint.host && it.port == endpoint.port &&
                it.username.isEmpty() && it.password.isEmpty()
        } ?: throw LsbException("验证码资源地址不受信任")
        val request = Request.Builder().url(url).build()
        return try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) throw LsbException("验证码资源下载失败（${response.code}）")
                val declaredSize = response.body?.contentLength() ?: -1L
                if (declaredSize > maxBytes) throw LsbException("验证码资源大小异常")
                response.body?.bytes()?.also {
                    if (it.isEmpty() || it.size > maxBytes) throw LsbException("验证码资源大小异常")
                } ?: throw LsbException("验证码资源下载失败")
            }
        } catch (error: LsbException) {
            throw error
        } catch (_: Exception) {
            throw LsbException("验证码资源下载超时，请重试")
        }
    }
}
