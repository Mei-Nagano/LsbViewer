package sb.linux.client.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * 应用形状（DESIGN_SYSTEM D05「形状与层次」）。
 *
 * 普通页面与列表保持平面（none），常驻容器用 small/medium，面板 large，
 * 模态 extraLarge；按钮、导航指示器、头像使用组件自身的 full 形状，
 * 不因 full 存在就把 56dp FAB 改成圆形。
 */
internal val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(28.dp),
)
