package sb.linux.client.ui.patterns.topic

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import sb.linux.client.domain.topic.TopicCard

/** 置顶、标题和尾部标签共用文本行，续行从左侧开始，不保留置顶缩进（§3.6）。 */
@Composable
internal fun TopicCardTitle(card: TopicCard, titleColor: Color?, tagColor: Color?, onTagClick: (() -> Unit)?) {
    val labels = if (sb.linux.client.ui.LocalHideTopicTags.current) emptyList() else card.sourceTags.ifEmpty { buildList {
        if (card.hot) add("热")
        if (card.featured) add("精华")
        if (card.lotteryStatus.isNotBlank()) add(card.lotteryStatus)
        if (card.cardStatus.isNotBlank()) add(card.cardStatus)
    } }.let { if (card.pinned) listOf("置顶") + it else it }
        .let { if (card.unread) it + "未读" else it }.distinct()
    val scheme = MaterialTheme.colorScheme
    val twoRows = sb.linux.client.ui.LocalTopicCardAvatarMetadataRow.current
    val titleStyle = MaterialTheme.typography.titleSmall
    val tagStyle = if (twoRows) MaterialTheme.typography.labelMedium else MaterialTheme.typography.labelSmall
    @Composable
    fun tag(label: String) {
        val source = card.styledTags.firstOrNull { it.label == label }
        if (source != null && tagColor == null) {
            SourceTopicTagView(source, Modifier.then(if (onTagClick != null) Modifier.clickable(onClick = onTagClick) else Modifier),
                textStyle = tagStyle)
            return
        }
        val background = tagColor ?: when {
            label == "置顶" || label.contains("红包") -> scheme.errorContainer
            label == "热" || label.contains("发卡") -> scheme.tertiaryContainer
            label == "精华" -> scheme.secondaryContainer
            label == "未读" -> scheme.surfaceContainerHigh
            else -> scheme.primaryContainer
        }
        TopicTagCapsule(label, background, modifier = Modifier
            .then(if (onTagClick != null) Modifier.clickable(onClick = onTagClick) else Modifier), textStyle = tagStyle)
    }
    MeasuredInlineTopicTitle(card.title, labels,
        style = titleStyle.copy(
            fontWeight = if (card.pinned || card.featured) FontWeight.Bold else FontWeight.SemiBold),
        color = titleColor ?: Color.Unspecified,
        tag = { tag(it) })
}
