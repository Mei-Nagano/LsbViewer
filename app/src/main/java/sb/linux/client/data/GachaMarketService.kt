package sb.linux.client.data

import android.net.Uri
import sb.linux.client.domain.gacha.GachaMarketListing
import sb.linux.client.domain.gacha.GachaMarketPage
import sb.linux.client.domain.gacha.GachaMarketRepository

internal class GachaMarketService(private val client: LsbClient) : GachaMarketRepository {
    override suspend fun load(query: String, rarity: String, sort: String, page: Int): GachaMarketPage {
        val url = "/gacha_market?q=${Uri.encode(query)}&rarity=${Uri.encode(rarity)}" +
            "&sort=${Uri.encode(sort)}&p=$page"
        val parsed = GachaCatalogHtmlParser.parseGachaMarket(client.get(url).html)
        return parsed.copy(listings = when (sort) {
            "price_asc" -> parsed.listings.sortedBy { it.price }
            "price_desc" -> parsed.listings.sortedByDescending { it.price }
            else -> parsed.listings
        })
    }

    override suspend fun purchase(listing: GachaMarketListing, quantity: Int): String? {
        val fields = buildMap {
            put("_csrf", client.csrf())
            putAll(listing.action.fields)
            put("quantity", quantity.toString())
        }
        val response = client.postForm(listing.action.action, fields)
        return if (response.url.contains("form_error")) {
            HtmlTextParser.extractError(response.html)
        } else {
            null
        }
    }
}
