package sb.linux.client.ui.feature.search

import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.runtime.remember
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.NavBackStackEntry
import androidx.compose.runtime.collectAsState
import sb.linux.client.domain.search.SearchResultItem
import sb.linux.client.domain.search.SearchScope
import sb.linux.client.domain.search.SearchSort
import sb.linux.client.ui.session.Session
import sb.linux.client.ui.components.feedback.EmptyBox
import sb.linux.client.ui.components.feedback.LoadingBox
import sb.linux.client.ui.theme.LocalAppSpacing

/** Search UI backed by the source site's current GET/Meilisearch contract. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(session: Session, nav: NavHostController, entry: NavBackStackEntry) {
    val initialQuery = entry?.arguments?.getString("q").orEmpty()
    val initialScope = SearchScope.fromValue(
        entry?.arguments?.getString("scope") ?: entry?.arguments?.getString("field"),
    )
    val initialSort = SearchSort.fromValue(entry?.arguments?.getString("sort"))
    val cached = remember(initialQuery, initialScope, initialSort) {
        session.searchCache.get()?.takeIf {
            it.request.normalizedText == initialQuery.trim() &&
                it.request.scope == initialScope && it.request.sort == initialSort &&
                System.currentTimeMillis() - it.time < SEARCH_CACHE_TTL_MS
        }
    }
    val factory = remember(session, initialQuery, initialScope, initialSort, cached) {
        SearchViewModelFactory(
            repository = session.searchRepository,
            initialQuery = initialQuery,
            initialScope = initialScope,
            initialSort = initialSort,
            cached = cached,
            onCacheSaved = session.searchCache::save,
        )
    }
    val viewModel: SearchViewModel = viewModel(viewModelStoreOwner = entry, factory = factory, key = "search")
    val state by viewModel.uiState.collectAsState()
    val spacing = LocalAppSpacing.current
    val searchFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) { searchFocus.requestFocus() }
    SearchChargeDialog(state.pendingSearch, viewModel::confirmSearch, viewModel::dismissConfirmation)
    LifecycleResumeEffect(viewModel, state.scope) {
        viewModel.loadCharge()
        onPauseOrDispose { }
    }

    LaunchedEffect(initialQuery, initialScope, initialSort) {
        if (initialQuery.isNotBlank() && cached == null) viewModel.search(1)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("搜索") },
                navigationIcon = {
                    IconButton(onClick = { nav.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                    }
                },
            )
        },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            Column(Modifier.padding(horizontal = spacing.pageHorizontal, vertical = spacing.item)) {
                OutlinedTextField(
                    value = state.query,
                    onValueChange = viewModel::updateQuery,
                    modifier = Modifier.fillMaxWidth().focusRequester(searchFocus),
                    placeholder = { Text(if (state.scope == SearchScope.USER) "搜索用户名" else "搜索标题、主题内容和回帖") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { viewModel.search(1) }),
                    shape = RoundedCornerShape(28.dp),
                    leadingIcon = { Icon(Icons.Filled.Search, null, Modifier.size(20.dp)) },
                    trailingIcon = {
                        if (state.query.isNotEmpty()) {
                            IconButton(onClick = { viewModel.updateQuery("") }) { Icon(Icons.Filled.Close, "清空") }
                        }
                    },
                )
                Spacer(Modifier.size(spacing.item))
                SearchChargeNotice(state.charge, state.chargeError)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(spacing.item),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                ) {
                    SearchScope.entries.forEach { candidate ->
                        FilterChip(
                            selected = state.scope == candidate,
                            onClick = { viewModel.selectScope(candidate) },
                            label = { Text(candidate.displayLabel()) },
                        )
                    }
                }
                if (state.scope != SearchScope.USER) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(spacing.item),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = spacing.item).horizontalScroll(rememberScrollState()),
                    ) {
                        Text("排序", style = MaterialTheme.typography.labelMedium)
                        SearchSort.entries.forEach { candidate ->
                            FilterChip(
                                selected = state.sort == candidate,
                                onClick = { viewModel.selectSort(candidate) },
                                label = { Text(candidate.displayLabel()) },
                            )
                        }
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    // 搜索是页面主操作：使用 Filled 按钮，忙碌时保留文字与尺寸
                    Button(onClick = { viewModel.search(1) }, enabled = !state.loading) {
                        Icon(Icons.Filled.Search, null, Modifier.size(16.dp))
                        Spacer(Modifier.width(5.dp))
                        Text(if (state.loading) "搜索中…" else "搜索")
                    }
                }
                state.error?.let {
                    sb.linux.client.ui.components.feedback.AppNotice(it)
                }
            }

            val pageData = state.resultPage
            when {
                state.loading -> LoadingBox()
                pageData == null -> EmptyBox("输入关键词搜索全站")
                pageData.items.isEmpty() -> EmptyBox("没有找到相关内容")
                else -> LazyColumn(contentPadding = PaddingValues(vertical = spacing.item)) {
                    items(pageData.items, key = { item -> itemKey(item) }) { item ->
                        when (item) {
                            is SearchResultItem.Topic -> TopicSearchItem(item) { nav.navigate("topic/${item.topicId}") }
                            is SearchResultItem.User -> UserSearchItem(item) { nav.navigate("user/${item.userId}") }
                        }
                    }
                    item {
                        SearchPagination(
                            page = state.page,
                            hasPrevious = pageData.hasPrevious,
                            hasNext = pageData.hasNext,
                            onPage = viewModel::search,
                        )
                    }
                }
            }
        }
    }
}

private const val SEARCH_CACHE_TTL_MS = 30 * 60_000L
