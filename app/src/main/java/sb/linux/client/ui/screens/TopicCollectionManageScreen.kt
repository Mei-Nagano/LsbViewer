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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.navigation.NavHostController
import kotlinx.coroutines.launch
import sb.linux.client.common.collection.TopicCollectionActionForm
import sb.linux.client.data.Session
import sb.linux.client.ui.ErrorBox
import sb.linux.client.ui.LoadingBox

/** 源站 v9 淘帖管理页：负责加载、提交和页面状态，展示组件位于同域专用文件。 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopicCollectionManageScreen(session: Session, nav: NavHostController) {
    val path = nav.currentBackStackEntry?.arguments?.getString("path").orEmpty()
    var title by remember { mutableStateOf("淘帖管理") }
    var actions by remember { mutableStateOf<List<TopicCollectionActionForm>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var submitting by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var pending by remember { mutableStateOf<TopicCollectionActionForm?>(null) }
    var editedValues by remember { mutableStateOf<Map<String, Map<String, String>>>(emptyMap()) }
    val scope = rememberCoroutineScope()

    suspend fun reload(): Boolean {
        loading = true
        error = null
        return try {
            val page = session.topicCollectionService.manage(path)
            title = page.title
            actions = page.actions
            editedValues = page.actions.associate { form ->
                formKey(form) to form.controls.associate { it.name to it.value }
            }
            true
        } catch (e: Exception) {
            error = e.message ?: "加载失败"
            false
        } finally {
            loading = false
        }
    }

    fun load() = scope.launch { reload() }
    LaunchedEffect(path) { reload() }

    pending?.let { form ->
        val destructive = form.isDestructive()
        AlertDialog(
            onDismissRequest = { if (!submitting) pending = null },
            title = { Text(if (destructive) "确认${form.label}" else "提交${form.label}") },
            text = {
                val target = form.targetLabel.takeIf { it.isNotBlank() }
                Text(
                    if (destructive && target != null) {
                        "将移除：$target\n\n此操作可能无法撤回，请确认后再提交。"
                    } else if (destructive) "此操作可能无法撤回，请确认目标和内容后再提交。"
                    else "修改将同步到源站，提交后会重新读取专辑状态。",
                )
            },
            dismissButton = {
                TextButton(enabled = !submitting, onClick = { pending = null }) { Text("取消") }
            },
            confirmButton = {
                Button(
                    enabled = !submitting,
                    onClick = {
                        submitting = true
                        scope.launch {
                            try {
                                val values = editedValues[formKey(form)].orEmpty()
                                session.topicCollectionService.execute(form.withEditedValues(values))
                                val loaded = reload()
                                val success = form.targetLabel.takeIf { it.isNotBlank() }
                                    ?.let { "已移除：$it" }
                                    ?: "已同步并刷新专辑"
                                session.showToast(if (loaded) success else "操作已提交，但刷新失败")
                            } catch (e: Exception) {
                                session.showToast(e.message ?: "提交失败")
                            } finally {
                                submitting = false
                                pending = null
                            }
                        }
                    },
                    colors = if (destructive) ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError,
                    ) else ButtonDefaults.buttonColors(),
                ) { Text(if (submitting) "提交中…" else "确认") }
            },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    IconButton(onClick = { nav.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                    }
                },
                actions = {
                    IconButton(enabled = !loading && !submitting, onClick = { load() }) {
                        Icon(Icons.Filled.Refresh, "刷新")
                    }
                },
            )
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
                        val key = formKey(form)
                        editedValues = editedValues + (key to (editedValues[key].orEmpty() + (name to value)))
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
