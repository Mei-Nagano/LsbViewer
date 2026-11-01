package sb.linux.client.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.CircleShape
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
import sb.linux.client.ui.*
import sb.linux.client.ui.theme.*
import sb.linux.client.ui.content.*
import sb.linux.client.ui.patterns.topic.*
import sb.linux.client.ui.patterns.user.*
import sb.linux.client.ui.components.feedback.*
import sb.linux.client.ui.components.layout.*
import sb.linux.client.ui.components.navigation.*

/** AI 总结卡片（置顶显示）：未总结时是一颗手动触发按钮，出结果后按 Markdown 渲染 */
@Composable
fun AiSummaryCard(
    summary: String?,
    busy: Boolean,
    error: String?,
    enabled: Boolean,
    onRun: () -> Unit,
    onRegenerate: () -> Unit,
    onDismiss: () -> Unit,
) {
    if (busy || summary != null || error != null) {
        Surface(
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.secondaryContainer,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Column(Modifier.padding(LocalAppSpacing.current.pageHorizontal)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .size(26.dp)
                            .clip(MaterialTheme.shapes.small)
                            .background(MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.AutoAwesome, null, Modifier.size(15.dp), tint = MaterialTheme.colorScheme.onSecondaryContainer)
                    }
                    Spacer(Modifier.width(8.dp))
                    Text("AI 总结", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSecondaryContainer)
                    Spacer(Modifier.weight(1f))
                    // 重新总结：结果按帖缓存，没这个入口的话同一帖再也换不出新总结
                    if (!busy && summary != null) {
                        IconButton(onClick = onRegenerate, modifier = Modifier.size(32.dp)) {
                            Icon(
                                Icons.Filled.Refresh, "重新总结",
                                Modifier.size(17.dp),
                                tint = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                    TextButton(onClick = onDismiss) { Text("关闭") }
                }
                Spacer(Modifier.height(6.dp))
                when {
                    busy -> Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AppInlineLoading(size = 15.dp)
                        Text("正在总结…", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSecondaryContainer)
                    }
                    error != null -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(error, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                        FilledTonalButton(
                            onClick = onRegenerate,
                            enabled = enabled,
                            shape = CircleShape
                        ) { Text("重试") }
                    }
                    // 模型返回的是 Markdown：直接 Text 会把 ## 和 ** 记号原样显示出来
                    summary != null -> MarkdownText(
                        summary,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                }
            }
        }
    } else {
        FilledTonalButton(
            onClick = onRun,
            enabled = enabled,
            shape = CircleShape,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.dp)
        ) {
            Icon(Icons.Filled.AutoAwesome, null, Modifier.size(15.dp))
            Spacer(Modifier.width(6.dp))
            Text(if (enabled) "AI 总结本帖" else "AI 未配置")
        }
    }
}

/** 发卡帖兑换卡片 */
