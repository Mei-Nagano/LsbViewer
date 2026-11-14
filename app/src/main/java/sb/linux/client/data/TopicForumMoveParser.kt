package sb.linux.client.data

import org.jsoup.nodes.Document
import sb.linux.client.domain.topic.TopicForumMove

/** 折叠容器不代表无权限；只渲染服务端实际下发的移动表单，不自造端点。 */
internal object TopicForumMoveParser {
    fun parse(document: Document): TopicForumMove? {
        val panel = document.selectFirst("[id$=-topic_forum_move]") ?: return null
        val formElement = panel.selectFirst("form")
        val form = formElement?.clone()?.let { AccountManagementParser.parseForm(it, document.baseUri()) }
        val note = panel.clone().also { it.select("form,script,style").remove() }.text().trim()
        if (form == null && note.isBlank()) return null
        return TopicForumMove(note, form?.copy(id = "topic-forum-move:${form.id}",
            confirmation = form.confirmation.ifBlank { "确认移动此帖到所选板块？" }), anchorId = panel.id())
    }
}
