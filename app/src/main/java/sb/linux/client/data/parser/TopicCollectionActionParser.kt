package sb.linux.client.data.parser
import org.jsoup.nodes.Element
import sb.linux.client.common.collection.TopicCollectionActionForm
import sb.linux.client.common.collection.TopicCollectionOperation
import sb.linux.client.common.collection.TopicCollectionFormField

object TopicCollectionActionParser {
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

    internal fun TopicCollectionActionForm.withAction(
        value: String,
        operation: TopicCollectionOperation,
        label: String,
    ): TopicCollectionActionForm = copy(
        operation = operation,
        fields = fields.filterNot { it.first == "topic_collections_action" } + ("topic_collections_action" to value),
        label = label,
    )
}
