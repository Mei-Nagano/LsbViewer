package sb.linux.client.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.foundation.text.*
import androidx.compose.foundation.text.selection.*
import androidx.compose.material.icons.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.*
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.platform.*
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.*
import androidx.compose.ui.text.input.*
import androidx.compose.ui.text.style.*
import androidx.compose.ui.unit.*
import androidx.compose.ui.window.*
import sb.linux.client.ui.*
import sb.linux.client.ui.theme.*
import sb.linux.client.ui.content.*
import sb.linux.client.ui.patterns.topic.*
import sb.linux.client.ui.patterns.user.*
import sb.linux.client.ui.components.feedback.*
import sb.linux.client.ui.components.layout.*
import sb.linux.client.ui.components.navigation.*

import sb.linux.client.domain.topic.PostEntry
/** 导出格式选择弹窗：MD3 底部弹层，圆角色调图标容器 + 标题/副标题 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportDialog(
    posts: List<PostEntry>,
    busy: String?,
    loadingPosts: Boolean = false,
    allLoaded: Boolean = false,
    onLoadAll: () -> Unit = {},
    onDismiss: () -> Unit,
    onExport: (kind: String, scopeMode: String, fromFloor: Int, toFloor: Int, multiPage: Boolean, selectedIds: Set<Long>) -> Unit,
) {
    var scopeMode by remember { mutableStateOf("all") }
    var fromFloor by remember { mutableStateOf("0") }
    var toFloor by remember { mutableStateOf("20") }
    var multiPage by remember { mutableStateOf(true) }
    var selectedIds by remember { mutableStateOf(posts.map { it.id }.toSet()) }
    ModalBottomSheet(onDismissRequest = { if (busy == null) onDismiss() },
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true), sheetGesturesEnabled = false) {
        Column(Modifier.fillMaxHeight(0.9f).verticalScroll(rememberScrollState()).padding(horizontal = LocalAppSpacing.current.pageHorizontal)) {
            Row(
                Modifier.padding(start = 4.dp, top = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("导出帖子", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onDismiss, enabled = busy == null) { Text("关闭") }
            }
            Text(
                "选择内容范围和格式，文件保存后可在「设置 → 已导出的帖子」查看",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
            )
            TextButton(enabled = busy == null && !loadingPosts && !allLoaded, onClick = onLoadAll) {
                Text(if (loadingPosts) "正在加载完整帖子…" else if (allLoaded) "已加载完整帖子（${posts.size} 楼）" else "加载完整帖子后导出（当前 ${posts.size} 楼）")
            }
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                listOf("all" to "已加载", "main" to "仅正文", "range" to "范围", "selected" to "勾选").forEachIndexed { index, (value, label) ->
                    SegmentedButton(
                        selected = scopeMode == value,
                        onClick = { scopeMode = value },
                        shape = SegmentedButtonDefaults.itemShape(index, 4),
                    ) { Text(label) }
                }
            }
            if (scopeMode == "range") {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = fromFloor, onValueChange = { fromFloor = it.filter(Char::isDigit) },
                        label = { Text("起始楼层") }, singleLine = true, modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        value = toFloor, onValueChange = { toFloor = it.filter(Char::isDigit) },
                        label = { Text("结束楼层") }, singleLine = true, modifier = Modifier.weight(1f),
                    )
                }
            }
            if (scopeMode == "selected") {
                Row {
                    TextButton(onClick = { selectedIds = posts.map { it.id }.toSet() }) { Text("全选") }
                    TextButton(onClick = { selectedIds = emptySet() }) { Text("清空") }
                    Text("已选 ${selectedIds.size} 层", modifier = Modifier.padding(12.dp))
                }
                LazyColumn(Modifier.fillMaxWidth().heightIn(max = 220.dp)) {
                    items(posts, key = { it.id }) { post ->
                        Row(Modifier.fillMaxWidth().clickable {
                            selectedIds = if (post.id in selectedIds) selectedIds - post.id else selectedIds + post.id
                        }, verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = post.id in selectedIds, onCheckedChange = null)
                            Text(if (post.floor == 0) "正文 · ${post.authorName}" else "#${post.floor} · ${post.authorName}", maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
            sb.linux.client.ui.components.input.AppSwitchRow(
                title = "长图自动分页",
                subtitle = "按实际高度分页，单篇超长正文也完整导出",
                checked = multiPage,
                onCheckedChange = { multiPage = it },
                compact = true,
            )
            listOf(
                Triple("html", "导出 HTML", "保留原帖结构与样式，适合存档"),
                Triple("md", "导出 Markdown", "纯文本轻量格式，便于编辑"),
                Triple("img", "导出长图", "渲染为图片，方便直接分享"),
            ).forEach { (kind, label, subtitle) ->
                ExportSheetItem(
                    icon = when (kind) {
                        "html" -> Icons.Filled.Code
                        "md" -> Icons.Filled.Title
                        else -> Icons.Filled.Image
                    },
                    title = label,
                    subtitle = subtitle,
                    busy = busy == kind,
                    enabled = busy == null && !loadingPosts && (scopeMode != "selected" || selectedIds.isNotEmpty()),
                    onClick = {
                        onExport(
                            kind,
                            scopeMode,
                            fromFloor.toIntOrNull() ?: 0,
                            toFloor.toIntOrNull() ?: Int.MAX_VALUE,
                            multiPage,
                            selectedIds,
                        )
                    }
                )
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

/** MD3 列表项：圆角色调图标容器 + 标题/副标题（导出弹层用） */
@Composable
private fun ExportSheetItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    busy: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            Modifier
                .size(42.dp)
                .clip(MaterialTheme.shapes.large)
                .background(MaterialTheme.colorScheme.secondaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon, null, Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
        Column(Modifier.weight(1f)) {
            Text(if (busy) "导出中…" else title, style = MaterialTheme.typography.bodyLarge)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
