package sb.linux.client.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import sb.linux.client.ui.LocalLinkHandler
import sb.linux.client.ui.components.feedback.ErrorBox
import sb.linux.client.ui.components.feedback.LoadingBox
import sb.linux.client.ui.feature.profile.*
import sb.linux.client.ui.patterns.settings.SettingsCard
import sb.linux.client.ui.session.Session
import sb.linux.client.ui.theme.LocalAppSpacing

/** 顶部总数与佩戴是主信息；分区为单行导航，持有列表保留真实佩戴／赠送表单。 */
@Composable
fun GachaProfileScreen(session: Session, nav: NavHostController, sectionUrl: String? = null) {
    val account = session.account.loginState
    AccountPageGate(account.loggedIn && account.userId > 0, "我的称号", "登录后管理称号", nav, settingsStyle = true) {
        val owner = session.accountIdentityVersion
        val factory = remember(session.accountManagementRepository, account.userId, owner) {
            AccountManagementViewModelFactory(session.accountManagementRepository, { session.accountIdentityVersion }, { session.refreshSession() })
        }
        val collection: AccountManagementViewModel = viewModel(key = "titles:collection:${account.userId}:$owner", factory = factory)
        val sectionFactory = remember(collection) {
            AccountManagementViewModelFactory(session.accountManagementRepository, { session.accountIdentityVersion }, {
                collection.load("/gacha_profile")
                session.refreshSession()
            })
        }
        val section: AccountManagementViewModel = viewModel(key = "titles:section:${account.userId}:$owner", factory = sectionFactory)
        val overviewFactory = remember(owner, account.userId) {
            GachaOverviewFactory(session.userPageRepository, session.accountManagementRepository, account.userId) { session.accountIdentityVersion }
        }
        val overview: GachaOverviewViewModel = viewModel(key = "gacha-overview:${account.userId}:$owner", factory = overviewFactory)
        LaunchedEffect(collection.mutationVersion, section.mutationVersion) {
            overview.refresh(collection.mutationVersion + section.mutationVersion > 0)
        }
        val initialSection = sectionUrl?.takeUnless { android.net.Uri.parse(it).let { uri ->
            uri.path == "/gacha_profile" && uri.query.isNullOrBlank()
        } }
        var selectedUrl by remember(collection, initialSection) { mutableStateOf(initialSection) }
        var dirty by remember(collection) { mutableStateOf(false) }
        var pendingLink by remember(collection) { mutableStateOf<String?>(null) }
        val open = LocalLinkHandler.current
        val spacing = LocalAppSpacing.current
        val state = if (selectedUrl == null) collection.state else section.state
        val active = if (selectedUrl == null) collection else section
        val busy = collection.state.busy || section.state.busy
        val exit = rememberSettingsExit(dirty) { nav.popBackStack() }
        fun navigate(url: String) {
            if (isAccountManagementLink(url)) {
                dirty = false
                if (android.net.Uri.parse(url).let { it.path == "/gacha_profile" && it.query.isNullOrBlank() }) {
                    selectedUrl = null
                    collection.load("/gacha_profile")
                } else { selectedUrl = url; section.load(url) }
            } else open?.invoke(url)
        }
        fun requestLink(url: String) { if (!busy) { if (dirty) pendingLink = url else navigate(url) } }
        LaunchedEffect(collection) { collection.load("/gacha_profile") }
        LaunchedEffect(initialSection, section) { initialSection?.let { section.load(it) } }
        LaunchedEffect(active, state.revision) { dirty = false }
        SettingsPage("我的称号", nav, onBack = exit, headerActions = {
            sb.linux.client.ui.components.navigation.AppRefreshButton(onClick = {
                overview.refresh(true)
                requestLink(selectedUrl ?: "https://linux.sb/gacha_profile")
            }, enabled = !busy && !state.loading)
        }) {
            GachaOverviewCard(overview, account.userId)
            SettingsCard {
                Column(Modifier.padding(spacing.pageHorizontal), verticalArrangement = Arrangement.spacedBy(spacing.item)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.MilitaryTech, null, Modifier.size(24.dp), tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(spacing.item))
                        Text("称号收藏", Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                        Text(collection.state.page?.titleCollection?.total?.ifBlank { "未知" } ?: "加载中…", style = MaterialTheme.typography.labelLarge)
                    }
                    Text("已佩戴：" + collection.state.page?.titleCollection?.equipped.orEmpty().ifBlank {
                        session.account.loginState.titleBadge?.name ?: "未佩戴"
                    }, style = MaterialTheme.typography.bodyLarge)

                }
            }
            val tabs = (listOf(sb.linux.client.domain.account.AccountNode.Link("我的称号", "https://linux.sb/gacha_profile")) + collection.state.page?.sections.orEmpty()).distinctBy { android.net.Uri.parse(it.url).path }
            if (tabs.isNotEmpty()) LazyRow(horizontalArrangement = Arrangement.spacedBy(spacing.item)) {
                items(tabs, key = { it.url }) { tab ->
                    FilterChip(shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp), selected = (selectedUrl ?: "https://linux.sb/gacha_profile").let { current ->
                        val path = android.net.Uri.parse(current).path.orEmpty()
                        val tabPath = android.net.Uri.parse(tab.url).path.orEmpty()
                        path == tabPath || (tabPath == "/gacha_market" && path.startsWith("/gacha_market"))
                    } == true, onClick = { requestLink(tab.url) }, enabled = !busy,
                        label = { Text(tab.label) }, leadingIcon = {
                            Icon(when (android.net.Uri.parse(tab.url).path) {
                                "/gacha" -> Icons.Filled.Casino
                                "/gacha_forge_center" -> Icons.Filled.LocalFireDepartment
                                "/gacha_recycle_center" -> Icons.Filled.Recycling
                                "/gacha_recipes" -> Icons.Filled.Hub
                                "/gacha_market" -> Icons.Filled.Storefront
                                else -> Icons.Filled.MilitaryTech
                            }, null, Modifier.size(18.dp))
                        })
                }
            }
            if (selectedUrl != null) state.page?.titleHeader?.let { header ->
                Column(verticalArrangement = Arrangement.spacedBy(spacing.inline)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(header.title, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                        if (header.stat.isNotBlank()) Text(header.stat, style = MaterialTheme.typography.labelMedium)
                    }
                    if (header.description.isNotBlank()) Text(header.description, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            when {
                state.loading -> LoadingBox(Modifier.fillMaxWidth().height(200.dp))
                state.page == null -> ErrorBox(state.error ?: "称号内容不可用", Modifier.fillMaxWidth().height(200.dp)) {
                    active.load(selectedUrl ?: "/gacha_profile")
                }
                else -> {
                    state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                    key(active, state.revision) {
                        CompositionLocalProvider(LocalAccountVerificationGateway provides session.capVerificationGateway,
                            LocalGachaForgeAll provides section::forgeAll) {
                            AccountNodes(state.page!!.nodes, session.accountManagementRepository, !busy,
                                onLink = ::requestLink, onDirty = { dirty = true }, onSubmit = active::submit)
                        }
                    }
                    section.forgeProgress?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                    if (busy) {
                        LinearProgressIndicator(Modifier.fillMaxWidth())
                        if (section.forgeProgress != null) TextButton(onClick = section::stopForge) { Text("停止后续熔炼") }
                    }
                }
            }
        }
        pendingLink?.let { url ->
            AlertDialog(onDismissRequest = { pendingLink = null }, title = { Text("有未保存的修改") },
                text = { Text("切换页面将丢弃当前输入。") },
                confirmButton = { TextButton(onClick = { pendingLink = null; navigate(url) }) { Text("继续") } },
                dismissButton = { TextButton(onClick = { pendingLink = null }) { Text("取消") } })
        }
    }
}
