package sb.linux.client.ui

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.material3.LocalTonalElevationEnabled
import androidx.compose.runtime.remember
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
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
