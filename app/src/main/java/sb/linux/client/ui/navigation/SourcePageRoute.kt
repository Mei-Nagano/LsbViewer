package sb.linux.client.ui.navigation

import android.net.Uri
import sb.linux.client.common.network.Endpoints

/** 明确映射已实现的页面；未知源站路径保留完整 URL，由站内网页承载。 */
internal fun sourcePageRoute(url: String, accountId: Long): String? {
    val uri = Uri.parse(Endpoints.abs(url))
    if (!Endpoints.isForumHost(uri.host)) return null
    val nativeUrl = uri.buildUpon().scheme("https").authority(Endpoints.HOST).build().toString()
    val path = uri.path.orEmpty().trimEnd('/').ifBlank { "/" }
    fun query(name: String, default: String = "") = uri.getQueryParameter(name) ?: default
    fun encoded(value: String) = Uri.encode(value)
    fun accountPage(title: String) = "accountPage?title=${encoded(title)}&url=${encoded(nativeUrl)}"
    if (path in setOf("/", "/index.php") && query("sort") == "following" &&
        (query("follow_view") in setOf("topics", "following", "followers") || query("follow_uid").isNotBlank()))
        return "followList?url=${encoded(nativeUrl)}"
    if (sourceHomeSelection(url) != null) return "home"
    when (path) {
        "/login" -> return "login"
        "/forum_list" -> return "forums"
        "/nodes" -> return "nodes?url=${encoded(nativeUrl)}"
        "/direct_messages" -> return "directMessages"
        "/notifications" -> return "notifications"
        "/invite_center" -> return "inviteCenter"
        "/community_wallet" -> return "communityWallet"
        "/promotions" -> return accountPage("我的广告")
        "/post_signature" -> return "personalSignature"
        "/user_privacy" -> return "userPrivacy"
        "/user_block" -> return "userBlock?url=${encoded(nativeUrl)}"
        "/gacha_profile" -> return "gachaPage?url=${encoded(nativeUrl)}"
        "/community_activity" -> return accountPage("活跃度")
        "/identity_center" -> return "identityCenter"
        "/topic_collections" -> return "nodes?url=${encoded(if (query("tab") == "everyone") "/nodes?tab=popular" else "/nodes?tab=mine")}"
        "/topic_edit" -> return "editorPage?url=${encoded(nativeUrl)}&tid=${query("topic_id", query("id", "0")).toLongOrNull() ?: 0}"
        "/donate" -> query("topic_id").toLongOrNull()?.takeIf { it > 0 }?.let { return "topic/$it?donate=true" }
        "/content_report" -> query("target_id").toLongOrNull()?.let { return "report/${encoded(query("type", "topic"))}/$it" }
        "/leaderboard" -> return "leaderboard?type=${encoded(query("type", "points"))}"
        "/search" -> return if (query("scope") == "node") "web?url=${encoded(nativeUrl)}&inApp=true"
            else "search?q=${encoded(query("q", query("keyword")))}&scope=${encoded(query("scope", query("field", "all")))}&sort=${encoded(query("sort", "relevance"))}"
        "/index.php" -> {
            if (query("a") == "user_block") return "userBlock?url=${encoded(nativeUrl)}"
            if (query("sort") == "following" && query("follow_view") in setOf("following", "followers"))
                return "followList?url=${encoded(nativeUrl)}"
        }
        "/user" -> {
            val uid = query("uid").toLongOrNull()
            val tab = query("tab", "topics")
            if (uid != null && uid > 0) return userRoute(uid, tab, nativeUrl, accountId)
            if (query("username").isNotBlank()) return "userByName?name=${encoded(query("username"))}&tab=${encoded(tab)}"
        }
    }
    if (Regex("/node/[0-9]+").matches(path)) return "nodes?url=${encoded(nativeUrl)}"
    if (path == "/gacha" || path.startsWith("/gacha_")) return "gachaPage?url=${encoded(nativeUrl)}"
    Regex("^/topic_edit/(\\d+)$").find(path)?.let { return "editorPage?url=${encoded(nativeUrl)}&tid=${it.groupValues[1]}" }
    Regex("^/community_activity/\\d+$").takeIf { it.matches(path) }?.let { return accountPage("活跃度") }
    Regex("^/content_report/(\\d+)$").find(path)?.let { return "report/${encoded(query("type", "topic"))}/${it.groupValues[1]}" }
    Regex("^/direct_messages/(\\d+)$").find(path)?.let { return "chat/${it.groupValues[1]}" }
    Regex("^/topic_collection/\\d+$").takeIf { it.matches(path) }?.let { return "nodes?url=${encoded("/nodes?tab=popular")}" }
    Regex("^/topic_collection_manage(?:/\\d+)?$").takeIf { it.matches(path) }?.let { return "nodes?url=${encoded("/nodes?tab=mine")}" }
    Regex("^/forum/(\\d+)$").find(path)?.let { return "forum/${it.groupValues[1]}?p=${query("p", "1").toIntOrNull()?.coerceAtLeast(1) ?: 1}" }
    Regex("^/user/(\\d+)$").find(path)?.let { return userRoute(it.groupValues[1].toLong(), query("tab", "topics"), nativeUrl, accountId) }
    Regex("^/topic/(\\d+)$").find(path)?.let {
        if (query("poll_view").isNotBlank()) return "pollPage?url=${encoded(nativeUrl)}"
        val page = query("p").toIntOrNull()?.coerceAtLeast(1) ?: 0
        val floor = query("floor", "0").toIntOrNull()?.coerceAtLeast(0) ?: 0
        val replyId = query("replyid", "0").toLongOrNull()?.coerceAtLeast(0) ?: 0
        return "topic/${it.groupValues[1]}?p=$page&floor=$floor&replyid=$replyId"
    }
    return null
}

/** 主列表的 URL 参数仍作用于原生列表，双栏时交给主导航。 */
internal fun sourceHomeSelection(url: String): Pair<Int, Int>? {
    val uri = Uri.parse(Endpoints.abs(url))
    if (!Endpoints.isForumHost(uri.host)) return null
    val path = uri.path.orEmpty().trimEnd('/').ifBlank { "/" }
    if (path !in setOf("/", "/index.php", "/topic_featured", "/hot_discussions", "/unread_topic_notice_footprint", "/topic_essence_review_list") ||
        !uri.getQueryParameter("a").isNullOrBlank() || uri.getQueryParameter("follow_view") in setOf("topics", "following", "followers") ||
        (uri.getQueryParameter("sort") == "following" && !uri.getQueryParameter("follow_uid").isNullOrBlank())) return null
    val sort = when (path) {
        "/topic_featured" -> "featured"; "/hot_discussions" -> "hot"
        "/unread_topic_notice_footprint" -> "footprint"; "/topic_essence_review_list" -> "essence"
        else -> uri.getQueryParameter("sort") ?: "comment"
    }
    val combo = when (sort) { "lucky" -> 1; "card" -> 2; "essence" -> 3; "red_packet" -> 4; else -> 0 }
    val tab = listOf("comment", "post", "featured", "hot", "following", "footprint").indexOf(sort)
    return if (combo > 0 || tab >= 0) combo to if (combo > 0) 1 else tab else null
}

private fun userRoute(uid: Long, tab: String, url: String, accountId: Long): String? = when (tab) {
    "attachments" -> if (uid == accountId) "accountPage?title=${Uri.encode("附件管理")}&url=${Uri.encode(url)}" else null
    "notifications" -> if (uid == accountId) "notifications" else null
    "topics", "replies", "favorites", "points_rewards" -> "user/$uid?tab=${Uri.encode(tab)}"
    else -> null
}
