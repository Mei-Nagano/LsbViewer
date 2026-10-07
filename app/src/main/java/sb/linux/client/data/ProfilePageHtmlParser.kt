package sb.linux.client.data

import org.jsoup.nodes.Element
import sb.linux.client.domain.profile.AvatarPickerData
import sb.linux.client.domain.profile.ProfileSettingsField
import sb.linux.client.domain.profile.ProfileSettingsForm
import sb.linux.client.domain.profile.ProfileSettingsPage

object ProfilePageHtmlParser {
    /** label.grid > span / aria-label / placeholder / 字段名映射 */
    private fun labelOf(el: Element): String {
        el.parent()?.takeIf { it.tagName() == "label" }?.selectFirst("span")?.text()?.trim()
            ?.takeIf { it.isNotBlank() }?.let { return it }
        el.attr("aria-label").trim().takeIf { it.isNotBlank() }?.let { return it }
        el.attr("placeholder").trim().takeIf { it.isNotBlank() }?.let { return it }
        return FIELD_LABELS[el.attr("name")] ?: el.attr("name")
    }

    private val FIELD_LABELS = mapOf(
        "username" to "用户名", "user_name" to "用户名",
        "email" to "邮箱", "new_email" to "新邮箱",
        "password" to "新密码", "password2" to "确认新密码",
        "old_password" to "当前密码", "current_password" to "当前密码",
        "bio" to "个人简介", "signature" to "个人简介",
        "avatar" to "头像", "avatar_file" to "头像",
    )

