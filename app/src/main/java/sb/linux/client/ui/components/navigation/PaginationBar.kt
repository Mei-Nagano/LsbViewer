package sb.linux.client.ui.components.navigation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import sb.linux.client.ui.theme.AppPaginationDefaults

@Composable
fun PaginationBar(
    page: Int, totalPages: Int, modifier: Modifier = Modifier, onPage: (Int) -> Unit,
) {
    if (totalPages <= 1) return
    // 胶囊整体高度，两端箭头为等宽可点区域
    val h = AppPaginationDefaults.height
    val normalTint = MaterialTheme.colorScheme.onSurface
    val disabledTint = MaterialTheme.colorScheme.onSurface.copy(alpha = AppPaginationDefaults.disabledAlpha)
    Row(
        modifier
            .fillMaxWidth()
            .padding(AppPaginationDefaults.containerPadding),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = AppPaginationDefaults.shape,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier.height(h)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // 上一页：胶囊内左端箭头（首页时禁用变淡）
                Box(
                    Modifier
                        .size(h)
                        .clip(AppPaginationDefaults.shape)
                        .then(
                            if (page > 1) Modifier.clickable { onPage((page - 1).coerceAtLeast(1)) }
                            else Modifier
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        "上一页",
                        modifier = Modifier.size(AppPaginationDefaults.iconSize),
                        tint = if (page > 1) normalTint else disabledTint
                    )
                }
                Text(
                    "$page",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(AppPaginationDefaults.pageLabelPadding)
                )
                Text(
                    " / $totalPages",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(AppPaginationDefaults.totalLabelPadding)
                )
                // 下一页：胶囊内右端箭头（末页时禁用变淡）
                Box(
                    Modifier
                        .size(h)
                        .clip(AppPaginationDefaults.shape)
                        .then(
                            if (page < totalPages) Modifier.clickable { onPage((page + 1).coerceAtMost(totalPages)) }
                            else Modifier
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        "下一页",
                        modifier = Modifier.size(AppPaginationDefaults.iconSize),
                        tint = if (page < totalPages) normalTint else disabledTint
                    )
                }
            }
        }
    }
}
