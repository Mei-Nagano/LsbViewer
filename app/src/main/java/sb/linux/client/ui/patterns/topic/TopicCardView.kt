package sb.linux.client.ui.patterns.topic

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import sb.linux.client.domain.topic.TopicCard
import sb.linux.client.ui.theme.LocalSurfaceOpacity

/**
 * 帖子卡片（沿用 main 分支样式）：圆角卡片容器，左侧头像，中间标题与元信息，
 * 板块徽标固定在右上角。
 *
 * compact = 单列精简模式：隐藏头像与回复数。
 * onElementClick 非空时进入「预览改色」模式：点按各元素回调对应 [CardColorOverrides] key（标题不可改色）。
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TopicCardView(
    card: TopicCard,
    onClick: () -> Unit,
    onForumClick: (Long) -> Unit = {},
    compact: Boolean = false,
    views: Int = -1,
    showComments: Boolean = true,   // false 时不显示评论数（浏览历史只展示浏览时间）
    showForum: Boolean = true,      // false 时不显示板块徽标（浏览历史）
    onElementClick: ((key: String) -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
) {
    val titleColor = remember(card.titleColor) {
        runCatching { Color(android.graphics.Color.parseColor(card.titleColor)) }
            .getOrNull()?.takeIf { card.titleColor.isNotBlank() }
    }
    // 外观设置-实时预览的自定义元素颜色（未覆盖时跟随主题）
    val cardColors = LocalCardColorOverrides.current
    val tagColor = cardColors[CardColorOverrides.TAG]
    val bgOverride = cardColors[CardColorOverrides.BACKGROUND]
    val timeOverride = cardColors[CardColorOverrides.TIME]
    val userOverride = cardColors[CardColorOverrides.USER]
    val commentsOverride = cardColors[CardColorOverrides.COMMENTS]
    val forumOverride = cardColors[CardColorOverrides.FORUM]

    Surface(
        shape = MaterialTheme.shapes.large,
        color = (bgOverride ?: MaterialTheme.colorScheme.surfaceContainerLow)
            .copy(alpha = LocalSurfaceOpacity.current),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 5.dp)
            .clip(MaterialTheme.shapes.large)
            .combinedClickable(
                onClick = {
                    // 预览改色模式：点按卡片空白处改背景色；正常模式打开帖子
                    if (onElementClick != null) onElementClick(CardColorOverrides.BACKGROUND)
                    else onClick()
                },
                onLongClick = if (onElementClick == null) onLongClick else null,
            ),
    ) {
        if (sb.linux.client.ui.LocalTopicCardAvatarMetadataRow.current) {
            Column(Modifier.padding(horizontal = 12.dp, vertical = 9.dp)) {
                TopicCardTitle(card, titleColor, tagColor,
                    onElementClick?.let { { it(CardColorOverrides.TAG) } })
                Spacer(Modifier.size(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (!compact) {
                        sb.linux.client.ui.patterns.user.UserAvatar(card.authorId, card.avatarUrl, 34, online = card.online)
                        Spacer(Modifier.width(10.dp))
                    }
                    Box(Modifier.weight(1f)) {
                        TopicCardMetadata(card, showForum, showComments && !compact, onForumClick,
                            userOverride, timeOverride, commentsOverride, forumOverride, onElementClick, separateAuthor = true)
                    }
                }
            }
        } else Row(Modifier.padding(horizontal = 12.dp, vertical = 9.dp)) {
            if (!compact) {
                sb.linux.client.ui.patterns.user.UserAvatar(card.authorId, card.avatarUrl, 34, online = card.online)
                Spacer(Modifier.width(10.dp))
            }
            Column(Modifier.weight(1f)) {
                TopicCardTitle(card, titleColor, tagColor,
                    onElementClick?.let { { it(CardColorOverrides.TAG) } })
                Spacer(Modifier.size(4.dp))
                TopicCardMetadata(card, showForum, showComments && !compact, onForumClick,
                    userOverride, timeOverride, commentsOverride, forumOverride, onElementClick)
            }
        }
    }
}
