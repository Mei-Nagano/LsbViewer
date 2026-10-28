package sb.linux.client.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

private val CardCurrencyLabels = listOf("points" to "积分", "wallet" to "烧饼")

@Composable
internal fun VirtualCardTopicFields(
    cardName: String,
    onCardNameChange: (String) -> Unit,
    cardCurrency: String,
    onCardCurrencyChange: (String) -> Unit,
    currencyMenu: Boolean,
    onCurrencyMenuChange: (Boolean) -> Unit,
    cardPrice: String,
    onCardPriceChange: (String) -> Unit,
    cardLimit: String,
    onCardLimitChange: (String) -> Unit,
    cardAutoReply: Boolean,
    onCardAutoReplyChange: (Boolean) -> Unit,
    cardAutoReplyContent: String,
    onCardAutoReplyContentChange: (String) -> Unit,
    cardValues: String,
    onCardValuesChange: (String) -> Unit,
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
                "用户用积分或烧饼兑换卡密；编辑帖子时卡密列表只显示未售出的卡。",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            OutlinedTextField(
                value = cardName, onValueChange = onCardNameChange,
                label = { Text("卡片名称") },
                supportingText = { Text("例如：月卡兑换码、软件激活码") },
                singleLine = true, shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            )
            Box {
                OutlinedButton(onClick = { onCurrencyMenuChange(true) }) {
                    Text("积分种类：${CardCurrencyLabels.firstOrNull { it.first == cardCurrency }?.second ?: "积分"}")
                }
                DropdownMenu(expanded = currencyMenu, onDismissRequest = { onCurrencyMenuChange(false) }) {
                    CardCurrencyLabels.forEach { (v, l) ->
                        DropdownMenuItem(
                            text = { Text(l) },
                            onClick = { onCardCurrencyChange(v); onCurrencyMenuChange(false) }
                        )
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = cardPrice, onValueChange = onCardPriceChange,
                    label = { Text("兑换价格") },
                    singleLine = true, shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = cardLimit, onValueChange = onCardLimitChange,
                    label = { Text("每人限购") },
                    supportingText = { Text("0 为不限制") },
                    singleLine = true, shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.weight(1f)
                )
            }
            sb.linux.client.ui.components.input.AppSwitchRow(
                title = "购买成功后自动回帖",
                subtitle = "开启后，买家兑换成功会自动回复本帖",
                checked = cardAutoReply,
                onCheckedChange = onCardAutoReplyChange,
                compact = true,
            )
            if (cardAutoReply) {
                OutlinedTextField(
                    value = cardAutoReplyContent,
                    onValueChange = onCardAutoReplyContentChange,
                    label = { Text("自动回帖内容") },
                    supportingText = { Text("支持变量 {card_name} {price} {username}") },
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }
            OutlinedTextField(
                value = cardValues, onValueChange = onCardValuesChange,
                label = { Text("添加虚拟卡") },
                supportingText = { Text("每行一张卡，最多 5000 张") },
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 120.dp)
            )
        }
    }
}

