package sb.linux.client.data

import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject
import sb.linux.client.domain.community.DirectMessageThread

internal class AppSettingsPrivateMessageStore(private val prefs: SharedPreferences) {
    private fun pmArray(): JSONArray =
        runCatching { JSONArray(prefs.getString("local_pm_messages", "[]") ?: "[]") }.getOrDefault(JSONArray())

    /** 追加一条私信消息，仅保留最近 300 条。
     *  - 收到的私信按「对方+内容」去重（通知页反复拉全量），命中回填资料并刷新天级 ts；
     *    兼容旧版把引用拼进正文的拼法（"引用 正文"），命中时迁移为分离后的干净正文。
     *  - synthetic（依据对方回复引用补造的我方消息）：已存在同内容则不再补。
     *  - App 内真实发送的消息不去重，总是追加。
     *  排序规则见 pmThread：先按 ts 自然日分组，同一天内按 seq。 */
    fun addPmMessage(
        partnerId: Long, partnerName: String, avatarUrl: String,
        incoming: Boolean, content: String, ts: Long = 0L,
        quoteText: String = "", synthetic: Boolean = false,
    ) {
        if (partnerId <= 0) return
        val t = if (ts > 0) ts else System.currentTimeMillis()
        val old = pmArray()
        // 新 seq 必须大于所有已存 seq，也要大于旧记录的读取兜底值（数组长度，见 pmThread）
        var maxSeq = old.length().toLong()
        for (i in 0 until old.length()) {
            val o = old.optJSONObject(i) ?: continue
            maxSeq = maxOf(maxSeq, o.optLong("seq", 0L))
        }
        // 旧版正文 = "引用 + 空格 + 正文"，迁移期去重同时匹配该拼法
        val legacyContent = if (quoteText.isNotBlank()) "$quoteText $content" else ""
        val rest = JSONArray()
        var dedupHit = false       // 收到的消息命中去重（回填后不追加）
        var syntheticExists = false// 补造的我方消息已存在（或真发过同内容）则不再补
        for (i in 0 until old.length()) {
            val o = old.optJSONObject(i) ?: continue
            val samePartner = o.optLong("partnerId") == partnerId
            val stored = o.optString("content")
            if (incoming && !dedupHit && samePartner &&
                (stored == content || (legacyContent.isNotBlank() && stored == legacyContent))
            ) {
                rest.put(
                    JSONObject(o.toString())
                        .put("ts", t)
                        .put("content", content)
                        .put("quote", quoteText)
                        .put("partnerName", partnerName.ifBlank { o.optString("partnerName") })
                        .put("avatarUrl", avatarUrl.ifBlank { o.optString("avatarUrl") })
                )
                dedupHit = true
            } else {
                if (synthetic && !syntheticExists && samePartner && stored == content) syntheticExists = true
                rest.put(o)
            }
        }
        val out = JSONArray()
        if (!dedupHit && !syntheticExists) {
            out.put(
                JSONObject()
                    .put("partnerId", partnerId).put("partnerName", partnerName)
                    .put("avatarUrl", avatarUrl).put("incoming", incoming)
                    .put("content", content).put("ts", t)
                    .put("seq", maxSeq + 1L)
                    .put("quote", quoteText)
                    .put("synthetic", synthetic)
                    // App 内发送的有精确时间；通知解析与引用补造的只有天级
                    .put("timeExact", !incoming && !synthetic)
                    .put("read", incoming)   // 我发出的始终视为已读；对方发来的以此作为标记（markPmRead 会翻成 true）
            )
        }
        for (i in 0 until rest.length()) {
            if (out.length() >= 300) break
            out.put(rest.optJSONObject(i))
        }
        prefs.edit().putString("local_pm_messages", out.toString()).apply()
    }

    /** 与某人的会话里是否已有指定内容的消息（判重：决定是否按引用补造我方消息） */
    fun pmHasContent(partnerId: Long, content: String): Boolean {
        if (content.isBlank()) return false
        val arr = pmArray()
        for (i in 0 until arr.length()) {
            val o = arr.optJSONObject(i) ?: continue
            if (o.optLong("partnerId") == partnerId && o.optString("content") == content) return true
        }
        return false
    }

