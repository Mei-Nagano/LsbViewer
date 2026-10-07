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

/** 全局形状：比默认更圆润的 MD3 风格 */
val LocalSurfaceOpacity = androidx.compose.runtime.staticCompositionLocalOf { 1f }

private val LsbShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

/** 应用字体（24）：把所选 FontFamily 应用到全部文字样式（保持 M3 默认字号/行高/字重） */
private fun appTypography(ff: FontFamily?, scale: Float, weightLevel: Int): Typography {
    val t = Typography()
    val sizeScale = scale.coerceIn(0.85f, 1.30f)
    val weightDelta = weightLevel.coerceIn(0, 2) * 100
    fun TextStyle.adjusted() = copy(
        fontFamily = ff ?: fontFamily,
        fontSize = fontSize * sizeScale,
        lineHeight = lineHeight * sizeScale,
        fontWeight = FontWeight(((fontWeight ?: FontWeight.Normal).weight + weightDelta).coerceAtMost(900)),
    )
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
    customBackground: Color? = null,
    keepBackgroundColor: Boolean = true,
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
    // 统一固定所有中性色角色；同时覆盖系统动态色，不能只固定 background 一个字段。
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
        surface = finalScheme.surface.copy(alpha = surfaceOpacity),
        surfaceVariant = finalScheme.surfaceVariant.copy(alpha = surfaceOpacity),
        surfaceContainer = finalScheme.surfaceContainer.copy(alpha = surfaceOpacity),
        surfaceContainerLowest = finalScheme.surfaceContainerLowest.copy(alpha = surfaceOpacity),
        surfaceContainerLow = finalScheme.surfaceContainerLow.copy(alpha = surfaceOpacity),
        surfaceContainerHigh = finalScheme.surfaceContainerHigh.copy(alpha = surfaceOpacity),
        surfaceContainerHighest = finalScheme.surfaceContainerHighest.copy(alpha = surfaceOpacity),
    )
    MaterialTheme(
        colorScheme = surfaces,
        typography = appTypography(fontFamily, fontScale, fontWeightLevel),
        shapes = LsbShapes,
    ) {
        // 防止 Surface 的色调海拔再次用主色给中性面板叠色。
        CompositionLocalProvider(LocalTonalElevationEnabled provides !keepBackgroundColor,
            LocalSurfaceOpacity provides if (wallpaper == null) 1f else surfaceOpacity) {
            if (wallpaper == null) content()
            else Box(Modifier.fillMaxSize().background(finalScheme.background)) {
                Image(wallpaper, contentDescription = null, modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop, alpha = backgroundImageOpacity.coerceIn(0.05f, 0.65f))
                content()
            }
        }
    }
}
