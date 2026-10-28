package sb.linux.client.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import sb.linux.client.ui.common.content.TopicTocHeading
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.runtime.withFrameNanos
@Composable
internal fun BoxScope.TopicScrollControls(
    headings: List<TopicTocHeading>,
    bodyVisible: Boolean,
    readingProgress: Float,
    tocOpen: Boolean,
    onTocOpenChange: (Boolean) -> Unit,
    headingPositions: Map<Int, Float>,
    bodyTop: Float,
    contentTopInRoot: Float,
    topBarPx: Float,
    listState: LazyListState,
    scope: CoroutineScope,
    returnPosition: Pair<Int, Int>?,
    onReturnPositionChange: (Pair<Int, Int>?) -> Unit,
    currentFloor: Int,
    onFloorJump: () -> Unit,
) {
    if (headings.isNotEmpty() && bodyVisible) {
        Surface(
            onClick = { onTocOpenChange(true) },
            modifier = Modifier.align(Alignment.CenterEnd).padding(end = 3.dp).width(14.dp).height(150.dp),
            shape = RoundedCornerShape(50),
            color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.9f),
        ) {
            Box(Modifier.fillMaxSize().padding(4.dp)) {
                Box(
                    Modifier.fillMaxWidth()
                        .fillMaxHeight(readingProgress.coerceAtLeast(0.025f))
                        .align(Alignment.TopCenter)
                        .clip(RoundedCornerShape(50))
                        .background(MaterialTheme.colorScheme.primary)
                )
            }
        }
    }
    if (tocOpen) {
        AlertDialog(
            onDismissRequest = { onTocOpenChange(false) },
            title = { Text("文章目录") },
            text = {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 440.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    itemsIndexed(headings) { index, heading ->
                        Text(
                            heading.text,
                            style = when (heading.level) {
                                1 -> MaterialTheme.typography.titleMedium
                                2 -> MaterialTheme.typography.titleSmall
                                else -> MaterialTheme.typography.bodyMedium
                            },
                            fontWeight = if (heading.level <= 2) FontWeight.SemiBold else FontWeight.Normal,
                            modifier = Modifier.fillMaxWidth().clickable {
                                onTocOpenChange(false)
                                scope.launch {
                                    listState.scrollToItem(1)
                                    withFrameNanos { }
                                    withFrameNanos { }
                                    headingPositions[index]?.let { y ->
                                        listState.scrollBy(bodyTop + y - contentTopInRoot - topBarPx - 8f)
                                    }
                                }
                            }.padding(start = ((heading.level - 1).coerceAtLeast(0) * 14).dp, top = 9.dp, bottom = 9.dp),
                        )
                    }
                }
            },
            confirmButton = { TextButton(onClick = { onTocOpenChange(false) }) { Text("关闭") } },
        )
    }
    val showBackTop by remember(listState) {
        derivedStateOf { listState.firstVisibleItemIndex > 1 }
    }
    AnimatedVisibility(visible = showBackTop, modifier = Modifier.align(Alignment.BottomEnd), enter = scaleIn(), exit = scaleOut()) {
        SmallFloatingActionButton(
            onClick = {
                onReturnPositionChange(listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset)
                scope.launch { listState.animateScrollToItem(0) }
            },
            modifier = Modifier.padding(end = 16.dp, bottom = 16.dp),
            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.92f),
            contentColor = MaterialTheme.colorScheme.onSurface,
        ) { Icon(Icons.Filled.KeyboardArrowUp, "回到顶部") }
    }
    AnimatedVisibility(
        visible = FLOOR_PILL_ENABLED && currentFloor > 0,
        modifier = Modifier.align(Alignment.BottomEnd),
        enter = scaleIn(),
        exit = scaleOut(),
    ) {
        Surface(
            onClick = onFloorJump,
            shape = RoundedCornerShape(50),
            color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.92f),
            modifier = Modifier.padding(end = 16.dp, bottom = 76.dp),
        ) {
            Text(
                "#" + currentFloor,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            )
        }
    }
    AnimatedVisibility(
        visible = returnPosition != null,
        modifier = Modifier.align(Alignment.BottomCenter),
        enter = scaleIn(),
        exit = scaleOut(),
    ) {
        Row(
            Modifier.padding(bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SmallFloatingActionButton(
                onClick = {
                    returnPosition?.let { (index, offset) ->
                        scope.launch { listState.animateScrollToItem(index, offset) }
                    }
                    onReturnPositionChange(null)
                },
                containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.95f),
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            ) {
                Row(Modifier.padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(Icons.AutoMirrored.Filled.Chat, null, Modifier.size(15.dp))
                    Text("回到原位置", style = MaterialTheme.typography.labelMedium)
                }
            }
            SmallFloatingActionButton(
                onClick = { onReturnPositionChange(null) },
                containerColor = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.95f),
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            ) { Icon(Icons.Filled.Close, "关闭", Modifier.size(16.dp)) }
        }
    }
}
