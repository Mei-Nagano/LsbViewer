package sb.linux.client.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GachaPullResultParserTest {
    @Test
    fun pullActionsKeepServerCosts() {
        val center = HtmlParser.parseGachaCenter(
            """
            <main>
              <div class="gacha-actions">
                <form action="/gacha_pull"><button data-cost="10">抽一次 (10 积分)</button></form>
                <form action="/gacha_pull_10"><button data-cost="90">十连抽 (90 积分)</button></form>
                <form action="/gacha_pull_100"><button data-cost="800">百连抽 (800 积分)</button></form>
              </div>
            </main>
            """.trimIndent(),
        )

        assertEquals(listOf(10, 90, 800), center.pullActions.map { it.cost })
    }

    @Test
    fun parsesSinglePullResult() {
        val result = GachaPullResultParser.parse(
            """
            <main class="gacha-result">
              <h2>抽取结果</h2>
              <article class="gacha-result-card gacha-result-ssr">
                <span class="gacha-result-icon">🍀</span>
                <strong class="gacha-result-name">欧皇</strong>
                <span class="gacha-result-rarity">SSR</span>
                <span class="gacha-result-new">NEW</span>
              </article>
            </main>
            """.trimIndent(),
        )

        assertEquals("抽取结果", result.title)
        assertEquals("欧皇", result.items.single().badge.name)
        assertTrue(result.items.single().isNew)
    }

    @Test
    fun parsesMultiPullItemsAndQuantities() {
        val result = GachaPullResultParser.parse(
            """
            <section class="gacha-result">
              <h2>十连抽结果</h2>
              <div class="gacha-pull-10-grid">
                <article class="gacha-pull-10-item">
                  <span class="gacha-pull-10-icon">🐣</span>
                  <strong class="gacha-pull-10-name">萌新</strong>
                  <span class="gacha-pull-10-rarity">N</span>
                  <span class="gacha-pull-10-quantity">× 8</span>
                </article>
                <article class="gacha-pull-10-item">
                  <span class="gacha-pull-10-icon">⭐</span>
                  <strong class="gacha-pull-10-name">论坛之星</strong>
                  <span class="gacha-pull-10-rarity">SR</span>
                  <span class="gacha-pull-10-quantity">× 2</span>
                </article>
              </div>
            </section>
            """.trimIndent(),
        )

        assertEquals(2, result.items.size)
        assertEquals(10, result.items.sumOf { it.quantity })
    }
}
