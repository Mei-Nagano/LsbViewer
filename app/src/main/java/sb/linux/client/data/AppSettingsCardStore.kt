package sb.linux.client.data

import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject
import sb.linux.client.domain.gacha.CardRedemptionRecord
import sb.linux.client.domain.topic.VirtualCard

internal class AppSettingsCardStore(private val prefs: SharedPreferences) {
    private fun cardRedemptionArray(): JSONArray = runCatching {
        JSONArray(prefs.getString("card_redemptions", "[]") ?: "[]")
    }.getOrDefault(JSONArray())

    /** 从帖子“我的购买记录”同步本机档案。按帖子 + 兑换码去重，最新发现的记录置顶。 */
    fun recordCardRedemptions(topicId: Long, topicTitle: String, card: VirtualCard?) {
        if (topicId <= 0 || card == null || card.orders.isEmpty()) return
        val old = cardRedemptionArray()
        val incomingKeys = card.orders.map { "$topicId\u0000${it.code}" }.toSet()
        val out = JSONArray()
        val now = System.currentTimeMillis()
        card.orders.forEach { order ->
            out.put(
                JSONObject()
                    .put("topicId", topicId).put("topicTitle", topicTitle)
                    .put("cardTitle", card.title).put("code", order.code)
                    .put("price", order.price).put("sourceTime", order.time)
                    .put("recordedAt", now)
            )
        }
        for (i in 0 until old.length()) {
            val item = old.optJSONObject(i) ?: continue
            val key = "${item.optLong("topicId")}\u0000${item.optString("code")}"
            if (key !in incomingKeys) out.put(item)
        }
        while (out.length() > 500) out.remove(out.length() - 1)
        prefs.edit().putString("card_redemptions", out.toString()).apply()
    }

    fun cardRedemptions(): List<CardRedemptionRecord> {
        val arr = cardRedemptionArray()
        return buildList {
            for (i in 0 until arr.length()) {
                val item = arr.optJSONObject(i) ?: continue
                add(
                    CardRedemptionRecord(
                        topicId = item.optLong("topicId"),
                        topicTitle = item.optString("topicTitle"),
                        cardTitle = item.optString("cardTitle"),
                        code = item.optString("code"),
                        price = item.optString("price"),
                        sourceTime = item.optString("sourceTime"),
                        recordedAt = item.optLong("recordedAt"),
                    )
                )
            }
        }
    }

    fun clearCardRedemptions() = prefs.edit().remove("card_redemptions").apply()

    // ---------------- 使用统计 ----------------
}
