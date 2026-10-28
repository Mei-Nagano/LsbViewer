package sb.linux.client.ui.screens

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import sb.linux.client.domain.community.ForumCount
import sb.linux.client.domain.community.HotTopic

@Composable
internal fun DrawerTitle(text: String) {
    sb.linux.client.ui.components.layout.AppGroupLabel(text)
}

/** 站点统计卡片：数字项按每行 3 列「数值上、标签下」展示；尾部文本项（如最新用户）整体一行居中 */
@Composable
internal fun SidebarStatsCard(stats: List<Pair<String, String>>) {
    val numeric = stats.filter { it.second.any(Char::isDigit) }
    val textual = stats.filter { !it.second.any(Char::isDigit) }
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        numeric.chunked(3).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                row.forEach { (label, value) ->
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            value,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                        )
                        Text(
                            label,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
        textual.forEach { (label, value) ->
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (label.isNotEmpty()) {
                    Text(
                        label,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.width(4.dp))
                }
                Text(
                    value,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/**
 * 解析站点统计原文为「标签-值」列表：
 * 数字项按「标签 数字」成对提取（兼容「标签: 数字」「标签 · 数字」等分隔变体）；
 * 尾部无数字文本（如「最新: 用户名」）按冒号或末个空格拆分。
 * 解析不出任何项时返回空列表，由调用方回退原文展示。
 */
internal fun parseSidebarStats(text: String): List<Pair<String, String>> {
    val cleaned = text.replace(Regex("""\s+"""), " ").trim()
    if (cleaned.isEmpty()) return emptyList()
    val leadSep = Regex("""^[·|/、,，;；\-—:：\s]+""")
    fun cleanLabel(s: String) =
        s.replace(":", "").replace("：", "").replace(leadSep, "").trim()
    val pairs = mutableListOf<Pair<String, String>>()
    val numRe = Regex("""\d[\d,，]*""")
    var lastEnd = 0
    numRe.findAll(cleaned).forEach { m ->
        val label = cleanLabel(cleaned.substring(lastEnd, m.range.first))
        if (label.isNotEmpty()) pairs += label to m.value
        lastEnd = m.range.last + 1
    }
    val tail = cleaned.substring(lastEnd).replace(leadSep, "").trim()
    if (tail.isNotEmpty()) {
        val colon = tail.indexOfLast { it == ':' || it == '：' }
        if (colon >= 0) {
            val l = tail.substring(0, colon).trim()
            val v = tail.substring(colon + 1).trim()
            if (l.isNotEmpty() && v.isNotEmpty()) pairs += l to v
        } else {
            val sp = tail.indexOfLast { it == ' ' }
            if (sp > 0) pairs += tail.substring(0, sp).trim() to tail.substring(sp + 1).trim()
            else pairs += "" to tail
        }
    }
    return pairs.filter { it.first.isNotEmpty() || it.second.isNotEmpty() }
}

/** 侧板选项条目：无图标，统一圆角矩形背景框住（快捷功能 / 最近版块等） */
@Composable
internal fun DrawerPillRow(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = modifier.padding(vertical = 2.dp)) {
        sb.linux.client.ui.components.layout.AppMenuRow(text, trailing = {}, compact = true, onClick = onClick)
    }
}

/** 侧板热帖条目：标题/热度，圆角矩形背景，无图标 */
@Composable
internal fun HotTopicRow(t: HotTopic, onOpenTopic: (Long) -> Unit, modifier: Modifier = Modifier) {
    Surface(
        onClick = { onOpenTopic(t.topicId) },
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = modifier.padding(vertical = 2.dp),
    ) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 7.dp)) {
            Text(
                t.title, style = MaterialTheme.typography.bodyMedium,
                maxLines = 1, overflow = TextOverflow.Ellipsis
            )
            if (t.meta.isNotBlank()) {
                Text(
                    t.meta, style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1, overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/** 侧板版块条目：名称 +（单列展示时的）帖子数量角标 */
@Composable
internal fun ForumEntryRow(
    f: ForumCount,
    onOpenForum: (Long) -> Unit,
    modifier: Modifier = Modifier,
    showCount: Boolean = false,
) {
    Surface(
        onClick = { onOpenForum(f.id) },
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = modifier.padding(vertical = 2.dp),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                f.name, style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            // 单列展示时在板块右侧显示帖量（源站侧边栏的版块主题数）
            if (showCount && f.count.isNotBlank()) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f)
                ) {
                    Text(
                        f.count,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}
