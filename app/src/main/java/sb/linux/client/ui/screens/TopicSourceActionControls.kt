package sb.linux.client.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import sb.linux.client.domain.account.*
import sb.linux.client.domain.topic.*
import sb.linux.client.ui.feature.topic.TopicInteractionViewModel
import sb.linux.client.ui.session.Session

internal data class TopicSourceActionContext(val page: TopicPageData, val session: Session,
    val interactions: TopicInteractionViewModel, val onRefresh: () -> Unit,
    val onOpenSource: (String) -> Unit = {})
internal val LocalTopicSourceActions = staticCompositionLocalOf<TopicSourceActionContext?> { null }

/** 只有服务端下发的可操作表单才显示置顶／认可；不以本地称号名推算权限。 */
@Composable
internal fun TopicPostSourceActions(post: PostEntry) {
    val context = LocalTopicSourceActions.current ?: return
    if (!context.session.account.loginState.loggedIn || post.sourceActions.none { it.buttons.any { button -> button.enabled } }) return
    TopicActionForms(post.sourceActions, context)
}

@Composable
private fun TopicActionForms(forms: List<AccountForm>, context: TopicSourceActionContext) {
    val owner = context.session.accountIdentityVersion
    var busy by remember(forms, owner) { mutableStateOf(false) }
    CompositionLocalProvider(LocalAccountVerificationGateway provides context.session.accountVerificationGateway) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            forms.forEach { form -> NativeAccountForm(form, context.session.accountManagementRepository,
                !busy, onDirty = {}, onSubmit = { submitted, fields ->
                    if (!busy && context.session.accountIdentityVersion == owner) {
                        busy = true
                        context.interactions.submitSourceAction(context.page, submitted, fields) { result ->
                            if (context.session.accountIdentityVersion == owner) {
                                busy = false
                                result.onSuccess { value ->
                                    context.session.showToast(value.message)
                                    if (value.succeeded) { context.session.refreshSession(); context.onRefresh() }
                                }.onFailure { context.session.showToast(it.message ?: "操作失败") }
                            }
                        }
                    }
                }, embedded = true, compact = form.fields.isEmpty()) }
            if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
        }
    }
}

@Composable
internal fun TopicFooterSourceActions(page: TopicPageData, session: Session, interactions: TopicInteractionViewModel,
    onRefresh: () -> Unit) {
    if (page.footerPanels.isEmpty() || !session.account.loginState.loggedIn) return
    val owner = session.accountIdentityVersion
    var open by remember(page, owner) { mutableStateOf<String?>(null) }
    var panel by remember(page, owner) { mutableStateOf<AccountManagementPage?>(null) }
    var loading by remember(page, owner) { mutableStateOf(false) }
    var error by remember(page, owner) { mutableStateOf<String?>(null) }
    Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        page.footerPanels.forEach { link -> OutlinedButton(shape = RoundedCornerShape(12.dp), enabled = !loading, onClick = {
            if (open == link.url && error == null) { open = null; panel = null }
            else {
                open = link.url; panel = null; error = null; loading = true
                interactions.loadSourcePanel(page, link.url) { result ->
                    if (session.accountIdentityVersion == owner) {
                        loading = false
                        result.onSuccess { panel = it }.onFailure { error = it.message ?: "读取操作失败" }
                    }
                }
            }
        }) { Text(link.label) } }
        if (loading) LinearProgressIndicator(Modifier.fillMaxWidth())
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        panel?.let { value ->
            value.nodes.filterIsInstance<AccountNode.Text>().forEach { Text(it.value, style = MaterialTheme.typography.bodySmall) }
            TopicActionForms(value.nodes.filterIsInstance<AccountNode.Form>().map { it.value }, TopicSourceActionContext(page, session, interactions, onRefresh))
        }
    }
}
