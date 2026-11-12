package sb.linux.client.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import sb.linux.client.domain.topic.PostEntry
import sb.linux.client.ui.theme.LocalAppSpacing

/** Match the source user-block-hidden-reply avatar and bordered notice. */
@Composable
internal fun BlockedPostPlaceholder(
    post: PostEntry,
    avatarSize: Int,
    modifier: Modifier = Modifier,
    onFloor: (Int) -> Unit = {},
) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(LocalAppSpacing.current.contentGap)) {
        Surface(
            shape = MaterialTheme.shapes.extraSmall,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
            modifier = Modifier.size(avatarSize.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text("×", style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(post.authorName.ifBlank { "已屏蔽用户" }, Modifier.weight(1f),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (post.floor > 0) Text("#${post.floor}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.clickable { onFloor(post.floor) }.padding(horizontal = 6.dp))
            }
            if (post.timeText.isNotBlank()) Text(post.timeText,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Surface(
                shape = MaterialTheme.shapes.extraSmall,
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            ) {
                Text(post.blockedPlaceholder.ifBlank { "该用户已被屏蔽，内容已隐藏" },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp))
            }
        }
    }
}
