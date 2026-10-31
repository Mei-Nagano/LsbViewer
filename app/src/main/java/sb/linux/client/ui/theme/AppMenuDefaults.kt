package sb.linux.client.ui.theme

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

/**
 * 分组与菜单行的专属视觉值，从迁移前的 `AppGroupLabel` / `AppSection` / `AppMenuRow`
 * 原样提取（见 UI_DEVELOPMENT_GUIDE 4.1「菜单圆角与行内边距」）。
 *
 * 放在这里而不是各页面：同一语义的圆角、行内边距和禁用透明度只有一套来源。
 */
object AppMenuDefaults {

    /** 分组卡片容器圆角（D05 large：大区块与设置分组） */
    val cardShape = RoundedCornerShape(16.dp)

    /** 分组标签内边距 */
    val labelPadding = PaddingValues(start = 12.dp, top = 10.dp, bottom = 2.dp)

    /** 常规菜单行内边距 */
    val rowPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)

    /**
     * 分组卡片内的说明文字、滑条、分隔线左右内边距。
     *
     * 必须与 [rowPadding] 的左右值一致，否则说明文字会和同一张卡片里的
     * 开关行/菜单行错开（历史上各页曾用 18/14/10dp）。页面不要再单独写字面值。
     */
    val contentHorizontal = 16.dp

    /** 紧凑菜单行内边距 */
    val compactRowPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)

    /** 行首图标与文本之间 */
    val rowSpacing = 12.dp

    /** 单行设置行最小高度（D05；文字可继续撑高，不固定裁切） */
    val singleLineMinHeight = 56.dp

    /** 带说明行的最小高度（双行 / 三行按真实文字继续增长） */
    val twoLineMinHeight = 72.dp

    /** 标题与说明之间 */
    val titleSubtitleSpacing = 3.dp

    /** 禁用时的前景透明度；视觉与操作同时失效由调用方传入 enabled 保证 */
    const val disabledAlpha = 0.38f
}
