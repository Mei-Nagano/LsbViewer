package sb.linux.client.domain.topic

/**
 * 旧版阅读位置：严格四段 CSV（floor、listIndex、scrollOffset、时间）的解析结果。
 * 新版仍完整保留该记录与两小时 TTL，供旧版本与旧备份读取。
 */
data class TopicReadingPosition(val floor: Int, val listIndex: Int, val scrollOffset: Int)

/**
 * 版本化阅读锚点（`v2:<topicId>`）：只在对应旧记录仍存在且有效时使用。
 * 楼层/帖子标识优先，listIndex 与像素 offset 仅作为回退。
 */
data class TopicReadingAnchor(
    val floor: Int,
    val postId: Long,
    val listIndex: Int,
    val scrollOffset: Int,
)

interface TopicReadingPositionRepository {
    /** 始终写入旧四段记录；postId > 0 时额外写入版本化锚点。 */
    fun save(topicId: Long, floor: Int, listIndex: Int, scrollOffset: Int, postId: Long = 0)

    fun get(topicId: Long): TopicReadingPosition?

    /** 仅在旧记录仍有效时返回版本化锚点；缺失/过期/格式不识别时返回 null。 */
    fun getAnchor(topicId: Long): TopicReadingAnchor?

    fun clear()
}
