package sb.linux.client.domain.topic

import sb.linux.client.domain.account.AccountForm

enum class InviteRelayActionStyle {
    DEFAULT,
    PRIMARY,
    HANDOFF_PRIMARY,
    MANAGE,
}

data class InviteRelayAction(
    val label: String,
    val url: String,
    val style: InviteRelayActionStyle = InviteRelayActionStyle.DEFAULT,
    val opensDialog: Boolean = true,
)

/** 帖子页主楼下方的邀请码接力面板（.invite-relay-panel）。 */
data class InviteRelayPanel(
    val topicId: Long,
    val title: String = "邀请码接力",
    val awaitingHandoff: Boolean = false,
    val pendingBadge: String = "",
    val stockText: String = "",
    val totalText: String = "",
    val quotaText: String = "",
    val forceNoticeTitle: String = "",
    val forceNoticeBody: String = "",
    val deadlineText: String = "",
    val statusNotes: List<String> = emptyList(),
    val actions: List<InviteRelayAction> = emptyList(),
)

/** 邀请码接力弹窗内的文件上传控件定义。 */
data class InviteRelayFileField(
    val name: String,
    val label: String,
    val accept: String = "image/*",
    val required: Boolean = false,
)

/** 邀请码接力弹窗内表单提交按钮的确认元数据。 */
data class InviteRelaySubmitMeta(
    val confirmMessage: String = "",
    val claimConfirm: Boolean = false,
)

/** 邀请码接力弹窗内表单（在 AccountForm 基础上补充文件字段与源站特有确认协议）。 */
data class InviteRelayForm(
    val form: AccountForm,
    val fileFields: List<InviteRelayFileField> = emptyList(),
    val confirmMessage: String = "",
    val publishConfirm: Boolean = false,
    val submitMeta: Map<Int, InviteRelaySubmitMeta> = emptyMap(),
    val isInventoryBatch: Boolean = false,
)

/** 邀请码接力弹窗内可渲染的语义节点（严格对应源站 .invite-relay-page DOM）。 */
sealed interface InviteRelayNode {
    data class Text(
        val value: String,
        val heading: Int = 0,
        val style: TextStyleKind = TextStyleKind.BODY,
    ) : InviteRelayNode

    data class CodeBox(
        val label: String,
        val code: String,
        val copyLabel: String = "复制",
    ) : InviteRelayNode

    data class ActionBar(val actions: List<InviteRelayAction>) : InviteRelayNode
    data class Link(val label: String, val url: String) : InviteRelayNode
    data class EvidenceImage(val url: String, val description: String = "") : InviteRelayNode
    data class Form(val value: InviteRelayForm) : InviteRelayNode
    data class Details(val summary: String, val openByDefault: Boolean, val children: List<InviteRelayNode>) : InviteRelayNode
    data class Card(val title: String = "", val children: List<InviteRelayNode>) : InviteRelayNode

    enum class TextStyleKind {
        BODY,
        MUTED,
        DEADLINE,
        WARNING,
        PENDING,
    }
}

data class InviteRelayViewData(
    val url: String,
    val title: String,
    val nodes: List<InviteRelayNode>,
)

data class InviteRelaySubmitResult(
    val topicId: Long,
    val viewUrl: String,
    val notice: String,
    val updatedPanel: InviteRelayPanel? = null,
)

data class InviteRelayFileUpload(
    val fieldName: String,
    val fileName: String,
    val mimeType: String,
    val bytes: ByteArray,
)

interface InviteRelayRepository {
    suspend fun loadView(url: String): InviteRelayViewData
    suspend fun submitForm(
        actionUrl: String,
        fields: List<Pair<String, String>>,
        files: List<InviteRelayFileUpload> = emptyList(),
    ): InviteRelaySubmitResult
}
