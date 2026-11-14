package sb.linux.client.ui.patterns.user

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import sb.linux.client.domain.topic.TitleBadge
import sb.linux.client.ui.common.content.htmlTextLength
import sb.linux.client.ui.content.HtmlContent
import sb.linux.client.ui.TitleBadgeView
import sb.linux.client.ui.theme.LocalAppSpacing
import sb.linux.client.ui.theme.LocalAppSizes

/** Shared visual model; callers supply source data and their own navigation. */
data class UserInfoCardData(
    val userId: Long,
    val username: String,
    val avatarUrl: String,
    val userGroup: String = "",
    val points: String = "",
    val titleBadge: TitleBadge? = null,
    val online: Boolean = false,
    val bioHtml: String = "",
    val following: String = "",
    val followers: String = "",
    val wallet: String = "",
    val subtitle: String = "",
    val groupBadge: sb.linux.client.domain.account.UserGroupBadge? = null,
)

@Composable
fun UserInfoCard(
    data: UserInfoCardData,
    modifier: Modifier = Modifier,
    onProfile: (() -> Unit)? = null,
    onStatistic: ((String) -> Unit)? = null,
    headerActions: (@Composable RowScope.() -> Unit)? = null,
    emphasisWeight: FontWeight? = FontWeight.SemiBold,
) {
    val openLink = sb.linux.client.ui.LocalLinkHandler.current
    val statisticClick = onStatistic ?: openLink
    val spacing = LocalAppSpacing.current
    val sizes = LocalAppSizes.current
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(Modifier.padding(horizontal = spacing.pageHorizontal)) {
            Column(Modifier.fillMaxWidth()
                .then(if (onProfile != null) Modifier.clickable(onClick = onProfile) else Modifier)
                .padding(vertical = spacing.pageVertical),
                verticalArrangement = Arrangement.spacedBy(spacing.inline)) {
                Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(spacing.pageHorizontal)) {
                    if (data.userId > 0) UserAvatar(data.userId, data.avatarUrl, sizes.avatarProfile.value.toInt(),
                        online = data.online, onProfile = onProfile, shape = MaterialTheme.shapes.extraSmall)
                    else if (data.avatarUrl.isNotBlank()) Avatar(data.avatarUrl, sizes.avatarProfile.value.toInt(), online = data.online, shape = MaterialTheme.shapes.extraSmall)
                    else Surface(shape = MaterialTheme.shapes.extraSmall, color = MaterialTheme.colorScheme.surfaceContainerHigh) {
                        Box(Modifier.size(sizes.avatarProfile), contentAlignment = Alignment.Center) {
                            Icon(Icons.Filled.Person, null, Modifier.size(sizes.avatarReply), tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                    Column(Modifier.weight(1f).fillMaxHeight()) {
                        Text(data.username.ifBlank { "饼友" }, style = MaterialTheme.typography.titleLarge,
                            fontWeight = emphasisWeight, modifier = Modifier.fillMaxWidth(),
                            maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Spacer(Modifier.weight(1f))
                        // UID 与身份组以辅助文字紧随姓名（§2.1、§3.6）。
                        val identity = listOfNotNull(
                            data.userId.takeIf { it > 0 }?.let { "UID $it" },
                            data.groupBadge?.name?.takeIf(String::isNotBlank)
                                ?: data.userGroup.takeIf(String::isNotBlank),
                        ).joinToString(" · ")
                        Box(Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(end = if (headerActions != null) sizes.touchTarget * 2 else 0.dp)) {
                                if (identity.isNotBlank()) Text(identity,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                                data.titleBadge?.let { TitleBadgeView(it, small = true) }
                            }
                            if (headerActions != null) Row(
                                Modifier.matchParentSize().wrapContentSize(Alignment.CenterEnd),
                                verticalAlignment = Alignment.CenterVertically,
                                content = headerActions,
                            )
                        }
                    }
                }
                if (data.subtitle.isNotBlank()) Text(data.subtitle, style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (data.userId > 0) Row(Modifier.fillMaxWidth().padding(bottom = spacing.pageVertical),
                horizontalArrangement = Arrangement.spacedBy(spacing.item)) {
                listOf(
                    Triple("关注", data.following, "${sb.linux.client.common.network.Endpoints.BASE}/index.php?sort=following&follow_view=following&follow_uid=${data.userId}"),
                    Triple("粉丝", data.followers, "${sb.linux.client.common.network.Endpoints.BASE}/index.php?sort=following&follow_view=followers&follow_uid=${data.userId}"),
                    Triple("积分", data.points, "${sb.linux.client.common.network.Endpoints.BASE}/user/${data.userId}?tab=points_rewards"),
                    Triple("烧饼", data.wallet, "${sb.linux.client.common.network.Endpoints.BASE}/community_wallet"),
                ).forEach { (label, value, url) ->
                    Column(Modifier.weight(1f)
                        .then(if (statisticClick != null) Modifier.clickable(onClickLabel = "查看$label") { statisticClick(url) } else Modifier)
                        .padding(vertical = spacing.inline), horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(spacing.inline)) {
                        Text(value.ifBlank { "—" }, style = MaterialTheme.typography.titleMedium,
                            fontWeight = emphasisWeight)
                        Text(label, style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            if (data.bioHtml.isNotBlank()) Box(Modifier.padding(bottom = spacing.pageVertical)) { UserInfoBio(data.bioHtml) }
        }
    }
}

@Composable
private fun UserInfoBio(html: String) {
    val long = remember(html) { htmlTextLength(html) > 80 }
    var expanded by remember(html) { mutableStateOf(!long) }
    Column {
        Box(Modifier.fillMaxWidth()
            .then(if (long && !expanded) Modifier.height(96.dp).clipToBounds() else Modifier)) {
            HtmlContent(html, Modifier.fillMaxWidth(), onFloor = {})
        }
        if (long) TextButton(onClick = { expanded = !expanded }) {
            Text(if (expanded) "收起简介" else "展开简介")
        }
    }
}
