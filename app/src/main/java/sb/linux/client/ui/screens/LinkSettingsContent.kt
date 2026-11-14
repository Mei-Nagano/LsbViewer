package sb.linux.client.ui.screens
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import sb.linux.client.ui.session.Session

import androidx.lifecycle.viewmodel.compose.viewModel
import sb.linux.client.ui.feature.settings.GeneralSettingsViewModel
import sb.linux.client.ui.feature.settings.GeneralSettingsViewModelFactory
import sb.linux.client.ui.theme.AppMenuDefaults
import sb.linux.client.ui.patterns.settings.SettingsChoiceRow
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun LinkSettingsContent(session: Session) {
    val factory = remember(session.generalSettingsRepository) { GeneralSettingsViewModelFactory(session.generalSettingsRepository) }
    val viewModel: GeneralSettingsViewModel = viewModel(key = "link-settings", factory = factory)
    val linkMode = viewModel.state.settings.linkOpenMode
            // ---------- 打开链接方式 ----------
            GroupCard {
                SettingsChoiceRow(
                    title = "外部链接打开方式",
                    subtitle = "站内楼层与个人主页始终在应用内打开",
                    options = listOf(0 to "应用内打开", 1 to "外部浏览器"),
                    selectedValue = linkMode, onSelect = viewModel::setLinkOpenMode,
                )
                SwitchRow(
                    "隐藏外链跳转提示",
                    "自动访问外部网站，黑名单网址仍显示风险提醒",
                    checked = viewModel.state.settings.hideExternalJumpReminder,
                    onCheckedChange = viewModel::setHideExternalJumpReminder,
                )
                SwitchRow(
                    "链接预览卡片",
                    "首次点击显示网站图标、标题和介绍，再点击卡片或打开按钮访问网站",
                    checked = session.linkPreviewEnabled,
                    onCheckedChange = { session.preferences.saveLinkPreviewEnabled(it) },
                )
            }



}
