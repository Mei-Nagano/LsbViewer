package sb.linux.client.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmarks
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import kotlinx.coroutines.launch
import sb.linux.client.data.*
import sb.linux.client.ui.EmptyBox
import sb.linux.client.ui.ErrorBox
import sb.linux.client.ui.LoadingBox
import sb.linux.client.ui.TopicCardView

/** 源站表单驱动的淘帖管理：建专辑、收录、订阅、编辑和移除使用原字段及权限。 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollectionDetailScreen(session: Session, nav: NavHostController) {
    val initialPath = nav.currentBackStackEntry?.arguments?.getString("path").orEmpty()
    var path by remember(initialPath) { mutableStateOf(initialPath) }
    var opPage by remember { mutableStateOf<GachaOperationPage?>(null) }
    var topics by remember { mutableStateOf<List<TopicCard>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var pending by remember { mutableStateOf<Pair<GachaOperationForm, List<Pair<String, String>>>?>(null) }
    var submitting by remember { mutableStateOf(false) }
    // 收录模式（从帖子进入）以表单为主，浏览模式以内容为主。
    val addMode = path.startsWith("/topic/")
    val scope = rememberCoroutineScope()
    fun load() = scope.launch {
        loading = true; error = null
        try {
            require(Regex("^/topic(?:_collection)?/\\d+(?:\\?p=\\d+)?$").matches(path)) { "无效的专辑路径" }
            val response = session.client.get(path)
            check(!response.url.contains("login")) { "请先登录" }
            opPage = HtmlParser.parseGachaOperationPage(response.html, collectionsOnly = true)
            topics = if (path.startsWith("/topic_collection/")) HtmlParser.parseTopicList(response.html).first else emptyList()
        } catch (e: Exception) { error = e.message ?: "加载失败" }
        finally { loading = false }
    }
    LaunchedEffect(path) { load() }
    pending?.let { (form, fields) ->
        AlertDialog(
            onDismissRequest = { if (!submitting) pending = null },
            title = { Text("确认${form.label}") },
            text = { Text("此操作会同步至源站。删除或移除操作可能无法撤回，请核对后提交。") },
            dismissButton = { TextButton(enabled = !submitting, onClick = { pending = null }) { Text("取消") } },
            confirmButton = { Button(enabled = !submitting, onClick = {
                submitting = true
                scope.launch {
                    try {
                        val result = session.client.postFormPairs(form.action,
                            listOf("_csrf" to session.client.csrf()) + fields.filterNot { it.first == "_csrf" })
                        check(!result.url.contains("login")) { "登录已失效" }
                        check(!result.url.contains("form_error")) { HtmlParser.extractError(result.html) }
                        session.showToast("已提交并同步专辑")
                        load()
                    } catch (e: Exception) { session.showToast(e.message ?: "提交失败") }
                    finally { submitting = false; pending = null }
                }
            }) { Text(if (submitting) "提交中…" else "确认") } },
        )
    }
    val pageLinks = opPage?.links.orEmpty().filter { Regex("^/topic_collection/\\d+(?:\\?p=\\d+)?$").matches(it.second) }
    val forms = opPage?.forms.orEmpty()
    // 订阅/取消订阅提到顶栏：这是专辑页最常用的一步，不用和删除、移除挤在同一排里找
    val subscribeForm = forms.firstOrNull { it.fields.isEmpty() && it.label.contains("订阅") }
    Scaffold(topBar = { TopAppBar(
        title = { Text(if (addMode) "收录到淘帖专辑" else opPage?.title.orEmpty().ifBlank { "淘帖专辑" }, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        navigationIcon = { IconButton(onClick = { nav.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回") } },
        actions = {
            subscribeForm?.let { form ->
                FilledTonalButton(
                    enabled = form.enabled && !submitting && !loading,
                    onClick = { pending = form to form.hiddenFields },
                    shape = RoundedCornerShape(50),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                    // 源站文案是「订阅专辑」，顶栏里已经在专辑页，去掉后缀省出标题的位置
                ) { Text(form.label.removeSuffix("专辑"), style = MaterialTheme.typography.labelLarge, maxLines = 1) }
            }
            IconButton(enabled = !loading, onClick = { load() }) { Icon(Icons.Filled.Refresh, "刷新") }
        },
    ) }) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            when {
                loading && topics.isEmpty() && forms.isEmpty() -> LoadingBox()
                error != null && topics.isEmpty() && forms.isEmpty() -> ErrorBox(error!!) { load() }
                !loading && error == null && topics.isEmpty() && forms.isEmpty() ->
                    EmptyBox("当前没有内容或可执行操作\n请确认登录状态和专辑权限")
                else -> CollectionDetailList(
                    nav = nav, opPage = opPage, topics = topics,
                    forms = forms.filterNot { it === subscribeForm },
                    pageLinks = pageLinks, path = path, loading = loading, error = error, addMode = addMode,
                    submitting = submitting,
                    onPath = { path = it }, onReload = { load() }, onPending = { pending = it },
                )
            }
        }
    }
}

/** 专辑内容列表：操作按钮 → 说明卡 → 帖子卡片 → 分页胶囊。 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CollectionDetailList(
    nav: NavHostController,
    opPage: GachaOperationPage?,
    topics: List<TopicCard>,
    forms: List<GachaOperationForm>,
    pageLinks: List<Pair<String, String>>,
    path: String,
    loading: Boolean,
    error: String?,
    addMode: Boolean,
    submitting: Boolean,
    onPath: (String) -> Unit,
    onReload: () -> Unit,
    onPending: (Pair<GachaOperationForm, List<Pair<String, String>>>) -> Unit,
) {
    // 无输入项的表单只是一个按钮（删除、移除等；订阅已提到顶栏），收成一排胶囊置顶；
    // 需要填内容的表单（新建、编辑说明）保留卡片，跟在按钮下方。
    val quickForms = forms.filter { it.fields.isEmpty() }
    val inputForms = forms.filter { it.fields.isNotEmpty() }
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 12.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (loading) item { LinearProgressIndicator(Modifier.fillMaxWidth().padding(horizontal = 14.dp)) }
        // 已有内容时错误降级为条幅，不清空正在看的列表
        error?.let { message ->
            item {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.errorContainer,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp),
                ) {
                    Row(Modifier.padding(start = 14.dp, end = 6.dp, top = 4.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(message, color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                        TextButton(onClick = onReload) { Text("重试") }
                    }
                }
            }
        }
        // 管理操作置顶，进页面就能点，不用滚到最下面
        if (quickForms.isNotEmpty()) {
            item {
                FlowRow(
                    Modifier.fillMaxWidth().padding(horizontal = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    quickForms.forEach { form ->
                        FilledTonalButton(
                            enabled = form.enabled && !submitting && !loading,
                            onClick = { onPending(form to form.hiddenFields) },
                            shape = RoundedCornerShape(50),
                            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 8.dp),
                        ) { Text(form.label, style = MaterialTheme.typography.labelLarge, maxLines = 1) }
                    }
                }
            }
        }
        itemsIndexed(inputForms) { _, form ->
            Box(Modifier.padding(horizontal = 14.dp)) {
                GachaDynamicForm(form, !submitting && !loading) { onPending(form to it) }
            }
        }
        // 专辑说明：源站说明文字集中成一张卡，不再逐条铺开占满屏
        opPage?.notes.orEmpty().filter { it.isNotBlank() }.takeIf { it.isNotEmpty() }?.let { notes ->
            item {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp),
                ) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Filled.Bookmarks, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                            Text("专辑说明", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                        }
                        notes.forEach {
                            Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
        // 内容区：与首页/板块一致的帖子卡片，替换原来的窄行 ListItem
        if (topics.isNotEmpty()) {
            item {
                Text("收录内容 · ${topics.size}", style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(horizontal = 18.dp))
            }
            itemsIndexed(topics, key = { _, t -> t.topicId }) { _, topic ->
                TopicCardView(topic, onClick = { nav.navigate("topic/${topic.topicId}") })
            }
        } else if (!addMode && !loading && error == null) {
            item {
                Text("这个专辑还没有收录内容", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 4.dp))
            }
        }
        // 翻页：源站分页链接做成一行胶囊，不再竖排文字按钮
        if (pageLinks.isNotEmpty()) {
            item {
                Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    pageLinks.forEach { (label, href) ->
                        val current = href == path
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = if (current) MaterialTheme.colorScheme.secondaryContainer
                            else MaterialTheme.colorScheme.surfaceContainerHigh,
                            modifier = Modifier.clip(RoundedCornerShape(50)).clickable(enabled = !current && !loading) { onPath(href) },
                        ) {
                            Text(label, style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (current) FontWeight.Bold else FontWeight.Medium,
                                color = if (current) MaterialTheme.colorScheme.onSecondaryContainer
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp))
                        }
                    }
                }
            }
        }
    }
}
