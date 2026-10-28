package sb.linux.client.ui.screens
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import sb.linux.client.ui.session.Session
import sb.linux.client.ui.patterns.topic.TopicCardView
import sb.linux.client.domain.topic.TopicCard

@Composable
internal fun PostCardPreview(
    scheme: ColorScheme,
    onPick: (String) -> Unit,
) {
    Surface(
        Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = scheme.surfaceContainerLow
    ) {
        Column(Modifier.padding(vertical = 12.dp)) {
            // 顶部标题行（唯一的改色提示，避免重复 3.7）
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 16.dp)
            ) {
                Text(
                    "实时预览 · 点按元素可改色",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                    color = scheme.onSurface
                )
            }
            Spacer(Modifier.height(4.dp))
            TopicCardView(
                card = TopicCard(
                    topicId = 0,
                    title = "预览帖子标题：与首页帖子完全一致的样式",
                    authorId = 1,
                    authorName = "用户名",
                    forumId = 1,
                    forumName = "闲聊灌水",
                    replies = 34,
                    lastReplier = "",
                    timeText = "3 小时前",
                    hot = true,
                    featured = true,
                ),
                onClick = {},
                onForumClick = {},
                onElementClick = onPick,
            )
        }
    }
}

/** 元素选择器（实时预览下半部分）：默认折叠，点击标题行展开（3.5） */
@Composable
internal fun ElementSelectorList(
    session: Session,
    scheme: ColorScheme,
    onClickRole: (PreviewRole) -> Unit,
    onClickElement: (String) -> Unit,
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    Surface(
        Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = scheme.surfaceContainerHigh
    ) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            // 折叠/展开标题行（默认折叠）
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { expanded = !expanded }
                    .padding(horizontal = 4.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "配色角色与卡片元素",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = scheme.onSurface
                )
                Spacer(Modifier.weight(1f))
                Text(
                    if (expanded) "收起" else "展开",
                    style = MaterialTheme.typography.labelMedium,
                    color = scheme.primary
                )
                Icon(
                    if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    null, tint = scheme.onSurfaceVariant
                )
            }
            if (!expanded) {
                Text(
                    "展开可按列表方式修改配色角色与卡片元素颜色",
                    style = MaterialTheme.typography.labelSmall,
                    color = scheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
                )
            } else {
                Text(
                    "配色角色",
                    style = MaterialTheme.typography.labelSmall,
                    color = scheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 4.dp, top = 4.dp, bottom = 2.dp)
                )
                PreviewRole.entries.forEach { role ->
                    val color = previewRoleColor(scheme, role)
                    val overridden = when (role) {
                        PreviewRole.PRIMARY -> session.themePrimary != null
                        PreviewRole.SECONDARY -> session.themeSecondary != null
                        PreviewRole.TERTIARY -> session.themeTertiary != null
                    }
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onClickRole(role) }
                            .padding(horizontal = 4.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(Modifier.size(30.dp).clip(RoundedCornerShape(9.dp)).background(color))
                        Spacer(Modifier.width(10.dp))
                        Text(role.label, style = MaterialTheme.typography.bodyMedium, color = scheme.onSurface, fontWeight = FontWeight.Medium)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            if (overridden) "已自定义" else "跟随主题",
                            style = MaterialTheme.typography.labelSmall,
                            color = scheme.onSurfaceVariant
                        )
                        Spacer(Modifier.weight(1f))
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = scheme.onSurfaceVariant)
                    }
                }
                Text(
                    "帖子卡片元素",
                    style = MaterialTheme.typography.labelSmall,
                    color = scheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 4.dp, top = 10.dp, bottom = 2.dp)
                )
                CardElements.forEach { el ->
                    val color = cardElementColor(session, scheme, el.key)
                    val overridden = session.cardColors.containsKey(el.key)
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onClickElement(el.key) }
                            .padding(horizontal = 4.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(Modifier.size(30.dp).clip(RoundedCornerShape(9.dp)).background(color))
                        Spacer(Modifier.width(10.dp))
                        Text(el.label, style = MaterialTheme.typography.bodyMedium, color = scheme.onSurface, fontWeight = FontWeight.Medium)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            if (overridden) "已自定义" else "跟随主题",
                            style = MaterialTheme.typography.labelSmall,
                            color = scheme.onSurfaceVariant
                        )
                        Spacer(Modifier.weight(1f))
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = scheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

/** 顶部日/夜/系统快捷切换：紧凑横向按钮，图标与文字并排，避免文字竖排导致容器过高 */
