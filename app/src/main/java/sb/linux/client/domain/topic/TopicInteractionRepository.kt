package sb.linux.client.domain.topic

data class TopicActionResult(val message: String, val succeeded: Boolean)

data class TopicFavoriteChange(
    val wasFavorite: Boolean,
    val page: TopicPageData,
)

interface TopicInteractionRepository {
    suspend fun react(topicId: Long, page: TopicPageData, post: PostEntry, points: Int, reason: String): String
    suspend fun loadDonateFeed(topicId: Long): List<DanmakuItem>
    suspend fun loadDonateForm(topicId: Long): DonationForm
    suspend fun donate(topicId: Long, amount: String, message: String): String?
    suspend fun joinLottery(page: TopicPageData, option: String): TopicActionResult
    suspend fun applyForEssence(page: TopicPageData, application: EssenceApplication): TopicActionResult
    suspend fun topUpEssence(page: TopicPageData, topUp: EssencePointsTopUp, amount: Int): TopicActionResult
    suspend fun vote(page: TopicPageData, poll: TopicPoll, selected: List<TopicPollOption>, reason: String): TopicActionResult
    suspend fun buyVirtualCard(page: TopicPageData, card: VirtualCard): TopicActionResult
    suspend fun toggleFavorite(page: TopicPageData, wasFavorite: Boolean): TopicFavoriteChange?
    fun toggleCommentFavorite(topicId: Long, topicTitle: String, post: PostEntry): Pair<Set<Long>, String>
}
