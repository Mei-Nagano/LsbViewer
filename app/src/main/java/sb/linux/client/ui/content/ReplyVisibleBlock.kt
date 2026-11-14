package sb.linux.client.ui.content

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import sb.linux.client.ui.theme.LocalAppSpacing

/** 锁定状态只展示源站通知；已公开内容保留内部 Markdown、链接和媒体。 */
@Composable
internal fun ReplyVisibleBlock(block: ContentBlock, modifier: Modifier, content: @Composable (String) -> Unit) {
    val spacing = LocalAppSpacing.current
    Surface(modifier.fillMaxWidth().padding(vertical = spacing.item), shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
        Column(Modifier.padding(spacing.contentGap), verticalArrangement = Arrangement.spacedBy(spacing.item)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(spacing.item)) {
                Icon(if (block.replyVisibleLocked) Icons.Filled.Lock else Icons.Filled.LockOpen,
                    null, tint = MaterialTheme.colorScheme.primary)
                Text(if (block.replyVisibleLocked) "回复后可见" else "回复可见内容 · 已解锁",
                    style = MaterialTheme.typography.titleSmall)
            }
            if (block.replyVisibleLocked) Text(block.annotated?.text.orEmpty().ifBlank { "回复本帖后即可查看这部分内容。" },
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            else content(block.replyVisibleHtml.orEmpty())
        }
    }
}
