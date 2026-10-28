package sb.linux.client.ui.components.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import sb.linux.client.ui.theme.LocalAppSizes

/**
 * 二级页面外壳：标题、返回、actions、底栏／悬浮按钮与内容 inset。
 *
 * 约定（UI_DEVELOPMENT_GUIDE 5.2）：
 * - 不接收 `NavHostController`，返回动作由 Route / 适配层以 `onBack` 传入；
 *   `onBack` 传 `null` 表示该页当前不可返回（例如按状态条件显示返回的页面），
 *   此时不渲染返回图标，顶栏标题随之左移；
 * - 不强制滚动，内容自己决定普通 `Column` 还是 `LazyColumn` 消费 [PaddingValues]，
 *   避免外壳套滚动导致列表嵌套或恢复位置丢失；
 * - 顶栏标题、返回图标与无障碍描述由此处统一，页面不再各拼一份；
 * - 标题按顶栏惯例单行省略：长标题（用户名、专辑名）不再各自传 maxLines/overflow。
 *
 * 不提供底栏、悬浮按钮或自定义表面色：需要这些形态的页面（私信会话、帖子页、
 * 用户主页回到顶部按钮）各自持有，等确有多个调用方共用时再统一，避免先加参数后找用途。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppPageScaffold(
    title: String,
    onBack: (() -> Unit)?,
    modifier: Modifier = Modifier,
    actions: @Composable RowScope.() -> Unit = {},
    content: @Composable (PaddingValues) -> Unit,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                modifier = Modifier.heightIn(min = LocalAppSizes.current.topBarHeight),
                title = { Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    if (onBack != null) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                        }
                    }
                },
                actions = actions,
            )
        },
        content = content,
    )
}
