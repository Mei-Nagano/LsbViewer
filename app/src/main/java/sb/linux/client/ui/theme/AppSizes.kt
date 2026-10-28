package sb.linux.client.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 命名控件尺寸（DESIGN_SYSTEM D05「尺寸角色」）。
 *
 * 头像三档、标准图标与触区从这里取；高度表里的最小值允许文字撑高，
 * 页面不得用固定 height 裁掉大字号说明或关键按钮文字。
 */
@Immutable
data class AppSizes(
    /** 标准图标（含菜单行行首图标） */
    val icon: Dp = 24.dp,
    /** 独立操作的最小触区 */
    val touchTarget: Dp = 48.dp,
    /** 普通顶栏内容高度（status bar inset 另算） */
    val topBarHeight: Dp = 64.dp,
    /** 根底栏内容高度（系统底部 inset 另算） */
    val navigationBarHeight: Dp = 80.dp,
    /** 主导航 rail 宽度 */
    val railWidth: Dp = 80.dp,
    /** 社区发帖 FAB */
    val fabSize: Dp = 56.dp,
    /** 帖子/会话列表头像 */
    val avatarList: Dp = 40.dp,
    /** 评论头像 */
    val avatarReply: Dp = 32.dp,
    /** 资料头像 */
    val avatarProfile: Dp = 64.dp,
    /** 正文阅读列最大宽度 */
    val readingMaxWidth: Dp = 720.dp,
    /** 普通表单最大宽度 */
    val formMaxWidth: Dp = 640.dp,
    /** 对话框最大宽度 */
    val dialogMaxWidth: Dp = 560.dp,
    /** 回复工具区最小高度（含换行按实际占位） */
    val replyBarMinHeight: Dp = 64.dp,
)

/** 由 [LsbTheme] 在应用根部注入。 */
val LocalAppSizes = staticCompositionLocalOf { AppSizes() }
