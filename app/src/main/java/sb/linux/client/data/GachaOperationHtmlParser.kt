package sb.linux.client.data

import org.jsoup.nodes.Element
import sb.linux.client.domain.gacha.GachaFormField
import sb.linux.client.domain.gacha.GachaFormOption
import sb.linux.client.domain.gacha.GachaOperationForm
import sb.linux.client.domain.gacha.GachaOperationPage

object GachaOperationHtmlParser {
    /**
     * 选项控件所属的选项容器，由内向外最多四层。容器里一旦出现第二个 checkbox/radio，
     * 说明已经越过本行，再往上取就会串到别人的称号，到此为止。
     */
    private fun optionHolders(control: Element): List<Element> {
        val holders = mutableListOf<Element>()
        var node = control.parent()
        while (node != null && holders.size < 4) {
            if (node.select("input[type=checkbox], input[type=radio]").size > 1) break
            holders += node
            node = node.parent()
        }
        return holders
    }

    /**
     * 熔炼/回收页里 checkbox、radio 的称号名。源站把称号渲染成 .gacha-title-badge 卡片，
     * 控件本身既没有 label 也没有文字，直接取字段名的话界面上只会看到 title_ids，
     * 分不清勾的是哪个称号。没有称号徽章时返回空，交给后面的 label 兜底。
     */
    private fun gachaOptionLabel(control: Element): String {
        for (holder in optionHolders(control)) {
            val badge = SourceHtmlSupport.gachaTitleOf(holder) ?: continue
            val head = listOf(badge.icon, badge.name).filter { it.isNotBlank() }.joinToString(" ")
            return listOf(head, badge.rarity, badge.serial, optionSideText(holder))
                .filter { it.isNotBlank() }.joinToString(" · ")
        }
        return ""
    }

    /** 同一种称号的多枚实例共用聚合标题；序号和持有数量留在实例标签中。 */
    private fun gachaOptionGroupLabel(control: Element): String {
        for (holder in optionHolders(control)) {
            val badge = SourceHtmlSupport.gachaTitleOf(holder) ?: continue
            return listOf(badge.icon, badge.name, badge.rarity)
                .filter { it.isNotBlank() }.joinToString(" · ")
        }
        return ""
    }

    /** 既没有称号徽章又没有 label 的选项：退回选项行的可见文字，至少比字段名可读。 */
    private fun gachaOptionText(control: Element): String =
        optionHolders(control).firstNotNullOfOrNull { optionSideText(it).takeIf(String::isNotBlank) }.orEmpty()

    /** 选项容器里除徽章和控件外的可见文字，例如持有数量「×2」；成段的说明文字不取。 */
    private fun optionSideText(el: Element): String = el.clone()
        .also { it.select(".gacha-title-badge, .gacha-title-post-badge, input, select, textarea, button").remove() }
        .text().replace(Regex("""\s+"""), " ").trim()
        .takeIf { it.length in 1..40 }.orEmpty()

