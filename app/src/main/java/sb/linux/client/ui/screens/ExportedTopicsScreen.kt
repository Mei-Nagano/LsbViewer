package sb.linux.client.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import sb.linux.client.ui.components.feedback.AppConfirmDialog
import sb.linux.client.ui.components.feedback.EmptyBox
import sb.linux.client.ui.components.feedback.LoadingBox
import sb.linux.client.ui.components.navigation.AppPageScaffold
import sb.linux.client.ui.session.Session
import sb.linux.client.ui.theme.LocalAppSpacing
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date

/**
 * 已导出的本地 HTML：平面列表显示真实格式、大小与状态；点击应用内查看，删除只删本地文件。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportedTopicsScreen(session: Session, nav: NavHostController) {
    val context = LocalContext.current
    val locale = LocalConfiguration.current.locales[0]
    val dateFormat = remember(locale) { SimpleDateFormat("yyyy-MM-dd HH:mm", locale) }
    var files by remember { mutableStateOf<List<File>?>(null) }
    var deleteTarget by remember { mutableStateOf<File?>(null) }

    fun refresh() {
        files = File(context.filesDir, "exports").takeIf { it.isDirectory }
            ?.listFiles { f -> f.isFile && f.extension.equals("html", true) }
            ?.sortedByDescending { it.lastModified() }
            ?: emptyList()
    }

    LaunchedEffect(Unit) { refresh() }

    fun openFile(f: File) {
        nav.navigate("exportedHtml?path=${android.net.Uri.encode(f.absolutePath)}")
    }

    AppPageScaffold(
        title = "已导出的帖子",
        onBack = { nav.popBackStack() },
        actions = {
            IconButton(onClick = { refresh() }) { Icon(Icons.Filled.Refresh, "刷新") }
        },
    ) { pad ->
        Column(Modifier.padding(pad).fillMaxSize()) {
            val list = files
            when {
                list == null -> LoadingBox()
                list.isEmpty() -> EmptyBox("暂无已导出的帖子", "在帖子右上角菜单选择「导出帖子」导出 HTML")
                else -> LazyColumn(contentPadding = PaddingValues(vertical = LocalAppSpacing.current.pageVertical)) {
                    items(list, key = { it.absolutePath }) { f ->
                        Row(
                            Modifier.fillMaxWidth().clickable { openFile(f) }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    f.nameWithoutExtension,
                                    style = MaterialTheme.typography.titleMedium,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    exportMeta(f, dateFormat),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            IconButton(onClick = { deleteTarget = f }, modifier = Modifier.size(48.dp)) {
                                Icon(
                                    Icons.Filled.Delete,
                                    contentDescription = "删除文件",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        HorizontalDivider(
                            Modifier.padding(start = 16.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                        )
                    }
                }
            }
        }
    }

    deleteTarget?.let { f ->
        AppConfirmDialog(
            title = "删除导出文件",
            message = "确定删除「${f.nameWithoutExtension}」吗？只删除本机 HTML 文件，不影响源站帖子。",
            onDismiss = { deleteTarget = null },
            onConfirm = {
                runCatching { f.delete() }
                deleteTarget = null
                refresh()
            },
            confirmText = "删除", destructive = true, dismissText = "取消",
        )
    }
}

/** 真实格式/状态：空文件与不可读分别说明，不伪装成正常记录。 */
private fun exportMeta(file: File, dateFormat: SimpleDateFormat): String {
    val state = when {
        !file.canRead() -> "无法读取"
        file.length() == 0L -> "空文件"
        else -> fmtBytes(file.length())
    }
    return "HTML · $state · ${dateFormat.format(Date(file.lastModified()))}"
}
