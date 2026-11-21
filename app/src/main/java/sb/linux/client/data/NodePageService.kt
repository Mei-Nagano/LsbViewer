package sb.linux.client.data

import org.jsoup.Jsoup
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import sb.linux.client.common.network.Endpoints
import sb.linux.client.domain.community.NodeLink
import sb.linux.client.domain.community.NodePage
import sb.linux.client.domain.community.NodeRepository
import sb.linux.client.domain.community.NodeSummary
import java.net.URI

internal class NodePageService(private val client: LsbClient) : NodeRepository {
    override suspend fun load(url: String): NodePage {
        val target = URI(Endpoints.abs(url))
        require(Endpoints.isForumHost(target.host) &&
            (target.path == "/nodes" || Regex("/node/[0-9]+").matches(target.path))) { "节点地址无效" }
        val response = client.get(target.toString())
        if (URI(response.url).path == "/login") {
            return NodePage(target.toString(), "节点中心", "", "", emptyList(), emptyList(), emptyList(),
                1, 1, "登录后查看节点", true)
        }
        return NodePageParser.parse(response.html, response.url)
    }
}

internal object NodePageParser {
    fun parse(html: String, url: String): NodePage {
        val document = Jsoup.parse(html, url)
        val panel = document.selectFirst(".forum-main > .main-panel") ?: error("未找到节点内容")
        val header = panel.selectFirst(".node-node-head")
        val tabs = panel.select(".node-tabs a[href]").map {
            NodeLink(it.text(), Endpoints.abs(it.absUrl("href")), it.hasClass("active"))
        }
        val nodes = panel.select(".node-node-list > .node-node-row").mapNotNull { item ->
            val link = item.selectFirst("a.post-title[href]") ?: return@mapNotNull null
            val target = link.absUrl("href").toHttpUrlOrNull() ?: return@mapNotNull null
            if (!Endpoints.isForumHost(target.host) || !Regex("/node/[0-9]+").matches(target.encodedPath)) return@mapNotNull null
            NodeSummary(
                Endpoints.abs(target.toString()), link.text(), item.selectFirst(".node-card-desc")?.text().orEmpty(),
                item.selectFirst(".post-meta")?.children()?.joinToString(" · ") { it.text() }.orEmpty(),
                item.select(".node-tag").joinToString(" · ") { it.text() },
                item.selectFirst("img.avatar-img")?.absUrl("src")?.let(Endpoints::abs).orEmpty(),
            )
        }
        // 只把节点主题列表交给帖子解析器，节点卡片中的 /node/ID 不是帖子 ID。
        val topics = TopicListParser.parseTopicList(panel.selectFirst(".node-topic-list")?.outerHtml().orEmpty()).first
        val pagination = TopicListParser.parsePagination(document)
        val empty = panel.selectFirst(".empty-state")
        return NodePage(
            url = url,
            title = header?.selectFirst("h1")?.text().orEmpty().ifBlank { "节点中心" },
            description = header?.selectFirst(".node-node-description")?.text().orEmpty(),
            meta = header?.selectFirst(".node-node-meta")?.children()?.joinToString(" · ") { it.text() }.orEmpty(),
            tabs = tabs,
            nodes = nodes,
            topics = topics,
            page = pagination.first,
            totalPages = pagination.second,
            emptyMessage = empty?.text().orEmpty().ifBlank { "暂无内容" },
            loginRequired = empty?.selectFirst("a[href='/login']") != null,
        )
    }
}
