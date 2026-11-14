package sb.linux.client.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import sb.linux.client.domain.account.AccountField
import java.time.*
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SourceDateTimeField(field: AccountField, value: String, enabled: Boolean, onChange: (String) -> Unit) {
    var stage by remember { mutableIntStateOf(0) }
    val initial = runCatching { LocalDateTime.parse(value) }.getOrNull() ?: LocalDateTime.now()
    var selectedDate by remember { mutableStateOf(initial.toLocalDate()) }
    val date = rememberDatePickerState(initialSelectedDateMillis = initial.toLocalDate().atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli())
    val time = rememberTimePickerState(initialHour = initial.hour, initialMinute = initial.minute, is24Hour = true)
    Column {
        OutlinedTextField(value, onChange, enabled = enabled, label = { Text(field.label) }, singleLine = true,
            supportingText = { Text(listOf(field.min.takeIf(String::isNotBlank)?.let { "最早 $it" },
                field.max.takeIf(String::isNotBlank)?.let { "最晚 $it" }).filterNotNull().joinToString(" · ")) }, modifier = Modifier.fillMaxWidth())
        TextButton(onClick = { stage = 1 }, enabled = enabled) { Text("选择日期与时间") }
    }
    if (stage == 1) DatePickerDialog(onDismissRequest = { stage = 0 }, confirmButton = {
        TextButton(onClick = {
            date.selectedDateMillis?.let { selectedDate = Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate(); stage = 2 }
        }) { Text("选择时间") }
    }, dismissButton = { TextButton(onClick = { stage = 0 }) { Text("取消") } }) { DatePicker(date) }
    if (stage == 2) AlertDialog(onDismissRequest = { stage = 0 }, title = { Text("选择时间") }, text = { TimePicker(time) },
        confirmButton = { TextButton(onClick = {
            onChange(selectedDate.atTime(time.hour, time.minute).format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm")))
            stage = 0
        }) { Text("确定") } }, dismissButton = { TextButton(onClick = { stage = 0 }) { Text("取消") } })
}
