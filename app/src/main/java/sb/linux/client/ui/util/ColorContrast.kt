package sb.linux.client.ui.util

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

/**
 * 按亮度自动选择黑/白前景，保证任意背景色下可读。
 *
 * 纯表现函数：只依赖颜色本身，不持有主题、页面状态或数据来源
 * （UI_DEVELOPMENT_GUIDE 7.1「主题色与源站颜色转换」）。
 */
fun onColorFor(bg: Color): Color =
    if (bg.luminance() > 0.5f) Color.Black else Color.White
