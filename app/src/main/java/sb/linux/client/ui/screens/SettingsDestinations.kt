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
fun StorageSettingsScreen(session: Session, nav: NavHostController) {
    SettingsPage("存储与备份", nav) {
        GroupLabel("空间占用")
        CacheAnalysisCard(session, 0)
        // 原「立即清理 / 退出时自动清理 / 本地备份 / WebDAV 云备份」三级页直接在此展示
        DataManagementContent(session, automatic = false, showAnalysis = false)
        DataManagementContent(session, automatic = true, showAnalysis = false)
        GroupLabel("备份与恢复")
        GroupLabel("本地备份")
        TransferSettingsContent(session, cloud = false)
        GroupLabel("WebDAV 云备份")
        TransferSettingsContent(session, cloud = true, showItems = false)
    }
}

@Composable
fun MyContentScreen(session: Session, nav: NavHostController) {
    SettingsPage("我的内容", nav) {
        listOf(Triple("我的隐私", "原站个人主页可见性设置", "userPrivacy"),
            Triple("社区活跃度", "原站评分、等级与活跃记录", "communityActivity"),
            Triple("浏览历史", "阅读记录与位置", "footprint"),
            Triple("附件管理", "原站附件 · 空间占用 · 回收站", "attachments"),
            Triple("我的收藏", "帖子与评论收藏", "favorites"),
            Triple("AI 总结历史", "已保存的总结与总结收藏", "aiHistory"),
            Triple("发卡兑换记录", "帖子、卡名与兑换码", "cardRedemptions"),
            Triple("已导出的帖子", "本机 HTML 文件 · 大小与状态", "exportedTopics"),
        ).forEach { (title, summary, route) -> SettingMenuRow(title, summary) { nav.navigate(route) } }
    }
}
