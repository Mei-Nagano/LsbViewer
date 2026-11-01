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

import androidx.compose.runtime.saveable.rememberSaveable
import sb.linux.client.ui.navigation.LocalMasterNav
import sb.linux.client.ui.feature.settings.SettingsSearchIndex
import sb.linux.client.ui.theme.LocalAppSpacing

@Composable
fun AppSettingsScreen(session: Session, nav: NavHostController) {
    var query by rememberSaveable { mutableStateOf("") }
    val master = LocalMasterNav.current
    SettingsPage("设置", nav, onBack = { (master ?: nav).popBackStack() }) {
        OutlinedTextField(value = query, onValueChange = { query = it }, singleLine = true,
            placeholder = { Text("搜索设置") }, modifier = Modifier.fillMaxWidth())
        if (query.isBlank()) {
            SettingsCategories.forEach { (title, summary, route) ->
                SettingMenuRow(title, summary) { nav.navigate(route) }
            }
            HorizontalDivider()
            SettingMenuRow("我的内容", "浏览历史 · 收藏 · 总结 · 兑换记录 · 导出 · 统计") { nav.navigate("myContent") }
        } else {
            val results = SettingsSearchIndex.search(query)
            if (results.isEmpty()) Text(
                "没有找到相关设置",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = LocalAppSpacing.current.item),
            )
            results.forEach { SettingMenuRow(it.title, it.category) { nav.navigate(it.route) } }
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
    Triple("应用与账号", "版本更新 · 个人资料 · 重置设置", "generalSettings"),
)
