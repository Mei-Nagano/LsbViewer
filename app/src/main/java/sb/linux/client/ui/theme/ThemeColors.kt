package sb.linux.client.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/**
 * 默认静态色板（DESIGN_SYSTEM D02）。
 *
 * 品牌种子为 [BrandSeed]，本表是 SchemeTonalSpot / contrastLevel=0 的冻结输出：
 * 种子不等于最终 primary，页面不得把种子或本表的参考值当组件色直接使用。
 * 用户自定义种子仍走 MaterialKolor 生成；本表只服务「默认」配色且未被角色覆盖时。
 *
 * `background` 兼容映射 surface，`onBackground` 映射 onSurface，旧 `surfaceVariant`
 * 映射 surfaceContainerHighest；新组件不继续扩散这些旧别名。
 */
internal val BrandSeed: Color = Color(0xFF375BAA)

/** 「默认」配色的持久化 key；与用户选择的预设/自定义色区分。 */
internal const val DEFAULT_THEME_KEY = "default"

internal val DefaultLightColorScheme: ColorScheme = lightColorScheme(
    primary = Color(0xFF485D92),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFDAE2FF),
    onPrimaryContainer = Color(0xFF2F4578),
    inversePrimary = Color(0xFFB1C5FF),
    secondary = Color(0xFF585E71),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFDCE2F9),
    onSecondaryContainer = Color(0xFF404659),
    tertiary = Color(0xFF735572),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFED7FA),
    onTertiaryContainer = Color(0xFF593D59),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF93000A),
    background = Color(0xFFFAF8FF),
    onBackground = Color(0xFF1A1B21),
    surface = Color(0xFFFAF8FF),
    onSurface = Color(0xFF1A1B21),
    surfaceDim = Color(0xFFDAD9E0),
    surfaceBright = Color(0xFFFAF8FF),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF4F3FA),
    surfaceContainer = Color(0xFFEEEDF4),
    surfaceContainerHigh = Color(0xFFE8E7EF),
    surfaceContainerHighest = Color(0xFFE2E2E9),
    surfaceVariant = Color(0xFFE2E2E9),
    onSurfaceVariant = Color(0xFF44464F),
    outline = Color(0xFF757780),
    outlineVariant = Color(0xFFC5C6D0),
    inverseSurface = Color(0xFF2F3036),
    inverseOnSurface = Color(0xFFF1F0F7),
    surfaceTint = Color(0xFF485D92),
    scrim = Color(0xFF000000),
)

internal val DefaultDarkColorScheme: ColorScheme = darkColorScheme(
    primary = Color(0xFFB1C5FF),
    onPrimary = Color(0xFF162E60),
    primaryContainer = Color(0xFF2F4578),
    onPrimaryContainer = Color(0xFFDAE2FF),
    inversePrimary = Color(0xFF485D92),
    secondary = Color(0xFFC0C6DC),
    onSecondary = Color(0xFF2A3042),
    secondaryContainer = Color(0xFF404659),
    onSecondaryContainer = Color(0xFFDCE2F9),
    tertiary = Color(0xFFE0BBDD),
    onTertiary = Color(0xFF412742),
    tertiaryContainer = Color(0xFF593D59),
    onTertiaryContainer = Color(0xFFFED7FA),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF121318),
    onBackground = Color(0xFFE2E2E9),
    surface = Color(0xFF121318),
    onSurface = Color(0xFFE2E2E9),
    surfaceDim = Color(0xFF121318),
    surfaceBright = Color(0xFF38393F),
    surfaceContainerLowest = Color(0xFF0D0E13),
    surfaceContainerLow = Color(0xFF1A1B21),
    surfaceContainer = Color(0xFF1E1F25),
    surfaceContainerHigh = Color(0xFF282A2F),
    surfaceContainerHighest = Color(0xFF33343A),
    surfaceVariant = Color(0xFF33343A),
    onSurfaceVariant = Color(0xFFC5C6D0),
    outline = Color(0xFF8F9099),
    outlineVariant = Color(0xFF44464F),
    inverseSurface = Color(0xFFE2E2E9),
    inverseOnSurface = Color(0xFF2F3036),
    surfaceTint = Color(0xFFB1C5FF),
    scrim = Color(0xFF000000),
)
