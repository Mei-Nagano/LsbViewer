package sb.linux.client.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.navigation.NavHostController
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import sb.linux.client.domain.sticker.StickerItem
import sb.linux.client.domain.sticker.StickerPickerConfig
import sb.linux.client.domain.sticker.StickerScope
import sb.linux.client.ui.content.StickerAsyncImage
import sb.linux.client.ui.session.Session

/**
 * 原生表情包选择器：对齐源站 sticker_pack 插件全部能力。
 * - 站点表情 / 最近使用 / 我的表情（按 hideMine 隐藏，按用户记忆上次选中的标签）
 * - 搜索标题（180ms 防抖，上限 80 字）
 * - 单击在当前选区插入 Markdown 并异步上报最近使用（不关闭面板，支持连选）
 * - 长按预览大图与标题
 * - 分页与失败重试
 * - 「我的表情」下提供「管理我的表情」入口，关闭管理页返回后自动刷新列表
 */
@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
internal fun StickerPickerDialog(
    session: Session,
    config: StickerPickerConfig,
    onDismiss: () -> Unit,
    onInsert: (StickerItem) -> String?,
) {
    val repo = session.stickerPackRepository
    val scopes = remember(config.hideMine) { config.availableScopes() }
    var scope by remember(config.userId, config.hideMine) {
        val saved = repo.savedScope(config.userId)
        mutableStateOf(if (saved != null && saved in scopes) saved else StickerScope.SYSTEM)
    }
    var query by remember { mutableStateOf("") }
    var page by remember { mutableIntStateOf(1) }
    var items by remember { mutableStateOf<List<StickerItem>>(emptyList()) }
    var hasMore by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(true) }
    var failed by remember { mutableStateOf(false) }
    var statusText by remember { mutableStateOf("正在加载…") }
    var reloadNonce by remember { mutableIntStateOf(0) }
    var previewItem by remember { mutableStateOf<StickerItem?>(null) }
    var manageOpen by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(config, scope, query, page, reloadNonce) {
        if (query.isNotEmpty() && page == 1) delay(180)
        loading = true
        failed = false
        statusText = "正在加载…"
        try {
            val result = repo.loadPage(config, scope, query, page)
            items = result.items
            hasMore = result.more
            statusText = when {
                result.items.isNotEmpty() -> ""
                scope == StickerScope.RECENT && query.isBlank() -> "还没有最近使用的表情"
                else -> "没有找到表情"
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            items = emptyList()
            hasMore = false
            failed = true
            statusText = "加载失败，请重试"
        } finally {
            loading = false
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 24.dp)
                .widthIn(max = 540.dp)
                .heightIn(max = 560.dp),
            shape = MaterialTheme.shapes.extraLarge,
            tonalElevation = 6.dp,
            color = MaterialTheme.colorScheme.surface,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "选择表情",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Filled.Close, "关闭表情面板")
                    }
                }

                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    scopes.forEachIndexed { index, tab ->
                        SegmentedButton(
                            selected = scope == tab,
                            onClick = {
                                if (scope != tab) {
                                    scope = tab
                                    page = 1
                                    repo.saveScope(config.userId, tab)
                                }
                            },
                            shape = SegmentedButtonDefaults.itemShape(index = index, count = scopes.size),
                            label = { Text(tab.label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        )
                    }
                }

                OutlinedTextField(
                    value = query,
                    onValueChange = { next ->
                        if (next.length <= 80) {
                            query = next
                            page = 1
                        }
                    },
                    placeholder = { Text("搜索表情标题") },
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Filled.Search, null) },
                    trailingIcon = if (query.isNotEmpty()) {
                        {
                            IconButton(onClick = { query = ""; page = 1 }) {
                                Icon(Icons.Filled.Close, "清空搜索")
                            }
                        }
                    } else null,
                    shape = MaterialTheme.shapes.large,
                    modifier = Modifier.fillMaxWidth(),
                )

                Text(
                    text = statusText.ifBlank { "点击插入，长按预览" },
                    style = MaterialTheme.typography.bodySmall,
                    color = if (failed) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = true)
                        .heightIn(min = 180.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    if (loading && items.isEmpty()) {
                        CircularProgressIndicator(modifier = Modifier.size(28.dp))
                    } else if (items.isNotEmpty()) {
                        LazyVerticalGrid(
                            columns = GridCells.Adaptive(minSize = 54.dp),
                            modifier = Modifier.fillMaxSize(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            items(items, key = { "${it.id}:${it.url}" }) { item ->
                                Box(
                                    modifier = Modifier
                                        .size(54.dp)
                                        .clip(MaterialTheme.shapes.small)
                                        .combinedClickable(
                                            onClick = {
                                                val reject = onInsert(item)
                                                if (reject != null) {
                                                    statusText = reject
                                                } else {
                                                    statusText = if (item.title.isNotBlank()) "已插入「${item.title}」" else "已插入表情"
                                                    coroutineScope.launch { repo.recordUsage(config, item.id) }
                                                }
                                            },
                                            onLongClick = { previewItem = item },
                                        ),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    StickerAsyncImage(
                                        url = item.url,
                                        contentDescription = item.title.ifBlank { "表情" },
                                        modifier = Modifier.size(40.dp),
                                        contentScale = ContentScale.Fit,
                                    )
                                }
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        if (failed) {
                            OutlinedButton(onClick = { reloadNonce++ }) { Text("重试") }
                        } else if (page > 1 || hasMore) {
                            OutlinedButton(
                                onClick = { if (page > 1) page-- },
                                enabled = page > 1 && !loading,
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            ) { Text("上一页") }
                            Text(
                                "第 $page 页",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 4.dp),
                            )
                            OutlinedButton(
                                onClick = { if (hasMore) page++ },
                                enabled = hasMore && !loading,
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            ) { Text("下一页") }
                        }
                    }
                    if (scope == StickerScope.MINE && config.manageUrl.isNotBlank()) {
                        TextButton(onClick = { manageOpen = true }) {
                            Text("管理我的表情")
                        }
                    }
                }
            }
        }
    }

    previewItem?.let { item ->
        AlertDialog(
            onDismissRequest = { previewItem = null },
            title = if (item.title.isNotBlank()) {
                { Text(item.title, style = MaterialTheme.typography.titleMedium) }
            } else null,
            text = {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    StickerAsyncImage(
                        url = item.url,
                        contentDescription = item.title.ifBlank { "表情预览" },
                        modifier = Modifier
                            .widthIn(min = 64.dp, max = item.width.coerceIn(64, 240).dp)
                            .heightIn(min = 64.dp, max = 240.dp),
                        contentScale = ContentScale.Fit,
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val target = item
                    previewItem = null
                    val reject = onInsert(target)
                    if (reject != null) {
                        statusText = reject
                    } else {
                        statusText = if (target.title.isNotBlank()) "已插入「${target.title}」" else "已插入表情"
                        coroutineScope.launch { repo.recordUsage(config, target.id) }
                    }
                }) { Text("插入") }
            },
            dismissButton = {
                TextButton(onClick = { previewItem = null }) { Text("关闭") }
            },
        )
    }

    if (manageOpen) {
        StickerManageOverlayDialog(
            session = session,
            url = config.manageUrl.ifBlank { "/sticker_pack_mine" },
            onDismiss = {
                manageOpen = false
                if (scope == StickerScope.MINE) reloadNonce++
            },
        )
    }
}

