package sb.linux.client.ui.navigation

import sb.linux.client.ui.components.feedback.AppInlineLoading
import sb.linux.client.common.network.Endpoints
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.CompositionLocalProvider
import androidx.navigation.NavHostController
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.viewmodel.compose.viewModel
import sb.linux.client.domain.linkpreview.LinkPreviewInfo
import sb.linux.client.ui.session.Session
import sb.linux.client.ui.LocalLinkHandler
import sb.linux.client.ui.feature.linkpreview.LinkPreviewViewModel
import sb.linux.client.ui.feature.linkpreview.LinkPreviewViewModelFactory
import sb.linux.client.ui.feature.settings.GeneralSettingsViewModel
import sb.linux.client.ui.feature.settings.GeneralSettingsViewModelFactory

@Composable
internal fun AppLinkProvider(
    session: Session,
    linkNav: NavHostController,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val masterNav = LocalMasterNav.current
    val settingsFactory = remember(session.generalSettingsRepository) {
        GeneralSettingsViewModelFactory(session.generalSettingsRepository)
    }
    val settingsViewModel: GeneralSettingsViewModel = viewModel(factory = settingsFactory)
    val previewFactory = remember(session.linkPreviewRepository) {
        LinkPreviewViewModelFactory(session.linkPreviewRepository)
    }
    val previewViewModel: LinkPreviewViewModel = viewModel(factory = previewFactory)
    var linkPreviewTarget by remember { mutableStateOf<String?>(null) }
    var linkPreviewInfo by remember { mutableStateOf<LinkPreviewInfo?>(null) }
    var linkPreviewLoading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val signaturesHidden = remember(session.accountIdentityVersion) { mutableStateOf(false) }
    val signatureRestoreVersion = remember(session.accountIdentityVersion) { mutableStateOf(0) }

    fun openLinkDirect(raw: String) {
        val url = Endpoints.abs(raw)
        val uri = android.net.Uri.parse(url)
        if (Endpoints.isInternal(uri.host)) {
            val path = uri.path.orEmpty().trimEnd('/').ifBlank { "/" }
            sourceHomeSelection(url)?.let { (combo, index) ->
                session.homeBrowse.homeCombo = combo
                session.homeBrowse.homeTabIndex = index
                (masterNav ?: linkNav).navigate("home") { launchSingleTop = true }
                return
            }
            val route = sourcePageRoute(url, session.account.loginState.userId)
            if (route != null) {
                (if (route == "forums") masterNav ?: linkNav else linkNav).navigate(route)
                return
            }
            linkNav.navigate("web?url=${android.net.Uri.encode(url)}")
            return
        }
        if (settingsViewModel.linkOpenMode() == 0) {
            linkNav.navigate("web?url=${android.net.Uri.encode(url)}")
        } else {
            runCatching {
                context.startActivity(
                    android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url))
                        .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK),
                )
            }
        }
    }

    fun openResolvedLink(url: String) {
        val uri = android.net.Uri.parse(url)
        if (session.generalSettingsRepository.current().hideExternalJumpReminder &&
            Endpoints.isInternal(uri.host) && uri.path?.trimEnd('/') == "/jump") {
            scope.launch {
                val target = try { session.externalJumpRepository.permittedTarget(url) }
                catch (cancelled: CancellationException) { throw cancelled }
                catch (_: Exception) { null }
                openLinkDirect(target ?: url)
            }
        } else openLinkDirect(url)
    }

    fun openLink(raw: String) {
        val url = Endpoints.abs(raw)
        val host = runCatching { android.net.Uri.parse(url).host.orEmpty() }.getOrDefault("")
        val externalJump = SourceExternalJump.target(url)
        if (!session.linkPreviewEnabled || (Endpoints.isInternal(host) && externalJump == null)) openResolvedLink(url)
        else {
            linkPreviewInfo = null
            linkPreviewTarget = url
        }
    }

    LaunchedEffect(linkPreviewTarget) {
        val url = linkPreviewTarget ?: return@LaunchedEffect
        linkPreviewLoading = true
        try {
            linkPreviewInfo = previewViewModel.load(SourceExternalJump.target(url) ?: url)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            linkPreviewInfo = LinkPreviewInfo(url, android.net.Uri.parse(url).host ?: url, "", "")
        }
        linkPreviewLoading = false
    }

    if (linkPreviewTarget != null) {
        AlertDialog(
            onDismissRequest = { linkPreviewTarget = null; linkPreviewInfo = null },
            title = { Text("链接预览") },
            text = {
                if (linkPreviewLoading) {
                    androidx.compose.foundation.layout.Row(verticalAlignment = Alignment.CenterVertically) {
                        AppInlineLoading()
                        androidx.compose.foundation.layout.Spacer(Modifier.width(10.dp))
                        Text("正在读取网站信息…")
                    }
                } else {
                    val info = linkPreviewInfo
                    androidx.compose.foundation.layout.Row(verticalAlignment = Alignment.Top) {
                        coil.compose.AsyncImage(
                            model = info?.iconUrl,
                            contentDescription = null,
                            modifier = Modifier.size(42.dp).clip(MaterialTheme.shapes.medium),
                        )
                        androidx.compose.foundation.layout.Spacer(Modifier.width(12.dp))
                        androidx.compose.foundation.layout.Column {
                            Text(info?.title.orEmpty().ifBlank { "未知网站" }, style = MaterialTheme.typography.titleMedium)
                            if (!info?.description.isNullOrBlank()) {
                                androidx.compose.foundation.layout.Spacer(Modifier.height(4.dp))
                                Text(info!!.description, style = MaterialTheme.typography.bodySmall, maxLines = 4)
                            }
                            androidx.compose.foundation.layout.Spacer(Modifier.height(6.dp))
                            Text(info?.url ?: linkPreviewTarget.orEmpty(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val target = linkPreviewTarget ?: return@TextButton
                    linkPreviewTarget = null; linkPreviewInfo = null
                    openResolvedLink(target)
                }) { Text("打开链接") }
            },
            dismissButton = { TextButton(onClick = { linkPreviewTarget = null; linkPreviewInfo = null }) { Text("取消") } },
        )
    }
    CompositionLocalProvider(
        sb.linux.client.ui.screens.LocalAllSignaturesHidden provides signaturesHidden,
        sb.linux.client.ui.screens.LocalSignatureRestoreVersion provides signatureRestoreVersion,
        sb.linux.client.ui.patterns.user.LocalUserBlockChanged provides session::onUserSummaryBlockChanged,
        sb.linux.client.ui.patterns.user.LocalUserSummaryRepository provides session.userSummaryRepository,
        sb.linux.client.ui.patterns.user.LocalAvatarUserLookupRepository provides session.userLookupRepository,
        sb.linux.client.ui.patterns.user.LocalAvatarAccountId provides session.account.loginState.userId,
        sb.linux.client.ui.patterns.user.LocalAvatarAccountVersion provides session.accountIdentityVersion,
        sb.linux.client.ui.patterns.user.LocalUserFollowChanged provides session::onUserSummaryFollowChanged,
        LocalLinkHandler provides { openLink(it) },
        content = content,
    )
}

