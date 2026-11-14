package sb.linux.client.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import sb.linux.client.ui.components.feedback.AppConfirmDialog
import sb.linux.client.ui.feature.profile.PersonalSignatureViewModel
import sb.linux.client.ui.feature.profile.PersonalSignatureViewModelFactory
import sb.linux.client.ui.patterns.settings.SettingsCard
import sb.linux.client.ui.session.Session
import sb.linux.client.ui.theme.LocalAppSpacing

// 主层级为签名编辑和保存，源站规则是辅助说明；沿用设置外壳及主题字阶/间距。
@Composable
internal fun PersonalSignatureScreen(session: Session, nav: NavHostController) {
    val account = session.account.loginState
    AccountPageGate(account.loggedIn && account.userId > 0, "个人签名", "登录后管理个人签名", nav,
        settingsStyle = true) {
        val factory = remember(session.personalSignatureRepository) {
            PersonalSignatureViewModelFactory(session.personalSignatureRepository)
        }
        val viewModel: PersonalSignatureViewModel = viewModel(factory = factory, key = "personal-signature-${account.userId}-${session.accountIdentityVersion}")
        val state by viewModel.uiState.collectAsState()
        var confirmExit by remember { mutableStateOf(false) }
        val spacing = LocalAppSpacing.current
        fun back() { if (state.changed || state.saving) confirmExit = true else nav.popBackStack() }
        BackHandler { back() }
        LaunchedEffect(viewModel) { viewModel.load() }

        SettingsPage("个人签名", nav, onBack = { back() }) {
            if (state.loading) LinearProgressIndicator(Modifier.fillMaxWidth())
            state.page?.let { page ->
                SettingsCard {
                    Column(Modifier.padding(spacing.pageHorizontal),
                        verticalArrangement = Arrangement.spacedBy(spacing.contentGap)) {
                        if (page.description.isNotBlank()) Text(page.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        SettingsTextField(
                            value = state.signature, onValueChange = viewModel::editSignature,
                            label = { Text("签名内容") },
                            supportingText = { Text(page.signatureHint.removePrefix("签名内容").trim()) },
                            enabled = !state.loading && !state.saving,
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.large,
                        )
                    }
                }
                SettingsCard {
                    SwitchRow("隐藏全站帖子签名",
                        page.hiddenHint.removePrefix("隐藏全站帖子签名").trim(), state.hidden,
                        enabled = !state.loading && !state.saving,
                        onCheckedChange = viewModel::editHidden)
                }
                Button(onClick = viewModel::save,
                    enabled = state.changed && !state.loading && !state.saving,
                    modifier = Modifier.fillMaxWidth()) {
                    Text(if (state.saving) "正在保存…" else "保存设置")
                }
            }
            state.error?.let { error ->
                SettingsCard {
                    Column(Modifier.padding(spacing.pageHorizontal)) {
                        Text(error, style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error)
                        if (state.page == null && !state.loading) TextButton(onClick = viewModel::load) { Text("重试") }
                    }
                }
            }
            state.message?.let { Text(it, style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary) }
        }
        if (confirmExit) AppConfirmDialog(
            title = "离开个人签名", message = if (state.saving) "正在保存签名，是否离开？" else "修改尚未保存，是否放弃修改并返回？",
            onDismiss = { confirmExit = false },
            onConfirm = { confirmExit = false; nav.popBackStack() },
            confirmText = "离开", dismissText = "继续编辑",
        )
    }
}
