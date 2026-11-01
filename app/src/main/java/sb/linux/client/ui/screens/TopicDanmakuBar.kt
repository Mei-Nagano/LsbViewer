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
import sb.linux.client.domain.topic.DanmakuItem
import sb.linux.client.ui.*
import sb.linux.client.ui.theme.*
import sb.linux.client.ui.content.*
import sb.linux.client.ui.patterns.topic.*
import sb.linux.client.ui.patterns.user.*
import sb.linux.client.ui.components.feedback.*
import sb.linux.client.ui.components.layout.*
import sb.linux.client.ui.components.navigation.*
/** 打赏弹幕条：横向自动滚动展示最近打赏；切换时把当前展示的弹幕滚动为可见区第一条 */
@Composable
fun DanmakuBar(items: List<DanmakuItem>) {
    val rowState = rememberLazyListState()
    LaunchedEffect(items) {
        var target = 0
        while (true) {
            if (items.isNotEmpty()) {
                // target 即当前应展示的弹幕下标：滚动到其位置（成为可见区第一条）
                // 若该弹幕较长，滚动距离随内容长度自然增加，确保其完整可见
                rowState.animateScrollToItem(target)
                target = (target + 1) % items.size
                // 若剩余弹幕不足以铺满可视区，回到开头
                val first = rowState.layoutInfo.visibleItemsInfo.firstOrNull()?.index ?: 0
                if (items.size - first <= 2) target = 0
            }
            delay(2600)
        }
    }
    LazyRow(
        state = rowState,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        contentPadding = PaddingValues(horizontal = LocalAppSpacing.current.pageHorizontal),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        itemsIndexed(items, key = { i, d -> "$i-${d.user}-${d.amount}" }) { _, d ->
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.75f)
            ) {
                Row(
                    Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Avatar(d.avatarUrl, 15)
                    Text(
                        "${d.user} ${d.action} ${d.amount}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                        maxLines = 1
                    )
                }
            }
        }
    }
}
