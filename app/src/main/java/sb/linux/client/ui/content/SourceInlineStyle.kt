package sb.linux.client.ui.content

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import org.jsoup.nodes.Element

/** 保留编辑器下发的内联样式，不用网页布局属性改变原生操作区。 */
internal fun sourceInlineStyle(element: Element): SpanStyle {
    val css = element.attr("style").split(';').mapNotNull { part ->
        val values = part.split(':', limit = 2)
        if (values.size == 2) values[0].trim().lowercase() to values[1].trim().lowercase() else null
    }.toMap()
    fun color(raw: String?): Color = raw?.let { value ->
        val rgb = Regex("rgba?\\(([^)]+)\\)").matchEntire(value)?.groupValues?.get(1)?.split(',')?.map { it.trim().toFloatOrNull() }
        if (rgb != null && rgb.size >= 3 && rgb.take(3).all { it != null })
            Color(rgb[0]!!.toInt().coerceIn(0,255), rgb[1]!!.toInt().coerceIn(0,255), rgb[2]!!.toInt().coerceIn(0,255),
                ((rgb.getOrNull(3) ?: 1f) * 255).toInt().coerceIn(0,255))
        else runCatching { Color(android.graphics.Color.parseColor(value)) }.getOrNull()
    } ?: Color.Unspecified
    val size = css["font-size"].orEmpty()
    val number = size.removeSuffix("px").removeSuffix("em").removeSuffix("%").toFloatOrNull()
    val weight = css["font-weight"]
    val decoration = css["text-decoration"].orEmpty()
    return SpanStyle(
        color = color(css["color"] ?: element.attr("color").takeIf(String::isNotBlank)),
        background = color(css["background-color"]),
        fontSize = when {
            number == null -> TextUnit.Unspecified
            size.endsWith("px") -> number.coerceIn(8f, 72f).sp
            size.endsWith("em") -> number.coerceIn(.5f, 4f).em
            size.endsWith("%") -> (number / 100).coerceIn(.5f, 4f).em
            else -> TextUnit.Unspecified
        },
        fontWeight = when { weight == "bold" -> FontWeight.Bold; weight?.toIntOrNull() != null -> FontWeight(weight.toInt().coerceIn(100,900)); else -> null },
        fontStyle = if (css["font-style"] == "italic") FontStyle.Italic else null,
        textDecoration = when {
            element.normalName() == "u" || "underline" in decoration -> TextDecoration.Underline
            "line-through" in decoration -> TextDecoration.LineThrough
            else -> null
        },
    )
}
