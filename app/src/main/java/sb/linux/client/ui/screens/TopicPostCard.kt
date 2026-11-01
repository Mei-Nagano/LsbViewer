package sb.linux.client.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.CircleShape
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
@Composable
fun PostCard(
    post: PostEntry,
    loggedIn: Boolean,
    isOpAuthor: Boolean,
    onUser: () -> Unit,
    onQuote: () -> Unit,
    onLike: () -> Unit,
    onCoin: () -> Unit,
    onReport: () -> Unit,
    onFloor: (Int) -> Unit,
    onThread: () -> Unit,
    isMainPost: Boolean = false,
    highlight: Boolean = false,
    onCopy: () -> Unit = {},
    onDonate: (() -> Unit)? = null,
    onEditUser: (Long) -> Unit = {},
    threadEnabled: Boolean = true,
    threadReplyCount: Int = 0,
    replyParentAvatar: String = "",
    showReplyCapsule: Boolean = true,
    childReplyCount: Int = 0,
    childrenCollapsed: Boolean = false,
    onToggleChildren: () -> Unit = {},
    commentFavorited: Boolean = false,
    onFavComment: (() -> Unit)? = null,
    onHeadingPosition: (Int, Float) -> Unit = { _, _ -> },
    onBodyLayout: (Float, Float) -> Unit = { _, _ -> },
    searchHighlight: String = "",
    onSearchPosition: (Float) -> Unit = {},
) {
    // 背景图模式下正文必须有独立不透明保护面，否则文字直接压在图片上。
    val protectSurface = LocalSurfaceOpacity.current < 0.999f
    if (isMainPost) {
        // ---------- 楼主正文：与阅读列同宽，不再套卡片背景 ----------
        Column(
            Modifier
                .fillMaxWidth()
                .background(if (protectSurface) MaterialTheme.colorScheme.surface else Color.Transparent)
                .padding(horizontal = LocalAppSpacing.current.pageHorizontal, vertical = 12.dp),
        ) {
            PostCardContent(
                post, loggedIn, isOpAuthor,
                avatarSize = 40,
                onUser, onQuote, onLike, onCoin, onReport, onFloor, onThread,
                showFloorBadge = false,
                onCopy = onCopy,
                onDonate = onDonate,
                onEditUser = onEditUser,
                threadEnabled = threadEnabled,
                threadReplyCount = threadReplyCount,
                showReplyCapsule = showReplyCapsule,
                replyParentAvatar = replyParentAvatar,
                childReplyCount = childReplyCount,
                childrenCollapsed = childrenCollapsed,
                onToggleChildren = onToggleChildren,
                onHeadingPosition = onHeadingPosition,
                onBodyLayout = onBodyLayout,
                searchHighlight = searchHighlight,
                onSearchPosition = onSearchPosition,
            )
        }
    } else {
        // ---------- 回复：平面列表项，不再为每条回复包大圆角卡片 ----------
        // 高亮仍用整行底色表示楼层定位，不加阴影或描边容器。
        val bg by animateColorAsState(
            targetValue = when {
                highlight -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f)
                protectSurface -> MaterialTheme.colorScheme.surface
                else -> Color.Transparent
            },
            animationSpec = tween(400),
            label = "postBg"
        )
        Column(
            Modifier
                .fillMaxWidth()
                .background(bg)
                .combinedClickable(onClick = {}, onLongClick = onCopy)
                .padding(horizontal = LocalAppSpacing.current.pageHorizontal, vertical = 10.dp),
        ) {
            PostCardContent(
                post, loggedIn, isOpAuthor,
                avatarSize = 32,
                onUser, onQuote, onLike, onCoin, onReport, onFloor, onThread,
                showFloorBadge = true,
                onCopy = onCopy,
                onDonate = onDonate,
                onEditUser = onEditUser,
                threadEnabled = threadEnabled,
                threadReplyCount = threadReplyCount,
                showReplyCapsule = showReplyCapsule,
                replyParentAvatar = replyParentAvatar,
                childReplyCount = childReplyCount,
                childrenCollapsed = childrenCollapsed,
                onToggleChildren = onToggleChildren,
                commentFavorited = commentFavorited,
                onFavComment = onFavComment,
                onHeadingPosition = onHeadingPosition,
                onBodyLayout = onBodyLayout,
                searchHighlight = searchHighlight,
                onSearchPosition = onSearchPosition,
                comment = true,
            )
        }
    }
}

/** 帖内统一徽章胶囊：UID / 楼主 / 头衔（组别）/ 称号共用同一规格，
 *  labelSmall 字号 · 16dp 最小高度 · 45% 透明背景 · 6dp/1dp 内边距，仅配色语义不同 */
@Composable
internal fun PillBadge(text: String, fg: Color, bg: Color, modifier: Modifier = Modifier) {
    Surface(
        shape = CircleShape,
        color = bg.copy(alpha = 0.45f),
        modifier = modifier.heightIn(min = 16.dp)
    ) {
        Text(
            text,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            color = fg,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
        )
    }
}
