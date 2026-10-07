package sb.linux.client.data.parser
import sb.linux.client.common.network.Endpoints
import org.jsoup.Jsoup
import sb.linux.client.common.collection.TopicCollectionOperation
import sb.linux.client.common.collection.TopicCollectionPicker
import sb.linux.client.common.collection.TopicCollectionPickerOption

object TopicCollectionPickerParser {
    fun parsePicker(html: String): TopicCollectionPicker? {
        val document = Jsoup.parse(html, Endpoints.BASE)
        val form = document.selectFirst("form[data-topic-collections-add-form]") ?: return null
        val topicId = form.selectFirst("input[name=topic_id]")?.attr("value")?.toLongOrNull() ?: return null
        val options = form.select("select[data-topic-collections-select] option")
            .mapNotNull { option ->
                val id = option.attr("value").toLongOrNull() ?: return@mapNotNull null
                TopicCollectionPickerOption(id, option.text().trim(), option.attr("data-included") == "1")
            }
        val actions = TopicCollectionActionParser.parseActions(document)
        val addForm = actions.firstOrNull { it.operation == TopicCollectionOperation.ADD_ITEM }
        val removeAllAvailable = form.select("option[value=__topic_collections_remove_all__]").isNotEmpty()
        return TopicCollectionPicker(
            topicId = topicId,
            options = options,
            actions = actions,
            createForm = actions.firstOrNull { it.operation == TopicCollectionOperation.CREATE },
            removeAllForm = actions.firstOrNull { it.operation == TopicCollectionOperation.REMOVE_ALL_ITEMS }
                ?: TopicCollectionActionParser.run {
                    addForm?.takeIf { removeAllAvailable }?.withAction(
                        "item_remove_all",
                        TopicCollectionOperation.REMOVE_ALL_ITEMS,
                        "全部取消收录",
                    )
                },
        )
    }

}

