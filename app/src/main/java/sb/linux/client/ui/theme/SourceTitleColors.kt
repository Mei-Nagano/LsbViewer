package sb.linux.client.ui.theme

import androidx.compose.ui.graphics.Color
import sb.linux.client.domain.topic.TitleBadge

/** 源站日夜面板底色，用于叠加半透明称号背景，避免透出应用动态主题色。 */
internal fun sourceTitleSurface(dark: Boolean): Color =
    if (dark) Color(0xFF171B27) else Color(0xFFFFFFFF)

/** 页面内联的称号 CSS 变量优先；站点共享变量按原站日夜配色映射。 */
internal fun sourceTitleColor(title: TitleBadge, property: String, fallback: Color, dark: Boolean): Color {
    val values = title.sourceStyle.split(';').mapNotNull {
        val parts = it.split(':', limit = 2)
        if (parts.size == 2) parts[0].trim() to parts[1].trim() else null
    }.toMap()
    val raw = values["--gacha-$property"] ?: return fallback
    val palette = if (dark) mapOf(
        "warning" to "#f6a04e", "warning-soft" to "#252019",
        "danger" to "#f47278", "danger-soft" to "#241b20",
        "info" to "#6aaeff", "info-soft" to "#19212a",
        "text" to "#e2e5ed", "text-muted" to "#9399ab", "text-subtle" to "#636a80",
        "success" to "#2ab86e", "success-soft" to "#19211d",
        "brand" to "#9db5ca", "brand-hover" to "#6b7c90", "brand-soft" to "#202b37",
        "line" to "#3f4e60", "line-soft" to "#202432",
        "panel" to "#171b27", "bg" to "#0f121a", "inverse-text" to "#f1f5f9",
    ) else mapOf(
        "warning" to "#c2410c", "warning-soft" to "#fff7ed",
        "danger" to "#d94b4b", "danger-soft" to "#fff3f3",
        "info" to "#4a9eff", "info-soft" to "#eef4ff",
        "text" to "#000000", "text-muted" to "#555555", "text-subtle" to "#999999",
        "success" to "#169c52", "success-soft" to "#ecfdf3",
        "brand" to "#405d78", "brand-hover" to "#27ae60", "brand-soft" to "#e8eff6",
        "line" to "#d7dde5", "line-soft" to "#f0f2f5",
        "panel" to "#ffffff", "bg" to "#f4f4f4", "inverse-text" to "#ffffff",
    )
    val value = if (raw.startsWith("var(--")) {
        val inner = raw.removePrefix("var(--").substringBefore(')')
        val key = inner.substringBefore(',').trim()
        palette[key] ?: inner.substringAfter(',', "").trim().takeIf { it.isNotEmpty() }
    } else raw
    if (value == null) return fallback
    val rgba = Regex("rgba?\\(([^)]+)\\)").matchEntire(value)
    if (rgba != null) {
        val parts = rgba.groupValues[1].split(',').map { it.trim().toFloatOrNull() }
        if (parts.size in 3..4 && parts.all { it != null }) {
            return Color(parts[0]!! / 255f, parts[1]!! / 255f, parts[2]!! / 255f,
                parts.getOrNull(3) ?: 1f)
        }
    }
    val normalized = if (value.startsWith("#") && value.length == 4) {
        "#${value[1]}${value[1]}${value[2]}${value[2]}${value[3]}${value[3]}"
    } else value
    return runCatching { Color(android.graphics.Color.parseColor(normalized)) }.getOrDefault(fallback)
}
