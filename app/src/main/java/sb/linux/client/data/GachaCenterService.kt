package sb.linux.client.data

import sb.linux.client.domain.gacha.GachaAction
import sb.linux.client.domain.gacha.GachaCenter
import sb.linux.client.domain.gacha.GachaCenterRepository
import sb.linux.client.domain.gacha.GachaPullReceipt

internal class GachaCenterService(
    private val client: LsbClient,
    private val settings: AppSettings,
) : GachaCenterRepository {
    override fun confirmPullEnabled(): Boolean = settings.confirmGachaPull

    override fun setConfirmPullEnabled(enabled: Boolean) {
        settings.confirmGachaPull = enabled
    }

    override suspend fun load(): GachaCenter =
        GachaCatalogHtmlParser.parseGachaCenter(client.get("/gacha").html)

    override suspend fun pull(action: GachaAction): GachaPullReceipt {
        val response = client.postForm(
            action.action,
            buildMap {
                put("_csrf", client.csrf())
                putAll(action.fields)
            },
        )
        check(!response.url.contains("login")) { "请重新登录" }
        check(!response.url.contains("form_error")) { HtmlTextParser.extractError(response.html) }
        val result = GachaPullResultParser.parse(response.html)
        if (action.cost > 0) {
            settings.recordUsageEvent("gacha", title = action.label, value = action.cost)
        }
        return GachaPullReceipt(result, action.cost)
    }
}
