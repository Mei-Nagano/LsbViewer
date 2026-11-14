package sb.linux.client.data
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import sb.linux.client.domain.topic.EssenceApplication
import sb.linux.client.domain.topic.TopicPoll
import sb.linux.client.domain.topic.TopicPollOption

internal object TopicPollParser {
    fun parse(document: org.jsoup.nodes.Document, essencePanel: org.jsoup.nodes.Element?, essenceApplication: EssenceApplication?): TopicPoll? {
        val ordinary = document.selectFirst(".poll-card")
        if (ordinary != null) return parseOrdinary(ordinary)
        val pollForm = document.selectFirst(
            ".topic-essence-review-vote-form[action], .topic-poll form[action], .poll-card form[action], form[class*=poll][action], " +
                "form[action]:has(input[type=radio][name*=poll]), form[action]:has(input[type=checkbox][name*=poll])"
        )
        return pollForm?.let { form ->
            val root = form.parents().firstOrNull { el ->
                el.classNames().any { it.contains("poll", ignoreCase = true) || it == "topic-essence-review-panel" }
            } ?: form
            val inputs = form.select("input[type=radio][name], input[type=checkbox][name]")
            val panelText = root.text().replace(Regex("""\s+"""), " ").trim()
            val unavailableReason = when {
                panelText.contains("不能给自己", true) || panelText.contains("不能为自己", true) ||
                    panelText.contains("作者不能", true) -> "作者不能参与自己的申精投票"
                else -> ""
            }
            val options = inputs.mapNotNull { input ->
                val name = input.attr("name"); val value = input.attr("value")
                if (name.isBlank() || value.isBlank()) return@mapNotNull null
                val id = input.id()
                val label = (if (id.isNotBlank()) form.select("label[for]").firstOrNull { it.attr("for") == id } else null)
                    ?.text()?.trim()
                    ?: input.closest("label")?.text()?.trim()
                    ?: input.parent()?.text()?.trim()
                    ?: value
                TopicPollOption(name, value, label.ifBlank { value }, input.hasAttr("checked"), input.hasAttr("disabled"))
            }
            TopicPoll(
                title = if (form.hasClass("topic-essence-review-vote-form")) "精华申请 · 社区投票" else root.selectFirst("h2, h3, h4, .poll-title")?.text()?.trim().orEmpty().ifBlank { "投票" },
                note = if (form.hasClass("topic-essence-review-vote-form")) root.clone().apply { select("form").remove() }.text()
                    else root.selectFirst(".poll-note, .poll-description, small, p")?.text()?.trim().orEmpty(),
                action = form.attr("action"),
                fields = SourceHtmlSupport.hiddenFields(form),
                options = options,
                multiple = inputs.any { it.attr("type").equals("checkbox", true) },
                submitLabel = form.selectFirst("button[type=submit], input[type=submit]")?.let { it.text().ifBlank { it.attr("value") } }?.trim().orEmpty().ifBlank { "提交投票" },
                closed = unavailableReason.isBlank() && (options.isEmpty() || inputs.all { it.hasAttr("disabled") }),
                unavailableReason = unavailableReason,
                reasonName = form.selectFirst("textarea[name]")?.attr("name").orEmpty(),
                reasonRequired = form.selectFirst("textarea[name]")?.hasAttr("required") == true,
                reasonHint = form.selectFirst("textarea[name]")?.attr("placeholder").orEmpty(),
                reasonMaxLength = form.selectFirst("textarea[name]")?.attr("maxlength")?.toIntOrNull()?.coerceAtLeast(1) ?: 300,
            )
        } ?: essencePanel?.takeIf { essenceApplication == null }?.let { panel ->
            val text = panel.text().replace(Regex("""\s+"""), " ").trim()
            val status = panel.attr("data-status").lowercase()
            val authorBlocked = text.contains("不能给自己", true) || text.contains("不能为自己", true) ||
                text.contains("作者不能", true) || status in setOf("author", "self", "owner")
            val ended = status in setOf("closed", "expired", "approved", "rejected", "finished") ||
                listOf("投票已结束", "评议已结束", "已过期").any { text.contains(it) }
            TopicPoll(
                title = "精华申请 · 社区投票",
                note = text,
                closed = ended,
                unavailableReason = if (authorBlocked) "作者不能参与自己的申精投票"
                    else if (!ended) "当前账号不能参与此投票" else "",
            )
        }
    }
    private fun parseOrdinary(root: Element): TopicPoll {
        val form = root.selectFirst("form.poll-form")
        val inputs = form?.select("input[type=radio][name], input[type=checkbox][name]").orEmpty()
        val status = root.selectFirst(".poll-status")?.text().orEmpty()
        val submit = form?.selectFirst("button[type=submit]")
        val view = root.selectFirst("a[href*=poll_view]")
        val badge = root.selectFirst(".poll-status-badge")?.text().orEmpty()
        val footer = root.select(".poll-foot-main > span").map { it.text() }
        val initial = form?.selectFirst("[name=poll_initial]")?.attr("value").orEmpty()
        val voted = inputs.any { it.hasAttr("checked") } ||
            initial.trim() !in setOf("", "0", "[]", "null") ||
            listOf("已投", "已参与", "已选择", "修改投票").any { status.contains(it) || badge.contains(it) || submit?.text()?.contains(it) == true }
        val multiple = inputs.any { it.attr("type") == "checkbox" } || status.contains("多选")
        val maximum = Regex("(?:最多|限选|最多可选|最多选择)\\s*(\\d+)").find(status)?.groupValues?.get(1)?.toIntOrNull()
            ?: if (multiple) Int.MAX_VALUE else 1
        val results = root.select(".poll-option-row:has(.poll-result-count)").map { row ->
            val bar = row.selectFirst("[role=progressbar]")
            val value = bar?.attr("aria-valuenow")?.toFloatOrNull() ?: 0f
            val max = bar?.attr("aria-valuemax")?.toFloatOrNull() ?: 100f
            sb.linux.client.domain.topic.TopicPollResult(row.selectFirst(".poll-option-text")?.text().orEmpty(),
                row.selectFirst(".poll-result-count")?.text().orEmpty(), (value / max.coerceAtLeast(1f)).coerceIn(0f, 1f))
        }
        return TopicPoll(
            title = root.selectFirst(".poll-head-title strong")?.text().orEmpty().ifBlank { "帖子投票" },
            note = listOf(status, root.selectFirst(".poll-foot-main")?.text().orEmpty()).filter(String::isNotBlank).joinToString("\n"),
            action = form?.attr("action").orEmpty(), fields = form?.let(SourceHtmlSupport::hiddenFields).orEmpty(),
            options = inputs.map { input -> TopicPollOption(input.attr("name"), input.attr("value"),
                input.closest("label")?.selectFirst(".poll-option-text")?.text().orEmpty(), input.hasAttr("checked"), input.hasAttr("disabled")) },
            multiple = multiple, maxChoices = maximum, submitLabel = submit?.text().orEmpty().ifBlank { "投票" },
            submitName = submit?.attr("name").orEmpty(), submitValue = submit?.attr("value").orEmpty(),
            closed = listOf("结束", "截止", "过期", "关闭").any { badge.contains(it) } || status.contains("投票已结束"),
            results = results, viewUrl = view?.attr("href").orEmpty(), viewLabel = view?.text().orEmpty(),
            resultView = root.hasClass("poll-results-view"), selectionMustChange = form?.selectFirst("[name=poll_initial]") != null,
            voted = voted, statusLabel = badge,
            deadline = footer.firstOrNull { it.contains("截止时间") }.orEmpty(),
            participants = footer.firstOrNull { it.contains("人参与") }.orEmpty(),
            remainingActions = Regex("剩余\\s*\\d+\\s*次操作").find(status)?.value.orEmpty(),
            choiceLabel = status.substringBefore(" · ").ifBlank { if (multiple) "多选投票" else "单选投票" } +
                if (multiple && maximum != Int.MAX_VALUE) " · 最多 $maximum 项" else "",
        )
    }

}
