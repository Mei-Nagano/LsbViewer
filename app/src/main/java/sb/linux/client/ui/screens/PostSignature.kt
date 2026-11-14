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
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
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
internal val LocalAllSignaturesHidden = staticCompositionLocalOf<MutableState<Boolean>> { mutableStateOf(false) }
internal val LocalSignatureRestoreVersion = staticCompositionLocalOf<MutableState<Int>> { mutableStateOf(0) }
internal const val SIGNATURE_RECOVERY_HINT = "可在「应用设置 → 阅读与交互 → 正文与评论」中恢复显示签名。"

@Composable
internal fun PostSignature(html: String, modifier: Modifier = Modifier) {
    val restoreVersion = LocalSignatureRestoreVersion.current.value
    var visible by rememberSaveable(html, restoreVersion) { mutableStateOf(true) }
    val allHidden = LocalAllSignaturesHidden.current
    var confirmation by remember { mutableStateOf(false) }
    val showing = visible && !allHidden.value
    // 0.78 叠在 surface 上约 4.9:1，满足 D06 的 4.5:1 正文门槛；再淡就不合格
    val color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.78f)
    // 折叠开关用主题色，避免与签名正文同色同号而被读成一段文字
    val toggleColor = MaterialTheme.colorScheme.primary
    val text = remember(html, color) {
        renderInline(Jsoup.parse(html, Endpoints.BASE).body(), color, Color.Transparent)
    }
    val linkHandler = LocalLinkHandler.current
    val uriHandler = LocalUriHandler.current
    if (showing) Column(modifier) {
    ClickableText(text, style = MaterialTheme.typography.bodySmall.copy(
        color = color,
    )) { offset ->
        text.getStringAnnotations("URL", offset, offset).firstOrNull()?.item?.let { raw ->
            val url = Endpoints.abs(raw)
            if (linkHandler != null) linkHandler(url) else uriHandler.openUri(url)
        }
    }
    Text("隐藏签名",
        style = MaterialTheme.typography.labelSmall, color = toggleColor,
        modifier = Modifier.clickable { confirmation = true }.padding(top = 4.dp, bottom = 6.dp))
    }
    if (confirmation) AlertDialog(onDismissRequest = { confirmation = false }, title = { Text("隐藏所有签名？") },
        text = { Text("可隐藏应用内所有帖子和评论的签名，或只隐藏当前签名。\n\n$SIGNATURE_RECOVERY_HINT") },
        confirmButton = { TextButton(onClick = { confirmation = false; allHidden.value = true }) { Text("隐藏所有") } },
        dismissButton = { TextButton(onClick = { confirmation = false; visible = false }) { Text("仅隐藏此条") } })
}
