package sb.linux.client.ui.theme

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

/**
 * 内容摘要卡片（`ReplySummaryCard` 及后续帖子/通知适配）的专属视觉值，
 * 从迁移前实现原样提取（见 UI_DEVELOPMENT_GUIDE 4.1「提示与摘要圆角」）。
 *
 * 完整帖子正文与私信气泡不属于本摘要语义，保持各自布局。
 */
object AppSummaryDefaults {

    /** 摘要卡片圆角（D05 medium） */
    val cardShape = RoundedCornerShape(12.dp)

    /** 卡片之间的外边距 */
    val itemPadding = PaddingValues(horizontal = 14.dp, vertical = 5.dp)

    /** 卡片内容内边距 */
    val contentPadding = PaddingValues(14.dp)

    /** 标题 / 正文 / 时间之间；标题行内的 8dp 间距走 `AppSpacing.item`，不在这里另立一份 */
    val contentSpacing = 6.dp
}
