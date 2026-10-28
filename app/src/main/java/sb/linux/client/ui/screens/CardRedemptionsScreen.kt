package sb.linux.client.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import sb.linux.client.domain.gacha.CardRedemptionRecord
import sb.linux.client.ui.feature.gacha.CardRedemptionsViewModel
import sb.linux.client.ui.feature.gacha.CardRedemptionsViewModelFactory
import sb.linux.client.ui.session.Session
import java.text.SimpleDateFormat
import java.util.Date

/**
 * 发卡兑换记录：兑换码默认隐藏，按需查看/复制；空码显示“未记录兑换码”，不展示空白行。
 * 清空只删除本机记录，不改变源站兑换结果。
 */
@Composable
fun CardRedemptionsScreen(session: Session, nav: NavHostController) {
    val factory = remember(session.cardRedemptionsRepository) {
        CardRedemptionsViewModelFactory(session.cardRedemptionsRepository)
    }
    val model: CardRedemptionsViewModel = viewModel(factory = factory)
    LaunchedEffect(model) { model.refresh() }
    val records = model.records
    val context = LocalContext.current
    val clipboard = remember(context) { context.getSystemService(ClipboardManager::class.java) }
    var revealed by remember { mutableStateOf<Set<Long>>(emptySet()) }
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault()) }

    SettingsPage("发卡兑换记录", nav) {
        if (records.isEmpty()) {
            GroupCard {
                Text(
                    "暂无兑换记录\n打开已兑换的发卡帖后会自动保存；游客也能查看本机记录。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(18.dp),
                )
            }
        } else {
            GroupCard {
                records.forEachIndexed { index, record ->
                    RedemptionRow(
                        record = record,
                        revealed = record.recordedAt in revealed,
                        dateText = dateFormat.format(Date(record.recordedAt)),
                        onToggleReveal = {
                            revealed = if (record.recordedAt in revealed) revealed - record.recordedAt
                            else revealed + record.recordedAt
                        },
                        onCopy = {
                            clipboard?.setPrimaryClip(ClipData.newPlainText("兑换码", record.code))
                            session.showToast("兑换码已复制")
                        },
                        onOpenTopic = { nav.navigate("topic/${record.topicId}") },
                    )
                    if (index != records.lastIndex) {
                        HorizontalDivider(
                            Modifier.padding(horizontal = 18.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                        )
                    }
                }
            }
            SettingsResetRow(
                "清空兑换记录",
                "只删除本机兑换记录，不影响源站兑换结果；此操作无法撤销。",
                model::clear,
            )
        }
    }
}

@Composable
private fun RedemptionRow(
    record: CardRedemptionRecord,
    revealed: Boolean,
    dateText: String,
    onToggleReveal: () -> Unit,
    onCopy: () -> Unit,
    onOpenTopic: () -> Unit,
) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 12.dp)) {
        Text(
            record.topicTitle.ifBlank { "帖子 #${record.topicId}" },
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.fillMaxWidth().clickable(onClick = onOpenTopic).padding(vertical = 2.dp),
        )
        if (record.cardTitle.isNotBlank()) {
            Text(record.cardTitle, style = MaterialTheme.typography.bodyMedium)
        }
        val meta = listOf(record.price, record.sourceTime).filter { it.isNotBlank() }.joinToString(" · ")
        if (meta.isNotBlank()) {
            Text(
                meta,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        if (record.code.isBlank()) {
            Text(
                "未记录兑换码",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 6.dp),
            )
        } else {
            Text(
                if (revealed) record.code else maskCode(record.code),
                style = MaterialTheme.typography.bodyLarge,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(top = 6.dp),
            )
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = onToggleReveal) { Text(if (revealed) "隐藏" else "查看") }
                TextButton(onClick = onCopy) { Text("复制") }
            }
        }
        Text(
            "记录于 $dateText",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

/** 只保留首尾两字符；短码整体隐藏，避免从长度推断内容。 */
private fun maskCode(code: String): String = if (code.length <= 6) "••••••"
else code.take(2) + "••••" + code.takeLast(2)
