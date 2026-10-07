package sb.linux.client.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
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
            .clip(RoundedCornerShape(10.dp))
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
    Surface(color = MaterialTheme.colorScheme.surface, tonalElevation = 3.dp) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(50),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier.weight(1.8f).height(52.dp),
            ) {
                Row(
                    Modifier.fillMaxSize().padding(horizontal = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        if (loggedIn) "说点什么…" else "登录后回复",
                        Modifier.weight(1f).clip(RoundedCornerShape(50)).clickable(onClick = onReply).padding(horizontal = 10.dp, vertical = 10.dp),
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                    )
                    if (showLike) TopicBarAction(if (liked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder, if (likeCount > 0) "$likeCount" else "点赞", liked, onLike)
                }
            }
            Surface(
                shape = RoundedCornerShape(50),
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
        modifier = Modifier.clip(RoundedCornerShape(12.dp)).clickable(onClick = onClick).padding(horizontal = 7.dp, vertical = 3.dp),
    ) {
        val color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
        Icon(icon, label, Modifier.size(18.dp), tint = color)
        Text(label, style = MaterialTheme.typography.labelSmall, color = color, maxLines = 1)
    }
}

/** 液态玻璃评论底栏（评论底栏样式设置开启时）：悬浮胶囊，实时折射身后滚动的帖子内容
 *  （vibrancy 提饱和 + blur + lens 深度折射/边缘色散）；伪输入条 + 点赞主楼 + 收藏，
 *  交互与经典 ReplyBar 完全一致；Scaffold 空置 bottomBar 后内容区已含导航栏内边距，
 *  这里只需底部悬浮边距，无需再垫 navigationBarsPadding */
@Composable
internal fun GlassReplyBar(
    modifier: Modifier = Modifier,
    backdrop: Backdrop,
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
    // 玻璃表面色调：浅色主题垫白、深色主题垫黑，保证内容可读性（同玻璃底栏）
    val light = MaterialTheme.colorScheme.background.luminance() > 0.5f
    val surfaceTint = if (light) Color.White.copy(alpha = 0.35f) else Color.Black.copy(alpha = 0.4f)
    Row(
        modifier.padding(start = 14.dp, end = 14.dp, bottom = 10.dp).fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            Modifier.weight(1f).height(52.dp).drawBackdrop(
                backdrop, { RoundedCornerShape(50) },
                effects = { vibrancy(); blur(6.dp.toPx()); lens(18.dp.toPx(), 18.dp.toPx(), depthEffect = true, chromaticAberration = true) },
                onDrawSurface = { drawRect(surfaceTint) },
            ).padding(horizontal = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                if (loggedIn) "说点什么…" else "登录后回复",
                Modifier.weight(1f).clip(RoundedCornerShape(50)).clickable(onClick = onReply).padding(horizontal = 9.dp, vertical = 9.dp),
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
            if (showLike) TopicBarAction(if (liked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder, if (likeCount > 0) "$likeCount" else "点赞", liked, onLike)
        }
        Row(
            Modifier.weight(1f).height(52.dp).drawBackdrop(
                backdrop, { RoundedCornerShape(50) },
                effects = { vibrancy(); blur(6.dp.toPx()); lens(18.dp.toPx(), 18.dp.toPx(), depthEffect = true, chromaticAberration = true) },
                onDrawSurface = { drawRect(surfaceTint) },
            ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            TopicBarAction(Icons.Filled.PushPin, if (pinned) "已钉住" else "钉住", pinned, onPin)
            if (showFavorite) TopicBarAction(if (favorited) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder, if (favorited) "已收藏" else "收藏", favorited, onFavorite)
        }
    }
}

/** 未登录评论区引导卡 */
@Composable
internal fun LoginPromptCard(count: Int, onLogin: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Column(
            Modifier.padding(vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(50))
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Lock, null, Modifier.size(24.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.height(12.dp))
            Text("还有 $count 条回复", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(4.dp))
            Text(
                "登录后即可查看全部评论并参与讨论",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(16.dp))
            Button(onClick = onLogin, shape = RoundedCornerShape(50)) { Text("立即登录") }
        }
    }
}


