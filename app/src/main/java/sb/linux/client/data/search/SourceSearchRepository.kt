package sb.linux.client.data.search

import sb.linux.client.data.LsbClient
import sb.linux.client.domain.search.SearchPage
import sb.linux.client.domain.search.SearchQuery
import sb.linux.client.data.parser.SearchPageParser
import sb.linux.client.domain.search.SearchRepository

/** Retrieves and parses the server-rendered linux.sb Meilisearch page. */
class SourceSearchRepository(private val client: LsbClient) : SearchRepository {
    override suspend fun search(query: SearchQuery): SearchPage {
        val normalized = query.normalized()
        require(normalized.normalizedText.length >= 2) { "关键词至少 2 个字符" }
        val response = client.get(SearchRequestUrl.pathFor(normalized))
        if (response.url.contains("/login")) error("请先登录后再搜索")
        return SearchPageParser.parse(response.html, normalized)
    }
}
