package sb.linux.client.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import sb.linux.client.ui.patterns.settings.settingsItemShape
import sb.linux.client.ui.session.Session
import sb.linux.client.ui.theme.LocalAppSpacing

/** 设置入口的内容与跳转集中组合；详情继续由原有控件和 ViewModel 拥有。 */
@Composable
private fun SettingsSectionMenu(entries: List<Triple<String, String, String>>, nav: NavHostController) {
    Column(verticalArrangement = Arrangement.spacedBy(LocalAppSpacing.current.inline)) {
        entries.forEachIndexed { index, (title, description, route) ->
            SettingMenuRow(title, description, shape = settingsItemShape(index, entries.size)) { nav.navigate(route) }
        }
    }
}

@Composable
fun BrowseSettingsScreen(session: Session, nav: NavHostController) {
    SettingsPage("阅读与交互", nav) {
        SettingsSectionMenu(listOf(
            Triple("列表与分页", "滚动方式、每页数量与首页缓存", "browseList"),
            Triple("正文与评论", "表格、UID、编码识别与打赏弹幕", "browseContent"),
            Triple("帖子交互", "对话串联与收藏确认", "browseInteraction"),
            Triple("侧边栏", "排列方式与在线用户", "browseSidebar"),
            Triple("屏蔽规则", "管理屏蔽词、用户与版块", "blockWords"),
        ), nav)
        SettingsResetRow("重置阅读与交互设置") {
            session.browseSettingsRepository.reset()
            session.applyBrowseSettings(session.browseSettingsRepository.current())
        }
    }
}

@Composable
fun NetworkSettingsScreen(session: Session, nav: NavHostController) {
    SettingsPage("网络与链接", nav) {
        SettingsSectionMenu(listOf(
            Triple("应用代理", "选择代理类型与连接地址", "networkProxy"),
            Triple("加密 DNS", "DoH 服务、自动优化与服务器管理", "networkDns"),
            Triple("链接设置", "外部链接打开方式与预览卡片", "networkLinks"),
        ), nav)
        SettingsResetRow("重置网络设置") {
            session.proxySettingsRepository.resetNetworkSettings()
            session.showToast("已重置网络设置")
        }
    }
}

@Composable
fun AiSettingsScreen(session: Session, nav: NavHostController, section: String = "menu") {
    if (section != "menu") {
        AiSettingsDetailScreen(session, nav, section)
        return
    }
    SettingsPage("AI 助手", nav) {
        SettingsSectionMenu(listOf(
            Triple("总结行为", "开启总结、自动运行与评论解析", "aiBehavior"),
            Triple("接口与配置方案", "API、模型、提示词与已保存方案", "aiConfiguration"),
            Triple("总结历史", "查看与管理已保存的总结", "aiHistory"),
        ), nav)
    }
}

@Composable
fun GeneralSettingsScreen(session: Session, nav: NavHostController) {
    SettingsPage("应用与账号", nav) {
        SettingsSectionMenu(listOf(
            Triple("个人资料与账号", "管理原站个人资料与账号", "settings"),
            Triple("版本更新", "检查新版本与设置检查时机", "appUpdates"),
            Triple("关于应用", "应用信息、项目地址与开源协议", "about"),
        ), nav)
        SettingsResetRow("重置所有应用设置") {
            session.preferences.resetAllAppSettings()
            session.showToast("已重置所有应用设置")
        }
    }
}

@Composable
fun StorageSettingsScreen(session: Session, nav: NavHostController) {
    SettingsPage("存储与备份", nav) {
        SettingsSectionMenu(listOf(
            Triple("空间占用", "查看图片、帖子与其他缓存大小", "storageAnalysis"),
            Triple("立即清理", "选择需要清理的本地数据", "storageClear"),
            Triple("自动清理", "设置退出后清理的项目", "storageAutomatic"),
        ), nav)
        GroupLabel("备份与恢复")
        SettingsSectionMenu(listOf(
            Triple("本地备份", "选择备份内容与导入导出 JSON", "backupLocal"),
            Triple("WebDAV 云备份", "服务器配置、云端备份与恢复", "backupCloud"),
        ), nav)
    }
}

@Composable
internal fun StorageSettingsDetailScreen(session: Session, nav: NavHostController, section: String) {
    val title = when (section) {
        "analysis" -> "空间占用"
        "clear" -> "立即清理"
        "automatic" -> "自动清理"
        "local" -> "本地备份"
        else -> "WebDAV 云备份"
    }
    SettingsPage(title, nav) {
        when (section) {
            "analysis" -> CacheAnalysisCard(session, 0)
            "clear" -> DataManagementContent(session, automatic = false, showAnalysis = false)
            "automatic" -> DataManagementContent(session, automatic = true, showAnalysis = false)
            "local" -> TransferSettingsContent(session, cloud = false)
            "cloud" -> TransferSettingsContent(session, cloud = true)
        }
    }
}
