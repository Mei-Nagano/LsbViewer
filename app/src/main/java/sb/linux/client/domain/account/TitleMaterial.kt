package sb.linux.client.domain.account

import sb.linux.client.domain.topic.TitleBadge

data class TitleMaterial(val badge: TitleBadge, val description: String, val stock: String,
    val selector: AccountField, val quantity: AccountField, val formId: String)
data class TitleMaterialAction(val form: AccountForm, val source: String, val target: String,
    val cost: Int, val price: Long, val countName: String)
