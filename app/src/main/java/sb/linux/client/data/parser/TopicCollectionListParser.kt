package sb.linux.client.data.parser
import sb.linux.client.common.network.Endpoints
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import sb.linux.client.common.collection.TopicCollectionListPage
import sb.linux.client.common.collection.TopicCollectionRelation
import sb.linux.client.common.collection.TopicCollectionSummary
import sb.linux.client.common.collection.TopicCollectionTab

object TopicCollectionListParser {
    fun parseList(html: String, tab: TopicCollectionTab, sourceUrl: String = ""): TopicCollectionListPage {
        val document = Jsoup.parse(html, Endpoints.BASE)
        val redirectedToLogin = sourceUrl.contains("/login") || document.selectFirst("form[action*=/login]") != null
        val items = document.select("li.topic-collections-collection-row, li:has(a[href^=/topic_collection/])")
            .mapNotNull(::parseSummary)
            .distinctBy { it.collectionId }
        return TopicCollectionListPage(tab, items, sourceUrl, redirectedToLogin && items.isEmpty())
    }

    private fun parseSummary(row: Element): TopicCollectionSummary? {
        val link = row.selectFirst("a[href^=/topic_collection/]") ?: return null
        val id = idFrom(link.attr("href"))
        if (id <= 0) return null
        val author = row.selectFirst("a[href^=/user/]")
        val meta = row.select(".post-meta > span").eachText()
        val tag = row.selectFirst(".topic-collections-tag")?.text()?.trim().orEmpty()
        return TopicCollectionSummary(
            collectionId = id,
            title = link.text().trim(),
            authorId = idFrom(author?.attr("href").orEmpty()),
            authorName = author?.text()?.trim().orEmpty(),
            avatarUrl = absoluteUrl(row.selectFirst("img")?.attr("src").orEmpty()),
            visibility = tag,
            articleCount = meta.firstOrNull { it.contains("篇") || it.contains("文章") }.orEmpty(),
            subscriberCount = meta.firstOrNull { it.contains("订阅") }.orEmpty(),
            updatedText = meta.firstOrNull { it.startsWith("更新") || it.contains("前") }.orEmpty(),
            createdText = meta.firstOrNull { it.startsWith("创建") }.orEmpty(),
            description = row.selectFirst(".topic-collections-card-desc")?.text()?.trim().orEmpty(),
            relation = relationFrom(row),
            subscribed = row.select("[class*=subscribed], .topic-collections-tag-subscribed").isNotEmpty(),
            managePath = row.selectFirst("a[href*=/topic_collection_manage]")?.attr("href").orEmpty(),
        )
    }

    internal fun relationFrom(element: Element): TopicCollectionRelation {
        val text = element.text()
        return when {
            text.contains("协作") || text.contains("协作者") -> TopicCollectionRelation.COLLABORATOR
            text.contains("已订阅") -> TopicCollectionRelation.SUBSCRIBED
            text.contains("我的") || text.contains("创建者") -> TopicCollectionRelation.OWNER
            text.contains("公开") -> TopicCollectionRelation.PUBLIC
            else -> TopicCollectionRelation.UNKNOWN
        }
    }

    internal fun idFrom(value: String): Long = Regex("/(?:topic_collection|topic)/(\\d+)").find(value)?.groupValues?.get(1)?.toLongOrNull() ?: 0L
    internal fun absoluteUrl(value: String): String = when {
        value.isBlank() -> ""
        value.startsWith("http", true) -> value
        value.startsWith("//") -> "https:$value"
        else -> Endpoints.abs(value)
    }

}

