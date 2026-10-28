package sb.linux.client.ui.screens

import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.navigation.NavHostController
import sb.linux.client.domain.community.NotificationItem
import sb.linux.client.ui.components.feedback.EmptyBox

internal fun LazyListScope.userNotificationRows(notifications: List<NotificationItem>, nav: NavHostController) {
    if (notifications.isEmpty()) item { EmptyBox("暂无通知") }
    else itemsIndexed(notifications, key = { index, item -> "notif-${item.partnerId}-${item.link}-$index" }) { _, item ->
        NotificationSummaryCard(item, nav)
    }
}
