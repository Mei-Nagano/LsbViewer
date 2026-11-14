package sb.linux.client.ui.screens

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue

internal fun replaceReplySelection(value: TextFieldValue, text: String): TextFieldValue {
    val start = value.selection.min.coerceIn(0, value.text.length)
    val end = value.selection.max.coerceIn(0, value.text.length)
    return TextFieldValue(value.text.replaceRange(start, end, text), TextRange(start + text.length))
}

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

/** 工具栏的行首标记作用于完整行；模板选中占位文字，便于直接改写。 */
internal fun applySourceEditorMarkup(value: TextFieldValue, before: String, after: String, placeholder: String): TextFieldValue {
    val start = value.selection.min
    val end = value.selection.max
    if (after.isEmpty() && before in setOf("## ", "> ", "- ", "1. ")) {
        val lineStart = value.text.substring(0, start).lastIndexOf('\n') + 1
        val lineEnd = value.text.indexOf('\n', end).takeIf { it >= 0 } ?: value.text.length
        val content = value.text.substring(lineStart, lineEnd).ifBlank { placeholder }
        val marked = content.lines().mapIndexed { index, line -> (if (before == "1. ") "${index + 1}. " else before) + line }.joinToString("\n")
        return TextFieldValue(value.text.replaceRange(lineStart, lineEnd, marked), TextRange(lineStart + before.length, lineStart + marked.length))
    }
    val content = value.text.substring(start, end).ifEmpty { placeholder }
    val block = before + content + after
    return TextFieldValue(value.text.replaceRange(start, end, block), TextRange(start + before.length, start + before.length + content.length))
}

internal fun insertSourceEditorBlock(value: TextFieldValue, text: String): TextFieldValue {
    val prefix = if (value.selection.min > 0 && value.text[value.selection.min - 1] != '\n') "\n" else ""
    return replaceReplySelection(value, prefix + text + "\n")
}

internal fun insertReplyImage(value: TextFieldValue, url: String): TextFieldValue {
    val start = value.selection.min.coerceIn(0, value.text.length)
    val end = value.selection.max.coerceIn(0, value.text.length)
    val markdown = "![图片]($url)"
    val updated = value.text.substring(0, start) + markdown + value.text.substring(end)
    return TextFieldValue(updated, selection = TextRange(start + markdown.length))
}
