package sb.linux.client.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.navigation.NavHostController
import androidx.lifecycle.viewmodel.compose.viewModel
import sb.linux.client.ui.session.Session
import sb.linux.client.ui.*
import sb.linux.client.ui.feature.community.DirectMessagesViewModel
import sb.linux.client.ui.feature.community.DirectMessagesViewModelFactory

/** 新版源站私信联系人页：联系人、时间和最近消息均直接来自 /direct_messages。
 *  showBack=false 用于底栏入口（顶层页面没有上一级可返回）。 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DirectMessagesScreen(session: Session, nav: NavHostController, showBack: Boolean = true) {
    val factory = remember(session.privateMessageRepository) {
        DirectMessagesViewModelFactory(session.privateMessageRepository)
    }
    val viewModel: DirectMessagesViewModel = viewModel(factory = factory)
    val state by viewModel.uiState.collectAsState()
    LaunchedEffect(viewModel) { viewModel.load() }

    Scaffold(topBar = {
        TopAppBar(
            title = { Text("我的私信") },
            navigationIcon = {
                if (showBack) IconButton(onClick = { nav.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回") }
            },
            actions = { IconButton(onClick = viewModel::load) { Icon(Icons.Filled.Refresh, "刷新") } }
        )
    }) { pad ->
        when {
            state.loading -> Box(Modifier.padding(pad).fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            state.error != null -> Box(Modifier.padding(pad).fillMaxSize(), contentAlignment = Alignment.Center) { Text(state.error!!) }
            state.contacts.isEmpty() -> Box(Modifier.padding(pad).fillMaxSize(), contentAlignment = Alignment.Center) { Text("暂无私信") }
            else -> LazyColumn(Modifier.padding(pad).fillMaxSize()) {
                items(state.contacts, key = { it.userId }) { c ->
                    ListItem(
                        headlineContent = { Text(c.username, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        supportingContent = { Text(c.preview, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        leadingContent = { Avatar(c.avatarUrl, 42) },
                        trailingContent = { Text(c.timeText, style = MaterialTheme.typography.labelSmall) },
                        modifier = Modifier.clickable {
                            nav.navigate("chat/${c.userId}?name=${android.net.Uri.encode(c.username)}&avatar=${android.net.Uri.encode(c.avatarUrl)}")
                        }
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
                }
            }
        }
    }
}
