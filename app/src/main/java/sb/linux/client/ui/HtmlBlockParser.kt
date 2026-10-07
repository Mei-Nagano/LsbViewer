package sb.linux.client.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.buildAnnotatedString
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import sb.linux.client.ui.util.hasNestedBlockChild
internal fun parseHtmlToBlocks(html: String, linkColor: Color, codeBg: Color, mergeImages: Boolean = true): List<ContentBlock> {
    val root = runCatching { Jsoup.parseBodyFragment(html).body() }.getOrNull() ?: return emptyList()
    val blocks = mutableListOf<ContentBlock>()

    fun renderInlineTo(el: Element): AnnotatedString = renderInline(el, linkColor, codeBg)

    /** 按源站节点顺序拆分一段内的文字与图片，避免把穿插图片统一挪到段尾。 */
    fun collectInlineContent(el: Element) {
        var wrapper = Element("span")
        val alignment = blockAlignment(el)

        fun flushText() {
            val sb = trimInlineEdges(cleanImagePlaceholder(renderInlineTo(wrapper)))
            if (sb.text.isNotBlank()) blocks.add(ContentBlock(annotated = sb, textAlign = alignment))
            wrapper = Element("span")
        }

        el.childNodes().forEach { node ->
            when {
                node is Element && node.tagName().equals("img", ignoreCase = true) -> {
                    flushText()
                    blocks.add(ContentBlock(imageUrl = abs(node)))
                }
                node is Element && node.select("img").isNotEmpty() -> {
                    flushText()
                    collectInlineContent(node)
                }
                else -> wrapper.appendChild(node.clone())
            }
        }
        flushText()
    }

    /** 递归收集列表项，支持无序/有序及嵌套，保留层级缩进 */
    fun collectList(listEl: Element, level: Int) {
        val ordered = listEl.tagName().lowercase() == "ol"
        listEl.children().forEachIndexed { i, child ->
            if (child.tagName().lowercase() == "li") {
                // 渲染列表项文本时去掉内嵌子列表，仅保留该项自身内容
                val liCopy = child.clone()
                liCopy.select("ul, ol").remove()
                val imgs = liCopy.select("img").toList()
                liCopy.select("img").remove()
                val raw = renderInlineTo(liCopy)
                // 列表项同理：图片抽离后去掉文本首尾的 <br>/占位空格
                val sb = if (imgs.isNotEmpty()) trimInlineEdges(cleanImagePlaceholder(raw)) else raw
                if (sb.text.isNotBlank()) {
                    val bullet = if (ordered) "${i + 1}." else "•"
                    blocks.add(
                        ContentBlock(
                            annotated = buildAnnotatedString { append(bullet); append(" "); append(sb) },
                            isList = true, indent = level, textAlign = blockAlignment(child),
                            anchorIds = anchorIds(child),
                        )
                    )
                }
                imgs.forEach { blocks.add(ContentBlock(imageUrl = abs(it))) }
                // 嵌套子列表（源站 markdown 缩进层级）
                child.children().forEach { sub ->
                    if (sub.tagName().lowercase() in setOf("ul", "ol")) collectList(sub, level + 1)
                }
            }
        }
    }

        fun collectQuote(el: Element, depth: Int) {
        val children = el.children()
        if (children.isEmpty()) {
            val sb = renderInlineTo(el)
            if (sb.text.isNotBlank()) blocks.add(ContentBlock(
                annotated = sb, isQuote = true, quoteDepth = depth, textAlign = blockAlignment(el),
                anchorIds = anchorIds(el),
            ))
            return
        }
        children.forEach { child ->
            if (child.tagName().equals("blockquote", ignoreCase = true)) {
                collectQuote(child, depth + 1)
            } else {
                val own = child.clone().also { it.select("blockquote").remove() }
                val sb = renderInlineTo(own)
                if (sb.text.isNotBlank()) blocks.add(ContentBlock(
                    annotated = sb, isQuote = true, quoteDepth = depth, textAlign = blockAlignment(child),
                    anchorIds = anchorIds(child),
                ))
                child.children().filter { it.tagName().equals("blockquote", ignoreCase = true) }
                    .forEach { collectQuote(it, depth + 1) }
            }
        }
    }

    var nextDetailsId = 0

    fun collect(el: Element) {
        el.children().forEach { c ->
            when (c.tagName().lowercase()) {
                "p", "div", "section" -> {
                    // 源站 markdown 表格外包一层 div.markdown-table-wrap（仅含单个 table）：
                    // 落入 renderInline 会被拍平成一行文字，这里提取内层表格按网格渲染；
                    // 泛化处理——只含单个 table 子节点的容器一律按表格解析（类名改版也兼容）
                    val onlyTable = c.children().singleOrNull()
                        ?.takeIf { it.tagName().lowercase() == "table" }
                    if (onlyTable != null) {
                        val rows = parseTopicTable(onlyTable, linkColor, codeBg)
                        if (rows.isNotEmpty()) blocks.add(ContentBlock(tableRows = rows))
                    } else if (c.hasNestedBlockChild()) {
                        collect(c)
                    } else if (c.select("img").isNotEmpty()) {
                        collectInlineContent(c)
                    } else renderTopicBlock(c, linkColor, codeBg)?.let { blocks.add(it) }
                }
                "article" -> collect(c)
                "blockquote" -> collectQuote(c, 0)
                "details" -> {
                    val id = ++nextDetailsId
                    val summary = c.children().firstOrNull { it.tagName().equals("summary", ignoreCase = true) }
                    blocks.add(ContentBlock(
                        annotated = summary?.let(::renderInlineTo)?.takeIf { it.text.isNotBlank() }
                            ?: AnnotatedString("详情"),
                        detailsId = id,
                        isDetailsSummary = true,
                        detailsOpenByDefault = c.hasAttr("open"),
                        textAlign = summary?.let(::blockAlignment) ?: blockAlignment(c),
                        anchorIds = anchorIds(summary ?: c),
                    ))
                    val body = c.clone()
                    body.children().firstOrNull { it.tagName().equals("summary", ignoreCase = true) }?.remove()
                    val contentStart = blocks.size
                    collect(body)
                    for (index in contentStart until blocks.size) {
                        blocks[index] = blocks[index].copy(foldPath = listOf(id) + blocks[index].foldPath)
                    }
                }
                "pre", "hr" -> renderTopicBlock(c, linkColor, codeBg)?.let { blocks.add(it) }
                "ul", "ol" -> collectList(c, 0)
                "h1", "h2", "h3", "h4", "h5", "h6" -> {
                    val sb = renderInlineTo(c)
                    if (sb.text.isNotBlank()) blocks.add(
                        ContentBlock(
                            annotated = sb,
                            isHeading = true,
                            headingLevel = c.tagName().substring(1).toIntOrNull() ?: 1,
                            textAlign = blockAlignment(c),
                            anchorIds = anchorIds(c),
                        )
                    )
                }
                "img" -> blocks.add(ContentBlock(imageUrl = abs(c)))
                "iframe" -> parseEmbeddedVideo(c)?.let { blocks.add(ContentBlock(embeddedVideo = it)) }
                "table" -> {
                    // 表格按行列结构解析，渲染为真正的网格（含表头加粗）
                    val rows = parseTopicTable(c, linkColor, codeBg)
                    if (rows.isNotEmpty()) blocks.add(ContentBlock(tableRows = rows))
                }
                "br" -> {}
                else -> {
                    if (c.hasNestedBlockChild()) collect(c)
                    else if (c.select("img").isNotEmpty()) collectInlineContent(c)
                    else renderTopicBlock(c, linkColor, codeBg)?.let { blocks.add(it) }
                }
            }
        }
    }

    val hasBlock = root.children().any {
        it.tagName().lowercase() in setOf(
            "p", "div", "section", "article", "br", "blockquote", "pre", "ul", "ol",
            "h1", "h2", "h3", "h4", "h5", "h6", "img", "iframe", "table", "section", "hr", "details"
        )
    }
    if (!hasBlock) {
        val sb = renderInlineTo(root)
        if (sb.text.isNotBlank()) blocks.add(ContentBlock(annotated = sb))
    } else {
        collect(root)
    }
    // 连续多张图片合并为一个翻页块（中间仅隔空白/占位文本，无实际文字）
    // mergeImages=false（发帖预览）时保持每张图独立展示，与编辑顺序一一对应
    val source = if (mergeImages) {
        val merged = mutableListOf<ContentBlock>()
        for (b in blocks) {
            if (b.imageUrl != null) {
                val last = merged.lastOrNull()
                if (last != null && last.imageUrl != null && last.foldPath == b.foldPath) {
                    merged[merged.size - 1] = last.copy(imageUrls = last.imageUrls + b.imageUrl)
                } else {
                    merged.add(b.copy(imageUrls = listOf(b.imageUrl)))
                }
            } else {
                merged.add(b)
            }
        }
        merged
    } else blocks
    // @用户名#楼层号：给紧跟 @用户 链接之后的 #N 加楼层注解（点击应用内跳转对应楼层）
    return source.map { b ->
        if (b.annotated != null) b.copy(annotated = annotateFloors(b.annotated!!)) else b
    }
}
