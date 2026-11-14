package sb.linux.client.data

import org.jsoup.nodes.Document
import sb.linux.client.common.network.Endpoints
import sb.linux.client.domain.account.AccountNode

/** 源站按需加载的帖子底部操作；只暴露红包，其余能力继续使用已有原生面板。 */
internal object TopicSourceActionsParser {
    fun panels(doc: Document): List<AccountNode.Link> = doc.select("[data-topic-footer-action=red_packet][data-panel-url]")
        .mapNotNull { panel ->
            val url = AccountManagementParser.resolve(Endpoints.BASE, panel.attr("data-panel-url"))
            if (url.isBlank()) null else AccountNode.Link(panel.selectFirst("summary")?.text().orEmpty().ifBlank { "追加红包" }, url)
        }
}
