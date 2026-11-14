package sb.linux.client.common.content

import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.nodes.Node
import org.jsoup.nodes.TextNode

/** 富文本粘贴保留正文结构；不执行脚本，也不保留剪贴板中的样式与事件。 */
internal fun clipboardHtmlToMarkdown(html: String): String {
    val root = Jsoup.parseBodyFragment(html).body().apply { select("script,style,noscript,iframe").remove() }
    fun render(node: Node): String {
        if (node is TextNode) return node.wholeText.replace('\u00a0', ' ')
        if (node !is Element) return ""
        val tag = node.normalName()
        val text = node.childNodes().joinToString("") { render(it) }
        return when (tag) {
            "br" -> "\n"
            "strong", "b" -> "**$text**"
            "em", "i" -> "*$text*"
            "del", "s", "strike" -> "~~$text~~"
            "a" -> node.attr("href").takeIf { it.startsWith("http://") || it.startsWith("https://") }?.let { "[$text]($it)" } ?: text
            "img" -> node.attr("src").takeIf { it.startsWith("http://") || it.startsWith("https://") }?.let { "![${node.attr("alt").ifBlank { "图片" }}]($it)" }.orEmpty()
            "pre" -> "\n```\n${node.wholeText()}\n```\n"
            "code" -> if (node.parent()?.normalName() == "pre") text else "`$text`"
            "blockquote" -> "\n" + text.trim().lines().joinToString("\n") { "> $it" } + "\n"
            "li" -> (if (node.parent()?.normalName() == "ol") "${node.elementSiblingIndex() + 1}. " else "- ") + text.trim() + "\n"
            "ul", "ol" -> "\n$text\n"
            "h1", "h2", "h3", "h4", "h5", "h6" -> "\n${"#".repeat(tag.drop(1).toInt())} $text\n"
            "table" -> {
                val rows = node.select("tr").map { row -> row.children().filter { it.normalName() in setOf("td", "th") }.map { render(it).trim().replace("|", "\\|").replace("\n", "<br>") } }
                if (rows.isEmpty()) "" else "\n| ${rows.first().joinToString(" | ")} |\n| ${List(rows.first().size) { "---" }.joinToString(" | ")} |\n" +
                    rows.drop(1).joinToString("\n") { "| ${it.joinToString(" | ")} |" } + "\n"
            }
            "p", "div", "section", "article" -> "\n$text\n"
            else -> text
        }
    }
    return render(root).replace(Regex("\n{3,}"), "\n\n").trim()
}
