package sb.linux.client.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Title
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import kotlinx.coroutines.launch
import org.jsoup.Jsoup
import sb.linux.client.data.HtmlParser
import sb.linux.client.data.LsbException
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewTopicScreen(session: Session, nav: NavHostController, editId: Long = 0L) {
    var forums by remember { mutableStateOf<List<ForumOption>>(emptyList()) }
    var forumId by remember { mutableStateOf<String?>(null) }
    var forumExpanded by remember { mutableStateOf(false) }
    var title by remember { mutableStateOf("") }
    var body by remember { mutableStateOf("") }
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
    var loading by remember { mutableStateOf(true) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var confirmOpen by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(editId) {
        try {
            val path = if (editId > 0) "/topic_edit?id=$editId" else "/topic_edit"
            val resp = session.client.get(path)
            forums = parseForums(resp.html)
            announcement = parseAnnouncement(resp.html)
            specialType = parseSpecialType(resp.html)
            forumId = forums.firstOrNull()?.id
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
            }
        } catch (e: Exception) {
            error = e.message
        } finally { loading = false }
    }

    fun insert(before: String, after: String = before, placeholder: String = "") {
        body = buildString {
            append(body)
            if (body.isNotEmpty() && !body.endsWith("\n")) append("\n")
            append(before)
            if (placeholder.isNotBlank()) append(placeholder)
            append(after)
        }
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
                val resp = session.client.postFormPairs("/topic_edit", form)
                if (resp.url.contains("form_error")) {
                    error = HtmlParser.extractError(resp.html).ifBlank { "发布失败" }
                } else {
                    session.showToast("发布成功")
                    nav.popBackStack()
                }
            } catch (e: Exception) {
                error = e.message ?: "发布失败"
            } finally { busy = false }
        }
    }

    // 发帖前确认弹窗：展示源站发帖公告，每次发布前都要确认
    if (confirmOpen) {
        AlertDialog(
            onDismissRequest = { confirmOpen = false },
            title = { Text(if (announcement.isNotBlank()) "发帖公告" else "确认发布") },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    if (announcement.isNotBlank()) {
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
                    Text(if (busy) "发布中…" else "确认发布")
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
                    IconButton(onClick = { nav.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                    }
                },
                actions = {
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
                    ) { Text(if (busy) "发布中…" else "发布") }
                }
            )
        }
    ) { pad ->
        if (loading) {
            sb.linux.client.ui.LoadingBox()
            return@Scaffold
        }
        Column(
            Modifier
                .padding(pad)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
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

            // Markdown 工具栏
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    IconButton(onClick = { insert("**", "**", "粗体") }) { Icon(Icons.Filled.FormatBold, "粗体") }
                    IconButton(onClick = { insert("*", "*", "斜体") }) { Icon(Icons.Filled.FormatItalic, "斜体") }
                    IconButton(onClick = { insert("## ", "", "标题") }) { Icon(Icons.Filled.Title, "标题") }
                    IconButton(onClick = { insert("> ", "", "引用") }) { Icon(Icons.Filled.FormatQuote, "引用") }
                    IconButton(onClick = { insert("`", "`", "代码") }) { Icon(Icons.Filled.Code, "代码") }
                    IconButton(onClick = { insert("```\n", "\n```", "代码块") }) { Icon(Icons.Filled.Code, "代码块") }
                    IconButton(onClick = { insert("- ", "", "列表项") }) { Icon(Icons.AutoMirrored.Filled.FormatListBulleted, "列表") }
                    IconButton(onClick = { insert("[", "](https://)", "链接文字") }) { Icon(Icons.Filled.Link, "链接") }
                }
            }

            OutlinedTextField(
                value = body,
                onValueChange = { body = it },
                label = { Text("内容 (Markdown)") },
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 260.dp),
                textStyle = TextStyle(fontFamily = FontFamily.Monospace)
            )
            Text(
                "支持 Markdown 语法，与源站编辑器一致。@用户名 可提及他人。",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
