package sb.linux.client.ui.patterns.topic

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import sb.linux.client.domain.topic.TopicCard
import sb.linux.client.ui.Badge
import sb.linux.client.ui.util.onColorFor

/**
 * 条目辅助标签：常显最多两枚，优先保留源站正在进行的状态；
 * 其余收进「+N」入口，点开后就地展开，不把标题挤成窄列。
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun TopicCardTags(card: TopicCard, overrideColor: Color?) {
    val labels = card.sourceTags.ifEmpty { buildList {
        if (card.pinned) add("置顶")
        if (card.hot) add("热")
        if (card.featured) add("精华")
        if (card.lotteryStatus.isNotBlank()) add(card.lotteryStatus)
        if (card.cardStatus.isNotBlank()) add(card.cardStatus)
    } } + if (card.unread) listOf("未读") else emptyList()
    val distinct = labels.distinct()
    if (distinct.isEmpty()) return
    var expanded by remember(card.topicId) { mutableStateOf(false) }
    val visible = if (expanded) distinct else distinct.take(2)
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        visible.forEach { label -> TagBadge(label, overrideColor) }
        if (!expanded && distinct.size > visible.size) {
            val scheme = MaterialTheme.colorScheme
            Badge(
                "+${distinct.size - visible.size}",
                overrideColor ?: scheme.surfaceContainerHighest,
                overrideColor?.let { onColorFor(it) } ?: scheme.onSurfaceVariant,
                modifier = Modifier.clip(RoundedCornerShape(50)).clickable { expanded = true },
            )
        }
    }
}

@Composable
private fun TagBadge(label: String, overrideColor: Color?) {
    val scheme = MaterialTheme.colorScheme
    val background = overrideColor ?: when {
        label == "置顶" || label.contains("红包") -> scheme.errorContainer
        label == "热" || label.contains("发卡") -> scheme.tertiaryContainer
        label == "精华" -> scheme.secondaryContainer
        // 未读是阅读状态，不与其他业务标签共用同一种蓝底
        label == "未读" -> scheme.surfaceContainerHighest
        else -> scheme.primaryContainer
    }
    Badge(label, background, onColorFor(background))
}
