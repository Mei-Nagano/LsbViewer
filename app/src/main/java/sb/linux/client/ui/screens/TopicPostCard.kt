package sb.linux.client.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
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
    if (isMainPost) {
        // ---------- 楼主正文：正文长按为系统原生文本选择，不再弹功能菜单（2.6） ----------
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.13f))
                .padding(start = 14.dp, end = 14.dp, top = 12.dp, bottom = 2.dp)
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
        // ---------- 回复：单独卡片容器，长按弹功能菜单；高亮 = 楼层定位（2.13） ----------
        // 背景色 + 描边同时作为定位提示，让被定位楼层一眼可见
        val bg by animateColorAsState(
            targetValue = if (highlight) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.62f)
            else MaterialTheme.colorScheme.surfaceContainer,
            animationSpec = tween(400),
            label = "postBg"
        )
        val borderColor by animateColorAsState(
            targetValue = if (highlight) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0f),
            animationSpec = tween(400),
            label = "postBorder"
        )
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = bg,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.dp)
                // 先裁剪再挂手势，长按遮罩与圆角容器完全贴合（2.17）
                .clip(RoundedCornerShape(16.dp))
                .border(
                    width = if (highlight) 2.dp else 0.dp,
                    color = borderColor,
                    shape = RoundedCornerShape(16.dp),
                )
                .combinedClickable(onClick = {}, onLongClick = onCopy)
        ) {
            Column(Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
                PostCardContent(
                    post, loggedIn, isOpAuthor,
                    avatarSize = 34,
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
                )
            }
        }
    }
}

/** 帖内统一徽章胶囊：UID / 楼主 / 头衔（组别）/ 称号共用同一规格，
 *  labelSmall 字号 · 16dp 最小高度 · 45% 透明背景 · 6dp/1dp 内边距，仅配色语义不同 */
@Composable
internal fun PillBadge(text: String, fg: Color, bg: Color, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(50),
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
