package sb.linux.client.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import sb.linux.client.domain.topic.PostEntry
import sb.linux.client.ui.theme.LocalAppSpacing
internal data class TopicSearchHit(val post: PostEntry, val snippet: String)

@Composable
internal fun SearchHighlightedText(text: String, query: String, maxLines: Int) {
    val highlight = MaterialTheme.colorScheme.tertiaryContainer
    val marked = remember(text, query, highlight) {
        buildAnnotatedString {
            append(text)
            if (query.isNotBlank()) {
                Regex(Regex.escape(query), RegexOption.IGNORE_CASE).findAll(text).forEach { match ->
                    addStyle(
                        SpanStyle(background = highlight, fontWeight = FontWeight.SemiBold),
                        match.range.first,
                        match.range.last + 1,
                    )
                }
            }
        }
    }
    Text(marked, maxLines = maxLines, overflow = TextOverflow.Ellipsis)
}

@Composable
internal fun TopicSearchOverlay(
    visible: Boolean,
    hits: List<TopicSearchHit>,
    query: String,
    selectedIndex: Int,
    listOpen: Boolean,
    allPagesLoaded: Boolean,
    totalPages: Int,
    loadingMore: Boolean,
    onClose: () -> Unit,
    onQueryChange: (String) -> Unit,
    onSelectedIndexChange: (Int) -> Unit,
    onListOpenChange: (Boolean) -> Unit,
    onLoadAllPages: () -> Unit,
    onOpenHit: (TopicSearchHit) -> Unit,
) {
    if (!visible) return
    val keyboard = LocalSoftwareKeyboardController.current
    fun openHit(index: Int) {
        if (hits.isEmpty()) return
        val target = (index + hits.size) % hits.size
        onSelectedIndexChange(target)
        keyboard?.hide()
        onOpenHit(hits[target])
    }
    Surface(
        shape = RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp),
        tonalElevation = 8.dp,
        shadowElevation = 12.dp,
        modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 8.dp),
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = LocalAppSpacing.current.contentGap,
                    vertical = LocalAppSpacing.current.item,
                ),
            verticalArrangement = Arrangement.spacedBy(LocalAppSpacing.current.item)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(onClick = onClose) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "关闭搜索") }
                OutlinedTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { openHit(selectedIndex) }),
                    placeholder = { Text("搜索正文、代码块、评论或用户名") },
                    leadingIcon = { Icon(Icons.Filled.Search, null) },
                    trailingIcon = {
                        if (query.isNotEmpty()) IconButton(onClick = { onQueryChange("") }) {
                            Icon(Icons.Filled.Close, "清空")
                        }
                    },
                    modifier = Modifier.weight(1f),
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    if (query.isBlank()) "正文 · 代码 · 评论 · 用户"
                    else if (hits.isEmpty()) "没有找到结果"
                    else (selectedIndex + 1).toString() + " / " + hits.size,
                    modifier = Modifier.weight(1f).padding(start = 8.dp),
                    style = MaterialTheme.typography.labelMedium,
                )
                IconButton(enabled = hits.isNotEmpty(), onClick = { openHit(selectedIndex - 1) }) {
                    Icon(Icons.Filled.KeyboardArrowUp, "上一个")
                }
                IconButton(enabled = hits.isNotEmpty(), onClick = { openHit(selectedIndex + 1) }) {
                    Icon(Icons.Filled.ExpandMore, "下一个")
                }
                IconButton(enabled = hits.isNotEmpty(), onClick = { onListOpenChange(!listOpen) }) {
                    Icon(Icons.AutoMirrored.Filled.FormatListBulleted, "全部结果")
                }
            }
            if (!allPagesLoaded && totalPages > 1) {
                TextButton(onClick = onLoadAllPages, enabled = !loadingMore) {
                    Text(if (loadingMore) "正在加载剩余评论…" else "当前为已加载内容，点击搜索全帖")
                }
            }
            if (listOpen && hits.isNotEmpty()) {
                LazyColumn(Modifier.fillMaxWidth().heightIn(max = (LocalConfiguration.current.screenHeightDp * 0.36f).dp)) {
                    itemsIndexed(hits, key = { _, hit -> hit.post.id }) { index, hit ->
                        val post = hit.post
                        ListItem(
                            headlineContent = {
                                SearchHighlightedText(
                                    if (post.floor > 0) "#" + post.floor + " · " + post.authorName else "楼主 · " + post.authorName,
                                    query.trim(),
                                    maxLines = 1,
                                )
                            },
                            supportingContent = { SearchHighlightedText(hit.snippet, query.trim(), maxLines = 2) },
                            leadingContent = {
                                if (index == selectedIndex) Icon(Icons.Filled.Search, null, tint = MaterialTheme.colorScheme.primary)
                            },
                            modifier = Modifier.clickable { onListOpenChange(false); openHit(index) },
                        )
                    }
                }
            }
        }
    }
}
