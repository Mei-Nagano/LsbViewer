package sb.linux.client.ui.patterns.topic

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.PersonOutline
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import sb.linux.client.domain.topic.TopicCard
import sb.linux.client.ui.TitleBadgeView

/** 恢复自然换行，作者与称号同组；顺序为作者、版块、回复数、最新评论人、时间（§3.6）。 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun TopicCardMetadata(
    card: TopicCard, showForum: Boolean, showComments: Boolean, onForumClick: (Long) -> Unit,
    userColor: Color?, timeColor: Color?, commentsColor: Color?, forumColor: Color?,
    onElementClick: ((String) -> Unit)?,
) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        MetadataItem(Icons.Filled.PersonOutline, card.authorName, userColor ?: MaterialTheme.colorScheme.primary,
            modifier = Modifier.align(Alignment.CenterVertically),
            onClick = onElementClick?.let { { it(CardColorOverrides.USER) } }) {
            card.titleBadge?.let { TitleBadgeView(it, small = true) }
        }
        if (showForum && card.forumName.isNotBlank()) MetadataItem(Icons.Filled.FolderOpen, card.forumName,
            forumColor ?: MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.align(Alignment.CenterVertically),
            onClick = { if (onElementClick != null) onElementClick(CardColorOverrides.FORUM) else onForumClick(card.forumId) })
        if (showComments) MetadataItem(Icons.AutoMirrored.Filled.Chat, card.replies.toString(),
            commentsColor ?: MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.align(Alignment.CenterVertically),
            onClick = onElementClick?.let { { it(CardColorOverrides.COMMENTS) } })
        if (showComments && card.lastReplier.isNotBlank()) MetadataItem(Icons.Filled.PersonOutline, card.lastReplier,
            userColor ?: MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.align(Alignment.CenterVertically))
        if (card.timeText.isNotBlank()) MetadataItem(Icons.Filled.Schedule, card.timeText,
            timeColor ?: MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.align(Alignment.CenterVertically),
            onClick = onElementClick?.let { { it(CardColorOverrides.TIME) } })
    }
}

@Composable
private fun MetadataItem(icon: ImageVector, text: String, color: Color, modifier: Modifier = Modifier, onClick: (() -> Unit)? = null,
    suffix: @Composable RowScope.() -> Unit = {}) {
    Row(modifier.then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        Icon(icon, null, Modifier.size(12.dp), tint = color)
        Text(text, style = MaterialTheme.typography.labelSmall, color = color,
            maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
        suffix()
    }
}
