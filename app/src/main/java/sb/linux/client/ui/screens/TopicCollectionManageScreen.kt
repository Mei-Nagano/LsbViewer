package sb.linux.client.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.navigation.NavHostController
import sb.linux.client.common.collection.TopicCollectionActionForm
import sb.linux.client.common.collection.withEditedValues
import sb.linux.client.common.collection.key
import sb.linux.client.ui.feature.collection.TopicCollectionManageViewModel
import sb.linux.client.ui.feature.collection.TopicCollectionManageViewModelFactory
import sb.linux.client.ui.session.Session
import sb.linux.client.ui.components.feedback.ErrorBox
import sb.linux.client.ui.components.feedback.LoadingBox

/** 源站 v9 淘帖管理页：负责加载、提交和页面状态，展示组件位于同域专用文件。 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopicCollectionManageScreen(session: Session, nav: NavHostController) {
    val path = nav.currentBackStackEntry?.arguments?.getString("path").orEmpty()
    val factory = remember(session.topicCollectionRepository) {
        TopicCollectionManageViewModelFactory(session.topicCollectionRepository)
    }
    val viewModel: TopicCollectionManageViewModel = viewModel(factory = factory)
    val state by viewModel.uiState.collectAsState()
    val title = state.title
    val actions = state.actions
    val loading = state.loading
    val submitting = state.submitting
    val error = state.error
    var pending by remember { mutableStateOf<TopicCollectionActionForm?>(null) }
    val editedValues = state.editedValues
    fun load() = viewModel.load(path)
    LaunchedEffect(path, viewModel) { viewModel.load(path) }
    LaunchedEffect(state.actionMessage) {
        state.actionMessage?.let { session.showToast(it); pending = null; viewModel.clearActionMessage() }
    }

    pending?.let { form ->
        val destructive = form.isDestructive()
        sb.linux.client.ui.components.feedback.AppConfirmDialog(
            title = if (destructive) "确认${form.label}" else "提交${form.label}",
            message = if (destructive && form.targetLabel.isNotBlank())
                "将移除：${form.targetLabel}\n\n此操作可能无法撤回，请确认后再提交。"
                else if (destructive) "此操作可能无法撤回，请确认目标和内容后再提交。"
                else "修改将同步到源站，提交后会重新读取专辑状态。",
            enabled = !submitting, destructive = destructive,
            confirmText = if (submitting) "提交中…" else "确认", onDismiss = { pending = null },
            onConfirm = {
                val values = editedValues[form.key()].orEmpty()
                val success = form.targetLabel.takeIf { it.isNotBlank() }?.let { "已移除：$it" } ?: "已同步并刷新专辑"
                viewModel.submit(form, path, values, success)
            },
        )
    }

    sb.linux.client.ui.components.navigation.AppPageScaffold(
        title = title,
        onBack = { nav.popBackStack() },
        actions = {
            IconButton(enabled = !loading && !submitting, onClick = { load() }) {
                Icon(Icons.Filled.Refresh, "刷新")
            }
        },
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            when {
                loading && actions.isEmpty() -> LoadingBox()
                error != null && actions.isEmpty() -> ErrorBox(error!!) { load() }
                else -> ManageActionList(
                    title = title,
                    actions = actions,
                    loading = loading,
                    submitting = submitting,
                    error = error,
                    editedValues = editedValues,
                    onValueChange = { form, name, value ->
                        viewModel.updateValue(form, name, value)
                    },
                    onSubmit = { form, values ->
                        val missing = form.requiredFieldMissing(values)
                        if (missing != null) session.showToast("请填写$missing") else pending = form
                    },
                    onReload = { load() },
                )
            }
        }
    }
}
