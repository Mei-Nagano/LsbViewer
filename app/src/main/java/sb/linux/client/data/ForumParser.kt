package sb.linux.client.data
import sb.linux.client.domain.community.ForumInfo

/** 源站版块目录和版块页头解析。 */
object ForumParser {
    fun parseForumList(html: String): List<ForumInfo> {
        val d = SourceHtmlSupport.sourceDocument(html)
        val out = mutableListOf<ForumInfo>()
        d.select("a[href^=/forum/]").forEach { a ->
            val id = SourceHtmlSupport.idFrom(a.attr("href"))
            if (id > 0 && a.text().isNotBlank()) {
                val name = a.selectFirst(
                    ".forum-enhancements-name, .forum-enhancements-sidebar-name, .forum-name"
                )?.text()?.trim() ?: a.text().trim()
                val count = a.selectFirst(
                    ".forum-enhancements-count, .forum-enhancements-sidebar-count"
                )?.text()?.trim() ?: ""
                val existing = out.find { it.id == id }
                if (existing == null) {
                    out.add(ForumInfo(id, name, a.attr("title") ?: "", count))
                }
            }
        }
        if (out.isEmpty()) {
            // 论坛列表页失效时，从首页顶部/移动菜单兜底
            d.select(".forum-more-panel a[href^=/forum/], .mobile-menu-link[href^=/forum/]").forEach { a ->
                val id = SourceHtmlSupport.idFrom(a.attr("href"))
                if (id > 0 && a.text().isNotBlank() && out.none { it.id == id }) {
                    out.add(ForumInfo(id, a.text().trim(), a.attr("title") ?: ""))
                }
            }
        }
        return out.distinctBy { it.id }
    }

    fun parseForumHeader(html: String): Triple<String, String, Int> {
        val d = SourceHtmlSupport.sourceDocument(html)
        val h = d.selectFirst(".main-panel h1, .main-panel h2, h1")?.text() ?: ""
        val desc = d.selectFirst(".forum-description, .main-panel p")?.text() ?: ""
        val topics = Regex("""(\d+)\s*个?主题""").find(d.selectFirst(".main-panel")?.text() ?: "")
            ?.groupValues?.get(1)?.toIntOrNull() ?: 0
        return Triple(h, desc, topics)
    }

}
