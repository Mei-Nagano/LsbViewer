package sb.linux.client.data

import sb.linux.client.domain.account.CheckinInfo

object CheckinHtmlParser {
    fun parseCheckin(html: String): CheckinInfo {
        val d = SourceHtmlSupport.sourceDocument(html)
        if (d.selectFirst("form[action=/login], input[name=username], input[name=password]") != null) {
            return CheckinInfo(canCheckin = false, checkedToday = false)
        }
        val full = (d.selectFirst("main")?.text() ?: d.body()?.text() ?: "")
            .replace(Regex("""\s+"""), " ").trim()
        val canCheckin = d.selectFirst("form[action=/daily_checkin], form[action*=/daily_checkin]") != null
        val streak = listOf(
            Regex("""连续签到\s*(\d+)\s*天"""),
            Regex("""已连续签到\s*(\d+)\s*天"""),
            Regex("""连续签到\s*[:：]?\s*(\d+)"""),
            Regex("""连续天数\s*[:：]?\s*(\d+)"""),
            Regex("""连续\s*(\d+)\s*天"""),
            Regex("""(\d+)\s*连续天数"""),
            Regex("""(\d+)\s*连续签到"""),
            Regex("""(\d+)\s*连续\s*天"""),
        ).mapNotNull { it.find(full)?.groupValues?.get(1)?.toIntOrNull() }.firstOrNull() ?: 0
        val total = listOf(
            Regex("""累计签到\s*[:：]?\s*(\d+)"""),
            Regex("""累计天数\s*[:：]?\s*(\d+)"""),
            Regex("""(?:累计|累积|总计|总共)签到\s*(\d+)\s*天"""),
            Regex("""累计\s*(\d+)\s*天"""),
            Regex("""(\d+)\s*累计签到"""),
            Regex("""(\d+)\s*累计天数"""),
            Regex("""(\d+)\s*累计\s*天"""),
        ).mapNotNull { it.find(full)?.groupValues?.get(1)?.toIntOrNull() }.firstOrNull() ?: 0
        val doneWords = listOf(
            "今日已签到", "今天已签到", "今日已经签到", "已经签到",
            "签到成功", "已完成", "明日再来", "明天再来",
        )
        val checkedToday = doneWords.any { full.contains(it) } || !canCheckin && full.contains("签到")
        val core = d.selectFirst(
            ".daily-checkin-message, .daily-checkin-result, .home-signin-tip, .main-panel .notice, .main-panel .alert"
        )?.text()?.replace(Regex("""\s+"""), " ")?.trim()
            ?: Regex("""(签到成功[^。！!]{0,40}|已连续签到\s*\d+\s*天[^。！!]{0,40}|今日已签到)""")
                .find(full)?.value ?: ""
        return CheckinInfo(
            streak = streak,
            total = total,
            canCheckin = canCheckin,
            checkedToday = checkedToday,
            message = core,
        )
    }

}
