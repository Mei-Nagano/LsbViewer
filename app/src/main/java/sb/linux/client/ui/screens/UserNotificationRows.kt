package sb.linux.client.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import sb.linux.client.domain.community.NotificationItem
import sb.linux.client.ui.EmptyBox

internal fun LazyListScope.userNotificationRows(
    notifications: List<NotificationItem>,
    nav: NavHostController,
) {
    if (notifications.isEmpty()) {
        item { EmptyBox("暂无通知") }
    } else {
        itemsIndexed(notifications, key = { index, item -> "notif-${item.partnerId}-${item.link}-$index" }) { _, item ->
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 5.dp),
            ) {
                Column(Modifier.padding(14.dp)) {
                    Row(
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(
                            item.fromUser,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (item.unread) {
                            Text(
                                "未读",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onError,
                                modifier = Modifier.clip(RoundedCornerShape(6.dp))
                                    .background(MaterialTheme.colorScheme.error)
                                    .padding(horizontal = 6.dp, vertical = 1.dp),
                            )
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(item.content, style = MaterialTheme.typography.bodyMedium, maxLines = 4, overflow = TextOverflow.Ellipsis)
                    Spacer(Modifier.height(4.dp))
                    Text(item.timeText, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    val topicId = Regex("""/topic/(\d+)""").find(item.link)?.groupValues?.get(1)
                    when {
                        topicId != null -> TextButton(onClick = { nav.navigate("topic/$topicId") }) { Text("查看主题") }
                        else -> {
                            val partnerId = item.partnerId.takeIf { it > 0 }
                                ?: Regex("""/notify/(\d+)""").find(item.link)?.groupValues?.get(1)?.toLongOrNull() ?: 0L
                            if (partnerId > 0) TextButton(
                                onClick = {
                                    nav.navigate("chat/$partnerId?name=${android.net.Uri.encode(item.fromUser)}&avatar=${android.net.Uri.encode(item.partnerAvatar)}")
                                },
                            ) { Text("回复") }
                        }
                    }
                }
            }
        }
    }
}
