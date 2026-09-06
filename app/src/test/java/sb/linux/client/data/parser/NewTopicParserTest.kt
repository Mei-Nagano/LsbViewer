package sb.linux.client.data.parser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NewTopicParserTest {
    @Test
    fun `parses forum and edit metadata from source form`() {
        val html = """
            <form data-confirm="需支付编辑费用">
              <input name="title" value="原标题">
              <textarea name="body">正文</textarea>
              <input name="id" value="42">
              <input type="hidden" name="sb_limit_edit_time_42" value="token">
              <select name="forum_id">
                <option value="1">公告</option><option value="2" selected>闲聊</option>
              </select>
              <select name="reply_order"><option value="asc">正序</option><option value="desc" selected>倒序</option></select>
              <input name="topic_special_type" value="lottery" checked>
            </form>
            <div class="sb-limit-edit-time-quote">编辑将在 10 分钟内完成</div>
            <div class="community-lottery-publish-warning">请勿发布虚假抽奖</div>
            <div class="community-lottery-rule-note">达到人数后开奖</div>
        """.trimIndent()

        assertEquals(listOf(ForumOption("1", "公告"), ForumOption("2", "闲聊")), parseForums(html))
        assertEquals(Triple("原标题", "正文", "42"), parseEditValues(html))
        assertEquals("2", parseSelectedForumId(html))
        assertEquals("desc" to listOf("asc" to "正序", "desc" to "倒序"), parseReplyOrderOptions(html))
        assertEquals("lottery", parseSpecialType(html))
        assertEquals("需支付编辑费用", parseSourceEditMeta(html).confirmation)
        assertEquals("编辑将在 10 分钟内完成", parseSourceEditMeta(html).notice)
        assertEquals(mapOf("sb_limit_edit_time_42" to "token"), parseSourceEditMeta(html).hiddenFields)
        assertEquals("请勿发布虚假抽奖\n\n达到人数后开奖", parseAnnouncement(html))
    }

    @Test
    fun `parses lottery defaults and prize rows`() {
        val html = """
            <form><input name="title" value="标题">
              <input name="lottery_draw_at" value="2026-09-06T20:00">
              <input name="lottery_participant_target" value="20">
              <input name="lottery_min_reply_chars" value="8">
              <input name="lottery_reply_captcha_required" checked>
              <div class="community-lottery-prize-row">
                <input name="lottery_prize_name[]" value="烧饼">
                <select name="lottery_prize_type[]"><option value="wallet" selected>烧饼</option></select>
                <input name="lottery_prize_quantity[]" value="2">
                <textarea name="lottery_prize_value[]">10</textarea>
              </div>
            </form>
        """.trimIndent()

        val result = parseLotteryInit(html)
        assertEquals("2026-09-06T20:00", result.drawAt)
        assertEquals("20", result.target)
        assertEquals("8", result.minChars)
        assertTrue(result.captchaRequired)
        assertEquals(listOf(PrizeRow("烧饼", "wallet", "2", "10")), result.prizes)
    }

    @Test
    fun `parses card defaults and fallback values`() {
        val result = parseCardInit("<form><input name='title' value='标题'><input name='virtual_card_name' value='月卡'></form>")

        assertEquals("月卡", result.name)
        assertEquals("points", result.currency)
        assertEquals("1", result.price)
        assertEquals("1", result.limit)
        assertFalse(result.autoReply)
        assertEquals("已成功兑换虚拟卡「{card_name}」。", result.autoReplyContent)
        assertEquals("", result.values)
    }
}
