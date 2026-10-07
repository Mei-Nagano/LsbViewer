package sb.linux.client.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TopicTopBar(
    modifier: Modifier,
    title: String,
    showTitle: Boolean,
    searchOpen: Boolean,
    menuExpanded: Boolean,
    danmakuVisible: Boolean,
    exporting: Boolean,
    infiniteScroll: Boolean,
    onMenuExpandedChange: (Boolean) -> Unit,
    onBack: () -> Unit,
    onComments: () -> Unit,
    onJumpFloor: () -> Unit,
    onSearch: () -> Unit,
    onOpenBrowser: () -> Unit,
    onCopyLink: () -> Unit,
    onToggleDanmaku: () -> Unit,
    onExport: () -> Unit,
    onCollections: () -> Unit,
    onRefresh: () -> Unit,
    onToggleScrollMode: () -> Unit,
) {
    if (!searchOpen) TopAppBar(
        modifier = modifier,
        title = {
            AnimatedVisibility(visible = showTitle, enter = fadeIn(), exit = fadeOut()) {
                Text(title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        },
        navigationIcon = {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回") }
        },
        actions = {
            IconButton(onClick = onComments) { Icon(Icons.AutoMirrored.Filled.Chat, "直达评论区") }
            Box {
                IconButton(onClick = { onMenuExpandedChange(true) }) { Icon(Icons.Filled.MoreVert, "菜单") }
                DropdownMenu(expanded = menuExpanded, onDismissRequest = { onMenuExpandedChange(false) }) {
                    DropdownMenuItem(
                        text = { Text("跳转楼层") },
                        leadingIcon = { Icon(Icons.Filled.FormatListNumbered, null) },
                        onClick = { onMenuExpandedChange(false); onJumpFloor() },
                    )
                    DropdownMenuItem(
                        text = { Text("帖内搜索") },
                        leadingIcon = { Icon(Icons.Filled.Search, null) },
                        onClick = { onMenuExpandedChange(false); onSearch() },
                    )
                    DropdownMenuItem(
                        text = { Text("在浏览器打开") },
                        leadingIcon = { Icon(Icons.Filled.OpenInNew, null) },
                        onClick = { onMenuExpandedChange(false); onOpenBrowser() },
                    )
                    DropdownMenuItem(
                        text = { Text("复制链接") },
                        leadingIcon = { Icon(Icons.Filled.Link, null) },
                        onClick = { onMenuExpandedChange(false); onCopyLink() },
                    )
                    DropdownMenuItem(
                        text = { Text(if (danmakuVisible) "关闭打赏弹幕" else "显示打赏弹幕") },
                        leadingIcon = { Icon(Icons.Filled.Subtitles, null) },
                        onClick = { onMenuExpandedChange(false); onToggleDanmaku() },
                    )
                    DropdownMenuItem(
                        text = { Text(if (exporting) "导出中…" else "导出帖子") },
                        leadingIcon = { Icon(Icons.Filled.IosShare, null) },
                        onClick = { onMenuExpandedChange(false); onExport() },
                    )
                    DropdownMenuItem(
                        text = { Text("收录到淘帖") },
                        leadingIcon = { Icon(Icons.Filled.CollectionsBookmark, null) },
                        onClick = { onMenuExpandedChange(false); onCollections() },
                    )
                    DropdownMenuItem(
                        text = { Text("刷新") },
                        leadingIcon = { Icon(Icons.Filled.Refresh, null) },
                        onClick = { onMenuExpandedChange(false); onRefresh() },
                    )
                    DropdownMenuItem(
                        text = { Text(if (infiniteScroll) "切换为翻页模式" else "切换为无限滚动") },
                        leadingIcon = { Icon(Icons.Filled.SwapVert, null) },
                        onClick = { onMenuExpandedChange(false); onToggleScrollMode() },
                    )
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
        windowInsets = WindowInsets(0, 0, 0, 0),
    )
}
