package sb.linux.client.ui.content

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.UnfoldLess
import androidx.compose.material.icons.filled.UnfoldMore
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import sb.linux.client.ui.theme.LocalReadingType



@Composable
fun CodeBlockView(code: String, highlightQuery: String = "", onSearchPosition: (Float) -> Unit = {}) {
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current
    // 超长判定：行数超过 12 行或字符超过 600 才视为长代码
    val longCode = remember(code) {
        code.count { it == '\n' } + 1 > 12 || code.length > 600
    }
    var expanded by remember(code) { mutableStateOf(!longCode) }
    LaunchedEffect(highlightQuery) {
        if (highlightQuery.isNotBlank() && code.contains(highlightQuery.trim(), ignoreCase = true)) expanded = true
    }
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        modifier = Modifier.padding(vertical = 4.dp)
    ) {
        Column {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(start = 12.dp, end = 2.dp, top = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "代码块",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                IconButton(
                    onClick = {
                        clipboard.setText(AnnotatedString(code))
                        android.widget.Toast.makeText(context, "已复制代码", android.widget.Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.size(30.dp)
                ) {
                    Icon(
                        Icons.Filled.ContentCopy, "复制代码",
                        Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                // 折叠按钮仅长代码显示（31）
                if (longCode) {
                    IconButton(
                        onClick = { expanded = !expanded },
                        modifier = Modifier.size(30.dp)
                    ) {
                        Icon(
                            if (expanded) Icons.Filled.UnfoldLess else Icons.Filled.UnfoldMore,
                            if (expanded) "折叠" else "展开",
                            Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            val horizontal = rememberScrollState()
            val vertical = rememberScrollState()
            val text = @Composable {
                LinkText(
                    text = addSearchHighlights(AnnotatedString(code), highlightQuery, MaterialTheme.colorScheme.tertiaryContainer),
                    onOpenUrl = {},
                    searchQuery = highlightQuery,
                    onSearchPosition = onSearchPosition,
                    // 代码 14/22 等宽；不自动换行，代码符号不被换行破坏。
                    style = LocalReadingType.current.code,
                )
            }
            // 长代码局部横滚 + 纵向限高，页面本身不整体横向滚动。
            Box(
                Modifier
                    .then(if (expanded) Modifier else Modifier.heightIn(max = 200.dp).verticalScroll(vertical))
                    .horizontalScroll(horizontal)
                    .padding(12.dp)
            ) { text() }
        }
    }
}

/**
 * 正文表格：按行列结构渲染的网格。
 * 列宽均分（weight），单元格文本自动换行，超宽表格无需横向滑动；
 * 表头行（th）加粗并加背景色区分。
 * 单元格内链接（URL/FLOOR 注解）用 LinkText 渲染，可点击跳转。
 */
