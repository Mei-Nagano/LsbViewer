package sb.linux.client.common.collection

import sb.linux.client.data.TopicCard

/** Which source tab is being displayed. */
enum class TopicCollectionTab(val queryValue: String) {
    MINE("mine"),
    EVERYONE("everyone"),
}

/** Relationship between the signed-in user and a collection. */
enum class TopicCollectionRelation {
    OWNER,
    COLLABORATOR,
    SUBSCRIBED,
    PUBLIC,
    UNKNOWN,
}

/** A collection summary as rendered by /topic_collections. */
data class TopicCollectionSummary(
    val collectionId: Long,
    val title: String,
    val authorId: Long = 0,
    val authorName: String = "",
    val avatarUrl: String = "",
    val visibility: String = "",
    val articleCount: String = "",
    val subscriberCount: String = "",
    val updatedText: String = "",
    val createdText: String = "",
    val description: String = "",
    val relation: TopicCollectionRelation = TopicCollectionRelation.UNKNOWN,
    val subscribed: Boolean = false,
    val managePath: String = "",
)

data class TopicCollectionListPage(
    val tab: TopicCollectionTab,
    val items: List<TopicCollectionSummary>,
    val sourceUrl: String = "",
    val requiresLogin: Boolean = false,
)

data class TopicCollectionPageInfo(
    val currentPage: Int = 1,
    val totalPages: Int = 1,
    val previousPath: String = "",
    val nextPath: String = "",
)

data class TopicCollectionDetail(
    val summary: TopicCollectionSummary,
    val description: String = "",
    val subscriberCount: String = "",
    val topics: List<TopicCard> = emptyList(),
    val pageInfo: TopicCollectionPageInfo = TopicCollectionPageInfo(),
    val managePath: String = "",
    val actions: List<TopicCollectionActionForm> = emptyList(),
)

data class TopicCollectionManagePage(
    val title: String,
    val actions: List<TopicCollectionActionForm> = emptyList(),
)

/** Raw source form kept inside the repository boundary. */
data class TopicCollectionActionForm(
    val operation: TopicCollectionOperation,
    val method: String,
    val action: String,
    val fields: List<Pair<String, String>>,
    val label: String,
    val enabled: Boolean = true,
    val controls: List<TopicCollectionFormField> = emptyList(),
    val targetLabel: String = "",
)

data class TopicCollectionFormField(
    val name: String,
    val label: String,
    val type: String = "text",
    val value: String = "",
    val options: List<String> = emptyList(),
    val required: Boolean = false,
)

enum class TopicCollectionOperation {
    CREATE,
    UPDATE,
    DELETE,
    SUBSCRIBE,
    UNSUBSCRIBE,
    ADD_ITEM,
    REMOVE_ITEM,
    REMOVE_ALL_ITEMS,
    ADD_COLLABORATOR,
    REMOVE_COLLABORATOR,
    UNKNOWN,
}

data class TopicCollectionPickerOption(
    val collectionId: Long,
    val title: String,
    val included: Boolean,
    val visibility: String = "",
)

data class TopicCollectionPicker(
    val topicId: Long,
    val options: List<TopicCollectionPickerOption>,
    val actions: List<TopicCollectionActionForm> = emptyList(),
    val createForm: TopicCollectionActionForm? = null,
    val removeAllForm: TopicCollectionActionForm? = null,
)

/**
 * 源站只渲染一张收录表单，网页端 JS 按所选专辑改写 `topic_collections_action` 与 `collection_id`
 * 后再提交。这里复刻同一改写，不做「当前状态取反」的本地推断：
 * 已收录的专辑发 `item_remove`，未收录的发 `item_add`，两者都是源站的显式操作值。
 *
 * @return 可直接提交的表单；源站未提供收录表单时为 null。
 */
fun TopicCollectionPicker.actionFor(option: TopicCollectionPickerOption): TopicCollectionActionForm? {
    val template = actions.firstOrNull { it.operation == TopicCollectionOperation.ADD_ITEM } ?: return null
    val removing = option.included
    return template.copy(
        operation = if (removing) TopicCollectionOperation.REMOVE_ITEM else TopicCollectionOperation.ADD_ITEM,
        label = if (removing) "移出专辑" else "收录",
        fields = template.fields.filterNot {
            it.first == FIELD_COLLECTION_ID || it.first == FIELD_SOURCE_ACTION
        } + (FIELD_COLLECTION_ID to option.collectionId.toString()) +
            (FIELD_SOURCE_ACTION to if (removing) ACTION_ITEM_REMOVE else ACTION_ITEM_ADD),
    )
}

private const val FIELD_COLLECTION_ID = "collection_id"
private const val FIELD_SOURCE_ACTION = "topic_collections_action"
private const val ACTION_ITEM_ADD = "item_add"
private const val ACTION_ITEM_REMOVE = "item_remove"
