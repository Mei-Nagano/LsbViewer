package sb.linux.client.ui.patterns.topic

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import sb.linux.client.domain.topic.TopicCard

/** 置顶、标题和尾部标签共用文本行，续行从左侧开始，不保留置顶缩进（§3.6）。 */
@Composable
internal fun TopicCardTitle(card: TopicCard, titleColor: Color?, tagColor: Color?, onTagClick: (() -> Unit)?) {
    val labels = card.sourceTags.ifEmpty { buildList {
        if (card.hot) add("热")
        if (card.featured) add("精华")
        if (card.lotteryStatus.isNotBlank()) add(card.lotteryStatus)
        if (card.cardStatus.isNotBlank()) add(card.cardStatus)
    } }.let { if (card.pinned) listOf("置顶") + it else it }
        .let { if (card.unread) it + "未读" else it }.distinct()
    val scheme = MaterialTheme.colorScheme
    val density = LocalDensity.current
    val measurer = rememberTextMeasurer()
    @Composable
    fun tag(label: String) {
        val source = card.styledTags.firstOrNull { it.label == label }
        if (source != null && tagColor == null) {
            SourceTopicTagView(source, Modifier.then(if (onTagClick != null) Modifier.clickable(onClick = onTagClick) else Modifier))
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
            .then(if (onTagClick != null) Modifier.clickable(onClick = onTagClick) else Modifier))
    }
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val availableWidth = with(density) { maxWidth.roundToPx() }.coerceAtLeast(1)
        val inlineTags = labels.associateWith { label ->
            val source = card.styledTags.firstOrNull { it.label == label }.takeIf { tagColor == null }
            val textOnly = source?.classes?.split(' ')?.contains("virtual-card-list-price") == true
            val horizontalPadding = if (textOnly) 0.dp else if (source != null) 14.dp else 12.dp
            val verticalPadding = if (textOnly) 0.dp else 4.dp
            val paddingPx = with(density) { horizontalPadding.roundToPx() }
            val style = MaterialTheme.typography.labelSmall.let {
                if (source == null) it else it.copy(fontWeight = if (textOnly) FontWeight.SemiBold else FontWeight.Bold)
            }
            val size = measurer.measure(label, style = style,
                constraints = Constraints(maxWidth = (availableWidth - paddingPx).coerceAtLeast(1))).size
            val placeholder = with(density) {
                Placeholder(
                    width = (size.width + paddingPx).coerceAtMost(availableWidth).toSp(),
                    height = (size.height.toDp() + verticalPadding).toSp(),
                    placeholderVerticalAlign = PlaceholderVerticalAlign.Center,
                )
            }
            InlineTextContent(placeholder) { tag(label) }
        }
        val title = buildAnnotatedString {
            if ("置顶" in labels) {
                appendInlineContent("置顶", "置顶")
                append(" ")
            }
            append(card.title)
            labels.filter { it != "置顶" }.forEach { label ->
                append(" ")
                appendInlineContent(label, label)
            }
        }
        Text(title, modifier = Modifier.fillMaxWidth(), inlineContent = inlineTags,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = if (card.pinned || card.featured) FontWeight.Bold else FontWeight.SemiBold,
            color = titleColor ?: Color.Unspecified)
    }
}
