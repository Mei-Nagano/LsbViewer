package sb.linux.client.ui.theme

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

/**
 * 行内提示（`AppNotice`）的专属圆角与内边距，从迁移前实现原样提取
 * （见 UI_DEVELOPMENT_GUIDE 4.1「提示与摘要圆角」）。
 *
 * 与摘要卡片分别持有：两者语义不同，即使当前数值接近也不共用同一个常量。
 */
object AppNoticeDefaults {

    /** 提示条容器圆角（D05 medium） */
    val shape = RoundedCornerShape(12.dp)

    /** 提示条内容内边距：右侧留给操作按钮，故小于左侧 */
    val contentPadding = PaddingValues(start = 14.dp, end = 6.dp, top = 4.dp, bottom = 4.dp)
}
