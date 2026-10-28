package sb.linux.client.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import sb.linux.client.domain.topic.PrizeRow

private val PrizeTypeLabels = listOf(
    "points" to "积分", "wallet" to "烧饼", "code" to "兑换码", "manual" to "手工发奖",
)

@Composable
internal fun LotteryTopicFields(
    drawAt: String,
    onDrawAtChange: (String) -> Unit,
    participantTarget: String,
    onParticipantTargetChange: (String) -> Unit,
    minReplyChars: String,
    onMinReplyCharsChange: (String) -> Unit,
    replyCaptcha: Boolean,
    onReplyCaptchaChange: (Boolean) -> Unit,
    prizes: List<PrizeRow>,
    onPrizesChange: (List<PrizeRow>) -> Unit,
    prizeTypeMenu: Int?,
    onPrizeTypeMenuChange: (Int?) -> Unit,
) {    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                "用户回帖参与抽奖；发布抽奖至少需托管 5 个烧饼。",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            OutlinedTextField(
                value = drawAt, onValueChange = onDrawAtChange,
                label = { Text("开奖时间") },
                supportingText = { Text("格式 2026-08-25 20:00，最晚 3 天后") },
                singleLine = true, shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = participantTarget, onValueChange = onParticipantTargetChange,
                label = { Text("自动开奖人数") },
                supportingText = { Text("达到该参与人数后自动开奖，填 0 不启用") },
                singleLine = true, shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = minReplyChars, onValueChange = onMinReplyCharsChange,
                label = { Text("参与回复最少字数") },
                supportingText = { Text("去除空白后计算，低于门槛不能回复参与") },
                singleLine = true, shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            )
            sb.linux.client.ui.components.input.AppSwitchRow(
                title = "回帖需要验证码",
                subtitle = "默认开启，防止脚本批量回帖参与抽奖",
                checked = replyCaptcha,
                onCheckedChange = onReplyCaptchaChange,
                compact = true,
            )
            HorizontalDivider()
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("奖品设置", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                FilledTonalIconButton(onClick = { onPrizesChange(prizes + PrizeRow()) }) {
                    Icon(Icons.Filled.Add, "添加奖品")
                }
            }
            prizes.forEachIndexed { i, p ->
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                ) {
                    Column(
                        Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(
                                value = p.name,
                                onValueChange = { v ->
                                    onPrizesChange(prizes.toMutableList().also { it[i] = p.copy(name = v) })
                                },
                                label = { Text("奖品名称") },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = { onPrizesChange(prizes.filterIndexed { j, _ -> j != i }) }) {
                                Icon(Icons.Filled.Close, "删除奖品")
                            }
                        }
                        Box {
                            OutlinedButton(onClick = { onPrizeTypeMenuChange(i) }) {
                                Text("类型：${PrizeTypeLabels.firstOrNull { it.first == p.type }?.second ?: "积分"}")
                            }
                            DropdownMenu(
                                expanded = prizeTypeMenu == i,
                                onDismissRequest = { onPrizeTypeMenuChange(null) }
                            ) {
                                PrizeTypeLabels.forEach { (v, l) ->
                                    DropdownMenuItem(
                                        text = { Text(l) },
                                        onClick = {
                                            onPrizesChange(prizes.toMutableList().also { it[i] = p.copy(type = v) })
                                            onPrizeTypeMenuChange(null)
                                        }
                                    )
                                }
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = p.quantity,
                                onValueChange = { v ->
                                    onPrizesChange(prizes.toMutableList().also { it[i] = p.copy(quantity = v) })
                                },
                                label = { Text("份数") },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = p.value,
                                onValueChange = { v ->
                                    onPrizesChange(prizes.toMutableList().also { it[i] = p.copy(value = v) })
                                },
                                label = {
                                    Text(
                                        if (p.type == "points") "每份积分"
                                        else if (p.type == "wallet") "每份烧饼数"
                                        else if (p.type == "code") "兑换码（每行一个）"
                                        else "说明"
                                    )
                                },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1.4f)
                            )
                        }
                    }
                }
            }
        }
    }
}

