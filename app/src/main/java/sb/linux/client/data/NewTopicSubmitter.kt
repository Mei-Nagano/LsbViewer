package sb.linux.client.data

import sb.linux.client.common.error.LsbException
import sb.linux.client.domain.topic.NewTopicSubmission
import sb.linux.client.domain.topic.PrizeRow

internal class NewTopicSubmitter(
    private val client: LsbClient,
) {
    suspend fun submit(input: NewTopicSubmission): String? {
        if (!input.loggedIn) throw LsbException("请先登录")
        val form = mutableListOf(
            "_csrf" to client.csrf(),
            "id" to input.internalId,
            "forum_id" to input.forumId,
            "title" to input.title,
            "body" to input.body,
        )
        if (input.editId > 0) form += input.hiddenFields.toList()
        if (input.editId > 0 && input.replyOrder.isNotBlank()) {
            form += "reply_order" to input.replyOrder
        }
        when (input.specialType) {
            "lottery" -> appendLotteryFields(form, input)
            "virtual_card" -> appendVirtualCardFields(form, input)
        }
        val path = if (input.editId > 0) "/topic_edit/${input.editId}" else "/topic_edit"
        val response = client.postFormPairs(path, form)
        return if (response.url.contains("form_error")) {
            HtmlTextParser.extractError(response.html).ifBlank { "发布失败" }
        } else {
            null
        }
    }

    private fun appendLotteryFields(form: MutableList<Pair<String, String>>, input: NewTopicSubmission) {
        form += "topic_special_type" to "lottery"
        form += "community_lottery_original_type" to ""
        form += "lottery_draw_at" to input.drawAt.trim().replace(" ", "T")
        form += "lottery_participant_target" to input.participantTarget.trim()
        form += "lottery_min_reply_chars" to input.minReplyChars.trim()
        if (input.replyCaptcha) form += "lottery_reply_captcha_required" to "1"
        input.prizes.filter { it.name.isNotBlank() }.forEach { prize ->
            form += "lottery_prize_name[]" to prize.name.trim()
            form += "lottery_prize_type[]" to prize.type
            form += "lottery_prize_quantity[]" to prize.quantity.trim().ifBlank { "1" }
            form += "lottery_prize_value[]" to prize.value.trim()
        }
    }

    private fun appendVirtualCardFields(form: MutableList<Pair<String, String>>, input: NewTopicSubmission) {
        form += "topic_special_type" to "virtual_card"
        form += "virtual_card_original_type" to ""
        form += "virtual_card_name" to input.cardName.trim()
        form += "virtual_card_currency" to input.cardCurrency
        form += "virtual_card_price" to input.cardPrice.trim().ifBlank { "1" }
        form += "virtual_card_purchase_limit" to input.cardLimit.trim().ifBlank { "1" }
        if (input.cardAutoReply) {
            form += "virtual_card_auto_reply" to "1"
            form += "virtual_card_auto_reply_content" to input.cardAutoReplyContent
        }
        form += "virtual_card_values" to input.cardValues
    }
}

