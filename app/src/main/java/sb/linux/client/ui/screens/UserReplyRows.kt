package sb.linux.client.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import sb.linux.client.domain.community.ReplyItem
import sb.linux.client.ui.Badge
import sb.linux.client.ui.EmptyBox

internal fun LazyListScope.userReplyRows(
    replies: List<ReplyItem>,
    onOpenTopic: (Long) -> Unit,
) {
    if (replies.isEmpty()) {
        item { EmptyBox("暂无回帖") }
    } else {
        itemsIndexed(replies, key = { index, reply -> "${reply.replyId}-${reply.topicId}-$index" }) { _, reply ->
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                onClick = { onOpenTopic(reply.topicId) },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 5.dp),
            ) {
                Column(Modifier.padding(14.dp)) {
                    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        Text(
                            reply.title,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                        if (reply.forumName.isNotBlank()) {
                            Spacer(Modifier.width(8.dp))
                            Badge(
                                reply.forumName,
                                MaterialTheme.colorScheme.secondaryContainer,
                                MaterialTheme.colorScheme.onSecondaryContainer,
                            )
                        }
                    }
                    if (reply.excerpt.isNotBlank()) {
                        Spacer(Modifier.height(6.dp))
                        Text(
                            reply.excerpt,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        reply.timeText,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
