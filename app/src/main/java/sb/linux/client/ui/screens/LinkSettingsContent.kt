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
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun LinkSettingsContent(session: Session) {
    val factory = remember(session.generalSettingsRepository) { GeneralSettingsViewModelFactory(session.generalSettingsRepository) }
    val viewModel: GeneralSettingsViewModel = viewModel(key = "link-settings", factory = factory)
    val linkMode = viewModel.state.settings.linkOpenMode
            // ---------- 打开链接方式 ----------
            GroupCard {
                Column(Modifier.padding(horizontal = AppMenuDefaults.contentHorizontal, vertical = 10.dp)) {
                    Text("打开外部链接方式", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "帖子内的外部链接与网页的打开方式；站内跳转（楼层、主页）始终在应用内进行",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(10.dp))
                    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                        SegmentedButton(
                            selected = linkMode == 0,
                            onClick = { viewModel.setLinkOpenMode(0) },
                            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                            modifier = Modifier.weight(1f)
                        ) { Text("应用内打开", maxLines = 1) }
                        SegmentedButton(
                            selected = linkMode == 1,
                            onClick = { viewModel.setLinkOpenMode(1) },
                            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                            modifier = Modifier.weight(1f)
                        ) { Text("外部浏览器打开", maxLines = 1) }
                    }
                }
                SwitchRow(
                    "链接预览卡片",
                    "首次点击显示网站图标、标题和介绍，再点击卡片或打开按钮访问网站",
                    checked = session.linkPreviewEnabled,
                    onCheckedChange = { session.preferences.saveLinkPreviewEnabled(it) },
                )
            }



}
