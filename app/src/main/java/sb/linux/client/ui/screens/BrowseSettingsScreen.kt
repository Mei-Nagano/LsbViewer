package sb.linux.client.ui.screens
import sb.linux.client.ui.patterns.settings.searchingFor
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import sb.linux.client.ui.theme.AppMenuDefaults

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun BrowseSettingsDetailScreen(session: Session, nav: NavHostController, section: String) {
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

    val title = when (section) {
        "list" -> "列表与分页"
        "sidebar" -> "侧边栏"
        "content" -> "正文与评论"
        else -> "帖子交互"
    }
    SettingsPage(title, nav) {
        if (section == "list") {
            GroupCard {
                listOf(Triple("首页无限滚动", "home", homeInf), Triple("板块无限滚动", "forum", forumInf),
                    Triple("评论无限滚动", "topic", topicInf)).forEach { (title, key, enabled) ->
                    SwitchRow(title, "关闭后使用分页", enabled) { value ->
                        viewModel.selectScope(key)
                        viewModel.applyScrollMode(value)
                    }
                }
                // 翻页相关设置：仅当存在翻页分类时展示（3.14）
                if (anyPaged || searchingFor("每页帖子数（5 - 50）", "评论每页数量（5 - 100）")) {
                    GroupCard {
                        SettingsTextField(
                            value = topicsText,
                            onValueChange = viewModel::setTopicsText,
                            label = { Text("每页帖子数（5 - 50）") }, searchLabel = "每页帖子数（5 - 50）",
                            suffix = { Text("条") },
                            singleLine = true,
                            shape = MaterialTheme.shapes.large,
                            modifier = Modifier.fillMaxWidth()
                        )
                        SettingsTextField(
                            value = commentsText,
                            onValueChange = viewModel::setCommentsText,
                            label = { Text("评论每页数量（5 - 100）") }, searchLabel = "评论每页数量（5 - 100）",
                            suffix = { Text("条") },
                            singleLine = true,
                            shape = MaterialTheme.shapes.large,
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
                    "启动时显示上次保存的首页，手动刷新后更新",
                    checked = settings.homeKeepCache,
                    onCheckedChange = viewModel::setHomeKeepCache
                )
                SwitchRow(
                    "折叠刷新后的新帖子",
                    "刷新后将新增帖子收起，点击提示条展开",
                    checked = settings.collapseNewTopics,
                    onCheckedChange = viewModel::setCollapseNewTopics
                )
            }
        }
        if (section == "list") {
            GroupCard("帖子卡片") {
                SwitchRow("头像与元数据同行", "头像右侧分两行显示作者和帖子信息",
                    checked = settings.topicCardAvatarMetadataRow,
                    onCheckedChange = viewModel::setTopicCardAvatarMetadataRow)
                SwitchRow("隐藏帖子标签", "隐藏置顶、精华、抽奖和发卡标签，作者称号继续显示",
                    checked = settings.hideTopicTags,
                    onCheckedChange = viewModel::setHideTopicTags)
            }
        }
        if (section == "sidebar") {
            GroupCard {
                SwitchRow(
                    "侧边栏双列显示",
                    "当前源站侧边栏使用单列导航，此项暂不影响排列",
                    checked = settings.sidebarTwoColumns,
                    onCheckedChange = viewModel::setSidebarTwoColumns
                )
                SwitchRow(
                    "侧边栏展示在线用户",
                    "显示在线用户头像与名字；在线总数始终展示",
                    checked = settings.sidebarShowOnlineUsers,
                    onCheckedChange = viewModel::setSidebarShowOnlineUsers
                )
            }
        }
        if (section == "content") {
            val allSignaturesHidden = LocalAllSignaturesHidden.current
            val signatureRestoreVersion = LocalSignatureRestoreVersion.current
            GroupCard {
                SettingMenuRow("恢复显示帖子签名", "重新显示本次使用中隐藏的帖子与评论签名",
                    showChevron = false) {
                    allSignaturesHidden.value = false
                    signatureRestoreVersion.value++
                    session.showToast("已恢复显示签名")
                }
                SwitchRow(
                    "显示打赏弹幕",
                    "在正文与评论之间滚动显示打赏信息，也可在帖子菜单中切换",
                    checked = settings.danmakuOn,
                    onCheckedChange = viewModel::setDanmaku
                )
                SwitchRow(
                    "Markdown 表格横向滑动",
                    if (settings.tableDisplayMode == 1) "保持表格内容的可读宽度，超出屏幕时左右滑动" else "关闭后压缩列宽，使表格适应屏幕",
                    checked = settings.tableDisplayMode == 1,
                    onCheckedChange = { viewModel.setTableDisplayMode(if (it) 1 else 0) }
                )
                SwitchRow(
                    "显示 UID",
                    "在帖子作者信息中显示 UID 数字",
                    checked = settings.showUid,
                    onCheckedChange = viewModel::setShowUid
                )
                SwitchRow(
                    "智能识别编码内容",
                    "将识别到的 Base64、Hex、URL、Unicode 和 ROT13 内容解码显示",
                    checked = settings.smartDecodeEnabled,
                    onCheckedChange = viewModel::setSmartDecodeEnabled
                )
            }
        }
        if (section == "interaction") {
            GroupCard {
                // 正文长按已交回系统原生选择菜单（复制/翻译/搜索由系统提供），这里不再有自定义菜单项配置
                SwitchRow(
                    "对话串联",
                    "在评论区显示「查看对话」入口，方便阅读关联回复",
                    checked = settings.threadDialogEnabled,
                    onCheckedChange = viewModel::setThreadDialogEnabled
                )
                SwitchRow(
                    "取消收藏确认",
                    "当前文章取消收藏始终需要确认，此开关暂不影响该操作",
                    checked = settings.unfavoriteConfirm,
                    onCheckedChange = viewModel::setUnfavoriteConfirm
                )
            }
        }
    }
}
