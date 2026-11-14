package sb.linux.client.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import sb.linux.client.domain.account.*
import sb.linux.client.ui.LocalLinkHandler
import sb.linux.client.ui.components.feedback.ErrorBox
import sb.linux.client.ui.components.feedback.LoadingBox
import sb.linux.client.ui.feature.profile.*
import sb.linux.client.ui.session.Session
import sb.linux.client.ui.theme.LocalAppSpacing

/** 主体由源站解析，视觉层只使用应用主题与原生控件。 */
@Composable
internal fun NativeAccountScreen(
    session: Session, nav: NavHostController, title: String, path: String,
    onChanged: () -> Unit = {},
) {
    if (android.net.Uri.parse(path).path.orEmpty().startsWith("/gacha")) {
        WebScreen(session, nav, sourceUrl = path,
            initialContentSelector = ".gacha-profile-page, .gacha-center-page, main")
        return
    }
    val account = session.account.loginState
    AccountPageGate(account.loggedIn && account.userId > 0, title, "登录后管理$title", nav, settingsStyle = true) {
        val version = session.accountIdentityVersion
        val factory = remember(session.accountManagementRepository, account.userId, version) {
            AccountManagementViewModelFactory(session.accountManagementRepository, { session.accountIdentityVersion }, onChanged)
        }
        val vm: AccountManagementViewModel = viewModel(key = "account:$path:${account.userId}:$version", factory = factory)
        val state = vm.state
        var dirty by remember(vm, state.revision) { mutableStateOf(false) }
        var pendingLink by remember(vm) { mutableStateOf<String?>(null) }
        val open = LocalLinkHandler.current
        val exit = rememberSettingsExit(dirty) { nav.popBackStack() }
        LaunchedEffect(vm, path) { vm.load(path) }
        val activityPage = path.startsWith("/community_activity/")
        SettingsPage(title, nav, onBack = exit, headerActions = {
            sb.linux.client.ui.components.navigation.AppRefreshButton(onClick = {
                if (dirty) pendingLink = state.page?.url ?: path else vm.load(state.page?.url ?: path)
            }, enabled = !state.busy, loading = state.loading)
        }) {
            when {
                state.loading -> LoadingBox(Modifier.fillMaxWidth().height(240.dp))
                state.page == null -> ErrorBox(state.error ?: "内容不可用", Modifier.fillMaxWidth().height(240.dp), onRetry = { vm.load(path) })
                else -> {
                    state.error?.let { message ->
                        Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.errorContainer) {
                            Text(message, Modifier.fillMaxWidth().padding(LocalAppSpacing.current.pageHorizontal), color = MaterialTheme.colorScheme.onErrorContainer)
                        }
                    }
                    val page = state.page!!
                    val followLink: (String) -> Unit = { url ->
                        if (!state.busy) {
                            if (dirty) pendingLink = url
                            else if (isAccountManagementLink(url)) vm.load(url) else open?.invoke(url)
                        }
                    }
                    key(state.revision) {
                        AccountNodes(page.nodes, session.accountManagementRepository, !state.busy,
                            onLink = followLink, onDirty = { dirty = true }, onSubmit = vm::submit)
                    }
                    if (state.busy) LinearProgressIndicator(Modifier.fillMaxWidth())
                }
            }
        }
        pendingLink?.let { url ->
            AlertDialog(onDismissRequest = { pendingLink = null }, title = { Text("有未保存的修改") },
                text = { Text("继续将丢弃当前输入。") },
                confirmButton = { TextButton(onClick = { pendingLink = null; dirty = false; if (isAccountManagementLink(url)) vm.load(url) else open?.invoke(url) }) { Text("继续") } },
                dismissButton = { TextButton(onClick = { pendingLink = null }) { Text("取消") } })
        }
    }
}

internal fun isAccountManagementLink(url: String): Boolean {
    val uri = android.net.Uri.parse(url)
    if (!sb.linux.client.common.network.Endpoints.isForumHost(uri.host)) return false
    val path = uri.path.orEmpty()
    return path in setOf("/community_wallet", "/user_privacy", "/user_block", "/promotions", "/invite_center") ||
        path.startsWith("/community_activity/") ||
        (path.startsWith("/user/") && uri.getQueryParameter("tab") == "attachments") ||
        (path == "/index.php" && (uri.getQueryParameter("a") == "user_block" ||
            (uri.getQueryParameter("sort") == "following" && uri.getQueryParameter("follow_view") in setOf("following", "followers"))))
}

