package sb.linux.client.ui.screens

import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.*
import androidx.compose.ui.text.input.TextFieldValue

/** 软键盘通过文本更新写入回车，使用相同的续写规则。 */
internal fun continueSourceEditorLine(previous: TextFieldValue, next: TextFieldValue): TextFieldValue {
    if (previous.composition != null || next.composition != null || !previous.selection.collapsed) return next
    val end = previous.selection.end
    if (next.text != previous.text.substring(0, end) + "\n" + previous.text.substring(end)) return next
    val lineStart = previous.text.substring(0, end).lastIndexOf('\n') + 1
    val match = Regex("^(\\s*)([-+*]|\\d+[.)]|>)\\s+(.*)$").find(previous.text.substring(lineStart, end)) ?: return next
    if (match.groupValues[3].isBlank()) return replaceReplySelection(
        previous.copy(selection = androidx.compose.ui.text.TextRange(lineStart, end)), "")
    val marker = match.groupValues[2]
    val number = marker.dropLast(1).toIntOrNull()
    val prefix = match.groupValues[1] + if (number == null) marker else "${number + 1}${marker.last()}"
    return replaceReplySelection(next, "$prefix ")
}

/** 中文选词不拦截回车；Ctrl/Cmd+Enter 提交，普通回车续写列表与引用。 */
internal fun Modifier.sourceEditorKeys(value: TextFieldValue, enabled: Boolean,
    onChange: (TextFieldValue) -> Unit, onSubmit: () -> Unit): Modifier = onPreviewKeyEvent { event ->
    if (!enabled || event.type != KeyEventType.KeyDown || event.key != Key.Enter || value.composition != null) return@onPreviewKeyEvent false
    if (event.isCtrlPressed || event.isMetaPressed) { onSubmit(); return@onPreviewKeyEvent true }
    if (event.isShiftPressed || event.isAltPressed || !value.selection.collapsed) return@onPreviewKeyEvent false
    val end = value.selection.end
    val lineStart = value.text.substring(0, end).lastIndexOf('\n') + 1
    val line = value.text.substring(lineStart, end)
    val match = Regex("^(\\s*)([-+*]|\\d+[.)]|>)\\s+(.*)$").find(line) ?: return@onPreviewKeyEvent false
    val prefix = match.groupValues[1]
    val marker = match.groupValues[2]
    if (match.groupValues[3].isBlank()) onChange(replaceReplySelection(value.copy(selection = androidx.compose.ui.text.TextRange(lineStart, end)), ""))
    else {
        val number = marker.dropLast(1).toIntOrNull()
        onChange(replaceReplySelection(value, "\n$prefix${if (number == null) marker else "${number + 1}${marker.last()}"} "))
    }
    true
}
