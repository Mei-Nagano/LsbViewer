package sb.linux.client.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import sb.linux.client.ui.theme.LocalAppSpacing
import sb.linux.client.ui.session.Session
import sb.linux.client.common.content.HtmlTextConversion
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.ScrollState
import androidx.compose.ui.text.TextRange

@Composable
internal fun NewTopicFormContent(
    pad: PaddingValues,
    form: NewTopicFormState,
    session: Session,
    nav: NavHostController,
    editId: Long,
    body: String,
    bodySelection: TextRange,
    fullScreenEditor: Boolean,
    previewMode: Boolean,
    pageScrollState: ScrollState,
    bodyBringIntoViewRequester: BringIntoViewRequester,
    onBodyChange: (TextFieldValue) -> Unit,
    onInsert: (String, String, String) -> Unit,
    onUploadImage: () -> Unit,
) {
        val spacing = LocalAppSpacing.current
        if (form.loading) {
            sb.linux.client.ui.components.feedback.LoadingBox()
            return
        }
        if (fullScreenEditor) {
            Column(
                Modifier.padding(pad).fillMaxSize().imePadding()
                    .padding(horizontal = spacing.pageHorizontal, vertical = spacing.item),
                verticalArrangement = Arrangement.spacedBy(spacing.item),
            ) {
                sb.linux.client.ui.content.MarkdownEditorTools(
                    onInsert = { b, a, p -> onInsert(b, a, p) },
                    onUpload = { onUploadImage() },
                    uploading = form.imageUploadBusy,
                )
                OutlinedTextField(
                    value = TextFieldValue(body, bodySelection),
                    onValueChange = { onBodyChange(it) },
                    label = { Text("内容 (Markdown)") },
                    shape = MaterialTheme.shapes.large,
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    textStyle = TextStyle(fontFamily = FontFamily.Monospace),
                )
            }
            return
        }
        Column(
            Modifier
                .padding(pad)
                .fillMaxSize()
                .imePadding()
                .verticalScroll(pageScrollState)
                .padding(spacing.pageHorizontal),
            verticalArrangement = Arrangement.spacedBy(spacing.contentGap)
        ) {
            if (!session.account.loginState.loggedIn) {
                Surface(
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.errorContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(LocalAppSpacing.current.pageHorizontal), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            "未登录，发布前请先登录",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Button(onClick = { nav.navigate("login") }) { Text("去登录") }
                    }
                }
            }
            form.error?.let {
                sb.linux.client.ui.components.feedback.AppNotice(it)
            }

            // 版块选择
            Box {
                OutlinedButton(onClick = { form.forumExpanded = true }, shape = MaterialTheme.shapes.large) {
                    Text("版块：${form.forums.firstOrNull { it.id == form.forumId }?.name ?: "选择"}")
                }
                DropdownMenu(expanded = form.forumExpanded, onDismissRequest = { form.forumExpanded = false }) {
                    form.forums.forEach { f ->
                        DropdownMenuItem(
                            text = { Text(f.name) },
                            onClick = { form.forumId = f.id; form.forumExpanded = false }
                        )
                    }
                }
            }

            // 帖子类型：普通 / 抽奖 / 发卡（与源站 topic_special_type 一致）
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = form.specialType == "",
                    onClick = { form.specialType = "" },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 3),
                ) { Text("普通帖") }
                SegmentedButton(
                    selected = form.specialType == "lottery",
                    onClick = { form.specialType = "lottery" },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 3),
                ) { Text("抽奖帖") }
                SegmentedButton(
                    selected = form.specialType == "virtual_card",
                    onClick = { form.specialType = "virtual_card" },
                    shape = SegmentedButtonDefaults.itemShape(index = 2, count = 3),
                ) { Text("发卡帖") }
            }

            LotteryTopicFields(
                drawAt = form.drawAt,
                onDrawAtChange = { form.drawAt = it },
                participantTarget = form.participantTarget,
                onParticipantTargetChange = { form.participantTarget = it },
                minReplyChars = form.minReplyChars,
                onMinReplyCharsChange = { form.minReplyChars = it },
                replyCaptcha = form.replyCaptcha,
                onReplyCaptchaChange = { form.replyCaptcha = it },
                prizes = form.prizes,
                onPrizesChange = { form.prizes = it },
                prizeTypeMenu = form.prizeTypeMenu,
                onPrizeTypeMenuChange = { form.prizeTypeMenu = it },
            )
            VirtualCardTopicFields(
                cardName = form.cardName,
                onCardNameChange = { form.cardName = it },
                cardCurrency = form.cardCurrency,
                onCardCurrencyChange = { form.cardCurrency = it },
                currencyMenu = form.currencyMenu,
                onCurrencyMenuChange = { form.currencyMenu = it },
                cardPrice = form.cardPrice,
                onCardPriceChange = { form.cardPrice = it },
                cardLimit = form.cardLimit,
                onCardLimitChange = { form.cardLimit = it },
                cardAutoReply = form.cardAutoReply,
                onCardAutoReplyChange = { form.cardAutoReply = it },
                cardAutoReplyContent = form.cardAutoReplyContent,
                onCardAutoReplyContentChange = { form.cardAutoReplyContent = it },
                cardValues = form.cardValues,
                onCardValuesChange = { form.cardValues = it },
            )
            OutlinedTextField(
                value = form.title, onValueChange = { form.title = it },
                label = { Text("标题") }, singleLine = true,
                shape = MaterialTheme.shapes.large,
                modifier = Modifier.fillMaxWidth()
            )

            if (editId > 0 && form.replyOrderOptions.isNotEmpty()) {
                Box {
                    OutlinedButton(onClick = { form.replyOrderMenu = true }, shape = MaterialTheme.shapes.large) {
                        Text("回帖排序：${form.replyOrderOptions.firstOrNull { it.first == form.replyOrder }?.second.orEmpty()}")
                    }
                    DropdownMenu(expanded = form.replyOrderMenu, onDismissRequest = { form.replyOrderMenu = false }) {
                        form.replyOrderOptions.forEach { (value, label) ->
                            DropdownMenuItem(
                                text = { Text(label) },
                                onClick = { form.replyOrder = value; form.replyOrderMenu = false },
                            )
                        }
                    }
                }
            }

            if (!previewMode) sb.linux.client.ui.content.MarkdownEditorTools(
                onInsert = { b, a, p -> onInsert(b, a, p) },
                onUpload = { onUploadImage() },
                uploading = form.imageUploadBusy,
            )

            if (previewMode) {
                // 预览：Markdown → HTML 后用帖子同款渲染器展示（表格/代码块/链接等所见即所得）
                val previewHtml = remember(body) { HtmlTextConversion.markdownToHtml(body) }
                Surface(
                    shape = MaterialTheme.shapes.large,
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
                            modifier = Modifier.padding(LocalAppSpacing.current.pageHorizontal)
                        )
                    } else {
                        sb.linux.client.ui.content.HtmlContent(
                            html = previewHtml,
                            modifier = Modifier.padding(LocalAppSpacing.current.pageHorizontal),
                            mergeImages = false,
                            // 每敲一个键都是全新 HTML，不写共享解析缓存（否则挤掉阅读中的楼层）
                            cacheable = false
                        )
                    }
                }
            } else {
                OutlinedTextField(
                    value = TextFieldValue(body, bodySelection),
                    onValueChange = { onBodyChange(it) },
                    label = { Text("内容 (Markdown)") },
                    shape = MaterialTheme.shapes.large,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 260.dp)
                        .bringIntoViewRequester(bodyBringIntoViewRequester)
                        .onFocusChanged { form.bodyFocused = it.isFocused },
                    textStyle = TextStyle(fontFamily = FontFamily.Monospace)
                )
            }
        }
}

