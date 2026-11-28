package sb.linux.client.ui.patterns.user

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import sb.linux.client.domain.account.UserGroupBadge
import sb.linux.client.ui.theme.LocalDarkTheme

/** 源站身份组的日夜调色与胶囊形状；称号继续使用独立的 TitleBadgeView。 */
@Composable
internal fun SourceUserGroupBadge(badge: UserGroupBadge, modifier: Modifier = Modifier) {
    val dark = LocalDarkTheme.current
    val colors = when (badge.palette) {
        "ocean" -> if (dark) "#82c8ff" to "#123a59" else "#1769aa" to "#e6f3ff"
        "violet" -> if (dark) "#cbb3ff" to "#382760" else "#7652b8" to "#f0eaff"
        "rose" -> if (dark) "#ffacc4" to "#60243c" else "#bd3e68" to "#ffe9f0"
        "amber" -> if (dark) "#ffd08a" to "#5d3c10" else "#a85d00" to "#fff1d9"
        "jade" -> if (dark) "#8ce3bd" to "#143f31" else "#167758" to "#e2f7ed"
        "sky" -> if (dark) "#8cdeea" to "#123f48" else "#087d91" to "#dff7fa"
        else -> if (dark) "#c4d0dd" to "#303b48" else "#4f5f72" to "#eef2f6"
    }
    fun color(property: String, fallback: String): Color {
        val key = "--user-uid-badge-$property"
        val raw = badge.sourceStyle.split(';').map { it.split(':', limit = 2) }
            .firstOrNull { it.size == 2 && it[0].trim() == key }?.get(1)?.trim()
        return runCatching { Color(android.graphics.Color.parseColor(raw ?: fallback)) }
            .getOrElse { Color(android.graphics.Color.parseColor(fallback)) }
    }
    Surface(shape = CircleShape, color = color("bg", colors.second), modifier = modifier) {
        Text(badge.name, Modifier.padding(horizontal = 7.dp, vertical = 1.dp),
            style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Normal,
            color = color("color", colors.first))
    }
}
