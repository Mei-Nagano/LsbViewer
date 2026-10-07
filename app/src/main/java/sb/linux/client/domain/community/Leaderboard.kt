package sb.linux.client.domain.community

import sb.linux.client.domain.topic.TitleBadge

data class LeaderRow(
    val rank: Int,
    val userId: Long,
    val username: String,
    val userGroup: String,
    val valueText: String,
    val avatarUrl: String = "",
    val online: Boolean = false,
    val titleBadge: TitleBadge? = null,
)

interface LeaderboardRepository {
    suspend fun loadLeaderboard(type: String): List<LeaderRow>
}
