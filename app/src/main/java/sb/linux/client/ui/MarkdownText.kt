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
 * `## ` `**` `- ` 这些记号原样显示出来。项目不引入 markdown 渲染库（多为 KMP 包，
 * 和 BOM 里的 Compose 版本对齐成本大于收益），这里手写够用的子集：
 * 标题（含 `##标题` 这类不带空格的中文写法与 Setext 下划线写法）、有序/无序列表（含缩进、
 * 任务列表勾选框）、引用（连续 > 行合并）、分割线、围栏代码块（复用 CodeBlockView）、
 * GFM 管道表格，行内 **粗体** *斜体* `代码` ~~删除线~~ [链接](url) 和 \\ 转义。
 * 未覆盖的语法（脚注、HTML）按普通段落原样显示，不会丢内容。
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

/** 渲染用配色与链接回调（一次组合内固定，避免每个块重复读 CompositionLocal） */
private class MdTheme(
    val text: Color,
    val muted: Color,
    val link: Color,
    val codeBg: Color,
    val onLink: (String) -> Unit,
)

private sealed interface MdBlock {
    /** level 1..6 */
    data class Heading(val level: Int, val text: String) : MdBlock
    data class Paragraph(val text: String) : MdBlock
    /** marker 为 null 表示无序列表，否则是 "1." 这类序号原文或任务列表的 ☐/☑ */
    data class Item(val indent: Int, val marker: String?, val text: String) : MdBlock
    data class Quote(val text: String) : MdBlock
    data class Code(val code: String) : MdBlock
    /** GFM 管道表格：首行为表头，各行已按最宽列数补齐 */
    data class Table(val rows: List<List<String>>) : MdBlock
    data object Rule : MdBlock
}

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

// ---------------- 块级解析 ----------------

/** 三个及以上 - * _ （可夹空格）构成分割线，需在无序列表之前判定，否则 `---` 会被吃成列表项 */
private fun isRule(line: String): Boolean {
    val s = line.filterNot { it == ' ' }
    return s.length >= 3 && (s.all { it == '-' } || s.all { it == '*' } || s.all { it == '_' })
}

private val RE_HEADING = Regex("""^(#{1,6})[ \t]*(\S.*)$""")
private val RE_BULLET = Regex("""^[-*+]\s+(.*)$""")
private val RE_ORDERED = Regex("""^(\d{1,3}[.)])\s+(.*)$""")
private val RE_TASK = Regex("""^\[([ xX])]\s+(.*)$""")
private val RE_TABLE_CELL_DELIM = Regex("""^:?-+:?$""")

/**
 * `## 标题`，另外容忍中文常见的 `##标题`（不带空格）——但只在 # 后紧跟汉字/全角标点时，
 * 否则 `#3 楼这样的引用` 会被误判成标题。
 */
private fun headingOf(body: String): MdBlock.Heading? {
    val m = RE_HEADING.find(body) ?: return null
    val level = m.groupValues[1].length
    val text = m.groupValues[2].trim()
    val spaced = body.getOrNull(level).let { it == ' ' || it == '\t' }
    if (!spaced && text.first().code < 0x2000) return null
    return MdBlock.Heading(level, text)
}

/** 把 `| a | b |` 拆成单元格；`\|` 是转义竖线，不参与分列 */
private fun splitTableRow(line: String): List<String> {
    var s = line.trim()
    if (s.startsWith("|")) s = s.substring(1)
    if (s.endsWith("|") && !s.endsWith("""\|""")) s = s.dropLast(1)
    val cells = mutableListOf<String>()
    val cell = StringBuilder()
    var i = 0
    while (i < s.length) {
        val c = s[i]
        when {
            c == '\\' && s.getOrNull(i + 1) == '|' -> { cell.append('|'); i++ }
            c == '|' -> { cells += cell.toString().trim(); cell.setLength(0) }
            else -> cell.append(c)
        }
        i++
    }
    cells += cell.toString().trim()
    return cells
}

/** 表格分隔行 `|---|:--:|`：必须带竖线，避免和 `---` 分割线混淆 */
private fun isTableDelimiter(line: String): Boolean {
    if (!line.contains('|')) return false
    val cells = splitTableRow(line)
    return cells.isNotEmpty() && cells.all { RE_TABLE_CELL_DELIM.matches(it) }
}

