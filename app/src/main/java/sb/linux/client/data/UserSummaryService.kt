package sb.linux.client.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import sb.linux.client.common.network.Endpoints
import sb.linux.client.domain.account.UserSummary
import sb.linux.client.domain.account.UserSummaryRepository
import sb.linux.client.domain.account.UserSummaryStat

/** Use the same compact card endpoint and visibility flags as the website. */
internal class UserSummaryService(private val client: LsbClient) : UserSummaryRepository {
    override suspend fun toggleBlock(userId: Long): UserSummary = withContext(Dispatchers.IO) {
        val user = load(userId)
        check(user.canBlock && !user.self) { "无法屏蔽此用户" }
        // The site supplies the filter endpoint; do not guess a route or replace other rules.
        val document = SourceHtmlSupport.sourceDocument(client.get("/").html)
        val base = document.selectFirst("[data-bio-card-api]")?.attr("data-bio-card-filter").orEmpty()
        val uri = android.net.Uri.parse(Endpoints.abs(base))
        check(base.isNotBlank() && Endpoints.isInternal(uri.host)) { "原站未提供屏蔽入口" }
        val original = JSONObject(client.get(base).html)
        check(original.optInt("ok") == 1) { "读取屏蔽设置失败" }
        val settings = original.getJSONObject("settings")
        val old = settings.optJSONArray("blocked_users") ?: org.json.JSONArray()
        val names = (0 until old.length()).map { old.getString(it) }
        val removing = names.any { it.equals(user.username, ignoreCase = true) }
        val updated = if (removing) names.filterNot { it.equals(user.username, ignoreCase = true) }
            else names + user.username
        val pairs = mutableListOf(
            "action" to "save", "_csrf" to original.optString("csrf"),
            "user_enabled" to if (settings.optBoolean("enabled") || settings.optInt("enabled") == 1) "1" else "",
            "show_notice" to if (settings.optBoolean("show_notice") || settings.optInt("show_notice") == 1) "1" else "",
        )
        for (key in listOf("preset_keywords", "custom_keywords", "blocked_forums", "regex_keywords")) {
            val values = settings.optJSONArray(key) ?: org.json.JSONArray()
            for (index in 0 until values.length()) pairs += "$key[]" to values.optString(index)
        }
        updated.forEach { pairs += "blocked_users[]" to it }
        val saved = JSONObject(client.postFormPairs(base, pairs).html)
        check(saved.optInt("ok") == 1) { saved.optString("message").ifBlank { "保存失败" } }
        val verified = JSONObject(client.get(base).html)
        check(verified.optInt("ok") == 1) { "无法确认屏蔽状态" }
        val users = verified.getJSONObject("settings").optJSONArray("blocked_users") ?: org.json.JSONArray()
        val now = (0 until users.length()).any { users.getString(it).equals(user.username, ignoreCase = true) }
        check(now != removing) { "屏蔽设置未生效，可能已达到原站上限" }
        user.copy(blocked = now)
    }

    override suspend fun load(userId: Long): UserSummary = withContext(Dispatchers.IO) {
        require(userId > 0)
        val response = client.get("/bio_card_api?id=$userId")
        val data = JSONObject(response.html)
        check(data.optInt("ok") == 1 || data.optBoolean("ok")) { "无法加载用户信息" }
        val user = data.getJSONObject("user")
        check(user.optLong("id") == userId) { "用户信息不匹配" }
        fun flag(key: String) = user.optBoolean(key) || user.optInt(key) == 1
        fun text(key: String) = if (user.isNull(key)) "" else user.optString(key)
        fun url(key: String): String {
            val raw = text(key)
            if (raw.isBlank()) return ""
            val absolute = Endpoints.abs(raw)
            return absolute.takeIf { it.startsWith("https://") || it.startsWith("http://") }.orEmpty()
        }
        val stats = buildList {
            if (!user.isNull("level")) add(UserSummaryStat("等级", "Lv ${text("level")}"))
            if (!user.isNull("topics")) add(UserSummaryStat("主题", text("topics"), url("topics_url")))
            if (!user.isNull("points")) add(UserSummaryStat("积分", text("points")))
            if (!user.isNull("replies")) add(UserSummaryStat("回复", text("replies"), url("replies_url")))
        }
        UserSummary(
            id = userId, username = text("username"), avatar = url("avatar"), home = url("home"),
            subtitle = listOf("group_name", "level_name", "created_at").map(::text)
                .filter(String::isNotBlank).joinToString(" · "),
            bio = text("bio"), stats = stats, canBlock = flag("can_block"), blocked = flag("blocked"),
            self = flag("self"), canFollow = flag("can_follow"), canNotify = flag("can_notify"),
            notify = url("notify"),
        )
    }
}
