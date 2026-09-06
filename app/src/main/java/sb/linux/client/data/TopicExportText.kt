package sb.linux.client.data

import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import sb.linux.client.util.escapeHtml

/** 帖子 HTML / Markdown 文本渲染，不涉及文件系统或 Android View。 */
object TopicExportText {
    fun buildHtml(title: String, posts: List<PostEntry>, url: String): String {
        val sb = StringBuilder()
        sb.append("<!doctype html><html lang=\"zh-CN\"><head><meta charset=\"utf-8\">")
        sb.append("<meta name=\"viewport\" content=\"width=device-width,initial-scale=1\">")
        sb.append("<title>").append(escapeHtml(title)).append("</title>")
        sb.append("<style>body{font-family:system-ui,-apple-system,sans-serif;max-width:760px;margin:0 auto;padding:16px;line-height:1.7;color:#222}")
        sb.append("article{border:1px solid #e3e3e3;border-radius:10px;padding:14px;margin:12px 0}")
        sb.append(".meta{color:#888;font-size:13px;margin-bottom:6px}img{max-width:100%}pre{background:#f5f5f5;padding:10px;border-radius:8px;overflow:auto}blockquote{border-left:3px solid #99a;margin:0;padding-left:10px;color:#555}</style>")
        sb.append("</head><body><h1>").append(escapeHtml(title)).append("</h1>")
        sb.append("<p class=\"meta\">来源：<a href=\"").append(escapeHtml(url)).append("\">").append(escapeHtml(url)).append("</a></p>")
        posts.forEach { post ->
            sb.append("<article>")
            sb.append("<div class=\"meta\"><b>").append(escapeHtml(post.authorName)).append("</b>")
            if (post.floor > 0) sb.append(" · #").append(post.floor)
            sb.append(" · ").append(escapeHtml(post.timeText)).append("</div>")
            sb.append(post.contentHtml)
            sb.append("</article>")
        }
        sb.append("</body></html>")
        return sb.toString()
    }

    fun buildMarkdown(title: String, posts: List<PostEntry>, url: String): String {
        val sb = StringBuilder()
        sb.append("# ").append(title).append("\n\n")
        sb.append("> 来源：").append(url).append("\n\n")
        sb.append("---\n\n")
        posts.forEach { post ->
            sb.append("**").append(post.authorName).append("**")
            if (post.floor > 0) sb.append(" `#").append(post.floor).append("`")
            sb.append(" · ").append(post.timeText).append("\n\n")
            sb.append(htmlToMarkdown(post.contentHtml)).append("\n\n---\n\n")
        }
        return sb.toString()
    }

    /** 服务端渲染的帖子 HTML → Markdown（覆盖常用元素）。 */
    fun htmlToMarkdown(html: String): String {
        val root = runCatching { Jsoup.parseBodyFragment(html).body() }.getOrNull() ?: return ""
        val sb = StringBuilder()

        fun inline(element: Element): String {
            val out = StringBuilder()
            element.childNodes().forEach { node ->
                when (node) {
                    is org.jsoup.nodes.TextNode -> out.append(node.text())
                    is Element -> when (node.tagName().lowercase()) {
                        "br" -> out.append("\n")
                        "strong", "b" -> out.append("**").append(inline(node)).append("**")
                        "em", "i" -> out.append("*").append(inline(node)).append("*")
                        "del", "s" -> out.append("~~").append(inline(node)).append("~~")
                        "code" -> out.append("`").append(node.text()).append("`")
                        "a" -> {
                            val href = node.attr("abs:href").ifBlank { node.attr("href") }
                            out.append("[").append(node.text()).append("](").append(href).append(")")
                        }
                        "img" -> {
                            val src = node.attr("abs:src").ifBlank { node.attr("src") }
                            out.append("![图片](").append(src).append(")")
                        }
                        else -> out.append(inline(node))
                    }
                    else -> Unit
                }
            }
            return out.toString()
        }

        fun block(element: Element) {
            when (element.tagName().lowercase()) {
                "p", "div", "section" -> sb.append(inline(element)).append("\n\n")
                "pre" -> sb.append("```\n").append(element.wholeText().trim()).append("\n```\n\n")
                "blockquote" -> sb.append(inline(element).trim().lines().joinToString("\n") { "> $it" }).append("\n\n")
                "h1" -> sb.append("# ").append(inline(element)).append("\n\n")
                "h2" -> sb.append("## ").append(inline(element)).append("\n\n")
                "h3" -> sb.append("### ").append(inline(element)).append("\n\n")
                "h4", "h5", "h6" -> sb.append("#### ").append(inline(element)).append("\n\n")
                "ul", "ol" -> element.select("> li").forEachIndexed { index, li ->
                    sb.append(if (element.tagName().lowercase() == "ol") "${index + 1}. " else "- ")
                        .append(inline(li).trim()).append("\n")
                }.also { sb.append("\n") }
                "hr" -> sb.append("---\n\n")
                "table" -> {
                    val rows = element.select("tr")
                    rows.forEachIndexed { index, row ->
                        val cells = row.select("th, td").map { it.text().trim() }
                        sb.append("| ").append(cells.joinToString(" | ")).append(" |\n")
                        if (index == 0) sb.append("|").append(cells.joinToString("") { "--- |" }).append("\n")
                    }
                    sb.append("\n")
                }
                "img" -> sb.append("![图片](").append(element.attr("abs:src").ifBlank { element.attr("src") }).append(")\n\n")
                else -> {
                    val text = inline(element).trim()
                    if (text.isNotEmpty()) sb.append(text).append("\n\n")
                }
            }
        }

        root.children().forEach(::block)
        if (sb.isEmpty()) {
            val text = inline(root).trim()
            if (text.isNotEmpty()) sb.append(text).append("\n")
        }
        return sb.toString().replace(Regex("\n{3,}"), "\n\n").trim()
    }
}
