package sb.linux.client.data.search

import sb.linux.client.data.LsbClient
import sb.linux.client.domain.search.SearchPage
import sb.linux.client.domain.search.SearchQuery
import sb.linux.client.data.parser.SearchPageParser
import sb.linux.client.domain.search.SearchRepository
import sb.linux.client.domain.search.SearchCharge
import sb.linux.client.domain.search.SearchConfirmationRequired
import org.jsoup.Jsoup
import sb.linux.client.data.parser.SearchChargeParser
import sb.linux.client.domain.search.SearchScope
import java.net.URI
import java.net.URLDecoder

/** Retrieves and parses the server-rendered linux.sb Meilisearch page. */
class SourceSearchRepository(private val client: LsbClient) : SearchRepository {
    override suspend fun loadCharge(scope: SearchScope): SearchCharge {
        val response = client.get("/search?scope=${scope.value}")
        checkResponse(response)
        return SearchChargeParser.parse(Jsoup.parse(response.html)) ?: error("无法读取原站搜索费用")
    }

    override suspend fun search(query: SearchQuery, confirmedCost: Int?): SearchPage {
        val normalized = query.normalized()
        require(normalized.normalizedText.length >= 2) { "关键词至少 2 个字符" }
        require(normalized.normalizedText.length <= 120) { "关键词最多 120 个字符" }
        val response = if (normalized.access.isNotBlank()) {
            client.get(SearchRequestUrl.pathFor(normalized))
        } else {
            val initial = client.get("/search?scope=${normalized.scope.value}")
            checkResponse(initial)
            val initialDocument = Jsoup.parse(initial.html)
            val form = initialDocument.selectFirst("form.meilisearch-search-form")
                ?: error("未找到原站搜索表单")
            check(form.attr("method").equals("post", true) && form.attr("action") == "/meilisearch_search") {
                "原站搜索表单已变化，请更新应用"
            }
            val charge = SearchChargeParser.parse(initialDocument) ?: error("无法读取原站搜索费用")
            val cost = charge.cost
            val balance = charge.balance
            check(cost >= 0 && balance >= cost) { "积分不足，本次搜索需要 $cost 积分" }
            if (cost > 0 && (confirmedCost == null || confirmedCost != cost)) {
                throw SearchConfirmationRequired(charge)
            }
            val fields = form.select("input[type=hidden][name]")
                .associate { it.attr("name") to it.attr("value") }.toMutableMap()
            check(!fields["_csrf"].isNullOrBlank()) { "搜索验证信息缺失，请重新登录" }
            fields["q"] = normalized.normalizedText
            fields["scope"] = normalized.scope.value
            fields["charge_confirmed"] = "1"
            client.postForm(form.attr("action"), fields)
        }
        checkResponse(response)
        val access = URI(response.url).rawQuery.orEmpty().split('&')
            .firstOrNull { it.startsWith("access=") }?.substringAfter('=')
            ?.let { URLDecoder.decode(it, "UTF-8") }.orEmpty()
        check(access.isNotBlank()) { "搜索授权失效，请重新搜索并确认费用" }
        val authorized = normalized.copy(access = access)
        val resultResponse = if (normalized.access.isBlank() &&
            (normalized.sort != sb.linux.client.domain.search.SearchSort.RELEVANCE || normalized.page > 1)) {
            client.get(SearchRequestUrl.pathFor(authorized))
        } else response
        checkResponse(resultResponse)
        val document = Jsoup.parse(resultResponse.html)
        check(document.selectFirst(".meilisearch-search-summary") != null) {
            "原站未返回搜索结果，授权可能已过期，请重新搜索"
        }
        return SearchPageParser.parse(resultResponse.html, authorized)
    }

    private fun checkResponse(response: LsbClient.Resp) {
        if (response.url.contains("/login")) error("请先登录后再搜索")
        check(response.code in 200..299) { "搜索请求失败（${response.code}）" }
    }
}
