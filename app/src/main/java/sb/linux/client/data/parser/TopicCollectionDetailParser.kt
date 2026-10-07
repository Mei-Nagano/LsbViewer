package sb.linux.client.data.parser
import sb.linux.client.common.network.Endpoints
import org.jsoup.Jsoup
import sb.linux.client.common.collection.TopicCollectionDetail
import sb.linux.client.common.collection.TopicCollectionPageInfo
import sb.linux.client.common.collection.TopicCollectionSummary
import sb.linux.client.data.SourceHtmlSupport
import sb.linux.client.data.TopicListParser

object TopicCollectionDetailParser {
    fun parseDetail(html: String, sourceUrl: String = ""): TopicCollectionDetail {
        val document = Jsoup.parse(html, Endpoints.BASE)
        val title = document.selectFirst(".topic-collections-collection-title h1, .topic-collections-collection-title .post-content-title, h1")
            ?.text()?.trim().orEmpty()
        val authorLink = document.selectFirst(".topic-collections-collection-meta a[href^=/user/], .topic-collections-collection-head a[href^=/user/]")
        val meta = document.select(".topic-collections-collection-meta > span").eachText()
        val description = document.selectFirst(".topic-collections-collection-description")?.text()?.trim().orEmpty()
        val collectionId = TopicCollectionListParser.idFrom(sourceUrl)
        val summary = TopicCollectionSummary(
            collectionId = collectionId,
            title = title,
            authorId = TopicCollectionListParser.idFrom(authorLink?.attr("href").orEmpty()),
            authorName = authorLink?.text()?.trim().orEmpty(),
            articleCount = meta.firstOrNull { it.contains("篇") } ?: "",
            subscriberCount = meta.firstOrNull { it.contains("订阅") } ?: "",
            description = description,
            relation = TopicCollectionListParser.relationFrom(document),
            managePath = document.selectFirst("a[href*=/topic_collection_manage]")?.attr("href").orEmpty(),
        )
        val topics = runCatching { TopicListParser.parseTopicList(html).first }.getOrDefault(emptyList())
        return TopicCollectionDetail(
            summary = summary,
            description = description,
            subscriberCount = summary.subscriberCount,
            topics = topics,
            pageInfo = parsePageInfo(document, sourceUrl),
            managePath = summary.managePath,
            actions = TopicCollectionActionParser.parseActions(document),
        )
    }

    private fun parsePageInfo(document: org.jsoup.nodes.Document, sourceUrl: String): TopicCollectionPageInfo {
        val current = Regex("[?&]p=(\\d+)").find(sourceUrl)?.groupValues?.get(1)?.toIntOrNull() ?: 1
        val links = document.select("a[href*='p=']").mapNotNull { link ->
            val page = Regex("[?&]p=(\\d+)").find(link.attr("href"))?.groupValues?.get(1)?.toIntOrNull() ?: return@mapNotNull null
            page to link.attr("href")
        }
        return TopicCollectionPageInfo(current, links.maxOfOrNull { it.first } ?: current,
            links.firstOrNull { it.first == current - 1 }?.second.orEmpty(),
            links.firstOrNull { it.first == current + 1 }?.second.orEmpty())
    }

}

