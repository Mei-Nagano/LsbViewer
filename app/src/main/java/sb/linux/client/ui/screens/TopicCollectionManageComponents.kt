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
import androidx.compose.foundation.shape.RoundedCornerShape
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
import sb.linux.client.common.collection.TopicCollectionFormField
import sb.linux.client.common.collection.TopicCollectionOperation
import sb.linux.client.ui.EmptyBox

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
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = 12.dp,
            bottom = 24.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding(),
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            ManageHeader(
                title = title,
                deleteAction = deleteAction,
                enabled = !submitting && !loading,
                onDelete = { deleteAction?.let { onSubmit(it, editedValues[formKey(it)].orEmpty()) } },
            )
        }
        if (loading) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
        error?.let { message -> item { ManageErrorBanner(message, onReload) } }
        if (actions.isEmpty()) {
            item { EmptyBox("源站没有提供可执行的管理操作") }
        } else {
            if (normal.isNotEmpty()) item { ManageSectionTitle("专辑设置与协作") }
            items(normal, key = { formKey(it) }) { form ->
                ManageActionCard(
                    form = form,
                    values = editedValues[formKey(form)].orEmpty(),
                    enabled = !submitting && !loading,
                    onValueChange = { name, value -> onValueChange(form, name, value) },
                    onSubmit = { onSubmit(form, editedValues[formKey(form)].orEmpty()) },
                )
            }
            if (contentRemovals.isNotEmpty()) item { ManageSectionTitle("收录内容") }
            items(contentRemovals, key = { formKey(it) }) { form ->
                ManageRemovalRow(form, !submitting && !loading) {
                    onSubmit(form, editedValues[formKey(form)].orEmpty())
                }
            }
            if (collaboratorRemovals.isNotEmpty()) item { ManageSectionTitle("协作者") }
            items(collaboratorRemovals, key = { formKey(it) }) { form ->
                ManageRemovalRow(form, !submitting && !loading) {
                    onSubmit(form, editedValues[formKey(form)].orEmpty())
                }
            }
            if (destructive.isNotEmpty()) item { ManageSectionTitle("危险操作") }
            items(destructive, key = { formKey(it) }) { form ->
                ManageActionCard(
                    form = form,
                    values = editedValues[formKey(form)].orEmpty(),
                    enabled = !submitting && !loading,
                    onValueChange = { name, value -> onValueChange(form, name, value) },
                    onSubmit = { onSubmit(form, editedValues[formKey(form)].orEmpty()) },
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
        shape = RoundedCornerShape(14.dp),
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
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            Modifier.padding(horizontal = 16.dp, vertical = 15.dp),
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
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.errorContainer,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            Modifier.padding(start = 14.dp, end = 6.dp, top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onReload) { Text("重试") }
        }
    }
}

@Composable
private fun ManageActionCard(
    form: TopicCollectionActionForm,
    values: Map<String, String>,
    enabled: Boolean,
    onValueChange: (String, String) -> Unit,
    onSubmit: () -> Unit,
) {
    val destructive = form.isDestructive()
    val borderColor = if (destructive) MaterialTheme.colorScheme.error.copy(alpha = .45f)
    else MaterialTheme.colorScheme.outlineVariant.copy(alpha = .65f)
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = if (destructive) MaterialTheme.colorScheme.errorContainer.copy(alpha = .22f)
        else MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, borderColor),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Surface(
                    shape = CircleShape,
                    color = if (destructive) MaterialTheme.colorScheme.errorContainer
                    else MaterialTheme.colorScheme.secondaryContainer,
                ) {
                    Icon(
                        form.operation.icon(),
                        contentDescription = null,
                        tint = if (destructive) MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(9.dp).size(20.dp),
                    )
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(form.label, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Text(
                        form.operation.description(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            form.controls.forEach { control ->
                ManageField(control, values[control.name].orEmpty(), enabled, onValueChange)
            }
            if (destructive) {
                Text(
                    "请谨慎操作，源站可能无法恢复。",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error,
                )
                Button(
                    enabled = enabled && form.enabled,
                    onClick = onSubmit,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError,
                    ),
                ) { Text(form.label) }
            } else {
                FilledTonalButton(enabled = enabled && form.enabled, onClick = onSubmit) { Text(form.label) }
            }
        }
    }
}

