package sb.linux.client.data.parser

import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import sb.linux.client.common.collection.TopicCollectionActionForm
import sb.linux.client.common.collection.TopicCollectionDetail
import sb.linux.client.common.collection.TopicCollectionListPage
import sb.linux.client.common.collection.TopicCollectionOperation
import sb.linux.client.common.collection.TopicCollectionPageInfo
import sb.linux.client.common.collection.TopicCollectionPicker
import sb.linux.client.common.collection.TopicCollectionPickerOption
import sb.linux.client.common.collection.TopicCollectionFormField
import sb.linux.client.common.collection.TopicCollectionRelation
import sb.linux.client.common.collection.TopicCollectionSummary
import sb.linux.client.common.collection.TopicCollectionTab
import sb.linux.client.data.Endpoints
import sb.linux.client.data.HtmlParser

/** Parser for the v9 topic_collections plugin. It follows source semantics, not visual order. */
object TopicCollectionParser {
    fun parseList(html: String, tab: TopicCollectionTab, sourceUrl: String = ""): TopicCollectionListPage {
        val document = Jsoup.parse(html, Endpoints.BASE)
        val redirectedToLogin = sourceUrl.contains("/login") || document.selectFirst("form[action*=/login]") != null
        val items = document.select("li.topic-collections-collection-row, li:has(a[href^=/topic_collection/])")
            .mapNotNull(::parseSummary)
            .distinctBy { it.collectionId }
        return TopicCollectionListPage(tab, items, sourceUrl, redirectedToLogin && items.isEmpty())
    }

    fun parseDetail(html: String, sourceUrl: String = ""): TopicCollectionDetail {
        val document = Jsoup.parse(html, Endpoints.BASE)
        val title = document.selectFirst(".topic-collections-collection-title h1, .topic-collections-collection-title .post-content-title, h1")
            ?.text()?.trim().orEmpty()
        val authorLink = document.selectFirst(".topic-collections-collection-meta a[href^=/user/], .topic-collections-collection-head a[href^=/user/]")
        val meta = document.select(".topic-collections-collection-meta > span").eachText()
        val description = document.selectFirst(".topic-collections-collection-description")?.text()?.trim().orEmpty()
        val collectionId = idFrom(sourceUrl)
        val summary = TopicCollectionSummary(
            collectionId = collectionId,
            title = title,
            authorId = idFrom(authorLink?.attr("href").orEmpty()),
            authorName = authorLink?.text()?.trim().orEmpty(),
            articleCount = meta.firstOrNull { it.contains("篇") } ?: "",
            subscriberCount = meta.firstOrNull { it.contains("订阅") } ?: "",
            description = description,
            relation = relationFrom(document),
            managePath = document.selectFirst("a[href*=/topic_collection_manage]")?.attr("href").orEmpty(),
        )
        val topics = runCatching { HtmlParser.parseTopicList(html).first }.getOrDefault(emptyList())
        return TopicCollectionDetail(
            summary = summary,
            description = description,
            subscriberCount = summary.subscriberCount,
            topics = topics,
            pageInfo = parsePageInfo(document, sourceUrl),
            managePath = summary.managePath,
            actions = parseActions(document),
        )
    }

    fun parsePicker(html: String): TopicCollectionPicker? {
        val document = Jsoup.parse(html, Endpoints.BASE)
        val form = document.selectFirst("form[data-topic-collections-add-form]") ?: return null
        val topicId = form.selectFirst("input[name=topic_id]")?.attr("value")?.toLongOrNull() ?: return null
        val options = form.select("select[data-topic-collections-select] option")
            .mapNotNull { option ->
                val id = option.attr("value").toLongOrNull() ?: return@mapNotNull null
                TopicCollectionPickerOption(id, option.text().trim(), option.attr("data-included") == "1")
            }
        val actions = parseActions(document)
        val addForm = actions.firstOrNull { it.operation == TopicCollectionOperation.ADD_ITEM }
        val removeAllAvailable = form.select("option[value=__topic_collections_remove_all__]").isNotEmpty()
        return TopicCollectionPicker(
            topicId = topicId,
            options = options,
            actions = actions,
            createForm = actions.firstOrNull { it.operation == TopicCollectionOperation.CREATE },
            removeAllForm = actions.firstOrNull { it.operation == TopicCollectionOperation.REMOVE_ALL_ITEMS }
                ?: addForm?.takeIf { removeAllAvailable }?.withAction("item_remove_all", TopicCollectionOperation.REMOVE_ALL_ITEMS, "全部取消收录"),
        )
    }

