package sb.linux.client.data

import sb.linux.client.domain.gacha.SourceFormProtocol
import sb.linux.client.domain.gacha.GachaAction
import sb.linux.client.domain.gacha.GachaCenter
import sb.linux.client.domain.gacha.GachaMarketListing
import sb.linux.client.domain.gacha.GachaMarketPage
import sb.linux.client.domain.gacha.GachaPoolRow
import sb.linux.client.domain.gacha.GachaProfile
import sb.linux.client.domain.gacha.GachaTitleItem
import sb.linux.client.domain.topic.TitleBadge
object GachaCatalogHtmlParser {
    fun parseGachaProfile(html: String): GachaProfile {
        val d = SourceHtmlSupport.sourceDocument(html)
        val candidates = d.select(".gacha-profile-titles .gacha-title-badge, .gacha-profile-titles .gacha-title-post-badge, .gacha-profile-grid .gacha-title-badge")
            .map { badge ->
                val collection = badge.parents().firstOrNull {
                    it.hasClass("gacha-profile-titles") || it.hasClass("gacha-profile-grid")
                }
                // 取集合以内完整的单称号条目，不能只取装备按钮附近的内层容器，
                // 否则放在同级的持有数量与说明会丢失。
                badge.parents().takeWhile { it !== collection }
                    .lastOrNull { it.select(".gacha-title-badge, .gacha-title-post-badge").size == 1 }
                    ?: badge
            }.distinct()
        val titles = candidates.mapNotNull { el ->
            if (el.hasClass("empty-state")) return@mapNotNull null
            val badge = SourceHtmlSupport.gachaTitleOf(el) ?: el.selectFirst(".gacha-title-badge")?.let { b ->
                TitleBadge(
                    icon = b.selectFirst(".gacha-title-icon")?.text()?.trim() ?: "",
                    name = b.selectFirst(".gacha-title-name")?.text()?.trim() ?: "",
                    rarity = b.selectFirst(".gacha-title-rarity")?.text()?.trim() ?: "",
                    serial = b.selectFirst(".gacha-title-serial")?.text()?.trim() ?: "",
                )
            }
            if (badge == null || badge.name.isBlank()) return@mapNotNull null
            val full = el.text().replace(Regex("""\s+"""), " ").trim()
            // 数量：条目里「×N」或「x N」形式的持有数（无则 1）
            val count = el.selectFirst("[data-count]")?.attr("data-count")?.toIntOrNull()
                ?: Regex("""(?:[×x]|数量\s*[:：]?|持有\s*[:：]?)\s*(\d+)""").find(full)?.groupValues?.get(1)?.toIntOrNull()
                ?: Regex("""(\d+)\s*[个枚份]""").find(full)?.groupValues?.get(1)?.toIntOrNull() ?: 1
            // 装备状态：类标记 is-equipped / 文本含「已装备」
            val equipped = el.hasClass("is-equipped") || el.selectFirst(".is-equipped, [class*=equipped]") != null || full.contains("已装备") || full.contains("已佩戴")
            val actions = el.select("form[action]").mapNotNull { form ->
                val btn = form.selectFirst("button") ?: return@mapNotNull null
                GachaAction(
                    label = btn.text().trim(),
                    action = form.attr("action"),
                    fields = SourceHtmlSupport.hiddenFields(form),
                    enabled = !btn.hasAttr("disabled"),
                )
            }.filter {
                it.label.isNotBlank() && it.action.isNotBlank() &&
                    !it.label.contains("赠送", ignoreCase = true) && !it.label.contains("gift", ignoreCase = true)
            }
            GachaTitleItem(badge = badge, equipped = equipped, count = count, actions = actions)
        }
        return GachaProfile(
            stat = d.selectFirst(".gacha-center-stat")?.text()?.trim() ?: "",
            titles = titles.distinctBy { Triple(it.badge.name, it.badge.rarity, it.badge.serial) },
        )
    }

