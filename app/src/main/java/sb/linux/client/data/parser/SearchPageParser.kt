package sb.linux.client.data.parser

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import sb.linux.client.data.Endpoints
import sb.linux.client.data.SearchPage
import sb.linux.client.data.SearchQuery
import sb.linux.client.data.SearchResultItem
import java.net.URI

/** Parses the server-rendered Meilisearch page without relying on its visual text order. */
object SearchPageParser {
    fun parse(html: String, requested: SearchQuery): SearchPage {
        val document = Jsoup.parse(html, Endpoints.BASE)
        val query = queryFromPage(document, requested)
        val items = if (query.scope == sb.linux.client.data.SearchScope.USER) {
            parseUsers(document)
        } else {
            parseTopics(document)
        }
        val pagination = parsePagination(document, query.page)
        return SearchPage(
            query = query,
            totalCount = parseTotalCount(document),
            items = items,
            hasPrevious = pagination.first,
            hasNext = pagination.second,
        )
    }

    private fun queryFromPage(document: Document, requested: SearchQuery): SearchQuery {
        val form = document.selectFirst("form.meilisearch-search-form")
        val text = form?.selectFirst("input[name=q]")?.attr("value")?.ifBlank { null }
            ?: requested.normalizedText
        val scope = form?.selectFirst("input[name=scope]")?.attr("value")
            ?.let { sb.linux.client.data.SearchScope.fromValue(it) } ?: requested.scope
        val sort = document.selectFirst("select[name=sort] option[selected]")?.attr("value")
            ?.let { sb.linux.client.data.SearchSort.fromValue(it) } ?: requested.sort
        return requested.copy(
            text = text,
            scope = scope,
            sort = sort,
            page = parseCurrentPage(document) ?: requested.normalizedPage,
        )
    }

    private fun parseTopics(document: Document): List<SearchResultItem.Topic> =
        document.select("li.meilisearch-search-result").mapNotNull { item ->
            val link = item.selectFirst("a.meilisearch-search-result-title") ?: return@mapNotNull null
            val topicId = idFrom(link.attr("href"))
            if (topicId <= 0) return@mapNotNull null
            val meta = item.select(".meilisearch-search-result-meta > span").map { it.text().trim() }
            SearchResultItem.Topic(
                topicId = topicId,
                title = link.text().trim(),
                snippet = item.selectFirst(".meilisearch-search-result-snippet")?.text()?.trim().orEmpty(),
                matchedBy = meta.getOrNull(0).orEmpty(),
                forumName = meta.getOrNull(1).orEmpty(),
                timeText = meta.getOrNull(2).orEmpty(),
                replies = numberFrom(meta.firstOrNull { it.contains("条回复") }),
                views = numberFrom(meta.firstOrNull { it.contains("次浏览") }),
            )
        }

    private fun parseUsers(document: Document): List<SearchResultItem.User> =
        document.select("li.meilisearch-search-user-result").mapNotNull { item ->
            val link = item.selectFirst("a.meilisearch-search-user-link") ?: return@mapNotNull null
            val userId = idFrom(link.attr("href"))
            if (userId <= 0) return@mapNotNull null
            SearchResultItem.User(
                userId = userId,
                username = item.selectFirst(".meilisearch-search-user-main strong")?.text()?.trim().orEmpty(),
                avatarUrl = absoluteUrl(item.selectFirst("img")?.attr("src").orEmpty()),
                meta = item.selectFirst(".meilisearch-search-user-meta")?.text()?.trim().orEmpty(),
            )
        }

    private fun parseTotalCount(document: Document): Int {
        val summary = document.selectFirst(".meilisearch-search-summary")?.text().orEmpty()
        return Regex("([\\d,]+)\\s*个(?:主题|用户)")
            .find(summary)?.groupValues?.get(1)?.replace(",", "")?.toIntOrNull() ?: 0
    }

    private fun parsePagination(document: Document, fallbackPage: Int): Pair<Boolean, Boolean> {
        val pagination = document.selectFirst("nav.meilisearch-search-pagination") ?: return false to false
        val current = parseCurrentPage(document) ?: fallbackPage
        val hasPrevious = pagination.select("a").any { it.text().contains("上一页") && current > 1 }
        val hasNext = pagination.select("a").any {
            it.text().contains("下一页") && pageFrom(it.attr("href"))?.let { next -> next > current } == true
        }
        return hasPrevious to hasNext
    }

    private fun parseCurrentPage(document: Document): Int? =
        document.selectFirst("nav.meilisearch-search-pagination span")?.text()
            ?.let { Regex("第\\s*(\\d+)\\s*页").find(it)?.groupValues?.get(1)?.toIntOrNull() }

    private fun pageFrom(href: String): Int? =
        runCatching { URI(href).query.orEmpty() }
            .getOrNull()?.split('&')?.firstNotNullOfOrNull {
                val (key, value) = it.split('=', limit = 2).let { pair -> pair.firstOrNull().orEmpty() to pair.getOrNull(1).orEmpty() }
                value.toIntOrNull().takeIf { key == "p" }
            }

    private fun numberFrom(value: String?): Int =
        value?.replace(",", "")?.let { Regex("\\d+").find(it)?.value?.toIntOrNull() } ?: 0

    private fun idFrom(href: String): Long =
        Regex("/(?:topic|user)/(\\d+)").find(href)?.groupValues?.get(1)?.toLongOrNull() ?: 0L

    private fun absoluteUrl(url: String): String = when {
        url.startsWith("http") -> url
        url.startsWith("//") -> "https:$url"
        else -> Endpoints.abs(url)
    }
}
