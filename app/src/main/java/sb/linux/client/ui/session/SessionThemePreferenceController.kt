package sb.linux.client.ui.session

import android.app.Application
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal class SessionThemePreferenceController(
    private val context: Application,
    private val session: SessionState,
) {
    private val prefs = context.getSharedPreferences(
        "lsb_prefs",
        android.content.Context.MODE_PRIVATE,
    )

    fun saveTheme(mode: ThemeModePref, dyn: Boolean, colorKey: String = session.themeColorKey) {
        session.themeMode = mode
        session.dynamicColor = dyn
        session.themeColorKey = colorKey
        prefs.edit().putInt("theme_mode", mode.ordinal).putBoolean("dynamic_color", dyn)
            .putString("theme_color", colorKey).apply()
    }

    fun saveOledDark(v: Boolean) {
        session.oledDark = v
        prefs.edit().putBoolean("oled_dark", v).apply()
    }

    fun saveThemeStyle(index: Int) {
        session.themeStyle = index
        prefs.edit().putInt("theme_style", index).apply()
    }

    fun saveThemeContrast(v: Float) {
        session.themeContrast = v
        prefs.edit().putFloat("theme_contrast", v).apply()
    }

    // ---------------- 自定义字体（24） ----------------

    /** 自定义字体文件（固定私有路径）：custom 档位从此加载 */
    val customFontFile: java.io.File
        get() = java.io.File(context.filesDir, "fonts/custom_font.ttf")

    fun saveFont(key: String) {
        session.fontKey = key
        prefs.edit().putString("font_key", key).apply()
    }

    fun saveFontScale(value: Float) {
        session.fontScale = value.coerceIn(0.85f, 1.30f)
        prefs.edit().putFloat("font_scale", session.fontScale).apply()
    }

    fun saveFontWeightLevel(value: Int) {
        session.fontWeightLevel = value.coerceIn(0, 2)
        prefs.edit().putInt("font_weight_level", session.fontWeightLevel).apply()
    }

    /** 导入自定义字体文件（24）：复制进私有目录并切换到 custom 档；校验可解析才生效 */
    suspend fun saveCustomFont(uri: android.net.Uri, displayName: String): Boolean {
        val ok = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            runCatching {
                val app = context
                customFontFile.parentFile?.mkdirs()
                app.contentResolver.openInputStream(uri)?.use { input ->
                    customFontFile.outputStream().use { input.copyTo(it) }
                } ?: return@runCatching false
                // 校验确为可用字体文件（损坏/选错文件时报失败）
                android.graphics.Typeface.createFromFile(customFontFile) != null
            }.getOrDefault(false)
        }
        if (ok) {
            session.customFontName = displayName
            session.fontKey = "custom"
            prefs.edit()
                .putString("font_key", "custom")
                .putString("custom_font_name", displayName)
                .apply()
        }
        return ok
    }

    /** 移除自定义字体文件并回到默认字体 */
    fun clearCustomFont() {
        runCatching { customFontFile.delete() }
        session.customFontName = ""
        session.fontKey = "default"
        prefs.edit()
            .putString("font_key", "default")
            .remove("custom_font_name")
            .apply()
    }

    /** 持久化元素级覆盖色（null=跟随种子色方案） */
    fun saveThemePrimary(c: Color?) { session.themePrimary = c; prefs.edit().putString("theme_primary_override", colorPref(c)).apply() }
    fun saveThemeSecondary(c: Color?) { session.themeSecondary = c; prefs.edit().putString("theme_secondary_override", colorPref(c)).apply() }
    fun saveThemeTertiary(c: Color?) { session.themeTertiary = c; prefs.edit().putString("theme_tertiary_override", colorPref(c)).apply() }
    fun saveThemeBackground(c: Color?) {
        session.themeBackground = c
        prefs.edit().putString("theme_background_override", colorPref(c)).apply()
    }
    suspend fun importBackgroundImage(uri: android.net.Uri) {
        val encoded = withContext(Dispatchers.IO) {
            val context = context
            val result = coil.Coil.imageLoader(context).execute(coil.request.ImageRequest.Builder(context)
                .data(uri).size(1920, 1920).scale(coil.size.Scale.FIT).precision(coil.size.Precision.EXACT).allowHardware(false).build())
            val original = (result.drawable as? android.graphics.drawable.BitmapDrawable)?.bitmap
                ?: throw IllegalArgumentException("无法读取此背景图片")
            val flat = android.graphics.Bitmap.createBitmap(original.width, original.height, android.graphics.Bitmap.Config.ARGB_8888)
            val bytes = try {
                android.graphics.Canvas(flat).apply { drawColor(android.graphics.Color.WHITE); drawBitmap(original, 0f, 0f, null) }
                java.io.ByteArrayOutputStream().use { out ->
                    check(flat.compress(android.graphics.Bitmap.CompressFormat.JPEG, 85, out)) { "图片处理失败" }
                    require(out.size() <= 4 * 1024 * 1024) { "背景图片处理后仍过大，请选择较小图片" }
                    out.toByteArray()
                }
            } finally { flat.recycle() }
            android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
        }
        session.backgroundImage = encoded
        session.backgroundImageEnabled = true
        prefs.edit().putString("background_image", encoded).putBoolean("background_image_enabled", true).apply()
    }
    fun saveBackgroundImageEnabled(value: Boolean) {
        session.backgroundImageEnabled = value
        prefs.edit().putBoolean("background_image_enabled", value).apply()
    }
    fun saveBackgroundImageOpacity(value: Float) {
        session.backgroundImageOpacity = value.coerceIn(0.05f, 0.65f)
        prefs.edit().putFloat("background_image_opacity", session.backgroundImageOpacity).apply()
    }
    fun saveSurfaceOpacity(value: Float) {
        session.surfaceOpacity = value.coerceIn(0.2f, 1f)
        prefs.edit().putFloat("surface_opacity", session.surfaceOpacity).apply()
    }
    fun removeBackgroundImage() {
        session.backgroundImage = ""; session.backgroundImageEnabled = false
        prefs.edit().remove("background_image").remove("background_image_enabled").apply()
    }
    fun saveKeepBackgroundColor(v: Boolean) {
        session.keepBackgroundColor = v
        prefs.edit().putBoolean("keep_background_color", v).apply()
    }

    /** 把自定义主题色加入预选（去重），并持久化 */
    fun addThemeCustomColor(key: String) {
        if (key !in session.themeCustomColors) session.themeCustomColors = session.themeCustomColors + key
        persistCustomColors()
    }

    /** 从预选中移除自定义主题色 */
    fun removeThemeCustomColor(key: String) {
        session.themeCustomColors = session.themeCustomColors - key
        persistCustomColors()
    }

    private fun persistCustomColors() {
        prefs.edit().putStringSet("theme_custom_colors", session.themeCustomColors.toSet()).apply()
    }

    private fun colorPref(c: Color?): String =
        c?.let { (it.toArgb().toLong() and 0xFFFFFFL).toString(16).padStart(6, '0') } ?: ""

    internal fun prefColor(s: String?): Color? =
        s?.takeIf { it.isNotEmpty() }?.toLongOrNull(16)?.let { Color(0xFF000000L or it) }

    /** 帖子卡片元素颜色持久化串：k1=hex;k2=hex */
    private fun serializeCardColors(m: Map<String, Color>): String =
        m.entries.joinToString(";") { "${it.key}=${colorPref(it.value)}" }

    internal fun parseCardColors(s: String?): Map<String, Color> =
        s?.split(';')?.mapNotNull { e ->
            val i = e.indexOf('=')
            if (i <= 0) null else prefColor(e.substring(i + 1))?.let { e.substring(0, i) to it }
        }?.toMap() ?: emptyMap()

    /** 保存/清除帖子卡片某元素的自定义颜色（null=恢复跟随主题） */
    fun saveCardColor(key: String, c: Color?) {
        session.updateCardColors(if (c == null) session.cardColors - key else session.cardColors + (key to c))
        prefs.edit().putString("card_colors", serializeCardColors(session.cardColors)).apply()
    }

    /** 重置外观设置（含卡片元素色与自定义色预选） */
    fun resetThemeSettings() {
        prefs.edit()
            .remove("theme_mode").remove("dynamic_color").remove("theme_color")
            .remove("oled_dark").remove("theme_style").remove("theme_contrast")
            .remove("theme_primary_override").remove("theme_secondary_override").remove("theme_tertiary_override")
            .remove("theme_background_override").remove("keep_background_color")
            .remove("theme_custom_colors").remove("card_colors")
            .remove("font_key").remove("custom_font_name")
            .remove("font_scale").remove("font_weight_level")
            // exact_theme_colors 是已删除的「强调色精确取值」开关，顺手清掉旧值
            .remove("exact_theme_colors")
            .remove("background_image").remove("background_image_enabled").remove("background_image_opacity")
            .remove("surface_opacity")
            .apply()
        session.themeMode = ThemeModePref.SYSTEM
        session.dynamicColor = true
        session.themeColorKey = "default"
        session.oledDark = false
        session.themeStyle = 0
        session.themeContrast = 0f
        session.themePrimary = null; session.themeSecondary = null; session.themeTertiary = null
        session.themeBackground = null
        session.backgroundImage = ""; session.backgroundImageEnabled = false; session.backgroundImageOpacity = 0.25f
        session.surfaceOpacity = 1f
        session.keepBackgroundColor = true
        session.themeCustomColors = emptyList()
        session.updateCardColors(emptyMap())
        session.fontKey = "default"
        session.customFontName = ""
        session.fontScale = 1f
        session.fontWeightLevel = 0
    }

}




