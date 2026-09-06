package sb.linux.client.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import kotlinx.coroutines.launch
import sb.linux.client.common.collection.TopicCollectionActionForm
import sb.linux.client.data.Session

/** Source v9 independent management page. Forms are rendered only when supplied by the source. */
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

    fun load() = scope.launch {
        loading = true; error = null
        try {
            val page = session.topicCollectionService.manage(path)
            title = page.title
            actions = page.actions
            editedValues = page.actions.associate { form ->
                formKey(form) to form.controls.associate { it.name to it.value }
            }
        } catch (e: Exception) { error = e.message ?: "加载失败" }
        finally { loading = false }
    }
    LaunchedEffect(path) { load() }

    pending?.let { form ->
        AlertDialog(
            onDismissRequest = { if (!submitting) pending = null },
            title = { Text("确认${form.label}") },
            text = { Text("此操作将直接同步到源站。") },
            dismissButton = { TextButton(enabled = !submitting, onClick = { pending = null }) { Text("取消") } },
            confirmButton = {
                Button(enabled = !submitting, onClick = {
                    submitting = true
                    scope.launch {
                        try {
                            session.topicCollectionService.execute(form.withEditedValues(editedValues[formKey(form)].orEmpty()))
                            session.showToast("已同步源站")
                            load()
                        } catch (e: Exception) { session.showToast(e.message ?: "提交失败") }
                        finally { submitting = false; pending = null }
                    }
                }) { Text(if (submitting) "提交中…" else "确认") }
            },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = { IconButton(onClick = { nav.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回") } },
                actions = { IconButton(enabled = !loading, onClick = { load() }) { Icon(Icons.Filled.Refresh, "刷新") } },
            )
        },
    ) { padding ->
        when {
            loading && actions.isEmpty() -> Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) { CircularProgressIndicator() }
            error != null && actions.isEmpty() -> Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) { Text(error!!); TextButton(onClick = { load() }) { Text("重试") } }
            else -> LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (loading) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
                if (actions.isEmpty()) item { Text("源站没有提供可执行的管理操作") }
                items(actions, key = { "${it.action}:${it.label}:${it.fields.hashCode()}" }) { form ->
                    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        form.controls.forEach { control ->
                            val value = editedValues[formKey(form)]?.get(control.name).orEmpty()
                            OutlinedTextField(
                                value = value,
                                onValueChange = { next ->
                                    editedValues = editedValues + (formKey(form) to (editedValues[formKey(form)].orEmpty() + (control.name to next)))
                                },
                                label = { Text(control.label) },
                                singleLine = control.type != "textarea",
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
                            Button(enabled = !submitting && !loading && form.enabled, onClick = { pending = form }) { Text(form.label) }
                        }
                    }
                }
            }
        }
    }
}

private fun formKey(form: TopicCollectionActionForm): String = "${form.action}:${form.label}:${form.fields.hashCode()}"

private fun TopicCollectionActionForm.withEditedValues(values: Map<String, String>): TopicCollectionActionForm {
    if (values.isEmpty()) return this
    val controlNames = controls.map { it.name }.toSet()
    return copy(fields = fields.filterNot { it.first in controlNames } + values.toList())
}
