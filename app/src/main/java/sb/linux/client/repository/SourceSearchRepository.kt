package sb.linux.client.repository

import sb.linux.client.data.LsbClient
import sb.linux.client.data.SearchPage
import sb.linux.client.data.SearchQuery
import sb.linux.client.data.parser.SearchPageParser

/** Retrieves and parses the server-rendered linux.sb Meilisearch page. */
class SourceSearchRepository(private val client: LsbClient) : SearchRepository {
    override suspend fun search(query: SearchQuery): SearchPage {
        val normalized = query.normalized()
        require(normalized.normalizedText.length >= 2) { "关键词至少 2 个字符" }
        val response = client.get(normalized.toPath())
        if (response.url.contains("/login")) error("请先登录后再搜索")
        return SearchPageParser.parse(response.html, normalized)
    }
}