    fun parseActions(document: org.jsoup.nodes.Document): List<TopicCollectionActionForm> =
        document.select("form[action]").flatMap { form -> parseFormActions(form) }

    private fun parseFormActions(form: Element): List<TopicCollectionActionForm> {
        val action = form.attr("action").trim()
        if (action.isBlank() || action.startsWith("http", true) || action.contains("/login")) return emptyList()
        val method = form.attr("method").trim().ifBlank { "post" }
        if (method.equals("get", true)) return emptyList()
        val base = buildList {
            form.select("input[name]:not([disabled])").filterNot { input ->
                input.attr("type").equals("submit", true) ||
                    ((input.attr("type").equals("checkbox", true) || input.attr("type").equals("radio", true)) && !input.hasAttr("checked"))
            }.forEach { input -> add(input.attr("name") to input.attr("value")) }
            form.select("select[name]:not([disabled])").forEach { select ->
                select.select("option[selected]").forEach { option -> add(select.attr("name") to option.attr("value")) }
            }
            form.select("textarea[name]:not([disabled])").forEach { textarea -> add(textarea.attr("name") to textarea.text()) }
        }.toMutableList()
        val buttons = form.select("button[type=submit], button:not([type]), input[type=submit]")
        val controls = form.select("input[name]:not([type=hidden]):not([type=submit]):not([disabled]), select[name]:not([disabled]), textarea[name]:not([disabled])").map { control ->
            val type = if (control.tagName() == "select") "select" else control.attr("type").ifBlank { control.tagName() }
            val value = when {
                type.equals("checkbox", true) || type.equals("radio", true) -> {
                    if (control.hasAttr("checked")) control.attr("value").ifBlank { "1" } else ""
                }
                control.tagName() == "textarea" -> control.text()
                control.tagName() == "select" -> control.selectFirst("option[selected]")?.attr("value").orEmpty()
                else -> control.attr("value")
            }
            TopicCollectionFormField(
                name = control.attr("name"),
                label = control.closest("label")?.text()?.trim().orEmpty()
                    .ifBlank { control.attr("aria-label") }
                    .ifBlank { control.attr("placeholder") }
                    .ifBlank { control.attr("name") },
                type = type,
                value = value,
                options = if (control.tagName() == "select") control.select("option").map { it.text().trim() } else emptyList(),
                required = control.hasAttr("required"),
            )
        }
        val candidates = if (buttons.isEmpty()) listOf<Element?>(null) else buttons
        return candidates.map { button ->
            val fields = base.toMutableList()
            button?.attr("name")?.takeIf { it.isNotBlank() }?.let { fields += it to button.attr("value") }
            val text = listOfNotNull(button?.text(), button?.attr("value"), form.text()).joinToString(" ").trim()
            val operation = operationFrom(text, fields)
            TopicCollectionActionForm(
                operation = operation,
                method = method,
                action = button?.attr("formaction")?.ifBlank { action } ?: action,
                fields = fields,
                label = button?.text()?.trim().orEmpty().ifBlank { form.selectFirst("legend, h2, h3")?.text()?.trim().orEmpty() }.ifBlank { "提交" },
                enabled = button?.hasAttr("disabled") != true,
                controls = controls,
                targetLabel = targetLabel(form, operation, fields),
            )
        }.filter { it.operation != TopicCollectionOperation.UNKNOWN }
    }

    /** 管理页把目标名称放在同一列表项的 span/link 中，表单本身只有隐藏 ID 和“移除”按钮。 */
    private fun targetLabel(
        form: Element,
        operation: TopicCollectionOperation,
        fields: List<Pair<String, String>>,
    ): String {
        if (operation != TopicCollectionOperation.REMOVE_ITEM &&
            operation != TopicCollectionOperation.REMOVE_COLLABORATOR
        ) return ""
        val row = form.closest("li, tr")
        val linkSelector = if (operation == TopicCollectionOperation.REMOVE_ITEM) {
            "a[href^=/topic/]"
        } else {
            "a[href^=/user/], a[href^='/user?']"
        }
        return row?.selectFirst(linkSelector)?.text()?.trim().orEmpty()
            .ifBlank { row?.children()?.firstOrNull { it.tagName() == "span" }?.text()?.trim().orEmpty() }
            .ifBlank { form.attr("data-target-label").trim() }
            .ifBlank { hiddenTargetLabel(operation, fields) }
    }

