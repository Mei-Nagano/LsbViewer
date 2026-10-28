package sb.linux.client.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmarks
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import kotlinx.coroutines.launch
import sb.linux.client.common.collection.TopicCollectionActionForm
import sb.linux.client.common.collection.TopicCollectionOperation
import sb.linux.client.domain.gacha.GachaOperationForm
import sb.linux.client.domain.gacha.GachaOperationPage
import sb.linux.client.domain.topic.TopicCard
import sb.linux.client.ui.components.feedback.EmptyBox
import sb.linux.client.ui.components.feedback.ErrorBox
import sb.linux.client.ui.components.feedback.LoadingBox
import sb.linux.client.ui.patterns.topic.TopicCardView

internal fun TopicCollectionActionForm.toGachaForm() = GachaOperationForm(
    label = label,
    action = action,
    hiddenFields = fields,
    enabled = enabled,
)

/** 专辑内容列表：操作按钮 → 说明卡 → 帖子卡片 → 分页胶囊。 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun CollectionDetailList(
    nav: NavHostController,
    opPage: GachaOperationPage?,
    topics: List<TopicCard>,
    forms: List<GachaOperationForm>,
    pageLinks: List<Pair<String, String>>,
    path: String,
    loading: Boolean,
    error: String?,
    submitting: Boolean,
    onPath: (String) -> Unit,
    onReload: () -> Unit,
    onPending: (Pair<GachaOperationForm, List<Pair<String, String>>>) -> Unit,
) {
    // 无输入项的表单只是一个按钮（删除、移除等；订阅已提到顶栏），收成一排胶囊置顶；
    // 需要填内容的表单（新建、编辑说明）保留卡片，跟在按钮下方。
    val quickForms = forms.filter { it.fields.isEmpty() }
    val inputForms = forms.filter { it.fields.isNotEmpty() }
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 12.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (loading) item { LinearProgressIndicator(Modifier.fillMaxWidth().padding(horizontal = 14.dp)) }
        // 已有内容时错误降级为条幅，不清空正在看的列表
        error?.let { message ->
            item {
                sb.linux.client.ui.components.feedback.AppNotice(message, Modifier.padding(horizontal = 14.dp), onAction = onReload)
            }
        }
        // 管理操作置顶，进页面就能点，不用滚到最下面
        if (quickForms.isNotEmpty()) {
            item {
                FlowRow(
                    Modifier.fillMaxWidth().padding(horizontal = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    quickForms.forEach { form ->
                        FilledTonalButton(
                            enabled = form.enabled && !submitting && !loading,
                            onClick = { onPending(form to form.hiddenFields) },
                            shape = RoundedCornerShape(50),
                            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 8.dp),
                        ) { Text(form.label, style = MaterialTheme.typography.labelLarge, maxLines = 1) }
                    }
                }
            }
        }
        itemsIndexed(inputForms) { _, form ->
            Box(Modifier.padding(horizontal = 14.dp)) {
                GachaDynamicForm(form, !submitting && !loading) {
                    onPending(form to it)
                }
            }
        }
        // 专辑说明：源站说明文字集中成一张卡，不再逐条铺开占满屏
        opPage?.notes.orEmpty().filter { it.isNotBlank() }.takeIf { it.isNotEmpty() }?.let { notes ->
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp),
                ) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Filled.Bookmarks, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                            Text("专辑说明", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                        }
                        notes.forEach {
                            Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
        // 内容区：与首页/板块一致的帖子卡片，替换原来的窄行 ListItem
        if (topics.isNotEmpty()) {
            item {
                Text("收录内容 · ${topics.size}", style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(horizontal = 18.dp))
            }
            itemsIndexed(topics, key = { _, t -> t.topicId }) { _, topic ->
                TopicCardView(topic, onClick = { nav.navigate("topic/${topic.topicId}") })
            }
        } else if (!loading && error == null) {
            item {
                Text("这个专辑还没有收录内容", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 4.dp))
            }
        }
        // 翻页：源站分页链接做成一行胶囊，不再竖排文字按钮
        if (pageLinks.isNotEmpty()) {
            item {
                Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    pageLinks.forEach { (label, href) ->
                        val current = href == path
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = if (current) MaterialTheme.colorScheme.secondaryContainer
                            else MaterialTheme.colorScheme.surfaceContainerHigh,
                            modifier = Modifier.clip(RoundedCornerShape(50)).clickable(enabled = !current && !loading) { onPath(href) },
                        ) {
                            Text(label, style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (current) FontWeight.Bold else FontWeight.Medium,
                                color = if (current) MaterialTheme.colorScheme.onSecondaryContainer
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp))
                        }
                    }
                }
            }
        }
    }
}
