package sb.linux.client.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 命名间距（DESIGN_SYSTEM D05「间距角色」）。
 *
 * 以 8dp 为主要节奏，4/12dp 服务微间距；1dp 分隔线、图标描边不机械改成 8dp。
 * 页面左右边距由外壳按窗口统一决定：窄窗 16dp、宽窗 24dp、极窄（<360dp）12dp。
 */
@Immutable
data class AppSpacing(
    /** 标签内部、紧邻图文 */
    val inline: Dp = 4.dp,
    /** 同组元素、控件与辅助信息 */
    val item: Dp = 8.dp,
    /** 头像与标题列等更松的同组间距 */
    val contentGap: Dp = 12.dp,
    /** 页面左右内边距（窄窗） */
    val pageHorizontal: Dp = 16.dp,
    /** 页面左右内边距（宽窗／双栏 pane 内） */
    val pageHorizontalWide: Dp = 24.dp,
    /** 页面上下内边距 */
    val pageVertical: Dp = 16.dp,
    /** 独立分组之间 */
    val section: Dp = 24.dp,
    /** 正文段落之间（阅读专用，D04「段后 16dp」） */
    val paragraph: Dp = 16.dp,
    /** 双栏之间 */
    val paneGap: Dp = 24.dp,
    /** 独立内容段／资料头与列表之间 */
    val largeSection: Dp = 32.dp,
)

/**
 * 由 [LsbTheme] 在应用根部注入；预览与独立组合回退到同一组设计默认值，
 * 不要求连接真实账号或宿主。
 */
val LocalAppSpacing = staticCompositionLocalOf { AppSpacing() }
