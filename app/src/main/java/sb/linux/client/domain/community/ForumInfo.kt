package sb.linux.client.domain.community

data class ForumInfo(
    val id: Long,
    val name: String,
    val description: String = "",
    val topicCount: String = "",
)
