package sb.linux.client.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import kotlinx.coroutines.launch
import sb.linux.client.data.SearchPage
import sb.linux.client.data.SearchQuery
import sb.linux.client.data.SearchResultCache
import sb.linux.client.data.SearchResultItem
import sb.linux.client.data.SearchScope
import sb.linux.client.data.SearchSort
import sb.linux.client.data.Session
import sb.linux.client.repository.SourceSearchRepository
import sb.linux.client.service.SearchService
import sb.linux.client.ui.EmptyBox
import sb.linux.client.ui.LoadingBox

/** Search UI backed by the source site's current GET/Meilisearch contract. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(session: Session, nav: NavHostController) {
    val entry = nav.currentBackStackEntry
    val initialQuery = entry?.arguments?.getString("q").orEmpty()
    val initialScope = SearchScope.fromValue(
        entry?.arguments?.getString("scope") ?: entry?.arguments?.getString("field"),
    )
    val initialSort = SearchSort.fromValue(entry?.arguments?.getString("sort"))
    val service = remember(session) { SearchService(SourceSearchRepository(session.client)) }
    val cached = remember(initialQuery, initialScope, initialSort) {
        session.searchCache?.takeIf {
            it.request.normalizedText == initialQuery.trim() &&
                it.request.scope == initialScope && it.request.sort == initialSort &&
                System.currentTimeMillis() - it.time < SEARCH_CACHE_TTL_MS
        }
    }

    var query by remember { mutableStateOf(initialQuery) }
    var searchScope by remember { mutableStateOf(cached?.request?.scope ?: initialScope) }
    var sort by remember { mutableStateOf(cached?.request?.sort ?: initialSort) }
    var page by remember { mutableIntStateOf(cached?.page?.query?.normalizedPage ?: 1) }
    var resultPage by remember { mutableStateOf<SearchPage?>(cached?.page) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()

    fun search(targetPage: Int = 1) {
        val request = SearchQuery(query, searchScope, sort, targetPage)
        if (request.normalizedText.length < 2) {
            error = "关键词至少 2 个字符"
            return
        }
        coroutineScope.launch {
            loading = true
            error = null
            try {
                val pageData = service.search(request)
                resultPage = pageData
                page = pageData.query.normalizedPage
                session.searchCache = SearchResultCache(request = pageData.query, page = pageData, time = System.currentTimeMillis())
            } catch (e: Exception) {
                error = e.message ?: "搜索失败"
            } finally {
                loading = false
            }
        }
    }

    LaunchedEffect(initialQuery, initialScope, initialSort) {
        if (initialQuery.isNotBlank() && cached == null) search(1)
    }

    Scaffold(topBar = { TopAppBar(title = { Text("搜索") }) }) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            Column(Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text(if (searchScope == SearchScope.USER) "搜索用户名" else "搜索标题、主题内容和回帖") },
                    singleLine = true,
                    shape = RoundedCornerShape(28.dp),
                    leadingIcon = { Icon(Icons.Filled.Search, null, Modifier.size(20.dp)) },
                    trailingIcon = {
                        if (query.isNotEmpty()) {
                            IconButton(onClick = { query = "" }) { Icon(Icons.Filled.Close, "清空") }
                        }
                    },
                )
                Spacer(Modifier.size(10.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                ) {
                    SearchScope.entries.forEach { candidate ->
                        FilterChip(
                            selected = searchScope == candidate,
                            onClick = {
                                searchScope = candidate
                                if (candidate == SearchScope.USER) sort = SearchSort.RELEVANCE
                            },
                            label = { Text(candidate.label) },
                        )
                    }
                }
                if (searchScope != SearchScope.USER) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 8.dp).horizontalScroll(rememberScrollState()),
                    ) {
                        Text("排序", style = MaterialTheme.typography.labelMedium)
                        SearchSort.entries.forEach { candidate ->
                            FilterChip(
                                selected = sort == candidate,
                                onClick = { sort = candidate },
                                label = { Text(candidate.label) },
                            )
                        }
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    FilledTonalButton(onClick = { resultPage = null; search(1) }, enabled = !loading) {
                        Icon(Icons.Filled.Search, null, Modifier.size(16.dp))
                        Spacer(Modifier.width(5.dp))
                        Text(if (loading) "搜索中…" else "搜索")
                    }
                }
                error?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }

            val pageData = resultPage
            when {
                loading -> LoadingBox()
                pageData == null -> EmptyBox("输入关键词搜索全站")
                pageData.items.isEmpty() -> EmptyBox("没有找到相关内容")
                else -> LazyColumn(contentPadding = PaddingValues(vertical = 8.dp)) {
                    items(pageData.items, key = { item -> itemKey(item) }) { item ->
                        when (item) {
                            is SearchResultItem.Topic -> TopicSearchItem(item) { nav.navigate("topic/${item.topicId}") }
                            is SearchResultItem.User -> UserSearchItem(item) { nav.navigate("user/${item.userId}") }
                        }
                    }
                    item {
                        SearchPagination(
                            page = page,
                            hasPrevious = pageData.hasPrevious,
                            hasNext = pageData.hasNext,
                            onPage = { search(it) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TopicSearchItem(item: SearchResultItem.Topic, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(Modifier.padding(14.dp)) {
            Text(item.title, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
            if (item.snippet.isNotBlank()) {
                Text(item.snippet, style = MaterialTheme.typography.bodySmall, maxLines = 3, overflow = TextOverflow.Ellipsis)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
                if (item.matchedBy.isNotBlank()) Text(item.matchedBy, style = MaterialTheme.typography.labelSmall)
                Text(item.forumName, style = MaterialTheme.typography.labelSmall)
                Text(item.timeText, style = MaterialTheme.typography.labelSmall)
                Text("${item.replies} 回复 · ${item.views} 浏览", style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
private fun UserSearchItem(item: SearchResultItem.User, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp)) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Person, null, Modifier.size(28.dp), tint = MaterialTheme.colorScheme.primary)
            Column(Modifier.padding(start = 12.dp)) {
                Text(item.username, style = MaterialTheme.typography.titleMedium)
                Text(item.meta, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun SearchPagination(page: Int, hasPrevious: Boolean, hasNext: Boolean, onPage: (Int) -> Unit) {
    if (!hasPrevious && !hasNext) return
    Row(
        Modifier.fillMaxWidth().padding(12.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(enabled = hasPrevious, onClick = { onPage(page - 1) }) { Icon(Icons.Filled.ArrowBack, "上一页") }
        Text("第 $page 页", modifier = Modifier.padding(horizontal = 10.dp))
        IconButton(enabled = hasNext, onClick = { onPage(page + 1) }) { Icon(Icons.Filled.ArrowForward, "下一页") }
    }
}

private fun itemKey(item: SearchResultItem): String = when (item) {
    is SearchResultItem.Topic -> "topic:${item.topicId}"
    is SearchResultItem.User -> "user:${item.userId}"
}

private const val SEARCH_CACHE_TTL_MS = 30 * 60_000L
