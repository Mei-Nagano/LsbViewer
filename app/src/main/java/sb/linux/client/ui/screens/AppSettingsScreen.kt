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
    SettingsPage("设置", nav, onBack = { (master ?: nav).popBackStack() }, showTopBar = false) {
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
                    SettingMenuRow(entry.title, entry.category, shape = settingsItemShape(index, results.size)) { nav.navigate(entry.route) }
                }
            }
        }
    }
}

internal val SettingsCategories = listOf(
    Triple("阅读与交互", "列表加载 · 正文 · 评论 · 侧边栏 · 屏蔽规则", "browseSettings"),
    Triple("外观与布局", "主题配色 · 页面背景 · 字体 · 主导航与单双栏", "themeSettings"),
    Triple("发帖与图片上传", "内置图床 · 自定义上传接口", "uploadSettings"),
    Triple("网络与链接", "应用代理 · 加密 DNS · 链接打开与预览", "networkSettings"),
    Triple("AI 助手", "总结行为 · 接口 · 模型 · 配置方案", "aiSettings"),
    Triple("存储与备份", "空间占用 · 清理 · 本地备份 · WebDAV", "storageSettings"),
)
