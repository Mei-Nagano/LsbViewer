package sb.linux.client.ui.patterns.user

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
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
)

@Composable
fun UserInfoCard(
    data: UserInfoCardData,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
    onProfile: (() -> Unit)? = null,
) {
    val padding = if (compact) 14.dp else 18.dp
    val avatarSize = if (compact) 46 else 60
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(Modifier.padding(padding), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                UserAvatar(data.userId, data.avatarUrl, avatarSize, online = data.online, onProfile = onProfile)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(data.username.ifBlank { "饼友" },
                            style = if (compact) MaterialTheme.typography.titleSmall else MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                                .clickable(enabled = onProfile != null) { onProfile?.invoke() })
                        data.titleBadge?.let { TitleBadgeView(it, small = true) }
                    }
                    if (data.userGroup.isNotBlank()) Text(data.userGroup,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (onProfile != null) IconButton(onClick = onProfile) {
                    Icon(Icons.Filled.ChevronRight, "个人主页", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                UserInfoLabel("积分 ${data.points.ifBlank { "-" }}")
                if (data.userId > 0) UserInfoLabel("UID ${data.userId}")
            }
            if (data.bioHtml.isNotBlank()) UserInfoBio(data.bioHtml)
        }
    }
}

@Composable
private fun UserInfoLabel(text: String) {
    Surface(shape = MaterialTheme.shapes.extraSmall, color = MaterialTheme.colorScheme.surfaceContainer) {
        Text(text, style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp))
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
