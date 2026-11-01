package sb.linux.client.ui.theme

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.unit.dp

/**
 * 分页控件（`PaginationBar`）的专属尺寸，从迁移前实现原样提取
 * （见 UI_DEVELOPMENT_GUIDE 4.1「分页尺寸」）。
 *
 * 胶囊高度与可点区域同为 [height]；按设计系统 D05「交互触区至少 48×48dp」取 48dp，
 * 两端箭头不再借用小于 48dp 的点击区域。
 */
object AppPaginationDefaults {

    /** 胶囊高度，同时是两端箭头的可点区域（≥48dp 触区） */
    val height = 48.dp

    /** 箭头图标尺寸 */
    val iconSize = 20.dp

    /** 胶囊形状（50% 圆角，等价于圆形） */
    val shape = CircleShape

    /** 胶囊容器上下外边距 */
    val containerPadding = PaddingValues(vertical = 10.dp)

    /** 当前页码左内边距 */
    val pageLabelPadding = PaddingValues(start = 6.dp)

    /** 总页数右内边距 */
    val totalLabelPadding = PaddingValues(end = 6.dp)

    /** 到边界时箭头透明度 */
    const val disabledAlpha = 0.3f
}
