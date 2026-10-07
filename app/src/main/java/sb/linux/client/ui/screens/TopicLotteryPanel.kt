package sb.linux.client.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.foundation.text.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.*
import androidx.compose.ui.text.style.*
import androidx.compose.ui.unit.*
import kotlinx.coroutines.*
import sb.linux.client.domain.topic.LotteryPanel
import sb.linux.client.ui.*
/** 抽奖帖面板：状态 / 奖品 / 条件 / 中奖名单（已结束默认折叠，自己中奖高亮）/ 参与方式 */
@Composable
fun LotteryPanelView(
    lp: LotteryPanel,
    loggedIn: Boolean,
    onJoin: () -> Unit,
    onJoinForm: (String) -> Unit,
    selfName: String = "",
) {
    val drawn = lp.status.contains("开奖") || lp.result.isNotBlank() || lp.winners.isNotEmpty()
    // 已开奖：默认折叠中奖名单；自己中奖则默认展开并高亮
    val selfWon = selfName.isNotBlank() && lp.winners.any { it.user.startsWith(selfName) }
    var expanded by remember(lp) { mutableStateOf(!drawn || selfWon) }
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = if (drawn) MaterialTheme.colorScheme.surfaceContainerHighest
        else MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("抽奖帖", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    if (lp.note.isNotBlank()) {
                        Text(lp.note, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    Badge(
                        lp.status.ifBlank { if (drawn) "已开奖" else "抽奖中" },
                        if (drawn) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.primary,
                        if (drawn) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onPrimary,
                    )
                    if (lp.participants.isNotBlank()) {
                        Spacer(Modifier.height(3.dp))
                        Text(
                            lp.participants, style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            if (lp.prizes.isNotEmpty()) {
                Text("奖品", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                lp.prizes.forEach { p ->
                    Text("· $p", style = MaterialTheme.typography.bodySmall)
                }
            }
            if (lp.condition.isNotBlank()) {
                Text(lp.condition, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
            }
            if (lp.result.isNotBlank() || lp.winners.isNotEmpty()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { expanded = !expanded }
                        .padding(vertical = 2.dp)
                ) {
                    Text("开奖结果", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    if (selfWon) {
                        Spacer(Modifier.width(6.dp))
                        Badge("恭喜你中奖了", MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.onPrimary)
                    }
                    Spacer(Modifier.weight(1f))
                    Text(
                        if (expanded) "收起" else "展开",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                if (expanded) {
                    if (lp.result.isNotBlank()) Text(lp.result, style = MaterialTheme.typography.bodySmall)
                    lp.winners.forEach { w ->
                        val isSelf = selfName.isNotBlank() && w.user.startsWith(selfName)
                        Text(
                            "· ${w.user}${if (w.prize.isNotBlank()) " · ${w.prize}" else ""}",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isSelf) MaterialTheme.colorScheme.primary else Color.Unspecified,
                            fontWeight = if (isSelf) FontWeight.Bold else FontWeight.Normal,
                        )
                        // 兑换码（仅本人可见）：用代码块单独成行展示，方便一键复制
                        if (w.code.isNotBlank()) {
                            Spacer(Modifier.height(4.dp))
                            CodeBlockView(w.code)
                        }
                    }
                }
            }
            lp.joinedText.takeIf { it.isNotBlank() }?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
            }
            // 参与：源站规则为回复参与；存在表单选项时按选项提交
            if (!drawn) {
                if (lp.joinAction.isNotBlank() && lp.options.isNotEmpty()) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        lp.options.forEach { (v, label) ->
                            FilledTonalButton(onClick = { onJoinForm(v) }, enabled = loggedIn) { Text(label) }
                        }
                    }
                } else if (lp.joinAction.isNotBlank()) {
                    Button(onClick = { onJoinForm("") }, enabled = loggedIn, modifier = Modifier.fillMaxWidth()) {
                        Text(if (loggedIn) "参与抽奖" else "登录后参与")
                    }
                } else {
                    Button(onClick = onJoin, enabled = loggedIn, modifier = Modifier.fillMaxWidth()) {
                        Text(if (loggedIn) "回复参与抽奖" else "登录后参与")
                    }
                }
            }
        }
    }
}
