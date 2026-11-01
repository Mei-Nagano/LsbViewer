package sb.linux.client.ui.content

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import sb.linux.client.ui.LocalTableDisplayMode



@Composable
internal fun TableView(
    rows: List<List<TableCellData>>,
    onOpenUrl: (String) -> Unit,
    onFloor: (Int) -> Unit = {},
    highlightQuery: String = "",
    onSearchPosition: (Float) -> Unit = {},
    onAnchor: (String) -> Unit = {},
    onImageClick: (List<String>, Int) -> Unit = { _, _ -> },
) {
    val colCount = rows.maxOf { it.size }
    val horizontal = LocalTableDisplayMode.current == 1
    val tableWidth = (colCount * 160 + 12).dp
    val firstMatchCell = rows.flatten().firstOrNull {
        highlightQuery.isNotBlank() && it.text.contains(highlightQuery.trim(), ignoreCase = true)
    }
    val horizontalState = rememberScrollState()
    val density = LocalDensity.current
    LaunchedEffect(highlightQuery, horizontal) {
        if (horizontal && firstMatchCell != null) {
            val column = rows.first { firstMatchCell in it }.indexOf(firstMatchCell)
            horizontalState.scrollTo(with(density) { (column * 160).dp.roundToPx() })
        }
    }
    Column(Modifier.fillMaxWidth()) {
    Box(Modifier.fillMaxWidth().then(if (horizontal) Modifier.horizontalScroll(horizontalState) else Modifier)) {
      Surface(
          shape = MaterialTheme.shapes.medium,
          color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.5f),
          modifier = Modifier.padding(vertical = 4.dp).then(if (horizontal) Modifier.width(tableWidth) else Modifier.fillMaxWidth())
      ) {
        Column(Modifier.fillMaxWidth().padding(6.dp)) {
            rows.forEachIndexed { rIdx, row ->
                // 首个含表头的行视为表头行，整行加背景
                val isHeaderRow = row.any { it.isHeader }
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(MaterialTheme.shapes.small)
                        .background(
                            if (isHeaderRow) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                            else Color.Transparent
                        )
                ) {
                    repeat(colCount) { cIdx ->
                        val cell = row.getOrNull(cIdx)
                        Box(
                            Modifier
                                .weight(1f)
                                .padding(horizontal = 5.dp, vertical = 4.dp)
                        ) {
                            val c = cell
                            if (c != null && (c.text.isNotBlank() || c.imageUrls.isNotEmpty())) {
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                if (c.annotated != null && c.text.isNotBlank()) {
                                    // 富文本单元格：链接可点击（颜色已烘焙进注解 SpanStyle）
                                    LinkText(
                                        text = addSearchHighlights(c.annotated, highlightQuery, MaterialTheme.colorScheme.tertiaryContainer),
                                        onOpenUrl = onOpenUrl,
                                        onFloor = onFloor,
                                        onAnchor = onAnchor,
                                        style = MaterialTheme.typography.bodySmall.copy(textAlign = c.textAlign ?: TextAlign.Start),
                                        fontWeight = if (c.isHeader) FontWeight.Bold else null,
                                        searchQuery = highlightQuery,
                                        onSearchPosition = { y -> if (c === firstMatchCell) onSearchPosition(y) },
                                        modifier = Modifier.fillMaxWidth(),
                                    )
                                } else if (c.text.isNotBlank()) {
                                    Text(
                                        addSearchHighlights(AnnotatedString(c.text), highlightQuery, MaterialTheme.colorScheme.tertiaryContainer),
                                        style = MaterialTheme.typography.bodySmall.copy(textAlign = c.textAlign ?: TextAlign.Start),
                                        fontWeight = if (c.isHeader) FontWeight.Bold else null,
                                        color = if (c.isHeader) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.fillMaxWidth(),
                                    )
                                }
                                c.imageUrls.forEachIndexed { imageIndex, imageUrl ->
                                    BodyImage(
                                        url = imageUrl,
                                        modifier = Modifier.fillMaxWidth(),
                                        onClick = { onImageClick(c.imageUrls, imageIndex) },
                                    )
                                }
                                }
                            }
                        }
                    }
                }
                if (rIdx < rows.lastIndex) {
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 2.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                    )
                }
            }
        }
      }
    }
    if (horizontal && horizontalState.maxValue > 0) {
        BoxWithConstraints(
            Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 3.dp)
        ) {
            val viewportFraction = (maxWidth / tableWidth).coerceIn(0.12f, 1f)
            val thumbWidth = maxWidth * viewportFraction
            val travel = maxWidth - thumbWidth
            val progress = horizontalState.value.toFloat() / horizontalState.maxValue.toFloat()
            val travelPx = with(LocalDensity.current) { travel.toPx() }.coerceAtLeast(1f)
            Box(
                Modifier.fillMaxWidth().height(6.dp).clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            )
            Box(
                Modifier.offset(x = travel * progress).width(thumbWidth).height(6.dp)
                    .clip(CircleShape).background(MaterialTheme.colorScheme.primary)
                    .pointerInput(horizontalState.maxValue, travelPx) {
                        detectHorizontalDragGestures { change, dragAmount ->
                            change.consume()
                            horizontalState.dispatchRawDelta(
                                dragAmount * horizontalState.maxValue.toFloat() / travelPx
                            )
                        }
                    }
            )
        }
    }
    }
}

/** 在不破坏原有粗体、链接和行内代码 Span 的前提下叠加搜索命中背景。 */
