package sb.linux.client.ui.screens

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue

internal fun insertReplyMarkup(
    value: TextFieldValue,
    before: String,
    after: String = before,
    placeholder: String = "",
): TextFieldValue {
    val text = value.text
    val start = value.selection.min.coerceIn(0, text.length)
    val end = value.selection.max.coerceIn(0, text.length)
    val selected = text.substring(start, end)
    val content = selected.ifEmpty { placeholder.takeIf(String::isNotBlank).orEmpty() }
    val updated = text.substring(0, start) + before + content + after + text.substring(end)
    val cursor = start + before.length + content.length + after.length
    return TextFieldValue(updated, selection = TextRange(cursor))
}

internal fun insertReplyImage(value: TextFieldValue, url: String): TextFieldValue {
    val start = value.selection.min.coerceIn(0, value.text.length)
    val end = value.selection.max.coerceIn(0, value.text.length)
    val markdown = "![图片]($url)"
    val updated = value.text.substring(0, start) + markdown + value.text.substring(end)
    return TextFieldValue(updated, selection = TextRange(start + markdown.length))
}
