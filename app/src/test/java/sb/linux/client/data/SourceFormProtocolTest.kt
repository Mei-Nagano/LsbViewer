package sb.linux.client.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SourceFormProtocolTest {
    @Test
    fun marketPriceAcceptsThousandsSeparator() {
        val page = HtmlParser.parseGachaMarket(
            """
            <main>
              <article class="gacha-market-card">
                <span class="gacha-title-badge gacha-title-ssr">
                  <span class="gacha-title-icon">💎</span>
                  <span class="gacha-title-name">氪金大佬</span>
                  <span class="gacha-title-rarity">SSR</span>
                </span>
                <p class="gacha-market-meta">单价 1,000 积分 剩余 1 个 剩余时间 24 小时</p>
                <form class="gacha-market-buy" action="/gacha_market_buy">
                  <input name="quantity" max="1">
                </form>
              </article>
            </main>
            """.trimIndent(),
        )

        assertEquals(1_000, page.listings.single().price)
    }

    @Test
    fun forgeUsesLinkedQuantitiesAndServerCost() {
        val page = HtmlParser.parseGachaOperationPage(
            """
            <main>
              <form id="gachaForgen" method="post" action="/gacha_forge">
                <input type="hidden" name="forge_count" value="0">
                <input type="hidden" name="source_rarity">
                <button type="submit" disabled data-gacha-forge-source="N" data-gacha-forge-cost="3">N × 3 → R</button>
              </form>
              <article class="gacha-operation-card">
                <span class="gacha-title-badge gacha-title-n">
                  <span class="gacha-title-name">新手</span><span class="gacha-title-rarity">N</span>
                </span>
                <label><input type="checkbox" form="gachaForgen" name="material_title_ids[]" value="1"></label>
                <input type="number" form="gachaForgen" name="material_quantities[1]" value="4" min="1" max="4">
              </article>
              <article class="gacha-operation-card">
                <span class="gacha-title-badge gacha-title-n">
                  <span class="gacha-title-name">活跃</span><span class="gacha-title-rarity">N</span>
                </span>
                <label><input type="checkbox" form="gachaForgen" name="material_title_ids[]" value="2"></label>
                <input type="number" form="gachaForgen" name="material_quantities[2]" value="2" min="1" max="2">
              </article>
            </main>
            """.trimIndent(),
        )
        val form = page.forms.single()
        val firstChoice = form.fields.indexOfFirst { it.type == "checkbox" && it.value == "1" }
        val secondChoice = form.fields.indexOfFirst { it.type == "checkbox" && it.value == "2" }
        val firstQuantity = SourceFormProtocol.quantityFieldIndex(form, firstChoice)!!
        val secondQuantity = SourceFormProtocol.quantityFieldIndex(form, secondChoice)!!
        val values = form.fields.indices.associateWith { form.fields[it].value }.toMutableMap().apply {
            this[firstQuantity] = "2"
            this[secondQuantity] = "1"
        }
        val checked = mapOf(firstChoice to true, secondChoice to true)

        assertEquals(3, form.minSelections)
        assertEquals(3, SourceFormProtocol.selectedChoiceCount(form, values, checked, emptyMap(), emptyMap()))
        val pairs = SourceFormProtocol.buildSubmissionPairs(form, values, checked, emptyMap(), emptyMap())
        assertEquals("1", pairs.single { it.first == "forge_count" }.second)
        assertEquals("N", pairs.single { it.first == "source_rarity" }.second)
        assertTrue(pairs.contains("material_quantities[1]" to "2"))
        assertTrue(pairs.contains("material_quantities[2]" to "1"))
        assertEquals(2, pairs.count { it.first == "material_title_ids[]" })

        val oneTitleValues = values.toMutableMap().apply { this[firstQuantity] = "3" }
        val oneTitlePairs = SourceFormProtocol.buildSubmissionPairs(
            form, oneTitleValues, mapOf(firstChoice to true, secondChoice to false), emptyMap(), emptyMap(),
        )
        assertNull(validateSourceForm(form, oneTitlePairs))

        oneTitleValues[firstQuantity] = "2"
        val insufficientPairs = SourceFormProtocol.buildSubmissionPairs(
            form, oneTitleValues, mapOf(firstChoice to true, secondChoice to false), emptyMap(), emptyMap(),
        )
        assertEquals("请至少选择 3 个", validateSourceForm(form, insufficientPairs))
    }

    @Test
    fun commentRewardTiersFollowSourceAttribute() {
        assertEquals(listOf(1, 5, 10, 50), SourceFormProtocol.reactionTiers("1,5,10,50"))
    }
}
