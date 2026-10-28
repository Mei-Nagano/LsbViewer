package sb.linux.client.ui.screens
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import sb.linux.client.ui.session.Session
import androidx.lifecycle.viewmodel.compose.viewModel
import sb.linux.client.ui.feature.settings.BrowseSettingsViewModel
import sb.linux.client.ui.feature.settings.BrowseSettingsViewModelFactory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowseSettingsScreen(session: Session, nav: NavHostController) {
    val factory = remember(session.browseSettingsRepository) {
        BrowseSettingsViewModelFactory(session.browseSettingsRepository, session::applyBrowseSettings)
    }
    val viewModel: BrowseSettingsViewModel = viewModel(factory = factory)
    val state = viewModel.state
    val settings = state.settings
    val homeInf = state.homeInfinite
    val forumInf = state.forumInfinite
    val topicInf = state.topicInfinite
    val anyPaged = state.anyPaged
    val topicsText = state.topicsText
    val commentsText = state.commentsText

    SettingsPage("阅读与交互", nav) {


            GroupCard("列表与分页", icon = Icons.AutoMirrored.Filled.ViewList) {
                listOf(Triple("首页无限滚动", "home", homeInf), Triple("板块无限滚动", "forum", forumInf),
                    Triple("评论无限滚动", "topic", topicInf)).forEach { (title, key, enabled) ->
                    SwitchRow(title, "关闭后使用分页", enabled) { value ->
                        viewModel.selectScope(key)
                        viewModel.applyScrollMode(value)
                    }
                }
                // 翻页相关设置：仅当存在翻页分类时展示（3.14）
                if (anyPaged) {
                    HorizontalDivider(Modifier.padding(horizontal = 18.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                    Column(Modifier.padding(horizontal = 18.dp, vertical = 8.dp)) {
                        OutlinedTextField(
                            value = topicsText,
                            onValueChange = viewModel::setTopicsText,
                            label = { Text("每页帖子数（5 - 50）") },
                            suffix = { Text("条") },
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(10.dp))
                        OutlinedTextField(
                            value = commentsText,
                            onValueChange = viewModel::setCommentsText,
                            label = { Text("评论每页数量（5 - 100）") },
                            suffix = { Text("条") },
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text(
                            "翻页模式下每页展示的帖子与评论条数",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 6.dp)
                        )
                    }
                }
                SwitchRow(
                    "启动时不刷新首页",
                    "打开应用直接显示上次缓存内容，手动刷新才更新",
                    checked = settings.homeKeepCache,
                    onCheckedChange = viewModel::setHomeKeepCache
                )
                SwitchRow(
                    "折叠刷新后的新帖子",
                    "刷新回到顶部，把新增帖子收纳为折叠条，点击后展开",
                    checked = settings.collapseNewTopics,
                    onCheckedChange = viewModel::setCollapseNewTopics
                )
            }
            GroupCard("侧边栏") {
                SwitchRow(
                    "侧边栏双列显示",
                    "快捷功能与版块列表双列排布（可在首页侧边栏切换）",
                    checked = settings.sidebarTwoColumns,
                    onCheckedChange = viewModel::setSidebarTwoColumns
                )
                SwitchRow(
                    "侧边栏展示在线用户",
                    "在侧边栏展示源站在线用户及其人数",
                    checked = settings.sidebarShowOnlineUsers,
                    onCheckedChange = viewModel::setSidebarShowOnlineUsers
                )
            }
            GroupCard("正文与评论", icon = Icons.AutoMirrored.Filled.Chat) {
                SwitchRow(
                    "显示打赏弹幕",
                    "在帖子正文与评论区之间滚动展示打赏信息，可在帖子菜单中单独开关",
                    checked = settings.danmakuOn,
                    onCheckedChange = viewModel::setDanmaku
                )
                SwitchRow(
                    "Markdown 表格横向滑动",
                    if (settings.tableDisplayMode == 1) "保持单元格可读宽度，可左右滑动" else "关闭时自动压缩列宽以适应手机屏幕",
                    checked = settings.tableDisplayMode == 1,
                    onCheckedChange = { viewModel.setTableDisplayMode(if (it) 1 else 0) }
                )
                SwitchRow(
                    "显示 UID",
                    "在帖子作者头像下方仅显示 UID 数字",
                    checked = settings.showUid,
                    onCheckedChange = viewModel::setShowUid
                )
                SwitchRow(
                    "智能识别编码内容",
                    "识别 Base64、URL Base64、Hex、URL、Unicode 和显式 ROT13 内容",
                    checked = settings.smartDecodeEnabled,
                    onCheckedChange = viewModel::setSmartDecodeEnabled
                )
            }
            GroupCard("帖子交互", initiallyExpanded = false, icon = Icons.Filled.TouchApp) {
                // 正文长按已交回系统原生选择菜单（复制/翻译/搜索由系统提供），这里不再有自定义菜单项配置
                SwitchRow(
                    "对话串联",
                    "源站已支持树形结构评论，此功能不再需要；开启后评论区显示「查看对话」入口",
                    checked = settings.threadDialogEnabled,
                    onCheckedChange = viewModel::setThreadDialogEnabled
                )
                SwitchRow(
                    "取消收藏确认",
                    "关闭后已收藏帖子点击收藏图标直接取消收藏，不再提示",
                    checked = settings.unfavoriteConfirm,
                    onCheckedChange = viewModel::setUnfavoriteConfirm
                )
            }
            SettingMenuRow("屏蔽规则", "关键词 · 用户 · 板块") { nav.navigate("blockWords") }
            SettingsResetRow("重置阅读与交互设置", onConfirm = viewModel::reset)
    }
}

/** AI 总结设置子页面（OpenAI 兼容 API） */
