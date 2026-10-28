package sb.linux.client.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * 应用字阶（DESIGN_SYSTEM D04）。
 *
 * 数值是本项目的应用表，不是「不可调整的 MD3 原始字表」：
 * - 默认系统 sans-serif 与中文 fallback，不额外下载字体；
 * - 中文为主的整段文字 tracking=0；
 * - 用户字体、应用字号因子与字重增量在此处各应用一次，系统 sp 缩放继续交给 Compose。
 */
private fun baseTypography(): Typography = Typography(
    displayLarge = TextStyle(fontSize = 57.sp, lineHeight = 64.sp, fontWeight = FontWeight.Normal),
    displayMedium = TextStyle(fontSize = 45.sp, lineHeight = 52.sp, fontWeight = FontWeight.Normal),
    displaySmall = TextStyle(fontSize = 36.sp, lineHeight = 44.sp, fontWeight = FontWeight.Normal),
    headlineLarge = TextStyle(fontSize = 32.sp, lineHeight = 40.sp, fontWeight = FontWeight.Normal),
    headlineMedium = TextStyle(fontSize = 28.sp, lineHeight = 36.sp, fontWeight = FontWeight.Normal),
    headlineSmall = TextStyle(fontSize = 24.sp, lineHeight = 32.sp, fontWeight = FontWeight.Normal),
    titleLarge = TextStyle(fontSize = 22.sp, lineHeight = 28.sp, fontWeight = FontWeight.Normal),
    titleMedium = TextStyle(fontSize = 16.sp, lineHeight = 24.sp, fontWeight = FontWeight.Medium),
    titleSmall = TextStyle(fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.Medium),
    bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 24.sp, fontWeight = FontWeight.Normal),
    bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.Normal),
    bodySmall = TextStyle(fontSize = 12.sp, lineHeight = 18.sp, fontWeight = FontWeight.Normal),
    labelLarge = TextStyle(fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.Medium),
    labelMedium = TextStyle(fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.Medium),
    labelSmall = TextStyle(fontSize = 11.sp, lineHeight = 16.sp, fontWeight = FontWeight.Medium),
)

/** 默认全文字重不额外 +100；只有用户显式选择字重档位时才叠加。 */
private fun TextStyle.styled(family: FontFamily?, scale: Float, weightDelta: Int) = copy(
    fontFamily = family ?: fontFamily,
    fontSize = fontSize * scale,
    lineHeight = lineHeight * scale,
    letterSpacing = 0.sp,
    fontWeight = FontWeight(((fontWeight ?: FontWeight.Normal).weight + weightDelta).coerceAtMost(900)),
)

internal fun appTypography(fontFamily: FontFamily?, fontScale: Float, weightLevel: Int): Typography {
    val t = baseTypography()
    val scale = fontScale.coerceIn(0.85f, 1.30f)
    val delta = weightLevel.coerceIn(0, 2) * 100
    fun TextStyle.adjusted() = styled(fontFamily, scale, delta)
    return t.copy(
        displayLarge = t.displayLarge.adjusted(), displayMedium = t.displayMedium.adjusted(),
        displaySmall = t.displaySmall.adjusted(), headlineLarge = t.headlineLarge.adjusted(),
        headlineMedium = t.headlineMedium.adjusted(), headlineSmall = t.headlineSmall.adjusted(),
        titleLarge = t.titleLarge.adjusted(), titleMedium = t.titleMedium.adjusted(),
        titleSmall = t.titleSmall.adjusted(), bodyLarge = t.bodyLarge.adjusted(),
        bodyMedium = t.bodyMedium.adjusted(), bodySmall = t.bodySmall.adjusted(),
        labelLarge = t.labelLarge.adjusted(), labelMedium = t.labelMedium.adjusted(),
        labelSmall = t.labelSmall.adjusted(),
    )
}

/**
 * 阅读专用字阶（DESIGN_SYSTEM D04「阅读专用角色」）。
 *
 * 与设置页的 bodyLarge 分开：正文 18/30，评论 16/26，代码等宽 14/22；
 * 应用字号因子只在这里应用一次，页面不得再乘一次。
 */
@Immutable
data class AppReadingType(
    val body: TextStyle,
    val quote: TextStyle,
    val code: TextStyle,
    val heading1: TextStyle,
    val heading2: TextStyle,
    val heading3: TextStyle,
    val comment: TextStyle,
)

internal fun appReadingType(fontFamily: FontFamily?, fontScale: Float, weightLevel: Int): AppReadingType {
    val scale = fontScale.coerceIn(0.85f, 1.30f)
    val delta = weightLevel.coerceIn(0, 2) * 100
    fun reading(size: Int, line: Int, weight: FontWeight, family: FontFamily? = fontFamily) = TextStyle(
        fontFamily = family,
        fontSize = (size * scale).sp,
        lineHeight = (line * scale).sp,
        letterSpacing = 0.sp,
        fontWeight = FontWeight((weight.weight + delta).coerceAtMost(900)),
    )
    return AppReadingType(
        body = reading(18, 30, FontWeight.Normal),
        quote = reading(16, 26, FontWeight.Normal),
        code = reading(14, 22, FontWeight.Normal, FontFamily.Monospace),
        heading1 = reading(28, 36, FontWeight.Medium),
        heading2 = reading(24, 32, FontWeight.Medium),
        heading3 = reading(20, 28, FontWeight.Medium),
        comment = reading(16, 26, FontWeight.Normal),
    )
}

/** 无宿主时的回退（预览、独立组合），保持默认字号与系统字体。 */
val LocalReadingType = staticCompositionLocalOf { appReadingType(null, 1f, 0) }
