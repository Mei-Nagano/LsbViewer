package sb.linux.client.ui.theme

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

/**
 * 页面级状态占位（`LoadingBox` / `ErrorBox` / `EmptyBox` / `LoginRequiredBox`）的
 * 专属版式值，从迁移前实现原样提取（见 UI_DEVELOPMENT_GUIDE 5.1「首次加载/失败/空态」）。
 *
 * 三种占位共用同一套图标容器与间距，仅颜色语义与附加动作不同。
 */
object AppStateDefaults {

    /** 全屏状态占位的外边距 */
    val pagePadding = PaddingValues(24.dp)

    /** 图标圆形容器直径 */
    val glyphContainerSize = 56.dp

    /** 圆形容器内图标尺寸 */
    val glyphSize = 26.dp

    /** 圆形容器形状（50% 圆角，等价于圆形） */
    val glyphShape = RoundedCornerShape(50)

    /** 圆形底色的透明度 */
    const val glyphBackgroundAlpha = 0.6f

    /** 失败态：图标容器与文案之间 */
    val errorSpacing = 12.dp

    /** 空态 / 登录引导：图标容器与文案之间 */
    val infoSpacing = 10.dp

    /** 附加按钮胶囊形状 */
    val actionShape = RoundedCornerShape(50)
}
