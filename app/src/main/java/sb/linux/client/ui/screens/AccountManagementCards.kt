package sb.linux.client.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import sb.linux.client.domain.account.*
import sb.linux.client.ui.patterns.topic.TopicTagCapsule
import sb.linux.client.ui.theme.LocalAppSpacing

/** 主信息是分区／余额；描述为次级，卡片与操作使用现有 M3 表面和 spacing token。 */
@Composable
internal fun AccountSectionCard(title: String, description: String, trailing: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit) {
    val spacing = LocalAppSpacing.current
    Surface(shape = MaterialTheme.shapes.extraLarge, color = MaterialTheme.colorScheme.surfaceContainerLow) {
        Column(Modifier.fillMaxWidth().padding(spacing.pageHorizontal), verticalArrangement = Arrangement.spacedBy(spacing.contentGap)) {
            if (title.isNotBlank() || trailing != null) Row(verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(spacing.item)) {
                Text(title, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                trailing?.invoke()
            }
            if (description.isNotBlank()) Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            content()
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun AccountStatistics(values: List<Pair<String, String>>) {
    val spacing = LocalAppSpacing.current
    FlowRow(maxItemsInEachRow = 3, horizontalArrangement = Arrangement.spacedBy(spacing.item),
        verticalArrangement = Arrangement.spacedBy(spacing.item)) {
        values.forEach { (label, value) ->
            Surface(modifier = Modifier.weight(1f).widthIn(min = 88.dp).fillMaxRowHeight(),
                shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surfaceContainerHigh) {
                Column(Modifier.padding(spacing.contentGap), verticalArrangement = Arrangement.spacedBy(spacing.inline)) {
                    Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(value.ifBlank { "—" }, style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
}

@Composable
internal fun AttachmentStorageCard(node: AccountNode.Storage, repository: AccountManagementRepository,
    enabled: Boolean, onLink: (String) -> Unit, onDirty: () -> Unit,
    onSubmit: (AccountForm, List<Pair<String, String>>) -> Unit) {
    AccountSectionCard(node.title, "", trailing = { if (node.available.isNotBlank()) TopicTagCapsule(node.available) }) {
        val spacing = LocalAppSpacing.current
        if (node.description.isNotBlank()) Text(node.description, style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        LinearProgressIndicator(progress = { node.fraction }, drawStopIndicator = {}, modifier = Modifier.fillMaxWidth().height(8.dp))
        AccountStatistics(node.statistics)
        node.form?.let { NativeAccountForm(it, repository, enabled, onDirty, onSubmit, embedded = true) }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(spacing.item)) {
            node.links.forEach { link -> TextButton(onClick = { onLink(link.url) }, enabled = enabled) { Text(link.label) } }
        }
    }
}

@Composable
internal fun PromotionManagementCard(node: AccountNode.Promotion, repository: AccountManagementRepository,
    enabled: Boolean, onLink: (String) -> Unit, onDirty: () -> Unit,
    onSubmit: (AccountForm, List<Pair<String, String>>) -> Unit) {
    val spacing = LocalAppSpacing.current
    Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surfaceContainerHigh) {
        Column(Modifier.fillMaxWidth().padding(spacing.contentGap), verticalArrangement = Arrangement.spacedBy(spacing.item)) {
            if (node.previewUrl.isNotBlank()) Surface(onClick = { onLink(node.targetUrl) }, enabled = enabled,
                shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surfaceContainer) {
                AsyncImage(node.previewUrl, node.title, Modifier.fillMaxWidth().aspectRatio(460f / 120f), contentScale = ContentScale.Fit)
            }
            if (node.previewUrl.isBlank() && node.targetUrl.isNotBlank()) Surface(onClick = { onLink(node.targetUrl) }, enabled = enabled,
                shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surfaceContainer) {
                Text(node.previewLabel.ifBlank { "查看广告" }, Modifier.fillMaxWidth().padding(spacing.contentGap),
                    style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
            }
            Text(node.title, style = MaterialTheme.typography.titleSmall)
            Text(node.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            AccountNodes(listOf(AccountNode.Actions(node.actions)), repository, enabled, onLink, onDirty, onSubmit, depth = 1)
        }
    }
}

@Composable
internal fun InvitationManagementCard(node: AccountNode.Invitation, repository: AccountManagementRepository,
    enabled: Boolean, onDirty: () -> Unit, onSubmit: (AccountForm, List<Pair<String, String>>) -> Unit) {
    val context = LocalContext.current
    val clipboard = remember(context) { context.getSystemService(ClipboardManager::class.java) }
    var copied by remember(node.link) { mutableStateOf(false) }
    var quantity by remember(node.form) { mutableStateOf(node.form?.fields?.firstOrNull { it.name == "remaining_count" }?.value.orEmpty()) }
    val target = quantity.toLongOrNull()?.times(node.cost.toLongOrNull() ?: 0L)
    AccountSectionCard("我的分享链接", node.description) {
        val spacing = LocalAppSpacing.current
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(spacing.item)) {
            Surface(Modifier.weight(1f), shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surfaceContainerHigh) {
                Text(node.link.ifBlank { "暂无邀请链接" }, Modifier.padding(spacing.contentGap), style = MaterialTheme.typography.bodyMedium)
            }
            FilledTonalButton(onClick = { clipboard?.setPrimaryClip(ClipData.newPlainText("邀请链接", node.link)); copied = true },
                enabled = node.link.isNotBlank()) { Text(if (copied) "已复制" else "复制") }
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(spacing.item), verticalArrangement = Arrangement.spacedBy(spacing.inline)) {
            TopicTagCapsule(node.status.ifBlank { "状态未知" }, color = MaterialTheme.colorScheme.secondaryContainer)
            TopicTagCapsule("剩余可用次数 ${node.remaining.ifBlank { "—" }}")
        }
        AccountStatistics(listOf("已托管积分" to node.escrow, "调整后托管" to (target?.toString() ?: "—")))
        node.form?.let { form -> NativeAccountForm(form, repository, enabled, onDirty, onSubmit, embedded = true,
            onValuesChange = { values -> quantity = values.firstOrNull { it.first == "remaining_count" }?.second.orEmpty() }) }
        if (node.rule.isNotBlank()) {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Text(node.rule, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
