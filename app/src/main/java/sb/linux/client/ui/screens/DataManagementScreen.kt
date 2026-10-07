package sb.linux.client.ui.screens
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import sb.linux.client.ui.session.Session
import sb.linux.client.ui.feature.settings.DataManagementViewModel
import sb.linux.client.ui.feature.settings.DataManagementViewModelFactory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DataManagementScreen(session: Session, nav: NavHostController) {
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
        "usage_stats" to ("使用统计" to "操作事件与已保存的 AI 分析"),
        "ai_config" to ("AI 配置与密钥" to "API 地址、密钥、模型、提示词及配置方案"),
        "drafts" to ("发帖草稿" to "未发布的标题、正文和特殊帖配置"),
        "link_previews" to ("链接预览缓存" to "网站标题、简介和图标地址"),
        "background_image" to ("自定义背景图片" to "删除应用内保存的背景图片，恢复主题底色"),
    )
    var selected by remember { mutableStateOf(emptySet<String>()) }
    var confirmClear by remember { mutableStateOf(false) }
    var cacheRevision by remember { mutableIntStateOf(0) }

    if (confirmClear) AlertDialog(
        onDismissRequest = { confirmClear = false },
        title = { Text("清理所选数据") },
        text = { Text("确定删除已选择的 ${selected.size} 类本地数据吗？此操作无法撤销。") },
        confirmButton = { TextButton(onClick = {
            confirmClear = false
            viewModel.clear(selected) { result ->
                result.onSuccess {
                    selected = emptySet()
                    cacheRevision++
                }.onFailure { session.showToast(it.message ?: "清理失败") }
            }
        }) { Text("清理") } },
        dismissButton = { TextButton(onClick = { confirmClear = false }) { Text("取消") } },
    )

    Scaffold(topBar = {
        TopAppBar(
            title = { Text("数据管理") },
            navigationIcon = { IconButton(onClick = { nav.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回") } },
        )
    }) { pad ->
        Column(
            Modifier.padding(pad).fillMaxSize().verticalScroll(rememberScrollState()).padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            GroupLabel("空间占用")
            GroupCard {
                Column(Modifier.fillMaxWidth().padding(18.dp)) {
                    CacheAnalysisCard(session, cacheRevision)
                }
            }
            GroupLabel("立即清理")
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                options.forEachIndexed { index, (key, labels) ->
                    val checked = key in selected
                    Surface(
                        onClick = {
                            selected = if (checked) selected - key else selected + key
                        },
                        shape = RoundedCornerShape(16.dp),
                        color = if (checked) MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.72f)
                            else MaterialTheme.colorScheme.surfaceContainerLow,
                        border = BorderStroke(
                            if (checked) 1.5.dp else 0.5.dp,
                            if (checked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                        ),
                    ) {
                        Row(
                            Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Checkbox(checked = checked, onCheckedChange = null)
                            Spacer(Modifier.width(4.dp))
                            Column(Modifier.weight(1f)) {
                                Text(labels.first, style = MaterialTheme.typography.bodyLarge, fontWeight = if (checked) FontWeight.SemiBold else FontWeight.Normal)
                                Text(labels.second, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            if (checked) Icon(Icons.Filled.CheckCircle, "已选择", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
            Button(
                onClick = { confirmClear = true }, enabled = selected.isNotEmpty(),
                modifier = Modifier.fillMaxWidth(),
            ) { Text("清理所选项") }

            GroupCard("退出时自动清理", initiallyExpanded = false, icon = Icons.Filled.Delete) {
                Text(
                    "正常退出时清理所选项目；系统强制结束进程时，会在下次启动补做。所选历史、草稿或 AI 配置会永久删除，请谨慎开启；账号登录不受影响。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
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

/** 本地使用统计：只统计应用内真实完成的行为；AI 分析同时参考收藏快照。 */
