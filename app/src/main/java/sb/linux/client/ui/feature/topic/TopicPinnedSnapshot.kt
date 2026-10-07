package sb.linux.client.ui.feature.topic

import sb.linux.client.domain.topic.DanmakuItem
import sb.linux.client.domain.topic.TopicPageData

/** In-process presentation state retained while a topic is temporarily pinned. */
data class TopicPinnedSnapshot(
    val topic: TopicPageData,
    val listIndex: Int,
    val offset: Int,
    val localPage: Int,
    val sort: Int,
    val collapsed: Set<Long>,
    val danmaku: List<DanmakuItem> = emptyList(),
    val danmakuEnabled: Boolean = false,
)
