package sb.linux.client.ui.session

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color

/** Compose-observable owner for the application appearance and typography snapshot. */
internal class SessionThemeState {
    var themeMode by mutableStateOf(ThemeModePref.SYSTEM)
    var dynamicColor by mutableStateOf(true)
    var themeColorKey by mutableStateOf("default")
    var oledDark by mutableStateOf(false)
    var themeStyle by mutableStateOf(0)
    var themeContrast by mutableStateOf(0f)
    var themePrimary by mutableStateOf<Color?>(null)
    var themeSecondary by mutableStateOf<Color?>(null)
    var themeTertiary by mutableStateOf<Color?>(null)
    var themeBackground by mutableStateOf<Color?>(null)
    var keepBackgroundColor by mutableStateOf(true)
    var backgroundImage by mutableStateOf("")
    var backgroundImageEnabled by mutableStateOf(false)
    var backgroundImageOpacity by mutableStateOf(0.25f)
    var surfaceOpacity by mutableFloatStateOf(1f)
    var themeCustomColors by mutableStateOf<List<String>>(emptyList())
    var fontKey by mutableStateOf("default")
    var customFontName by mutableStateOf("")
    var fontScale by mutableStateOf(1f)
    var fontWeightLevel by mutableIntStateOf(0)
    var cardColors by mutableStateOf<Map<String, Color>>(emptyMap())

    fun updateCardColors(value: Map<String, Color>) {
        cardColors = value
    }
}
