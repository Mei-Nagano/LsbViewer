package sb.linux.client.data

import java.net.URI
import java.net.URLDecoder
import sb.linux.client.common.network.Endpoints

/** 只跟随源站提供的同用户、同标签分页；相同内容的不同记录不去重。 */
internal class SourceRecordPages(private val client: LsbClient, private val identity: () -> Long) {
    suspend fun load(userId: Long, tab: String,
        onPage: suspend (LsbClient.Resp, Int, Int) -> Boolean = { _, _, _ -> true }): List<LsbClient.Resp> {
        require(userId > 0 && tab in setOf("points_rewards", "notifications"))
        val version = identity()
        val path = "/user/$userId"
        val pending = sortedMapOf(1 to "$path?tab=$tab")
        val visited = mutableSetOf<Int>()
        val responses = mutableListOf<LsbClient.Resp>()
        while (pending.isNotEmpty()) {
            check(visited.size < 1000) { "积分或通知历史过长，暂时无法完整读取" }
            val next = pending.entries.first()
            val page = next.key
            check(page == visited.size + 1) { "源站记录分页不连续，请重试" }
            val url = next.value
            pending.remove(page)
            if (!visited.add(page)) continue
            check(identity() == version) { "账号已切换，请重新加载" }
            val response = client.get(url)
            check(identity() == version) { "账号已切换，请重新加载" }
            val destination = URI(response.url)
            val params = query(destination)
            check(Endpoints.isInternal(destination.host) && destination.path.trimEnd('/') == path &&
                params["tab"] == tab && (params["p"]?.toIntOrNull() ?: 1) == page) {
                "记录页面不可访问，请确认登录状态与隐私设置"
            }
            val doc = SourceHtmlSupport.sourceDocument(response.html)
            if (doc.selectFirst(".pagination-bar") != null) {
                check(TopicListParser.parsePagination(doc).first == page) { "源站记录分页未正确切换，请重试" }
            }
            responses += response
            doc.select(".pagination-bar a[href], .pagination a[href], a[rel=next][href]").forEach { link ->
                val uri = runCatching { destination.resolve(link.attr("href")) }.getOrNull() ?: return@forEach
                val fields = query(uri)
                val number = fields["p"]?.toIntOrNull() ?: return@forEach
                if (uri.scheme == "https" && Endpoints.isInternal(uri.host) && uri.path.trimEnd('/') == path &&
                    fields["tab"] == tab && number > 0 && number !in visited) pending[number] = uri.toString()
            }
            val totalPages = maxOf(TopicListParser.parsePagination(doc).second, page, pending.keys.maxOrNull() ?: page)
            if (!onPage(response, page, totalPages)) break
            check(pending.isNotEmpty() || page >= totalPages) { "源站记录分页缺失，请重试" }
        }
        return responses
    }

    private fun query(uri: URI): Map<String, String> = uri.rawQuery.orEmpty().split('&').mapNotNull { field ->
        val parts = field.split('=', limit = 2)
        if (parts.size != 2) null else runCatching {
            URLDecoder.decode(parts[0], "UTF-8") to URLDecoder.decode(parts[1], "UTF-8")
        }.getOrNull()
    }.toMap()
}
