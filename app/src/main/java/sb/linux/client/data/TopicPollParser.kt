package sb.linux.client.data
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import sb.linux.client.domain.topic.EssenceApplication
import sb.linux.client.domain.topic.TopicPoll
import sb.linux.client.domain.topic.TopicPollOption

internal object TopicPollParser {
    fun parse(document: org.jsoup.nodes.Document, essencePanel: org.jsoup.nodes.Element?, essenceApplication: EssenceApplication?): TopicPoll? {
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
}