    /**
     * 解析称号系统的动态表单页。这里只限定 /gacha* 的提交表单，避免把页头搜索、登录等
     * 无关表单带进客户端；字段名称和值保持原样，提交时可完整复现重复 checkbox 参数。
     */
    fun parseGachaOperationPage(html: String, collectionsOnly: Boolean = false): GachaOperationPage {
        val d = SourceHtmlSupport.sourceDocument(html)
        val main = d.selectFirst("main") ?: d.body()
        val forms = main.select("form[action]").flatMap { form ->
            val action = form.attr("action").trim()
            val isSupportedOperation = if (collectionsOnly) action == "/topic_collections_action"
                else Regex("^/(gacha|identity|verify|creator)[a-zA-Z0-9_/?=&%-]*$").matches(action)
            // 只在源站显式写了 method=get 时跳过。缺省 method 的表单（源站熔炼/回收页就是这样）
            // 一律按可提交处理：之前连同缺省一起判成 GET 会让整页解析不出任何表单，
            // 界面上就是「按钮在但点不动」或压根没有按钮。
            if (!isSupportedOperation || form.attr("method").trim().equals("get", true)) return@flatMap emptyList()
            val hidden = form.select("input[type=hidden][name]:not([disabled])").map { it.attr("name") to it.attr("value") }
            val externalControls = main.select("input[form], select[form], textarea[form]").filter { it.attr("form") == form.id() && form.id().isNotBlank() }
            val controls = (form.select("input[name]:not([type=hidden]):not([type=submit]):not([type=button]), select[name], textarea[name]") + externalControls).distinct()
            val fields = controls
                .mapNotNull { control ->
                    val name = control.attr("name").trim()
                    if (name.isBlank() || control.hasAttr("disabled") || control.closest("fieldset[disabled]") != null) return@mapNotNull null
                    val tag = control.tagName()
                    val type = when (tag) {
                        "select" -> "select"
                        "textarea" -> "textarea"
                        else -> control.attr("type").lowercase().ifBlank { "text" }
                    }
                    val id = control.id()
                    // label 可能写在 form 外面（源站把选项卡片和表单拆成两块），所以整页找
                    val explicitLabel = id.takeIf { it.isNotBlank() }
                        ?.let { target -> main.select("label[for]").firstOrNull { it.attr("for") == target }?.text()?.trim() }
                    val wrappingLabel = control.closest("label")?.text()?.trim()
                    val fallbackLabel = control.attr("aria-label").trim()
                        .ifBlank { control.attr("placeholder").trim() }
                        .ifBlank { name }
                    // 选项控件优先取称号名：熔炼/回收的 checkbox 只带 id，文字在旁边的徽章里
                    val isOption = type == "checkbox" || type == "radio"
                    val label = (if (isOption) gachaOptionLabel(control) else "")
                        .ifBlank { explicitLabel.orEmpty() }
                        .ifBlank { wrappingLabel.orEmpty() }
                        .ifBlank { if (isOption) gachaOptionText(control) else "" }
                        .ifBlank { fallbackLabel }
                        .replace(Regex("""\s+"""), " ").trim()
                    GachaFormField(
                        name = name,
                        label = label,
                        groupLabel = if (isOption) gachaOptionGroupLabel(control) else "",
                        type = type,
                        value = if (tag == "textarea") control.text() else control.attr("value"),
                        placeholder = control.attr("placeholder"),
                        min = control.attr("min"),
                        max = control.attr("max"),
                        required = control.hasAttr("required"),
                        checked = control.hasAttr("checked"),
                        options = if (tag == "select") control.select("option").map { option ->
                            // optgroup 的分组名（源站按稀有度分组）拼在前面，空文字的选项退回 value
                            val group = option.closest("optgroup")?.attr("label")?.trim().orEmpty()
                            val text = option.text().trim().ifBlank { option.attr("value").trim() }
                            GachaFormOption(
                                if (option.hasAttr("value")) option.attr("value") else option.text(),
                                listOf(group, text).filter { it.isNotBlank() }.joinToString(" · "),
                                option.hasAttr("selected"),
                                option.hasAttr("disabled") || option.closest("optgroup[disabled]") != null,
                            )
                        } else emptyList(),
                        maxLength = control.attr("maxlength").toIntOrNull()?.coerceAtLeast(0) ?: Int.MAX_VALUE,
                        multiple = control.hasAttr("multiple"),
                    )
                }
            val externalButtons = main.select("button[form], input[type=submit][form]").filter {
                form.id().isNotBlank() && it.attr("form") == form.id()
            }
            val buttons = (form.select("button[type=submit], input[type=submit], button:not([type])") + externalButtons).distinct()
            buttons.map { button ->
            val submitFields = hidden.toMutableList()
            button.attr("name").takeIf { it.isNotBlank() }?.let { submitName ->
                submitFields += submitName to button.attr("value")
            }
            button.attr("data-gacha-forge-source").takeIf { it.isNotBlank() }?.let { sourceRarity ->
                submitFields.removeAll { it.first == "source_rarity" }
                submitFields += "source_rarity" to sourceRarity
            }
            val label = button.clone().also { it.select("small").remove() }.text().trim()
                .ifBlank { button?.attr("value")?.trim().orEmpty() }
                .ifBlank { form.selectFirst("h2, h3, legend")?.text()?.trim().orEmpty() }
                .ifBlank { "提交" }
            val submitAction = button.attr("formaction").ifBlank { action }
            // A submit override must stay within the same supported operation family.
            val choiceDrivenForge = (action.contains("forge", ignoreCase = true) ||
                action.contains("recycle", ignoreCase = true)) &&
                fields.any { it.type == "checkbox" || it.type == "radio" }
            val minSelections = if (choiceDrivenForge) {
                listOf(button, form).firstNotNullOfOrNull { node ->
                    listOf(
                        "data-min-selected", "data-min-selections", "data-required-count",
                        "data-min-count", "data-gacha-forge-cost",
                    )
                        .firstNotNullOfOrNull { attr -> node.attr(attr).toIntOrNull() }
                } ?: Regex("(?:至少选择|选择至少|需要选择|请选择)\\s*(\\d+)\\s*(?:个|枚|项)")
                    .find(form.text())?.groupValues?.get(1)?.toIntOrNull() ?: 1
            } else 0
            GachaOperationForm(label, if (submitAction == action) action else submitAction.takeIf {
                if (collectionsOnly) it == "/topic_collections_action" else Regex("^/(gacha|identity|verify|creator)[a-zA-Z0-9_/?=&%-]*$").matches(it)
            } ?: action, submitFields, fields, choiceDrivenForge ||
                (!button.hasAttr("disabled") && button.closest("fieldset[disabled]") == null), minSelections)
            }
        }
        val notes = main.select(".gacha-market-note, .gacha-note, .notice, .alert, .tips, .help-text")
            .eachText().map { it.replace(Regex("""\s+"""), " ").trim() }
            .filter { it.isNotBlank() }.distinct().take(8)
        val records = main.select("table tbody tr, .empty-state, .gacha-record, .order-item")
            .eachText().map { it.replace(Regex("""\s+"""), " ").trim() }
            .filter { it.isNotBlank() && it.length <= 500 }.distinct().take(100)
        return GachaOperationPage(
            title = main.selectFirst("h1")?.text()?.trim().orEmpty()
                .ifBlank { d.title().substringBefore("-").trim() },
            notes = notes,
            forms = forms,
            records = records,
            links = main.select(".pagination a[href]").map { it.text().trim() to it.attr("href") }
                .filter { it.second.startsWith("/") && !it.second.startsWith("//") }.distinct(),
        )
    }

    /** 新版私信联系人列表（/direct_messages）。 */
}
