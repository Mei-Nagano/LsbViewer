package sb.linux.client.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withContext
import org.json.JSONObject
import sb.linux.client.common.network.Endpoints
import sb.linux.client.domain.account.UserSummary
import sb.linux.client.domain.account.UserSummaryRepository
import sb.linux.client.domain.account.UserSummaryStat

/** Use the same compact card endpoint and visibility flags as the website. */
internal class UserSummaryService(private val client: LsbClient, private val currentUserId: () -> Long = { 0L }) : UserSummaryRepository {
    override suspend fun toggleFollow(userId: Long): UserSummary = withContext(Dispatchers.IO) {
        val owner = currentUserId()
        check(owner > 0 && owner != userId) { "登录后才能关注其他用户" }
        val user = load(userId)
        check(user.canFollow && !user.self) { "当前无法关注此用户" }
        val expected = !user.following
        val csrf = client.csrf()
        check(owner == currentUserId()) { "账号已变化，请重新加载" }
        val result = client.postAjax("/user_follow_toggle", mapOf(
            "_csrf" to csrf, "target_id" to userId.toString(),
            "follow_action" to if (expected) "follow" else "unfollow",
        ))
        check(owner == currentUserId()) { "账号已变化，请重新加载" }
        check(result.optBoolean("ok") || result.optInt("ok") == 1) {
            result.optString("message").ifBlank { "关注操作失败" }
        }
        val updated = load(userId)
        check(owner == currentUserId() && updated.following == expected) { "源站未确认关注状态，请重新加载" }
        updated
    }

    override suspend fun toggleBlock(userId: Long): UserSummary = withContext(Dispatchers.IO) {
        val user = load(userId)
        check(user.canBlock && !user.self) { "无法屏蔽此用户" }
        val owner = currentUserId()
        val blocked = UnifiedUserBlockService(client).toggle(userId, user.username)
        check(owner == currentUserId()) { "账号已变化，请重新加载" }
        user.copy(blocked = blocked)
    }

    override suspend fun load(userId: Long): UserSummary = withContext(Dispatchers.IO) {
        require(userId > 0)
        val owner = currentUserId()
        val response = client.get("/bio_card_api?id=$userId")
        val data = JSONObject(response.html)
        check(data.optInt("ok") == 1 || data.optBoolean("ok")) { "无法加载用户信息" }
        val user = data.getJSONObject("user")
        check(user.optLong("id") == userId) { "用户信息不匹配" }
        val followState = if (owner > 0) {
            try {
                client.getAjaxJsonForRepository("/user_follow_state?id=$userId").takeIf {
                    (it.optBoolean("ok") || it.optInt("ok") == 1) && it.optLong("id") == userId
                }
            } catch (cancelled: CancellationException) { throw cancelled
            } catch (_: Exception) { null }
        } else null
        fun flag(key: String) = user.optBoolean(key) || user.optInt(key) == 1
        fun text(key: String) = if (user.isNull(key)) "" else user.optString(key)
        fun url(key: String): String {
            val raw = text(key)
            if (raw.isBlank()) return ""
            val absolute = Endpoints.abs(raw)
            return absolute.takeIf { it.startsWith("https://") || it.startsWith("http://") }.orEmpty()
        }
        val stats = buildList {
            listOf("points" to "积分", "wallet" to "烧饼", "followers" to "粉丝",
                "following_count" to "关注", "topics" to "主题", "replies" to "回复").forEach { (key, label) ->
                val source = followState?.takeIf { !it.isNull(key) } ?: user
                if (!source.isNull(key)) {
                    val urlKey = if (key == "following_count") "following_url" else "${key}_url"
                    val rawUrl = source.optString(urlKey).ifBlank { text(urlKey) }
                    add(UserSummaryStat(label, source.optString(key),
                        rawUrl.takeIf(String::isNotBlank)?.let(Endpoints::abs).orEmpty()))
                }
            }
            if (!user.isNull("level")) add(UserSummaryStat("等级", "Lv ${text("level")}"))
        }
        val self = userId == owner || flag("self")
        val relation = if (owner > 0 && !self) runCatching { UnifiedUserBlockService(client).isBlocked(userId) } else null
        val canBlock = relation?.isSuccess == true
        val blocked = relation?.getOrNull() ?: false
        check(owner == currentUserId()) { "账号已变化，请重新加载" }
        UserSummary(
            id = userId, username = text("username"), avatar = url("avatar"), home = url("home"),
            subtitle = listOf("group_name", "level_name", "created_at").map(::text)
                .filter(String::isNotBlank).joinToString(" · "),
            bio = text("bio"), stats = stats, canBlock = canBlock, blocked = blocked,
            self = self, canFollow = !self && (flag("can_follow") || followState?.optBoolean("can_follow") == true || followState?.optInt("can_follow") == 1),
            canNotify = !self && (flag("can_notify") || url("notify").isNotBlank()),
            notify = url("notify"),
            following = followState?.let { it.optBoolean("following") || it.optInt("following") == 1 } ?: flag("following"),
        )
    }
}
