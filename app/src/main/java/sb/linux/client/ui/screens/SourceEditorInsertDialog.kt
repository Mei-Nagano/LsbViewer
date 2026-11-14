package sb.linux.client.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** 与 NB 编辑器相同的链接、媒体、公式、表格和隐藏内容插入语法。 */
@Composable
internal fun SourceEditorInsertDialog(tool: String, selection: String, onDismiss: () -> Unit,
    onInsert: (String) -> Unit, onVideo: (String) -> Unit) {
    var value by remember(tool) { mutableStateOf(when (tool) {
        "链接" -> "https://"; "插入公式" -> selection.ifBlank { "E = mc^2" }; "回复可见" -> selection
        else -> ""
    }) }
    var description by remember(tool) { mutableStateOf(selection.ifBlank { if (tool == "链接") "链接文字" else "图片描述" }) }
    var display by remember(tool) { mutableStateOf(false) }
    var columns by remember { mutableStateOf("2") }
    var rows by remember { mutableStateOf("2") }
    AlertDialog(onDismissRequest = onDismiss, title = { Text(tool) }, text = {
        Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (tool == "表格") {
                OutlinedTextField(columns, { columns = it.filter(Char::isDigit) }, label = { Text("列数（1–12）") })
                OutlinedTextField(rows, { rows = it.filter(Char::isDigit) }, label = { Text("正文行数（1–30）") })
            } else {
                OutlinedTextField(value, { value = it }, label = { Text(when (tool) {
                    "链接" -> "链接地址"; "插入图片" -> "图片地址"; "插入视频" -> "MP4、抖音、哔哩哔哩或 YouTube 分享链接"
                    "插入公式" -> "公式内容"; else -> "回复后可见的内容（支持 Markdown）"
                }) }, minLines = if (tool in setOf("插入公式", "回复可见")) 3 else 1)
                if (tool in setOf("插入图片", "链接")) OutlinedTextField(description, { description = it }, label = { Text("显示文字") })
                if (tool == "插入公式") Row { Checkbox(display, { display = it }); Text("独立公式") }
            }
        }
    }, confirmButton = {
        TextButton(enabled = tool == "表格" || value.isNotBlank(), onClick = {
            if (tool == "插入视频") onVideo(value.trim())
            else onInsert(when (tool) {
                "链接" -> "[$description](${value.trim()})"
                "插入图片" -> "![$description](${value.trim()})"
                "插入公式" -> if (display) "\n$$\n${value.trim()}\n$$\n" else "\\(${value.trim().replace('\n', ' ')}\\)"
                "回复可见" -> "\n[回复可见]\n${value.trim()}\n[/回复可见]\n"
                "表格" -> {
                    val cols = columns.toIntOrNull()?.coerceIn(1, 12) ?: 2
                    val count = rows.toIntOrNull()?.coerceIn(1, 30) ?: 2
                    "\n| " + (1..cols).joinToString(" | ") { "列 $it" } + " |\n| " + List(cols) { "---" }.joinToString(" | ") + " |\n" +
                        List(count) { "| " + List(cols) { "内容" }.joinToString(" | ") + " |" }.joinToString("\n") + "\n"
                }
                else -> value
            })
        }) { Text("插入") }
    }, dismissButton = { TextButton(onClick = onDismiss) { Text("关闭") } })
}
