package sb.linux.client.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.foundation.text.*
import androidx.compose.foundation.text.selection.*
import androidx.compose.material.icons.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.*
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.platform.*
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.*
import androidx.compose.ui.text.input.*
import androidx.compose.ui.text.style.*
import androidx.compose.ui.unit.*
import androidx.compose.ui.window.*
import sb.linux.client.ui.*
import sb.linux.client.ui.theme.*
import sb.linux.client.ui.content.*
import sb.linux.client.ui.patterns.topic.*
import sb.linux.client.ui.patterns.user.*
import sb.linux.client.ui.components.feedback.*
import sb.linux.client.ui.components.layout.*
import sb.linux.client.ui.components.navigation.*
import sb.linux.client.domain.topic.PostEntry
/** #N 对话串联弹窗：把引用链上的楼层按时间正序完整展示。 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThreadDialog(
    posts: List<PostEntry>?,
    loading: Boolean,
    error: String?,
    currentPostId: Long,
    onDismiss: () -> Unit,
    onUser: (Long) -> Unit,
    title: String = "对话串联",
    countText: (Int) -> String = { "共 ${it} 条相关楼层" },
    onJumpFloor: ((Int) -> Unit)? = null,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.padding(horizontal = 20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.AutoMirrored.Filled.Chat, null,
                    Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.width(8.dp))
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                if (!loading) TextButton(onClick = onDismiss) { Text("关闭") }
            }
            Spacer(Modifier.height(6.dp))
            when {
                loading -> Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(vertical = 20.dp)
                ) {
                    AppInlineLoading(size = 16.dp)
                    Text("正在串联全部相关楼层…", style = MaterialTheme.typography.bodyMedium)
                }
                posts == null || posts.isEmpty() -> Text(
                    error ?: "加载失败",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(vertical = 20.dp)
                )
                else -> {
                    Text(
                        countText(posts.size),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    LazyColumn(
                        modifier = Modifier
                            .heightIn(max = 440.dp)
                            .padding(top = 6.dp),
                        contentPadding = PaddingValues(bottom = 28.dp)
                    ) {
                        itemsIndexed(posts, key = { i, p -> "$i-${p.floor}-${p.id}" }) { i, p ->
                            ThreadPostItem(
                                post = p,
                                isCurrent = p.id == currentPostId,
                                showDivider = i < posts.size - 1,
                                onUser = { onUser(p.authorId) },
                                onJumpFloor = onJumpFloor,
                            )
                        }
                    }
                    if (error != null) {
                        Text(
                            error,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                    }
                }
            }
        }
    }
}

/** 对话链中的单条楼层：扁平条目，当前楼层高亮；支持点击楼层号跳转定位（2.16） */
@Composable
private fun ThreadPostItem(
    post: PostEntry,
    isCurrent: Boolean,
    showDivider: Boolean,
    onUser: () -> Unit,
    onJumpFloor: ((Int) -> Unit)? = null,
) {
    if (post.userBlocked) {
        BlockedPostPlaceholder(post, 28, Modifier.padding(vertical = 10.dp)) { onJumpFloor?.invoke(it) }
        if (showDivider && !isCurrent) HorizontalDivider()
        return
    }
    Column(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp)
            .then(
                if (isCurrent) Modifier
                    .clip(MaterialTheme.shapes.large)
                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.22f))
                    .padding(10.dp)
                else Modifier.padding(horizontal = 2.dp)
            )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            UserAvatar(post.authorId, post.avatarUrl, 28, online = post.online, onProfile = onUser)
            Text(
                post.authorName,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (post.floor > 0) {
                Text(
                    "#${post.floor}",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = if (onJumpFloor != null) Modifier
                        .clip(MaterialTheme.shapes.small)
                        .clickable { onJumpFloor(post.floor) }
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                    else Modifier
                )
            } else {
                // 与楼层卡片中的楼主徽章保持统一样式与配色
                Badge(
                    "楼主",
                    MaterialTheme.colorScheme.primaryContainer,
                    MaterialTheme.colorScheme.onPrimaryContainer,
                    small = true
                )
            }
            Spacer(Modifier.weight(1f))
            Text(
                post.timeText,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.height(8.dp))
        HtmlContent(post.contentHtml)
    }
    if (showDivider && !isCurrent) {
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
    }
}

/** 跳楼对话框 */
