package sb.linux.client.ui.components.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import sb.linux.client.ui.theme.LocalAppSizes

/**
 * 二级页面外壳：标题、返回、actions、底栏与内容 inset。
 *
 * 约定（UI_DEVELOPMENT_GUIDE 5.2）：
 * - 不接收 `NavHostController`，返回动作由 Route / 适配层以 `onBack` 传入；
 *   `onBack` 传 `null` 表示该页当前不可返回（例如按状态条件显示返回的页面），
 *   此时不渲染返回图标，顶栏标题随之左移；
 * - 需要非返回图标（抽屉菜单、自定义无障碍描述）时用 [navigationIcon] 覆盖；
 * - 标题默认单行省略；需要头像 + 文字等组合标题时用 [titleContent]；
 * - 不强制滚动，内容自己决定普通 `Column` 还是 `LazyColumn` 消费 [PaddingValues]，
 *   避免外壳套滚动导致列表嵌套或恢复位置丢失；
 * - 底栏只给需要常驻输入区的页面（如私信会话）用，普通页面留空；
 * - 顶栏标题、返回图标与无障碍描述由此处统一，页面不再各拼一份。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppPageScaffold(
    title: String,
    onBack: (() -> Unit)?,
    modifier: Modifier = Modifier,
    navigationIcon: (@Composable () -> Unit)? = null,
    titleContent: (@Composable () -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            AppTopBar(
                title = title,
                onBack = onBack,
                navigationIcon = navigationIcon,
                titleContent = titleContent,
                actions = actions,
            )
        },
        bottomBar = bottomBar,
        content = content,
    )
}

/**
 * 全应用统一顶栏：根页面（可内嵌在自有 Scaffold）与二级页面共用。
 *
 * - 高度取 [LocalAppSizes] 的 `topBarHeight`，避免各页 56/64dp 混用；
 * - [onBack] 非空时渲染标准返回；[navigationIcon] 非空时优先渲染自定义图标；
 * - [windowInsets] 供已消费状态栏的嵌套外壳传 `WindowInsets(0, 0, 0, 0)`；
 * - 标题单行省略；组合标题（头像 + 名称）用 [titleContent]。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTopBar(
    title: String,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    navigationIcon: (@Composable () -> Unit)? = null,
    titleContent: (@Composable () -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    windowInsets: WindowInsets = TopAppBarDefaults.windowInsets,
) {
    TopAppBar(
        modifier = modifier.heightIn(min = LocalAppSizes.current.topBarHeight),
        windowInsets = windowInsets,
        title = {
            if (titleContent != null) {
                titleContent()
            } else {
                Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        },
        navigationIcon = {
            when {
                navigationIcon != null -> navigationIcon()
                onBack != null -> IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                }
            }
        },
        actions = actions,
    )
}
