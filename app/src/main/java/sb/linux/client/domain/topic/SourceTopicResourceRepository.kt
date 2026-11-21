package sb.linux.client.domain.topic

import java.io.InputStream

/** Called on WebView's resource thread; the caller owns and closes the response stream. */
interface SourceTopicResourceRepository {
    fun load(url: String, headers: Map<String, String>): SourceTopicResource
    suspend fun recoverVerification(challenge: SourceResourceVerificationRequired): Boolean
}

/** 资源返回访问验证时，交给应用的交互验证流程，不能作为脚本交给 WebView。 */
class SourceResourceVerificationRequired(val url: String, val html: String) :
    java.io.IOException("源站资源需要完成安全验证")

data class SourceTopicResource(
    val mimeType: String,
    val encoding: String?,
    val status: Int,
    val reason: String,
    val headers: Map<String, String>,
    val body: InputStream,
)
