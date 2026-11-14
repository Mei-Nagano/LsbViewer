package sb.linux.client.ui.patterns.topic

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import sb.linux.client.domain.topic.SourceTopicTag
import sb.linux.client.domain.topic.TitleBadge
import sb.linux.client.ui.theme.sourceTitleColor

/** plugins.css 的标签语义与日夜变量；不把源站状态重新染成应用主题色。 */
@Composable
internal fun SourceTopicTagView(tag: SourceTopicTag, modifier: Modifier = Modifier) {
    val classes = tag.classes.split(' ').toSet()
    val dark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val role = when {
        "is-ended" in classes || "finished" in classes -> "text-muted"
        "drawn" in classes -> "success"
        "rejected" in classes || ("red-packet-title-status" in classes && "is-random" !in classes) -> "danger"
        "pending" in classes && "community-lottery-title-status" in classes -> "brand"
        "pending" in classes || "is-random" in classes || "pinned" in classes || "hot-discussions-badge" in classes ||
            "topic-management-featured-badge" in classes || "community-lottery-title-status" in classes -> "warning"
        else -> "brand"
    }
    val backgroundRole = if (role == "text-muted") "line-soft" else "$role-soft"
    fun variable(name: String, fallback: Color) = sourceTitleColor(
        TitleBadge(name = "", sourceStyle = "--gacha-color:var(--$name)"), "color", fallback, dark)
    var foreground = variable(role, MaterialTheme.colorScheme.onSurfaceVariant)
    var background = variable(backgroundRole, MaterialTheme.colorScheme.surfaceContainerHigh)
    if (dark && role == "brand") { foreground = Color(0xFF586779); background = Color(0xFF1F2937) }
    if (dark && role == "text-muted") background = Color(0xFF1E2332)
    if ("community-lottery-title-status" in classes && "open" in classes) {
        foreground = Color.White; background = Color(0xFFDC2626)
    }
    if ("jubaopen-title-badge" in classes) {
        foreground = Color(0xFFF1F5F9); background = Color(0xFF7C3AED)
    }
    if ("resource_vote-rate" in classes) {
        foreground = when {
            "resource_vote-rate-high" in classes -> Color(0xFF15803D)
            "resource_vote-rate-low" in classes -> Color(0xFFDC2626)
            else -> Color(0xFFA16207)
        }
        background = foreground.copy(alpha = 0.12f)
    }
    val inline = tag.style.split(';').mapNotNull { item -> item.split(':', limit = 2)
        .takeIf { it.size == 2 }?.let { it[0].trim() to it[1].trim() } }.toMap()
    fun inlineColor(name: String, fallback: Color) = inline[name]?.let { raw -> sourceTitleColor(
        TitleBadge(name = "", sourceStyle = "--gacha-color:$raw"), "color", fallback, dark) } ?: fallback
    foreground = inlineColor("color", foreground)
    background = inlineColor("background", inlineColor("background-color", background))
    if ("virtual-card-list-price" in classes) {
        Text(tag.label, modifier, color = foreground, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
    } else Surface(modifier = modifier, shape = if ("hot-discussions-badge" in classes) RoundedCornerShape(6.dp) else CircleShape, color = background) {
        Text(tag.label, Modifier.padding(horizontal = 7.dp, vertical = 2.dp), color = foreground,
            style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
    }
}
