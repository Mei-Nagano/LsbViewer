package sb.linux.client.ui.screens
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import sb.linux.client.ui.session.Session

import androidx.compose.runtime.saveable.rememberSaveable
import sb.linux.client.ui.navigation.LocalMasterNav
import sb.linux.client.ui.feature.settings.SettingsSearchIndex
import sb.linux.client.ui.theme.LocalAppSpacing
import sb.linux.client.ui.patterns.settings.settingsItemShape

@Composable
fun AppSettingsScreen(session: Session, nav: NavHostController) {
    var query by rememberSaveable { mutableStateOf("") }
    val master = LocalMasterNav.current
    val spacing = LocalAppSpacing.current
    SettingsPage("设置", nav, onBack = { (master ?: nav).popBackStack() }) {
        SettingsTextField(
            value = query, onValueChange = { query = it },
            placeholder = { Text("搜索设置") }, singleLine = true,
            leadingIcon = { Icon(Icons.Filled.Search, null) },
            shape = MaterialTheme.shapes.extraLarge, modifier = Modifier.fillMaxWidth(),
        )
        if (query.isBlank()) {
            Column(verticalArrangement = Arrangement.spacedBy(spacing.inline)) {
                SettingsCategories.forEachIndexed { index, (title, summary, route) ->
                    SettingMenuRow(title, summary, shape = settingsItemShape(index, SettingsCategories.size)) { nav.navigate(route) }
                }
            }
        } else {
            val results = SettingsSearchIndex.search(query)
            if (results.isEmpty()) Text(
                "没有找到相关设置",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = spacing.item),
            )
            Column(verticalArrangement = Arrangement.spacedBy(spacing.inline)) {
                results.forEachIndexed { index, entry ->
                    SettingMenuRow(entry.title, entry.category, shape = settingsItemShape(index, results.size)) {
                        sb.linux.client.ui.feature.settings.openSettingsSearchResult(nav, entry)
                    }
                }
            }
        }
    }
}

internal val SettingsCategories = listOf(
    Triple("阅读与交互", "滚动与分页 · 帖子显示 · 评论交互 · 在线用户", "browseSettings"),
    Triple("外观与布局", "主题与壁纸 · 字体与缩放 · 导航与宽屏布局", "themeSettings"),
    Triple("发帖与图片上传", "源站附件 · 内置图床 · 自定义图床", "uploadSettings"),
    Triple("网络与链接", "自动代理 · DoH · 链接与预览", "networkSettings"),
    Triple("AI 助手", "自动总结 · 接口与模型 · 配置方案 · 总结历史", "aiSettings"),
    Triple("存储与备份", "缓存占用 · 数据清理 · 本地与云端备份", "storageSettings"),
)
