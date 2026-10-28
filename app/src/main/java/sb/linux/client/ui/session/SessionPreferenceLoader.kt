package sb.linux.client.ui.session


import android.app.Application
import android.content.Context

internal class SessionPreferenceLoader(
    private val app: Application,
    private val session: Session,
    private val themePreferences: SessionThemePreferenceController,
) {
    fun reload() {
        session.aiSummaryHistoryRepository.clearCachedSummaries()
        val prefs = app.getSharedPreferences("lsb_prefs", Context.MODE_PRIVATE)
        session.themeMode = ThemeModePref.entries.getOrElse(prefs.getInt("theme_mode", 0)) {
            ThemeModePref.SYSTEM
        }
        session.dynamicColor = prefs.getBoolean("dynamic_color", true)
        session.themeColorKey = prefs.getString("theme_color", "default") ?: "default"
        session.oledDark = prefs.getBoolean("oled_dark", false)
        session.themeStyle = prefs.getInt("theme_style", 0)
        session.themeContrast = prefs.getFloat("theme_contrast", 0f)
        session.themePrimary = themePreferences.prefColor(prefs.getString("theme_primary_override", ""))
        session.themeSecondary = themePreferences.prefColor(prefs.getString("theme_secondary_override", ""))
        session.themeTertiary = themePreferences.prefColor(prefs.getString("theme_tertiary_override", ""))
        session.themeBackground = themePreferences.prefColor(prefs.getString("theme_background_override", ""))
        session.backgroundImage = prefs.getString("background_image", "").orEmpty()
        session.backgroundImageEnabled = prefs.getBoolean("background_image_enabled", false)
        session.backgroundImageOpacity = prefs.getFloat("background_image_opacity", 0.25f)
            .coerceIn(0.05f, 0.65f)
        session.surfaceOpacity = prefs.getFloat("surface_opacity", 1f).coerceIn(0.2f, 1f)
        // 键缺失 = 新默认（带轻微色调的 MD3 表面）；显式 true 仍保留旧的中性面策略。
        session.keepBackgroundColor = prefs.getBoolean("keep_background_color", false)
        session.themeCustomColors = (prefs.getStringSet("theme_custom_colors", emptySet()) ?: emptySet()).toList()
        session.fontKey = prefs.getString("font_key", "default") ?: "default"
        session.customFontName = prefs.getString("custom_font_name", "") ?: ""
        session.fontScale = prefs.getFloat("font_scale", 1f).coerceIn(0.85f, 1.30f)
        session.fontWeightLevel = prefs.getInt("font_weight_level", 0).coerceIn(0, 2)
        session.updateCardColors(themePreferences.parseCardColors(prefs.getString("card_colors", "")))
        session.applyBrowseSettings(session.browseSettingsRepository.current())
        val general = session.generalSettingsRepository.current()
        session.tabletMode = general.tabletMode
        session.singleContentColumn = general.singleContentColumn
        session.bottomBarStyle = general.bottomBarStyle
        session.bottomBarItems = general.bottomBarItems
        session.replyBarStyle = general.replyBarStyle
        session.linkPreviewEnabled = general.linkPreviewEnabled
        // 屏蔽规则按源站账号隔离；访客不继承旧版全局设置。
        session.blockedWords = emptySet()
        session.blockedUsers = emptySet()
    }
}