@Composable
private fun ManageField(
    control: TopicCollectionFormField,
    value: String,
    enabled: Boolean,
    onValueChange: (String, String) -> Unit,
) {
    if (control.type.equals("checkbox", true)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Checkbox(
                checked = value.isNotBlank(),
                onCheckedChange = { onValueChange(control.name, if (it) "1" else "") },
                enabled = enabled,
            )
            Text(
                if (control.required) "${control.label} *" else control.label,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(start = 4.dp),
            )
        }
    } else {
        OutlinedTextField(
            value = value,
            onValueChange = { onValueChange(control.name, it) },
            enabled = enabled,
            label = { Text(if (control.required) "${control.label} *" else control.label) },
            singleLine = !control.type.equals("textarea", true),
            minLines = if (control.type.equals("textarea", true)) 3 else 1,
            maxLines = if (control.type.equals("textarea", true)) 6 else 1,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

internal fun TopicCollectionActionForm.isDestructive(): Boolean = operation in setOf(
    TopicCollectionOperation.DELETE,
    TopicCollectionOperation.REMOVE_ITEM,
    TopicCollectionOperation.REMOVE_ALL_ITEMS,
    TopicCollectionOperation.REMOVE_COLLABORATOR,
)

private fun TopicCollectionActionForm.isCompactRemoval(operation: TopicCollectionOperation): Boolean =
    this.operation == operation && controls.isEmpty()

private fun TopicCollectionOperation.icon(): ImageVector = when (this) {
    TopicCollectionOperation.DELETE,
    TopicCollectionOperation.REMOVE_ITEM,
    TopicCollectionOperation.REMOVE_ALL_ITEMS -> Icons.Filled.Delete
    TopicCollectionOperation.UPDATE -> Icons.Filled.Edit
    TopicCollectionOperation.ADD_COLLABORATOR -> Icons.Filled.GroupAdd
    TopicCollectionOperation.REMOVE_COLLABORATOR -> Icons.Filled.PersonRemove
    else -> Icons.Filled.Tune
}

private fun TopicCollectionOperation.description(): String = when (this) {
    TopicCollectionOperation.UPDATE -> "编辑专辑信息"
    TopicCollectionOperation.ADD_COLLABORATOR -> "添加或管理协作者"
    TopicCollectionOperation.REMOVE_COLLABORATOR -> "移除协作者"
    TopicCollectionOperation.DELETE -> "删除整张专辑"
    TopicCollectionOperation.REMOVE_ITEM,
    TopicCollectionOperation.REMOVE_ALL_ITEMS -> "从专辑中移除收录内容"
    else -> "源站提供的专辑操作"
}

internal fun TopicCollectionActionForm.requiredFieldMissing(values: Map<String, String>): String? =
    controls.firstOrNull { it.required && values[it.name].orEmpty().isBlank() }?.label

internal fun formKey(form: TopicCollectionActionForm): String =
    "${form.action}:${form.label}:${form.fields.hashCode()}"

/** 只替换可编辑字段；未勾选的 checkbox/radio 不应提交空字段。 */
internal fun TopicCollectionActionForm.withEditedValues(values: Map<String, String>): TopicCollectionActionForm {
    if (values.isEmpty()) return this
    val controlNames = controls.map { it.name }.toSet()
    val editedFields = controls.mapNotNull { control ->
        val value = values[control.name] ?: return@mapNotNull null
        if (control.type.equals("checkbox", true) || control.type.equals("radio", true)) {
            value.takeIf { it.isNotBlank() }?.let { control.name to it }
        } else {
            control.name to value
        }
    }
    return copy(fields = fields.filterNot { it.first in controlNames } + editedFields)
}
