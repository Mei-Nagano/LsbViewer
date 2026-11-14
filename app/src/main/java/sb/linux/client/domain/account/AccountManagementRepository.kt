package sb.linux.client.domain.account

/** 源站主体的原生显示模型；不包含网页布局、脚本或会话凭据持久化。 */
data class AccountManagementPage(
    val url: String, val nodes: List<AccountNode>,
    val titleCollection: TitleCollectionSummary? = null,
    val sections: List<AccountNode.Link> = emptyList(),
    val titleHeader: AccountTitleHeader? = null,
)

data class AccountTitleHeader(val title: String, val description: String, val stat: String)

data class TitleCollectionSummary(val total: String, val equipped: String)

sealed interface AccountNode {
    data class Text(val value: String, val heading: Int = 0) : AccountNode
    data class Link(val label: String, val url: String) : AccountNode
    data class Image(val url: String, val description: String) : AccountNode
    data class Progress(val fraction: Float, val label: String = "") : AccountNode
    data class Group(val children: List<AccountNode>) : AccountNode
    data class Form(val value: AccountForm) : AccountNode
    data class Activity(
        val label: String, val score: String, val tier: String, val status: String,
        val metrics: List<AccountActivityMetric>, val reasons: List<String>,
    ) : AccountNode
    data class Details(val title: String, val children: List<AccountNode>) : AccountNode
    data class Grid(val children: List<AccountNode>, val columns: Int = 3) : AccountNode
    data class Badge(val value: sb.linux.client.domain.topic.TitleBadge) : AccountNode
    data class Badges(val values: List<sb.linux.client.domain.topic.TitleBadge>) : AccountNode
    data class Gift(val form: AccountForm) : AccountNode
    data class DrawActions(val forms: List<AccountForm>) : AccountNode
    data class OwnedTitle(val badge: sb.linux.client.domain.topic.TitleBadge, val meta: List<String>, val description: String, val actions: List<AccountForm>, val gift: AccountForm?, val equipped: Boolean) : AccountNode
    data class TitleCard(val children: List<AccountNode>, val equipped: Boolean = false) : AccountNode
    data class RarityStat(val rarity: String, val count: String, val rate: String, val style: String) : AccountNode
    data class Pills(val values: List<Pair<String, Boolean>>) : AccountNode
    data class News(val values: List<String>) : AccountNode
    data class Notice(val text: String) : AccountNode
    data class TitleMaterials(val actions: List<TitleMaterialAction>, val materials: List<TitleMaterial>) : AccountNode
    data class Section(val title: String, val description: String, val children: List<AccountNode>) : AccountNode
    data class Actions(val children: List<AccountNode>) : AccountNode
    data class Statistics(val values: List<Pair<String, String>>) : AccountNode
    data class Storage(val title: String, val description: String, val available: String, val fraction: Float,
        val statistics: List<Pair<String, String>>, val form: AccountForm?, val links: List<Link>) : AccountNode
    data class Attachments(val filters: List<Link>, val activeFilter: String, val files: List<ManagedAttachment>,
        val batchForm: AccountForm?) : AccountNode
    data class Promotion(val title: String, val description: String, val previewUrl: String, val targetUrl: String,
        val actions: List<AccountNode>, val previewLabel: String = "") : AccountNode
    data class Invitation(val link: String, val description: String, val status: String, val remaining: String,
        val escrow: String, val cost: String, val form: AccountForm?, val rule: String) : AccountNode
}

data class ManagedAttachment(val name: String, val url: String, val preview: String, val meta: String,
    val references: List<AccountNode.Link>, val referenceText: String, val selector: AccountField?,
    val actions: List<AttachmentAction>)

data class AttachmentAction(val form: AccountForm, val button: AccountSubmit)

data class AccountActivityMetric(
    val label: String, val value: String, val score: String,
    val fraction: Float?, val rule: String, val notes: List<String>,
)

data class AccountForm(
    val id: String,
    val action: String,
    val method: String,
    val hidden: List<Pair<String, String>>,
    val fields: List<AccountField>,
    val buttons: List<AccountSubmit>,
    val notes: List<String>,
    val recipientLookup: String = "",
    val imageUpload: String = "",
    val confirmation: String = "",
    val verification: LoginVerification? = null,
)

data class AccountField(
    val name: String, val type: String, val label: String, val value: String,
    val checked: Boolean = false, val disabled: Boolean = false,
    val required: Boolean = false, val maxLength: Int = Int.MAX_VALUE,
    val min: String = "", val max: String = "", val placeholder: String = "",
    val options: List<AccountOption> = emptyList(), val multiple: Boolean = false,
    val showWhenName: String = "", val showWhenValue: String = "", val showWhenDifferent: Boolean = false,
    val visibilityControl: Boolean = false,
)

data class AccountOption(val value: String, val label: String, val selected: Boolean, val disabled: Boolean)
data class AccountSubmit(val label: String, val name: String, val value: String, val enabled: Boolean,
    val action: String = "", val method: String = "")
data class AccountRecipient(val id: Long, val name: String)

interface AccountManagementRepository {
    suspend fun load(url: String): AccountManagementPage
    suspend fun submit(pageUrl: String, form: AccountForm, fields: List<Pair<String, String>>): AccountManagementPage
    suspend fun resolveRecipient(url: String, query: String): AccountRecipient
    suspend fun uploadPromotionImage(url: String, fields: List<Pair<String, String>>, file: java.io.File): String
}