    /** 称号抽取页：操作地址和池概率均以源站 DOM 为准。 */
    fun parseGachaCenter(html: String): GachaCenter {
        val d = SourceHtmlSupport.sourceDocument(html)
        val actions = d.select(".gacha-actions form[action]").mapNotNull { form ->
            val button = form.selectFirst("button[type=submit], button") ?: return@mapNotNull null
            GachaAction(
                label = button.text().trim(),
                action = form.attr("action"),
                fields = SourceHtmlSupport.hiddenFields(form),
                enabled = !button.hasAttr("disabled"),
                cost = button.attr("data-cost").toIntOrNull()
                    ?: Regex("""(\d[\d,，.]*)\s*积分""").find(button.text())
                        ?.groupValues?.get(1)?.let(SourceFormProtocol::parseInteger) ?: 0,
            )
        }
        val pool = d.select(".gacha-pool-rarity").mapNotNull { row ->
            val rarity = row.selectFirst(".gacha-pool-rarity-label")?.text()?.trim().orEmpty()
            if (rarity.isBlank() || rarity.uppercase().startsWith("UR")) return@mapNotNull null
            GachaPoolRow(
                rarity = rarity,
                countText = row.selectFirst(".gacha-pool-rarity-count")?.text()?.trim().orEmpty(),
                rateText = row.selectFirst(".gacha-pool-rarity-rate")?.text()?.trim().orEmpty(),
            )
        }
        return GachaCenter(
            pointsText = d.selectFirst(".gacha-center-header .gacha-center-stat")?.text()?.trim().orEmpty(),
            statsText = d.selectFirst(".gacha-sub-stats")?.text()?.trim().orEmpty(),
            pullActions = actions,
            pool = pool,
            allTitles = d.select(".gacha-all-item").mapNotNull(SourceHtmlSupport::gachaTitleOf),
            // 跑马灯会复制一组节点实现循环，按文本去重。
            news = d.select(".gacha-good-news-item").eachText().map { it.trim() }
                .filter { it.isNotBlank() }.distinct().take(10),
        )
    }

    /** 称号市场：保留每个购买表单的服务端 listing_id 及返回状态。 */
    fun parseGachaMarket(html: String): GachaMarketPage {
        val d = SourceHtmlSupport.sourceDocument(html)
        val listings = d.select("article.gacha-market-card").mapNotNull { card ->
            val badge = SourceHtmlSupport.gachaTitleOf(card) ?: return@mapNotNull null
            val form = card.selectFirst("form.gacha-market-buy[action]") ?: return@mapNotNull null
            val text = card.selectFirst(".gacha-market-meta")?.text()?.trim().orEmpty()
            val priceText = Regex("""单价\s*([\d,，.\s]+)\s*积分""")
                .find(text)?.groupValues?.get(1).orEmpty()
            val price = SourceFormProtocol.parseInteger(priceText) ?: 0
            val available = form.selectFirst("input[name=quantity]")?.attr("max")?.toIntOrNull()
                ?: Regex("""剩余\s*(\d+)\s*个""").find(text)?.groupValues?.get(1)?.toIntOrNull() ?: 1
            val timeLeft = Regex("""剩余时间\s*(.+)$""").find(text)?.groupValues?.get(1)?.trim().orEmpty()
            GachaMarketListing(
                badge = badge,
                price = price,
                available = available,
                timeLeft = timeLeft,
                action = GachaAction("购买", form.attr("action"), SourceHtmlSupport.hiddenFields(form)),
            )
        }
        val pageLinks = d.select(".pagination a[href*=p]").mapNotNull { a ->
            Regex("""[?&]p=(\d+)""").find(a.attr("href"))?.groupValues?.get(1)?.toIntOrNull()
        }
        val summary = d.selectFirst(".gacha-market-head")?.text()?.trim().orEmpty()
        val current = Regex("""当前\s*(\d+)-""").find(summary)?.groupValues?.get(1)?.toIntOrNull()
            ?.let { start -> ((start - 1) / 24) + 1 } ?: 1
        return GachaMarketPage(
            summary = summary,
            note = d.selectFirst(".gacha-market-note")?.text()?.trim().orEmpty(),
            listings = listings,
            currentPage = current,
            lastPage = pageLinks.maxOrNull() ?: current,
        )
    }

}
