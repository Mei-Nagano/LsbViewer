package sb.linux.client.data

data class PinnedTopicSnapshot(
    val topic: TopicPageData,
    val listIndex: Int,
    val offset: Int,
    val localPage: Int,
    val sort: Int,
    val collapsed: Set<Long>,
    val danmaku: List<DanmakuItem> = emptyList(),
    val danmakuEnabled: Boolean = false,
)
