package sb.linux.client.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.PersonRemove
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import sb.linux.client.common.collection.TopicCollectionActionForm
import sb.linux.client.common.collection.key
import sb.linux.client.common.collection.TopicCollectionFormField
import sb.linux.client.common.collection.TopicCollectionOperation
import sb.linux.client.ui.components.feedback.EmptyBox

/** 淘帖管理页的内容编排：设置、协作和危险操作分组，避免主屏幕承担展示细节。 */
@OptIn(ExperimentalMaterial3Api::class)
internal @Composable
fun ManageActionList(
    title: String,
    actions: List<TopicCollectionActionForm>,
    loading: Boolean,
    submitting: Boolean,
    error: String?,
    editedValues: Map<String, Map<String, String>>,
    onValueChange: (TopicCollectionActionForm, String, String) -> Unit,
    onSubmit: (TopicCollectionActionForm, Map<String, String>) -> Unit,
    onReload: () -> Unit,
) {
    val deleteAction = actions.firstOrNull { it.operation == TopicCollectionOperation.DELETE }
    val normal = actions.filterNot { it.isDestructive() }
    val contentRemovals = actions.filter { it.isCompactRemoval(TopicCollectionOperation.REMOVE_ITEM) }
    val collaboratorRemovals = actions.filter { it.isCompactRemoval(TopicCollectionOperation.REMOVE_COLLABORATOR) }
    val destructive = actions.filter {
        it.isDestructive() && it != deleteAction && it !in contentRemovals && it !in collaboratorRemovals
    }
    val spacing = sb.linux.client.ui.theme.LocalAppSpacing.current
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = spacing.pageHorizontal,
            end = spacing.pageHorizontal,
            top = spacing.contentGap,
            bottom = spacing.section + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding(),
        ),
        verticalArrangement = Arrangement.spacedBy(spacing.contentGap),
    ) {
        item {
            ManageHeader(
                title = title,
                deleteAction = deleteAction,
                enabled = !submitting && !loading,
                onDelete = { deleteAction?.let { onSubmit(it, editedValues[it.key()].orEmpty()) } },
            )
        }
        if (loading) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
        error?.let { message -> item { ManageErrorBanner(message, onReload) } }
        if (actions.isEmpty()) {
            item { EmptyBox("源站没有提供可执行的管理操作") }
        } else {
            if (normal.isNotEmpty()) item { ManageSectionTitle("专辑设置与协作") }
            items(normal, key = { it.key() }) { form ->
                ManageActionCard(
                    form = form,
                    values = editedValues[form.key()].orEmpty(),
                    enabled = !submitting && !loading,
                    onValueChange = { name, value -> onValueChange(form, name, value) },
                    onSubmit = { onSubmit(form, editedValues[form.key()].orEmpty()) },
                )
            }
            if (contentRemovals.isNotEmpty()) item { ManageSectionTitle("收录内容") }
            items(contentRemovals, key = { it.key() }) { form ->
                ManageRemovalRow(form, !submitting && !loading) {
                    onSubmit(form, editedValues[form.key()].orEmpty())
                }
            }
            if (collaboratorRemovals.isNotEmpty()) item { ManageSectionTitle("协作者") }
            items(collaboratorRemovals, key = { it.key() }) { form ->
                ManageRemovalRow(form, !submitting && !loading) {
                    onSubmit(form, editedValues[form.key()].orEmpty())
                }
            }
            if (destructive.isNotEmpty()) item { ManageSectionTitle("危险操作") }
            items(destructive, key = { it.key() }) { form ->
                ManageActionCard(
                    form = form,
                    values = editedValues[form.key()].orEmpty(),
                    enabled = !submitting && !loading,
                    onValueChange = { name, value -> onValueChange(form, name, value) },
                    onSubmit = { onSubmit(form, editedValues[form.key()].orEmpty()) },
                )
            }
        }
    }
}

/** 管理页的逐项移除保持单行：目标在左、操作在右，避免每条内容占一张大卡片。 */
@Composable
private fun ManageRemovalRow(
    form: TopicCollectionActionForm,
    enabled: Boolean,
    onSubmit: () -> Unit,
) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = .65f)),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            Modifier.padding(start = 12.dp, end = 4.dp, top = 5.dp, bottom = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                form.operation.icon(),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(18.dp),
            )
            Column(Modifier.weight(1f)) {
                Text(
                    form.targetLabel.ifBlank { "目标信息未提供" },
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    if (form.operation == TopicCollectionOperation.REMOVE_ITEM) "收录内容" else "协作者",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            TextButton(enabled = enabled && form.enabled, onClick = onSubmit) {
                Text("移除", color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
private fun ManageHeader(
    title: String,
    deleteAction: TopicCollectionActionForm?,
    enabled: Boolean,
    onDelete: () -> Unit,
) {
    val spacing = sb.linux.client.ui.theme.LocalAppSpacing.current
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.primaryContainer,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            Modifier.padding(horizontal = spacing.pageHorizontal, vertical = 15.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primary) {
                Icon(
                    Icons.Filled.Tune,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.padding(10.dp).size(24.dp),
                )
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text("管理专辑", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(title, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
            }
            deleteAction?.let { form ->
                TextButton(enabled = enabled && form.enabled, onClick = onDelete) {
                    Icon(
                        Icons.Filled.Delete,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(17.dp),
                    )
                    Text("删除", color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable
private fun ManageSectionTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 4.dp, top = 4.dp),
    )
}

@Composable
private fun ManageErrorBanner(message: String, onReload: () -> Unit) {
    sb.linux.client.ui.components.feedback.AppNotice(message, onAction = onReload)
}
