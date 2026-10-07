package sb.linux.client.data
import org.jsoup.nodes.Element
import sb.linux.client.domain.topic.EssenceApplication
import sb.linux.client.domain.topic.EssencePointsTopUp
import sb.linux.client.domain.topic.EssenceReview

internal object TopicEssenceReviewParser {
    fun parseApplication(essencePanel: org.jsoup.nodes.Element?): EssenceApplication? {
        return essencePanel?.takeIf {
            it.attr("data-status") == "not_applied" || it.selectFirst("form.topic-essence-review-apply-form") != null
        }?.let { panel ->
            val form = panel.selectFirst("form.topic-essence-review-apply-form[action]")
            val button = form?.selectFirst("button[type=submit], button")
            EssenceApplication(
                title = panel.selectFirst(".topic-essence-review-head strong")?.text().orEmpty().ifBlank { "申请帖子加精" },
                note = panel.clone().apply { select("form, .topic-essence-review-head").remove() }.text(),
                action = form?.attr("action").orEmpty(),
                fields = buildMap {
                    putAll(SourceHtmlSupport.hiddenFields(form))
                    button?.attr("name")?.takeIf { it.isNotBlank() }?.let { put(it, button.attr("value")) }
                },
                label = button?.text().orEmpty().ifBlank { "申请加精" },
                enabled = form != null && button != null && !button.hasAttr("disabled"),
            )
        }
    }

    fun parseReview(essencePanel: org.jsoup.nodes.Element?): EssenceReview? {
        return essencePanel?.let { panel ->
            val head = panel.selectFirst(".topic-essence-review-head")
            val progressEl = panel.selectFirst(".topic-essence-review-progress-row progress")
            val progressText = panel.selectFirst(".topic-essence-review-progress-row strong")?.text()?.trim().orEmpty()
            val progressMatch = Regex("""(-?\d+)\s*/\s*(\d+)""").find(progressText)
            val topUpRoot = panel.selectFirst("form.topic-essence-review-pool-topup[action]")
                ?: panel.selectFirst(".topic-essence-review-pool-topup form[action]")
            val amountInput = topUpRoot?.selectFirst("input[type=number][name], input[name*=point], input[name*=amount]")
                ?: topUpRoot?.selectFirst("input:not([type=hidden])[name]")
            val topUpButton = topUpRoot?.selectFirst("button[type=submit], input[type=submit]")
            val topUp = if (topUpRoot != null && amountInput != null) EssencePointsTopUp(
                action = topUpRoot.attr("action"),
                fields = buildMap {
                    putAll(SourceHtmlSupport.hiddenFields(topUpRoot))
                    topUpButton?.attr("name")?.takeIf { it.isNotBlank() }?.let { put(it, topUpButton.attr("value")) }
                },
                amountName = amountInput.attr("name"),
                defaultAmount = amountInput.attr("value"),
                min = amountInput.attr("min").toIntOrNull()?.coerceAtLeast(1) ?: 1,
                max = amountInput.attr("max").toIntOrNull()?.coerceAtLeast(1) ?: Int.MAX_VALUE,
                label = topUpRoot.selectFirst("label")?.ownText()?.trim().orEmpty().ifBlank { "追加积分" },
                hint = topUpRoot.selectFirst("small, .form-help")?.text()?.trim().orEmpty(),
                submitLabel = topUpButton?.let { it.text().ifBlank { it.attr("value") } }?.trim().orEmpty().ifBlank { "追加" },
                enabled = topUpRoot.attr("action").isNotBlank() && topUpButton?.hasAttr("disabled") != true && !amountInput.hasAttr("disabled"),
            ) else null
            val pool = panel.selectFirst(".topic-essence-review-pool-summary")
            EssenceReview(
                title = head?.selectFirst("strong")?.text()?.trim().orEmpty().ifBlank { "精华申请" },
                subtitle = head?.selectFirst("div > span")?.text()?.trim().orEmpty(),
                status = panel.attr("data-status").trim(),
                statusLabel = head?.selectFirst(".topic-essence-review-status")?.text()?.trim().orEmpty(),
                progress = progressMatch?.groupValues?.get(1)?.toIntOrNull()
                    ?: progressEl?.attr("value")?.toIntOrNull() ?: 0,
                target = progressMatch?.groupValues?.get(2)?.toIntOrNull()
                    ?: progressEl?.attr("max")?.toIntOrNull() ?: 0,
                progressText = progressText,
                progressNote = panel.selectFirst(".topic-essence-review-progress-note")?.text()?.trim().orEmpty(),
                meta = panel.select(".topic-essence-review-meta > *").eachText().map { it.trim() }.filter { it.isNotBlank() },
                poolTitle = pool?.children()?.firstOrNull { it.tagName() == "span" }?.text()?.trim().orEmpty(),
                poolItems = pool?.children()?.firstOrNull { it.tagName() == "div" }?.children()
                    ?.map { it.text().trim() }?.filter { it.isNotBlank() }.orEmpty(),
                actionNote = panel.select(".topic-essence-review-actions > .topic-essence-review-note, .topic-essence-review-actions > p")
                    .eachText().joinToString(" ").trim(),
                topUp = topUp,
            )
        }
    }
}
