package sb.linux.client.data

import sb.linux.client.domain.gacha.GachaAction
import sb.linux.client.domain.gacha.GachaProfile
import sb.linux.client.domain.gacha.GachaProfileRepository

internal class GachaProfileService(private val client: LsbClient) : GachaProfileRepository {
    override suspend fun load(): GachaProfile =
        GachaCatalogHtmlParser.parseGachaProfile(client.get("/gacha_profile").html)

    override suspend fun submit(action: GachaAction): String? {
        val response = client.postForm(
            action.action,
            buildMap {
                put("_csrf", client.csrf())
                putAll(action.fields)
            },
        )
        return if (response.url.contains("form_error")) {
            HtmlTextParser.extractError(response.html)
        } else {
            null
        }
    }
}