    /** 时间戳 → 聊天式时间（QQ/微信风格，固定不随查看时刻漂移）：
     *  精确（我发出的）：今天 HH:mm / 昨天 HH:mm / 今年 MM-dd HH:mm / 跨年 yyyy-MM-dd HH:mm
     *  天级（通知解析的只有"昨天/2天前"）：今天 / 昨天 / MM-dd / yyyy-MM-dd，不编造时刻 */
    private fun relativeTimeText(ts: Long, exact: Boolean): String {
        if (ts <= 0) return ""
        val cal = java.util.Calendar.getInstance()
        val now = java.util.Calendar.getInstance()
        cal.timeInMillis = ts
        val sameDay = cal.get(java.util.Calendar.YEAR) == now.get(java.util.Calendar.YEAR) &&
            cal.get(java.util.Calendar.DAY_OF_YEAR) == now.get(java.util.Calendar.DAY_OF_YEAR)
        val yesterday = run {
            val y = java.util.Calendar.getInstance()
            y.add(java.util.Calendar.DAY_OF_YEAR, -1)
            cal.get(java.util.Calendar.YEAR) == y.get(java.util.Calendar.YEAR) &&
                cal.get(java.util.Calendar.DAY_OF_YEAR) == y.get(java.util.Calendar.DAY_OF_YEAR)
        }
        val sameYear = cal.get(java.util.Calendar.YEAR) == now.get(java.util.Calendar.YEAR)
        return when {
            !exact -> when {
                sameDay -> "今天"
                yesterday -> "昨天"
                sameYear -> java.text.SimpleDateFormat("MM-dd", java.util.Locale.getDefault()).format(java.util.Date(ts))
                else -> java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date(ts))
            }
            sameDay -> java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(java.util.Date(ts))
            yesterday -> "昨天 " + java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(java.util.Date(ts))
            sameYear -> java.text.SimpleDateFormat("MM-dd HH:mm", java.util.Locale.getDefault()).format(java.util.Date(ts))
            else -> java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault()).format(java.util.Date(ts))
        }
    }

    /** ts 的自然日键（同年同日相等），供线程按天分组 */
    private fun dayKey(ts: Long): Long {
        val c = java.util.Calendar.getInstance()
        c.timeInMillis = ts
        return c.get(java.util.Calendar.YEAR).toLong() * 1000L + c.get(java.util.Calendar.DAY_OF_YEAR)
    }

    /** 与某人的私信线程（时间正序，用于聊天界面）：
     *  先按 ts 自然日分组，同一天内按 seq（写入顺序）。通知只提供天级时间，
     *  同日先后无法从时间戳分出，靠写入序保证稳定；旧记录无 seq 时按数组位置兜底
     *  （数组最新在前，位置越靠后越早 → 兜底 seq = length - index 保持旧数据先后）。 */
    fun pmThread(partnerId: Long): List<sb.linux.client.domain.community.PmMessage> {
        val arr = pmArray()
        val len = arr.length()
        return buildList {
            for (i in 0 until len) {
                val o = arr.optJSONObject(i) ?: continue
                if (o.optLong("partnerId") != partnerId) continue
                val ts = o.optLong("ts", 0L)
                val seq = if (o.has("seq")) o.optLong("seq") else (len - i).toLong()
                val exact = o.optBoolean("timeExact", !o.optBoolean("incoming"))
                add(
                    sb.linux.client.domain.community.PmMessage(
                        partnerId = partnerId,
                        partnerName = o.optString("partnerName"),
                        avatarUrl = o.optString("avatarUrl"),
                        incoming = o.optBoolean("incoming"),
                        content = o.optString("content"),
                        timeText = relativeTimeText(ts, exact),
                        ts = ts,
                        seq = seq,
                        timeExact = exact,
                        quoteText = o.optString("quote"),
                        synthetic = o.optBoolean("synthetic"),
                    )
                )
            }
        }.sortedWith(compareBy({ dayKey(it.ts) }, { it.seq }))
    }

    /** 源站完整线程覆盖该联系人本地快照，保留其他会话；避免同文消息被错误去重。 */
    fun savePmThread(thread: DirectMessageThread) {
        val out = JSONArray()
        val old = pmArray()
        for (i in 0 until old.length()) old.optJSONObject(i)?.takeIf { it.optLong("partnerId") != thread.partnerId }?.let(out::put)
        thread.messages.forEachIndexed { index, message ->
            out.put(JSONObject().put("partnerId", thread.partnerId).put("partnerName", thread.partnerName)
                .put("avatarUrl", thread.avatarUrl).put("incoming", message.incoming).put("content", message.content)
                .put("ts", message.ts).put("seq", index.toLong()).put("timeExact", true).put("read", true)
                .put("quote", message.quoteText))
        }
        prefs.edit().putString("local_pm_messages", out.toString()).apply()
    }

    /** 把某人的未读私信标记为已读 */
    fun markPmRead(partnerId: Long) {
        val arr = pmArray()
        val out = JSONArray()
        for (i in 0 until arr.length()) {
            val o = JSONObject(arr.optJSONObject(i).toString())
            if (o.optLong("partnerId") == partnerId && !o.optBoolean("read", false)) o.put("read", true)
            out.put(o)
        }
        prefs.edit().putString("local_pm_messages", out.toString()).apply()
    }

    // ---------------- 设置导出 / 导入 ----------------
}
