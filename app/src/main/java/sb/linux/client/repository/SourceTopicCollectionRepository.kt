package sb.linux.client.repository

import sb.linux.client.common.collection.TopicCollectionActionForm
import sb.linux.client.common.collection.TopicCollectionDetail
import sb.linux.client.common.collection.TopicCollectionListPage
import sb.linux.client.common.collection.TopicCollectionManagePage
import sb.linux.client.common.collection.TopicCollectionPicker
import sb.linux.client.common.collection.TopicCollectionTab
import sb.linux.client.data.HtmlParser
import sb.linux.client.data.LsbClient
import sb.linux.client.data.parser.TopicCollectionParser

/** Executes only same-origin, source-discovered forms and parses their authoritative HTML. */
class SourceTopicCollectionRepository(private val client: LsbClient) : TopicCollectionRepository {
    override suspend fun list(tab: TopicCollectionTab): TopicCollectionListPage {
        val response = client.get("/topic_collections?tab=${tab.queryValue}")
        if (response.url.contains("/login")) error("请先登录后查看我的淘帖")
        return TopicCollectionParser.parseList(response.html, tab, response.url)
    }

    override suspend fun detail(path: String): TopicCollectionDetail {
        require(DETAIL_PATH.matches(path)) { "无效的淘帖详情路径" }
        val response = client.get(path)
        if (response.url.contains("/login")) error("请先登录")
        return TopicCollectionParser.parseDetail(response.html, response.url)
    }

    override suspend fun picker(topicPath: String): TopicCollectionPicker? {
        require(TOPIC_PATH.matches(topicPath)) { "无效的主题路径" }
        val response = client.get(topicPath)
        if (response.url.contains("/login")) error("请先登录")
        return TopicCollectionParser.parsePicker(response.html)
    }

    override suspend fun manage(path: String): TopicCollectionManagePage {
        require(MANAGE_PATH.matches(path)) { "无效的淘帖管理路径" }
        val response = client.get(path)
        if (response.url.contains("/login")) error("请先登录")
        val document = org.jsoup.Jsoup.parse(response.html)
        return TopicCollectionManagePage(
            title = document.selectFirst("h1, h2")?.text()?.trim().orEmpty().ifBlank { "淘帖管理" },
            actions = TopicCollectionParser.parseActions(document),
        )
    }

    override suspend fun submit(form: TopicCollectionActionForm) {
        require(form.method.equals("post", true)) { "源站淘帖操作不是 POST，协议可能已更新" }
        require(SAME_ORIGIN_PATH.matches(form.action)) { "拒绝提交外部表单" }
        val csrf = client.csrf(forceRefresh = true)
        val fields = form.fields.filterNot { it.first == "_csrf" } + ("_csrf" to csrf)
        val response = client.postFormPairs(form.action, fields)
        if (response.url.contains("/login")) error("登录已失效")
        if (response.code !in 200..399) error(HtmlParser.extractError(response.html).ifBlank { "源站操作失败（${response.code}）" })
        val sourceError = HtmlParser.extractError(response.html)
        if (sourceError.isNotBlank()) error(sourceError)
    }

    private companion object {
        val DETAIL_PATH = Regex("^/topic_collection/\\d+(?:\\?p=\\d+)?$")
        val TOPIC_PATH = Regex("^/topic/\\d+(?:\\?p=\\d+)?$")
        val MANAGE_PATH = Regex("^/topic_collection_manage(?:/\\d+)?(?:\\?.*)?$")
        val SAME_ORIGIN_PATH = Regex("^/[A-Za-z0-9_./?=&%+\\-]+$")
    }
}
