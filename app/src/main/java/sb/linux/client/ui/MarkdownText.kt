package sb.linux.client.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.takeOrElse

/**
 * 轻量 Markdown 渲染（AI 总结用）：模型返回的是 Markdown，直接丢给 Text 会把
 * `## ` `**` `- ` 这些记号原样显示出来。块级语法解析位于 [MarkdownParser]，本文件只负责
 * Compose 渲染与行内富文本链接。项目不引入 markdown 渲染库，保留原有支持的语法子集。
 */
@Composable
fun MarkdownText(
    markdown: String,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.bodyMedium,
    color: Color = Color.Unspecified,
) {
    val blocks = remember(markdown) { parseMarkdown(markdown) }
    val uriHandler = LocalUriHandler.current
    val linkHandler = LocalLinkHandler.current
    // 次要色（列表符号、引用条、代码底）从正文色派生而非取 onSurfaceVariant：
    // 本组件会被放进 secondaryContainer 之类的彩色容器里，取主题的 onSurfaceVariant 会对比度不足
    val body = color.takeOrElse { LocalContentColor.current }
    val theme = MdTheme(
        text = body,
        muted = body.copy(alpha = 0.72f),
        link = MaterialTheme.colorScheme.primary,
        codeBg = body.copy(alpha = 0.12f),
        onLink = { url ->
            if (linkHandler != null) linkHandler(url) else runCatching { uriHandler.openUri(url) }
        },
    )
    Column(modifier, verticalArrangement = Arrangement.spacedBy(3.dp)) {
        blocks.forEach { MdBlockView(it, style, theme) }
    }
}

/** 渲染用配色与链接回调（一次组合内固定，避免每个块重复读 CompositionLocal）。 */
private class MdTheme(
    val text: Color,
    val muted: Color,
    val link: Color,
    val codeBg: Color,
    val onLink: (String) -> Unit,
)

@Composable
private fun MdBlockView(block: MdBlock, style: TextStyle, t: MdTheme) {
    when (block) {
        is MdBlock.Heading -> {
            // 标题按层级放大；lineHeight 跟着放大，否则大字号会被 style 的行高压扁
            val size = style.fontSize.takeOrElse { 14.sp } * when (block.level) {
                1 -> 1.35f
                2 -> 1.2f
                3 -> 1.1f
                else -> 1.0f
            }
            Text(
                mdInline(block.text, t),
                style = style.copy(
                    fontSize = size,
                    lineHeight = size * 1.4f,
                    fontWeight = FontWeight.Bold,
                ),
                color = t.text,
                modifier = Modifier.padding(top = if (block.level <= 2) 4.dp else 2.dp),
            )
        }

        is MdBlock.Paragraph -> Text(mdInline(block.text, t), style = style, color = t.text)

        is MdBlock.Item -> Row(
            Modifier.padding(start = (block.indent * 14).dp),
            verticalAlignment = Alignment.Top,
        ) {
            Text(
                block.marker ?: if (block.indent % 2 == 0) "•" else "◦",
                style = style,
                color = t.muted,
                modifier = Modifier
                    .widthIn(min = 16.dp)
                    .padding(end = 6.dp),
            )
            Text(mdInline(block.text, t), style = style, color = t.text)
        }

        is MdBlock.Quote -> Row(Modifier.height(IntrinsicSize.Min)) {
            VerticalDivider(thickness = 3.dp, color = t.muted.copy(alpha = 0.4f))
            Text(
                mdInline(block.text, t),
                style = style,
                color = t.muted,
                modifier = Modifier.padding(start = 8.dp),
            )
        }

        is MdBlock.Code -> CodeBlockView(block.code)

        // 表格：模型常无视「不要用表格」的提示，这里按行列渲染，否则整张表会以竖线原文糊在一起。
        // 底色从正文色派生（本组件可能位于彩色容器内），不取主题表面色。
        is MdBlock.Table -> Surface(
            shape = RoundedCornerShape(10.dp),
            color = t.muted.copy(alpha = 0.10f),
            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        ) {
            Column(Modifier.padding(4.dp)) {
                block.rows.forEachIndexed { r, row ->
                    if (r > 0) HorizontalDivider(color = t.muted.copy(alpha = 0.2f))
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                        row.forEach { cell ->
                            Text(
                                mdInline(cell, t),
                                style = if (r == 0) style.copy(fontWeight = FontWeight.Bold) else style,
                                color = t.text,
                                modifier = Modifier.weight(1f).padding(horizontal = 6.dp, vertical = 5.dp),
                            )
                        }
                    }
                }
            }
        }

        MdBlock.Rule -> HorizontalDivider(Modifier.padding(vertical = 4.dp))
    }
}

