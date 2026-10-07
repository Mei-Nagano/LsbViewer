package sb.linux.client.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import sb.linux.client.domain.topic.PostEntry
import sb.linux.client.ui.*
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun PostAuthorHeader(post: PostEntry, avatarSize: Int, isOpAuthor: Boolean, searchHighlight: String, showFloorBadge: Boolean, onUser: () -> Unit, onFloor: (Int) -> Unit) {
// 作者行：头像 + 名称/徽章 + 元信息，楼层号固定右上角
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Avatar(post.avatarUrl, avatarSize, Modifier.clickable(onClick = onUser), online = post.online)
                if (LocalShowUid.current && post.authorUid > 0) {
                    Text(
                        post.authorUid.toString(),
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f).offset(y = (-2).dp)) {
                // 用户信息行（源站格式）：名字后面直接跟 UID，随后是组别/楼主徽章。
                // 用 FlowRow 在放不下时自动换行，避免徽章挤占导致用户名被截断
                FlowRow(
                    verticalArrangement = Arrangement.Center,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Text(
                        addSearchHighlights(AnnotatedString(post.authorName), searchHighlight, MaterialTheme.colorScheme.tertiaryContainer),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (isOpAuthor) {
                        PillBadge("楼主", MaterialTheme.colorScheme.onPrimaryContainer, MaterialTheme.colorScheme.primaryContainer, Modifier.align(Alignment.CenterVertically))
                    }
                    if (post.userGroup.isNotBlank()) {
                        PillBadge(post.userGroup, MaterialTheme.colorScheme.onSecondaryContainer, MaterialTheme.colorScheme.secondaryContainer, Modifier.align(Alignment.CenterVertically))
                    }
                    if (post.essenceVoteLabel.isNotBlank()) {
                        val support = post.essenceVoteSupport
                        PillBadge(
                            post.essenceVoteLabel,
                            when (support) {
                                true -> MaterialTheme.colorScheme.onTertiaryContainer
                                false -> MaterialTheme.colorScheme.onErrorContainer
                                null -> MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            when (support) {
                                true -> MaterialTheme.colorScheme.tertiaryContainer
                                false -> MaterialTheme.colorScheme.errorContainer
                                null -> MaterialTheme.colorScheme.surfaceContainerHighest
                            },
                            Modifier.align(Alignment.CenterVertically),
                        )
                    }
                }
                // 元信息行：称号矮胶囊（字号与时间一致）· 发送时间 · IP 属地
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    post.titleBadge?.let { t ->
                        // 称号独立渲染，避免与用户组/头衔/积分拼成一个标签；UR 保留序列号特化。
                        TitleBadgeView(t, small = true)
                    }
                    Text(
                        buildString {
                            if (post.timeText.isNotBlank()) append(post.timeText)
                            if (post.ipLocation.isNotBlank()) {
                                if (isNotEmpty()) append(" · ")
                                append(post.ipLocation)
                            }
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            if (showFloorBadge && post.floor > 0) {
                Spacer(Modifier.width(8.dp))
                Text(
                    "#${post.floor}",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onFloor(post.floor) }
                        .padding(horizontal = 6.dp, vertical = 3.dp)
                )
            }
        }
}
