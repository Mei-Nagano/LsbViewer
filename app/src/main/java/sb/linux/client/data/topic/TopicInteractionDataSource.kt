package sb.linux.client.data.topic

import sb.linux.client.domain.topic.PostEntry
import sb.linux.client.domain.topic.DanmakuItem
import sb.linux.client.data.AppSettings
import sb.linux.client.data.LsbClient
import sb.linux.client.domain.topic.TopicPageData
import sb.linux.client.domain.topic.EssenceApplication
import sb.linux.client.domain.topic.EssencePointsTopUp
import sb.linux.client.domain.topic.TopicPoll
import sb.linux.client.domain.topic.TopicPollOption
import sb.linux.client.domain.topic.VirtualCard
import sb.linux.client.data.DonateFormParseResult
import sb.linux.client.data.DonateHtmlParser
import sb.linux.client.data.HtmlTextParser
import sb.linux.client.common.content.HtmlTextConversion
import sb.linux.client.domain.topic.DonationForm
import sb.linux.client.domain.topic.TopicActionResult
import sb.linux.client.domain.topic.TopicFavoriteChange
import sb.linux.client.domain.topic.TopicInteractionRepository

/** Keeps source form details and local topic snapshots out of the screen. */
internal class TopicInteractionDataSource(
    private val client: LsbClient,
    private val settings: AppSettings,
    private val clearTopicCache: (Long) -> Unit,
) : TopicInteractionRepository {
    private val donateSubmissionData = mutableMapOf<Long, DonateFormParseResult>()
    override suspend fun react(topicId: Long, page: TopicPageData, post: PostEntry, points: Int, reason: String): String {
        val response = client.postAjax(
            "/donate_reply_reaction",
            mapOf(
                "_csrf" to page.csrf,
                "donate_reaction_reply_id" to post.id.toString(),
                "donate_reaction_points" to points.toString(),
                "donate_reaction_reason" to reason,
            ),
        )
        val succeeded = response.optInt("ok") == 1
        return response.optString("message", if (succeeded) "已点赞" else "操作失败")
    }

    override suspend fun loadDonateFeed(topicId: Long) = client.getDonateFeed(topicId)

    override suspend fun loadDonateForm(topicId: Long): DonationForm {
        val parsed = DonateHtmlParser.parseDonate(client.get("/donate?topic_id=$topicId").html)
        donateSubmissionData[topicId] = parsed
        return parsed.form
    }

    override suspend fun donate(topicId: Long, amount: String, message: String): String? {
        val info = donateSubmissionData[topicId]
            ?: error("打赏表单已失效，请重新加载")
        val response = client.postForm(
            "/donate",
            mapOf(
                "_csrf" to info.csrf,
                "topic_id" to info.topicId.ifBlank { topicId.toString() },
                "request_key" to info.requestKey,
                "amount" to amount,
                "message" to message,
            ),
        )
        return if (response.url.contains("form_error")) {
            HtmlTextParser.extractError(response.html).ifBlank { "打赏失败" }
        } else {
            null
        }
    }

    override suspend fun joinLottery(page: TopicPageData, option: String): TopicActionResult {
        val panel = requireNotNull(page.lottery)
        fun fields(token: String) = buildMap {
            put("_csrf", token)
            putAll(panel.joinFields)
            if (option.isNotBlank()) put("option", option)
        }
        var response = client.postForm(
            panel.joinAction,
            fields(page.csrf.ifBlank { client.csrf() }),
        )
        if (response.url.contains("form_error") && response.html.contains("csrf", ignoreCase = true)) {
            response = client.postForm(
                panel.joinAction,
                fields(client.csrf(forceRefresh = true)),
            )
        }
        return result(response.url.contains("form_error"), "已参与抽奖", "参与失败", response.html)
    }

    override suspend fun applyForEssence(page: TopicPageData, application: EssenceApplication): TopicActionResult {
        val fields = buildList {
            add("_csrf" to page.csrf.ifBlank { client.csrf() })
            addAll(application.fields.filterKeys { it != "_csrf" }.toList())
        }
        val response = client.postFormPairs(application.action, fields)
        return result(response.url.contains("form_error"), "申精已提交", "申请失败", response.html)
    }

    override suspend fun topUpEssence(page: TopicPageData, topUp: EssencePointsTopUp, amount: Int): TopicActionResult {
        val fields = buildList {
            add("_csrf" to page.csrf.ifBlank { client.csrf() })
            addAll(topUp.fields.filterKeys { it != "_csrf" }.toList())
            add(topUp.amountName to amount.toString())
        }
        val response = client.postFormPairs(topUp.action, fields)
        return result(response.url.contains("form_error"), "已追加 $amount 积分", "追加失败", response.html)
    }

    override suspend fun vote(page: TopicPageData, poll: TopicPoll, selected: List<TopicPollOption>, reason: String): TopicActionResult {
        val fields = buildList {
            add("_csrf" to page.csrf.ifBlank { client.csrf() })
            addAll(poll.fields.toList())
            selected.forEach { add(it.name to it.value) }
            if (poll.reasonName.isNotBlank()) add(poll.reasonName to reason)
        }
        val response = client.postFormPairs(poll.action, fields)
        return result(response.url.contains("form_error"), "投票已提交", "投票失败", response.html)
    }

    override suspend fun buyVirtualCard(page: TopicPageData, card: VirtualCard): TopicActionResult {
        val response = client.postForm(
            card.buyAction,
            buildMap {
                put("_csrf", page.csrf)
                putAll(card.buyFields)
            },
        )
        return result(
            response.url.contains("form_error"),
            "兑换成功，卡片见本帖购买记录",
            "兑换失败",
            response.html,
        )
    }

    private fun result(rejected: Boolean, success: String, failure: String, html: String) =
        if (rejected) TopicActionResult(HtmlTextParser.extractError(html).ifBlank { failure }, false)
        else TopicActionResult(success, true)

    override suspend fun toggleFavorite(page: TopicPageData, wasFavorite: Boolean): TopicFavoriteChange? {
        val response = client.postForm(
            page.favAction,
            mapOf("_csrf" to page.csrf, "topic_id" to page.topicId.toString()),
        )
        if (response.url.contains("form_error")) return null
        val isFavorite = !wasFavorite
        clearTopicCache(page.topicId)
        if (wasFavorite) {
            settings.removeFavorite(page.topicId)
        } else {
            val author = page.posts.firstOrNull { it.floor == 0 } ?: page.posts.firstOrNull()
            settings.addFavorite(
                topicId = page.topicId,
                title = page.title,
                authorId = author?.authorId ?: 0,
                authorName = author?.authorName ?: "",
                forumId = 0,
                forumName = "",
                avatarUrl = author?.avatarUrl ?: "",
                titleColor = "",
            )
        }
        return TopicFavoriteChange(wasFavorite, page.copy(favorited = isFavorite))
    }

    override fun toggleCommentFavorite(topicId: Long, topicTitle: String, post: PostEntry): Pair<Set<Long>, String> {
        val isFavorite = post.id in settings.commentFavoriteList().map { it.replyId }.toSet()
        if (isFavorite) {
            settings.removeCommentFavorite(post.id)
        } else {
            val excerpt = HtmlTextConversion.htmlToText(post.contentHtml).trim().let {
                if (it.length > MAX_EXCERPT_LENGTH) it.take(MAX_EXCERPT_LENGTH) + "…" else it
            }
            settings.addCommentFavorite(
                replyId = post.id,
                topicId = topicId,
                topicTitle = topicTitle,
                floor = post.floor,
                authorId = post.authorId,
                authorName = post.authorName,
                avatarUrl = post.avatarUrl,
                content = excerpt,
            )
        }
        val ids = settings.commentFavoriteList().map { it.replyId }.toSet()
        return ids to if (isFavorite) "已取消收藏" else "已收藏该评论"
    }

    private companion object {
        const val MAX_EXCERPT_LENGTH = 120
    }
}