    /**
     * 解析 /profile 个人设置页（通用表单驱动）：
     * 提取展示信息（用户名/UID/邮箱/注册时间/积分）与全部可提交表单，
     * 表单字段动态渲染，自动适配源站实际功能。
     */
    internal fun parseProfilePage(html: String): ProfilePageParseResult {
        val d = SourceHtmlSupport.sourceDocument(html)
        val main = d.selectFirst("main") ?: d

        // 头像
        val avatar = main.selectFirst(".user-avatar-big img, .profile-avatar img")?.attr("src")
            ?: main.selectFirst("img.avatar-img, img[src*='avatar']")?.attr("src") ?: ""

        // 预置头像选择器：风格下拉 + 编号输入 + 头像基址（与源站 index.js 的 .avatar-picker 结构对应）
        val pickerEl = main.selectFirst(".avatar-picker")
        val styleSel = pickerEl?.selectFirst("select[name=avatar_style]")
        val seedInput = pickerEl?.selectFirst("input[name=avatar_seed]")
        val avatarPicker = if (pickerEl != null && styleSel != null && seedInput != null) {
            AvatarPickerData(
                styleField = styleSel.attr("name").ifBlank { "avatar_style" },
                seedField = seedInput.attr("name").ifBlank { "avatar_seed" },
                styles = styleSel.select("option")
                    .map { it.attr("value") to it.text().trim() }
                    .filter { it.first.isNotBlank() },
                currentStyle = styleSel.selectFirst("option[selected]")?.attr("value")?.ifBlank { null }
                    ?: styleSel.selectFirst("option")?.attr("value") ?: "",
                currentSeed = seedInput.attr("value").trim(),
                base = pickerEl.attr("data-avatar-base").trim(),
            )
        } else null

        // 信息键值对：label.grid（span + 控件值）→ dl dt/dd → 正文正则兜底
        val info = linkedMapOf<String, String>()
        fun valueOf(el: Element): String = when (el.tagName()) {
            "select" -> el.selectFirst("option[selected]")?.text() ?: ""
            "textarea" -> el.text().trim()
            "input" -> if (el.attr("type").equals("checkbox", true))
                (if (el.hasAttr("checked")) "开" else "关") else el.attr("value").trim()
            else -> el.text().trim()
        }
        main.select("label.grid").forEach { l ->
            val k = l.selectFirst("span")?.text()?.trim() ?: return@forEach
            val v = l.selectFirst("input, select, textarea")?.let { valueOf(it) } ?: ""
            if (k.isNotBlank() && v.isNotBlank()) info[k] = v
        }
        main.select("dl").forEach { dl ->
            val dts = dl.select("dt"); val dds = dl.select("dd")
            dts.forEachIndexed { i, dt ->
                val v = dds.getOrNull(i)?.text()?.trim() ?: return@forEachIndexed
                val k = dt.text().trim()
                if (k.isNotBlank() && v.isNotBlank()) info[k] = v
            }
        }
        val text = main.text().replace(Regex("""\s+"""), " ")
        if (info.keys.none { it.contains("UID") })
            Regex("""UID\s*[:：]?\s*(\d+)""").find(text)?.let { info["UID"] = it.groupValues[1] }
        if (info.keys.none { it.contains("邮箱") })
            Regex("""[\w.+-]+@[\w-]+\.[\w.]+""").find(text)?.let { info["邮箱"] = it.value }
        if (info.keys.none { it.contains("积分") })
            Regex("""积分\s*[:：]?\s*([\d.,]+[万kK]?)""").find(text)?.let { info["积分"] = it.groupValues[1] }
        if (info.keys.none { it.contains("注册") })
            Regex("""(?:注册时间|注册日期|加入时间|加入于|注册于)\s*[:：]?\s*(\d{4}[-/年]\d{1,2}[-/月]\d{1,2}日?)""")
                .find(text)?.let { info["注册时间"] = it.groupValues[1] }

        // 表单（排除搜索表单与无提交按钮的表单）
        val parsedForms = main.select("form").filter { f ->
            !f.classNames().contains("search-form") &&
                f.selectFirst("input[name=q]") == null &&
                f.selectFirst("button[type=submit], input[type=submit]") != null
        }.mapIndexed { index, f ->
            val fields = mutableListOf<ProfileSettingsField>()
            f.select("input, select, textarea").forEach { el ->
                val name = el.attr("name")
                if (name.isBlank() || name == "_csrf") return@forEach
                when (el.tagName()) {
                    "textarea" -> fields += ProfileSettingsField(name, "textarea", labelOf(el), el.text().trim(), el.hasAttr("required"))
                    "select" -> fields += ProfileSettingsField(
                        name, "select", labelOf(el),
                        el.selectFirst("option[selected]")?.attr("value")
                            ?: el.selectFirst("option")?.attr("value") ?: "",
                        el.hasAttr("required"),
                        el.select("option").map { it.attr("value") to it.text().trim() },
                    )
                    else -> {
                        val type = el.attr("type").ifBlank { "text" }.lowercase()
                        if (type == "submit" || type == "button" || type == "reset") return@forEach
                        if (type == "checkbox")
                            fields += ProfileSettingsField(name, "checkbox", labelOf(el), if (el.hasAttr("checked")) "1" else "0", el.hasAttr("required"))
                        else if (type == "radio") {
                            if (el.hasAttr("checked"))
                                fields += ProfileSettingsField(name, "radio", labelOf(el), el.attr("value"), el.hasAttr("required"))
                        } else
                            fields += ProfileSettingsField(name, type, labelOf(el), el.attr("value").trim(), el.hasAttr("required"))
                    }
                }
            }
            val fileEl = f.selectFirst("input[type=file]")
            val formId = "profile-form-$index"
            val submitText = f.selectFirst("button[type=submit], input[type=submit]")?.text()?.trim()
                ?.takeIf { it.isNotBlank() } ?: "保存"
            val sourceForm = ProfileSourceForm(
                action = f.attr("action").ifBlank { "/profile" },
                hasFile = fileEl != null && fileEl.attr("name").isNotBlank(),
                fileFieldName = fileEl?.attr("name") ?: "",
            )
            ParsedProfileForm(ProfileSettingsForm(
                id = formId,
                title = f.selectFirst("legend")?.text()?.trim()?.takeIf { it.isNotBlank() }
                    ?: f.parents().firstOrNull()?.selectFirst("> h2, > h3, > .quick-title")?.text()?.trim()?.takeIf { it.isNotBlank() }
                    ?: f.selectFirst("button[type=submit], input[type=submit]")?.text()?.trim()?.takeIf { it.isNotBlank() }
                    ?: "设置",
                hasFile = sourceForm.hasFile,
                fields = fields,
                submitText = submitText,
            ), sourceForm)
        }
        return ProfilePageParseResult(
            page = ProfileSettingsPage(
                avatarUrl = SourceHtmlSupport.absUrl(avatar),
                info = info.toList(),
                forms = parsedForms.map { it.form },
                avatarPicker = avatarPicker,
            ),
            sourceForms = parsedForms.associate { it.form.id to it.source },
        )
    }

}