private fun parseMarkdown(src: String): List<MdBlock> {
    val out = mutableListOf<MdBlock>()
    val para = StringBuilder()

    fun flushPara() {
        if (para.isNotBlank()) out += MdBlock.Paragraph(para.toString().trim())
        para.setLength(0)
    }

    val lines = src.replace("\r\n", "\n").replace('\r', '\n').split("\n")
    // 缩进步长取全文最小的非零缩进：模型有时用 2 空格、有时用 4 空格缩进，
    // 固定按 2 换算会把 4 空格风格的一级嵌套算成两级，符号（• / ◦）也跟着错位
    val unit = lines.asSequence()
        .filter { it.isNotBlank() }
        .map { it.replace("\t", "  ") }
        .map { it.length - it.trimStart().length }
        .filter { it > 0 }
        .minOrNull()?.coerceIn(2, 4) ?: 2
    var i = 0
    while (i < lines.size) {
        val line = lines[i].replace("\t", "  ").trimEnd()
        val body = line.trimStart()
        // 缩进层级：一个步长算一级，用于嵌套列表的左缩进与符号（• / ◦）交替；深层缩进封顶避免排版跑飞
        val indent = ((line.length - body.length) / unit).coerceAtMost(4)
        val heading = headingOf(body)

        when {
            // 围栏代码块：``` / ~~~ 起止，未闭合就吃到文末（流式返回时常见）
            body.startsWith("```") || body.startsWith("~~~") -> {
                flushPara()
                val fence = body.take(3)
                val code = StringBuilder()
                i++
                while (i < lines.size && !lines[i].trimStart().startsWith(fence)) {
                    code.append(lines[i]).append('\n')
                    i++
                }
                out += MdBlock.Code(code.toString().trimEnd('\n'))
            }

            // 管道表格：表头行 + 分隔行才算表格，普通含竖线的文字不会被吃掉
            body.contains('|') && isTableDelimiter(lines.getOrNull(i + 1)?.trim().orEmpty()) -> {
                flushPara()
                val rows = mutableListOf(splitTableRow(body))
                i += 2
                while (i < lines.size && lines[i].isNotBlank() && lines[i].contains('|')) {
                    rows += splitTableRow(lines[i]); i++
                }
                i--
                val width = rows.maxOf { it.size }
                out += MdBlock.Table(rows.map { row -> List(width) { row.getOrElse(it) { "" } } })
            }

            body.isEmpty() -> flushPara()

            // Setext 标题：上一行文字 + 一行 ===，模型偶尔这么写，不处理会露出等号
            body.length >= 2 && body.all { it == '=' } -> {
                val text = para.toString().trim()
                para.setLength(0)
                if (text.isEmpty()) out += MdBlock.Rule
                else {
                    val rest = text.substringBeforeLast('\n', "")
                    if (rest.isNotBlank()) out += MdBlock.Paragraph(rest.trim())
                    out += MdBlock.Heading(1, text.substringAfterLast('\n').trim())
                }
            }

            isRule(body) -> { flushPara(); out += MdBlock.Rule }

            heading != null -> { flushPara(); out += heading }

            // 引用：连续的 > 行合成一个块，否则每行各带一条竖条，看着像被切碎了
            body.startsWith(">") -> {
                flushPara()
                val quote = StringBuilder()
                while (i < lines.size) {
                    val q = lines[i].trimStart()
                    if (!q.startsWith(">")) break
                    if (quote.isNotEmpty()) quote.append('\n')
                    quote.append(q.removePrefix(">").removePrefix(" ").trimEnd())
                    i++
                }
                i--
                out += MdBlock.Quote(quote.toString().trim())
            }

            RE_BULLET.matches(body) -> {
                flushPara()
                val text = RE_BULLET.find(body)!!.groupValues[1].trim()
                val task = RE_TASK.find(text)
                out += if (task != null) {
                    // 任务列表：勾选框用字形表示，比原样显示 [ ] / [x] 直观
                    val done = task.groupValues[1].isNotBlank()
                    MdBlock.Item(indent, if (done) "☑" else "☐", task.groupValues[2].trim())
                } else MdBlock.Item(indent, null, text)
            }

            RE_ORDERED.matches(body) -> {
                flushPara()
                val m = RE_ORDERED.find(body)!!
                out += MdBlock.Item(indent, m.groupValues[1], m.groupValues[2].trim())
            }

            // 段落内的软换行保留为真换行：中文里按 Markdown 规范并成空格会留下突兀的空隙
            else -> {
                if (para.isNotEmpty()) para.append('\n')
                para.append(body)
            }
        }
        i++
    }
    flushPara()
    return out
}

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
