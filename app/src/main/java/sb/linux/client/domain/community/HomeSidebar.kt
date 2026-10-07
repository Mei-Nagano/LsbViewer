package sb.linux.client.domain.community

data class HomeSidebar(
    val forums: List<ForumCount> = emptyList(),
    val hotTopics: List<HotTopic> = emptyList(),
    val statsText: String = "",
    val newUsers: List<NewUser> = emptyList(),
    val onlineUsers: OnlineUsers = OnlineUsers(),
)

data class OnlineUsers(
    val count: String = "",
    val items: List<OnlineUser> = emptyList(),
    val moreText: String = "",
)

data class OnlineUser(val userId: Long, val username: String, val avatarUrl: String = "")

data class ForumCount(val id: Long, val name: String, val count: String, val color: String = "")

data class HotTopic(val topicId: Long, val title: String, val meta: String)

data class NewUser(val userId: Long, val username: String, val avatarUrl: String = "")
