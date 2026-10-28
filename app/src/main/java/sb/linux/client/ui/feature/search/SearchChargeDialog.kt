package sb.linux.client.ui.feature.search

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import sb.linux.client.domain.search.SearchCharge

@Composable
internal fun SearchChargeNotice(charge: SearchCharge?, error: String?) {
    val notice = charge?.let {
        it.notice.ifBlank {
            if (it.cost == 0) "当前账号可免费搜索"
            else "每次新搜索消耗 ${it.cost} 积分，当前可用 ${it.balance} 积分"
        }
    } ?: error ?: "正在读取原站搜索费用…"
    Text(notice, style = MaterialTheme.typography.bodySmall,
        color = if (error != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
internal fun SearchChargeDialog(pending: PendingSearch?, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    if (pending == null) return
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("确认搜索") },
        text = {
            Text("搜索“${pending.query.normalizedText}”将扣除 ${pending.charge.cost} 积分，" +
                "当前可用 ${pending.charge.balance} 积分。此次结果的翻页和范围切换不重复扣费。")
        },
        confirmButton = { TextButton(onClick = onConfirm) { Text("确认搜索") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}
