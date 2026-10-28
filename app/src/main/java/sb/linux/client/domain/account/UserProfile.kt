package sb.linux.client.domain.account

import sb.linux.client.domain.topic.TitleBadge

data class UserProfile(
    val userId: Long,
    val username: String,
    val avatarUrl: String,
    val rankText: String,
    val userGroup: String = "",
    val points: String = "",
    val bio: String = "",
    val stats: List<Pair<String, String>>,
    val online: Boolean = false,
    val titleBadge: TitleBadge? = null,
    val blockUrl: String = "",
    val blockLabel: String = "",
)
