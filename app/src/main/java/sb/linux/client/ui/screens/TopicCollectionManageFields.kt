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

@Composable
internal fun ManageActionCard(
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

internal fun TopicCollectionActionForm.isCompactRemoval(operation: TopicCollectionOperation): Boolean =
    this.operation == operation && controls.isEmpty()

internal fun TopicCollectionOperation.icon(): ImageVector = when (this) {
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
