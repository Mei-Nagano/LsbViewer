package sb.linux.client.domain.gacha

interface GachaMarketRepository {
    suspend fun load(query: String, rarity: String, sort: String, page: Int): GachaMarketPage
    suspend fun purchase(listing: GachaMarketListing, quantity: Int): String?
}
