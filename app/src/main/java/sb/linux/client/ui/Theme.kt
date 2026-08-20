package sb.linux.client.ui

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.materialkolor.PaletteStyle

enum class ThemeMode { SYSTEM, LIGHT, DARK }

/** 可选主题色预设（key 持久化存储） */
data class ThemeColorPreset(
    val key: String,
    val label: String,
    val lightPrimary: Color,
    val lightPrimaryContainer: Color,
    val lightOnPrimaryContainer: Color,
    val darkPrimary: Color,
    val darkPrimaryContainer: Color,
    val darkOnPrimaryContainer: Color,
)

private val ThemeColorPresets_Default = ThemeColorPreset(
    "default", "默认", Color(0xFF516185), Color(0xFFDDE1FF), Color(0xFF101433),
    Color(0xFFBAC5FF), Color(0xFF3A4778), Color(0xFF1B2A5E),
)

val ThemeColorPresets = listOf(
    ThemeColorPresets_Default,
    ThemeColorPreset("blue", "海蓝", Color(0xFF1E6FD9), Color(0xFFD6E3FF), Color(0xFF001B3F), Color(0xFFA8C8FF), Color(0xFF00497F), Color(0xFF00315B)),
    ThemeColorPreset("green", "森绿", Color(0xFF2E7D46), Color(0xFFBDF0C5), Color(0xFF00210B), Color(0xFF96D7A4), Color(0xFF1D5E31), Color(0xFF0B3A1C)),
    ThemeColorPreset("teal", "青碧", Color(0xFF00787A), Color(0xFFA0F0F0), Color(0xFF002020), Color(0xFF4DD9DA), Color(0xFF00696B), Color(0xFF003738)),
    ThemeColorPreset("purple", "黛紫", Color(0xFF6D5780), Color(0xFFECDCFF), Color(0xFF271439), Color(0xFFD3BCE8), Color(0xFF563E6B), Color(0xFF3F2952)),
    ThemeColorPreset("orange", "暖橙", Color(0xFF9A4B00), Color(0xFFFFDCC2), Color(0xFF3A1A00), Color(0xFFFFB77E), Color(0xFF824C00), Color(0xFF5A3500)),
    ThemeColorPreset("pink", "樱粉", Color(0xFFA6315F), Color(0xFFFFD9E4), Color(0xFF3F001C), Color(0xFFFFB1C6), Color(0xFF8E2E55), Color(0xFF6A1F3F)),
    ThemeColorPreset("red", "朱红", Color(0xFFB3261E), Color(0xFFFFDAD5), Color(0xFF410001), Color(0xFFFFB4A9), Color(0xFF93020A), Color(0xFF6D0E12)),
)

fun themeColorByKey(key: String): ThemeColorPreset =
    ThemeColorPresets.firstOrNull { it.key == key } ?: ThemeColorPresets_Default

/** 自定义主题色 key 格式：custom#RRGGBB（存持久化的 theme_color 字段） */
private const val CUSTOM_COLOR_PREFIX = "custom#"

fun isCustomColorKey(key: String): Boolean = key.startsWith(CUSTOM_COLOR_PREFIX)

/** 由 theme_color key 解析出种子色：预设取 lightPrimary，custom#RRGGBB 直接解析 */
fun colorSeed(key: String): Color {
    if (isCustomColorKey(key)) {
        val hex = key.removePrefix(CUSTOM_COLOR_PREFIX)
        val rgb = hex.toLongOrNull(16)
        if (rgb != null && rgb in 0..0xFFFFFFL) return Color(0xFF000000L or rgb)
    }
    return themeColorByKey(key).lightPrimary
}

/** 把种子色编码为持久化 key（如 color → custom#RRGGBB） */
fun colorToKey(color: Color): String {
    val rgb = (color.toArgb().toLong() and 0xFFFFFFL)
    return CUSTOM_COLOR_PREFIX + rgb.toString(16).padStart(6, '0')
}

/** Color → HSV（h 度 0..360，s/v 0..1）。避免依赖 Compose 的 HSV 扩展属性。 */
fun colorToHsv(c: Color): FloatArray {
    val out = FloatArray(3)
    android.graphics.Color.colorToHSV(c.toArgb(), out)
    return out
}

/** HSV → Color（h 度 0..360，s/v 0..1） */
fun hsvColor(h: Float, s: Float, v: Float): Color {
    val argb = android.graphics.Color.HSVToColor(
        floatArrayOf(h.coerceIn(0f, 360f), s.coerceIn(0f, 1f), v.coerceIn(0f, 1f))
    )
    return Color(argb.toLong() and 0xFFFFFFFFL)
}

/** 支持的所有调色风格（对应 ColorBlendr / material-color-utilities 的变体） */
val PaletteStyles: List<PaletteStyle> = PaletteStyle.entries

