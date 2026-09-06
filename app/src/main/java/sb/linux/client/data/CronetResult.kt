package sb.linux.client.data

import okhttp3.Headers
import okhttp3.HttpUrl
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.zip.GZIPInputStream

/** Cronet 单次请求的结果（30x 也算一次结果，location 非空表示需要外层跟随）。 */
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
            // Cronet 会自动解压响应体但保留 Content-Encoding 头；用 gzip 魔数判断当前内容是否仍是压缩态。
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

/** gzip 魔数（0x1f 0x8b）：区分「仍是压缩态」与「Cronet 已透明解压」。 */
private fun isGzip(bytes: ByteArray): Boolean =
    bytes.size >= 2 && bytes[0] == 0x1f.toByte() && bytes[1] == 0x8b.toByte()
