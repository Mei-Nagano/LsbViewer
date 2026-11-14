package sb.linux.client.data

import org.jsoup.nodes.Element
import java.net.URLEncoder

/** 用户关系只使用源站提供的 ID 表单，禁止改写关键词过滤的旧用户名名单。 */
internal class UnifiedUserBlockService(private val client: LsbClient) {
    suspend fun isBlocked(userId: Long): Boolean = removalForm(client.get(PATH), userId) != null

    suspend fun toggle(userId: Long, username: String): Boolean {
        val original = client.get(PATH)
        val removal = removalForm(original, userId)
        val response = if (removal != null) original else
            client.get("/index.php?a=user_block&q=${URLEncoder.encode(username, "UTF-8")}")
        val form = removal ?: forms(response).firstOrNull { target(it) == userId }
            ?: error("源站未提供此用户的屏蔽操作，请到用户屏蔽名单核对")
        val model = AccountManagementParser.parseForm(form, response.url)
        val button = model.buttons.firstOrNull { it.enabled } ?: error("源站当前不允许屏蔽此用户")
        val fields = model.hidden + if (button.name.isNotBlank()) listOf(button.name to button.value) else emptyList()
        AccountManagementService(client).submit(PATH, model, fields)
        val blocked = isBlocked(userId)
        check(blocked == (removal == null)) { "源站未确认屏蔽状态，请重新加载名单" }
        return blocked
    }

    private fun removalForm(response: LsbClient.Resp, userId: Long): Element? = forms(response).firstOrNull { form ->
        target(form) == userId && form.select("button,input[type=submit]").any {
            Regex("取消|解除|移除").containsMatchIn(it.text().ifBlank { it.attr("value") })
        }
    }
    private fun forms(response: LsbClient.Resp): List<Element> {
        check(!response.url.contains("/login")) { "登录已失效" }
        return SourceHtmlSupport.sourceDocument(response.html).select(".user-block-page form")
            .filter { it.selectFirst("input[name=blocked_user_id]") != null }
    }
    private fun target(form: Element): Long? = form.selectFirst("input[name=blocked_user_id]")?.attr("value")?.toLongOrNull()
    private companion object { const val PATH = "/user_block" }
}