/** 调色风格的中文展示名 */
fun paletteStyleLabel(style: PaletteStyle): String = when (style) {
    PaletteStyle.TonalSpot -> "色调定位"
    PaletteStyle.Vibrant -> "鲜艳"
    PaletteStyle.Expressive -> "表现"
    PaletteStyle.Neutral -> "中性"
    PaletteStyle.Monochrome -> "单色"
    PaletteStyle.Rainbow -> "彩虹"
    PaletteStyle.FruitSalad -> "水果沙拉"
    PaletteStyle.Fidelity -> "高还原"
    PaletteStyle.Content -> "内容"
}

/** 全局形状：比默认更圆润的 MD3 风格 */
private val LsbShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

@Composable
fun LsbTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    dynamicColor: Boolean = true,
    themeColorKey: String = "default",
    pureDark: Boolean = false,
    style: PaletteStyle = PaletteStyle.TonalSpot,
    contrastLevel: Double = 0.0,
    primary: Color? = null,
    secondary: Color? = null,
    tertiary: Color? = null,
    content: @Composable () -> Unit,
) {
    val darkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val context = LocalContext.current
    // 动态取色仅对系统色（"default"）生效；选任何主题色后走种子色派生方案
    val useDynamic = dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && themeColorKey == "default"
    val colorScheme = if (useDynamic) {
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    } else {
        // ColorBlendr 同款 HCT 主题引擎：从种子色按调色风格/对比度生成完整 MD3 三色树，
        // 支持 primary/secondary/tertiary 元素级覆盖。覆盖任意角色时这些颜色接管对应角色，
        // 未覆盖的角色仍由种子色派生（primary 被覆盖后作为新的种子色）。
        com.materialkolor.rememberDynamicColorScheme(
            seedColor = colorSeed(themeColorKey),
            isDark = darkTheme,
            isAmoled = pureDark,
            primary = primary,
            secondary = secondary,
            tertiary = tertiary,
            style = style,
            contrastLevel = contrastLevel,
        )
    }
    // 调色风格算法覆盖预览可调色的全部元素（3.12）：
    // 中性色（卡片背景 surfaceContainerLow / 时间与评论数 onSurfaceVariant 等）向主题色轻微偏移，
    // 偏移强度随各风格主色饱和度联动——切风格时所有元素同步变化；
    // 单色/中性风格下主色近灰，偏移自然趋近于 0，不破坏 MD3 层级。
    val styledScheme = if (!useDynamic) colorScheme.copy(
        background = androidx.compose.ui.graphics.lerp(colorScheme.background, colorScheme.primary, 0.05f),
        surface = androidx.compose.ui.graphics.lerp(colorScheme.surface, colorScheme.primary, 0.05f),
        surfaceContainerLowest = androidx.compose.ui.graphics.lerp(colorScheme.surfaceContainerLowest, colorScheme.primary, 0.04f),
        surfaceContainerLow = androidx.compose.ui.graphics.lerp(colorScheme.surfaceContainerLow, colorScheme.primary, 0.05f),
        surfaceContainer = androidx.compose.ui.graphics.lerp(colorScheme.surfaceContainer, colorScheme.primary, 0.06f),
        surfaceContainerHigh = androidx.compose.ui.graphics.lerp(colorScheme.surfaceContainerHigh, colorScheme.primary, 0.07f),
        surfaceContainerHighest = androidx.compose.ui.graphics.lerp(colorScheme.surfaceContainerHighest, colorScheme.primary, 0.08f),
        onSurfaceVariant = androidx.compose.ui.graphics.lerp(colorScheme.onSurfaceVariant, colorScheme.primary, 0.10f),
        outlineVariant = androidx.compose.ui.graphics.lerp(colorScheme.outlineVariant, colorScheme.primary, 0.06f),
    ) else colorScheme
    // OLED 纯黑模式：深色下把所有表面压到纯黑/近黑，省电且对比更强
    val finalScheme = if (darkTheme && pureDark) styledScheme.copy(
        background = Color.Black,
        surface = Color.Black,
        surfaceVariant = Color(0xFF121212),
        surfaceDim = Color.Black,
        surfaceBright = Color(0xFF1A1A1A),
        surfaceContainerLowest = Color.Black,
        surfaceContainerLow = Color(0xFF0A0A0A),
        surfaceContainer = Color(0xFF111111),
        surfaceContainerHigh = Color(0xFF161616),
        surfaceContainerHighest = Color(0xFF1C1C1C),
        outlineVariant = Color(0xFF262626),
    ) else colorScheme
    MaterialTheme(
        colorScheme = finalScheme,
        shapes = LsbShapes,
        content = content
    )
}
