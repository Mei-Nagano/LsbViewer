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
            Triple("列表与分页", "首页、版块与评论的滚动方式，以及每页数量和帖子卡片", "browseList"),
            Triple("正文与评论", "签名、打赏弹幕、表格滑动、UID 与编码内容", "browseContent"),
            Triple("帖子交互", "评论对话入口与取消收藏确认", "browseInteraction"),
            Triple("侧边栏", "侧栏排列偏好与在线用户头像显示", "browseSidebar"),
            Triple("屏蔽规则", "管理源站关键词、用户与版块屏蔽", "blockWords"),
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
            Triple("应用代理", "自动检测代理，或手动设置 HTTP / SOCKS5 地址", "networkProxy"),
            Triple("加密 DNS", "加密域名解析、VPN 旁路、自动优化与服务器测速", "networkDns"),
            Triple("链接设置", "外链打开方式、跳转提示和预览卡片", "networkLinks"),
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
            Triple("总结行为", "控制总结入口、进帖自动总结与评论内容解析", "aiBehavior"),
            Triple("接口与配置方案", "配置 API、模型、生成参数与可切换的方案", "aiConfiguration"),
            Triple("总结历史", "查看、收藏与管理已保存的总结", "aiHistory"),
        ), nav)
    }
}

@Composable
fun GeneralSettingsScreen(session: Session, nav: NavHostController) {
    SettingsPage("应用与账号", nav) {
        SettingsSectionMenu(listOf(
            Triple("个人资料与账号", "在应用内打开源站个人资料与账号设置", "settings"),
            Triple("版本更新", "选择自动检查时机，或立即查看新版本", "appUpdates"),
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
            Triple("空间占用", "查看图片及其他本地缓存的空间占用", "storageAnalysis"),
            Triple("立即清理", "按类别选择要删除的缓存、记录与配置", "storageClear"),
            Triple("自动清理", "选择退出应用后自动清理的数据类别", "storageAutomatic"),
        ), nav)
        GroupLabel("备份与恢复")
        SettingsSectionMenu(listOf(
            Triple("本地备份", "选择备份范围，导出或恢复本地 JSON 文件", "backupLocal"),
            Triple("WebDAV 云备份", "配置 WebDAV 网盘，备份或恢复所选内容", "backupCloud"),
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
