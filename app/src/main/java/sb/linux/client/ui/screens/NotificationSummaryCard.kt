package sb.linux.client.ui.screens

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import sb.linux.client.domain.community.NotificationItem
import sb.linux.client.ui.patterns.topic.ReplySummaryCard
import sb.linux.client.ui.patterns.user.UserAvatar

@Composable
internal fun NotificationSummaryCard(item: NotificationItem, nav: NavHostController) {
    ReplySummaryCard(item.fromUser, item.content, item.timeText, maxLines = 4,
        leading = { UserAvatar(item.partnerId, item.partnerAvatar, 34) },
        badge = { if (item.unread) Badge { Text("未读") } },
        footer = {
            val topicId = Regex("""/topic/(\d+)""").find(item.link)?.groupValues?.get(1)
            if (topicId != null) TextButton(onClick = { nav.navigate("topic/$topicId") }) { Text("查看主题") }
            else {
                val partnerId = item.partnerId.takeIf { it > 0 }
                    ?: Regex("""/notify/(\d+)""").find(item.link)?.groupValues?.get(1)?.toLongOrNull() ?: 0L
                if (partnerId > 0) TextButton(onClick = {
                    nav.navigate("chat/$partnerId?name=${android.net.Uri.encode(item.fromUser)}&avatar=${android.net.Uri.encode(item.partnerAvatar)}")
                }) { Text("回复") }
            }
        })
}
