package sb.linux.client.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import org.jsoup.nodes.Element

internal fun blockAlignment(element: Element): TextAlign? {
    val aligned = generateSequence(element) { it.parent() }.firstOrNull {
        it.hasAttr("align") || it.attr("style").contains("text-align", ignoreCase = true)
    } ?: return null
    val value = aligned.attr("align").ifBlank {
        Regex("""(?i)text-align\s*:\s*(left|center|right|justify)""")
            .find(aligned.attr("style"))?.groupValues?.get(1).orEmpty()
    }.lowercase()
    return when (value) {
        "left" -> TextAlign.Left
        "center" -> TextAlign.Center
        "right" -> TextAlign.Right
        "justify" -> TextAlign.Justify
        else -> null
    }
}

internal fun anchorIds(element: Element): List<String> = buildList {
    element.id().takeIf(String::isNotBlank)?.let(::add)
    element.select("[id]").map(Element::id).filter(String::isNotBlank).forEach(::add)
    element.attr("name").takeIf(String::isNotBlank)?.let(::add)
    element.select("[name]").map { it.attr("name") }.filter(String::isNotBlank).forEach(::add)
}.distinct()

internal fun cleanImagePlaceholder(source: AnnotatedString): AnnotatedString {
    val text = source.text
    if (!text.contains("[图片]")) return source
    val placeholder = "[图片]"
    val kept = StringBuilder(text.length)
    val newPositions = IntArray(text.length) { -1 }
    var index = 0
    while (index < text.length) {
        if (text.startsWith(placeholder, index)) {
            index += placeholder.length
        } else {
            newPositions[index] = kept.length
            kept.append(text[index])
            index++
        }
    }
    val output = kept.toString()
    if (output.isBlank()) return AnnotatedString("")
    return buildAnnotatedString {
        append(output)
        source.spanStyles.forEach { style ->
            val start = newPositions.getOrNull(style.start) ?: return@forEach
            val end = newPositions.getOrNull(style.end - 1)?.plus(1) ?: return@forEach
            if (start < end) addStyle(style.item, start, end)
        }
        source.getStringAnnotations(0, source.length).forEach { annotation ->
            val start = newPositions.getOrNull(annotation.start) ?: return@forEach
            val end = newPositions.getOrNull(annotation.end - 1)?.plus(1) ?: return@forEach
            if (start < end) addStringAnnotation(annotation.tag, annotation.item, start, end)
        }
    }
}

internal fun trimInlineEdges(source: AnnotatedString): AnnotatedString {
    val start = source.text.indexOfFirst { !it.isWhitespace() }
    if (start < 0) return AnnotatedString("")
    val end = source.text.indexOfLast { !it.isWhitespace() } + 1
    return if (start == 0 && end == source.length) source else source.subSequence(start, end)
}

internal fun renderTopicBlock(element: Element, linkColor: Color, codeBg: Color): ContentBlock? {
    val tag = element.tagName().lowercase()
    return when {
        tag == "img" -> ContentBlock(imageUrl = abs(element))
        tag == "hr" -> ContentBlock(isDivider = true)
        tag == "pre" -> ContentBlock(
            annotated = buildAnnotatedString { append(element.wholeText()) },
            isCode = true,
            textAlign = blockAlignment(element),
            anchorIds = anchorIds(element),
        )
        tag == "blockquote" -> renderInline(element, linkColor, codeBg).let { rich ->
            if (rich.text.isBlank()) null else ContentBlock(
                annotated = rich,
                isQuote = true,
                textAlign = blockAlignment(element),
                anchorIds = anchorIds(element),
            )
        }
        else -> renderInline(element, linkColor, codeBg).let { rich ->
            if (rich.text.isBlank()) null else ContentBlock(
                annotated = rich,
                textAlign = blockAlignment(element),
                anchorIds = anchorIds(element),
            )
        }
    }
}

internal fun parseTopicTable(element: Element, linkColor: Color, codeBg: Color): List<List<TableCellData>> =
    element.select("tr").mapNotNull { row ->
        val cells = row.select("th, td").map { cell ->
            val rich = renderInline(cell, linkColor, codeBg)
            TableCellData(
                text = rich.text.trim(),
                isHeader = cell.tagName().lowercase() == "th",
                annotated = rich.takeIf { it.text.isNotBlank() },
                imageUrls = cell.select("img").map(::abs).filter(String::isNotBlank),
                textAlign = blockAlignment(cell),
            )
        }
        cells.takeIf { rowCells -> rowCells.any { it.text.isNotBlank() || it.imageUrls.isNotEmpty() } }
    }