/** 编辑器内打开「管理我的表情」：全屏模态承载源站管理页，不丢弃编辑草稿；关闭后自动刷新我的表情。 */
@Composable
internal fun StickerManageOverlayDialog(
    session: Session,
    url: String,
    onDismiss: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
            EmbeddedSourceWebPage(
                session = session,
                url = url,
                title = "管理我的表情",
                initialContentSelector = ".sticker_pack_manage, main",
                onClose = onDismiss,
            )
        }
    }
}

/** 独立路由页：管理我的表情（与称号/个人资料一样使用源站页面承载全部上传、编辑、预览与删除能力）。 */
@Composable
fun SourceStickerManageScreen(
    session: Session,
    nav: NavHostController,
    url: String = "/sticker_pack_mine",
) {
    val account = session.account.loginState
    AccountPageGate(account.loggedIn && account.userId > 0, "我的表情", "登录后管理我的表情", nav) {
        WebScreen(
            session = session,
            nav = nav,
            sourceUrl = url.ifBlank { "/sticker_pack_mine" },
            title = "我的表情",
            initialContentSelector = ".sticker_pack_manage, main",
        )
    }
}

/** 无 NavBackStackEntry 依赖的轻量内嵌 WebView 壳，供弹窗内直接打开源站管理页。 */
@Composable
private fun EmbeddedSourceWebPage(
    session: Session,
    url: String,
    title: String,
    initialContentSelector: String?,
    onClose: () -> Unit,
) {
    val absUrl = remember(url) { sb.linux.client.common.network.Endpoints.abs(url) }
    val initialPath = remember(absUrl) { android.net.Uri.parse(absUrl).path }
    var loading by remember { mutableStateOf(true) }
    var loadProgress by remember { mutableIntStateOf(0) }
    var webView by remember { mutableStateOf<android.webkit.WebView?>(null) }
    var initialLoadFinished by remember(absUrl) { mutableStateOf(false) }
    val chooseFiles = rememberWebFileChooser()
    val windows = rememberSourceWebWindows()

    sb.linux.client.ui.components.navigation.AppPageScaffold(
        title = title,
        onBack = {
            if (windows.back()) Unit
            else if (webView?.canGoBack() == true) webView?.goBack()
            else onClose()
        },
        actions = {
            windows.WindowMenu()
            sb.linux.client.ui.components.navigation.AppRefreshButton(
                onClick = { (windows.activeWebView ?: webView)?.reload() },
                enabled = webView != null,
                loading = windows.activeWebView == null && loading,
            )
            IconButton(onClick = onClose) {
                Icon(Icons.Filled.Close, "关闭")
            }
        },
    ) { pad ->
        Box(Modifier.padding(pad).fillMaxSize()) {
            androidx.compose.ui.viewinterop.AndroidView(
                factory = { ctx ->
                    val primary = android.webkit.WebView(ctx).apply {
                        settings.javaScriptEnabled = true
                        settings.javaScriptCanOpenWindowsAutomatically = true
                        settings.domStorageEnabled = true
                        settings.userAgentString = sb.linux.client.common.network.ForumUserAgent.DEFAULT
                        settings.setSupportMultipleWindows(true)
                        webChromeClient = sb.linux.client.platform.webview.LinkWindowClient(
                            showWindow = windows::show,
                            closeWindow = windows::close,
                            showJsDialog = windows::showJsDialog,
                            chooseFiles = chooseFiles,
                            onProgress = { p -> loadProgress = p; loading = p < 100 },
                            onTitle = windows::title,
                            useNativeJsDialogs = true,
                        )
                        session.accountAuthenticationRepository.exportWebCookies()
                        webViewClient = object : android.webkit.WebViewClient() {
                            override fun onPageStarted(view: android.webkit.WebView, url: String, favicon: android.graphics.Bitmap?) {
                                loading = true; loadProgress = 0
                            }
                            override fun onPageFinished(view: android.webkit.WebView, finishedUrl: String) {
                                loading = false
                                val origin = android.net.Uri.parse(finishedUrl)
                                if (origin.scheme == "https" && sb.linux.client.common.network.Endpoints.isForumHost(origin.host)) {
                                    session.accountAuthenticationRepository.importWebCookies(finishedUrl)
                                }
                                val firstLoad = !initialLoadFinished
                                initialLoadFinished = true
                                if (firstLoad && initialContentSelector != null &&
                                    sb.linux.client.common.network.Endpoints.isInternal(origin.host) &&
                                    origin.path?.trimEnd('/') == initialPath?.trimEnd('/')
                                ) {
                                    view.evaluateJavascript(
                                        """(function(){var s=${org.json.JSONObject.quote(initialContentSelector)}.split(',');var t=s.map(function(x){return document.querySelector(x.trim())}).find(Boolean);if(t)t.scrollIntoView({block:'start'});})()""",
                                        null,
                                    )
                                }
                            }
                        }
                        sb.linux.client.platform.webview.WebViewDoh.load(this, absUrl)
                    }.also { webView = it }
                    sb.linux.client.platform.webview.WebViewWindowHost(ctx).apply { show(listOf(primary), primary) }
                },
                update = { windows.updateHost(it, webView) },
                modifier = Modifier.fillMaxSize().padding(top = if (windows.activeWebView != null) 56.dp else 0.dp),
                onRelease = { host ->
                    host.removeAllViews()
                    webView?.let { windows.close(it); it.stopLoading(); it.destroy() }
                    webView = null
                },
            )
            if (loading) {
                if (loadProgress > 0) LinearProgressIndicator(
                    progress = { loadProgress / 100f },
                    modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter),
                ) else LinearProgressIndicator(Modifier.fillMaxWidth().align(Alignment.TopCenter))
            }
            windows.InlineControls()
        }
    }
}