@Composable
internal fun AccountNodes(
    nodes: List<AccountNode>, repository: AccountManagementRepository, enabled: Boolean,
    onLink: (String) -> Unit, onDirty: () -> Unit,
    onSubmit: (AccountForm, List<Pair<String, String>>) -> Unit, depth: Int = 0,
) {
    val spacing = LocalAppSpacing.current
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(spacing.item)) {
        nodes.forEach { node ->
            when (node) {
                is AccountNode.Text -> Text(node.value,
                    style = when (node.heading) { 1, 2 -> MaterialTheme.typography.titleLarge; 3, 4 -> MaterialTheme.typography.titleMedium; else -> MaterialTheme.typography.bodyMedium },
                    fontWeight = if (node.heading > 0) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (node.heading > 0) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = if (depth == 0) Modifier.padding(horizontal = spacing.pageHorizontal, vertical = spacing.inline) else Modifier)
                is AccountNode.Link -> SettingMenuRow(node.label, "", showChevron = true, enabled = enabled) { onLink(node.url) }
                is AccountNode.Image -> AsyncImage(node.url, node.description, Modifier.fillMaxWidth().heightIn(max = 160.dp), contentScale = ContentScale.Fit)
                is AccountNode.Progress -> LinearProgressIndicator(progress = { node.fraction }, drawStopIndicator = {}, modifier = Modifier.fillMaxWidth())
                is AccountNode.Gift -> GachaGiftPanel(node.form, repository, enabled, onDirty, onSubmit)
                is AccountNode.DrawActions -> GachaDrawActions(node.forms, enabled, onSubmit)
                is AccountNode.OwnedTitle -> GachaOwnedTitleCard(node, repository, enabled, onDirty, onSubmit)
                is AccountNode.Form -> if (isGachaWearForm(node.value)) GachaWearControls(listOf(node.value), enabled, onDirty, onSubmit)
                    else NativeAccountForm(node.value, repository, enabled, onDirty, onSubmit, embedded = depth > 0)
                is AccountNode.Section -> AccountSectionCard(node.title, node.description) {
                    AccountNodes(node.children, repository, enabled, onLink, onDirty, onSubmit, depth + 1)
                }
                is AccountNode.Actions -> FlowRow(horizontalArrangement = Arrangement.spacedBy(spacing.item),
                    verticalArrangement = Arrangement.spacedBy(spacing.inline)) {
                    val wear = node.children.filterIsInstance<AccountNode.Form>().map { it.value }.filter(::isGachaWearForm)
                    if (wear.isNotEmpty()) GachaWearControls(wear, enabled, onDirty, onSubmit)
                    node.children.filterNot { it is AccountNode.Form && isGachaWearForm(it.value) }.forEach { child -> when (child) {
                        is AccountNode.Link -> OutlinedButton(onClick = { onLink(child.url) }, enabled = enabled,
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)) { Text(child.label) }
                        is AccountNode.Form -> Box(Modifier.widthIn(max = 320.dp)) {
                            NativeAccountForm(child.value, repository, enabled, onDirty, onSubmit, embedded = true,
                                compact = child.value.fields.isEmpty() && child.value.recipientLookup.isBlank() && child.value.imageUpload.isBlank())
                        }
                        else -> AccountNodes(listOf(child), repository, enabled, onLink, onDirty, onSubmit, depth + 1)
                    } }
                }
                is AccountNode.Statistics -> AccountStatistics(node.values)
                is AccountNode.Storage -> AttachmentStorageCard(node, repository, enabled, onLink, onDirty, onSubmit)
                is AccountNode.Attachments -> ManagedAttachmentsContent(node, enabled, onLink, onDirty, onSubmit)
                is AccountNode.Promotion -> PromotionManagementCard(node, repository, enabled, onLink, onDirty, onSubmit)
                is AccountNode.Invitation -> InvitationManagementCard(node, repository, enabled, onDirty, onSubmit)
                is AccountNode.Grid -> GachaAdaptiveGrid(node.children.size, node.columns,
                    minimumWidth = if (node.children.all { it is AccountNode.RarityStat }) 140.dp else 210.dp) { index ->
                    AccountNodes(listOf(node.children[index]), repository, enabled, onLink, onDirty, onSubmit, depth + 1)
                }
                is AccountNode.Badge -> GachaSourceBadge(node.value)
                is AccountNode.Badges -> FlowRow(horizontalArrangement = Arrangement.spacedBy(spacing.item),
                    verticalArrangement = Arrangement.spacedBy(spacing.item)) { node.values.forEach { GachaSourceBadge(it) } }
                is AccountNode.RarityStat -> GachaSourceRarityStat(node)
                is AccountNode.Pills -> GachaSourcePills(node.values)
                is AccountNode.News -> GachaSourceNews(node.values)
                is AccountNode.Notice -> GachaSourceNotice(node.text)
                is AccountNode.TitleMaterials -> GachaMaterialCards(node, enabled, onDirty, onSubmit)
                is AccountNode.TitleCard -> GachaSourceCard(node.equipped) {
                    AccountNodes(node.children, repository, enabled, onLink, onDirty, onSubmit, depth + 1)
                }
                is AccountNode.Activity -> CommunityActivityCards(node, enabled)
                is AccountNode.Details -> AccountDetailsCard(node.title, enabled) {
                    AccountNodes(node.children, repository, enabled, onLink, onDirty, onSubmit, depth + 1)
                }
                is AccountNode.Group -> Surface(shape = MaterialTheme.shapes.extraLarge, color = MaterialTheme.colorScheme.surfaceContainerLow) {
                    Column(Modifier.fillMaxWidth().padding(spacing.pageHorizontal)) {
                        AccountNodes(node.children, repository, enabled, onLink, onDirty, onSubmit, depth + 1)
                    }
                }
            }
        }
    }
}
