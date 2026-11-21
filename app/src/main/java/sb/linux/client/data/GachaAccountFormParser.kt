package sb.linux.client.data

import org.json.JSONObject
import org.jsoup.nodes.Element
import sb.linux.client.domain.account.AccountForm

/** 转译公开 plugins.js 的 gachaGiftModalOpen，不执行网页脚本。 */
internal object GachaAccountFormParser {
    fun gift(button: Element, base: String): AccountForm? {
        val call = button.attr("onclick")
        val prefix = "gachaGiftModalOpen("
        val start = call.indexOf(prefix)
        val end = call.lastIndexOf(')')
        if (start < 0 || end <= start) return null
        val data = runCatching { JSONObject(call.substring(start + prefix.length, end)) }.getOrNull() ?: return null
        val id = data.optLong("id")
        if (id <= 0) return null
        var root = button
        while (root.parent() != null) root = root.parent()!!
        val template = root.selectFirst("#gachaGiftModal form")?.clone() ?: return null
        template.selectFirst("[name=title_id]")?.attr("value", id.toString()) ?: return null
        val ur = data.optString("rarity") == "ur"
        val quantity = template.selectFirst("[name=quantity]")
        quantity?.attr("value", "1")?.attr("max", data.optInt("quantity", 1).coerceIn(1, 1000).toString())
        if (ur) quantity?.attr("disabled", "disabled") else quantity?.removeAttr("disabled")
        template.selectFirst(".gacha-gift-quantity")?.let { if (ur) it.attr("hidden", "hidden") else it.removeAttr("hidden") }
        val select = template.selectFirst(".gacha-gift-instance select")
        select?.empty()
        if (ur) {
            select?.removeAttr("disabled")
            select?.attr("required", "required")
            val instances = data.optJSONArray("instances")
            if (instances != null) for (i in 0 until instances.length()) {
                val item = instances.optJSONObject(i) ?: continue
                select?.appendElement("option")?.attr("value", item.optLong("id").toString())?.text("#${item.optString("serial")}")
            }
        } else select?.attr("disabled", "disabled")
        template.selectFirst(".gacha-gift-instance")?.let { if (ur) it.removeAttr("hidden") else it.attr("hidden", "hidden") }
        val form = AccountManagementParser.parseForm(template, base).copy(verification =
            sb.linux.client.data.parser.LoginVerificationParser.parse(template, base, root.ownerDocument() ?: root))
        return form.copy(id = "gift:$id", notes = listOf("赠送称号：${data.optString("name")}") + form.notes)
    }
}
