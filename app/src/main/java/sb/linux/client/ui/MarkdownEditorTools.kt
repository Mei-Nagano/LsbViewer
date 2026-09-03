package sb.linux.client.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

private data class EditorTool(val icon: ImageVector, val label: String, val before: String, val after: String = "", val placeholder: String = "")

@Composable
fun MarkdownEditorTools(onInsert: (String, String, String) -> Unit, onUpload: () -> Unit, uploading: Boolean = false) {
    val tools = listOf(
        EditorTool(Icons.Filled.FormatBold, "粗体", "**", "**", "粗体"),
        EditorTool(Icons.Filled.FormatItalic, "斜体", "*", "*", "斜体"),
        EditorTool(Icons.Filled.FormatStrikethrough, "删除线", "~~", "~~", "删除线"),
        EditorTool(Icons.Filled.Title, "标题", "## ", placeholder = "标题"),
        EditorTool(Icons.Filled.FormatQuote, "引用", "> ", placeholder = "引用"),
        EditorTool(Icons.Filled.Code, "行内代码", "`", "`", "代码"),
        EditorTool(Icons.Filled.DataObject, "代码块", "```\n", "\n```", "代码块"),
        EditorTool(Icons.AutoMirrored.Filled.FormatListBulleted, "列表", "- ", placeholder = "列表项"),
        EditorTool(Icons.Filled.FormatListNumbered, "有序列表", "1. ", placeholder = "列表项"),
        EditorTool(Icons.Filled.Link, "链接", "[", "](https://)", "链接文字"),
        EditorTool(Icons.Filled.TableChart, "表格", "\n| 列 1 | 列 2 |\n| --- | --- |\n| 内容 | 内容 |\n"),
        EditorTool(Icons.Filled.HorizontalRule, "分割线", "\n\n---\n\n"),
        EditorTool(Icons.Filled.Image, "上传图片", ""),
    )
    Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surfaceContainerLow) {
        Column(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(4.dp)) {
            tools.chunked(7).forEach { row ->
                Row {
                    row.forEach { tool ->
                        IconButton(onClick = {
                            if (tool.label == "上传图片") onUpload() else onInsert(tool.before, tool.after, tool.placeholder)
                        }, enabled = tool.label != "上传图片" || !uploading) {
                            if (tool.label == "上传图片" && uploading) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                            else Icon(tool.icon, tool.label, Modifier.size(20.dp))
                        }
                    }
                }
            }
        }
    }
}
