package sb.linux.client.ui.patterns.topic

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import sb.linux.client.domain.topic.TopicCard
import sb.linux.client.ui.TitleBadgeView
import sb.linux.client.ui.patterns.user.UserAvatar
import sb.linux.client.ui.theme.LocalAppSpacing
import sb.linux.client.ui.theme.LocalSurfaceOpacity

/**
 * 社区帖子条目（DESIGN_SYSTEM D07）：平面单列列表项。
 *
 * 起始为 48dp 头像槽（图像 40dp），后接 12dp 与弹性文字列；标题 titleMedium 最多两行，
 * 元信息 bodySmall 可换成两行；板块位于元信息而不是右侧大胶囊。整行打开帖子，
 * 头像打开用户；长按打开动作菜单，三个动作触区互不重叠。
 */
@OptIn(ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
fun TopicCardView(
    card: TopicCard,
    onClick: () -> Unit,
    onForumClick: (Long) -> Unit = {},
    compact: Boolean = false,
    views: Int = -1,
    showComments: Boolean = true,   // false 时不显示评论数（浏览历史只展示浏览时间）
    showForum: Boolean = true,      // false 时不显示板块（浏览历史）
    onElementClick: ((key: String) -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
) {
    val spacing = LocalAppSpacing.current
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
    Column(
        Modifier
            .fillMaxWidth()
            .background((bgOverride ?: Color.Transparent).copy(alpha = LocalSurfaceOpacity.current))
            .combinedClickable(
                onClick = {
                    // 预览改色模式：点按卡片空白处改背景色；正常模式打开帖子
                    if (onElementClick != null) onElementClick(CardColorOverrides.BACKGROUND) else onClick()
                },
                onLongClick = if (onElementClick == null) onLongClick else null,
            )
            .padding(horizontal = spacing.pageHorizontal, vertical = spacing.contentGap),
    ) {
        Row {
            if (!compact) {
                // 48dp 独立触区包住 40dp 头像，整行打开帖子、头像打开用户
                UserAvatar(
                    card.authorId,
                    card.avatarUrl,
                    40,
                    online = card.online,
                    minTouchTarget = true,
                )
                Spacer(Modifier.width(spacing.contentGap))
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(spacing.inline)) {
                TopicCardTags(card, tagColor)
                Text(
                    card.title,
                    style = MaterialTheme.typography.titleMedium,
                    // 读过条目降低强调但保持可读；源站彩色标题优先
                    color = titleColor ?: if (card.unread) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                FlowRow(
                    verticalArrangement = Arrangement.Center,
                    horizontalArrangement = Arrangement.spacedBy(spacing.item),
                ) {
                    Text(
                        card.authorName,
                        style = MaterialTheme.typography.bodySmall,
                        color = userOverride ?: MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.let { m ->
                            if (onElementClick != null)
                                m.clip(MaterialTheme.shapes.extraSmall).clickable { onElementClick(CardColorOverrides.USER) }
                            else m
                        },
                    )
                    card.titleBadge?.let { TitleBadgeView(it, small = true) }
                    if (showForum) {
                        Text(
                            card.forumName,
                            style = MaterialTheme.typography.bodySmall,
                            color = forumOverride ?: MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.clip(MaterialTheme.shapes.extraSmall).clickable {
                                if (onElementClick != null) onElementClick(CardColorOverrides.FORUM)
                                else onForumClick(card.forumId)
                            },
                        )
                    }
                    Text(
                        card.timeText,
                        style = MaterialTheme.typography.bodySmall,
                        color = timeOverride ?: MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.let { m ->
                            if (onElementClick != null)
                                m.clip(MaterialTheme.shapes.extraSmall).clickable { onElementClick(CardColorOverrides.TIME) }
                            else m
                        },
                    )
                    if (!compact && showComments) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp),
                            modifier = Modifier.let { m ->
                                if (onElementClick != null)
                                    m.clip(MaterialTheme.shapes.extraSmall).clickable { onElementClick(CardColorOverrides.COMMENTS) }
                                else m
                            },
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.Chat, null,
                                modifier = Modifier.size(14.dp),
                                tint = commentsOverride ?: MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                "${card.replies}",
                                style = MaterialTheme.typography.bodySmall,
                                color = commentsOverride ?: MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(spacing.contentGap))
        // inset divider：与标题列左缘对齐，列表保持平面
        HorizontalDivider(
            modifier = Modifier.padding(start = if (compact) 0.dp else 60.dp),
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
        )
    }
}
