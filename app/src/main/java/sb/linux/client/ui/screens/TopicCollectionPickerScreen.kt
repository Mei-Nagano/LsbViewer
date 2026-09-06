package sb.linux.client.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import kotlinx.coroutines.launch
import sb.linux.client.common.collection.TopicCollectionActionForm
import sb.linux.client.common.collection.TopicCollectionOperation
import sb.linux.client.common.collection.TopicCollectionPicker
import sb.linux.client.data.Session

/** Topic-page collection selector: mirrors the source add/remove/remove-all semantics. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopicCollectionPickerScreen(session: Session, nav: NavHostController, topicPath: String) {
    var picker by remember { mutableStateOf<TopicCollectionPicker?>(null) }
    var selectedId by remember { mutableStateOf<Long?>(null) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var pending by remember { mutableStateOf<TopicCollectionActionForm?>(null) }
    var submitting by remember { mutableStateOf(false) }
    var createValues by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var menuExpanded by remember { mutableStateOf(false) }
    var createOpen by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    fun load() = scope.launch {
        loading = true; error = null
        try {
            picker = session.topicCollectionService.picker(topicPath)
            selectedId = picker?.options?.firstOrNull()?.collectionId
            createValues = picker?.createForm?.controls.orEmpty().associate { it.name to it.value }
        } catch (e: Exception) { error = e.message ?: "加载失败" }
        finally { loading = false }
    }
    LaunchedEffect(topicPath) { load() }

    val selected = picker?.options.orEmpty().firstOrNull { it.collectionId == selectedId }
    // 源站只有一张动态表单，JS 根据所选 option 改写 topic_collections_action。
    val template = picker?.actions.orEmpty().firstOrNull { it.operation == TopicCollectionOperation.ADD_ITEM }
    val action = template?.let { form ->
        val operation = if (selected?.included == true) "item_remove" else "item_add"
        form.copy(
            operation = if (selected?.included == true) TopicCollectionOperation.REMOVE_ITEM else TopicCollectionOperation.ADD_ITEM,
            label = if (selected?.included == true) "移出专辑" else "收录",
            fields = form.fields.filterNot { it.first == "collection_id" || it.first == "topic_collections_action" } +
                ("collection_id" to (selectedId ?: 0L).toString()) + ("topic_collections_action" to operation),
        )
    }

    val createForm = picker?.createForm
    if (createOpen && createForm != null) {
        AlertDialog(
            onDismissRequest = { if (!submitting) createOpen = false },
            title = { Text("新建专辑") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    createForm.controls.forEach { control ->
                        if (control.type.equals("checkbox", true)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(control.label, Modifier.weight(1f))
                                Checkbox(
                                    checked = createValues[control.name] == "1",
                                    onCheckedChange = { checked -> createValues = createValues + (control.name to if (checked) "1" else "") },
                                )
                            }
                        } else {
                            OutlinedTextField(
                                value = createValues[control.name].orEmpty(),
                                onValueChange = { value -> createValues = createValues + (control.name to value) },
                                label = { Text(control.label) },
                                singleLine = control.type != "textarea",
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                }
            },
            dismissButton = { TextButton(enabled = !submitting, onClick = { createOpen = false }) { Text("取消") } },
            confirmButton = {
                Button(enabled = !submitting && createValues["name"].orEmpty().isNotBlank(), onClick = {
                    val controlNames = createForm.controls.map { it.name }.toSet()
                    val values = createValues.filterValues { it.isNotEmpty() }.toList()
                    pending = createForm.copy(fields = createForm.fields.filterNot { it.first in controlNames } + values)
                    createOpen = false
                }) { Text("创建并收录") }
            },
        )
    }

    pending?.let { form ->
        AlertDialog(
            onDismissRequest = { if (!submitting) pending = null },
            title = { Text("确认${form.label}") },
            text = { Text("此操作会同步到源站。") },
            dismissButton = { TextButton(enabled = !submitting, onClick = { pending = null }) { Text("取消") } },
            confirmButton = {
                Button(enabled = !submitting, onClick = {
                    submitting = true
                    scope.launch {
                        try { session.topicCollectionService.execute(form); session.showToast("淘帖已同步"); load() }
                        catch (e: Exception) { session.showToast(e.message ?: "提交失败") }
                        finally { submitting = false; pending = null }
                    }
                }) { Text(if (submitting) "提交中…" else "确认") }
            },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("收录到淘帖专辑") },
                navigationIcon = { IconButton(onClick = { nav.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回") } },
                actions = { IconButton(enabled = !loading, onClick = { load() }) { Icon(Icons.Filled.Refresh, "刷新") } },
            )
        },
    ) { padding ->
        when {
            loading && picker == null -> Column(Modifier.fillMaxSize().padding(padding), verticalArrangement = Arrangement.Center) { CircularProgressIndicator() }
            error != null && picker == null -> Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) { Text(error!!); TextButton(onClick = { load() }) { Text("重试") } }
            picker == null -> Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) { Text("源站未提供淘帖收录面板") }
            else -> LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                item {
                    Text("选择我管理的专辑")
                }
                item {
                    Box(Modifier.fillMaxWidth()) {
                        OutlinedButton(onClick = { menuExpanded = true }, modifier = Modifier.fillMaxWidth()) {
                            Text(selected?.title ?: "选择专辑或新建专辑")
                        }
                        DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                            picker!!.options.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(if (option.included) "${option.title}（已收录）" else option.title) },
                                    onClick = { selectedId = option.collectionId; menuExpanded = false },
                                )
                            }
                            picker!!.removeAllForm?.let { form ->
                                DropdownMenuItem(text = { Text("全部取消收录") }, onClick = { menuExpanded = false; pending = form })
                            }
                            picker!!.createForm?.let {
                                DropdownMenuItem(text = { Text("新建专辑") }, onClick = { menuExpanded = false; createOpen = true })
                            }
                        }
                    }
                }
                item {
                    Button(enabled = action != null && selectedId != null && !submitting, onClick = { pending = action }, modifier = Modifier.fillMaxWidth()) {
                        Text(if (selected?.included == true) "移出专辑" else "收录")
                    }
                }
            }
        }
    }
}
