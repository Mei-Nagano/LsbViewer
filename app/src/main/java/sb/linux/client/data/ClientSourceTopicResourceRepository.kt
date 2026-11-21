package sb.linux.client.data

import okhttp3.Request
import sb.linux.client.domain.topic.SourceTopicResource
import sb.linux.client.domain.topic.SourceTopicResourceRepository
import sb.linux.client.domain.topic.SourceResourceVerificationRequired
import sb.linux.client.common.network.Endpoints
import java.io.ByteArrayInputStream

/** 展示资源使用帖子请求的 Cookie、DoH、代理和 Cronet 回退。 */
internal class ClientSourceTopicResourceRepository(private val client: LsbClient) : SourceTopicResourceRepository {
    override suspend fun recoverVerification(challenge: SourceResourceVerificationRequired): Boolean =
        client.recoverAccess(challenge.url, challenge.html)

    override fun load(url: String, headers: Map<String, String>): SourceTopicResource {
        val request = Request.Builder().url(url)
            .tag(ImageTraffic::class.java, ImageTraffic)
            .apply {
                // Cookie/UA/压缩由共享客户端管理，不能用 WebView 的旧会话覆盖。
                for (name in listOf("Accept", "Accept-Language", "Referer", "Origin", "Range", "Cache-Control")) {
                    headers.entries.firstOrNull { it.key.equals(name, true) }?.let { header(it.key, it.value) }
                }
            }.build()
        val response = client.http.newCall(request).execute()
        val body = response.body
        val type = body?.contentType()
        if (Endpoints.isForumHost(response.request.url.host) &&
            (response.header("cf-mitigated") == "challenge" || type?.subtype == "html")) {
            val html = try { response.peekBody(64 * 1024).string() }
            catch (error: java.io.IOException) { response.close(); throw error }
            if (response.header("cf-mitigated") == "challenge" || ChallengePagePolicy.isChallenge(html)) {
                val target = response.request.url.toString()
                response.close()
                throw SourceResourceVerificationRequired(target, html)
            }
        }
        // OkHttp/Cronet 已解压；不可将原始传输编码和长度再交给 WebView。
        val responseHeaders = response.headers.names()
            .filterNot { it.lowercase() in setOf("content-encoding", "content-length", "transfer-encoding", "connection", "set-cookie") }
            .associateWith { response.header(it).orEmpty() }
        if (response.code in 300..399) {
            response.close()
            throw java.io.IOException("资源重定向未完成")
        }
        return SourceTopicResource(
            mimeType = type?.let { "${it.type}/${it.subtype}" } ?: "application/octet-stream",
            encoding = type?.charset()?.name(),
            status = response.code,
            reason = response.message.ifBlank { "HTTP ${response.code}" },
            headers = responseHeaders,
            body = body?.byteStream() ?: ByteArrayInputStream(ByteArray(0)).also { response.close() },
        )
    }
}
