package sb.linux.client.ui.screens

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.FormatStrikethrough
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.SentimentSatisfied
import androidx.compose.material.icons.filled.Title
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import org.jsoup.Jsoup
import sb.linux.client.data.HtmlParser
import sb.linux.client.data.LsbException
import sb.linux.client.data.ImageHostClient
import sb.linux.client.data.Session

private data class ForumOption(val id: String, val name: String)

/** 奖品行（抽奖帖，对应源站 lottery_prize_name[] 等数组字段） */
private data class PrizeRow(
    val name: String = "",
    val type: String = "points",   // points 积分 / wallet 烧饼 / code 兑换码 / manual 手工发奖
    val quantity: String = "1",
    val value: String = "",
)

private val PrizeTypeLabels = listOf(
    "points" to "积分", "wallet" to "烧饼", "code" to "兑换码", "manual" to "手工发奖",
)

private val CardCurrencyLabels = listOf("points" to "积分", "wallet" to "烧饼")

/** 从 /topic_edit 页面解析可选版块 */
private fun parseForums(html: String): List<ForumOption> {
    val d = Jsoup.parse(html, "https://linux.sb")
    return d.select("form select[name=forum_id] option").map { ForumOption(it.attr("value"), it.text()) }
}

/** 解析标题/正文/隐藏 id（编辑模式回填） */
private fun parseEditValues(html: String): Triple<String, String, String> {
    val d = Jsoup.parse(html, "https://linux.sb")
    val form = d.selectFirst("form:has(input[name=title])") ?: return Triple("", "", "0")
    return Triple(
        form.selectFirst("input[name=title]")?.attr("value") ?: "",
        form.selectFirst("textarea[name=body]")?.text() ?: "",
        form.selectFirst("input[name=id]")?.attr("value") ?: "0",
    )
}

private data class SourceEditMeta(
    val confirmation: String = "",
    val notice: String = "",
    val hiddenFields: Map<String, String> = emptyMap(),
)

/** 保留源站付费编辑确认文案及对应的一次性报价字段。 */
private fun parseSourceEditMeta(html: String): SourceEditMeta {
    val d = Jsoup.parse(html, "https://linux.sb")
    val form = d.selectFirst("form:has(input[name=title])") ?: return SourceEditMeta()
    val hidden = form.select("input[type=hidden][name]")
        .filter { it.attr("name").startsWith("sb_limit_edit_time_") }
        .associate { it.attr("name") to it.attr("value") }
    return SourceEditMeta(
        confirmation = form.attr("data-confirm").trim(),
        notice = d.selectFirst(".sb-limit-edit-time-quote")?.text()?.trim()
            ?.replace(Regex("\\s+"), " ").orEmpty(),
        hiddenFields = hidden,
    )
}

private fun parseSelectedForumId(html: String): String {
    val d = Jsoup.parse(html, "https://linux.sb")
    return d.selectFirst("form select[name=forum_id] option[selected]")?.attr("value").orEmpty()
}

/** 编辑页源站回帖排序选项；新建页不存在该控件。 */
private fun parseReplyOrderOptions(html: String): Pair<String, List<Pair<String, String>>> {
    val select = Jsoup.parse(html, "https://linux.sb").selectFirst("form select[name=reply_order]")
        ?: return "" to emptyList()
    val options = select.select("option").map { it.attr("value") to it.text().trim() }
    val selected = select.selectFirst("option[selected]")?.attr("value") ?: options.firstOrNull()?.first.orEmpty()
    return selected to options
}

/** 解析已选帖子类型（编辑抽奖帖/发卡帖时 radio 带 checked） */
private fun parseSpecialType(html: String): String {
    val d = Jsoup.parse(html, "https://linux.sb")
    val radio = d.selectFirst("input[name=topic_special_type][checked]") ?: return ""
    return radio.attr("value")   // lottery / virtual_card
}

/** 解析发帖公告：源站编辑页的发布警告（抽奖/发卡面板内），普通帖无则返回空 */
private fun parseAnnouncement(html: String): String {
    val d = Jsoup.parse(html, "https://linux.sb")
    // 发布警告（两个面板内容一致）：⚠️ 请勿发布虚假抽奖和发卡，否则封号处理！发布前请阅读《审核标准》。
    val warn = d.selectFirst(".community-lottery-publish-warning, .virtual-card-publish-warning")
        ?.text()?.trim()?.replace(Regex("\\s+"), " ")
    // 抽奖规则说明（含托管/开奖规则）
    val rule = d.selectFirst(".community-lottery-rule-note")?.text()?.trim()
    return listOfNotNull(warn?.ifBlank { null }, rule?.ifBlank { null }).joinToString("\n\n")
}

