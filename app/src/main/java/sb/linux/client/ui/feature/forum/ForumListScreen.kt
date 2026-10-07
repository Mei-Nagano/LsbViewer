package sb.linux.client.ui.feature.forum

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import kotlinx.coroutines.launch
import sb.linux.client.ui.session.Session
import sb.linux.client.ui.*
import sb.linux.client.util.TopicFilter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForumListScreen(session: Session, nav: NavHostController) {
    val factory = remember(session.forumRepository) {
        ForumListViewModelFactory(session.forumRepository)
    }
    val viewModel: ForumListViewModel = viewModel(factory = factory)
    val state by viewModel.uiState.collectAsState()
    LaunchedEffect(viewModel) { viewModel.load() }

    Scaffold(topBar = { TopAppBar(title = { Text("板块导航") }) }) { pad ->
        Box(Modifier.padding(pad)) {
            when {
                state.loading -> LoadingBox()
                state.error != null -> ErrorBox(state.error!!) { viewModel.load() }
                state.forums.isEmpty() -> EmptyBox("暂无板块")
                else -> LazyColumn(
                    // 底部留白让末条内容可滚出玻璃底栏（23）：Scaffold 已消耗导航栏 inset；
                    // 悬浮玻璃高 58 + 悬浮边距 14；经典底栏不加此留白
                    contentPadding = PaddingValues(
                        start = 14.dp, end = 14.dp, top = 10.dp,
                        bottom = 10.dp + if (session.bottomBarStyle != 0) 72.dp else 0.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(state.forums, key = { it.id }) { f ->
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerLow,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(18.dp))
                                .clickable { nav.navigate("forum/${f.id}") },
                        ) {
                            Row(
                                Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        f.name,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (f.description.isNotBlank()) {
                                        Text(
                                            f.description,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    } else if (f.topicCount.isNotBlank()) {
                                        Text(
                                            "${f.topicCount} 个主题",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                                Icon(
                                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                    null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

