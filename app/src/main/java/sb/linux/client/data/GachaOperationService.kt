package sb.linux.client.data

import sb.linux.client.domain.gacha.GachaOperationPage
import sb.linux.client.domain.gacha.GachaOperationRepository

internal class GachaOperationService(private val client: LsbClient) : GachaOperationRepository {
    override suspend fun load(path: String): GachaOperationPage {
        val response = client.get(path)
        check(!response.url.contains("login")) { "请先登录" }
        return GachaOperationHtmlParser.parseGachaOperationPage(response.html)
    }

    override suspend fun submit(path: String, fields: List<Pair<String, String>>): String? {
        val pairs = fields.filterNot { it.first == "_csrf" }.toMutableList()
        pairs.add(0, "_csrf" to client.csrf())
        val response = client.postFormPairs(path, pairs)
        check(!response.url.contains("login")) { "登录已失效" }
        return if (response.url.contains("form_error")) {
            HtmlTextParser.extractError(response.html)
        } else {
            null
        }
    }
}
