package sb.linux.client.ui

/** Markdown 渲染器使用的块级语法模型。 */
internal sealed interface MdBlock {
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

/** 把 `| a | b |` 拆成单元格；`\|` 是转义竖线，不参与分列。 */
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

/** 表格分隔行 `|---|:--:|`：必须带竖线，避免和 `---` 分割线混淆。 */
private fun isTableDelimiter(line: String): Boolean {
    if (!line.contains('|')) return false
    val cells = splitTableRow(line)
    return cells.isNotEmpty() && cells.all { RE_TABLE_CELL_DELIM.matches(it) }
}

/** 三个及以上 - * _ （可夹空格）构成分割线。 */
private fun isRule(line: String): Boolean {
    val s = line.filterNot { it == ' ' }
    return s.length >= 3 && (s.all { it == '-' } || s.all { it == '*' } || s.all { it == '_' })
}

/** 将 Markdown 文本解析为可渲染的块列表；不读取 Compose 状态，也不执行链接回调。 */
internal fun parseMarkdown(src: String): List<MdBlock> {
    val out = mutableListOf<MdBlock>()
    val para = StringBuilder()

    fun flushPara() {
        if (para.isNotBlank()) out += MdBlock.Paragraph(para.toString().trim())
        para.setLength(0)
    }

    val lines = src.replace("\r\n", "\n").replace('\r', '\n').split("\n")
    // 缩进步长取全文最小的非零缩进：模型有时用 2 空格、有时用 4 空格缩进。
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
        // 缩进层级封顶避免深层输入把排版推离可读区域。
        val indent = ((line.length - body.length) / unit).coerceAtMost(4)
        val heading = headingOf(body)

        when {
            // 围栏代码块：``` / ~~~ 起止，未闭合就吃到文末（流式返回时常见）。
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

            // 管道表格：表头行 + 分隔行才算表格，普通含竖线的文字不会被吃掉。
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

            // Setext 标题：上一行文字 + 一行 ===。
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

            // 引用：连续的 > 行合成一个块。
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
                    val done = task.groupValues[1].isNotBlank()
                    MdBlock.Item(indent, if (done) "☑" else "☐", task.groupValues[2].trim())
                } else MdBlock.Item(indent, null, text)
            }

            RE_ORDERED.matches(body) -> {
                flushPara()
                val m = RE_ORDERED.find(body)!!
                out += MdBlock.Item(indent, m.groupValues[1], m.groupValues[2].trim())
            }

            // 段落内的软换行保留为真换行，避免中文留下突兀空隙。
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
