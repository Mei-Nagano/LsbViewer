package sb.linux.client.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.foundation.text.selection.*
import androidx.compose.material.icons.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.layout.*
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.*
import androidx.compose.ui.unit.*
import sb.linux.client.ui.*
import sb.linux.client.domain.topic.PostEntry
@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
internal fun PostCardContent(
    post: PostEntry,
    loggedIn: Boolean,
    isOpAuthor: Boolean,
    avatarSize: Int,
    onUser: () -> Unit,
    onQuote: () -> Unit,
    onLike: () -> Unit,
    onCoin: () -> Unit,
    onReport: () -> Unit,
    onFloor: (Int) -> Unit,
    onThread: () -> Unit,
    showFloorBadge: Boolean,
    modifier: Modifier = Modifier,
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
    Column(modifier) {
        PostAuthorHeader(
            post = post,
            avatarSize = avatarSize,
            isOpAuthor = isOpAuthor,
            searchHighlight = searchHighlight,
            showFloorBadge = showFloorBadge,
            onUser = onUser,
            onFloor = onFloor,
        )
        Spacer(Modifier.height(10.dp))
        // 正文：交给系统原生长按选择菜单（复制/翻译/搜索由系统提供）；#楼层号可点击跳转
        SelectionContainer {
            HtmlContent(post.contentHtml, modifier = Modifier.onGloballyPositioned { onBodyLayout(it.positionInRoot().y, it.size.height.toFloat()) }, onFloor = onFloor, onHeadingPosition = onHeadingPosition, highlightQuery = searchHighlight, onSearchPosition = onSearchPosition)
        }
        SmartDecodedContentPanel(post.contentHtml)
        // 回复目标与本评论的子树折叠均位于卡片右下角；二者互不抢占点击区域。
        if ((showReplyCapsule && post.parentFloor > 0) || childReplyCount > 0) {
            Spacer(Modifier.height(6.dp))
            FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.End), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                if (showReplyCapsule && post.parentFloor > 0) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f),
                        modifier = Modifier.clip(RoundedCornerShape(50)).clickable { onFloor(post.parentFloor) }
                    ) {
                        Row(
                            Modifier.padding(horizontal = 9.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            if (replyParentAvatar.isNotBlank()) Avatar(replyParentAvatar, 16)
                            else Icon(Icons.AutoMirrored.Filled.Reply, null, Modifier.size(12.dp), tint = MaterialTheme.colorScheme.onSecondaryContainer)
                            Text("回复 #${post.parentFloor}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSecondaryContainer)
                        }
                    }
                }
                if (childReplyCount > 0) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.58f),
                        modifier = Modifier.clip(RoundedCornerShape(50)).clickable(onClick = onToggleChildren),
                    ) {
                        Row(Modifier.padding(horizontal = 9.dp, vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                if (childrenCollapsed) Icons.Filled.ExpandMore else Icons.Filled.ExpandLess,
                                null, Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onTertiaryContainer,
                            )
                            Spacer(Modifier.width(2.dp))
                            Text(
                                if (childrenCollapsed) "展开 $childReplyCount 条回复" else "折叠 $childReplyCount 条回复",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                            )
                        }
                    }
                }
            }
        }
        // 对话串联入口（默认关闭，设置中可开启）：本楼引用了其他楼层、或被其他楼层回复时显示
        val refFloors = post.referencedFloors.filter { it > 0 }
        if (threadEnabled && (refFloors.isNotEmpty() || threadReplyCount > 0)) {
            Spacer(Modifier.height(6.dp))
            Surface(
                shape = RoundedCornerShape(50),
                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f),
                modifier = Modifier.clip(RoundedCornerShape(50))
                    .clickable { onThread() }
            ) {
                Row(
                    Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.Chat, null,
                        Modifier.size(13.dp),
                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Text(
                        when {
                            refFloors.size > 1 ->
                                "查看 ${refFloors.joinToString(" ") { "#$it" }} 对话"
                            refFloors.size == 1 ->
                                "查看 #${refFloors.first()} 对话"
                            post.floor > 0 ->
                                "查看 #${post.floor} 的对话"
                            else ->
                                "查看楼主对话"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        }
        Spacer(Modifier.height(4.dp))
        // 功能按钮与正文之间的分割线
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
        Spacer(Modifier.height(4.dp))
        // 最后编辑信息：移到分割线下方显示（2.5）；编辑人可点击跳转其主页（2.12）
        if (post.editInfo.isNotBlank()) {
            EditInfoText(post, onEditUser)
            Spacer(Modifier.height(2.dp))
        }
        // 操作行：楼主正文 = 点赞 / 投币 / 举报（2.9 去除收藏）；回复 = 点赞 / 投币 / 回复 / 举报。
        // 树形回复（showFloorBadge）层级缩进挤占宽度，操作行只留图标省空间；
        // 状态由图标表达（点赞=实心爱心、投币/收藏=主题色），点赞数保留数字
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            if (post.canLike) {
                PostAction(
                    icon = if (post.liked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                    label = when {
                        showFloorBadge -> if (post.likeCount > 0) "${post.likeCount}" else ""
                        post.likeCount > 0 -> "赞 ${post.likeCount}"
                        else -> "点赞"
                    },
                    onClick = { if (loggedIn) onLike() },
                    iconTint = if (post.liked) MaterialTheme.colorScheme.primary else null
                )
            }
            // 投币（金额可自定义，理由选填）：与点赞共用 donate-reaction 表单，主楼/未登录无表单时不显示
            if (post.canLike) {
                PostAction(
                    icon = Icons.Filled.Paid,
                    label = if (showFloorBadge) "" else if (post.coined) "已投币" else "投币",
                    onClick = { if (loggedIn) onCoin() },
                    iconTint = if (post.coined) MaterialTheme.colorScheme.primary else null
                )
            }
            // 打赏：源站的打赏入口不再解析进正文，集成在分割线下方的操作行（仅楼主正文）
            onDonate?.let { donate ->
                PostAction(
                    icon = Icons.Filled.VolunteerActivism,
                    label = "打赏",
                    onClick = donate
                )
            }
            PostAction(
                icon = Icons.Filled.Flag,
                label = if (showFloorBadge) "" else "举报",
                onClick = onReport
            )
            // 本地收藏评论（34）：仅回复显示，收藏后图标填充
            if (showFloorBadge && onFavComment != null) {
                PostAction(
                    icon = if (commentFavorited) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
                    label = "",
                    onClick = onFavComment,
                    iconTint = if (commentFavorited) MaterialTheme.colorScheme.primary else null
                )
            }
            if (showFloorBadge) {
                // 回复的操作行：点赞 / 打赏 / 举报 / 回复（原"引用"统一为回复）
                PostAction(icon = Icons.Filled.FormatQuote, label = "", onClick = { if (loggedIn) onQuote() })
            }
            Spacer(Modifier.weight(1f))
        }
    }
}
