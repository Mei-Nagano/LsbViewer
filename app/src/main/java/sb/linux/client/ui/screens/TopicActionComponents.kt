package sb.linux.client.ui.screens

import sb.linux.client.ui.theme.LocalAppSpacing
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
/** 帖内轻量操作：图标 + 文字，无边框 */
@Composable
internal fun PostAction(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    tint: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    iconTint: Color? = null,
) {
    Row(
        Modifier
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(icon, null, Modifier.size(15.dp), tint = iconTint ?: tint)
        if (label.isNotBlank()) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = tint)
        }
    }
}

/** 底部快捷回复栏：伪输入条 + 点赞主楼 + 收藏 */
@Composable
internal fun ReplyBar(
    loggedIn: Boolean,
    liked: Boolean,
    favorited: Boolean,
    likeCount: Int,
    showLike: Boolean,
    showFavorite: Boolean,
    pinned: Boolean,
    onReply: () -> Unit,
    onLike: () -> Unit,
    onFavorite: () -> Unit,
    onPin: () -> Unit,
) {
    // 底栏容器统一走 surfaceContainer，与聊天输入栏、其它底栏保持同一种层次
    Surface(color = MaterialTheme.colorScheme.surfaceContainer) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = LocalAppSpacing.current.pageHorizontal, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier.weight(1.8f).height(52.dp),
            ) {
                Row(
                    Modifier.fillMaxSize().padding(horizontal = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        if (loggedIn) "说点什么…" else "登录后回复",
                        Modifier.weight(1f).clip(CircleShape).clickable(onClick = onReply).padding(horizontal = 10.dp, vertical = 10.dp),
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                    )
                    if (showLike) TopicBarAction(if (liked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder, if (likeCount > 0) "$likeCount" else "点赞", liked, onLike)
                }
            }
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier.weight(1f).height(52.dp),
            ) {
                Row(
                    Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    TopicBarAction(Icons.Filled.PushPin, if (pinned) "已钉住" else "钉住", pinned, onPin)
                    if (showFavorite) TopicBarAction(if (favorited) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder, if (favorited) "已收藏" else "收藏", favorited, onFavorite)
                }
            }
        }
    }
}

@Composable
private fun TopicBarAction(icon: ImageVector, label: String, active: Boolean, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clip(MaterialTheme.shapes.medium).clickable(onClick = onClick).padding(horizontal = 7.dp, vertical = 3.dp),
    ) {
        val color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
        Icon(icon, label, Modifier.size(18.dp), tint = color)
        Text(label, style = MaterialTheme.typography.labelSmall, color = color, maxLines = 1)
    }
}

/** 未登录评论区引导卡 */
@Composable
internal fun LoginPromptCard(count: Int, onLogin: () -> Unit) {
    val spacing = LocalAppSpacing.current
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = spacing.pageHorizontal, vertical = spacing.contentGap)
    ) {
        Column(
            Modifier.padding(vertical = spacing.section),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Lock, null, Modifier.size(24.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.height(spacing.contentGap))
            Text("还有 $count 条回复", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(spacing.inline))
            Text(
                "登录后即可查看全部评论并参与讨论",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(spacing.paragraph))
            Button(onClick = onLogin, shape = CircleShape) { Text("立即登录") }
        }
    }
}


