package sb.linux.client.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import sb.linux.client.data.TitleBadge
import sb.linux.client.ui.TitleBadgeView

/** 称号中心的滚动新闻条。 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun GachaNews(news: List<String>) {
    Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surfaceContainerLow) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            news.chunked((news.size + 2) / 3).forEachIndexed { row, values ->
                Text(
                    values.joinToString("     ✦     "),
                    Modifier.fillMaxWidth().basicMarquee(
                        iterations = Int.MAX_VALUE, initialDelayMillis = row * 600, velocity = (26 + row * 8).dp,
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
        }
    }
}

/** 全部称号目录：按稀有度分组并支持分组折叠。 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun GachaTitleGroups(titles: List<TitleBadge>) {
    var expanded by remember { mutableStateOf(setOf("SSR")) }
    titles.distinct().groupBy { it.rarity.uppercase() }
        .toSortedMap(compareBy { rarityOrder(it) })
        .forEach { (rarity, badges) ->
            val open = rarity in expanded
            Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surfaceContainerLow) {
                Column(Modifier.fillMaxWidth()) {
                    Row(
                        Modifier.fillMaxWidth()
                            .clickable { expanded = if (open) expanded - rarity else expanded + rarity }
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        RarityPill(rarity)
                        Text(
                            "${badges.size} 种",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f),
                        )
                        Icon(
                            if (open) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                            if (open) "收起" else "展开",
                            Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (open) {
                        FlowRow(
                            Modifier.fillMaxWidth().padding(start = 14.dp, end = 14.dp, bottom = 14.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            badges.forEach { TitleBadgeView(it) }
                        }
                    }
                }
            }
        }
}
