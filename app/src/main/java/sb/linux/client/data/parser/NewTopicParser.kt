package sb.linux.client.data.parser

import org.jsoup.Jsoup

internal data class ForumOption(val id: String, val name: String)

/** 奖品行（抽奖帖，对应源站 lottery_prize_name[] 等数组字段）。 */
internal data class PrizeRow(
    val name: String = "",
    val type: String = "points",
    val quantity: String = "1",
    val value: String = "",
)

/** 从 /topic_edit 页面解析可选版块。 */
internal fun parseForums(html: String): List<ForumOption> {
    val document = Jsoup.parse(html, "https://linux.sb")
    return document.select("form select[name=forum_id] option")
        .map { ForumOption(it.attr("value"), it.text()) }
}

/** 解析标题/正文/隐藏 id（编辑模式回填）。 */
internal fun parseEditValues(html: String): Triple<String, String, String> {
    val document = Jsoup.parse(html, "https://linux.sb")
    val form = document.selectFirst("form:has(input[name=title])") ?: return Triple("", "", "0")
    return Triple(
        form.selectFirst("input[name=title]")?.attr("value") ?: "",
        form.selectFirst("textarea[name=body]")?.text() ?: "",
        form.selectFirst("input[name=id]")?.attr("value") ?: "0",
    )
}

internal data class SourceEditMeta(
    val confirmation: String = "",
    val notice: String = "",
    val hiddenFields: Map<String, String> = emptyMap(),
)

/** 保留源站付费编辑确认文案及对应的一次性报价字段。 */
internal fun parseSourceEditMeta(html: String): SourceEditMeta {
    val document = Jsoup.parse(html, "https://linux.sb")
    val form = document.selectFirst("form:has(input[name=title])") ?: return SourceEditMeta()
    val hidden = form.select("input[type=hidden][name]")
        .filter { it.attr("name").startsWith("sb_limit_edit_time_") }
        .associate { it.attr("name") to it.attr("value") }
    return SourceEditMeta(
        confirmation = form.attr("data-confirm").trim(),
        notice = document.selectFirst(".sb-limit-edit-time-quote")?.text()?.trim()
            ?.replace(Regex("\\s+"), " ").orEmpty(),
        hiddenFields = hidden,
    )
}

internal fun parseSelectedForumId(html: String): String {
    val document = Jsoup.parse(html, "https://linux.sb")
    return document.selectFirst("form select[name=forum_id] option[selected]")?.attr("value").orEmpty()
}

/** 编辑页源站回帖排序选项；新建页不存在该控件。 */
internal fun parseReplyOrderOptions(html: String): Pair<String, List<Pair<String, String>>> {
    val select = Jsoup.parse(html, "https://linux.sb").selectFirst("form select[name=reply_order]")
        ?: return "" to emptyList()
    val options = select.select("option").map { it.attr("value") to it.text().trim() }
    val selected = select.selectFirst("option[selected]")?.attr("value") ?: options.firstOrNull()?.first.orEmpty()
    return selected to options
}

/** 解析已选帖子类型（编辑抽奖帖/发卡帖时 radio 带 checked）。 */
internal fun parseSpecialType(html: String): String {
    val document = Jsoup.parse(html, "https://linux.sb")
    val radio = document.selectFirst("input[name=topic_special_type][checked]") ?: return ""
    return radio.attr("value")
}

/** 解析发帖公告：源站编辑页的发布警告（抽奖/发卡面板内），普通帖无则返回空。 */
internal fun parseAnnouncement(html: String): String {
    val document = Jsoup.parse(html, "https://linux.sb")
    val warning = document.selectFirst(".community-lottery-publish-warning, .virtual-card-publish-warning")
        ?.text()?.trim()?.replace(Regex("\\s+"), " ")
    val rule = document.selectFirst(".community-lottery-rule-note")?.text()?.trim()
    return listOfNotNull(warning?.ifBlank { null }, rule?.ifBlank { null }).joinToString("\n\n")
}

internal data class LotteryInit(
    val drawAt: String,
    val target: String,
    val minChars: String,
    val captchaRequired: Boolean,
    val prizes: List<PrizeRow>,
)

/** 解析抽奖面板初始值（编辑模式回填）。 */
internal fun parseLotteryInit(html: String): LotteryInit {
    val document = Jsoup.parse(html, "https://linux.sb")
    val form = document.selectFirst("form:has(input[name=title])")
    fun value(name: String) = form?.selectFirst("[name=$name]")?.attr("value") ?: ""
    val drawAt = value("lottery_draw_at")
    val prizes = (form?.select(".community-lottery-prize-row") ?: emptyList()).map { row ->
        PrizeRow(
            // 数组字段的方括号必须放在带引号的属性值中，Jsoup CSS 解析器不会接受裸转义写法。
            name = row.selectFirst("[name='lottery_prize_name[]']")?.attr("value") ?: "",
            type = row.selectFirst("[name='lottery_prize_type[]'] option[selected]")?.attr("value")
                ?: row.selectFirst("[name='lottery_prize_type[]']")?.selectFirst("option")?.attr("value") ?: "points",
            quantity = row.selectFirst("[name='lottery_prize_quantity[]']")?.attr("value") ?: "1",
            value = row.selectFirst("[name='lottery_prize_value[]']")?.text() ?: "",
        )
    }
    return LotteryInit(
        drawAt = drawAt,
        target = value("lottery_participant_target").ifBlank { "0" },
        minChars = value("lottery_min_reply_chars").ifBlank { "5" },
        captchaRequired = form?.selectFirst("[name=lottery_reply_captcha_required]")?.hasAttr("checked") ?: true,
        prizes = prizes,
    )
}

internal data class CardInit(
    val name: String,
    val currency: String,
    val price: String,
    val limit: String,
    val autoReply: Boolean,
    val autoReplyContent: String,
    val values: String,
)

/** 解析发卡面板初始值（编辑模式回填）。 */
internal fun parseCardInit(html: String): CardInit {
    val document = Jsoup.parse(html, "https://linux.sb")
    val form = document.selectFirst("form:has(input[name=title])")
    fun value(name: String) = form?.selectFirst("[name=$name]")?.attr("value") ?: ""
    return CardInit(
        name = value("virtual_card_name"),
        currency = form?.selectFirst("[name=virtual_card_currency] option[selected]")?.attr("value") ?: "points",
        price = value("virtual_card_price").ifBlank { "1" },
        limit = value("virtual_card_purchase_limit").ifBlank { "1" },
        autoReply = form?.selectFirst("[name=virtual_card_auto_reply]")?.hasAttr("checked") ?: false,
        autoReplyContent = form?.selectFirst("[name=virtual_card_auto_reply_content]")?.text()
            ?: "已成功兑换虚拟卡「{card_name}」。",
        values = form?.selectFirst("[name=virtual_card_values]")?.text() ?: "",
    )
}
