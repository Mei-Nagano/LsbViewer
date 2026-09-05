/** HTML 正文节点遍历判定工具。 */
package sb.linux.client.ui.util

import org.jsoup.nodes.Element

private val NESTED_BLOCK_TAGS = setOf(
    "p", "div", "section", "article", "blockquote", "pre", "ul", "ol",
    "h1", "h2", "h3", "h4", "h5", "h6", "table", "hr", "details",
)

/**
 * 判断容器是否包含需要递归解析的块级子节点。
 *
 * 图片是行内内容，不能作为递归依据；否则递归只遍历元素节点时会丢失图片前后的文本节点。
 */
internal fun Element.hasNestedBlockChild(): Boolean =
    children().any { child -> child.tagName().lowercase() in NESTED_BLOCK_TAGS }
