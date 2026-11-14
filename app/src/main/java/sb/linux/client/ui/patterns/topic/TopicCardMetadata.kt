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

/** 默认元信息一行，头像同行模式单列出作者；其余元信息始终优先保留时间。 */
@Composable
internal fun TopicCardMetadata(
    card: TopicCard, showForum: Boolean, showComments: Boolean, onForumClick: (Long) -> Unit,
    userColor: Color?, timeColor: Color?, commentsColor: Color?, forumColor: Color?,
    onElementClick: ((String) -> Unit)?,
    separateAuthor: Boolean = false,
) {
    if (separateAuthor) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            MetadataItem(null, card.authorName, userColor ?: MaterialTheme.colorScheme.primary,
                modifier = Modifier.fillMaxWidth(),
                onClick = onElementClick?.let { { it(CardColorOverrides.USER) } },
                suffix = card.titleBadge?.let { badge -> { TitleBadgeView(badge, small = true) } })
            TopicCardDetails(card, showForum, showComments, onForumClick,
                userColor, timeColor, commentsColor, forumColor, onElementClick, includeAuthor = false)
        }
    } else TopicCardDetails(card, showForum, showComments, onForumClick,
        userColor, timeColor, commentsColor, forumColor, onElementClick, includeAuthor = true)
}

/** 时间保留自然宽度，其他项目在同一行省略。 */
@Composable
private fun TopicCardDetails(
    card: TopicCard, showForum: Boolean, showComments: Boolean, onForumClick: (Long) -> Unit,
    userColor: Color?, timeColor: Color?, commentsColor: Color?, forumColor: Color?,
    onElementClick: ((String) -> Unit)?, includeAuthor: Boolean,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(Modifier.weight(1f, fill = false), horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically) {
            if (includeAuthor) MetadataItem(Icons.Filled.PersonOutline, card.authorName, userColor ?: MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f, fill = false),
                onClick = onElementClick?.let { { it(CardColorOverrides.USER) } },
                suffix = card.titleBadge?.let { badge -> { TitleBadgeView(badge, small = true) } })
            if (showForum && card.forumName.isNotBlank()) MetadataItem(Icons.Filled.FolderOpen, card.forumName,
                forumColor ?: MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f, fill = false),
                onClick = { if (onElementClick != null) onElementClick(CardColorOverrides.FORUM) else onForumClick(card.forumId) })
            if (showComments) MetadataItem(Icons.AutoMirrored.Filled.Chat, card.replies.toString(),
                commentsColor ?: MaterialTheme.colorScheme.onSurfaceVariant,
                onClick = onElementClick?.let { { it(CardColorOverrides.COMMENTS) } })
            if (showComments && card.lastReplier.isNotBlank()) MetadataItem(if (includeAuthor) Icons.Filled.PersonOutline else null, card.lastReplier,
                userColor ?: MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f, fill = false))
        }
        if (card.timeText.isNotBlank()) MetadataItem(Icons.Filled.Schedule, card.timeText,
            timeColor ?: MaterialTheme.colorScheme.onSurfaceVariant,
            onClick = onElementClick?.let { { it(CardColorOverrides.TIME) } })
    }
}

@Composable
private fun MetadataItem(icon: ImageVector?, text: String, color: Color, modifier: Modifier = Modifier, onClick: (() -> Unit)? = null,
    suffix: (@Composable () -> Unit)? = null) {
    Row(modifier.then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        if (icon != null) Icon(icon, null, Modifier.size(12.dp), tint = color)
        Text(text, style = MaterialTheme.typography.labelSmall, color = color,
            maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
        if (suffix != null) Box(Modifier.weight(1f, fill = false)) { suffix() }
    }
}
