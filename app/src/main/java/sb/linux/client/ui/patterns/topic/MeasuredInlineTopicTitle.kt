package sb.linux.client.ui.patterns.topic

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.constrainWidth
import androidx.compose.ui.unit.constrainHeight

/** 占位尺寸来自真实标签；渲染复用同一约束，避免密度取整和二次换行裁字。 */
@Composable
internal fun MeasuredInlineTopicTitle(
    title: String,
    labels: List<String>,
    style: TextStyle,
    color: Color,
    tag: @Composable (String) -> Unit,
) {
    if (labels.isEmpty()) {
        Text(title, Modifier.fillMaxWidth(), style = style, color = color)
        return
    }
    SubcomposeLayout(Modifier.fillMaxWidth()) { constraints ->
        // BasicText 对 inline rect 取 floor，预留 1px 防止浮点往返丢失末行或末字。
        val tagConstraints = Constraints(maxWidth = (constraints.maxWidth - 1).coerceAtLeast(0))
        val inlineTags = labels.associateWith { label ->
            val measured = subcompose("measure:$label") { tag(label) }.single().measure(tagConstraints)
            InlineTextContent(Placeholder(
                width = (measured.width + 1).toSp(),
                height = (measured.height + 1).toSp(),
                placeholderVerticalAlign = PlaceholderVerticalAlign.Center,
            )) {
                Layout(content = { tag(label) }) { children, slotConstraints ->
                    // 不以 slot 宽度重新折行：长标签已经按真实栏宽测量过。
                    val child = children.single().measure(tagConstraints)
                    layout(slotConstraints.constrainWidth(child.width), slotConstraints.constrainHeight(child.height)) {
                        child.placeRelative(0, 0)
                    }
                }
            }
        }
        val text = buildAnnotatedString {
            if ("置顶" in labels) {
                appendInlineContent("置顶", "置顶")
                append(" ")
            }
            append(title)
            labels.filter { it != "置顶" }.forEach { label ->
                append(" ")
                appendInlineContent(label, label)
            }
        }
        val paragraph = subcompose("paragraph") {
            Text(text, Modifier.fillMaxWidth(), inlineContent = inlineTags, style = style, color = color)
        }.single().measure(constraints)
        layout(paragraph.width, paragraph.height) { paragraph.placeRelative(0, 0) }
    }
}
