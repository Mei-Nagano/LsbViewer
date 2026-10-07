package sb.linux.client.ui.screens

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import sb.linux.client.domain.topic.ForumOption
import sb.linux.client.domain.topic.PrizeRow
import sb.linux.client.domain.topic.SourceEditMeta

internal class NewTopicFormState {
    var forums by mutableStateOf<List<ForumOption>>(emptyList())
    var forumId by mutableStateOf<String?>(null)
    var forumExpanded by mutableStateOf(false)
    var title by mutableStateOf("")
    var internalId by mutableStateOf("0")
    var specialType by mutableStateOf("")
    var drawAt by mutableStateOf("")
    var participantTarget by mutableStateOf("0")
    var minReplyChars by mutableStateOf("5")
    var replyCaptcha by mutableStateOf(true)
    var prizes by mutableStateOf(listOf(PrizeRow()))
    var cardName by mutableStateOf("")
    var cardCurrency by mutableStateOf("points")
    var cardPrice by mutableStateOf("1")
    var cardLimit by mutableStateOf("1")
    var cardAutoReply by mutableStateOf(false)
    var cardAutoReplyContent by mutableStateOf("已成功兑换虚拟卡「{card_name}」。")
    var cardValues by mutableStateOf("")
    var currencyMenu by mutableStateOf(false)
    var prizeTypeMenu by mutableStateOf<Int?>(null)
    var announcement by mutableStateOf("")
    var sourceEditMeta by mutableStateOf(SourceEditMeta())
    var replyOrder by mutableStateOf("")
    var replyOrderOptions by mutableStateOf<List<Pair<String, String>>>(emptyList())
    var replyOrderMenu by mutableStateOf(false)
    var loading by mutableStateOf(true)
    var busy by mutableStateOf(false)
    var error by mutableStateOf<String?>(null)
    var confirmOpen by mutableStateOf(false)
    var restoreDraftOpen by mutableStateOf(false)
    var exitDraftOpen by mutableStateOf(false)
    var draftMenuOpen by mutableStateOf(false)
    var previewMode by mutableStateOf(false)
    var imageUploadBusy by mutableStateOf(false)
    var bodyFocused by mutableStateOf(false)
}
