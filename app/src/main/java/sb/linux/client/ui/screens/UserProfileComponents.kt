package sb.linux.client.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import sb.linux.client.data.UserProfile
import sb.linux.client.ui.Avatar
import sb.linux.client.ui.HtmlContent
import sb.linux.client.ui.TitleBadgeView

/** 用户主页资料头：头像、用户组、称号、积分/UID 和可折叠简介。 */
@Composable
internal fun UserProfileHeader(profile: UserProfile) {
    Column(Modifier.fillMaxWidth()) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            shape = RoundedCornerShape(22.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLow,
        ) {
            Column(Modifier.padding(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Avatar(profile.avatarUrl, 60, online = profile.online)
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                profile.username,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false),
                            )
                            profile.titleBadge?.let {
                                Spacer(Modifier.width(8.dp))
                                TitleBadgeView(it)
                            }
                        }
                        if (profile.userGroup.isNotBlank()) {
                            Spacer(Modifier.height(4.dp))
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = MaterialTheme.colorScheme.secondaryContainer,
                            ) {
                                Text(
                                    profile.userGroup,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(14.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                ) {
                    UserInfoPill(Icons.Filled.Star, "积分 ${profile.points.ifBlank { "-" }}")
                    UserInfoPill(Icons.Filled.Badge, "UID ${profile.userId}")
                }
                if (profile.bio.isNotBlank()) {
                    Spacer(Modifier.height(12.dp))
                    // 过长简介默认折叠，可展开/收起；不长的正常展示且不显示按钮。
                    val plainLen = org.jsoup.Jsoup.parse(profile.bio).text().length
                    val bioIsLong = plainLen > 80
                    var bioExpanded by remember(plainLen) { mutableStateOf(!bioIsLong) }
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .then(if (bioIsLong && !bioExpanded) Modifier.height(96.dp) else Modifier)
                            .then(if (bioIsLong && !bioExpanded) Modifier.clipToBounds() else Modifier),
                    ) {
                        HtmlContent(profile.bio, Modifier.fillMaxWidth(), onFloor = {})
                    }
                    if (bioIsLong) {
                        androidx.compose.material3.TextButton(onClick = { bioExpanded = !bioExpanded }) {
                            Text(if (bioExpanded) "收起" else "展开/收起")
                        }
                    }
                }
            }
        }
    }
}

/** 用户信息胶囊：图标 + 文本。 */
@Composable
private fun UserInfoPill(icon: ImageVector, text: String) {
    Surface(
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.surfaceContainerLowest.copy(alpha = 0.65f),
    ) {
        Row(
            Modifier.padding(horizontal = 11.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            androidx.compose.material3.Icon(icon, null, Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
            Text(
                text,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
