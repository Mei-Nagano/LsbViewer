package sb.linux.client.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp

/** 标题按级别缩放字号，还原源站 Markdown 标题层级视觉效果 */
@Composable
internal fun headingStyle(level: Int): androidx.compose.ui.text.TextStyle {
    val t = MaterialTheme.typography
    return when (level) {
        1 -> t.headlineMedium
        2 -> t.headlineSmall
        3 -> t.titleLarge
        else -> t.titleMedium
    }.copy(lineHeight = 28.sp)
}

