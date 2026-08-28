package sb.linux.client.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import kotlinx.coroutines.launch
import sb.linux.client.data.HtmlParser
import sb.linux.client.data.SearchResultCache
import sb.linux.client.data.Session
import sb.linux.client.data.TopicCard
import sb.linux.client.ui.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(session: Session, nav: NavHostController) {
    // 支持从首页顶栏搜索条带入关键词与搜索范围（search?q=…&field=…）
    val entry = nav.currentBackStackEntry
    val initialQuery = entry?.arguments?.getString("q").orEmpty()
    val initialField = entry?.arguments?.getString("field")
        ?.takeIf { it in setOf("title", "body", "reply") } ?: "title"
    // 复用会话内最近一次搜索结果：短时间内从结果页返回不重复搜索
    val cached = remember(initialQuery, initialField) {
        session.searchCache?.takeIf {
            it.query == initialQuery && it.field == initialField &&
                System.currentTimeMillis() - it.time < 30 * 60_000L
        }
    }
    var query by remember { mutableStateOf(initialQuery) }
    var field by remember { mutableStateOf(cached?.field ?: initialField) }
    var results by remember { mutableStateOf(cached?.results) }
    var page by remember { mutableIntStateOf(cached?.page ?: 1) }
    var totalPages by remember { mutableIntStateOf(cached?.totalPages ?: 1) }
    var lastQuery by remember { mutableStateOf(cached?.query ?: "") }
    var lastField by remember { mutableStateOf(cached?.field ?: "title") }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    val fieldLabel = mapOf("title" to "标题", "body" to "内容", "reply" to "回帖")

    fun search(p: Int) {
        if (query.trim().length < 2) {
            error = "关键词至少 2 个字符"; return
        }
        scope.launch {
            loading = true; error = null
            try {
                // 源站表单为 data-no-ajax 纯 POST /search，直接渲染结果页 HTML（非 JSON 重定向）
                // 字段：_csrf + q + field(title/body/reply)，分页在 POST body 带 p
                val csrf = session.client.csrf()
                val form = HashMap<String, String>().apply {
                    put("_csrf", csrf); put("field", field); put("q", query)
                    if (p > 1) put("p", p.toString())
                }
                val resp = session.client.postForm("/search", form)
                if (resp.url.contains("login")) {
                    error = "请先登录后再搜索"
                    return@launch
                }
                val errText = HtmlParser.extractError(resp.html)
                    .takeIf { it.isNotBlank() && resp.url.contains("form_error") }
                if (errText != null) {
                    error = errText
                    return@launch
                }
                val (items, pg) = HtmlParser.parseTopicList(resp.html)
                results = items; page = pg.first; totalPages = pg.second
                lastQuery = query; lastField = field
                // 保存到会话缓存，返回时直接复用
                session.searchCache = SearchResultCache(query, field, page, totalPages, items, System.currentTimeMillis())
            } catch (e: Exception) {
                error = e.message ?: "搜索失败"
            } finally { loading = false }
        }
    }

    // 从首页顶栏搜索进入（带关键词）时自动执行：范围/关键词已在顶栏选好，
    // 不必再点本页搜索按钮。命中会话缓存（同关键词同范围 30 分钟内）时直接复用
    LaunchedEffect(initialQuery, initialField) {
        if (initialQuery.isNotBlank() && cached == null) search(1)
    }

    Scaffold(topBar = { TopAppBar(title = { Text("搜索") }) }) { pad ->
        Column(Modifier.padding(pad).fillMaxSize()) {
            Column(Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                // MD3 胶囊搜索条：前置搜索图标，尾部搜索按钮
                OutlinedTextField(
                    value = query, onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("搜索全站内容…") },
                    singleLine = true,
                    shape = RoundedCornerShape(28.dp),
                    leadingIcon = { Icon(Icons.Filled.Search, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                    trailingIcon = {
                        if (query.isNotEmpty()) {
                            IconButton(onClick = { query = "" }) {
                                Icon(Icons.Filled.Close, "清空", Modifier.size(18.dp))
                            }
                        }
                    }
                )
                Spacer(Modifier.height(10.dp))
                // 搜索范围：分段选择胶囊
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                ) {
                    fieldLabel.forEach { (v, label) ->
                        FilterChip(
                            selected = field == v,
                            onClick = { field = v },
                            label = { Text(label) },
                        )
                    }
                    Spacer(Modifier.weight(1f))
                    FilledTonalButton(
                        onClick = { results = null; search(1) },
                        enabled = !loading,
                        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 8.dp)
                    ) {
                        if (loading) {
                            CircularProgressIndicator(Modifier.size(14.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Filled.Search, null, Modifier.size(16.dp))
                            Spacer(Modifier.width(5.dp))
                            Text("搜索")
                        }
                    }
                }
                Spacer(Modifier.height(6.dp))
                error?.let {
                    Spacer(Modifier.height(4.dp))
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
            val res = results
            when {
                loading -> LoadingBox()
                res == null -> EmptyBox("输入关键词搜索全站")
                res.isEmpty() -> EmptyBox("没有找到相关内容")
                else -> LazyColumn(contentPadding = PaddingValues(vertical = 8.dp)) {
                    items(res, key = { it.topicId }) { t ->
                        TopicCardView(t, onClick = { nav.navigate("topic/${t.topicId}") },
                            onForumClick = { fid -> if (fid > 0) nav.navigate("forum/$fid") })
                    }
                    item { PaginationBar(page, totalPages) { search(it) } }
                }
            }
        }
    }
}
