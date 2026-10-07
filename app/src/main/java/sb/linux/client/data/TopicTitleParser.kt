package sb.linux.client.data
import org.jsoup.nodes.Document

internal object TopicTitleParser {
    fun parse(document: Document): String {
        fun cleanTitle(s: String) = s
            .replace(Regex("[\\u200B\\u200C\\u200D\\uFEFF\\u00A0\\u3000\\u2060]"), " ")
            .trim()
        fun titleText(selector: String): String? = document.selectFirst(selector)?.clone()?.also {
            // 精华/抽奖/发卡状态徽章是 h1 的子节点。直接 h1.text() 会把“精华”等
            // 状态重复拼进标题；状态由 titleTag 单独展示，标题只保留文本本体。
            it.select(
                ".topic-management-featured-badge, .topic-management-detail-badge, " +
                    ".community-lottery-title-status, .virtual-card-title-status"
            ).remove()
        }?.text()
        return (titleText("h1.post-content-title")
            ?: titleText("h1.topic-title")
            ?: titleText(".main-panel h1")
            ?: titleText("h1")
            ?: run {
                // 从 toolbar 或 title 标签恢复
                val t = document.title()
                if (t.contains(" - ")) t.substringBefore(" - ") else t
            }).let(::cleanTitle)
    }
}
