package sb.linux.client.ui

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color

/** 固定无色相的 MD3 表面层级；强调色与警告色保留，换主题不会把整个页面染色。 */
internal fun neutralPageColors(source: ColorScheme, dark: Boolean): ColorScheme {
    fun gray(light: Long, night: Long) = Color(if (dark) night else light)
    val foreground = gray(0xFF1B1B1B, 0xFFE2E2E2)
    return source.copy(
        background = gray(0xFFFAFAFA, 0xFF121212),
        onBackground = foreground,
        surface = gray(0xFFFAFAFA, 0xFF121212),
        onSurface = foreground,
        surfaceVariant = gray(0xFFE2E2E2, 0xFF444444),
        onSurfaceVariant = gray(0xFF474747, 0xFFC6C6C6),
        surfaceDim = gray(0xFFDADADA, 0xFF121212),
        surfaceBright = gray(0xFFFAFAFA, 0xFF383838),
        surfaceContainerLowest = gray(0xFFFFFFFF, 0xFF0D0D0D),
        surfaceContainerLow = gray(0xFFF4F4F4, 0xFF1B1B1B),
        surfaceContainer = gray(0xFFEEEEEE, 0xFF202020),
        surfaceContainerHigh = gray(0xFFE8E8E8, 0xFF2A2A2A),
        surfaceContainerHighest = gray(0xFFE2E2E2, 0xFF353535),
        outline = gray(0xFF777777, 0xFF919191),
        outlineVariant = gray(0xFFC6C6C6, 0xFF474747),
        inverseSurface = gray(0xFF303030, 0xFFE2E2E2),
        inverseOnSurface = gray(0xFFF1F1F1, 0xFF303030),
        // primary/secondary/tertiary 的 Container 也是强调色：按钮、选中项、标签与
        // 搜索高亮都依赖它们，不得灰化。大面积普通面板应使用上方 surface 系列。
        // surfaceTint 保留原值；页面不叠色由 LocalTonalElevationEnabled 单独控制。
    )
}
