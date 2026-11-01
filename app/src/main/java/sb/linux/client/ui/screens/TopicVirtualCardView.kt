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
import sb.linux.client.domain.topic.VirtualCard
import sb.linux.client.ui.*
import sb.linux.client.ui.theme.*
import sb.linux.client.ui.content.*
import sb.linux.client.ui.patterns.topic.*
import sb.linux.client.ui.patterns.user.*
import sb.linux.client.ui.components.feedback.*
import sb.linux.client.ui.components.layout.*
import sb.linux.client.ui.components.navigation.*
@Composable
fun VirtualCardView(vc: VirtualCard, loggedIn: Boolean, onBuy: () -> Unit) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = if (vc.inStock) MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.8f)
        else MaterialTheme.colorScheme.surfaceContainerHighest,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = LocalAppSpacing.current.pageHorizontal, vertical = 6.dp)
    ) {
        Column(
            Modifier.padding(LocalAppSpacing.current.pageHorizontal),
            verticalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(vc.kicker, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onTertiaryContainer)
                    Text(vc.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                Badge(vc.status, MaterialTheme.colorScheme.surfaceContainerHigh, MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text("${vc.priceText}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            if (vc.footText.isNotBlank()) {
                Text(vc.footText, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onTertiaryContainer)
            }
            Text(
                vc.tip.ifBlank { if (loggedIn) "" else "登录后可使用积分兑换" },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onTertiaryContainer
            )
            // 我的购买记录：兑换码用代码块单独成行展示，方便一键复制
            if (vc.orders.isNotEmpty()) {
                Text("我的购买记录", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                vc.orders.forEach { o ->
                    Column {
                        CodeBlockView(o.code)
                        val meta = listOf(o.time, o.price).filter { it.isNotBlank() }
                        if (meta.isNotEmpty()) {
                            Text(
                                meta.joinToString(" · "),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        }
                    }
                }
            }
            if (vc.buyAction.isNotBlank() && vc.inStock) {
                Button(
                    onClick = onBuy,
                    enabled = loggedIn,
                    shape = CircleShape,
                    modifier = Modifier.fillMaxWidth()
                ) { Text(if (loggedIn) "确认兑换" else "登录后兑换") }
            } else if (!vc.inStock) {
                Text(
                    "已发完 / 暂不可兑换",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }
        }
    }
}

// ==================== 弹窗 ====================
