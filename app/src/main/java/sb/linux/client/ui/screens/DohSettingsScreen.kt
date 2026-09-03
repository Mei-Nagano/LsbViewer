package sb.linux.client.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import sb.linux.client.data.*
import sb.linux.client.ui.EmptyBox

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DohSettingsScreen(session: Session, nav: NavHostController) {
    val settings = session.settings
    var servers by remember { mutableStateOf(settings.dohServers()) }
    var selectedUrl by remember { mutableStateOf(settings.dohUrl) }
    var enabled by remember { mutableStateOf(settings.dohEnabled) }
    var bypassVpn by remember { mutableStateOf(settings.dohDisableOnVpn) }
    var customTab by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<DohServer?>(null) }
    var deleting by remember { mutableStateOf<DohServer?>(null) }
    val testJobs = remember { mutableStateMapOf<String, Job>() }
    val testSlots = remember { kotlinx.coroutines.sync.Semaphore(16) }
    val running = remember { mutableStateMapOf<String, Boolean>() }
    val results = remember { mutableStateMapOf<String, DohBenchmark>() }
    val scope = rememberCoroutineScope()
    fun save(items: List<DohServer>) { settings.saveDohServers(items); servers = settings.dohServers() }
    fun test(items: List<DohServer>) {
        for (server in items.filterNot { testJobs.containsKey(it.id) }) {
            results.remove(server.id)
            val job = scope.launch(start = kotlinx.coroutines.CoroutineStart.LAZY) {
                try {
                    testSlots.acquire()
                    try {
                        running[server.id] = true
                        results[server.id] = AppNetwork.benchmark(server.url)
                    } finally { testSlots.release() }
                } catch (e: CancellationException) { throw e }
                catch (_: Exception) { results[server.id] = DohBenchmark(emptyList(), emptyList(), 1) }
                finally { running.remove(server.id) }
            }
            testJobs[server.id] = job
            job.invokeOnCompletion { testJobs.remove(server.id) }
            job.start()
        }
    }
    editing?.let { server ->
        DohEditor(server, onDismiss = { editing = null }, onSave = { updated ->
            if (servers.any { it.id != updated.id && it.url == updated.url }) {
                session.showToast("此地址已在服务器列表中，请编辑已有项")
            } else {
                val previous = servers.find { it.id == updated.id }
                if (previous?.url == selectedUrl) { selectedUrl = updated.url; settings.dohUrl = updated.url }
                save(servers.filterNot { it.id == updated.id } + updated)
                results.remove(updated.id); editing = null
            }
        })
    }
    deleting?.let { server ->
        AlertDialog(onDismissRequest = { deleting = null }, title = { Text("删除服务器？") },
            text = { Text("删除「${server.name}」${if (server.url == selectedUrl) "后，将切换到第一个默认服务器" else "，不会影响其他服务器"}。") },
            confirmButton = { TextButton(onClick = {
                if (server.url == selectedUrl) { selectedUrl = DohServers.defaults.first().url; settings.dohUrl = selectedUrl }
                save(servers.filterNot { it.id == server.id }); results.remove(server.id); deleting = null
            }) { Text("删除") } }, dismissButton = { TextButton(onClick = { deleting = null }) { Text("取消") } })
    }
    val visible = servers.filter { it.builtIn != customTab }
    Scaffold(topBar = { TopAppBar(title = { Text("DNS over HTTPS") },
        navigationIcon = { IconButton(onClick = { nav.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回") } },
        actions = { IconButton(onClick = {
            customTab = true; editing = DohServer(java.util.UUID.randomUUID().toString(), "", "")
        }) { Icon(Icons.Filled.Add, "添加自定义服务器") } }) }) { pad ->
        LazyColumn(Modifier.padding(pad).fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                // 与应用设置共用 GroupCard/SwitchRow，避免这一页的开关和别处长得不一样
                GroupCard {
                    SwitchRow("启用 DoH", "使用选中的服务器加密解析域名", enabled) {
                        enabled = it; settings.dohEnabled = it
                    }
                    Text(
                        "当前选择：${servers.find { it.url == selectedUrl }?.name ?: selectedUrl}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
                    )
                    HorizontalDivider(
                        Modifier.padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                    )
                    SwitchRow("VPN 开启时自动停用", "保留 DoH 配置，VPN 结束后恢复；测速也遵守此规则", bypassVpn) {
                        bypassVpn = it; settings.dohDisableOnVpn = it
                    }
                }
            }
            item { Text("并发测速：每台查询一次 linux.sb，单项最多约 5 秒，本组全部同时进行。耗时包含 HTTPS 建连；测速不回退系统 DNS，不代表网页或下载速度，也不会切换所选服务器。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            item {
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    listOf(false to "默认（${DohServers.defaults.size}）", true to "自定义（${servers.count { !it.builtIn }}）").forEachIndexed { index, (value, label) ->
                        SegmentedButton(selected = customTab == value, onClick = { customTab = value }, shape = SegmentedButtonDefaults.itemShape(index, 2)) { Text(label) }
                    }
                }
            }
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    if (testJobs.isNotEmpty()) TextButton(onClick = { testJobs.values.toList().forEach { it.cancel() } }) { Text("取消全部") }
                    OutlinedButton(enabled = visible.any { it.id !in testJobs }, onClick = { test(visible) }) { Text("测速本组全部") }
                }
            }
            if (visible.isEmpty()) item { EmptyBox("还没有自定义服务器，点击右上角 + 添加") }
            items(visible, key = { it.id }) { server ->
                val selected = selectedUrl == server.url
                OutlinedCard(border = BorderStroke(if (selected) 2.dp else 1.dp, if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant)) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(Modifier.fillMaxWidth().selectable(selected, role = Role.RadioButton, onClick = { selectedUrl = server.url; settings.dohUrl = server.url }).heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = selected, onClick = null)
                            Spacer(Modifier.width(12.dp))
                            Text(server.name, style = MaterialTheme.typography.titleMedium, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        }
                        Text(server.url, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        if (server.note.isNotBlank()) Text(server.note, style = MaterialTheme.typography.bodyMedium)
                        if (server.id in testJobs) { LinearProgressIndicator(Modifier.fillMaxWidth()); Text(if (running[server.id] == true) "正在测速…" else "等待测速…", style = MaterialTheme.typography.labelMedium) }
                        results[server.id]?.let { result ->
                            Text(if (result.medianMs != null) "${result.medianMs} ms" else "失败",
                                color = if (result.medianMs != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error)
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            if (!server.builtIn) TextButton(enabled = server.id !in testJobs, onClick = { deleting = server }) { Text("删除") }
                            TextButton(enabled = server.id !in testJobs, onClick = { editing = server }) { Text("编辑") }
                            if (server.id in testJobs) TextButton(onClick = { testJobs[server.id]?.cancel() }) { Text("取消") }
                            else FilledTonalButton(onClick = { test(listOf(server)) }) { Text("测速") }
                        }
                    }
                }
            }
            item { Text("DoH 用于原生页面与应用内 HTTPS 网页，外部浏览器不受影响。切换配置会清理空闲连接，返回网页后请刷新。普通解析失败时回退系统 DNS；DoH 不是代理，不能保证解决网站本身或网络线路的访问问题。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
    }
}

@Composable
private fun DohEditor(server: DohServer, onDismiss: () -> Unit, onSave: (DohServer) -> Unit) {
    var name by remember(server.id) { mutableStateOf(server.name) }
    var url by remember(server.id) { mutableStateOf(server.url) }
    var note by remember(server.id) { mutableStateOf(server.note) }
    val valid = name.isNotBlank() && AppNetwork.isValidEndpoint(url)
    AlertDialog(onDismissRequest = onDismiss, title = { Text(if (server.url.isEmpty()) "添加 DoH 服务器" else "编辑 DoH 服务器") },
        text = { Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(name, { name = it.take(80) }, label = { Text("名称") }, singleLine = true, modifier = Modifier.fillMaxWidth(), supportingText = { Text("${name.length}/80") })
            OutlinedTextField(url, { url = it.trim() }, label = { Text("HTTPS 地址") }, readOnly = server.builtIn, modifier = Modifier.fillMaxWidth(),
                isError = url.isNotBlank() && !AppNetwork.isValidEndpoint(url), supportingText = { Text(if (server.builtIn) "默认地址固定；可编辑名称和备注" else "须为 HTTPS，不允许用户名、密码或 # 片段") })
            OutlinedTextField(note, { note = it.take(500) }, label = { Text("备注（可选）") }, modifier = Modifier.fillMaxWidth(), minLines = 2, maxLines = 4, supportingText = { Text("${note.length}/500") })
        } },
        confirmButton = { TextButton(enabled = valid, onClick = { onSave(server.copy(name = name.trim(), url = url.trim(), note = note.trim())) }) { Text("保存") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } })
}
