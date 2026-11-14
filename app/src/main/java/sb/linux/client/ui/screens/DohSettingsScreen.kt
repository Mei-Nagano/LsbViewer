package sb.linux.client.ui.screens
import sb.linux.client.ui.session.Session

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import sb.linux.client.domain.network.DohServer
import sb.linux.client.ui.feature.network.DohSettingsViewModel
import sb.linux.client.ui.feature.network.DohSettingsViewModelFactory
import sb.linux.client.ui.theme.LocalAppSpacing
import sb.linux.client.ui.patterns.settings.SettingsChoiceRow
import sb.linux.client.ui.patterns.settings.settingsItemShape

/** DNS over HTTPS 设置内容：并入「网络与链接」二级页展示，不再单独成页。 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DohSettingsContent(session: Session) {
    val viewModelFactory = remember(session.dohSettingsRepository) {
        DohSettingsViewModelFactory(session.dohSettingsRepository)
    }
    val dohViewModel: DohSettingsViewModel = viewModel(factory = viewModelFactory)
    val benchmarkState by dohViewModel.uiState.collectAsState()
    val configuration = benchmarkState.configuration
    val servers = configuration.servers
    val selectedUrl = configuration.selectedUrl
    fun save(items: List<DohServer>) = dohViewModel.saveServers(items)
    DisposableEffect(dohViewModel) {
        onDispose { dohViewModel.cancelAll() }
    }
    var editing by remember { mutableStateOf<DohServer?>(null) }
    var deleting by remember { mutableStateOf<DohServer?>(null) }
    fun test(items: List<DohServer>) = dohViewModel.test(items)
    fun addCustomServer() {
        editing = DohServer(java.util.UUID.randomUUID().toString(), "", "")
    }
    editing?.let { server ->
        DohEditor(server, isValidEndpoint = dohViewModel::isValidEndpoint, onDismiss = { editing = null }, onSave = { updated ->
            if (servers.any { it.id != updated.id && it.url == updated.url }) {
                session.showToast("此地址已在服务器列表中，请编辑已有项")
            } else {
                val previous = servers.find { it.id == updated.id }
                if (previous?.url == selectedUrl) dohViewModel.selectServer(updated.url)
                save(servers.filterNot { it.id == updated.id } + updated)
                dohViewModel.clearResult(updated.id); editing = null
            }
        })
    }
    deleting?.let { server ->
        sb.linux.client.ui.components.feedback.AppConfirmDialog(
            title = "删除服务器？",
            message = "删除「${server.name}」${if (server.url == selectedUrl) "后，将切换到第一个默认服务器" else "，不会影响其他服务器"}。",
            onDismiss = { deleting = null },
            onConfirm = {
                if (server.url == selectedUrl) dohViewModel.selectServer(dohViewModel.defaultServers.first().url)
                save(servers.filterNot { it.id == server.id }); dohViewModel.clearResult(server.id); deleting = null
            }, confirmText = "删除", destructive = true,
        )
    }
    val spacing = LocalAppSpacing.current

    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(spacing.contentGap)) {
        // 与应用设置共用 GroupCard/SwitchRow，避免这一页的开关和别处长得不一样
        GroupCard {
            SwitchRow("启用 DoH", "使用选中的服务器加密解析域名", configuration.enabled, onCheckedChange = dohViewModel::setEnabled)
            SwitchRow("对应用内打开的网站生效", "关闭后外部网站使用系统 DNS；源站页面仍使用已启用的 DoH", configuration.forWebsites,
                onCheckedChange = dohViewModel::setForWebsites)
            Text(
                "当前选择：${servers.find { it.url == selectedUrl }?.name ?: selectedUrl}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = spacing.contentGap, end = spacing.contentGap, bottom = spacing.item),
            )
            SwitchRow("VPN 开启时自动停用", "保留 DoH 配置，VPN 结束后恢复；测速也遵守此规则", configuration.bypassVpn, onCheckedChange = dohViewModel::setBypassVpn)
            SwitchRow("无 VPN 时自动优化", "自动启用 DoH，并在后台静默选择最快可用服务器", configuration.autoWithoutVpn, onCheckedChange = dohViewModel::setAutoWithoutVpn)
        }
        Text("并发测速：每台查询一次 linux.sb，单项最多约 5 秒，全部服务器统一参与测速。耗时包含 HTTPS 建连；测速不回退系统 DNS，不代表网页或下载速度，也不会切换所选服务器。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        GroupCard {
            SettingsChoiceRow("DoH 服务器", servers.map { it.url to it.name }, selectedUrl, dohViewModel::selectServer)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(spacing.item)) {
            OutlinedButton(onClick = { addCustomServer() }, modifier = Modifier.weight(1f)) {
                Icon(Icons.Filled.Add, null, Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("添加自定义服务器")
            }
            OutlinedButton(
                enabled = servers.any { it.id !in benchmarkState.testingIds },
                onClick = { test(servers) },
                modifier = Modifier.weight(1f),
            ) { Text("测速全部服务器") }
        }
        if (benchmarkState.testingIds.isNotEmpty()) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = dohViewModel::cancelAll) { Text("取消全部") }
            }
        }
        if (servers.isEmpty()) {
            Text(
                "暂无 DoH 服务器，点击上方「添加自定义服务器」新增",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = spacing.item),
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(spacing.inline)) {
            servers.forEachIndexed { index, server ->
                key(server.id) {
                    DohServerRow(
                        server = server,
                        selected = selectedUrl == server.url,
                        testing = server.id in benchmarkState.testingIds,
                        running = server.id in benchmarkState.runningIds,
                        result = benchmarkState.results[server.id],
                        shape = settingsItemShape(index, servers.size),
                        onSelect = { dohViewModel.selectServer(server.url) },
                        onTest = { test(listOf(server)) },
                        onCancel = { dohViewModel.cancel(server.id) },
                        onEdit = { editing = server },
                        onDelete = { deleting = server },
                    )
                }
            }
        }
        Text("DoH 启用时源站及其子域名使用加密 DNS，外部网站遵循上方开关。外部浏览器不受影响。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun DohEditor(
    server: DohServer,
    isValidEndpoint: (String) -> Boolean,
    onDismiss: () -> Unit,
    onSave: (DohServer) -> Unit,
) {
    var name by remember(server.id) { mutableStateOf(server.name) }
    var url by remember(server.id) { mutableStateOf(server.url) }
    var note by remember(server.id) { mutableStateOf(server.note) }
    val valid = name.isNotBlank() && isValidEndpoint(url)
    AlertDialog(onDismissRequest = onDismiss, title = { Text(if (server.url.isEmpty()) "添加 DoH 服务器" else "编辑 DoH 服务器") },
        text = { Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SettingsTextField(name, { name = it.take(80) }, label = { Text("名称") }, singleLine = true, modifier = Modifier.fillMaxWidth(), supportingText = { Text("${name.length}/80") })
            SettingsTextField(url, { url = it.trim() }, label = { Text("HTTPS 地址") }, readOnly = server.builtIn, modifier = Modifier.fillMaxWidth(),
                isError = url.isNotBlank() && !isValidEndpoint(url), supportingText = { Text(if (server.builtIn) "默认地址固定；可编辑名称和备注" else "须为 HTTPS，不允许用户名、密码或 # 片段") })
            SettingsTextField(note, { note = it.take(500) }, label = { Text("备注（可选）") }, modifier = Modifier.fillMaxWidth(), minLines = 2, maxLines = 4, supportingText = { Text("${note.length}/500") })
        } },
        confirmButton = { TextButton(enabled = valid, onClick = { onSave(server.copy(name = name.trim(), url = url.trim(), note = note.trim())) }) { Text("保存") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } })
}
