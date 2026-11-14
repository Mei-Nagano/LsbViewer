package sb.linux.client.util

import sb.linux.client.domain.community.FollowingUser

/** 相对文案没有秒级精度：按源站单位更新缓存龄，不伪造绝对日期。 */
internal fun followingMeta(user: FollowingUser, now: Long): String {
    if (user.followTimeText.isBlank() && user.followedAt == null) return user.meta
    val time = user.followedAt?.let { TimeFormat.relative(it) + "关注" } ?: run {
        val original = user.followTimeText
        val units = mapOf("秒" to 1L, "分钟" to 60L, "小时" to 3600L, "天" to 86400L,
            "个月" to 2_592_000L, "月" to 2_592_000L, "年" to 31_536_000L)
        val match = Regex("(\\d+)\\s*(秒|分钟|小时|天|个月|月|年)前关注").matchEntire(original)
        val elapsed = (now - user.capturedAt).coerceAtLeast(0)
        if (user.capturedAt <= 0 || elapsed < 60 || (match == null && original != "刚刚关注")) original
        else {
            val base = match?.let { it.groupValues[1].toLong() * units.getValue(it.groupValues[2]) } ?: 0
            val age = base + elapsed
            when {
                age < 60 -> "刚刚关注"
                age < 3600 -> "${age / 60}分钟前关注"
                age < 86400 -> "${age / 3600}小时前关注"
                age < 2_592_000 -> "${age / 86400}天前关注"
                age < 31_536_000 -> "${age / 2_592_000}个月前关注"
                else -> "${age / 31_536_000}年前关注"
            }
        }
    }
    return listOf(user.followersText, time).filter(String::isNotBlank).joinToString(" · ")
}
