package sb.linux.client.ui.feature.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import sb.linux.client.domain.search.SearchResultItem
import sb.linux.client.ui.theme.LocalAppSpacing

/** 搜索结果的平面列表项：主题结果用帖子摘要，用户结果用统一用户条目。 */
@Composable
internal fun TopicSearchItem(item: SearchResultItem.Topic, onClick: () -> Unit) {
    val spacing = LocalAppSpacing.current
    Column(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = spacing.pageHorizontal, vertical = spacing.contentGap),
    ) {
        Text(item.title, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
        if (item.snippet.isNotBlank()) {
            Text(
                item.snippet,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(spacing.item),
            modifier = Modifier.padding(top = spacing.item),
        ) {
            if (item.matchedBy.isNotBlank()) Text(item.matchedBy, style = MaterialTheme.typography.bodySmall)
            Text(item.forumName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(item.timeText, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("${item.replies} 回复 · ${item.views} 浏览", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        HorizontalDivider(
            Modifier.padding(top = spacing.contentGap),
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
        )
    }
}

@Composable
internal fun UserSearchItem(item: SearchResultItem.User, onClick: () -> Unit) {
    val spacing = LocalAppSpacing.current
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = spacing.pageHorizontal, vertical = spacing.contentGap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Filled.Person, null, Modifier.size(28.dp), tint = MaterialTheme.colorScheme.primary)
        Column(Modifier.padding(start = spacing.contentGap)) {
            Text(item.username, style = MaterialTheme.typography.titleMedium)
            Text(item.meta, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
internal fun SearchPagination(page: Int, hasPrevious: Boolean, hasNext: Boolean, onPage: (Int) -> Unit) {
    if (!hasPrevious && !hasNext) return
    Row(
        Modifier.fillMaxWidth().padding(
            horizontal = LocalAppSpacing.current.pageHorizontal,
            vertical = LocalAppSpacing.current.contentGap,
        ),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(enabled = hasPrevious, onClick = { onPage(page - 1) }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "上一页") }
        Text("第 $page 页", modifier = Modifier.padding(horizontal = 10.dp))
        IconButton(enabled = hasNext, onClick = { onPage(page + 1) }) { Icon(Icons.AutoMirrored.Filled.ArrowForward, "下一页") }
    }
}

internal fun itemKey(item: SearchResultItem): String = when (item) {
    is SearchResultItem.Topic -> "topic:${item.topicId}"
    is SearchResultItem.User -> "user:${item.userId}"
}
