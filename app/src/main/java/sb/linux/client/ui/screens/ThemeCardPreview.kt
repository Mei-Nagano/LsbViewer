package sb.linux.client.ui.screens
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import sb.linux.client.ui.patterns.settings.searchingFor
import sb.linux.client.ui.patterns.settings.settingsSearchAnchor

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

/** 元素选择器保留实时预览，配置项共用设置列表行。 */
@Composable
internal fun ElementSelectorList(
    session: Session, scheme: ColorScheme,
    onClickRole: (PreviewRole) -> Unit, onClickElement: (String) -> Unit,
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    val searched = searchingFor("配色角色与卡片元素", *PreviewRole.entries.map { it.label }.toTypedArray(),
        *CardElements.map { it.label }.toTypedArray())
    LaunchedEffect(searched) { if (searched) expanded = true }
    GroupCard {
        sb.linux.client.ui.patterns.settings.SettingsListRow("配色角色与卡片元素", "展开后修改主题角色与帖子卡片颜色",
            onClick = { expanded = !expanded }, trailing = {
                Icon(if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore, if (expanded) "收起" else "展开")
            })
        if (expanded) {
            GroupLabel("配色角色")
            PreviewRole.entries.forEach { role ->
                val overridden = when (role) {
                    PreviewRole.PRIMARY -> session.themePrimary != null
                    PreviewRole.SECONDARY -> session.themeSecondary != null
                    PreviewRole.TERTIARY -> session.themeTertiary != null
                }
                sb.linux.client.ui.patterns.settings.SettingsListRow(role.label, if (overridden) "已自定义" else "跟随主题",
                    onClick = { onClickRole(role) }, trailing = {
                        Box(Modifier.size(30.dp).clip(MaterialTheme.shapes.small).background(previewRoleColor(scheme, role)))
                    })
            }
            GroupLabel("帖子卡片元素")
            CardElements.forEach { el ->
                sb.linux.client.ui.patterns.settings.SettingsListRow(el.label, if (session.cardColors.containsKey(el.key)) "已自定义" else "跟随主题",
                    onClick = { onClickElement(el.key) }, trailing = {
                        Box(Modifier.size(30.dp).clip(MaterialTheme.shapes.small).background(cardElementColor(session, scheme, el.key)))
                    })
            }
        }
    }
}
