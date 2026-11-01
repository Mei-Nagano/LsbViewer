package sb.linux.client.ui.screens
import sb.linux.client.ui.session.Session


import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmarks
import androidx.compose.material.icons.filled.Refresh
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
import sb.linux.client.common.collection.TopicCollectionActionForm
import sb.linux.client.common.collection.TopicCollectionOperation
import sb.linux.client.ui.feature.collection.CollectionDetailViewModel
import sb.linux.client.ui.feature.collection.CollectionDetailViewModelFactory
import sb.linux.client.domain.gacha.GachaOperationForm
import sb.linux.client.domain.gacha.GachaOperationPage
import sb.linux.client.domain.topic.TopicCard
import sb.linux.client.ui.components.feedback.EmptyBox
import sb.linux.client.ui.components.feedback.ErrorBox
import sb.linux.client.ui.components.feedback.LoadingBox
import sb.linux.client.ui.patterns.topic.TopicCardView

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollectionDetailScreen(session: Session, nav: NavHostController) {
    val initialPath = nav.currentBackStackEntry?.arguments?.getString("path").orEmpty()
    var path by remember(initialPath) { mutableStateOf(initialPath) }
    val factory = remember(session.topicCollectionRepository) {
        CollectionDetailViewModelFactory(session.topicCollectionRepository)
    }
    val viewModel: CollectionDetailViewModel = viewModel(factory = factory)
    val state by viewModel.uiState.collectAsState()
    val detail = state.detail
    val sourceActions = detail?.actions.orEmpty()
    val managePath = detail?.managePath.orEmpty()
    val topics = detail?.topics.orEmpty()
    val opPage = detail?.let { current ->
        GachaOperationPage(
            title = current.summary.title,
            notes = listOf(current.description).filter { it.isNotBlank() },
            forms = current.actions.map { it.toGachaForm() },
            links = listOfNotNull(
                current.pageInfo.previousPath.takeIf { it.isNotBlank() }?.let { "上一页" to it },
                current.pageInfo.nextPath.takeIf { it.isNotBlank() }?.let { "下一页" to it },
            ),
        )
    }
    val loading = state.loading
    val error = state.error
    var pending by remember { mutableStateOf<Pair<GachaOperationForm, List<Pair<String, String>>>?>(null) }
    val submitting = state.submitting
    fun load() = viewModel.load(path)
    LaunchedEffect(path, viewModel) { viewModel.load(path) }
    LaunchedEffect(state.actionMessage) {
        state.actionMessage?.let { session.showToast(it); pending = null; viewModel.clearActionMessage() }
    }
    pending?.let { (form, fields) ->
        sb.linux.client.ui.components.feedback.AppConfirmDialog(
            title = "确认${form.label}",
            message = "此操作会同步至源站。删除或移除操作可能无法撤回，请核对后提交。",
            enabled = !submitting, confirmText = if (submitting) "提交中…" else "确认",
            onDismiss = { pending = null },
            onConfirm = {
                val sourceForm = sourceActions.firstOrNull {
                    it.action == form.action && it.label == form.label && it.fields == fields
                } ?: sourceActions.firstOrNull { it.action == form.action && it.label == form.label }
                if (sourceForm == null) session.showToast("源站操作已过期，请刷新后重试")
                else viewModel.execute(sourceForm, path.takeIf { it.startsWith("/topic_collection/") })
            },
        )
    }
    val pageLinks = opPage?.links.orEmpty().filter { Regex("^/topic_collection/\\d+(?:\\?p=\\d+)?$").matches(it.second) }
    val forms = opPage?.forms.orEmpty()
    // 订阅/取消订阅提到顶栏：这是专辑页最常用的一步，不用和删除、移除挤在同一排里找
    val subscribeForm = sourceActions.firstOrNull {
        it.operation == TopicCollectionOperation.SUBSCRIBE || it.operation == TopicCollectionOperation.UNSUBSCRIBE
    }?.toGachaForm()
    sb.linux.client.ui.components.navigation.AppPageScaffold(
        title = opPage?.title.orEmpty().ifBlank { "淘帖专辑" },
        onBack = { nav.popBackStack() },
        actions = {
            if (managePath.isNotBlank()) {
                TextButton(enabled = !loading && !submitting, onClick = {
                    nav.navigate("topicCollectionManage?path=${android.net.Uri.encode(managePath)}")
                }) { Text("管理") }
            }
            subscribeForm?.let { form ->
                FilledTonalButton(
                    enabled = form.enabled && !submitting && !loading,
                    onClick = { pending = form to form.hiddenFields },
                    shape = CircleShape,
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                    // 源站文案是「订阅专辑」，顶栏里已经在专辑页，去掉后缀省出标题的位置
                ) { Text(form.label.removeSuffix("专辑"), style = MaterialTheme.typography.labelLarge, maxLines = 1) }
            }
            IconButton(enabled = !loading, onClick = { load() }) { Icon(Icons.Filled.Refresh, "刷新") }
        },
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            when {
                loading && topics.isEmpty() && forms.isEmpty() -> LoadingBox()
                error != null && topics.isEmpty() && forms.isEmpty() -> ErrorBox(error!!) { load() }
                !loading && error == null && topics.isEmpty() && forms.isEmpty() ->
                    EmptyBox("当前没有内容或可执行操作", "请确认登录状态和专辑权限")
                else -> CollectionDetailList(
                    nav = nav, opPage = opPage, topics = topics,
                    forms = subscribeForm?.let { topForm -> forms.filterNot { it.action == topForm.action && it.label == topForm.label } } ?: forms,
                    pageLinks = pageLinks, path = path, loading = loading, error = error,
                    submitting = submitting,
                    onPath = { path = it }, onReload = { load() }, onPending = { pending = it },
                )
            }
        }
    }
}
