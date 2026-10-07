package sb.linux.client.data

import android.content.SharedPreferences

internal class AppSettingsAccountStore(private val prefs: SharedPreferences) {
    private val todayKey: String
        get() {
            val c = java.util.Calendar.getInstance()
            return "%04d-%02d-%02d".format(c.get(java.util.Calendar.YEAR),
                c.get(java.util.Calendar.MONTH) + 1, c.get(java.util.Calendar.DAY_OF_MONTH))
        }

    /** 今日是否已自动签到 */
    fun checkinDoneToday(): Boolean = prefs.getString("last_checkin", "") == todayKey

    fun markCheckinDone() = prefs.edit().putString("last_checkin", todayKey).apply()

    /** 缓存本次签到摘要（连续/累计天数），跨天或未缓存时返回 null；用于避免每次启动重复查询源站 */
    fun saveCheckinSummary(streak: Int, total: Int) {
        prefs.edit()
            .putString("last_checkin_date", todayKey)
            .putInt("last_checkin_streak", streak)
            .putInt("last_checkin_total", total)
            .apply()
    }

    /** 今日已签到时的本地签到摘要；跨天或从未缓存返回 null */
    fun checkinSummaryToday(): Pair<Int, Int>? {
        if (!checkinDoneToday() || prefs.getString("last_checkin_date", "") != todayKey) return null
        val streak = prefs.getInt("last_checkin_streak", 0)
        val total = prefs.getInt("last_checkin_total", 0)
        if (streak == 0 && total == 0) return null
        return streak to total
    }

    /** 清除签到缓存（切换账号时调用，避免显示旧账号的签到信息） */
    fun clearCheckinCache() {
        prefs.edit()
            .remove("last_checkin")
            .remove("last_checkin_date")
            .remove("last_checkin_streak")
            .remove("last_checkin_total")
            .apply()
    }

    // ---------------- 记住密码（登录页） ----------------

    var rememberPassword: Boolean
        get() = prefs.getBoolean("remember_password", false)
        set(v) = prefs.edit().putBoolean("remember_password", v).apply()

    var savedUsername: String
        get() = prefs.getString("saved_username", "") ?: ""
        set(v) = prefs.edit().putString("saved_username", v).apply()

    /** 仅做混淆存储（Base64），非加密保存 */
    var savedPassword: String
        get() = runCatching {
            String(android.util.Base64.decode(prefs.getString("saved_password", "") ?: "", android.util.Base64.NO_WRAP))
        }.getOrDefault("")
        set(v) = prefs.edit().putString(
            "saved_password",
            android.util.Base64.encodeToString(v.toByteArray(), android.util.Base64.NO_WRAP)
        ).apply()

    fun clearSavedPassword() =
        prefs.edit().remove("saved_password").remove("saved_username").apply()

    // ---------------- 本地浏览历史（不解析源站足迹） ----------------
}
