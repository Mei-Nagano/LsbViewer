package sb.linux.client.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 抽奖帖面板解析回归测试。
 * HTML 片段取自源站 linux.sb 真实抽奖帖（v8.6.5）：
 * 面板 section.community-lottery-card 渲染在主楼 .post-content 内，
 * 楼层解析会把它从 DOM 移除——面板解析必须先于楼层解析，否则永远取不到。
 */
class HtmlParserLotteryTest {

    /** 已开奖抽奖帖（含中奖名单），结构取自 /topic/14839 */
    private val drawnHtml = """
        <html><body>
        <div class="post-topic-title"><h1 class="post-content-title">邀请链接抽奖</h1>
        <span class="community-lottery-title-status drawn">已开奖</span></div>
        <ul class="post-list topic-post-list">
        <li class="post-item post-entry" id="post-14839" data-floor="1">
          <div class="post-avatar"><a class="avatar-profile-link" href="/user/950"><img class="avatar-img" src="/a.jpg"></a></div>
          <div class="post-body">
            <div class="post-head"><a class="post-title post-author" href="/user/950">SpaceXAI</a>
            <span class="post-user-group user-uid-badge" title="用户 UID">UID 950</span></div>
            <div class="post-meta"><span data-performance-time="1787370078">50分钟前</span></div>
          </div>
          <div class="post-content">
            <p>正文内容</p>
            <section class="community-lottery-card is-drawn"><header>
              <div><b>抽奖帖</b><span>本次抽奖已经完成</span></div>
              <div class="community-lottery-card-side"><strong class="community-lottery-status-pill">已开奖</strong><em>20 人参与</em></div>
            </header>
            <ul class="community-lottery-prizes"><li><strong>邀请链接（最后是邀请码）</strong><span>兑换码 · 5 份</span></li></ul>
            <div class="community-lottery-condition">到 2026-08-22 11:59 或 满 20 人自动开奖</div>
            <div class="community-lottery-result">实际中奖 5 人</div>
            <div class="community-lottery-winners"><strong>中奖名单</strong><ul>
              <li><a href="/user/22990">skillmd</a><span>邀请链接（最后是邀请码）</span></li>
              <li><a href="/user/3923">buyi</a><span>邀请链接（最后是邀请码）</span></li>
            </ul></div></section>
          </div>
        </li>
        </ul>
        </body></html>
    """.trimIndent()

    /** 抽奖中帖子（无名单），结构取自 /topic/14946 */
    private val openHtml = """
        <html><body>
        <div class="post-topic-title"><h1 class="post-content-title">国模公益站 100$ 30个</h1>
        <span class="community-lottery-title-status open">抽奖中</span></div>
        <ul class="post-list topic-post-list">
        <li class="post-item post-entry" id="post-14946" data-floor="1">
          <div class="post-content">
            <p>正文</p>
            <section class="community-lottery-card is-open"><header>
              <div><b>抽奖帖</b><span>回复满 5 字即参与，需完成人机验证</span></div>
              <div class="community-lottery-card-side"><strong class="community-lottery-status-pill">抽奖中</strong><em>5 人参与</em></div>
            </header>
            <ul class="community-lottery-prizes"><li><strong>100$ * 30</strong><span>兑换码 · 30 份</span></li></ul>
            <div class="community-lottery-condition">满 288 人自动开奖</div></section>
          </div>
        </li>
        </ul>
        </body></html>
    """.trimIndent()

    @Test
    fun `已开奖面板完整解析`() {
        val page = HtmlParser.parseTopicPage(drawnHtml, fallbackId = 14839)
        val lp = page.lottery ?: error("抽奖面板必须被解析到（楼层解析会移除该节点）")
        assertEquals("已开奖", lp.status)
        assertEquals("本次抽奖已经完成", lp.note)
        assertEquals("20 人参与", lp.participants)
        assertEquals("邀请链接（最后是邀请码） · 兑换码 · 5 份", lp.prizes.single())
        assertEquals("到 2026-08-22 11:59 或 满 20 人自动开奖", lp.condition)
        assertEquals("实际中奖 5 人", lp.result)
        assertEquals(2, lp.winners.size)
        assertEquals("skillmd", lp.winners[0].user)
        assertEquals("邀请链接（最后是邀请码）", lp.winners[0].prize)
        assertEquals("buyi", lp.winners[1].user)
        // 正文不应混入抽奖面板
        assertTrue(page.posts.first().contentHtml.isNotBlank())
        assertTrue(!page.posts.first().contentHtml.contains("community-lottery"))
    }

    @Test
    fun `抽奖中面板解析`() {
        val page = HtmlParser.parseTopicPage(openHtml, fallbackId = 14946)
        val lp = page.lottery ?: error("抽奖中面板必须被解析到")
        assertEquals("抽奖中", lp.status)
        assertEquals("回复满 5 字即参与，需完成人机验证", lp.note)
        assertEquals("5 人参与", lp.participants)
        assertEquals("100\$ * 30 · 兑换码 · 30 份", lp.prizes.single())
        assertEquals("满 288 人自动开奖", lp.condition)
        assertTrue(lp.winners.isEmpty())
    }

    @Test
    fun `标题状态徽章与楼层 UID 解析`() {
        val page = HtmlParser.parseTopicPage(drawnHtml, fallbackId = 14839)
        assertEquals("已开奖", page.titleTag)
        val post = page.posts.first()
        assertEquals(950L, post.authorUid)
        assertEquals("SpaceXAI", post.authorName)
        assertEquals(950L, post.authorId)
    }
}
