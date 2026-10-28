package sb.linux.client.ui.screens

import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.MaterialTheme
import sb.linux.client.domain.community.ReplyItem
import sb.linux.client.ui.Badge
import sb.linux.client.ui.components.feedback.EmptyBox
import sb.linux.client.ui.patterns.topic.ReplySummaryCard

internal fun LazyListScope.userReplyRows(replies: List<ReplyItem>, onOpenTopic: (Long) -> Unit) {
    if (replies.isEmpty()) item { EmptyBox("暂无回帖") }
    else itemsIndexed(replies, key = { index, reply -> "${reply.replyId}-${reply.topicId}-$index" }) { _, reply ->
        ReplySummaryCard(reply.title, reply.excerpt, reply.timeText, onClick = { onOpenTopic(reply.topicId) },
            badge = {
                if (reply.forumName.isNotBlank()) Badge(reply.forumName,
                    MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.onSecondaryContainer)
            })
    }
}
