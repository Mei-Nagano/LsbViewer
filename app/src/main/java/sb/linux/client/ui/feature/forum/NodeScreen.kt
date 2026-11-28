package sb.linux.client.ui.feature.forum

import android.net.Uri
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import kotlinx.coroutines.CancellationException
import sb.linux.client.common.network.Endpoints
import sb.linux.client.domain.community.NodePage
import sb.linux.client.ui.components.feedback.ErrorBox
import sb.linux.client.ui.components.feedback.LoadingBox
import sb.linux.client.ui.components.navigation.AppPageScaffold
import sb.linux.client.ui.components.navigation.AppRefreshButton
import sb.linux.client.ui.components.navigation.PaginationBar
import sb.linux.client.ui.patterns.topic.TopicCardView
import sb.linux.client.ui.patterns.user.Avatar
import sb.linux.client.ui.session.Session
import sb.linux.client.util.TopicFilter

@Composable
internal fun NodeScreen(session: Session, nav: NavHostController, initialUrl: String, showBack: Boolean = true) {
    var url by remember(initialUrl) { mutableStateOf(initialUrl) }
    var page by remember(initialUrl) { mutableStateOf<NodePage?>(null) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var revision by remember { mutableIntStateOf(0) }
    fun openWeb(target: String) { nav.navigate("web?url=" + Uri.encode(Endpoints.abs(target)) + "&inApp=true") }
    fun open(target: String) {
        val uri = Uri.parse(Endpoints.abs(target))
        when {
            uri.path == "/nodes" -> url = target
            uri.path?.matches(Regex("/node/[0-9]+")) == true ->
                nav.navigate("nodes?url=" + Uri.encode(target))
            uri.path == "/login" -> nav.navigate("login")
            else -> openWeb(target)
        }
    }
    LaunchedEffect(url, revision, session.accountIdentityVersion) {
        loading = true
        error = null
        try {
            page = session.nodeRepository.load(url)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (failure: Exception) {
            error = failure.message ?: "节点加载失败"
        } finally {
            loading = false
        }
    }
    AppPageScaffold(title = page?.title ?: "节点中心", onBack = if (showBack) ({ nav.popBackStack(); Unit }) else null, primaryPage = !showBack, actions = {
        TextButton(onClick = { openWeb(page?.url ?: url) }) { Text("节点操作") }
        AppRefreshButton(onClick = { revision++ }, loading = loading)
    }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            val current = page
            if (current != null) {
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    current.tabs.forEach { tab ->
                        FilterChip(selected = tab.selected, onClick = {
                            if (Uri.parse(tab.url).path == Uri.parse(current.url).path) url = tab.url else open(tab.url)
                        }, label = { Text(tab.title) })
                    }
                }
            }
            when {
                loading -> LoadingBox()
                error != null -> ErrorBox(error!!) { revision++ }
                current != null -> {
                    val topics = remember(current.topics, session.keywordFilter) {
                        TopicFilter.apply(current.topics, session.keywordFilter.settings, isHome = false)
                    }
                    key(current.url) {
                        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp)) {
                            if (current.description.isNotBlank() || current.meta.isNotBlank()) item {
                                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    if (current.description.isNotBlank()) Text(current.description)
                                    if (current.meta.isNotBlank()) Text(current.meta, style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            items(current.nodes, key = { it.url }) { node ->
                                Surface(onClick = { open(node.url) }, shape = MaterialTheme.shapes.large,
                                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 5.dp)) {
                                    Row(Modifier.padding(14.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                        Avatar(node.iconUrl, size = 40)
                                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Text(node.name, style = MaterialTheme.typography.titleMedium)
                                            if (node.badges.isNotBlank()) Text(node.badges, style = MaterialTheme.typography.labelMedium,
                                                color = MaterialTheme.colorScheme.primary)
                                            if (node.description.isNotBlank()) Text(node.description, style = MaterialTheme.typography.bodyMedium)
                                            if (node.meta.isNotBlank()) Text(node.meta, style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                }
                            }
                            items(topics, key = { "topic:" + it.topicId }) { topic ->
                                TopicCardView(topic, onClick = { nav.navigate("topic/${topic.topicId}?p=1") },
                                    onForumClick = { nav.navigate("forum/" + it) })
                            }
                            if (current.nodes.isEmpty() && topics.isEmpty()) item {
                                Column(Modifier.fillMaxWidth().padding(24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(if (current.topics.isEmpty()) current.emptyMessage else "本页帖子已按屏蔽规则过滤")
                                    if (current.loginRequired) Button(onClick = { nav.navigate("login") }) { Text("登录") }
                                }
                            }
                            if (current.totalPages > 1) item {
                                PaginationBar(current.page, current.totalPages) { number ->
                                    val uri = Uri.parse(current.url)
                                    url = uri.buildUpon().clearQuery().apply {
                                        uri.queryParameterNames.filter { it != "p" }.forEach { name ->
                                            uri.getQueryParameters(name).forEach { appendQueryParameter(name, it) }
                                        }
                                        appendQueryParameter("p", number.toString())
                                    }.build().toString()
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
