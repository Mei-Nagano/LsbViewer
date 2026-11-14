package sb.linux.client.data

import org.jsoup.nodes.Element
import sb.linux.client.domain.account.AccountActivityMetric
import sb.linux.client.domain.account.AccountNode

/** 数量、得分与规则直接读取源站；指南与指标的顺序不一致，必须按名称匹配。 */
internal object CommunityActivityParser {
    fun parseCard(card: Element): AccountNode.Activity {
        val guide = card.closest(".community-activity-page")?.selectFirst(".community-activity-guide")
        val definitions = guide?.selectFirst(".community-activity-guide-rules")
        val rules = definitions?.children().orEmpty().mapNotNull { entry ->
            val label = entry.selectFirst("dt")?.text().orEmpty()
            val rule = entry.selectFirst("dd")?.text().orEmpty()
            if (label.isBlank() || rule.isBlank()) null else label to rule
        }.toMap()
        val notes = listOfNotNull(
            guide?.selectFirst("p")?.text(),
            definitions?.nextElementSibling()?.takeIf { it.normalName() == "p" }?.text(),
        ).filter(String::isNotBlank).distinct()
        val metrics = card.select(".community-activity-signal-card").map { signal ->
            val label = signal.selectFirst(".community-activity-signal-head > div > span")?.text().orEmpty()
            check(label.isNotBlank()) { "未解析到活跃度指标名称，源站结构可能已变化" }
            val progress = signal.selectFirst("[role=progressbar]")
            val current = progress?.attr("aria-valuenow")?.toFloatOrNull()
            val maximum = progress?.attr("aria-valuemax")?.toFloatOrNull()
            AccountActivityMetric(
                label = label,
                value = signal.selectFirst(".community-activity-signal-head > div > strong")?.text().orEmpty().ifBlank { "未提供" },
                score = signal.selectFirst(".community-activity-signal-score")?.text().orEmpty().ifBlank { "得分未提供" },
                fraction = if (current != null && maximum != null && maximum > 0f && current.isFinite() && maximum.isFinite())
                    (current / maximum).coerceIn(0f, 1f) else null,
                rule = rules[label].orEmpty().ifBlank { "源站暂未提供这项指标的具体规则。" },
                notes = notes,
            )
        }
        check(metrics.isNotEmpty()) { "源站未提供活跃度指标，请刷新后重试" }
        val score = card.selectFirst(".community-activity-score-head strong")
        return AccountNode.Activity(
            label = card.selectFirst(".community-activity-score-head > div > span")?.text().orEmpty().ifBlank { "社区活跃度" },
            score = listOfNotNull(score?.ownText(), score?.selectFirst("small")?.text())
                .filter(String::isNotBlank).joinToString(" ").ifBlank { "未提供" },
            tier = card.selectFirst(".community-activity-tier")?.text().orEmpty(),
            status = card.selectFirst(".community-activity-status")?.text().orEmpty(),
            metrics = metrics,
            reasons = card.select(".community-activity-reasons li").map(Element::text).filter(String::isNotBlank),
        )
    }
}