private fun mdInline(text: String, t: MdTheme): AnnotatedString =
    buildAnnotatedString { appendInline(text, t) }

// ---------------- 行内解析 ----------------

// 交替分支按顺序匹配，长记号必须排在短记号前（*** > ** > *）。
// 斜体的前后界断言避免 snake_case 标识符和 2*3 这类算式被吃成斜体。
// 最后一支是反斜杠转义：同一位置只有它能匹配，所以放末尾也不会被别的分支抢走。
private val RE_INLINE = Regex(
    """`([^`\n]+)`""" +
        """|\*\*\*(.+?)\*\*\*""" +
        """|\*\*(.+?)\*\*""" +
        """|__(.+?)__""" +
        """|~~(.+?)~~""" +
        """|(?<![\w*])\*(?![\s*])([^*\n]+?)\*(?![\w*])""" +
        """|(?<![\w_])_(?![\s_])([^_\n]+?)_(?![\w_])""" +
        """|!?\[([^\]\n]*)]\(\s*([^)\s]+)[^)]*\)""" +
        """|\\([\\`*_~\[\]()#+.!|>{}-])"""
)

private fun AnnotatedString.Builder.appendInline(src: String, t: MdTheme, depth: Int = 0) {
    // 记号可嵌套（**粗体里的 *斜体***），递归处理内层；深度设上限防病态输入
    if (depth > 4) { append(src); return }
    var last = 0
    for (m in RE_INLINE.findAll(src)) {
        if (m.range.first < last) continue
        append(src.substring(last, m.range.first))
        last = m.range.last + 1
        val g = m.groupValues
        when {
            // 转义字符：原样输出被转义的记号本身（\* 显示为 *）
            g[10].isNotEmpty() -> append(g[10])

            g[1].isNotEmpty() -> withStyle(
                SpanStyle(fontFamily = FontFamily.Monospace, background = t.codeBg)
            ) { append(g[1]) }

            g[2].isNotEmpty() -> withStyle(
                SpanStyle(fontWeight = FontWeight.Bold, fontStyle = FontStyle.Italic)
            ) { appendInline(g[2], t, depth + 1) }

            g[3].isNotEmpty() -> withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                appendInline(g[3], t, depth + 1)
            }

            g[4].isNotEmpty() -> withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                appendInline(g[4], t, depth + 1)
            }

            g[5].isNotEmpty() -> withStyle(
                SpanStyle(textDecoration = TextDecoration.LineThrough)
            ) { appendInline(g[5], t, depth + 1) }

            g[6].isNotEmpty() -> withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                appendInline(g[6], t, depth + 1)
            }

            g[7].isNotEmpty() -> withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                appendInline(g[7], t, depth + 1)
            }

            g[9].isNotEmpty() -> {
                val url = g[9]
                withLink(
                    LinkAnnotation.Clickable(
                        tag = url,
                        styles = TextLinkStyles(
                            SpanStyle(color = t.link, textDecoration = TextDecoration.Underline)
                        ),
                    ) { t.onLink(url) }
                ) { append(g[8].ifBlank { url }) }
            }
        }
    }
    append(src.substring(last))
}
