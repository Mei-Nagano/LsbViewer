package sb.linux.client.domain.community

import sb.linux.client.domain.topic.TitleBadge

enum class FollowingView(val sourceValue: String, val label: String, val emptyText: String) {
    TOPICS("topics", "关注动态", "暂无关注动态"),
    FOLLOWING("following", "我的关注", "暂无关注"),
    FOLLOWERS("followers", "我的粉丝", "暂无粉丝"),
}

data class FollowingUser(
    val userId: Long,
    val username: String,
    val avatarUrl: String,
    val group: String,
    val meta: String,
    val titleBadge: TitleBadge? = null,
    val online: Boolean = false,
    val following: Boolean = false,
    val canFollow: Boolean = false,
    val groupBadge: sb.linux.client.domain.account.UserGroupBadge? = null,
    val followersText: String = "",
    val followTimeText: String = "",
    val followedAt: Long? = null,
    val capturedAt: Long = 0,
)
