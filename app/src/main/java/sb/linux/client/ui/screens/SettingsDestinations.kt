package sb.linux.client.ui.screens
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import sb.linux.client.ui.session.Session
import androidx.lifecycle.viewmodel.compose.viewModel
import sb.linux.client.ui.feature.media.ImageHostSettingsViewModel
import sb.linux.client.ui.feature.media.ImageHostSettingsViewModelFactory

@Composable
fun UploadSettingsScreen(session: Session, nav: NavHostController) {
    val factory = remember(session.imageHostSettingsRepository) { ImageHostSettingsViewModelFactory(session.imageHostSettingsRepository) }
    val model: ImageHostSettingsViewModel = viewModel(factory = factory)
    val state = model.state
    val dirty = state.urlDraft.trim() != state.settings.url || state.tokenDraft.trim() != state.settings.token ||
        state.fileFieldDraft.trim().ifBlank { "file" } != state.settings.fileField
    val exit = rememberSettingsExit(dirty) { nav.popBackStack() }
    SettingsPage("发帖与图片上传", nav, onBack = exit) { ImageHostSettingsCard(session, 0) }
}

@Composable
fun MyContentScreen(session: Session, nav: NavHostController) {
    SettingsPage("我的内容", nav) {
        val entries = buildList {
            addAll(listOf(Triple("抽奖奖品记录", "获奖通知与兑换码", "lotteryPrizes"),
            Triple("浏览历史", "阅读记录与位置", "footprint"),
            Triple("附件管理", "原站附件 · 空间占用 · 回收站", "attachments"),
            Triple("我的收藏", "帖子与评论收藏", "favorites"),
            Triple("AI 总结历史", "已保存的总结与总结收藏", "aiHistory"),
            Triple("发卡兑换记录", "源站兑换通知与积分消耗", "cardRedemptions"),
            Triple("已导出的帖子", "本机 HTML 文件 · 大小与状态", "exportedTopics"),
            ))
        }
        Column(verticalArrangement = Arrangement.spacedBy(sb.linux.client.ui.theme.LocalAppSpacing.current.inline)) {
            entries.forEachIndexed { index, (title, summary, route) ->
                SettingMenuRow(title, summary,
                    shape = sb.linux.client.ui.patterns.settings.settingsItemShape(index, entries.size)) { nav.navigate(route) }
            }
        }
    }
}
