package sb.linux.client.data


object ForumTimeParser {
    /** 源站相对时间 → 时间戳：支持「刚刚/N秒/N分钟/N小时/N天/周/月/年前、昨天/前天」与常见日期格式；解析失败返回 0 */
    fun parseRelativeTime(text: String): Long {
        val t = text.trim()
        if (t.isBlank()) return 0L
        val now = System.currentTimeMillis()
        Regex("""(\d+)\s*秒前""").find(t)?.let { return now - it.groupValues[1].toLong() * 1000L }
        Regex("""(\d+)\s*分钟前""").find(t)?.let { return now - it.groupValues[1].toLong() * 60_000L }
        Regex("""(\d+)\s*小时前""").find(t)?.let { return now - it.groupValues[1].toLong() * 3_600_000L }
        Regex("""(\d+)\s*天前""").find(t)?.let { return now - it.groupValues[1].toLong() * 86_400_000L }
        Regex("""(\d+)\s*周前""").find(t)?.let { return now - it.groupValues[1].toLong() * 7L * 86_400_000L }
        Regex("""(\d+)\s*月前""").find(t)?.let { return now - it.groupValues[1].toLong() * 30L * 86_400_000L }
        Regex("""(\d+)\s*年前""").find(t)?.let { return now - it.groupValues[1].toLong() * 365L * 86_400_000L }
        // 「昨天/前天」不带具体时刻：按当前时刻回退整/两个自然日（时刻精度源站未提供）
        if (t.contains("前天")) return now - 2L * 86_400_000L
        if (t.contains("昨天")) return now - 86_400_000L
        if (t.contains("刚刚") || t.contains("刚才")) return now
        // 日期格式：yyyy-MM-dd( HH:mm) / MM-dd( HH:mm，跨年自动回退一年)
        runCatching {
            java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.CHINA).parse(t)?.time
        }.getOrNull()?.let { return it }
        runCatching {
            java.text.SimpleDateFormat("MM-dd HH:mm", java.util.Locale.CHINA).parse(t)
        }.getOrNull()?.let { p ->
            val cal = java.util.Calendar.getInstance()
            cal.time = p
            cal.set(java.util.Calendar.YEAR, java.util.Calendar.getInstance().get(java.util.Calendar.YEAR))
            if (cal.timeInMillis > now) cal.add(java.util.Calendar.YEAR, -1)
            return cal.timeInMillis
        }
        runCatching {
            java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.CHINA).parse(t.substringBefore(" "))?.time
        }.getOrNull()?.let { return it }
        runCatching {
            java.text.SimpleDateFormat("MM-dd", java.util.Locale.CHINA).parse(t.substringBefore(" "))
        }.getOrNull()?.let { p ->
            val cal = java.util.Calendar.getInstance()
            cal.time = p
            cal.set(java.util.Calendar.YEAR, java.util.Calendar.getInstance().get(java.util.Calendar.YEAR))
            if (cal.timeInMillis > now) cal.add(java.util.Calendar.YEAR, -1)
            return cal.timeInMillis
        }
        return 0L
    }

}
