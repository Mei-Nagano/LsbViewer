package sb.linux.client.common.content

import org.jsoup.Jsoup
import sb.linux.client.util.escapeHtml

internal object HtmlTextConversion {
    /** HTML 转纯文本（供 AI 总结使用） */
    fun htmlToText(html: String): String =
        Regex("""\s+""").replace(
            html.replace(Regex("""<(br|/p|/li|/h[1-6]|/div|/blockquote)[^>]*>"""), "\n")
                .replace(Regex("""<[^>]+>"""), " ")
                .replace("&nbsp;", " ").replace("&amp;", "&")
                .replace("&lt;", "<").replace("&gt;", ">").replace("&quot;", "\""),
            " "
        ).trim()

    /** 帖内搜索专用纯文本：解码实体，并保留 pre/code 的真实内容与换行。 */
    fun htmlToSearchText(html: String): String {
        val body = Jsoup.parseBodyFragment(html).body()
        body.select("script, style, noscript").remove()
        return body.wholeText().replace('\u00A0', ' ').trim()
    }

    // ---------------- Markdown → HTML（发帖预览用） ----------------

    /**
     * 轻量 Markdown → HTML：覆盖编辑器工具栏与源站支持的语法——
     * 标题/粗斜体/删除线/行内代码/代码块/引用/有序无序列表/链接/图片/分隔线/表格。
     * 产物直接交给 HtmlContent 渲染，预览效果与帖子实际展示一致。
     */
    fun markdownToHtml(md: String): String {
        val out = StringBuilder()
        val lines = md.replace("\r\n", "\n").replace("\r", "\n").split("\n")
        var i = 0

        // 行内语法：先转义再替换标记（代码 span 优先保护，避免内部被二次处理）
        fun inline(s: String): String {
            val codeSpans = mutableListOf<String>()
            var t = escapeHtml(s).replace(Regex("`([^`\n]+)`")) {
                codeSpans += "<code>${it.groupValues[1]}</code>"; "\u0000${codeSpans.size - 1}\u0000"
            }
            t = t
                .replace(Regex("""!\[([^\]\n]*)\]\(([^)\s]+)[^)]*\)"""), """<img src="$2" alt="$1">""")
                .replace(Regex("""\[([^\]\n]+)\]\(([^)\s]+)[^)]*\)"""), """<a href="$2">$1</a>""")
                .replace(Regex("""\*\*([^*\n]+)\*\*"""), "<strong>$1</strong>")
                .replace(Regex("""\*([^*\n]+)\*"""), "<em>$1</em>")
                .replace(Regex("""~~([^~\n]+)~~"""), "<del>$1</del>")
                .replace(Regex("""==([^=\n]+)=="""), "<mark>$1</mark>")
                .replace(Regex("""\^([^\^\n]+)\^"""), "<sup>$1</sup>")
                .replace(Regex("""(?<!~)~([^~\n]+)~(?!~)"""), "<sub>$1</sub>")
            // 还原代码 span
            t = Regex("\u0000(\\d+)\u0000").replace(t) { codeSpans[it.groupValues[1].toInt()] }
            return t
        }

        while (i < lines.size) {
            val line = lines[i]

            // 代码块 ```
            if (line.trimStart().startsWith("```")) {
                val sb = StringBuilder()
                i++
                while (i < lines.size && !lines[i].trimStart().startsWith("```")) { sb.appendLine(lines[i]); i++ }
                i++ // 跳过结尾 ```
                out.append("<pre><code>").append(escapeHtml(sb.toString().trimEnd('\n'))).append("</code></pre>\n")
                continue
            }
            // 空行
            if (line.isBlank()) { i++; continue }
            // 分隔线
            if (Regex("^\\s*(---+|\\*\\*\\*+)\\s*$").matches(line)) {
                out.append("<hr>\n"); i++; continue
            }
            // 标题
            val h = Regex("^(#{1,6})\\s+(.*)$").find(line)
            if (h != null) {
                val lvl = h.groupValues[1].length
                out.append("<h$lvl>").append(inline(h.groupValues[2])).append("</h$lvl>\n")
                i++; continue
            }
            // 引用：按每行 > 的数量生成真正嵌套的 blockquote。
            if (line.trimStart().startsWith(">")) {
                var depth = 0
                while (i < lines.size && lines[i].trimStart().startsWith(">")) {
                    val current = lines[i].trimStart()
                    var pos = 0
                    var targetDepth = 0
                    while (pos < current.length && current[pos] == '>') {
                        targetDepth++
                        pos++
                        while (pos < current.length && current[pos] == ' ') pos++
                    }
                    while (depth < targetDepth) { out.append("<blockquote>"); depth++ }
                    while (depth > targetDepth) { out.append("</blockquote>"); depth-- }
                    out.append("<p>").append(inline(current.substring(pos))).append("</p>")
                    i++
                }
                while (depth > 0) { out.append("</blockquote>"); depth-- }
                out.append('\n')
                continue
            }
            // 表格：| a | b | 且下一行是 |---|---|
            if (line.trim().startsWith("|") && i + 1 < lines.size &&
                Regex("^\\s*\\|?[\\s:|-]+\\|?\\s*$").matches(lines[i + 1]) &&
                lines[i + 1].contains("-")
            ) {
                val header = line.trim().trim('|').split("|").map { it.trim() }
                out.append("<table><thead><tr>")
                header.forEach { out.append("<th>").append(inline(it)).append("</th>") }
                out.append("</tr></thead><tbody>")
                i += 2
                while (i < lines.size && lines[i].trim().startsWith("|")) {
                    val cells = lines[i].trim().trim('|').split("|").map { it.trim() }
                    out.append("<tr>")
                    header.indices.forEach { c -> out.append("<td>").append(inline(cells.getOrElse(c) { "" })).append("</td>") }
                    out.append("</tr>")
                    i++
                }
                out.append("</tbody></table>\n")
                continue
            }
            // 无序列表
            if (Regex("^\\s*[-*+]\\s+").containsMatchIn(line)) {
                out.append("<ul>")
                while (i < lines.size && Regex("^\\s*[-*+]\\s+").containsMatchIn(lines[i])) {
                    out.append("<li>").append(inline(Regex("^\\s*[-*+]\\s+").replace(lines[i], ""))).append("</li>")
                    i++
                }
                out.append("</ul>\n")
                continue
            }
            // 有序列表
            if (Regex("^\\s*\\d+\\.\\s+").containsMatchIn(line)) {
                out.append("<ol>")
                while (i < lines.size && Regex("^\\s*\\d+\\.\\s+").containsMatchIn(lines[i])) {
                    out.append("<li>").append(inline(Regex("^\\s*\\d+\\.\\s+").replace(lines[i], ""))).append("</li>")
                    i++
                }
                out.append("</ol>\n")
                continue
            }
            // 段落（连续非空行合并）。
            // 首行无条件消费：上面的表格分支要求下一行是 |---| 分隔行，未命中时
            // （分隔行还没敲、或表头就是最后一行）以 | 开头的行会被下面的循环条件
            // 排除，若不先消费首行则 i 不推进 → 外层 while 死循环。
            val sb = StringBuilder()
            sb.append(lines[i]).append('\n'); i++
            while (i < lines.size && lines[i].isNotBlank() &&
                !lines[i].trimStart().startsWith(">") && !lines[i].trimStart().startsWith("```") &&
                !Regex("^\\s*[-*+]\\s+").containsMatchIn(lines[i]) && !Regex("^\\s*\\d+\\.\\s+").containsMatchIn(lines[i]) &&
                !Regex("^(#{1,6})\\s+").containsMatchIn(lines[i]) && !lines[i].trim().startsWith("|")
            ) { sb.append(lines[i]).append('\n'); i++ }
            out.append("<p>").append(inline(sb.toString().trim())).append("</p>\n")
        }
        return out.toString()
    }

}
