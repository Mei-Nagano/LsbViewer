package sb.linux.client.ui.screens
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import sb.linux.client.ui.session.Session
import sb.linux.client.ui.feature.settings.DataManagementViewModel
import sb.linux.client.ui.feature.settings.DataManagementViewModelFactory
import sb.linux.client.ui.theme.AppMenuDefaults
import sb.linux.client.ui.theme.LocalAppSpacing

/** 数据清理内容：并入「存储与备份」二级页展示，不再单独成页。 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DataManagementContent(
    session: Session,
    automatic: Boolean,
    showAnalysis: Boolean = true,
) {
    val factory = remember(session.dataManagementRepository) {
        DataManagementViewModelFactory(session.dataManagementRepository)
    }
    val viewModel: DataManagementViewModel = viewModel(factory = factory)
    val autoClear = viewModel.autoClearItems
    val options = listOf(
        "home" to ("首页与侧边栏缓存" to "帖子列表和侧边栏快照"),
        "topic" to ("帖子与阅读量缓存" to "正文、评论、弹幕和阅读量"),
        "images" to ("图片缓存" to "头像及正文图片的磁盘缓存"),
        "reading_positions" to ("阅读位置" to "各帖子上次看到的位置"),
        "ai_history" to ("AI 总结历史" to "已保存的总结与运行内缓存"),
        "history" to ("浏览历史" to "本地帖子浏览记录"),
        "favorites" to ("本地收藏" to "帖子收藏快照"),
        "comment_favorites" to ("评论收藏" to "收藏的评论快照"),
        "card_redemptions" to ("发卡兑换记录" to "已自动保存的帖子与兑换码"),
        "ai_config" to ("AI 配置与密钥" to "API 地址、密钥、模型、提示词及配置方案"),
        "link_previews" to ("链接预览缓存" to "网站标题、简介和图标地址"),
        "background_image" to ("自定义背景图片" to "删除应用内保存的背景图片，恢复主题底色"),
    )
    var selected by remember { mutableStateOf(emptySet<String>()) }
    var confirmClear by remember { mutableStateOf(false) }
    var cacheRevision by remember { mutableIntStateOf(0) }
    val spacing = LocalAppSpacing.current

    if (confirmClear) sb.linux.client.ui.components.feedback.AppConfirmDialog(
        title = "清理所选数据",
        message = "确定删除已选择的 ${selected.size} 类本地数据吗？此操作无法撤销。",
        onDismiss = { confirmClear = false },
        onConfirm = {
            confirmClear = false
            viewModel.clear(selected) { result ->
                result.onSuccess {
                    selected = emptySet()
                    cacheRevision++
                }.onFailure { session.showToast(it.message ?: "清理失败") }
            }
        },
        confirmText = "清理", dismissText = "取消",
    )

    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(spacing.contentGap)) {
        if (!automatic) {
            if (showAnalysis) {
                GroupLabel("空间占用")
                GroupCard {
                    Column(Modifier.fillMaxWidth().padding(horizontal = AppMenuDefaults.contentHorizontal, vertical = 12.dp)) {
                        CacheAnalysisCard(session, cacheRevision)
                    }
                }
            }
            GroupLabel("立即清理")
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                options.forEach { (key, labels) ->
                    val checked = key in selected
                    ListItem(
                        headlineContent = { Text(labels.first) },
                        supportingContent = { Text(labels.second) },
                        trailingContent = { Checkbox(checked = checked, onCheckedChange = { value ->
                            selected = if (value) selected + key else selected - key
                        }) },
                        colors = ListItemDefaults.colors(containerColor = androidx.compose.ui.graphics.Color.Transparent),
                        modifier = Modifier.clickable { selected = if (checked) selected - key else selected + key },
                    )
                }
            }
            Button(
                onClick = { confirmClear = true }, enabled = selected.isNotEmpty(),
                modifier = Modifier.fillMaxWidth(),
            ) { Text("清理所选项") }
        }
        if (automatic) {
        GroupCard("退出时自动清理") {
            Text(
                "正常退出时清理所选项目；系统强制结束进程时，会在下次启动补做。所选历史或 AI 配置会永久删除，请谨慎开启；账号登录不受影响。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = AppMenuDefaults.contentHorizontal, vertical = 10.dp),
            )
            options.forEach { (key, labels) ->
                SwitchRow(
                    labels.first, labels.second,
                    checked = key in autoClear,
                    onCheckedChange = { enabled -> viewModel.setAutoClearEnabled(key, enabled) },
                )
            }
        }
        }
    }
}
