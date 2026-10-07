package sb.linux.client.data

import sb.linux.client.domain.topic.CardInit
import sb.linux.client.domain.topic.ForumOption
import sb.linux.client.domain.topic.LotteryInit
import sb.linux.client.domain.topic.SourceEditMeta
import sb.linux.client.domain.topic.NewTopicFormData
import sb.linux.client.data.parser.parseAnnouncement
import sb.linux.client.data.parser.parseCardInit
import sb.linux.client.data.parser.parseEditValues
import sb.linux.client.data.parser.parseForums
import sb.linux.client.data.parser.parseLotteryInit
import sb.linux.client.data.parser.parseReplyOrderOptions
import sb.linux.client.data.parser.parseSelectedForumId
import sb.linux.client.data.parser.parseSourceEditMeta
import sb.linux.client.data.parser.parseSpecialType

internal class NewTopicFormLoader(
    private val client: LsbClient,
) {
    suspend fun load(editId: Long): NewTopicFormData {
        val path = if (editId > 0) "/topic_edit/$editId" else "/topic_edit"
        val html = client.get(path).html
        val forums = parseForums(html)
        val (replyOrder, replyOrderOptions) = parseReplyOrderOptions(html)
        val specialType = parseSpecialType(html)
        val (title, body, internalId) = if (editId > 0) {
            parseEditValues(html)
        } else {
            Triple("", "", "0")
        }
        return NewTopicFormData(
            forums = forums,
            forumId = parseSelectedForumId(html).ifBlank { forums.firstOrNull()?.id.orEmpty() },
            announcement = parseAnnouncement(html),
            sourceEditMeta = parseSourceEditMeta(html),
            replyOrder = replyOrder,
            replyOrderOptions = replyOrderOptions,
            specialType = specialType,
            title = title,
            body = body,
            internalId = internalId,
            lottery = if (editId > 0 && specialType == "lottery") parseLotteryInit(html) else null,
            virtualCard = if (editId > 0 && specialType == "virtual_card") parseCardInit(html) else null,
        )
    }
}