private data class LotteryInit(
    val drawAt: String, val target: String, val minChars: String,
    val captchaRequired: Boolean, val prizes: List<PrizeRow>,
)

/** 解析抽奖面板初始值（编辑模式回填） */
private fun parseLotteryInit(html: String): LotteryInit {
    val d = Jsoup.parse(html, "https://linux.sb")
    val form = d.selectFirst("form:has(input[name=title])")
    fun v(name: String) = form?.selectFirst("[name=$name]")?.attr("value") ?: ""
    val drawAt = v("lottery_draw_at")   // datetime-local 原始格式 yyyy-MM-ddTHH:mm
    val prizes = (form?.select(".community-lottery-prize-row") ?: emptyList()).map { row ->
        PrizeRow(
            name = row.selectFirst("[name=lottery_prize_name\\[\\]]")?.attr("value") ?: "",
            type = row.selectFirst("[name=lottery_prize_type\\[\\]] option[selected]")?.attr("value")
                ?: row.selectFirst("[name=lottery_prize_type\\[\\]]")?.selectFirst("option")?.attr("value") ?: "points",
            quantity = row.selectFirst("[name=lottery_prize_quantity\\[\\]]")?.attr("value") ?: "1",
            value = row.selectFirst("[name=lottery_prize_value\\[\\]]")?.text() ?: "",
        )
    }
    return LotteryInit(
        drawAt = drawAt, target = v("lottery_participant_target").ifBlank { "0" },
        minChars = v("lottery_min_reply_chars").ifBlank { "5" },
        captchaRequired = form?.selectFirst("[name=lottery_reply_captcha_required]")?.hasAttr("checked") ?: true,
        prizes = prizes,
    )
}

private data class CardInit(
    val name: String, val currency: String, val price: String, val limit: String,
    val autoReply: Boolean, val autoReplyContent: String, val values: String,
)

