package sb.linux.client.ui.screens

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import sb.linux.client.ui.LocalLinkHandler
import androidx.navigation.NavHostController
import sb.linux.client.domain.community.NotificationItem
import sb.linux.client.ui.patterns.topic.ReplySummaryCard
import sb.linux.client.ui.patterns.user.UserAvatar

@Composable
internal fun NotificationSummaryCard(item: NotificationItem, nav: NavHostController) {
    val openLink = LocalLinkHandler.current
    ReplySummaryCard(item.fromUser, item.content, item.timeText, maxLines = 4,
        leading = { UserAvatar(item.partnerId, item.partnerAvatar, 34) },
        badge = { if (item.unread) Badge { Text("未读") } },
        footer = {
            val topicId = Regex("""/topic/(\d+)""").find(item.link)?.groupValues?.get(1)
            if (!item.isSystem && topicId != null) FilledTonalButton(
                onClick = { if (openLink != null) openLink(item.link) else nav.navigate("topic/$topicId") },
                shape = CircleShape, contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
            ) { Icon(Icons.Filled.OpenInNew, null, Modifier.size(16.dp)); Spacer(Modifier.width(6.dp)); Text("查看主题") }
            else {
                val partnerId = item.partnerId.takeIf { it > 0 }
                    ?: Regex("""/notify/(\d+)""").find(item.link)?.groupValues?.get(1)?.toLongOrNull() ?: 0L
                if (partnerId > 0) TextButton(onClick = {
                    nav.navigate("chat/$partnerId?name=${android.net.Uri.encode(item.fromUser)}&avatar=${android.net.Uri.encode(item.partnerAvatar)}")
                }) { Text("回复") }
            }
        })
}
