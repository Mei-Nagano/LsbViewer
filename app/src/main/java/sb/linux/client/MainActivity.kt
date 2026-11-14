package sb.linux.client

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.lifecycle.viewmodel.compose.viewModel
import sb.linux.client.ui.session.Session
import sb.linux.client.ui.session.ThemeModePref
import sb.linux.client.ui.theme.LsbTheme
import sb.linux.client.ui.theme.PaletteStyles
import sb.linux.client.ui.theme.ThemeMode
import sb.linux.client.ui.navigation.AppRoot

/** Activity 宿主：仅负责系统窗口、Session 创建和主题装配。 */
class MainActivity : ComponentActivity() {
    private var activeSession: Session? = null

    override fun onResume() {
        super.onResume()
        sb.linux.client.data.AppNetwork.detectProxyOnOpen()
        activeSession?.recoverSession()
    }

    override fun onStop() {
        super.onStop()
        if (isFinishing && !isChangingConfigurations) {
            activeSession?.let {
                it.dataManagementRepository.clearAutoClearItems()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = androidx.activity.SystemBarStyle.auto(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT,
            ),
            navigationBarStyle = androidx.activity.SystemBarStyle.auto(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT,
            ),
        )
        setContent {
            val session: Session = viewModel()
            SideEffect { activeSession = session }
            val dark = when (session.themeMode) {
                ThemeModePref.SYSTEM -> isSystemInDarkTheme()
                ThemeModePref.LIGHT -> false
                ThemeModePref.DARK -> true
            }
            LaunchedEffect(dark) {
                val controller = androidx.core.view.WindowCompat.getInsetsController(window, window.decorView)
                controller.isAppearanceLightStatusBars = !dark
                controller.isAppearanceLightNavigationBars = !dark
            }
            LaunchedEffect(session.hideStatusBar) {
                androidx.core.view.WindowCompat.getInsetsController(window, window.decorView).apply {
                    systemBarsBehavior = androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                    if (session.hideStatusBar) hide(androidx.core.view.WindowInsetsCompat.Type.statusBars())
                    else show(androidx.core.view.WindowInsetsCompat.Type.statusBars())
                }
            }
            val fontFamily = remember(session.fontKey, session.customFontName) {
                if (session.fontKey == "custom") {
                    session.preferences.customFontFile.takeIf { it.exists() }?.let { FontFamily(Font(it)) }
                } else null
            }
            LsbTheme(
                themeMode = when (session.themeMode) {
                    ThemeModePref.SYSTEM -> ThemeMode.SYSTEM
                    ThemeModePref.LIGHT -> ThemeMode.LIGHT
                    ThemeModePref.DARK -> ThemeMode.DARK
                },
                dynamicColor = session.dynamicColor,
                themeColorKey = session.themeColorKey,
                pureDark = session.oledDark,
                style = PaletteStyles.getOrElse(session.themeStyle) { com.materialkolor.PaletteStyle.TonalSpot },
                contrastLevel = session.themeContrast.toDouble(),
                primary = session.themePrimary,
                secondary = session.themeSecondary,
                tertiary = session.themeTertiary,
                customBackground = session.themeBackground,
                keepBackgroundColor = session.keepBackgroundColor,
                backgroundImage = if (session.backgroundImageEnabled) session.backgroundImage else "",
                backgroundImageOpacity = session.backgroundImageOpacity,
                surfaceOpacity = session.surfaceOpacity,
                fontFamily = fontFamily,
                fontScale = session.fontScale,
                fontWeightLevel = session.fontWeightLevel,
            ) {
                androidx.compose.runtime.CompositionLocalProvider(
                    sb.linux.client.ui.components.navigation.LocalHideStatusBar provides session.hideStatusBar,
                ) { AppRoot(session) }
            }
        }
    }
}