/** 解析发卡面板初始值（编辑模式回填） */
private fun parseCardInit(html: String): CardInit {
    val d = Jsoup.parse(html, "https://linux.sb")
    val form = d.selectFirst("form:has(input[name=title])")
    fun v(name: String) = form?.selectFirst("[name=$name]")?.attr("value") ?: ""
    return CardInit(
        name = v("virtual_card_name"),
        currency = form?.selectFirst("[name=virtual_card_currency] option[selected]")?.attr("value") ?: "points",
        price = v("virtual_card_price").ifBlank { "1" },
        limit = v("virtual_card_purchase_limit").ifBlank { "1" },
        autoReply = form?.selectFirst("[name=virtual_card_auto_reply]")?.hasAttr("checked") ?: false,
        autoReplyContent = form?.selectFirst("[name=virtual_card_auto_reply_content]")?.text()
            ?: "已成功兑换虚拟卡「{card_name}」。",
        values = form?.selectFirst("[name=virtual_card_values]")?.text() ?: "",
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun NewTopicScreen(session: Session, nav: NavHostController, editId: Long = 0L) {
    var forums by remember { mutableStateOf<List<ForumOption>>(emptyList()) }
    var forumId by remember { mutableStateOf<String?>(null) }
    var forumExpanded by remember { mutableStateOf(false) }
    var title by remember { mutableStateOf("") }
    var body by rememberSaveable { mutableStateOf("") }
    var bodySelection by remember { mutableStateOf(TextRange.Zero) }
    var internalId by remember { mutableStateOf("0") }
    // 帖子类型：空 = 普通帖 / lottery 抽奖帖 / virtual_card 发卡帖（与源站 radio topic_special_type 一致）
    var specialType by remember { mutableStateOf("") }
    // 抽奖字段
    var drawAt by remember { mutableStateOf("") }
    var participantTarget by remember { mutableStateOf("0") }
    var minReplyChars by remember { mutableStateOf("5") }
    var replyCaptcha by remember { mutableStateOf(true) }
    var prizes by remember { mutableStateOf(listOf(PrizeRow())) }
    // 发卡字段
    var cardName by remember { mutableStateOf("") }
    var cardCurrency by remember { mutableStateOf("points") }
    var cardPrice by remember { mutableStateOf("1") }
    var cardLimit by remember { mutableStateOf("1") }
    var cardAutoReply by remember { mutableStateOf(false) }
    var cardAutoReplyContent by remember { mutableStateOf("已成功兑换虚拟卡「{card_name}」。") }
    var cardValues by remember { mutableStateOf("") }
    var currencyMenu by remember { mutableStateOf(false) }
    var prizeTypeMenu by remember { mutableStateOf<Int?>(null) }
    // 发帖公告（源站发布警告），每次发布前确认弹窗展示
    var announcement by remember { mutableStateOf("") }
    var sourceEditMeta by remember { mutableStateOf(SourceEditMeta()) }
    var replyOrder by remember { mutableStateOf("") }
    var replyOrderOptions by remember { mutableStateOf<List<Pair<String, String>>>(emptyList()) }
    var replyOrderMenu by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(true) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var confirmOpen by remember { mutableStateOf(false) }
    var restoreDraftOpen by remember { mutableStateOf(false) }
    var exitDraftOpen by remember { mutableStateOf(false) }
    var draftMenuOpen by remember { mutableStateOf(false) }
    // 预览模式：正文 Markdown 实时渲染为帖子样式（HtmlContent）
    var previewMode by remember { mutableStateOf(false) }
    var fullScreenEditor by rememberSaveable { mutableStateOf(false) }
    var emojiMenuOpen by remember { mutableStateOf(false) }
    var imageUploadBusy by remember { mutableStateOf(false) }
    val pageScrollState = rememberScrollState()
    val bodyBringIntoViewRequester = remember { BringIntoViewRequester() }
    var bodyFocused by remember { mutableStateOf(false) }
    val imeVisible = WindowInsets.isImeVisible
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val draftPrefs = remember { context.getSharedPreferences("lsb_topic_drafts", android.content.Context.MODE_PRIVATE) }
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) scope.launch {
            imageUploadBusy = true
            try {
                val url = ImageHostClient.upload(context, session.settings, uri)
                val at = bodySelection.min.coerceIn(0, body.length)
                val end = bodySelection.max.coerceIn(at, body.length)
                val image = "![图片]($url)"
                body = body.replaceRange(at, end, image)
                bodySelection = TextRange(at + image.length)
                session.showToast("图片上传成功")
            } catch (e: Exception) {
                session.showToast(e.message ?: "图片上传失败")
            } finally { imageUploadBusy = false }
        }
    }

    // adjustResize 在 edge-to-edge 窗口中不会替滚动页保证焦点位置；IME 完成展开后再请求一次，
    // 让普通编辑模式和全屏模式一样，光标始终处于键盘上方。
    LaunchedEffect(bodyFocused, imeVisible) {
        if (bodyFocused && imeVisible) {
            delay(120)
            bodyBringIntoViewRequester.bringIntoView()
        }
    }

    fun saveDraft() {
        if (editId > 0) return
        val extra = org.json.JSONObject().apply {
            put("specialType", specialType); put("drawAt", drawAt); put("participantTarget", participantTarget)
            put("minReplyChars", minReplyChars); put("replyCaptcha", replyCaptcha)
            put("prizes", org.json.JSONArray(prizes.map { prize -> org.json.JSONObject().apply {
                put("name", prize.name); put("type", prize.type); put("quantity", prize.quantity); put("value", prize.value)
            } }))
            put("cardName", cardName); put("cardCurrency", cardCurrency); put("cardPrice", cardPrice)
            put("cardLimit", cardLimit); put("cardAutoReply", cardAutoReply)
            put("cardAutoReplyContent", cardAutoReplyContent); put("cardValues", cardValues)
        }
        draftPrefs.edit()
            .putString("title", title).putString("body", body)
            .putString("extra", extra.toString())
            .putString("forum_id", forumId ?: "").putLong("saved_at", System.currentTimeMillis())
            .apply()
        session.showToast("草稿已保存")
    }
    fun clearDraft() = draftPrefs.edit().clear().apply()

    LaunchedEffect(editId) {
        try {
            val path = if (editId > 0) "/topic_edit/$editId" else "/topic_edit"
            val resp = session.client.get(path)
            forums = parseForums(resp.html)
            announcement = parseAnnouncement(resp.html)
            sourceEditMeta = parseSourceEditMeta(resp.html)
            parseReplyOrderOptions(resp.html).let { (selected, options) ->
                replyOrder = selected; replyOrderOptions = options
            }
            specialType = parseSpecialType(resp.html)
            forumId = parseSelectedForumId(resp.html).ifBlank { forums.firstOrNull()?.id.orEmpty() }
            if (editId > 0) {
                val (t, b, id) = parseEditValues(resp.html)
                title = t; body = b; internalId = id
                when (specialType) {
                    "lottery" -> {
                        val li = parseLotteryInit(resp.html)
                        drawAt = li.drawAt; participantTarget = li.target
                        minReplyChars = li.minChars; replyCaptcha = li.captchaRequired
                        if (li.prizes.isNotEmpty()) prizes = li.prizes
                    }
                    "virtual_card" -> {
                        val ci = parseCardInit(resp.html)
                        cardName = ci.name; cardCurrency = ci.currency; cardPrice = ci.price
                        cardLimit = ci.limit; cardAutoReply = ci.autoReply
                        cardAutoReplyContent = ci.autoReplyContent; cardValues = ci.values
                    }
                }
            } else {
                internalId = "0"
                if (!draftPrefs.getString("body", "").isNullOrBlank() ||
                    !draftPrefs.getString("title", "").isNullOrBlank() || draftPrefs.contains("extra")
                ) restoreDraftOpen = true
            }
        } catch (e: Exception) {
            error = e.message
        } finally { loading = false }
    }

    fun insert(before: String, after: String = before, placeholder: String = "") {
        val start = bodySelection.min.coerceIn(0, body.length)
        val end = bodySelection.max.coerceIn(start, body.length)
        val selection = body.substring(start, end).ifEmpty { placeholder }
        val replacement = before + selection + after
        body = body.replaceRange(start, end, replacement)
        bodySelection = if (before.isEmpty() && after.isEmpty()) TextRange(start + replacement.length)
            else TextRange(start + before.length, start + before.length + selection.length)
    }

    /** 确认公告后的实际提交：按帖子类型组装表单字段（与源站网页端一致） */
    fun doSubmit() {
        val fid = forumId ?: run { error = "请选择版块"; return }
        busy = true; error = null
        scope.launch {
            try {
                if (!session.loginState.loggedIn) throw LsbException("请先登录")
                val csrf = session.client.csrf()
                val form = mutableListOf(
                    "_csrf" to csrf,
                    "id" to internalId,
                    "forum_id" to fid,
                    "title" to title,
                    "body" to body,
                )
                if (editId > 0) form += sourceEditMeta.hiddenFields.toList()
                if (editId > 0 && replyOrder.isNotBlank()) form += "reply_order" to replyOrder
                when (specialType) {
                    "lottery" -> {
                        // 抽奖帖：datetime-local 原始格式 yyyy-MM-ddTHH:mm
                        val drawAtVal = drawAt.trim().replace(" ", "T")
                        form += "topic_special_type" to "lottery"
                        form += "community_lottery_original_type" to ""
                        form += "lottery_draw_at" to drawAtVal
                        form += "lottery_participant_target" to participantTarget.trim()
                        form += "lottery_min_reply_chars" to minReplyChars.trim()
                        if (replyCaptcha) form += "lottery_reply_captcha_required" to "1"
                        // 奖品数组字段（重复键）
                        prizes.filter { it.name.isNotBlank() }.forEach { p ->
                            form += "lottery_prize_name[]" to p.name.trim()
                            form += "lottery_prize_type[]" to p.type
                            form += "lottery_prize_quantity[]" to p.quantity.trim().ifBlank { "1" }
                            form += "lottery_prize_value[]" to p.value.trim()
                        }
                    }
                    "virtual_card" -> {
                        form += "topic_special_type" to "virtual_card"
                        form += "virtual_card_original_type" to ""
                        form += "virtual_card_name" to cardName.trim()
                        form += "virtual_card_currency" to cardCurrency
                        form += "virtual_card_price" to cardPrice.trim().ifBlank { "1" }
                        form += "virtual_card_purchase_limit" to cardLimit.trim().ifBlank { "1" }
                        if (cardAutoReply) {
                            form += "virtual_card_auto_reply" to "1"
                            form += "virtual_card_auto_reply_content" to cardAutoReplyContent
                        }
                        form += "virtual_card_values" to cardValues
                    }
                }
                val submitPath = if (editId > 0) "/topic_edit/$editId" else "/topic_edit"
                val resp = session.client.postFormPairs(submitPath, form)
                if (resp.url.contains("form_error")) {
                    error = HtmlParser.extractError(resp.html).ifBlank { "发布失败" }
                } else {
                    if (editId == 0L) clearDraft()
                    if (editId == 0L) session.settings.recordUsageEvent("topic", title = title)
                    session.showToast(if (editId > 0) "编辑成功" else "发布成功")
                    nav.popBackStack()
                }
            } catch (e: Exception) {
                error = e.message ?: "发布失败"
            } finally { busy = false }
        }
    }

    val hasDraftContent = title.isNotBlank() || body.isNotBlank() || specialType.isNotBlank()
    BackHandler(enabled = fullScreenEditor || (editId == 0L && hasDraftContent)) {
        if (fullScreenEditor) fullScreenEditor = false else exitDraftOpen = true
    }

    if (restoreDraftOpen) {
        AlertDialog(
            onDismissRequest = { restoreDraftOpen = false },
            title = { Text("发现未发布草稿") },
            text = { Text("是否恢复标题、正文、版块及抽奖/发卡配置？") },
            confirmButton = { TextButton(onClick = {
                title = draftPrefs.getString("title", "").orEmpty()
                body = draftPrefs.getString("body", "").orEmpty()
                draftPrefs.getString("forum_id", "")?.takeIf { it.isNotBlank() }?.let { forumId = it }
                runCatching { org.json.JSONObject(draftPrefs.getString("extra", "{}").orEmpty()) }.getOrNull()?.let { extra ->
                    specialType = extra.optString("specialType")
                    drawAt = extra.optString("drawAt"); participantTarget = extra.optString("participantTarget", "0")
                    minReplyChars = extra.optString("minReplyChars", "5"); replyCaptcha = extra.optBoolean("replyCaptcha", true)
                    extra.optJSONArray("prizes")?.let { rows ->
                        prizes = (0 until rows.length()).mapNotNull { index -> rows.optJSONObject(index)?.let {
                            PrizeRow(it.optString("name"), it.optString("type", "points"), it.optString("quantity", "1"), it.optString("value"))
                        } }.ifEmpty { listOf(PrizeRow()) }
                    }
                    cardName = extra.optString("cardName"); cardCurrency = extra.optString("cardCurrency", "points")
                    cardPrice = extra.optString("cardPrice", "1"); cardLimit = extra.optString("cardLimit", "1")
                    cardAutoReply = extra.optBoolean("cardAutoReply"); cardAutoReplyContent = extra.optString("cardAutoReplyContent")
                    cardValues = extra.optString("cardValues")
                }
                restoreDraftOpen = false
            }) { Text("恢复") } },
            dismissButton = { TextButton(onClick = { clearDraft(); restoreDraftOpen = false }) { Text("丢弃") } }
        )
    }
    if (exitDraftOpen) {
        AlertDialog(
            onDismissRequest = { exitDraftOpen = false },
            title = { Text("保存到草稿箱？") },
            text = { Text("当前内容尚未发布，可以保存后下次继续编辑。") },
            confirmButton = { TextButton(onClick = { saveDraft(); exitDraftOpen = false; nav.popBackStack() }) { Text("保存并退出") } },
            dismissButton = {
                Row {
                    TextButton(onClick = { exitDraftOpen = false }) { Text("取消") }
                    TextButton(onClick = { clearDraft(); exitDraftOpen = false; nav.popBackStack() }) { Text("不保存") }
                }
            }
        )
    }

    // 发帖前确认弹窗：展示源站发帖公告，每次发布前都要确认
    if (confirmOpen) {
        AlertDialog(
            onDismissRequest = { confirmOpen = false },
            title = {
                Text(
                    if (editId > 0) "确认编辑"
                    else if (announcement.isNotBlank()) "发帖公告"
                    else "确认发布"
                )
            },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    val editMessage = listOf(sourceEditMeta.notice, sourceEditMeta.confirmation)
                        .filter { it.isNotBlank() }.distinct().joinToString("\n\n")
                    if (editId > 0 && editMessage.isNotBlank()) {
                        Text(editMessage, style = MaterialTheme.typography.bodyMedium)
                    } else if (announcement.isNotBlank()) {
                        Text(announcement, style = MaterialTheme.typography.bodyMedium)
                    } else {
                        Text(
                            "即将发布到「${forums.firstOrNull { it.id == forumId }?.name ?: "未选择"}」版块。" +
                                "请确认内容符合社区规则：不发布违规、广告或侵权内容。",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { confirmOpen = false; doSubmit() }, enabled = !busy) {
                    Text(if (busy) "提交中…" else if (editId > 0) "确认保存" else "确认发布")
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmOpen = false }) { Text("取消") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (editId > 0) "编辑主题" else "发布新主题") },
                navigationIcon = {
                    IconButton(onClick = {
                        if (fullScreenEditor) fullScreenEditor = false
                        else if (editId == 0L && hasDraftContent) exitDraftOpen = true
                        else nav.popBackStack()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                    }
                },
                actions = {
                    if (!previewMode) {
                        IconButton(onClick = { fullScreenEditor = !fullScreenEditor }) {
                            Icon(
                                if (fullScreenEditor) Icons.Filled.FullscreenExit else Icons.Filled.Fullscreen,
                                if (fullScreenEditor) "退出全屏编辑" else "全屏编辑",
                            )
                        }
                    }
                    if (editId == 0L) {
                        Box {
                            IconButton(onClick = { draftMenuOpen = true }) { Icon(Icons.Filled.Save, "草稿箱") }
                            DropdownMenu(expanded = draftMenuOpen, onDismissRequest = { draftMenuOpen = false }) {
                                DropdownMenuItem(text = { Text("保存当前草稿") }, enabled = hasDraftContent, onClick = { saveDraft(); draftMenuOpen = false })
                                DropdownMenuItem(text = { Text("恢复已保存草稿") }, enabled = draftPrefs.contains("saved_at"), onClick = { draftMenuOpen = false; restoreDraftOpen = true })
                            }
                        }
                    }
                    // 正文 Markdown 实时预览开关
                    if (!fullScreenEditor) IconButton(onClick = { previewMode = !previewMode }) {
                        Icon(
                            if (previewMode) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                            if (previewMode) "退出预览" else "预览"
                        )
                    }
                    // 发布前先弹公告确认（每次都要确认）
                    TextButton(
                        onClick = {
                            if (title.isBlank() || body.isBlank()) {
                                error = "标题和内容不能为空"; return@TextButton
                            }
                            if (forumId == null) { error = "请选择版块"; return@TextButton }
                            when (specialType) {
                                "lottery" -> {
                                    if (drawAt.isBlank()) { error = "请填写开奖时间"; return@TextButton }
                                    if (prizes.none { it.name.isNotBlank() }) {
                                        error = "请至少添加一项奖品"; return@TextButton
                                    }
                                }
                                "virtual_card" -> {
                                    if (cardName.isBlank()) { error = "请填写卡片名称"; return@TextButton }
                                    if (cardValues.isBlank()) { error = "请添加卡密"; return@TextButton }
                                }
                            }
                            confirmOpen = true
                        },
                        enabled = !busy
                    ) { Text(if (busy) "提交中…" else if (editId > 0) "保存" else "发布") }
                }
            )
        }
    ) { pad ->
        if (loading) {
            sb.linux.client.ui.LoadingBox()
            return@Scaffold
        }
        if (fullScreenEditor) {
            Column(
                Modifier.padding(pad).fillMaxSize().imePadding()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                sb.linux.client.ui.MarkdownEditorTools(
                    onInsert = { b, a, p -> insert(b, a, p) },
                    onUpload = { imagePicker.launch("image/*") },
                    uploading = imageUploadBusy,
                )
                OutlinedTextField(
                    value = TextFieldValue(body, bodySelection),
                    onValueChange = { body = it.text; bodySelection = it.selection },
                    label = { Text("内容 (Markdown)") },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    textStyle = TextStyle(fontFamily = FontFamily.Monospace),
                )
            }
            return@Scaffold
        }
        Column(
            Modifier
                .padding(pad)
                .fillMaxSize()
                .imePadding()
                .verticalScroll(pageScrollState)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (!session.loginState.loggedIn) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.errorContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            "未登录，发布前请先登录",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Button(onClick = { nav.navigate("login") }) { Text("去登录") }
                    }
                }
            }
            error?.let {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.errorContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        it,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                    )
                }
            }

            // 版块选择
            Box {
                OutlinedButton(onClick = { forumExpanded = true }, shape = RoundedCornerShape(14.dp)) {
                    Text("版块：${forums.firstOrNull { it.id == forumId }?.name ?: "选择"}")
                }
                DropdownMenu(expanded = forumExpanded, onDismissRequest = { forumExpanded = false }) {
                    forums.forEach { f ->
                        DropdownMenuItem(
                            text = { Text(f.name) },
                            onClick = { forumId = f.id; forumExpanded = false }
                        )
                    }
                }
            }

            // 帖子类型：普通 / 抽奖 / 发卡（与源站 topic_special_type 一致）
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = specialType == "",
                    onClick = { specialType = "" },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 3),
                ) { Text("普通帖") }
                SegmentedButton(
                    selected = specialType == "lottery",
                    onClick = { specialType = "lottery" },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 3),
                ) { Text("抽奖帖") }
                SegmentedButton(
                    selected = specialType == "virtual_card",
                    onClick = { specialType = "virtual_card" },
                    shape = SegmentedButtonDefaults.itemShape(index = 2, count = 3),
                ) { Text("发卡帖") }
            }

            // ---------- 抽奖帖面板 ----------
            if (specialType == "lottery") {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            "用户回帖参与抽奖；发布抽奖至少需托管 5 个烧饼。",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        OutlinedTextField(
                            value = drawAt, onValueChange = { drawAt = it },
                            label = { Text("开奖时间") },
                            supportingText = { Text("格式 2026-08-25 20:00，最晚 3 天后") },
                            singleLine = true, shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = participantTarget, onValueChange = { participantTarget = it },
                            label = { Text("自动开奖人数") },
                            supportingText = { Text("达到该参与人数后自动开奖，填 0 不启用") },
                            singleLine = true, shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = minReplyChars, onValueChange = { minReplyChars = it },
                            label = { Text("参与回复最少字数") },
                            supportingText = { Text("去除空白后计算，低于门槛不能回复参与") },
                            singleLine = true, shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Switch(checked = replyCaptcha, onCheckedChange = { replyCaptcha = it })
                            Spacer(Modifier.width(8.dp))
                            Column {
                                Text("回帖需要验证码", style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    "默认开启，防止脚本批量回帖参与抽奖",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        HorizontalDivider()
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("奖品设置", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.weight(1f))
                            FilledTonalIconButton(onClick = { prizes = prizes + PrizeRow() }) {
                                Icon(Icons.Filled.Add, "添加奖品")
                            }
                        }
                        prizes.forEachIndexed { i, p ->
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            ) {
                                Column(
                                    Modifier.padding(10.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        OutlinedTextField(
                                            value = p.name,
                                            onValueChange = { v ->
                                                prizes = prizes.toMutableList().also { it[i] = p.copy(name = v) }
                                            },
                                            label = { Text("奖品名称") },
                                            singleLine = true,
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier.weight(1f)
                                        )
                                        IconButton(onClick = { prizes = prizes.filterIndexed { j, _ -> j != i } }) {
                                            Icon(Icons.Filled.Close, "删除奖品")
                                        }
                                    }
                                    Box {
                                        OutlinedButton(onClick = { prizeTypeMenu = i }) {
                                            Text("类型：${PrizeTypeLabels.firstOrNull { it.first == p.type }?.second ?: "积分"}")
                                        }
                                        DropdownMenu(
                                            expanded = prizeTypeMenu == i,
                                            onDismissRequest = { prizeTypeMenu = null }
                                        ) {
                                            PrizeTypeLabels.forEach { (v, l) ->
                                                DropdownMenuItem(
                                                    text = { Text(l) },
                                                    onClick = {
                                                        prizes = prizes.toMutableList().also { it[i] = p.copy(type = v) }
                                                        prizeTypeMenu = null
                                                    }
                                                )
                                            }
                                        }
                                    }
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        OutlinedTextField(
                                            value = p.quantity,
                                            onValueChange = { v ->
                                                prizes = prizes.toMutableList().also { it[i] = p.copy(quantity = v) }
                                            },
                                            label = { Text("份数") },
                                            singleLine = true,
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier.weight(1f)
                                        )
                                        OutlinedTextField(
                                            value = p.value,
                                            onValueChange = { v ->
                                                prizes = prizes.toMutableList().also { it[i] = p.copy(value = v) }
                                            },
                                            label = {
                                                Text(
                                                    if (p.type == "points") "每份积分"
                                                    else if (p.type == "wallet") "每份烧饼数"
                                                    else if (p.type == "code") "兑换码（每行一个）"
                                                    else "说明"
                                                )
                                            },
                                            singleLine = true,
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier.weight(1.4f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ---------- 发卡帖面板 ----------
            if (specialType == "virtual_card") {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            "用户用积分或烧饼兑换卡密；编辑帖子时卡密列表只显示未售出的卡。",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        OutlinedTextField(
                            value = cardName, onValueChange = { cardName = it },
                            label = { Text("卡片名称") },
                            supportingText = { Text("例如：月卡兑换码、软件激活码") },
                            singleLine = true, shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Box {
                            OutlinedButton(onClick = { currencyMenu = true }) {
                                Text("积分种类：${CardCurrencyLabels.firstOrNull { it.first == cardCurrency }?.second ?: "积分"}")
                            }
                            DropdownMenu(expanded = currencyMenu, onDismissRequest = { currencyMenu = false }) {
                                CardCurrencyLabels.forEach { (v, l) ->
                                    DropdownMenuItem(
                                        text = { Text(l) },
                                        onClick = { cardCurrency = v; currencyMenu = false }
                                    )
                                }
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = cardPrice, onValueChange = { cardPrice = it },
                                label = { Text("兑换价格") },
                                singleLine = true, shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = cardLimit, onValueChange = { cardLimit = it },
                                label = { Text("每人限购") },
                                supportingText = { Text("0 为不限制") },
                                singleLine = true, shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Switch(checked = cardAutoReply, onCheckedChange = { cardAutoReply = it })
                            Spacer(Modifier.width(8.dp))
                            Column {
                                Text("购买成功后自动回帖", style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    "开启后，买家兑换成功会自动回复本帖",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        if (cardAutoReply) {
                            OutlinedTextField(
                                value = cardAutoReplyContent,
                                onValueChange = { cardAutoReplyContent = it },
                                label = { Text("自动回帖内容") },
                                supportingText = { Text("支持变量 {card_name} {price} {username}") },
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        OutlinedTextField(
                            value = cardValues, onValueChange = { cardValues = it },
                            label = { Text("添加虚拟卡") },
                            supportingText = { Text("每行一张卡，最多 5000 张") },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 120.dp)
                        )
                    }
                }
            }

            OutlinedTextField(
                value = title, onValueChange = { title = it },
                label = { Text("标题") }, singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            )

            if (editId > 0 && replyOrderOptions.isNotEmpty()) {
                Box {
                    OutlinedButton(onClick = { replyOrderMenu = true }, shape = RoundedCornerShape(14.dp)) {
                        Text("回帖排序：${replyOrderOptions.firstOrNull { it.first == replyOrder }?.second.orEmpty()}")
                    }
                    DropdownMenu(expanded = replyOrderMenu, onDismissRequest = { replyOrderMenu = false }) {
                        replyOrderOptions.forEach { (value, label) ->
                            DropdownMenuItem(
                                text = { Text(label) },
                                onClick = { replyOrder = value; replyOrderMenu = false },
                            )
                        }
                    }
                }
            }

            if (!previewMode) sb.linux.client.ui.MarkdownEditorTools(
                onInsert = { b, a, p -> insert(b, a, p) },
                onUpload = { imagePicker.launch("image/*") },
                uploading = imageUploadBusy,
            )

            if (previewMode) {
                // 预览：Markdown → HTML 后用帖子同款渲染器展示（表格/代码块/链接等所见即所得）
                val previewHtml = remember(body) { HtmlParser.markdownToHtml(body) }
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 260.dp)
                ) {
                    if (body.isBlank()) {
                        Text(
                            "暂无内容，退出预览继续编辑",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(16.dp)
                        )
                    } else {
                        sb.linux.client.ui.HtmlContent(
                            html = previewHtml,
                            modifier = Modifier.padding(16.dp),
                            mergeImages = false,
                            // 每敲一个键都是全新 HTML，不写共享解析缓存（否则挤掉阅读中的楼层）
                            cacheable = false
                        )
                    }
                }
            } else {
                OutlinedTextField(
                    value = TextFieldValue(body, bodySelection),
                    onValueChange = { body = it.text; bodySelection = it.selection },
                    label = { Text("内容 (Markdown)") },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 260.dp)
                        .bringIntoViewRequester(bodyBringIntoViewRequester)
                        .onFocusChanged { bodyFocused = it.isFocused },
                    textStyle = TextStyle(fontFamily = FontFamily.Monospace)
                )
            }
        }
    }
}
