package sb.linux.client.ui.screens
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import com.materialkolor.PaletteStyle
import com.materialkolor.rememberDynamicColorScheme
import sb.linux.client.ui.session.ThemeModePref

@Composable
internal fun isPreviewDark(mode: ThemeModePref): Boolean = when (mode) {
    ThemeModePref.SYSTEM -> isSystemInDarkTheme()
    ThemeModePref.LIGHT -> false
    ThemeModePref.DARK -> true
}

/** 由样式生成一套迷你预览方案（只用于预览，不落盘） */
@Composable
internal fun previewScheme(seed: Color, style: PaletteStyle, contrast: Double, dark: Boolean): ColorScheme =
    rememberDynamicColorScheme(
        seedColor = seed,
        isDark = dark,
        isAmoled = false,
        style = style,
        contrastLevel = contrast,
    )

/** 单个调色风格缩略色板项：用该风格实际生成的 primary/secondary/tertiary 色块预览 */