    private fun hiddenTargetLabel(
        operation: TopicCollectionOperation,
        fields: List<Pair<String, String>>,
    ): String {
        val readableNames = if (operation == TopicCollectionOperation.REMOVE_ITEM) {
            listOf("topic_title")
        } else {
            listOf("username", "user_name", "collaborator_name")
        }
        val idNames = if (operation == TopicCollectionOperation.REMOVE_ITEM) {
            listOf("topic_id", "topicId", "topic")
        } else {
            listOf("user_id", "collaborator_id")
        }
        readableNames.firstNotNullOfOrNull { name ->
            fields.firstOrNull { it.first == name }?.second?.trim()?.takeIf { it.isNotBlank() }
        }?.let { return it }
        val id = idNames.firstNotNullOfOrNull { name ->
            fields.firstOrNull { it.first == name }?.second?.trim()?.takeIf { it.isNotBlank() }
        } ?: return ""
        return if (operation == TopicCollectionOperation.REMOVE_ITEM) "帖子 #$id" else "用户 #$id"
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

    private fun operationFrom(text: String, fields: List<Pair<String, String>>): TopicCollectionOperation {
        val sourceAction = fields.lastOrNull { it.first == "topic_collections_action" }?.second.orEmpty()
        when (sourceAction) {
            "collection_create", "collection_create_add" -> return TopicCollectionOperation.CREATE
            "collection_update" -> return TopicCollectionOperation.UPDATE
            "collection_delete" -> return TopicCollectionOperation.DELETE
            "subscription_add" -> return TopicCollectionOperation.SUBSCRIBE
            "subscription_remove" -> return TopicCollectionOperation.UNSUBSCRIBE
            "item_add" -> return TopicCollectionOperation.ADD_ITEM
            "item_remove" -> return TopicCollectionOperation.REMOVE_ITEM
            "item_remove_all" -> return TopicCollectionOperation.REMOVE_ALL_ITEMS
            "collaborator_add" -> return TopicCollectionOperation.ADD_COLLABORATOR
            "collaborator_remove" -> return TopicCollectionOperation.REMOVE_COLLABORATOR
        }
        val value = (text + " " + fields.joinToString(" ") { "${it.first}=${it.second}" }).lowercase()
        return when {
            "item_remove_all" in value || "全部取消收录" in text -> TopicCollectionOperation.REMOVE_ALL_ITEMS
            "item_remove" in value || "移出专辑" in text -> TopicCollectionOperation.REMOVE_ITEM
            "item_add" in value || "收录" in text -> TopicCollectionOperation.ADD_ITEM
            "unsubscribe" in value || "取消订阅" in text -> TopicCollectionOperation.UNSUBSCRIBE
            "subscribe" in value || "订阅" in text -> TopicCollectionOperation.SUBSCRIBE
            "delete" in value || "删除专辑" in text -> TopicCollectionOperation.DELETE
            "collaborator" in value && ("remove" in value || "移除" in text) -> TopicCollectionOperation.REMOVE_COLLABORATOR
            "collaborator" in value || "协作者" in text -> TopicCollectionOperation.ADD_COLLABORATOR
            "update" in value || "保存" in text || "编辑" in text -> TopicCollectionOperation.UPDATE
            "create" in value || "创建" in text || "新建" in text -> TopicCollectionOperation.CREATE
            else -> TopicCollectionOperation.UNKNOWN
        }
    }

    private fun relationFrom(element: Element): TopicCollectionRelation {
        val text = element.text()
        return when {
            text.contains("协作") || text.contains("协作者") -> TopicCollectionRelation.COLLABORATOR
            text.contains("已订阅") -> TopicCollectionRelation.SUBSCRIBED
            text.contains("我的") || text.contains("创建者") -> TopicCollectionRelation.OWNER
            text.contains("公开") -> TopicCollectionRelation.PUBLIC
            else -> TopicCollectionRelation.UNKNOWN
        }
    }

    private fun idFrom(value: String): Long = Regex("/(?:topic_collection|topic)/(\\d+)").find(value)?.groupValues?.get(1)?.toLongOrNull() ?: 0L
    private fun absoluteUrl(value: String): String = when {
        value.isBlank() -> ""
        value.startsWith("http", true) -> value
        value.startsWith("//") -> "https:$value"
        else -> Endpoints.abs(value)
    }

    private fun TopicCollectionActionForm.withAction(
        value: String,
        operation: TopicCollectionOperation,
        label: String,
    ): TopicCollectionActionForm = copy(
        operation = operation,
        fields = fields.filterNot { it.first == "topic_collections_action" } + ("topic_collections_action" to value),
        label = label,
    )
}
