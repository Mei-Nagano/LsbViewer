package sb.linux.client.ui.content

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.buildAnnotatedString
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.nodes.Node
import org.jsoup.nodes.TextNode

/** 按 DOM 的文字／块节点顺序收集；不把容器内的裸文字丢掉或挪到段尾。 */
internal fun parseHtmlToBlocks(html: String, linkColor: Color, codeBg: Color, mergeImages: Boolean = true): List<ContentBlock> {
    val root = runCatching { Jsoup.parseBodyFragment(html, sb.linux.client.common.network.Endpoints.BASE).body() }.getOrNull()
        ?: return emptyList()
    val blocks = HtmlBlockCollector(linkColor, codeBg).read(root)
    val merged = mutableListOf<ContentBlock>()
    blocks.forEach { block ->
        val last = merged.lastOrNull()
        if (mergeImages && block.imageUrl != null && last?.imageUrl != null &&
            last.foldPath == block.foldPath && last.isQuote == block.isQuote && last.indent == block.indent) {
            merged[merged.lastIndex] = last.copy(imageUrls = last.imageUrls + block.imageUrl)
        } else merged += if (block.imageUrl != null) block.copy(imageUrls = listOf(block.imageUrl)) else block
    }
    return merged.map { block -> block.annotated?.let { block.copy(annotated = annotateFloors(it)) } ?: block }
}

private class HtmlBlockCollector(private val linkColor: Color, private val codeBg: Color) {
    private val blocks = mutableListOf<ContentBlock>()
    private var nextDetailsId = 0
    private val structural = setOf("p", "div", "section", "article", "blockquote", "pre", "ul", "ol", "li",
        "h1", "h2", "h3", "h4", "h5", "h6", "hr", "details", "table", "iframe", "img")

    fun read(root: Element): List<ContentBlock> { collect(root); return blocks }

    private fun shell(element: Element) = Element(element.tag(), element.baseUri(), element.attributes().clone())

    private fun collect(container: Element) {
        var inline = shell(container)
        fun flush() {
            var styled = inline
            generateSequence(container.parent()) { it.parent() }.takeWhile { it.normalName() !in setOf("body", "html") }
                .forEach { ancestor -> styled = shell(ancestor).appendChild(styled) }
            val rich = trimInlineEdges(renderInline(styled, linkColor, codeBg))
            if (rich.text.isNotBlank()) blocks += ContentBlock(annotated = rich,
                textAlign = blockAlignment(container), anchorIds = anchorIds(inline))
            else if (inline.select("br").isNotEmpty()) blocks += ContentBlock(spacerLines = inline.select("br").size)
            inline = shell(container)
        }
        container.childNodes().forEach { node ->
            if (node is Element && (node.normalName() in structural ||
                node.hasAttr("data-nb-editor-math") || node.hasClass("nb-editor-reply-visible") ||
                node.select("p,div,section,blockquote,pre,ul,ol,table,details,img,iframe,[data-nb-editor-math]").isNotEmpty())) {
                flush(); element(node)
            } else if (node is Element || node is TextNode) inline.appendChild(node.clone())
        }
        flush()
    }

    private fun element(element: Element) {
        val tag = element.normalName()
        if (tag in setOf("script", "style")) return
        if (element.hasAttr("data-nb-editor-math")) {
            blocks += ContentBlock(formulaSource = element.text(), formulaDisplay = element.attr("data-nb-editor-math") == "display")
            return
        }
        if (element.hasClass("nb-editor-reply-visible")) {
            val locked = element.hasClass("nb-editor-reply-visible-locked")
            val body = element.clone().apply { select(".nb-editor-reply-visible-label").remove() }
            blocks += ContentBlock(replyVisibleHtml = if (locked) "" else body.html(), replyVisibleLocked = locked,
                annotated = if (locked) AnnotatedString(element.text()) else null, anchorIds = anchorIds(element))
            return
        }
        when (tag) {
            "pre", "hr" -> renderTopicBlock(element, linkColor, codeBg)?.let(blocks::add)
            "img" -> blocks += ContentBlock(imageUrl = abs(element), anchorIds = anchorIds(element))
            "iframe" -> parseEmbeddedVideo(element)?.let { blocks += ContentBlock(embeddedVideo = it) }
            "table" -> parseTopicTable(element, linkColor, codeBg).takeIf { it.isNotEmpty() }
                ?.let { blocks += ContentBlock(tableRows = it, anchorIds = anchorIds(element)) }
            "ul", "ol" -> list(element)
            "blockquote" -> {
                val start = blocks.size
                collect(element)
                for (i in start until blocks.size) blocks[i] = blocks[i].copy(isQuote = true,
                    quoteDepth = blocks[i].quoteDepth + if (blocks[i].isQuote) 1 else 0)
            }
            "details" -> details(element)
            else -> {
                val heading = tag.matches(Regex("h[1-6]"))
                val start = blocks.size
                collect(element)
                if (tag == "p" && start < blocks.size) blocks[start] = blocks[start].copy(paragraphStart = true)
                if (heading) for (i in start until blocks.size) if (blocks[i].annotated != null)
                    blocks[i] = blocks[i].copy(isHeading = true, headingLevel = tag.drop(1).toInt(), anchorIds = anchorIds(element))
                if (start == blocks.size && tag == "p") blocks += ContentBlock(spacerLines = 1)
            }
        }
    }

    private fun list(element: Element) {
        val items = element.children().filter { it.normalName() == "li" }
        val ordered = element.normalName() == "ol"
        val reversed = element.hasAttr("reversed")
        var number = element.attr("start").toIntOrNull() ?: if (reversed) items.size else 1
        items.forEach { item ->
            number = item.attr("value").toIntOrNull() ?: number
            val marker = if (ordered) "$number." else "•"
            val start = blocks.size
            collect(item)
            val first = (start until blocks.size).firstOrNull { blocks[it].annotated != null && !blocks[it].isList }
            if (first != null) blocks[first] = blocks[first].copy(annotated = buildAnnotatedString {
                append("$marker "); append(blocks[first].annotated!!)
            }, isList = true, anchorIds = anchorIds(item))
            for (i in start until blocks.size) if (i != first)
                blocks[i] = blocks[i].copy(indent = blocks[i].indent + 1, isList = blocks[i].annotated != null)
            number += if (reversed) -1 else 1
        }
    }

    private fun details(element: Element) {
        val id = ++nextDetailsId
        val summary = element.children().firstOrNull { it.normalName() == "summary" }
        blocks += ContentBlock(annotated = summary?.let { renderInline(it, linkColor, codeBg) }
            ?: AnnotatedString("详情"), detailsId = id, isDetailsSummary = true,
            detailsOpenByDefault = element.hasAttr("open"), textAlign = blockAlignment(element), anchorIds = anchorIds(element))
        val body = element.clone().apply { children().firstOrNull { it.normalName() == "summary" }?.remove() }
        val start = blocks.size
        collect(body)
        for (i in start until blocks.size) blocks[i] = blocks[i].copy(foldPath = listOf(id) + blocks[i].foldPath)
    }
}
