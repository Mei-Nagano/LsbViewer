package sb.linux.client.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.platform.LocalConfiguration

/** 官方窗口宽度断点 600 / 840 / 1200 / 1600dp（DESIGN_SYSTEM §5.1）。 */
internal enum class AppWidthClass { Compact, Medium, Expanded, Large, ExtraLarge }

/**
 * 应用外壳的窗口策略：导航形式与是否双栏都由实际窗口尺寸推导。
 *
 * 双栏预算 `U = 窗口宽度 − 导航宽度 − 两侧页面边距`；本项目定值：
 * 列表最小 280dp、栏间距 24dp、详情最小 400dp，因此 `U ≥ 704dp` 才有双栏资格。
 * 用户在设置里选择「始终单内容栏」时只限制内容布局，不影响 bar/rail 的切换。
 */
@Immutable
internal data class AppWindowLayout(
    val widthDp: Int,
    val heightDp: Int,
    val widthClass: AppWidthClass,
    /** 宽 ≥600dp 且高 ≥480dp 时使用 rail；否则使用底部导航。 */
    val useRail: Boolean,
    /** 满足内容预算且用户未限制单栏时使用列表—详情双栏。 */
    val useTwoPane: Boolean,
    /** 双栏时列表栏宽度；单栏为 0。 */
    val masterPaneWidthDp: Int,
)

private const val RAIL_WIDTH_DP = 80
private const val TOP_TABS_HEIGHT_DP = 64
private const val LIST_MIN_DP = 280
private const val LIST_MAX_DP = 360
private const val DETAIL_MIN_DP = 400
private const val PANE_GAP_DP = 24
private const val PAGE_MARGIN_DP = 24
private const val LOW_HEIGHT_DP = 480
private const val TWO_PANE_MIN_DP = LIST_MIN_DP + PANE_GAP_DP + DETAIL_MIN_DP

@Composable
internal fun rememberAppWindowLayout(
    singleContentColumn: Boolean,
    twoItemLayout: Boolean = false,
): AppWindowLayout {
    val configuration = LocalConfiguration.current
    val width = configuration.screenWidthDp
    val height = configuration.screenHeightDp
    val widthClass = when {
        width < 600 -> AppWidthClass.Compact
        width < 840 -> AppWidthClass.Medium
        width < 1200 -> AppWidthClass.Expanded
        width < 1600 -> AppWidthClass.Large
        else -> AppWidthClass.ExtraLarge
    }
    val useRail = width >= 600 && height >= LOW_HEIGHT_DP
    // 两项兼容布局没有 rail，但顶部 Tab 占用高度；两者都从内容预算里扣除。
    val horizontalBudget = if (useRail && !twoItemLayout) RAIL_WIDTH_DP else 0
    val verticalBudget = if (twoItemLayout) TOP_TABS_HEIGHT_DP else 0
    val usableWidth = width - horizontalBudget - PAGE_MARGIN_DP * 2
    val usableHeight = height - verticalBudget
    val useTwoPane = !singleContentColumn && useRail && usableHeight >= LOW_HEIGHT_DP &&
        usableWidth >= TWO_PANE_MIN_DP
    val masterWidth = if (useTwoPane) {
        (usableWidth - PANE_GAP_DP - DETAIL_MIN_DP).coerceIn(LIST_MIN_DP, LIST_MAX_DP)
    } else 0
    return AppWindowLayout(
        widthDp = width,
        heightDp = height,
        widthClass = widthClass,
        useRail = useRail,
        useTwoPane = useTwoPane,
        masterPaneWidthDp = masterWidth,
    )
}
