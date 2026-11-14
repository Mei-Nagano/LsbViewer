package sb.linux.client.data

import java.net.URI
import sb.linux.client.common.network.Endpoints
import sb.linux.client.domain.linkpreview.ExternalJumpRepository

internal class ExternalJumpService(private val client: LsbClient) : ExternalJumpRepository {
    override suspend fun permittedTarget(url: String): String? {
        val uri = URI(url)
        if (!Endpoints.isInternal(uri.host) || uri.path.trimEnd('/') != "/jump") return null
        val response = client.get(url)
        if (response.code !in 200..299) return null
        val card = SourceHtmlSupport.sourceDocument(response.html).selectFirst("[data-link-jump-card]") ?: return null
        if (card.attr("data-link-jump-blocked") !in setOf("", "0")) return null
        val target = card.attr("data-link-jump-url")
        val destination = runCatching { URI(target) }.getOrNull() ?: return null
        return target.takeIf { destination.scheme in setOf("https", "http") && !destination.host.isNullOrBlank() && destination.userInfo == null }
    }
}
