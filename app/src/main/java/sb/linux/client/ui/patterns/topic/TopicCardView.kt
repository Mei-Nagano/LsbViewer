package sb.linux.client.ui.patterns.topic

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import sb.linux.client.domain.topic.TopicCard
import sb.linux.client.ui.TitleBadgeView
import sb.linux.client.ui.patterns.user.Avatar
import sb.linux.client.ui.theme.LocalSurfaceOpacity
import sb.linux.client.ui.util.onColorFor

/**
 * 帖子卡片（沿用 main 分支样式）：圆角卡片容器，左侧头像，中间标题与元信息，
 * 板块徽标固定在右上角。
 *
 * compact = 单列精简模式：隐藏头像与回复数。
 * onElementClick 非空时进入「预览改色」模式：点按各元素回调对应 [CardColorOverrides] key（标题不可改色）。
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
        shape = RoundedCornerShape(18.dp),
        color = (bgOverride ?: MaterialTheme.colorScheme.surfaceContainerLow)
            .copy(alpha = LocalSurfaceOpacity.current),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 5.dp)
            .clip(RoundedCornerShape(18.dp))
            .combinedClickable(
                onClick = {
                    // 预览改色模式：点按卡片空白处改背景色；正常模式打开帖子
                    if (onElementClick != null) onElementClick(CardColorOverrides.BACKGROUND) else onClick()
                },
                onLongClick = if (onElementClick == null) onLongClick else null,
            ),
    ) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 9.dp)) {
            if (!compact) {
                Avatar(card.avatarUrl, 34, online = card.online)
                Spacer(Modifier.width(10.dp))
            }
            Column(Modifier.weight(1f)) {
                // 顶行：标题（含标签）占左侧，板块标识固定在卡片右上角，与置顶/热等标签同一行以节省高度
                Row(verticalAlignment = Alignment.Top) {
                    Column(Modifier.weight(1f)) {
                        // 帖子标签（置顶/热/精华/未读/抽奖/发卡）置于标题前面
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                            modifier = Modifier.let { m ->
                                if (onElementClick != null)
                                    m.clip(RoundedCornerShape(6.dp)).clickable { onElementClick(CardColorOverrides.TAG) }
                                else m
                            },
                        ) {
                            TopicCardTags(card, tagColor)
                        }
                        Text(
                            card.title,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = if (card.pinned || card.featured) FontWeight.Bold else FontWeight.SemiBold,
                            // 标题颜色不可自定义，仅保留源站解析出的彩色标题
                            color = titleColor ?: Color.Unspecified,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            lineHeight = MaterialTheme.typography.titleSmall.lineHeight * 1.1f,
                            modifier = Modifier.padding(top = 2.dp),
                        )
                        Spacer(Modifier.size(4.dp))
                        // 用户名 / 时间 / 评论数：不使用分隔点。
                        // 用 FlowRow 让行内放不下时自动换行，避免称号徽章挤占导致用户名被截断
                        FlowRow(
                            verticalArrangement = Arrangement.Center,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                card.authorName,
                                style = MaterialTheme.typography.labelSmall,
                                color = userOverride ?: MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.let { m ->
                                    if (onElementClick != null)
                                        m.clip(RoundedCornerShape(6.dp)).clickable { onElementClick(CardColorOverrides.USER) }
                                    else m
                                },
                            )
                            // 作者称号（源站称号系统徽章）：用 small 缩小比例，避免过宽/换行
                            card.titleBadge?.let {
                                TitleBadgeView(it, small = true)
                            }
                            Text(
                                card.timeText,
                                style = MaterialTheme.typography.labelSmall,
                                color = timeOverride ?: MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.let { m ->
                                    if (onElementClick != null)
                                        m.clip(RoundedCornerShape(6.dp)).clickable { onElementClick(CardColorOverrides.TIME) }
                                    else m
                                },
                            )
                            if (!compact && showComments) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                                    modifier = Modifier.let { m ->
                                        if (onElementClick != null)
                                            m.clip(RoundedCornerShape(6.dp)).clickable { onElementClick(CardColorOverrides.COMMENTS) }
                                        else m
                                    },
                                ) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.Chat, null,
                                        modifier = Modifier.size(12.dp),
                                        tint = commentsOverride ?: MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                    Text(
                                        "${card.replies}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = commentsOverride ?: MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }
                    // 板块标识：固定在卡片右上角（与标题顶部对齐）
                    if (showForum) {
                        Spacer(Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = forumOverride ?: MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.65f),
                            onClick = {
                                if (onElementClick != null) onElementClick(CardColorOverrides.FORUM)
                                else onForumClick(card.forumId)
                            },
                        ) {
                            Text(
                                card.forumName,
                                style = MaterialTheme.typography.labelSmall,
                                color = forumOverride?.let { onColorFor(it) }
                                    ?: MaterialTheme.colorScheme.onSecondaryContainer,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}
