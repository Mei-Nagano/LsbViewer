package sb.linux.client.ui.theme

import android.os.Build
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.LocalTonalElevationEnabled
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Density
import com.materialkolor.PaletteStyle
import com.materialkolor.rememberDynamicColorScheme

/** 全局表面不透明度（背景图模式下由设置决定，其余为 1f）。 */
val LocalSurfaceOpacity = androidx.compose.runtime.staticCompositionLocalOf { 1f }

/**
 * 主题入口：颜色、字阶、形状与产品角色只在根部解析一次。
 *
 * 解析顺序（DESIGN_SYSTEM D02）：
 * 1. 用户主题模式决定浅/深；
 * 2. API 31+ 且用户选系统动态色时取系统完整 scheme；否则默认配色走冻结静态表，
 *    自定义种子/角色覆盖走 MaterialKolor 生成完整 scheme；
 * 3. 应用中性表面策略（旧「禁止页面染色」偏好）与 OLED；
 * 4. 背景图作为独立层组合，不改写所有颜色角色的 alpha。
 */
@Composable
fun LsbTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    dynamicColor: Boolean = true,
    themeColorKey: String = DEFAULT_THEME_KEY,
    pureDark: Boolean = false,
    style: PaletteStyle = PaletteStyle.TonalSpot,
    contrastLevel: Double = 0.0,
    primary: Color? = null,
    secondary: Color? = null,
    tertiary: Color? = null,
    customBackground: Color? = null,
    keepBackgroundColor: Boolean = false,
    backgroundImage: String = "",
    backgroundImageOpacity: Float = 0.25f,
    surfaceOpacity: Float = 1f,
    fontFamily: FontFamily? = null,
    fontScale: Float = 1f,
    fontWeightLevel: Int = 0,
    content: @Composable () -> Unit,
) {
    val darkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val context = LocalContext.current
    val roleOverridden = primary != null || secondary != null || tertiary != null
    // 动态取色只服务「默认」配色且未被角色覆盖；选了具体种子后走种子色派生方案。
    val useDynamic = dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
        themeColorKey == DEFAULT_THEME_KEY && !roleOverridden
    // 默认配色的「纯默认」分支使用冻结静态表；改动风格/对比度或覆盖角色时仍由引擎生成。
    val useFrozenDefault = themeColorKey == DEFAULT_THEME_KEY && !roleOverridden &&
        style == PaletteStyle.TonalSpot && contrastLevel == 0.0
    val colorScheme = when {
        useDynamic -> if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        useFrozenDefault -> if (darkTheme) DefaultDarkColorScheme else DefaultLightColorScheme
        else -> rememberDynamicColorScheme(
            seedColor = colorSeed(themeColorKey),
            isDark = darkTheme,
            isAmoled = false,
            primary = primary,
            secondary = secondary,
            tertiary = tertiary,
            style = style,
            contrastLevel = contrastLevel,
        )
    }
    // 旧「禁止页面染色」偏好仍保留含义：显式开启时把中性角色压成灰色，
    // 强调色与容器色不动。新安装默认关闭，使用带轻微色调的 MD3 表面。
    val styledScheme = if (keepBackgroundColor) neutralPageColors(colorScheme, darkTheme) else colorScheme
    // OLED 纯黑模式：深色下把所有表面压到纯黑/近黑，省电且对比更强
    val darkScheme = if (darkTheme && pureDark) styledScheme.copy(
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
    ) else styledScheme
    // 自定义背景只覆盖页面底色，且按取色器的原始色值应用，不交给主题引擎二次调色。
    val finalScheme = customBackground?.let { bg ->
        darkScheme.copy(
            background = bg,
            onBackground = if (bg.luminance() > 0.48f) Color.Black else Color.White,
        )
    } ?: darkScheme
    val wallpaper = remember(backgroundImage) {
        backgroundImage.takeIf { it.isNotEmpty() && it.length <= 6 * 1024 * 1024 }?.let { raw ->
            runCatching {
                val bytes = android.util.Base64.decode(raw, android.util.Base64.DEFAULT)
                val bounds = android.graphics.BitmapFactory.Options().apply { inJustDecodeBounds = true }
                android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
                require(bounds.outWidth in 1..1920 && bounds.outHeight in 1..1920)
                android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
            }.getOrNull()
        }
    }
    val surfaces = if (wallpaper == null) finalScheme else finalScheme.copy(
        background = Color.Transparent,
        // surface 保持不透明：它是正文、表单与页面的保护面；装饰性容器才允许透出背景图。
        surface = finalScheme.surface,
        surfaceVariant = finalScheme.surfaceVariant.copy(alpha = surfaceOpacity),
        surfaceContainer = finalScheme.surfaceContainer.copy(alpha = surfaceOpacity),
        surfaceContainerLowest = finalScheme.surfaceContainerLowest.copy(alpha = surfaceOpacity),
        surfaceContainerLow = finalScheme.surfaceContainerLow.copy(alpha = surfaceOpacity),
        surfaceContainerHigh = finalScheme.surfaceContainerHigh.copy(alpha = surfaceOpacity),
        surfaceContainerHighest = finalScheme.surfaceContainerHighest.copy(alpha = surfaceOpacity),
    )
    // 应用字号因子下沉到 LocalDensity（Density.fontScale）：
    // - 所有 sp 文本（MaterialTheme 字阶、阅读字阶、页面中固定 sp 如列表缩进/行高）统一缩放；
    // - dp 尺寸不受影响；系统字号缩放继续叠加，不再由字阶各自乘一次。
    val baseDensity = LocalDensity.current
    val appFontScale = fontScale.coerceIn(0.85f, 1.30f)
    val contentDensity = remember(baseDensity, appFontScale) {
        Density(baseDensity.density, baseDensity.fontScale * appFontScale)
    }
    MaterialTheme(
        colorScheme = surfaces,
        typography = appTypography(fontFamily, fontWeightLevel),
        shapes = AppShapes,
    ) {
        // 表面层次来自角色明度；Surface 的色调海拔不再用主色给中性面板叠色。
        // 命名间距、尺寸与阅读字阶由根部统一注入，页面只读角色，不自建页面级常量。
        val appSpacing = remember { AppSpacing() }
        val appSizes = remember { AppSizes() }
        val readingType = remember(fontFamily, fontWeightLevel) {
            appReadingType(fontFamily, fontWeightLevel)
        }
        CompositionLocalProvider(
            LocalDensity provides contentDensity,
            LocalTonalElevationEnabled provides false,
            LocalSurfaceOpacity provides if (wallpaper == null) 1f else surfaceOpacity,
            LocalAppSpacing provides appSpacing,
            LocalAppSizes provides appSizes,
            LocalReadingType provides readingType,
        ) {
            if (wallpaper == null) content()
            else Box(Modifier.fillMaxSize().background(finalScheme.background)) {
                Image(wallpaper, contentDescription = null, modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop, alpha = backgroundImageOpacity.coerceIn(0.05f, 0.65f))
                content()
            }
        }
    }
}
