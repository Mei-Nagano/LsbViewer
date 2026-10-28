package sb.linux.client.ui.theme

import androidx.compose.ui.graphics.Color
import sb.linux.client.domain.topic.TitleBadge

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
        "text-muted" to "#9399ab", "success" to "#2ab86e", "success-soft" to "#19211d",
    ) else mapOf(
        "warning" to "#c2410c", "warning-soft" to "#fff7ed",
        "danger" to "#d94b4b", "danger-soft" to "#fff3f3",
        "info" to "#4a9eff", "info-soft" to "#eef4ff", "text-muted" to "#555",
    )
    val value = if (raw.startsWith("var(--")) palette[raw.removePrefix("var(--").substringBefore(')')] else raw
    if (value == null) return fallback
    val rgba = Regex("rgba?\\(([^)]+)\\)").matchEntire(value)
    if (rgba != null) {
        val parts = rgba.groupValues[1].split(',').map { it.trim().toFloatOrNull() }
        if (parts.size in 3..4 && parts.all { it != null }) {
            return Color(parts[0]!! / 255f, parts[1]!! / 255f, parts[2]!! / 255f,
                parts.getOrNull(3) ?: 1f)
        }
    }
    return runCatching { Color(android.graphics.Color.parseColor(value)) }.getOrDefault(fallback)
}
