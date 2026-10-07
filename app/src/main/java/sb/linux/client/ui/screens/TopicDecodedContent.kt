package sb.linux.client.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.unit.dp
import sb.linux.client.ui.LocalSmartDecode
@Composable
internal fun SmartDecodedContentPanel(html: String) {
if (LocalSmartDecode.current) {
            val decodedItems = remember(html) { detectSmartEncodedContent(html) }
            if (decodedItems.isNotEmpty()) {
                val clipboard = LocalClipboardManager.current
                Spacer(Modifier.height(8.dp))
                decodedItems.forEach { decoded ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.55f),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                    ) {
                        Column(Modifier.padding(10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    "识别到 ${decoded.type}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                                    modifier = Modifier.weight(1f),
                                )
                                IconButton(
                                    onClick = { clipboard.setText(AnnotatedString(decoded.decoded)) },
                                    modifier = Modifier.size(30.dp),
                                ) { Icon(Icons.Filled.ContentCopy, "复制解码结果", Modifier.size(16.dp)) }
                            }
                            SelectionContainer {
                                Text(decoded.decoded, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
            }
        }
}
