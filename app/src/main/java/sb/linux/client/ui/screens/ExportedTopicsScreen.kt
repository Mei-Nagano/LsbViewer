package sb.linux.client.ui.screens
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import sb.linux.client.ui.session.Session
import sb.linux.client.ui.EmptyBox
import sb.linux.client.ui.LoadingBox
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportedTopicsScreen(session: Session, nav: NavHostController) {
    val context = LocalContext.current
    var files by remember { mutableStateOf<List<java.io.File>?>(null) }
    var deleteTarget by remember { mutableStateOf<java.io.File?>(null) }

    fun refresh() {
        files = java.io.File(context.filesDir, "exports").takeIf { it.isDirectory }
            ?.listFiles { f -> f.isFile && f.extension.equals("html", true) }
            ?.sortedByDescending { it.lastModified() }
            ?: emptyList()
    }

    LaunchedEffect(Unit) { refresh() }

    fun openFile(f: java.io.File) {
        // 应用内查看（3.21）：直接路由到本地 HTML 查看页，不再调起系统浏览器
        nav.navigate("exportedHtml?path=${android.net.Uri.encode(f.absolutePath)}")
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("已导出的帖子") },
                navigationIcon = {
                    IconButton(onClick = { nav.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                    }
                },
                actions = {
                    IconButton(onClick = { refresh() }) { Icon(Icons.Filled.Refresh, "刷新") }
                }
            )
        }
    ) { pad ->
        Box(Modifier.padding(pad).fillMaxSize()) {
            when {
                files == null -> LoadingBox()
                files!!.isEmpty() -> EmptyBox("暂无已导出的帖子\n在帖子右上角菜单选择「导出帖子」导出 HTML")
                else -> LazyColumn(
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(files!!, key = { it.absolutePath }) { f ->
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerLow,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(18.dp))
                                .clickable { openFile(f) }
                        ) {
                            Row(
                                Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        f.nameWithoutExtension,
                                        style = MaterialTheme.typography.titleSmall,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        "${fmtBytes(f.length())} · " +
                                            java.text.SimpleDateFormat(
                                                "yyyy-MM-dd HH:mm",
                                                androidx.compose.ui.platform.LocalConfiguration.current.locales[0]
                                            ).format(java.util.Date(f.lastModified())),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                IconButton(onClick = { deleteTarget = f }) {
                                    Icon(
                                        Icons.Filled.Delete,
                                        "删除",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // 删除确认
    deleteTarget?.let { f ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("删除导出文件") },
            text = { Text("确定删除「${f.nameWithoutExtension}」吗？该操作不可恢复。") },
            confirmButton = {
                TextButton(onClick = {
                    runCatching { f.delete() }
                    deleteTarget = null
                    refresh()
                }) { Text("删除") }
            },
            dismissButton = { TextButton(onClick = { deleteTarget = null }) { Text("取消") } }
        )
    }
}

// ==================== 关于应用（3.18） ====================

/** 关于应用：图标 / 名称 / 版本号 / 简介 / List group（开发人员、项目地址、开源协议、检查更新） */
