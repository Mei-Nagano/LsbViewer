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
import sb.linux.client.data.Session
import sb.linux.client.data.ThemeModePref
import sb.linux.client.ui.LsbTheme
import sb.linux.client.ui.PaletteStyles
import sb.linux.client.ui.ThemeMode
import sb.linux.client.ui.navigation.AppRoot

/** Activity 宿主：仅负责系统窗口、Session 创建和主题装配。 */
class MainActivity : ComponentActivity() {
    private var activeSession: Session? = null

    override fun onResume() {
        super.onResume()
        activeSession?.recoverSession()
    }

    override fun onStop() {
        super.onStop()
        if (isFinishing && !isChangingConfigurations) {
            activeSession?.let { it.clearDataItems(it.settings.autoClearItems, notify = false) }
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
            val fontFamily = remember(session.fontKey, session.customFontName) {
                if (session.fontKey == "custom") {
                    session.customFontFile.takeIf { it.exists() }?.let { FontFamily(Font(it)) }
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
                AppRoot(session)
            }
        }
    }
}
