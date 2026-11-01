package sb.linux.client.ui.screens

import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import org.jsoup.Jsoup
import sb.linux.client.common.network.Endpoints
import sb.linux.client.ui.LocalLinkHandler
import sb.linux.client.ui.content.renderInline

/** 签名位于正文下方，链接仍沿用应用的统一打开设置。 */
@Composable
internal fun PostSignature(html: String) {
    var visible by rememberSaveable(html) { mutableStateOf(true) }
    val color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f)
    val text = remember(html, color) {
        renderInline(Jsoup.parse(html, Endpoints.BASE).body(), color, Color.Transparent)
    }
    val linkHandler = LocalLinkHandler.current
    val uriHandler = LocalUriHandler.current
    Column {
    if (visible) ClickableText(text, style = MaterialTheme.typography.labelSmall.copy(
        color = color,
    )) { offset ->
        text.getStringAnnotations("URL", offset, offset).firstOrNull()?.item?.let { raw ->
            val url = Endpoints.abs(raw)
            if (linkHandler != null) linkHandler(url) else uriHandler.openUri(url)
        }
    }
    Text(if (visible) "隐藏签名" else "显示签名",
        style = MaterialTheme.typography.labelSmall, color = color,
        modifier = Modifier.clickable { visible = !visible }.padding(vertical = 6.dp))
    }
}
