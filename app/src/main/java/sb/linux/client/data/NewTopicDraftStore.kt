package sb.linux.client.data

import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject
import sb.linux.client.domain.topic.NewTopicDraft
import sb.linux.client.domain.topic.NewTopicDraftOptions
import sb.linux.client.domain.topic.PrizeRow
import sb.linux.client.domain.topic.NewTopicDraftRepository

/** Owns the existing local draft preference file and its JSON compatibility format. */
internal class NewTopicDraftStore(private val preferences: SharedPreferences) : NewTopicDraftRepository {
    override fun save(draft: NewTopicDraft) {
        val extra = JSONObject().apply {
            val options = requireNotNull(draft.options)
            put("specialType", options.specialType)
            put("drawAt", options.drawAt)
            put("participantTarget", options.participantTarget)
            put("minReplyChars", options.minReplyChars)
            put("replyCaptcha", options.replyCaptcha)
            put("prizes", JSONArray(options.prizes.orEmpty().map(::encodePrize)))
            put("cardName", options.cardName)
            put("cardCurrency", options.cardCurrency)
            put("cardPrice", options.cardPrice)
            put("cardLimit", options.cardLimit)
            put("cardAutoReply", options.cardAutoReply)
            put("cardAutoReplyContent", options.cardAutoReplyContent)
            put("cardValues", options.cardValues)
        }
        preferences.edit()
            .putString("title", draft.title)
            .putString("body", draft.body)
            .putString("extra", extra.toString())
            .putString("forum_id", draft.forumId)
            .putLong("saved_at", System.currentTimeMillis())
            .apply()
    }

    override fun load(): NewTopicDraft = NewTopicDraft(
        title = preferences.getString("title", "").orEmpty(),
        body = preferences.getString("body", "").orEmpty(),
        forumId = preferences.getString("forum_id", "").orEmpty(),
        options = decodeOptions(),
    )

    override fun hasRestorableDraft(): Boolean =
        !preferences.getString("body", "").isNullOrBlank() ||
            !preferences.getString("title", "").isNullOrBlank() || preferences.contains("extra")

    override fun hasSavedDraft(): Boolean = preferences.contains("saved_at")

    override fun clear() {
        preferences.edit().clear().apply()
    }

    private fun decodeOptions(): NewTopicDraftOptions? = runCatching {
        val extra = JSONObject(preferences.getString("extra", "{}").orEmpty())
        NewTopicDraftOptions(
            specialType = extra.optString("specialType"),
            drawAt = extra.optString("drawAt"),
            participantTarget = extra.optString("participantTarget", "0"),
            minReplyChars = extra.optString("minReplyChars", "5"),
            replyCaptcha = extra.optBoolean("replyCaptcha", true),
            prizes = extra.optJSONArray("prizes")?.let(::decodePrizes),
            cardName = extra.optString("cardName"),
            cardCurrency = extra.optString("cardCurrency", "points"),
            cardPrice = extra.optString("cardPrice", "1"),
            cardLimit = extra.optString("cardLimit", "1"),
            cardAutoReply = extra.optBoolean("cardAutoReply"),
            cardAutoReplyContent = extra.optString("cardAutoReplyContent"),
            cardValues = extra.optString("cardValues"),
        )
    }.getOrNull()

    private fun decodePrizes(rows: JSONArray): List<PrizeRow> =
        (0 until rows.length()).mapNotNull { index ->
            rows.optJSONObject(index)?.let { row ->
                PrizeRow(
                    row.optString("name"), row.optString("type", "points"),
                    row.optString("quantity", "1"), row.optString("value"),
                )
            }
        }.ifEmpty { listOf(PrizeRow()) }

    private fun encodePrize(prize: PrizeRow): JSONObject = JSONObject().apply {
        put("name", prize.name)
        put("type", prize.type)
        put("quantity", prize.quantity)
        put("value", prize.value)
    }
}
