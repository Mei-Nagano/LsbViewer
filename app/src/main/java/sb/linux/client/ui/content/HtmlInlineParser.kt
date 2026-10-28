package sb.linux.client.ui.content

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import org.jsoup.nodes.Element
import org.jsoup.nodes.Node
import org.jsoup.nodes.TextNode
import sb.linux.client.common.network.Endpoints
/**
 * 给 AnnotatedString 中的 #楼层号 添加 FLOOR 注解。
 * 仅处理紧跟 @用户 链接（/user/N 且文本以 @ 开头）之后的 #N，避免误伤普通话题标签。
 */
internal fun annotateFloors(s: AnnotatedString): AnnotatedString {
    val mentionRanges = s.getStringAnnotations("URL", 0, s.length).filter { a ->
        Regex("""/user/\d+""").containsMatchIn(a.item) &&
            s.text.substring(a.start, a.end).startsWith("@")
    }
    if (mentionRanges.isEmpty()) return s
    val floorRanges = mutableListOf<Triple<Int, Int, String>>() // start, end, item
    for (m in mentionRanges) {
        val rest = s.text.substring(m.end)
        // 仅匹配开头（允许前导空白），避免误伤正文中段的 #N
        val mm = Regex("""^[\s\u00A0]*#\d+""").find(rest) ?: continue
        floorRanges.add(
            Triple(
                m.end + mm.range.first,
                m.end + mm.range.last + 1,
                mm.value.trim().removePrefix("#")
            )
        )
    }
    if (floorRanges.isEmpty()) return s
    return buildAnnotatedString {
        append(s.text)
        s.spanStyles.forEach { addStyle(it.item, it.start, it.end) }
        s.getStringAnnotations(0, s.length).forEach { addStringAnnotation(it.tag, it.item, it.start, it.end) }
        floorRanges.forEach { (st, en, item) -> addStringAnnotation("FLOOR", item, st, en) }
    }
}

internal fun abs(el: Element): String =
    el.attr("abs:src").ifBlank { el.attr("src") }.let { if (it.startsWith("http")) it else Endpoints.abs(it) }

internal fun renderInline(el: Element, linkColor: Color, codeBg: Color): AnnotatedString = buildAnnotatedString {
    fun localAnchor(link: String): String? {
        val raw = when {
            link.startsWith("#") -> link.removePrefix("#")
            else -> runCatching { java.net.URI(link).rawFragment }.getOrNull()
                ?.takeIf { fragment ->
                    fragment.startsWith("fn", ignoreCase = true) ||
                        fragment.contains("footnote", ignoreCase = true)
                }
        } ?: return null
        return runCatching { java.net.URLDecoder.decode(raw, "UTF-8") }.getOrDefault(raw)
    }

    fun appendNode(
        node: Node,
        bold: Boolean,
        italic: Boolean,
        strike: Boolean,
        code: Boolean,
        link: String?,
        baseline: Int,
        marked: Boolean,
    ) {
        when (node) {
            is TextNode -> {
                val text = node.text()
                if (text.isNotBlank()) {
                    val start = length
                    withStyle(
                        SpanStyle(
                            fontWeight = if (bold) FontWeight.Bold else null,
                            fontStyle = if (italic) FontStyle.Italic else null,
                            textDecoration = if (strike) androidx.compose.ui.text.style.TextDecoration.LineThrough else null,
                            fontFamily = if (code) FontFamily.Monospace else null,
                            background = when {
                                code -> codeBg
                                marked -> Color(0x66FFD54F)
                                else -> Color.Unspecified
                            },
                            color = if (link != null) linkColor else Color.Unspecified,
                            baselineShift = when {
                                baseline > 0 -> BaselineShift.Superscript
                                baseline < 0 -> BaselineShift.Subscript
                                else -> null
                            },
                            fontSize = if (baseline != 0) 0.8.em else androidx.compose.ui.unit.TextUnit.Unspecified,
                        )
                    ) { append(text) }
                    if (link != null) {
                        val anchor = localAnchor(link)
                        if (anchor != null) addStringAnnotation("ANCHOR", anchor, start, length)
                        else addStringAnnotation("URL", link, start, length)
                        // 楼层链接（/topic/xxx?floor=N）：额外打 FLOOR 注解，点击在当前帖内跳转
                        if (anchor == null) Regex("""[?&]floor=(\d+)""").find(link)?.let { m ->
                            addStringAnnotation("FLOOR", m.groupValues[1], start, length)
                        }
                    } else if (!code) {
                        // 源站部分旧帖把裸 URL 当普通文本输出；在不改动原 HTML 的前提下
                        // 为 http(s) / www 链接补上可点击注解，并排除常见句末标点。
                        Regex("(?i)(https?://|www\\.)[^\\s<>{}\\[\\]（）]+")
                            .findAll(text).forEach { match ->
                                val raw = match.value.trimEnd('.', ',', ';', ':', '!', '?', '。', '，', '；', '：', '！', '？')
                                if (raw.isEmpty()) return@forEach
                                val st = start + match.range.first
                                val en = st + raw.length
                                val target = if (raw.startsWith("www.", ignoreCase = true)) "https://$raw" else raw
                                addStyle(
                                    SpanStyle(
                                        color = linkColor,
                                        textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline,
                                    ), st, en
                                )
                                addStringAnnotation("URL", target, st, en)
                            }
                    }
                }
            }
            is Element -> {
                val tag = node.tagName().lowercase()
                if (tag == "br") {
                    append("\n")
                    return
                }
                if (tag == "img") {
                    // 内联图片只占一个空格位，图片本体由 select("img") 另行成块展示；
                    // 不再写入 "[图片]" 占位文本，避免未被清理的路径把占位符露出来。
                    append(" ")
                    return
                }
                val newLink = if (tag == "a") node.attr("abs:href").ifBlank { node.attr("href") }.ifBlank { null } else link
                node.childNodes().forEach {
                    appendNode(
                        it,
                        bold || tag == "strong" || tag == "b",
                        italic || tag == "em" || tag == "i",
                        strike || tag == "del" || tag == "s",
                        code || tag == "code",
                        newLink,
                        when (tag) { "sup" -> 1; "sub" -> -1; else -> baseline },
                        marked || tag == "mark",
                    )
                }
            }
            else -> {}
        }
    }
    el.childNodes().forEach { appendNode(it, false, false, false, false, null, 0, false) }
}

