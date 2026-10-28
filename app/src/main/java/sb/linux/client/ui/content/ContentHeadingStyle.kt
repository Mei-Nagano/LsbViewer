package sb.linux.client.ui.content

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import sb.linux.client.ui.theme.LocalReadingType

/** 标题按级别取阅读字阶（D04：H1 28/36、H2 24/32、H3 及以下 20/28，字重 500）。 */
@Composable
internal fun headingStyle(level: Int): TextStyle {
    val reading = LocalReadingType.current
    return when (level) {
        1 -> reading.heading1
        2 -> reading.heading2
        else -> reading.heading3
    }
}
